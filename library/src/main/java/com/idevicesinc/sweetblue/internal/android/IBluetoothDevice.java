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
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.internal.IBleDevice;


/**
 * Interface used to abstract away the android class {@link BluetoothDevice}.
 *
 * @see AndroidBluetoothDevice for the default implementation
 */
public interface IBluetoothDevice
{

    void init();
    void setNativeDevice(BluetoothDevice device, P_DeviceHolder deviceHolder);
    int getBondState();
    String getAddress();
    String getName();
    boolean createBond();
    boolean isDeviceNull();
    boolean removeBond();
    boolean cancelBond();
    boolean equals(IBluetoothDevice device);
    boolean createBondSneaky(String methodName, boolean loggingEnabled);
    boolean isConnected();
    BluetoothDevice getNativeDevice();
    BluetoothGatt connect(Context context, boolean useAutoConnect, BluetoothGattCallback callback);
    void updateBleDevice(IBleDevice device);
    BleDevice getBleDevice();


    /**
     * Interface used by the library to instantiate a new instance of {@link IBluetoothDevice}
     */
    interface Factory
    {
        IBluetoothDevice newInstance(IBleDevice device);
    }

    /**
     * Default implementation of {@link Factory}.
     */
    class DefaultFactory implements Factory
    {
        @Override
        public IBluetoothDevice newInstance(IBleDevice device)
        {
            return new AndroidBluetoothDevice(device);
        }
    }

    /**
     * An instance of {@link DefaultFactory} used by the library, unless {@link com.idevicesinc.sweetblue.BleManagerConfig#bluetoothDeviceFactory} is changed.
     */
    Factory DEFAULT_FACTORY = new DefaultFactory();

    /**
     * Null instance of the {@link IBluetoothDevice} interface (rather than instantiating each time it's used).
     */
    IBluetoothDevice NULL = new IBluetoothDevice()
    {
        @Override public void setNativeDevice(BluetoothDevice device, P_DeviceHolder deviceHolder)
        {
        }

        @Override public void init()
        {
        }

        @Override public int getBondState()
        {
            return 0;
        }

        @Override public String getAddress()
        {
            return "";
        }

        @Override public String getName()
        {
            return "";
        }

        @Override public boolean createBond()
        {
            return false;
        }

        @Override public boolean isDeviceNull()
        {
            return false;
        }

        @Override public boolean removeBond()
        {
            return false;
        }

        @Override public boolean cancelBond()
        {
            return false;
        }

        @Override
        public boolean isConnected()
        {
            return false;
        }

        @Override public boolean equals(IBluetoothDevice device)
        {
            return device == this;
        }

        @Override public boolean createBondSneaky(String methodName, boolean loggingEnabled)
        {
            return false;
        }

        @Override public BluetoothDevice getNativeDevice()
        {
            return null;
        }

        @Override public BluetoothGatt connect(Context context, boolean useAutoConnect, BluetoothGattCallback callback)
        {
            return null;
        }

        @Override public void updateBleDevice(IBleDevice device)
        {
        }

        @Override public BleDevice getBleDevice()
        {
            return BleDevice.NULL;
        }
    };

}
