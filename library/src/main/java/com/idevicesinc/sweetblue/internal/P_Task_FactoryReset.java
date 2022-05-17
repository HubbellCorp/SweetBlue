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
import com.idevicesinc.sweetblue.utils.Utils_Reflection;


public class P_Task_FactoryReset extends PA_Task
{

    public P_Task_FactoryReset(IBleManager manager, I_StateListener listener)
    {
        super(manager, listener);
    }


    @Override
    protected BleTask getTaskType()
    {
        return BleTask.NUKE_BLE_STACK;
    }

    @Override
    void execute()
    {
        // This only gets called after ble is turned off, so we'll succeed right after calling it.
        Utils_Reflection.callBooleanReturnMethod(getManager().getNativeAdapter(), "factoryReset", false);
        succeed();
    }

    @Override
    public PE_TaskPriority getPriority()
    {
        return PE_TaskPriority.CRITICAL;
    }
}
