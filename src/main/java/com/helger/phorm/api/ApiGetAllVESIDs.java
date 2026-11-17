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
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.diver.api.coord.DVRCoordinate;
import com.helger.json.IJsonArray;
import com.helger.json.IJsonObject;
import com.helger.json.JsonArray;
import com.helger.json.JsonObject;
import com.helger.json.serialize.JsonWriter;
import com.helger.json.serialize.JsonWriterSettings;
import com.helger.phive.api.executorset.IValidationExecutorSet;
import com.helger.phive.xml.source.IValidationSourceXML;
import com.helger.phorm.AppConfig;
import com.helger.phorm.validation.AppValidator;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.app.PhotonUnifiedResponse;
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
  @NonNull
  protected String getEndpointName ()
  {
    return "get_vesids";
  }

  @Override
  public void invokeAPI (@NonNull final IAPIDescriptor aAPIDescriptor,
                         @NonNull @Nonempty final String sPath,
                         @NonNull final Map <String, String> aPathVariables,
                         @NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                         @NonNull final PhotonUnifiedResponse aUnifiedResponse) throws IOException
  {
    final boolean bIncludeDeprecated = aRequestScope.params ().containsKey ("include-deprecated");
    final String sLogPrefix = "[GetAllVesIDs] ";

    if (false)
      if (!verifyAuthOrSetForbidden (aRequestScope, aUnifiedResponse, sLogPrefix))
        return;

    final IJsonObject aJson = new JsonObject ();

    CommonAPIInvoker.invoke (aJson, () -> {
      final IJsonArray aJsonIDs = new JsonArray ();
      for (final IValidationExecutorSet <IValidationSourceXML> aEntry : AppValidator.getAllVESSorted ())
        if (bIncludeDeprecated || !aEntry.getStatus ().isDeprecated ())
        {
          final DVRCoordinate aVESID = aEntry.getID ();
          final String sLatestVersion = AppValidator.getLatestVersion (aVESID);
          final boolean bIsLatest = aVESID.getVersionString ().equals (sLatestVersion);

          final IJsonObject aObj = new JsonObject ().add ("vesid", aVESID.getAsSingleID ())
                                                    .add ("deprecated", aEntry.getStatus ().isDeprecated ())
                                                    .add ("name", aEntry.getDisplayName ());
          if (bIsLatest)
            aObj.add ("latest", true);
          // Only present for VES loaded from an external repository
          aObj.addIfNotNull ("repository", AppValidator.getExternalRepositoryIDOfVES (aVESID));
          aJsonIDs.add (aObj);
        }
      aJson.add ("count", aJsonIDs.size ());
      aJson.add ("vesids", aJsonIDs);
    });

    if (AppConfig.isLogResponsePayload ())
    {
      LOGGER.info (sLogPrefix +
                   "Response JSON is:\n" +
                   new JsonWriter (JsonWriterSettings.DEFAULT_SETTINGS_FORMATTED).writeAsString (aJson));
    }

    aUnifiedResponse.json (aJson);
  }
}
