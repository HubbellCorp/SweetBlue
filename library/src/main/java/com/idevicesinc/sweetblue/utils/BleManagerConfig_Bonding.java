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

package com.idevicesinc.sweetblue.utils;


import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.BleTask;


/**
 * Convenience class for setting common options when communicating with device which require bonding/pairing. This also assumes the device
 * will ask for a pin code (although if the device doesn't require a pin code, these settings should still work fine). This class should
 * be viewed as a good starting point to find the settings that work best with the device you're trying to connect/bond with. This also sets the
 * bond timeout to 30 seconds (setting it any longer than this is unnecessary as android seems to have a default timeout of 30 seconds).
 */
public class BleManagerConfig_Bonding extends BleManagerConfig
{

    {
        alwaysBondOnConnect = true;
        forceBondDialog = true;
        useLeTransportForBonding = true;
        tryBondingWhileDisconnected = true;
        taskTimeoutRequestFilter = new DefaultTaskTimeoutRequestFilter()
        {
            @Override
            public Please onEvent(TaskTimeoutRequestEvent e)
            {
                if (e.task() == BleTask.BOND)
                {
                    return Please.setTimeoutFor(Interval.secs(30));
                }
                return super.onEvent(e);
            }
        };
    }

}
