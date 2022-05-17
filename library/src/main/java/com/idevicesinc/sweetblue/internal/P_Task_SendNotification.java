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


import com.idevicesinc.sweetblue.BleCharacteristic;
import com.idevicesinc.sweetblue.BleStatuses;
import com.idevicesinc.sweetblue.BleTask;
import com.idevicesinc.sweetblue.ExchangeListener;
import com.idevicesinc.sweetblue.OutgoingListener;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.internal.android.IBluetoothDevice;
import com.idevicesinc.sweetblue.internal.android.P_DeviceHolder;
import com.idevicesinc.sweetblue.utils.FutureData;
import com.idevicesinc.sweetblue.utils.P_Const;
import com.idevicesinc.sweetblue.utils.Utils;
import java.util.UUID;


final class P_Task_SendNotification extends PA_Task_RequiresServerConnection implements PA_Task.I_StateListener
{
	private final IBleServer m_server;
	private final IBluetoothDevice m_nativeDevice;

	private final OutgoingListener m_responseListener;
	private final FutureData m_futureData;

	private final UUID m_charUuid;
	private final UUID m_serviceUuid;

	private final boolean m_confirm;

	private byte[] m_data_sent = null;


	public P_Task_SendNotification(IBleServer server, IBluetoothDevice device, final UUID serviceUuid, final UUID charUuid, final FutureData futureData, boolean confirm, final OutgoingListener responseListener)
	{
		super(server, device.getAddress());

		m_server = server;
		m_nativeDevice = device;
		m_futureData = futureData;
		m_responseListener = responseListener;
		m_charUuid = charUuid;
		m_serviceUuid = serviceUuid;
		m_confirm = confirm;
	}

	private byte[] data_sent()
	{
		if( m_data_sent == null )
		{
			m_data_sent = m_futureData.getData();
		}

		return m_data_sent;
	}

	@Override protected BleTask getTaskType()
	{
		return BleTask.SEND_NOTIFICATION;
	}

	@Override void execute()
	{
		final BleCharacteristic characteristic = getServer().getNativeBleCharacteristic(m_serviceUuid, m_charUuid, null);

		if( characteristic == null )
		{
			fail(OutgoingListener.Status.NO_MATCHING_TARGET, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
		}
		else
		{
			if( !P_Bridge_User.setCharValue(characteristic, data_sent()) )
			{
				fail(OutgoingListener.Status.FAILED_TO_SET_VALUE_ON_TARGET, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
			}
			else
			{
				final P_DeviceHolder holder = P_DeviceHolder.newHolder(m_nativeDevice.getNativeDevice());
				if( !getServer().getNativeLayer().notifyCharacteristicChanged(holder, characteristic, m_confirm) )
				{
					fail(OutgoingListener.Status.FAILED_TO_SEND_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
				}
				else
				{
					// SUCCESS, at least so far...we will see
				}
			}
		}
	}

	@Override protected void onNotExecutable()
	{
		fail(OutgoingListener.Status.NOT_CONNECTED, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
	}

	private ExchangeListener.Type getType()
	{
		return m_confirm ? ExchangeListener.Type.INDICATION : ExchangeListener.Type.NOTIFICATION;
	}

	private void fail(final OutgoingListener.Status status, final int gattStatus_received)
	{
		super.fail();

		invokeFailCallback(status, gattStatus_received);
	}

	private void invokeFailCallback(final OutgoingListener.Status status, final int gattStatus_received)
	{
		final OutgoingListener.OutgoingEvent e = P_Bridge_User.newOutgoingEvent(
			getServer().getBleServer(), P_DeviceHolder.newHolder(m_nativeDevice.getNativeDevice()), m_serviceUuid, m_charUuid, ExchangeListener.ExchangeEvent.NON_APPLICABLE_UUID, getType(),
			ExchangeListener.Target.CHARACTERISTIC, P_Const.EMPTY_BYTE_ARRAY, data_sent(), ExchangeListener.ExchangeEvent.NON_APPLICABLE_REQUEST_ID,
			/*offset=*/0, /*responseNeeded=*/false, status, BleStatuses.GATT_STATUS_NOT_APPLICABLE, gattStatus_received, /*solicited=*/true
		);

		getServer().invokeOutgoingListeners(e, m_responseListener);
	}

	protected void succeed(final int gattStatus)
	{
		super.succeed();

		final OutgoingListener.OutgoingEvent e = P_Bridge_User.newOutgoingEvent(
			getServer().getBleServer(), P_DeviceHolder.newHolder(m_nativeDevice.getNativeDevice()), m_serviceUuid, m_charUuid, ExchangeListener.ExchangeEvent.NON_APPLICABLE_UUID, getType(),
			ExchangeListener.Target.CHARACTERISTIC, P_Const.EMPTY_BYTE_ARRAY, data_sent(), ExchangeListener.ExchangeEvent.NON_APPLICABLE_REQUEST_ID,
			/*offset=*/0, /*responseNeeded=*/false, OutgoingListener.Status.SUCCESS, BleStatuses.GATT_STATUS_NOT_APPLICABLE, gattStatus, /*solicited=*/true
		);

		getServer().invokeOutgoingListeners(e, m_responseListener);
	}

	void onNotificationSent(final P_DeviceHolder device, final int gattStatus)
	{
		if( Utils.isSuccess(gattStatus) )
		{
			succeed(gattStatus);
		}
		else
		{
			fail(OutgoingListener.Status.REMOTE_GATT_FAILURE, gattStatus);
		}
	}

	public PE_TaskPriority getPriority()
	{
		return PE_TaskPriority.FOR_NORMAL_READS_WRITES;
	}

	@Override public void onStateChange( PA_Task task, PE_TaskState state )
	{
		if( state == PE_TaskState.SOFTLY_CANCELLED )
		{
			invokeFailCallback(getCancelStatusType(), BleStatuses.GATT_STATUS_NOT_APPLICABLE);
		}
		else if( state == PE_TaskState.TIMED_OUT )
		{
			invokeFailCallback(OutgoingListener.Status.TIMED_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
		}
	}
}
