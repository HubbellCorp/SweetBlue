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

import static com.idevicesinc.sweetblue.BleNodeConfig.HistoricalDataLogFilter.*;

import com.idevicesinc.sweetblue.BleNode;
import com.idevicesinc.sweetblue.HistoricalDataLoadListener;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.backend.historical.Backend_HistoricalDataList;
import com.idevicesinc.sweetblue.backend.historical.Backend_HistoricalDatabase;
import com.idevicesinc.sweetblue.utils.EmptyIterator;
import com.idevicesinc.sweetblue.utils.EpochTime;
import com.idevicesinc.sweetblue.utils.EpochTimeRange;
import com.idevicesinc.sweetblue.utils.ForEach_Returning;
import com.idevicesinc.sweetblue.utils.HistoricalData;
import com.idevicesinc.sweetblue.utils.HistoricalDataCursor;
import com.idevicesinc.sweetblue.utils.P_Const;

import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

final class P_HistoricalDataManager
{
	private static final EmptyIterator<HistoricalData> EMPTY_ITERATOR = new EmptyIterator<>();

	private final Object LIST_CREATE_MUTEX = new Object();

	private final HashMap<UUID, Backend_HistoricalDataList> m_lists = new HashMap<>();
	private final IBleNode m_endPoint;
	private final String m_macAddress;

	private HistoricalDataLoadListener m_defaultListener = null;

	private final P_HistoricalDataManager_PreviousUuids m_previousUuidsWithDataAdded;

	P_HistoricalDataManager(final IBleNode endpoint, final String macAddress)
	{
		m_endPoint = endpoint;
		m_macAddress = macAddress;

		m_previousUuidsWithDataAdded = new P_HistoricalDataManager_PreviousUuids(m_endPoint.getIManager().getApplicationContext(), macAddress);
	}

	void setListener(final HistoricalDataLoadListener listener)
	{
		m_defaultListener = listener;
	}

	Backend_HistoricalDatabase getDatabase()
	{
		return m_endPoint.getIManager().getHistoricalDatabase();
	}

	//GOOD
	private Backend_HistoricalDataList getList_doNotCreate(final UUID uuid)
	{
		synchronized(LIST_CREATE_MUTEX)
		{
			final Backend_HistoricalDataList existingList = m_lists.get(uuid);

			return existingList;
		}
	}

	//GOOD
	private Backend_HistoricalDataList getList_onlyCreateIfDataIsOnDisk(final UUID uuid)
	{
		synchronized(LIST_CREATE_MUTEX)
		{
			final Backend_HistoricalDataList existingList = m_lists.get(uuid);

			if( existingList == null )
			{
				final boolean tableExists = getDatabase().doesDataExist(m_macAddress, uuid);

				if( tableExists )
				{
					final String uuidName = m_endPoint.getIManager().getLogger().charName(uuid);
					final Backend_HistoricalDataList newList = PU_HistoricalData.newList(getDatabase(), m_macAddress, uuid, uuidName, tableExists);
					m_lists.put(uuid, newList);

					return newList;
				}
			}

			return existingList;
		}
	}

	//GOOD
	private Backend_HistoricalDataList getList_createIfNotExists(final UUID uuid)
	{
		synchronized(LIST_CREATE_MUTEX)
		{
			final Backend_HistoricalDataList existingList = m_lists.get(uuid);

			if( existingList == null )
			{
				final boolean tableExists = getDatabase().doesDataExist(m_macAddress, uuid);
				final String uuidName = m_endPoint.getIManager().getLogger().charName(uuid);

				final Backend_HistoricalDataList newList = PU_HistoricalData.newList(getDatabase(), m_macAddress, uuid, uuidName, tableExists);
				m_lists.put(uuid, newList);

				return newList;
			}
			else
			{
				return existingList;
			}
		}
	}

	//GOOD
	public void add_single(final UUID uuid, final byte[] data, final EpochTime epochTime, final Source source)
	{
		final Backend_HistoricalDataList list = getList_createIfNotExists(uuid);

		final Please please = PU_HistoricalData.getPlease(m_endPoint, m_macAddress, uuid, data, epochTime, source);

		if( PU_HistoricalData.add_earlyOut(list, please) )  return;

		final HistoricalData historicalData = m_endPoint.newHistoricalData(PU_HistoricalData.getAmendedData(data, please), PU_HistoricalData.getAmendedTimestamp(epochTime, please));

		m_previousUuidsWithDataAdded.addUuid(uuid);

		list.add_single(historicalData, P_Bridge_User.getPersistanceLevel(please), please.getLimit());
	}

