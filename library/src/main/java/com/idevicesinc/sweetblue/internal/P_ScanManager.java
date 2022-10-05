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


import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.os.Build;
import android.util.Log;

import com.idevicesinc.sweetblue.BleDeviceOrigin;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.BleScanApi;
import com.idevicesinc.sweetblue.BleScanPower;
import com.idevicesinc.sweetblue.BleStatuses;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.ScanFilter;
import com.idevicesinc.sweetblue.ScanOptions;
import com.idevicesinc.sweetblue.UhOhListener;
import com.idevicesinc.sweetblue.compat.L_Util;
import com.idevicesinc.sweetblue.di.SweetDIManager;
import com.idevicesinc.sweetblue.internal.android.IBluetoothDevice;
import com.idevicesinc.sweetblue.internal.android.P_DeviceHolder;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.Utils_String;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static com.idevicesinc.sweetblue.BleManagerState.SCANNING;
import static com.idevicesinc.sweetblue.BleManagerState.BOOST_SCANNING;
import static com.idevicesinc.sweetblue.BleManagerState.SCANNING_PAUSED;
import static com.idevicesinc.sweetblue.BleManagerState.STARTING_SCAN;


final class P_ScanManager
{

    private static final int SCAN_FAILED_ALREADY_STARTED = 1;
    private static final int SCAN_FAILED_APPLICATION_REGISTRATION_FAILED = 2;

    private final IBleManager m_manager;
    private AtomicReference<BleScanApi> mCurrentApi;
    private AtomicReference<BleScanPower> mCurrentPower;
    private Set<ScanInfo> m_scanEntries;
    private final Object entryLock = new Object();


    private final int m_retryCountMax = 3;
    private boolean m_triedToStartScanAfterTurnedOn;
    private boolean m_doingInfiniteScan;
    private boolean m_forceActualInfinite;
    private boolean m_triedToStartScanAfterResume;
    private boolean m_pausedForAutoStop;
    private double m_timeNotScanning;
    private double m_timePausedScan;
    private double m_totalTimeScanning;
    private double m_intervalTimeScanning;
    private double m_classicLength;
    private double m_timeClassicBoosting;
    private ScanOptions m_currentScanOptions;


    P_ScanManager(IBleManager mgr)
    {
        m_manager = mgr;
        mCurrentApi = new AtomicReference<>(mgr.getConfigClone().scanApi);
        mCurrentPower = new AtomicReference<>(BleScanPower.AUTO);
        m_scanEntries = new HashSet<>();
    }


    public final boolean startScan(PA_StateTracker.E_Intent intent, ScanOptions scanOptions)
    {
        m_currentScanOptions = scanOptions;
        m_timePausedScan = 0.0;
        m_totalTimeScanning = 0.0;
        BleScanApi scanApi = m_manager.getConfigClone().scanApi == BleScanApi.AUTO ? determineAutoApi() : m_manager.getConfigClone().scanApi;

        // If using a PendingIntent, we should ignore whats set in the Manager and force post lollipop behavior

        if (scanOptions.getPendingIntent() != null && scanApi != BleScanApi.POST_LOLLIPOP)
            scanApi = BleScanApi.POST_LOLLIPOP;

        switch (scanApi)
        {
            case CLASSIC:
                mCurrentApi.set(BleScanApi.CLASSIC);
                return tryClassicDiscovery(intent, true);
            case POST_LOLLIPOP:
                if (isBleScanReady())
                {
                    if (Utils.isLollipop())
                    {
                        mCurrentApi.set(BleScanApi.POST_LOLLIPOP);
                        return startScanPostLollipop(m_currentScanOptions.getScanTime().secs());
                    }
                    else
                    {
                        m_manager.getLogger().w("Tried to start post lollipop scan on a device not running lollipop or above! Defaulting to pre-lollipop scan instead.");
                        mCurrentApi.set(BleScanApi.PRE_LOLLIPOP);
                        return startScanPreLollipop(intent);
                    }
                }
                else
                {
                    m_manager.getLogger().e("Tried to start BLE scan, but scanning is not ready (most likely need to get permissions). Falling back to classic discovery.");
                    mCurrentApi.set(BleScanApi.CLASSIC);
                    return tryClassicDiscovery(PA_StateTracker.E_Intent.UNINTENTIONAL, true);
                }
            case AUTO:
            case PRE_LOLLIPOP:
                mCurrentApi.set(BleScanApi.PRE_LOLLIPOP);
                return startScanPreLollipop(intent);
            default:
                return false;
        }
    }

