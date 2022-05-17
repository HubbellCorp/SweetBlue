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

import com.idevicesinc.sweetblue.utils.BleSetupHelper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class EnablerTest extends BaseBleUnitTest
{
    @Test//(timeout = 30000)
    public void noPermissionsTest() throws Exception
    {
        startSynchronousTest();
        m_config.loggingOptions = LogOptions.ON;
        m_manager.setConfig(m_config);

        // Lets run a very basic set of tests with the enabler
        BleSetupHelper testEnabler = new BleSetupHelper(m_manager, m_activity, result -> {
            if (result.getSuccessful() && result.getEnabledPermissions().size() == 0 && result.getSkippedPermissions().size() == 0)
                EnablerTest.this.succeed();
            else
                EnablerTest.this.assertTrue(false);
        });

        testEnabler.start();
    }

    @Test//(timeout = 30000)
    public void customPermissionTest() throws Exception
    {
        startSynchronousTest();
        m_config.loggingOptions = LogOptions.ON;
        m_manager.setConfig(m_config);

        // Lets run a very basic set of tests with the enabler
        BleSetupHelper testEnabler = new BleSetupHelper(m_manager, m_activity, result -> {
            if (result.getSuccessful() && result.getEnabledPermissions().size() == 1 && result.getSkippedPermissions().size() == 0)
                EnablerTest.this.succeed();
            else
                EnablerTest.this.assertTrue(false);
        });

        testEnabler.setImpl(new BleSetupHelper.BluetoothEnablerImpl()
        {
            boolean test = false;

            @Override
            public boolean checkIsCustomPermissionRequired(Object metadata)
            {
                return true;
            }

            @Override
            public boolean checkIsCustomPermissionEnabled(Object metadata)
            {
                return test;
            }

            @Override
            public void requestCustomPermission(Object metadata)
            {
                test = true;
                onCustomPermissionRequestComplete();
            }
        });

        testEnabler.addCustomPermission(null);

        testEnabler.start();
    }
}