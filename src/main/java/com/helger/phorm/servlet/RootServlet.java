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
package com.helger.phorm.servlet;

import org.jspecify.annotations.NonNull;

import com.helger.diver.api.coord.DVRCoordinate;
import com.helger.html.hc.html.grouping.HCUL;
import com.helger.html.hc.html.root.HCHtml;
import com.helger.html.hc.html.traits.IHCTrait;
import com.helger.http.EHttpMethod;
import com.helger.phive.api.executorset.IValidationExecutorSet;
import com.helger.phive.xml.source.IValidationSourceXML;
import com.helger.photon.app.html.PhotonHTMLHelper;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.url.SimpleURL;
import com.helger.phorm.AppConfig;
import com.helger.phorm.CApp;
import com.helger.phorm.validation.AppValidator;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xservlet.AbstractXServlet;
import com.helger.xservlet.handler.simple.IXServletSimpleHandler;

public class RootServlet extends AbstractXServlet
{
  private static final class Hdl implements IXServletSimpleHandler, IHCTrait
  {
    private static final String PARAM_INCLUDE_DEPRECATED = "include-deprecated";

    public void handleRequest (@NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                               @NonNull final UnifiedResponse aUnifiedResponse) throws Exception
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
                                            .addChild (" to detect the payload type"));
          aUL.addItem (div ("HTTP POST to ").addChild (code ("/api/dd_and_validate"))
                                            .addChild (" to detect the payload type and perform a validation based on the result"));

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
           .addChild (div (a (new SimpleURL (aRequestScope.getURIDecoded ()).add (PARAM_INCLUDE_DEPRECATED)).addChild ("Show below list including deprecated entries")));
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
