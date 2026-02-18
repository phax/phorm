/*
 * Copyright (C) 2022-2026 Philip Helger
 *
 * All rights reserved.
 */
package com.helger.valsvc.ddd;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.helger.annotation.concurrent.Immutable;
import com.helger.ddd.DDDVersion;
import com.helger.ddd.DocumentDetails;
import com.helger.ddd.DocumentDetailsDeterminator;
import com.helger.ddd.model.DDDSyntaxList;
import com.helger.ddd.model.DDDValueProviderList;

/**
 * The utility class to configure and access DDD
 *
 * @author Philip Helger
 */
@Immutable
public final class ValSvcDDD
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ValSvcDDD.class);

  static
  {
    LOGGER.info ("Using DDD " + DDDVersion.getVersionNumber () + " with build date " + DDDVersion.getBuildTimestamp ());
  }

  // Use default configuration
  private static final DocumentDetailsDeterminator DDD = new DocumentDetailsDeterminator (DDDSyntaxList.getDefaultSyntaxList (),
                                                                                          DDDValueProviderList.getDefaultValueProviderList ());

  private ValSvcDDD ()
  {}

  @Nullable
  public static DocumentDetails findDocumentDetails (@NonNull final Element aRootElement)
  {
    // Static instance can be used
    return DDD.findDocumentDetails (aRootElement);
  }
}
