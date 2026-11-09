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
package com.helger.phorm.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.base.io.stream.StreamHelper;
import com.helger.io.resource.ClassPathResource;
import com.helger.kaltblut.core.model.EZugferdCountry;
import com.helger.phive.api.result.ValidationResult;
import com.helger.phive.api.result.ValidationResultList;
import com.helger.phorm.CApp;
import com.helger.phorm.validation.HybridFindingConverter;

/**
 * Test class for class {@link ApiPostHybridValidate}.
 *
 * @author Philip Helger
 */
public final class ApiPostHybridValidateTest
{
  @NonNull
  private static ValidationResultList _validate (@NonNull final String sFilename)
  {
    final byte [] aPdf = StreamHelper.getAllBytes (new ClassPathResource ("testfiles/hybrid/" + sFilename));
    assertNotNull (aPdf);

    return ApiPostHybridValidate.validateHybrid (aPdf, EZugferdCountry.DE, CApp.DEFAULT_LOCALE, "[test] ", x -> {}, x -> {});
  }

  @NonNull
  private static ValidationResultList _validateIssue22 (@NonNull final String sFilename)
  {
    final ValidationResultList aVRL = _validate (sFilename);
    assertFalse (aVRL.toString (), aVRL.getOverallValidity ().isValid ());

    // The 2 PDF carrier layers are fine, but the embedded XML layer is not
    assertEquals (3, aVRL.size ());
    assertTrue (aVRL.get (0).getValidity ().isValid ());
    assertTrue (aVRL.get (1).getValidity ().isValid ());

    // The embedded XML has the wrong BT-24 and could therefore not be validated
    final ValidationResult aEmbedded = aVRL.get (2);
    assertSame (HybridFindingConverter.ARTEFACT_EMBEDDED_XML, aEmbedded.getValidationArtefact ());
    assertTrue (aEmbedded.getValidity ().isInvalid ());
    assertEquals (2, aEmbedded.getErrorList ().size ());
    assertEquals (HybridFindingConverter.ERROR_ID_SPECIFICATION_ID, aEmbedded.getErrorList ().get (0).getErrorID ());
    return aVRL;
  }

  /**
   * Issue #22: the embedded CII uses the Peppol process ID together with the Factur-X EN 16931
   * guideline ID. The PDF carrier is fine, but the embedded XML must not silently skip validation.
   */
  @Test
  public void testIssue22 ()
  {
    _validateIssue22 ("issue22-peppol-process-factur-x-guideline.pdf");
  }

  /**
   * Issue #22: a second sample with the same combination of IDs, as a ZUGFeRD PDF.
   */
  @Test
  public void testIssue22ZugferdSample ()
  {
    _validateIssue22 ("issue22-zugferd-sample.pdf");
  }

  /**
   * A Factur-X EN 16931 invoice with the correct BT-24 must not get an embedded XML layer.
   */
  @Test
  public void testValidFacturX ()
  {
    final ValidationResultList aVRL = _validate ("factur-x-en16931-valid.pdf");
    for (final ValidationResult aVR : aVRL)
      assertNotSame (HybridFindingConverter.ARTEFACT_EMBEDDED_XML, aVR.getValidationArtefact ());

    // 2 PDF carrier layers and the XML layers
    assertTrue (aVRL.toString (), aVRL.size () > 2);
  }
}