    public final void stopScan()
    {
        m_currentScanOptions = null;
        stopScan_private(true, PA_StateTracker.E_Intent.INTENTIONAL);
    }

    public final double getTotalTimeScanning()
    {
        return m_totalTimeScanning;
    }


    final void stopNativeScan(final P_Task_Scan scanTask)
    {
        boolean pausing = false;
        if (m_currentScanOptions != null && m_currentScanOptions.isPeriodic())
        {
            pausing = true;
        }
        stopScan_private(pausing, scanTask.getIntent());
    }

    final void setInfiniteScan(boolean infinite, boolean force)
    {
        m_doingInfiniteScan = infinite;
        m_forceActualInfinite = force;
    }

    final boolean isInfiniteScan()
    {
        return m_doingInfiniteScan;
    }

    final boolean classicBoost(double scanTime)
    {
        m_classicLength = scanTime;
        return startClassicBoost();
    }

    final boolean isPreLollipopScan()
    {
        return mCurrentApi.get() == BleScanApi.PRE_LOLLIPOP;
    }

    final boolean isPostLollipopScan()
    {
        return mCurrentApi.get() == BleScanApi.POST_LOLLIPOP;
    }

    final boolean isClassicScan()
    {
        return mCurrentApi.get() == BleScanApi.CLASSIC;
    }

    final void pauseScan()
    {
        stopScan_private(false, PA_StateTracker.E_Intent.UNINTENTIONAL);
    }

    final void addScanResult(final P_DeviceHolder device, final int rssi, final byte[] scanRecord)
    {
        final ScanInfo info = new ScanInfo(device, rssi, scanRecord);
        synchronized (entryLock)
        {
            m_scanEntries.add(info);
        }
    }

    final void resetOptions()
    {
        m_currentScanOptions = null;
    }

    final void addBatchScanResults(final List<L_Util.ScanResult> devices)
    {
        synchronized (entryLock)
        {
            for (L_Util.ScanResult res : devices)
            {
                final ScanInfo info = new ScanInfo(res.getDevice(), res.getRssi(), res.getRecord());
                m_scanEntries.add(info);
            }
        }
    }

    final void resetTimeNotScanning()
    {
        m_timeNotScanning = 0.0;
    }

