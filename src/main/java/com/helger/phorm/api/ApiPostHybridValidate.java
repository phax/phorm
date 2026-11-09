/*
 * Copyright (C) 2022-2026 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.phorm.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.VisibleForTesting;
import com.helger.base.io.stream.StreamHelper;
import com.helger.base.string.StringHelper;
import com.helger.base.string.StringImplode;
import com.helger.base.wrapper.Wrapper;
import com.helger.ddd.DocumentDetails;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.diver.api.coord.DVRCoordinate;
import com.helger.http.CHttp;
import com.helger.http.header.specific.AcceptMimeTypeList;
import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;
import com.helger.json.serialize.JsonWriter;
import com.helger.json.serialize.JsonWriterSettings;
import com.helger.kaltblut.core.extract.HybridExtractor;
import com.helger.kaltblut.core.inspect.HybridInspector;
import com.helger.kaltblut.core.model.EZugferdCountry;
import com.helger.kaltblut.core.model.EZugferdFlavor;
import com.helger.kaltblut.core.model.HybridMetadata;
import com.helger.kaltblut.core.source.HybridLimits;
import com.helger.kaltblut.core.source.HybridSource;
import com.helger.kaltblut.core.source.IHybridSource;
import com.helger.kaltblut.core.validate.HybridValidationLayer;
import com.helger.kaltblut.core.validate.HybridValidationResult;
import com.helger.kaltblut.core.validate.HybridValidator;
import com.helger.mime.CMimeType;
import com.helger.mime.determine.MimeTypeDeterminator;
import com.helger.phive.api.executorset.IValidationExecutorSet;
import com.helger.phive.api.result.ValidationResult;
import com.helger.phive.api.result.ValidationResultList;
import com.helger.phive.api.source.ValidationSourceBinary;
import com.helger.phive.result.html.PhiveHtmlHelper;
import com.helger.phive.result.json.JsonValidationResultListHelper;
import com.helger.phive.result.xml.XMLValidationResultListHelper;
import com.helger.phive.xml.source.IValidationSourceXML;
import com.helger.phive.xml.source.ValidationSourceXML;
import com.helger.phorm.AppConfig;
import com.helger.phorm.AppVersion;
import com.helger.phorm.CApp;
import com.helger.phorm.ddd.PhormDDD;
import com.helger.phorm.telemetry.CPhormTelemetry;
import com.helger.phorm.telemetry.PhormMetrics;
import com.helger.phorm.validation.AppValidator;
import com.helger.phorm.validation.EHybridSpecificationID;
import com.helger.phorm.validation.HybridFindingConverter;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.schematron.svrl.SVRLResourceError;
import com.helger.servlet.request.RequestHelper;
import com.helger.telemetry.ETelemetrySpanKind;
import com.helger.telemetry.Telemetry;
import com.helger.telemetry.TelemetryAttributes;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroDocument;
import com.helger.xml.microdom.serialize.MicroWriter;
import com.helger.xml.serialize.read.DOMReader;
import com.helger.xml.serialize.write.EXMLSerializeIndent;
import com.helger.xml.serialize.write.XMLWriterSettings;

/**
 * Validate a hybrid invoice (ZUGFeRD / Factur-X PDF + embedded XML) by combining the kaltblut
 * carrier-side validation with the dd_and_validate XML business-rule validation. Both layers are
 * returned in a single phive {@link ValidationResultList}, with the PDF carrier layer first.
 *
 * @author Philip Helger
 */
public class ApiPostHybridValidate extends AbstractAPIInvoker
{
  public static final String QUERY_PARAM_COUNTRY = "country";

  private static final Logger LOGGER = LoggerFactory.getLogger (ApiPostHybridValidate.class);
  private static final AtomicInteger COUNTER = new AtomicInteger (0);

  @Override
  @NonNull
  protected String getEndpointName ()
  {
    return "hybrid_validate";
  }

