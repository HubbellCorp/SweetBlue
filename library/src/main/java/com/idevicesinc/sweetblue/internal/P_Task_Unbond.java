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

import android.annotation.SuppressLint;
import com.idevicesinc.sweetblue.BleTask;


final class P_Task_Unbond extends PA_Task_RequiresBleOn
{

	private final PE_TaskPriority m_priority;
	private boolean m_waitingForNativeSideToCatchUp = false;

	
	public P_Task_Unbond(IBleDevice device, I_StateListener listener, PE_TaskPriority priority)
	{
		super(device, listener);
		
		m_priority = priority == null ? PE_TaskPriority.FOR_EXPLICIT_BONDING_AND_CONNECTING : priority;
	}
	
	public P_Task_Unbond(IBleDevice device, I_StateListener listener)
	{
		this(device, listener, null);
	}

	
	@SuppressLint("NewApi")
	@Override public void execute()
	{
		if( getDevice().getNativeManager().isNativelyUnbonded() )
		{
			//--- DRK > Commenting this out cause it's a little louder than need be...redundant ending state gets the point across.
//			m_logger.w("Already not bonded!");
			
			redundant();
		}
		else
		{
			if( getDevice().getNativeManager().isNativelyBonding() )
			{
				if( false == cancelBondProcess() )
				{
					failImmediately();
				}
				else
				{
					// SUCCESS, so far...
				}
			}
			else if( getDevice().getNativeManager().isNativelyBonded() )
			{
				if( false == removeBond() )
				{
					failImmediately();
				}
				else
				{
					// SUCCESS, so far...
				}
			}
			else
			{
				getManager().ASSERT(false, "Expected to be bonding or bonded only.");

				failImmediately();
			}
		}
	}
	
	private boolean removeBond()
	{
		return getDevice().nativeManager().getDeviceLayer().removeBond();
	}
	
	private boolean cancelBondProcess()
	{
		return getDevice().nativeManager().getDeviceLayer().cancelBond();
	}

	void waitForNativeToCatchUp()
 	{
		m_waitingForNativeSideToCatchUp = true;
	}

	@Override
 	protected void update(double timeStep)
 	{
		if (m_waitingForNativeSideToCatchUp)
		{
			if (getDevice().getNativeManager().isNativelyUnbonded())
			{
				succeed();
			}
		}
	}

	@Override public boolean isExplicit()
	{
		return true; //TODO
	}
	
	@Override public PE_TaskPriority getPriority()
	{
		return m_priority;
	}
	
	@Override protected BleTask getTaskType()
	{
		return BleTask.UNBOND;
	}
}
