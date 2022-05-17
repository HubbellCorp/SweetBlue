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
import com.idevicesinc.sweetblue.BleScanApi;
import com.idevicesinc.sweetblue.BleScanPower;


/**
 * Convenience config class to use lower power. This sets the scan api to {@link BleScanApi#POST_LOLLIPOP}, and sets the
 * scan power to {@link BleScanPower#LOW_POWER}. Note that if the android device running Sweetblue is on an OS less than
 * Lollipop, scanning will still work, however, you can't control the power used with that API. This class also sets a slower
 * update rate, and is more aggressive about going into idle mode (along with idle mode being slower as well).
 */
public class BleManagerConfig_LowPower extends BleManagerConfig
{

    {
        scanApi = BleScanApi.POST_LOLLIPOP;
        scanPower = BleScanPower.LOW_POWER;
        autoUpdateRate = Interval.millis(50);
        idleUpdateRate = Interval.secs(2);
        minTimeToIdle = Interval.secs(10);
        autoReconnectDeviceWhenBleTurnsBackOn = false;
    }

}
