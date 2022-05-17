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

import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleNode;
import com.idevicesinc.sweetblue.HistoricalDataQueryListener;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.annotations.Extendable;
import com.idevicesinc.sweetblue.backend.historical.Backend_HistoricalDatabase;
import com.idevicesinc.sweetblue.internal.IBleDevice;
import com.idevicesinc.sweetblue.internal.IBleNode;
import com.idevicesinc.sweetblue.internal.IBleServer;

import java.util.UUID;

/**
 * Class used to construct queries for {@link BleDevice#select()}.
 */
@com.idevicesinc.sweetblue.annotations.Alpha
@Extendable
public class HistoricalDataQuery
{
	public static class Part_Select extends Part_AllowsFrom
	{
		private Part_Select(final HistoricalDataQuery query)
		{
			super(query);
		}
	}

	public static class Part_From extends Part
	{
		private Part_From(final HistoricalDataQuery query)
		{
			super(query);
		}
	}

	public static class Part_Where extends Part
	{
		private Part_Where(final HistoricalDataQuery query)
		{
			super(query);
		}

//		public Part_ClauseColumn data()
//		{
//			return column(HistoricalDataColumn.DATA);
//		}

		public Part_ClauseColumn epochTime()
		{
			return column(HistoricalDataColumn.EPOCH_TIME);
		}

		private Part_ClauseColumn column(final HistoricalDataColumn column)
		{
			return new Part_ClauseColumn(column, m_query);
		}
	}

	public static class Part_ClauseColumn extends Part
	{
		private final HistoricalDataColumn m_column;

		private Part_ClauseColumn(final HistoricalDataColumn column, final HistoricalDataQuery query)
		{
			super(query);

			m_column = column;
		}

		public HistoricalDataQuery between(final EpochTimeRange range)
		{
			m_query.m_where += " " + m_column.getColumnName() + " BETWEEN "+range.from().toMilliseconds() + " AND " + range.to().toMilliseconds();

			return m_query;
		}
	}

	public static class Part_SelectColumn extends Part_AllowsFrom
	{
		private Part_SelectColumn(final HistoricalDataQuery query)
		{
			super(query);
		}
	}

	public static class Part
	{
		protected final HistoricalDataQuery m_query;

		private Part(final HistoricalDataQuery query)
		{
			m_query = query;
		}
	}

	public static class Part_AllowsFrom extends Part_AllowsSelectColumn
	{
		private Part_AllowsFrom(HistoricalDataQuery query)
		{
			super(query);
		}

//		public HistoricalDataQuery from(final String macAddress, final UUID uuid)
//		{
//			m_query.m_macAddress = macAddress;
//			m_query.m_uuid = uuid;
//
//			return m_query;
//		}
//
//		public HistoricalDataQuery from(final BleDevice device, final UUID uuid)
//		{
//			return from(device.getMacAddress(), uuid);
//		}

		public HistoricalDataQuery from(final UUID uuid)
		{
			return from("", uuid);
		}

		public HistoricalDataQuery from(final String macAddress, final UUID uuid)
		{
			m_query.m_uuid = uuid;
			m_query.m_macAddress = macAddress;

			return m_query;
		}
	}

	public static class Part_AllowsSelectColumn extends Part
	{
		private Part_AllowsSelectColumn(HistoricalDataQuery query)
		{
			super(query);
		}

		public Part_SelectColumn data()
		{
			return column(HistoricalDataColumn.DATA);
		}

		public Part_SelectColumn epochTime()
		{
			return column(HistoricalDataColumn.EPOCH_TIME);
		}

		private Part_SelectColumn column(final HistoricalDataColumn column)
		{
			if( !m_query.m_select.isEmpty() )
			{
				m_query.m_select += ",";
			}

//			m_query.m_select += " " + column.getColumnName();
			m_query.m_select += " *";

			return new Part_SelectColumn(m_query);
		}

