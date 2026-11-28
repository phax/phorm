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
import com.helger.base.version.Version;
import com.helger.http.header.specific.AcceptMimeTypeList;
import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;
import com.helger.json.serialize.JsonWriter;
import com.helger.json.serialize.JsonWriterSettings;
import com.helger.mime.CMimeType;
import com.helger.phorm.AppConfig;
import com.helger.phorm.AppVersion;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.servlet.request.RequestHelper;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * Get the major and minor version of Phorm. The micro version and the build timestamp are
 * deliberately not disclosed: features only change with a minor version, and the exact patch level
 * of a publicly reachable instance tells an attacker which fixes are missing.
 *
 * @author Philip Helger
 * @since 2.2.18
 */
public class ApiGetVersion extends AbstractAPIInvoker
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ApiGetVersion.class);

  @Override
  @NonNull
  protected String getEndpointName ()
  {
    return "get_version";
  }

  @Override
  public void invokeAPI (@NonNull final IAPIDescriptor aAPIDescriptor,
                         @NonNull @Nonempty final String sPath,
                         @NonNull final Map <String, String> aPathVariables,
                         @NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                         @NonNull final PhotonUnifiedResponse aUnifiedResponse) throws IOException
  {
    final String sLogPrefix = "[GetVersion] ";
    final Version aVersion = AppVersion.getVersion ();

    final AcceptMimeTypeList aAcceptMimeTypes = RequestHelper.getAcceptMimeTypes (aRequestScope.getRequest ());
    if (aAcceptMimeTypes.explicitlySupportsMimeType (CMimeType.TEXT_PLAIN))
    {
      // Provide response as plain text, e.g. "2.2"
      final String sText = aVersion.getAsStringMajorMinor ();

      if (AppConfig.isLogResponsePayload ())
        LOGGER.info (sLogPrefix + "Response text is: " + sText);

      aUnifiedResponse.text (sText);
    }
    else
    {
      final IJsonObject aJson = new JsonObject ();
      CommonAPIInvoker.invoke (aJson, () -> {
        aJson.add ("major", aVersion.getMajor ());
        aJson.add ("minor", aVersion.getMinor ());
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
}