    // Returns if the startScan boolean is true or not.
    final boolean update(double timeStep, long currentTime)
    {
        // Cache the config instance
        final BleManagerConfig config = m_manager.getConfigClone();
        if (m_manager.is(SCANNING))
        {
            m_totalTimeScanning += timeStep;
            m_intervalTimeScanning += timeStep;

            int size = m_scanEntries.size();

            handleScanEntries(size);

            if (!m_forceActualInfinite && m_doingInfiniteScan && Interval.isEnabled(config.infiniteScanInterval) && m_intervalTimeScanning >= config.infiniteScanInterval.secs())
                pauseScan();
        }

        if (m_manager.is(SCANNING_PAUSED) && !m_pausedForAutoStop)
        {
            if (m_currentScanOptions == null)
            {
                // The only way the options could have been nulled out are if stopScan was called somewhere.
                clearScanningFlags();
            }
            else
            {
                m_timePausedScan += timeStep;

                if (m_doingInfiniteScan)
                {
                    Interval pauseTime = Interval.isEnabled(config.infinitePauseInterval) ? config.infinitePauseInterval : Interval.secs(BleManagerConfig.DEFAULT_SCAN_INFINITE_PAUSE_TIME);
                    if (m_timePausedScan >= pauseTime.secs())
                    {
                        m_manager.getLogger().i("Restarting paused scan...");
                        startScan(PA_StateTracker.E_Intent.INTENTIONAL, m_currentScanOptions);
                    }
                }
            }
        }

        if (!m_manager.isAny(SCANNING))
            m_timeNotScanning += timeStep;

        boolean stopClassicBoost = false;

        if (m_manager.is(BOOST_SCANNING))
        {
            m_timeClassicBoosting += timeStep;
            if (m_timeClassicBoosting >= m_classicLength)
                stopClassicBoost = true;
        }

        boolean startScan = doAutoAndPeriodicScanChecks(currentTime, config);

        if (startScan)
        {
            if (m_manager.canPerformAutoScan())
                m_manager.startScan(m_currentScanOptions != null ? m_currentScanOptions : new ScanOptions());
        }

        final P_Task_Scan scanTask = m_manager.getTaskManager().get(P_Task_Scan.class, m_manager);

        if (scanTask != null)
        {
            if (stopClassicBoost)
            {
                m_timeClassicBoosting = 0;
                stopClassicDiscovery();
                scanTask.onClassicBoostFinished();
            }

            if (scanTask.getState() == PE_TaskState.EXECUTING)
                m_manager.tryPurgingStaleDevices(scanTask.getAggregatedTimeArmedAndExecuting());
        }

        return startScan;
    }

    private void clearScanningFlags()
    {
        m_manager.getStateTracker().update(PA_StateTracker.E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, SCANNING, false, SCANNING_PAUSED, false,
                BOOST_SCANNING, false, STARTING_SCAN, false);
    }

    final boolean doAutoAndPeriodicScanChecks(long currentTime, BleManagerConfig config)
    {
        if (m_manager.is(SCANNING))
            return false;

        if (m_currentScanOptions != null && m_manager.ready() && !m_manager.is(BOOST_SCANNING))
        {
            if (m_manager.isForegrounded())
            {
                if (Interval.isEnabled(config.autoScanDelayAfterBleTurnsOn) && !m_triedToStartScanAfterTurnedOn && (currentTime - m_manager.timeTurnedOn()) >= config.autoScanDelayAfterBleTurnsOn.millis())
                {
                    m_triedToStartScanAfterTurnedOn = true;

                    if (!m_manager.isScanning())
                    {
                        m_manager.getLogger().i("Auto starting scan after BLE turned back on...");
                        return true;
                    }
                }
                else if (m_pausedForAutoStop && Interval.isEnabled(config.autoScanDelayAfterResume) && !m_triedToStartScanAfterResume && m_manager.timeForegrounded() >= Interval.secs(config.autoScanDelayAfterResume))
                {
                    m_triedToStartScanAfterResume = true;

                    if (!m_manager.is(SCANNING))
                    {
                        m_pausedForAutoStop = false;
                        m_manager.getLogger().i("Auto starting scan after resume...");
                        return true;
                    }
                }
            }
            if (m_currentScanOptions.isPeriodic() && !m_manager.isAny(SCANNING, STARTING_SCAN))
            {
                double scanInterval = Interval.secs(m_manager.isForegrounded() ? m_currentScanOptions.getPauseTime() : config.autoScanPauseTimeWhileAppIsBackgrounded);

                // If there's already a scan task in the queue, we can just skip this check
                boolean scanTaskInQueue = m_manager.getTaskManager().isInQueue(P_Task_Scan.class, m_manager);

                if (!scanTaskInQueue && Interval.isEnabled(scanInterval) && m_timeNotScanning >= scanInterval)
                {
                    m_manager.getLogger().i("Starting scan as part of a periodic scan...");
                    return true;
                }
            }
        }
        return false;
    }

    final boolean isPeriodicScan()
    {
        return m_currentScanOptions == null || m_currentScanOptions.isPeriodic();
    }

