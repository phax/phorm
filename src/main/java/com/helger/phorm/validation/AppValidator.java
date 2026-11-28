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

import java.io.File;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.GuardedBy;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.concurrent.SimpleReadWriteLock;
import com.helger.base.exception.InitializationException;
import com.helger.base.state.ESuccess;
import com.helger.base.string.StringHelper;
import com.helger.base.string.StringImplode;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsLinkedHashMap;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsOrderedMap;
import com.helger.datetime.helper.PDTFactory;
import com.helger.diagnostics.error.SingleError;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.diver.api.coord.DVRCoordinate;
import com.helger.phive.api.execute.ValidationExecutionManager;
import com.helger.phive.api.executorset.IValidationExecutorSet;
import com.helger.phive.api.executorset.ValidationExecutorSetRegistry;
import com.helger.phive.api.result.ValidationResultList;
import com.helger.phive.api.validity.IValidityDeterminator;
import com.helger.phive.rules.all.PhiveRulesValidation;
import com.helger.phive.rules.all.legacy.PhiveRulesLegacyValidation;
import com.helger.phive.ves.engine.load.LoadedVES;
import com.helger.phive.xml.source.IValidationSourceXML;
import com.helger.phorm.AppConfig;
import com.helger.phorm.CApp;
import com.helger.phorm.repo.ExternalVES;
import com.helger.phorm.repo.ExternalVESLoader;
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
  private static final Logger LOGGER = LoggerFactory.getLogger (AppValidator.class);

  private static final ValidationExecutorSetRegistry <IValidationSourceXML> VESREG = new ValidationExecutorSetRegistry <> ();
  // Only the built-in VES, to resolve the requirements of external VES. Never changes after the
  // static initialisation and has its own lock.
  private static final ValidationExecutorSetRegistry <IValidationSourceXML> BUILTIN_VESREG = new ValidationExecutorSetRegistry <> ();
  private static final IValidityDeterminator <IValidationSourceXML> VD = IValidityDeterminator.createDefault ();
  // Guards the registry against lookups while the external VES are swapped
  private static final SimpleReadWriteLock RW_LOCK = new SimpleReadWriteLock ();
  // Key is the VESID, value is the repository ID
  @GuardedBy ("RW_LOCK")
  private static final ICommonsOrderedMap <DVRCoordinate, String> EXTERNAL_VESIDS = new CommonsLinkedHashMap <> ();
  @GuardedBy ("RW_LOCK")
  private static final ICommonsList <String> EXTERNAL_REPO_IDS = new CommonsArrayList <> ();
  @GuardedBy ("RW_LOCK")
  private static OffsetDateTime s_aExternalLoadDT;
  static
  {
    PhiveRulesValidation.initPhiveRules (VESREG);
    PhiveRulesLegacyValidation.initPhiveRulesLegacy (VESREG);
    // Same instances, so no additional memory for the rules is needed
    for (final IValidationExecutorSet <IValidationSourceXML> aVES : VESREG.getAll ())
      BUILTIN_VESREG.registerValidationExecutorSet (aVES);
  }

  private AppValidator ()
  {}

  @NonNull
  public static ICommonsList <IValidationExecutorSet <IValidationSourceXML>> getAllVES ()
  {
    return RW_LOCK.readLockedGet (VESREG::getAll);
  }

  @NonNull
  public static ICommonsList <IValidationExecutorSet <IValidationSourceXML>> getAllVESSorted ()
  {
    return getAllVES ().getSortedInline (Comparator.comparing (x -> x.getID ().getAsSingleID ()));
  }

  public static boolean containsVES (@NonNull final DVRCoordinate aVESID)
  {
    return getVESOrNull (aVESID) != null;
  }

  @Nullable
  public static IValidationExecutorSet <IValidationSourceXML> getVESOrNull (@NonNull final DVRCoordinate aVESID)
  {
    return RW_LOCK.readLockedGet (() -> VESREG.getOfID (aVESID));
  }

  @Nullable
  public static String getLatestVersion (@NonNull final DVRCoordinate aVESID)
  {
    final IValidationExecutorSet <IValidationSourceXML> aLatest = RW_LOCK.readLockedGet (() -> VESREG.getLatestVersion (aVESID.getGroupID (),
                                                                                                                        aVESID.getArtifactID (),
                                                                                                                        null));
    return aLatest == null ? null : aLatest.getID ().getVersionString ();
  }

  @NonNull
  public static IValidationExecutorSet <IValidationSourceXML> getVES (@NonNull final DVRCoordinate aVESID)
  {
    final IValidationExecutorSet <IValidationSourceXML> aVES = getVESOrNull (aVESID);
    if (aVES == null)
      throw new IllegalStateException ("Unexpected VESID " + aVESID.getAsSingleID ());
    return aVES;
  }

  /**
   * Get the ID of the external repository the provided VES was loaded from.
   *
   * @param aVESID
   *        The VESID to check. May not be <code>null</code>.
   * @return <code>null</code> if the VES is unknown or one of the built-in VES.
   * @since 2.3.0
   */
  @Nullable
  public static String getExternalRepositoryIDOfVES (@NonNull final DVRCoordinate aVESID)
  {
    return RW_LOCK.readLockedGet (() -> EXTERNAL_VESIDS.get (aVESID));
  }

  /**
   * @return A copy of all VESIDs loaded from external repositories, mapped to the ID of the
   *         repository they were loaded from. Never <code>null</code>.
   * @since 2.3.0
   */
  @NonNull
  @ReturnsMutableCopy
  public static ICommonsOrderedMap <DVRCoordinate, String> getAllExternalVESIDs ()
  {
    return RW_LOCK.readLockedGet (EXTERNAL_VESIDS::getClone);
  }

  /**
   * @return The IDs of all external repositories in the order of declaration. Never
   *         <code>null</code>.
   * @since 2.3.0
   */
  @NonNull
  @ReturnsMutableCopy
  public static ICommonsList <String> getAllExternalRepositoryIDs ()
  {
    return RW_LOCK.readLockedGet (EXTERNAL_REPO_IDS::getClone);
  }

  /**
   * @return The date and time when the external repositories were last loaded successfully.
   *         <code>null</code> if they were never loaded.
   * @since 2.3.0
   */
  @Nullable
  public static OffsetDateTime getExternalRepositoriesLoadDateTime ()
  {
    return RW_LOCK.readLockedGet (() -> s_aExternalLoadDT);
  }

  /**
   * Resolve a VES required by an external VES, that is not contained in the repository of the
   * external VES, against the built-in VES. External VES of other repositories are never used.
   *
   * @param aRequiredVES
   *        The requirement to resolve. May not be <code>null</code>.
   * @param aErrorList
   *        The error list. Not used, as the caller reports an unresolvable requirement.
   * @return <code>null</code> if no built-in VES matches. Pseudo versions are resolved against the
   *         built-in VES only.
   */
  @Nullable
  private static IValidationExecutorSet <IValidationSourceXML> _resolveBuiltinRequirement (final LoadedVES.@NonNull RequiredVES aRequiredVES,
                                                                                            @NonNull final ErrorList aErrorList)
  {
    final IValidationExecutorSet <IValidationSourceXML> ret = BUILTIN_VESREG.getOfID (aRequiredVES.getRequiredVESID ());
    if (ret != null)
      LOGGER.info ("Resolved the required VESID '" +
                   aRequiredVES.getRequiredVESID ().getAsSingleID () +
                   "' to the built-in VES '" +
                   ret.getID ().getAsSingleID () +
                   "'");
    return ret;
  }

  @NonNull
  private static ESuccess _loadExternalRepositories (@Nullable final File aConfigFile,
                                                     @NonNull final ErrorList aErrorList)
  {
    final ICommonsList <String> aNewRepoIDs;
    final ICommonsList <ExternalVES> aNewVES;
    if (aConfigFile == null)
    {
      // No configuration file - all external VES are removed
      aNewRepoIDs = new CommonsArrayList <> ();
      aNewVES = new CommonsArrayList <> ();
    }
    else
    {
      LOGGER.info ("Loading external repositories from '" + aConfigFile.getAbsolutePath () + "'");
      final ExternalVESLoader.LoadResult aResult = ExternalVESLoader.load (aConfigFile,
                                                                                  AppValidator::_resolveBuiltinRequirement,
                                                                                  aErrorList);
      if (aResult == null)
        return ESuccess.FAILURE;
      aNewRepoIDs = aResult.getAllRepositoryIDs ();
      aNewVES = aResult.getAllVES ();
    }

    return RW_LOCK.writeLockedGet (() -> {
      // Everything registered that was not loaded externally is built-in
      for (final ExternalVES aExternalVES : aNewVES)
      {
        final DVRCoordinate aVESID = aExternalVES.getVES ().getID ();
        if (VESREG.getOfID (aVESID) != null && !EXTERNAL_VESIDS.containsKey (aVESID))
          aErrorList.add (SingleError.builderError ()
                                     .errorFieldName (aExternalVES.getRepositoryID ())
                                     .errorText ("The VESID '" +
                                                 aVESID.getAsSingleID () +
                                                 "' from repository '" +
                                                 aExternalVES.getRepositoryID () +
                                                 "' is already registered as a built-in VES")
                                     .build ());
      }
      if (aErrorList.containsAtLeastOneError ())
        return ESuccess.FAILURE;

      // Remove all previously loaded external VES
      for (final DVRCoordinate aVESID : EXTERNAL_VESIDS.keySet ())
        VESREG.unregisterValidationExecutorSet (aVESID);
      EXTERNAL_VESIDS.clear ();

      // Register the new ones
      for (final ExternalVES aExternalVES : aNewVES)
      {
        VESREG.registerValidationExecutorSet (aExternalVES.getVES ());
        EXTERNAL_VESIDS.put (aExternalVES.getVES ().getID (), aExternalVES.getRepositoryID ());
      }
      EXTERNAL_REPO_IDS.setAll (aNewRepoIDs);
      s_aExternalLoadDT = aConfigFile == null ? null : PDTFactory.getCurrentOffsetDateTimeUTC ();

      LOGGER.info ("Registered " + aNewVES.size () + " VES from " + aNewRepoIDs.size () + " external repositories");
      return ESuccess.SUCCESS;
    });
  }

  /**
   * Load all VES from the repositories of the provided configuration file and replace all
   * previously loaded external VES with them. The replacement is atomic: if any error occurs,
   * nothing is changed. A VESID that is already registered as a built-in VES is an error. A VES
   * required by an external VES is searched in the same repository first and then in the built-in
   * VES.
   *
   * @param aConfigFile
   *        The repository configuration file. May be <code>null</code> to remove all external VES.
   * @param aErrorList
   *        The error list to be filled. May not be <code>null</code>.
   * @return {@link ESuccess#SUCCESS} if the external VES were replaced.
   * @since 2.3.0
   */
  @NonNull
  public static ESuccess loadExternalRepositories (@Nullable final File aConfigFile,
                                                   @NonNull final ErrorList aErrorList)
  {
    return Telemetry.withSpan (CPhormTelemetry.SPAN_REPOSITORIES_LOAD, ETelemetrySpanKind.INTERNAL, aSpan -> {
      final ESuccess eSuccess = _loadExternalRepositories (aConfigFile, aErrorList);
      final boolean bSuccess = eSuccess.isSuccess ();
      aSpan.setAttribute (CPhormTelemetry.ATTR_REPOSITORIES_SUCCESS, bSuccess);
      // The counts reflect the new state only on success
      if (bSuccess)
        aSpan.setAttribute (CPhormTelemetry.ATTR_REPOSITORIES_COUNT, getAllExternalRepositoryIDs ().size ())
             .setAttribute (CPhormTelemetry.ATTR_REPOSITORIES_VES_COUNT, getAllExternalVESIDs ().size ());
      PhormMetrics.REPOSITORY_LOADS.add (1, TelemetryAttributes.builder ().put ("success", bSuccess).build ());
      return eSuccess;
    });
  }

  /**
   * @return The repository configuration file from the configuration property
   *         <code>phorm.repositories.file</code>. <code>null</code> if the property is not set.
   * @since 2.3.0
   */
  @Nullable
  public static File getConfiguredRepositoriesFile ()
  {
    final String sFile = StringHelper.trim (AppConfig.getRepositoriesFile ());
    return StringHelper.isEmpty (sFile) ? null : new File (sFile);
  }

  /**
   * Load the external repositories as configured in <code>phorm.repositories.file</code>. Called
   * once on application startup.
   *
   * @throws InitializationException
   *         if loading failed. This aborts the startup.
   * @since 2.3.0
   */
  public static void initExternalRepositories ()
  {
    final File aConfigFile = getConfiguredRepositoriesFile ();
    if (aConfigFile == null)
    {
      LOGGER.info ("No external repositories are configured");
      return;
    }

    final ErrorList aErrorList = new ErrorList ();
    if (loadExternalRepositories (aConfigFile, aErrorList).isFailure ())
      throw new InitializationException ("Failed to load the external repositories from '" +
                                         aConfigFile.getAbsolutePath () +
                                         "':\n  " +
                                         StringImplode.imploder ()
                                                      .source (aErrorList.getAllFailures (),
                                                               x -> x.getAsString (CApp.DEFAULT_LOCALE))
                                                      .separator ("\n  ")
                                                      .build ());
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
