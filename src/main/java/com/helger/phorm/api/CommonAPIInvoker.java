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
package com.helger.phorm.api;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.iface.IThrowingRunnable;
import com.helger.base.timing.StopWatch;
import com.helger.datetime.helper.PDTFactory;
import com.helger.json.IJsonObject;
import com.helger.phive.result.json.CPhiveJson;
import com.helger.phive.result.json.PhiveJsonHelper;
import com.helger.phive.result.xml.CPhiveXML;
import com.helger.phive.result.xml.PhiveXMLHelper;
import com.helger.xml.microdom.IMicroElement;

@Immutable
public final class CommonAPIInvoker
{
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
      aJson.add (CPhiveJson.JSON_SUCCESS, false);
      aJson.add (CPhiveJson.JSON_EXCEPTION, PhiveJsonHelper.getJsonStackTrace (ex));
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
      aXML.addElement (CPhiveXML.XML_SUCCESS).addText (false);
      aXML.addChild (PhiveXMLHelper.getXMLStackTrace (ex, CPhiveXML.XML_EXCEPTION));
    }
    aSW.stop ();

    aXML.addElement ("invocationDateTime").addText (DateTimeFormatter.ISO_ZONED_DATE_TIME.format (aQueryDT));
    aXML.addElement ("invocationDurationMillis").addText (aSW.getMillis ());
  }
}
