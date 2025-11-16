/*
 * Copyright (C) 2022-2025 Philip Helger
 *
 * All rights reserved.
 */
package com.helger.valsvc.api;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.iface.IThrowingRunnable;
import com.helger.base.timing.StopWatch;
import com.helger.datetime.helper.PDTFactory;
import com.helger.json.IJsonObject;
import com.helger.phive.result.json.PhiveJsonHelper;
import com.helger.phive.result.xml.PhiveXMLHelper;
import com.helger.xml.microdom.IMicroElement;

@Immutable
public final class CommonAPIInvoker
{
  public static final String JSON_SUCCESS = "success";

  private CommonAPIInvoker ()
  {}

  public static void invoke (@NonNull final IJsonObject aJson, @NonNull final IThrowingRunnable <Exception> r)
  {
    final ZonedDateTime aQueryDT = PDTFactory.getCurrentZonedDateTimeUTC ();
    final StopWatch aSW = StopWatch.createdStarted ();
    try
    {
      r.run ();
    }
    catch (final Exception ex)
    {
      aJson.add (JSON_SUCCESS, false);
      aJson.add ("exception", PhiveJsonHelper.getJsonStackTrace (ex));
    }
    aSW.stop ();

    aJson.add ("invocationDateTime", DateTimeFormatter.ISO_ZONED_DATE_TIME.format (aQueryDT));
    aJson.add ("invocationDurationMillis", aSW.getMillis ());
  }

  public static void invoke (@NonNull final IMicroElement aXML, @NonNull final IThrowingRunnable <Exception> r)
  {
    final ZonedDateTime aQueryDT = PDTFactory.getCurrentZonedDateTimeUTC ();
    final StopWatch aSW = StopWatch.createdStarted ();
    try
    {
      r.run ();
    }
    catch (final Exception ex)
    {
      aXML.addElement (JSON_SUCCESS).addText (false);
      aXML.addChild (PhiveXMLHelper.getXMLStackTrace (ex, "exception"));
    }
    aSW.stop ();

    aXML.addElement ("invocationDateTime").addText (DateTimeFormatter.ISO_ZONED_DATE_TIME.format (aQueryDT));
    aXML.addElement ("invocationDurationMillis").addText (aSW.getMillis ());
  }
}
