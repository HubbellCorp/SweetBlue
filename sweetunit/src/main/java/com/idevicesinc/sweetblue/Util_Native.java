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


import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.ScanRecord;
import android.content.Intent;
import android.os.Build;
import com.idevicesinc.sweetblue.compat.L_Util;
import com.idevicesinc.sweetblue.compat.P_Bridge_Compat;
import com.idevicesinc.sweetblue.internal.IBleDevice;
import com.idevicesinc.sweetblue.internal.IBleManager;
import com.idevicesinc.sweetblue.internal.P_Bridge_BleDevice;
import com.idevicesinc.sweetblue.internal.P_Bridge_BleServer;
import com.idevicesinc.sweetblue.internal.P_Bridge_Internal;
import com.idevicesinc.sweetblue.internal.P_Bridge_BleManager;
import com.idevicesinc.sweetblue.internal.android.AdapterConst;
import com.idevicesinc.sweetblue.internal.android.P_DeviceHolder;
import com.idevicesinc.sweetblue.internal.android.P_GattHolder;
import com.idevicesinc.sweetblue.internal.android.ProfileConst;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Phy;
import com.idevicesinc.sweetblue.utils.Util_Unit;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.Utils_ScanRecord;

import java.util.List;
import java.util.UUID;

/**
 * Utility class for simulating Bluetooth operations (read/writes, notifications, etc). When unit testing, you will need to use this class
 * to simulate bluetooth operations. {@link UnitTestBluetoothGatt}, {@link UnitTestBluetoothDevice}, and {@link UnitTestBluetoothManager} use this class. If you are implementing your own
 * version of those classes, you will need to use this class to simulate the native callbacks.
 * <p>
 * This is not in the utils package as it accesses several package private methods.
 */
public final class Util_Native
{

    private static int m_requestId = 0;

    private static int nextRequestId()
    {
        return ++m_requestId;
    }

    private Util_Native()
    {
    }

    /**
     * Overload of {@link #sendBluetoothStateChange(BleManager, int, int, Interval)}, with {@link Interval#ZERO} delay.
     */
    public static void sendBluetoothStateChange(BleManager manager, int previousState, int newState)
    {
        sendBluetoothStateChange(manager, previousState, newState, Interval.ZERO);
    }

