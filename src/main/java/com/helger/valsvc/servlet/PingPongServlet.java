/*
 * Copyright (C) 2022-2024 Philip Helger
 *
 * All rights reserved.
 */
package com.helger.valsvc.servlet;

import java.nio.charset.StandardCharsets;

import com.helger.commons.http.EHttpMethod;
import com.helger.commons.mime.CMimeType;
import com.helger.xservlet.AbstractXServlet;

public final class PingPongServlet extends AbstractXServlet
{
  public static final String SERVLET_DEFAULT_NAME = "ping";
  public static final String SERVLET_DEFAULT_PATH = "/" + SERVLET_DEFAULT_NAME;

  public PingPongServlet ()
  {
    handlerRegistry ().registerHandler (EHttpMethod.GET,
                                        (aRequestScope,
                                         aUnifiedResponse) -> aUnifiedResponse.setContentAndCharset ("pong",
                                                                                                     StandardCharsets.ISO_8859_1)
                                                                              .setMimeType (CMimeType.TEXT_PLAIN)
                                                                              .disableCaching ());
  }
}
