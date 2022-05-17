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

package com.idevicesinc.sweetblue.backend.historical;


import android.content.Context;
import android.database.Cursor;
import com.idevicesinc.sweetblue.internal.IBleManager;
import com.idevicesinc.sweetblue.utils.EmptyCursor;
import com.idevicesinc.sweetblue.utils.EpochTimeRange;
import com.idevicesinc.sweetblue.utils.ForEach_Void;
import com.idevicesinc.sweetblue.utils.HistoricalData;
import com.idevicesinc.sweetblue.utils.HistoricalDataCursor;
import java.util.UUID;

/**
 * Defines a specification for an interface over a disk-persisted database (probably SQL-based but not necessarily)
 * storing arbitrary historical data for each MAC-address/UUID combination provided.
 */
public class Backend_HistoricalDatabase_Default implements Backend_HistoricalDatabase
{
	private static final HistoricalDataCursor EMPTY_CURSOR = new P_HistoricalDataCursor_Empty();

	private boolean m_hasShownWarning = true;

	public Backend_HistoricalDatabase_Default(final Context context)
	{
	}

	public Backend_HistoricalDatabase_Default()
	{
	}

	private void printWarning()
	{
		if( m_hasShownWarning == true )  return;

		Backend_HistoricalDataList_Default.printWarning();

		m_hasShownWarning = true;
	}

	@Override public void init(final IBleManager manager)
	{

	}

	@Override public void add_single(final String macAddress, final UUID uuid, final HistoricalData data, final long maxCountToDelete)
	{
		printWarning();
	}

	@Override public void add_multiple_start()
	{
		printWarning();
	}

	@Override public void add_multiple_next(final String macAddress, final UUID uuid, final HistoricalData data)
	{
		printWarning();
	}

	@Override public void add_multiple_end()
	{
		printWarning();
	}

	@Override public void delete_singleUuid_all(final String macAddress, final UUID uuid)
	{
		printWarning();
	}

	@Override public void delete_singleUuid_inRange(final String macAddress, final UUID uuid, final EpochTimeRange range, final long maxCountToDelete)
	{
		printWarning();
	}

	@Override public void delete_singleUuid_singleDate(final String macAddress, final UUID uuid, final long date)
	{
		printWarning();
	}

	@Override public void delete_multipleUuids(final String[] macAddresses, final UUID[] uuids, final EpochTimeRange range, final long count)
	{
		printWarning();
	}

	@Override public boolean doesDataExist(final String macAddress, final UUID uuid)
	{
		printWarning();

		return false;
	}

	@Override public void load(final String macAddress, final UUID uuid, final EpochTimeRange range, final ForEach_Void<HistoricalData> forEach)
	{
		printWarning();
	}

	@Override public int getCount(String macAddress, UUID uuid, final EpochTimeRange range)
	{
		printWarning();

		return 0;
	}

	@Override public HistoricalDataCursor getCursor(String macAddress, UUID uuid, EpochTimeRange range)
	{
		printWarning();

		return EMPTY_CURSOR;
	}

	@Override public Cursor query(String query)
	{
		printWarning();

		return EmptyCursor.SINGLETON;
	}

	@Override public String getTableName(String macAddress, UUID uuid)
	{
		printWarning();

		return "";
	}
}
