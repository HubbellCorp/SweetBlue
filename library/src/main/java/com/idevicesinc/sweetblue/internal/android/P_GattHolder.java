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


import android.bluetooth.BluetoothGatt;
import com.idevicesinc.sweetblue.utils.UsesCustomNull;

/**
 * Class used internally to hold an instance of {@link android.bluetooth.BluetoothGatt}.
 */
public final class P_GattHolder implements UsesCustomNull
{

    public final static P_GattHolder NULL = new P_GattHolder(null);

    private final BluetoothGatt m_gatt;


    P_GattHolder(BluetoothGatt gatt)
    {
        m_gatt = gatt;
    }


    public final BluetoothGatt getGatt()
    {
        return m_gatt;
    }


    @Override
    public boolean isNull()
    {
        return this == NULL;
    }
}
