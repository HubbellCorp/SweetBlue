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

import com.idevicesinc.sweetblue.utils.ForEach_Breakable;
import com.idevicesinc.sweetblue.utils.ForEach_Void;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;


final class P_ClientManager
{

	private final IBleServer m_server;
	private final HashSet<String> m_allConnectingOrConnectedClients = new HashSet<>();


	P_ClientManager(final IBleServer server)
	{
		m_server = server;
	}


	final void onConnecting(final String macAddress)
	{
		m_allConnectingOrConnectedClients.add(macAddress);
	}

	final void onConnected(final String macAddress)
	{
		m_allConnectingOrConnectedClients.add(macAddress);
	}


	final void getClients(final ForEach_Void<String> forEach, final int stateMask)
	{
		getClients_private(forEach, getClients(stateMask));
	}

	private void getClients_private(final ForEach_Void<String> forEach, final Iterator<String> iterator)
	{
		while( iterator.hasNext() )
		{
			final String client = iterator.next();

			forEach.next(client);
		}
	}

	final void getClients(final ForEach_Breakable<String> forEach, final int stateMask)
	{
		getClients_private(forEach, getClients(stateMask));
	}

	private void getClients_private(final ForEach_Breakable<String> forEach, final Iterator<String> iterator)
	{
		while( iterator.hasNext() )
		{
			final String client = iterator.next();

			final ForEach_Breakable.Please please = forEach.next(client);

			if( please.shouldContinue() == false )
			{
				break;
			}
		}
	}

	final Iterator<String> getClients(final int stateMask)
	{
		return new ClientIterator(stateMask);
	}

	final List<String> getClients_List(final int stateMask)
	{
		if( getClientCount() == 0 )
		{
			return newEmptyList();
		}
		else
		{
			final Iterator<String> iterator = getClients(stateMask);
			final ArrayList<String> toReturn = new ArrayList<>();

			while( iterator.hasNext() )
			{
				toReturn.add(iterator.next());
			}

			return toReturn;
		}
	}

	final int getClientCount()
	{
		return m_allConnectingOrConnectedClients.size();
	}

	final int getClientCount(final int stateMask)
	{
		final Iterator<String> iterator = getClients(stateMask);
		int count = 0;

		while( iterator.hasNext() )
		{
			final String client = iterator.next();

			count++;
		}

		return count;
	}

	private List<String> newEmptyList()
	{
		return new ArrayList<>();
	}

	private final class ClientIterator implements Iterator<String>
	{
		private final int m_stateMask;

		private String m_next = null;
		private String m_returned = null;

		private final Iterator<String> m_all = m_allConnectingOrConnectedClients.iterator();

		ClientIterator(final int stateMask)
		{
			m_stateMask = stateMask;

			findNext();
		}

		private void findNext()
		{
			while( m_all.hasNext() )
			{
				final String client = m_all.next();

				if( m_stateMask != 0x0 && m_server.isAny(client, m_stateMask) )
				{
					m_next = client;

					break;
				}
				else if( m_stateMask == 0x0 )
				{
					m_next = client;

					break;
				}
			}
		}

		@Override public final boolean hasNext()
		{
			return m_next != null;
		}

		@Override public final String next()
		{
			m_returned = m_next;

			if( m_next == null )
			{
				throw new NoSuchElementException("No more clients associated with this server.");
			}

			m_next = null;
			findNext();

			return m_returned;
		}

		@Override public final void remove()
		{
			if( m_returned == null )
			{
				throw new IllegalStateException("remove() was already called.");
			}

			final String toRemove = m_returned;
			m_returned = null;
			m_all.remove();
			m_server.disconnect(toRemove);
		}
	}
}
