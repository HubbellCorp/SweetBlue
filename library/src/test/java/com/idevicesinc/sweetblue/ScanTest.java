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


import com.idevicesinc.sweetblue.compat.L_Util;
import com.idevicesinc.sweetblue.internal.P_Bridge_BleManager;
import com.idevicesinc.sweetblue.internal.android.P_DeviceHolder;
import com.idevicesinc.sweetblue.utils.BleScanRecord;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.ManufacturerData;
import com.idevicesinc.sweetblue.utils.Pointer;
import com.idevicesinc.sweetblue.utils.UpdateThreadType;
import com.idevicesinc.sweetblue.utils.Util_Unit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class ScanTest extends BaseBleUnitTest
{

    static final int LEEWAY = 500;

    @Test(timeout = 10000)
    public void scanApiClassicTest() throws Exception
    {
        m_config.scanApi = BleScanApi.CLASSIC;
        m_manager.setConfig(m_config);
        m_manager.setListener_State(e -> {
            if (e.didEnter(BleManagerState.SCANNING))
            {
                ScanTest.this.assertTrue("Scan Api: " + ScanTest.this.getScanApi().name(), ScanTest.this.getScanApi() == BleScanApi.CLASSIC);
                m_manager.stopScan();
                ScanTest.this.succeed();
            }
        });
        m_manager.startScan(Interval.FIVE_SECS);
        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void scanClassicOneTimeTest() throws Exception
    {
        m_config.scanApi = BleScanApi.CLASSIC;
        m_config.loggingOptions = LogOptions.ON;
        m_manager.setConfig(m_config);
        m_manager.setListener_State(e -> {
            if (e.didEnter(BleManagerState.SCANNING))
            {
                ScanTest.this.assertTrue("Scan Api: " + ScanTest.this.getScanApi().name(), ScanTest.this.getScanApi() == BleScanApi.CLASSIC);
            }
            else if (e.didExit(BleManagerState.SCANNING))
            {
                ScanTest.this.assertFalse("Scan task is in the queue, when it should not be!", P_Bridge_BleManager.isInQueue(m_manager.getIBleManager(), "com.idevicesinc.sweetblue.internal.P_Task_Scan") || P_Bridge_BleManager.isCurrent(m_manager.getIBleManager(), "com.idevicesinc.sweetblue.internal.P_Task_Scan"));
                ScanTest.this.succeed();
            }
        });

        m_manager.startScan(Interval.FIVE_SECS);
        startAsyncTest();
    }

    @Test(timeout = 10000)
    public void scanApiPreLollipop() throws Exception
    {
        m_config.scanApi = BleScanApi.PRE_LOLLIPOP;
        m_manager.setConfig(m_config);
        m_manager.setListener_State(e -> {
            if (e.didEnter(BleManagerState.SCANNING))
            {
                ScanTest.this.assertTrue("Scan Api: " + ScanTest.this.getScanApi().name(), ScanTest.this.getScanApi() == BleScanApi.PRE_LOLLIPOP);
                m_manager.stopScan();
                ScanTest.this.succeed();
            }
        });
        m_manager.startScan(Interval.FIVE_SECS);
        startAsyncTest();
    }

    @Test(timeout = 10000)
    public void scanApiPostLollipop() throws Exception
    {
        m_config.scanApi = BleScanApi.POST_LOLLIPOP;
        m_manager.setConfig(m_config);
        m_manager.setListener_State(e -> {
            if (e.didEnter(BleManagerState.SCANNING))
            {
                ScanTest.this.assertTrue("Scan Api: " + ScanTest.this.getScanApi().name(), ScanTest.this.getScanApi() == BleScanApi.POST_LOLLIPOP);
                m_manager.stopScan();
                ScanTest.this.succeed();
            }
        });
        m_manager.startScan(Interval.FIVE_SECS);
        startAsyncTest();
    }

    @Test(timeout = 20000)
    public void inifiteScanWithPauseTest() throws Exception
    {
        m_config.scanApi = BleScanApi.POST_LOLLIPOP;
        m_manager.setConfig(m_config);
        m_manager.setListener_State(new ManagerStateListener()
        {
            boolean scanStarted = false;
            boolean scanPaused = false;

            @Override
            public void onEvent(ManagerStateListener.StateEvent e)
            {
                if (e.didEnter(BleManagerState.SCANNING))
                {
                    if (!scanPaused)
                    {
                        scanStarted = true;
                    }
                    else
                    {
                        succeed();
                    }
                }
                else if (e.didEnter(BleManagerState.SCANNING_PAUSED))
                {
                    assertTrue(scanStarted);
                    scanPaused = true;
                }
            }
        });
        m_manager.startScan(Interval.INFINITE);
        startAsyncTest();
    }

    @Test(timeout = 30000)
    public void inifiteScanForcedTest() throws Exception
    {
        m_config.scanApi = BleScanApi.POST_LOLLIPOP;
        m_config.updateLoopCallback = timestep_seconds -> {
            double timeScanning = m_manager.getTimeInState(BleManagerState.SCANNING).secs();
            if (timeScanning >= 12.0)
            {
                ScanTest.this.succeed();
            }
        };
        m_manager.setConfig(m_config);
        m_manager.setListener_State(e -> {
            if (e.didEnter(BleManagerState.SCANNING))
            {
            }
            else if (e.didEnter(BleManagerState.SCANNING_PAUSED))
            {
                ScanTest.this.assertFalse("Scanning paused for a forced infinite scan!", true);
            }
        });

        m_manager.startScan(new ScanOptions().scanFor(Interval.INFINITE).forceIndefinite(true));
        startAsyncTest();
    }

    @Test(timeout = 10000)
    public void scanApiAuto() throws Exception
    {
        m_config.scanApi = BleScanApi.AUTO;
        m_manager.setConfig(m_config);
        m_manager.setListener_State(e -> {
            if (e.didEnter(BleManagerState.SCANNING))
            {
                ScanTest.this.assertTrue("Scan Api: " + ScanTest.this.getScanApi().name(), ScanTest.this.getScanApi() == BleScanApi.POST_LOLLIPOP);
                m_manager.stopScan();
                ScanTest.this.succeed();
            }
        });
        m_manager.startScan(Interval.FIVE_SECS);
        startAsyncTest();
    }

    @Test(timeout = 30000)
    public void scanApiAutoSwitchApiTest() throws Exception
    {
        m_config.scanApi = BleScanApi.AUTO;
        m_manager.setConfig(m_config);
        m_manager.setListener_State(new ManagerStateListener()
        {
            boolean paused = false;

            @Override public void onEvent(ManagerStateListener.StateEvent e)
            {
                if (e.didEnter(BleManagerState.SCANNING))
                {
                    if (!paused)
                    {
                        assertTrue("Scan Api: " + getScanApi().name(), getScanApi() == BleScanApi.POST_LOLLIPOP);
                    }
                    else
                    {
                        paused = true;
                        assertTrue("Scan Api: " + getScanApi().name(), getScanApi() == BleScanApi.PRE_LOLLIPOP);
                        m_manager.stopScan();
                        succeed();
                    }
                }
                else if (e.didEnter(BleManagerState.SCANNING_PAUSED))
                {
                    paused = true;
                }
            }
        });
        m_manager.startScan();
        startAsyncTest();
    }

    @Test(timeout = 10000)
    public void scanClassicBoostTest() throws Exception
    {
        m_config.scanApi = BleScanApi.AUTO;
        m_config.scanClassicBoostLength = Interval.secs(BleManagerConfig.DEFAULT_CLASSIC_SCAN_BOOST_TIME);
        m_manager.setConfig(m_config);
        m_manager.setListener_State(new ManagerStateListener()
        {

            boolean boosted = false;

            @Override public void onEvent(ManagerStateListener.StateEvent e)
            {
                if (e.didEnter(BleManagerState.BOOST_SCANNING))
                {
                    boosted = true;
                }
                if (e.didEnter(BleManagerState.SCANNING))
                {
                    assertTrue(boosted);
                    assertFalse(m_manager.is(BleManagerState.BOOST_SCANNING));
                    m_manager.stopScan();
                    succeed();
                }
            }
        });
        m_manager.startScan(Interval.FIVE_SECS);
        startAsyncTest();
    }

    // While the scan itself is only 2 seconds, it takes a couple seconds for the test
    // to spin up the Java VM, so the timeout adds some padding to give it enough
    // time
    @Test(timeout = 4000)
    public void singleScanWithInterval() throws Exception
    {
        doSingleScanTest(2000);
        startAsyncTest();
    }

    @Test(timeout = 7000)
    public void periodicScanTest() throws Exception
    {
        doPeriodicScanTest(1000);
        startAsyncTest();
    }

    @Test(timeout = 7000)
    public void periodicScanWithOptionsTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;
        m_manager.setConfig(m_config);
        doPeriodicScanOptionsTest(1000);
        startAsyncTest();
    }

    @Test(timeout = 14000)
    public void highPriorityScanTest() throws Exception
    {
        final AtomicInteger connected = new AtomicInteger(0);

        final BleDevice device2;

        m_manager.setListener_Discovery(e -> {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                e.device().connect();
                if (connected.getAndIncrement() == 1)
                {
                    m_manager.startScan(new ScanOptions().scanFor(Interval.TEN_SECS).asHighPriority(true));
                }
            }
        });

        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device");
        device2 = m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device 2");

        m_manager.setListener_State(e -> {
            if (e.didEnter(BleManagerState.SCANNING))
            {
                ScanTest.this.assertTrue(P_Bridge_BleManager.isInQueue(m_manager.getIBleManager(), device2.getIBleDevice(), "com.idevicesinc.sweetblue.internal.P_Task_Connect"));
                int position = P_Bridge_BleManager.getPositionInQueue(m_manager.getIBleManager(), device2.getIBleDevice(), "com.idevicesinc.sweetblue.internal.P_Task_Connect");
                ScanTest.this.assertTrue(position != -1);
                ScanTest.this.succeed();
            }
        });
        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void scanRecordUpdateTest() throws Exception
    {
        final int START_FLAGS = 15;

        final int CHANGED_FLAGS = 28;

        final String macAddress = Util_Unit.randomMacAddress();

        m_config.loggingOptions = LogOptions.ON;

        m_manager.setConfig(m_config);

        final BleScanRecord scanInfo = new BleScanRecord().setAdvFlags((byte) START_FLAGS).setName("Testerino").setTxPower((byte) 1).addManufacturerData((short) 0, null);
        final byte[] record = scanInfo.buildPacket();

        m_manager.setListener_Discovery(e -> {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                ScanTest.this.assertTrue("Advertising flags don't match! Expected: " + START_FLAGS + " Got: " + e.device().getAdvertisingFlags(), e.device().getAdvertisingFlags() == START_FLAGS);
                ManufacturerData mdata = new ManufacturerData();
                mdata.m_id = 57;
                List<ManufacturerData> list = new ArrayList<>();
                list.add(mdata);
                scanInfo.setName("Testerino4").setAdvFlags((byte) CHANGED_FLAGS).setTxPower((byte) 10).setManufacturerDataList(list);
                byte[] newRecord = scanInfo.buildPacket();
                Util_Native.advertiseDevice(m_manager, -35, newRecord, macAddress, Interval.millis(1250));
            }
            else if (e.was(DiscoveryListener.LifeCycle.REDISCOVERED))
            {
                ScanTest.this.assertTrue(e.device().getAdvertisingFlags() == CHANGED_FLAGS);
                ScanTest.this.assertTrue(e.device().getName_native().contains("4"));
                ScanTest.this.assertTrue(e.device().getTxPower() == 10);
                ScanTest.this.assertTrue(e.device().getManufacturerId() == 57);
                ScanTest.this.succeed();
            }
        });

        m_manager.setListener_State(e -> {
            if (e.didEnter(BleManagerState.SCANNING))
            {
                Util_Native.advertiseDevice(m_manager, -45, record, macAddress);
            }
        });

        m_manager.startScan();

        startAsyncTest();
    }

    @Test(timeout = 12000)
    public void stopScanOnPauseTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;
        m_config.stopScanOnPause = true;
        m_config.updateThreadType = UpdateThreadType.THREAD;

        final AtomicBoolean scanStarted = new AtomicBoolean(false);

        m_manager.setConfig(m_config);

        m_manager.setListener_State(e -> {
            if (e.didEnter(BleManagerState.SCANNING))
            {
                scanStarted.set(true);
                m_manager.onPause();
            }
            else if (e.didExit(BleManagerState.SCANNING) && scanStarted.get())
                ScanTest.this.succeed();
        });

        m_manager.startScan();

        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void stopScanOnFilteredDiscoverTest() throws Exception
    {
        List<String> deviceNames = new ArrayList<>(Arrays.asList("test_device", "testerino", "somethingsomething", "cantthinkofanythingelse", "another_test_device", "stillcantthinkofanything", "something"));
        String filter = "something";

        List<L_Util.ScanResult> scanResults = new ArrayList<>();
        for (int i = 0; i < deviceNames.size(); i++)
        {
            final String macAddress = Util_Unit.randomMacAddress();
            final int rssi = Util_Unit.randomRssi();
            final BleScanRecord scanInfo = new BleScanRecord().setName(deviceNames.get(i));
            final byte[] scanRecord = scanInfo.buildPacket();
            L_Util.ScanResult scanResult = new L_Util.ScanResult(P_DeviceHolder.newNullHolder(macAddress), rssi, scanRecord);
            scanResults.add(scanResult);
        }

        ScanFilter scanFilter = e -> ScanFilter.Please.acknowledgeIf(e.name_normalized().contains(filter)).thenStopScan();

        DiscoveryListener discoveryListener = e ->
        {
            assertTrue("Got: " + e.device().getName_normalized() + " Expected Name Containing: " + filter, e.device().getName_normalized().contains(filter));
            assertFalse(m_manager.isScanning());
            succeed();
        };

        m_manager.setListener_State(e -> {
            if (e.didEnter(BleManagerState.SCANNING)) {
                Util_Native.advertiseDeviceList(m_manager, scanResults, Interval.ZERO);
            }
        });

        // Discovery listener is passed to startScan rather than set on the manager to make use of the
        // ephemeral discovery listener.
        m_manager.startScan(scanFilter, discoveryListener);

        startAsyncTest();
    }

    @Test(timeout = 12000)
    public void scanDelayAfterResumeTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;
        m_config.autoScanDelayAfterResume = Interval.secs(2.0);

        final AtomicInteger scanState = new AtomicInteger(0);
        final AtomicLong resumeStart = new AtomicLong(0);

        m_manager.setConfig(m_config);

        m_manager.setListener_State(e -> {
            if (e.didEnter(BleManagerState.SCANNING))
            {
                if (scanState.incrementAndGet() > 1)
                {
                    long diff = m_manager.getIBleManager().currentTime() - resumeStart.get();
                    ScanTest.this.assertTrue("Diff: " + diff, diff >= 2000);
                    ScanTest.this.succeed();
                }
                else
                {
                    m_manager.onPause();
                    resumeStart.set(m_manager.getIBleManager().currentTime());
                    m_manager.onResume();
                }
            }
        });

        m_manager.startScan();

        startAsyncTest();
    }


    @Test(timeout = 12000)
    public void scanDelayAfterBleTurnsOnTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;
        m_config.autoScanDelayAfterBleTurnsOn = Interval.secs(2.0);

        final AtomicInteger scanState = new AtomicInteger(0);
        final AtomicLong resumeStart = new AtomicLong(0);

        m_manager.setConfig(m_config);

        m_manager.setListener_State(e -> {
            if (e.didEnter(BleManagerState.SCANNING))
            {
                if (scanState.incrementAndGet() > 1)
                {
                    long diff = m_manager.getIBleManager().currentTime() - resumeStart.get();
                    ScanTest.this.assertTrue("Diff: " + diff, diff >= 2000);
                    ScanTest.this.succeed();
                }
                else
                {
                    Util_Native.simulateBleTurningOff(m_manager, Interval.millis(50));
                    resumeStart.set(m_manager.getIBleManager().currentTime());
                    Util_Native.simulateBleTurningOn(m_manager, Interval.millis(200), Interval.millis(50));
                }
            }
        });

        m_manager.startScan();

        startAsyncTest();
    }



    private void doPeriodicScanTest(final long scanTime) throws Exception
    {
        final AtomicBoolean didStop = new AtomicBoolean(false);
        final AtomicLong time = new AtomicLong();
        m_manager.setListener_State(e -> {
            if (e.didExit(BleManagerState.SCANNING))
            {
                if (didStop.get())
                {
                    long diff = m_manager.getIBleManager().currentTime() - time.get();
                    // Make sure that our scan time is correct, this checks against
                    // 3x the scan amount (2 scans, 1 pause). We may need to add a bit
                    // to LEEWAY here, as it's going through 3 iterations, but for now
                    // it seems to be ok for the test

                    // We also need to account for boost scan time here

                    long boostTime = ScanTest.this.getBoostTime();

                    long targetTime = (scanTime * 3) + boostTime;
                    ScanTest.this.assertTrue("Target: " + targetTime + "  Actual: " + time.get() + "  Diff: " + diff, diff >= targetTime);
                    ScanTest.this.succeed();
                }
                else
                {
                    didStop.set(true);
                }
            }
        });
        time.set(m_manager.getIBleManager().currentTime());

        ScanOptions scanOptions = new ScanOptions();
        scanOptions.scanPeriodically(Interval.millis(scanTime), Interval.millis(scanTime));

        m_manager.startScan(scanOptions);
    }

    private void doPeriodicScanOptionsTest(final long scanTime) throws Exception
    {
        final AtomicBoolean didStop = new AtomicBoolean(false);
        final AtomicLong time = new AtomicLong();
        m_manager.setListener_State(e -> {
            if (e.didExit(BleManagerState.SCANNING))
            {
                if (didStop.get())
                {
                    long diff = m_manager.getIBleManager().currentTime() - time.get();
                    // Make sure that our scan time is correct, this checks against
                    // 3x the scan amount (2 scans, 1 pause). We may need to add a bit
                    // to LEEWAY here, as it's going through 3 iterations, but for now
                    // it seems to be ok for the test

                    // We also need to account for boost scan time here

                    long boostTime = ScanTest.this.getBoostTime();

                    long targetTime = (scanTime * 3) + boostTime;


                    ScanTest.this.assertTrue("Target time: " + targetTime + " Diff: " + diff, (diff - LEEWAY) < targetTime && targetTime < (diff + LEEWAY));
                    ScanTest.this.succeed();
                }
                else
                {
                    didStop.set(true);
                }
            }
        });
        time.set(m_manager.getIBleManager().currentTime());
        ScanOptions options = new ScanOptions().scanPeriodically(Interval.millis(scanTime), Interval.millis(scanTime));
        m_manager.startScan(options);
    }

    private long getBoostTime()
    {
        return Interval.isEnabled(m_manager.getConfigClone().scanClassicBoostLength.millis()) ? m_manager.getConfigClone().scanClassicBoostLength.millis() * 2 : 0;
    }

    private void doSingleScanTest(final long scanTime) throws Exception
    {
        final Pointer<Long> time = new Pointer<>();
        m_manager.setListener_State(e -> {
            if (e.didEnter(BleManagerState.SCANNING))
            {
                time.value = System.currentTimeMillis();
            }
            if (e.didExit(BleManagerState.SCANNING))
            {
                // Make sure our scan time is approximately correct
                long diff = System.currentTimeMillis() - time.value;
                ScanTest.this.assertTrue("Scan didn't run the appropriate amount of time. Requested time = " + scanTime + " Diff = " + diff, ((diff - LEEWAY) < scanTime && scanTime < (diff + LEEWAY)));
                ScanTest.this.succeed();
            }
        });
        m_manager.startScan(Interval.millis(scanTime));
    }

}
