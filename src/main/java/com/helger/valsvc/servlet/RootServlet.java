/*
 * Copyright (C) 2022-2024 Philip Helger
 *
 * All rights reserved.
 */
package com.helger.valsvc.servlet;

import javax.annotation.Nonnull;

import com.helger.commons.http.EHttpMethod;
import com.helger.commons.url.SimpleURL;
import com.helger.diver.api.coord.DVRCoordinate;
import com.helger.html.hc.html.grouping.HCUL;
import com.helger.html.hc.html.root.HCHtml;
import com.helger.html.hc.html.traits.IHCTrait;
import com.helger.phive.api.executorset.IValidationExecutorSet;
import com.helger.phive.xml.source.IValidationSourceXML;
import com.helger.photon.app.html.PhotonHTMLHelper;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.valsvc.AppConfig;
import com.helger.valsvc.CApp;
import com.helger.valsvc.validation.AppValidator;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xservlet.AbstractXServlet;
import com.helger.xservlet.handler.simple.IXServletSimpleHandler;

public class RootServlet extends AbstractXServlet
{
  private static final class Hdl implements IXServletSimpleHandler, IHCTrait
  {
    private static final String PARAM_INCLUDE_DEPRECATED = "include-deprecated";

    public void handleRequest (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                               @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
    {
      final boolean bIncludeDeprecated = aRequestScope.params ().containsKey (PARAM_INCLUDE_DEPRECATED);

      final HCHtml h = new HCHtml ().setLanguage ("en");
      h.head ().setPageTitle (CApp.APP_NAME);
      h.body ().addChild (h3 (CApp.APP_NAME));
      if (AppConfig.isTestVersion ())
      {
        {
          final HCUL aUL = new HCUL ();
          aUL.addItem (div ("HTTP GET to").addChild (a (new SimpleURL ("ping")).addChild (code ("/ping")))
                                          .addChild (" for keep-alive or health checks"));
          aUL.addItem (div ("HTTP GET to").addChild (a (new SimpleURL ("status")).addChild (code ("/status")))
                                          .addChild (" for status information"));
          aUL.addItem (div ("HTTP POST to ").addChild (code ("/api/validate/{vesid}"))
                                            .addChild (" for validating messages"));
          aUL.addItem (div ("HTTP GET to").addChild (a (new SimpleURL ("api/get/vesids")).addChild (code ("/api/get/vesids")))
                                          .addChild (" to get all registered VESIDs"));
          aUL.addItem (div ("HTTP POST to ").addChild (code ("/api/determinedoctype"))
                                            .addChild (" for detecting the payload type"));
          h.body ().addChild (div ("Supported APIs are:").addChild (aUL));
        }

        final HCUL aUL = new HCUL ();
        for (final IValidationExecutorSet <IValidationSourceXML> x : AppValidator.getAllVESSorted ())
          if (bIncludeDeprecated || !x.getStatus ().isDeprecated ())
          {
            final DVRCoordinate aVESID = x.getID ();
            final String sLatestVersion = AppValidator.getLatestVersion (aVESID);
            final boolean bIsLatest = aVESID.getVersionString ().equals (sLatestVersion);

            aUL.addAndReturnItem (code (aVESID.getAsSingleID ()))
               .addChild (" - " + x.getDisplayName ())
               .addChild (bIsLatest ? strong (" [latest]") : null)
               .addChild (x.getStatus ().isDeprecated () ? strong (" deprecated") : null);
          }
        if (!bIncludeDeprecated)
          h.body ()
           .addChild (div (a (new SimpleURL (aRequestScope.getURIDecoded ()).add (PARAM_INCLUDE_DEPRECATED)).addChild ("Show below list including duplicates")));
        h.body ()
         .addChild (div ("Supported VESIDs are" + (bIncludeDeprecated ? " (including deprecated)" : "") + ":")
                                                                                                              .addChild (aUL));
      }
      PhotonHTMLHelper.createHTMLResponse (aRequestScope, aUnifiedResponse, x -> h);
    }
  }

  public RootServlet ()
  {
    handlerRegistry ().registerHandler (EHttpMethod.GET, new Hdl ());
    handlerRegistry ().copyHandlerToAll (EHttpMethod.GET);
  }
}
