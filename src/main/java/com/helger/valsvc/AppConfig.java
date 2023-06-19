/*
 * Copyright (C) Philip Helger
 *
 * All rights reserved.
 */
package com.helger.valsvc;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.debug.GlobalDebug;
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

  @Nonnull
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

  public static boolean isStatusAPIEnabled ()
  {
    return getConfig ().getAsBoolean ("valsvc.statusapi.enabled", true);
  }
}
