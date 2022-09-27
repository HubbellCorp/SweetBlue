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


import android.annotation.SuppressLint;

import com.idevicesinc.sweetblue.defaults.DefaultDeviceReconnectFilter;
import com.idevicesinc.sweetblue.rx.RxBleDevice;
import com.idevicesinc.sweetblue.rx.RxBleTransaction;
import com.idevicesinc.sweetblue.rx.exception.ConnectException;
import com.idevicesinc.sweetblue.utils.GattDatabase;
import com.idevicesinc.sweetblue.utils.Pointer;
import com.idevicesinc.sweetblue.utils.UpdateThreadType;
import com.idevicesinc.sweetblue.utils.Util_Unit;
import com.idevicesinc.sweetblue.utils.Uuids;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.disposables.Disposable;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class RxConnectTest extends RxBaseBleUnitTest
{

    private RxBleDevice m_device;

    private final GattDatabase batteryDb = new GattDatabase().addService(Uuids.BATTERY_SERVICE_UUID)
            .addCharacteristic(Uuids.BATTERY_LEVEL).setProperties().read().setPermissions().read().completeService();


    @Test(timeout = 12000)
    public void connectCreatedDeviceTest() throws Exception
    {
        m_device = null;

        m_manager.setConfig(m_config);

        m_disposables.add(m_manager.observeDiscoveryEvents().subscribe(e ->
        {
            if (e.wasDiscovered() || e.wasRediscovered())
            {
                m_device = e.device();
                m_disposables.add(m_device.connect().subscribe(this::succeed, throwable -> assertTrue("Failed to connect to the device!", false)));
            }
        }));

        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device");

        startAsyncTest();
    }

    @Test(timeout = 40000)
    public void retryConnectionTest() throws Exception
    {
        m_device = null;

        m_config.gattFactory = device -> new ConnectFailBluetoothGatt(device, ConnectFailBluetoothGatt.FailurePoint.POST_CONNECTING_BLE, ConnectFailBluetoothGatt.FailureType.DISCONNECT_GATT_ERROR);
        m_config.defaultDeviceStates = new BleDeviceState[]{BleDeviceState.BLE_DISCONNECTED};

        m_manager.setConfig(m_config);

        final Pointer<Integer> timesTried = new Pointer<>(0);

        m_device = m_manager.newDevice(Util_Unit.randomMacAddress());

        m_disposables.add(
                m_manager.observeDeviceStateEvents().subscribe(e ->
                {
                    if (e.didEnter(BleDeviceState.BLE_DISCONNECTED))
                    {
                        if (timesTried.value < 3)
                        {
                            timesTried.value++;
                            assertTrue(e.device().is(BleDeviceState.RETRYING_BLE_CONNECTION));
                        }
                        else
                        {
                            assertFalse(e.device().is(BleDeviceState.RETRYING_BLE_CONNECTION));
                            m_device = null;
                            succeed();
                        }
                    }
                })
        );

        m_device.setListener_Reconnect(new DefaultDeviceReconnectFilter(3, 3));

        m_disposables.add(
                m_device.connect().subscribe(() -> {}, throwable -> {})
        );

        startAsyncTest();
    }

    @Test(timeout = 12000)
    public void connectDiscoveredDeviceTest() throws Exception
    {
        m_device = null;

        m_config.defaultScanFilter = e -> ScanFilter.Please.acknowledgeIf(e.name_native().equals("Test Device"));

        m_config.updateThreadType = UpdateThreadType.THREAD;
        m_config.loggingOptions = LogOptions.ON;

        m_manager.setConfig(m_config);

        m_disposables.add(m_manager.observeMgrStateEvents().subscribe(e ->
        {
            if (e.didEnter(BleManagerState.SCANNING))
            {
                Util_Native.advertiseNewDevice(m_manager.getBleManager(), -45, "Test Device");
            }
        }));

        m_disposables.add(m_manager.scan(new ScanOptions()).subscribe(e ->
        {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED) || e.was(DiscoveryListener.LifeCycle.REDISCOVERED))
            {
                m_manager.stopScan();
                m_device = e.device();
                m_disposables.add(m_device.connect().subscribe(this::succeed));
            }
        }));

        startAsyncTest();
    }

    @Test(timeout = 30000)
    public void connectDiscoveredMultipleDeviceTest() throws Exception
    {
        m_device = null;

        m_config.updateThreadType = UpdateThreadType.THREAD;
        m_config.defaultScanFilter = e -> ScanFilter.Please.acknowledgeIf(e.name_native().contains("Test Device"));

        m_config.loggingOptions = LogOptions.ON;

        m_manager.setConfig(m_config);

        m_disposables.add(m_manager.observeMgrStateEvents().subscribe(e ->
        {
            if (e.didEnter(BleManagerState.SCANNING))
            {
                Util_Native.advertiseNewDevice(m_manager.getBleManager(), -45, "Test Device #1");
                Util_Native.advertiseNewDevice(m_manager.getBleManager(), -35, "Test Device #2");
                Util_Native.advertiseNewDevice(m_manager.getBleManager(), -60, "Test Device #3");
            }
        }));

        final Pointer<Integer> connected = new Pointer<>(0);

        m_disposables.add(m_manager.scan().subscribe(e ->
        {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED) || e.was(DiscoveryListener.LifeCycle.REDISCOVERED))
            {
                final RxBleDevice device = e.device();
                m_disposables.add(device.connect().subscribe(() ->
                {
                    connected.value++;
                    System.out.println(device.getName_override() + " connected. #" + connected.value);
                    if (connected.value == 3)
                    {
                        succeed();
                    }
                }));
            }
        }));

        startAsyncTest();
    }

    @Test(timeout = 40000)
    public void connectFailTest() throws Exception
    {
        m_device = null;

        m_config.updateThreadType = UpdateThreadType.THREAD;
        m_config.loggingOptions = LogOptions.ON;
        m_config.gattFactory = device -> new ConnectFailBluetoothGatt(device, ConnectFailBluetoothGatt.FailurePoint.POST_CONNECTING_BLE, ConnectFailBluetoothGatt.FailureType.DISCONNECT_GATT_ERROR);

        m_manager.setConfig(m_config);

        m_disposables.add(m_manager.observeDiscoveryEvents().subscribe(e ->
        {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                m_device = e.device();
                m_disposables.add(m_device.connect_withRetries().subscribe(e1 -> succeed(), throwable ->
                {
                    ConnectException ex = (ConnectException) throwable;
                    System.out.println("Connection fail event: " + ex.getEvent().toString());
                    if (ex.getEvent().failureCountSoFar() == 3)
                    {
                        succeed();
                    }
                }));
            }
        }));

        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device");

        startAsyncTest();
    }

    @Test(timeout = 40000)
    public void connectThenFailDiscoverServicesTest() throws Exception
    {
        m_device = null;

        m_config.updateThreadType = UpdateThreadType.THREAD;
        m_config.loggingOptions = LogOptions.ON;
        m_config.gattFactory = device -> new ConnectFailBluetoothGatt(device, ConnectFailBluetoothGatt.FailurePoint.SERVICE_DISCOVERY, ConnectFailBluetoothGatt.FailureType.SERVICE_DISCOVERY_FAILED);

        m_config.connectFailRetryConnectingOverall = true;

        m_manager.setConfig(m_config);

        m_disposables.add(m_manager.observeDiscoveryEvents().subscribe(e ->
        {
            if (e.wasDiscovered())
            {
                m_device = e.device();
                m_disposables.add(m_device.connect_withRetries().subscribe(e1 ->
                {
                    assertFalse("Connection was successful when it shouldn't have been! Fail count: " + e1.failEvent().failureCountSoFar(), e1.wasSuccess());
                    System.out.println("Connection fail event: " + e1.failEvent().toString());
                    assertFalse("Fail count is 3. This assert shouldn't have tripped!", e1.failEvent().failureCountSoFar() == 3);

                }, throwable ->
                {
                    ConnectException ex = (ConnectException) throwable;
                    System.out.println("Connection fail event: " + ex.getEvent().toString());
                    assertTrue("Fail count doesn't match expected count of 3. Fail count: " + ex.getEvent().failureCountSoFar(), ex.getEvent().failureCountSoFar() == 3);
                    succeed();
                }));
            }
        }));

        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device");

        startAsyncTest();
    }

    @Test(timeout = 90000)
    public void connectThenTimeoutThenFailTest() throws Exception
    {
        m_device = null;

        m_config.updateThreadType = UpdateThreadType.THREAD;
        m_config.loggingOptions = LogOptions.ON;
        m_config.gattFactory = device -> new ReadWriteFailBluetoothGatt(device, batteryDb, ReadWriteFailBluetoothGatt.FailType.TIME_OUT);

        m_config.connectFailRetryConnectingOverall = false;

        m_manager.setConfig(m_config);

        final RxBleTransaction.RxInit init = new RxBleTransaction.RxInit()
        {
            @SuppressLint("CheckResult")
            @Override
            protected void start()
            {
                BleRead read = new BleRead(Uuids.BATTERY_SERVICE_UUID, Uuids.BATTERY_LEVEL);
                m_disposables.add(read(read).subscribe(e ->
                {
                    assertFalse("Read was successful! How did this happen?", e.wasSuccess());
                    if (!e.wasSuccess())
                    {
//                            fail();
                    }
                }, throwable -> {}));
                m_disposables.add(read(read).subscribe(e ->
                {
                    assertFalse("Read was successful! How did this happen?", e.wasSuccess());
                    if (!e.wasSuccess())
                    {
                        fail();
                    }
                }, throwable -> fail() ));
            }
        };

        m_disposables.add(m_manager.observeDiscoveryEvents().subscribe(e ->
        {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                m_device = e.device();
                m_disposables.add(m_device.connect(init).subscribe(() ->
                {
                }, throwable ->
                {
                    ConnectException ex = (ConnectException) throwable;
                    assertTrue(ex.getEvent().status() == DeviceReconnectFilter.Status.INITIALIZATION_FAILED);
                    System.out.println("Connection fail event: " + ex.getEvent().toString());
                    succeed();
                }));
            }
        }));

        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device");

        startAsyncTest();
    }

    @Test(timeout = 12000)
    public void disconnectDuringConnectTest() throws Exception
    {
        m_device = null;

        m_config.updateThreadType = UpdateThreadType.THREAD;
        m_config.loggingOptions = LogOptions.ON;
        m_config.gattFactory = UnitTestBluetoothGatt::new;
        m_config.defaultDeviceStates = new BleDeviceState[]{BleDeviceState.BLE_CONNECTING, BleDeviceState.BLE_DISCONNECTED};

        m_manager.setConfig(m_config);

        final AtomicBoolean hasConnected = new AtomicBoolean(false);

        m_disposables.add(m_manager.observeDiscoveryEvents().subscribe(e ->
        {
            if (e.wasDiscovered())
            {
                m_device = e.device();
                Disposable stateDisposable = m_device.observeStateEvents().subscribe(e1 ->
                {
                    System.out.print(e1);
                    if (e1.didEnter(BleDeviceState.BLE_CONNECTING))
                    {
                        hasConnected.set(true);
                        m_device.disconnect();
                    }
                    else if (hasConnected.get() && e1.didEnter(BleDeviceState.BLE_DISCONNECTED))
                    {
                        succeed();
                    }
                });
                m_disposables.add(stateDisposable);
                m_disposables.add(m_device.connect().subscribe(() -> {}, throwable -> {}));
            }
        }));

        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device");
        startAsyncTest();
    }

    @Test(timeout = 12000)
    public void connectThenDisconnectTest() throws Exception
    {
        m_device = null;

        m_config.updateThreadType = UpdateThreadType.THREAD;
        m_config.loggingOptions = LogOptions.ON;
        m_config.defaultDeviceStates = new BleDeviceState[] { BleDeviceState.BLE_CONNECTED, BleDeviceState.BLE_DISCONNECTED };

        m_manager.setConfig(m_config);

        final AtomicBoolean hasConnected = new AtomicBoolean(false);

        m_disposables.add(m_manager.observeDiscoveryEvents().subscribe(e ->
        {
            if (e.wasDiscovered())
            {
                m_device = e.device();
                m_disposables.add(m_device.observeStateEvents().subscribe(e1 ->
                {
                    if (e1.didEnter(BleDeviceState.BLE_CONNECTED))
                    {
                        hasConnected.set(true);
                        m_device.disconnect();
                    }
                    else if (hasConnected.get() && e1.didEnter(BleDeviceState.BLE_DISCONNECTED))
                    {
                        succeed();
                    }
                }));
                m_disposables.add(m_device.connect().subscribe(() -> {}, throwable -> {}));
            }
        }));

        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device");
        startAsyncTest();
    }

}
