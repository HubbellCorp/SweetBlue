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


import com.idevicesinc.sweetblue.BleStatuses;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.di.SweetDIManager;
import com.idevicesinc.sweetblue.internal.android.AndroidBluetoothServer;
import com.idevicesinc.sweetblue.internal.android.IBluetoothDevice;
import com.idevicesinc.sweetblue.internal.android.IBluetoothServer;
import com.idevicesinc.sweetblue.internal.android.P_DeviceHolder;
import com.idevicesinc.sweetblue.internal.android.P_ServerHolder;

import java.util.HashMap;
import java.util.HashSet;


final class P_BleServerNativeManager
{

	private final IBleServer m_server;
	private final IBleManager m_mngr;

	private IBluetoothServer m_nativeLayer;
	private final P_BleServer_ListenerProcessor m_nativeListener;

	private final HashMap<String, Integer> m_nativeConnectionStates = new HashMap<>();
	private final HashSet<String> m_ignoredDisconnects = new HashSet<>();

	
	P_BleServerNativeManager(IBleServer server )
	{
		m_server = server;
		m_mngr = m_server.getIManager();

		m_nativeListener = server.isNull() ? null : new P_BleServer_ListenerProcessor(server);

		if( server.isNull() )
			m_nativeLayer = null;
	}


	final P_BleServer_ListenerProcessor getNativeListener()
	{
		return m_nativeListener;
	}

	final void ignoreNextImplicitDisconnect(final String macAddress)
	{
		m_ignoredDisconnects.add(macAddress);
	}

	final boolean shouldIgnoreImplicitDisconnect(final String macAddress)
	{
		final boolean toReturn = m_ignoredDisconnects.contains(macAddress);

		clearImplicitDisconnectIgnoring(macAddress);

		return toReturn;
	}

	final void clearImplicitDisconnectIgnoring(final String macAddress)
	{
		m_ignoredDisconnects.remove(macAddress);
	}

	final void closeServer()
	{
		if( m_nativeLayer == null || m_nativeLayer.isServerNull() )
		{
			m_mngr.ASSERT(false, "Native server is already closed and nulled out.");
		}
		else
		{
			final IBluetoothServer native_local = m_nativeLayer;

			m_nativeLayer = null;

			native_local.close();
		}
	}

	final boolean openServer()
	{
		if( m_nativeLayer != null && !m_nativeLayer.isServerNull() )
		{
			m_mngr.ASSERT(false, "Native server is already not null!");

			return true;
		}
		else
		{
			assertThatAllClientsAreDisconnected();

			clearAllConnectionStates();

			final P_ServerHolder holder = m_mngr.managerLayer().openGattServer(m_mngr.getApplicationContext(), m_server.getInternalListener());
			m_nativeLayer = SweetDIManager.getInstance().get(IBluetoothServer.class, m_mngr, holder);

			return !m_nativeLayer.isServerNull();
		}
	}

	final boolean isDisconnecting(final String macAddress)
	{
		return getNativeState(macAddress) == BleStatuses.SERVER_DISCONNECTING;
	}

	final boolean isDisconnected(final String macAddress)
	{
		return getNativeState(macAddress) == BleStatuses.SERVER_DISCONNECTED;
	}

	final boolean isConnected(final String macAddress)
	{
		return getNativeState(macAddress) == BleStatuses.SERVER_CONNECTED;
	}

	final boolean isConnecting(final String macAddress)
	{
		return getNativeState(macAddress) == BleStatuses.SERVER_CONNECTING;
	}

	final boolean isConnectingOrConnected(final String macAddress)
	{
		final int  nativeState = getNativeState(macAddress);

		return nativeState == BleStatuses.SERVER_CONNECTING || nativeState == BleStatuses.SERVER_CONNECTED;
	}

	final boolean isDisconnectingOrDisconnected(final String macAddress)
	{
		final int  nativeState = getNativeState(macAddress);

		return nativeState == BleStatuses.SERVER_DISCONNECTING || nativeState == BleStatuses.SERVER_DISCONNECTED;
	}

	final int getNativeState(final String macAddress)
	{
		if( m_nativeConnectionStates.containsKey(macAddress) )
		{
			return m_nativeConnectionStates.get(macAddress);
		}
		else
		{
			return BleStatuses.SERVER_DISCONNECTED;
		}
	}

	final IBluetoothServer getNative()
	{
		if (m_nativeLayer == null)
		{
			return AndroidBluetoothServer.NULL;
		}
		return m_nativeLayer;
	}

	final void updateNativeConnectionState(final String macAddress, final int state)
	{
		m_nativeConnectionStates.put(macAddress, state);
	}

	final void updateNativeConnectionState(final P_DeviceHolder device)
	{
		if( m_nativeLayer == null || m_nativeLayer.isServerNull() )
		{
			m_mngr.ASSERT(false, "Did not expect native server to be null when implicitly refreshing state.");
		}
		else
		{
			final IBluetoothDevice layer = SweetDIManager.getInstance().get(IBluetoothDevice.class, P_BleDeviceImpl.EMPTY_DEVICE(m_server.getIManager()));
			layer.setNativeDevice(device.getDevice(), P_DeviceHolder.NULL);
			final int nativeState = m_server.getIManager().managerLayer().getConnectionState( layer, BleStatuses.PROFILE_GATT );

			updateNativeConnectionState(device.getAddress(), nativeState);
		}
	}



	private void assertThatAllClientsAreDisconnected()
	{
		if( m_nativeConnectionStates.size() == 0 )  return;

		for( String macAddress : m_nativeConnectionStates.keySet() )
		{
			final Integer state = m_nativeConnectionStates.get(macAddress);

			if( state != null && state != BleStatuses.SERVER_DISCONNECTED )
			{
				m_mngr.ASSERT(false, "Found a server connection state that is not disconnected when it should be.");

				return;
			}
		}
	}

	private void clearAllConnectionStates()
	{
		m_nativeConnectionStates.clear();
	}
}
