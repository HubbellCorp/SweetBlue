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
import com.idevicesinc.sweetblue.BleDeviceConfig;
import com.idevicesinc.sweetblue.BleNodeConfig;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.backend.Backend_Modules;
import com.idevicesinc.sweetblue.backend.historical.Backend_HistoricalDataList_Default;
import com.idevicesinc.sweetblue.backend.historical.Backend_HistoricalDataList;
import com.idevicesinc.sweetblue.backend.historical.Backend_HistoricalDatabase;
import com.idevicesinc.sweetblue.backend.historical.Backend_HistoricalDatabase_Default;
import com.idevicesinc.sweetblue.utils.EpochTime;
import com.idevicesinc.sweetblue.utils.EpochTimeRange;

import java.lang.reflect.InvocationTargetException;
import java.util.UUID;


final class PU_HistoricalData
{
	private static final BleNodeConfig.HistoricalDataLogFilter.Please DO_NOT_LOG = BleDeviceConfig.HistoricalDataLogFilter.Please.doNotLog();

	static Backend_HistoricalDataList newList(final Backend_HistoricalDatabase database, final String macAddress, UUID uuid, final String uuidName, final boolean doesTableExist)
	{
		final Class<? extends Backend_HistoricalDataList> listClass = Backend_Modules.HISTORICAL_DATA_LIST;

		Backend_HistoricalDataList newList = null;

		if( listClass != null )
		{
			try
			{
				newList = listClass.newInstance();
			}
			catch(InstantiationException e)
			{
			}
			catch(IllegalAccessException e)
			{
			}
		}

		newList = newList != null ? newList : new Backend_HistoricalDataList_Default();

		newList.init(database, macAddress, uuid, uuidName, doesTableExist);

		return newList;
	}

	static Backend_HistoricalDatabase newDatabase(final Context context, final IBleManager manager)
	{
		final Class<? extends Backend_HistoricalDatabase> databaseClass = Backend_Modules.HISTORICAL_DATABASE;

		Backend_HistoricalDatabase newDatabase = null;

		if( databaseClass != null )
		{
			try
			{
				newDatabase = databaseClass.getDeclaredConstructor(Context.class).newInstance(context);
			}
			catch(InstantiationException e)
			{
			}
			catch(IllegalAccessException e)
			{
			}
			catch(NoSuchMethodException e)
			{
			}
			catch(InvocationTargetException e)
			{
			}
		}

		newDatabase = newDatabase != null ? newDatabase : new Backend_HistoricalDatabase_Default(context);

		newDatabase.init(manager);

		return newDatabase;
	}

	static BleDeviceConfig.HistoricalDataLogFilter getFilter(final IBleNode endpoint)
	{
		final BleDeviceConfig.HistoricalDataLogFilter filter_config_device = endpoint.conf_node().historicalDataLogFilter;
		final BleDeviceConfig.HistoricalDataLogFilter filter_config_mngr = endpoint.conf_mngr().historicalDataLogFilter;

		return filter_config_device != null ? filter_config_device : filter_config_mngr;
	}

	static BleDeviceConfig.HistoricalDataLogFilter.HistoricalDataLogEvent newEvent(final IBleNode node, final String macAddress, final UUID uuid, final byte[] data, final EpochTime epochTime, final BleDeviceConfig.HistoricalDataLogFilter.Source source)
	{
		final BleDeviceConfig.HistoricalDataLogFilter.HistoricalDataLogEvent event = P_Bridge_User.newHistoricalDataLogEvent(node.getIManager().getBleNode(node), macAddress, uuid, data, epochTime, source);

		return event;
	}

	static BleDeviceConfig.HistoricalDataLogFilter.Please getPlease(final IBleNode node, final String macAddress, final UUID uuid, final byte[] data, final EpochTime epochTime, final BleDeviceConfig.HistoricalDataLogFilter.Source source)
	{
		final BleDeviceConfig.HistoricalDataLogFilter filter = getFilter(node);

		if( filter == null )
		{
			return DO_NOT_LOG;
		}
		else
		{
			final BleDeviceConfig.HistoricalDataLogFilter.HistoricalDataLogEvent event = newEvent(node, macAddress, uuid, data, epochTime, source);
			final BleDeviceConfig.HistoricalDataLogFilter.Please please = filter.onEvent(event);

			return please != null ? please : DO_NOT_LOG;
		}
	}

	static byte[] getAmendedData(final byte[] original, final BleDeviceConfig.HistoricalDataLogFilter.Please please)
	{
		return please.getAmendedData() != null ? please.getAmendedData() : original;
	}

	static EpochTime getAmendedTimestamp(final EpochTime original, final BleDeviceConfig.HistoricalDataLogFilter.Please please)
	{
		return !please.getAmendedEpochTime().isNull() ? please.getAmendedEpochTime() : original;
	}

	static boolean add_earlyOut(final Backend_HistoricalDataList list, final BleDeviceConfig.HistoricalDataLogFilter.Please please)
	{
		if( P_Bridge_User.getPersistanceLevel(please) == BleDeviceConfig.HistoricalDataLogFilter.PersistenceLevel_NONE )
		{
			final long numberToDelete = please.getLimit() != Long.MAX_VALUE ? list.getCount(EpochTimeRange.FROM_MIN_TO_MAX)-please.getLimit() : 0;

			if( numberToDelete > 0 )
			{
				list.delete_fromMemoryAndDatabase(EpochTimeRange.FROM_MIN_TO_MAX, numberToDelete);
			}

			return true;
		}

		return false;
	}
}
