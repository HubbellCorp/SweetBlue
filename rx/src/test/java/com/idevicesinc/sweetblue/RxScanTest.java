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


import com.idevicesinc.sweetblue.internal.P_Bridge_BleManager;
import com.idevicesinc.sweetblue.rx.RxBleDevice;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class RxScanTest extends RxBaseBleUnitTest
{

    private static final int LEEWAY = 500;


    @Test(timeout = 10000)
    public void scanApiPostLollipop() throws Exception
    {
        m_config.scanApi = BleScanApi.POST_LOLLIPOP;
        m_manager.setConfig(m_config);
        m_disposables.add(m_manager.observeMgrStateEvents().subscribe(e ->
        {
            if (e.didEnter(BleManagerState.SCANNING))
            {
                assertTrue("Scan Api: " + getScanApi().name(), getScanApi() == BleScanApi.POST_LOLLIPOP);
                m_manager.stopScan();
                succeed();
            }
        }));
        m_manager.scan().subscribe();
        startAsyncTest();
    }

    @Test(timeout = 20000)
    public void inifiteScanWithPauseTest() throws Exception
    {
        m_config.scanApi = BleScanApi.POST_LOLLIPOP;
        m_manager.setConfig(m_config);
        final AtomicBoolean scanStarted = new AtomicBoolean(false);
        final AtomicBoolean scanPaused = new AtomicBoolean(false);
        m_disposables.add(m_manager.observeMgrStateEvents().subscribe(e ->
        {
            if (e.didEnter(BleManagerState.SCANNING))
            {
                if (scanPaused.compareAndSet(false, true))
                    succeed();
            }
            else if (e.didEnter(BleManagerState.SCANNING_PAUSED))
            {
                assertTrue(scanStarted.get());
                scanPaused.set(true);
            }
        }));

        m_disposables.add(m_manager.scan().subscribe());
        startAsyncTest();
    }

    @Test(timeout = 30000)
    public void inifiteScanForcedTest() throws Exception
    {
        m_config.scanApi = BleScanApi.POST_LOLLIPOP;
        m_config.updateLoopCallback = timestep_seconds ->
        {
            double timeScanning = m_manager.getTimeInState(BleManagerState.SCANNING).secs();
            if (timeScanning >= 15.0)
            {
                succeed();
            }
        };
        m_manager.setConfig(m_config);
        m_disposables.add(m_manager.observeMgrStateEvents().subscribe(e ->
        {
            if (e.didEnter(BleManagerState.SCANNING))
            {
            }
            else if (e.didEnter(BleManagerState.SCANNING_PAUSED))
            {
                assertFalse("Scanning paused for a forced infinite scan!", true);
            }
        }));

        m_disposables.add(m_manager.scan(new ScanOptions().scanFor(Interval.INFINITE).forceIndefinite(true)).subscribe());
        startAsyncTest();
    }

    @Test(timeout = 10000)
    public void scanApiAuto() throws Exception
    {
        m_config.scanApi = BleScanApi.AUTO;
        m_manager.setConfig(m_config);
        m_disposables.add(m_manager.observeMgrStateEvents().subscribe(e ->
        {
            if (e.didEnter(BleManagerState.SCANNING))
            {
                assertTrue("Scan Api: " + getScanApi().name(), getScanApi() == BleScanApi.POST_LOLLIPOP);
                m_manager.stopScan();
                succeed();
            }
        }));

        m_disposables.add(m_manager.scan(new ScanOptions().scanFor(Interval.FIVE_SECS)).subscribe());
        startAsyncTest();
    }

    @Test(timeout = 30000)
    public void scanApiAutoSwitchApiTest() throws Exception
    {
        m_config.scanApi = BleScanApi.AUTO;
        m_manager.setConfig(m_config);
        final AtomicBoolean paused = new AtomicBoolean(false);
        m_disposables.add(m_manager.observeMgrStateEvents().subscribe(e ->
        {
            if (e.didEnter(BleManagerState.SCANNING))
            {
                if (!paused.get())
                {
                    assertTrue("Scan Api: " + getScanApi().name(), getScanApi() == BleScanApi.POST_LOLLIPOP);
                }
                else
                {
                    paused.set(true);
                    assertTrue("Scan Api: " + getScanApi().name(), getScanApi() == BleScanApi.PRE_LOLLIPOP);
                    m_manager.stopScan();
                    succeed();
                }
            }
            else if (e.didEnter(BleManagerState.SCANNING_PAUSED))
            {
                paused.set(true);
            }
        }));

        m_disposables.add(m_manager.scan().subscribe());
        startAsyncTest();
    }

    @Test(timeout = 10000)
    public void scanClassicBoostTest() throws Exception
    {
        m_config.scanApi = BleScanApi.AUTO;
        m_config.scanClassicBoostLength = Interval.secs(BleManagerConfig.DEFAULT_CLASSIC_SCAN_BOOST_TIME);
        m_manager.setConfig(m_config);
        final AtomicBoolean boosted = new AtomicBoolean(false);
        m_disposables.add(m_manager.observeMgrStateEvents().subscribe(e ->
        {
            if (e.didEnter(BleManagerState.BOOST_SCANNING))
            {
                boosted.set(true);
            }
            if (e.didEnter(BleManagerState.SCANNING))
            {
                assertTrue(boosted.get());
                assertFalse(m_manager.is(BleManagerState.BOOST_SCANNING));
                m_manager.stopScan();
                succeed();
            }
        }));
        m_disposables.add(m_manager.scan().subscribe());
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

    @Test(timeout = 15000)
    public void highPriorityScanTest() throws Exception
    {
        final AtomicInteger connected = new AtomicInteger(0);

        final RxBleDevice device2;

        m_disposables.add(m_manager.observeDiscoveryEvents().subscribe(e ->
        {
            if (e.wasDiscovered())
            {
                m_disposables.add(e.device().connect().subscribe(() -> {}, throwable -> {}));
                if (connected.getAndIncrement() == 1)
                {
                    m_disposables.add(m_manager.scan(new ScanOptions().scanFor(Interval.TEN_SECS).asHighPriority(true)).subscribe());
                }
            }
        }));


        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device");
        device2 = m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device 2");

        m_disposables.add(m_manager.observeMgrStateEvents().subscribe(e ->
        {
            if (e.didEnter(BleManagerState.SCANNING))
            {
                assertTrue(P_Bridge_BleManager.isInQueue(m_manager.getBleManager().getIBleManager(), device2.getBleDevice().getIBleDevice(), "com.idevicesinc.sweetblue.internal.P_Task_Connect"));
                int position = P_Bridge_BleManager.getPositionInQueue(m_manager.getBleManager().getIBleManager(), device2.getBleDevice().getIBleDevice(), "com.idevicesinc.sweetblue.internal.P_Task_Connect");
                assertTrue(position != -1);
                succeed();
            }
        }));

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

        BleScanRecord scanInfo = new BleScanRecord().setAdvFlags((byte) START_FLAGS).setName("Testerino").setTxPower((byte) 1).addManufacturerData((short) 0, null);
        byte[] record = scanInfo.buildPacket();

        m_disposables.add(m_manager.observeMgrStateEvents().subscribe(e ->
        {
            if (e.didEnter(BleManagerState.SCANNING))
            {
                Util_Native.advertiseDevice(m_manager.getBleManager(), -45, record, macAddress);
            }
        }));

        m_disposables.add(m_manager.scan().subscribe(e ->
        {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                assertTrue("Advertising flags don't match! Expected: " + START_FLAGS + " Got: " + e.device().getAdvertisingFlags(), e.device().getAdvertisingFlags() == START_FLAGS);
                ManufacturerData mdata = new ManufacturerData();
                mdata.m_id = 57;
                List<ManufacturerData> list = new ArrayList<>();
                list.add(mdata);
                scanInfo.setName("Testerino4").setAdvFlags((byte) CHANGED_FLAGS).setTxPower((byte) 10).setManufacturerDataList(list);
                byte[] newRecord = scanInfo.buildPacket();
                Util_Native.advertiseDevice(m_manager.getBleManager(), -35, newRecord, macAddress, Interval.millis(1250));
            }
            else if (e.was(DiscoveryListener.LifeCycle.REDISCOVERED))
            {
                assertTrue(e.device().getAdvertisingFlags() == CHANGED_FLAGS);
                assertTrue(e.device().getName_native().contains("4"));
                assertTrue(e.device().getTxPower() == 10);
                assertTrue(e.device().getManufacturerId() == 57);
                succeed();
            }
        }));

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

        m_disposables.add(m_manager.observeMgrStateEvents().subscribe(e ->
        {
            if (e.didEnter(BleManagerState.SCANNING))
            {
                scanStarted.set(true);
                m_manager.onPause();
            }
            else if (e.didExit(BleManagerState.SCANNING) && scanStarted.get())
                succeed();
        }));

        m_disposables.add(m_manager.scan().subscribe());

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

        m_disposables.add(m_manager.observeMgrStateEvents().subscribe(e ->
        {
            if (e.didEnter(BleManagerState.SCANNING))
            {
                if (scanState.incrementAndGet() > 1)
                {
                    long diff = m_manager.getBleManager().getIBleManager().currentTime() - resumeStart.get();
                    assertTrue("Diff: "  + diff, diff >= 2000);
                    succeed();
                }
                else
                {
                    m_manager.onPause();
                    resumeStart.set(m_manager.getBleManager().getIBleManager().currentTime());
                    m_manager.onResume();
                }
            }
        }));

        m_disposables.add(m_manager.scan().subscribe());

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

        m_disposables.add(m_manager.observeMgrStateEvents().subscribe(e ->
        {
            if (e.didEnter(BleManagerState.SCANNING))
            {
                if (scanState.incrementAndGet() > 1)
                {
                    long diff = m_manager.getBleManager().getIBleManager().currentTime() - resumeStart.get();
                    assertTrue("Diff: "  + diff, (diff >= 2000 && diff < (2000 + LEEWAY)));
                    succeed();
                }
                else
                {
                    Util_Native.sendBluetoothStateChange(m_manager.getBleManager(), BleStatuses.STATE_ON, BleStatuses.STATE_OFF, Interval.ZERO);
                    resumeStart.set(m_manager.getBleManager().getIBleManager().currentTime());
                    Util_Native.sendBluetoothStateChange(m_manager.getBleManager(), BleStatuses.STATE_OFF, BleStatuses.STATE_ON, Interval.ZERO);
                }
            }
        }));

        m_disposables.add(m_manager.scan().subscribe());

        startAsyncTest();
    }





    private void doPeriodicScanTest(final long scanTime) throws Exception
    {
        final AtomicBoolean didStop = new AtomicBoolean(false);
        final AtomicLong time = new AtomicLong();
        m_disposables.add(m_manager.observeMgrStateEvents().subscribe(e ->
        {
            if (e.didExit(BleManagerState.SCANNING))
            {
                if (didStop.get())
                {
                    long diff = m_manager.getBleManager().getIBleManager().currentTime() - time.get();
                    // Make sure that our scan time is correct, this checks against
                    // 3x the scan amount (2 scans, 1 pause). We may need to add a bit
                    // to LEEWAY here, as it's going through 3 iterations, but for now
                    // it seems to be ok for the test

                    // We also need to account for boost scan time here

                    long boostTime = getBoostTime();

                    long targetTime = (scanTime * 3) + boostTime;
                    assertTrue("Target: " + targetTime + "  Actual: " + time.get() + "  Diff: " + diff, (diff - LEEWAY) < targetTime && targetTime < (diff + LEEWAY));
                    succeed();
                }
                else
                {
                    didStop.set(true);
                }
            }
        }));
        time.set(m_manager.getBleManager().getIBleManager().currentTime());
        m_disposables.add(m_manager.scan(new ScanOptions().scanPeriodically(Interval.millis(scanTime), Interval.millis(scanTime))).subscribe());
    }

    private void doPeriodicScanOptionsTest(final long scanTime) throws Exception
    {
        final AtomicBoolean didStop = new AtomicBoolean(false);
        final AtomicLong time = new AtomicLong();
        m_disposables.add(m_manager.observeMgrStateEvents().subscribe(e ->
        {
            if (e.didExit(BleManagerState.SCANNING))
            {
                if (didStop.get())
                {
                    long diff = m_manager.getBleManager().getIBleManager().currentTime() - time.get();
                    // Make sure that our scan time is correct, this checks against
                    // 3x the scan amount (2 scans, 1 pause). We may need to add a bit
                    // to LEEWAY here, as it's going through 3 iterations, but for now
                    // it seems to be ok for the test

                    // We also need to account for boost scan time here

                    long boostTime = getBoostTime();

                    long targetTime = (scanTime * 3) + boostTime;


                    assertTrue("Target time: " + targetTime + " Diff: " + diff, (diff - LEEWAY) < targetTime && targetTime < (diff + LEEWAY));
                    succeed();
                }
                else
                {
                    didStop.set(true);
                }
            }
        }));
        time.set(m_manager.getBleManager().getIBleManager().currentTime());
        ScanOptions options = new ScanOptions().scanPeriodically(Interval.millis(scanTime), Interval.millis(scanTime));
        m_disposables.add(m_manager.scan(options).subscribe());
    }

    private long getBoostTime()
    {
        return Interval.isEnabled(m_manager.getConfigClone().scanClassicBoostLength.millis()) ? m_manager.getConfigClone().scanClassicBoostLength.millis() * 2 : 0;
    }

    private void doSingleScanTest(final long scanTime) throws Exception
    {
        final Pointer<Long> time = new Pointer<>();
        m_disposables.add(m_manager.observeMgrStateEvents().subscribe(e ->
        {
            if (e.didEnter(BleManagerState.SCANNING))
            {
                time.value = System.currentTimeMillis();
            }
            if (e.didExit(BleManagerState.SCANNING))
            {
                // Make sure our scan time is approximately correct
                long diff = System.currentTimeMillis() - time.value;
                assertTrue("Scan didn't run the appropriate amount of time. Requested time = " + scanTime +  " Diff = " + diff, ((diff - LEEWAY) < scanTime && scanTime < (diff + LEEWAY)));
                succeed();
            }
        }));
        m_disposables.add(m_manager.scan(new ScanOptions().scanFor(Interval.millis(scanTime))).subscribe());
    }

}
