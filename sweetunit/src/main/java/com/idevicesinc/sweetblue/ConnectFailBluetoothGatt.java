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
 * Implementation of {@link UnitTestBluetoothGatt} which will fail connection attempts. You can set where the failure point is, and the type.
 *
 * @see FailurePoint
 * @see FailureType
 */
public class ConnectFailBluetoothGatt extends UnitTestBluetoothGatt
{

    private final FailurePoint m_failPoint;
    private final FailureType m_failType;


    /**
     * Overload of {@link #ConnectFailBluetoothGatt(IBleDevice, FailurePoint, FailureType)}, which sets the failure type to
     * {@link FailureType#DISCONNECT_GATT_ERROR}.
     */
    public ConnectFailBluetoothGatt(IBleDevice device, FailurePoint failPoint)
    {
        this(device, failPoint, FailureType.DISCONNECT_GATT_ERROR);
    }

    /**
     * Overload of {@link #ConnectFailBluetoothGatt(IBleDevice, FailurePoint, FailureType, Interval)} which doesn't delay the failure at all.
     */
    public ConnectFailBluetoothGatt(IBleDevice device, FailurePoint failPoint, FailureType failType)
    {
        this(device, failPoint, failType, Interval.DISABLED);
    }

    /**
     * Base constructor used when not providing a {@link GattDatabase} instance. The {@link FailurePoint} dictates where in the process
     * the failure will happen, and the {@link FailureType} is the type of failure to simulate.
     */
    public ConnectFailBluetoothGatt(IBleDevice device, FailurePoint failPoint, FailureType failType, Interval delayTime)
    {
        super(device);
        m_failPoint = failPoint;
        m_failType = failType;
        setDelayTime(delayTime);
    }

    /**
     * Overload of {@link #ConnectFailBluetoothGatt(IBleDevice, GattDatabase, FailurePoint, FailureType, Interval)} which doesn't delay the failure at all.
     */
    public ConnectFailBluetoothGatt(IBleDevice device, GattDatabase gattDb, FailurePoint failPoint, FailureType failType)
    {
        this(device, gattDb, failPoint, failType, Interval.DISABLED);
    }

    /**
     * Base constructor used when passing in a {@link GattDatabase} instance. The {@link FailurePoint} dictates where in the process
     * the failure will happen, and the {@link FailureType} is the type of failure to simulate.
     */
    public ConnectFailBluetoothGatt(IBleDevice device, GattDatabase gattDb, FailurePoint failPoint, FailureType failType, Interval delayTime)
    {
        super(device, gattDb);
        m_failPoint = failPoint;
        m_failType = failType;
        setDelayTime(delayTime);
    }

    @Override
    public void setToConnecting()
    {
        if (m_failPoint == FailurePoint.PRE_CONNECTING_BLE)
        {
            Util_Native.setToDisconnected(getBleDevice(), getStatus(), getDelayTime());
        }
        else
        {
            super.setToConnecting();
        }
    }

    @Override
    public void setToConnected()
    {
        if (m_failPoint == FailurePoint.POST_CONNECTING_BLE)
        {
            Util_Native.setToDisconnected(getBleDevice(), getStatus(), getDelayTime());
        }
        else
        {
            super.setToConnected();
        }
    }

    @Override
    public void setServicesDiscovered()
    {
        if (m_failPoint == FailurePoint.SERVICE_DISCOVERY)
        {
            if (m_failType == FailureType.SERVICE_DISCOVERY_FAILED)
            {
                Util_Native.failDiscoverServices(getBleDevice(), getStatus(), getDelayTime());
            }
            else
            {
                Util_Native.setToDisconnected(getBleDevice(), getStatus(), getDelayTime());
            }
        }
    }

    private int getStatus()
    {
        switch (m_failType)
        {
            case DISCONNECT_GATT_ERROR:
            case SERVICE_DISCOVERY_FAILED:
                return BleStatuses.GATT_ERROR;
            default:
                return BleStatuses.GATT_STATUS_NOT_APPLICABLE;
        }
    }

    /**
     * Enumeration used to dictate where in the connection process the failure should happen.
     */
    public enum FailurePoint
    {
        /**
         * This simulates a failure that happens before the device even gets to the connecting state.
         */
        PRE_CONNECTING_BLE,

        /**
         * This simulates a failure that happens after the device gets into the connecting state.
         */
        POST_CONNECTING_BLE,

        /**
         * This simulates a failure after actually getting connected, but service discovery fails.
         */
        SERVICE_DISCOVERY
    }

    /**
     * Enumeration used to dictate what type of failure should be reported.
     */
    public enum FailureType
    {
        /**
         * This basically simulates the dreaded 133 gatt error.
         */
        DISCONNECT_GATT_ERROR,

        /**
         * This failure simulates a simple time out.
         */
        TIMEOUT,

        /**
         * This will essentially look like {@link #DISCONNECT_GATT_ERROR}, just that it's used only when service discovery fails.
         */
        SERVICE_DISCOVERY_FAILED
    }
}
