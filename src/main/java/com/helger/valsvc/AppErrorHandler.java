/*
 * Copyright (C) 2022-2025 Philip Helger
 *
 * All rights reserved.
 */
package com.helger.valsvc;

import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.photon.core.interror.InternalErrorBuilder;
import com.helger.photon.core.interror.callback.AbstractErrorCallback;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

public final class AppErrorHandler extends AbstractErrorCallback
{
  @Override
  protected void onError (@NonNull final Throwable t,
                          @Nullable final IRequestWebScopeWithoutResponse aRequestScope,
                          @NonNull @Nonempty final String sErrorCode,
                          @Nullable final Map <String, String> aCustomAttrs)
  {
    new InternalErrorBuilder ().setThrowable (t)
                               .setRequestScope (aRequestScope)
                               .addErrorMessage (sErrorCode)
                               .addCustomData (aCustomAttrs)
                               .handle ();
  }

  public static void doSetup ()
  {
    // Set global internal error handlers
    new AppErrorHandler ().install ();
  }
}
