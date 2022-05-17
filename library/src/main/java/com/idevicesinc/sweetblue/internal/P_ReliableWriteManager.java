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
import com.idevicesinc.sweetblue.BleStatuses;
import com.idevicesinc.sweetblue.BleWrite;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.ReadWriteListener;
import com.idevicesinc.sweetblue.internal.android.P_GattHolder;
import com.idevicesinc.sweetblue.utils.Utils;


final class P_ReliableWriteManager
{
	private final IBleDevice m_device;

	private ReadWriteListener m_listener;

	P_ReliableWriteManager(final IBleDevice device)
	{
		m_device = device;
	}

	public void onDisconnect()
	{
		m_listener = null;
	}

	ReadWriteListener.ReadWriteEvent newEvent(final ReadWriteListener.Status status, final int gattStatus, final boolean solicited)
	{
		return P_Bridge_User.newReadWriteEvent(m_device.getBleDevice(), BleWrite.INVALID, ReadWriteListener.Type.WRITE, ReadWriteListener.Target.RELIABLE_WRITE, status, gattStatus, 0.0, 0.0, solicited);
	}

	private ReadWriteListener.ReadWriteEvent getGeneralEarlyOutEvent()
	{
		final int gattStatus = BleStatuses.GATT_STATUS_NOT_APPLICABLE;

		if( m_device.isNull() )
		{
			return newEvent(ReadWriteListener.Status.NULL_DEVICE, BleStatuses.GATT_STATUS_NOT_APPLICABLE, /*solicited=*/true);
		}
		else
		{
			if( false == m_device.is(BleDeviceState.BLE_CONNECTED) )
			{
				return newEvent(ReadWriteListener.Status.NOT_CONNECTED, BleStatuses.GATT_STATUS_NOT_APPLICABLE, /*solicited=*/true);
			}
			else if( true == m_device.is(BleDeviceState.RECONNECTING_SHORT_TERM) )
			{
				return newEvent(ReadWriteListener.Status.NOT_CONNECTED, BleStatuses.GATT_STATUS_NOT_APPLICABLE, /*solicited=*/true);
			}
			else
			{
				return null;
			}
		}
	}

	private ReadWriteListener.ReadWriteEvent getNeverBeganEarlyOutEvent()
	{
		if( m_listener == null )
		{
			final ReadWriteListener.ReadWriteEvent e_earlyOut = getGeneralEarlyOutEvent();

			if( e_earlyOut != null )
			{
				return e_earlyOut;
			}
			else
			{
				final ReadWriteListener.ReadWriteEvent e_earlyOut_specific = newEvent(ReadWriteListener.Status.RELIABLE_WRITE_NEVER_BEGAN, BleStatuses.GATT_STATUS_NOT_APPLICABLE, /*solicited=*/true);

				return e_earlyOut_specific;
			}
		}
		else
		{
			return null;
		}
	}

	public ReadWriteListener.ReadWriteEvent begin(final ReadWriteListener listener)
	{
		final ReadWriteListener.ReadWriteEvent e_earlyOut = getGeneralEarlyOutEvent();

		if( e_earlyOut != null )
		{
			m_device.invokeReadWriteCallback(listener, e_earlyOut);

			return e_earlyOut;
		}
		else
		{
			if( m_listener != null )
			{
				final ReadWriteListener.ReadWriteEvent e_earlyOut_specific = newEvent(ReadWriteListener.Status.RELIABLE_WRITE_ALREADY_BEGAN, BleStatuses.GATT_STATUS_NOT_APPLICABLE, /*solicited=*/true);

				m_device.invokeReadWriteCallback(listener, e_earlyOut_specific);

				return e_earlyOut_specific;
			}
			else
			{
				if( false == m_device.nativeManager().getGattLayer().beginReliableWrite() )
				{
					final ReadWriteListener.ReadWriteEvent e_earlyOut_specific = newEvent(ReadWriteListener.Status.RELIABLE_WRITE_FAILED_TO_BEGIN, BleStatuses.GATT_STATUS_NOT_APPLICABLE, /*solicited=*/true);

					m_device.invokeReadWriteCallback(listener, e_earlyOut_specific);

					return e_earlyOut_specific;
				}
				else
				{
					m_listener = listener;

					return P_Bridge_User.newReadWriteEventNULL(m_device.getBleDevice());
				}
			}
		}
	}

	public ReadWriteListener.ReadWriteEvent abort()
	{
		final ReadWriteListener.ReadWriteEvent e_earlyOut = getNeverBeganEarlyOutEvent();

		if( e_earlyOut != null )
		{
			m_device.invokeReadWriteCallback(m_listener, e_earlyOut);

			return e_earlyOut;
		}
		else
		{
			final ReadWriteListener listener = m_listener;
			m_listener = null;

			abortReliableWrite();

			final ReadWriteListener.ReadWriteEvent e = newEvent(ReadWriteListener.Status.RELIABLE_WRITE_ABORTED, BleStatuses.GATT_STATUS_NOT_APPLICABLE, /*solicited=*/true);

			m_device.invokeReadWriteCallback(listener, e);

			return e;
		}
	}

	private void abortReliableWrite()
	{
		m_device.nativeManager().getGattLayer().abortReliableWrite(m_device.getNative().getNativeDevice());
	}

	public ReadWriteListener.ReadWriteEvent execute()
	{
		final ReadWriteListener.ReadWriteEvent e_earlyOut = getNeverBeganEarlyOutEvent();

		if( e_earlyOut != null )
		{
			m_device.invokeReadWriteCallback(m_listener, e_earlyOut);

			return e_earlyOut;
		}
		else
		{
			final ReadWriteListener listener = m_listener;
			m_listener = null;

			final P_Task_ExecuteReliableWrite task = new P_Task_ExecuteReliableWrite(m_device, listener, m_device.getOverrideReadWritePriority());

			m_device.getIManager().getTaskManager().add(task);

			return P_Bridge_User.newReadWriteEventNULL(m_device.getBleDevice());
		}
	}

	public void onReliableWriteCompleted_unsolicited(final P_GattHolder gatt, final int gattStatus)
	{
		final ReadWriteListener listener = m_listener;
		m_listener = null;

		final ReadWriteListener.Status status = Utils.isSuccess(gattStatus) ? ReadWriteListener.Status.SUCCESS : ReadWriteListener.Status.REMOTE_GATT_FAILURE;
		final ReadWriteListener.ReadWriteEvent e = newEvent(status, gattStatus, /*solicited=*/false);

		m_device.invokeReadWriteCallback(listener, e);
	}
}