    final void onResume()
    {
        m_triedToStartScanAfterResume = false;

        if (m_doingInfiniteScan && !m_manager.isScanning())
        {
            m_triedToStartScanAfterResume = true;

            m_manager.startScan(m_currentScanOptions);
        }
        else if (Interval.isDisabled(m_manager.getConfigClone().autoScanDelayAfterResume))
        {
            m_triedToStartScanAfterResume = true;
        }
    }

    final BleScanApi getCurrentApi()
    {
        return mCurrentApi.get();
    }

    final BleScanPower getCurrentPower()
    {
        return mCurrentPower.get();
    }

    final void onPause()
    {
        m_triedToStartScanAfterResume = false;
        if (m_manager.getConfigClone().stopScanOnPause && m_manager.isScanning())
        {
            if (m_currentScanOptions != null && m_currentScanOptions.isContinuous())
            {
                m_pausedForAutoStop = true;
                pauseScan();
            }
            else
                m_manager.stopScan(PA_StateTracker.E_Intent.UNINTENTIONAL);
        }
    }

    final void onScanResult(int callbackType, L_Util.ScanResult result)
    {
        final String macAddress = result != null && result.getDevice() != null ? result.getDevice().getAddress() : null;
        m_manager.getLogger().log_native(Log.VERBOSE, macAddress, "Discovered new device via POST-LOLLIPOP scan.");
        if (result != null)
            addScanResult(result.getDevice(), result.getRssi(), result.getRecord());
    }

    final void onBatchScanResult(int callbackType, List<L_Util.ScanResult> results)
    {
        if (m_manager.getLogger().isEnabled())
        {
            for (L_Util.ScanResult res : results)
            {
                final String macAddress = res != null && res.getDevice() != null ? res.getDevice().getAddress() : null;
                m_manager.getLogger().log_native(Log.VERBOSE, macAddress, "Discovered new device via POST-LOLLIPOP scan (batch).");
            }
        }
        addBatchScanResults(results);
    }

    final void onScanFailed(int errorCode)
    {
        if (errorCode == SCAN_FAILED_ALREADY_STARTED)
        {
            m_manager.ASSERT(false, "Got an error stating the scan has already started when trying to start a scan.");
            // We're already scanning, so nothing to do here
        }
        else if (errorCode == SCAN_FAILED_APPLICATION_REGISTRATION_FAILED)
        {
            fail();

            m_manager.uhOh(UhOhListener.UhOh.START_BLE_SCAN_FAILED);
        }
        else
        {
            m_manager.getLogger().e_native(Utils_String.concatStrings("Post lollipop scan failed with error code ", String.valueOf(errorCode)));

            if (m_manager.getConfigClone().revertToClassicDiscoveryIfNeeded)
            {
                m_manager.getLogger().i("Reverting to a CLASSIC scan...");
                tryClassicDiscovery(PA_StateTracker.E_Intent.UNINTENTIONAL, /*suppressUhOh=*/false);

                mCurrentApi.set(BleScanApi.CLASSIC);
            }
            else
                fail();
        }
    }


    private BleScanApi determineAutoApi()
    {
        if (mCurrentApi.get() != BleScanApi.POST_LOLLIPOP)
        {
            return BleScanApi.POST_LOLLIPOP;
        }
        return BleScanApi.PRE_LOLLIPOP;
    }