		public Part_Function min(final HistoricalDataColumn column)
		{
			return function("min", column);
		}

		public Part_Function max(final HistoricalDataColumn column)
		{
			return function("max", column);
		}

		public Part_Function avg(final HistoricalDataColumn column)
		{
			return function("avg", column);
		}

		private Part_Function function(final String function, final HistoricalDataColumn column)
		{
			if( !m_query.m_select.isEmpty() )
			{
				m_query.m_select += ",";
			}

			m_query.m_select += " "+function+"(CAST(\"+column.getColumnName()+\" AS INTEGER))";
//			m_query.m_selectParenCount++;

			return new Part_Function(m_query);
		}
	}

	public static class Part_Function extends Part_AllowsFrom
	{
		private Part_Function(final HistoricalDataQuery query)
		{
			super(query);
		}
	}

	private final IBleNode m_node;

	private UUID m_uuid = null;
	private String m_select = "";
	private String m_where = "";

	private String m_macAddress = "";

	final Backend_HistoricalDatabase m_database;

	private HistoricalDataQuery(final IBleNode node, final Backend_HistoricalDatabase database)
	{
		m_node = node;
		m_database = database;
	}

//	private void addSelectParens()
//	{
//		for( int i = 0; i < m_selectParenCount; i++ )
//		{
//			m_select += ")";
//		}
//
//		m_selectParenCount = 0;
//	}

	public static Part_Select select(final IBleNode device, final Backend_HistoricalDatabase database)
	{
		final HistoricalDataQuery query = new HistoricalDataQuery(device, database);

		return new Part_Select(query);
	}

	public Part_Where where()
	{
		return new Part_Where(this);
	}

	private UUID getUuidOrInvalid()
	{
		final UUID uuid = m_uuid != null ? m_uuid : Uuids.INVALID;

		return uuid;
	}

	private String getTableName()
	{
		final UUID uuid = getUuidOrInvalid();
		final String tableName = m_database.getTableName(m_macAddress, uuid);

		return tableName;
	}

	private String makeQuery()
	{
		final String tableName = getTableName();
		String query = "SELECT" + m_select + " FROM " + tableName;

		if( !m_where.isEmpty() )
		{
			query += " WHERE" + m_where;
		}

		return query;
	}

	private HistoricalDataQueryListener.HistoricalDataQueryEvent go_earlyOut()
	{
		final UUID uuid = getUuidOrInvalid();

		if( false == m_database.doesDataExist(m_macAddress, uuid) )
		{
			BleNode node;
			if (this instanceof IBleDevice)
				node = ((IBleDevice) m_node).getBleDevice();
			else
				node = ((IBleServer) m_node).getBleServer();

			return P_Bridge_User.newHistoricalDataQueryEvent(node, uuid, EmptyCursor.SINGLETON, HistoricalDataQueryListener.Status.NO_TABLE, "");
		}

		return null;
	}

	public HistoricalDataQueryListener.HistoricalDataQueryEvent go()
	{
		final HistoricalDataQueryListener.HistoricalDataQueryEvent e_earlyOut = go_earlyOut();

		if( e_earlyOut != null )
		{
			return e_earlyOut;
		}

		return m_node.queryHistoricalData(makeQuery());
	}

	public void go(final HistoricalDataQueryListener listener)
	{
		final HistoricalDataQueryListener.HistoricalDataQueryEvent e_earlyOut = go_earlyOut();

		if( e_earlyOut != null )
		{
			listener.onEvent(e_earlyOut);

			return;
		}

		m_node.queryHistoricalData(makeQuery(), listener);
	}

	private String getMacAddress()
	{
		if( m_node instanceof IBleDevice)
		{
			return m_macAddress == null || m_macAddress.isEmpty() ? ((IBleDevice)m_node).getMacAddress() : m_macAddress;
		}
		else
		{
			return m_macAddress;
		}
	}
}
