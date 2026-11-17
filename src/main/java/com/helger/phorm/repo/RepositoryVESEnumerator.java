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

import org.jspecify.annotations.NonNull;

import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsLinkedHashSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsOrderedSet;
import com.helger.diver.api.coord.DVRCoordinate;
import com.helger.diver.api.version.DVRVersion;
import com.helger.diver.repo.RepoStorageKeyOfArtefact;
import com.helger.diver.repo.toc.IRepoStorageWithToc;
import com.helger.diver.repo.toc.IRepoTopTocService;
import com.helger.diver.repo.toc.RepoToc;
import com.helger.phive.ves.engine.load.VESLoader;

/**
 * Find all VES in a repository: all group IDs and artifact IDs from the top-level ToC, all versions
 * from the ToC per artifact, and from these all coordinates that have a {@link VESLoader#FILE_EXT_VES}
 * artefact.
 *
 * @author Philip Helger
 */
@Immutable
public final class RepositoryVESEnumerator
{
  private RepositoryVESEnumerator ()
  {}

  /**
   * Get all VESIDs contained in the provided repository.
   *
   * @param aRepo
   *        The repository to scan. May not be <code>null</code>.
   * @param aTopTocService
   *        The initialised top-level ToC service of the repository. May not be <code>null</code>.
   * @return All VESIDs in a stable order. Never <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  public static ICommonsList <DVRCoordinate> getAllVESIDs (@NonNull final IRepoStorageWithToc aRepo,
                                                           @NonNull final IRepoTopTocService aTopTocService)
  {
    ValueEnforcer.notNull (aRepo, "Repo");
    ValueEnforcer.notNull (aTopTocService, "TopTocService");

    // All absolute group IDs
    final ICommonsOrderedSet <String> aGroupIDs = new CommonsLinkedHashSet <> ();
    for (final String sTopLevelGroupID : aTopTocService.getAllTopLevelGroupNames ())
    {
      aGroupIDs.add (sTopLevelGroupID);
      aGroupIDs.addAll (aTopTocService.getAllAbsoluteSubGroupNamesRecursive (sTopLevelGroupID));
    }

    final ICommonsList <DVRCoordinate> ret = new CommonsArrayList <> ();
    for (final String sGroupID : aGroupIDs)
      for (final String sArtifactID : aTopTocService.getAllArtefacts (sGroupID))
      {
        final RepoToc aToc = aRepo.readTocModel (sGroupID, sArtifactID);
        if (aToc != null)
          for (final DVRVersion aVersion : aToc.getAllVersionsOnly ())
          {
            final DVRCoordinate aCoord = new DVRCoordinate (sGroupID, sArtifactID, aVersion);
            if (aRepo.exists (RepoStorageKeyOfArtefact.of (aCoord, VESLoader.FILE_EXT_VES)))
              ret.add (aCoord);
          }
      }
    return ret;
  }
}
