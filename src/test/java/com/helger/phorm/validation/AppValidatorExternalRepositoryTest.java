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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

import com.helger.base.io.stream.StreamHelper;
import com.helger.diagnostics.error.list.ErrorList;
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
import com.helger.io.resource.ClassPathResource;
import com.helger.phive.api.executorset.IValidationExecutorSet;
import com.helger.phive.api.result.ValidationResultList;
import com.helger.phive.xml.source.IValidationSourceXML;
import com.helger.phive.xml.source.ValidationSourceXML;
import com.helger.phorm.CApp;
import com.helger.xml.serialize.read.DOMReader;

/**
 * Test class for the external repository support of class {@link AppValidator}.
 *
 * @author Philip Helger
 */
public final class AppValidatorExternalRepositoryTest
{
  private static final File BASE_DIR = new File ("target/test-external-repo");
  private static final String GROUP_ID = "com.helger.phorm.test";
  private static final DVRCoordinate VESID_XSD = DVRCoordinate.parseOrNull (GROUP_ID + ":test_xsd:1.0");
  private static final DVRCoordinate VESID_SCH = DVRCoordinate.parseOrNull (GROUP_ID + ":test_sch:1.0");

  @NonNull
  private static String _getVESXSD (@NonNull final DVRCoordinate aVESID)
  {
    return "<ves xmlns=\"urn:com:helger:phive:ves:v1.0\">" +
           "<groupId>" +
           aVESID.getGroupID () +
           "</groupId>" +
           "<artifactId>" +
           aVESID.getArtifactID () +
           "</artifactId>" +
           "<version>" +
           aVESID.getVersionString () +
           "</version>" +
           "<name>phorm XSD test</name>" +
           "<xsd><resource><groupId>" +
           GROUP_ID +
           "</groupId><artifactId>mini</artifactId><version>1.0</version><type>xsd</type></resource></xsd>" +
           "</ves>";
  }

  @NonNull
  private static String _getVESSchematron ()
  {
    return "<ves xmlns=\"urn:com:helger:phive:ves:v1.0\">" +
           "<groupId>" +
           GROUP_ID +
           "</groupId><artifactId>test_sch</artifactId><version>1.0</version>" +
           "<name>phorm Schematron test</name>" +
           "<requires><groupId>" +
           GROUP_ID +
           "</groupId><artifactId>test_xsd</artifactId><version>1.0</version></requires>" +
           "<schematron><resource><groupId>" +
           GROUP_ID +
           "</groupId><artifactId>mini-sch</artifactId><version>1.0</version><type>sch</type></resource>" +
           "<engine>iso-schematron</engine></schematron>" +
           "</ves>";
  }

  private static void _write (@NonNull final RepoStorageLocalFileSystem aRepo,
                              @NonNull final DVRCoordinate aCoord,
                              @NonNull final String sFileExt,
                              @NonNull final byte [] aData)
  {
    assertTrue (aRepo.write (RepoStorageKeyOfArtefact.of (aCoord, sFileExt), RepoStorageContentByteArray.of (aData))
                     .isSuccess ());
  }

  @NonNull
  private static byte [] _getResourceBytes (@NonNull final String sFilename)
  {
    final byte [] ret = StreamHelper.getAllBytes (new ClassPathResource ("external-repo-src/" + sFilename));
    assertNotNull (sFilename, ret);
    return ret;
  }

  /**
   * Create a repository containing the XSD resource and the provided VES.
   */
  private static void _createRepo (@NonNull final String sDirName, @NonNull final String... aVESs)
  {
    final RepoStorageLocalFileSystem aRepo = new RepoStorageLocalFileSystem (new File (BASE_DIR, sDirName),
                                                                             sDirName,
                                                                             ERepoWritable.WITH_WRITE,
                                                                             ERepoDeletable.WITHOUT_DELETE);
    aRepo.setAuditor (new RepoTopTocAuditor (new RepoTopTocServiceRepoBasedXML ()));

    _write (aRepo, DVRCoordinate.parseOrNull (GROUP_ID + ":mini:1.0"), ".xsd", _getResourceBytes ("mini.xsd"));
    _write (aRepo, DVRCoordinate.parseOrNull (GROUP_ID + ":mini-sch:1.0"), ".sch", _getResourceBytes ("mini.sch"));
    for (final String sVES : aVESs)
    {
      // Determine the VESID from the VES itself
      final Document aDoc = DOMReader.readXMLDOM (sVES);
      assertNotNull (aDoc);
      final String sGroupID = aDoc.getElementsByTagName ("groupId").item (0).getTextContent ();
      final String sArtifactID = aDoc.getElementsByTagName ("artifactId").item (0).getTextContent ();
      final String sVersion = aDoc.getElementsByTagName ("version").item (0).getTextContent ();
      _write (aRepo,
              DVRCoordinate.parseOrNull (sGroupID + ":" + sArtifactID + ":" + sVersion),
              ".ves",
              sVES.getBytes (StandardCharsets.UTF_8));
    }
  }

