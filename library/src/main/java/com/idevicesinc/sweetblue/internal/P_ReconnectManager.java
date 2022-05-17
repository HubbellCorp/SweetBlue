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

import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleNode;
import com.idevicesinc.sweetblue.BleStatuses;
import com.idevicesinc.sweetblue.DeviceReconnectFilter.ConnectFailEvent;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.ReconnectFilter;
import com.idevicesinc.sweetblue.utils.Interval;


final class P_ReconnectManager
{
	private static final double NOT_RUNNING = -1.0;

	private final P_BleDeviceImpl m_device;
	private ConnectFailEvent m_connectionFailEvent;

	private double m_totalTime;
	private int m_attemptCount;
	private double m_delay = 0.0;
	private Interval m_timeout = null;
	private double m_timeTracker = NOT_RUNNING;
	private int m_gattStatusOfOriginalDisconnect = BleStatuses.GATT_STATUS_NOT_APPLICABLE;
	private final boolean m_isShortTerm;

	private ReconnectFilter.ConnectionLostPlease m_cachedConnectionLostPlease;


	
	P_ReconnectManager(final IBleDevice device, ConnectFailEvent nullConnectionFailEvent, final boolean isShortTerm)
	{

		m_device = (P_BleDeviceImpl) device;
		m_isShortTerm = isShortTerm;
		
		m_connectionFailEvent = nullConnectionFailEvent;
	}
	
	void attemptStart(final int gattStatusOfDisconnect)
	{
		m_totalTime = 0.0;
		m_attemptCount = 0;
		m_connectionFailEvent = m_device.getConnectionManager().NULL_CONNECTIONFAIL_INFO();

		// If the timeout interval is not null, then we have already pulled the delay time
		// This makes it so we don't call onConnectionLost again
		if (m_timeout == null)
		{
			m_delay = getDelayTime(m_connectionFailEvent);
		}
		
		if( m_delay < 0.0 )
		{
			m_timeTracker = NOT_RUNNING;
			m_gattStatusOfOriginalDisconnect = BleStatuses.GATT_STATUS_NOT_APPLICABLE;
		}
		else
		{
			if( !isRunning() )
			{
				m_device.getIManager().pushWakeLock();
			}
			
			m_timeTracker = 0.0;
			m_gattStatusOfOriginalDisconnect = gattStatusOfDisconnect;
		}
	}

	ReconnectFilter.ConnectionLostPlease onConnectionLost(ReconnectFilter.ConnectFailEvent failEvent) {
		ReconnectFilter.Type type = m_isShortTerm ? ReconnectFilter.Type.SHORT_TERM__SHOULD_CONTINUE : ReconnectFilter.Type.LONG_TERM__SHOULD_CONTINUE;
		ReconnectFilter.ConnectionLostEvent event = newEvent(m_device.getBleDevice(), m_device.getMacAddress(), m_attemptCount, Interval.secs(m_totalTime), Interval.secs(m_delay), failEvent, type);
		m_cachedConnectionLostPlease = getFilter().onConnectionLost(event);

		m_timeout = P_Bridge_User.timeout(m_cachedConnectionLostPlease);

		if (m_timeout != null)
		{
			final Interval delay = m_cachedConnectionLostPlease != null ? P_Bridge_User.interval(m_cachedConnectionLostPlease) : null;
			m_delay = delay != null ? delay.secs() : Interval.DISABLED.secs();
		}

		return m_cachedConnectionLostPlease;
	}

	int gattStatusOfOriginalDisconnect()
	{
		return m_gattStatusOfOriginalDisconnect;
	}
	
	boolean isRunning()
	{
		return m_timeTracker >= 0.0;
	}
	
	private ReconnectFilter getFilter()
	{
		// Look for explicitly set reconnect filter, first on the device itself, then the manager, then fall back to the config ones

		ReconnectFilter filter = m_device.getListener_Reconnect();

		if (filter != null)	return filter;

		filter = m_device.getIManager().getDefaultDeviceReconnectFilter();

		if (filter != null)	return filter;

		filter = m_device.conf_device().reconnectFilter;

		return filter != null ? filter : m_device.conf_mngr().reconnectFilter;
	}

	private ReconnectFilter.ConnectionLostPlease getConnectionLostPlease(ReconnectFilter.ConnectFailEvent failEvent, boolean forceCallOnConnectLost)
	{
		if (m_cachedConnectionLostPlease != null && !forceCallOnConnectLost)
			return m_cachedConnectionLostPlease;


		ReconnectFilter.Type type = m_isShortTerm ? ReconnectFilter.Type.SHORT_TERM__SHOULD_CONTINUE : ReconnectFilter.Type.LONG_TERM__SHOULD_CONTINUE;
		ReconnectFilter.ConnectionLostEvent event = newEvent(m_device.getBleDevice(), m_device.getMacAddress(), m_attemptCount, Interval.secs(m_totalTime), Interval.secs(m_delay), failEvent, type);
		m_cachedConnectionLostPlease = getFilter().onConnectionLost(event);

		m_timeout = P_Bridge_User.timeout(m_cachedConnectionLostPlease);

		type = m_isShortTerm ? ReconnectFilter.Type.SHORT_TERM__SHOULD_TRY_AGAIN : ReconnectFilter.Type.LONG_TERM__SHOULD_TRY_AGAIN;
		event = newEvent(m_device.getBleDevice(), m_device.getMacAddress(), m_attemptCount, Interval.secs(m_totalTime), Interval.secs(m_delay), failEvent, type);
		ReconnectFilter.ConnectionLostPlease please = getFilter().onConnectionLost(event);

		return m_cachedConnectionLostPlease;
	}

