/*
 * Copyright (C) Philip Helger
 *
 * All rights reserved.
 */
package com.helger.valsvc.servlet;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.ServletContext;

import org.slf4j.bridge.SLF4JBridgeHandler;

import com.helger.commons.CGlobal;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.vendor.VendorInfo;
import com.helger.dao.DAOException;
import com.helger.photon.api.APIDescriptor;
import com.helger.photon.api.APIPath;
import com.helger.photon.api.IAPIRegistry;
import com.helger.photon.audit.DoNothingAuditor;
import com.helger.photon.audit.IAuditItem;
import com.helger.photon.audit.IAuditManager;
import com.helger.photon.audit.IAuditor;
import com.helger.photon.core.locale.ILocaleManager;
import com.helger.photon.core.servlet.WebAppListener;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.mgr.PhotonSecurityManager.FactoryXML;
import com.helger.scope.singleton.SingletonHelper;
import com.helger.valsvc.AppConfig;
import com.helger.valsvc.AppErrorHandler;
import com.helger.valsvc.AppMetaManager;
import com.helger.valsvc.AppSecurity;
import com.helger.valsvc.CApp;
import com.helger.valsvc.api.ApiGetAllVESIDs;
import com.helger.valsvc.api.ApiPostValidate;
import com.helger.xservlet.requesttrack.RequestTrackerSettings;

/**
 * Callbacks for the application server
 *
 * @author Philip Helger
 */
public final class AppWebAppListener extends WebAppListener
{
  private static OffsetDateTime s_aStartupDateTime;

  @Nullable
  public static OffsetDateTime getStartupDateTime ()
  {
    return s_aStartupDateTime;
  }

  public AppWebAppListener ()
  {
    setHandleStatisticsOnEnd (false);
  }

  @Override
  protected String getInitParameterDebug (@Nonnull final ServletContext aSC)
  {
    return AppConfig.getGlobalDebug ();
  }

  @Override
  protected String getInitParameterProduction (@Nonnull final ServletContext aSC)
  {
    return AppConfig.getGlobalProduction ();
  }

  @Override
  protected String getDataPath (@Nonnull final ServletContext aSC)
  {
    return AppConfig.getDataPath ();
  }

  @Override
  protected boolean shouldCheckFileAccess (@Nonnull final ServletContext aSC)
  {
    return AppConfig.isCheckFileAccess ();
  }

  @Override
  protected String getInitParameterNoStartupInfo (@Nonnull final ServletContext aSC)
  {
    return "true";
  }

  @Override
  protected void initGlobalSettings ()
  {
    s_aStartupDateTime = PDTFactory.getCurrentOffsetDateTimeUTC ();

    // Internal stuff:
    VendorInfo.setInceptionYear (2022);

    // Avoid startup error logs
    SingletonHelper.setDebugConsistency (false);

    // Logging: JUL to SLF4J
    SLF4JBridgeHandler.removeHandlersForRootLogger ();
    SLF4JBridgeHandler.install ();

    // Request tracker
    RequestTrackerSettings.setLongRunningRequestsCheckEnabled (true);
    RequestTrackerSettings.setLongRunningRequestCheckIntervalMilliseconds (10 * CGlobal.MILLISECONDS_PER_SECOND);
    RequestTrackerSettings.setParallelRunningRequestsCheckEnabled (true);
  }

  @Override
  protected void initLocales (@Nonnull final ILocaleManager aLocaleMgr)
  {
    aLocaleMgr.registerLocale (CApp.DEFAULT_LOCALE);
    aLocaleMgr.setDefaultLocale (CApp.DEFAULT_LOCALE);
  }

  // TODO replace with ph-oton version in >= 8.4.1
  private static final class DoNothingAuditManager implements IAuditManager
  {
    private final IAuditor m_aAuditor = new DoNothingAuditor ( () -> null);

    public boolean isInMemory ()
    {
      return true;
    }

    @Nullable
    public String getBaseDir ()
    {
      return null;
    }

    @Nonnull
    public IAuditor getAuditor ()
    {
      return m_aAuditor;
    }

    @Nonnull
    public List <IAuditItem> getLastAuditItems (final int nMaxItems)
    {
      return new CommonsArrayList <> ();
    }

    public void stop ()
    {}

    @Nullable
    public LocalDate getEarliestAuditDate ()
    {
      return null;
    }
  }

  @Override
  protected void initSecurity ()
  {
    PhotonSecurityManager.setFactory (new FactoryXML ()
    {
      @Override
      public IAuditManager createAuditManager () throws DAOException
      {
        // Avoid creating audits
        return new DoNothingAuditManager ();
      }
    });
    // Set all security related stuff
    AppSecurity.init ();
  }

  @Override
  protected void initManagers ()
  {
    AppErrorHandler.doSetup ();
    AppMetaManager.getInstance ();
  }

  @Override
  protected void initAPI (@Nonnull final IAPIRegistry aAPIRegistry)
  {
    aAPIRegistry.registerAPI (new APIDescriptor (APIPath.post ("/validate/{vesid}"), ApiPostValidate.class));
    aAPIRegistry.registerAPI (new APIDescriptor (APIPath.get ("/get/vesids"), ApiGetAllVESIDs.class));
  }
}
