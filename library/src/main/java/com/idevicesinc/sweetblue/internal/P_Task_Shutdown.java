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


import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleTask;
import java.util.List;


public class P_Task_Shutdown extends PA_Task
{


    public P_Task_Shutdown(IBleManager manager, I_StateListener listener)
    {
        super(manager, listener);
    }

    @Override
    protected BleTask getTaskType()
    {
        return BleTask.SHUTDOWN;
    }

    @Override
    void execute()
    {
        doConnectedDeviceCheck();
    }

    private void doConnectedDeviceCheck()
    {
        // Ensure all devices have been disconnected
        List<IBleDevice> connectedDevices = getManager().getDevices_List(BleDeviceState.BLE_CONNECTED);
        // If the list isn't empty, then some devices didn't get disconnected. As this is when shutting down,
        // we'll just log it and allow things to continue, so we don't lock apps up when trying to close
        if (!connectedDevices.isEmpty())
        {
            getLogger().e(String.format("%d devices were not disconnected when shutting down!", connectedDevices.size()));
            // Clear the semaphore to ensure we dont lock up the app
            getManager().clearShutdownSemaphore();
            fail();
        }
        else
        {
            // Release the semaphore held by the shutdown() method
            getManager().clearShutdownSemaphore();
        }
    }

    @Override
    public PE_TaskPriority getPriority()
    {
        // RB - Trying a priority of LOW here. This will be higher than any scan task, but lower than any disconnect task. Ideally, this task should
        // only run when all disconnect tasks have completed.
        return PE_TaskPriority.LOW;
    }
}