	//GOOD
	public void add_single(final UUID uuid, final HistoricalData historicalData, final Source source)
	{
		final Backend_HistoricalDataList list = getList_createIfNotExists(uuid);

		final Please please = PU_HistoricalData.getPlease(m_endPoint, m_macAddress, uuid, historicalData.getBlob(), historicalData.getEpochTime(), source);

		final HistoricalData historicalData_override;

		if( please.getAmendedData() != null || !please.getAmendedEpochTime().isNull() )
		{
			historicalData_override = m_endPoint.newHistoricalData(PU_HistoricalData.getAmendedData(historicalData.getBlob(), please), PU_HistoricalData.getAmendedTimestamp(historicalData.getEpochTime(), please));
		}
		else
		{
			historicalData_override = historicalData;
		}

		if( PU_HistoricalData.add_earlyOut(list, please) )  return;

		m_previousUuidsWithDataAdded.addUuid(uuid);

		list.add_single(historicalData_override, P_Bridge_User.getPersistanceLevel(please), please.getLimit());
	}

	//GOOD
	public void add_multiple(final UUID uuid, final Iterator<HistoricalData> historicalData)
	{
		final Backend_HistoricalDataList list = getList_createIfNotExists(uuid);

		final Please please = PU_HistoricalData.getPlease(m_endPoint, m_macAddress, uuid, P_Const.EMPTY_BYTE_ARRAY, EpochTime.NULL, Source.MULTIPLE_MANUAL_ADDITIONS);

		if( PU_HistoricalData.add_earlyOut(list, please) )  return;

		m_previousUuidsWithDataAdded.addUuid(uuid);

		list.add_multiple(historicalData, P_Bridge_User.getPersistanceLevel(please), please.getLimit());
	}

	//GOOD
	public void add_multiple(final UUID uuid, final ForEach_Returning<HistoricalData> historicalData)
	{
		final Backend_HistoricalDataList list = getList_createIfNotExists(uuid);

		final Please please = PU_HistoricalData.getPlease(m_endPoint, m_macAddress, uuid, P_Const.EMPTY_BYTE_ARRAY, EpochTime.NULL, Source.MULTIPLE_MANUAL_ADDITIONS);

		if( PU_HistoricalData.add_earlyOut(list, please) )  return;

		m_previousUuidsWithDataAdded.addUuid(uuid);

		list.add_multiple(historicalData, P_Bridge_User.getPersistanceLevel(please), please.getLimit());
	}

	//GOOD
	public HistoricalData getWithOffset(final UUID uuid, final EpochTimeRange range, final int offset)
	{
		final Backend_HistoricalDataList list = getList_onlyCreateIfDataIsOnDisk(uuid);

		if( list == null )
		{
			return HistoricalData.NULL;
		}
		else
		{
			return list.get(range, offset);
		}
	}

	//GOOD
	public void delete(final UUID uuid, final EpochTimeRange range, final long limit, final boolean memoryOnly)
	{
		final Backend_HistoricalDataList list = getList_doNotCreate(uuid);

		if( memoryOnly )
		{
			if( list != null )
			{
				list.delete_fromMemoryOnly(range, limit);
			}
		}
		else
		{
			if( list != null )
			{
				list.delete_fromMemoryAndDatabase(range, limit);
			}
			else
			{
				getDatabase().delete_singleUuid_inRange(m_macAddress, uuid, range, limit);
			}
		}
	}

	// GOOD
	public void clearEverything()
	{
		delete_all(EpochTimeRange.FROM_MIN_TO_MAX, Long.MAX_VALUE, false);

		m_previousUuidsWithDataAdded.clearAll();
	}

