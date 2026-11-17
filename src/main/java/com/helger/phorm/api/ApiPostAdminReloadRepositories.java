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
import java.util.Comparator;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.base.state.ESuccess;
import com.helger.collection.commons.ICommonsSet;
import com.helger.diagnostics.error.IError;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.diver.api.coord.DVRCoordinate;
import com.helger.http.CHttp;
import com.helger.json.IJsonArray;
import com.helger.json.IJsonObject;
import com.helger.json.JsonArray;
import com.helger.json.JsonObject;
import com.helger.json.serialize.JsonWriter;
import com.helger.json.serialize.JsonWriterSettings;
import com.helger.phive.result.json.CPhiveJson;
import com.helger.phive.result.json.PhiveJsonHelper;
import com.helger.phorm.AppConfig;
import com.helger.phorm.CApp;
import com.helger.phorm.validation.AppValidator;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * Admin API: reload the external repositories from the configured repository configuration file.
 * If anything fails, the previously loaded VES stay active.
 *
 * @author Philip Helger
 */
public class ApiPostAdminReloadRepositories extends AbstractAPIInvoker
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ApiPostAdminReloadRepositories.class);

  @Override
  @NonNull
  protected String getEndpointName ()
  {
    return "admin_reload_repositories";
  }

  @NonNull
  private static IJsonArray _getAsSortedJsonArray (@NonNull final ICommonsSet <DVRCoordinate> aVESIDs)
  {
    final IJsonArray ret = new JsonArray ();
    for (final DVRCoordinate aVESID : aVESIDs.getSorted (Comparator.naturalOrder ()))
      ret.add (aVESID.getAsSingleID ());
    return ret;
  }

  @Override
  public void invokeAPI (@NonNull final IAPIDescriptor aAPIDescriptor,
                         @NonNull @Nonempty final String sPath,
                         @NonNull final Map <String, String> aPathVariables,
                         @NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                         @NonNull final PhotonUnifiedResponse aUnifiedResponse) throws IOException
  {
    final String sLogPrefix = "[AdminReloadRepositories] ";

    if (!verifyAdminAuthOrSetForbidden (aRequestScope, aUnifiedResponse, sLogPrefix))
      return;

    final IJsonObject aJson = new JsonObject ();
    final ErrorList aErrorList = new ErrorList ();
    final ICommonsSet <DVRCoordinate> aOldVESIDs = AppValidator.getAllExternalVESIDs ().copyOfKeySet ();

    LOGGER.info (sLogPrefix + "Reloading the external repositories");
    final ESuccess eSuccess = AppValidator.loadExternalRepositories (AppValidator.getConfiguredRepositoriesFile (),
                                                                     aErrorList);
    CommonAPIInvoker.invoke (aJson, () -> {
      aJson.add (CPhiveJson.JSON_SUCCESS, eSuccess.isSuccess ());
      if (eSuccess.isSuccess ())
      {
        final ICommonsSet <DVRCoordinate> aNewVESIDs = AppValidator.getAllExternalVESIDs ().copyOfKeySet ();
        aJson.add ("added", _getAsSortedJsonArray (aNewVESIDs.getAll (x -> !aOldVESIDs.contains (x))));
        aJson.add ("removed", _getAsSortedJsonArray (aOldVESIDs.getAll (x -> !aNewVESIDs.contains (x))));
        ApiGetAdminRepositories.fillRepositoryStatus (aJson);
      }

      final IJsonArray aJsonErrors = new JsonArray ();
      for (final IError aError : aErrorList)
        aJsonErrors.add (PhiveJsonHelper.getJsonError (aError, CApp.DEFAULT_LOCALE));
      aJson.add ("errors", aJsonErrors);
    });

    if (eSuccess.isSuccess ())
      LOGGER.info (sLogPrefix + "Successfully reloaded the external repositories");
    else
      LOGGER.error (sLogPrefix + "Failed to reload the external repositories - keeping the previous state");

    if (AppConfig.isLogResponsePayload ())
    {
      LOGGER.info (sLogPrefix +
                   "Response JSON is:\n" +
                   new JsonWriter (JsonWriterSettings.DEFAULT_SETTINGS_FORMATTED).writeAsString (aJson));
    }

    aUnifiedResponse.json (aJson);
    if (eSuccess.isFailure ())
      aUnifiedResponse.setStatus (CHttp.HTTP_INTERNAL_SERVER_ERROR);
  }
}
