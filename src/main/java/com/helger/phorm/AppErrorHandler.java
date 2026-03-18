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
package com.helger.phorm;

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
