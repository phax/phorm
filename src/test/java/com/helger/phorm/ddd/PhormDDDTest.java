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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.Test;
import org.w3c.dom.Document;

import com.helger.ddd.DocumentDetails;
import com.helger.xml.serialize.read.DOMReader;

/**
 * Test class for class {@link PhormDDD}.
 *
 * @author Philip Helger
 */
public final class PhormDDDTest
{
  @Test
  public void testDetermineDocumentDetailsFromSBDHWrappedInvoice ()
  {
    final String sXML = """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <StandardBusinessDocument xmlns="http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader">
                          <StandardBusinessDocumentHeader>
                            <HeaderVersion>1.0</HeaderVersion>
                          </StandardBusinessDocumentHeader>
                          <Invoice xmlns="urn:oasis:names:specification:ubl:schema:xsd:Invoice-2"
                                   xmlns:cac="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2"
                                   xmlns:cbc="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2">
                            <cbc:CustomizationID>urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0</cbc:CustomizationID>
                            <cbc:ProfileID>urn:fdc:peppol.eu:2017:poacc:billing:01:1.0</cbc:ProfileID>
                            <cbc:ID>test-1</cbc:ID>
                            <cac:AccountingSupplierParty>
                              <cac:Party>
                                <cbc:EndpointID schemeID="0088">9482348239847239874</cbc:EndpointID>
                              </cac:Party>
                            </cac:AccountingSupplierParty>
                            <cac:AccountingCustomerParty>
                              <cac:Party>
                                <cbc:EndpointID schemeID="0002">FR23342</cbc:EndpointID>
                              </cac:Party>
                            </cac:AccountingCustomerParty>
                          </Invoice>
                        </StandardBusinessDocument>
                        """;
    final Document aDoc = DOMReader.readXMLDOM (sXML.getBytes (StandardCharsets.UTF_8));
    assertNotNull (aDoc);
    assertNotNull (aDoc.getDocumentElement ());

    final DocumentDetails aDD = PhormDDD.findDocumentDetails (aDoc.getDocumentElement ());
    assertNotNull (aDD);
    assertTrue (aDD.hasVESID ());
    assertTrue (aDD.getVESID ().startsWith ("eu.peppol.bis3:invoice:"));

    final Document aBusinessDoc = PhormDDD.getBusinessDocument (aDoc);
    assertNotNull (aBusinessDoc.getDocumentElement ());
    assertTrue ("Invoice".equals (aBusinessDoc.getDocumentElement ().getLocalName ()));
  }
}
