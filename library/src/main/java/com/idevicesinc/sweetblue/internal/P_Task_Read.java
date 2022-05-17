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

import java.util.UUID;

import android.bluetooth.BluetoothGattCharacteristic;

import com.idevicesinc.sweetblue.BleCharacteristic;
import com.idevicesinc.sweetblue.BleOp;
import com.idevicesinc.sweetblue.BleRead;
import com.idevicesinc.sweetblue.BleStatuses;
import com.idevicesinc.sweetblue.BleTask;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent;
import com.idevicesinc.sweetblue.ReadWriteListener.Status;
import com.idevicesinc.sweetblue.ReadWriteListener.Target;
import com.idevicesinc.sweetblue.ReadWriteListener.Type;
import com.idevicesinc.sweetblue.UhOhListener.UhOh;
import com.idevicesinc.sweetblue.internal.android.P_GattHolder;


final class P_Task_Read extends PA_Task_ReadOrWrite
{
	private final Type m_type;
	

	public P_Task_Read(IBleDevice device, BleRead read, Type type, boolean requiresBonding, IBleTransaction txn, PE_TaskPriority priority)
	{
		super(device, read, requiresBonding, txn, priority);
		m_type = type;
	}
	
	@Override protected ReadWriteEvent newReadWriteEvent(Status status, int gattStatus, Target target, BleOp bleOp)
	{
		BleRead read = new BleRead(bleOp.getServiceUuid(), bleOp.getCharacteristicUuid()).setDescriptorFilter(bleOp.getDescriptorFilter());
		return P_Bridge_User.newReadWriteEvent(getDevice().getBleDevice(), read, m_type, target, status, gattStatus, getTotalTime(), getTotalTimeExecuting(), /*solicited=*/true);
	}

	@Override protected void executeReadOrWrite()
	{
		final BleCharacteristic filteredChar = getFilteredCharacteristic();
		final BleCharacteristic char_native = filteredChar != null ? filteredChar : getDevice().getNativeBleCharacteristic(getServiceUuid(), getCharUuid(), m_bleOp.getDescriptorFilter());

		if( char_native == null || char_native.isNull() || char_native.hasUhOh())
		{
			Status status;
			if (char_native != null && char_native.hasUhOh())
				status = char_native.getUhOh() == UhOh.CONCURRENT_EXCEPTION ? Status.GATT_CONCURRENT_EXCEPTION : Status.GATT_RANDOM_EXCEPTION;
			else
				status = Status.NO_MATCHING_TARGET;
			fail(status, BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), getCharUuid(), ReadWriteEvent.NON_APPLICABLE_UUID);
		}
		else
		{
			if (!m_bleOp.isServiceUuidValid())
				m_bleOp.setServiceUUID(char_native.getService().getUuid());

			if( false == getDevice().nativeManager().readCharacteristic(char_native) )
			{
				fail(Status.FAILED_TO_SEND_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), getCharUuid(), ReadWriteEvent.NON_APPLICABLE_UUID);
			}
			else
			{
				// DRK > SUCCESS, for now...
			}
		}
	}

	public void onCharacteristicRead(P_GattHolder gatt, UUID uuid, byte[] value, int gattStatus)
	{
		getManager().ASSERT(getDevice().nativeManager().gattEquals(gatt), "");
		 
		if( false == this.isForCharacteristic(uuid) )  return;

		onCharacteristicOrDescriptorRead(gatt, uuid, value, gattStatus, m_type);
	}
	
	@Override public void onStateChange(PA_Task task, PE_TaskState state)
	{
		super.onStateChange(task, state);
		
		if( state == PE_TaskState.TIMED_OUT )
		{
			getLogger().w(getLogger().charName(getCharUuid()) + " read timed out!");

			final ReadWriteEvent event = newReadWriteEvent(Status.TIMED_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), m_bleOp);
			
			getDevice().invokeReadWriteCallback(m_bleOp.getReadWriteListener(), event);
			
			getManager().uhOh(UhOh.READ_TIMED_OUT);
		}
		else if( state == PE_TaskState.SOFTLY_CANCELLED )
		{
			final ReadWriteEvent event = newReadWriteEvent(getCancelType(), BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), m_bleOp);
			getDevice().invokeReadWriteCallback(m_bleOp.getReadWriteListener(), event);
		}
	}
	
	@Override protected BleTask getTaskType()
	{
		return BleTask.READ;
	}
}
