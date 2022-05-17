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


import com.idevicesinc.sweetblue.internal.P_Bridge_BleManager;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.UpdateThreadType;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;



@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class IdleTest extends BaseBleUnitTest
{

    @Test(timeout = 4000)
    public void enterIdleTest() throws Exception
    {
        m_config.updateThreadType = UpdateThreadType.THREAD;
        m_config.loggingOptions = LogOptions.ON;

        m_config.minTimeToIdle = Interval.secs(2.0);

        m_manager.setConfig(m_config);

        m_manager.setListener_State(e -> {
            if (e.didEnter(BleManagerState.IDLE))
            {
                IdleTest.this.assertTrue(P_Bridge_BleManager.getUpdateRate(m_manager.getIBleManager()) == m_config.idleUpdateRate.millis());
                IdleTest.this.succeed();
            }
        });

        startAsyncTest();
    }

    @Test(timeout = 6000)
    public void exitIdleTest() throws Exception
    {
        m_config.updateThreadType = UpdateThreadType.THREAD;
        m_config.loggingOptions = LogOptions.ON;
        m_config.minTimeToIdle = Interval.ONE_SEC;

        m_manager.setConfig(m_config);

        m_manager.setListener_State(e -> {
            if (e.didEnter(BleManagerState.IDLE))
            {
                IdleTest.this.assertTrue(P_Bridge_BleManager.getUpdateRate(m_manager.getIBleManager()) == m_config.idleUpdateRate.millis());
                m_manager.startScan();
            }
            else if (e.didExit(BleManagerState.IDLE))
            {
                IdleTest.this.assertTrue(P_Bridge_BleManager.getUpdateRate(m_manager.getIBleManager()) == m_config.autoUpdateRate.millis());
                IdleTest.this.succeed();
            }
        });

        startAsyncTest();
    }

}
