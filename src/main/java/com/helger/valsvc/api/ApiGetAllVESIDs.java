/*
 * Copyright (C) Philip Helger
 *
 * All rights reserved.
 */
package com.helger.valsvc.api;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.http.CHttp;
import com.helger.commons.string.StringHelper;
import com.helger.json.IJsonArray;
import com.helger.json.IJsonObject;
import com.helger.json.JsonArray;
import com.helger.json.JsonObject;
import com.helger.json.serialize.JsonWriter;
import com.helger.json.serialize.JsonWriterSettings;
import com.helger.phive.api.executorset.IValidationExecutorSet;
import com.helger.phive.api.executorset.VESID;
import com.helger.phive.engine.source.IValidationSourceXML;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.valsvc.AppConfig;
import com.helger.valsvc.validation.AppValidator;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * Get all registered VES IDs
 *
 * @author Philip Helger
 */
public class ApiGetAllVESIDs extends AbstractAPIInvoker
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ApiGetAllVESIDs.class);

  @Override
  public void invokeAPI (@Nonnull final IAPIDescriptor aAPIDescriptor,
                         @Nonnull @Nonempty final String sPath,
                         @Nonnull final Map <String, String> aPathVariables,
                         @Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                         @Nonnull final PhotonUnifiedResponse aUnifiedResponse) throws IOException
  {
    final boolean bIncludeDeprecated = aRequestScope.params ().containsKey ("include-deprecated");
    final String sLogPrefix = "[GetAllVesIDs] ";

    if (false)
    {
      // Security check
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug (sLogPrefix + "Verifying specific HTTP header with token");

      final String sToken = aRequestScope.headers ().getFirstHeaderValue (HEADER_X_TOKEN);
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
    }

    final IJsonObject aJson = new JsonObject ();

    CommonAPIInvoker.invoke (aJson, () -> {
      final IJsonArray aJsonIDs = new JsonArray ();
      for (final IValidationExecutorSet <IValidationSourceXML> aEntry : AppValidator.getAllVESSorted ())
        if (bIncludeDeprecated || !aEntry.isDeprecated ())
        {
          final VESID aVESID = aEntry.getID ();
          final String sLatestVersion = AppValidator.getLatestVersion (aVESID);
          final boolean bIsLatest = aVESID.getVersion ().equals (sLatestVersion);

          aJsonIDs.add (new JsonObject ().add ("vesid", aVESID.getAsSingleID ())
                                         .add ("deprecated", aEntry.isDeprecated ())
                                         .add ("name", aEntry.getDisplayName ())
                                         .addIf ("latest", "true", x -> bIsLatest));
        }
      aJson.add ("count", aJsonIDs.size ());
      aJson.addJson ("vesids", aJsonIDs);
    });

    LOGGER.info (sLogPrefix +
                 "Response JSON is:\n" +
                 new JsonWriter (JsonWriterSettings.DEFAULT_SETTINGS_FORMATTED).writeAsString (aJson));

    aUnifiedResponse.json (aJson);
  }
}
