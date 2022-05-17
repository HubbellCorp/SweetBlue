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


import android.app.PendingIntent;
import android.content.Intent;

import com.idevicesinc.sweetblue.annotations.Advanced;
import com.idevicesinc.sweetblue.utils.Interval;

/**
 * Class used to feed options for scanning via {@link BleManager#startScan(ScanOptions)}.
 */
public final class ScanOptions
{

    /**
     * Match mode for discovering devices. Aggressive will match devices quicker for discovery,
     * whereas Sticky requires higher signal strength, and more occurrences in the scan before
     * showing up in discovery.
     *
     * Please be aware that this will only be used on devices running Marshmallow or higher (it will
     * be ignored otherwise)
     *
     * @see <a href="https://developer.android.com/reference/android/bluetooth/le/ScanSettings#MATCH_MODE_AGGRESSIVE">
     *     https://developer.android.com/reference/android/bluetooth/le/ScanSettings#MATCH_MODE_AGGRESSIVE"</a>
     */
    public enum MatchMode
    {
        /**
         * In Aggressive mode, hw will determine a match sooner even with feeble signal strength and few number of sightings/match in a duration.
         * This is the default setting.
         */
        AGGRESSIVE(1), // ScanSettings.MATCH_MODE_AGGRESSIVE = 1

        /**
         * For sticky mode, higher threshold of signal strength and sightings is required before reporting by hw
         */
        STICKY(2); // ScanSettings.MATCH_MODE_STICKY = 2

        public final int internalAndroidValue;

        MatchMode(int value)
        {
            this.internalAndroidValue = value;
        }
    }

    public enum MatchNumber
    {
        /**
         * Match few advertisement per filter, depends on current capability and availability of the resources in hw
         */
        FEW_ADVERTISEMENT(2),

        /**
         * Match as many advertisement per filter as hw could allow, depends on current capability and availability of the resources in hw
         * This is the default setting.
         */
        MAX_ADVERTISEMENT(3),

        /**
         * Match one advertisement per filter
         */
        ONE_ADVERTISEMENT(1);

        public final int internalAndroidValue;

        MatchNumber(int androidValue)
        {
            internalAndroidValue = androidValue;
        }
    }

    Interval m_scanTime = Interval.INFINITE;
    Interval m_pauseTime;
    ScanFilter m_scanFilter;
    ScanFilter.ApplyMode m_scanFilterApplyMode = ScanFilter.ApplyMode.CombineEither;
    DiscoveryListener m_discoveryListener;
    PendingIntent m_pendingIntent = null;
    MatchMode m_matchMode = MatchMode.AGGRESSIVE;
    MatchNumber m_matchNumber = MatchNumber.MAX_ADVERTISEMENT;
    boolean m_isPeriodic;
    boolean m_isPriorityScan;
    boolean m_forceIndefinite;


    public ScanOptions()
    {}

    public ScanOptions(ScanFilter scanFilter)
    {
        m_scanFilter = scanFilter;
    }

    public ScanOptions(DiscoveryListener listener_nullable)
    {
        m_discoveryListener = listener_nullable;
    }

    public ScanOptions(ScanFilter scanFilter, DiscoveryListener listener_nullable)
    {
        m_scanFilter = scanFilter;
        m_discoveryListener = listener_nullable;
    }


    /**
     * Scan indefinitely until {@link BleManager#stopScan()} is called. If this is called after
     * {@link #scanPeriodically(Interval, Interval)}, this will override the periodic scan.
     */
    public final ScanOptions scanInfinitely()
    {
        return scanFor(Interval.INFINITE);
    }

    /**
     * Scan for the specified amount of time. This method implies a one-time scan. If you want to
     * perform a periodic scan, then use {@link #scanPeriodically(Interval, Interval)} instead.
     *
     * If this is called after {@link #scanPeriodically(Interval, Interval)}, it will override the periodic scan.
     */
    public final ScanOptions scanFor(Interval time)
    {
        m_isPeriodic = false;
        m_pauseTime = null;
        m_scanTime = time.secs() < 0.0 ? Interval.INFINITE : time;
        return this;
    }

    /**
     * Force a indefinite scan. If you choose to scan indefinitely, and don't set this, SweetBlue will automatically pause the scan, and resume it shortly
     * thereafter, to make sure scan results keep coming in as expected. If you pass in <code>true</code> here, the scan will just run until you call
     * {@link BleManager#stopScan()}. There's really no reason to do this, but it's left in here to be flexible.
     */
    @Advanced
    public final ScanOptions forceIndefinite(boolean force)
    {
        m_forceIndefinite = force;
        return this;
    }

