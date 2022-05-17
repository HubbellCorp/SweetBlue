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


import com.idevicesinc.sweetblue.utils.Util_Unit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class DeviceManagerTest extends BaseBleUnitTest
{


    @Test(timeout = 20000)
    public void removeDevicesFromCacheTest() throws Exception
    {
        final long m_timeStarted = System.currentTimeMillis();
        m_config.loggingOptions = LogOptions.ON;
        m_manager.setConfig(m_config);
        new Thread(() -> {
            while (m_timeStarted + 5000 > System.currentTimeMillis())
            {
                m_manager.newDevice(Util_Unit.randomMacAddress());
                try
                {
                    Thread.sleep(25);
                } catch (Exception e)
                {
                }
            }
        }).start();
        new Thread(() -> {
            while (m_timeStarted + 5000 > System.currentTimeMillis())
            {
                try
                {
                    Thread.sleep(500);
                } catch (Exception e)
                {
                }
                m_manager.removeAllDevicesFromCache();
            }
            DeviceManagerTest.this.succeed();
        }).start();
        startAsyncTest();
    }

}
