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
import com.idevicesinc.sweetblue.utils.Utils_String;

/**
 * Provide an implementation to {@link BleServer#setListener_State(ServerStateListener)} and/or
 * {@link BleManager#setListener_ServerState(ServerStateListener)} to receive state change events.
 *
 * @see BleServerState
 * @see BleServer#setListener_State(ServerStateListener)
 */
@com.idevicesinc.sweetblue.annotations.Lambda
public interface ServerStateListener extends GenericListener_Void<ServerStateListener.StateEvent>
{
    /**
     * Subclass that adds the {@link #server()}, {@link #macAddress()}, and {@link #gattStatus()} fields.
     */
    @Immutable
    class StateEvent extends State.ChangeEvent<BleServerState>
    {
        /**
         * The server undergoing the state change.
         */
        public final BleServer server() {  return m_server;  }
        private final BleServer m_server;

        /**
         * Returns the mac address of the client that's undergoing the state change with this {@link #server()}.
         */
        public final String macAddress()  {  return m_macAddress;  }
        private final String m_macAddress;

        /**
         * The change in gattStatus that may have precipitated the state change, or {@link BleStatuses#GATT_STATUS_NOT_APPLICABLE}.
         * For example if {@link #didEnter(State)} with {@link BleServerState#DISCONNECTED} is <code>true</code> and
         * {@link #didExit(State)} with {@link BleServerState#CONNECTING} is also <code>true</code> then {@link #gattStatus()} may be greater
         * than zero and give some further hint as to why the connection failed.
         */
        public final int gattStatus() {  return m_gattStatus;  }
        private final int m_gattStatus;

        /*package*/ StateEvent(BleServer server, String macAddress, int oldStateBits, int newStateBits, int intentMask, int gattStatus)
        {
            super(oldStateBits, newStateBits, intentMask);

            m_server = server;
            m_gattStatus = gattStatus;
            m_macAddress = macAddress;
        }

        @Override public final String toString()
        {
            return Utils_String.toString
            (
                this.getClass(),
//					"server",			server().getName_debug(),
                "entered",	Utils_String.toString(enterMask(),						BleServerState.VALUES()),
                "exited", 			Utils_String.toString(exitMask(),						BleServerState.VALUES()),
                "current",			Utils_String.toString(newStateBits(),					BleServerState.VALUES()),
                "gattStatus",       CodeHelper.gattStatus(gattStatus(), true)
            );
        }
    }

    /**
     * Called when a server's bitwise {@link BleServerState} changes. As many bits as possible are flipped at the same time.
     */
    void onEvent(final StateEvent e);
}
