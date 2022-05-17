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

package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.internal.P_Bridge_Internal;
import com.idevicesinc.sweetblue.internal.P_SweetHandler;


/**
 * Bridge class used by {@link com.idevicesinc.sweetblue.rx.schedulers.SweetBlueSchedulers} to setup the scheduler.
 */
public final class P_Bridge
{

    public static P_SweetHandler getUpdateHandler()
    {
        BleManager mgr = BleManager.s_instance;
        if (mgr == null)
            throw new NullPointerException("BleManager is null! You must first instantiate BleManager.");

        return P_Bridge_Internal.getUpdateHandler(mgr.getIBleManager());
    }


    private P_Bridge()
    {
        throw new AssertionError("No instances.");
    }

}
