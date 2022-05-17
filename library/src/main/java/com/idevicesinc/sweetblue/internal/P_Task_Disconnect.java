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


final class P_Task_Disconnect extends PA_Task_RequiresBleOn
{
	private final PE_TaskPriority m_priority;
	private final boolean m_explicit;
	private int m_gattStatus = BleStatuses.GATT_STATUS_NOT_APPLICABLE;
	private final boolean m_cancellableByConnect;
	private Integer m_overrideOrdinal = null;

	private final boolean m_saveLastDisconnect;
	
	public P_Task_Disconnect(IBleDevice device, I_StateListener listener, boolean explicit, PE_TaskPriority priority, final boolean cancellableByConnect)
	{
		this(device, listener, explicit, priority, cancellableByConnect, false);
	}

	public P_Task_Disconnect(IBleDevice device, I_StateListener listener, boolean explicit, PE_TaskPriority priority, final boolean cancellableByConnect, final boolean saveLastDisconnect)
	{
		super(device, listener);

		m_saveLastDisconnect = saveLastDisconnect;
		m_priority = priority == null ? PE_TaskPriority.FOR_EXPLICIT_BONDING_AND_CONNECTING : priority;
		m_explicit = explicit;

		m_cancellableByConnect = cancellableByConnect;
	}
	
	@Override int getOrdinal()
	{
		if( m_overrideOrdinal != null )
		{
			return m_overrideOrdinal;
		}
		else
		{
			return super.getOrdinal();
		}
	}
	
	public void setOverrideOrdinal(int ordinal)
	{
		m_overrideOrdinal = ordinal;
	}
	
	@Override public boolean isExplicit()
	{
		return m_explicit;
	}

	public boolean shouldSaveLastDisconnect()
	{
		return m_saveLastDisconnect;
	}
	
	@Override public void execute()
	{
		if( !getDevice().getNativeManager().isNativelyConnectingOrConnected() )
		{
			getLogger().w("Already disconnected!");
			
			redundant();
			
			return;
		}
		
		if( getDevice().nativeManager().isGattNull() )
		{
			getLogger().w("Already disconnected and gatt==null!");
			
			redundant();
			
			return;
		}
		
		if( getDevice().getNativeManager()./*already*/isNativelyDisconnecting() )
		{
			// nothing to do
			
			return;
		}
		
//		if( m_explicit )
//		{
			getDevice().nativeManager().disconnect();
//		}
//		else
//		{
//			// DRK > nothing to do...wait for implicit disconnect task to complete...note we're probably
//			// never going to get here cause I've never observed STATE_DISCONNECTING.
//		}
	}
	
	public int getGattStatus()
	{
		return m_gattStatus;
	}
	
	public void onNativeSuccess(int gattStatus)
	{
		m_gattStatus = gattStatus;
		
		succeed();
	}
	
	@Override public PE_TaskPriority getPriority()
	{
		return m_priority;
	}
	
	boolean isCancellable()
	{
		return this.m_explicit && this.m_cancellableByConnect;
	}
	
	@Override protected boolean isSoftlyCancellableBy(PA_Task task)
	{
		if( task.getClass() == P_Task_Connect.class && this.getDevice().equals(task.getDevice()) )
		{
			if( isCancellable() )
			{
				return true;
			}
		}
		
		return super.isSoftlyCancellableBy(task);
	}
	
	@Override protected BleTask getTaskType()
	{
		return BleTask.DISCONNECT;
	}
}
