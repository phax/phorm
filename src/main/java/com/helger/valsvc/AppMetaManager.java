/*
 * Copyright (C) 2022-2025 Philip Helger
 *
 * All rights reserved.
 */
package com.helger.valsvc;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.style.UsedViaReflection;
import com.helger.base.exception.InitializationException;
import com.helger.base.lang.clazz.ClassHelper;
import com.helger.scope.IScope;
import com.helger.scope.singleton.AbstractGlobalSingleton;

public final class AppMetaManager extends AbstractGlobalSingleton
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AppMetaManager.class);

  @Deprecated
  @UsedViaReflection
  public AppMetaManager ()
  {}

  private void _runSystemMigrations ()
  {
    // empty
  }

  @Override
  protected void onAfterInstantiation (@NonNull final IScope aScope)
  {
    try
    {
      // Init managers

      // Migrate
      _runSystemMigrations ();

      LOGGER.info (ClassHelper.getClassLocalName (this) + " was initialized");
    }
    catch (final Exception ex)
    {
      throw new InitializationException ("Failed to init " + ClassHelper.getClassLocalName (this), ex);
    }
  }

  @NonNull
  public static AppMetaManager getInstance ()
  {
    return getGlobalSingleton (AppMetaManager.class);
  }
}
