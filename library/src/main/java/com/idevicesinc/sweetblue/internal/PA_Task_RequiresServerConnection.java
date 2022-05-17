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

import android.bluetooth.BluetoothGattServer;

import com.idevicesinc.sweetblue.BleManagerState;
import com.idevicesinc.sweetblue.OutgoingListener;

abstract class PA_Task_RequiresServerConnection extends PA_Task_RequiresBleOn
{
	final String m_macAddress;

	public PA_Task_RequiresServerConnection(IBleServer server, final String macAddress)
	{
		super(server, null);

		m_macAddress = macAddress;
	}

	protected final OutgoingListener.Status getCancelStatusType()
	{
		IBleManager mngr = this.getManager();

		if( mngr.isAny(BleManagerState.TURNING_OFF, BleManagerState.OFF) )
		{
			return OutgoingListener.Status.CANCELLED_FROM_BLE_TURNING_OFF;
		}
		else
		{
			return OutgoingListener.Status.CANCELLED_FROM_DISCONNECT;
		}
	}
	
	@Override protected boolean isExecutable()
	{
		boolean shouldBeExecutable = super.isExecutable() && getServer().getNativeManager().getNativeState(m_macAddress) == BluetoothGattServer.STATE_CONNECTED;
		
		if( shouldBeExecutable )
		{
			return true;
		}
		
		return false;
	}
	
	@Override protected boolean isSoftlyCancellableBy(PA_Task task)
	{
		if( task.getClass() == P_Task_DisconnectServer.class && this.getServer().equals(task.getServer()) )
		{
			final P_Task_DisconnectServer task_cast = (P_Task_DisconnectServer) task;

			if( task_cast.m_nativeDevice != null && task_cast.m_nativeDevice.getAddress().equals(m_macAddress) )
			{
				if( task_cast.getOrdinal() > this.getOrdinal() )
				{
					return true;
				}
			}
		}
		
		return super.isSoftlyCancellableBy(task);
	}
	
	@Override protected void attemptToSoftlyCancel(PA_Task task)
	{
		super.attemptToSoftlyCancel(task);
		
		//--- DRK > The following logic became necessary due to the following situation:
		//---		* device connected successfully.
		//---		* getting service task started execution, sent out get services call.
		//---		* something related to the get services call (probably, gatt status code 142/0x8E) made us disconnect, resulting in connection fail callback
		//---		* we get no error callback for getting services, thus...
		//---		* getting services task was still executing until it timed out, prompting another connection fail callback even though we already failed from the root cause.
		//---		NOTE that this was only directly observed for discovering services, but who knows, maybe it can happen for reads/writes/etc. as well. Normally, I'm pretty sure,
		//---		reads/writes fail first then you get the disconnect callback.
		if( task.getClass() == P_Task_DisconnectServer.class && this.getServer().equals(task.getServer()) )
		{
			final P_Task_DisconnectServer task_cast = (P_Task_DisconnectServer) task;

			if( task_cast.m_nativeDevice != null && task_cast.m_nativeDevice.getAddress().equals(m_macAddress) )
			{
				if( !task_cast.isExplicit() )
				{
					//--- DRK > Not sure why the "not is connected" qualifier was there..
					//---		MAYBE something to do with onNativeDisconnect doing soft cancellation after state change.
					if( getState() == PE_TaskState.EXECUTING )//&& !getDevice().is(BleDeviceState.BLE_CONNECTED) )
					{
						softlyCancel();
					}
				}
			}
		}
	}
}
