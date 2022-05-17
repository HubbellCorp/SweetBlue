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
import java.util.List;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class ShutdownTest extends BaseBleUnitTest
{

    @Test(timeout = 30000L)
    public void shutdownDisconnectTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;
        m_config.blockingShutdown = true;

        m_manager.setConfig(m_config);

        final BleDevice device1 = m_manager.newDevice(Util_Unit.randomMacAddress(), "TesterUno");
        final BleDevice device2 = m_manager.newDevice(Util_Unit.randomMacAddress(), "TesterDos");
        final BleDevice device3 = m_manager.newDevice(Util_Unit.randomMacAddress(), "TesterTres");
        final BleDevice device4 = m_manager.newDevice(Util_Unit.randomMacAddress(), "TesterCuatro");
        final BleDevice device5 = m_manager.newDevice(Util_Unit.randomMacAddress(), "TesterCinco");

        device1.connect();
        device2.connect();
        device3.connect();
        device4.connect();
        device5.connect(e ->
        {
            assertTrue("Device 5 failed to connect! WTF?!", e.wasSuccess());
            m_manager.shutdown();
            List<BleDevice> connectedDevices = m_manager.getDevices_List(BleDeviceState.BLE_CONNECTED);
            assertNotNull("Connected devices list is null! This be bad, yar.", connectedDevices);
            // TODO - Uncomment the following line once SWEET-752 is completed
            assertTrue("Connected devices list is not empty!", connectedDevices.isEmpty());
            m_manager = null;
            succeed();
        });

        startAsyncTest();
    }

}
