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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.helger.annotation.concurrent.Immutable;
import com.helger.ddd.DDDVersion;
import com.helger.ddd.DocumentDetails;
import com.helger.ddd.DocumentDetailsDeterminator;
import com.helger.ddd.model.DDDSyntaxList;
import com.helger.ddd.model.DDDValueProviderList;
import com.helger.xml.XMLFactory;

/**
 * The utility class to configure and access DDD
 *
 * @author Philip Helger
 */
@Immutable
public final class PhormDDD
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PhormDDD.class);
  private static final String NS_SBDH = "http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader";
  private static final String ELEMENT_STANDARD_BUSINESS_DOCUMENT = "StandardBusinessDocument";
  private static final String ELEMENT_STANDARD_BUSINESS_DOCUMENT_HEADER = "StandardBusinessDocumentHeader";

  static
  {
    LOGGER.info ("Using DDD " + DDDVersion.getVersionNumber () + " with build date " + DDDVersion.getBuildTimestamp ());
  }

  // Use default configuration
  private static final DocumentDetailsDeterminator DDD = new DocumentDetailsDeterminator (DDDSyntaxList.getDefaultSyntaxList (),
                                                                                          DDDValueProviderList.getDefaultValueProviderList ());

  private PhormDDD ()
  {}

  @Nullable
  public static Element findBusinessDocumentElement (@NonNull final Element aRootElement)
  {
    if (NS_SBDH.equals (aRootElement.getNamespaceURI ()) &&
        ELEMENT_STANDARD_BUSINESS_DOCUMENT.equals (aRootElement.getLocalName ()))
    {
      for (Node aChild = aRootElement.getFirstChild (); aChild != null; aChild = aChild.getNextSibling ())
        if (aChild.getNodeType () == Node.ELEMENT_NODE)
        {
          final Element aChildElement = (Element) aChild;
          if (!(NS_SBDH.equals (aChildElement.getNamespaceURI ()) &&
                ELEMENT_STANDARD_BUSINESS_DOCUMENT_HEADER.equals (aChildElement.getLocalName ())))
            return aChildElement;
        }
      return null;
    }
    return aRootElement;
  }

  @NonNull
  public static Document getBusinessDocument (@NonNull final Document aDoc)
  {
    final Element aBusinessElement = findBusinessDocumentElement (aDoc.getDocumentElement ());
    if (aBusinessElement == null || aBusinessElement == aDoc.getDocumentElement ())
      return aDoc;

    final Document aBusinessDoc = XMLFactory.newDocument ();
    aBusinessDoc.appendChild (aBusinessDoc.importNode (aBusinessElement, true));
    return aBusinessDoc;
  }

  @Nullable
  public static DocumentDetails findDocumentDetails (@NonNull final Element aRootElement)
  {
    final Element aBusinessElement = findBusinessDocumentElement (aRootElement);
    if (aBusinessElement == null)
      return null;

    // Static instance can be used
    return DDD.findDocumentDetails (aBusinessElement);
  }
}
