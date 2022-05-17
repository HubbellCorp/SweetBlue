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
import com.idevicesinc.sweetblue.ReadWriteListener;
import com.idevicesinc.sweetblue.internal.android.P_GattHolder;
import com.idevicesinc.sweetblue.utils.Utils;

final class P_Task_ExecuteReliableWrite extends PA_Task_RequiresConnection implements PA_Task.I_StateListener
{
	private final PE_TaskPriority m_priority;
	private final ReadWriteListener m_listener;

	public P_Task_ExecuteReliableWrite(IBleDevice device, ReadWriteListener listener, PE_TaskPriority priority)
	{
		super(device, null);

		m_priority = priority;
		m_listener = listener;
	}

	private ReadWriteListener.ReadWriteEvent newEvent(final ReadWriteListener.Status status, final int gattStatus, final boolean solicited)
	{
		return getDevice().getReliableWriteManager().newEvent(status, gattStatus, solicited);
	}

	private void invokeListeners(final ReadWriteListener.Status status, final int gattStatus)
	{
		final ReadWriteListener.ReadWriteEvent event = newEvent(ReadWriteListener.Status.NOT_CONNECTED, gattStatus, /*solicited=*/true);

		getDevice().invokeReadWriteCallback(m_listener, event);
	}

	@Override protected void onNotExecutable()
	{
		super.onNotExecutable();

		invokeListeners(ReadWriteListener.Status.NOT_CONNECTED, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
	}

	@Override protected BleTask getTaskType()
	{
		return BleTask.RELIABLE_WRITE;
	}

	@Override void execute()
	{
		if( false == getDevice().nativeManager().executeReliableWrite() )
		{
			fail(ReadWriteListener.Status.FAILED_TO_SEND_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
		}
		else
		{
			// SUCCESS, so far
		}
	}

	public void onReliableWriteCompleted(final P_GattHolder gatt, final int gattStatus)
	{
		if( Utils.isSuccess(gattStatus) )
		{
			succeed();

			invokeListeners(ReadWriteListener.Status.SUCCESS, gattStatus);
		}
		else
		{
			fail(ReadWriteListener.Status.REMOTE_GATT_FAILURE, gattStatus);
		}
	}

	private void fail(final ReadWriteListener.Status status, final int gattStatus)
	{
		super.fail();

		invokeListeners(ReadWriteListener.Status.REMOTE_GATT_FAILURE, gattStatus);
	}

	@Override public PE_TaskPriority getPriority()
	{
		return m_priority;
	}

	@Override public void onStateChange(PA_Task task, PE_TaskState state)
	{
		if( state == PE_TaskState.TIMED_OUT )
		{
			getDevice().invokeReadWriteCallback(m_listener, newEvent(ReadWriteListener.Status.TIMED_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, /*solicited=*/true));
		}
		else if( state == PE_TaskState.SOFTLY_CANCELLED )
		{
			getDevice().invokeReadWriteCallback(m_listener, newEvent(getCancelType(), BleStatuses.GATT_STATUS_NOT_APPLICABLE, /*solicited=*/true));
		}
	}
}
