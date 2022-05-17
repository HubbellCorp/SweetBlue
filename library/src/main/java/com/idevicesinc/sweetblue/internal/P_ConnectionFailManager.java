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


import static com.idevicesinc.sweetblue.BleDeviceState.BLE_CONNECTED;
import static com.idevicesinc.sweetblue.BleDeviceState.BLE_CONNECTING;
import static com.idevicesinc.sweetblue.BleDeviceState.CONNECTING_OVERALL;
import static com.idevicesinc.sweetblue.BleDeviceState.RECONNECTING_LONG_TERM;

import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleStatuses;
import com.idevicesinc.sweetblue.DeviceConnectListener;
import com.idevicesinc.sweetblue.DeviceReconnectFilter;
import com.idevicesinc.sweetblue.DeviceReconnectFilter.ConnectFailEvent;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.ReconnectFilter;
import com.idevicesinc.sweetblue.ReconnectFilter.ConnectFailPlease;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Utils_Config;
import com.idevicesinc.sweetblue.utils.Utils_String;

import static com.idevicesinc.sweetblue.BleDeviceState.RETRYING_BLE_CONNECTION;
import static com.idevicesinc.sweetblue.internal.P_ConnectFailPlease.*;

import java.util.ArrayList;
import java.util.Stack;


final class P_ConnectionFailManager
{
    private final IBleDevice m_device;


    private final Stack<DeviceReconnectFilter> m_connectionFailListenerStack;

    private int m_failCount = 0;
    private BleDeviceState m_highestStateReached_total = null;

    private Long m_timeOfFirstConnect = null;
    private Long m_timeOfLastConnectFail = null;
    private P_ConnectFailPlease m_pendingConnectionRetry = null;

    // This boolean is here to prevent trying to reconnect when we've fallen out of reconnecting long term
    private boolean m_triedReconnectingLongTerm = false;

    private final ArrayList<ConnectFailEvent> m_history = new ArrayList<>();

    P_ConnectionFailManager(IBleDevice device)
    {
        m_device = device;

        m_connectionFailListenerStack = new Stack<>();

        resetFailCount();
    }

    final void onLongTermTimedOut()
    {
        m_triedReconnectingLongTerm = true;
    }

    final void onExplicitDisconnect()
    {
        resetFailCount();
    }

    final boolean hasPendingConnectionFailEvent()
    {
        return m_pendingConnectionRetry != null;
    }

    final P_ConnectFailPlease getPendingConnectionFailRetry()
    {
        return m_pendingConnectionRetry;
    }

    final void clearPendingRetry()
    {
        m_pendingConnectionRetry = null;
    }

    final void onFullyInitialized()
    {
        resetFailCount();
    }

    final void onExplicitConnectionStarted()
    {
        resetFailCount();

        m_timeOfFirstConnect = System.currentTimeMillis();
    }

    private void resetFailCount()
    {
        if (!m_device.isNull())
        {
            m_device.getIManager().getPostManager().runOrPostToUpdateThread(() -> {
                m_failCount = 0;
                m_highestStateReached_total = null;
                m_timeOfFirstConnect = m_timeOfLastConnectFail = null;
                m_history.clear();
                m_triedReconnectingLongTerm = false;
            });
        }
    }

    final int getRetryCount()
    {
        int retryCount = m_failCount;

        return retryCount;
    }

