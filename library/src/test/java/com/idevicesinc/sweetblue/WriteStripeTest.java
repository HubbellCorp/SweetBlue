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
import com.idevicesinc.sweetblue.utils.ByteBuffer;
import com.idevicesinc.sweetblue.utils.GattDatabase;
import com.idevicesinc.sweetblue.utils.Util_Unit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Random;
import java.util.UUID;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class WriteStripeTest extends BaseBleUnitTest
{

    private final static UUID tempServiceUuid = UUID.fromString("1234666a-1000-2000-8000-001199334455");
    private final static UUID tempUuid = UUID.fromString("1234666b-1000-2000-8000-001199334455");
    private final static UUID tempDescUuid = UUID.fromString("1234666d-1000-2000-8000-001199334455");


    private BleDevice m_device;

    private GattDatabase db = new GattDatabase().addService(tempServiceUuid)
            .addCharacteristic(tempUuid).setProperties().write().setPermissions().write().build()
            .addDescriptor(tempDescUuid).setPermissions().write().completeService();

    private ByteBuffer m_buffer;


    @Test(timeout = 15000)
    public void stripedWriteTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;

        m_buffer = new ByteBuffer();

        m_manager.setConfig(m_config);

        m_manager.setListener_Discovery(e -> {

            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                m_device = e.device();
                m_device.connect(e1 -> {

                    WriteStripeTest.this.assertTrue(e1.wasSuccess());
                    final byte[] data = new byte[100];
                    new Random().nextBytes(data);
                    final BleWrite bleWrite = new BleWrite(tempUuid).setBytes(data);
                    m_device.write(bleWrite, e11 -> {

                        WriteStripeTest.this.assertTrue(e11.wasSuccess());
                        WriteStripeTest.this.assertArrayEquals(data, m_buffer.bytesAndClear());
                        WriteStripeTest.this.succeed();
                    });
                });
            }
        });

        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device");

        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void stripedWriteDescriptorTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;

        m_buffer = new ByteBuffer();

        m_manager.setConfig(m_config);

        m_manager.setListener_Discovery(e -> {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                m_device = e.device();
                m_device.connect(e1 -> {
                    WriteStripeTest.this.assertTrue(e1.wasSuccess());
                    final byte[] data = new byte[100];
                    new Random().nextBytes(data);
                    final BleDescriptorWrite bleWrite = new BleDescriptorWrite(tempUuid).setDescriptorUUID(tempDescUuid).setBytes(data);
                    m_device.write(bleWrite, e11 -> {
                        WriteStripeTest.this.assertTrue(e11.wasSuccess());
                        WriteStripeTest.this.assertArrayEquals(data, m_buffer.bytesAndClear());
                        WriteStripeTest.this.succeed();
                    });
                });
            }
        });

        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device");

        startAsyncTest();
    }

    @Override
    public void postSetup()
    {
        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, inputs -> new StripeBluetoothGatt(inputs.get(0)));
    }


    private final class StripeBluetoothGatt extends UnitTestBluetoothGatt
    {


        public StripeBluetoothGatt(IBleDevice device)
        {
            super(device, db);
        }

        @Override
        public boolean setCharValue(BleCharacteristic characteristic, byte[] data)
        {
            m_buffer.append(data);
            return super.setCharValue(characteristic, data);
        }

        @Override
        public boolean setDescValue(BleDescriptor descriptor, byte[] data)
        {
            m_buffer.append(data);
            return super.setDescValue(descriptor, data);
        }
    }
}
