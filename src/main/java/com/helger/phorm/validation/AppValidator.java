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

import java.util.Comparator;
import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.collection.commons.ICommonsList;
import com.helger.diver.api.coord.DVRCoordinate;
import com.helger.phive.api.execute.ValidationExecutionManager;
import com.helger.phive.api.executorset.IValidationExecutorSet;
import com.helger.phive.api.executorset.ValidationExecutorSetRegistry;
import com.helger.phive.api.result.ValidationResultList;
import com.helger.phive.api.validity.IValidityDeterminator;
import com.helger.phive.rules.all.PhiveRulesValidation;
import com.helger.phive.rules.all.legacy.PhiveRulesLegacyValidation;
import com.helger.phive.xml.source.IValidationSourceXML;
import com.helger.phorm.telemetry.CPhormTelemetry;
import com.helger.phorm.telemetry.PhormMetrics;
import com.helger.telemetry.ETelemetrySpanKind;
import com.helger.telemetry.Telemetry;
import com.helger.telemetry.TelemetryAttributes;

/**
 * Default validation repository
 *
 * @author Philip Helger
 */
public class AppValidator
{
  private static final ValidationExecutorSetRegistry <IValidationSourceXML> VESREG = new ValidationExecutorSetRegistry <> ();
  private static final IValidityDeterminator <IValidationSourceXML> VD = IValidityDeterminator.createDefault ();
  static
  {
    PhiveRulesValidation.initPhiveRules (VESREG);
    PhiveRulesLegacyValidation.initPhiveRulesLegacy (VESREG);
    AppValidatorReadiness.markReady ();
  }

  private AppValidator ()
  {}

  @NonNull
  public static ICommonsList <IValidationExecutorSet <IValidationSourceXML>> getAllVES ()
  {
    return VESREG.getAll ();
  }

  @NonNull
  public static ICommonsList <IValidationExecutorSet <IValidationSourceXML>> getAllVESSorted ()
  {
    return VESREG.getAll ().getSortedInline (Comparator.comparing (x -> x.getID ().getAsSingleID ()));
  }

  public static boolean containsVES (@NonNull final DVRCoordinate aVESID)
  {
    return getVESOrNull (aVESID) != null;
  }

  @Nullable
  public static IValidationExecutorSet <IValidationSourceXML> getVESOrNull (@NonNull final DVRCoordinate aVESID)
  {
    return VESREG.getOfID (aVESID);
  }

  @Nullable
  public static String getLatestVersion (@NonNull final DVRCoordinate aVESID)
  {
    final IValidationExecutorSet <IValidationSourceXML> aLatest = VESREG.getLatestVersion (aVESID.getGroupID (),
                                                                                           aVESID.getArtifactID (),
                                                                                           null);
    return aLatest == null ? null : aLatest.getID ().getVersionString ();
  }

  @NonNull
  public static IValidationExecutorSet <IValidationSourceXML> getVES (@NonNull final DVRCoordinate aVESID)
  {
    final IValidationExecutorSet <IValidationSourceXML> aVES = VESREG.getOfID (aVESID);
    if (aVES == null)
      throw new IllegalStateException ("Unexpected VESID " + aVESID.getAsSingleID ());
    return aVES;
  }

  @NonNull
  public static ValidationResultList validate (@NonNull final IValidationExecutorSet <IValidationSourceXML> aVES,
                                               @NonNull final IValidationSourceXML aSrc,
                                               @NonNull final Locale aDisplayLocale,
                                               @NonNull final String sVia)
  {
    final String sVESID = aVES.getID ().getAsSingleID ();
    final String sVESName = aVES.getDisplayName ();

    return Telemetry.withSpan (CPhormTelemetry.SPAN_PHIVE_VALIDATE, ETelemetrySpanKind.INTERNAL, aSpan -> {
      aSpan.setAttribute (CPhormTelemetry.ATTR_VESID, sVESID).setAttribute (CPhormTelemetry.ATTR_VESID_NAME, sVESName);

      final ValidationResultList aVRL = new ValidationExecutionManager <> (VD, aVES).executeValidation (aSrc,
                                                                                                        aDisplayLocale);

      final int nErrors = aVRL.getAllErrors ().size ();
      final int nFailures = aVRL.getAllFailures ().size ();
      final int nWarnings = nFailures - nErrors;
      final boolean bValid = aVRL.getOverallValidity ().isValid ();
      final long nDurationMs = aVRL.hasValidationDuration () ? aVRL.getValidationDuration ().toMillis () : 0L;

      aSpan.setAttribute (CPhormTelemetry.ATTR_VALIDATION_LAYERS, aVRL.size ())
           .setAttribute (CPhormTelemetry.ATTR_VALIDATION_ERRORS, nErrors)
           .setAttribute (CPhormTelemetry.ATTR_VALIDATION_WARNINGS, nWarnings)
           .setAttribute (CPhormTelemetry.ATTR_VALIDATION_VALID, bValid)
           .setAttribute (CPhormTelemetry.ATTR_VALIDATION_DURATION_MS, nDurationMs);

      PhormMetrics.VALIDATION_RUNS.add (1,
                                        TelemetryAttributes.builder ()
                                                           .put ("vesid", sVESID)
                                                           .put ("valid", bValid)
                                                           .put ("via", sVia)
                                                           .build ());
      PhormMetrics.VALIDATION_DURATION.record (nDurationMs,
                                               TelemetryAttributes.builder ().put ("vesid", sVESID).build ());
      if (nErrors > 0)
        PhormMetrics.VALIDATION_FINDINGS.add (nErrors,
                                              TelemetryAttributes.builder ()
                                                                 .put ("vesid", sVESID)
                                                                 .put ("severity", "error")
                                                                 .build ());
      if (nWarnings > 0)
        PhormMetrics.VALIDATION_FINDINGS.add (nWarnings,
                                              TelemetryAttributes.builder ()
                                                                 .put ("vesid", sVESID)
                                                                 .put ("severity", "warn")
                                                                 .build ());
      return aVRL;
    });
  }
}
