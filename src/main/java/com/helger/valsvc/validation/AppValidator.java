/*
 * Copyright (C) 2022-2025 Philip Helger
 *
 * All rights reserved.
 */
package com.helger.valsvc.validation;

import java.util.Comparator;
import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Document;

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
import com.helger.phive.ubl.UBLValidation;
import com.helger.phive.ublbe.UBLBEValidation;
import com.helger.phive.xml.source.IValidationSourceXML;
import com.helger.phive.xml.source.ValidationSourceXML;
import com.helger.phive.xrechnung.XRechnungValidation;
import com.helger.phive.zatca.ZATCAValidation;
import com.helger.phive.zugferd.ZugferdValidation;

/**
 * Default validation repository
 *
 * @author Philip Helger
 */
public class AppValidator
{
  private static final ValidationExecutorSetRegistry <IValidationSourceXML> VESREG = new ValidationExecutorSetRegistry <> ();
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
    UBLValidation.initUBLAllVersions (VESREG);
    UBLBEValidation.initUBLBE (VESREG);
    XRechnungValidation.initXRechnung (VESREG);
    ZATCAValidation.initZATCA (VESREG);
    ZugferdValidation.initZugferd (VESREG);
  }

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

  private AppValidator ()
  {}

  @NonNull
  public static ValidationResultList validate (@NonNull final DVRCoordinate aVESID,
                                               @NonNull final Document aDoc,
                                               @NonNull final Locale aDisplayLocale)
  {
    // Start validation
    return ValidationExecutionManager.executeValidation (IValidityDeterminator.createDefault (),
                                                         getVES (aVESID),
                                                         ValidationSourceXML.create ("uploaded content", aDoc),
                                                         aDisplayLocale);
  }
}
