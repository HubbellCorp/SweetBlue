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

import com.idevicesinc.sweetblue.BleStatuses;
import com.idevicesinc.sweetblue.BleTask;
import com.idevicesinc.sweetblue.IncomingListener;
import com.idevicesinc.sweetblue.OutgoingListener;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.internal.android.P_DeviceHolder;
import com.idevicesinc.sweetblue.utils.FutureData;
import com.idevicesinc.sweetblue.utils.P_Const;


final class P_Task_SendReadWriteResponse extends PA_Task_RequiresServerConnection implements PA_Task.I_StateListener
{
	private final IncomingListener.IncomingEvent m_requestEvent;
	private final IncomingListener.Please m_please;

	private byte[] m_data_sent = null;

	public P_Task_SendReadWriteResponse(IBleServer server, final IncomingListener.IncomingEvent requestEvent, IncomingListener.Please please)
	{
		super( server, requestEvent.macAddress());

		m_requestEvent = requestEvent;
		m_please = please;
	}

	private byte[] data_sent()
	{
		if( m_data_sent == null )
		{
			final FutureData data = P_Bridge_User.getFutureData(m_please);
			m_data_sent = data != null ? data.getData() : P_Const.EMPTY_BYTE_ARRAY;
			m_data_sent = m_data_sent != null ? m_data_sent : P_Const.EMPTY_BYTE_ARRAY;
		}

		return m_data_sent;
	}

	@Override public void onStateChange( PA_Task task, PE_TaskState state )
	{
		if( state == PE_TaskState.SOFTLY_CANCELLED )
		{
			invokeFailCallback(getCancelStatusType());
		}
		else if( state == PE_TaskState.TIMED_OUT )
		{
			invokeFailCallback(OutgoingListener.Status.TIMED_OUT);
		}
	}

	private void fail(final OutgoingListener.Status status)
	{
		super.fail();

		invokeFailCallback(status);
	}

	private void invokeFailCallback(OutgoingListener.Status status)
	{
		final OutgoingListener.OutgoingEvent e = P_Bridge_User.newOutgoingEvent(m_requestEvent, data_sent(), status, P_Bridge_User.gattStatus(m_please), BleStatuses.GATT_STATUS_NOT_APPLICABLE);

		getServer().invokeOutgoingListeners(e, P_Bridge_User.getOutgoingListener(m_please));
	}

	@Override protected void succeed()
	{
		super.succeed();

		final OutgoingListener.OutgoingEvent e = P_Bridge_User.newOutgoingEvent(m_requestEvent, data_sent(), OutgoingListener.Status.SUCCESS, P_Bridge_User.gattStatus(m_please), BleStatuses.GATT_STATUS_NOT_APPLICABLE);

		getServer().invokeOutgoingListeners(e, P_Bridge_User.getOutgoingListener(m_please));
	}

	@Override void execute()
	{
		final P_DeviceHolder holder = P_DeviceHolder.newHolder(m_requestEvent.nativeDevice());
		if( false == getServer().getNativeLayer().sendResponse(holder, m_requestEvent.requestId(), P_Bridge_User.gattStatus(m_please), m_requestEvent.offset(), data_sent()) )
		{
			fail(OutgoingListener.Status.FAILED_TO_SEND_OUT);
		}
		else
		{
			// SUCCESS, and we'll wait a half second or so (for now hardcoded) until actually succeeding, cause there's no native callback for this one.
		}
	}

	@Override protected void onNotExecutable()
	{
		fail(OutgoingListener.Status.NOT_CONNECTED);
	}

	@Override protected void update(double timeStep)
	{
		final double timeToSuccess = .25d; //TODO

		if( getState() == PE_TaskState.EXECUTING && getTotalTimeExecuting() >= timeToSuccess )
		{
			succeed();
		}
	}

	@Override public PE_TaskPriority getPriority()
	{
		return PE_TaskPriority.FOR_NORMAL_READS_WRITES;
	}

	@Override protected BleTask getTaskType()
	{
		return BleTask.SEND_READ_WRITE_RESPONSE;
	}
}
