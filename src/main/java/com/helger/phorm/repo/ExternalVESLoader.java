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
package com.helger.phorm.repo;

import java.io.File;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.reflection.GenericReflection;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsLinkedHashMap;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsOrderedMap;
import com.helger.diagnostics.error.SingleError;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.diver.api.coord.DVRCoordinate;
import com.helger.diver.repo.config.ConfiguredRepoStorage;
import com.helger.diver.repo.config.ERepoTopTocType;
import com.helger.diver.repo.config.RepoStorageConfig;
import com.helger.diver.repo.config.RepoStorageConfigReader;
import com.helger.diver.repo.config.RepoStorageFactoryRegistry;
import com.helger.phive.api.executorset.IValidationExecutorSet;
import com.helger.phive.api.source.IValidationSource;
import com.helger.phive.ves.engine.load.LoadedVES;
import com.helger.phive.ves.engine.load.VESLoader;
import com.helger.phive.ves.model.v1.EVESSyntax;
import com.helger.phive.xml.source.IValidationSourceXML;
import com.helger.phorm.CApp;

/**
 * Load all VES from the repositories described in a repository configuration file. Nothing is
 * registered here - see <code>AppValidator</code> for that.
 *
 * @author Philip Helger
 * @since 2.3.0
 */
@Immutable
public final class ExternalVESLoader
{
  /**
   * The result of loading all external repositories.
   *
   * @author Philip Helger
   */
  @Immutable
  public static final class LoadResult
  {
    private final ICommonsList <String> m_aRepositoryIDs;
    private final ICommonsList <ExternalVES> m_aVES;

    public LoadResult (@NonNull final ICommonsList <String> aRepositoryIDs, @NonNull final ICommonsList <ExternalVES> aVES)
    {
      ValueEnforcer.notNull (aRepositoryIDs, "RepositoryIDs");
      ValueEnforcer.notNull (aVES, "VES");
      m_aRepositoryIDs = aRepositoryIDs.getClone ();
      m_aVES = aVES.getClone ();
    }

    /**
     * @return The IDs of all configured repositories in the order of declaration. Never
     *         <code>null</code>.
     */
    @NonNull
    @ReturnsMutableCopy
    public ICommonsList <String> getAllRepositoryIDs ()
    {
      return m_aRepositoryIDs.getClone ();
    }

    /**
     * @return All VES loaded from all repositories. Never <code>null</code>.
     */
    @NonNull
    @ReturnsMutableCopy
    public ICommonsList <ExternalVES> getAllVES ()
    {
      return m_aVES.getClone ();
    }
  }

  private static final Logger LOGGER = LoggerFactory.getLogger (ExternalVESLoader.class);

  private ExternalVESLoader ()
  {}

  /**
   * Load a single VES from a repository.
   *
   * @return <code>null</code> if loading failed. In that case an error was added.
   */
  @Nullable
  private static IValidationExecutorSet <IValidationSourceXML> _loadVES (@NonNull final VESLoader aLoader,
                                                                        @NonNull final String sRepoID,
                                                                        @NonNull final DVRCoordinate aVESID,
                                                                        @NonNull final ErrorList aErrorList)
  {
    final String sErrorPrefix = "VES '" + aVESID.getAsSingleID () + "' from repository '" + sRepoID + "': ";

    final ErrorList aLoadingErrors = new ErrorList ();
    final LoadedVES aLoadedVES = aLoader.loadVESFromRepo (aVESID, aLoadingErrors);
    aLoadingErrors.forEach (x -> aErrorList.add (SingleError.builder (x)
                                                            .errorText (sErrorPrefix + x.getErrorText (CApp.DEFAULT_LOCALE))
                                                            .build ()));
    if (aLoadedVES == null || aLoadingErrors.containsAtLeastOneError ())
    {
      aErrorList.add (SingleError.builderError ().errorFieldName (sRepoID).errorText (sErrorPrefix + "failed to load").build ());
      return null;
    }

    final EVESSyntax eSyntax = aLoadedVES.getHeader ().getVESSyntax ();
    if (eSyntax != EVESSyntax.XSD && eSyntax != EVESSyntax.SCHEMATRON)
    {
      aErrorList.add (SingleError.builderError ()
                                 .errorFieldName (sRepoID)
                                 .errorText (sErrorPrefix +
                                             "the syntax '" +
                                             eSyntax.getID () +
                                             "' is not supported - only XML Schema and Schematron are supported")
                                 .build ());
      return null;
    }

    if (!aLoadedVES.hasExecutor ())
    {
      aErrorList.add (SingleError.builderError ()
                                 .errorFieldName (sRepoID)
                                 .errorText (sErrorPrefix + "the VES has no executor and can't be used for validation")
                                 .build ());
      return null;
    }

    // Only XML executors are contained, as ensured by the syntax check above
    final IValidationExecutorSet <IValidationSource> aVES = aLoadedVES.createVES ();
    return GenericReflection.uncheckedCast (aVES);
  }

