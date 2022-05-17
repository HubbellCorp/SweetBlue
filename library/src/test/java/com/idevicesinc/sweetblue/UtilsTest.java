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


import com.idevicesinc.sweetblue.utils.*;
import com.idevicesinc.sweetblue.utils.Uuids;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.idevicesinc.sweetblue.framework.AbstractTestClass;


public class UtilsTest extends AbstractTestClass
{

    @Test(timeout = 5000)
    public void matchingUUIDTest() throws Exception
    {
        startSynchronousTest();
        List<UUID> adIds = new ArrayList<>();
        adIds.add(Uuids.BATTERY_LEVEL);
        adIds.add(Uuids.BATTERY_SERVICE_UUID);
        List<UUID> looking = new ArrayList<>();
        looking.add(Uuids.BATTERY_LEVEL);
        assert Utils.haveMatchingIds(adIds, looking);
        looking.clear();
        looking.add(Uuids.DEVICE_INFORMATION_SERVICE_UUID);
        assertFalse(Utils.haveMatchingIds(adIds, looking));
        succeed();
    }

}