	//GOOD
	public void delete_all(final EpochTimeRange range, final long limit, final boolean memoryOnly)
	{
		final Iterator<UUID> knownUuids = m_previousUuidsWithDataAdded.getUuids();

		final UUID[] uuids = !memoryOnly ? new UUID[m_previousUuidsWithDataAdded.getCount()] : null;
		final String[] macs = uuids != null ? new String[uuids.length] : null;

		int i = 0;

		while( knownUuids.hasNext() )
		{
			final UUID ith = knownUuids.next();

			if( uuids != null )
			{
				uuids[i] = ith;
				macs[i] = m_macAddress;
			}

			i++;

			Backend_HistoricalDataList list = getList_doNotCreate(ith);

			if( list == null )  continue;

			if( memoryOnly )
			{
				list.delete_fromMemoryOnly(range, limit);
			}
			else
			{
				list.delete_fromMemoryOnlyForNowButDatabaseSoon(range, limit);
			}
		}

		if( !memoryOnly )
		{
			getDatabase().delete_multipleUuids(macs, uuids, range, Long.MAX_VALUE);
		}
	}

	//GOOD
	public Iterator<HistoricalData> getIterator(final UUID uuid, final EpochTimeRange range)
	{
		final Backend_HistoricalDataList list = getList_onlyCreateIfDataIsOnDisk(uuid);

		if( list != null )
		{
			return list.getIterator(range);
		}
		else
		{
			return EMPTY_ITERATOR;
		}
	}

	//GOOD
	public boolean doForEach(final UUID uuid, final EpochTimeRange range, Object forEach)
	{
		final Backend_HistoricalDataList list = getList_onlyCreateIfDataIsOnDisk(uuid);

		if( list != null )
		{
			return list.doForEach(range, forEach);
		}
		else
		{
			return false;
		}
	}

	//GOOD
	public int getCount(UUID uuid, final EpochTimeRange range)
	{
		final Backend_HistoricalDataList list = getList_onlyCreateIfDataIsOnDisk(uuid);

		return list != null ? list.getCount(range) : 0;
	}

	//GOOD
	public HistoricalDataCursor getCursor(final UUID uuid, final EpochTimeRange range)
	{
		final Backend_HistoricalDataList list = getList_doNotCreate(uuid);

		if( list != null )
		{
			return list.getCursor(range);
		}
		else
		{
			return getDatabase().getCursor(m_macAddress, uuid, range);
		}
	}

	//GOOD
	public boolean hasHistoricalData(final UUID uuid, final EpochTimeRange range)
	{
		return getCount(uuid, range) > 0;
	}

	//GOOD
	public boolean hasHistoricalData(final EpochTimeRange range)
	{
		final boolean memoryOnly = false;
		final Iterator<UUID> previousUuids = m_previousUuidsWithDataAdded.getUuids();

		while( previousUuids.hasNext() )
		{
			final UUID ithUuid = previousUuids.next();

			final Backend_HistoricalDataList ithList = getList_doNotCreate(ithUuid);

			if( memoryOnly )
			{
				if( ithList != null && ithList.getCount(range) > 0 )
				{
					return true;
				}
			}
			else
			{
				if( ithList != null && ithList.getCount(range) > 0 )
				{
					return true;
				}
				else
				{
					if( getDatabase().getCount(m_macAddress, ithUuid, range) > 0 )
					{
						return true;
					}
				}
			}
		}

		return false;
	}

	//TODO: Need to implement the uui==null case
	public void load(final UUID uuid_nullable, final HistoricalDataLoadListener listener_nullable)
	{
		if( isLoaded(uuid_nullable) )
		{
			if( uuid_nullable != null )
			{
				final Backend_HistoricalDataList list = getList_doNotCreate(uuid_nullable);

				if( list != null )
				{
					invokeListeners(uuid_nullable, list.getRange(), HistoricalDataLoadListener.Status.ALREADY_LOADED, listener_nullable);
				}
				else
				{
					m_endPoint.getIManager().ASSERT(false, "list should not have been null.");
				}
			}
			else
			{

			}
		}
		else
		{
			if( uuid_nullable != null )
			{
				if( hasHistoricalData(uuid_nullable, EpochTimeRange.FROM_MIN_TO_MAX) )
				{
					final Backend_HistoricalDataList list = getList_createIfNotExists(uuid_nullable);

					if( list.getLoadState() == Backend_HistoricalDataList.LOAD_STATE__LOADING )
					{
						invokeListeners(uuid_nullable, EpochTimeRange.NULL, HistoricalDataLoadListener.Status.ALREADY_LOADING, listener_nullable);
					}
					else
					{
						invokeListeners(uuid_nullable, EpochTimeRange.NULL, HistoricalDataLoadListener.Status.STARTED_LOADING, listener_nullable);
					}

					list.load(() -> m_endPoint.getIManager().getPostManager().runOrPostToUpdateThread(() -> {
                        if (list.getLoadState() == Backend_HistoricalDataList.LOAD_STATE__LOADED)
                        {
                            invokeListeners(uuid_nullable, list.getRange(), HistoricalDataLoadListener.Status.LOADED, listener_nullable);
                        }
                        else if (list.getLoadState() == Backend_HistoricalDataList.LOAD_STATE__NOT_LOADED)
                        {
                            //--- DRK > Should be fringe but technically possible if user is doing things on multiple threads (they shouldn't but if they do...).
                            invokeListeners(uuid_nullable, EpochTimeRange.NULL, HistoricalDataLoadListener.Status.NOTHING_TO_LOAD, listener_nullable);
                        }
                        else
                        {
                            m_endPoint.getIManager().ASSERT(false, "Didn't expect to still be loading historical data.");
                        }
                    }));
				}
				else
				{
					invokeListeners(uuid_nullable, EpochTimeRange.NULL, HistoricalDataLoadListener.Status.NOTHING_TO_LOAD, listener_nullable);
				}
			}
			else
			{

			}
		}
	}