    private void handleScanEntries(int size)
    {
        if (size > 0)
        {
            final List<ScanInfo> infos;

            // Get our max scan entries to process based off the update loop rate, with
            // a minimum of 5.
            final long upRate = m_manager.getConfigClone().autoUpdateRate.millis();
            final int maxEntries = (int) Math.min(size, Math.max(5, upRate));
            infos = new ArrayList<>(maxEntries);
            synchronized (entryLock)
            {
                int current = 0;
                final Iterator<ScanInfo> it = m_scanEntries.iterator();
                while (it.hasNext() && current < maxEntries)
                {
                    infos.add(it.next());
                    it.remove();
                    current++;
                }
            }

            final List<DiscoveryEntry> entries = new ArrayList<>(infos.size());

            for (ScanInfo info : infos)
            {
                final IBluetoothDevice layer = SweetDIManager.getInstance().get(IBluetoothDevice.class, P_BleDeviceImpl.EMPTY_DEVICE(m_manager));
                layer.setNativeDevice(info.m_device.getDevice(), info.m_device);

                if (m_manager.getConfigClone().enableCrashResolver)
                {
                    if (mCurrentApi.get() == BleScanApi.PRE_LOLLIPOP)
                    {
                        m_manager.getCrashResolver().notifyScannedDevice(layer, getPreLScanCallback(), null);
                    }
                    else
                    {
                        m_manager.getCrashResolver().notifyScannedDevice(layer, null, L_Util.getNativeScanCallback());
                    }
                }

                entries.add(DiscoveryEntry.newEntry(layer, info.m_rssi, info.m_record));
            }

            m_manager.onDiscoveredFromNativeStack(entries);
        }
    }

    private boolean startClassicDiscovery()
    {
        return m_manager.managerLayer().startDiscovery();
    }

    private boolean startClassicBoost()
    {
        boolean success = startClassicDiscovery();
        if (success)
        {
            m_manager.getStateTracker().update(PA_StateTracker.E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, STARTING_SCAN, false, BOOST_SCANNING, true);
        }
        return success;
    }

    private boolean isBleScanReady()
    {
        return m_manager.isScanningReady();
    }

    private void stopScan_private(boolean stopping, PA_StateTracker.E_Intent intent)
    {
        m_intervalTimeScanning = 0.0;
        switch (mCurrentApi.get())
        {
            case CLASSIC:
                stopClassicDiscovery();
                break;
            case POST_LOLLIPOP:
                if (Utils.isLollipop())
                {
                    stopScanPostLollipop();
                }
                else
                {
                    stopScanPreLollipop();
                }
                break;
            case AUTO:
            case PRE_LOLLIPOP:
                stopScanPreLollipop();
        }
        if (stopping)
        {
            m_manager.getStateTracker().update(intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE, SCANNING, false, BOOST_SCANNING, false, SCANNING_PAUSED, false);
        }
        else
        {
            m_manager.getStateTracker().update(intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE, SCANNING, false, SCANNING_PAUSED, true, BOOST_SCANNING, false);
        }
        // Clear out the scan entries list so we don't end up caching old discoveries (it's possible there's a large amount of time between scans, so
        // what's held in the list may not actually be within range anymore, or some other data on it has changed).
        synchronized (entryLock)
        {
            m_scanEntries.clear();
        }
    }

    private boolean startScanPreLollipop(PA_StateTracker.E_Intent intent)
    {
        //--- DRK > Not sure how useful this retry loop is. I've definitely seen startLeScan
        //---		fail but then work again at a later time (seconds-minutes later), so
        //---		it's possible that it can recover although I haven't observed it in this loop.
        int retryCount = 0;

        while (retryCount <= m_retryCountMax)
        {
            final boolean success = startLeScan();

            if (success)
            {
                if (retryCount >= 1)
                {
                    // Used to be an assert case, not sure it's necessary anymore
                }

                break;
            }

            retryCount++;

            if (retryCount <= m_retryCountMax)
            {
                if (retryCount == 1)
                {
                    m_manager.getLogger().w("Failed first startLeScan() attempt. Calling stopLeScan() then trying again...");

                    stopLeScan();
                }
                else
                {
                    m_manager.getLogger().w("Failed startLeScan() attempt number " + retryCount + ". Trying again...");
                }
            }
        }

        if (retryCount > m_retryCountMax)
        {
            m_manager.getLogger().w("Pre-Lollipop LeScan totally failed to start!");

            tryClassicDiscovery(PA_StateTracker.E_Intent.UNINTENTIONAL, false);
            return true;
        }
        else
        {
            if (retryCount > 0)
            {
                m_manager.getLogger().w("Started native scan with " + (retryCount + 1) + " attempts.");
            }

            if (m_manager.getConfigClone().enableCrashResolver)
            {
                m_manager.getCrashResolver().start();
            }

            setStateToScanning();

            return true;
        }
    }

