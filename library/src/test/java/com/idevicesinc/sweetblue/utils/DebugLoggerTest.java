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

package com.idevicesinc.sweetblue.utils;


import com.idevicesinc.sweetblue.framework.AbstractTestClass;

import org.junit.Test;
import java.util.List;


public class DebugLoggerTest extends AbstractTestClass
{

    @Test
    public void getLogListTest() throws Exception
    {
        startSynchronousTest();
        DebugLogger log = new DebugLogger(true, 10);
        log.onLogEntry(2, "tag", "2");
        log.onLogEntry(2, "tag", "3");
        log.onLogEntry(2, "tag", "4");
        log.onLogEntry(2, "tag", "5");
        log.onLogEntry(2, "tag", "6");
        log.onLogEntry(2, "tag", "7");
        log.onLogEntry(2, "tag", "8");
        log.onLogEntry(2, "tag", "9");
        log.onLogEntry(2, "tag", "10");
        log.onLogEntry(2, "tag", "11");
        log.onLogEntry(2, "tag", "12");
        List<String> logList = log.getLogList();
        assertTrue(logList.size() == 10);
        assertTrue(logList.get(9).endsWith("12"));
        long time = System.currentTimeMillis();
        succeed();
    }

    @Test
    public void getLastLogsTest() throws Exception
    {
        startSynchronousTest();
        DebugLogger log = new DebugLogger(true, 10);
        log.onLogEntry(2, "tag", "2");
        log.onLogEntry(2, "tag", "3");
        log.onLogEntry(2, "tag", "4");
        log.onLogEntry(2, "tag", "5");
        log.onLogEntry(2, "tag", "6");
        log.onLogEntry(2, "tag", "7");
        log.onLogEntry(2, "tag", "8");
        log.onLogEntry(2, "tag", "9");
        log.onLogEntry(2, "tag", "10");
        log.onLogEntry(2, "tag", "11");
        log.onLogEntry(2, "tag", "12");
        List<String> list = log.getLastLogs(5);
        assertTrue(list.size() == 5);
        assertTrue(list.get(4).endsWith("12"));
        succeed();
    }

    @Test
    public void getLastLogTest() throws Exception
    {
        startSynchronousTest();
        DebugLogger log = new DebugLogger(true, 10);
        log.onLogEntry(2, "tag", "2");
        log.onLogEntry(2, "tag", "3");
        log.onLogEntry(2, "tag", "4");
        log.onLogEntry(2, "tag", "5");
        log.onLogEntry(2, "tag", "6");
        log.onLogEntry(2, "tag", "7");
        log.onLogEntry(2, "tag", "8");
        log.onLogEntry(2, "tag", "9");
        log.onLogEntry(2, "tag", "10");
        log.onLogEntry(2, "tag", "11");
        log.onLogEntry(2, "tag", "12");
        String last = log.getLastLog();
        assertNotNull(last);
        assertTrue(last.endsWith("12"));
        succeed();
    }
}
