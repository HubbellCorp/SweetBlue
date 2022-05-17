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
import com.idevicesinc.sweetblue.utils.Utils_Config;


final class P_Task_BondPopupHack extends PA_Task_RequiresBleOn
{

    private double scanTime = 0;


    public P_Task_BondPopupHack(IBleDevice device, I_StateListener listener)
    {
        super(device, listener);
    }

    @Override protected BleTask getTaskType()
    {
        return BleTask.DISCOVER_SERVICES;
    }

    @Override void execute()
    {
        getDevice().nativeManager().startDiscovery();
    }

    @Override protected void update(double timeStep)
    {
        super.update(timeStep);
        scanTime += timeStep;
        Interval maxTime = Utils_Config.interval(getDevice().conf_device().forceBondHackInterval, getDevice().conf_mngr().forceBondHackInterval);
        if (Interval.isDisabled(maxTime))
        {
            // In case the interval comes back null, or disabled, set it to one second. It could be argued that disabled should mean 0 delay, but if that
            // were the case, this hack wouldn't work.
            maxTime = Interval.ONE_SEC;
        }
        if (scanTime >= maxTime.secs())
        {
            getDevice().nativeManager().cancelDiscovery();
            succeed();
        }
    }

    @Override protected void failWithoutRetry()
    {
        super.failWithoutRetry();
        getManager().getTaskManager().fail(P_Task_Bond.class, getDevice());
    }

    @Override public PE_TaskPriority getPriority()
    {
        return PE_TaskPriority.MEDIUM;
    }
}
