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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unece.cefact.namespaces.sbdh.StandardBusinessDocument;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.helger.annotation.Nonempty;
import com.helger.base.io.stream.StreamHelper;
import com.helger.base.numeric.mutable.MutableBoolean;
import com.helger.base.timing.StopWatch;
import com.helger.base.wrapper.Wrapper;
import com.helger.ddd.DocumentDetails;
import com.helger.ddd.IDDDDocumentUnwrappingCallback;
import com.helger.ddd.unwrap.DDDDocumentUnwrapperSBDH;
import com.helger.diagnostics.error.SingleError;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.diver.api.coord.DVRCoordinate;
import com.helger.http.CHttp;
import com.helger.http.header.specific.AcceptMimeTypeList;
import com.helger.io.resource.ClassPathResource;
import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;
import com.helger.json.serialize.JsonWriter;
import com.helger.json.serialize.JsonWriterSettings;
import com.helger.mime.CMimeType;
import com.helger.peppol.sbdh.PeppolSBDHData;
import com.helger.peppol.sbdh.PeppolSBDHDataReader;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.peppolid.peppol.PeppolIdentifierHelper;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.phive.api.EValidationBaseType;
import com.helger.phive.api.ValidationType;
import com.helger.phive.api.artefact.IValidationArtefact;
import com.helger.phive.api.artefact.ValidationArtefact;
import com.helger.phive.api.executorset.IValidationExecutorSet;
import com.helger.phive.api.result.ValidationResult;
import com.helger.phive.api.result.ValidationResultList;
import com.helger.phive.api.validity.EExtendedValidity;
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
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.sbdh.SBDMarshaller;
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
 * Perform document type determination and than validation only via API
 *
 * @author Philip Helger
 */
