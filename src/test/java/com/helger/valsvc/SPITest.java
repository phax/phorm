/*
 * Copyright (C) 2022-2024 Philip Helger
 *
 * All rights reserved.
 */
package com.helger.valsvc;

import org.junit.Test;

import com.helger.commons.mock.SPITestHelper;
import com.helger.photon.core.mock.PhotonCoreValidator;

public final class SPITest
{
  @Test
  public void testBasic () throws Exception
  {
    SPITestHelper.testIfAllSPIImplementationsAreValid ();
    PhotonCoreValidator.validateExternalResources ();
  }
}
