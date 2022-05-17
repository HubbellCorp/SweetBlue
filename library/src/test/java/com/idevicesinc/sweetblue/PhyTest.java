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


import com.idevicesinc.sweetblue.utils.Phy;
import com.idevicesinc.sweetblue.utils.Util_Unit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;


@Config(manifest = Config.NONE, sdk = 26)
@RunWith(RobolectricTestRunner.class)
public class PhyTest extends BaseBleUnitTest
{

    @Test(timeout = 10000)
    public void highSpeedDefaultTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;

        doDefaultTest(Phy.HIGH_SPEED);

        startAsyncTest();
    }

    @Test(timeout = 10000)
    public void highSpeedManualTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;

        doManualTest(Phy.HIGH_SPEED);

        startAsyncTest();
    }

    @Test(timeout = 10000)
    public void longRange2XDefaultTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;

        doDefaultTest(Phy.LONG_RANGE_2X);

        startAsyncTest();
    }

    @Test(timeout = 10000)
    public void longRange2XManualTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;

        doManualTest(Phy.LONG_RANGE_2X);

        startAsyncTest();
    }

    @Test(timeout = 10000)
    public void longRange4XDefaultTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;

        doDefaultTest(Phy.LONG_RANGE_4X);

        startAsyncTest();
    }

    @Test(timeout = 10000)
    public void longRange4XManualTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;

        doManualTest(Phy.LONG_RANGE_4X);

        startAsyncTest();
    }


    private void doDefaultTest(final Phy phyOption)
    {
        m_config.phyOptions = phyOption;

        m_manager.setConfig(m_config);

        final BleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "Speedster");

        device.connect(e -> {
            if (e.wasSuccess())
            {
                PhyTest.this.assertTrue("Expecting " + phyOption.name() + ", but got " + device.getIBleDevice().getPhy_private().name(), device.getIBleDevice().getPhy_private() == phyOption);
                PhyTest.this.succeed();
            }
            else
                throw new RuntimeException("Connect failed! This shouldn't happen!!!");
        });
    }

    private void doManualTest(final Phy phyOption)
    {
        m_manager.setConfig(m_config);

        final BleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "Speedster");

        device.connect(e -> {
            if (e.wasSuccess())
            {
                device.setPhyOptions(phyOption, e1 -> {
                    assertTrue(e1.wasSuccess());
                    assertTrue("Expecting " + phyOption.name() + ", but got " + device.getIBleDevice().getPhy_private().name(), device.getIBleDevice().getPhy_private() == phyOption);
                    succeed();
                });
            }
            else
                throw new RuntimeException("Connect failed! This shouldn't happen!!!");
        });
    }

}
