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


import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.content.Context;
import android.os.Handler;

import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.compat.K_Util;
import com.idevicesinc.sweetblue.compat.M_Util;
import com.idevicesinc.sweetblue.compat.O_Util;
import com.idevicesinc.sweetblue.internal.IBleDevice;
import com.idevicesinc.sweetblue.internal.IBleManager;
import com.idevicesinc.sweetblue.internal.P_Bridge_Internal;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.Utils_Reflection;


/**
 * Default implementation of {@link IBluetoothDevice}, and wraps {@link BluetoothDevice}. This class is used by default
 * by the library, and the only time it should NOT be used, is when unit testing.
 *
 * @see com.idevicesinc.sweetblue.BleManagerConfig#bluetoothDeviceFactory
 */
public final class AndroidBluetoothDevice implements IBluetoothDevice
{


    private static final String METHOD_NAME__REMOVE_BOND			= "removeBond";
    private static final String METHOD_NAME__CANCEL_BOND_PROCESS	= "cancelBondProcess";
    private static final String IS_CONNECTED_METHOD_NAME            = "isConnected";
    private static final int TRANSPORT_LE                           = 2; // Taken from BluetoothDevice.TRANSPORT_LE -- only available on API23+

    private BluetoothDevice m_native_device;
    private IBleDevice m_device;


    public AndroidBluetoothDevice(IBleDevice device)
    {
        m_device = device;
    }

    @Override public void init()
    {
    }

    @Override public BleDevice getBleDevice()
    {
        return m_device.getBleDevice();
    }

    @Override public void updateBleDevice(IBleDevice device)
    {
        m_device = device;
    }

    @SuppressLint("MissingPermission")
    @Override
    public int getBondState() {
        if (m_native_device != null)
        {
            return m_native_device.getBondState();
        }
        return 0;
    }

    @Override
    public String getAddress() {
        if (m_native_device != null)
        {
            return m_native_device.getAddress();
        }
        return "";
    }

    @Override
    public boolean isConnected()
    {
        return Utils_Reflection.callBooleanReturnMethod(m_native_device, IS_CONNECTED_METHOD_NAME, false);
    }

    @SuppressLint("MissingPermission")
    @Override public String getName()
    {
        if (m_native_device != null)
        {
            return m_native_device.getName();
        }
        return "";
    }

    @Override
    public boolean createBond() {
        if (m_native_device != null)
        {
            if (Utils.isKitKat())
            {
                return K_Util.createBond(m_native_device);
            }
        }
        return false;
    }

    @Override
    public boolean removeBond() {
        return Utils_Reflection.callBooleanReturnMethod(m_native_device, METHOD_NAME__REMOVE_BOND, P_Bridge_Internal.loggingEnabled(getManager()));
    }

    @Override
    public boolean cancelBond() {
        return Utils_Reflection.callBooleanReturnMethod(m_native_device, METHOD_NAME__CANCEL_BOND_PROCESS, P_Bridge_Internal.loggingEnabled(getManager()));
    }

    private IBleManager getManager()
    {
        return m_device.getIManager();
    }

    @Override
    public boolean isDeviceNull() {
        return m_native_device == null;
    }

    @Override
    public boolean equals(IBluetoothDevice device) {
        if (device == null) return false;
        if (device == this) return true;
        return m_native_device.equals(device.getNativeDevice());
    }

    @Override
    public boolean createBondSneaky(String methodName, boolean loggingEnabled) {
        if (m_native_device != null && Utils.isKitKat())
        {
            final Class[] paramTypes = new Class[] { int.class };
            return Utils_Reflection.callBooleanReturnMethod(m_native_device, methodName, paramTypes, loggingEnabled, TRANSPORT_LE);
        }
        return false;
    }

    @Override
    public void setNativeDevice(BluetoothDevice device, P_DeviceHolder deviceHolder) {
        m_native_device = device;
    }

    @Override
    public BluetoothDevice getNativeDevice() {
        return m_native_device;
    }

    @SuppressLint("MissingPermission")
    @Override
    public BluetoothGatt connect(Context context, boolean useAutoConnect, BluetoothGattCallback callback) {
        if (m_native_device != null)
        {

            if (Utils.isOreo()) {
                Handler handler = P_Bridge_Internal.getUpdateHandler_Android(getManager());
                if (handler != null) {
                    return O_Util.connect(m_native_device, useAutoConnect, context, callback, handler);
                } else {
                    return O_Util.connect(m_native_device, useAutoConnect, context, callback);
                }
            }
            else if (Utils.isMarshmallow())
                return M_Util.connect(m_native_device, useAutoConnect, context, callback);
            else
                return m_native_device.connectGatt(context, useAutoConnect, callback);
        }
        return null;
    }

}
