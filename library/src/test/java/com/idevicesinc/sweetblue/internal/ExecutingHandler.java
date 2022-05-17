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


public class ExecutingHandler
{
    private final BleTask m_taskType;
    private final IBleDevice m_device;
    private final ExecutingListener m_listener;


    private ExecutingHandler(BleTask task, IBleDevice device, ExecutingListener listener)
    {
        m_taskType = task;
        m_device = device;
        m_listener = listener;
    }


    void onEvent(PA_Task task, PE_TaskState state)
    {
        if (task.getTaskType() == m_taskType && state == PE_TaskState.EXECUTING && task.getDevice() != null && task.getDevice().equals(m_device))
            m_listener.onExecuting();
    }

    public static ExecutingHandler newHandler(IBleDevice device, BleTask taskType, ExecutingListener listener)
    {
        return new ExecutingHandler(taskType, device, listener);
    }
}
