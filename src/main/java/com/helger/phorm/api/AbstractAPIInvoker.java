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
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.base.string.StringHelper;
import com.helger.base.timing.StopWatch;
import com.helger.http.CHttp;
import com.helger.json.serialize.JsonWriterSettings;
import com.helger.phorm.AppConfig;
import com.helger.phorm.AppVersion;
import com.helger.phorm.telemetry.CPhormTelemetry;
import com.helger.phorm.telemetry.PhormMetrics;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.api.IAPIExecutor;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.telemetry.ETelemetrySpanKind;
import com.helger.telemetry.Telemetry;
import com.helger.telemetry.TelemetryAttributes;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * Abstract base invoker for REST API
 *
 * @author Philip Helger
 */
public abstract class AbstractAPIInvoker implements IAPIExecutor
{
  public static final String HEADER_X_TOKEN = "X-Token";
  private static final Logger LOGGER = LoggerFactory.getLogger (AbstractAPIInvoker.class);

  public abstract void invokeAPI (@NonNull final IAPIDescriptor aAPIDescriptor,
                                  @NonNull @Nonempty final String sPath,
                                  @NonNull final Map <String, String> aPathVariables,
                                  @NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                                  @NonNull final PhotonUnifiedResponse aUnifiedResponse) throws IOException;

  /**
   * @return The short endpoint tag used as the span suffix and the {@code phorm.endpoint} attribute
   *         (e.g. {@code "validate"}, {@code "dd_and_validate"}). Never <code>null</code>.
   */
  @NonNull
  @Nonempty
  protected abstract String getEndpointName ();

  private static boolean _verifyTokenOrSetForbidden (@NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                                                     @NonNull final PhotonUnifiedResponse aUnifiedResponse,
                                                     @NonNull @Nonempty final String sLogPrefix,
                                                     @Nullable final String sRequiredToken)
  {
    return Telemetry.withSpan (CPhormTelemetry.SPAN_AUTH_CHECK, ETelemetrySpanKind.INTERNAL, aSpan -> {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug (sLogPrefix + "Verifying specific HTTP header with token");

      final String sToken = aRequestScope.headers ().getFirstHeaderValue (HEADER_X_TOKEN);
      final int nTokenLength = sToken == null ? 0 : sToken.length ();
      aSpan.setAttribute (CPhormTelemetry.ATTR_AUTH_TOKEN_LENGTH, nTokenLength);

      if (StringHelper.isEmpty (sToken))
      {
        aSpan.setAttribute (CPhormTelemetry.ATTR_AUTH_RESULT, "missing_token");
        LOGGER.error (sLogPrefix + "The specific token header is missing");
        aUnifiedResponse.setStatus (CHttp.HTTP_FORBIDDEN);
        return Boolean.FALSE;
      }

      if (!sToken.equals (sRequiredToken))
      {
        aSpan.setAttribute (CPhormTelemetry.ATTR_AUTH_RESULT, "wrong_token");
        LOGGER.error (sLogPrefix + "The specified token value does not match the configured required token");
        aUnifiedResponse.setStatus (CHttp.HTTP_FORBIDDEN);
        return Boolean.FALSE;
      }

      aSpan.setAttribute (CPhormTelemetry.ATTR_AUTH_RESULT, "ok");
      return Boolean.TRUE;
    }).booleanValue ();
  }

