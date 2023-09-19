/*
 * Copyright (C) Philip Helger
 *
 * All rights reserved.
 */
package com.helger.valsvc;

import java.io.IOException;
import java.io.UncheckedIOException;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.exception.InitializationException;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.lang.NonBlockingProperties;

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
    final NonBlockingProperties aVersionProps = new NonBlockingProperties ();
    try
    {
      aVersionProps.load (ClassPathResource.getInputStream (VALSVC_VERSION_FILENAME));
    }
    catch (final IOException ex)
    {
      throw new UncheckedIOException (ex);
    }
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
   * @return The version number of the ValSvc read from the internal properties
   *         file. Never <code>null</code>.
   */
  @Nonnull
  public static String getVersionNumber ()
  {
    return VERSION_NUMBER;
  }

  /**
   * @return The build timestamp of the ValSvc read from the internal properties
   *         file. Never <code>null</code>.
   */
  @Nonnull
  public static String getBuildTimestamp ()
  {
    return TIMESTAMP;
  }
}
