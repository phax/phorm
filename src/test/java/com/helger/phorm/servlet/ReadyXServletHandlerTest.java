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
package com.helger.phorm.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import com.helger.http.CHttp;
import com.helger.mime.CMimeType;
import com.helger.servlet.mock.MockHttpServletRequest;
import com.helger.servlet.mock.MockHttpServletResponse;
import com.helger.servlet.response.UnifiedResponse;

/**
 * Test class for class {@link ReadyXServletHandler}.
 *
 * @author Philip Helger
 */
public final class ReadyXServletHandlerTest
{
  private static MockHttpServletResponse _getResponse (final boolean bReady) throws IOException
  {
    final MockHttpServletRequest aRequest = new MockHttpServletRequest ();
    final UnifiedResponse aUnifiedResponse = UnifiedResponse.createSimple (aRequest);
    ReadyXServletHandler.fillResponse (bReady, aUnifiedResponse);

    final MockHttpServletResponse ret = new MockHttpServletResponse ();
    aUnifiedResponse.applyToResponse (ret);
    return ret;
  }

  @Test
  public void testReady () throws IOException
  {
    final MockHttpServletResponse aResponse = _getResponse (true);
    assertEquals (CHttp.HTTP_OK, aResponse.getStatus ());
    assertTrue (aResponse.getContentType ().startsWith (CMimeType.APPLICATION_JSON.getAsString ()));
    assertEquals ("{\"ready\":true}", aResponse.getContentAsString (StandardCharsets.UTF_8));
  }

  @Test
  public void testNotReady () throws IOException
  {
    // The payload must survive the HTTP 503
    final MockHttpServletResponse aResponse = _getResponse (false);
    assertEquals (CHttp.HTTP_SERVICE_UNAVAILABLE, aResponse.getStatus ());
    assertTrue (aResponse.getContentType ().startsWith (CMimeType.APPLICATION_JSON.getAsString ()));
    assertEquals ("{\"ready\":false}", aResponse.getContentAsString (StandardCharsets.UTF_8));
  }
}
