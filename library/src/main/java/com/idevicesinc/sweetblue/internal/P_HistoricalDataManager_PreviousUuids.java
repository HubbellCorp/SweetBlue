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

import android.content.Context;
import android.content.SharedPreferences;

import com.idevicesinc.sweetblue.utils.Uuids;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

final class P_HistoricalDataManager_PreviousUuids
{
	private static final int ACCESS_MODE = Context.MODE_PRIVATE;
	private static final String NAMESPACE = "sweetblue__previous_historical_data_uuids";

	private final String m_macAddress;
	private final Context m_context;

	private final Set<UUID> m_uuids = new HashSet<>();

	private boolean m_loaded = false;

	public P_HistoricalDataManager_PreviousUuids(final Context context, final String macAddress)
	{
		m_context = context;
		m_macAddress = macAddress;
	}

	public void addUuid(final UUID uuid)
	{
		synchronized(this)
		{
			load();

			if( m_uuids.contains(uuid) ) return;

			m_uuids.add(uuid);

			save();
		}
	}

	public void clearUuid(final UUID uuid)
	{
		synchronized(this)
		{
			load();

			m_uuids.remove(uuid);

			save();
		}
	}

	public void clearAll()
	{
		synchronized(this)
		{
			m_uuids.clear();
			prefs().edit().clear().commit();
		}
	}

	public int getCount()
	{
		synchronized(this)
		{
			load();

			return m_uuids.size();
		}
	}

	public Iterator<UUID> getUuids()
	{
		synchronized(this)
		{
			load();

			return m_uuids.iterator();
		}
	}

	private SharedPreferences prefs()
	{
		final SharedPreferences prefs = m_context.getSharedPreferences(NAMESPACE, ACCESS_MODE);

		return prefs;
	}

	private void load()
	{
		if( m_loaded )  return;

		final SharedPreferences prefs = prefs();

		if( prefs.contains(m_macAddress) )
		{
			final Set<String> uuids = prefs.getStringSet(m_macAddress, null);

			if( uuids != null && !uuids.isEmpty() )
			{
				final Iterator<String> iterator = uuids.iterator();

				while( iterator.hasNext() )
				{
					final String uuid_string = iterator.next();

					if( uuid_string == null || uuid_string.isEmpty() )  continue;

					final UUID uuid = Uuids.fromString(uuid_string);

					m_uuids.add(uuid);
				}
			}
		}

		m_loaded = true;
	}

	private void save()
	{
		if( m_uuids.isEmpty() )  return;

		final Iterator<UUID> iterator = m_uuids.iterator();
		final Set<String> toSave = new HashSet<>();

		while( iterator.hasNext() )
		{
			final UUID uuid = iterator.next();

			if( uuid == null )  continue;

			toSave.add(uuid.toString());
		}

		prefs().edit().putStringSet(m_macAddress, toSave).commit();
	}
}
