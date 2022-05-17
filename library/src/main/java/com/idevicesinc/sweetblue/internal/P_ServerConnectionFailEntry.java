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

import android.bluetooth.BluetoothDevice;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.ServerConnectListener;
import com.idevicesinc.sweetblue.ServerReconnectFilter;
import com.idevicesinc.sweetblue.internal.android.P_DeviceHolder;
import com.idevicesinc.sweetblue.utils.Interval;
import java.util.ArrayList;


final class P_ServerConnectionFailEntry
{
	private int m_failCount = 0;

	private Long m_timeOfFirstConnect = null;
	private Long m_timeOfLastConnectFail = null;

	private final ArrayList<ServerReconnectFilter.ConnectFailEvent> m_history = new ArrayList<>();

	private final P_ServerConnectionFailManager m_mngr;


	P_ServerConnectionFailEntry(final P_ServerConnectionFailManager mngr)
	{
		m_mngr = mngr;

		resetFailCount();
	}

	void onExplicitDisconnect()
	{
		resetFailCount();
	}

	void onExplicitConnectionStarted()
	{
		resetFailCount();

		m_timeOfFirstConnect = System.currentTimeMillis();
	}

	private void resetFailCount()
	{
		m_failCount = 0;
		m_timeOfFirstConnect = m_timeOfLastConnectFail = null;
		m_history.clear();
	}

	void onNativeConnectFail(final P_DeviceHolder nativeDevice, final ServerReconnectFilter.Status status, final int gattStatus)
	{
		final long currentTime = System.currentTimeMillis();

		//--- DRK > Can be null if this is a spontaneous connect (can happen with autoConnect sometimes for example).
		m_timeOfFirstConnect = m_timeOfFirstConnect != null ? m_timeOfFirstConnect : currentTime;
		final Long timeOfLastConnectFail = m_timeOfLastConnectFail != null ? m_timeOfLastConnectFail : m_timeOfFirstConnect;
		final Interval attemptTime_latest = Interval.delta(timeOfLastConnectFail, currentTime);
		final Interval attemptTime_total = Interval.delta(m_timeOfFirstConnect, currentTime);

		m_failCount++;

		final ServerReconnectFilter.ConnectFailEvent e = P_Bridge_User.newServerConnectFailEvent(
			m_mngr.m_server.getBleServer(), nativeDevice.getDevice(), status, m_failCount, attemptTime_latest, attemptTime_total,
			gattStatus, ServerReconnectFilter.AutoConnectUsage.NOT_APPLICABLE, m_history
		);

		m_history.add(e);

		boolean canceledOrExplicit = status == ServerReconnectFilter.Status.CANCELLED_FROM_DISCONNECT || status == ServerReconnectFilter.Status.CANCELLED_FROM_BLE_TURNING_OFF;
		final ServerConnectListener.ConnectEvent event = P_Bridge_User.newServerConnectEvent(m_mngr.m_server.getBleServer(), nativeDevice.getAddress(), e);

		if (!canceledOrExplicit)
		{
			final P_ConnectFailPlease ePlease__PE_Please = m_mngr.invokeCallback(e);

			if (ePlease__PE_Please.isRetry())
				m_mngr.m_server.connect_internal(nativeDevice, true);
			else
			{
				m_mngr.m_server.invokeConnectListeners(event);
				resetFailCount();
			}
		}
		else
			m_mngr.m_server.invokeConnectListeners(event);
	}
}
