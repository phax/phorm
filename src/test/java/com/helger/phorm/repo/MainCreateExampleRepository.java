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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.helger.diver.api.coord.DVRCoordinate;
import com.helger.diver.repo.ERepoDeletable;
import com.helger.diver.repo.ERepoWritable;
import com.helger.diver.repo.RepoStorageContentByteArray;
import com.helger.diver.repo.RepoStorageKeyOfArtefact;
import com.helger.diver.repo.impl.RepoStorageLocalFileSystem;
import com.helger.diver.repo.toc.RepoTopTocAuditor;
import com.helger.diver.repo.toc.RepoTopTocServiceRepoBasedXML;
import com.helger.io.file.FileOperationManager;
import com.helger.io.file.SimpleFileIO;
import com.helger.phive.ves.engine.load.VESLoader;
import com.helger.xml.serialize.read.DOMReader;

/**
 * Create the ph-diver repository <code>example-repository/example-rules</code> from the files in
 * <code>example-repository/source</code>. Run it from the project root folder after changing a
 * source file.
 *
 * @author Philip Helger
 */
public final class MainCreateExampleRepository
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainCreateExampleRepository.class);

  private static final File BASE_DIR = new File ("example-repository");
  private static final File SOURCE_DIR = new File (BASE_DIR, "source");
  private static final String REPO_ID = "example-rules";

  private static void _write (@NonNull final RepoStorageLocalFileSystem aRepo,
                              @NonNull final DVRCoordinate aCoord,
                              @NonNull final String sFileExt,
                              @NonNull final String sSourceFilename)
  {
    final byte [] aData = SimpleFileIO.getAllFileBytes (new File (SOURCE_DIR, sSourceFilename));
    if (aData == null)
      throw new IllegalStateException ("Failed to read source file '" + sSourceFilename + "'");

    if (aRepo.write (RepoStorageKeyOfArtefact.of (aCoord, sFileExt), RepoStorageContentByteArray.of (aData))
             .isFailure ())
      throw new IllegalStateException ("Failed to write " + aCoord.getAsSingleID () + sFileExt);
    LOGGER.info ("Wrote " + aCoord.getAsSingleID () + sFileExt + " from '" + sSourceFilename + "'");
  }

  private static void _writeVES (@NonNull final RepoStorageLocalFileSystem aRepo,
                                 @NonNull final String sSourceFilename)
  {
    // The VESID is taken from the VES itself
    final Document aDoc = DOMReader.readXMLDOM (new File (SOURCE_DIR, sSourceFilename));
    if (aDoc == null)
      throw new IllegalStateException ("Failed to parse VES '" + sSourceFilename + "'");
    final DVRCoordinate aVESID = DVRCoordinate.parseOrNull (aDoc.getElementsByTagName ("groupId")
                                                                .item (0)
                                                                .getTextContent () +
                                                            ":" +
                                                            aDoc.getElementsByTagName ("artifactId")
                                                                .item (0)
                                                                .getTextContent () +
                                                            ":" +
                                                            aDoc.getElementsByTagName ("version")
                                                                .item (0)
                                                                .getTextContent ());
    _write (aRepo, aVESID, VESLoader.FILE_EXT_VES, sSourceFilename);
  }

  public static void main (final String [] args)
  {
    final File aRepoDir = new File (BASE_DIR, REPO_ID);
    FileOperationManager.INSTANCE.deleteDirRecursiveIfExisting (aRepoDir);

    final RepoStorageLocalFileSystem aRepo = new RepoStorageLocalFileSystem (aRepoDir,
                                                                             REPO_ID,
                                                                             ERepoWritable.WITH_WRITE,
                                                                             ERepoDeletable.WITHOUT_DELETE);
    // Maintains the top-level ToC, which Phorm needs to find the VES
    aRepo.setAuditor (new RepoTopTocAuditor (new RepoTopTocServiceRepoBasedXML ()));

    // The resources referenced from the VES
    _write (aRepo, DVRCoordinate.parseOrNull ("com.example:order-xsd:1.0.0"), ".xsd", "order.xsd");
    _write (aRepo, DVRCoordinate.parseOrNull ("com.example:order-sch:1.0.0"), ".sch", "order.sch");

    // The VES themselves
    _writeVES (aRepo, "order.ves");
    _writeVES (aRepo, "order-rules.ves");

    LOGGER.info ("Created repository in '" + aRepoDir.getAbsolutePath () + "'");
  }
}