  /**
   * Verify the {@value #HEADER_X_TOKEN} HTTP header against the configured required token. On
   * failure sets HTTP 403 on the response and returns <code>false</code>. Records a
   * {@code phorm.auth.check} span with {@code phorm.auth.result} and
   * {@code phorm.auth.token.length} attributes (the token <em>value</em> is never recorded).
   *
   * @param aRequestScope
   *        The current request scope. Never <code>null</code>.
   * @param aUnifiedResponse
   *        The response to mark as 403 on failure. Never <code>null</code>.
   * @param sLogPrefix
   *        Log line prefix for error messages. Never <code>null</code>.
   * @return <code>true</code> when authentication succeeded, <code>false</code> when the response
   *         has been marked as 403.
   */
  protected final boolean verifyAuthOrSetForbidden (@NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                                                    @NonNull final PhotonUnifiedResponse aUnifiedResponse,
                                                    @NonNull @Nonempty final String sLogPrefix)
  {
    return _verifyTokenOrSetForbidden (aRequestScope, aUnifiedResponse, sLogPrefix, AppConfig.getAPIRequiredToken ());
  }

  /**
   * Verify the {@value #HEADER_X_TOKEN} HTTP header against the configured admin token
   * (<code>phorm.api.admin.token</code>). Behaves like
   * {@link #verifyAuthOrSetForbidden(IRequestWebScopeWithoutResponse, PhotonUnifiedResponse, String)}
   * otherwise. If no admin token is configured, every request is rejected.
   *
   * @param aRequestScope
   *        The current request scope. Never <code>null</code>.
   * @param aUnifiedResponse
   *        The response to mark as 403 on failure. Never <code>null</code>.
   * @param sLogPrefix
   *        Log line prefix for error messages. Never <code>null</code>.
   * @return <code>true</code> when authentication succeeded, <code>false</code> when the response
   *         has been marked as 403.
   * @since 2.3.0
   */
  protected final boolean verifyAdminAuthOrSetForbidden (@NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                                                         @NonNull final PhotonUnifiedResponse aUnifiedResponse,
                                                         @NonNull @Nonempty final String sLogPrefix)
  {
    final String sAdminToken = AppConfig.getAdminToken ();
    if (StringHelper.isEmpty (sAdminToken))
    {
      LOGGER.error (sLogPrefix + "No admin token is configured");
      aUnifiedResponse.setStatus (CHttp.HTTP_FORBIDDEN);
      return false;
    }
    return _verifyTokenOrSetForbidden (aRequestScope, aUnifiedResponse, sLogPrefix, sAdminToken);
  }

  public final void invokeAPI (@NonNull final IAPIDescriptor aAPIDescriptor,
                               @NonNull @Nonempty final String sPath,
                               @NonNull final Map <String, String> aPathVariables,
                               @NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                               @NonNull final UnifiedResponse aUnifiedResponse) throws IOException
  {
    final StopWatch aSW = StopWatch.createdStarted ();
    final String sEndpoint = getEndpointName ();
    final String sRoute = aAPIDescriptor.getPathDescriptor ().getAsURLString ();
    final String sHttpMethod = aRequestScope.getMethod ();

    PhormMetrics.REQUESTS_RECEIVED.add (1, TelemetryAttributes.builder ().put ("endpoint", sEndpoint).build ());

    Telemetry.withSpanVoidThrowing ("phorm.api." + sEndpoint, ETelemetrySpanKind.INTERNAL, aSpan -> {
      aSpan.setAttribute (CPhormTelemetry.ATTR_ENDPOINT, sEndpoint)
           .setAttribute (CPhormTelemetry.ATTR_HTTP_METHOD, sHttpMethod)
           .setAttribute (CPhormTelemetry.ATTR_HTTP_ROUTE, sRoute)
           .setAttribute (CPhormTelemetry.ATTR_APP_VERSION, AppVersion.getVersionNumber ());

      final PhotonUnifiedResponse aPUR = (PhotonUnifiedResponse) aUnifiedResponse;
      aPUR.setJsonWriterSettings (JsonWriterSettings.DEFAULT_SETTINGS_FORMATTED);
      invokeAPI (aAPIDescriptor, sPath, aPathVariables, aRequestScope, aPUR);
    });

    aSW.stop ();
    LOGGER.info ("[API] Successfully finished '" + sRoute + "' after " + aSW.getMillis () + " milliseconds");
  }
}
