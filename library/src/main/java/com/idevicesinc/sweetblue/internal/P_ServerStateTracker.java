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

import com.idevicesinc.sweetblue.BleServerState;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.ServerStateListener;
import com.idevicesinc.sweetblue.utils.State;
import java.util.List;
import static com.idevicesinc.sweetblue.BleServerState.CONNECTED;
import static com.idevicesinc.sweetblue.BleServerState.CONNECTING;


final class P_ServerStateTracker
{
	private ServerStateListener m_stateListener;
	private final IBleServer m_server;


	P_ServerStateTracker(IBleServer server)
	{
		m_server = server;
	}
	
	public void setListener(ServerStateListener listener)
	{
		m_stateListener = listener;
	}

	BleServerState getOldConnectionState(final String macAddress)
	{
		final int stateMask = getStateMask(macAddress);

		if( BleServerState.CONNECTING.overlaps(stateMask) )
		{
			return CONNECTING;
		}
		else if( BleServerState.CONNECTED.overlaps(stateMask) )
		{
			return CONNECTED;
		}
		else
		{
			m_server.getIManager().ASSERT(false, "Expected to be connecting or connected for an explicit disconnect.");

			return BleServerState.NULL;
		}
	}

	void doStateTransition(final String macAddress, final BleServerState oldState, final BleServerState newState, final State.ChangeIntent intent, final int gattStatus)
	{
		final int currentBits = m_server.getStateMask(macAddress);

		final int oldState_bit = false == oldState.isNull() ? oldState.bit() : 0x0;
		final int newState_bit = newState.bit();
		final int intentBits = intent == State.ChangeIntent.INTENTIONAL ? 0xFFFFFFFF : 0x00000000;

		final int oldBits = (currentBits | oldState_bit) & ~newState_bit;
		final int newBits = (currentBits | newState_bit) & ~oldState_bit;
		final int intentMask = (oldBits | newBits) & intentBits;

		final ServerStateListener.StateEvent e = P_Bridge_User.newServerStateEvent(m_server.getBleServer(), macAddress, oldBits, newBits, intentMask, gattStatus);

		fireEvent(e);
	}

	private void fireEvent(final ServerStateListener.StateEvent e)
	{
		if( m_stateListener != null )
		{
			m_server.getIManager().postEvent(m_stateListener, e);
		}

		final ServerStateListener listener = m_server.getIManager().getDefaultServerStateListener();
		if( listener != null )
		{
			m_server.getIManager().postEvent(listener, e);
		}
	}

	public int getStateMask(final String macAddress)
	{
		final P_TaskManager queue = m_server.getIManager().getTaskManager();
		final List<PA_Task> queue_raw = queue.getRaw();
		final int bitForUnknownState = BleServerState.DISCONNECTED.bit();
		final PA_Task current = queue.getCurrent();

		if( m_server.getNativeManager().isConnectingOrConnected(macAddress) )
		{
			for( int i = queue_raw.size()-1; i >= 0; i-- )
			{
				final PA_Task ith = queue_raw.get(i);

				if( ith.isFor(P_Task_ConnectServer.class, m_server, macAddress) )
				{
					return BleServerState.CONNECTING.bit();
				}

				if( ith.isFor(P_Task_DisconnectServer.class, m_server, macAddress) )
				{
					return BleServerState.DISCONNECTED.bit();
				}
			}

			if( current != null && current.isFor(P_Task_DisconnectServer.class, m_server, macAddress) )
			{
				return BleServerState.DISCONNECTED.bit();
			}
			else if( m_server.getNativeManager().isConnected(macAddress) )
			{
				return BleServerState.CONNECTED.bit();
			}
			else if( m_server.getNativeManager().isConnecting(macAddress) )
			{
				return BleServerState.CONNECTING.bit();
			}
			else
			{
				m_server.getIManager().ASSERT(false, "Expected to be connecting or connected when getting state mask for server.");

				return bitForUnknownState;
			}
		}
		else if( m_server.getNativeManager().isDisconnectingOrDisconnected(macAddress) )
		{
			for( int i = queue_raw.size()-1; i >= 0; i-- )
			{
				final PA_Task ith = queue_raw.get(i);

				if( ith.isFor(P_Task_DisconnectServer.class, m_server, macAddress) )
				{
					return BleServerState.DISCONNECTED.bit();
				}

				if( ith.isFor(P_Task_ConnectServer.class, m_server, macAddress) )
				{
					return BleServerState.CONNECTING.bit();
				}
			}

			if( current != null && current.isFor(P_Task_ConnectServer.class, m_server, macAddress) )
			{
				return BleServerState.CONNECTING.bit();
			}
			else
			{
				return BleServerState.DISCONNECTED.bit();
			}
		}
		else
		{
			m_server.getIManager().ASSERT(false, "Native server is in an unknown state.");

			return bitForUnknownState;
		}
	}
}
