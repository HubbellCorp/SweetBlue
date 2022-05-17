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

package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.internal.IBleDevice;
import com.idevicesinc.sweetblue.utils.GattDatabase;
import com.idevicesinc.sweetblue.utils.Interval;

/**
 * Basic sub-class of {@link UnitTestBluetoothGatt} which will fail all reads/writes. Use {@link FailType} to specify what
 * type of failure.
 */
public class ReadWriteFailBluetoothGatt extends UnitTestBluetoothGatt
{

    private final FailType m_failType;


    public ReadWriteFailBluetoothGatt(IBleDevice device, GattDatabase gattDb)
    {
        this(device, gattDb, FailType.GATT_ERROR, Interval.DISABLED);
    }

    public ReadWriteFailBluetoothGatt(IBleDevice device, GattDatabase gattDb, FailType failType)
    {
        this(device, gattDb, failType, Interval.DISABLED);
    }

    public ReadWriteFailBluetoothGatt(IBleDevice device, GattDatabase gattDb, FailType failType, Interval delayTime)
    {
        super(device, gattDb);
        m_failType = failType;
        setDelayTime(delayTime);
    }

    @Override
    public void sendReadResponse(BleCharacteristic characteristic, byte[] data)
    {
        if (m_failType == FailType.GATT_ERROR)
            Util_Native.readError(getBleDevice(), characteristic, BleStatuses.GATT_ERROR, getDelayTime());
        else
        {
            // If it's a time out, just do nothing
        }
    }

    /**
     * Enumeration used to specify the type of failure.
     */
    public enum FailType
    {
        /**
         * This simulates the dreaded gatt 133 error.
         */
        GATT_ERROR,

        /**
         * Simulates a read/write timeout.
         */
        TIME_OUT
    }
}
