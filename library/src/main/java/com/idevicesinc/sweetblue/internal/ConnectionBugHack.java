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

import com.idevicesinc.sweetblue.BleDeviceConfig;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BondFilter;
import com.idevicesinc.sweetblue.BondListener;
import com.idevicesinc.sweetblue.DeviceConnectListener;
import com.idevicesinc.sweetblue.DeviceStateListener;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.UhOhListener;
import com.idevicesinc.sweetblue.utils.Interval;
import static com.idevicesinc.sweetblue.BleDeviceState.BLE_CONNECTED;
import static com.idevicesinc.sweetblue.BleDeviceState.BLE_DISCONNECTED;
import static com.idevicesinc.sweetblue.BleDeviceState.BONDED;


public class ConnectionBugHack
{

    private final IBleDevice m_device;
    private BleDeviceConfig m_cachedConfig;
    private BleDeviceState[] m_cachedStates;
    private double m_timeFixing = 0.0;
    private boolean m_isFixing;
    private final DeviceStateListener m_stateListener;
    private final DeviceConnectListener m_connectListener;


    public ConnectionBugHack(IBleDevice device)
    {
        m_device = device;
        m_stateListener = new TempStateListener();
        m_connectListener = new EmptyConnectionListener();
    }


    public final void checkAndHandleConnectionBug()
    {
        if (!m_isFixing)
        {
            if (checkConnectionStatus())
            {
                m_isFixing = true;
                BondFilter.ConnectionBugEvent.Please please = m_device.getConfig().bondFilter.onEvent(P_Bridge_User.newBondConnectionBugEvent(m_device.getBleDevice()));
                if (P_Bridge_User.shouldTryConnectionBugFix(please))
                {
                    prepForFix();
                    m_device.unbond(e ->
                    {
                        if (e.wasSuccess() && e.type() == BondListener.BondEvent.Type.UNBOND)
                        {
                            m_device.bond(e1 ->
                            {
                                if (e1.wasSuccess() && e1.type() == BondListener.BondEvent.Type.BOND)
                                {
                                    // We pass no listener in here, as we set a state listener above. It is there where we will then disconnect
                                    m_device.getBleDevice().connect();
                                }
                                else
                                {
                                    getLogger().w("Couldn't get bonded again as part of the connection fix! BondEvent: " + e1.toString());
                                    reset();
                                }
                            });
                        }
                    });
                }
            }
        }
    }




    final void update(double timeStep)
    {
        if (m_isFixing)
        {
            m_timeFixing += timeStep;
            final Interval timeout = m_device.getConfig().connectionBugFixTimeout;
            if (Interval.isEnabled(timeout) && m_timeFixing >= timeout.secs())
            {
                getLogger().w("Connection bug fix timed out for device with mac address of " + m_device.getMacAddress());
                if (!m_device.is(BLE_DISCONNECTED))
                    m_device.disconnect();
                reset();
            }
        }
    }




    private void prepForFix()
    {
        m_cachedConfig = m_device.getConfig();
        m_cachedStates = m_cachedConfig.defaultDeviceStates;
        m_cachedConfig.defaultDeviceStates = BleDeviceState.VALUES();
        m_device.setConfig(m_cachedConfig);
        m_device.pushListener_Connect(m_connectListener);
        m_device.pushListener_State(m_stateListener);
    }

    private void reset()
    {
        m_timeFixing = 0.0;
        m_isFixing = false;
        m_device.popListener_State(m_stateListener);
        m_device.popListener_Connect(m_connectListener);
        if (m_cachedConfig != null)
        {
            m_cachedConfig.defaultDeviceStates = m_cachedStates;
            m_device.setConfig(m_cachedConfig);
        }
    }

    private boolean checkConnectionStatus()
    {
        // On the first bond to a device, Android leaves the connection open almost 100% of the time. However, when probing for the state, it will report
        // as being disconnected. Yet, no other phones will discover said device, and a BLE sniffer has proven that the connection IS still alive.
        // This check is here to detect this condition.
        boolean isDisconnected = !m_device.is(BLE_CONNECTED);
        if (isDisconnected)
        {
            boolean lowLevelIsConnected = m_device.getNative().isConnected();
            if (lowLevelIsConnected)
            {
                // We think we're disconnected, but the low level check is telling us otherwise. So we're pretty sure at this point, we're in this case where
                // the connection is actually open, but android's reporting it as not so.
                m_device.getIManager().uhOh(UhOhListener.UhOh.CONNECTION_STILL_ALIVE);
                return true;
            }
        }
        return false;
    }

    private P_Logger getLogger()
    {
        return m_device.getIManager().getLogger();
    }



    private final static class EmptyConnectionListener implements DeviceConnectListener
    {
        @Override
        public void onEvent(ConnectEvent e)
        {
        }
    }

    private final class TempStateListener implements DeviceStateListener
    {
        @Override
        public void onEvent(StateEvent e)
        {
            if (e.didEnter(BLE_CONNECTED))
            {
                m_device.disconnect();
                reset();
            }
        }
    }



}
