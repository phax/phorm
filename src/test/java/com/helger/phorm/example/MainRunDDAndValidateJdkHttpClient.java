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
package com.helger.phorm.example;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import com.helger.phorm.validation.MainRunValidation;

/**
 * Self-contained example that shows how to call the phorm <code>POST /api/dd_and_validate</code>
 * endpoint using nothing but the Java 17 (JDK) built-in HTTP client
 * ({@link java.net.http.HttpClient}). This endpoint first auto-determines the document type (aka
 * "document details determination" / "DDD") of the posted XML and then validates it against the
 * matching validation ruleset (VESID) in one single call - so the caller does not need to know the
 * VESID upfront.
 * <p>
 * Unlike {@link MainRunValidation}, this example intentionally avoids the ph-httpclient / Apache
 * HttpClient dependency and relies purely on the JDK. That makes it a good copy-paste starting
 * point for projects that do not want to pull in any additional HTTP libraries.
 * <p>
 * <b>How the endpoint works</b> (see
 * <code>com.helger.phorm.api.ApiPostDetermineDocTypeAndValidate</code>):
 * <ul>
 * <li>HTTP method: <code>POST</code></li>
 * <li>URL: <code>/api/dd_and_validate</code> (no VESID in the path - it is determined
 * automatically)</li>
 * <li>Request body: the raw XML document to validate (may be a plain business document or a Peppol
 * SBDH-wrapped document)</li>
 * <li>Request <code>Content-Type</code>: <code>application/xml</code></li>
 * <li>Authentication: a shared secret sent in the <code>X-Token</code> HTTP header. It must match
 * the server side <code>phorm.api.requiredtoken</code> configuration value. When that server
 * setting is empty, no token is required.</li>
 * <li>Response format: negotiated via the <code>Accept</code> header - request
 * <code>application/xml</code> for XML, <code>text/html</code> for a human readable HTML report,
 * anything else (or nothing) yields the default JSON response.</li>
 * </ul>
 * <p>
 * This is an example {@code main} method (not a JUnit test) - to run it, start a phorm instance
 * first (e.g. {@code com.helger.phorm.jetty.RunInJettyPhorm}), then run this class.
 *
 * @author Philip Helger
 */
public final class MainRunDDAndValidateJdkHttpClient
{
  // === Input placeholders - adjust these to your environment ===============

  /**
   * Base URL of the running phorm instance. Replace with your real host/port, e.g.
   * "https://phorm.example.org".
   */
  private static final String BASE_URL = "http://localhost:8090";

  /**
   * The shared secret expected by the server in the "X-Token" header. This must match the
   * "phorm.api.requiredtoken" configuration value of the target instance. The default value shipped
   * in application.properties for local development is "phorm-dev-token".
   */
  private static final String API_TOKEN = "phorm-dev-token";

  /**
   * Path to the XML file that shall be sent for determination + validation. Replace with the path
   * to your own document. This example reuses a bundled Peppol BIS Billing 3.0 test invoice.
   */
  private static final Path INPUT_XML_FILE = Path.of ("src/test/resources/testfiles/peppol-bis3/base-example.xml");

  private MainRunDDAndValidateJdkHttpClient ()
  {}

