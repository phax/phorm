<?xml version="1.0" encoding="UTF-8"?>
<!--

    Copyright (C) 2022-2026 Philip Helger (www.helger.com)
    philip[at]helger[dot]com

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

            http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

-->
<schema xmlns="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2">
  <ns prefix="o" uri="urn:example:order:1.0" />
  <ns prefix="xs" uri="http://www.w3.org/2001/XMLSchema" />
  <pattern>
    <rule context="/o:order/o:line">
      <assert id="ORD-01" flag="fatal" test="xs:decimal(o:amount) = xs:decimal(o:quantity) * xs:decimal(o:price)">The line amount must be quantity multiplied by price</assert>
      <assert id="ORD-02" flag="warning" test="xs:decimal(o:quantity) &gt; 0">The line quantity should be positive</assert>
    </rule>
    <rule context="/o:order">
      <assert id="ORD-03" flag="fatal" test="xs:decimal(o:total) = sum(o:line/xs:decimal(o:amount))">The order total must be the sum of all line amounts</assert>
      <assert id="ORD-04" flag="fatal" test="count(o:line/o:id) = count(distinct-values(o:line/o:id))">The line IDs must be unique</assert>
    </rule>
  </pattern>
</schema>
