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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import android.bluetooth.BluetoothGatt;
import com.idevicesinc.sweetblue.BleCharacteristic;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleNotify;
import com.idevicesinc.sweetblue.BleOp;
import com.idevicesinc.sweetblue.BleRead;
import com.idevicesinc.sweetblue.BleStatuses;
import com.idevicesinc.sweetblue.DescriptorFilter;
import com.idevicesinc.sweetblue.NotificationListener;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.ReadWriteListener;
import com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent;
import com.idevicesinc.sweetblue.ReadWriteListener.Status;
import com.idevicesinc.sweetblue.ReadWriteListener.Target;
import com.idevicesinc.sweetblue.ReadWriteListener.Type;
import com.idevicesinc.sweetblue.BondFilter.CharacteristicEventType;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.P_Const;
import com.idevicesinc.sweetblue.utils.PresentData;


final class P_PollManager
{
	static final int E_NotifyState__NOT_ENABLED		= 0;
	static final int E_NotifyState__ENABLING 		= 1;
	static final int E_NotifyState__ENABLED			= 2;
	

	
	private final IBleDevice m_device;
	private final ArrayList<CallbackEntry> m_entries = new ArrayList<>();
	private final Object m_entryLock = new Object();
	

	P_PollManager(IBleDevice device)
	{
		m_device = device;
	}

	final void clear()
	{
		synchronized (m_entryLock)
		{
			m_entries.clear();
		}
	}

	final void startPoll(final BleOp bleOp, Interval interval, boolean trackChanges, boolean usingNotify)
	{
		if( m_device.isNull() )  return;

		boolean allowDuplicatePollEntries = P_Bridge_User.bool(m_device.conf_device().allowDuplicatePollEntries, m_device.conf_mngr().allowDuplicatePollEntries);

		if( !allowDuplicatePollEntries )
		{
			final List<CallbackEntry> entryList;
			synchronized (m_entryLock)
			{
				entryList = new ArrayList<>(m_entries);
			}
			for( int i = entryList.size()-1; i >= 0; i-- )
			{
				CallbackEntry ithEntry = entryList.get(i);

				if( ithEntry.m_bleOp.getCharacteristicUuid().equals(bleOp.getCharacteristicUuid()) )
				{
					ithEntry.m_interval = interval.secs();
				}

				if( ithEntry.isFor(bleOp, interval.secs(), usingNotify) )
				{
					if( ithEntry.trackingChanges() == trackChanges)
					{
						ithEntry.m_pollingReadListener.addListener(bleOp.getReadWriteListener());

						return;
					}
				}
			}
		}

		CallbackEntry newEntry = new CallbackEntry(m_device, bleOp, interval.secs(), trackChanges, usingNotify);

		if( usingNotify )
		{
			final int/*_E_NotifyState*/ state = getNotifyState(bleOp.getServiceUuid(), bleOp.getCharacteristicUuid());
			newEntry.m_notifyState = state;
		}

		synchronized (m_entryLock)
		{
			m_entries.add(newEntry);
		}
	}

	final void stopPoll(BleOp bleOp, Double interval_nullable, boolean usingNotify)
	{
		if( m_device.isNull() )  return;

		synchronized (m_entryLock)
		{
			for (int i = m_entries.size() - 1; i >= 0; i--)
			{
				CallbackEntry ithEntry = m_entries.get(i);

				if (ithEntry.isFor(bleOp, interval_nullable, usingNotify))
				{
					m_entries.remove(i);
				}
			}
		}
	}

	final void update(double timeStep)
	{
		synchronized (m_entryLock)
		{
			for (int i = 0; i < m_entries.size(); i++)
			{
				CallbackEntry ithEntry = m_entries.get(i);

				ithEntry.update(timeStep);
			}
		}
	}

	final void onCharacteristicChangedFromNativeNotify(final UUID serviceUuid, final UUID charUuid, byte[] value)
	{
		final List<CallbackEntry> entryList;
		synchronized (m_entryLock)
		{
			entryList = new ArrayList<>(m_entries);
		}
		for( int i = 0; i < entryList.size(); i++ )
		{
			CallbackEntry ithEntry = entryList.get(i);

			// An NPE was reported from a customer where it looks like the CallbackEntry here is null. Not sure how this could happen,
			// so we're just guarding against it now
			if (ithEntry == null)
				continue;

			if( ithEntry.isFor(serviceUuid, charUuid) && ithEntry.usingNotify() )
			{
				ithEntry.onCharacteristicChangedFromNativeNotify(value);
			}
		}
	}