    private void setStateToScanning()
    {
        m_manager.getStateTracker().update(PA_StateTracker.E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, SCANNING, true, SCANNING_PAUSED, false, STARTING_SCAN, false, BOOST_SCANNING, false);
    }

    private boolean startScanPostLollipop(double scanTime)
    {
        int nativePowerMode;
        boolean success = true;
        BleScanPower power = m_manager.getConfigClone().scanPower;
        if (power == BleScanPower.AUTO)
        {
            if (m_manager.isForegrounded())
            {
                if (scanTime == Double.POSITIVE_INFINITY)
                {
                    power = BleScanPower.MEDIUM_POWER;
                    nativePowerMode = BleScanPower.MEDIUM_POWER.getNativeMode();
                }
                else
                {
                    power = BleScanPower.HIGH_POWER;
                    nativePowerMode = BleScanPower.HIGH_POWER.getNativeMode();
                }
            }
            else
            {
                power = BleScanPower.LOW_POWER;
                nativePowerMode = BleScanPower.LOW_POWER.getNativeMode();
            }
        }
        else
        {
            if (power == BleScanPower.VERY_LOW_POWER)
            {
                if (!Utils.isMarshmallow())
                {
                    m_manager.getLogger().e("BleScanPower set to VERY_LOW, but device is not running Marshmallow. Defaulting to LOW instead.");
                    power = BleScanPower.LOW_POWER;
                }
            }
            nativePowerMode = power.getNativeMode();
        }

        if (m_currentScanOptions.getPendingIntent() != null)
        {
            if (Utils.isOreo())
                success = startPendingIntentScan(
                        nativePowerMode,
                        m_currentScanOptions.getMatchMode().internalAndroidValue,
                        m_currentScanOptions.getMatchNumber().internalAndroidValue,
                        m_currentScanOptions.getPendingIntent());
            else
                success = false;
        }
        else if (Utils.isMarshmallow())
        {
            startMScan(
                    nativePowerMode,
                    m_currentScanOptions.getMatchMode().internalAndroidValue,
                    m_currentScanOptions.getMatchNumber().internalAndroidValue);
        }
        else
        {
            startLScan(nativePowerMode);
        }

        mCurrentPower.set(power);

        if (success)
            setStateToScanning();

        return success;
    }

    private boolean tryClassicDiscovery(final PA_StateTracker.E_Intent intent, final boolean suppressUhOh)
    {
        boolean intentional = intent == PA_StateTracker.E_Intent.INTENTIONAL;
        if (intentional || m_manager.getConfigClone().revertToClassicDiscoveryIfNeeded)
        {
            if (false == startClassicDiscovery())
            {
                m_manager.getLogger().w("Classic discovery failed to start!");

                fail();

                m_manager.uhOh(UhOhListener.UhOh.CLASSIC_DISCOVERY_FAILED);

                return false;
            }
            else
            {
                if (false == suppressUhOh)
                {
                    m_manager.uhOh(UhOhListener.UhOh.START_BLE_SCAN_FAILED__USING_CLASSIC);
                }
                setStateToScanning();

                return true;
            }
        }
        else
        {
            fail();

            m_manager.uhOh(UhOhListener.UhOh.START_BLE_SCAN_FAILED);

            return false;
        }
    }

    private boolean startLeScan()
    {
        return m_manager.managerLayer().startLeScan(getPreLScanCallback());
    }

    private void startLScan(int mode)
    {
        m_manager.managerLayer().startLScan(mode, getReportDelay(), getPostLCallback());
    }

    private void startMScan(int mode, int matchMode, int matchNum)
    {
        m_manager.managerLayer().startMScan(mode, matchMode, matchNum, getReportDelay(), getPostLCallback());
    }

