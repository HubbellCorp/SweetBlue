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


final class P_Task_Delayer extends PA_Task
{

    private final Interval m_delayTime;
    private double m_timeDelayed = -1.0;


    public P_Task_Delayer(IBleManager manager, I_StateListener listener, Interval delayTime)
    {
        super(manager, listener);
        m_delayTime = delayTime;
    }

    @Override protected BleTask getTaskType()
    {
        return BleTask.DELAY;
    }

    @Override void execute()
    {
        m_timeDelayed = 0.0;
    }

    @Override protected void update(double timeStep)
    {
        super.update(timeStep);
        if (m_timeDelayed >= 0.0)
        {
            m_timeDelayed += timeStep;
            if (m_timeDelayed >= m_delayTime.secs())
            {
                succeed();
            }
        }
    }

    @Override public PE_TaskPriority getPriority()
    {
        return PE_TaskPriority.CRITICAL;
    }
}