	private double getDelayTime(ConnectFailEvent connectFailEvent)
	{
		final ReconnectFilter filter = getFilter();

		if( filter == null )
		{
			return Interval.DISABLED.secs();
		}
		else
		{
			ReconnectFilter.Type type = m_isShortTerm ? ReconnectFilter.Type.SHORT_TERM__SHOULD_TRY_AGAIN : ReconnectFilter.Type.LONG_TERM__SHOULD_TRY_AGAIN;
			ReconnectFilter.ConnectionLostEvent event = newEvent(m_device.getBleDevice(), m_device.getMacAddress(), m_attemptCount, Interval.secs(m_totalTime), Interval.secs(m_delay), connectFailEvent, type);
			ReconnectFilter.ConnectionLostPlease please = filter.onConnectionLost(event);

			m_device.getIManager().getLogger().checkPlease(please, ReconnectFilter.ConnectionLostPlease.class);

			if( false == P_Bridge_User.shouldPersist(please) )
			{
				return Interval.DISABLED.secs();
			}
			else
			{
				final Interval delay = please != null ? P_Bridge_User.interval(please) : null;
				return delay != null ? delay.secs() : Interval.DISABLED.secs();
			}
		}
	}

	void onConnectionFailed(final ConnectFailEvent connectionFailInfo)
	{
		if (!isRunning())
		{
			return;
		}
		
		m_attemptCount++;
		m_timeTracker = 0.0;

		if (!shouldStop())
		{
			m_connectionFailEvent = connectionFailInfo;
			// If we have a timeout, then we dont call onConnectionLost again
			if (m_timeout == null)
			{
				m_delay = getDelayTime(m_connectionFailEvent);
			}
			m_timeTracker = 0.0;
		}
	}
	
	void update(double timeStep)
	{
		if( !isRunning() )  return;

		m_totalTime += timeStep;
		
		if( !m_isShortTerm && !m_device.is(BleDeviceState.RECONNECTING_LONG_TERM) )  return;
		if( m_isShortTerm && !m_device.is(BleDeviceState.RECONNECTING_SHORT_TERM) )  return;
		
		m_timeTracker += timeStep;

		if (m_timeTracker >= m_delay)
		{
			if (!m_device.is_internal(BleDeviceState.CONNECTING_OVERALL) && shouldContinueRunning())
			{
				m_device.getConnectionManager().attemptReconnect();
			}
		}
	}

	private boolean shouldStop()
	{
		boolean stop;
		if (m_timeout == null)
		{

			final ReconnectFilter.ConnectionLostPlease please = getConnectionLostPlease(m_connectionFailEvent, false);

			m_device.getIManager().getLogger().checkPlease(please, ReconnectFilter.ConnectionLostPlease.class);

			stop = please == null || P_Bridge_User.shouldPersist(please);
		}
		else
		{
			stop = m_timeout.lte(m_totalTime);
		}

		if (stop)
		{
			final int gattStatusOfOriginalDisconnect = gattStatusOfOriginalDisconnect();

			stop();

			if (m_isShortTerm)
			{
				m_device.onNativeDisconnect(false, gattStatusOfOriginalDisconnect, false, true);
			}
			else
			{
				m_device.onLongTermReconnectTimeOut();
				m_device.onNativeDisconnect(false, gattStatusOfOriginalDisconnect, false, true);
				m_device.dropReconnectingLongTermState();
			}
		}
		return stop;
	}

	private boolean shouldContinueRunning()
	{
		ReconnectFilter persistFilter = getFilter();

		// This is for old behavior. Before, if the persist filter was null, we'd ignore it and continue running without explicitly stopping. This achieves
		// the same behavior.
		if( persistFilter == null )  return true;

		// If should stop is true, then we should not continue, so flip the boolean here
		return !shouldStop();
	}
	
	final void stop()
	{
		if( isRunning() )
		{
			m_device.getIManager().popWakeLock();
		}
		
		m_timeTracker = NOT_RUNNING;
		m_attemptCount = 0;
		m_totalTime = 0.0;
		m_cachedConnectionLostPlease = null;
		m_connectionFailEvent = m_device.getConnectionManager().NULL_CONNECTIONFAIL_INFO();
		m_gattStatusOfOriginalDisconnect = BleStatuses.GATT_STATUS_NOT_APPLICABLE;
	}

	private ReconnectFilter.ConnectionLostEvent newEvent(BleNode node, final String macAddress, int failureCount, Interval totalTimeReconnecting, Interval previousDelay, ReconnectFilter.ConnectFailEvent connectionFailEvent, final ReconnectFilter.Type type)
	{
		return P_Bridge_User.newConnectLostEvent(node, macAddress, failureCount, totalTimeReconnecting, previousDelay, connectionFailEvent, type);
	}
}
