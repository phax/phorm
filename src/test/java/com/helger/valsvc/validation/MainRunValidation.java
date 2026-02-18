/*
 * Copyright (C) 2022-2026 Philip Helger
 *
 * All rights reserved.
 */
package com.helger.valsvc.validation;

import java.io.IOException;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;

import com.helger.base.io.stream.StreamHelper;
import com.helger.httpclient.HttpClientManager;
import com.helger.httpclient.response.ResponseHandlerString;
import com.helger.io.resource.FileSystemResource;
import com.helger.io.resource.IReadableResource;

/**
 * Example code to run the validation via HTTP client
 *
 * @author Philip Helger
 */
public final class MainRunValidation
{
  public static void main (final String [] args) throws IOException
  {
    final IReadableResource aRes = new FileSystemResource ("src/test/resources/testfiles/peppol-bis3/base-example.xml");
    try (final HttpClientManager aHCM = new HttpClientManager ())
    {
      final HttpPost aPost = new HttpPost ("http://localhost:8080/api/validate/eu.peppol.bis3:invoice:latest");
      aPost.setEntity (new ByteArrayEntity (StreamHelper.getAllBytes (aRes), ContentType.APPLICATION_XML));
      aPost.addHeader ("X-Token", "4cKyX6OKBs80nWPyOamn");
      final String ret = aHCM.execute (aPost, new ResponseHandlerString (ContentType.APPLICATION_JSON));
      System.out.println (ret);
    }
  }
}
