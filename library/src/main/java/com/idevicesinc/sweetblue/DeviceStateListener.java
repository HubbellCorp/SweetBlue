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


import com.idevicesinc.sweetblue.annotations.Immutable;
import com.idevicesinc.sweetblue.utils.CodeHelper;
import com.idevicesinc.sweetblue.utils.GenericListener_Void;
import com.idevicesinc.sweetblue.utils.State;
import com.idevicesinc.sweetblue.utils.Utils_State;
import com.idevicesinc.sweetblue.utils.Utils_String;
import static com.idevicesinc.sweetblue.BleDeviceState.CONNECTED;
import static com.idevicesinc.sweetblue.BleDeviceState.CONNECTING;
import static com.idevicesinc.sweetblue.BleDeviceState.DISCONNECTED;
import static com.idevicesinc.sweetblue.BleDeviceState.RECONNECTING_SHORT_TERM;

/**
 * Provide an implementation to {@link BleDevice#setListener_State(DeviceStateListener)} and/or
 * {@link BleManager#setListener_DeviceState(DeviceStateListener)} to receive state change events.
 *
 * @see BleDeviceState
 * @see BleDevice#setListener_State(DeviceStateListener)
 */
@com.idevicesinc.sweetblue.annotations.Lambda
public interface DeviceStateListener extends GenericListener_Void<DeviceStateListener.StateEvent>
{

    /**
     * Subclass that adds the device field.
     */
    @Immutable
    class StateEvent extends State.ChangeEvent<BleDeviceState>
    {
        /**
         * The device undergoing the state change.
         */
        public final BleDevice device()
        {
            return m_device;
        }

        private final BleDevice m_device;

        /**
         * Convience to return the mac address of {@link #device()}.
         */
        public final String macAddress()
        {
            return m_device.getMacAddress();
        }

        /**
         * The change in gattStatus that may have precipitated the state change, or {@link BleStatuses#GATT_STATUS_NOT_APPLICABLE}.
         * For example if {@link #didEnter(State)} with {@link BleDeviceState#BLE_DISCONNECTED} is <code>true</code> and
         * {@link #didExit(State)} with {@link BleDeviceState#BLE_CONNECTING} is also <code>true</code> then {@link #gattStatus()} may be greater
         * than zero and give some further hint as to why the connection failed.
         * <br><br>
         * See {@link DeviceReconnectFilter.ConnectFailEvent#gattStatus()} for more information.
         */
        public final int gattStatus()
        {
            return m_gattStatus;
        }

        private final int m_gattStatus;

        private final boolean m_isSimple;

        /**
         * Returns <code>true</code> if any of the simple states have changed. The simple states are really the only ones most apps should care about. So, on state changes, you
         * can just call this method to see if you should do anything or not (barring special circumstances).
         */
        public final boolean isSimple()
        {
            return m_isSimple;
        }

        StateEvent(BleDevice device, int oldStateBits, int newStateBits, int intentMask, int gattStatus)
        {
            super(oldStateBits, newStateBits, intentMask);

            m_isSimple = checkSimpleStateChange(oldStateBits, newStateBits);

            this.m_device = device;
            this.m_gattStatus = gattStatus;
        }

        private boolean checkSimpleStateChange(int oldStateBits, int newStateBits)
        {
            boolean newDisconnect = Utils_State.query(newStateBits, DISCONNECTED);
            boolean oldDisconnect = Utils_State.query(oldStateBits, DISCONNECTED);
            if (newDisconnect != oldDisconnect)
                return true;
            boolean newConnecting = Utils_State.query(newStateBits, CONNECTING);
            boolean oldConnecting = Utils_State.query(oldStateBits, CONNECTING);
            if (newConnecting != oldConnecting)
                return true;
            boolean newConnected = Utils_State.query(newStateBits, CONNECTED);
            boolean oldConnected = Utils_State.query(oldStateBits, CONNECTED);
            if (newConnected != oldConnected)
                return true;
            return false;
        }

        @Override public final String toString()
        {
            if (device().is(RECONNECTING_SHORT_TERM))
            {
                return Utils_String.toString
                        (
                                this.getClass(),
                                "device", device().getName_debug(),
                                "entered", Utils_String.toString(enterMask(), BleDeviceState.VALUES()),
                                "exited", Utils_String.toString(exitMask(), BleDeviceState.VALUES()),
                                "current", Utils_String.toString(newStateBits(), BleDeviceState.VALUES()),
                                "current_native", Utils_String.toString(device().getNativeStateMask(), BleDeviceState.VALUES()),
                                "gattStatus", CodeHelper.gattStatus(gattStatus(), true)
                        );
            }
            else
            {
                return Utils_String.toString
                        (
                                this.getClass(),
                                "device", device().getName_debug(),
                                "entered", Utils_String.toString(enterMask(), BleDeviceState.VALUES()),
                                "exited", Utils_String.toString(exitMask(), BleDeviceState.VALUES()),
                                "current", Utils_String.toString(newStateBits(), BleDeviceState.VALUES()),
                                "gattStatus", CodeHelper.gattStatus(gattStatus(), true)
                        );
            }
        }
    }

    @Override
    void onEvent(StateEvent e);
}