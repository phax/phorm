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
package com.helger.phorm.servlet;

import java.time.OffsetDateTime;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.helger.base.CGlobal;
import com.helger.base.exception.InitializationException;
import com.helger.base.string.StringHelper;
import com.helger.commons.vendor.VendorInfo;
import com.helger.datetime.helper.PDTFactory;
import com.helger.photon.api.APIDescriptor;
import com.helger.photon.api.APIPath;
import com.helger.photon.api.IAPIRegistry;
import com.helger.photon.core.locale.ILocaleManager;
import com.helger.photon.core.servlet.WebAppListener;
import com.helger.scope.singleton.SingletonHelper;
import com.helger.phorm.AppConfig;
import com.helger.phorm.AppErrorHandler;
import com.helger.phorm.AppMetaManager;
import com.helger.phorm.AppSecurity;
import com.helger.phorm.CApp;
import com.helger.phorm.api.ApiGetAllVESIDs;
import com.helger.phorm.api.ApiPostDetermineDocDetails;
import com.helger.phorm.api.ApiPostDetermineDocTypeAndValidate;
import com.helger.phorm.api.ApiPostValidate;
import com.helger.xservlet.requesttrack.RequestTrackerSettings;

import jakarta.servlet.ServletContext;

/**
 * Callbacks for the application server
 *
 * @author Philip Helger
 */
public final class AppWebAppListener extends WebAppListener
{
  private static OffsetDateTime s_aStartupDateTime;

  @Nullable
  public static OffsetDateTime getStartupDateTime ()
  {
    return s_aStartupDateTime;
  }

  public AppWebAppListener ()
  {
    setHandleStatisticsOnEnd (false);
  }

  @Override
  protected String getInitParameterDebug (@NonNull final ServletContext aSC)
  {
    return AppConfig.getGlobalDebug ();
  }

  @Override
  protected String getInitParameterProduction (@NonNull final ServletContext aSC)
  {
    return AppConfig.getGlobalProduction ();
  }

  @Override
  protected String getDataPath (@NonNull final ServletContext aSC)
  {
    return AppConfig.getDataPath ();
  }

  @Override
  protected boolean shouldCheckFileAccess (@NonNull final ServletContext aSC)
  {
    return AppConfig.isCheckFileAccess ();
  }

  @Override
  protected String getInitParameterNoStartupInfo (@NonNull final ServletContext aSC)
  {
    return "true";
  }

  @Override
  protected void initGlobalSettings ()
  {
    s_aStartupDateTime = PDTFactory.getCurrentOffsetDateTimeUTC ();

    // Internal stuff:
    VendorInfo.setInceptionYear (2022);

    // Avoid startup error logs
    SingletonHelper.setDebugConsistency (false);

    // Logging: JUL to SLF4J
    SLF4JBridgeHandler.removeHandlersForRootLogger ();
    SLF4JBridgeHandler.install ();

    // Request tracker
    RequestTrackerSettings.setLongRunningRequestsCheckEnabled (true);
    RequestTrackerSettings.setLongRunningRequestCheckIntervalMilliseconds (10 * CGlobal.MILLISECONDS_PER_SECOND);
    RequestTrackerSettings.setParallelRunningRequestsCheckEnabled (true);

    // Check configuration
    // This property is by default in the "private-application.properties" file
    if (StringHelper.isEmpty (AppConfig.getAPIRequiredToken ()))
      throw new InitializationException ("The configuration property 'phorm.api.requiredtoken' is not set or empty. This is a required configuration.");
  }

  @Override
  protected void initLocales (@NonNull final ILocaleManager aLocaleMgr)
  {
    aLocaleMgr.registerLocale (CApp.DEFAULT_LOCALE);
    aLocaleMgr.setDefaultLocale (CApp.DEFAULT_LOCALE);
  }

  @Override
  protected void initSecurity ()
  {
    // Set all security related stuff
    AppSecurity.init ();
  }

  @Override
  protected void initManagers ()
  {
    AppErrorHandler.doSetup ();
    AppMetaManager.getInstance ();
  }

  @Override
  protected void initAPI (@NonNull final IAPIRegistry aAPIRegistry)
  {
    aAPIRegistry.registerAPI (new APIDescriptor (APIPath.post ("/validate/{vesid}"), ApiPostValidate.class));
    aAPIRegistry.registerAPI (new APIDescriptor (APIPath.post ("/dd_and_validate"),
                                                 ApiPostDetermineDocTypeAndValidate.class));
    aAPIRegistry.registerAPI (new APIDescriptor (APIPath.get ("/get/vesids"), ApiGetAllVESIDs.class));
    aAPIRegistry.registerAPI (new APIDescriptor (APIPath.post ("/determinedoctype"), ApiPostDetermineDocDetails.class));
  }
}
