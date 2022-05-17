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


import com.idevicesinc.sweetblue.utils.Pointer;
import com.idevicesinc.sweetblue.utils.UpdateThreadType;
import com.idevicesinc.sweetblue.utils.Util_Unit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class GattRefreshTest extends BaseBleUnitTest
{

    @Test(timeout = 12000)
    public void connectThenRefreshGattTest() throws Exception
    {

        m_config.updateThreadType = UpdateThreadType.THREAD;
        m_config.loggingOptions = LogOptions.ON;
        BleDeviceState[] states = new BleDeviceState[] { BleDeviceState.CONNECTED, BleDeviceState.SERVICES_DISCOVERED };
        m_config.defaultDeviceStates = states;

        m_manager.setConfig(m_config);

        final Pointer<Boolean> refreshingGatt = new Pointer<>(false);


        m_manager.setListener_Discovery(e -> {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED) || e.was(DiscoveryListener.LifeCycle.REDISCOVERED))
            {
                final BleDevice device = e.device();
                device.setListener_State(e1 -> {
                    if (e1.didEnter(BleDeviceState.SERVICES_DISCOVERED))
                    {
                        if (refreshingGatt.value)
                        {
                            GattRefreshTest.this.succeed();
                        }
                    }
                    if (e1.didEnter(BleDeviceState.CONNECTED))
                    {
                        refreshingGatt.value = true;
                        e1.device().refreshGattDatabase();
                    }
                });
                device.connect();
            }
        });

        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device");

        startAsyncTest();

    }

}
