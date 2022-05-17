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


import com.idevicesinc.sweetblue.utils.Interval;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class ScanPowerTest extends BaseBleUnitTest
{

    @Test(timeout = 15000)
    public void scanPowerVeryLow() throws Exception
    {
        m_config.scanApi = BleScanApi.POST_LOLLIPOP;
        m_config.scanPower = BleScanPower.VERY_LOW_POWER;
        m_manager.setConfig(m_config);
        m_manager.setListener_State(e -> {
            if (e.didEnter(BleManagerState.SCANNING))
            {
                ScanPowerTest.this.assertTrue("Scan Power: " + ScanPowerTest.this.getScanPower().name(), ScanPowerTest.this.getScanPower() == BleScanPower.VERY_LOW_POWER);
                m_manager.stopScan();
                ScanPowerTest.this.succeed();
            }
        });
        m_manager.startScan(Interval.FIVE_SECS);
        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void scanPowerLow() throws Exception
    {
        m_config.scanApi = BleScanApi.POST_LOLLIPOP;
        m_config.scanPower = BleScanPower.LOW_POWER;
        m_manager.setConfig(m_config);
        m_manager.setListener_State(e -> {
            if (e.didEnter(BleManagerState.SCANNING))
            {
                ScanPowerTest.this.assertTrue("Scan Power: " + ScanPowerTest.this.getScanPower().name(), ScanPowerTest.this.getScanPower() == BleScanPower.LOW_POWER);
                m_manager.stopScan();
                ScanPowerTest.this.succeed();
            }
        });
        m_manager.startScan(Interval.FIVE_SECS);
        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void scanPowerMedium() throws Exception
    {
        m_config.scanApi = BleScanApi.POST_LOLLIPOP;
        m_config.scanPower = BleScanPower.MEDIUM_POWER;
        m_manager.setConfig(m_config);
        m_manager.setListener_State(e -> {
            if (e.didEnter(BleManagerState.SCANNING))
            {
                ScanPowerTest.this.assertTrue("Scan Power: " + ScanPowerTest.this.getScanPower().name(), ScanPowerTest.this.getScanPower() == BleScanPower.MEDIUM_POWER);
                m_manager.stopScan();
                ScanPowerTest.this.succeed();
            }
        });
        m_manager.startScan(Interval.FIVE_SECS);
        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void scanPowerHigh() throws Exception
    {
        m_config.scanApi = BleScanApi.POST_LOLLIPOP;
        m_config.scanPower = BleScanPower.HIGH_POWER;
        m_manager.setConfig(m_config);
        m_manager.setListener_State(e -> {
            if (e.didEnter(BleManagerState.SCANNING))
            {
                ScanPowerTest.this.assertTrue("Scan Power: " + ScanPowerTest.this.getScanPower().name(), ScanPowerTest.this.getScanPower() == BleScanPower.HIGH_POWER);
                m_manager.stopScan();
                ScanPowerTest.this.succeed();
            }
        });
        m_manager.startScan(Interval.FIVE_SECS);
        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void scanPowerAutoForeground() throws Exception
    {
        m_config.scanApi = BleScanApi.POST_LOLLIPOP;
        m_config.scanPower = BleScanPower.AUTO;
        m_manager.setConfig(m_config);
        m_manager.onResume();
        m_manager.setListener_State(e -> {
            if (e.didEnter(BleManagerState.SCANNING))
            {
                // We're in the foreground, and NOT running an infinite scan, so this should be High power here
                ScanPowerTest.this.assertTrue("Scan Power: " + ScanPowerTest.this.getScanPower().name(), ScanPowerTest.this.getScanPower() == BleScanPower.HIGH_POWER);
                m_manager.stopScan();
                ScanPowerTest.this.succeed();
            }
        });
        m_manager.startScan(Interval.FIVE_SECS);
        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void scanPowerAutoInfinite() throws Exception
    {
        m_config.scanApi = BleScanApi.POST_LOLLIPOP;
        m_config.scanPower = BleScanPower.AUTO;
        m_manager.setConfig(m_config);
        m_manager.onResume();
        m_manager.setListener_State(e -> {
            if (e.didEnter(BleManagerState.SCANNING))
            {
                // We're in the foreground, and running an infinite scan, so this should be Medium power here
                ScanPowerTest.this.assertTrue("Scan Power: " + ScanPowerTest.this.getScanPower().name(), ScanPowerTest.this.getScanPower() == BleScanPower.MEDIUM_POWER);
                m_manager.stopScan();
                ScanPowerTest.this.succeed();
            }
        });
        m_manager.startScan();
        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void scanPowerAutoBackground() throws Exception
    {
        m_config.scanApi = BleScanApi.POST_LOLLIPOP;
        m_config.scanPower = BleScanPower.AUTO;
        m_manager.setConfig(m_config);
        m_manager.onPause();
        m_manager.setListener_State(e -> {
            if (e.didEnter(BleManagerState.SCANNING))
            {
                // We're in the background, so this should be low power here
                ScanPowerTest.this.assertTrue("Scan Power: " + ScanPowerTest.this.getScanPower().name(), ScanPowerTest.this.getScanPower() == BleScanPower.LOW_POWER);
                m_manager.stopScan();
                m_manager.onResume();
                ScanPowerTest.this.succeed();
            }
        });
        m_manager.startScan(Interval.FIVE_SECS);
        startAsyncTest();
    }

}
