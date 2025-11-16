/*
 * Copyright (C) 2022-2025 Philip Helger
 *
 * All rights reserved.
 */
package com.helger.valsvc.api;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.helger.annotation.Nonempty;
import com.helger.base.iface.IThrowingRunnable;
import com.helger.base.string.StringHelper;
import com.helger.base.timing.StopWatch;
import com.helger.diver.api.coord.DVRCoordinate;
import com.helger.http.CHttp;
import com.helger.http.header.specific.AcceptMimeTypeList;
import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;
import com.helger.json.serialize.JsonWriter;
import com.helger.json.serialize.JsonWriterSettings;
import com.helger.mime.CMimeType;
import com.helger.phive.api.result.ValidationResultList;
import com.helger.phive.result.json.JsonValidationResultListHelper;
import com.helger.phive.result.json.PhiveJsonHelper;
import com.helger.phive.result.xml.PhiveXMLHelper;
import com.helger.phive.result.xml.XMLValidationResultListHelper;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.servlet.request.RequestHelper;
import com.helger.valsvc.AppConfig;
import com.helger.valsvc.AppVersion;
import com.helger.valsvc.CApp;
import com.helger.valsvc.validation.AppValidator;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroDocument;
import com.helger.xml.microdom.serialize.MicroWriter;
import com.helger.xml.serialize.read.DOMReader;
import com.helger.xml.serialize.write.EXMLSerializeIndent;
import com.helger.xml.serialize.write.XMLWriterSettings;

/**
 * Perform validation only via API
 *
 * @author Philip Helger
 */
public class ApiPostValidate extends AbstractAPIInvoker
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ApiPostValidate.class);
  private static final AtomicInteger COUNTER = new AtomicInteger (0);

  @Override
  public void invokeAPI (@NonNull final IAPIDescriptor aAPIDescriptor,
                         @NonNull @Nonempty final String sPath,
                         @NonNull final Map <String, String> aPathVariables,
                         @NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                         @NonNull final PhotonUnifiedResponse aUnifiedResponse) throws IOException
  {
    aUnifiedResponse.disableCaching ();
    final String sLogPrefix = "[VALIDATE-" + AppVersion.getVersionNumber () + "-" + COUNTER.incrementAndGet () + "] ";

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
    final Document aDoc = DOMReader.readXMLDOM (aRequestScope.getRequest ().getInputStream ());
    if (aDoc == null || aDoc.getDocumentElement () == null)
    {
      final String sErrorMsg = "Failed to read the message body as XML";
      LOGGER.error (sLogPrefix + sErrorMsg);
      aUnifiedResponse.text (sErrorMsg).setStatus (CHttp.HTTP_BAD_REQUEST);
      return;
    }

    final String sVESID = aPathVariables.get ("vesid");
    final DVRCoordinate aVESID = DVRCoordinate.parseOrNull (sVESID);
    if (aVESID == null)
    {
      final String sErrorMsg = "The VESID '" + sVESID + "' could not be parsed.";
      LOGGER.error (sLogPrefix + sErrorMsg);
      aUnifiedResponse.text (sErrorMsg).setStatus (CHttp.HTTP_BAD_REQUEST);
      return;
    }
    if (AppValidator.getVESOrNull (aVESID) == null)
    {
      final String sErrorMsg = "The VESID '" + sVESID + "' could not be resolved.";
      LOGGER.error (sLogPrefix + sErrorMsg);
      aUnifiedResponse.text (sErrorMsg).setStatus (CHttp.HTTP_BAD_REQUEST);
      return;
    }

    final Locale aDisplayLocale = CApp.DEFAULT_LOCALE;
    final IJsonObject aResultJson = new JsonObject ();
    final IMicroDocument aResultXML = new MicroDocument ();
    final IMicroElement aResultXMLRoot = aResultXML.addElement ("validationResults");

    final IThrowingRunnable <Exception> aRunnable = () -> {
      final boolean bOverallSuccess;
      {
        // validation
        final StopWatch aSW = StopWatch.createdStarted ();

        LOGGER.info (sLogPrefix + "Performing validation using VESID '" + aVESID.getAsSingleID () + "'");

        // Perform validation
        final ValidationResultList aValidationResultList = AppValidator.validate (aVESID, aDoc, aDisplayLocale);
        aSW.stop ();

        // Convert to JSON/XML
        // Don't emit validation source content
        final boolean bEmitValidationSourceContent = false;
        new JsonValidationResultListHelper ().ves (AppValidator.getVES (aVESID))
                                             .sourceToJson (vs -> PhiveJsonHelper.getJsonValidationSource (vs,
                                                                                                           bEmitValidationSourceContent))
                                             .applyTo (aResultJson,
                                                       aValidationResultList,
                                                       aDisplayLocale,
                                                       aSW.getMillis ());
        new XMLValidationResultListHelper ().ves (AppValidator.getVES (aVESID))
                                            .sourceToXML (vs -> PhiveXMLHelper.getXMLValidationSource (vs,
                                                                                                       bEmitValidationSourceContent,
                                                                                                       PhiveXMLHelper.XML_VALIDATION_SOURCE))
                                            .applyTo (aResultXMLRoot,
                                                      aValidationResultList,
                                                      aDisplayLocale,
                                                      aSW.getMillis ());

        bOverallSuccess = aValidationResultList.containsNoError ();
        if (bOverallSuccess)
          LOGGER.info (sLogPrefix + "Validation was performed and no errors were found (" + aSW.getMillis () + " ms)");
        else
          LOGGER.error (sLogPrefix +
                        "Don't send out the document because it contains validation errors (" +
                        aSW.getMillis () +
                        " ms)");
      }

      if (!bOverallSuccess)
      {
        if (AppConfig.isUseHttp400OnValidationFailure ())
        {
          // Return error status
          aUnifiedResponse.setStatus (CHttp.HTTP_BAD_REQUEST);
        }
      }
    };

    final AcceptMimeTypeList aAcceptMimeTypes = RequestHelper.getAcceptMimeTypes (aRequestScope.getRequest ());
    if (aAcceptMimeTypes.explicitlySupportsMimeType (CMimeType.APPLICATION_XML))
    {
      // Provide response as XML
      CommonAPIInvoker.invoke (aResultXMLRoot, aRunnable);
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
    {
      // Provide response as JSON
      CommonAPIInvoker.invoke (aResultJson, aRunnable);
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