  @Nullable
  private static EZugferdCountry _parseCountry (@Nullable final String sCountry,
                                                @NonNull @Nonempty final String sLogPrefix)
  {
    if (StringHelper.isEmpty (sCountry))
      return null;

    final EZugferdCountry eCountry = EZugferdCountry.getFromIDOrNull (sCountry.toUpperCase (Locale.ROOT));
    if (eCountry == null)
      LOGGER.warn (sLogPrefix +
                   "Unknown country code '" +
                   sCountry +
                   "' - falling back to default. Valid values are: DE, FR, OTHER");
    return eCountry;
  }

  private static void _checkSpecificationID (@Nullable final HybridMetadata aMetadata,
                                             @Nullable final String sSpecificationID,
                                             @NonNull final String sLogPrefix,
                                             @NonNull final ErrorList aEmbeddedXmlErrors)
  {
    // ZUGFeRD 1.0 uses a different XML syntax and other profiles
    if (aMetadata == null || aMetadata.getFlavor () == null || aMetadata.getFlavor () == EZugferdFlavor.ZUGFERD_1)
      return;

    // Unknown profiles are reported by kaltblut; XRECHNUNG is not covered
    final EHybridSpecificationID eSpecID = EHybridSpecificationID.getFromProfileOrNull (aMetadata.getProfile ());
    if (eSpecID == null)
      return;

    if (!eSpecID.isAllowedSpecificationID (sSpecificationID))
    {
      final String sErrorMsg = "The specification identifier (BT-24) '" +
                               sSpecificationID +
                               "' of the embedded invoice XML is not allowed for the profile '" +
                               eSpecID.getProfile ().getID () +
                               "' declared in the XMP metadata. Allowed values are: " +
                               StringImplode.getImploded (", ", eSpecID.getAllSpecificationIDs ());
      LOGGER.warn (sLogPrefix + sErrorMsg);
      aEmbeddedXmlErrors.add (HybridFindingConverter.createEmbeddedXmlError (HybridFindingConverter.ERROR_ID_SPECIFICATION_ID,
                                                                             sErrorMsg));
    }
  }

