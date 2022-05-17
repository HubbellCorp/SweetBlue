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


import android.bluetooth.BluetoothGattServer;
import com.idevicesinc.sweetblue.utils.UsesCustomNull;

/**
 * Class used to temporarily hold an instance of {@link BluetoothGattServer}. This is only meant to wrap callbacks
 * from the native layer to the internal layer. Once in the native layer, the device will be passed into
 * an instance of {@link IBluetoothServer}.
 */
public final class P_ServerHolder implements UsesCustomNull
{

    public final static P_ServerHolder NULL = new P_ServerHolder(null);

    private final BluetoothGattServer m_server;


    P_ServerHolder(BluetoothGattServer server)
    {
        m_server = server;
    }


    public final BluetoothGattServer getServer()
    {
        return m_server;
    }


    @Override public boolean isNull()
    {
        return m_server == null;
    }


    public static P_ServerHolder newHolder(BluetoothGattServer server)
    {
        return new P_ServerHolder(server);
    }
}
