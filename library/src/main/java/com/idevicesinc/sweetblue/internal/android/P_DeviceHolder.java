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
import android.content.Intent;

import com.idevicesinc.sweetblue.utils.P_Const;
import com.idevicesinc.sweetblue.utils.UsesCustomNull;

/**
 * Class used to temporarily hold an instance of {@link BluetoothDevice}. This is only meant to wrap callbacks
 * from the native layer to the internal layer. Once in the native layer, the device will be passed into
 * an instance of {@link IBluetoothDevice}.
 */
public final class P_DeviceHolder implements UsesCustomNull
{

    public final static P_DeviceHolder NULL = new P_DeviceHolder(P_Const.NULL_MAC);

    private final BluetoothDevice m_device;
    private final String m_macAddress;


    P_DeviceHolder(BluetoothDevice device)
    {
        m_device = device;
        if (m_device != null)
            m_macAddress = m_device.getAddress();
        else
            m_macAddress = P_Const.NULL_MAC;
    }

    private P_DeviceHolder(String macAddress)
    {
        m_device = null;
        m_macAddress = macAddress;
    }


    public final BluetoothDevice getDevice()
    {
        return m_device;
    }

    public final String getAddress()
    {
        return m_device != null ? m_device.getAddress() : m_macAddress;
    }


    public static P_DeviceHolder newHolder(BluetoothDevice device)
    {
        return new P_DeviceHolder(device);
    }

    public static P_DeviceHolder newHolder(Intent intent)
    {
        final BluetoothDevice device = intent.getParcelableExtra(DeviceConst.EXTRA_DEVICE);
        return newHolder(device);
    }

    public static P_DeviceHolder newHolder(BluetoothDevice device, String macAddress)
    {
        if (device == null)
            return newNullHolder(macAddress);
        else
            return newHolder(device);
    }

    public static P_DeviceHolder newNullHolder(String macAddress)
    {
        return new P_DeviceHolder(macAddress);
    }

    @Override
    public boolean isNull()
    {
        return m_device == null;
    }
}
