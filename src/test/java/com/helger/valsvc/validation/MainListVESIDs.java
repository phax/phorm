/*
 * Copyright (C) 2022-2024 Philip Helger
 *
 * All rights reserved.
 */
package com.helger.valsvc.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainListVESIDs
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainListVESIDs.class);

  public static void main (final String [] args)
  {
    AppValidator.getAllVESSorted ()
                .forEach (x -> LOGGER.info (x.getID ().getAsSingleID () + " - " + x.getDisplayName ()));
  }
}
