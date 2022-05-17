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


import android.bluetooth.BluetoothGattServerCallback;
import com.idevicesinc.sweetblue.BleCharacteristic;
import com.idevicesinc.sweetblue.BleDescriptor;
import com.idevicesinc.sweetblue.BleService;

/**
 * Interface to strictly type callbacks for server instances (BluetoothGattServer).
 */
public interface IServerListener
{

    BluetoothGattServerCallback getCallback();

    interface Callback
    {
        void onPhyRead(P_DeviceHolder device, int txPhy, int rxPhy, int status);
        void onPhyUpdate(P_DeviceHolder device, int txPhy, int rxPhy, int status);
        void onMtuChanged(P_DeviceHolder device, int mtu);
        void onConnectionStateChange(P_DeviceHolder device, int status, int newState);
        void onCharacteristicReadRequest(P_DeviceHolder device, int requestId, int offset, BleCharacteristic characteristic);
        void onCharacteristicWriteRequest(P_DeviceHolder device, int requestId, BleCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value);
        void onDescriptorReadRequest(P_DeviceHolder device, int requestId, int offset, BleDescriptor descriptor);
        void onDescriptorWriteRequest(P_DeviceHolder device, int requestId, BleDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value);
        void onExecuteWrite(P_DeviceHolder device, int requestId, boolean execute);
        void onNotificationSent(P_DeviceHolder device, int status);
        void onServiceAdded(int status, BleService service);
    }


    interface Factory
    {
        IServerListener newInstance(Callback callback);
    }

    Factory DEFAULT_FACTORY = new DefaultFactory();

    class DefaultFactory implements Factory
    {
        @Override
        public IServerListener newInstance(Callback callback)
        {
            return new ServerListenerImpl(callback);
        }
    }


}