    private boolean startPendingIntentScan(int mode, int matchMode, int matchNum, PendingIntent pendingIntent)
    {
        return m_manager.managerLayer().startPendingIntentScan(mode, matchMode, matchNum, getReportDelay(), pendingIntent);
    }

    private Interval getReportDelay()
    {
        Interval delay = m_manager.getConfigClone().scanReportDelay;
        if (Build.MODEL.toLowerCase(Locale.US).contains("pixel"))
            delay = Interval.ZERO;
        return delay;
    }

    private void fail()
    {
        m_manager.getTaskManager().fail(P_Task_Scan.class, m_manager);
    }

    private void stopScanPreLollipop()
    {
        try
        {
            stopLeScan();
        } catch (Exception e)
        {
            m_manager.getLogger().e("Got an exception (" + e.getClass().getSimpleName() + ") with a message of " + e.getMessage() + " when trying to stop a pre-lollipop scan!");
        }
    }

    private void stopLeScan()
    {
        doStopScan();
    }

    private void stopScanPostLollipop()
    {
        doStopScan();
    }

    // This needs to be public as it's called directly from the BleManager instance. This is due to the way PendingIntent scans work (there may not be
    // a scan task in the queue).
    public final void stopPendingIntentScan(PendingIntent pendingIntent)
    {
        m_manager.managerLayer().stopPendingIntentScan(pendingIntent);
    }

    private void doStopScan()
    {
        // Splitting between pre/post lollipop is done at the native layer in AndroidBluetoothManager
        m_manager.managerLayer().stopLeScan(getPreLScanCallback());
    }

    private BluetoothAdapter.LeScanCallback getPreLScanCallback()
    {
        return m_manager.getNativeManager().getListenerProcessor().getInternalListener().getPreLollipopCallback();
    }

    private L_Util.ScanCallback getPostLCallback()
    {
        return m_manager.getNativeManager().getListenerProcessor().getInternalListener().getPostLollipopCallback();
    }

    private void stopClassicDiscovery()
    {
        m_manager.managerLayer().cancelDiscovery();
    }


    // Class used to store relevant info regarding device discovery. This is only used to pass info into the BleManager, then gets tossed.
    static final class DiscoveryEntry
    {
        private final IBluetoothDevice deviceLayer;
        private final int rssi;
        private final byte[] scanRecord;

        IBleDevice m_bleDevice;
        BleDeviceOrigin m_origin;
        ScanFilter.ScanEvent m_scanEvent;
        boolean m_newlyDiscovered;
        boolean m_stopScan;


        DiscoveryEntry(IBluetoothDevice layer, int rssi, byte[] record)
        {
            deviceLayer = layer;
            this.rssi = rssi;
            scanRecord = record;
        }

        IBluetoothDevice device()
        {
            return deviceLayer;
        }

        int rssi()
        {
            return rssi;
        }

        byte[] record()
        {
            return scanRecord;
        }

        static DiscoveryEntry newEntry(IBluetoothDevice layer, int rssi, byte[] record)
        {
            return new DiscoveryEntry(layer, rssi, record);
        }
    }

    // Class used to temporarily hold scan information when devices first get discovered via a scan. A lot can come in at one time, or very quickly, so we preserve the info
    // and process in the update loop.
    private final static class ScanInfo
    {
        private final P_DeviceHolder m_device;
        private final int m_rssi;
        private final byte[] m_record;

        ScanInfo(P_DeviceHolder device, int rssi, byte[] record)
        {
            m_device = device;
            m_rssi = rssi;
            m_record = record;
        }

        @Override
        public int hashCode()
        {
            if (m_device != null)
            {
                return m_device.getAddress().hashCode();
            }
            return super.hashCode();
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == null)
                return false;
            if (obj instanceof ScanInfo)
            {
                ScanInfo other = (ScanInfo) obj;
                if ((m_device == null && other.m_device != null) || (m_device != null && other.m_device == null))
                    return false;
                return m_device != null && m_device.getAddress().equals(other.m_device.getAddress());
            }
            return false;
        }
    }


}
