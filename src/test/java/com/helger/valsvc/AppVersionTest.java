/*
 * Copyright (C) Philip Helger
 *
 * All rights reserved.
 */
package com.helger.valsvc;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public final class AppVersionTest
{
  @Test
  public void testBasic () throws Exception
  {
    assertNotNull (AppVersion.getVersionNumber ());
    assertNotNull (AppVersion.getBuildTimestamp ());
  }
}
