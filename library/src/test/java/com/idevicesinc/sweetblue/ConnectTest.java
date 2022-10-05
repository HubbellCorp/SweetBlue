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


import com.idevicesinc.sweetblue.defaults.DefaultDeviceReconnectFilter;
import com.idevicesinc.sweetblue.di.SweetDIManager;
import com.idevicesinc.sweetblue.internal.IBleDevice;
import com.idevicesinc.sweetblue.internal.android.IBluetoothDevice;
import com.idevicesinc.sweetblue.internal.android.IBluetoothGatt;
import com.idevicesinc.sweetblue.utils.GattDatabase;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Pointer;
import com.idevicesinc.sweetblue.utils.UpdateThreadType;
import com.idevicesinc.sweetblue.utils.Util_Unit;
import com.idevicesinc.sweetblue.utils.Uuids;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class ConnectTest extends BaseBleUnitTest
{


    private GattDatabase batteryDb = new GattDatabase().addService(Uuids.BATTERY_SERVICE_UUID)
            .addCharacteristic(Uuids.BATTERY_LEVEL).setProperties().read().setPermissions().read().completeService();


    @Test(timeout = 12000)
    public void connectCreatedDeviceTest() throws Exception
    {
        m_config.updateThreadType = UpdateThreadType.THREAD;

        doConnectCreatedDeviceTest(m_config);

        startAsyncTest();
    }

    @Test(timeout = 12000)
    public void connectCreatedDeviceTest_main() throws Exception
    {
        m_config.updateThreadType = UpdateThreadType.MAIN;

        doConnectCreatedDeviceTest(m_config);
        startAsyncTest();
    }

    private void doConnectCreatedDeviceTest(BleManagerConfig config)
    {
        m_manager.setConfig(config);

        m_manager.setListener_Discovery(e -> {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED) || e.was(DiscoveryListener.LifeCycle.REDISCOVERED))
            {
                e.device().connect(e1 -> {
                    assertTrue(e1.wasSuccess());
                    succeed();
                });
            }
        });

        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device");
    }

    @Test(timeout = 40000)
    public void retryConnectionTest() throws Exception
    {
        m_config.updateThreadType = UpdateThreadType.THREAD;
        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, inputs -> new ConnectFailBluetoothGatt(inputs.get(0), ConnectFailBluetoothGatt.FailurePoint.POST_CONNECTING_BLE, ConnectFailBluetoothGatt.FailureType.DISCONNECT_GATT_ERROR));
        m_config.defaultDeviceStates = new BleDeviceState[] { BleDeviceState.BLE_DISCONNECTED };

        m_manager.setConfig(m_config);

        BleDevice m_device = m_manager.newDevice(Util_Unit.randomMacAddress());

        m_device.setListener_Reconnect(new DefaultDeviceReconnectFilter(3, 3));

        m_device.setListener_State(new DeviceStateListener()
        {
            int timesTried = 0;

            @Override
            public void onEvent(StateEvent e)
            {
                if (e.didEnter(BleDeviceState.BLE_DISCONNECTED))
                {
                    if (timesTried < 3)
                    {
                        timesTried++;
                        assertTrue(e.device().is(BleDeviceState.RETRYING_BLE_CONNECTION));
                    }
                    else
                    {
                        assertFalse(e.device().is(BleDeviceState.RETRYING_BLE_CONNECTION));
                        succeed();
                    }
                }
            }
        });

        m_device.connect();

        startAsyncTest();
    }

    @Test(timeout = 20000)
    public void autoReconnectWhenBleTurnsBackOnTest() throws Exception
    {
        m_config.autoReconnectDeviceWhenBleTurnsBackOn = true;
        m_manager.setConfig(m_config);

        final BleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "Turnonautoconnector");

        device.connect(e -> {
            ConnectTest.this.assertTrue(e.wasSuccess());
            // Simulate BLE getting turned off
            Util_Native.simulateBleTurningOff(m_manager, Interval.millis(150));
            device.setListener_State(e1 -> {
                if (e1.didEnter(BleDeviceState.CONNECTED))
                    succeed();
            });
        });

        m_manager.setListener_State(e -> {
            if (e.didEnter(BleManagerState.OFF))
            {
                Util_Native.simulateBleTurningOn(m_manager, Interval.millis(150));
            }
        });

        startAsyncTest();
    }


    @Test(timeout = 12000)
    public void connectDiscoveredDeviceTest() throws Exception
    {
        m_config.defaultScanFilter = e -> ScanFilter.Please.acknowledgeIf(e.name_native().equals("Test Device"));

        m_config.updateThreadType = UpdateThreadType.THREAD;

        doConnectDiscoveredDeviceTest(m_config);

        startAsyncTest();
    }

    @Test(timeout = 12000)
    public void connectDiscoveredDeviceTest_main() throws Exception
    {
        m_config.defaultScanFilter = e -> ScanFilter.Please.acknowledgeIf(e.name_native().equals("Test Device"));

        m_config.updateThreadType = UpdateThreadType.MAIN;

        doConnectDiscoveredDeviceTest(m_config);
        startAsyncTest();
    }

    private void doConnectDiscoveredDeviceTest(BleManagerConfig config)
    {
        m_manager.setConfig(config);

        m_manager.setListener_Discovery(e -> {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED) || e.was(DiscoveryListener.LifeCycle.REDISCOVERED))
            {
                m_manager.stopScan();
                e.device().connect(e1 -> {
                    ConnectTest.this.assertTrue(e1.wasSuccess());
                    ConnectTest.this.succeed();
                });
            }
        });

        m_manager.setListener_State(e -> {
            if (e.didEnter(BleManagerState.SCANNING))
            {
                Util_Native.advertiseNewDevice(m_manager, -45, "Test Device");
            }
        });

        m_manager.startScan();
    }

    @Test(timeout = 30000)
    public void connectDiscoveredMultipleDeviceTest() throws Exception
    {
        m_config.updateThreadType = UpdateThreadType.THREAD;
        m_config.defaultScanFilter = e -> ScanFilter.Please.acknowledgeIf(e.name_native().contains("Test Device"));


        doConnectDiscoveredMultipleDeviceTest(m_config);
        startAsyncTest();
    }

    @Test(timeout = 30000)
    public void connectDiscoveredMultipleDeviceTest_main() throws Exception
    {
        m_config.updateThreadType = UpdateThreadType.MAIN;
        m_config.defaultScanFilter = e -> ScanFilter.Please.acknowledgeIf(e.name_native().contains("Test Device"));


        doConnectDiscoveredMultipleDeviceTest(m_config);

        startAsyncTest();
    }

    private void doConnectDiscoveredMultipleDeviceTest(BleManagerConfig config)
    {
        m_manager.setConfig(config);

        m_manager.setListener_Discovery(new DiscoveryListener()
        {
            final Pointer<Integer> connected = new Pointer(0);

            @Override
            public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED) || e.was(LifeCycle.REDISCOVERED))
                {
                    e.device().connect(e1 -> {
                        assertTrue(e1.wasSuccess());
                        connected.value++;
                        System.out.println(e1.device().getName_override() + " connected. #" + connected.value);
                        if (connected.value == 3)
                        {
                            succeed();
                        }
                    });
                }
            }
        });

        m_manager.setListener_State(e -> {
            if (e.didEnter(BleManagerState.SCANNING))
            {
                Util_Native.advertiseNewDevice(m_manager, -45, "Test Device #1");
                Util_Native.advertiseNewDevice(m_manager, -35, "Test Device #2");
                Util_Native.advertiseNewDevice(m_manager, -60, "Test Device #3");
            }
        });

        m_manager.startScan();
    }

    @Test(timeout = 40000)
    public void connectFailTest() throws Exception
    {
        m_config.updateThreadType = UpdateThreadType.THREAD;
        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, inputs -> new ConnectFailBluetoothGatt(inputs.get(0), ConnectFailBluetoothGatt.FailurePoint.POST_CONNECTING_BLE, ConnectFailBluetoothGatt.FailureType.DISCONNECT_GATT_ERROR));
        doConnectFailTest(m_config);

        startAsyncTest();
    }

    @Test(timeout = 40000)
    public void connectFailTest_main() throws Exception
    {
        m_config.updateThreadType = UpdateThreadType.MAIN;
        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, inputs -> new ConnectFailBluetoothGatt(inputs.get(0), ConnectFailBluetoothGatt.FailurePoint.POST_CONNECTING_BLE, ConnectFailBluetoothGatt.FailureType.DISCONNECT_GATT_ERROR));
        doConnectFailTest(m_config);
        startAsyncTest();
    }

    private void doConnectFailTest(BleManagerConfig config)
    {
        m_manager.setConfig(config);

        m_manager.setListener_Discovery(e -> {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                e.device().connect(e1 -> {
                    if (e1.wasSuccess())
                    {
                        ConnectTest.this.succeed();
                    }
                    else
                    {
                        System.out.println("Connection fail event: " + e1.failEvent().toString());
                        if (e1.failEvent().failureCountSoFar() == 3)
                        {
                            ConnectTest.this.succeed();
                        }
                    }
                });
            }
        });

        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device");
    }

    @Test(timeout = 40000)
    public void connectFailManagerTest() throws Exception
    {
        m_config.updateThreadType = UpdateThreadType.THREAD;
        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, inputs -> new ConnectFailBluetoothGatt(inputs.get(0), ConnectFailBluetoothGatt.FailurePoint.POST_CONNECTING_BLE, ConnectFailBluetoothGatt.FailureType.DISCONNECT_GATT_ERROR));
        doConnectFailManagerTest(m_config);

        startAsyncTest();
    }

    @Test(timeout = 40000)
    public void connectFailManagerTest_main() throws Exception
    {
        m_config.updateThreadType = UpdateThreadType.MAIN;
        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, inputs -> new ConnectFailBluetoothGatt(inputs.get(0), ConnectFailBluetoothGatt.FailurePoint.POST_CONNECTING_BLE, ConnectFailBluetoothGatt.FailureType.DISCONNECT_GATT_ERROR));
        doConnectFailManagerTest(m_config);
        startAsyncTest();
    }

    private void doConnectFailManagerTest(BleManagerConfig config)
    {
        m_manager.setConfig(config);

        m_manager.setListener_DeviceReconnect(new DefaultDeviceReconnectFilter()
        {
            @Override
            public ConnectFailPlease onConnectFailed(ConnectFailEvent e)
            {
                System.out.println("Connection fail event: " + e.toString());
                if (e.failureCountSoFar() == 3)
                {
                    succeed();
                }
                return super.onConnectFailed(e);
            }
        });

        m_manager.setListener_Discovery(e -> {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                e.device().connect(e1 -> {
                    if (e1.wasSuccess())
                    {
                        ConnectTest.this.succeed();
                    }
                });
            }
        });

        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device");
    }

    @Test(timeout = 40000)
    public void connectThenDisconnectBeforeServiceDiscoveryTest() throws Exception
    {
        m_config.updateThreadType = UpdateThreadType.THREAD;
        m_config.connectFailRetryConnectingOverall = true;
        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, inputs -> new ConnectFailBluetoothGatt(inputs.get(0), ConnectFailBluetoothGatt.FailurePoint.SERVICE_DISCOVERY, ConnectFailBluetoothGatt.FailureType.DISCONNECT_GATT_ERROR));

        doConnectThenDisconnectBeforeServiceDiscoveryTest(m_config);

        startAsyncTest();
    }

    @Test(timeout = 40000)
    public void connectThenDisconnectBeforeServiceDiscoveryTest_main() throws Exception
    {
        m_config.updateThreadType = UpdateThreadType.MAIN;
        m_config.connectFailRetryConnectingOverall = true;
        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, inputs -> new ConnectFailBluetoothGatt(inputs.get(0), ConnectFailBluetoothGatt.FailurePoint.SERVICE_DISCOVERY, ConnectFailBluetoothGatt.FailureType.DISCONNECT_GATT_ERROR));
        doConnectThenDisconnectBeforeServiceDiscoveryTest(m_config);
        startAsyncTest();
    }

    private void doConnectThenDisconnectBeforeServiceDiscoveryTest(BleManagerConfig config)
    {
        m_manager.setConfig(config);

        m_manager.setListener_Discovery(e -> {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                e.device().connect(e1 -> {
                    if (!e1.wasSuccess())
                    {
                        System.out.println("Connection fail event: " + e1.failEvent().toString());
                        if (e1.failEvent().failureCountSoFar() == 3)
                        {
                            ConnectTest.this.succeed();
                        }
                    }
                });
            }
        });

        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device");
    }

    @Test(timeout = 40000)
    public void connectThenFailDiscoverServicesTest() throws Exception
    {
        m_config.updateThreadType = UpdateThreadType.THREAD;
        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, inputs -> new ConnectFailBluetoothGatt(inputs.get(0), ConnectFailBluetoothGatt.FailurePoint.SERVICE_DISCOVERY, ConnectFailBluetoothGatt.FailureType.SERVICE_DISCOVERY_FAILED));
        m_config.connectFailRetryConnectingOverall = true;

        doConnectThenFailDiscoverServicesTest(m_config);

        startAsyncTest();
    }

    @Test(timeout = 40000)
    public void connectThenFailDiscoverServicesTest_main() throws Exception
    {
        m_config.updateThreadType = UpdateThreadType.MAIN;
        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, inputs -> new ConnectFailBluetoothGatt(inputs.get(0), ConnectFailBluetoothGatt.FailurePoint.SERVICE_DISCOVERY, ConnectFailBluetoothGatt.FailureType.SERVICE_DISCOVERY_FAILED));

        m_config.connectFailRetryConnectingOverall = true;

        doConnectThenFailDiscoverServicesTest(m_config);
        startAsyncTest();
    }

    private void doConnectThenFailDiscoverServicesTest(BleManagerConfig config)
    {
        m_manager.setConfig(config);

        m_manager.setListener_Discovery(e -> {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                e.device().connect(e1 -> {
                    if (!e1.wasSuccess())
                    {
                        System.out.println("Connection fail event: " + e1.failEvent().toString());
                        if (e1.failEvent().failureCountSoFar() == 3)
                        {
                            ConnectTest.this.succeed();
                        }
                    }
                });
            }
        });

        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device");
    }

    @Test(timeout = 90000)
    public void connectThenTimeoutThenFailTest() throws Exception
    {
        m_config.updateThreadType = UpdateThreadType.THREAD;
        m_config.taskTimeoutRequestFilter = new BleNodeConfig.DefaultTaskTimeoutRequestFilter()
        {
            @Override
            public Please onEvent(TaskTimeoutRequestEvent e)
            {
                if (e.task() == BleTask.READ)
                    return Please.setTimeoutFor(Interval.secs(2.0));
                else
                    return super.onEvent(e);
            }
        };
        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, inputs -> new ReadWriteFailBluetoothGatt(inputs.get(0), batteryDb, ReadWriteFailBluetoothGatt.FailType.TIME_OUT));

        m_config.connectFailRetryConnectingOverall = false;

        doconnectThenTimeoutThenFailTest(m_config);

        startAsyncTest();
    }

    @Test(timeout = 90000)
    public void connectThenTimeoutThenFailTest_main() throws Exception
    {
        m_config.updateThreadType = UpdateThreadType.MAIN;
        m_config.taskTimeoutRequestFilter = new BleNodeConfig.DefaultTaskTimeoutRequestFilter()
        {
            @Override
            public Please onEvent(TaskTimeoutRequestEvent e)
            {
                if (e.task() == BleTask.READ)
                    return Please.setTimeoutFor(Interval.secs(2.0));
                else
                    return super.onEvent(e);
            }
        };
        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, inputs -> new ReadWriteFailBluetoothGatt(inputs.get(0), batteryDb, ReadWriteFailBluetoothGatt.FailType.TIME_OUT));

        m_config.connectFailRetryConnectingOverall = false;

        doconnectThenTimeoutThenFailTest(m_config);
        startAsyncTest();
    }

    private void doconnectThenTimeoutThenFailTest(BleManagerConfig config)
    {
        m_manager.setConfig(config);

        final BleTransaction.Init init = new BleTransaction.Init()
        {
            @Override
            protected void start()
            {
                BleRead read = new BleRead(Uuids.BATTERY_SERVICE_UUID, Uuids.BATTERY_LEVEL).setReadWriteListener(e -> {
                    assertFalse("Read was successful! How did this happen?", e.wasSuccess());
                    if (!e.wasSuccess())
                    {
//                            fail();
                    }
                });
                read(read);
                read.setReadWriteListener(e -> {
                    assertFalse("Read was successful! How did this happen?", e.wasSuccess());
                    if (!e.wasSuccess())
                    {
                        fail();
                    }
                });
                read(read);
            }
        };

        m_manager.setListener_Discovery(e -> {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                e.device().connect(null, init, e1 -> {
                    if (!e1.wasSuccess())
                    {
                        ConnectTest.this.assertTrue(e1.failEvent().status() == DeviceReconnectFilter.Status.INITIALIZATION_FAILED);
                        System.out.println("Connection fail event: " + e1.toString());
                        ConnectTest.this.succeed();
                    }
                });
            }
        });

        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device");
    }

    @Test(timeout = 40000)
    public void connectThenFailInitTxnTest() throws Exception
    {
        m_config.updateThreadType = UpdateThreadType.THREAD;
        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, inputs -> new ReadWriteFailBluetoothGatt(inputs.get(0), batteryDb, ReadWriteFailBluetoothGatt.FailType.GATT_ERROR));
        m_config.connectFailRetryConnectingOverall = true;

        doConnectThenFailInitTxnTest(m_config);

        startAsyncTest();
    }

    @Test(timeout = 40000)
    public void connectThenFailInitTxnTest_main() throws Exception
    {
        m_config.updateThreadType = UpdateThreadType.MAIN;
        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, inputs -> new ReadWriteFailBluetoothGatt(inputs.get(0), batteryDb, ReadWriteFailBluetoothGatt.FailType.GATT_ERROR));
        m_config.connectFailRetryConnectingOverall = true;

        doConnectThenFailInitTxnTest(m_config);

        startAsyncTest();
    }

    private void doConnectThenFailInitTxnTest(BleManagerConfig config)
    {
        m_manager.setConfig(config);

        final BleTransaction.Init init = new BleTransaction.Init()
        {
            @Override
            protected void start()
            {
                BleRead read = new BleRead(Uuids.BATTERY_SERVICE_UUID, Uuids.BATTERY_LEVEL).setReadWriteListener(e -> {
                    assertFalse("Read was successful! How did this happen?", e.wasSuccess());
                    if (!e.wasSuccess())
                    {
                        fail();
                    }
                });
                read(read);
            }
        };

        m_manager.setListener_Discovery(e -> {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                e.device().connect(null, init, e1 -> {
                    if (!e1.wasSuccess())
                    {
                        System.out.println("Connection fail event: " + e1.failEvent().toString());
                        if (e1.failEvent().failureCountSoFar() == 3)
                        {
                            ConnectTest.this.succeed();
                        }
                    }
                });
            }
        });

        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device");
    }

    @Test(timeout = 12000)
    public void disconnectDuringConnectTest() throws Exception
    {
        m_config.updateThreadType = UpdateThreadType.THREAD;
        m_config.defaultDeviceStates = new BleDeviceState[] { BleDeviceState.BLE_CONNECTING, BleDeviceState.BLE_DISCONNECTED };

        doDisconnectDuringConnectTest(m_config);
        startAsyncTest();
    }

    @Test(timeout = 12000)
    public void disconnectDuringConnectTest_main() throws Exception
    {
        m_config.updateThreadType = UpdateThreadType.MAIN;
        m_config.defaultDeviceStates = new BleDeviceState[] { BleDeviceState.BLE_CONNECTING, BleDeviceState.BLE_DISCONNECTED };

        doDisconnectDuringConnectTest(m_config);
        startAsyncTest();
    }

    private void doDisconnectDuringConnectTest(BleManagerConfig config)
    {
        m_manager.setConfig(config);

        m_manager.setListener_Discovery(new DiscoveryListener()
        {

            boolean hasConnected = false;

            @Override
            public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED))
                {
                    e.device().setListener_State(e1 -> {
                        System.out.print(e1);
                        if (e1.didEnter(BleDeviceState.BLE_CONNECTING))
                        {
                            hasConnected = true;
                            e1.device().disconnect();
                        }
                        else if (hasConnected && e1.didEnter(BleDeviceState.BLE_DISCONNECTED))
                        {
                            succeed();
                        }
                    });
                    e.device().connect();
                }
            }
        });

        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device");
    }

    @Test(timeout = 12000)
    public void disconnectDuringServiceDiscoveryTest() throws Exception
    {
        m_config.updateThreadType = UpdateThreadType.THREAD;
        m_config.defaultDeviceStates = new BleDeviceState[] { BleDeviceState.DISCOVERING_SERVICES, BleDeviceState.BLE_DISCONNECTED };

        doDisconnectDuringServiceDiscoveryTest(m_config);
        startAsyncTest();
    }

    @Test(timeout = 12000)
    public void disconnectDuringServiceDiscoveryTest_main() throws Exception
    {
        m_config.updateThreadType = UpdateThreadType.MAIN;
        m_config.defaultDeviceStates = new BleDeviceState[] { BleDeviceState.DISCOVERING_SERVICES, BleDeviceState.BLE_DISCONNECTED };

        doDisconnectDuringServiceDiscoveryTest(m_config);
        startAsyncTest();
    }

    private void doDisconnectDuringServiceDiscoveryTest(BleManagerConfig config)
    {
        m_manager.setConfig(config);

        m_manager.setListener_Discovery(new DiscoveryListener()
        {

            boolean hasConnected = false;

            @Override
            public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED))
                {
                    e.device().setListener_State(e1 -> {
                        if (e1.didEnter(BleDeviceState.DISCOVERING_SERVICES))
                        {
                            hasConnected = true;
                            e1.device().disconnect();
                        }
                        else if (hasConnected && e1.didEnter(BleDeviceState.BLE_DISCONNECTED))
                        {
                            succeed();
                        }
                    });
                    e.device().connect();
                }
            }
        });

        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device");
    }


    @Test(timeout = 12000)
    public void connectThenDisconnectTest() throws Exception
    {
        m_config.updateThreadType = UpdateThreadType.THREAD;
        m_config.defaultDeviceStates = new BleDeviceState[] { BleDeviceState.BLE_CONNECTED, BleDeviceState.BLE_DISCONNECTED };

        doConnectThenDisconnectTest(m_config);
        startAsyncTest();
    }

    @Test(timeout = 12000)
    public void connectThenDisconnectTest_main() throws Exception
    {
        m_config.updateThreadType = UpdateThreadType.MAIN;
        m_config.defaultDeviceStates = new BleDeviceState[] { BleDeviceState.BLE_CONNECTED, BleDeviceState.BLE_DISCONNECTED };

        doConnectThenDisconnectTest(m_config);
        startAsyncTest();
    }

    private void doConnectThenDisconnectTest(BleManagerConfig config)
    {
        m_manager.setConfig(config);

        m_manager.setListener_Discovery(new DiscoveryListener()
        {

            boolean hasConnected = false;

            @Override
            public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED))
                {
                    e.device().setListener_State(e1 -> {
                        if (e1.didEnter(BleDeviceState.BLE_CONNECTED))
                        {
                            hasConnected = true;
                            e1.device().disconnect();
                        }
                        else if (hasConnected && e1.didEnter(BleDeviceState.BLE_DISCONNECTED))
                        {
                            succeed();
                        }
                    });
                    e.device().connect();
                }
            }
        });

        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device");
    }


    @Override
    public BleManagerConfig getConfig()
    {
        m_config = new BleManagerConfig();
        m_config.bluetoothManagerImplementation = new UnitTestBluetoothManager();
        m_config.logger = new UnitTestLogger();
        m_config.loggingOptions = LogOptions.ON;
        m_config.updateThreadType = UpdateThreadType.THREAD;
        return m_config;
    }

}
