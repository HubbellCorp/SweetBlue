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


import android.bluetooth.BluetoothGattCharacteristic;

import com.idevicesinc.sweetblue.di.SweetDIManager;
import com.idevicesinc.sweetblue.internal.IBleDevice;
import com.idevicesinc.sweetblue.internal.android.IBluetoothGatt;
import com.idevicesinc.sweetblue.utils.GattDatabase;
import com.idevicesinc.sweetblue.utils.UpdateThreadType;
import com.idevicesinc.sweetblue.utils.Util_Unit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.UUID;

import static org.junit.Assert.assertTrue;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class WriteTypeTest extends BaseBleUnitTest
{

    private final static UUID m_WriteService = UUID.randomUUID();
    private final static UUID m_WriteChar = UUID.randomUUID();


    private GattDatabase db = new GattDatabase().addService(m_WriteService)
            .addCharacteristic(m_WriteChar).setProperties().write().write_no_response().signed_write().setPermissions().write().signed_write().completeService();

    @Test(timeout = 15000)
    public void writeNoResponseTest() throws Exception
    {
        doWriteTest(ReadWriteListener.Type.WRITE_NO_RESPONSE, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void writeSignedTest() throws Exception
    {
        doWriteTest(ReadWriteListener.Type.WRITE_SIGNED, BluetoothGattCharacteristic.WRITE_TYPE_SIGNED);
        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void defaultWriteTest() throws Exception
    {
        doWriteTest(ReadWriteListener.Type.WRITE, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        startAsyncTest();
    }


    private void doWriteTest(final ReadWriteListener.Type writeType, final int checkType) throws Exception
    {
        m_manager.setListener_Discovery(e -> {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                e.device().connect(e12 -> {
                    WriteTypeTest.this.assertTrue(e12.wasSuccess());
                    BleWrite write = new BleWrite(m_WriteService, m_WriteChar);
                    write.setBytes(new byte[]{0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9}).setWriteType(writeType)
                            .setReadWriteListener(e1 -> {
                                WriteTypeTest.this.assertTrue(e1.status().name(), e1.wasSuccess());
                                BleCharacteristic ch = e1.characteristic();
                                WriteTypeTest.this.assertTrue(ch.getCharacteristic().getWriteType() == checkType);
                                WriteTypeTest.this.succeed();
                            });
                    e12.device().write(write);
                });
            }
        });

        m_manager.newDevice(Util_Unit.randomMacAddress());
    }

    @Override
    public BleManagerConfig getConfig()
    {
        BleManagerConfig config = super.getConfig();
        m_config.loggingOptions = LogOptions.ON;
        m_config.updateThreadType = UpdateThreadType.THREAD;
        return config;
    }

    @Override
    public void postSetup()
    {
        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, inputs -> new UnitTestBluetoothGatt((IBleDevice) inputs[0], db));
    }
}