    final P_ConnectFailPlease onConnectionFailed(final P_DisconnectReason disconnectReason)
    {
        if (disconnectReason.getConnectFailReason() == null) return DO_NOT_RETRY;

        final long currentTime = System.currentTimeMillis();

        //--- DRK > Can be null if this is a spontaneous connect (can happen with autoConnect sometimes for example).
        m_timeOfFirstConnect = m_timeOfFirstConnect != null ? m_timeOfFirstConnect : currentTime;
        final Long timeOfLastConnectFail = m_timeOfLastConnectFail != null ? m_timeOfLastConnectFail : m_timeOfFirstConnect;
        final Interval attemptTime_latest = Interval.delta(timeOfLastConnectFail, currentTime);
        final Interval attemptTime_total = Interval.delta(m_timeOfFirstConnect, currentTime);

        m_device.getIManager().getLogger().w(Utils_String.makeString(disconnectReason.getConnectFailReason(), ", timing=", disconnectReason.getTiming()));

        if (disconnectReason.getIsAttemptingLongTermReconnect())
        {
            m_failCount = 1;
        }
        else
        {
            m_failCount++;
        }

        if (m_highestStateReached_total == null)
        {
            m_highestStateReached_total = disconnectReason.getHighestState();
        }
        else
        {
            if (disconnectReason.isHighestStateReachedHigherThan(P_Bridge_User.getConnectionOrdinal(m_highestStateReached_total)))
            {
                m_highestStateReached_total = disconnectReason.getHighestState();
            }
        }

        final ConnectFailEvent moreInfo = P_Bridge_User.newConnectFailEvent(m_device.getBleDevice(), m_failCount, disconnectReason, m_highestStateReached_total, attemptTime_latest, attemptTime_total);

        addToHistory(moreInfo);

        //--- DRK > Not invoking callback if we're attempting short-term reconnect.
        P_ConnectFailPlease retryChoice__PE_Please = m_device.is(BleDeviceState.RECONNECTING_SHORT_TERM) ? DO_NOT_RETRY : invokeCallback(moreInfo, false);

        //--- DRK > Retry choice doesn't matter if we're attempting reconnect.
        retryChoice__PE_Please = !disconnectReason.getIsAttemptingLongTermReconnect() ? retryChoice__PE_Please : DO_NOT_RETRY;

        //--- DRK > Disabling retry if app-land decided to call connect() themselves in fail callback...hopefully fringe but must check for now.
        //--- RB > Commenting this out for now. If the user calls connect in the fail callback, it gets posted to the update thread, so this shouldn't
        // be an issue anymore. Right now with the new changes to threading, this is causing issues (so a reconnect attempt doesn't happen when it should)
//		retryChoice__PE_Please = m_device.is_internal(BleDeviceState.CONNECTING_OVERALL) ? Please.PE_Please_DO_NOT_RETRY : retryChoice__PE_Please;

        if (disconnectReason.isCanceled())
        {
            retryChoice__PE_Please = DO_NOT_RETRY;
        }
        else
        {
            final P_ReconnectManager reconnectMngr = m_device.reconnectMngr();
            final int gattStatusOfOriginalDisconnect = reconnectMngr.gattStatusOfOriginalDisconnect();
            final boolean wasRunning = reconnectMngr.isRunning();

            reconnectMngr.onConnectionFailed(moreInfo);

            if (wasRunning && !reconnectMngr.isRunning())
            {
                if (m_device.is(RECONNECTING_LONG_TERM))
                {
                    m_triedReconnectingLongTerm = true;
                }
                else if (m_device.is(BleDeviceState.RECONNECTING_SHORT_TERM))
                {
                    retryChoice__PE_Please = DO_NOT_RETRY;
                    m_device.onNativeDisconnect(false, gattStatusOfOriginalDisconnect, false, true);
                }
            }
        }

        final boolean retryConnectOverall = Utils_Config.bool(m_device.conf_device().connectFailRetryConnectingOverall, m_device.conf_mngr().connectFailRetryConnectingOverall);

        if (!m_triedReconnectingLongTerm && retryChoice__PE_Please != NULL && retryChoice__PE_Please.isRetry() && !m_device.is(BLE_CONNECTED))
        {
            if (m_device.isAny_internal(BLE_CONNECTED, BLE_CONNECTING, CONNECTING_OVERALL))
            {
                m_device.setStateToDisconnected(disconnectReason.getIsAttemptingLongTermReconnect(), true, PA_StateTracker.E_Intent.UNINTENTIONAL, disconnectReason.getGattStatus());
            }
            m_device.getConnectionManager().attemptReconnect();
        }
        else
        {
            if (!m_device.is(BLE_CONNECTED))
            {
                m_failCount = 0;
            }
            else
            {
                m_pendingConnectionRetry = retryChoice__PE_Please;
            }
            if (m_device.isAny_internal(BLE_CONNECTED, BLE_CONNECTING, CONNECTING_OVERALL))
            {
                boolean isretryConnectOverall = retryChoice__PE_Please.isRetry() && retryConnectOverall;
                m_device.setStateToDisconnected(disconnectReason.getIsAttemptingLongTermReconnect(), isretryConnectOverall, PA_StateTracker.E_Intent.UNINTENTIONAL, disconnectReason.getGattStatus());
            }
        }

        return retryChoice__PE_Please;
    }

