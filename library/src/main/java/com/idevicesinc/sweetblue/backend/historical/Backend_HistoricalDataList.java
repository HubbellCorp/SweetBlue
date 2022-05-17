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

import com.idevicesinc.sweetblue.utils.EpochTimeRange;
import com.idevicesinc.sweetblue.utils.ForEach_Returning;
import com.idevicesinc.sweetblue.utils.HistoricalData;
import com.idevicesinc.sweetblue.utils.HistoricalDataCursor;

import java.util.Iterator;
import java.util.UUID;

/**
 * Defines a specification for an interface over an in-memory list of historical data that optionally syncs to/from
 * disk using an implementation of {@link com.idevicesinc.sweetblue.backend.historical.Backend_HistoricalDatabase}.
 */
public interface Backend_HistoricalDataList
{
	public static int LOAD_STATE__NOT_LOADED	= 0;
	public static int LOAD_STATE__LOADING		= 1;
	public static int LOAD_STATE__LOADED		= 2;

	public interface AsyncLoadCallback
	{
		void onDone();
	}

	void init(final Backend_HistoricalDatabase database, final String macAddress, final UUID uuid, final String uuidName, final boolean hasExistingTable);

	void add_single(final HistoricalData historicalData, final int persistenceLevel, final long limit);

	void add_multiple(final Iterator<HistoricalData> historicalData, final int persistenceLevel, final long limit);

	void add_multiple(final ForEach_Returning<HistoricalData> historicalData, final int persistenceLevel, final long limit);

	int getCount(final EpochTimeRange range);

	HistoricalData get(final EpochTimeRange range, final int offset);

	Iterator<HistoricalData> getIterator(final EpochTimeRange range);

	boolean doForEach(final EpochTimeRange range, final Object forEach);

	void delete_fromMemoryOnly(final EpochTimeRange range, final long count);

	void delete_fromMemoryOnlyForNowButDatabaseSoon(final EpochTimeRange range, final long count);

	void delete_fromMemoryAndDatabase(final EpochTimeRange range, final long count);

	String getMacAddress();

	void load(final AsyncLoadCallback callback_nullable);

	int getLoadState();

	HistoricalDataCursor getCursor(final EpochTimeRange range);

	EpochTimeRange getRange();
}
