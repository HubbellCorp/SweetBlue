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

package com.idevicesinc.sweetblue.internal.android;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import com.idevicesinc.sweetblue.BleCharacteristic;
import com.idevicesinc.sweetblue.BleDescriptor;


/**
 * Class which implements {@link IDeviceListener}, and extends {@link BluetoothGattCallback}, to pipe native
 * callbacks to the internal layer. Any native objects are wrapped before being sent to the internal layer.
 */
final class DeviceListenerImpl extends BluetoothGattCallback implements IDeviceListener
{

    private final IDeviceListener.Callback m_callback;


    DeviceListenerImpl(Callback callback)
    {
        m_callback = callback;
    }



    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
    {
        if (m_callback != null)
            m_callback.onConnectionStateChange(new P_GattHolder(gatt), status, newState, true, null);
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
    {
        if (m_callback != null)
            m_callback.onCharacteristicChanged(new P_GattHolder(gatt), new BleCharacteristic(characteristic));
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
    {
        if (m_callback != null)
            m_callback.onCharacteristicRead(new P_GattHolder(gatt), new BleCharacteristic(characteristic), status);
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
    {
        if (m_callback != null)
            m_callback.onCharacteristicWrite(new P_GattHolder(gatt), new BleCharacteristic(characteristic), status);
    }

    @Override
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
    {
        if (m_callback != null)
            m_callback.onDescriptorRead(new P_GattHolder(gatt), new BleDescriptor(descriptor), status);
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
    {
        if (m_callback != null)
            m_callback.onDescriptorWrite(new P_GattHolder(gatt), new BleDescriptor(descriptor), status);
    }

    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status)
    {
        if (m_callback != null)
            m_callback.onMtuChanged(new P_GattHolder(gatt), mtu, status);
    }

    @Override
    public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status)
    {
        if (m_callback != null)
            m_callback.onPhyRead(new P_GattHolder(gatt), txPhy, rxPhy, status);
    }

    @Override
    public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status)
    {
        if (m_callback != null)
            m_callback.onPhyUpdate(new P_GattHolder(gatt), txPhy, rxPhy, status);
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status)
    {
        if (m_callback != null)
            m_callback.onReadRemoteRssi(new P_GattHolder(gatt), rssi, status);
    }

    @Override
    public void onReliableWriteCompleted(BluetoothGatt gatt, int status)
    {
        if (m_callback != null)
            m_callback.onReliableWriteCompleted(new P_GattHolder(gatt), status);
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status)
    {
        if (m_callback != null)
            m_callback.onServicesDiscovered(new P_GattHolder(gatt), status);
    }

    @Override
    public BluetoothGattCallback getNativeCallback()
    {
        return this;
    }
}