    /**
     * Do a periodic scan. If you want to do a one-time scan, then use {@link #scanFor(Interval)}
     * instead.
     */
    public final ScanOptions scanPeriodically(Interval scanTime, Interval pauseTime)
    {
        m_isPeriodic = true;
        m_scanTime = scanTime;
        m_pauseTime = pauseTime;
        return this;
    }

    /**
     * Set a {@link com.idevicesinc.sweetblue.ScanFilter} for this scan.
     */
    public final ScanOptions withScanFilter(ScanFilter filter)
    {
        m_scanFilter = filter;
        return this;
    }

    /**
     * Set a {@link com.idevicesinc.sweetblue.ScanFilter.ApplyMode} for this scan.
     */
    public final ScanOptions withScanFilterApplyMode(ScanFilter.ApplyMode applyMode)
    {
        m_scanFilterApplyMode = applyMode;
        return this;
    }

    /**
     * Set the {@link MatchMode} for this scan.
     */
    public final ScanOptions withMatchMode(MatchMode matchMode)
    {
        m_matchMode = matchMode;
        return this;
    }

    /**
     * Set the {@link MatchNumber} for this scan.
     */
    public final ScanOptions withMatchNumber(MatchNumber matchNumber)
    {
        m_matchNumber = matchNumber;
        return this;
    }

    /**
     * Set a {@link com.idevicesinc.sweetblue.DiscoveryListener} for this scan.
     */
    public final ScanOptions withDiscoveryListener(DiscoveryListener listener)
    {
        m_discoveryListener = listener;
        return this;
    }

    /**
     * NOTE: This will only work for devices running android 8 (Oreo) or higher (API level 26+). If a scan is started with this
     * option on a device running android 7 or lower, <b>a scan will not be started.</b> You must make sure to check the result
     * of {@link BleManager#startScan()} to know if a scan started.
     *
     * NOTE 2: {@link DiscoveryListener} is ignored when using this feature.
     *
     * Set a {@link PendingIntent} for the system to use as a callback to your application when
     * new devices are discovered. This is a special scan in that you will get back native
     * {@link android.bluetooth.BluetoothDevice} instances in your activity/receiver. It is recommended
     * to use {@link BleManager#getDevices(Intent)} to convert the native android bluetooth device instances
     * to {@link BleDevice} instances for you.
     *
     * To execute a scan properly, you need to create a PendingIntent like below:
     * <pre>
     * <code>
     *     PendingIntent pIntent = PendingIntent.getBroadcast(activity, 42, new Intent(activity, MyScanReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);
     * </code>
     * </pre>
     *
     * You also must remember to create a class which extends {@link android.content.BroadcastReceiver}, and this class must be
     * added to your AndroidManifest.xml file. eg:
     * <pre>
     *     <code>
     *          {@code <receiver android:name="com.myapp.MyScanReceiver" >
     *             <intent-filter>
     *                 <action android:name="com.myapp.ACTION_FOUND" />
     *             </intent-filter>
     *         </receiver> }
     *     </code>
     * </pre>
     */
    public final ScanOptions withPendingIntent(PendingIntent pendingIntent)
    {
        m_pendingIntent = pendingIntent;
        return this;
    }

    /**
     * This will set the scan to be of the highest priority. This should ONLY be used if you absolutely
     * need it! With this active, ONLY scanning will happen (even if you call connect on a device, or
     * read/write, etc), until you call {@link BleManager#stopScan()}.
     */
    @Advanced
    public final ScanOptions asHighPriority(boolean highPriority)
    {
        m_isPriorityScan = highPriority;
        return this;
    }


    public final Interval getScanTime()
    {
        return m_scanTime;
    }

    public final Interval getPauseTime()
    {
        return m_pauseTime;
    }

    public final ScanFilter getScanFilter()
    {
        return m_scanFilter;
    }

    public final MatchMode getMatchMode()
    {
        return m_matchMode;
    }

    public final MatchNumber getMatchNumber()
    {
        return m_matchNumber;
    }

    public final ScanFilter.ApplyMode getApplyMode()
    {
        return m_scanFilterApplyMode;
    }

    public final DiscoveryListener getDiscoveryListener()
    {
        return m_discoveryListener;
    }

    public final PendingIntent getPendingIntent()
    {
        return m_pendingIntent;
    }

    public final boolean isPeriodic()
    {
        return m_isPeriodic;
    }

    public final boolean isPriorityScan()
    {
        return m_isPriorityScan;
    }

    public final boolean isForceIndefinite()
    {
        return m_forceIndefinite;
    }

    /**
     * Returns <code>true</code> if this instance is a periodic scan, or indefinite.
     */
    public boolean isContinuous()
    {
        return m_isPriorityScan || m_forceIndefinite || m_scanTime == Interval.INFINITE;
    }

}
