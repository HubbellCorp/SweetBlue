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


import com.idevicesinc.sweetblue.BleDescriptor;
import com.idevicesinc.sweetblue.BleDescriptorRead;
import com.idevicesinc.sweetblue.BleOp;
import com.idevicesinc.sweetblue.BleStatuses;
import com.idevicesinc.sweetblue.BleTask;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent;
import com.idevicesinc.sweetblue.ReadWriteListener.Status;
import com.idevicesinc.sweetblue.ReadWriteListener.Target;
import com.idevicesinc.sweetblue.ReadWriteListener.Type;
import com.idevicesinc.sweetblue.UhOhListener.UhOh;
import com.idevicesinc.sweetblue.internal.android.P_GattHolder;
import java.util.UUID;


final class P_Task_ReadDescriptor extends PA_Task_ReadOrWrite
{

	private final Type m_type;


	public P_Task_ReadDescriptor(IBleDevice device, BleDescriptorRead read, Type type, boolean requiresBonding, IBleTransaction txn, PE_TaskPriority priority)
	{
		super(device, read, requiresBonding, txn, priority);

		m_type = type;
	}
	
	@Override protected ReadWriteEvent newReadWriteEvent(Status status, int gattStatus, Target target, BleOp bleOp)
	{
		final BleDescriptorRead read = new BleDescriptorRead(bleOp.getServiceUuid(), bleOp.getCharacteristicUuid());
		if (bleOp instanceof BleDescriptorRead)
		{
			read.setDescriptorUUID(((BleDescriptorRead) bleOp).getDescriptorUuid());
		}
		read.setDescriptorFilter(bleOp.getDescriptorFilter());
		return P_Bridge_User.newReadWriteEvent(getDevice().getBleDevice(), read, m_type, target, status, gattStatus, getTotalTime(), getTotalTimeExecuting(), /*solicited=*/true);
	}

	@Override protected UUID getDescUuid()
	{
		return ((BleDescriptorRead) m_bleOp).getDescriptorUuid();
	}

	@Override protected Target getDefaultTarget()
	{
		return Target.DESCRIPTOR;
	}

	@Override protected void executeReadOrWrite()
	{
		final BleDescriptor desc_native = getDevice().getNativeBleDescriptor(getServiceUuid(), getCharUuid(), getDescUuid());

		if( desc_native == null )
		{
			fail(Status.NO_MATCHING_TARGET, BleStatuses.GATT_STATUS_NOT_APPLICABLE, Target.DESCRIPTOR, getCharUuid(), getDescUuid());
		}
		else
		{
			if (!m_bleOp.isServiceUuidValid())
				m_bleOp.setServiceUUID(desc_native.getCharacteristic().getService().getUuid());
			if (!m_bleOp.isCharUuidValid())
				m_bleOp.setCharacteristicUUID(desc_native.getCharacteristic().getUuid());

			if( false == getDevice().nativeManager().readDescriptor(desc_native) )
			{
				fail(Status.FAILED_TO_SEND_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, Target.DESCRIPTOR, getCharUuid(), getDescUuid());
			}
			else
			{
				// DRK > SUCCESS, for now...
			}
		}
	}

	@Override
	public void onDescriptorRead(P_GattHolder gatt, UUID uuid, byte[] value, int gattStatus)
	{
		getManager().ASSERT(getDevice().nativeManager().gattEquals(gatt), "");

		onCharacteristicOrDescriptorRead(gatt, uuid, value, gattStatus, m_type);
	}
	
	@Override public void onStateChange(PA_Task task, PE_TaskState state)
	{
		super.onStateChange(task, state);
		
		if( state == PE_TaskState.TIMED_OUT )
		{
			getLogger().w(getLogger().descriptorName(getDescUuid()) + " read timed out!");

			final ReadWriteEvent event = newReadWriteEvent(Status.TIMED_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, Target.DESCRIPTOR, m_bleOp);
			
			getDevice().invokeReadWriteCallback(m_bleOp.getReadWriteListener(), event);
			
			getManager().uhOh(UhOh.READ_TIMED_OUT);
		}
		else if( state == PE_TaskState.SOFTLY_CANCELLED )
		{
			final ReadWriteEvent event = newReadWriteEvent(getCancelType(), BleStatuses.GATT_STATUS_NOT_APPLICABLE, Target.DESCRIPTOR, m_bleOp);
			getDevice().invokeReadWriteCallback(m_bleOp.getReadWriteListener(), event);
		}
	}
	
	@Override protected BleTask getTaskType()
	{
		return BleTask.READ_DESCRIPTOR;
	}
}
