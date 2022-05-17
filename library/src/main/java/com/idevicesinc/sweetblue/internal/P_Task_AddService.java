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


import com.idevicesinc.sweetblue.AddServiceListener;
import com.idevicesinc.sweetblue.BleManagerState;
import com.idevicesinc.sweetblue.BleService;
import com.idevicesinc.sweetblue.BleStatuses;
import com.idevicesinc.sweetblue.BleTask;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.internal.android.IBluetoothServer;
import com.idevicesinc.sweetblue.utils.Utils;


final class P_Task_AddService extends PA_Task_RequiresBleOn implements PA_Task.I_StateListener
{
	private final BleService m_service;
	private final AddServiceListener m_addListener;

	private boolean m_cancelledInTheMiddleOfExecuting = false;


	public P_Task_AddService(IBleServer server, final BleService service, final AddServiceListener addListener)
	{
		super(server, null);

		m_service = service;
		m_addListener = addListener;
	}

	@Override void execute()
	{
		final IBluetoothServer server_native_nullable = getServer().getNativeLayer();

		if( server_native_nullable.isServerNull() )
		{
			if( !getServer().getNativeManager().openServer() )
			{
				failImmediately(AddServiceListener.Status.SERVER_OPENING_FAILED);

				return;
			}
		}

		final IBluetoothServer server_native = getServer().getNativeLayer();

		if( server_native.isServerNull() )
		{
			failImmediately(AddServiceListener.Status.SERVER_OPENING_FAILED);

			getManager().ASSERT(false, "Server should not be null after successfully opening!");
		}
		else
		{
			if( server_native.addService(m_service) )
			{
				// SUCCESS, so far...
			}
			else
			{
				failImmediately(AddServiceListener.Status.FAILED_IMMEDIATELY);
			}
		}
	}

	@Override protected void onNotExecutable()
	{
		getManager().ASSERT(false, "Should never have gotten into the queue, and if BLE goes OFF in the mean time should be removed from queue.");

		super.onNotExecutable();

		invokeFailCallback(AddServiceListener.Status.BLE_NOT_ON, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
	}

	public BleService getService()
	{
		return m_service;
	}

	private void fail(final AddServiceListener.Status status, final int gattStatus)
	{
		super.fail();

		invokeFailCallback(status, gattStatus);
	}

	private void failImmediately(final AddServiceListener.Status status)
	{
		super.failImmediately();

		invokeFailCallback(status, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
	}

	private void invokeFailCallback(final AddServiceListener.Status status, final int gattStatus)
	{
		final AddServiceListener.ServiceAddEvent e = P_Bridge_User.newServiceAddEvent(
			getServer().getBleServer(), m_service.getService(), status, gattStatus, /*solicited=*/true
		);

		getServer().getServerServiceManager().invokeListeners(e, m_addListener);
	}

	protected void succeed(final int gattStatus)
	{
		super.succeed();

		final AddServiceListener.ServiceAddEvent e = P_Bridge_User.newServiceAddEvent(
			getServer().getBleServer(), m_service.getService(), AddServiceListener.Status.SUCCESS, BleStatuses.GATT_SUCCESS, /*solicited=*/true
		);

		getServer().getServerServiceManager().invokeListeners(e, m_addListener);
	}

	public boolean cancelledInTheMiddleOfExecuting()
	{
		return m_cancelledInTheMiddleOfExecuting;
	}

	public void onServiceAdded(final int gattStatus, final BleService service)
	{
		if( m_cancelledInTheMiddleOfExecuting )
		{
			final IBluetoothServer server_native_nullable = getServer().getNativeLayer();

			if( !server_native_nullable.isServerNull() )
			{
				server_native_nullable.removeService(service);
			}

			//--- DRK > Not invoking appland callback because it was already send in call to cancel() back in time.
			fail();
		}
		else
		{
			if( Utils.isSuccess(gattStatus) )
			{
				succeed(gattStatus);
			}
			else
			{
				fail(AddServiceListener.Status.FAILED_EVENTUALLY, gattStatus);
			}
		}
	}

	public void cancel(final AddServiceListener.Status status)
	{
		if( this.getState() == PE_TaskState.ARMED )
		{
			fail();
		}
		else if( this.getState() == PE_TaskState.EXECUTING )
		{
			//--- DRK > We don't actually fail the task here because we let it run
			//--- 		its course until we get a callback from the native stack.
			m_cancelledInTheMiddleOfExecuting = true;
		}
		else
		{
			clearFromQueue();
		}

		invokeFailCallback(status, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
	}

	protected final AddServiceListener.Status getCancelStatusType()
	{
		IBleManager mngr = this.getManager();

		if( mngr.isAny(BleManagerState.TURNING_OFF, BleManagerState.OFF) )
		{
			return AddServiceListener.Status.CANCELLED_FROM_BLE_TURNING_OFF;
		}
		else
		{
			return AddServiceListener.Status.CANCELLED_FROM_DISCONNECT;
		}
	}

	@Override public PE_TaskPriority getPriority()
	{
		return PE_TaskPriority.FOR_EXPLICIT_BONDING_AND_CONNECTING;
	}

	@Override protected BleTask getTaskType()
	{
		return BleTask.ADD_SERVICE;
	}

	@Override public void onStateChange(PA_Task task, PE_TaskState state)
	{
		if( state == PE_TaskState.SOFTLY_CANCELLED )
		{
			invokeFailCallback(getCancelStatusType(), BleStatuses.GATT_STATUS_NOT_APPLICABLE);
		}
		else if( state == PE_TaskState.TIMED_OUT )
		{
			invokeFailCallback(AddServiceListener.Status.TIMED_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
		}
	}
}
