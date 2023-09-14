/*
 * Copyright (C) Philip Helger
 *
 * All rights reserved.
 */
package com.helger.valsvc.validation;

import java.util.Comparator;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.w3c.dom.Document;

import com.helger.commons.collection.impl.ICommonsList;
import com.helger.diver.api.version.VESID;
import com.helger.phive.api.execute.ValidationExecutionManager;
import com.helger.phive.api.executorset.IValidationExecutorSet;
import com.helger.phive.api.executorset.ValidationExecutorSetRegistry;
import com.helger.phive.api.result.ValidationResultList;
import com.helger.phive.en16931.EN16931Validation;
import com.helger.phive.peppol.PeppolValidation;
import com.helger.phive.xml.source.IValidationSourceXML;
import com.helger.phive.xml.source.ValidationSourceXML;

/**
 * Default validation repository
 *
 * @author Philip Helger
 */
public class AppValidator
{
  private static final ValidationExecutorSetRegistry <IValidationSourceXML> VER = new ValidationExecutorSetRegistry <> ();
  static
  {
    EN16931Validation.initEN16931 (VER);
    PeppolValidation.initStandard (VER);
  }

  @Nonnull
  public static ICommonsList <IValidationExecutorSet <IValidationSourceXML>> getAllVES ()
  {
    return VER.getAll ();
  }

  @Nonnull
  public static ICommonsList <IValidationExecutorSet <IValidationSourceXML>> getAllVESSorted ()
  {
    return VER.getAll ().getSortedInline (Comparator.comparing (x -> x.getID ().getAsSingleID ()));
  }

  @Nullable
  public static IValidationExecutorSet <IValidationSourceXML> getVESOrNull (@Nonnull final VESID aVESID)
  {
    return VER.getOfID (aVESID);
  }

  @Nullable
  public static String getLatestVersion (@Nonnull final VESID aVESID)
  {
    final IValidationExecutorSet <IValidationSourceXML> aLatest = VER.getLatestVersion (aVESID.getGroupID (),
                                                                                        aVESID.getArtifactID (),
                                                                                        null);
    return aLatest == null ? null : aLatest.getID ().getVersionString ();
  }

  @Nonnull
  public static IValidationExecutorSet <IValidationSourceXML> getVES (@Nonnull final VESID aVESID)
  {
    final IValidationExecutorSet <IValidationSourceXML> aVES = VER.getOfID (aVESID);
    if (aVES == null)
      throw new IllegalStateException ("Unexpected VESID " + aVESID.getAsSingleID ());
    return aVES;
  }

  private AppValidator ()
  {}

  @Nonnull
  public static ValidationResultList validate (@Nonnull final VESID aVESID,
                                               @Nonnull final Document aDoc,
                                               @Nonnull final Locale aDisplayLocale)
  {
    // Start validation
    return ValidationExecutionManager.executeValidation (getVES (aVESID),
                                                         ValidationSourceXML.create ("uploaded content", aDoc),
                                                         aDisplayLocale);
  }
}
