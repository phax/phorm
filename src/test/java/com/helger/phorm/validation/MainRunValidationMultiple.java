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

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.concurrent.ExecutorServiceHelper;
import com.helger.base.io.stream.StreamHelper;
import com.helger.base.timing.StopWatch;
import com.helger.httpclient.HttpClientManager;
import com.helger.httpclient.response.ResponseHandlerHttpEntity;
import com.helger.io.resource.FileSystemResource;
import com.helger.io.resource.IReadableResource;

public class MainRunValidationMultiple
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainRunValidationMultiple.class);

  public static void main (final String [] args)
  {
    final IReadableResource aRes = new FileSystemResource ("src/test/resources/testfiles/peppol-bis3/base-example.xml");
    try (final HttpClientManager aHCM = new HttpClientManager ())
    {
      final byte [] aPayload = StreamHelper.getAllBytes (aRes);
      final ExecutorService aES = Executors.newFixedThreadPool (8);
      final StopWatch aSW = StopWatch.createdStarted ();
      final int nCount = 10_000;
      for (int i = 0; i < nCount; ++i)
        aES.submit ( () -> {
          final HttpPost aPost = new HttpPost ("http://localhost:8080/api/validate/eu.peppol.bis3:invoice:latest");
          aPost.setEntity (new ByteArrayEntity (aPayload, ContentType.APPLICATION_XML));
          aPost.addHeader ("X-Token", "4phorm-dev-token");
          try
          {
            aHCM.execute (aPost, ResponseHandlerHttpEntity.INSTANCE);
          }
          catch (final IOException ex)
          {
            LOGGER.error ("Error: " + ex.getMessage ());
          }
        });
      ExecutorServiceHelper.shutdownAndWaitUntilAllTasksAreFinished (aES);
      aSW.stop ();
      LOGGER.info (nCount + " validation took " + aSW.getMillis () + " ms");
    }
  }
}