	final int/*__E_NotifyState*/ getNotifyState(final UUID serviceUuid, final UUID charUuid)
	{
		int/*__E_NotifyState*/ highestState = E_NotifyState__NOT_ENABLED;

		final List<CallbackEntry> entryList;
		synchronized (m_entryLock)
		{
			entryList = new ArrayList<>(m_entries);
		}

		for( int i = 0; i < entryList.size(); i++ )
		{
			CallbackEntry ithEntry = entryList.get(i);
			
			if( ithEntry.isFor(serviceUuid, charUuid) )
			{
				if( ithEntry.m_notifyState > highestState )
				{
					highestState = ithEntry.m_notifyState;
				}
			}
		}
		
		return highestState;
	}

	final void onNotifyStateChange(final UUID serviceUuid, final UUID charUuid, int/*__E_NotifyState*/ state)
	{
		final List<CallbackEntry> entryList;
		synchronized (m_entryLock)
		{
			entryList = new ArrayList<>(m_entries);
		}
		for( int i = 0; i < entryList.size(); i++ )
		{
			CallbackEntry ithEntry = entryList.get(i);
			
			if( ithEntry.usingNotify() && ithEntry.isFor(serviceUuid, charUuid) )
			{
				ithEntry.m_notifyState = state;
			}
		}
	}

	final void resetNotifyStates()
	{
		synchronized (m_entryLock)
		{
			for (int i = 0; i < m_entries.size(); i++)
			{
				CallbackEntry ithEntry = m_entries.get(i);

				ithEntry.m_notifyState = E_NotifyState__NOT_ENABLED;
			}
		}
	}

	final void enableNotifications_assumesWeAreConnected()
	{
		final List<CallbackEntry> entryList;
		synchronized (m_entryLock)
		{
			entryList = new ArrayList<>(m_entries);
		}
		for( int i = 0; i < entryList.size(); i++ )
		{
			CallbackEntry ithEntry = entryList.get(i);
			
			if( ithEntry.usingNotify() )
			{
				final UUID m_serviceUuid = ithEntry.m_bleOp.getServiceUuid();
				final UUID m_charUuid = ithEntry.m_bleOp.getCharacteristicUuid();
				final DescriptorFilter m_descriptorFilter = ithEntry.m_bleOp.getDescriptorFilter();

				int/*__E_NotifyState*/ notifyState = getNotifyState(m_serviceUuid, m_charUuid);
				
				BleCharacteristic characteristic = m_device.getNativeBleCharacteristic(m_serviceUuid, m_charUuid, m_descriptorFilter);
				
				//--- DRK > This was observed to happen while doing iterative testing on a dev board that was changing
				//---		its gatt database again and again...I guess service discovery "succeeded" but the service
				//---		wasn't actually found, so downstream we got an NPE.
				if( characteristic == null )
				{
					continue;
				}
				
				if( notifyState == E_NotifyState__NOT_ENABLED )
				{
					final BleNotify notify = new BleNotify(m_serviceUuid, m_charUuid).
							setDescriptorFilter(m_descriptorFilter).
							setReadWriteListener(ithEntry.m_pollingReadListener);
					notify.setData(P_Const.EMPTY_FUTURE_DATA);
					ReadWriteListener.ReadWriteEvent earlyOutResult = m_device.getServiceManager().getEarlyOutEvent(notify, ReadWriteListener.Type.ENABLING_NOTIFICATION, Target.CHARACTERISTIC);

					if( earlyOutResult != null )
					{
						ithEntry.m_pollingReadListener.onEvent(earlyOutResult);
					}
					else
					{
						if (m_device.conf_device().autoEnableNotifiesOnReconnect)
						{
							m_device.getBondManager().bondIfNeeded(characteristic.getUuid(), CharacteristicEventType.ENABLE_NOTIFY);

							m_device.getIManager().getTaskManager().add(new P_Task_ToggleNotify(m_device, notify, /*enable=*/true, null, m_device.getOverrideReadWritePriority()));

							notifyState = E_NotifyState__ENABLING;
						}
					}
				}
				
				if( notifyState == E_NotifyState__ENABLED && ithEntry.m_notifyState != E_NotifyState__ENABLED )
				{
					ReadWriteEvent result = newAlreadyEnabledEvent(characteristic, m_serviceUuid, m_charUuid, m_descriptorFilter);
					ithEntry.m_pollingReadListener.onEvent(result);
				}
				
				ithEntry.m_notifyState = notifyState;
			}
		}
	}