public class ApiPostDetermineDocTypeAndValidate extends AbstractAPIInvoker
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ApiPostDetermineDocTypeAndValidate.class);
  private static final AtomicInteger COUNTER = new AtomicInteger (0);

  // Validation artefact representing the Peppol SBDH (envelope) validation layer that is performed
  // in Java code (not phive rule based) while unwrapping the document.
  private static final IValidationArtefact ARTEFACT_PEPPOL_SBDH = new ValidationArtefact (new ValidationType ("peppol-sbdh",
                                                                                                              EValidationBaseType.OTHER,
                                                                                                              "Peppol SBDH",
                                                                                                              false,
                                                                                                              false),
                                                                                          new ClassPathResource ("peppol-sbdh/PeppolSBDHDataReader"));

  @Override
  @NonNull
  protected String getEndpointName ()
  {
    return "dd_and_validate";
  }

  private static boolean _isPeppolBISBilling (@Nullable final IDocumentTypeIdentifier aDocumentTypeID)
  {
    if (aDocumentTypeID == null)
      return false;

    // Peppol BIS Billing
    // Peppol BIS Self-Billing
    if (EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30.hasSameContent (aDocumentTypeID) ||
        EPredefinedDocumentTypeIdentifier.CREDITNOTE_EN16931_PEPPOL_V30.hasSameContent (aDocumentTypeID) ||
        EPredefinedDocumentTypeIdentifier.INVOICE_CEN_EU_EN16931_2017_COMPLIANT_FDC_PEPPOL_EU_2017_POACC_SELFBILLING_3_0.hasSameContent (aDocumentTypeID) ||
        EPredefinedDocumentTypeIdentifier.CREDITNOTE_CEN_EU_EN16931_2017_COMPLIANT_FDC_PEPPOL_EU_2017_POACC_SELFBILLING_3_0.hasSameContent (aDocumentTypeID))
      return true;

    // Any Peppol PINT
    if (PeppolIdentifierHelper.DOCUMENT_TYPE_SCHEME_PEPPOL_DOCTYPE_WILDCARD.equals (aDocumentTypeID.getScheme ()) &&
        aDocumentTypeID.getValue ().contains (PeppolIdentifierHelper.PEPPOL_PINT_INDICATOR))
      return true;

    // Some other document
    return false;
  }

  @Override
  public void invokeAPI (@NonNull final IAPIDescriptor aAPIDescriptor,
                         @NonNull @Nonempty final String sPath,
                         @NonNull final Map <String, String> aPathVariables,
                         @NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                         @NonNull final PhotonUnifiedResponse aUnifiedResponse) throws IOException
  {
    aUnifiedResponse.disableCaching ();
    final String sLogPrefix = "[DD+VALIDATE-" +
                              AppVersion.getVersionNumber () +
                              "-" +
                              COUNTER.incrementAndGet () +
                              "] ";

    if (!verifyAuthOrSetForbidden (aRequestScope, aUnifiedResponse, sLogPrefix))
      return;

    // Read the payload as XML
    LOGGER.info (sLogPrefix + "Trying to read payload as XML");
    final byte [] aPayloadBytes = Telemetry.withSpanThrowing (CPhormTelemetry.SPAN_PAYLOAD_READ,
                                                              ETelemetrySpanKind.INTERNAL,
                                                              aSpan -> {
                                                                final byte [] aBytes = StreamHelper.getAllBytes (aRequestScope.getRequest ()
                                                                                                                              .getInputStream ());
                                                                final int nLen = aBytes == null ? 0 : aBytes.length;
                                                                aSpan.setAttribute (CPhormTelemetry.ATTR_PAYLOAD_SIZE_BYTES,
                                                                                    nLen)
                                                                     .setAttribute (CPhormTelemetry.ATTR_PAYLOAD_KIND,
                                                                                    "xml");
                                                                PhormMetrics.PAYLOAD_BYTES.record (nLen,
                                                                                                   TelemetryAttributes.builder ()
                                                                                                                      .put ("endpoint",
                                                                                                                            getEndpointName ())
                                                                                                                      .put ("kind",
                                                                                                                            "xml")
                                                                                                                      .build ());
                                                                return aBytes;
                                                              });

    final Document aDoc = Telemetry.withSpan (CPhormTelemetry.SPAN_XML_PARSE, ETelemetrySpanKind.INTERNAL, aSpan -> {
      final Document aD = DOMReader.readXMLDOM (aPayloadBytes);
      if (aD != null && aD.getDocumentElement () != null)
      {
        aSpan.setAttribute (CPhormTelemetry.ATTR_XML_ROOT_LOCALNAME, aD.getDocumentElement ().getLocalName ())
             .setAttribute (CPhormTelemetry.ATTR_XML_ROOT_NAMESPACE, aD.getDocumentElement ().getNamespaceURI ());
      }
      return aD;
    });

    if (aDoc == null || aDoc.getDocumentElement () == null)
    {
      final String sErrorMsg = "Failed to read the message body as XML";
      LOGGER.error (sLogPrefix + sErrorMsg);
      aUnifiedResponse.text (sErrorMsg).setStatus (CHttp.HTTP_BAD_REQUEST);
      return;
    }

    // Determine document details
    LOGGER.info (sLogPrefix + "Trying to determine document details");
    final Wrapper <Element> aWrapperInnerElement = Wrapper.empty ();
    final MutableBoolean aSbdhValidated = new MutableBoolean (false);
    final Wrapper <StandardBusinessDocument> aWrapperParsedSbd = Wrapper.empty ();
    final Wrapper <Duration> aWrapperSbdhValidationDuration = Wrapper.of (Duration.ZERO);
    final ErrorList aSbdhErrorList = new ErrorList ();
    final IDDDDocumentUnwrappingCallback aUnwrappingCallback = (aUnwrapper, aOuterElement, aInnerElement) -> {
      // Was it an SBDH?
      if (DDDDocumentUnwrapperSBDH.WRAPPING_TYPE.equals (aUnwrapper.getWrappingType ()))
      {
        // Yes, it's an unwrapped SBDH
        aSbdhValidated.set (true);

        final StopWatch aSW = StopWatch.createdStarted ();

        // Parse as SBD and remember errors
        final StandardBusinessDocument aSBD = new SBDMarshaller ().setCollectErrors (aSbdhErrorList)
                                                                  .read (aOuterElement);
        if (aSBD != null)
        {
          aWrapperParsedSbd.set (aSBD);
          // Validate as Peppol SBDH
          new PeppolSBDHDataReader (PeppolIdentifierFactory.INSTANCE).validateData (aSBD.getStandardBusinessDocumentHeader (),
                                                                                    aInnerElement,
                                                                                    aSbdhErrorList);
        }
        aSW.stop ();
        aWrapperSbdhValidationDuration.set (aSW.getDuration ());
      }
    };

    final DocumentDetails aDD = PhormDDD.findDocumentDetails (aDoc.getDocumentElement (),
                                                              aUnwrappingCallback,
                                                              aWrapperInnerElement::set);
    if (aDD == null || !aDD.hasVESID ())
    {
      final String sErrorMsg = "Failed to determine the document type details";
      LOGGER.error (sLogPrefix + sErrorMsg);
      aUnifiedResponse.text (sErrorMsg).setStatus (CHttp.HTTP_BAD_REQUEST);
      return;
    }

    // If the payload was an SBDH, validate the inner element instead
    final IValidationSourceXML aValSrc = aWrapperInnerElement.isSet () ? ValidationSourceXML.createPartial (null,
                                                                                                            aWrapperInnerElement.get ())
                                                                       : ValidationSourceXML.create (null, aDoc);

    final String sVESID = aDD.getVESID ();
    final DVRCoordinate aVESID = DVRCoordinate.parseOrNull (sVESID);
    if (aVESID == null)
    {
      final String sErrorMsg = "The VESID '" + sVESID + "' could not be parsed.";
      LOGGER.error (sLogPrefix + sErrorMsg);
      aUnifiedResponse.text (sErrorMsg).setStatus (CHttp.HTTP_BAD_REQUEST);
      return;
    }

    final var aVES = Telemetry.withSpan (CPhormTelemetry.SPAN_VESID_RESOLVE, ETelemetrySpanKind.INTERNAL, aSpan -> {
      aSpan.setAttribute (CPhormTelemetry.ATTR_VESID, aVESID.getAsSingleID ());
      final IValidationExecutorSet <IValidationSourceXML> aResolved = AppValidator.getVESOrNull (aVESID);
      final boolean bResolved = aResolved != null;
      final boolean bDeprecated = bResolved && aResolved.getStatus ().isDeprecated ();
      aSpan.setAttribute (CPhormTelemetry.ATTR_VESID_RESOLVED, bResolved)
           .setAttribute (CPhormTelemetry.ATTR_VESID_DEPRECATED, bDeprecated);
      PhormMetrics.VESID_RESOLUTIONS.add (1,
                                          TelemetryAttributes.builder ()
                                                             .put ("resolved", bResolved)
                                                             .put ("deprecated", bDeprecated)
                                                             .build ());
      return aResolved;
    });
    if (aVES == null)
    {
      final String sErrorMsg = "The VESID '" + sVESID + "' could not be resolved.";
      LOGGER.error (sLogPrefix + sErrorMsg);
      aUnifiedResponse.text (sErrorMsg).setStatus (CHttp.HTTP_BAD_REQUEST);
      return;
    }

    final Locale aDisplayLocale = CApp.DEFAULT_LOCALE;
    final Wrapper <ValidationResultList> aWrappedVRL = Wrapper.empty ();

    final Runnable aValidationRunnable = () -> {
      // validation
      LOGGER.info (sLogPrefix + "Performing validation using VESID '" + aVESID.getAsSingleID () + "'");

      // Perform main validation
      final ValidationResultList aValidationResultList = AppValidator.validate (aVES, aValSrc, aDisplayLocale, "dd");

      // If the payload was wrapped in a Peppol SBDH, the SBDH was validated during unwrapping.
      // Include those results as the first (envelope) validation layer.
      if (aSbdhValidated.booleanValue ())
      {
        // If it was SBDH wrapped, and the SBDH is correct under Peppol rules and if the payload is
        // a Peppol BIS Billing document, verify the Endpoint ID references as well
        if (aSbdhErrorList.containsNoError () && _isPeppolBISBilling (aDD.getDocumentTypeID ()))
        {
          // Convert the identifiers to Peppol Identifiers
          final IParticipantIdentifier aDDSender = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifier (aDD.getSenderID ());
          final IParticipantIdentifier aDDReceiver = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifier (aDD.getReceiverID ());
          if (aDDSender != null || aDDReceiver != null)
          {
            final PeppolSBDHData aPeppolSBD = new PeppolSBDHDataReader (PeppolIdentifierFactory.INSTANCE).extractDataUnchecked (aWrapperParsedSbd.get ()
                                                                                                                                                 .getStandardBusinessDocumentHeader (),
                                                                                                                                aWrapperInnerElement.get ());
            if (aDDSender != null && !aPeppolSBD.getSenderAsIdentifier ().hasSameContent (aDDSender))
            {
              aSbdhErrorList.add (SingleError.builderError ()
                                             .errorFieldName ("Sender/Identifier")
                                             .errorText ("The SBDH sender '" +
                                                         aPeppolSBD.getSenderURIEncoded () +
                                                         "' differs from the payload sender ID '" +
                                                         aDDSender.getURIEncoded () +
                                                         "' - they must match according to Peppol BIS Billing rules")
                                             .build ());
            }
            if (aDDReceiver != null && !aPeppolSBD.getReceiverAsIdentifier ().hasSameContent (aDDReceiver))
            {
              aSbdhErrorList.add (SingleError.builderError ()
                                             .errorFieldName ("Receiver/Identifier")
                                             .errorText ("The SBDH receiver '" +
                                                         aPeppolSBD.getReceiverURIEncoded () +
                                                         "' differs from the payload receiver ID '" +
                                                         aDDReceiver.getURIEncoded () +
                                                         "' - they must match according to Peppol BIS Billing rules")
                                             .build ());
            }
          }
        }

        final EExtendedValidity eSbdhValidity = aSbdhErrorList.containsAtLeastOneError () ? EExtendedValidity.INVALID
                                                                                          : EExtendedValidity.VALID;
        aValidationResultList.addAt (0,
                                     new ValidationResult (ARTEFACT_PEPPOL_SBDH,
                                                           aSbdhErrorList,
                                                           eSbdhValidity,
                                                           aWrapperSbdhValidationDuration.get ().toMillis ()));

        // Increase overall validation duration
        if (aValidationResultList.getValidationDuration () != null)
          aValidationResultList.setValidationDuration (aValidationResultList.getValidationDuration ()
                                                                            .plus (aWrapperSbdhValidationDuration.get ()));
      }

      aWrappedVRL.set (aValidationResultList);

      if (aValidationResultList.getOverallValidity ().isValid ())
      {
        LOGGER.info (sLogPrefix +
                     "Validation was performed and the document is considered valid (" +
                     aValidationResultList.getValidationDuration () +
                     ")");
      }
      else
      {
        LOGGER.error (sLogPrefix +
                      "Don't send out the document as the document is considered invalid (" +
                      aValidationResultList.getValidationDuration () +
                      ")");

        if (AppConfig.isUseHttp400OnValidationFailure ())
        {
          // Return error status
          aUnifiedResponse.setStatus (CHttp.HTTP_BAD_REQUEST);
        }
      }
    };

    // Don't emit validation source content
    final boolean bEmitValidationSourceContent = false;

    final AcceptMimeTypeList aAcceptMimeTypes = RequestHelper.getAcceptMimeTypes (aRequestScope.getRequest ());
    if (aAcceptMimeTypes.explicitlySupportsMimeType (CMimeType.APPLICATION_XML))
    {
      // Provide response as XML
      final IMicroDocument aResultXML = new MicroDocument ();
      final IMicroElement aResultXMLRoot = aResultXML.addElement ("validationResults");
      aDD.appendToMicroElement (aResultXMLRoot);

      CommonAPIInvoker.invoke (aResultXMLRoot, aValidationRunnable::run);

      // Perform conversion
      new XMLValidationResultListHelper ().ves (aVES)
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
                                                                       .ves (aVES)
                                                                       .errorTestExtractor ((error,
                                                                                             locale) -> error instanceof final SVRLResourceError aSvrlError ? aSvrlError.getTest ()
                                                                                                                                                            : null)
                                                                       .sourceData (bEmitValidationSourceContent ? new String (aPayloadBytes,
                                                                                                                               StandardCharsets.UTF_8)
                                                                                                                 : null)
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
        aResultJson.add ("documentDetails", aDD.getAsJson ());

        CommonAPIInvoker.invoke (aResultJson, aValidationRunnable::run);

        // Perform conversion
        new JsonValidationResultListHelper ().ves (aVES)
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
