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
import com.idevicesinc.sweetblue.internal.android.IBluetoothGatt;
import com.idevicesinc.sweetblue.rx.RxBleDevice;
import com.idevicesinc.sweetblue.rx.RxBleManagerConfig;
import com.idevicesinc.sweetblue.utils.GattDatabase;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Util_Unit;
import com.idevicesinc.sweetblue.utils.Uuids;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.Arrays;
import java.util.UUID;



@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public final class RxDuplicateCharTest extends RxBaseBleUnitTest
{

    private final static UUID mTestService = Uuids.fromShort("ABCD");
    private final static UUID mTestChar = Uuids.fromShort("1234");
    private final static UUID mTestDesc = Uuids.CHARACTERISTIC_PRESENTATION_FORMAT_DESCRIPTOR_UUID;
    private final static UUID mNotifyDesc = Uuids.CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR_UUID;

    private RxBleDevice m_device;

    private final GattDatabase db = new GattDatabase().addService(mTestService)
            .addCharacteristic(mTestChar).setValue(new byte[]{0x2, 0x3, 0x4, 0x5, 0x6}).setProperties().readWriteNotify().setPermissions().readWrite().build()
            .addDescriptor(mTestDesc).setValue(new byte[]{0x1}).setPermissions().read().completeChar()
            .addCharacteristic(mTestChar).setValue(new byte[]{0x2, 0x3, 0x4, 0x5, 0x6}).setProperties().readWriteNotify().setPermissions().readWrite().build()
            .addDescriptor(mTestDesc).setValue(new byte[]{0x2}).setPermissions().read().completeService();

    private final GattDatabase db2 = new GattDatabase().addService(mTestService)
            .addCharacteristic(mTestChar).setValue(new byte[]{0x2, 0x3, 0x4, 0x5, 0x6}).setProperties().readWriteNotify().setPermissions().readWrite().build()
            .addDescriptor(mNotifyDesc).setPermissions().readWrite().completeChar()
            .addCharacteristic(mTestChar).setValue(new byte[]{0x2, 0x3, 0x4, 0x5, 0x6}).setProperties().readWrite().setPermissions().readWrite().completeService();


    @Test(timeout = 10000)
    public void writeCharWhenMultipleExistTest() throws Exception
    {
        m_device = null;

        m_disposables.add(m_manager.observeDiscoveryEvents().subscribe(e ->
        {
            if (e.wasDiscovered())
            {
                m_device = e.device();
                m_disposables.add(m_device.connect().subscribe(() ->
                {
                    final BleWrite bleWrite = new BleWrite(mTestChar).setBytes(new byte[]{0x0, 0x1, 0x2, 0x3});
                    bleWrite.setDescriptorFilter(new DescriptorFilter()
                    {
                        @Override
                        public Please onEvent(DescriptorEvent event)
                        {
                            return Please.acceptIf(event.value()[0] == 0x2);
                        }

                        @Override
                        public UUID descriptorUuid()
                        {
                            return mTestDesc;
                        }
                    });
                    m_disposables.add(m_device.write(bleWrite).subscribe(e11 ->
                    {
                        assertTrue(e11.status().name(), e11.wasSuccess());
                        assertTrue(e11.characteristic().getDescriptor(mTestDesc).getValue()[0] == 2);
                        succeed();
                    }));
                }));
            }
        }));

        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device");

        startAsyncTest();
    }

    @Test(timeout = 10000)
    public void readCharWhenMultipleExistTest() throws Exception
    {
        m_device = null;

        m_disposables.add(m_manager.observeDiscoveryEvents().subscribe(e ->
        {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                m_device = e.device();
                m_disposables.add(m_device.connect().subscribe(() ->
                {
                    BleRead read = new BleRead(mTestChar).setDescriptorFilter(new DescriptorFilter()
                    {
                        @Override
                        public Please onEvent(DescriptorEvent event)
                        {
                            return Please.acceptIf(event.value()[0] == 0x2);
                        }

                        @Override
                        public UUID descriptorUuid()
                        {
                            return mTestDesc;
                        }
                    });
                    m_disposables.add(m_device.read(read).subscribe(e11 ->
                    {
                        assertTrue(e11.status().name(), e11.wasSuccess());
                        assertTrue(e11.characteristic().getDescriptor(mTestDesc).getValue()[0] == 2);
                        assertTrue("Goal: " + Arrays.toString(new byte[]{0x2, 0x3, 0x4, 0x5, 0x6}) + " Return: " + Arrays.toString(e11.data()), Arrays.equals(e11.data(), new byte[]{0x2, 0x3, 0x4, 0x5, 0x6}));
                        succeed();
                    }));
                }));
            }
        }));

        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device");

        startAsyncTest();

    }

    @Test(timeout = 10000)
    public void readCharWhenMultipleExistOutOfSpecTest() throws Exception
    {
        m_device = null;

        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, args -> new UnitTestBluetoothGatt(args.get(0), db2));

        m_manager.setConfig(m_config);

        m_disposables.add(m_manager.observeDiscoveryEvents().subscribe(e ->
        {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                m_device = e.device();
                m_disposables.add(m_device.connect().subscribe(() ->
                {
                    BleRead read = new BleRead(mTestChar).setDescriptorFilter(new DescriptorFilter()
                    {
                        @Override
                        public Please onEvent(DescriptorEvent event)
                        {
                            // we're looking to read the char withOUT the notification descriptor
                            return Please.acceptIf(event.characteristic().getDescriptor(mNotifyDesc) == null);
                        }

                        @Override
                        public UUID descriptorUuid()
                        {
                            return null;
                        }
                    });
                    m_disposables.add(m_device.read(read).subscribe(e11 ->
                    {
                        assertTrue(e11.status().name(), e11.wasSuccess());
                        assertTrue(e11.characteristic().getDescriptor(mNotifyDesc).isNull());
                        assertTrue(Arrays.equals(e11.data(), new byte[]{0x2, 0x3, 0x4, 0x5, 0x6}));
                        succeed();
                    }));
                }));
            }
        }));

        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device");

        startAsyncTest();

    }

    @Test(timeout = 10000)
    public void enableNotifyMultipleExistTest() throws Exception
    {
        m_device = null;

        m_disposables.add(m_manager.observeDiscoveryEvents().subscribe(e ->
        {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                m_device = e.device();
                m_disposables.add(m_device.connect().subscribe(() ->
                {
                    BleNotify notify = new BleNotify(mTestService, mTestChar).setDescriptorFilter(new DescriptorFilter()
                    {
                        @Override
                        public Please onEvent(DescriptorEvent event)
                        {
                            return Please.acceptIf(event.value()[0] == 0x2);
                        }

                        @Override
                        public UUID descriptorUuid()
                        {
                            return mTestDesc;
                        }
                    }).setForceReadTimeout(Interval.DISABLED);
                    m_disposables.add(m_device.enableNotify(notify).subscribe(e11 ->
                    {
                        if (e11.type() == NotificationListener.Type.ENABLING_NOTIFICATION)
                        {
                            assertTrue(e11.wasSuccess());
                            succeed();
                        }
                    }));
                }));
            }
        }));

        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device");

        startAsyncTest();
    }

    @Override
    public void postSetup()
    {
        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, args -> new UnitTestBluetoothGatt(args.get(0), db));
    }

    @Override
    public RxBleManagerConfig getConfig()
    {
        RxBleManagerConfig config = super.getConfig();
        config.loggingOptions = LogOptions.ON;
        return config;
    }

}
