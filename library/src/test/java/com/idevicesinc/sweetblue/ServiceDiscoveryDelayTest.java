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


import com.idevicesinc.sweetblue.internal.IBleDevice;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Util_Unit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;



@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class ServiceDiscoveryDelayTest extends BaseBleUnitTest
{

    @Test(timeout = 15000)
    public void delayedServiceDiscoveryTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;
        m_config.useGattRefresh = false;
        m_config.gattFactory = BluetoothGatt::new;
        m_config.serviceDiscoveryDelay = Interval.ONE_SEC;
        m_config.defaultDeviceStates = new BleDeviceState[] { BleDeviceState.DISCOVERING_SERVICES };

        m_manager.setConfig(m_config);

        BleDevice fake = m_manager.newDevice(Util_Unit.randomMacAddress(), "Fakey Fakerson");

        fake.setListener_State(e -> {
            if (e.didExit(BleDeviceState.DISCOVERING_SERVICES))
            {
                Interval time = e.device().getTimeInState(BleDeviceState.DISCOVERING_SERVICES);
                ServiceDiscoveryDelayTest.this.assertTrue("Expected time to be 1.0 secs or greater, instead it took " + time.secs() + "secs.", time.secs() >= 1.0);
                ServiceDiscoveryDelayTest.this.succeed();
            }
        });

        fake.connect();

        startAsyncTest();
    }


    private static final class BluetoothGatt extends UnitTestBluetoothGatt
    {

        public BluetoothGatt(IBleDevice device)
        {
            super(device);
        }

        @Override
        public Interval getDelayTime()
        {
            return Interval.millis(5);
        }
    }

}
