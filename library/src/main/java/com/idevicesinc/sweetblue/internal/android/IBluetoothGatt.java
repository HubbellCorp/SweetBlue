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
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.content.Context;
import com.idevicesinc.sweetblue.BleCharacteristic;
import com.idevicesinc.sweetblue.BleConnectionPriority;
import com.idevicesinc.sweetblue.BleDescriptor;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleService;
import com.idevicesinc.sweetblue.UhOhListener;
import com.idevicesinc.sweetblue.internal.IBleDevice;
import com.idevicesinc.sweetblue.utils.LogFunction;
import com.idevicesinc.sweetblue.utils.Phy;
import java.util.List;
import java.util.UUID;

/**
 * Interface used to abstract away the android class {@link BluetoothGatt}.
 *
 * @see AndroidBluetoothGatt for the default implementation
 */
public interface IBluetoothGatt
{

    void setGatt(BluetoothGatt gatt);
    UhOhListener.UhOh closeGatt();
    BluetoothGatt getGatt();
    Boolean getAuthRetryValue();
    boolean equals(P_GattHolder gatt);
    List<BleService> getNativeServiceList(LogFunction logger);


    BleService getBleService(UUID serviceUuid, LogFunction logger);
    boolean isGattNull();
    void connect(IBluetoothDevice device, Context context, boolean useAutoConnect, BluetoothGattCallback callback);
    void disconnect();
    boolean requestMtu(int mtu);
    boolean refreshGatt();
    boolean setPhy(Phy options);
    boolean readPhy();
    boolean readCharacteristic(BleCharacteristic characteristic);
    boolean setCharValue(BleCharacteristic characteristic, byte[] data);
    boolean writeCharacteristic(BleCharacteristic characteristic);
    boolean setCharacteristicNotification(BleCharacteristic characteristic, boolean enable);
    boolean readDescriptor(BleDescriptor descriptor);
    boolean setDescValue(BleDescriptor descriptor, byte[] data);
    boolean writeDescriptor(BleDescriptor descriptor);
    boolean requestConnectionPriority(BleConnectionPriority priority);
    boolean discoverServices();
    boolean executeReliableWrite();
    boolean beginReliableWrite();
    void abortReliableWrite(BluetoothDevice device);
    boolean readRemoteRssi();
    BleDevice getBleDevice();


    /**
     * Interface used by the library to instantiate a new instance of {@link IBluetoothGatt}
     */
    interface Factory
    {
        IBluetoothGatt newInstance(IBleDevice device);
    }

    /**
     * Default implementation of {@link Factory}.
     */
    class DefaultFactory implements Factory
    {
        @Override
        public IBluetoothGatt newInstance(IBleDevice device)
        {
            return new AndroidBluetoothGatt(device);
        }
    }

    /**
     * An instance of {@link DefaultFactory} used by the library, unless {@link com.idevicesinc.sweetblue.BleManagerConfig#gattFactory} is changed.
     */
    Factory DEFAULT_FACTORY = new DefaultFactory();

}
