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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class NullDeviceTest extends BaseBleUnitTest
{

    @Test
    public void clearAllDataTest() throws Exception
    {
        startSynchronousTest();

        BleDevice device = BleDevice.NULL;

        // Not asserting anything here. This method used to crash when called. Simply calling the method
        // will suffice to prove that it works (the test will fail if an exception is thrown)
        device.clearAllData();

        succeed();
    }

}