  /**
   * Load all VES from all repositories of the provided configuration file. Every problem is an
   * error: an unreadable configuration, a repository that can't be created, a repository without a
   * <code>diver-xml</code> top-level ToC, a VES that can't be loaded (including its requirements),
   * a VES with an unsupported syntax and the same VESID in more than one repository.
   *
   * @param aConfigFile
   *        The repository configuration file. May not be <code>null</code>.
   * @param aErrorList
   *        The error list to be filled. May not be <code>null</code>.
   * @return <code>null</code> if at least one error occurred.
   */
  @Nullable
  public static LoadResult load (@NonNull final File aConfigFile, @NonNull final ErrorList aErrorList)
  {
    ValueEnforcer.notNull (aConfigFile, "ConfigFile");
    ValueEnforcer.notNull (aErrorList, "ErrorList");

    final RepoStorageConfig aConfig = RepoStorageConfigReader.read (aConfigFile, aErrorList);
    if (aConfig == null)
      return null;

    final ICommonsList <ConfiguredRepoStorage> aRepos = aConfig.createAllRepoStorages (RepoStorageFactoryRegistry.createDefault (),
                                                                                       aErrorList);
    if (aErrorList.containsAtLeastOneError ())
      return null;

    final ICommonsList <String> aRepoIDs = new CommonsArrayList <> ();
    final ICommonsOrderedMap <DVRCoordinate, ExternalVES> aAllVES = new CommonsLinkedHashMap <> ();
    for (final ConfiguredRepoStorage aRepo : aRepos)
    {
      final String sRepoID = aRepo.getID ();
      aRepoIDs.add (sRepoID);

      if (!aRepo.hasTopTocService ())
      {
        aErrorList.add (SingleError.builderError ()
                                   .errorFieldName (sRepoID)
                                   .errorText ("Repository '" +
                                               sRepoID +
                                               "' must use the top-level ToC type '" +
                                               ERepoTopTocType.DIVER_XML.getID () +
                                               "', so that its VES can be found")
                                   .build ());
        continue;
      }

      // Eager, so that problems with required VES are detected now and not on first validation
      final VESLoader aLoader = new VESLoader (aRepo.getRepoStorage ()).setUseEagerRequirementLoading (true);
      final ICommonsList <DVRCoordinate> aVESIDs = RepositoryVESEnumerator.getAllVESIDs (aRepo.getRepoStorage (),
                                                                                         aRepo.getTopTocService ());
      LOGGER.info ("Found " + aVESIDs.size () + " VES in repository '" + sRepoID + "'");

      for (final DVRCoordinate aVESID : aVESIDs)
      {
        final ExternalVES aExisting = aAllVES.get (aVESID);
        if (aExisting != null)
        {
          aErrorList.add (SingleError.builderError ()
                                     .errorFieldName (sRepoID)
                                     .errorText ("The VESID '" +
                                                 aVESID.getAsSingleID () +
                                                 "' is contained in repository '" +
                                                 aExisting.getRepositoryID () +
                                                 "' and in repository '" +
                                                 sRepoID +
                                                 "'")
                                     .build ());
          continue;
        }

        final IValidationExecutorSet <IValidationSourceXML> aVES = _loadVES (aLoader, sRepoID, aVESID, aErrorList);
        if (aVES != null)
          aAllVES.put (aVESID, new ExternalVES (sRepoID, aVES));
      }
    }

    if (aErrorList.containsAtLeastOneError ())
      return null;
    return new LoadResult (aRepoIDs, aAllVES.copyOfValues ());
  }
}
