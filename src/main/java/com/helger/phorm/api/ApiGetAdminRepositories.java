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

import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.collection.commons.ICommonsOrderedMap;
import com.helger.diver.api.coord.DVRCoordinate;
import com.helger.json.IJsonArray;
import com.helger.json.IJsonObject;
import com.helger.json.JsonArray;
import com.helger.json.JsonObject;
import com.helger.json.serialize.JsonWriter;
import com.helger.json.serialize.JsonWriterSettings;
import com.helger.phorm.AppConfig;
import com.helger.phorm.validation.AppValidator;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * Admin API: get the external repositories and the VES loaded from them
 *
 * @author Philip Helger
 */
public class ApiGetAdminRepositories extends AbstractAPIInvoker
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ApiGetAdminRepositories.class);

  @Override
  @NonNull
  protected String getEndpointName ()
  {
    return "admin_get_repositories";
  }

  /**
   * Fill the status of the external repositories into the provided JSON object.
   *
   * @param aJson
   *        The JSON object to fill. May not be <code>null</code>.
   */
  static void fillRepositoryStatus (@NonNull final IJsonObject aJson)
  {
    final File aConfigFile = AppValidator.getConfiguredRepositoriesFile ();
    aJson.addIfNotNull ("configFile", aConfigFile == null ? null : aConfigFile.getAbsolutePath ());

    final OffsetDateTime aLoadDT = AppValidator.getExternalRepositoriesLoadDateTime ();
    aJson.addIfNotNull ("loadDateTime",
                        aLoadDT == null ? null : DateTimeFormatter.ISO_OFFSET_DATE_TIME.format (aLoadDT));

    final ICommonsOrderedMap <DVRCoordinate, String> aExternalVESIDs = AppValidator.getAllExternalVESIDs ();

    final IJsonArray aJsonRepos = new JsonArray ();
    for (final String sRepoID : AppValidator.getAllExternalRepositoryIDs ())
      aJsonRepos.add (new JsonObject ().add ("id", sRepoID)
                                       .add ("vesCount",
                                             aExternalVESIDs.values ().stream ().filter (sRepoID::equals).count ()));
    aJson.add ("repositories", aJsonRepos);

    final IJsonArray aJsonVESIDs = new JsonArray ();
    for (final Map.Entry <DVRCoordinate, String> aEntry : aExternalVESIDs.entrySet ())
      aJsonVESIDs.add (new JsonObject ().add ("vesid", aEntry.getKey ().getAsSingleID ())
                                        .add ("repository", aEntry.getValue ()));
    aJson.add ("vesids", aJsonVESIDs);
  }

  @Override
  public void invokeAPI (@NonNull final IAPIDescriptor aAPIDescriptor,
                         @NonNull @Nonempty final String sPath,
                         @NonNull final Map <String, String> aPathVariables,
                         @NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                         @NonNull final PhotonUnifiedResponse aUnifiedResponse) throws IOException
  {
    final String sLogPrefix = "[AdminGetRepositories] ";

    if (!verifyAdminAuthOrSetForbidden (aRequestScope, aUnifiedResponse, sLogPrefix))
      return;

    final IJsonObject aJson = new JsonObject ();
    CommonAPIInvoker.invoke (aJson, () -> fillRepositoryStatus (aJson));

    if (AppConfig.isLogResponsePayload ())
    {
      LOGGER.info (sLogPrefix +
                   "Response JSON is:\n" +
                   new JsonWriter (JsonWriterSettings.DEFAULT_SETTINGS_FORMATTED).writeAsString (aJson));
    }

    aUnifiedResponse.json (aJson);
  }
}
