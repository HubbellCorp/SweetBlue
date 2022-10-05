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


import com.idevicesinc.sweetblue.di.SweetDIManager;
import com.idevicesinc.sweetblue.internal.IBleDevice;
import com.idevicesinc.sweetblue.internal.android.IBluetoothGatt;
import com.idevicesinc.sweetblue.utils.GattDatabase;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Pointer;
import com.idevicesinc.sweetblue.utils.Util_Unit;
import com.idevicesinc.sweetblue.utils.Uuids;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Random;

import static org.junit.Assert.assertTrue;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class PollingTest extends BaseBleUnitTest
{

    private GattDatabase db = new GattDatabase().addService(Uuids.BATTERY_SERVICE_UUID)
            .addCharacteristic(Uuids.BATTERY_LEVEL).setValue(new byte[]{100}).setPermissions().read().setProperties().read().completeService();


    @Override
    public void postSetup()
    {
        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, BatteryBluetoothGatt.class);
    }

    @Test(timeout = 30000)
    public void rssiPollTest() throws Exception
    {
        final BleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "Rssi Poll Tester");
        final Pointer<Integer> counter = new Pointer<>(0);
        device.connect(e -> {
            assertTrue(e.wasSuccess());
            device.startRssiPoll(Interval.ONE_SEC, e1 -> {
                assertTrue(e1.wasSuccess());
                counter.value++;
                if (counter.value >= 5)
                    succeed();
            });
        });
        startAsyncTest();
    }

    @Test(timeout = 30000)
    public void batteryPollTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;

        m_manager.setConfig(m_config);

        final BleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "Battery Poll Tester");
        final Pointer<Integer> counter = new Pointer<>(0);
        device.connect(e -> {
            assertTrue(e.wasSuccess());
            device.startPoll(Uuids.BATTERY_LEVEL, Interval.ONE_SEC, e1 -> {
                assertTrue(e1.status().name(), e1.wasSuccess());
                counter.value++;
                if (counter.value >= 5)
                    succeed();
            });
        });
        startAsyncTest();
    }

    private final class BatteryBluetoothGatt extends UnitTestBluetoothGatt
    {

        public BatteryBluetoothGatt(IBleDevice device)
        {
            super(device, db);
        }

        @Override
        public void sendReadResponse(BleCharacteristic characteristic, byte[] data)
        {
            Random r = new Random();
            int level = r.nextInt(99) + 1;
            super.sendReadResponse(characteristic, new byte[]{(byte) level});
        }
    }
}
