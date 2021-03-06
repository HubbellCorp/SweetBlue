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


import com.idevicesinc.sweetblue.internal.P_Bridge_BleManager;
import com.idevicesinc.sweetblue.utils.Interval;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public final class RxManagerStateTest extends RxBaseBleUnitTest
{

    @Test(timeout = 20000)
    public void onToOffTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;

        m_config.bluetoothManagerImplementation = new UnitTestBluetoothManager();

        m_manager.setConfig(m_config);

        assertTrue(m_manager.is(BleManagerState.ON));

        m_disposables.add(m_manager.observeMgrStateEvents().subscribe(e ->
        {
            if (e.didEnter(BleManagerState.TURNING_OFF))
            {
                System.out.println("Bluetooth is turning off...");

                if (e.didEnter(BleManagerState.OFF))
                {
                    succeed();
                }
            }
            else if (e.didEnter(BleManagerState.OFF))
            {
                succeed();
            }
        }));

        m_manager.turnOff();

        startAsyncTest();
    }

    @Test(timeout = 20000)
    public void onToOffToOnTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;

        m_config.bluetoothManagerImplementation = new UnitTestBluetoothManager();

        m_manager.setConfig(m_config);

        assertTrue(m_manager.is(BleManagerState.ON));

        m_disposables.add(m_manager.observeMgrStateEvents().subscribe(e ->
        {
            if (e.didEnter(BleManagerState.TURNING_OFF))
            {
                System.out.println("Bluetooth is turning off...");

                if (e.didEnter(BleManagerState.OFF))
                {
                    m_manager.turnOn();
                }
            }
            else if (e.didEnter(BleManagerState.OFF))
            {
                m_manager.turnOn();
            }
            else if (e.didEnter(BleManagerState.TURNING_ON))
            {
                System.out.println("Bluetooth is turning on...");
            }
            else if (e.didEnter(BleManagerState.ON))
            {
                succeed();
            }
        }));

        m_manager.turnOff();

        startAsyncTest();
    }

    @Test(timeout = 20000)
    public void turningOffToTurningOnTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;

        final DontTurnOffBluetoothManager layer = new DontTurnOffBluetoothManager();
        m_config.bluetoothManagerImplementation = layer;

        m_manager.setConfig(m_config);

        m_disposables.add(m_manager.observeMgrStateEvents().subscribe(e ->
        {
            if (e.didEnter(BleManagerState.TURNING_OFF))
            {
                System.out.println("Bluetooth is turning off...");
                Util_Native.sendBluetoothStateChange(m_manager.getBleManager(), BleStatuses.STATE_TURNING_OFF, BleStatuses.STATE_TURNING_ON);
                Util_Native.sendBluetoothStateChange(m_manager.getBleManager(), BleStatuses.STATE_TURNING_ON, BleStatuses.STATE_ON, Interval.secs(0.5));
            }
            else if (e.didEnter(BleManagerState.TURNING_ON))
            {
                System.out.print("Bluetooth is turning on...");
            }
            else if (e.didEnter(BleManagerState.ON))
            {
                succeed();
            }
        }));

        P_Bridge_BleManager.postUpdateDelayed(m_manager.getBleManager().getIBleManager(), m_manager::turnOff, 50);

        startAsyncTest();
    }

    private static class DontTurnOffBluetoothManager extends UnitTestBluetoothManager
    {
        // Don't turn the state to off, so we stay in the turning off state to test going into turning on/ble turning on from here
        @Override protected void setToOff()
        {
        }
    }

}
