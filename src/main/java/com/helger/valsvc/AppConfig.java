/*
 * Copyright (C) 2022-2025 Philip Helger
 *
 * All rights reserved.
 */
package com.helger.valsvc;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.base.debug.GlobalDebug;
import com.helger.config.ConfigFactory;
import com.helger.config.IConfig;

/**
 * This class provides access to the settings as contained in the
 * <code>application.properties</code> file.
 *
 * @author Philip Helger
 */
public final class AppConfig
{
  @Deprecated
  private AppConfig ()
  {}

  @NonNull
  public static IConfig getConfig ()
  {
    return ConfigFactory.getDefaultConfig ();
  }

  @Nullable
  public static String getGlobalDebug ()
  {
    return getConfig ().getAsString ("global.debug");
  }

  @Nullable
  public static String getGlobalProduction ()
  {
    return getConfig ().getAsString ("global.production");
  }

  @Nullable
  public static String getDataPath ()
  {
    return getConfig ().getAsString ("webapp.datapath");
  }

  public static boolean isCheckFileAccess ()
  {
    return getConfig ().getAsBoolean ("webapp.checkfileaccess", false);
  }

  public static boolean isTestVersion ()
  {
    return getConfig ().getAsBoolean ("webapp.testversion", GlobalDebug.isDebugMode ());
  }

  @Nullable
  public static String getAPIRequiredToken ()
  {
    return getConfig ().getAsString ("valsvc.api.requiredtoken");
  }

  public static boolean isUseHttp400OnValidationFailure ()
  {
    return getConfig ().getAsBoolean ("valsvc.api.response.onfailure.http400", true);
  }

  public static boolean isLogResponsePayload ()
  {
    return getConfig ().getAsBoolean ("valsvc.api.response.log.payload", false);
  }

  public static boolean isStatusAPIEnabled ()
  {
    return getConfig ().getAsBoolean ("valsvc.statusapi.enabled", true);
  }
}
