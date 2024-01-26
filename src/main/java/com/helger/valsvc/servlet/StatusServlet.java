/*
 * Copyright (C) 2022-2024 Philip Helger
 *
 * All rights reserved.
 */
package com.helger.valsvc.servlet;

import com.helger.commons.http.EHttpMethod;
import com.helger.xservlet.AbstractXServlet;

/**
 * The servlet to show the application status.
 *
 * @author Philip Helger
 */
public class StatusServlet extends AbstractXServlet
{
  public static final String SERVLET_DEFAULT_NAME = "status";
  public static final String SERVLET_DEFAULT_PATH = '/' + SERVLET_DEFAULT_NAME;

  public StatusServlet ()
  {
    handlerRegistry ().registerHandler (EHttpMethod.GET, new StatusXServletHandler ());
    handlerRegistry ().unregisterHandler (EHttpMethod.OPTIONS);
  }
}
