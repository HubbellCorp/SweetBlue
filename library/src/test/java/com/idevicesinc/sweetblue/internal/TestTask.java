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


import android.util.Log;

import com.idevicesinc.sweetblue.BleTask;



public class TestTask extends PA_Task
{
    private Object mMetadata = null;
    private PE_TaskPriority mPriority = PE_TaskPriority.LOW;

    public interface Listener
    {
        void onExecute(TestTask task);
    }

    public Object getMetadata()
    {
        return mMetadata;
    }

    public TestTask(IBleServer server, Object metadata)
    {
        super(server, null);
        mMetadata = metadata;
    }

    public TestTask(IBleDevice device, Object metadata)
    {
        super(device, null);
        mMetadata = metadata;
    }

    public TestTask(IBleManager manager, Object metadata, final Listener listener)
    {
        super(manager, (task, state) -> {
            Log.d("TAG", "Task transitioned to " + state);
            if (listener != null && task instanceof TestTask && state == PE_TaskState.EXECUTING)
            {
                listener.onExecute((TestTask) task);
            }
        });
        mMetadata = metadata;
    }

    @Override
    protected BleTask getTaskType()
    {
        return BleTask.READ;
    }

    public void setPriority(PE_TaskPriority priority)
    {
        mPriority = priority;
    }

    @Override
    void execute()
    {
        succeed();
    }

    @Override
    public PE_TaskPriority getPriority()
    {
        return mPriority;
    }
}
