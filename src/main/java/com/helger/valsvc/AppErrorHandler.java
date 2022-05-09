/*
 * Copyright (C) Philip Helger
 *
 * All rights reserved.
 */
package com.helger.valsvc;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.photon.core.interror.InternalErrorBuilder;
import com.helger.photon.core.interror.callback.AbstractErrorCallback;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

public final class AppErrorHandler extends AbstractErrorCallback
{
  @Override
  protected void onError (@Nonnull final Throwable t,
                          @Nullable final IRequestWebScopeWithoutResponse aRequestScope,
                          @Nonnull @Nonempty final String sErrorCode,
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
