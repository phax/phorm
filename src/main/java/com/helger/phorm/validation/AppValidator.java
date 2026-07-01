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
import com.helger.phive.cii.CIIValidation;
import com.helger.phive.ciuspt.CIUS_PTValidation;
import com.helger.phive.ciusro.CIUS_ROValidation;
import com.helger.phive.ebinterface.EbInterfaceValidation;
import com.helger.phive.ehf.EHFValidation;
import com.helger.phive.en16931.EN16931Validation;
import com.helger.phive.energieefactuur.EnergieEFactuurValidation;
import com.helger.phive.eracun.HReRacunValidation;
import com.helger.phive.facturae.FacturaeValidation;
import com.helger.phive.fatturapa.FatturaPAValidation;
import com.helger.phive.finvoice.FinvoiceValidation;
import com.helger.phive.france.FranceCTCValidation;
import com.helger.phive.ksef.KSeFValidation;
import com.helger.phive.oioubl.OIOUBLValidation;
import com.helger.phive.peppol.PeppolValidation;
import com.helger.phive.peppol.italy.PeppolItalyValidation;
import com.helger.phive.peppol.legacy.PeppolLegacyValidationBisEurope;
import com.helger.phive.setu.SETUValidation;
import com.helger.phive.simplerinvoicing.SimplerInvoicingValidation;
import com.helger.phive.svefaktura.SvefakturaValidation;
import com.helger.phive.teapps.TEAPPSValidation;
import com.helger.phive.turkey.TurkeyEFaturaValidation;
import com.helger.phive.ubl.UBLValidation;
import com.helger.phive.ublbe.UBLBEValidation;
import com.helger.phive.xml.source.IValidationSourceXML;
import com.helger.phive.xrechnung.XRechnungValidation;
import com.helger.phive.zatca.ZATCAValidation;
import com.helger.phive.zugferd.ZugferdValidation;
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
    EN16931Validation.initEN16931 (VESREG);
    CIIValidation.initCII (VESREG);
    CIUS_PTValidation.initCIUS_PT (VESREG);
    CIUS_ROValidation.initCIUS_RO (VESREG);
    EbInterfaceValidation.initEbInterface (VESREG);
    EHFValidation.initEHF (VESREG);
    FacturaeValidation.initFacturae (VESREG);
    FatturaPAValidation.initFatturaPA (VESREG);
    FinvoiceValidation.initFinvoice (VESREG);
    FranceCTCValidation.initFranceCTC (VESREG);
    HReRacunValidation.init (VESREG);
    KSeFValidation.initKSeF (VESREG);
    OIOUBLValidation.initOIOUBL (VESREG);
    PeppolValidation.initStandard (VESREG);
    PeppolLegacyValidationBisEurope.init (VESREG);
    PeppolItalyValidation.init (VESREG);
    SETUValidation.initSETU (VESREG);
    SimplerInvoicingValidation.initSimplerInvoicing (VESREG);
    // After SimplerInvoicing:
    EnergieEFactuurValidation.initEnergieEFactuur (VESREG);
    SvefakturaValidation.initSvefaktura (VESREG);
    TEAPPSValidation.initTEAPPS (VESREG);
    TurkeyEFaturaValidation.initTurkeyEFatura (VESREG);
    UBLValidation.initUBLAllVersions (VESREG);
    UBLBEValidation.initUBLBE (VESREG);
    XRechnungValidation.initXRechnung (VESREG);
    ZATCAValidation.initZATCA (VESREG);
    ZugferdValidation.initZugferd (VESREG);
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
