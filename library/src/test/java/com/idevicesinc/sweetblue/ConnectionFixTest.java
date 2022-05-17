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
import com.idevicesinc.sweetblue.internal.P_Bridge_Internal;
import com.idevicesinc.sweetblue.utils.Util_Unit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class ConnectionFixTest extends BaseBleUnitTest
{


    @Test(timeout = 15000)
    public void connectionBugFixTest() throws Exception
    {
        // We're looking for the states to go in this order.
        final List<BleDeviceState> stateList = new ArrayList<>();
        stateList.add(BleDeviceState.BONDING);
        stateList.add(BleDeviceState.BONDED);
        stateList.add(BleDeviceState.UNBONDED);
        stateList.add(BleDeviceState.BONDING);
        stateList.add(BleDeviceState.BONDED);
        // The hack fix calls connect right after bond, so we'll get a state where it exits BLE_DISCONNECTED
        // Adding a null entry here so when we listen for state changes later, we know this one needs to check if the state exited
        // As the fix is a hack, may as well use a hack to test it
        stateList.add(null);
        stateList.add(BleDeviceState.BLE_CONNECTED);
        stateList.add(BleDeviceState.BLE_DISCONNECTED);

        m_config.bluetoothDeviceFactory = ConnectionBugDevice::new;
        m_config.bondFilter = new BleDeviceConfig.DefaultBondFilter()
        {
            @Override
            public ConnectionBugEvent.Please onEvent(ConnectionBugEvent e)
            {
                return ConnectionBugEvent.Please.tryFix();
            }
        };
        m_config.defaultDeviceStates = new BleDeviceState[] { BleDeviceState.UNBONDED, BleDeviceState.BONDING, BleDeviceState.BONDED, BleDeviceState.BLE_CONNECTED, BleDeviceState.BLE_DISCONNECTED };
        m_manager.setConfig(m_config);

        final BleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "Buggie McBugface");

        m_manager.setListener_DeviceState(new DeviceStateListener()
        {
            int currentIndex = 0;

            @Override
            public void onEvent(StateEvent e)
            {
                // We should only ever be seeing state events for one device, as that's all we've "discovered"
                assertThat(e.device(), is(equalTo(device)));
                BleDeviceState expectedState = stateList.get(currentIndex);
                if (expectedState != null)
                    assertThat("Expected " + expectedState.name(), e.didEnter(expectedState), is(true));
                else
                    assertThat("Expected exiting of BLE_DISCONNECTED", e.didExit(BleDeviceState.BLE_DISCONNECTED), is(true));
                currentIndex++;
                if (expectedState == BleDeviceState.BLE_DISCONNECTED)
                    succeed();
            }
        });

        device.bond();
        startAsyncTest();
    }

    @Test(timeout = 20000)
    public void connectionBugFixLoopTest() throws Exception
    {
        // We're looking for the states to go in this order.
        final List<BleDeviceState> stateList = new ArrayList<>();
        stateList.add(BleDeviceState.BONDING);
        stateList.add(BleDeviceState.BONDED);
        stateList.add(BleDeviceState.UNBONDED);
        stateList.add(BleDeviceState.BONDING);
        stateList.add(BleDeviceState.BONDED);
        // The hack fix calls connect right after bond, so we'll get a state where it exits BLE_DISCONNECTED
        // Adding a null entry here so when we listen for state changes later, we know this one needs to check if the state exited
        // As the fix is a hack, may as well use a hack to test it
        stateList.add(null);
        stateList.add(BleDeviceState.BLE_CONNECTED);
        stateList.add(BleDeviceState.BLE_DISCONNECTED);

        m_config.bluetoothDeviceFactory = ConnectionBugBrokenDevice::new;
        m_config.bondFilter = new BleDeviceConfig.DefaultBondFilter()
        {
            @Override
            public ConnectionBugEvent.Please onEvent(ConnectionBugEvent e)
            {
                return ConnectionBugEvent.Please.tryFix();
            }
        };
        m_config.defaultDeviceStates = new BleDeviceState[] { BleDeviceState.UNBONDED, BleDeviceState.BONDING, BleDeviceState.BONDED, BleDeviceState.BLE_CONNECTED, BleDeviceState.BLE_DISCONNECTED };
        m_manager.setConfig(m_config);

        final BleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "Buggie McBugface");

        m_manager.setListener_DeviceState(new DeviceStateListener()
        {
            int currentIndex = 0;

            @Override
            public void onEvent(StateEvent e)
            {
                // We should only ever be seeing state events for one device, as that's all we've "discovered"
                assertThat(e.device(), is(equalTo(device)));

                if (currentIndex < stateList.size())
                {
                    BleDeviceState expectedState = stateList.get(currentIndex);
                    if (expectedState != null)
                        assertThat("Expected " + expectedState.name(), e.didEnter(expectedState), is(true));
                    else
                        assertThat("Expected exiting of BLE_DISCONNECTED", e.didExit(BleDeviceState.BLE_DISCONNECTED), is(true));

                    if (currentIndex == stateList.size() - 1)
                        // If we don't see a state change in 3 seconds, succeed this test
                        P_Bridge_Internal.postUpdateDelayed(m_manager.getIBleManager(), () -> succeed(), 3000);
                }
                else
                {
                    assertEquals("Processed another state unexpectedly. Possible loop condition.", stateList.size(), currentIndex);
                }
                currentIndex++;
            }
        });

        device.bond();
        startAsyncTest();
    }


    private final class ConnectionBugDevice extends UnitTestBluetoothDevice
    {

        private boolean m_hasCheckedBug = false;


        protected ConnectionBugDevice(IBleDevice device)
        {
            super(device);
        }

        @Override
        public boolean isConnected()
        {
            if (!m_hasCheckedBug)
            {
                m_hasCheckedBug = true;
                return getBleDevice().isAny(BleDeviceState.BONDING, BleDeviceState.BONDED) || super.isConnected();
            }
            return super.isConnected();
        }
    }

    private final class ConnectionBugBrokenDevice extends UnitTestBluetoothDevice
    {

        protected ConnectionBugBrokenDevice(IBleDevice device)
        {
            super(device);
        }

        @Override
        public boolean isConnected()
        {
            return getBleDevice().isAny(BleDeviceState.BONDING, BleDeviceState.BONDED) || super.isConnected();
        }
    }


    @Override
    public BleManagerConfig getConfig()
    {
        BleManagerConfig config = super.getConfig();
        config.loggingOptions = LogOptions.ON;
        return config;
    }
}
