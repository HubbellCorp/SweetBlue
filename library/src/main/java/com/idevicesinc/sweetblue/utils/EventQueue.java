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

package com.idevicesinc.sweetblue.utils;

import java.util.ArrayList;

public final class EventQueue
{
	private static class DispatchEntry
	{
		private final GenericListener_Void listener;
		private final Event event;

		public DispatchEntry(final GenericListener_Void listener_in, final Event event_in)
		{
			listener = listener_in;
			event = event_in;
		}
	}

	private final ArrayList<DispatchEntry> m_queue = new ArrayList<>();

	public EventQueue()
	{
	}

	public <T_Event extends Event> void add(final GenericListener_Void<T_Event> listener_nullable, final T_Event event_nullable)
	{
		if( listener_nullable != null && event_nullable != null )
		{
			final DispatchEntry entry = new DispatchEntry(listener_nullable, event_nullable);

			m_queue.add(entry);
		}
	}

	public void dispatch()
	{
		//--- DRK > For now purposely not dispatching recursive additions in order to force async behavior.
		final int size = m_queue.size();

		for( int i = 0; i < size; i++ )
		{
			final DispatchEntry entry = m_queue.get(i);

			entry.listener.onEvent(entry.event);

			m_queue.set(i, null);
		}

		while( m_queue.size() > 0 && m_queue.get(0) == null )
		{
			m_queue.remove(0);
		}
	}
}
