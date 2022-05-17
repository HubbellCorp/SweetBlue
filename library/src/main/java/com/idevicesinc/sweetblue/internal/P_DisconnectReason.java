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

package com.idevicesinc.sweetblue.internal;

import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleStatuses;
import com.idevicesinc.sweetblue.DeviceReconnectFilter;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.ReadWriteListener;
import com.idevicesinc.sweetblue.ReconnectFilter;
import com.idevicesinc.sweetblue.utils.Utils_Config;

/**
 * Class used to hold various objects related to disconnecting. This is an internal only class, not meant for public consumption.
 */
public final class P_DisconnectReason
{

    private PE_TaskPriority m_disconnectPriority_nullable = null;
    private DeviceReconnectFilter.Status m_connectionFailReasonIfConnecting = null;
    private DeviceReconnectFilter.Timing m_timing = DeviceReconnectFilter.Timing.NOT_APPLICABLE;
    private final int gattStatus;
    private int m_bondFailReason = BleStatuses.BOND_FAIL_REASON_NOT_APPLICABLE;
    private boolean m_undiscoverAfter = false;
    private boolean m_isAttemptingLongTermReconnect = false;
    private boolean m_isForcedRemoteDisconnect = false;
    private BleDeviceState m_highestState = BleDeviceState.NULL;
    private ReconnectFilter.AutoConnectUsage m_autoConnectUsage = ReconnectFilter.AutoConnectUsage.NOT_APPLICABLE;
    private ReadWriteListener.ReadWriteEvent m_txnFailReason;


    P_DisconnectReason(int gattStatus)
    {
        this.gattStatus = gattStatus;
    }

    P_DisconnectReason(int gattStatus, DeviceReconnectFilter.Timing timing)
    {
        this.gattStatus = gattStatus;
        m_timing = timing;
    }


    public final int getGattStatus()
    {
        return gattStatus;
    }

    public final P_DisconnectReason setTiming(DeviceReconnectFilter.Timing timing)
    {
        m_timing = timing;
        return this;
    }

    public final DeviceReconnectFilter.Timing getTiming()
    {
        return m_timing;
    }

    public final P_DisconnectReason setPriority(PE_TaskPriority priority)
    {
        m_disconnectPriority_nullable = priority;
        return this;
    }

    public final PE_TaskPriority getPriority()
    {
        return m_disconnectPriority_nullable;
    }

    public final P_DisconnectReason setConnectFailReason(DeviceReconnectFilter.Status reason)
    {
        m_connectionFailReasonIfConnecting = reason;
        return this;
    }

    public final DeviceReconnectFilter.Status getConnectFailReason()
    {
        return m_connectionFailReasonIfConnecting;
    }

    public final P_DisconnectReason setBondFailReason(int failReason)
    {
        m_bondFailReason = failReason;
        return this;
    }

    public final int getBondFailReason()
    {
        return m_bondFailReason;
    }

    public final P_DisconnectReason setUndiscoverAfter(boolean undiscoverAfter)
    {
        m_undiscoverAfter = undiscoverAfter;
        return this;
    }

    public final boolean getIsUndiscoverAfter()
    {
        return m_undiscoverAfter;
    }

    public final P_DisconnectReason setTxnFailReason(ReadWriteListener.ReadWriteEvent txnReason)
    {
        m_txnFailReason = txnReason;
        return this;
    }

    public final ReadWriteListener.ReadWriteEvent getTxnFailReason()
    {
        return m_txnFailReason;
    }

    public final P_DisconnectReason setAttemptingLongTermReconnect(boolean attemptingLongTermReconnect)
    {
        m_isAttemptingLongTermReconnect = attemptingLongTermReconnect;
        return this;
    }

    public final boolean getIsAttemptingLongTermReconnect()
    {
        return m_isAttemptingLongTermReconnect;
    }

    public final boolean getIsForcedRemoteDisconnect()
    {
        return m_isForcedRemoteDisconnect;
    }

    public final P_DisconnectReason setIsForcedRemoteDisconnect(boolean isForcedRemoteDisconnect)
    {
        m_isForcedRemoteDisconnect = isForcedRemoteDisconnect;
        return this;
    }

    public final P_DisconnectReason setHighestState(BleDeviceState highestState)
    {
        m_highestState = highestState;
        return this;
    }

    public final BleDeviceState getHighestState()
    {
        if (m_highestState == null)
            return BleDeviceState.NULL;
        return m_highestState;
    }

    public final P_DisconnectReason setAutoConnectUsage(ReconnectFilter.AutoConnectUsage usage)
    {
        m_autoConnectUsage = usage;
        return this;
    }

    public final ReconnectFilter.AutoConnectUsage getAutoConnectUsage()
    {
        return m_autoConnectUsage;
    }


    //// Some convenience methods

    public final boolean isCanceled()
    {
        return m_connectionFailReasonIfConnecting != null && m_connectionFailReasonIfConnecting.wasCancelled();
    }

    public final boolean isExplicit()
    {
        return m_connectionFailReasonIfConnecting != null && P_Bridge_User.wasExplicit(m_connectionFailReasonIfConnecting);
    }

    public final boolean shouldTryBondingWhileDisconnected(BleDevice device)
    {
        final IBleDevice dev = P_Bridge_User.getIBleDevice(device);
        return m_connectionFailReasonIfConnecting == DeviceReconnectFilter.Status.BONDING_FAILED && Utils_Config.bool(dev.conf_device().tryBondingWhileDisconnected, dev.conf_mngr().tryBondingWhileDisconnected);
    }

    public final boolean isRogueDisconnect()
    {
        return m_connectionFailReasonIfConnecting != null && m_connectionFailReasonIfConnecting == DeviceReconnectFilter.Status.ROGUE_DISCONNECT;
    }

    public final boolean isHighestStateReachedHigherThan(int connectionOrdinal)
    {
        return m_highestState != null && P_Bridge_User.getConnectionOrdinal(m_highestState) > connectionOrdinal;
    }

}
