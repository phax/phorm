/*
 * Copyright (C) 2022-2024 Philip Helger
 *
 * All rights reserved.
 */
package com.helger.valsvc.validation;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * Test class for class {@link AppValidator}.
 *
 * @author Philip Helger
 */
public class AppValidatorTest
{
  @Test
  public void testBasic ()
  {
    // Make sure the registrations all work
    assertNotNull (AppValidator.getAllVESSorted ());
  }
}