	//GOOD
	public boolean isLoaded(final UUID uuid_nullable)
	{
		if( uuid_nullable != null )
		{
			final Backend_HistoricalDataList list = getList_doNotCreate(uuid_nullable);

			if( list != null )
			{
				return list.getLoadState() == Backend_HistoricalDataList.LOAD_STATE__LOADED;
			}
			else
			{
				return false;
			}
		}
		else
		{
			final Iterator<UUID> previousUuids = m_previousUuidsWithDataAdded.getUuids();

			while( previousUuids.hasNext() )
			{
				final UUID ithUuid = previousUuids.next();

				final Backend_HistoricalDataList ithList = getList_doNotCreate(ithUuid);

				if( ithList == null || ithList.getLoadState() != Backend_HistoricalDataList.LOAD_STATE__LOADED )
				{
					return false;
				}
			}

			return true;
		}
	}

	//GOOD
	public boolean isLoading(final UUID uuid_nullable)
	{
		if( uuid_nullable != null )
		{
			final Backend_HistoricalDataList list = getList_doNotCreate(uuid_nullable);

			if( list != null )
			{
				return list.getLoadState() == Backend_HistoricalDataList.LOAD_STATE__LOADING;
			}
			else
			{
				return false;
			}
		}
		else
		{
			final Iterator<UUID> previousUuids = m_previousUuidsWithDataAdded.getUuids();

			while( previousUuids.hasNext() )
			{
				final UUID ithUuid = previousUuids.next();

				final Backend_HistoricalDataList ithList = getList_doNotCreate(ithUuid);

				if( ithList == null )  continue;

				if( ithList.getLoadState() == Backend_HistoricalDataList.LOAD_STATE__LOADING )
				{
					return true;
				}
			}

			return false;
		}
	}

	void invokeListeners(final UUID uuid, final EpochTimeRange range, final HistoricalDataLoadListener.Status status, final HistoricalDataLoadListener listener_nullable)
	{
		HistoricalDataLoadListener.HistoricalDataLoadEvent event = null;

		event = invokeListener(uuid, range, listener_nullable, status, event);
		event = invokeListener(uuid, range, m_defaultListener, status, event);
		event = invokeListener(uuid, range, m_endPoint.getIManager().getHistoricalDataLoadListener(), status, event);
	}

	private HistoricalDataLoadListener.HistoricalDataLoadEvent invokeListener(final UUID uuid, final EpochTimeRange range, final HistoricalDataLoadListener listener_nullable, final HistoricalDataLoadListener.Status status, final HistoricalDataLoadListener.HistoricalDataLoadEvent event_nullable)
	{
		BleNode node;
		if (m_endPoint instanceof IBleDevice)
			node = ((IBleDevice) m_endPoint).getBleDevice();
		else
			node = ((IBleServer) m_endPoint).getBleServer();

		final HistoricalDataLoadListener.HistoricalDataLoadEvent event = event_nullable != null ? event_nullable : P_Bridge_User.newHistoricalDataLoadEvent(node, m_macAddress, uuid, range, status);
		if( listener_nullable != null )
		{
			m_endPoint.getIManager().postEvent(listener_nullable, event);
		}

		return event;
	}

	public String getTableName(final UUID uuid)
	{
		return getDatabase().getTableName(m_macAddress, uuid);
	}

}
