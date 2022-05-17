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
import com.idevicesinc.sweetblue.internal.android.IBluetoothGatt;
import com.idevicesinc.sweetblue.utils.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.UUID;
import com.idevicesinc.sweetblue.utils.GattDatabase;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class GetServiceExceptionTest extends BaseBleUnitTest
{

    private GattDatabase db = new GattDatabase().addService(Uuids.BATTERY_SERVICE_UUID).
            addCharacteristic(Uuids.BATTERY_LEVEL).setPermissions().read().setProperties().read().completeService();

    private GattDatabase db2 = new GattDatabase().addService(Uuids.BATTERY_SERVICE_UUID).
            addCharacteristic(Uuids.BATTERY_LEVEL).setPermissions().read().setProperties().read().build().
            addDescriptor(Uuids.CHARACTERISTIC_USER_DESCRIPTION_DESCRIPTOR_UUID).setValue(new byte[] { 0x05 }).setPermissions().read().completeChar().
            addCharacteristic(Uuids.BATTERY_LEVEL).setPermissions().read().setProperties().read().build().
            addDescriptor(Uuids.CHARACTERISTIC_USER_DESCRIPTION_DESCRIPTOR_UUID).setValue(new byte[] { 0x08 }).setPermissions().read().completeService();

    @Test(timeout = 15000)
    public void getServiceExceptionTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;

        m_manager.setConfig(m_config);

        final BleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "ImaBlowUp");

        device.connect(e -> {
            GetServiceExceptionTest.this.assertTrue(e.wasSuccess());
            BleRead read = new BleRead(Uuids.BATTERY_SERVICE_UUID, Uuids.BATTERY_LEVEL).setReadWriteListener(e1 -> {
                GetServiceExceptionTest.this.assertTrue(e1.status() == ReadWriteListener.Status.GATT_CONCURRENT_EXCEPTION);
                GetServiceExceptionTest.this.succeed();
            });
            device.read(read);
        });

        startAsyncTest();
    }


    @Override
    public IBluetoothGatt getGattLayer(IBleDevice device)
    {
        return new ConcurrentBluetoothGatt(device);
    }

    private class ConcurrentBluetoothGatt extends UnitTestBluetoothGatt
    {

        public ConcurrentBluetoothGatt(IBleDevice device)
        {
            super(device, db);
        }

        @Override
        public BleService getBleService(UUID serviceUuid, LogFunction logger)
        {
            return new BleService(UhOhListener.UhOh.CONCURRENT_EXCEPTION);
        }

    }

}