    final ArrayList<ConnectFailEvent> getHistory()
    {
        return new ArrayList<>(m_history);
    }

    private void addToHistory(ConnectFailEvent event)
    {
        int maxSize = Math.max(Utils_Config.integer(m_device.conf_device().maxConnectionFailHistorySize, m_device.conf_mngr().maxConnectionFailHistorySize), 1);
        if (m_history.size() >= maxSize)
        {
            m_history.remove(0);
        }
        m_history.add(event);
    }

    final P_ConnectFailPlease invokeCallback(final ConnectFailEvent moreInfo, boolean skipRetryLogic)
    {
        // We now pump the event to both the manager, and device listeners. However, if there is both set, the device one will override
        // anything set in the default manager one. If neither are set, then we use the static default one.
        P_ConnectFailPlease retryChoice__PE_Please = NULL;

        if (!skipRetryLogic)
        {
            final DeviceReconnectFilter listener = getListener();
            boolean dispatched = false;

            final DeviceReconnectFilter filter = m_device.getIManager().getDefaultDeviceReconnectFilter();

            if (filter != null && listener == null)
            {
                final ConnectFailPlease please = filter.onConnectFailed(moreInfo);
                retryChoice__PE_Please = please != null ? P_Bridge_User.internalPlease(please) : NULL;

                m_device.getIManager().getLogger().checkPlease(please, ConnectFailPlease.class);

                dispatched = true;
            }

            if (listener != null)
            {
                final ConnectFailPlease please = listener.onConnectFailed(moreInfo);
                retryChoice__PE_Please = please != null ? P_Bridge_User.internalPlease(please) : NULL;

                m_device.getIManager().getLogger().checkPlease(please, ConnectFailPlease.class);

                dispatched = true;
            }

            if (!dispatched)
            {
                final ConnectFailPlease please = P_BleDeviceImpl.DEFAULT_CONNECTION_FAIL_LISTENER.onConnectFailed(moreInfo);

                if (please != null && please.isRetry())
                    m_device.getStateTracker().update(PA_StateTracker.E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, RETRYING_BLE_CONNECTION, true);

                retryChoice__PE_Please = please != null ? P_Bridge_User.internalPlease(please) : NULL;

                m_device.getIManager().getLogger().checkPlease(please, ConnectFailPlease.class);
            }

            retryChoice__PE_Please = retryChoice__PE_Please != NULL ? retryChoice__PE_Please : DO_NOT_RETRY;

        }

        final DeviceConnectListener.ConnectEvent event = P_Bridge_User.newConnectEvent(m_device.getBleDevice(), moreInfo, retryChoice__PE_Please.isRetry());
        m_device.getConnectionManager().invokeConnectCallbacks(event);

        return retryChoice__PE_Please;
    }

    public final DeviceReconnectFilter getListener()
    {
        if (m_connectionFailListenerStack.empty())  return null;

        return m_connectionFailListenerStack.peek();
    }

    public final void setListener(DeviceReconnectFilter listener)
    {
        m_connectionFailListenerStack.clear();
        m_connectionFailListenerStack.push(listener);
    }

    public final void clearListenerStack()
    {
        m_connectionFailListenerStack.clear();
    }

    public final void pushListener(DeviceReconnectFilter listener)
    {
        if (listener != null)
            m_connectionFailListenerStack.push(listener);
    }

    public final boolean popListener()
    {
        if (m_device.isNull() || m_connectionFailListenerStack.empty())
            return false;

        m_connectionFailListenerStack.pop();
        return true;
    }

    public final boolean popListener(DeviceReconnectFilter listener)
    {
        if (m_device.isNull()) return false;

        return !m_connectionFailListenerStack.empty() && m_connectionFailListenerStack.remove(listener);
    }
}
