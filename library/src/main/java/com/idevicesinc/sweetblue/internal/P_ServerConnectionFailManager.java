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


import com.idevicesinc.sweetblue.P_Bridge_User;
import static com.idevicesinc.sweetblue.internal.P_ConnectFailPlease.*;
import com.idevicesinc.sweetblue.ServerReconnectFilter;
import com.idevicesinc.sweetblue.defaults.DefaultServerReconnectFilter;
import com.idevicesinc.sweetblue.internal.android.P_DeviceHolder;

import java.util.HashMap;


final class P_ServerConnectionFailManager
{
	private static ServerReconnectFilter DEFAULT_CONNECTION_FAIL_LISTENER = new DefaultServerReconnectFilter();

	final IBleServer m_server;

	private ServerReconnectFilter m_connectionFailListener = DEFAULT_CONNECTION_FAIL_LISTENER;

	private final HashMap<String, P_ServerConnectionFailEntry> m_entries = new HashMap<>();

	P_ServerConnectionFailManager(final IBleServer server)
	{
		m_server = server;
	}

	private P_ServerConnectionFailEntry getOrCreateEntry(final String macAddress)
	{
		final P_ServerConnectionFailEntry entry_nullable = m_entries.get(macAddress);

		if( entry_nullable != null )
		{
			return entry_nullable;
		}
		else
		{
			final P_ServerConnectionFailEntry entry = new P_ServerConnectionFailEntry(this);

			m_entries.put(macAddress, entry);

			return entry;
		}
	}

	void onExplicitDisconnect(final String macAddress)
	{
		getOrCreateEntry(macAddress).onExplicitDisconnect();
	}

	void onExplicitConnectionStarted(final String macAddress)
	{
		getOrCreateEntry(macAddress).onExplicitConnectionStarted();
	}

	public void setListener(ServerReconnectFilter listener)
	{
		m_connectionFailListener = listener;
	}

	public ServerReconnectFilter getListener()
	{
		return m_connectionFailListener;
	}

	void onNativeConnectFail(final P_DeviceHolder nativeDevice, final ServerReconnectFilter.Status status, final int gattStatus)
	{
		getOrCreateEntry(nativeDevice.getAddress()).onNativeConnectFail(nativeDevice, status, gattStatus);
	}

	P_ConnectFailPlease invokeCallback(final ServerReconnectFilter.ConnectFailEvent e)
	{
		final P_ConnectFailPlease ePlease__PE_Please;

		if( m_connectionFailListener != null )
		{
			final ServerReconnectFilter.ConnectFailPlease please = m_connectionFailListener.onConnectFailed(e);

			ePlease__PE_Please = please != null ? P_Bridge_User.internalPlease(please) : DO_NOT_RETRY;
		}
		else
		{
			final ServerReconnectFilter filter = m_server.getIManager().getDefaultServerReconnectFilter();
			if (filter != null)
			{
				final ServerReconnectFilter.ConnectFailPlease please = m_server.getIManager().getDefaultServerReconnectFilter().onConnectFailed(e);

				ePlease__PE_Please = please != null ? P_Bridge_User.internalPlease(please) : DO_NOT_RETRY;
			}
			else
				ePlease__PE_Please = DO_NOT_RETRY;
		}

		return ePlease__PE_Please;
	}
}
