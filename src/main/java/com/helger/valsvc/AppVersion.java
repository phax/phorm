/*
 * Copyright (C) 2022-2025 Philip Helger
 *
 * All rights reserved.
 */
package com.helger.valsvc;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.exception.InitializationException;
import com.helger.base.rt.NonBlockingProperties;
import com.helger.base.rt.PropertiesHelper;
import com.helger.io.resource.ClassPathResource;

/**
 * Validation Service version number
 *
 * @author Philip Helger
 */
@Immutable
public final class AppVersion
{
  public static final String VALSVC_VERSION_FILENAME = "valsvc-version.properties";

  private static final String VERSION_NUMBER;
  private static final String TIMESTAMP;

  static
  {
    // Read version number
    final NonBlockingProperties aVersionProps = PropertiesHelper.loadProperties (ClassPathResource.getInputStream (VALSVC_VERSION_FILENAME,
                                                                                                                   AppVersion.class.getClassLoader ()));
    if (aVersionProps == null)
      throw new InitializationException ("Failed to read Validation Service version properties");

    VERSION_NUMBER = aVersionProps.get ("version");
    if (VERSION_NUMBER == null)
      throw new InitializationException ("Error determining Validation Service version number!");

    TIMESTAMP = aVersionProps.get ("timestamp");
    if (TIMESTAMP == null)
      throw new InitializationException ("Error determining Validation Service build timestamp!");
  }

  private AppVersion ()
  {}

  /**
   * @return The version number of the ValSvc read from the internal properties file. Never
   *         <code>null</code>.
   */
  @NonNull
  public static String getVersionNumber ()
  {
    return VERSION_NUMBER;
  }

  /**
   * @return The build timestamp of the ValSvc read from the internal properties file. Never
   *         <code>null</code>.
   */
  @NonNull
  public static String getBuildTimestamp ()
  {
    return TIMESTAMP;
  }
}
