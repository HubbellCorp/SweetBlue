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
import com.idevicesinc.sweetblue.rx.RxBleDevice;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Util_Unit;
import com.idevicesinc.sweetblue.utils.Utils_ScanRecord;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public final class RxUndiscoverTest extends RxBaseBleUnitTest
{

    @Test(timeout = 15000)
    public void undiscoverConnectedDeviceTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;

        m_manager.setConfig(m_config);

        final RxBleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "Undiscover Me!");

        m_disposables.add(m_manager.observeDiscoveryEvents().subscribe(e ->
        {
            if (e.wasUndiscovered())
            {
                assertFalse(P_Bridge_BleManager.hasDevice(m_manager.getBleManager().getIBleManager(), device.getBleDevice().getIBleDevice()));
                assertFalse(device.is(BleDeviceState.CONNECTED));
                succeed();
            }
        }));

        m_disposables.add(device.connect().subscribe(device::undiscover));

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

        m_disposables.add(m_manager.observeDiscoveryEvents().subscribe(e ->
        {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                assertTrue(e.macAddress().equals(mac));
            }
            else if (e.was(DiscoveryListener.LifeCycle.UNDISCOVERED))
            {
                assertTrue(e.macAddress().equals(mac));
                double lastDiscovery = e.device().getBleDevice().getIBleDevice().getTimeSinceLastDiscovery();
                assertTrue(" Last discovery: " + lastDiscovery, lastDiscovery >= 5.0);
                succeed();
            }
        }));


        m_manager.scan().subscribe();

        Util_Native.advertiseDevice(m_manager.getBleManager(), -45, Utils_ScanRecord.newScanRecord("DerpyDerpDerp"), mac);

        startAsyncTest();
    }

}
