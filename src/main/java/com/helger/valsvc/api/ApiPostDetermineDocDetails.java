/*
 * Copyright (C) 2022-2025 Philip Helger
 *
 * All rights reserved.
 */
package com.helger.valsvc.api;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.helger.annotation.Nonempty;
import com.helger.base.string.StringHelper;
import com.helger.ddd.DocumentDetails;
import com.helger.http.CHttp;
import com.helger.http.header.specific.AcceptMimeTypeList;
import com.helger.json.IJsonObject;
import com.helger.json.serialize.JsonWriter;
import com.helger.json.serialize.JsonWriterSettings;
import com.helger.mime.CMimeType;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.servlet.request.RequestHelper;
import com.helger.valsvc.AppConfig;
import com.helger.valsvc.AppVersion;
import com.helger.valsvc.ddd.ValSvcDDD;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.serialize.MicroWriter;
import com.helger.xml.serialize.read.DOMReader;
import com.helger.xml.serialize.write.EXMLSerializeIndent;
import com.helger.xml.serialize.write.XMLWriterSettings;

import jakarta.annotation.Nonnull;

/**
 * Determine the document type and return it
 *
 * @author Philip Helger
 */
public final class ApiPostDetermineDocDetails extends AbstractAPIInvoker
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ApiPostDetermineDocDetails.class);
  private static final AtomicInteger COUNTER = new AtomicInteger (0);

  @Override
  public void invokeAPI (@Nonnull final IAPIDescriptor aAPIDescriptor,
                         @Nonnull @Nonempty final String sPath,
                         @Nonnull final Map <String, String> aPathVariables,
                         @Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                         @Nonnull final PhotonUnifiedResponse aUnifiedResponse) throws IOException
  {
    aUnifiedResponse.disableCaching ();
    final String sLogPrefix = "[DETERMINE-" + AppVersion.getVersionNumber () + "-" + COUNTER.incrementAndGet () + "] ";

    // Check request validity
    final String sToken = aRequestScope.headers ().getFirstHeaderValue (HEADER_X_TOKEN);
    if (StringHelper.isEmpty (sToken))
    {
      final String sErrorMsg = "The specific token header is missing";
      LOGGER.error (sLogPrefix + sErrorMsg);
      aUnifiedResponse.text (sErrorMsg).setStatus (CHttp.HTTP_FORBIDDEN);
      return;
    }
    if (!sToken.equals (AppConfig.getAPIRequiredToken ()))
    {
      final String sErrorMsg = "The specified token value does not match the configured required token";
      LOGGER.error (sLogPrefix + sErrorMsg);
      aUnifiedResponse.text (sErrorMsg).setStatus (CHttp.HTTP_FORBIDDEN);
      return;
    }

    // Read the payload as XML
    LOGGER.info (sLogPrefix + "Trying to read payload as XML");
    final Document aDoc = DOMReader.readXMLDOM (aRequestScope.getRequest ().getInputStream ());
    if (aDoc == null || aDoc.getDocumentElement () == null)
    {
      final String sErrorMsg = "Failed to read the message body as XML";
      LOGGER.error (sLogPrefix + sErrorMsg);
      aUnifiedResponse.text (sErrorMsg).createBadRequest ();
      return;
    }

    LOGGER.info (sLogPrefix + "Trying to determine payload type");
    final DocumentDetails aDD = ValSvcDDD.findDocumentDetails (aDoc.getDocumentElement ());
    if (aDD == null)
    {
      final String sErrorMsg = "Failed to determine the document types";
      LOGGER.error (sLogPrefix + sErrorMsg);
      // HTTP no content cannot have content
      aUnifiedResponse.createNoContent ();
      return;
    }

    final AcceptMimeTypeList aAcceptMimeTypes = RequestHelper.getAcceptMimeTypes (aRequestScope.getRequest ());
    if (aAcceptMimeTypes.explicitlySupportsMimeType (CMimeType.APPLICATION_XML))
    {
      // Provide response as XML
      IMicroElement eRoot = new MicroElement ("documentDetails");
      aDD.appendToMicroElement (eRoot);

      if (AppConfig.isLogResponsePayload ())
      {
        LOGGER.info (sLogPrefix +
                     "Response XML is:\n" +
                     MicroWriter.getNodeAsString (eRoot,
                                                  new XMLWriterSettings ().setIndent (EXMLSerializeIndent.INDENT_AND_ALIGN)));
      }
      aUnifiedResponse.xml (eRoot);
    }
    else
    {
      final IJsonObject aJson = aDD.getAsJson ();

      if (AppConfig.isLogResponsePayload ())
      {
        LOGGER.info (sLogPrefix +
                     "Response JSON is:\n" +
                     new JsonWriter (JsonWriterSettings.DEFAULT_SETTINGS_FORMATTED).writeAsString (aJson));
      }

      aUnifiedResponse.json (aJson);
    }
  }
}
