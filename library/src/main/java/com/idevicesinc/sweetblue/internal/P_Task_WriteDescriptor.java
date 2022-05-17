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
import com.idevicesinc.sweetblue.BleDescriptorWrite;
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
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.Uuids;
import java.util.UUID;


final class P_Task_WriteDescriptor extends PA_Task_ReadOrWrite
{

	public P_Task_WriteDescriptor(IBleDevice device, BleDescriptorWrite write, boolean requiresBonding, IBleTransaction txn, PE_TaskPriority priority)
	{
		super(device, write, requiresBonding, txn, priority);
	}

	@Override
	protected ReadWriteEvent newReadWriteEvent(Status status, int gattStatus, Target target, BleOp bleOp)
	{
		UUID descUuid = (bleOp instanceof BleDescriptorWrite) ? ((BleDescriptorWrite) bleOp).getDescriptorUuid() : Uuids.INVALID;
		final BleOp op = P_Bridge_User.createBleOp(bleOp.getServiceUuid(), bleOp.getCharacteristicUuid(), descUuid, bleOp.getDescriptorFilter(), bleOp.getData().getData(), Type.WRITE);
		return P_Bridge_User.newReadWriteEvent(getDevice().getBleDevice(), op, Type.WRITE, target, status, gattStatus, getTotalTime(), getTotalTimeExecuting(), /*solicited=*/true);
	}

	@Override
	protected UUID getDescUuid()
	{
		return ((BleDescriptorWrite) m_bleOp).getDescriptorUuid();
	}

	@Override
	protected Target getDefaultTarget()
	{
		return Target.DESCRIPTOR;
	}

	private byte[] getData()
	{
		return m_bleOp.getData().getData();
	}

	@Override protected void executeReadOrWrite()
	{

		if( false == write_earlyOut(getData()) )
		{
			final BleDescriptor desc_native = getDevice().getNativeBleDescriptor(getServiceUuid(), getCharUuid(), getDescUuid());

			if( desc_native == null )
			{
				fail(Status.NO_MATCHING_TARGET, BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), getCharUuid(), getDescUuid());
			}
			else
			{
				if (!m_bleOp.isServiceUuidValid())
					m_bleOp.setServiceUUID(desc_native.getCharacteristic().getService().getUuid());
				if (!m_bleOp.isCharUuidValid())
					m_bleOp.setCharacteristicUUID(desc_native.getCharacteristic().getUuid());

				if( false == getDevice().nativeManager().setDescValue(desc_native, getData()) )
				{
					fail(Status.FAILED_TO_SET_VALUE_ON_TARGET, BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), getCharUuid(), getDescUuid());
				}
				else
				{
					if( false == getDevice().nativeManager().writeDescriptor(desc_native) )
					{
						fail(Status.FAILED_TO_SEND_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), getCharUuid(), getDescUuid());
					}
					else
					{
						// DRK > SUCCESS, for now...
					}
				}
			}
		}
	}
	
	public void onDescriptorWrite(P_GattHolder gatt, UUID uuid, int gattStatus)
	{
		getManager().ASSERT(getDevice().nativeManager().gattEquals(gatt), "");

//		if( !this.isForCharacteristic(uuid) )  return;

		if( false == acknowledgeCallback(gattStatus) )  return;

		if( Utils.isSuccess(gattStatus) )
		{
			succeedWrite();
		}
		else
		{
			fail(Status.REMOTE_GATT_FAILURE, gattStatus, getDefaultTarget(), getCharUuid(), getDescUuid());
		}
	}
	
	@Override public void onStateChange(PA_Task task, PE_TaskState state)
	{
		super.onStateChange(task, state);
		
		if( state == PE_TaskState.TIMED_OUT )
		{
			getLogger().w(getLogger().descriptorName(getDescUuid()) + " read timed out!");

			final ReadWriteEvent event = newReadWriteEvent(Status.TIMED_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), m_bleOp);
			
			getDevice().invokeReadWriteCallback(m_bleOp.getReadWriteListener(), event);
			
			getManager().uhOh(UhOh.WRITE_TIMED_OUT);
		}
		else if( state == PE_TaskState.SOFTLY_CANCELLED )
		{
			final ReadWriteEvent event = newReadWriteEvent(getCancelType(), BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), m_bleOp);
			getDevice().invokeReadWriteCallback(m_bleOp.getReadWriteListener(), event);
		}
	}
	
	@Override protected BleTask getTaskType()
	{
		return BleTask.WRITE_DESCRIPTOR;
	}
}
