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


import android.bluetooth.BluetoothAdapter;

/**
 * Class used to hold values from {@link BluetoothAdapter}
 */
public final class AdapterConst
{

    private AdapterConst()
    {
        throw new RuntimeException("No instances!");
    }

    public final static String EXTRA_PREVIOUS_STATE = BluetoothAdapter.EXTRA_PREVIOUS_STATE;
    public final static String EXTRA_STATE = BluetoothAdapter.EXTRA_STATE;

    public final static String ACTION_STATE_CHANGED = BluetoothAdapter.ACTION_STATE_CHANGED;
    public final static String ACTION_DISCOVERY_FINISHED = BluetoothAdapter.ACTION_DISCOVERY_FINISHED;
    public final static String ACTION_DISCOVERY_STARTED = BluetoothAdapter.ACTION_DISCOVERY_STARTED;


    public final static int ERROR = BluetoothAdapter.ERROR;
    public final static int STATE_OFF = BluetoothAdapter.STATE_OFF;
    public final static int STATE_TURNING_ON = BluetoothAdapter.STATE_TURNING_ON;
    public static final int STATE_ON = BluetoothAdapter.STATE_ON;
    public static final int STATE_TURNING_OFF = BluetoothAdapter.STATE_TURNING_OFF;


}
