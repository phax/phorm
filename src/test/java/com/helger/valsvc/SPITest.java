/*
 * Copyright (C) 2022-2026 Philip Helger
 *
 * All rights reserved.
 */
package com.helger.valsvc;

import org.junit.Test;

import com.helger.photon.core.mock.PhotonCoreValidator;
import com.helger.unittestext.SPITestHelper;

public final class SPITest
{
  @Test
  public void testBasic () throws Exception
  {
    SPITestHelper.testIfAllSPIImplementationsAreValid ();
    PhotonCoreValidator.validateExternalResources ();
  }
}