  /**
   * Run the hybrid validation: the PDF carrier layers of kaltblut first, then the validation of the
   * embedded XML.
   *
   * @param aPayloadBytes
   *        The PDF bytes. May not be <code>null</code>.
   * @param eCountry
   *        The optional ZUGFeRD country. May be <code>null</code>.
   * @param aDisplayLocale
   *        The display locale to use. May not be <code>null</code>.
   * @param sLogPrefix
   *        The log prefix to use. May not be <code>null</code>.
   * @param aDDConsumer
   *        Invoked with the determined document details of the embedded XML. May not be
   *        <code>null</code>.
   * @param aVESConsumer
   *        Invoked with the validation executor set used for the embedded XML. May not be
   *        <code>null</code>.
   * @return The combined validation result list. Never <code>null</code>.
   */
  @NonNull
  @VisibleForTesting
  static ValidationResultList validateHybrid (final byte @NonNull [] aPayloadBytes,
                                              @Nullable final EZugferdCountry eCountry,
                                              @NonNull final Locale aDisplayLocale,
                                              @NonNull final String sLogPrefix,
                                              @NonNull final Consumer <DocumentDetails> aDDConsumer,
                                              @NonNull final Consumer <IValidationExecutorSet <IValidationSourceXML>> aVESConsumer)
  {
    final IHybridSource aHybridSource = HybridSource.fromBytes (aPayloadBytes);

    final HybridLimits aHybridLimits = HybridLimits.DEFAULTS;

    // 1) Run hybrid PDF carrier validation
    LOGGER.info (sLogPrefix +
                 "Running hybrid PDF carrier validation" +
                 (eCountry != null ? " (country=" + eCountry + ")" : "") +
                 " with limits: " +
                 aHybridLimits);

    final HybridValidator aHybridValidator = new HybridValidator ();
    aHybridValidator.getSettings ().setLimits (aHybridLimits);
    if (eCountry != null)
      aHybridValidator.getSettings ().setCountry (eCountry);

    final HybridValidationResult aHybridResult = Telemetry.withSpan (CPhormTelemetry.SPAN_KALTBLUT_VALIDATE,
                                                                     ETelemetrySpanKind.INTERNAL,
                                                                     aSpan -> {
                                                                       aSpan.setAttribute (CPhormTelemetry.ATTR_PAYLOAD_SIZE_BYTES,
                                                                                           aPayloadBytes.length);
                                                                       if (eCountry != null)
                                                                         aSpan.setAttribute (CPhormTelemetry.ATTR_HYBRID_COUNTRY,
                                                                                             eCountry.name ());

                                                                       try
                                                                       {
                                                                         final HybridValidationResult aRes = aHybridValidator.validate (aHybridSource);
                                                                         aSpan.setAttribute (CPhormTelemetry.ATTR_HYBRID_LAYERS,
                                                                                             aRes.getAllLayers ()
                                                                                                 .size ());
                                                                         PhormMetrics.KALTBLUT_RUNS.add (1,
                                                                                                         TelemetryAttributes.builder ()
                                                                                                                            .put ("outcome",
                                                                                                                                  "ok")
                                                                                                                            .put ("country",
                                                                                                                                  eCountry != null ? eCountry.name ()
                                                                                                                                                   : "none")
                                                                                                                            .build ());
                                                                         return aRes;
                                                                       }
                                                                       catch (final IOException ex)
                                                                       {
                                                                         PhormMetrics.KALTBLUT_RUNS.add (1,
                                                                                                         TelemetryAttributes.builder ()
                                                                                                                            .put ("outcome",
                                                                                                                                  "parse_fail")
                                                                                                                            .put ("country",
                                                                                                                                  eCountry != null ? eCountry.name ()
                                                                                                                                                   : "none")
                                                                                                                            .build ());
                                                                         /*
                                                                          * Fatal: cannot proceed
                                                                          * without parsing the PDF
                                                                          */
                                                                         LOGGER.error (sLogPrefix +
                                                                                       "Failed to parse PDF for hybrid validation",
                                                                                       ex);
                                                                         throw new IllegalStateException ("Failed to parse PDF for hybrid validation: " +
                                                                                                          ex.getMessage (),
                                                                                                          ex);
                                                                       }
                                                                     });

    // Convert to our VRL
    final ValidationResultList aVRL = new ValidationResultList (ValidationSourceBinary.create (null, aPayloadBytes));
    for (final HybridValidationLayer aLayer : aHybridResult.getAllLayers ())
      aVRL.add (HybridFindingConverter.toValidationResult (aLayer));
    final int nCarrierLayerCount = aVRL.size ();

    // Read the XMP metadata for the profile
    HybridMetadata aMetadata;
    try
    {
      aMetadata = HybridInspector.readMetadata (aHybridSource, aHybridLimits);
    }
    catch (final IOException ex)
    {
      LOGGER.warn (sLogPrefix + "Could not read the XMP metadata: " + ex.getMessage ());
      aMetadata = null;
    }

    // All errors on the embedded XML
    final ErrorList aEmbeddedXmlErrors = new ErrorList ();

    // 2) Try to extract and validate the embedded XML

    // Extract the XML from the PDF
    final byte [] aXmlBytes = Telemetry.withSpan (CPhormTelemetry.SPAN_KALTBLUT_EXTRACT_XML,
                                                  ETelemetrySpanKind.INTERNAL,
                                                  aSpan -> {
                                                    try
                                                    {
                                                      final byte [] aBytes = HybridExtractor.extractInvoiceXml (aHybridSource);
                                                      final int nLen = aBytes == null ? 0 : aBytes.length;
                                                      aSpan.setAttribute (CPhormTelemetry.ATTR_HYBRID_XML_EXTRACTED,
                                                                          nLen > 0)
                                                           .setAttribute (CPhormTelemetry.ATTR_HYBRID_XML_BYTES, nLen);
                                                      if (nLen > 0)
                                                      {
                                                        PhormMetrics.KALTBLUT_EMBEDDED_XML_BYTES.record (nLen,
                                                                                                         TelemetryAttributes.builder ()
                                                                                                                            .put ("country",
                                                                                                                                  eCountry != null ? eCountry.name ()
                                                                                                                                                   : "none")
                                                                                                                            .build ());
                                                      }
                                                      return aBytes;
                                                    }
                                                    catch (final IOException ex)
                                                    {
                                                      aSpan.recordException (ex)
                                                           .setAttribute (CPhormTelemetry.ATTR_HYBRID_XML_EXTRACTED,
                                                                          false);
                                                      LOGGER.warn (sLogPrefix +
                                                                   "Could not extract invoice XML: " +
                                                                   ex.getMessage ());
                                                      return null;
                                                    }
                                                  });

    if (aXmlBytes == null || aXmlBytes.length == 0)
    {
      final String sErrorMsg = "No embedded invoice XML could be extracted from the PDF";
      LOGGER.warn (sLogPrefix + sErrorMsg);
      aEmbeddedXmlErrors.add (HybridFindingConverter.createEmbeddedXmlError (null, sErrorMsg));
    }
    else
    {
      LOGGER.info (sLogPrefix + "Successfully extracted " + aXmlBytes.length + " XML bytes from PDF");

      final Document aDoc = DOMReader.readXMLDOM (aXmlBytes);
      if (aDoc == null || aDoc.getDocumentElement () == null)
      {
        final String sErrorMsg = "The embedded invoice XML could not be parsed";
        LOGGER.warn (sLogPrefix + sErrorMsg);
        aEmbeddedXmlErrors.add (HybridFindingConverter.createEmbeddedXmlError (null, sErrorMsg));
      }
      else
      {
        // Run DDD
        final Wrapper <Element> aInnerElement = Wrapper.empty ();
        // No unwrapper callback needed, as hybrid invoices don't contain SBDH or so
        final DocumentDetails aDD = PhormDDD.findDocumentDetails (aDoc.getDocumentElement (), null, aInnerElement::set);
        // Remember the partial document details as well
        if (aDD != null)
        {
          aDDConsumer.accept (aDD);

          // Check the BT-24 against the profile of the XMP metadata
          _checkSpecificationID (aMetadata, aDD.getCustomizationID (), sLogPrefix, aEmbeddedXmlErrors);
        }

        if (aDD == null || !aDD.hasVESID ())
        {
          final String sErrorMsg = "Failed to determine the document type of the embedded invoice XML, so it could not be validated";
          LOGGER.warn (sLogPrefix + sErrorMsg);
          aEmbeddedXmlErrors.add (HybridFindingConverter.createEmbeddedXmlError (null, sErrorMsg));
        }
        else
        {
          final var aVESID = DVRCoordinate.parseOrNull (aDD.getVESID ());
          final var aVES = aVESID == null ? null : AppValidator.getVESOrNull (aVESID);
          if (aVES == null)
          {
            final String sErrorMsg = "The VESID '" +
                                     aDD.getVESID () +
                                     "' of the embedded invoice XML could not be resolved, so it could not be validated";
            LOGGER.warn (sLogPrefix + sErrorMsg);
            aEmbeddedXmlErrors.add (HybridFindingConverter.createEmbeddedXmlError (null, sErrorMsg));
          }
          else
          {
            aVESConsumer.accept (aVES);

            final IValidationSourceXML aValSrc = aInnerElement.isSet () ? ValidationSourceXML.createPartial (null,
                                                                                                             aInnerElement.get ())
                                                                        : ValidationSourceXML.create (null, aDoc);

            LOGGER.info (sLogPrefix + "Performing XML validation using VESID '" + aVESID.getAsSingleID () + "'");

            // Run XML validation
            final ValidationResultList aXmlVRL = AppValidator.validate (aVES, aValSrc, aDisplayLocale, "hybrid");
            for (final ValidationResult aXmlLayer : aXmlVRL)
              aVRL.add (aXmlLayer);
          }
        }
      }
    }

    // The embedded XML layer goes directly after the PDF carrier layers
    if (aEmbeddedXmlErrors.isNotEmpty ())
      aVRL.addAt (nCarrierLayerCount, HybridFindingConverter.createEmbeddedXmlResult (aEmbeddedXmlErrors));

    // Aggregate duration: kaltblut layer + any XML layers
    Duration aTotalDuration = Duration.ZERO;
    for (final ValidationResult aVR : aVRL)
      aTotalDuration = aTotalDuration.plus (aVR.getDuration ());
    aVRL.setValidationDuration (aTotalDuration);

    return aVRL;
  }

