/*
 * Copyright (C) 2022-2026 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.phorm.ddd;

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
public final class PhormDDD
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PhormDDD.class);

  static
  {
    LOGGER.info ("Using DDD " + DDDVersion.getVersionNumber () + " with build date " + DDDVersion.getBuildTimestamp ());
  }

  // Use default configuration
  private static final DocumentDetailsDeterminator DDD = new DocumentDetailsDeterminator (DDDSyntaxList.getDefaultSyntaxList (),
                                                                                          DDDValueProviderList.getDefaultValueProviderList ());

  private PhormDDD ()
  {}

  @Nullable
  public static DocumentDetails findDocumentDetails (@NonNull final Element aRootElement)
  {
    // Static instance can be used
    return DDD.findDocumentDetails (aRootElement);
  }
}
