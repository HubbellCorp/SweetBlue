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

import com.idevicesinc.sweetblue.utils.Percent;
import org.junit.Test;
import com.idevicesinc.sweetblue.framework.AbstractTestClass;


public class PercentTest extends AbstractTestClass
{

    @Test(timeout = 5000)
    public void smokeTests() throws Exception
    {
        startSynchronousTest();
        assertEquals(0, Percent.ZERO.toDouble(), 0);
        assertEquals(100, Percent.HUNDRED.toDouble(), 0);
        succeed();
    }

    @Test(timeout = 5000)
    public void percentToFractionTest() throws Exception
    {
        startSynchronousTest();
        Percent p = Percent.fromDouble(79.5);
        assertEquals(.795, p.toFraction(), 0);
        succeed();
    }

    @Test(timeout = 5000)
    public void clampTest() throws Exception
    {
        startSynchronousTest();
        Percent p = Percent.fromDouble_clamped(125);
        assertEquals(100, p.toDouble(), 0);
        p = Percent.fromDouble(125).clamp();
        assertEquals(100, p.toDouble(), 0);
        p = Percent.fromDouble_clamped(-25);
        assertEquals(0, p.toDouble(), 0);
        p = Percent.fromDouble(-25).clamp();
        assertEquals(0, p.toDouble(), 0);
        succeed();
    }

    @Test(timeout = 5000)
    public void ceilingFloorTest() throws Exception
    {
        startSynchronousTest();
        Percent p = Percent.fromDouble(71.87);
        assertEquals(71, p.toInt_floor(), 0);
        assertEquals(72, p.toInt_ceil(), 0);
        succeed();
    }
}
