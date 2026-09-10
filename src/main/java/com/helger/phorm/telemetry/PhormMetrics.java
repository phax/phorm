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
package com.helger.phorm.telemetry;

import com.helger.annotation.concurrent.Immutable;
import com.helger.phorm.validation.AppValidator;
import com.helger.telemetry.ITelemetryCounter;
import com.helger.telemetry.ITelemetryGauge;
import com.helger.telemetry.ITelemetryHistogram;
import com.helger.telemetry.TelemetryMetrics;

/**
 * Static holder for all phorm telemetry instruments. Resolved once per JVM via the
 * {@link TelemetryMetrics} facade; if no meter SPI is on the classpath the underlying instruments
 * are cheap no-ops.
 *
 * @author Philip Helger
 */
@Immutable
public final class PhormMetrics
{
  /** Inbound API requests, by endpoint and outcome. */
  public static final ITelemetryCounter REQUESTS_RECEIVED = TelemetryMetrics.counter ("phorm.requests.received",
                                                                                      "Inbound API requests accepted by phorm",
                                                                                      "{request}");

  /** Validation runs, by VESID, validity and entry point. */
  public static final ITelemetryCounter VALIDATION_RUNS = TelemetryMetrics.counter ("phorm.validation.runs",
                                                                                    "Phive validation executions",
                                                                                    "{run}");

  /** Duration of a phive validation run. */
  public static final ITelemetryHistogram VALIDATION_DURATION = TelemetryMetrics.histogram ("phorm.validation.duration",
                                                                                            "Phive validation duration",
                                                                                            "ms");

  /** Validation findings, by VESID and severity. */
  public static final ITelemetryCounter VALIDATION_FINDINGS = TelemetryMetrics.counter ("phorm.validation.findings",
                                                                                        "Phive validation findings emitted",
                                                                                        "{finding}");

  /** Inbound payload size, by endpoint and payload kind (xml|pdf). */
  public static final ITelemetryHistogram PAYLOAD_BYTES = TelemetryMetrics.histogram ("phorm.payload.bytes",
                                                                                      "Request payload size",
                                                                                      "By");

  /** DDD determinations, by match status and detected syntax. */
  public static final ITelemetryCounter DDD_DETERMINATIONS = TelemetryMetrics.counter ("phorm.ddd.determinations",
                                                                                       "DDD determinations performed",
                                                                                       "{determination}");

  /** VESID registry lookups, by resolved/deprecated state. */
  public static final ITelemetryCounter VESID_RESOLUTIONS = TelemetryMetrics.counter ("phorm.vesid.resolutions",
                                                                                      "VESID registry lookups",
                                                                                      "{lookup}");

  /** Hybrid PDF carrier validation runs, by outcome and country. */
  public static final ITelemetryCounter KALTBLUT_RUNS = TelemetryMetrics.counter ("phorm.kaltblut.runs",
                                                                                  "Kaltblut PDF carrier validation runs",
                                                                                  "{run}");

  /** Size of XML extracted from a hybrid PDF, by country. */
  public static final ITelemetryHistogram KALTBLUT_EMBEDDED_XML_BYTES = TelemetryMetrics.histogram ("phorm.kaltblut.embedded_xml.bytes",
                                                                                                    "Size of XML extracted from a hybrid PDF",
                                                                                                    "By");

  /** Number of validation executor sets currently registered. */
  public static final ITelemetryGauge VES_REGISTRY_SIZE = TelemetryMetrics.gauge ("phorm.ves.registry.size",
                                                                                  "Number of registered validation executor sets",
                                                                                  "{ves}",
                                                                                  () -> AppValidator.getAllVES ()
                                                                                                    .size ());

  private PhormMetrics ()
  {}

  /**
   * Force initialisation of all static instruments. Call once during application startup so the
   * observable gauge is registered with the meter provider.
   */
  public static void init ()
  {
    // Touching the class triggers static initialisation
  }
}
