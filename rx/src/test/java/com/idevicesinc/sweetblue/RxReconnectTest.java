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


import android.bluetooth.BluetoothGattCallback;
import android.content.Context;
import com.idevicesinc.sweetblue.defaults.DefaultDeviceReconnectFilter;
import com.idevicesinc.sweetblue.di.SweetDIManager;
import com.idevicesinc.sweetblue.internal.IBleDevice;
import com.idevicesinc.sweetblue.internal.android.IBluetoothDevice;
import com.idevicesinc.sweetblue.internal.android.IBluetoothGatt;
import com.idevicesinc.sweetblue.rx.RxBleDevice;
import com.idevicesinc.sweetblue.utils.GattDatabase;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Util_Unit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class RxReconnectTest extends RxBaseBleUnitTest
{

    private final static BleDeviceState[] CONNECTED_STATES = new BleDeviceState[] { BleDeviceState.INITIALIZED, BleDeviceState.CONNECTED, BleDeviceState.DISCOVERED,
            BleDeviceState.SERVICES_DISCOVERED, BleDeviceState.AUTHENTICATED, BleDeviceState.BLE_CONNECTED };
    private final static BleDeviceState[] LONG_TERM_STATES = new BleDeviceState[] { BleDeviceState.BLE_DISCONNECTED, BleDeviceState.ADVERTISING, BleDeviceState.DISCOVERED,
            BleDeviceState.RECONNECTING_LONG_TERM, BleDeviceState.DISCONNECTED };
    private final static BleDeviceState[] DISCONNECTED_STATES = new BleDeviceState[] { BleDeviceState.DISCONNECTED, BleDeviceState.BLE_DISCONNECTED, BleDeviceState.ADVERTISING,
            BleDeviceState.DISCOVERED };



    @Test(timeout = 15000)
    public void simpleReconnectShortTermTest() throws Exception
    {

        m_config.loggingOptions = LogOptions.ON;
        m_config.defaultDeviceStates = new BleDeviceState[] { BleDeviceState.RECONNECTING_SHORT_TERM };

        m_manager.setConfig(m_config);

        final RxBleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "Reconnecter");

        m_disposables.add(device.connect().subscribe(() ->
        {
            Util_Native.setToDisconnected(device.getBleDevice());
        }));

        m_disposables.add(device.observeStateEvents().subscribe(e ->
        {
            if (e.didExit(BleDeviceState.RECONNECTING_SHORT_TERM))
            {
                assertTrue("Device didn't match short term states! " + device, device.isAll(CONNECTED_STATES));
                succeed();
            }
        }));

        startAsyncTest();
    }

    @Test(timeout = 1500000)
    public void reconnectShortTermFailTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;
        m_config.defaultDeviceStates = new BleDeviceState[] { BleDeviceState.RECONNECTING_SHORT_TERM };

        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, args -> new ShortTermGatt(args.get(0), 2));

        m_manager.setConfig(m_config);

        final RxBleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "Reconnecter");

        m_disposables.add(device.connect().subscribe(() ->
        {
            Util_Native.setToDisconnected(device.getBleDevice());
        }));

        m_disposables.add(device.observeStateEvents().subscribe(e ->
        {
            if (e.didExit(BleDeviceState.RECONNECTING_SHORT_TERM))
            {
                assertTrue("Device didn't match short term states! " + device, device.isAll(CONNECTED_STATES));
                succeed();
            }
        }));

        startAsyncTest();
    }

    @Test(timeout = 12000)
    public void simpleReconnectLongTermTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;
        m_config.reconnectFilter = new DefaultDeviceReconnectFilter(Interval.millis(25), Interval.millis(25), Interval.millis(250), Interval.millis(500));
        m_config.defaultDeviceStates = new BleDeviceState[] { BleDeviceState.RECONNECTING_SHORT_TERM, BleDeviceState.RECONNECTING_LONG_TERM };
        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, args -> new ShortTermGatt(args.get(0), 4));

        m_manager.setConfig(m_config);

        final RxBleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "Longtermer");

        m_disposables.add(device.connect().subscribe(() ->
        {
            Util_Native.setToDisconnected(device.getBleDevice());
        }));

        m_disposables.add(device.observeStateEvents().subscribe(e ->
        {
            if (e.didEnter(BleDeviceState.RECONNECTING_LONG_TERM))
            {
                assertTrue("Device didn't match long term states! " + device, device.isAll(LONG_TERM_STATES));
                assertFalse("Device is still in RECONNECTING_SHORT_TERM!", device.is(BleDeviceState.RECONNECTING_SHORT_TERM));
                succeed();
            }
        }));

        startAsyncTest();
    }

    @Test(timeout = 12000)
    public void reconnectLongTermTimeoutTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;
        m_config.reconnectFilter = new DefaultDeviceReconnectFilter(Interval.millis(25), Interval.millis(25), Interval.millis(250), Interval.millis(50));
        m_config.defaultDeviceStates = new BleDeviceState[] { BleDeviceState.RECONNECTING_SHORT_TERM, BleDeviceState.RECONNECTING_LONG_TERM };
        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, args -> new ShortTermGatt(args.get(0), 4));

        m_manager.setConfig(m_config);

        final RxBleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "Longtermer");

        m_disposables.add(device.connect().subscribe(() ->
        {
            Util_Native.setToDisconnected(device.getBleDevice());
        }));

        m_disposables.add(device.observeStateEvents().subscribe(e ->
        {
            if (e.didEnter(BleDeviceState.RECONNECTING_LONG_TERM))
                assertTrue("Device didn't match long term states! " + device, device.isAll(LONG_TERM_STATES));

            else if (e.didExit(BleDeviceState.RECONNECTING_LONG_TERM))
            {
                assertTrue("Device didn't match disconnected states! " + device, device.isAll(DISCONNECTED_STATES));
                assertFalse("Device is still in a reconnecting state! " + device, device.isAny(BleDeviceState.RECONNECTING_LONG_TERM, BleDeviceState.RECONNECTING_SHORT_TERM));
                succeed();
            }
        }));

        startAsyncTest();
    }

    @Test(timeout = 12000)
    public void reconnectLongTermSuccessTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;
        m_config.reconnectFilter = new DefaultDeviceReconnectFilter(Interval.millis(25), Interval.millis(25), Interval.millis(250), Interval.millis(5000));
        m_config.defaultDeviceStates = new BleDeviceState[] { BleDeviceState.RECONNECTING_SHORT_TERM, BleDeviceState.RECONNECTING_LONG_TERM };
        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, args -> new ShortTermGatt(args.get(0), 4));

        m_manager.setConfig(m_config);

        final RxBleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "Longtermer");

        m_disposables.add(device.connect().subscribe(() ->
        {
            Util_Native.setToDisconnected(device.getBleDevice());
        }));

        m_disposables.add(device.observeStateEvents().subscribe(e ->
        {
            if (e.didEnter(BleDeviceState.RECONNECTING_LONG_TERM))
                assertTrue("Device didn't match long term states! " + device, device.isAll(LONG_TERM_STATES));

            else if (e.didExit(BleDeviceState.RECONNECTING_LONG_TERM))
            {
                assertTrue("Device didn't match connected states! " + device, device.isAll(CONNECTED_STATES));
                assertFalse("Device is still in a reconnecting state! " + device, device.isAny(BleDeviceState.RECONNECTING_LONG_TERM, BleDeviceState.RECONNECTING_SHORT_TERM));
                succeed();
            }
        }));

        startAsyncTest();
    }


    private final static class ShortTermGatt extends UnitTestBluetoothGatt
    {

        private final int m_failAmount;
        private int m_curFailCount = 0;

        private ShortTermGatt(IBleDevice device, int failCount)
        {
            super(device);
            m_failAmount = failCount;
        }

        private ShortTermGatt(IBleDevice device, GattDatabase gattDb, int failCount)
        {
            super(device, gattDb);
            m_failAmount = failCount;
        }

        @Override public void connect(IBluetoothDevice device, Context context, boolean useAutoConnect, BluetoothGattCallback callback)
        {
            if (getBleDevice().isAny(BleDeviceState.RECONNECTING_SHORT_TERM, BleDeviceState.RECONNECTING_LONG_TERM) && m_curFailCount < m_failAmount)
            {
                m_curFailCount++;
                Util_Native.setToDisconnected(getBleDevice());
            }
            else
                super.connect(device, context, useAutoConnect, callback);
        }
    }

}