  public static void main (final String [] aArgs) throws IOException, InterruptedException
  {
    // --- 1. Read the XML payload to be validated ---------------------------
    // The endpoint expects the raw XML bytes as the request body. We read the
    // file as-is (no re-encoding) so that the original byte content and
    // encoding declaration are preserved.
    final byte [] aPayloadBytes = Files.readAllBytes (INPUT_XML_FILE);
    System.out.println ("Read " + aPayloadBytes.length + " bytes from '" + INPUT_XML_FILE + "'");

    // --- 2. Build the JDK HttpClient ---------------------------------------
    // A single HttpClient instance is thread-safe and can be reused for many
    // requests. Here we set a connect timeout and let it follow normal HTTP
    // redirects.
    final HttpClient aHttpClient = HttpClient.newBuilder ()
                                             .connectTimeout (Duration.ofSeconds (10))
                                             .followRedirects (HttpClient.Redirect.NORMAL)
                                             .build ();

    // --- 3. Assemble the POST request --------------------------------------
    // Target endpoint: POST {BASE_URL}/api/dd_and_validate
    final URI aEndpointURI = URI.create (BASE_URL + "/api/dd_and_validate");

    final HttpRequest aRequest = HttpRequest.newBuilder ()
                                            .uri (aEndpointURI)
                                            // Send the raw XML document as the request body
                                            .POST (BodyPublishers.ofByteArray (aPayloadBytes))
                                            // The payload is XML
                                            .header ("Content-Type", "application/xml; charset=UTF-8")
                                            // Ask for a JSON response. Use "application/xml" to get
                                            // XML or "text/html" to get a human-readable HTML
                                            // report instead.
                                            .header ("Accept", "application/json")
                                            // Authentication: the shared secret token. Omit this
                                            // header only if the server has no required token
                                            // configured.
                                            .header ("X-Token", API_TOKEN)
                                            // Overall request timeout (waiting for the response)
                                            .timeout (Duration.ofSeconds (60))
                                            .build ();

    // --- 4. Execute the request synchronously ------------------------------
    System.out.println ("POSTing to " + aEndpointURI + " ...");
    final HttpResponse <String> aResponse = aHttpClient.send (aRequest, BodyHandlers.ofString (StandardCharsets.UTF_8));

    // --- 5. Evaluate the response ------------------------------------------
    final int nStatusCode = aResponse.statusCode ();
    final String sResponseBody = aResponse.body ();

    System.out.println ("HTTP status code: " + nStatusCode);
    System.out.println ("Response body:");
    System.out.println (sResponseBody);

    // Interpretation hints:
    // * 200 OK - the request was processed. NOTE: with the default
    // server setting "phorm.api.response.onfailure.http400=true"
    // a *content-wise invalid* document is answered with HTTP
    // 400 instead of 200, so a 400 does not necessarily mean a
    // malformed request - inspect the body to be sure.
    // * 400 Bad Req. - either the body could not be parsed as XML, the document
    // type could not be determined, or the document is
    // considered invalid by the ruleset (see note above).
    // * 403 Forbidden - the X-Token header was missing or did not match the
    // server side "phorm.api.requiredtoken".
    //
    // The default JSON response body contains (amongst others):
    // {
    // "documentDetails" : { ... }, // detected sender/receiver/doctype/VESID/...
    // "ves" : { ... }, // the resolved validation executor set (vesid/name/status)
    // "validationSource": { ... }, // sourceTypeID + partialSource flag
    // "success" : true|false, // overall validation success
    // "interrupted" : true|false,
    // "mostSevereErrorLevel" : "ERROR", // "SUCCESS" when everything passed
    // "result" : [ ... ], // per validation-layer results incl. error "item" list
    // "durationMS" : 644, // pure validation duration
    // "invocationDateTime" : "2026-07-09T12:34:56.789Z",
    // "invocationDurationMillis": 123 // total API invocation duration
    // }
    // For XML output the root element is <validationResults>; for HTML a ready
    // to render report is returned. Change the "Accept" header above accordingly.

    // With the default server setting "phorm.api.response.onfailure.http400=true"
    // the endpoint answers a *content-wise invalid* document with HTTP 400. So
    // for this example we treat the two most relevant status codes explicitly:
    // * 2xx - the document was determined and validated successfully (valid)
    // * 400 - the document is invalid (or could not be parsed / determined)
    // * everything else - a request/transport/authentication problem
    switch (nStatusCode)
    {
      case 200 -> System.out.println ("=> Document is VALID - it passed determination and validation.");
      case 400 -> System.out.println ("=> Document is INVALID - it was rejected by the validation ruleset " +
                                      "(or the body could not be parsed as XML or the document type could not " +
                                      "be determined). Inspect the response body above for the details.");
      case 403 -> System.out.println ("=> Authentication failed - the 'X-Token' header was missing or did not " +
                                      "match the server side 'phorm.api.requiredtoken'.");
      default -> System.out.println ("=> Server returned an unexpected status (" +
                                     nStatusCode +
                                     ") - see body above for details.");
    }
  }
}
