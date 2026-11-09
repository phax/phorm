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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.array.ArrayHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.kaltblut.core.model.EZugferdProfile;

/**
 * The BT-24 specification identifiers that the embedded XML of a ZUGFeRD 2.x / Factur-X hybrid
 * invoice may use, per profile declared in <code>ConformanceLevel</code> of the XMP metadata. Both
 * the primary Factur-X (<code>urn:factur-x.eu:1p0</code>) and the legacy ZUGFeRD 2.0
 * (<code>urn:zugferd.de:2p0</code>) identifiers are allowed. The profiles COMFORT (ZUGFeRD 1.0)
 * and XRECHNUNG are not covered.
 *
 * @author Philip Helger
 */
public enum EHybridSpecificationID
{
  MINIMUM (EZugferdProfile.MINIMUM, "urn:factur-x.eu:1p0:minimum", "urn:zugferd.de:2p0:minimum"),
  BASIC_WL (EZugferdProfile.BASIC_WL, "urn:factur-x.eu:1p0:basicwl", "urn:zugferd.de:2p0:basicwl"),
  BASIC (EZugferdProfile.BASIC,
         "urn:cen.eu:en16931:2017#compliant#urn:factur-x.eu:1p0:basic",
         "urn:cen.eu:en16931:2017#compliant#urn:zugferd.de:2p0:basic"),
  EN_16931 (EZugferdProfile.EN_16931, "urn:cen.eu:en16931:2017"),
  EXTENDED (EZugferdProfile.EXTENDED,
            "urn:cen.eu:en16931:2017#conformant#urn:factur-x.eu:1p0:extended",
            "urn:cen.eu:en16931:2017#conformant#urn:zugferd.de:2p0:extended");

  private final EZugferdProfile m_eProfile;
  private final String [] m_aSpecificationIDs;

  EHybridSpecificationID (@NonNull final EZugferdProfile eProfile,
                          @NonNull @Nonempty final String... aSpecificationIDs)
  {
    m_eProfile = eProfile;
    m_aSpecificationIDs = aSpecificationIDs;
  }

  /**
   * @return The kaltblut profile from the XMP metadata. Never <code>null</code>.
   */
  @NonNull
  public EZugferdProfile getProfile ()
  {
    return m_eProfile;
  }

  /**
   * @return All BT-24 specification identifiers allowed for this profile. Never <code>null</code>
   *         nor empty.
   */
  @NonNull
  @Nonempty
  @ReturnsMutableCopy
  public ICommonsList <String> getAllSpecificationIDs ()
  {
    return new CommonsArrayList <> (m_aSpecificationIDs);
  }

  /**
   * Check if the provided BT-24 specification identifier is allowed for this profile.
   *
   * @param sSpecificationID
   *        The BT-24 value of the embedded XML. May be <code>null</code>.
   * @return <code>true</code> if it is allowed, <code>false</code> if not.
   */
  public boolean isAllowedSpecificationID (@Nullable final String sSpecificationID)
  {
    return ArrayHelper.contains (m_aSpecificationIDs, sSpecificationID);
  }

  @Nullable
  public static EHybridSpecificationID getFromProfileOrNull (@Nullable final EZugferdProfile eProfile)
  {
    if (eProfile != null)
      for (final EHybridSpecificationID e : values ())
        if (e.m_eProfile == eProfile)
          return e;
    return null;
  }
}
