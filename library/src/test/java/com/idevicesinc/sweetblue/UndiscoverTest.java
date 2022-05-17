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
import com.idevicesinc.sweetblue.utils.Util_Unit;
import com.idevicesinc.sweetblue.utils.Utils_ScanRecord;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class UndiscoverTest extends BaseBleUnitTest
{


    @Test(timeout = 15000)
    public void undiscoverConnectedDeviceTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;

        m_manager.setConfig(m_config);

        final BleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "Undiscover Me!");

        m_manager.setListener_Discovery(e -> {
            if (e.was(DiscoveryListener.LifeCycle.UNDISCOVERED))
            {
                UndiscoverTest.this.assertFalse(P_Bridge_BleManager.hasDevice(m_manager.getIBleManager(), device.getIBleDevice()));
                UndiscoverTest.this.assertFalse(device.is(BleDeviceState.CONNECTED));
                UndiscoverTest.this.succeed();
            }
        });

        device.connect(e -> {
            UndiscoverTest.this.assertTrue(e.wasSuccess());
            device.undiscover();
        });

        startAsyncTest();
    }

    @Test(timeout = 20000)
    public void undiscoveryTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;
        m_config.undiscoveryKeepAlive = Interval.secs(5.0);
        m_config.minScanTimeNeededForUndiscovery = Interval.secs(1.0);

        m_manager.setConfig(m_config);

        final String mac = Util_Unit.randomMacAddress();

        m_manager.setListener_Discovery(e -> {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                UndiscoverTest.this.assertTrue(e.macAddress().equals(mac));
            }
            else if (e.was(DiscoveryListener.LifeCycle.UNDISCOVERED))
            {
                UndiscoverTest.this.assertTrue(e.macAddress().equals(mac));
                double lastDiscovery = e.device().getIBleDevice().getTimeSinceLastDiscovery();
                UndiscoverTest.this.assertTrue(" Last discovery: " + lastDiscovery, lastDiscovery >= 5.0);
                UndiscoverTest.this.succeed();
            }
        });

        m_manager.startScan();

        Util_Native.advertiseDevice(m_manager, -45, Utils_ScanRecord.newScanRecord("DerpyDerpDerp"), mac);

        startAsyncTest();
    }


}