    /**
     * Sends a bluetooth state change, such as {@link BluetoothAdapter#STATE_ON}, {@link BluetoothAdapter#STATE_OFF}, etc.
     */
    public static void sendBluetoothStateChange(BleManager manager, int previousState, int newState, Interval delay)
    {
        final IBleManager mgr = manager.getIBleManager();
        P_Bridge_BleManager.postUpdateDelayed(mgr, () ->
        {
            ((UnitTestBluetoothManager) manager.getIBleManager().managerLayer()).manuallySetState(newState);
            Intent intent = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
            intent.putExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, previousState);
            intent.putExtra(BluetoothAdapter.EXTRA_STATE, newState);
            P_Bridge_BleManager.onNativeBleStateChangeFromBroadcastReceiver(mgr, null, intent);

        }, delay.millis());
    }

    /**
     * Overload of {@link #simulateBleTurningOn(BleManager, Interval, Interval)} with no start delay
     */
    public static void simulateBleTurningOn(BleManager manager, Interval delay)
    {
        simulateBleTurningOn(manager, Interval.ZERO, delay);
    }

    public static void forceOn(BleManager manager)
    {
        P_Bridge_BleManager.forceOn(manager.getIBleManager());
    }

    /**
     * Convenience method to simulate BLE being turned on. This just calls {@link #sendBluetoothStateChange(BleManager, int, int, Interval)} twice.
     * Once to set the state to {@link AdapterConst#STATE_TURNING_ON}, then again to {@link AdapterConst#STATE_ON}. The first call is made after the given startDelay,
     * whereas the second is delayed by the amount given. If the given {@link BleManager} reports it's already {@link BleManagerState#ON}, then this method
     * will do nothing.
     */
    public static void simulateBleTurningOn(BleManager manager, Interval startDelay, Interval delay)
    {
        P_Bridge_BleManager.postUpdateDelayed(manager.getIBleManager(), () ->
        {
            if (manager.is(BleManagerState.ON))
                return;
            sendBluetoothStateChange(manager, AdapterConst.STATE_OFF, AdapterConst.STATE_TURNING_ON);
            sendBluetoothStateChange(manager, AdapterConst.STATE_TURNING_ON, AdapterConst.STATE_ON, delay);

        }, startDelay.millis());

    }

    /**
     * Overload of {@link #simulateBleTurningOff(BleManager, Interval, Interval)}, with no start delay
     */
    public static void simulateBleTurningOff(BleManager manager, Interval delay)
    {
        simulateBleTurningOff(manager, Interval.ZERO, delay);
    }

    public static void simulateBleTurningOff(BleManager manager, Interval startDelay, Interval delay)
    {
        P_Bridge_BleManager.postUpdateDelayed(manager.getIBleManager(), () ->
        {
            if (manager.is(BleManagerState.OFF))
                return;
            sendBluetoothStateChange(manager, AdapterConst.STATE_ON, AdapterConst.STATE_TURNING_OFF, startDelay);
            sendBluetoothStateChange(manager, AdapterConst.STATE_TURNING_OFF, AdapterConst.STATE_OFF, delay);

        }, startDelay.millis());

    }

    /**
     * Overload of {@link #bondSuccess(BleDevice, Interval)} which delays the callback by 50ms.
     */
    public static void bondSuccess(BleDevice device)
    {
        bondSuccess(device, Interval.millis(50));
    }

    /**
     * Send the callback that a bond was successful, and delays the callback by the amount of time specified
     */
    public static void bondSuccess(final BleDevice device, Interval delay)
    {
        setBondStatusSuccess(device, BleStatuses.DEVICE_BOND_BONDED, delay);
    }

    public static void unbondSuccess(final BleDevice device, Interval delay)
    {
        setBondStatusSuccess(device, BleStatuses.DEVICE_BOND_UNBONDED, delay);
    }

    /**
     * Allows you to set a new bonding state, and pump it through the system as if it's a native callback.
     */
    public static void setBondStatusSuccess(final BleDevice device, int newState, Interval delay)
    {
        final IBleManager mgr = fromDevice(device);
        P_Bridge_BleManager.postUpdateDelayed(mgr, () -> {
            int oldState;
            if (device.is(BleDeviceState.UNBONDED))
            {
                oldState = BluetoothDevice.BOND_NONE;
            }
            else if (device.is(BleDeviceState.BONDING))
            {
                oldState = BluetoothDevice.BOND_BONDING;
            }
            else
            {
                oldState = BluetoothDevice.BOND_BONDED;
            }

            P_Bridge_BleManager.onNativeBondStateChanged(mgr, mgr.getConfigClone().newDeviceLayer(device.getIBleDevice()), oldState, newState, 0);
        }, delay.millis());
    }

    /**
     * Overload of {@link #bondFail(BleDevice, int, Interval)} which delays the callback by 50ms.
     */
    public static void bondFail(BleDevice device, int failReason)
    {
        bondFail(device, failReason, Interval.millis(50));
    }

    /**
     * Send a callback that a bond has failed with the provided reason..something like {@link BleStatuses#UNBOND_REASON_AUTH_FAILED}, or {@link BleStatuses#UNBOND_REASON_REMOTE_DEVICE_DOWN}, and
     * delays the callback by the amount specified.
     */
    public static void bondFail(final BleDevice device, final int failReason, Interval delay)
    {
        final IBleManager mgr = fromDevice(device);
        P_Bridge_BleManager.postUpdateDelayed(mgr, () -> {
            int oldState;
            if (device.is(BleDeviceState.UNBONDED))
            {
                oldState = BluetoothDevice.BOND_NONE;
            }
            else if (device.is(BleDeviceState.BONDING))
            {
                oldState = BluetoothDevice.BOND_BONDING;
            }
            else
            {
                oldState = BluetoothDevice.BOND_BONDED;
            }
            P_Bridge_BleManager.onNativeBondStateChanged(mgr, mgr.getConfigClone().newDeviceLayer(device.getIBleDevice()), oldState, BluetoothDevice.BOND_NONE, failReason);
        }, delay.millis());
    }

    /**
     * Overload of {@link #readError(BleDevice, BleCharacteristic, int, Interval)} which delays the callback by 50ms.
     */
    public static void readError(BleDevice device, BleCharacteristic characteristic, int gattStatus)
    {
        readError(device, characteristic, gattStatus, Interval.millis(50));
    }

    /**
     * Send a callback that a read has failed, with the gattStatus provided, for instance {@link BleStatuses#GATT_ERROR}, which delays the callback by the amount specified.
     */
    public static void readError(final BleDevice device, final BleCharacteristic characteristic, final int gattStatus, Interval delay)
    {
        P_Bridge_BleManager.postUpdateDelayed(fromDevice(device), () ->
                {
                    P_Bridge_BleDevice.onCharacteristicRead(device.getIBleDevice(), P_GattHolder.NULL, characteristic, gattStatus);
                }
        , delay.millis());
    }

    /**
     * Overload of {@link #readSuccess(BleDevice, BleCharacteristic, byte[], Interval)} which delays the callback by 50ms.
     */
    public static void readSuccess(final BleDevice device, final BleCharacteristic characteristic, final byte[] data)
    {
        readSuccess(device, characteristic, data, Interval.millis(50));
    }

    /**
     * Send a callback that a read was successful, with the data to send back from the read, and delays the callback by the amount specified.
     */
    public static void readSuccess(final BleDevice device, final BleCharacteristic characteristic, final byte[] data, Interval delay)
    {
        P_Bridge_BleManager.postUpdateDelayed(fromDevice(device), () -> {
            characteristic.setValue(data);
            P_Bridge_BleDevice.onCharacteristicRead(device.getIBleDevice(), P_GattHolder.NULL, characteristic, BleStatuses.GATT_SUCCESS);
        }, delay.millis());
    }

    /**
     * Overload of {@link #readDescSuccess(BleDevice, BleDescriptor, byte[], Interval)} which delays the callback by 50ms.
     */
    public static void readDescSuccess(final BleDevice device, final BleDescriptor descriptor, final byte[] data)
    {
        readDescSuccess(device, descriptor, data, Interval.millis(50));
    }

    /**
     * Send a callback that a descriptor read was successful, with the data to return, and delays the callback by the amount specified.
     */
    public static void readDescSuccess(final BleDevice device, final BleDescriptor descriptor, final byte[] data, Interval delay)
    {
        P_Bridge_BleManager.postUpdateDelayed(fromDevice(device), () -> {
            descriptor.setValue(data);
            P_Bridge_BleDevice.onDescriptorRead(device.getIBleDevice(), P_GattHolder.NULL, descriptor, BleStatuses.GATT_SUCCESS);
        }, delay.millis());
    }

    /**
     * Overload of {@link #readDescError(BleDevice, BleDescriptor, int, long)} which delays the callback by 50ms.
     */
    public static void readDescError(final BleDevice device, final BleDescriptor descriptor, final int gattStatus)
    {
        readDescError(device, descriptor, gattStatus, 50);
    }

    /**
     * Send a callback that a descriptor read failed with the given gattStatus, and delays the callback by the amount specified.
     */
    public static void readDescError(final BleDevice device, final BleDescriptor descriptor, final int gattStatus, long delay)
    {
        P_Bridge_BleManager.postUpdateDelayed(fromDevice(device), () -> {
            P_Bridge_BleDevice.onDescriptorRead(device.getIBleDevice(), P_GattHolder.NULL, descriptor, gattStatus);
        }, delay);
    }

    /**
     * Overload of {@link #writeDescSuccess(BleDevice, BleDescriptor, Interval)} which delays the callback by 50ms.
     */
    public static void writeDescSuccess(final BleDevice device, final BleDescriptor descriptor)
    {
        writeDescSuccess(device, descriptor, Interval.millis(50));
    }

    /**
     * Send a callback that a descriptor write suceeded, and delay the callback by the amount specified.
     */
    public static void writeDescSuccess(final BleDevice device, final BleDescriptor descriptor, Interval delay)
    {
        P_Bridge_BleManager.postUpdateDelayed(fromDevice(device), () -> {
            P_Bridge_BleDevice.onDescriptorWrite(device.getIBleDevice(), P_GattHolder.NULL, descriptor, BleStatuses.GATT_SUCCESS);
        }, delay.millis());
    }

    /**
     * Overload of {@link #writeDescError(BleDevice, BleDescriptor, int, Interval)} which delays the callback by 50ms.
     */
    public static void writeDescError(final BleDevice device, final BleDescriptor descriptor, final int gattStatus)
    {
        writeDescError(device, descriptor, gattStatus, Interval.millis(50));
    }

    /**
     * Send a callback that a descriptor write failed, with the given gattStatus, and delay the callback by the amount specified.
     */
    public static void writeDescError(final BleDevice device, final BleDescriptor descriptor, final int gattStatus, Interval delay)
    {
        P_Bridge_BleManager.postUpdateDelayed(fromDevice(device), () -> {
            P_Bridge_BleDevice.onDescriptorWrite(device.getIBleDevice(), P_GattHolder.NULL, descriptor, gattStatus);
        }, delay.millis());
    }

    /**
     * Overload of {@link #writeSuccess(BleDevice, BleCharacteristic, Interval)} which delays the callback by 50ms.
     */
    public static void writeSuccess(final BleDevice device, final BleCharacteristic characteristic)
    {
        writeSuccess(device, characteristic, Interval.millis(50));
    }

    /**
     * Send a callback that a write succeeded, and delay the callback by the amount specified.
     */
    public static void writeSuccess(final BleDevice device, final BleCharacteristic characteristic, Interval delay)
    {
        P_Bridge_BleManager.postUpdateDelayed(fromDevice(device), () -> {
            P_Bridge_BleDevice.onCharacteristicWrite(device.getIBleDevice(), P_GattHolder.NULL, characteristic, BleStatuses.GATT_SUCCESS);
        }, delay.millis());
    }

    /**
     * Overload of {@link #writeError(BleDevice, BleCharacteristic, int, Interval)} which delays the callback by 50ms.
     */
    public static void writeError(final BleDevice device, final BleCharacteristic characteristic, final int gattStatus)
    {
        writeError(device, characteristic, gattStatus, Interval.millis(50));
    }

    /**
     * Send a callback that a write failed, with the given gattStatus, and delay the callback by the amount specified.
     */
    public static void writeError(final BleDevice device, final BleCharacteristic characteristic, final int gattStatus, Interval delay)
    {
        P_Bridge_BleManager.postUpdateDelayed(fromDevice(device), () -> {
            P_Bridge_BleDevice.onCharacteristicWrite(device.getIBleDevice(), P_GattHolder.NULL, characteristic, gattStatus);
        }, delay.millis());
    }

    /**
     * Send a callback to a server instance, mimicking a peripheral writing to the server.
     */
    public static void sendWriteToServer(final BleServer server, final String macAddress, final UUID characteristicUuid, final byte[] value, Interval delay)
    {
        final BleCharacteristic ch = server.getNativeBleCharacteristic(characteristicUuid);
        P_Bridge_BleManager.postUpdateDelayed(fromServer(server), () ->
        {
            P_Bridge_BleServer.onCharacteristicWriteRequest(server.getIBleServer(), P_DeviceHolder.newNullHolder(macAddress), nextRequestId(), ch, false, true, 0, value);
        }, delay.millis());
    }

    public static void readFromServer(final BleServer server, final String macAddress, final UUID characteristicUuid, Interval delay)
    {
        final BleCharacteristic ch = server.getNativeBleCharacteristic(characteristicUuid);
        P_Bridge_BleManager.postUpdateDelayed(fromServer(server), () ->
        {
            P_Bridge_BleServer.onCharacteristicReadRequesst(server.getIBleServer(), P_DeviceHolder.newNullHolder(macAddress), nextRequestId(), 0, ch);
        }, delay.millis());
    }

    public static void addServiceSuccess(final BleServer server, final BleService service, Interval delay)
    {
        P_Bridge_BleManager.postUpdateDelayed(fromServer(server), () ->
        {
            P_Bridge_BleServer.onServiceAdded(server.getIBleServer(), BleStatuses.GATT_SUCCESS, service);
        }, delay.millis());
    }

    /**
     * Overload of {@link #requestMTUSuccess(BleDevice, int, Interval)} which delays the callback by 50ms.
     */
    public static void requestMTUSuccess(final BleDevice device, final int mtu)
    {
        requestMTUSuccess(device, mtu, Interval.millis(50));
    }

    /**
     * Send a callback that says an MTU request was successful, with the newly negotiated mtu size, and delay the callback by the amount specified.
     */
    public static void requestMTUSuccess(final BleDevice device, final int mtu, Interval delay)
    {
        P_Bridge_BleManager.postUpdateDelayed(fromDevice(device), () -> {
            P_Bridge_BleDevice.onMtuChanged(device.getIBleDevice(), P_GattHolder.NULL, mtu, BleStatuses.GATT_SUCCESS);
        }, delay.millis());
    }

    /**
     * Overload of {@link #requestMTUError(BleDevice, int, int, Interval)} which delays the callback by 50ms.
     */
    public static void requestMTUError(final BleDevice device, final int mtu, final int gattStatus)
    {
        requestMTUError(device, mtu, gattStatus, Interval.millis(50));
    }

    /**
     * Send a callback that says an MTU request failed, with the given gattStatus, and delay the callback by the amount specified.
     */
    public static void requestMTUError(final BleDevice device, final int mtu, final int gattStatus, Interval delay)
    {
        P_Bridge_BleManager.postUpdateDelayed(fromDevice(device), () -> {
            P_Bridge_BleDevice.onMtuChanged(device.getIBleDevice(), P_GattHolder.NULL, mtu, gattStatus);
        }, delay.millis());
    }

    /**
     * Send a callback that says a request to change the physical layer (bluetooth 5 feature) was successful, with the given {@link Phy}, and
     * the amount of time to delay before the callback is sent.
     */
    public static void setPhySuccess(final BleDevice device, final Phy phyOptions, Interval delay)
    {
        P_Bridge_BleManager.postUpdateDelayed(fromDevice(device), () -> {
            P_Bridge_BleDevice.onPhyUpdate(device.getIBleDevice(), P_GattHolder.NULL, phyOptions.getTxMask(), phyOptions.getRxMask(), BleStatuses.GATT_SUCCESS);
        }, delay.millis());
    }

    /**
     * Overload of {@link #setPhyFailure(BleDevice, Phy, int, Interval)}, which has a delay of 50ms.
     */
    public static void setPhyFailure(final BleDevice device, final Phy phyOptions, int gattStatus)
    {
        setPhyFailure(device, phyOptions, gattStatus, Interval.millis(50));
    }

    /**
     * Send a callback that says a request to change the physical layer (bluetooth 5 feature) failed with the given gatt status code
     */
    public static void setPhyFailure(final BleDevice device, final Phy phyOptions, final int gattStatus, Interval delay)
    {
        P_Bridge_BleManager.postUpdateDelayed(fromDevice(device), () -> {
            P_Bridge_BleDevice.onPhyUpdate(device.getIBleDevice(), P_GattHolder.NULL, phyOptions.getTxMask(), phyOptions.getRxMask(), gattStatus);
        }, delay.millis());
    }

    /**
     * Send a callback that says a request to read the physical layer (bluetooth 5 feature) was successful, with the given {@link Phy}, and
     * the amount of time to delay before the callback is sent.
     */
    public static void readPhySuccess(final BleDevice device, final Phy phyOption, Interval delay)
    {
        P_Bridge_BleManager.postUpdateDelayed(fromDevice(device), () -> {
            P_Bridge_BleDevice.onPhyRead(device.getIBleDevice(), P_GattHolder.NULL, phyOption.getTxMask(), phyOption.getRxMask(), BleStatuses.GATT_SUCCESS);
        }, delay.millis());
    }

    /**
     * Overload of {@link #readPhyFailure(BleDevice, Phy, int, Interval)} , which has a delay of 50ms.
     */
    public static void readPhyFailure(final BleDevice device, final Phy phyOption, final int gattStatus)
    {
        readPhyFailure(device, phyOption, gattStatus, Interval.millis(50));
    }

    /**
     * Send a callback that says a request to read the physical layer (bluetooth 5 feature) failed with the given gatt status code
     */
    public static void readPhyFailure(final BleDevice device, final Phy phyOption, final int gattStatus, Interval delay)
    {
        P_Bridge_BleManager.postUpdateDelayed(fromDevice(device), () -> {
            P_Bridge_BleDevice.onPhyRead(device.getIBleDevice(), P_GattHolder.NULL, phyOption.getTxMask(), phyOption.getRxMask(), gattStatus);
        }, delay.millis());
    }

    /**
     * Overload of {@link #remoteRssiSuccess(BleDevice, int, Interval)} which delays the callback by 50ms.
     */
    public static void remoteRssiSuccess(final BleDevice device, final int rssi)
    {
        remoteRssiSuccess(device, rssi, Interval.millis(50));
    }

    /**
     * Send a callback that a read remote rssi succeeded with the given rssi value, and delay the callback by the amount specified.
     */
    public static void remoteRssiSuccess(final BleDevice device, final int rssi, Interval delay)
    {
        P_Bridge_BleManager.postUpdateDelayed(fromDevice(device), () -> {
            P_Bridge_BleDevice.onReadRemoteRssi(device.getIBleDevice(), P_GattHolder.NULL, rssi, BleStatuses.GATT_SUCCESS);
        }, delay.millis());
    }

    /**
     * Overload of {@link #remoteRssiError(BleDevice, int, Interval)} which delays the callback by 50ms.
     */
    public static void remoteRssiError(final BleDevice device, final int gattStatus)
    {
        remoteRssiError(device, gattStatus, Interval.millis(50));
    }

    /**
     * Send a callback that a remote rssi read has failed with the given gattStatus, and delay the callback by the amount specified.
     */
    public static void remoteRssiError(final BleDevice device, final int gattStatus, Interval delay)
    {
        P_Bridge_BleManager.postUpdateDelayed(fromDevice(device), () -> {
            P_Bridge_BleDevice.onReadRemoteRssi(device.getIBleDevice(), P_GattHolder.NULL, device.getRssi(), gattStatus);
        }, delay.millis());
    }

    /**
     * Overload of {@link #sendNotification(BleDevice, BleCharacteristic, byte[], Interval)} which delays the callback by 50ms.
     */
    public static void sendNotification(final BleDevice device, final BleCharacteristic characteristic, final byte[] data)
    {
        sendNotification(device, characteristic, data, Interval.millis(50));
    }

    /**
     * Simulate a notification being received with the given data, and delay the callback by the amount specified.
     */
    public static void sendNotification(final BleDevice device, final BleCharacteristic characteristic, final byte[] data, Interval delay)
    {
        P_Bridge_BleManager.postUpdateDelayed(fromDevice(device), () ->
        {
            characteristic.setValue(data);
            P_Bridge_BleDevice.onCharacteristicChanged(device.getIBleDevice(), P_GattHolder.NULL, characteristic);
        }, delay.millis());
    }

    public static void succeedDiscoverServices(BleDevice device)
    {
        P_Bridge_BleDevice.onServicesDiscovered(device.getIBleDevice(), null, BleStatuses.GATT_SUCCESS);
    }

    public static void failDiscoverServices(BleDevice device, int gattStatus)
    {
        P_Bridge_BleDevice.onServicesDiscovered(device.getIBleDevice(), P_GattHolder.NULL, gattStatus);
    }

    public static void failDiscoverServices(final BleDevice device, final int gattStatus, Interval delay)
    {
        P_Bridge_BleManager.postUpdateDelayed(fromDevice(device), () -> {
            P_Bridge_BleDevice.onServicesDiscovered(device.getIBleDevice(), P_GattHolder.NULL, gattStatus);
        }, delay.millis());
    }

    /**
     * Overload of {@link #setToConnecting(BleDevice, int)} which sets the gattStatus to {@link BleStatuses#GATT_SUCCESS}.
     */
    public static void setToConnecting(final BleDevice device)
    {
        setToConnecting(device, BleStatuses.GATT_SUCCESS);
    }

    /**
     * Overload of {@link #setToConnecting(BleDevice, int, Interval)} which delays the callback by 50ms.
     */
    public static void setToConnecting(final BleDevice device, int gattStatus)
    {
        setToConnecting(device, gattStatus, Interval.millis(50));
    }

    /**
     * Overload of {@link #setToConnecting(BleDevice, int, boolean, Interval)} which updates the internal state as well.
     */
    public static void setToConnecting(final BleDevice device, int gattStatus, Interval delay)
    {
        setToConnecting(device, gattStatus, true, delay);
    }

    /**
     * Send a callback to set a device's state to {@link BluetoothGatt#STATE_CONNECTING}, with the given gattStatus, whether or not to update the internal state, and delay
     * the callback by the amount specified.
     */
    public static void setToConnecting(final BleDevice device, final int gattStatus, final boolean updateDeviceState, Interval delay)
    {
        final IBleManager mgr = fromDevice(device);
        final IBleDevice dev = device.getIBleDevice();
        P_Bridge_BleManager.postUpdateDelayed(mgr, () -> {
            if (updateDeviceState)
            {
                UnitTestBluetoothManager layer = P_Bridge_BleManager.getManagerLayer(dev);
                layer.updateDeviceState(dev, BleStatuses.DEVICE_CONNECTING);
            }
            P_Bridge_BleDevice.onConnectionStateChange(dev, P_GattHolder.NULL, gattStatus, BleStatuses.DEVICE_CONNECTING);
        }, delay.millis());
    }

    /**
     * Send a callback to set the server's state to {@link android.bluetooth.BluetoothProfile#STATE_CONNECTING}, with the given gattStatus, and delay
     * the callback by the amount specified.
     */
    public static void setToConnecting(final BleServer server, final String macAddress, final int gattStatus, Interval delay)
    {
        P_Bridge_BleManager.postUpdateDelayed(server.getIBleServer().getIManager(), () ->
        {
            P_Bridge_BleServer.onConnectionStateChange(server.getIBleServer(), P_DeviceHolder.newNullHolder(macAddress), gattStatus, ProfileConst.STATE_CONNECTING);
        }, delay.millis());
    }

    /**
     * Overload of {@link #setToConnected(BleDevice, int)} which sets the gattStatus to {@link BleStatuses#GATT_SUCCESS}.
     */
    public static void setToConnected(final BleDevice device)
    {
        setToConnected(device, BleStatuses.GATT_SUCCESS);
    }

    /**
     * Overload of {@link #setToConnected(BleDevice, int, Interval)}, which delays the callback by 50ms.
     */
    public static void setToConnected(final BleDevice device, int gattStatus)
    {
        setToConnected(device, gattStatus, Interval.millis(50));
    }

    /**
     * Overload of {@link #setToConnected(BleDevice, int, boolean, Interval)} which updates the internal state as well.
     */
    public static void setToConnected(final BleDevice device, int gattStatus, Interval delay)
    {
        setToConnected(device, gattStatus, true, delay);
    }

    /**
     * Send a callback to set a device's state to {@link BluetoothGatt#STATE_CONNECTED}, with the given gattStatus, whether or not to update the internal state, and delay
     * the callback by the amount specified.
     */
    public static void setToConnected(final BleDevice device, final int gattStatus, final boolean updateDeviceState, Interval delay)
    {
        P_Bridge_BleManager.postUpdateDelayed(fromDevice(device), () -> {
            final IBleDevice dev = device.getIBleDevice();
            if (updateDeviceState)
            {
                final UnitTestBluetoothManager layer = P_Bridge_BleManager.getManagerLayer(dev);
                layer.updateDeviceState(dev, BleStatuses.DEVICE_CONNECTED);
            }
            P_Bridge_BleDevice.onConnectionStateChange(dev, P_GattHolder.NULL, gattStatus, BleStatuses.DEVICE_CONNECTED);
        }, delay.millis());
    }

    /**
     * Send a callback to set the server's state to {@link android.bluetooth.BluetoothProfile#STATE_CONNECTED}, with the given gattStatus, and delay
     * the callback by the amount specified.
     */
    public static void setToConnected(final BleServer server, final String macAddress, Interval delay)
    {
        P_Bridge_BleManager.postUpdateDelayed(fromServer(server), () ->
        {
            P_Bridge_BleServer.onConnectionStateChange(server.getIBleServer(), P_DeviceHolder.newNullHolder(macAddress), BleStatuses.GATT_SUCCESS, ProfileConst.STATE_CONNECTED);
        }, delay.millis());
    }

    /**
     * Overload of {@link #setToDisconnected(BleDevice, int)} with sets the gattStatus to {@link BleStatuses#GATT_SUCCESS}.
     */
    public static void setToDisconnected(BleDevice device)
    {
        setToDisconnected(device, BleStatuses.GATT_SUCCESS);
    }

    /**
     * Overload of {@link #setToDisconnected(BleDevice, int, Interval)} which delays the callback by 50ms.
     */
    public static void setToDisconnected(BleDevice device, int gattStatus)
    {
        setToDisconnected(device, gattStatus, Interval.millis(50));
    }

    /**
     * Overload of {@link #setToDisconnected(BleDevice, int, boolean, Interval)} which sets the device's internal state.
     */
    public static void setToDisconnected(BleDevice device, int gattStatus, Interval delay)
    {
        setToDisconnected(device, gattStatus, true, delay);
    }

    /**
     * Send a callback to set a device's state to {@link BluetoothGatt#STATE_DISCONNECTED}, with the given gattStatus, whether or not to update the internal state, and delay
     * the callback by the amount specified.
     */
    public static void setToDisconnected(final BleDevice device, final int gattStatus, final boolean updateDeviceState, Interval delay)
    {
        P_Bridge_BleManager.postUpdateDelayed(fromDevice(device), () -> {
            final IBleDevice dev = device.getIBleDevice();
            if (updateDeviceState)
            {
                final UnitTestBluetoothManager layer = P_Bridge_BleManager.getManagerLayer(dev);
                layer.updateDeviceState(dev, BleStatuses.DEVICE_DISCONNECTED);
            }
            P_Bridge_BleDevice.onConnectionStateChange(dev, P_GattHolder.NULL, gattStatus, BleStatuses.DEVICE_DISCONNECTED);
        }, delay.millis());
    }

    /**
     * Send a callback to set the server's state to {@link android.bluetooth.BluetoothProfile#STATE_DISCONNECTED}, with the given gattStatus, and delay
     * the callback by the amount specified.
     */
    public static void setToDisconnected(final BleServer server, final String macAddress, final int gattStatus, Interval delay)
    {
        P_Bridge_BleManager.postUpdateDelayed(server.getIBleServer().getIManager(), () ->
        {
            P_Bridge_BleServer.onConnectionStateChange(server.getIBleServer(), P_DeviceHolder.newNullHolder(macAddress), gattStatus, ProfileConst.STATE_CONNECTED);
        }, delay.millis());
    }

    /**
     * Simulate a device that is advertising, so SweetBlue picks up on it (as long as scanning is occurring at the time you call this method).
     * Use one of the methods {@link Utils_ScanRecord#newScanRecord(String)}, {@link Utils_ScanRecord#newScanRecord(String, UUID)}, etc to get the byte[] of the scan record easily.
     * This will generate a random mac address for the device.
     *
     * @see #advertiseDevice(BleManager, int, byte[], String)
     */
    public static void advertiseNewDevice(BleManager mgr, int rssi, byte[] scanRecord)
    {
        advertiseDevice(mgr, rssi, scanRecord, Util_Unit.randomMacAddress());
    }

    /**
     * Overload of {@link #advertiseDevice(BleManager, int, byte[], String, Interval)} with no delay.
     */
    public static void advertiseDevice(BleManager mgr, int rssi, byte[] scanRecord, String macAddress)
    {
        advertiseDevice(mgr, rssi, scanRecord, macAddress, Interval.ZERO);
    }

    /**
     * Simulate a device that is advertising, so SweetBlue picks up on it (as long as scanning is occurring at the time you call this method).
     * Use one of the methods {@link Utils_ScanRecord#newScanRecord(String)}, {@link Utils_ScanRecord#newScanRecord(String, UUID)}, etc to get the byte[] of the scan record easily.
     * You can use this method to "update" a device's scan record, but you must make sure to use the same mac address for this to work.
     */
    public static void advertiseDevice(final BleManager mgr, final int rssi, final byte[] scanRecord, final String macAddress, Interval delay)
    {
        final IBleManager manager = mgr.getIBleManager();
        P_Bridge_BleManager.postUpdateDelayed(manager, () -> {
            if (!mgr.is(BleManagerState.SCANNING))
                P_Bridge_Internal.logE(manager, "Tried to advertise a device when not scanning!");
            P_Bridge_BleManager.addScanResult(manager, P_DeviceHolder.newNullHolder(macAddress), rssi, scanRecord);
        }, delay.millis());
    }

    /**
     * Simulate a batch of devices that are advertising, so SweetBlue picks up on it (as long as scanning is occuring at the time you call this method).
     */
    public static void advertiseDeviceList(final BleManager mgr, final List<L_Util.ScanResult> devices, Interval delay) {
        final IBleManager manager = mgr.getIBleManager();
        P_Bridge_BleManager.postUpdateDelayed(manager, () -> {
            if (!mgr.is(BleManagerState.SCANNING))
                P_Bridge_Internal.logE(manager, "Tried to advertise a device when not scanning!");
            P_Bridge_BleManager.addBatchScanResults(manager, devices);
        }, delay.millis());
    }

    /**
     * Overload of {@link #advertiseNewDevice(BleManager, int, byte[])}, which creates the byte[] scanRecord from the name you provide.
     */
    public static void advertiseNewDevice(BleManager mgr, int rssi, String deviceName)
    {
        advertiseNewDevice(mgr, rssi, Utils_ScanRecord.newScanRecord(deviceName));
    }

    public static void setToAdvertising(BleManager mgr, AdvertiseSettings settings, L_Util.AdvertisingCallback callback)
    {
        setToAdvertising(mgr, settings, callback, Interval.millis(50));
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void setToAdvertising(BleManager mgr, final AdvertiseSettings settings, L_Util.AdvertisingCallback callback, Interval delay)
    {
        if (Utils.isLollipop())
        {
            P_Bridge_Compat.setAdvListener(callback);
            P_Bridge_BleManager.postUpdateDelayed(mgr.getIBleManager(), () -> {
                L_Util.getNativeAdvertisingCallback().onStartSuccess(settings);
            }, delay.millis());
        }
    }









    private static IBleManager fromDevice(BleDevice device)
    {
        return device.getIBleDevice().getIManager();
    }

    private static IBleManager fromServer(BleServer server)
    {
        return server.getIBleServer().getIManager();
    }

}
