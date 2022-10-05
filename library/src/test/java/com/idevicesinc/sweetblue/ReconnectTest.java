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
import com.idevicesinc.sweetblue.defaults.NoReconnectFilter;
import com.idevicesinc.sweetblue.di.SweetDIManager;
import com.idevicesinc.sweetblue.internal.ExecutingHandler;
import com.idevicesinc.sweetblue.internal.IBleDevice;
import com.idevicesinc.sweetblue.internal.P_InternalBridge;
import com.idevicesinc.sweetblue.internal.android.IBluetoothDevice;
import com.idevicesinc.sweetblue.internal.android.IBluetoothGatt;
import com.idevicesinc.sweetblue.utils.GattDatabase;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Util_Unit;
import com.idevicesinc.sweetblue.utils.Uuids;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class ReconnectTest extends BaseBleUnitTest
{

    private final static BleDeviceState[] CONNECTED_STATES = new BleDeviceState[] { BleDeviceState.INITIALIZED, BleDeviceState.CONNECTED, BleDeviceState.DISCOVERED,
            BleDeviceState.SERVICES_DISCOVERED, BleDeviceState.AUTHENTICATED, BleDeviceState.BLE_CONNECTED };
    private final static BleDeviceState[] LONG_TERM_STATES = new BleDeviceState[] { BleDeviceState.BLE_DISCONNECTED, BleDeviceState.ADVERTISING, BleDeviceState.DISCOVERED,
            BleDeviceState.RECONNECTING_LONG_TERM, BleDeviceState.DISCONNECTED };
    private final static BleDeviceState[] DISCONNECTED_STATES = new BleDeviceState[] { BleDeviceState.DISCONNECTED, BleDeviceState.BLE_DISCONNECTED, BleDeviceState.ADVERTISING,
            BleDeviceState.DISCOVERED };

    private final GattDatabase m_db = new GattDatabase().
            addService(Uuids.BATTERY_SERVICE_UUID).addCharacteristic(Uuids.BATTERY_LEVEL).setPermissions().read().setProperties().read().completeService();


    @Test(timeout = 15000)
    public void simpleReconnectShortTermTest() throws Exception
    {

        m_config.loggingOptions = LogOptions.ON;
        m_config.defaultDeviceStates = new BleDeviceState[] { BleDeviceState.RECONNECTING_SHORT_TERM };

        m_manager.setConfig(m_config);

        final BleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "Reconnecter");

        device.connect(e -> {
            assertTrue(e.wasSuccess());

            Util_Native.setToDisconnected(device);

        });

        device.setListener_State(e -> {
            if (e.didExit(BleDeviceState.RECONNECTING_SHORT_TERM))
            {
                assertTrue("Device didn't match short term states! " + device, device.isAll(CONNECTED_STATES));
                succeed();
            }
        });

        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void reconnectDisabledFromMgrConfigTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;
        m_config.defaultDeviceStates = new BleDeviceState[] { BleDeviceState.DISCONNECTED, BleDeviceState.RECONNECTING_LONG_TERM, BleDeviceState.RECONNECTING_SHORT_TERM };
        m_config.reconnectFilter = new NoReconnectFilter();

        m_manager.setConfig(m_config);

        final BleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "Reconnecter");

        device.connect(e -> {
            assertTrue(e.wasSuccess());

            Util_Native.setToDisconnected(device);

        });

        device.setListener_State(e -> {
            assertFalse(e.didEnter(BleDeviceState.RECONNECTING_SHORT_TERM));
            assertFalse(e.didEnter(BleDeviceState.RECONNECTING_LONG_TERM));
            if (e.didEnter(BleDeviceState.DISCONNECTED))
                succeed();
        });

        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void reconnectDisabledFromDeviceConfigTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;
        m_config.defaultDeviceStates = new BleDeviceState[] { BleDeviceState.DISCONNECTED, BleDeviceState.RECONNECTING_LONG_TERM, BleDeviceState.RECONNECTING_SHORT_TERM };
        BleDeviceConfig d_config = new BleDeviceConfig_UnitTest();
        d_config.reconnectFilter = new NoReconnectFilter();

        m_manager.setConfig(m_config);

        final BleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "Reconnecter", d_config);

        device.connect(e -> {
            assertTrue(e.wasSuccess());

            Util_Native.setToDisconnected(device);

        });

        device.setListener_State(e -> {
            assertFalse(e.didEnter(BleDeviceState.RECONNECTING_SHORT_TERM));
            assertFalse(e.didEnter(BleDeviceState.RECONNECTING_LONG_TERM));
            if (e.didEnter(BleDeviceState.DISCONNECTED))
                succeed();
        });

        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void reconnectDisabledFromMgrSetMethodTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;
        m_config.defaultDeviceStates = new BleDeviceState[] { BleDeviceState.DISCONNECTED, BleDeviceState.RECONNECTING_LONG_TERM, BleDeviceState.RECONNECTING_SHORT_TERM };
        DeviceReconnectFilter reconnectFilter = new NoReconnectFilter();

        m_manager.setConfig(m_config);

        m_manager.setListener_DeviceReconnect(reconnectFilter);

        final BleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "Reconnecter");

        device.connect(e -> {
            assertTrue(e.wasSuccess());

            Util_Native.setToDisconnected(device);

        });

        device.setListener_State(e -> {
            assertFalse(e.didEnter(BleDeviceState.RECONNECTING_SHORT_TERM));
            assertFalse(e.didEnter(BleDeviceState.RECONNECTING_LONG_TERM));
            if (e.didEnter(BleDeviceState.DISCONNECTED))
                succeed();
        });

        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void reconnectDisabledFromDeviceSetMethodTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;
        m_config.defaultDeviceStates = new BleDeviceState[] { BleDeviceState.DISCONNECTED, BleDeviceState.RECONNECTING_LONG_TERM, BleDeviceState.RECONNECTING_SHORT_TERM };
        DeviceReconnectFilter reconnectFilter = new NoReconnectFilter();

        m_manager.setConfig(m_config);

        final BleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "Reconnecter");

        device.setListener_Reconnect(reconnectFilter);

        device.connect(e -> {
            assertTrue(e.wasSuccess());

            Util_Native.setToDisconnected(device);

        });

        device.setListener_State(e -> {
            assertFalse(e.didEnter(BleDeviceState.RECONNECTING_SHORT_TERM));
            assertFalse(e.didEnter(BleDeviceState.RECONNECTING_LONG_TERM));
            if (e.didEnter(BleDeviceState.DISCONNECTED))
                succeed();
        });

        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void reconnectShortTermFailTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;
        m_config.defaultDeviceStates = new BleDeviceState[] { BleDeviceState.RECONNECTING_SHORT_TERM };
        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class,
                inputs -> new ShortTermGatt((IBleDevice) inputs[0], 2));

        m_manager.setConfig(m_config);

        final BleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "Reconnecter");

        device.connect(e -> {
            assertTrue(e.wasSuccess());

            Util_Native.setToDisconnected(device);

        });

        device.setListener_State(e -> {
            if (e.didExit(BleDeviceState.RECONNECTING_SHORT_TERM))
            {
                assertTrue("Device didn't match short term states! " + device, device.isAll(CONNECTED_STATES));
                succeed();
            }
        });

        startAsyncTest();
    }

    @Test(timeout = 10000)
    public void ensureConnectFailIsCalledTest() throws Exception
    {
        final int RETRY_COUNT = 3;

        m_config.loggingOptions = LogOptions.ON;
        m_config.reconnectFilter = new DefaultDeviceReconnectFilter(RETRY_COUNT, RETRY_COUNT, Interval.millis(25), Interval.millis(25), Interval.millis(250), Interval.millis(500)) {

            int count = 0;

            @Override
            public ConnectFailPlease onConnectFailed(ConnectFailEvent e)
            {
                assertTrue("Got more failures than expected!", count <= RETRY_COUNT);
                count++;
                // Check against the retry count + 1, so the last retry can be executed
                if (count == RETRY_COUNT + 1)
                {
                    // Wait a couple of seconds before succeeding the task, to ensure we don't get another call when not expected
                    Timer timer = new Timer();
                    TimerTask task = new TimerTask()
                    {
                        @Override
                        public void run()
                        {
                            succeed();
                        }
                    };
                    timer.schedule(task, 2000);
                }
                return super.onConnectFailed(e);
            }
        };
        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, NeverConnectGatt.class);

        m_manager.setConfig(m_config);

        final BleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "ConnectFailTester");

        device.connect();

        startAsyncTest();
    }

    @Test(timeout = 12000)
    public void simpleReconnectLongTermTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;
        m_config.reconnectFilter = new DefaultDeviceReconnectFilter(Interval.millis(25), Interval.millis(25), Interval.millis(250), Interval.millis(500)) {
            @Override
            public ConnectFailPlease onConnectFailed(ConnectFailEvent e)
            {
                return super.onConnectFailed(e);
            }
        };
        m_config.defaultDeviceStates = new BleDeviceState[] { BleDeviceState.RECONNECTING_SHORT_TERM, BleDeviceState.RECONNECTING_LONG_TERM };
        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, inputs -> new ShortTermGatt((IBleDevice) inputs[0], 4));

        m_manager.setConfig(m_config);

        final BleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "Longtermer");

        device.connect(e -> {
            assertTrue(e.wasSuccess());

            Util_Native.setToDisconnected(device);
        });

        device.setListener_State(e -> {
            if (e.didEnter(BleDeviceState.RECONNECTING_LONG_TERM))
            {
                assertTrue("Device didn't match long term states! " + device, device.isAll(LONG_TERM_STATES));
                assertFalse("Device is still in RECONNECTING_SHORT_TERM!", device.is(BleDeviceState.RECONNECTING_SHORT_TERM));
                succeed();
            }
        });

        startAsyncTest();
    }

    @Test(timeout = 12000)
    public void reconnectLongTermTimeoutTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;
        m_config.reconnectFilter = new DefaultDeviceReconnectFilter(Interval.millis(25), Interval.millis(25), Interval.millis(250), Interval.millis(500));
        m_config.defaultDeviceStates = new BleDeviceState[] { BleDeviceState.RECONNECTING_SHORT_TERM, BleDeviceState.RECONNECTING_LONG_TERM };
        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, inputs -> new ShortTermGatt((IBleDevice) inputs[0], 50));

        m_manager.setConfig(m_config);

        final BleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "Longtermer");

        device.connect(e -> {
            assertTrue(e.wasSuccess());

            Util_Native.setToDisconnected(device);
        });

        device.setListener_State(e -> {
            if (e.didEnter(BleDeviceState.RECONNECTING_LONG_TERM))
                assertTrue("Device didn't match long term states! " + device, device.isAll(LONG_TERM_STATES));

            else if (e.didExit(BleDeviceState.RECONNECTING_LONG_TERM))
            {
                assertTrue("Device didn't match disconnected states! " + device, device.isAll(DISCONNECTED_STATES));
                assertFalse("Device is still in a reconnecting state! " + device, device.isAny(BleDeviceState.RECONNECTING_LONG_TERM, BleDeviceState.RECONNECTING_SHORT_TERM));
                succeed();
            }
        });

        startAsyncTest();
    }

    @Test(timeout = 12000)
    public void reconnectLongTermSuccessTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;
        m_config.reconnectFilter = new DefaultDeviceReconnectFilter(Interval.millis(25), Interval.millis(25), Interval.millis(250), Interval.millis(5000));
        m_config.defaultDeviceStates = new BleDeviceState[] { BleDeviceState.RECONNECTING_SHORT_TERM, BleDeviceState.RECONNECTING_LONG_TERM };
        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, inputs -> new ShortTermGatt((IBleDevice) inputs[0], 4));

        m_manager.setConfig(m_config);

        final BleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "Longtermer");

        device.connect(e -> {
            ReconnectTest.this.assertTrue(e.wasSuccess());

            Util_Native.setToDisconnected(device);
        });

        device.setListener_State(e -> {
            if (e.didEnter(BleDeviceState.RECONNECTING_LONG_TERM))
                ReconnectTest.this.assertTrue("Device didn't match long term states! " + device, device.isAll(LONG_TERM_STATES));

            else if (e.didExit(BleDeviceState.RECONNECTING_LONG_TERM))
            {
                ReconnectTest.this.assertTrue("Device didn't match connected states! " + device, device.isAll(CONNECTED_STATES));
                ReconnectTest.this.assertFalse("Device is still in a reconnecting state! " + device, device.isAny(BleDeviceState.RECONNECTING_LONG_TERM, BleDeviceState.RECONNECTING_SHORT_TERM));
                ReconnectTest.this.succeed();
            }
        });

        startAsyncTest();
    }

    @Test(timeout = 12000)
    public void reconnectShortTermWhileExecutingTaskTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;
        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, inputs -> new ReconnectGatt((IBleDevice) inputs[0], m_db));
        m_config.defaultDeviceStates = new BleDeviceState[] { BleDeviceState.CONNECTED };
        m_manager.setConfig(m_config);

        final BleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "ConnectTesterer");
        final AtomicBoolean m_didDisconnect = new AtomicBoolean(false);

        // We're only really doing one read. The first time executing, we're going to simulate a disconnect. Then if we see the
        // task come up again in the executing state, we know it successfully interrupted and re-added to the queue.
        ExecutingHandler handler = ExecutingHandler.newHandler(device.getIBleDevice(), BleTask.READ, () -> {
            if (m_didDisconnect.get())
            {
                // Make sure we actually got reconnected.
                ReconnectTest.this.assertTrue("Read task got re-added to the queue, and executing before getting connected!", device.is(BleDeviceState.CONNECTED));
                ReconnectTest.this.succeed();
            }
            else
            {
                m_didDisconnect.set(true);
                Util_Native.setToDisconnected(device);
            }
        });

        P_InternalBridge.listenForExecutingState(m_manager.getIBleManager(), handler);

        device.connect(e -> {
            ReconnectTest.this.assertTrue(e.wasSuccess());
            BleRead read = new BleRead(Uuids.BATTERY_SERVICE_UUID, Uuids.BATTERY_LEVEL);
            device.read(read);
        });

        startAsyncTest();
    }

    // This is to test that a reconnect attempt does NOT happen, as it shouldn't
    @Test(timeout = 15000)
    public void disconnectRemoteReconnectTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;
        m_config.defaultDeviceStates = new BleDeviceState[] { BleDeviceState.CONNECTED, BleDeviceState.RECONNECTING_SHORT_TERM, BleDeviceState.DISCONNECTED };
        m_manager.setConfig(m_config);

        final BleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "DeezTests");

        device.setListener_State(stateEvent ->
        {
            assertFalse("Entered reconnecting state!", stateEvent.didEnter(BleDeviceState.RECONNECTING_SHORT_TERM));

            if (stateEvent.didEnter(BleDeviceState.DISCONNECTED))
            {
                succeed();
            }
        });
        device.connect(e ->
        {
            assertTrue(e.wasSuccess());
            device.disconnect_remote();
        });

        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void disconnectRemoteWhileConnectingTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;
        m_config.defaultDeviceStates = new BleDeviceState[] { BleDeviceState.CONNECTED, BleDeviceState.RECONNECTING_SHORT_TERM, BleDeviceState.CONNECTING };
        m_config.reconnectFilter = new DefaultDeviceReconnectFilter()
        {
            @Override
            public ConnectFailPlease onConnectFailed(ConnectFailEvent e)
            {
                assertEquals(e.status().ordinal(), Status.ROGUE_DISCONNECT.ordinal());
                succeed();
                return super.onConnectFailed(e);
            }
        };
        m_manager.setConfig(m_config);

        final BleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "DeezTests");

        device.setListener_State(stateEvent ->
        {
            assertFalse("Entered reconnecting state!", stateEvent.didEnter(BleDeviceState.RECONNECTING_SHORT_TERM));
            
            if (stateEvent.didEnter(BleDeviceState.CONNECTING))
            {
                device.disconnect_remote();
            }
        });

        device.connect();

        startAsyncTest();
    }

    /**
     * The purpose of this test is to ensure {@link ReconnectFilter#onConnectionLost(ReconnectFilter.ConnectionLostEvent)} is called twice for a single
     * connection lost event when using {@link DefaultDeviceReconnectFilter}.
     */
    @Test(timeout = 20000)
    public void onConnectLostWithDefaultTest() throws Exception
    {
        final int MAX_CALLS = 2;
        final int delayTime = 2000;
        m_config.loggingOptions = LogOptions.ON;
        m_config.reconnectFilter = new DefaultDeviceReconnectFilter()
        {
            int counter = 0;

            @Override
            public ConnectionLostPlease onConnectionLost(ConnectionLostEvent e)
            {
                if (e.device().is(BleDeviceState.RECONNECTING_LONG_TERM))
                    return ConnectionLostPlease.stopRetrying();

                counter++;
                assertTrue("Got an extra onConnectionLost call than expected", counter <= MAX_CALLS);
                if (counter == MAX_CALLS)
                {
                    Timer timer = new Timer();
                    TimerTask task = new TimerTask()
                    {
                        @Override
                        public void run()
                        {
                            succeed();
                        }
                    };
                    timer.schedule(task, delayTime);
                }

                return super.onConnectionLost(e);
            }
        };

        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, inputs -> new ConnectOnceThenLoseGatt((IBleDevice) inputs[0]));
        m_manager.setConfig(m_config);

        final BleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "Reconnect Tester");

        device.connect(e -> {
            Util_Native.setToDisconnected(device);
        });

        startAsyncTest();
    }

    private final class NeverConnectGatt extends UnitTestBluetoothGatt
    {

        public NeverConnectGatt(IBleDevice device)
        {
            super(device);
        }

        @Override
        public void connect(IBluetoothDevice device, Context context, boolean useAutoConnect, BluetoothGattCallback callback)
        {
            Util_Native.setToDisconnected(getBleDevice(), BleStatuses.GATT_ERROR, Interval.millis(250));
        }

        @Override
        public boolean isGattNull()
        {
            return false;
        }
    }

    private final class ConnectOnceThenLoseGatt extends UnitTestBluetoothGatt
    {

        private boolean m_hasConnected;

        public ConnectOnceThenLoseGatt(IBleDevice device)
        {
            super(device);
        }

        @Override
        public void connect(IBluetoothDevice device, Context context, boolean useAutoConnect, BluetoothGattCallback callback)
        {
            if (!m_hasConnected) {
                m_hasConnected = true;
                super.connect(device, context, useAutoConnect, callback);
            }
        }
    }

    private final class ReconnectGatt extends UnitTestBluetoothGatt
    {

        public ReconnectGatt(IBleDevice device, GattDatabase db)
        {
            super(device, db);
        }


        @Override
        public void sendReadResponse(BleCharacteristic characteristic, byte[] data)
        {
            // Do nothing, we don't want a response coming back in this case, as we're testing reconnecting while a read task is executing.
        }
    }

    private final class ShortTermGatt extends UnitTestBluetoothGatt
    {

        private final int m_failAmount;
        private int m_curFailCount = 0;

        protected ShortTermGatt(IBleDevice device, int failCount)
        {
            super(device);
            m_failAmount = failCount;
        }

        protected ShortTermGatt(IBleDevice device, GattDatabase gattDb, int failCount)
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
