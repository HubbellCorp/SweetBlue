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

/**
 * Class used to hold values from {@link BluetoothDevice}.
 */
public final class DeviceConst
{

    private DeviceConst()
    {
        throw new RuntimeException("No instances!");
    }

    public static final String EXTRA_REASON = "android.bluetooth.device.extra.REASON";
    public static final String EXTRA_BOND_STATE = BluetoothDevice.EXTRA_BOND_STATE;
    public static final String EXTRA_PREVIOUS_BOND_STATE = BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE;
    public static final String EXTRA_DEVICE = BluetoothDevice.EXTRA_DEVICE;
    public static final String EXTRA_RSSI = BluetoothDevice.EXTRA_RSSI;


    public static final String ACTION_DISAPPEARED = "android.bluetooth.device.action.DISAPPEARED";
    public static final String ACTION_BOND_STATE_CHANGED = BluetoothDevice.ACTION_BOND_STATE_CHANGED;
    public static final String ACTION_ACL_CONNECTED = BluetoothDevice.ACTION_ACL_CONNECTED;
    public static final String ACTION_ACL_DISCONNECT_REQUESTED = BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED;
    public static final String ACTION_ACL_DISCONNECTED = BluetoothDevice.ACTION_ACL_DISCONNECTED;
    public static final String ACTION_PAIRING_REQUEST = "android.bluetooth.device.action.PAIRING_REQUEST";
    public static final String ACTION_FOUND = BluetoothDevice.ACTION_FOUND;
    public static final String ACTION_UUID = BluetoothDevice.ACTION_UUID;


    public static final int ERROR = BluetoothDevice.ERROR;
    public static final int BOND_NONE = BluetoothDevice.BOND_NONE;
    public static final int BOND_BONDED = BluetoothDevice.BOND_BONDED;
    public static final int BOND_BONDING = BluetoothDevice.BOND_BONDING;

}
