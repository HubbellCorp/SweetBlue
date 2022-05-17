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

import com.idevicesinc.sweetblue.utils.Distance;
import org.junit.Test;
import com.idevicesinc.sweetblue.framework.AbstractTestClass;


public class DistanceTest extends AbstractTestClass
{

    @Test(timeout = 5000)
    public void smokeTests() throws Exception {
        startSynchronousTest();
        Distance d = Distance.meters(4.0);
        assertEquals(4.0, d.meters(), 0);
        assertEquals(4.0 * 3.28084, d.feet(), 0);
        assertEquals(0.0, Distance.ZERO.meters(), 0);
        assertEquals(-1.0, Distance.INVALID.meters(), 0);
        succeed();
    }

    @Test(timeout = 5000)
    public void meterToFeetTest() throws Exception {
        startSynchronousTest();
        double meters = 4.0;
        double feet = meters * 3.28084;
        assertEquals(feet, Distance.meters(meters).feet(), 0);
        succeed();
    }

    @Test(timeout = 5000)
    public void feetToMeterTest() throws Exception {
        startSynchronousTest();
        double feet = 25;
        double meters = feet / 3.28084;
        assertEquals(meters, Distance.feet(feet).meters(), 0);
        succeed();
    }

}