  @NonNull
  private static File _createConfig (@NonNull final String sFilename, @NonNull final String sRepositories)
  {
    final File ret = new File (BASE_DIR, sFilename);
    SimpleFileIO.writeFile (ret,
                            "<repositories xmlns=\"urn:fdc:com:helger:diver:repoconfig:v1.0\">" +
                                 sRepositories +
                                 "</repositories>",
                            StandardCharsets.UTF_8);
    return ret;
  }

  @NonNull
  private static String _repo (@NonNull final String sID, @NonNull final String sTopToc)
  {
    return "<repository id=\"" +
           sID +
           "\" type=\"localfs\" toptoc=\"" +
           sTopToc +
           "\"><property name=\"basedir\">" +
           sID +
           "</property></repository>";
  }

  @NonNull
  private static ValidationResultList _validate (@NonNull final DVRCoordinate aVESID, @NonNull final String sFilename)
  {
    final IValidationExecutorSet <IValidationSourceXML> aVES = AppValidator.getVESOrNull (aVESID);
    assertNotNull (aVES);
    final Document aDoc = DOMReader.readXMLDOM (new ClassPathResource ("external-repo-src/" + sFilename));
    assertNotNull (aDoc);
    return AppValidator.validate (aVES, ValidationSourceXML.create (null, aDoc), CApp.DEFAULT_LOCALE, "test");
  }

  @BeforeClass
  public static void beforeClass ()
  {
    FileOperationManager.INSTANCE.deleteDirRecursiveIfExisting (BASE_DIR);

    // Built-in VESID used in a repository
    final DVRCoordinate aBuiltinVESID = AppValidator.getAllVES ().getFirstOrNull ().getID ();

    _createRepo ("repo-a", _getVESXSD (VESID_XSD), _getVESSchematron ());
    _createRepo ("repo-b", _getVESXSD (VESID_XSD));
    _createRepo ("repo-builtin", _getVESXSD (aBuiltinVESID));
  }

  @After
  public void after ()
  {
    // Remove all external VES again
    final ErrorList aErrorList = new ErrorList ();
    assertTrue (AppValidator.loadExternalRepositories (null, aErrorList).isSuccess ());
    assertTrue (AppValidator.getAllExternalVESIDs ().isEmpty ());
    assertTrue (AppValidator.getAllExternalRepositoryIDs ().isEmpty ());
  }

  @Test
  public void testLoadAndValidate ()
  {
    final int nBuiltinCount = AppValidator.getAllVES ().size ();
    final ErrorList aErrorList = new ErrorList ();
    assertTrue (aErrorList.toString (),
                AppValidator.loadExternalRepositories (_createConfig ("valid.xml", _repo ("repo-a", "diver-xml")),
                                                       aErrorList)
                            .isSuccess ());
    assertFalse (aErrorList.toString (), aErrorList.containsAtLeastOneError ());

    assertEquals (nBuiltinCount + 2, AppValidator.getAllVES ().size ());
    assertTrue (AppValidator.containsVES (VESID_XSD));
    assertTrue (AppValidator.containsVES (VESID_SCH));
    assertEquals ("repo-a", AppValidator.getExternalRepositoryIDOfVES (VESID_XSD));
    assertEquals ("repo-a", AppValidator.getExternalRepositoryIDOfVES (VESID_SCH));
    assertNull (AppValidator.getExternalRepositoryIDOfVES (AppValidator.getAllVES ().getFirstOrNull ().getID ()));
    assertEquals ("[repo-a]", AppValidator.getAllExternalRepositoryIDs ().toString ());
    assertNotNull (AppValidator.getExternalRepositoriesLoadDateTime ());

    // XSD only
    assertTrue (_validate (VESID_XSD, "valid.xml").getOverallValidity ().isValid ());
    assertTrue (_validate (VESID_XSD, "invalid-sch.xml").getOverallValidity ().isValid ());

    // XSD from the required VES plus Schematron
    ValidationResultList aVRL = _validate (VESID_SCH, "valid.xml");
    assertEquals (2, aVRL.size ());
    assertTrue (aVRL.getOverallValidity ().isValid ());

    aVRL = _validate (VESID_SCH, "invalid-sch.xml");
    assertEquals (2, aVRL.size ());
    assertTrue (aVRL.getOverallValidity ().isInvalid ());
    assertEquals (1, aVRL.getAllErrors ().size ());
    assertEquals ("MINI-1", aVRL.getAllErrors ().getFirstOrNull ().getErrorID ());
  }