  @Override
  public void invokeAPI (@NonNull final IAPIDescriptor aAPIDescriptor,
                         @NonNull @Nonempty final String sPath,
                         @NonNull final Map <String, String> aPathVariables,
                         @NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                         @NonNull final PhotonUnifiedResponse aUnifiedResponse) throws IOException
  {
    aUnifiedResponse.disableCaching ();
    final String sLogPrefix = "[HYBRID-VALIDATE-" +
                              AppVersion.getVersionNumber () +
                              "-" +
                              COUNTER.incrementAndGet () +
                              "] ";

    if (!verifyAuthOrSetForbidden (aRequestScope, aUnifiedResponse, sLogPrefix))
      return;

    // Read the payload as PDF bytes
    LOGGER.info (sLogPrefix + "Reading payload as PDF");
    final byte [] aPayloadBytes = Telemetry.withSpanThrowing (CPhormTelemetry.SPAN_PAYLOAD_READ,
                                                              ETelemetrySpanKind.INTERNAL,
                                                              aSpan -> {
                                                                final byte [] aBytes = StreamHelper.getAllBytes (aRequestScope.getRequest ()
                                                                                                                              .getInputStream ());
                                                                final int nLen = aBytes == null ? 0 : aBytes.length;
                                                                aSpan.setAttribute (CPhormTelemetry.ATTR_PAYLOAD_SIZE_BYTES,
                                                                                    nLen)
                                                                     .setAttribute (CPhormTelemetry.ATTR_PAYLOAD_KIND,
                                                                                    "pdf");
                                                                PhormMetrics.PAYLOAD_BYTES.record (nLen,
                                                                                                   TelemetryAttributes.builder ()
                                                                                                                      .put ("endpoint",
                                                                                                                            getEndpointName ())
                                                                                                                      .put ("kind",
                                                                                                                            "pdf")
                                                                                                                      .build ());
                                                                return aBytes;
                                                              });
    if (aPayloadBytes == null || aPayloadBytes.length == 0)
    {
      final String sErrorMsg = "The request body is empty";
      LOGGER.error (sLogPrefix + sErrorMsg);
      aUnifiedResponse.text (sErrorMsg).setStatus (CHttp.HTTP_BAD_REQUEST);
      return;
    }

    if (!CMimeType.APPLICATION_PDF.equals (MimeTypeDeterminator.getInstance ()
                                                               .getMimeTypeFromBytes (aPayloadBytes, null)))
    {
      final String sErrorMsg = "The request body is not a PDF document";
      LOGGER.error (sLogPrefix + sErrorMsg);
      aUnifiedResponse.text (sErrorMsg).setStatus (CHttp.HTTP_BAD_REQUEST);
      return;
    }

    // Optional country parameter
    final EZugferdCountry eCountry = _parseCountry (aRequestScope.params ().getAsString (QUERY_PARAM_COUNTRY),
                                                    sLogPrefix);

    final Locale aDisplayLocale = CApp.DEFAULT_LOCALE;
    final Wrapper <ValidationResultList> aWrappedVRL = Wrapper.empty ();
    final Wrapper <IValidationExecutorSet <IValidationSourceXML>> aWrappedVES = Wrapper.empty ();
    final Wrapper <DocumentDetails> aWrappedDD = Wrapper.empty ();

    final Runnable aValidationRunnable = () -> {
      final ValidationResultList aVRL = validateHybrid (aPayloadBytes,
                                                        eCountry,
                                                        aDisplayLocale,
                                                        sLogPrefix,
                                                        aWrappedDD::set,
                                                        aWrappedVES::set);
      aWrappedVRL.set (aVRL);

      if (aVRL.getOverallValidity ().isValid ())
      {
        LOGGER.info (sLogPrefix +
                     "Hybrid validation completed and the document is considered valid (" +
                     aVRL.getValidationDuration () +
                     ")");
      }
      else
      {
        LOGGER.error (sLogPrefix +
                      "Hybrid validation completed and the document is considered invalid (" +
                      aVRL.getValidationDuration () +
                      ")");

        if (AppConfig.isUseHttp400OnValidationFailure ())
        {
          aUnifiedResponse.setStatus (CHttp.HTTP_BAD_REQUEST);
        }
      }
    };

    // Don't emit validation source content for PDFs (would be binary)
    final boolean bEmitValidationSourceContent = false;

    final AcceptMimeTypeList aAcceptMimeTypes = RequestHelper.getAcceptMimeTypes (aRequestScope.getRequest ());
    if (aAcceptMimeTypes.explicitlySupportsMimeType (CMimeType.APPLICATION_XML))
    {
      // Provide response as XML
      final IMicroDocument aResultXML = new MicroDocument ();
      final IMicroElement aResultXMLRoot = aResultXML.addElement ("validationResults");
      if (eCountry != null)
        aResultXMLRoot.addElement ("country").addText (eCountry.name ());

      CommonAPIInvoker.invoke (aResultXMLRoot, aValidationRunnable::run);

      if (aWrappedDD.isSet ())
        aWrappedDD.get ().appendToMicroElement (aResultXMLRoot);

      // Perform conversion
      new XMLValidationResultListHelper ().ves (aWrappedVES.get ())
                                          .sourceToXMLDefault (bEmitValidationSourceContent)
                                          .applyTo (aResultXMLRoot, aWrappedVRL.get (), aDisplayLocale);

      if (AppConfig.isLogResponsePayload ())
      {
        LOGGER.info (sLogPrefix +
                     "Response XML is:\n" +
                     MicroWriter.getNodeAsString (aResultXML,
                                                  new XMLWriterSettings ().setIndent (EXMLSerializeIndent.INDENT_AND_ALIGN)));
      }
      aUnifiedResponse.xml (aResultXML);
    }
    else
      if (aAcceptMimeTypes.explicitlySupportsMimeType (CMimeType.TEXT_HTML))
      {
        // Provide response as HTML
        aValidationRunnable.run ();

        // Perform conversion
        final String sResultHtml = new PhiveHtmlHelper (aDisplayLocale).useDefaultCSS ()
                                                                       .ves (aWrappedVES.get ())
                                                                       .errorTestExtractor ((error,
                                                                                             locale) -> error instanceof final SVRLResourceError aSvrlError ? aSvrlError.getTest ()
                                                                                                                                                            : null)
                                                                       .sourceData (null)
                                                                       .createHtml (aWrappedVRL.get (),
                                                                                    new XMLWriterSettings ().setIndent (EXMLSerializeIndent.INDENT_AND_ALIGN));

        if (AppConfig.isLogResponsePayload ())
        {
          LOGGER.info (sLogPrefix + "Response HTML is:\n" + sResultHtml);
        }
        aUnifiedResponse.setContentAndCharset (sResultHtml, StandardCharsets.UTF_8).setMimeType (CMimeType.TEXT_HTML);
      }
      else
      {
        // Provide response as JSON
        final IJsonObject aResultJson = new JsonObject ();
        if (eCountry != null)
          aResultJson.add ("country", eCountry.name ());

        CommonAPIInvoker.invoke (aResultJson, aValidationRunnable::run);

        // Document defaults to JSON
        if (aWrappedDD.isSet ())
          aResultJson.add ("documentDetails", aWrappedDD.get ().getAsJson ());

        // Validation results to JSON
        if (aWrappedVRL.isSet ())
          new JsonValidationResultListHelper ().ves (aWrappedVES.get ())
                                               .sourceToJsonDefault (bEmitValidationSourceContent)
                                               .applyTo (aResultJson, aWrappedVRL.get (), aDisplayLocale);

        if (AppConfig.isLogResponsePayload ())
        {
          LOGGER.info (sLogPrefix +
                       "Response JSON is:\n" +
                       new JsonWriter (JsonWriterSettings.DEFAULT_SETTINGS_FORMATTED).writeAsString (aResultJson));
        }
        aUnifiedResponse.json (aResultJson);
      }
  }
}
