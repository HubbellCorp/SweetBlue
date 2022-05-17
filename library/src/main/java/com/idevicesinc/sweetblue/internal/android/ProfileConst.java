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


import android.bluetooth.BluetoothProfile;


/**
 * Class used to hold values from {@link BluetoothProfile}
 */
public final class ProfileConst
{

    private ProfileConst()
    {
        throw new RuntimeException("No instances!");
    }


    public final static int STATE_DISCONNECTING = BluetoothProfile.STATE_DISCONNECTING;
    public final static int STATE_DISCONNECTED = BluetoothProfile.STATE_DISCONNECTED;
    public final static int STATE_CONNECTING = BluetoothProfile.STATE_CONNECTING;
    public final static int STATE_CONNECTED = BluetoothProfile.STATE_CONNECTED;

}
