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

import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.utils.Event;
import com.idevicesinc.sweetblue.utils.GenericListener_Void;


public interface ServerConnectListener extends GenericListener_Void<ServerConnectListener.ConnectEvent>
{

    class ConnectEvent extends Event
    {

        private final BleServer m_server;
        private final String m_macAddress;
        private final ServerReconnectFilter.ConnectFailEvent m_failEvent;

        ConnectEvent(BleServer server, String macAddress, ServerReconnectFilter.ConnectFailEvent failEvent)
        {
            m_server = server;
            m_macAddress = macAddress;
            m_failEvent = failEvent;
        }


        public final @Nullable(Nullable.Prevalence.NEVER) BleServer server()
        {
            return m_server;
        }

        public final @Nullable(Nullable.Prevalence.NEVER) String macAddress()
        {
            return m_macAddress;
        }

        /**
         * Returns the {@link com.idevicesinc.sweetblue.ServerReconnectFilter.ConnectFailEvent} instance. This will be <code>null</code> if
         * {@link #wasSuccess()} returns <code>true</code>.
         */
        public final @Nullable(Nullable.Prevalence.NORMAL) ServerReconnectFilter.ConnectFailEvent failEvent()
        {
            return m_failEvent;
        }

        /**
         * Returns <code>true</code> if a connection was established to the device with {@link #macAddress()}. At this point, the device is connected,
         * and ready to be communicated with. If this returns <code>false</code>, you can retrieve failure information
         * by calling {@link #failEvent()}.
         */
        public final boolean wasSuccess()
        {
            return m_server.is(m_macAddress, BleServerState.CONNECTED);
        }

        /**
         * Convenience method to check if this connection is going to be retried or not. This simply calls {@link BleServer#is(String, BleServerState)}, with
         * {@link #macAddress()}, and {@link BleServerState#RETRYING_CONNECTION}.
         */
        public final boolean isRetrying()
        {
            return m_server.is(m_macAddress, BleServerState.RETRYING_CONNECTION);
        }

    }

}
