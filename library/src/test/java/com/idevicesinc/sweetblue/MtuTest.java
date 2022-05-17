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
import com.idevicesinc.sweetblue.utils.Util_Unit;
import com.idevicesinc.sweetblue.utils.Uuids;
import com.idevicesinc.sweetblue.utils.GattDatabase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.Random;
import java.util.UUID;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class MtuTest extends BaseBleUnitTest
{

    @Test(timeout = 10000)
    public void setMtuTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;

        m_manager.setConfig(m_config);

        final BleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "MaTu");

        device.connect(e -> {
            MtuTest.this.assertTrue(e.wasSuccess());
            device.negotiateMtu(100, e1 -> {
                MtuTest.this.assertTrue(e1.wasSuccess());
                MtuTest.this.assertTrue(e1.mtu() == 100);
                MtuTest.this.succeed();
            });
        });

        startAsyncTest();
    }

    @Test(timeout = 20000)
    public void autoNegotiateMtuTest() throws Exception
    {
        m_config.autoNegotiateMtuOnReconnect = true;
        m_config.loggingOptions = LogOptions.ON;
        m_config.defaultDeviceStates = new BleDeviceState[] { BleDeviceState.RECONNECTING_SHORT_TERM, BleDeviceState.CONNECTED };

        m_manager.setConfig(m_config);

        final BleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "MaTu");

        device.connect(e -> {
            MtuTest.this.assertTrue(e.wasSuccess());
            device.negotiateMtu(100, e1 -> {
                MtuTest.this.assertTrue(e1.wasSuccess());
                MtuTest.this.assertTrue(e1.mtu() == 100);
                // Now simulate a disconnect to get sweetblue to reconnect, and auto negotiate the mtu
                Util_Native.setToDisconnected(device, BleStatuses.GATT_ERROR);
            });
        });

        device.setListener_State(e -> {
            if (e.didExit(BleDeviceState.RECONNECTING_SHORT_TERM) && device.is(BleDeviceState.CONNECTED))
            {
                MtuTest.this.assertTrue(device.getMtu() == 100);
                MtuTest.this.succeed();
            }
        });

        startAsyncTest();
    }

    @Test(timeout = 20000)
    public void doNotAutoNegotiateMtuTest() throws Exception
    {
        m_config.autoNegotiateMtuOnReconnect = false;
        m_config.loggingOptions = LogOptions.ON;
        m_config.defaultDeviceStates = new BleDeviceState[] { BleDeviceState.RECONNECTING_SHORT_TERM, BleDeviceState.CONNECTED };

        m_manager.setConfig(m_config);

        final BleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "MaTu");

        device.connect(e -> {
            MtuTest.this.assertTrue(e.wasSuccess());
            device.negotiateMtu(100, e1 -> {
                MtuTest.this.assertTrue(e1.wasSuccess());
                MtuTest.this.assertTrue(e1.mtu() == 100);
                // Now simulate a disconnect to get sweetblue to reconnect, and auto negotiate the mtu
                Util_Native.setToDisconnected(device, BleStatuses.GATT_ERROR);
            });
        });

        device.setListener_State(e -> {
            if (e.didExit(BleDeviceState.RECONNECTING_SHORT_TERM) && device.is(BleDeviceState.CONNECTED))
            {
                MtuTest.this.assertTrue(device.getMtu() == BleDeviceConfig.DEFAULT_MTU_SIZE);
                MtuTest.this.succeed();
            }
        });

        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void doNotTestMTUResultTest() throws Exception
    {

        m_config.loggingOptions = LogOptions.ON;
        m_config.mtuTestCallback = new MtuTestCallback()
        {
            @Override
            public Please onTestRequest(MtuTestEvent event)
            {
                return Please.doNothing();
            }

            @Override
            public void onResult(TestResult result)
            {
                release();
            }
        };

        m_manager.setConfig(m_config);

        BleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "Mtu-inator");

        device.connect(new BleTransaction.Init()
        {
            @Override
            protected void start()
            {
                negotiateMtu(100);
            }
        });

        startAsyncTest();
    }

    @Test(timeout = 25000)
    public void mtuTestFailTest() throws Exception
    {

        final int mtuSize = 100;
        final UUID serviceUuid = Uuids.random();
        final UUID charUuid = Uuids.random();
        final byte[] data = new byte[mtuSize];
        new Random().nextBytes(data);

        final GattDatabase db = new GattDatabase()
                .addService(serviceUuid)
                .addCharacteristic(charUuid).setProperties().readWrite().setPermissions().readWrite().completeService();

        m_config.loggingOptions = LogOptions.ON;

        m_config.gattFactory = device -> new MtuFailBluetoothGatt(device, db);

        m_config.mtuTestCallback = new MtuTestCallback()
        {
            @Override
            public Please onTestRequest(MtuTestEvent event)
            {
                return Please.doWriteTest(serviceUuid, charUuid, data);
            }

            @Override
            public void onResult(TestResult result)
            {
                assertFalse(result.wasSuccess());
                assertTrue(result.result() == TestResult.Result.WRITE_TIMED_OUT);
                release();
            }
        };

        m_manager.setConfig(m_config);

        final BleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "Mtu-inator_the_revenge");

        device.connect(new BleTransaction.Init()
        {
            @Override
            protected void start()
            {
                negotiateMtu(mtuSize);
            }
        });

        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void testMtuSuccessTest() throws Exception
    {

        final int mtuSize = 100;
        final UUID serviceUuid = Uuids.random();
        final UUID charUuid = Uuids.random();
        final byte[] data = new byte[mtuSize];
        new Random().nextBytes(data);

        final GattDatabase db = new GattDatabase()
                .addService(serviceUuid)
                .addCharacteristic(charUuid).setProperties().readWrite().setPermissions().readWrite().completeService();

        m_config.loggingOptions = LogOptions.ON;

        m_config.gattFactory = device -> new UnitTestBluetoothGatt(device, db);

        m_config.mtuTestCallback = new MtuTestCallback()
        {
            @Override
            public Please onTestRequest(MtuTestEvent event)
            {
                return Please.doWriteTest(serviceUuid, charUuid, data);
            }

            @Override
            public void onResult(TestResult result)
            {
                assertTrue(result.wasSuccess());
                release();
            }
        };

        m_manager.setConfig(m_config);

        final BleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "Mtu-inator_the_revenge");

        device.connect(new BleTransaction.Init()
        {
            @Override
            protected void start()
            {
                negotiateMtu(mtuSize);
            }
        });

        startAsyncTest();

    }

    private static class MtuFailBluetoothGatt extends UnitTestBluetoothGatt
    {

        public MtuFailBluetoothGatt(IBleDevice device, GattDatabase db)
        {
            super(device, db);
        }

        @Override
        public boolean writeCharacteristic(BleCharacteristic characteristic)
        {
            return true;
        }
    }

}
