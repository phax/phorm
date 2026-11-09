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
import com.helger.base.lang.EnumHelper;
import com.helger.phive.api.EValidationBaseType;
import com.helger.phive.api.IValidationType;

/**
 * Phorm-specific {@link IValidationType} constants for non-phive validation layers (e.g. the hybrid
 * PDF carrier validation produced by kaltblut).
 *
 * @author Philip Helger
 */
public enum EPhormValidationType implements IValidationType
{
  /**
   * The PDF carrier (BR-HYBRID-* and PDF/A-3) layer of a ZUGFeRD / Factur-X hybrid invoice, as
   * produced by kaltblut.
   */
  PDF_HYBRID_CARRIER ("pdf-hybrid-carrier", EValidationBaseType.PDF, "Hybrid PDF Carrier"),
  /**
   * The embedded invoice XML of a ZUGFeRD / Factur-X hybrid invoice, used to report that it could
   * not be extracted, parsed or assigned to a validation executor set.
   */
  HYBRID_EMBEDDED_XML ("hybrid-embedded-xml", EValidationBaseType.XML, "Hybrid embedded XML");

  private final String m_sID;
  private final EValidationBaseType m_eBaseType;
  private final String m_sName;

  EPhormValidationType (@NonNull final String sID,
                        @NonNull final EValidationBaseType eBaseType,
                        @NonNull final String sName)
  {
    m_sID = sID;
    m_eBaseType = eBaseType;
    m_sName = sName;
  }

  @NonNull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @NonNull
  public EValidationBaseType getBaseType ()
  {
    return m_eBaseType;
  }

  @NonNull
  @Nonempty
  public String getName ()
  {
    return m_sName;
  }

  public boolean isStopValidationOnError ()
  {
    return false;
  }

  public boolean isContextRequired ()
  {
    return false;
  }

  @Nullable
  public static EPhormValidationType getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (EPhormValidationType.class, sID);
  }
}
