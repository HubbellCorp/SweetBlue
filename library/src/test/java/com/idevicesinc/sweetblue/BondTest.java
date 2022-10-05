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
import com.idevicesinc.sweetblue.internal.android.IBluetoothDevice;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Pointer;
import com.idevicesinc.sweetblue.utils.Util_Unit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;



@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class BondTest extends BaseBleUnitTest
{


    @Test(timeout = 20000)
    public void bondTest() throws Exception
    {
        BleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "Test device #1");
        device.bond(e -> {
            BondTest.this.assertTrue(e.wasSuccess());
            BondTest.this.succeed();
        });

        startAsyncTest();
    }

    @Test(timeout = 20000)
    public void ephemeralListenerTest() throws Exception
    {
        final BleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "Bonder");
        final Pointer<Integer> counter = new Pointer<>(0);

        device.bond(e ->
        {
            assertTrue("Bond failed!", e.wasSuccess());
            counter.value++;
            assertTrue("Ephemeral listener got called more than once!",counter.value < 2);
            device.unbond(e1 ->
            {
                assertTrue("Failed to unbond!", e1.wasSuccess());
                counter.value++;
                assertTrue("Ephemeral listener got called more than once!",counter.value < 3);
                device.bond(e2 ->
                {
                    assertTrue("Bond failed!", e2.wasSuccess());
                    succeed();
                });
            });
        });
        startAsyncTest();
    }


    @Test(timeout = 20000)
    public void bondRetryTest() throws Exception
    {
        SweetDIManager.getInstance().registerTransient(IBluetoothDevice.class, BondFailACoupleTimesLayer.class);

        m_manager.setConfig(m_config);

        BleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "Test device #2");
        device.bond(e -> {
            BondTest.this.assertTrue(e.wasSuccess());
            BondTest.this.succeed();
        });

        startAsyncTest();
    }

    @Test(timeout = 20000)
    public void bondFilterTest() throws Exception
    {
        SweetDIManager.getInstance().registerTransient(IBluetoothDevice.class, BondFailAfterOnceLayer.class);

        m_config.bondFilter = new BondFilter()
        {
            @Override
            public Please onEvent(StateChangeEvent e)
            {
                return Please.bondIf(e.didEnter(BleDeviceState.DISCOVERED));
            }

            @Override
            public Please onEvent(CharacteristicEvent e)
            {
                return Please.doNothing();
            }

            @Override
            public ConnectionBugEvent.Please onEvent(ConnectionBugEvent e)
            {
                return ConnectionBugEvent.Please.doNothing();
            }
        };

        m_manager.setConfig(m_config);

        m_manager.setListener_Bond(e -> {
            BondTest.this.assertFalse(e.wasSuccess());
            BondTest.this.succeed();
        });

        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test device #3");

        startAsyncTest();
    }

    @Test(timeout = 10000)
    public void bondWhileDisconnectedTest() throws Exception
    {
        m_config.tryBondingWhileDisconnected = true;
        m_config.alwaysBondOnConnect = true;

        m_manager.setConfig(m_config);

        final BleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "Test device #1");
        device.setListener_State(e -> {
            if (e.didEnter(BleDeviceState.BONDED))
                assertFalse(device.is(BleDeviceState.BLE_CONNECTED));
        });
        device.connect(e -> {
            assertTrue(device.is(BleDeviceState.BONDED));
            assertTrue(e.wasSuccess());
            succeed();
        });

        startAsyncTest();
    }

    @Test(timeout = 10000)
    public void bondWhileConnectedTest() throws Exception
    {
        m_config.tryBondingWhileDisconnected = false;
        m_config.alwaysBondOnConnect = true;

        m_manager.setConfig(m_config);

        final BleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "Test device #1");
        device.setListener_State(e -> {
            if (e.didEnter(BleDeviceState.BONDED))
                BondTest.this.assertTrue(device.is(BleDeviceState.BLE_CONNECTED));
        });
        device.connect(e -> {
            BondTest.this.assertTrue(device.is(BleDeviceState.BONDED));
            BondTest.this.assertTrue(e.wasSuccess());
            BondTest.this.succeed();
        });

        startAsyncTest();
    }


    @Override
    public BleManagerConfig getConfig()
    {
        BleManagerConfig config = super.getConfig();
        config.loggingOptions = LogOptions.ON;
        return config;
    }

    private final class BondFailAfterOnceLayer extends BondFailACoupleTimesLayer
    {
        BondFailAfterOnceLayer(IBleDevice bleDevice)
        {
            super(bleDevice, 1);
        }
    }

    private class BondFailACoupleTimesLayer extends UnitTestBluetoothDevice
    {

        private int m_failsSoFar;
        private final int m_maxFails;

        public BondFailACoupleTimesLayer(IBleDevice device)
        {
            this(device, 3);
        }

        public BondFailACoupleTimesLayer(IBleDevice device, int maxFails)
        {
            super(device);
            m_maxFails = maxFails;
        }

        @Override
        public boolean createBond()
        {
            if (m_failsSoFar >= m_maxFails)
                return super.createBond();
            else
            {
                m_failsSoFar++;
                Util_Native.bondFail(getBleDevice(), BleStatuses.UNBOND_REASON_REMOTE_DEVICE_DOWN, Interval.millis(250));
                System.out.println("Failing bond request. Fails so far: " + m_failsSoFar);
            }
            return true;
        }

        @Override
        public boolean createBondSneaky(String methodName, boolean loggingEnabled)
        {
            if (m_failsSoFar >= 2)
                return super.createBondSneaky(methodName, loggingEnabled);
            else
            {
                m_failsSoFar++;
                Util_Native.bondFail(getBleDevice(), BleStatuses.UNBOND_REASON_REMOTE_DEVICE_DOWN, Interval.millis(250));
            }
            return true;
        }
    }
}
