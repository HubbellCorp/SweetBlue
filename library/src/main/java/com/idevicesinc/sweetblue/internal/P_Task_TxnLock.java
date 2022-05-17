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

import com.idevicesinc.sweetblue.BleTask;
import com.idevicesinc.sweetblue.utils.Interval;


final class P_Task_TxnLock extends PA_Task_RequiresBleOn
{
	private final IBleTransaction m_txn;
	
	public P_Task_TxnLock(IBleDevice device, IBleTransaction txn)
	{
		super(device, null);
		
		m_txn = txn;
	}
	
	@Override protected double getInitialTimeout()
	{
		return Interval.DISABLED.secs();
	}
	
	public IBleTransaction getTxn()
	{
		return m_txn;
	}
	
	@Override public void execute()
	{
		//--- DRK > Nothing to do here...basically just spins infinitely until read/write comes in or txn ends.
	}
	
	@Override public PE_TaskPriority getPriority()
	{
		return PE_TaskPriority.FOR_EXPLICIT_BONDING_AND_CONNECTING;
	}
	
	@Override public boolean isInterruptableBy(PA_Task task)
	{
		if( task instanceof PA_Task_Transactionable )
		{
			PA_Task_Transactionable task_cast = (PA_Task_Transactionable) task;
			
			if( this.getDevice() == task_cast.getDevice() && this.getTxn() == task_cast.getTxn() )
			{
				return true;
			}
		}
		else if( task instanceof P_Task_Bond )
		{
			P_Task_Bond task_cast = (P_Task_Bond) task;
			
			if( this.getDevice() == task_cast.getDevice() )
			{
				return true;
			}
		}
		
		return super.isInterruptableBy(task);
	}

	@Override protected BleTask getTaskType()
	{
		return null;
	}
}
