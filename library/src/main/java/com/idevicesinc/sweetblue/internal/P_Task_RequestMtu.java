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
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.ReadWriteListener;
import com.idevicesinc.sweetblue.internal.android.P_GattHolder;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent;
import com.idevicesinc.sweetblue.ReadWriteListener.Status;


final class P_Task_RequestMtu extends PA_Task_Transactionable implements PA_Task.I_StateListener
{

	protected final ReadWriteListener m_readWriteListener;
	private final int m_mtu;


	public P_Task_RequestMtu(IBleDevice device, ReadWriteListener readWriteListener, IBleTransaction txn_nullable, PE_TaskPriority priority, final int mtu)
	{
		super(device, txn_nullable, false, priority);
		
		m_readWriteListener = readWriteListener;
		m_mtu = mtu;
	}
	
	private ReadWriteEvent newEvent(Status status, int gattStatus, int mtu)
	{
		return P_Bridge_User.newReadWriteEventMtu(getDevice().getBleDevice(), /*mtu=*/mtu, status, gattStatus, getTotalTime(), getTotalTimeExecuting(), /*solicited=*/true);
	}

	@Override protected final void onNotExecutable()
	{
		super.onNotExecutable();

		getDevice().invokeReadWriteCallback(m_readWriteListener, newEvent(Status.NOT_CONNECTED, BleStatuses.GATT_STATUS_NOT_APPLICABLE, 0));
	}
	
	private void fail(Status status, int gattStatus)
	{
		this.fail();

		getDevice().invokeReadWriteCallback(m_readWriteListener, newEvent(status, gattStatus, 0));
	}

	@Override public final void execute()
	{
		if( Utils.isLollipop() )
		{
			if( false == getDevice().nativeManager().requestMtu(m_mtu))
			{
				fail(Status.FAILED_TO_SEND_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
			}
			else
			{
				// SUCCESS, so far...
			}
		}
		else
		{
			//--- DRK > Should be checked for before the task is even created but just being anal.
			fail(Status.ANDROID_VERSION_NOT_SUPPORTED, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
		}
	}
	
	private void succeed(int gattStatus, int mtu)
	{
		super.succeed();

		try
		{
			getDevice().setThreadLocalTransaction(getTxn());
			getDevice().onMtuChanged();
		}
		finally
		{
			getDevice().setThreadLocalTransaction(null);
		}

		final ReadWriteEvent event = newEvent(Status.SUCCESS, gattStatus, mtu);
		
		getDevice().invokeReadWriteCallback(m_readWriteListener, event);
	}
	
	final void onMtuChanged(P_GattHolder gatt, int mtu, int gattStatus)
	{
		getManager().ASSERT(getDevice().nativeManager().gattEquals(gatt), "");
		
		if( Utils.isSuccess(gattStatus) )
		{
			succeed(gattStatus, mtu);
		}
		else
		{
			fail(Status.REMOTE_GATT_FAILURE, gattStatus);
		}
	}
	
	@Override public final void onStateChange(PA_Task task, PE_TaskState state)
	{
		if( state == PE_TaskState.TIMED_OUT )
		{
			getDevice().invokeReadWriteCallback(m_readWriteListener, newEvent(Status.TIMED_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, 0));
		}
		else if( state == PE_TaskState.SOFTLY_CANCELLED )
		{
			getDevice().invokeReadWriteCallback(m_readWriteListener, newEvent(getCancelType(), BleStatuses.GATT_STATUS_NOT_APPLICABLE, 0));
		}
	}
	
	@Override protected final BleTask getTaskType()
	{
		return BleTask.SET_MTU;
	}
}
