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

package com.idevicesinc.sweetblue.internal;


import com.idevicesinc.sweetblue.BleCharacteristic;
import com.idevicesinc.sweetblue.BleDescriptor;
import com.idevicesinc.sweetblue.internal.android.P_GattHolder;


public final class P_Bridge_BleDevice
{

    // No instances
    private P_Bridge_BleDevice() {}


    public static void onConnectionStateChange(IBleDevice device, P_GattHolder gatt, int status, int newState)
    {
        device.getNativeManager().getNativeListener().onConnectionStateChange(gatt, status, newState, true, null);
    }

    public static void onCharacteristicChanged(IBleDevice device, P_GattHolder gatt, BleCharacteristic characteristic)
    {
        device.getNativeManager().getNativeListener().onCharacteristicChanged(gatt, characteristic);
    }

    public static void onCharacteristicRead(IBleDevice device, P_GattHolder gatt, BleCharacteristic characteristic, int status)
    {
        device.getNativeManager().getNativeListener().onCharacteristicRead(gatt, characteristic, status);
    }

    public static void onCharacteristicWrite(IBleDevice device, P_GattHolder gatt, BleCharacteristic characteristic, int status)
    {
        device.getNativeManager().getNativeListener().onCharacteristicWrite(gatt, characteristic, status);
    }

    public static void onDescriptorRead(IBleDevice device, P_GattHolder gatt, BleDescriptor descriptor, int status)
    {
        device.getNativeManager().getNativeListener().onDescriptorRead(gatt, descriptor, status);
    }

    public static void onDescriptorWrite(IBleDevice device, P_GattHolder gatt, BleDescriptor descriptor, int status)
    {
        device.getNativeManager().getNativeListener().onDescriptorWrite(gatt, descriptor, status);
    }

    public static void onMtuChanged(IBleDevice device, P_GattHolder gatt, int mtu, int status)
    {
        device.getNativeManager().getNativeListener().onMtuChanged(gatt, mtu, status);
    }

    public static void onPhyRead(IBleDevice device, P_GattHolder gatt, int txPhy, int rxPhy, int status)
    {
        device.getNativeManager().getNativeListener().onPhyRead(gatt, txPhy, rxPhy, status);
    }

    public static void onPhyUpdate(IBleDevice device, P_GattHolder gatt, int txPhy, int rxPhy, int status)
    {
        device.getNativeManager().getNativeListener().onPhyUpdate(gatt, txPhy, rxPhy, status);
    }

    public static void onReadRemoteRssi(IBleDevice device, P_GattHolder gatt, int rssi, int status)
    {
        device.getNativeManager().getNativeListener().onReadRemoteRssi(gatt, rssi, status);
    }

    public static void onReliableWriteCompleted(IBleDevice device, P_GattHolder gatt, int status)
    {
        device.getNativeManager().getNativeListener().onReliableWriteCompleted(gatt, status);
    }

    public static void onServicesDiscovered(IBleDevice device, P_GattHolder gatt, int status)
    {
        device.getNativeManager().getNativeListener().onServicesDiscovered(gatt, status);
    }
}
