/*

  Copyright 2022 Hubbell Incorporated

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.

  You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.

 */

package com.idevicesinc.sweetblue;


import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Implementation of {@link SweetLogger} which just prints logs to the console via System.out.println().
 */
public class UnitTestLogger implements SweetLogger
{
    private SimpleDateFormat m_timeFormatter;


    @Override public void onLogEntry(int level, String tag, String msg)
    {
        if (m_timeFormatter == null)
            m_timeFormatter = new SimpleDateFormat("[HH:mm:ss.SSS]");
        StringBuilder b = new StringBuilder();
        b.append(m_timeFormatter.format(new Date())).append("  ");
        switch (level)
        {
            case Log.ASSERT:
                b.append("A");
                break;
            case Log.DEBUG:
                b.append("D");
                break;
            case Log.ERROR:
                b.append("E");
                break;
            case Log.VERBOSE:
                b.append("V");
                break;
            case Log.WARN:
                b.append("W");
                break;
            default:
                b.append("I");
                break;
        }
        b.append("/").append(tag).append(" : ").append(msg);
        System.out.println(b.toString());
    }
}
