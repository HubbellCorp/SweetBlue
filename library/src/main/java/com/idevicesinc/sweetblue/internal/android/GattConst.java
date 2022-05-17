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

/**
 * Class used to hold values from {@link BluetoothGatt}
 */
public final class GattConst
{

    private GattConst()
    {
        throw new RuntimeException("No instances!");
    }


    public final static int GATT_SERVER = BluetoothGatt.GATT_SERVER;
    public final static int STATE_DISCONNECTED = BluetoothGatt.STATE_DISCONNECTED;

}
