/*
 * Copyright (C) Philip Helger
 *
 * All rights reserved.
 */
package com.helger.valsvc.api;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.http.CHttp;
import com.helger.commons.string.StringHelper;
import com.helger.commons.timing.StopWatch;
import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;
import com.helger.json.serialize.JsonWriter;
import com.helger.json.serialize.JsonWriterSettings;
import com.helger.phive.api.executorset.VESID;
import com.helger.phive.api.result.ValidationResultList;
import com.helger.phive.json.PhiveJsonHelper;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.valsvc.AppConfig;
import com.helger.valsvc.AppVersion;
import com.helger.valsvc.CApp;
import com.helger.valsvc.validation.AppValidator;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.serialize.read.DOMReader;

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
  public void invokeAPI (@Nonnull final IAPIDescriptor aAPIDescriptor,
                         @Nonnull @Nonempty final String sPath,
                         @Nonnull final Map <String, String> aPathVariables,
                         @Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                         @Nonnull final PhotonUnifiedResponse aUnifiedResponse) throws IOException
  {
    aUnifiedResponse.disableCaching ();
    final String sLogPrefix = "[VALIDATE-" + AppVersion.getVersionNumber () + "-" + COUNTER.incrementAndGet () + "] ";

    // Security check
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug (sLogPrefix + "Verifying specific HTTP header with token");

    final String sToken = aRequestScope.getRequest ().getHeader (HEADER_X_TOKEN);
    if (StringHelper.hasNoText (sToken))
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
    final VESID aVESID = VESID.parseIDOrNull (sVESID);
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
    final IJsonObject aJson = new JsonObject ();

    CommonAPIInvoker.invoke (aJson, () -> {
      final boolean bOverallSuccess;
      {
        // validation
        final StopWatch aSW = StopWatch.createdStarted ();

        LOGGER.info (sLogPrefix + "Performing validation using VESID '" + aVESID.getAsSingleID () + "'");

        // Perform validation
        final ValidationResultList aValidationResultList = AppValidator.validate (aVESID, aDoc, aDisplayLocale);
        aSW.stop ();

        // Convert to JSON
        PhiveJsonHelper.applyValidationResultList (aJson,
                                                   AppValidator.getVES (aVESID),
                                                   aValidationResultList,
                                                   aDisplayLocale,
                                                   aSW.getMillis (),
                                                   null,
                                                   null);

        bOverallSuccess = aValidationResultList.containsNoError ();
        if (bOverallSuccess)
          LOGGER.info (sLogPrefix + "Validation was performed and no errors were found");
        else
          LOGGER.error (sLogPrefix + "Don't send out the document because it contains validation errors");
      }

      if (!bOverallSuccess)
      {
        // Return error status
        aUnifiedResponse.setStatus (CHttp.HTTP_BAD_REQUEST);
      }
    });

    LOGGER.info (sLogPrefix +
                 "Response JSON is:\n" +
                 new JsonWriter (JsonWriterSettings.DEFAULT_SETTINGS_FORMATTED).writeAsString (aJson));

    aUnifiedResponse.json (aJson);
  }
}