	final ReadWriteEvent newAlreadyEnabledEvent(BleCharacteristic characteristic, final UUID serviceUuid, final UUID characteristicUuid, final DescriptorFilter descriptorFilter)
	{
		//--- DRK > Just being anal with the null check here.
		byte[] writeValue = characteristic != null ? P_Task_ToggleNotify.getWriteValue(characteristic, /*enable=*/true) : P_Const.EMPTY_BYTE_ARRAY;
		int gattStatus = BluetoothGatt.GATT_SUCCESS;
		final BleNotify notify = new BleNotify(serviceUuid, characteristicUuid).setDescriptorFilter(descriptorFilter);
		notify.setData(new PresentData(writeValue));
		ReadWriteEvent result = P_Bridge_User.newReadWriteEvent(m_device.getBleDevice(), notify, Type.ENABLING_NOTIFICATION, Target.DESCRIPTOR, Status.SUCCESS, gattStatus, 0.0, 0.0, /*solicited=*/true);
		return result;
	}





	private static class PollingReadListener extends P_WrappingReadWriteListener
	{
		CallbackEntry m_entry;
		private ReadWriteListener m_overrideListener;

		PollingReadListener(ReadWriteListener readWriteListener, P_SweetHandler handler, boolean postToMain)
		{
			super(null, handler, postToMain);

			addListener(readWriteListener);
		}

		private boolean hasListener(ReadWriteListener listener)
		{
			return listener == m_overrideListener;
		}

		private void addListener(ReadWriteListener listener)
		{
			m_overrideListener = listener;
		}

		private void init(CallbackEntry entry)
		{
			m_entry = entry;
		}

		@Override
		public void onEvent(ReadWriteEvent result)
		{
			m_entry.onSuccessOrFailure();

			super.onEvent(m_overrideListener, result);
		}
	}

	private static class TrackingWrappingReadListener extends PollingReadListener
	{
		private byte[] m_lastValue = null;


		TrackingWrappingReadListener(ReadWriteListener readWriteListener, P_SweetHandler handler, boolean postToMain)
		{
			super(readWriteListener, handler, postToMain);
		}

		@Override
		public final void onEvent(ReadWriteEvent event)
		{
			if( event.status() == Status.SUCCESS )
			{
				if (event.type() == Type.PSUEDO_NOTIFICATION)
				{
					NotificationListener.NotificationEvent e = NotificationListener.NotificationEvent.fromReadWriteEvent(event.device(), event);

					P_Bridge_User.getIBleDevice(event.device()).invokeNotificationCallback(null, e);

					super.onEvent(event);
				}
				else if (m_lastValue == null || !Arrays.equals(m_lastValue, event.data()))
				{
					super.onEvent(event);
				}
				else
				{
					m_entry.onSuccessOrFailure();
				}

				m_lastValue = event.data();
			}
			else
			{
				m_lastValue = null;

				super.onEvent(event);
			}
		}
	}

	private static class CallbackEntry
	{
		private final IBleDevice m_device;
		private final PollingReadListener m_pollingReadListener;
		private double m_interval;
		private BleOp m_bleOp;
		private final boolean m_usingNotify;
		private int/*_E_NotifyState*/ m_notifyState;

		private double m_timeTracker;
		private boolean m_waitingForResponse;

		CallbackEntry(IBleDevice device, BleOp bleOp, double interval, boolean trackChanges, boolean usingNotify)
		{
			m_bleOp = bleOp;
			m_interval = interval;
			m_device = device;
			m_usingNotify = usingNotify;
			m_notifyState = E_NotifyState__NOT_ENABLED;

			m_timeTracker = interval; // to get it to do a first read pretty much instantly.

			if( trackChanges || m_usingNotify)
			{
				m_pollingReadListener = new TrackingWrappingReadListener(m_bleOp.getReadWriteListener(), m_device.getIManager().getPostManager().getUIHandler(), m_device.getIManager().getConfigClone().postCallbacksToMainThread);
			}
			else
			{
				m_pollingReadListener = new PollingReadListener(m_bleOp.getReadWriteListener(), m_device.getIManager().getPostManager().getUIHandler(), m_device.getIManager().getConfigClone().postCallbacksToMainThread);
			}

			m_pollingReadListener.init(this);
		}

		final boolean trackingChanges()
		{
			return m_pollingReadListener instanceof TrackingWrappingReadListener;
		}

		final boolean usingNotify()
		{
			return m_usingNotify;
		}

