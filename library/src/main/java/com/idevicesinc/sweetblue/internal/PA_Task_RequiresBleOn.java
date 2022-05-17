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



abstract class PA_Task_RequiresBleOn extends PA_Task
{
	public PA_Task_RequiresBleOn(IBleServer server, I_StateListener listener)
	{
		super(server, listener);
	}
	
	public PA_Task_RequiresBleOn(IBleManager manager, I_StateListener listener)
	{
		super(manager, listener);
	}
	
	public PA_Task_RequiresBleOn(IBleDevice device, I_StateListener listener)
	{
		super(device, listener);
	}

	@Override protected boolean isExecutable()
	{
		return super.isExecutable() && getManager().managerLayer().isBluetoothEnabled();
	}
	
	@Override public boolean isCancellableBy(PA_Task task)
	{
		if( task instanceof P_Task_TurnBleOff )
		{
			return true;
		}
		else if( task instanceof P_Task_CrashResolver )
		{
			final P_Task_CrashResolver task_cast = (P_Task_CrashResolver) task;

			if( task_cast.isForReset() )
			{
				return true;
			}
			else
			{
				return super.isCancellableBy(task);
			}
		}
		else
		{
			return super.isCancellableBy(task);
		}
	}
}
