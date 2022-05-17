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
import com.idevicesinc.sweetblue.BleStatuses;
import com.idevicesinc.sweetblue.BleTask;
import com.idevicesinc.sweetblue.BleWrite;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent;
import com.idevicesinc.sweetblue.ReadWriteListener.Status;
import com.idevicesinc.sweetblue.ReadWriteListener.Target;
import com.idevicesinc.sweetblue.ReadWriteListener.Type;
import com.idevicesinc.sweetblue.internal.android.P_GattHolder;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.UhOhListener.UhOh;


final class P_Task_Write extends PA_Task_ReadOrWrite
{

	public P_Task_Write(IBleDevice device, BleWrite write, boolean requiresBonding, IBleTransaction txn, PE_TaskPriority priority)
	{
		super(device, write, requiresBonding, txn, priority);

	}

	@Override
	protected ReadWriteEvent newReadWriteEvent(final Status status, final int gattStatus, final Target target, BleOp bleOp)
	{
		final BleCharacteristic char_native = getDevice().getNativeBleCharacteristic(bleOp.getServiceUuid(), bleOp.getCharacteristicUuid());
		final Type type = P_DeviceServiceManager.modifyResultType(char_native, Type.WRITE);
		final BleWrite write = new BleWrite(bleOp.getServiceUuid(), bleOp.getCharacteristicUuid()).setDescriptorFilter(bleOp.getDescriptorFilter()).setBytes(bleOp.getData().getData());

		return P_Bridge_User.newReadWriteEvent(getDevice().getBleDevice(), write, type, target, status, gattStatus, getTotalTime(), getTotalTimeExecuting(), /*solicited=*/true);
	}

	private BleWrite get()
	{
		return (BleWrite) m_bleOp;
	}

	@Override protected void executeReadOrWrite()
	{
		if( false == write_earlyOut(m_bleOp.getData().getData()) )
		{
			final BleCharacteristic char_native = getFilteredCharacteristic() != null ? getFilteredCharacteristic() : getDevice().getNativeBleCharacteristic(getServiceUuid(), getCharUuid(), m_bleOp.getDescriptorFilter());

			if( char_native == null )
			{
				fail(Status.NO_MATCHING_TARGET, BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), getCharUuid(), ReadWriteEvent.NON_APPLICABLE_UUID);
			}
			else
			{
				if (!m_bleOp.isServiceUuidValid())
					m_bleOp.setServiceUUID(char_native.getService().getUuid());

				// Set the write type now, if it is not null
				P_Bridge_User.setCharWriteType(char_native, get().getWriteType());

				if( false == getDevice().nativeManager().setCharValue(char_native, m_bleOp.getData().getData()) )
				{
					fail(Status.FAILED_TO_SET_VALUE_ON_TARGET, BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), getCharUuid(), ReadWriteEvent.NON_APPLICABLE_UUID);
				}
				else
				{
					if( false == getDevice().nativeManager().writeCharacteristic(char_native) )
					{
						fail(Status.FAILED_TO_SEND_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), getCharUuid(), ReadWriteEvent.NON_APPLICABLE_UUID);
					}
					else
					{
						// SUCCESS, for now...
					}
				}
			}
		}
	}

	public void onCharacteristicWrite(final P_GattHolder gatt, final UUID uuid, final int gattStatus)
	{
		 getManager().ASSERT(getDevice().nativeManager().gattEquals(gatt), "");
		 
		 if( false == this.isForCharacteristic(uuid) )  return;
		 
		 if( false == acknowledgeCallback(gattStatus) )  return;

		 if( Utils.isSuccess(gattStatus) )
		 {
			 succeedWrite();
		 }
		 else
		 {
			 fail(Status.REMOTE_GATT_FAILURE, gattStatus, getDefaultTarget(), uuid, ReadWriteEvent.NON_APPLICABLE_UUID);
		 }
	}

	@Override public void onStateChange(final PA_Task task, final PE_TaskState state)
	{
		super.onStateChange(task, state);
		
		if( state == PE_TaskState.TIMED_OUT )
		{
			getLogger().w(getLogger().charName(getCharUuid()) + " write timed out!");
			
			getDevice().invokeReadWriteCallback(m_bleOp.getReadWriteListener(), newReadWriteEvent(Status.TIMED_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), m_bleOp));
			
			getManager().uhOh(UhOh.WRITE_TIMED_OUT);
		}
		else if( state == PE_TaskState.SOFTLY_CANCELLED )
		{
			getDevice().invokeReadWriteCallback(m_bleOp.getReadWriteListener(), newReadWriteEvent(getCancelType(), BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), m_bleOp));
		}
	}
	
	@Override protected BleTask getTaskType()
	{
		return BleTask.WRITE;
	}
}