		final boolean isFor(BleOp bleOp, Double interval_nullable, boolean usingNotify)
		{
			return
					usingNotify == m_usingNotify																											&&
							(m_bleOp.getServiceUuid() == null || bleOp.getServiceUuid() == null || m_bleOp.getServiceUuid().equals(bleOp.getServiceUuid()))	&&
							descriptorMatches(m_bleOp.getDescriptorFilter(), bleOp.getDescriptorFilter())													&&
							bleOp.getCharacteristicUuid().equals(m_bleOp.getCharacteristicUuid())															&&
							(Interval.isDisabled(interval_nullable) || interval_nullable == m_interval)														&&
							(bleOp.getReadWriteListener() == null || m_pollingReadListener.hasListener(bleOp.getReadWriteListener()))	 					;
		}

		final boolean descriptorMatches(DescriptorFilter curfilter, DescriptorFilter newFilter)
		{
			if (curfilter == null)
			{
				if (newFilter == null)
				{
					return true;
				}
				return false;
			}
			else
			{
				if (newFilter == null)
				{
					return false;
				}
				if (curfilter.equals(newFilter))
				{
					return true;
				}
			}
			return false;
		}

		final boolean isFor(final UUID serviceUuid, final UUID charUuid)
		{
			final UUID curServiceUuid = m_bleOp.getServiceUuid();
			final UUID curCharUuid = m_bleOp.getCharacteristicUuid();
			if( serviceUuid == null || curServiceUuid == null )
			{
				return charUuid.equals(curCharUuid);
			}
			else
			{
				return charUuid.equals(curCharUuid) && curServiceUuid != null && curServiceUuid.equals(serviceUuid);
			}
		}

		final void onCharacteristicChangedFromNativeNotify(byte[] value)
		{
			//--- DRK > The early-outs in this method are for when, for example, a native onNotify comes in on a random thread,
			//---		BleDevice#disconnect() is called on main thread before notify gets passed to main thread (to here).
			//---		Explicit disconnect clears all service/characteristic state and notify shouldn't get sent to app-land
			//---		regardless.
			if( m_device.is(BleDeviceState.BLE_DISCONNECTED) )  return;

			final UUID m_serviceUuid = m_bleOp.getServiceUuid();
			final UUID m_charUuid = m_bleOp.getCharacteristicUuid();
			final DescriptorFilter m_descriptorFilter = m_bleOp.getDescriptorFilter();

			BleCharacteristic characteristic = m_device.getNativeBleCharacteristic(m_serviceUuid, m_charUuid);

			if( characteristic.isNull() )  return;

			NotificationListener.Type type = P_DeviceServiceManager.getProperNotificationType(characteristic, NotificationListener.Type.NOTIFICATION);
			int gattStatus = BleStatuses.GATT_STATUS_NOT_APPLICABLE;
			final BleNotify notify = new BleNotify(m_serviceUuid, m_charUuid).setDescriptorFilter(m_descriptorFilter);
			notify.setData(new PresentData(value));

			NotificationListener.Status status;

			if (value == null)
				status = NotificationListener.Status.NULL_DATA;
			else if (value.length == 0)
				status = NotificationListener.Status.EMPTY_DATA;
			else
				status = NotificationListener.Status.SUCCESS;

			NotificationListener.NotificationEvent result = P_Bridge_User.newNotificationEvent(m_device.getBleDevice(), notify, type, status, gattStatus, 0.0, 0.0, true);
			m_device.invokeNotificationCallback(null, result);

			m_timeTracker = 0.0;
		}

		final void onSuccessOrFailure()
		{
			m_waitingForResponse = false;
			m_timeTracker = 0.0;
		}

		final void update(double timeStep)
		{
			if( m_interval <= 0.0 )  return;
			if( m_interval == Interval.INFINITE.secs() )  return;

			m_timeTracker += timeStep;

			if( m_timeTracker >= m_interval )
			{
				m_timeTracker = 0.0;

				if( m_device.is(BleDeviceState.INITIALIZED) && !m_device.is(BleDeviceState.RECONNECTING_SHORT_TERM) )
				{
					if( !m_waitingForResponse )
					{
						m_waitingForResponse = true;
						final Type type = trackingChanges() ? Type.PSUEDO_NOTIFICATION : Type.POLL;
						final BleRead read = new BleRead(m_bleOp.getServiceUuid(), m_bleOp.getCharacteristicUuid()).setReadWriteListener(m_pollingReadListener);
						m_device.read_internal(type, read);
					}
				}
			}
		}
	}
}
