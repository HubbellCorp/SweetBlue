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


import com.idevicesinc.sweetblue.BleDeviceConfig;
import com.idevicesinc.sweetblue.BleDeviceOrigin;
import com.idevicesinc.sweetblue.internal.android.IBluetoothDevice;

/**
 * Interface which represents a Bluetooth Device (a peripheral)
 */
public interface IBleDevice extends IBleNode, IBleDevice_User, IBleDevice_Internal
{

    /**
     * Factory interface used to create new instances of IBleDevice
     */
    interface Factory
    {
        IBleDevice newInstance(IBleManager mngr, IBluetoothDevice device_native, String name_normalized, String name_native, BleDeviceOrigin origin, BleDeviceConfig config_nullable, boolean isNull);
    }

    /**
     * Default factory implementation which instantiates BleDeviceImpl
     */
    Factory DEFAULT_FACTORY = new DefaultFactory();


    class DefaultFactory implements Factory
    {
        @Override
        public IBleDevice newInstance(IBleManager mngr, IBluetoothDevice device_native, String name_normalized, String name_native, BleDeviceOrigin origin, BleDeviceConfig config_nullable, boolean isNull)
        {
            return new P_BleDeviceImpl(mngr, device_native, name_normalized, name_native, origin, config_nullable, isNull);
        }
    }

}
