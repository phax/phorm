/*
 * Copyright (C) 2022-2024 Philip Helger
 *
 * All rights reserved.
 */
package com.helger.valsvc.jetty;

import javax.annotation.concurrent.Immutable;

import com.helger.photon.jetty.JettyStarter;

/**
 * Run as a standalone web application in Jetty on port 8080.<br>
 * http://localhost:8080/
 *
 * @author Philip Helger
 */
@Immutable
public final class RunInJettyValidationService
{
  public static void main (final String [] args) throws Exception
  {
    new JettyStarter (RunInJettyValidationService.class).run ();
  }
}
