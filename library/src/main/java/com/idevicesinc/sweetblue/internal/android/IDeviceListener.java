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


import android.bluetooth.BluetoothGattCallback;
import com.idevicesinc.sweetblue.BleCharacteristic;
import com.idevicesinc.sweetblue.BleDescriptor;

/**
 * Interface to abstract away any native callbacks/listeners for devices -- {@link android.bluetooth.BluetoothDevice}.
 */
public interface IDeviceListener
{

    BluetoothGattCallback getNativeCallback();


    /**
     * Interface used to pipe native callbacks to the internal layer.
     */
    interface Callback
    {
        /**
         * @param isExplicit is currently only used for disconnections, to know if the task for it was user called or not.
         */
        void onConnectionStateChange(P_GattHolder gatt, int status, int newState, boolean isNativeCallback, Boolean isExplicit);
        void onCharacteristicChanged(P_GattHolder gatt, BleCharacteristic characteristic);
        void onCharacteristicRead(P_GattHolder gatt, BleCharacteristic characteristic, int status);
        void onCharacteristicWrite(P_GattHolder gatt, BleCharacteristic characteristic, int status);
        void onDescriptorRead(P_GattHolder gatt, BleDescriptor descriptor, int status);
        void onDescriptorWrite(P_GattHolder gatt, BleDescriptor descriptor, int status);
        void onMtuChanged(P_GattHolder gatt, int mtu, int status);
        void onPhyRead(P_GattHolder gatt, int txPhy, int rxPhy, int status);
        void onPhyUpdate(P_GattHolder gatt, int txPhy, int rxPhy, int status);
        void onReadRemoteRssi(P_GattHolder gatt, int rssi, int status);
        void onReliableWriteCompleted(P_GattHolder gatt, int status);
        void onServicesDiscovered(P_GattHolder gatt, int status);
    }

    interface Factory
    {
        IDeviceListener newInstance(Callback callback);
    }

    Factory DEFAULT_FACTORY = new DefaultFactory();

    class DefaultFactory implements Factory
    {
        @Override
        public IDeviceListener newInstance(Callback callback)
        {
            return new DeviceListenerImpl(callback);
        }
    }

}
