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

package com.idevicesinc.sweetblue.utils;


import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import com.idevicesinc.sweetblue.BleCharacteristic;
import com.idevicesinc.sweetblue.BleDescriptor;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleService;
import com.idevicesinc.sweetblue.LogOptions;
import com.idevicesinc.sweetblue.BleManagerConfig;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Class which simply houses static final empty constructs which are used throughout the library (rather than instantiating new ones
 * every time)
 */
public final class P_Const
{

    private static final Iterator<BluetoothGattService> EMPTY_NATIVE_SERVICE_ITERATOR = new EmptyIterator<>();

    private static final Iterator<BleService> EMPTY_SERVICE_ITERATOR                  = new EmptyIterator<>();

    public final static List<BluetoothGattService> EMPTY_SERVICE_LIST                 = new ArrayList<BluetoothGattService>(0)
    {
        @Override public Iterator<BluetoothGattService> iterator()
        {
            return EMPTY_NATIVE_SERVICE_ITERATOR;
        }
    };

    public final static List<BleService> EMPTY_BLESERVICE_LIST                          = new ArrayList<BleService>(0)
    {
        @Override
        public Iterator iterator()
        {
            return EMPTY_SERVICE_ITERATOR;
        }
    };

    public static final List<BleDevice> EMPTY_BLEDEVICE_LIST                            = new ArrayList<>(0);

    public static final List<BluetoothGattCharacteristic> EMPTY_CHARACTERISTIC_LIST     = new ArrayList<>(0);

    public static final List<BleCharacteristic> EMPTY_BLECHARACTERISTIC_LIST            = new ArrayList<>(0);

    public static final List<BluetoothGattDescriptor> EMPTY_DESCRIPTOR_LIST             = new ArrayList<>(0);

    public static final List<BleDescriptor> EMPTY_BLEDESCRIPTOR_LIST                    = new ArrayList<>(0);

    public final static byte[] EMPTY_BYTE_ARRAY                                         = new byte[0];

    public static final UUID[] EMPTY_UUID_ARRAY			                                = new UUID[0];

    public static final FutureData EMPTY_FUTURE_DATA	                                = new PresentData(EMPTY_BYTE_ARRAY);

    /**
     * Spells out "Decaff Coffee"...clever, right? I figure all zeros or
     * something would actually have a higher chance of collision in a dev
     * environment.
     */
    public static final String NULL_MAC                                                 = "DE:CA:FF:C0:FF:EE";

    public static final String NULL_STRING                                              = "NULL";

    /**
     * Used if {@link BleManagerConfig#loggingOptions} is not {@link LogOptions#OFF}. Gives threads names so they are more easily identifiable.
     */
    @com.idevicesinc.sweetblue.annotations.Advanced
    public static final String[] debugThreadNames                                       = {
                                                                                            "ABE", "BOB", "CAM", "DON", "ELI", "FAY", "GUS", "HAL", "IAN", "JAY", "KAY", "LEO",
                                                                                            "MAX", "NED", "OLA", "PAT", "QUE", "RON", "SAL", "TED", "UVA", "VAL", "WES", "XIB", "YEE", "ZED"
                                                                                          };

}
