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
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.helger.annotation.Nonempty;
import com.helger.base.io.stream.StreamHelper;
import com.helger.base.string.StringHelper;
import com.helger.base.wrapper.Wrapper;
import com.helger.ddd.DocumentDetails;
import com.helger.diver.api.coord.DVRCoordinate;
import com.helger.http.CHttp;
import com.helger.http.header.specific.AcceptMimeTypeList;
import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;
import com.helger.json.serialize.JsonWriter;
import com.helger.json.serialize.JsonWriterSettings;
import com.helger.mime.CMimeType;
import com.helger.phive.api.executorset.IValidationExecutorSet;
import com.helger.phive.api.result.ValidationResultList;
import com.helger.phive.result.html.PhiveHtmlHelper;
import com.helger.phive.result.json.JsonValidationResultListHelper;
import com.helger.phive.result.xml.XMLValidationResultListHelper;
import com.helger.phive.xml.source.IValidationSourceXML;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.schematron.svrl.SVRLResourceError;
import com.helger.servlet.request.RequestHelper;
import com.helger.phorm.AppConfig;
import com.helger.phorm.AppVersion;
import com.helger.phorm.CApp;
import com.helger.phorm.ddd.PhormDDD;
import com.helger.phorm.validation.AppValidator;
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

    // Security check
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug (sLogPrefix + "Verifying specific HTTP header with token");

    final String sToken = aRequestScope.headers ().getFirstHeaderValue (HEADER_X_TOKEN);
    if (StringHelper.isEmpty (sToken))
    {
      LOGGER.error (sLogPrefix + "The specific token header is missing");
      aUnifiedResponse.setStatus (CHttp.HTTP_FORBIDDEN);
      return;
    }
    if (!sToken.equals (AppConfig.getAPIRequiredToken ()))
    {
      LOGGER.error (sLogPrefix + "The specified token value does not match the configured required token");
      aUnifiedResponse.setStatus (CHttp.HTTP_FORBIDDEN);
      return;
    }

    // Read the payload as XML
    LOGGER.info (sLogPrefix + "Trying to read payload as XML");
    final byte [] aPayloadBytes = StreamHelper.getAllBytes (aRequestScope.getRequest ().getInputStream ());
    final Document aDoc = DOMReader.readXMLDOM (aPayloadBytes);
    if (aDoc == null || aDoc.getDocumentElement () == null)
    {
      final String sErrorMsg = "Failed to read the message body as XML";
      LOGGER.error (sLogPrefix + sErrorMsg);
      aUnifiedResponse.text (sErrorMsg).setStatus (CHttp.HTTP_BAD_REQUEST);
      return;
    }

    // Determine document details
    LOGGER.info (sLogPrefix + "Trying to determine document details");
    final DocumentDetails aDD = PhormDDD.findDocumentDetails (aDoc.getDocumentElement ());
    if (aDD == null || !aDD.hasVESID ())
    {
      final String sErrorMsg = "Failed to determine the document type details";
      LOGGER.error (sLogPrefix + sErrorMsg);
      aUnifiedResponse.text (sErrorMsg).setStatus (CHttp.HTTP_BAD_REQUEST);
      return;
    }

    final String sVESID = aDD.getVESID ();
    final DVRCoordinate aVESID = DVRCoordinate.parseOrNull (sVESID);
    if (aVESID == null)
    {
      final String sErrorMsg = "The VESID '" + sVESID + "' could not be parsed.";
      LOGGER.error (sLogPrefix + sErrorMsg);
      aUnifiedResponse.text (sErrorMsg).setStatus (CHttp.HTTP_BAD_REQUEST);
      return;
    }
    final IValidationExecutorSet <IValidationSourceXML> aVES = AppValidator.getVES (aVESID);
    if (aVES == null)
    {
      final String sErrorMsg = "The VESID '" + sVESID + "' could not be resolved.";
      LOGGER.error (sLogPrefix + sErrorMsg);
      aUnifiedResponse.text (sErrorMsg).setStatus (CHttp.HTTP_BAD_REQUEST);
      return;
    }

    final Locale aDisplayLocale = CApp.DEFAULT_LOCALE;
    final Wrapper <ValidationResultList> aWrappedVRL = Wrapper.empty ();

    final Runnable aRunnable = () -> {
      // validation
      LOGGER.info (sLogPrefix + "Performing validation using VESID '" + aVESID.getAsSingleID () + "'");

      // Perform validation
      final ValidationResultList aValidationResultList = AppValidator.validate (aVES, aDoc, aDisplayLocale);
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

      CommonAPIInvoker.invoke (aResultXMLRoot, aRunnable::run);

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
        aRunnable.run ();

        // Perform conversion
        final String sResultHtml = new PhiveHtmlHelper (aDisplayLocale).useDefaultCSS ()
                                                                       .ves (aVES)
                                                                       .errorTestExtractor ( (error,
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

        CommonAPIInvoker.invoke (aResultJson, aRunnable::run);

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
