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

import java.util.UUID;

/**
 * Enumeration/abstraction of database columns used to persist {@link HistoricalData}.
 */
public enum HistoricalDataColumn
{
	EPOCH_TIME("date"),
	DATA("data");

	private final String m_name;

	private HistoricalDataColumn(final String name)
	{
		m_name = name;
	}

	/**
	 * Gets the name of this database column - you can use this for example to do raw queries through {@link com.idevicesinc.sweetblue.BleDevice#queryHistoricalData(String)}.
	 */
	public String getColumnName()
	{
		return m_name;
	}

	/**
	 * Gets the name of this database column - you can use this for example to help navigate the {@link android.database.Cursor}
	 * returned by {@link com.idevicesinc.sweetblue.BleDevice#queryHistoricalData(String)}.
	 */
	public int getColumnIndex()
	{
		return ordinal();
	}
}
