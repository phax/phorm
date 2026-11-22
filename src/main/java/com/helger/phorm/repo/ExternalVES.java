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

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.phive.api.executorset.IValidationExecutorSet;
import com.helger.phive.xml.source.IValidationSourceXML;

/**
 * A validation executor set that was loaded from an external repository.
 *
 * @author Philip Helger
 * @since 2.3.0
 */
@Immutable
public final class ExternalVES
{
  private final String m_sRepositoryID;
  private final IValidationExecutorSet <IValidationSourceXML> m_aVES;

  public ExternalVES (@NonNull @Nonempty final String sRepositoryID,
                      @NonNull final IValidationExecutorSet <IValidationSourceXML> aVES)
  {
    ValueEnforcer.notEmpty (sRepositoryID, "RepositoryID");
    ValueEnforcer.notNull (aVES, "VES");
    m_sRepositoryID = sRepositoryID;
    m_aVES = aVES;
  }

  /**
   * @return The ID of the repository the VES was loaded from. Neither <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  public String getRepositoryID ()
  {
    return m_sRepositoryID;
  }

  /**
   * @return The loaded validation executor set. Never <code>null</code>.
   */
  @NonNull
  public IValidationExecutorSet <IValidationSourceXML> getVES ()
  {
    return m_aVES;
  }
}
