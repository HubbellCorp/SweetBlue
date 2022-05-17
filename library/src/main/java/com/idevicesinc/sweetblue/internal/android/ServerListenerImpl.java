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


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import com.idevicesinc.sweetblue.BleCharacteristic;
import com.idevicesinc.sweetblue.BleDescriptor;
import com.idevicesinc.sweetblue.BleService;


final class ServerListenerImpl extends BluetoothGattServerCallback implements IServerListener
{

    private final Callback m_callback;


    public ServerListenerImpl(Callback callback)
    {
        m_callback = callback;
    }


    @Override
    public void onPhyRead(BluetoothDevice device, int txPhy, int rxPhy, int status)
    {
        if (m_callback != null)
            m_callback.onPhyRead(new P_DeviceHolder(device), txPhy, rxPhy, status);
    }

    @Override
    public void onPhyUpdate(BluetoothDevice device, int txPhy, int rxPhy, int status)
    {
        if (m_callback != null)
            m_callback.onPhyUpdate(new P_DeviceHolder(device), txPhy, rxPhy, status);
    }

    @Override
    public void onMtuChanged(BluetoothDevice device, int mtu)
    {
        if (m_callback != null)
            m_callback.onMtuChanged(new P_DeviceHolder(device), mtu);
    }

    @Override
    public void onConnectionStateChange(BluetoothDevice device, int status, int newState)
    {
        if (m_callback != null)
            m_callback.onConnectionStateChange(new P_DeviceHolder(device), status, newState);
    }

    @Override
    public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic)
    {
        if (m_callback != null)
            m_callback.onCharacteristicReadRequest(new P_DeviceHolder(device), requestId, offset, new BleCharacteristic(characteristic));
    }

    @Override
    public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value)
    {
        if (m_callback != null)
            m_callback.onCharacteristicWriteRequest(new P_DeviceHolder(device), requestId, new BleCharacteristic(characteristic), preparedWrite, responseNeeded, offset, value);
    }

    @Override
    public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor)
    {
        if (m_callback != null)
            m_callback.onDescriptorReadRequest(new P_DeviceHolder(device), requestId, offset, new BleDescriptor(descriptor));
    }

    @Override
    public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value)
    {
        if (m_callback != null)
            m_callback.onDescriptorWriteRequest(new P_DeviceHolder(device), requestId, new BleDescriptor(descriptor), preparedWrite, responseNeeded, offset, value);
    }

    @Override
    public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute)
    {
        if (m_callback != null)
            m_callback.onExecuteWrite(new P_DeviceHolder(device), requestId, execute);
    }

    @Override
    public void onNotificationSent(BluetoothDevice device, int status)
    {
        if (m_callback != null)
            m_callback.onNotificationSent(new P_DeviceHolder(device), status);
    }

    @Override
    public void onServiceAdded(int status, BluetoothGattService service)
    {
        if (m_callback != null)
            m_callback.onServiceAdded(status, new BleService(service));
    }

    @Override
    public BluetoothGattServerCallback getCallback()
    {
        return this;
    }
}
