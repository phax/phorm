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
package com.helger.phorm.ddd;

import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.numeric.mutable.MutableBoolean;
import com.helger.ddd.DDDVersion;
import com.helger.ddd.DocumentDetails;
import com.helger.ddd.DocumentDetailsDeterminator;
import com.helger.ddd.IDDDDocumentUnwrappingCallback;
import com.helger.ddd.model.DDDSyntaxList;
import com.helger.ddd.model.DDDValueProviderList;
import com.helger.phorm.telemetry.CPhormTelemetry;
import com.helger.phorm.telemetry.PhormMetrics;
import com.helger.telemetry.ETelemetrySpanKind;
import com.helger.telemetry.Telemetry;
import com.helger.telemetry.TelemetryAttributes;

/**
 * The utility class to configure and access DDD
 *
 * @author Philip Helger
 */
@Immutable
public final class PhormDDD
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PhormDDD.class);

  static
  {
    LOGGER.info ("Using DDD " + DDDVersion.getVersionNumber () + " with build date " + DDDVersion.getBuildTimestamp ());
  }

  // Use default configuration
  private static final DocumentDetailsDeterminator DDD;
  static
  {
    DDD = new DocumentDetailsDeterminator (DDDSyntaxList.getDefaultSyntaxList (),
                                           DDDValueProviderList.getDefaultValueProviderList ()).addDefaultUnwrappers ();
  }

  private PhormDDD ()
  {}

  @Nullable
  public static DocumentDetails findDocumentDetails (@NonNull final Element aRootElement,
                                                     @Nullable final IDDDDocumentUnwrappingCallback aUnwrappingCallback,
                                                     @Nullable final Consumer <Element> aEffectiveElementConsumer)
  {
    return Telemetry.withSpan (CPhormTelemetry.SPAN_DDD_DETERMINE, ETelemetrySpanKind.INTERNAL, aSpan -> {
      aSpan.setAttribute (CPhormTelemetry.ATTR_XML_ROOT_LOCALNAME, aRootElement.getLocalName ())
           .setAttribute (CPhormTelemetry.ATTR_XML_ROOT_NAMESPACE, aRootElement.getNamespaceURI ());

      final MutableBoolean aWasUnwrapped = new MutableBoolean (false);
      final IDDDDocumentUnwrappingCallback aEffectiveUnwrappingCallback = (aUnwrapper,
                                                                           aOuterElement,
                                                                           aInnerElement) -> {
        // Remember that it was unwrapped
        aWasUnwrapped.set (true);

        // Call parameter callback (if any)
        if (aUnwrappingCallback != null)
          aUnwrappingCallback.onUnwrap (aUnwrapper, aOuterElement, aInnerElement);
      };
      final DocumentDetails aDD = DDD.findDocumentDetails (aRootElement,
                                                           aEffectiveUnwrappingCallback,
                                                           aEffectiveElementConsumer);

      final boolean bMatched = aDD != null;
      final String sSyntax = aDD != null && aDD.hasSyntaxID () ? aDD.getSyntaxID () : null;
      aSpan.setAttribute (CPhormTelemetry.ATTR_DDD_MATCHED, bMatched);
      if (aDD != null)
      {
        aSpan.setAttribute (CPhormTelemetry.ATTR_DDD_SYNTAX, sSyntax)
             .setAttribute (CPhormTelemetry.ATTR_DDD_PROCESS_ID,
                            aDD.hasProcessID () ? aDD.getProcessID ().getURIEncoded () : null)
             .setAttribute (CPhormTelemetry.ATTR_DDD_CUSTOMIZATION_ID,
                            aDD.hasCustomizationID () ? aDD.getCustomizationID () : null)
             .setAttribute (CPhormTelemetry.ATTR_DDD_VESID, aDD.hasVESID () ? aDD.getVESID () : null)
             .setAttribute (CPhormTelemetry.ATTR_DDD_UNWRAPPED, aWasUnwrapped.booleanValue ());
      }

      PhormMetrics.DDD_DETERMINATIONS.add (1,
                                           TelemetryAttributes.builder ()
                                                              .put ("matched", bMatched)
                                                              .put ("syntax", sSyntax != null ? sSyntax : "none")
                                                              .build ());
      return aDD;
    });
  }
}
