/*
 * Copyright (C) 2022-2024 Philip Helger
 *
 * All rights reserved.
 */
package com.helger.valsvc;

import java.util.Locale;

import javax.annotation.concurrent.Immutable;

import com.helger.photon.security.CSecurity;

@Immutable
public final class CApp
{
  public static final Locale DEFAULT_LOCALE = Locale.UK;

  public static final String APP_NAME = "Standalone Validation Service";

  // User group IDs
  public static final String USERGROUPID_SUPERUSER = CSecurity.USERGROUP_ADMINISTRATORS_ID;

  private CApp ()
  {}
}
