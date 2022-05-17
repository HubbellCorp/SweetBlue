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

import com.idevicesinc.sweetblue.utils.EpochTime;
import com.idevicesinc.sweetblue.utils.HistoricalData;
import com.idevicesinc.sweetblue.utils.HistoricalDataCursor;
import com.idevicesinc.sweetblue.utils.P_Const;


class P_HistoricalDataCursor_Empty implements HistoricalDataCursor
{
	private static final byte[] EMPTY_BLOB = P_Const.EMPTY_BYTE_ARRAY;

	@Override public int getCount()
	{
		return 0;
	}

	@Override public int getPosition()
	{
		return -1;
	}

	@Override public boolean move(int offset)
	{
		return false;
	}

	@Override public boolean moveToPosition(int position)
	{
		return false;
	}

	@Override public boolean moveToFirst()
	{
		return false;
	}

	@Override public boolean moveToLast()
	{
		return false;
	}

	@Override public boolean moveToNext()
	{
		return false;
	}

	@Override public boolean moveToPrevious()
	{
		return false;
	}

	@Override public boolean isFirst()
	{
		return false;
	}

	@Override public boolean isLast()
	{
		return false;
	}

	@Override public boolean isBeforeFirst()
	{
		return false;
	}

	@Override public boolean isAfterLast()
	{
		return false;
	}

	@Override public void close()
	{

	}

	@Override public boolean isClosed()
	{
		return true;
	}

	@Override public long getEpochTime()
	{
		return EpochTime.NULL.toMilliseconds();
	}

	@Override public byte[] getBlob()
	{
		return EMPTY_BLOB;
	}

	@Override public HistoricalData getHistoricalData()
	{
		return HistoricalData.NULL;
	}
}
