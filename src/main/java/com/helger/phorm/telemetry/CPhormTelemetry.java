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
package com.helger.phorm.telemetry;

import com.helger.annotation.concurrent.Immutable;

/**
 * Constants for phorm telemetry: instrumentation scope, span names, attribute keys.
 *
 * @author Philip Helger
 */
@Immutable
public final class CPhormTelemetry
{
  /** OpenTelemetry instrumentation scope name. */
  public static final String SCOPE_NAME = "com.helger.phorm";

  // === Span names ===
  public static final String SPAN_API_VALIDATE = "phorm.api.validate";
  public static final String SPAN_API_DD_AND_VALIDATE = "phorm.api.dd_and_validate";
  public static final String SPAN_API_DETERMINE_DOCTYPE = "phorm.api.determinedoctype";
  public static final String SPAN_API_HYBRID_VALIDATE = "phorm.api.hybrid_validate";
  public static final String SPAN_API_GET_VESIDS = "phorm.api.get_vesids";
  public static final String SPAN_API_ADMIN_GET_REPOSITORIES = "phorm.api.admin_get_repositories";
  public static final String SPAN_API_ADMIN_RELOAD_REPOSITORIES = "phorm.api.admin_reload_repositories";

  public static final String SPAN_AUTH_CHECK = "phorm.auth.check";
  public static final String SPAN_PAYLOAD_READ = "phorm.payload.read";
  public static final String SPAN_XML_PARSE = "phorm.xml.parse";
  public static final String SPAN_DDD_DETERMINE = "phorm.ddd.determine";
  public static final String SPAN_VESID_RESOLVE = "phorm.vesid.resolve";
  public static final String SPAN_PHIVE_VALIDATE = "phorm.phive.validate";
  public static final String SPAN_KALTBLUT_VALIDATE = "phorm.kaltblut.validate";
  public static final String SPAN_KALTBLUT_EXTRACT_XML = "phorm.kaltblut.extract_xml";
  public static final String SPAN_RESPONSE_SERIALIZE = "phorm.response.serialize";
  public static final String SPAN_REPOSITORIES_LOAD = "phorm.repositories.load";

  // === Attribute keys ===
  public static final String ATTR_HTTP_METHOD = "http.request.method";
  public static final String ATTR_HTTP_ROUTE = "http.route";

  public static final String ATTR_ENDPOINT = "phorm.endpoint";
  public static final String ATTR_APP_VERSION = "phorm.app.version";
  public static final String ATTR_REQUEST_SEQ = "phorm.request.seq";

  public static final String ATTR_AUTH_RESULT = "phorm.auth.result";
  public static final String ATTR_AUTH_TOKEN_LENGTH = "phorm.auth.token.length";

  public static final String ATTR_PAYLOAD_SIZE_BYTES = "phorm.payload.size_bytes";
  public static final String ATTR_PAYLOAD_KIND = "phorm.payload.kind";

  public static final String ATTR_XML_ROOT_LOCALNAME = "xml.root.localname";
  public static final String ATTR_XML_ROOT_NAMESPACE = "xml.root.namespace";

  public static final String ATTR_DDD_MATCHED = "phorm.ddd.matched";
  public static final String ATTR_DDD_SYNTAX = "phorm.ddd.syntax";
  public static final String ATTR_DDD_PROCESS_ID = "phorm.ddd.process_id";
  public static final String ATTR_DDD_CUSTOMIZATION_ID = "phorm.ddd.customization_id";
  public static final String ATTR_DDD_VESID = "phorm.ddd.vesid";
  public static final String ATTR_DDD_UNWRAPPED = "phorm.ddd.unwrapped";

  public static final String ATTR_VESID = "phorm.vesid";
  public static final String ATTR_VESID_NAME = "phorm.vesid.name";
  public static final String ATTR_VESID_RESOLVED = "phorm.vesid.resolved";
  public static final String ATTR_VESID_DEPRECATED = "phorm.vesid.deprecated";

  public static final String ATTR_VALIDATION_LAYERS = "phorm.validation.layers";
  public static final String ATTR_VALIDATION_ERRORS = "phorm.validation.errors";
  public static final String ATTR_VALIDATION_WARNINGS = "phorm.validation.warnings";
  public static final String ATTR_VALIDATION_VALID = "phorm.validation.valid";
  public static final String ATTR_VALIDATION_DURATION_MS = "phorm.validation.duration_ms";

  public static final String ATTR_HYBRID_COUNTRY = "phorm.hybrid.country";
  public static final String ATTR_HYBRID_LAYERS = "phorm.hybrid.layers";
  public static final String ATTR_HYBRID_XML_EXTRACTED = "phorm.hybrid.xml.extracted";
  public static final String ATTR_HYBRID_XML_BYTES = "phorm.hybrid.xml.bytes";

  public static final String ATTR_RESPONSE_FORMAT = "phorm.response.format";

  public static final String ATTR_INCLUDE_DEPRECATED = "phorm.include_deprecated";

  public static final String ATTR_REPOSITORIES_SUCCESS = "phorm.repositories.success";
  public static final String ATTR_REPOSITORIES_COUNT = "phorm.repositories.count";
  public static final String ATTR_REPOSITORIES_VES_COUNT = "phorm.repositories.ves_count";

  private CPhormTelemetry ()
  {}
}
