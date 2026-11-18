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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsSet;
import com.helger.diagnostics.error.IError;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.diver.api.coord.DVRCoordinate;
import com.helger.phive.api.executorset.IValidationExecutorSet;
import com.helger.phive.api.result.ValidationResultList;
import com.helger.phive.xml.source.IValidationSourceXML;
import com.helger.phive.xml.source.ValidationSourceXML;
import com.helger.phorm.CApp;
import com.helger.xml.serialize.read.DOMReader;

/**
 * Test the example repository in <code>example-repository</code>.
 *
 * @author Philip Helger
 */
public final class ExampleRepositoryTest
{
  private static final File BASE_DIR = new File ("example-repository");
  private static final DVRCoordinate VESID_ORDER = DVRCoordinate.parseOrNull ("com.example:order:1.0.0");
  private static final DVRCoordinate VESID_ORDER_RULES = DVRCoordinate.parseOrNull ("com.example:order-rules:1.0.0");

  @NonNull
  private static ValidationResultList _validate (@NonNull final DVRCoordinate aVESID, @NonNull final String sFilename)
  {
    final IValidationExecutorSet <IValidationSourceXML> aVES = AppValidator.getVESOrNull (aVESID);
    assertNotNull (aVES);
    final Document aDoc = DOMReader.readXMLDOM (new File (BASE_DIR, "testfiles/" + sFilename));
    assertNotNull (aDoc);
    return AppValidator.validate (aVES, ValidationSourceXML.create (null, aDoc), CApp.DEFAULT_LOCALE, "test");
  }

  @NonNull
  private static ICommonsSet <String> _getAllErrorIDs (@NonNull final ValidationResultList aVRL)
  {
    final ICommonsSet <String> ret = new CommonsHashSet <> ();
    for (final IError aError : aVRL.getAllFailures ())
      ret.add (aError.getErrorID ());
    return ret;
  }

  @Before
  public void before ()
  {
    final ErrorList aErrorList = new ErrorList ();
    assertTrue (aErrorList.toString (),
                AppValidator.loadExternalRepositories (new File (BASE_DIR, "repositories.xml"), aErrorList)
                            .isSuccess ());
  }

  @After
  public void after ()
  {
    // Remove all external VES again
    assertTrue (AppValidator.loadExternalRepositories (null, new ErrorList ()).isSuccess ());
  }

  @Test
  public void testLoaded ()
  {
    assertEquals ("example-rules", AppValidator.getExternalRepositoryIDOfVES (VESID_ORDER));
    assertEquals ("example-rules", AppValidator.getExternalRepositoryIDOfVES (VESID_ORDER_RULES));
    assertEquals (2, AppValidator.getAllExternalVESIDs ().size ());
  }

  @Test
  public void testValid ()
  {
    for (final DVRCoordinate aVESID : new DVRCoordinate [] { VESID_ORDER, VESID_ORDER_RULES })
    {
      final ValidationResultList aVRL = _validate (aVESID, "order-valid.xml");
      assertTrue (aVRL.toString (), aVRL.getOverallValidity ().isValid ());
      assertTrue (aVRL.toString (), aVRL.getAllFailures ().isEmpty ());
    }
  }

  @Test
  public void testInvalidXsd ()
  {
    final ValidationResultList aVRL = _validate (VESID_ORDER_RULES, "order-invalid-xsd.xml");
    assertTrue (aVRL.toString (), aVRL.getOverallValidity ().isInvalid ());
    // The Schematron layer is skipped after the XSD failed
    assertEquals (aVRL.toString (), 2, aVRL.size ());
    assertTrue (aVRL.toString (), aVRL.get (1).isSkipped ());
  }

  @Test
  public void testInvalidRules ()
  {
    // XML Schema only: fine
    assertTrue (_validate (VESID_ORDER, "order-invalid-rules.xml").getOverallValidity ().isValid ());

    final ValidationResultList aVRL = _validate (VESID_ORDER_RULES, "order-invalid-rules.xml");
    assertTrue (aVRL.toString (), aVRL.getOverallValidity ().isInvalid ());
    final ICommonsSet <String> aErrorIDs = _getAllErrorIDs (aVRL);
    for (final String sErrorID : new String [] { "ORD-01", "ORD-02", "ORD-03", "ORD-04" })
      assertTrue (aErrorIDs.toString (), aErrorIDs.contains (sErrorID));

    // ORD-02 is only a warning
    for (final IError aError : aVRL.getAllFailures ())
      assertEquals (aError.toString (), !aError.getErrorID ().equals ("ORD-02"), aError.isError ());
  }
}
