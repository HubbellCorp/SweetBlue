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

import com.idevicesinc.sweetblue.utils.Interval;
import org.junit.Test;
import com.idevicesinc.sweetblue.framework.AbstractTestClass;


public class IntervalTest extends AbstractTestClass
{

    @Test(timeout = 5000)
    public void smokeTest() throws Exception
    {
        startSynchronousTest();
        assertEquals(-1.0, Interval.DISABLED.secs(), 0);
        assertEquals(Double.POSITIVE_INFINITY, Interval.INFINITE.secs(), 0);
        assertEquals(0.0, Interval.ZERO.secs(), 0);
        assertEquals(1000, Interval.ONE_SEC.millis(), 0);
        assertEquals(5.0, Interval.FIVE_SECS.secs(), 0);
        assertEquals(10000, Interval.TEN_SECS.millis(), 0);
        succeed();
    }

    @Test(timeout = 5000)
    public void enablingTests() throws Exception
    {
        startSynchronousTest();
        Interval in = Interval.secs(400);
        assert Interval.isEnabled(in);
        in = Interval.secs(-400);
        assert Interval.isDisabled(in);
        succeed();
    }

    @Test(timeout = 5000)
    public void secsToMillisTest() throws Exception
    {
        startSynchronousTest();
        Interval in = Interval.secs(400);
        assertEquals(400 * 1000, in.millis());
        succeed();
    }

    @Test(timeout = 5000)
    public void millisToSecsTest() throws Exception
    {
        startSynchronousTest();
        Interval in = Interval.millis(500000);
        assertEquals(500000 / 1000, in.secs(), 0);
        succeed();
    }

    @Test(timeout = 5000)
    public void deltaTest() throws Exception
    {
        startSynchronousTest();
        Interval in = Interval.delta(1000, 10000);
        assertEquals(10000 - 1000, in.millis());
        assertEquals((10000 - 1000) / 1000, in.secs(), 0);
        succeed();
    }

}