  @Test
  public void testDuplicateBetweenRepositories ()
  {
    final int nBuiltinCount = AppValidator.getAllVES ().size ();
    final ErrorList aErrorList = new ErrorList ();
    assertTrue (AppValidator.loadExternalRepositories (_createConfig ("duplicate.xml",
                                                                      _repo ("repo-a", "diver-xml") +
                                                                                       _repo ("repo-b", "diver-xml")),
                                                       aErrorList)
                            .isFailure ());
    assertEquals (aErrorList.toString (), 1, aErrorList.getAllErrors ().size ());
    assertTrue (aErrorList.getAllErrors ()
                          .getFirstOrNull ()
                          .getErrorText (CApp.DEFAULT_LOCALE)
                          .contains ("is contained in repository 'repo-a' and in repository 'repo-b'"));
    assertEquals (nBuiltinCount, AppValidator.getAllVES ().size ());
    assertFalse (AppValidator.containsVES (VESID_XSD));
  }

  @Test
  public void testDuplicateBuiltin ()
  {
    final int nBuiltinCount = AppValidator.getAllVES ().size ();
    final ErrorList aErrorList = new ErrorList ();
    assertTrue (AppValidator.loadExternalRepositories (_createConfig ("builtin.xml",
                                                                      _repo ("repo-builtin", "diver-xml")),
                                                       aErrorList)
                            .isFailure ());
    assertEquals (aErrorList.toString (), 1, aErrorList.getAllErrors ().size ());
    assertTrue (aErrorList.getAllErrors ().getFirstOrNull ().getErrorText (CApp.DEFAULT_LOCALE).contains ("built-in"));
    assertEquals (nBuiltinCount, AppValidator.getAllVES ().size ());
    assertTrue (AppValidator.getAllExternalVESIDs ().isEmpty ());
  }

  @Test
  public void testMissingTopToc ()
  {
    final ErrorList aErrorList = new ErrorList ();
    assertTrue (AppValidator.loadExternalRepositories (_createConfig ("no-toptoc.xml", _repo ("repo-a", "none")),
                                                       aErrorList)
                            .isFailure ());
    assertEquals (aErrorList.toString (), 1, aErrorList.getAllErrors ().size ());
    assertTrue (aErrorList.getAllErrors ()
                          .getFirstOrNull ()
                          .getErrorText (CApp.DEFAULT_LOCALE)
                          .contains ("must use the top-level ToC type 'diver-xml'"));
    assertFalse (AppValidator.containsVES (VESID_XSD));
  }

  @Test
  public void testReloadKeepsStateOnFailure ()
  {
    ErrorList aErrorList = new ErrorList ();
    assertTrue (AppValidator.loadExternalRepositories (_createConfig ("valid.xml", _repo ("repo-a", "diver-xml")),
                                                       aErrorList)
                            .isSuccess ());
    assertTrue (AppValidator.containsVES (VESID_SCH));

    // Reload fails
    aErrorList = new ErrorList ();
    assertTrue (AppValidator.loadExternalRepositories (new File (BASE_DIR, "does-not-exist.xml"), aErrorList)
                            .isFailure ());
    assertTrue (aErrorList.containsAtLeastOneError ());

    // Previous state is kept
    assertTrue (AppValidator.containsVES (VESID_XSD));
    assertTrue (AppValidator.containsVES (VESID_SCH));
    assertEquals ("[repo-a]", AppValidator.getAllExternalRepositoryIDs ().toString ());

    // Reload of the same state works, because the existing VESIDs are external ones
    aErrorList = new ErrorList ();
    assertTrue (aErrorList.toString (),
                AppValidator.loadExternalRepositories (new File (BASE_DIR, "valid.xml"), aErrorList).isSuccess ());
    assertTrue (AppValidator.containsVES (VESID_SCH));
  }
}
