/*
 * Copyright (C) 2022-2026 Philip Helger
 *
 * All rights reserved.
 */
package com.helger.valsvc.jetty;

import java.io.IOException;

import com.helger.photon.jetty.JettyStopper;

public final class JettyStopValidationService
{
  public static void main (final String [] args) throws IOException
  {
    new JettyStopper ().run ();
  }
}
