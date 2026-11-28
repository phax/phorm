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
package com.helger.phorm.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.ThreadSafe;

/**
 * The readiness state of {@link AppValidator}. This is a separate class, because accessing
 * {@link AppValidator} triggers the loading of all VES and blocks until it is finished.
 *
 * @author Philip Helger
 * @since 2.2.18
 */
@ThreadSafe
public final class AppValidatorReadiness
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AppValidatorReadiness.class);

  private static volatile boolean s_bReady = false;

  private AppValidatorReadiness ()
  {}

  /**
   * Called by {@link AppValidator} after all VES were loaded.
   */
  static void markReady ()
  {
    s_bReady = true;
  }

  /**
   * @return <code>true</code> if all VES are loaded and validation requests can be served,
   *         <code>false</code> if not (yet). This method never blocks.
   */
  public static boolean isReady ()
  {
    return s_bReady;
  }

  /**
   * Start loading all VES of {@link AppValidator} in a background thread, so that the application
   * startup is not delayed and the readiness changes to <code>true</code> without a validation
   * request. Requests arriving before the loading has finished wait for it.
   */
  public static void startLoadingInBackground ()
  {
    final Thread aThread = new Thread ( () -> {
      try
      {
        LOGGER.info ("Starting to load all VES");
        final int nCount = AppValidator.getAllVES ().size ();
        LOGGER.info ("Finished loading " + nCount + " VES");
      }
      catch (final LinkageError ex)
      {
        // E.g. an ExceptionInInitializerError - the readiness stays false
        LOGGER.error ("Failed to load the VES - phorm is not ready", ex);
      }
    }, "phorm-ves-loader");
    aThread.setDaemon (true);
    aThread.start ();
  }
}
