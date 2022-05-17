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
import com.idevicesinc.sweetblue.rx.RxBleDevice;
import com.idevicesinc.sweetblue.rx.RxBleManagerConfig;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Util_Unit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public final class RxBondTest extends RxBaseBleUnitTest
{

    @Test(timeout = 20000)
    public void bondTest() throws Exception
    {
        RxBleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "Test device #1");
        m_disposables.add(device.bond().subscribe(e -> succeed()));
        startAsyncTest();
    }

    @Test(timeout = 20000)
    public void bondRetryTest() throws Exception
    {
        m_config.bluetoothDeviceFactory = device -> new BondFailACoupleTimesLayer(device, 3);

        m_manager.setConfig(m_config);

        RxBleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "Test device #2");
        m_disposables.add(device.bond().subscribe(e -> succeed()));

        startAsyncTest();
    }

    @Test(timeout = 20000)
    public void bondFilterTest() throws Exception
    {
        m_config.bluetoothDeviceFactory = device -> new BondFailACoupleTimesLayer(device, 1);

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

        m_disposables.add(m_manager.observeBondEvents().subscribe(e -> succeed()));

        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test device #3");

        startAsyncTest();
    }


    @Override
    public RxBleManagerConfig getConfig()
    {
        RxBleManagerConfig config = super.getConfig();
        config.loggingOptions = LogOptions.ON;
        return config;
    }


    private final static class BondFailACoupleTimesLayer extends UnitTestBluetoothDevice
    {

        private int m_failsSoFar;
        private final int m_maxFails;


        public BondFailACoupleTimesLayer(IBleDevice device, int maxFails)
        {
            super(device);
            m_maxFails = maxFails;
        }

        @Override
        public boolean createBond()
        {
            if (m_failsSoFar >= m_maxFails)
            {
                return super.createBond();
            }
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
            {
                return super.createBondSneaky(methodName, loggingEnabled);
            }
            else
            {
                m_failsSoFar++;
                Util_Native.bondFail(getBleDevice(), BleStatuses.UNBOND_REASON_REMOTE_DEVICE_DOWN, Interval.millis(250));
            }
            return true;
        }
    }

}
