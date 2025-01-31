/*
 * Copyright (C) 2022-2024 Philip Helger
 *
 * All rights reserved.
 */
package com.helger.valsvc.api;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.timing.StopWatch;
import com.helger.json.serialize.JsonWriterSettings;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.api.IAPIExecutor;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * Abstract base invoker for REST API
 *
 * @author Philip Helger
 */
public abstract class AbstractAPIInvoker implements IAPIExecutor
{
  public static final String HEADER_X_TOKEN = "X-Token";
  private static final Logger LOGGER = LoggerFactory.getLogger (AbstractAPIInvoker.class);

  public abstract void invokeAPI (@Nonnull final IAPIDescriptor aAPIDescriptor,
                                  @Nonnull @Nonempty final String sPath,
                                  @Nonnull final Map <String, String> aPathVariables,
                                  @Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                                  @Nonnull final PhotonUnifiedResponse aUnifiedResponse) throws IOException;

  public final void invokeAPI (@Nonnull final IAPIDescriptor aAPIDescriptor,
                               @Nonnull @Nonempty final String sPath,
                               @Nonnull final Map <String, String> aPathVariables,
                               @Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                               @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    final StopWatch aSW = StopWatch.createdStarted ();

    final PhotonUnifiedResponse aPUR = (PhotonUnifiedResponse) aUnifiedResponse;
    aPUR.setJsonWriterSettings (JsonWriterSettings.DEFAULT_SETTINGS_FORMATTED);
    invokeAPI (aAPIDescriptor, sPath, aPathVariables, aRequestScope, aPUR);

    aSW.stop ();
    LOGGER.info ("[API] Successfully finished '" +
                 aAPIDescriptor.getPathDescriptor ().getAsURLString () +
                 "' after " +
                 aSW.getMillis () +
                 " milliseconds");
  }
}
