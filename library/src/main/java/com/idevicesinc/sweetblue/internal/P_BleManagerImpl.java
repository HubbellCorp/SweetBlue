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


import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import com.idevicesinc.sweetblue.AddServiceListener;
import com.idevicesinc.sweetblue.AdvertisingListener;
import com.idevicesinc.sweetblue.AssertListener;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDeviceConfig;
import com.idevicesinc.sweetblue.BleDeviceOrigin;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.BleManagerState;
import com.idevicesinc.sweetblue.BleNode;
import com.idevicesinc.sweetblue.BleScanApi;
import com.idevicesinc.sweetblue.BleServer;
import com.idevicesinc.sweetblue.BleServerState;
import com.idevicesinc.sweetblue.BleService;
import com.idevicesinc.sweetblue.BleStatuses;
import com.idevicesinc.sweetblue.BondListener;
import com.idevicesinc.sweetblue.DeviceConnectListener;
import com.idevicesinc.sweetblue.DeviceReconnectFilter;
import com.idevicesinc.sweetblue.DeviceStateListener;
import com.idevicesinc.sweetblue.DiscoveryListener;
import com.idevicesinc.sweetblue.HistoricalDataLoadListener;
import com.idevicesinc.sweetblue.IncomingListener;
import com.idevicesinc.sweetblue.LogOptions;
import com.idevicesinc.sweetblue.ManagerStateListener;
import com.idevicesinc.sweetblue.NotificationListener;
import com.idevicesinc.sweetblue.OutgoingListener;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.ReadWriteListener;
import com.idevicesinc.sweetblue.ReconnectFilter;
import com.idevicesinc.sweetblue.ResetListener;
import com.idevicesinc.sweetblue.ScanFilter;
import com.idevicesinc.sweetblue.ScanOptions;
import com.idevicesinc.sweetblue.ServerConnectListener;
import com.idevicesinc.sweetblue.ServerReconnectFilter;
import com.idevicesinc.sweetblue.ServerStateListener;
import com.idevicesinc.sweetblue.UhOhListener;
import com.idevicesinc.sweetblue.annotations.Advanced;
import com.idevicesinc.sweetblue.annotations.Experimental;
import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.backend.historical.Backend_HistoricalDatabase;
import com.idevicesinc.sweetblue.compat.PermissionsCompat;
import com.idevicesinc.sweetblue.compat.L_Util;
import com.idevicesinc.sweetblue.compat.S_Util;
import com.idevicesinc.sweetblue.internal.android.IDeviceListener;
import com.idevicesinc.sweetblue.internal.android.IManagerListener;
import com.idevicesinc.sweetblue.internal.android.IServerListener;
import com.idevicesinc.sweetblue.internal.android.IBluetoothDevice;
import com.idevicesinc.sweetblue.internal.android.IBluetoothManager;
import com.idevicesinc.sweetblue.internal.android.P_DeviceHolder;
import com.idevicesinc.sweetblue.utils.EpochTime;
import com.idevicesinc.sweetblue.utils.Event;
import com.idevicesinc.sweetblue.utils.ForEach_Breakable;
import com.idevicesinc.sweetblue.utils.ForEach_Void;
import com.idevicesinc.sweetblue.utils.GattDatabase;
import com.idevicesinc.sweetblue.utils.GenericListener_Void;
import com.idevicesinc.sweetblue.utils.HistoricalData;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.P_Const;
import com.idevicesinc.sweetblue.utils.State;
import com.idevicesinc.sweetblue.utils.TimeTracker;
import com.idevicesinc.sweetblue.internal.PA_StateTracker.E_Intent;
import com.idevicesinc.sweetblue.utils.UpdateThreadType;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.Utils_ScanRecord;
import com.idevicesinc.sweetblue.utils.Utils_String;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import static com.idevicesinc.sweetblue.BleManagerState.*;


public final class P_BleManagerImpl implements IBleManager
{

    private final static long UPDATE_LOOP_WARNING_DELAY = 10000;
    private static final String LOCATION_PERMISSION_NAMESPACE = "location_permission_namespace";
    private static final String LOCATION_PERMISSION_KEY = "location_permission_key";


    // This is to make it easy to return a BleDevice when working with IBleDevices
    private static Map<String, BleDevice> m_deviceMap;
    // This doesn't really need to be a map, as it only holds one entry now, but leaving it as a map in case
    // we ever figure out a way to have multiple
    private static Map<IBleServer, BleServer> m_serverMap;


    private final Context m_context;
    private UpdateRunnable m_updateRunnable;
    private final P_ScanFilterManager m_filterMngr;
    private final P_BluetoothCrashResolver m_crashResolver;
    private P_Logger m_logger;
    private BleManagerConfig m_config;
    private final P_DeviceManager m_deviceMngr;
    private final P_DeviceManager m_deviceMngr_cache;
    private final P_BleManagerNativeManager m_nativeManager;
    private final P_ManagerStateTracker m_stateTracker;
    private P_PostManager m_postManager;
    private P_ScanManager m_scanManager;
    private final P_TaskManager m_taskManager;
    private P_UhOhThrottler m_uhOhThrottler;
    private P_WakeLockManager m_wakeLockMngr;

    private HistoricalDataLoadListener m_historicalDataLoadListener;
    private DiscoveryListener m_discoveryListener;
    private DiscoveryListener m_ephemeralDiscoveryListener;
    private P_WrappingResetListener m_resetListeners;
    private AssertListener m_assertionListener;
    private DeviceStateListener m_defaultDeviceStateListener;
    private DeviceReconnectFilter m_defaultDeviceReconnectFilter;
    private ServerReconnectFilter m_defaultServerReconnectFilter;
    private ServerConnectListener m_defaultServerConnectFilter;
    private DeviceConnectListener m_defaultDeviceConnectListener;
    private BondListener m_defaultBondListener;
    private ReadWriteListener m_defaultReadWriteListener;
    private NotificationListener m_defaultNotificationListener;
    private final P_DiskOptionsManager m_diskOptionsMngr;
    private PA_Task.I_StateListener m_defaultTaskStateListener;

    private double m_timeForegrounded = 0.0;
    private long m_timeTurnedOn = 0;
    private long m_lastTaskExecution;
    private long m_currentTick;
    private boolean m_isForegrounded = false;
    private boolean m_ready = false;
    private long m_lastUpdateLoopWarning = 0;


    private ServerStateListener m_defaultServerStateListener;
    private OutgoingListener m_defaultServerOutgoingListener;
    private IncomingListener m_defaultServerIncomingListener;
    private AddServiceListener m_serviceAddListener;
    private AdvertisingListener m_advertisingListener;

    private final Backend_HistoricalDatabase m_historicalDatabase;

    private final IDeviceListener.Factory m_deviceListenerFactory;
    private final IServerListener.Factory m_serverListenerFactory;
    private final IManagerListener.Factory m_managerListenerFactory;

    private Application.ActivityLifecycleCallbacks m_activityCallbacks;

    IBleServer m_server = null;
    private final Semaphore m_shutdownSemaphore;




    P_BleManagerImpl(Context context, BleManagerConfig config)
    {
        // Hack to tell the manager the app is foregrounded, if the manager was created after the activity has already resumed.
        if (context instanceof Activity)
        {
            m_isForegrounded = ((Activity) context).hasWindowFocus();
        }

        m_deviceMap = new HashMap<>();
        m_serverMap = new HashMap<>();

        m_deviceListenerFactory = IDeviceListener.DEFAULT_FACTORY;
        m_serverListenerFactory = IServerListener.DEFAULT_FACTORY;
        m_managerListenerFactory = IManagerListener.DEFAULT_FACTORY;

        m_context = context.getApplicationContext();

        m_currentTick = System.currentTimeMillis();

        addLifecycleCallbacks();
        m_config = config.clone();

        // Start up the time tracker
        TimeTracker.createInstance(config.timeTrackerSetting);

        m_logger = new P_Logger(this, P_Const.debugThreadNames, m_config.uuidNameMaps, m_config.loggingOptions, m_config.logger);

        m_logger.e("Creating BleManager instance...");

        m_scanManager = new P_ScanManager(this);
        m_historicalDatabase = PU_HistoricalData.newDatabase(context, this);
        m_diskOptionsMngr = new P_DiskOptionsManager(this);
        m_filterMngr = new P_ScanFilterManager(this, m_config.defaultScanFilter);
        if (m_config.bluetoothManagerImplementation.isManagerNull())
        {
            m_config.bluetoothManagerImplementation.resetManager(m_context);
        }
        int nativeStateInt = m_config.bluetoothManagerImplementation.getState();
        BleManagerState nativeState = P_Bridge_User.getState(nativeStateInt);

        if (m_timeTurnedOn == 0 && nativeState.overlaps(BluetoothAdapter.STATE_ON))
        {
            m_timeTurnedOn = m_currentTick;
        }

        m_stateTracker = new P_ManagerStateTracker(this);
        m_stateTracker.append(nativeState, E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
        m_stateTracker.update_native(nativeStateInt);
        m_taskManager = new P_TaskManager(this);
        m_crashResolver = new P_BluetoothCrashResolver(m_context);
        m_deviceMngr = new P_DeviceManager(this);
        m_deviceMngr_cache = new P_DeviceManager(this);

        m_lastTaskExecution = System.currentTimeMillis();

        m_nativeManager = new P_BleManagerNativeManager(this);

        initConfigDependentMembers();

        updateLogger();

        if (!P_Bridge_User.isUnitTest(m_config))
        {
            m_postManager.postToMain(() -> m_postManager.postToUpdateThread(() -> m_logger.printBuildInfo()));
        }

        m_shutdownSemaphore = new Semaphore(0);

    }


    public final void setConfig(@Nullable(Nullable.Prevalence.RARE) BleManagerConfig config_nullable)
    {
        m_config = config_nullable != null ? config_nullable.clone() : new BleManagerConfig();
        updateTimeTracker();
        updateLogger();
        initConfigDependentMembers();
    }

    public final BleManagerConfig getConfigClone()
    {
        return m_config.clone();
    }

    /**
     * Returns whether the manager is in any of the provided states.
     */
    public final boolean isAny(BleManagerState... states)
    {
        for (int i = 0; i < states.length; i++)
        {
            if (is(states[i])) return true;
        }

        return false;
    }

    /**
     * Returns whether the manager is in all of the provided states.
     *
     * @see #isAny(BleManagerState...)
     */
    public final boolean isAll(BleManagerState... states)
    {
        for (int i = 0; i < states.length; i++)
        {
            if (!is(states[i])) return false;
        }

        return true;
    }

    /**
     * Returns whether the manager is in the provided state.
     *
     * @see #isAny(BleManagerState...)
     */
    public final boolean is(final BleManagerState state)
    {
        return state.overlaps(getStateMask());
    }

    /**
     * Returns <code>true</code> if there is partial bitwise overlap between the provided value and {@link #getStateMask()}.
     *
     * @see #isAll(int)
     */
    public final boolean isAny(final int mask_BleManagerState)
    {
        return (getStateMask() & mask_BleManagerState) != 0x0;
    }

    /**
     * Returns <code>true</code> if there is complete bitwise overlap between the provided value and {@link #getStateMask()}.
     *
     * @see #isAny(int)
     */
    public final boolean isAll(final int mask_BleManagerState)
    {
        return (getStateMask() & mask_BleManagerState) == mask_BleManagerState;
    }

    /**
     * See similar comment for {@link BleDevice#getTimeInState(BleDeviceState)}.
     *
     * @see BleDevice#getTimeInState(BleDeviceState)
     */
    public final Interval getTimeInState(BleManagerState state)
    {
        return Interval.millis(m_stateTracker.getTimeInState(state.ordinal()));
    }

    /**
     * Checks the underlying stack to see if BLE is supported on the phone.
     */
    public final boolean isBleSupported()
    {
        PackageManager pm = m_context.getPackageManager();
        boolean hasBLE = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);

        return hasBLE;
    }

    /**
     * Checks to see if the device is running an Android OS which supports
     * advertising.
     */
    public final boolean isAdvertisingSupportedByAndroidVersion()
    {
        return Utils.isLollipop();
    }

    /**
     * Checks to see if the device supports advertising.
     */
    public final boolean isAdvertisingSupportedByChipset()
    {
        if (isAdvertisingSupportedByAndroidVersion())
        {
            return managerLayer().isMultipleAdvertisementSupported();
        }
        else
        {
            return false;
        }
    }

    /**
     * Checks to see if the device supports advertising BLE services.
     */
    public final boolean isAdvertisingSupported()
    {
        return isAdvertisingSupportedByAndroidVersion() && isAdvertisingSupportedByChipset();
    }

    public final boolean isBluetooth5SupportedByAndroidVersion()
    {
        return Utils.isOreo();
    }

    /**
     * Returns <code>true</code> if the android device is running Oreo (8.0), and if the hardware supports Bluetooth 5's long range
     * feature.
     */
    public final boolean isBluetooth5LongRangeSupported()
    {
        return Utils.isOreo() && managerLayer().isBluetooth5LongRangeSupported();
    }

    /**
     * Returns <code>true</code> if the android device is running Oreo (8.0), and if the hardware supports Bluetooth 5's high speed feature.
     */
    public final boolean isBluetooth5HighSpeedSupported()
    {
        return Utils.isOreo() && managerLayer().isBluetooth5HighSpeedSupported();
    }

    /**
     * Disables BLE if manager is {@link BleManagerState#ON}. This disconnects all current
     * connections, stops scanning, and forgets all discovered devices.
     */
    public final void turnOff()
    {
        turnOff_private(false);
    }

    /**
     * Returns the native manager.
     */
    @Advanced
    public final BluetoothManager getNative()
    {
        return managerLayer().getNativeManager();
    }

    /**
     * Returns the native bluetooth adapter.
     */
    @Advanced
    public final BluetoothAdapter getNativeAdapter()
    {
        return managerLayer().getNativeAdaptor();
    }

    /**
     * Sets a default backup {@link HistoricalDataLoadListener} that will be invoked
     * for all historical data loads to memory for all uuids for all devices.
     */
    public final void setListener_HistoricalDataLoad(@Nullable(Nullable.Prevalence.NORMAL) final HistoricalDataLoadListener listener_nullable)
    {
        m_historicalDataLoadListener = listener_nullable;
    }

    public final void setListener_UhOh(@Nullable(Nullable.Prevalence.NORMAL) UhOhListener listener_nullable)
    {
        m_uhOhThrottler.setListener(listener_nullable);
    }

    /**
     * Set a listener here to be notified whenever {@link #ASSERT(boolean)} fails.
     * Mostly for use by internal library developers.
     */
    public final void setListener_Assert(@Nullable(Nullable.Prevalence.NORMAL) AssertListener listener_nullable)
    {
        m_assertionListener = listener_nullable;
    }

    /**
     * Set a listener here to be notified whenever a {@link BleDevice} is discovered, rediscovered, or undiscovered.
     */
    public final void setListener_Discovery(@Nullable(Nullable.Prevalence.NORMAL) DiscoveryListener listener_nullable)
    {
        m_discoveryListener = listener_nullable;
    }

    /**
     * Returns the discovery listener set with {@link #setListener_Discovery(DiscoveryListener)} or
     * {@link BleManagerConfig#defaultDiscoveryListener}, or <code>null</code> if not set.
     */
    public final DiscoveryListener getListener_Discovery()
    {
        return m_discoveryListener;
    }

    /**
     * Set a listener here to be notified whenever this manager's {@link BleManagerState} changes.
     */
    public final void setListener_State(@Nullable(Nullable.Prevalence.NORMAL) ManagerStateListener listener_nullable)
    {
        m_stateTracker.setListener(listener_nullable);
    }

    /**
     * Convenience method to listen for all changes in {@link BleDeviceState} for all devices.
     * The listener provided will get called in addition to and after the listener, if any, provided
     * to {@link BleDevice#setListener_State(DeviceStateListener)}.
     *
     * @see BleDevice#setListener_State(DeviceStateListener)
     */
    public final void setListener_DeviceState(@Nullable(Nullable.Prevalence.NORMAL) DeviceStateListener listener_nullable)
    {
        m_defaultDeviceStateListener = listener_nullable;
    }

    /**
     * Convenience method to handle server connection fail events at the manager level. The listener provided
     * will only get called if the server whose connection failed doesn't have a listener provided to
     * {@link BleServer#setListener_ReconnectFilter(ServerReconnectFilter)}. This is unlike the behavior
     * behind (for example) {@link #setListener_ServerState(ServerStateListener)} because
     * {@link ServerReconnectFilter#onConnectFailed(ReconnectFilter.ConnectFailEvent)} requires a return value.
     *
     * @see BleServer#setListener_ReconnectFilter(ServerReconnectFilter)
     */
    public final void setListener_ServerReconnectFilter(@Nullable(Nullable.Prevalence.NORMAL) ServerReconnectFilter listener_nullable)
    {
        m_defaultServerReconnectFilter = listener_nullable;
    }

    /**
     * Convenience method to listener for all server connections. This listener is somewhat pointless, as you can only have
     * one {@link BleServer}. However, in the case we extend that in the future, this will be here.
     */
    public final void setListener_ServerConnect(@Nullable(Nullable.Prevalence.NORMAL) ServerConnectListener listener_nullable)
    {
        m_defaultServerConnectFilter = listener_nullable;
    }

    /**
     * Convenience method to handle server request events at the manager level. The listener provided
     * will only get called if the server receiving a request doesn't have a listener provided to
     * {@link BleServer#setListener_Incoming(IncomingListener)} . This is unlike the behavior (for example)
     * behind {@link #setListener_Outgoing(OutgoingListener)} because
     * {@link IncomingListener#onEvent(IncomingListener.IncomingEvent)} requires a return value.
     *
     * @see BleServer#setListener_Incoming(IncomingListener)
     */
    public final void setListener_Incoming(@Nullable(Nullable.Prevalence.NORMAL) IncomingListener listener_nullable)
    {
        m_defaultServerIncomingListener = listener_nullable;
    }

    /**
     * Convenience method to listen for all service addition events for all servers.
     * The listener provided will get called in addition to and after the listener, if any, provided
     * to {@link BleServer#setListener_ServiceAdd(AddServiceListener)}.
     *
     * @see BleServer#setListener_ServiceAdd(AddServiceListener)
     */
    public final void setListener_ServiceAdd(@Nullable(Nullable.Prevalence.NORMAL) AddServiceListener listener_nullable)
    {
        m_serviceAddListener = listener_nullable;
    }

    /**
     * Convenience method to listen for all changes in {@link BleServerState} for all servers.
     * The listener provided will get called in addition to and after the listener, if any, provided
     * to {@link BleServer#setListener_State(ServerStateListener)}.
     *
     * @see BleServer#setListener_State(ServerStateListener)
     */
    public final void setListener_ServerState(@Nullable(Nullable.Prevalence.NORMAL) ServerStateListener listener_nullable)
    {
        m_defaultServerStateListener = listener_nullable;
    }

    /**
     * Convenience method to listen for completion of all outgoing messages from
     * {@link BleServer} instances. The listener provided will get called in addition to and after the listener, if any, provided
     * to {@link BleServer#setListener_Outgoing(OutgoingListener)}.
     *
     * @see BleServer#setListener_Outgoing(OutgoingListener)
     */
    public final void setListener_Outgoing(@Nullable(Nullable.Prevalence.NORMAL) OutgoingListener listener_nullable)
    {
        m_defaultServerOutgoingListener = listener_nullable;
    }

    /**
     * Convenience method to handle connection fail events at the manager level. You should be aware that if there is a listener
     * set via {@link BleDevice#setListener_Reconnect(DeviceReconnectFilter)}, the return value of that one will be used
     * internally by SweetBlue for that {@link BleDevice} (but the event will still be pumped to the listener provided here).
     *
     * @see BleDevice#setListener_Reconnect(DeviceReconnectFilter)
     */
    public final void setListener_DeviceReconnect(@Nullable(Nullable.Prevalence.NORMAL) DeviceReconnectFilter listener_nullable)
    {
        m_defaultDeviceReconnectFilter = listener_nullable;
    }

    public final void setListener_DeviceConnect(@Nullable(Nullable.Prevalence.NORMAL) DeviceConnectListener listener_nullable)
    {
        m_defaultDeviceConnectListener = listener_nullable;
    }

    /**
     * Convenience method to set a default back up listener for all {@link com.idevicesinc.sweetblue.BondListener.BondEvent}s across all {@link BleDevice} instances.
     */
    public final void setListener_Bond(@Nullable(Nullable.Prevalence.NORMAL) BondListener listener_nullable)
    {
        m_defaultBondListener = listener_nullable;
    }

    /**
     * Sets a default backup {@link ReadWriteListener} that will be called for all {@link BleDevice} instances.
     * <br><br>
     * TIP: Place some analytics code in the listener here.
     */
    public final void setListener_Read_Write(@Nullable(Nullable.Prevalence.NORMAL) ReadWriteListener listener_nullable)
    {
        m_defaultReadWriteListener = listener_nullable;
    }

    public final void setListener_Notification(@Nullable(Nullable.Prevalence.NORMAL) NotificationListener listener_nullable)
    {
        m_defaultNotificationListener = listener_nullable;
    }

    /**
     * Set a listener here to be notified of the result of starting to advertise.
     */
    public final void setListener_Advertising(AdvertisingListener listener)
    {
        m_advertisingListener = listener;
    }

    public final void setListener_TaskState(PA_Task.I_StateListener listener)
    {
        m_defaultTaskStateListener = listener;
    }

    public final PA_Task.I_StateListener getDefaultTaskStateListener()
    {
        return m_defaultTaskStateListener;
    }

    public final boolean startScan(ScanOptions options)
    {
        showScanWarningIfNeeded();

        return startScan_private(options);
    }

    /**
     * Requires the {@link android.Manifest.permission#WAKE_LOCK} permission. Gives you access to the internal
     * wake lock as a convenience and eventually calls {@link android.os.PowerManager.WakeLock#acquire()}.
     *
     * @see BleManagerConfig#manageCpuWakeLock
     */
    @Advanced
    public final void pushWakeLock()
    {
        m_wakeLockMngr.push();
    }

    /**
     * Opposite of {@link #pushWakeLock()}, eventually calls {@link android.os.PowerManager.WakeLock#release()}.
     */
    @Advanced
    public final void popWakeLock()
    {
        m_wakeLockMngr.pop();
    }

    /**
     * Fires a callback to {@link AssertListener} if condition is false. Will post a {@link android.util.Log#ERROR}-level
     * message with a stack trace to the console as well if {@link BleManagerConfig#loggingOptions} is not {@link LogOptions#OFF}.
     */
    @Advanced
    public final boolean ASSERT(boolean condition)
    {
        return ASSERT(condition, "");
    }

    /**
     * Same as {@link #ASSERT(boolean)} but with an added message.
     */
    @Advanced
    public final boolean ASSERT(boolean condition, String message)
    {
        if (!condition)
        {
            Exception dummyException = null;
            message = message != null ? message : "";

            if (m_logger.isEnabled() || m_assertionListener != null)
            {
                dummyException = new Exception();
            }

            if (m_logger.isEnabled())
            {
                Log.e(BleManager.class.getSimpleName(), "ASSERTION FAILED " + message, dummyException);
            }

            if (m_assertionListener != null)
            {
                final AssertListener.AssertEvent event = P_Bridge_User.newAssertEvent(this, message, dummyException.getStackTrace());

                postEvent(m_assertionListener, event);
            }

            return false;
        }

        return true;
    }

    /**
     * Returns the abstracted bitwise state mask representation of {@link BleManagerState} for the manager instance.
     *
     * @see BleManagerState
     */
    public final int getStateMask()
    {
        return m_stateTracker.getState();
    }

    /**
     * Enables BLE if manager is currently {@link BleManagerState#OFF} or {@link BleManagerState#TURNING_OFF}, otherwise does nothing.
     * For a convenient way to ask your user first see {@link #turnOnWithIntent(android.app.Activity, int)}.
     */
    public final void turnOn()
    {
        if (isAny(TURNING_ON, ON)) return;

        if (is(OFF))
        {
            m_stateTracker.update(E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, TURNING_ON, true, OFF, false);
        }

        m_taskManager.add(new P_Task_TurnBleOn(this, /*implicit=*/false));
        if (m_timeTurnedOn == 0)
        {
            m_timeTurnedOn = m_currentTick;
        }
    }

    /**
     * This is essentially a big red reset button for the Bluetooth stack. Use it ruthlessly
     * when the stack seems to be acting up, like when you can't connect to a device that you should be
     * able to connect to. It's similar to calling {@link #turnOff()} then {@link #turnOn()},
     * but also does other things like removing all bonds (similar to {@link #unbondAll()}) and
     * other "special sauce" such that you should use this method instead of trying to reset the
     * stack manually with component calls.
     * <br><br>
     * It's good app etiquette to first prompt the user to get permission to reset because
     * it will affect Bluetooth system-wide and in other apps.
     *
     * @see BleManagerState#RESETTING
     */
    public final void reset()
    {
        reset(null);
    }

    /**
     * Same as {@link #reset()} but with a convenience callback for when the reset is
     * completed and the native BLE stack is (should be) back to normal.
     *
     * @see BleManagerState#RESETTING
     */
    public final void reset(ResetListener listener)
    {
        reset_private(false, listener);
    }

    /**
     * Similar to {@link BleManager#reset()}, only this also calls the factoryReset method hidden in {@link BluetoothAdapter} after turning
     * off BLE, and running the crash resolver. It's not clear what this method does, hence why this is marked as being experimental.
     *
     * @see #reset()
     */
    @Experimental
    public final void nukeBle()
    {
        nukeBle(null);
    }

    /**
     * Similar to {@link BleManager#reset(ResetListener)}, only this also calls the factoryReset method hidden in {@link BluetoothAdapter} after turning
     * off BLE, and running the crash resolver. It's not clear what this method does, hence why this is marked as being experimental.
     *
     * @see #reset(ResetListener)
     */
    @Experimental
    public final void nukeBle(ResetListener resetListener)
    {
        reset_private(true, resetListener);
    }

    /**
     * Removes bonds for all devices that are {@link BleDeviceState#BONDED}.
     * Essentially a convenience method for calling {@link BleDevice#unbond()},
     * on each device individually.
     */
    public final void unbondAll()
    {
        m_deviceMngr.unbondAll(null, BondListener.Status.CANCELLED_FROM_UNBOND);
    }

    /**
     * Disconnects all devices that are {@link BleDeviceState#BLE_CONNECTED}.
     * Essentially a convenience method for calling {@link BleDevice#disconnect()},
     * on each device individually.
     */
    public final void disconnectAll()
    {
        m_deviceMngr.disconnectAll();
    }

    /**
     * Same as {@link #disconnectAll()} but drills down to {@link BleDevice#disconnect_remote()} instead.
     */
    public final void disconnectAll_remote()
    {
        m_deviceMngr.disconnectAll_remote();
    }

    /**
     * Undiscovers all devices that are {@link BleDeviceState#DISCOVERED}.
     * Essentially a convenience method for calling {@link BleDevice#undiscover()},
     * on each device individually.
     */
    public final void undiscoverAll()
    {
        m_deviceMngr.undiscoverAll();
    }

    public final void turnOnLocationWithIntent_forOsServices(final Activity callingActivity, int requestCode)
    {
        final Intent enableLocationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);

        callingActivity.startActivityForResult(enableLocationIntent, requestCode);
    }

    /**
     * Overload of {@link #turnOnLocationWithIntent_forOsServices(Activity, int)} if you don't care about result.
     *
     * @see com.idevicesinc.sweetblue.utils.BluetoothEnabler
     */
    public final void turnOnLocationWithIntent_forOsServices(final Activity callingActivity)
    {
        final Intent enableLocationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);

        callingActivity.startActivity(enableLocationIntent);

        if (false == Utils.isMarshmallow())
        {
            m_logger.w("You may use this method but since the phone is at " + Build.VERSION.SDK_INT + " and the requirement is " + Build.VERSION_CODES.M + ", it is not necessary for scanning.");
        }
    }

    /**
     * Returns <code>true</code> if {@link #turnOnLocationWithIntent_forPermissions(Activity, int)} will pop a system dialog, <code>false</code> if it will bring
     * you to the OS's Application Settings. The <code>true</code> case happens if the app has never shown a request Location Permissions dialog or has shown a request Location Permission dialog and the user has yet to select "Never ask again". This method is used to weed out the false
     * negative from {@link Activity#shouldShowRequestPermissionRationale(String)} when the Location Permission has never been requested. Make sure to use this in conjunction with {@link #isLocationEnabledForScanning_byRuntimePermissions()}
     * which will tell you if permissions are already enabled.
     *
     * @see com.idevicesinc.sweetblue.utils.BluetoothEnabler
     */
    public final boolean willLocationPermissionSystemDialogBeShown(Activity callingActivity)
    {
        if (Utils.isMarshmallow())
        {
            SharedPreferences preferences = callingActivity.getSharedPreferences(LOCATION_PERMISSION_NAMESPACE, Context.MODE_PRIVATE);
            boolean hasNeverAskAgainBeenSelected = !PermissionsCompat.shouldShowRequestPermissionRationale(callingActivity);//Call only returns true if Location permission has been previously denied. Returns false if "Never ask again" has been selected
            boolean hasLocationPermissionSystemDialogShownOnce = preferences.getBoolean(LOCATION_PERMISSION_KEY, false);

            return (!hasLocationPermissionSystemDialogShownOnce) || (hasLocationPermissionSystemDialogShownOnce && !hasNeverAskAgainBeenSelected);
        }
        else
        {
            return false;
        }
    }

    /**
     * If {@link #isLocationEnabledForScanning_byOsServices()} returns <code>false</code>, you can use this method to allow the user to enable location
     * through an OS intent. The result of the request (i.e. what the user chose) is passed back through {@link Activity#onRequestPermissionsResult(int, String[], int[])}
     * with the requestCode provided as the second parameter to this method. If the user selected "Never ask again" the function will open up the app settings screen where the
     * user can navigate to enable the permissions.
     *
     * @see #isLocationEnabledForScanning_byRuntimePermissions()
     * @see com.idevicesinc.sweetblue.utils.BluetoothEnabler
     */
    public final void turnOnLocationWithIntent_forPermissions(final Activity callingActivity, int requestCode)
    {
        if (Utils.isMarshmallow())
        {
            if (false == isLocationEnabledForScanning_byRuntimePermissions() && false == willLocationPermissionSystemDialogBeShown(callingActivity))
            {
                final Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                final Uri uri = Uri.fromParts("package", callingActivity.getPackageName(), null);
                intent.setData(uri);
                callingActivity.startActivityForResult(intent, requestCode);
            }
            else
            {
                final SharedPreferences.Editor editor = callingActivity.getSharedPreferences(LOCATION_PERMISSION_NAMESPACE, Context.MODE_PRIVATE).edit();
                editor.putBoolean(LOCATION_PERMISSION_KEY, true).commit();
                PermissionsCompat.requestPermissions(callingActivity, requestCode, getConfigClone().requestBackgroundOperation);
            }
        }
        else
        {
            m_logger.w("BleManager.turnOnLocationWithIntent_forPermissions() is only applicable for API levels 23 and above so this method does nothing.");
        }
    }

    /**
     * This method will only do anything on devices running Android 12 or higher. This just makes the initial
     * request for the necessary permissions depending on {@link BleManagerConfig#requestBackgroundOperation},
     * and {@link BleManagerConfig#requestAdvertisePermission}. You will have to handle the result yourself, if
     * you call this method. This method is used by the {@link com.idevicesinc.sweetblue.utils.BleSetupHelper},
     * so if you are using that class, you don't need to call this method at all.
     * @see com.idevicesinc.sweetblue.utils.BleSetupHelper
     * @see <a href="https://developer.android.com/guide/topics/connectivity/bluetooth/permissions"></a>
     */
    public final void requestBluetoothPermissions(final Activity callingActivity, int requestCode)
    {
        if (Utils.isAndroid12()) {
            BleManagerConfig cfg = getConfigClone();
            S_Util.requestPermissions(callingActivity, requestCode, cfg.requestBackgroundOperation, cfg.requestAdvertisePermission);
        }
        else
        {
            m_logger.w("BleManager.requestBluetoothPermissions() is only applicable for API levels 31 and above, so this method does nothing.");
        }
    }

    public final boolean isScanningReady()
    {
        boolean ready = true;
        if (Utils.isAndroid12()) {
            // check for location as well, if doNotRequestLocation is false
            ready = getConfigClone().doNotRequestLocation ?
                    areBluetoothPermissionsEnabled() :
                    areBluetoothPermissionsEnabled() && isLocationEnabledForScanning();
        } else if (Utils.isMarshmallow()) {
            ready = isLocationEnabledForScanning();
        }
        return ready && is(ON);
    }

    /**
     * Convenience method which reports <code>true</code> if the {@link BleManager} is in any of the following states: <br><br>
     * {@link BleManagerState#SCANNING}, {@link BleManagerState#SCANNING_PAUSED}, {@link BleManagerState#BOOST_SCANNING}, or {@link BleManagerState#STARTING_SCAN}
     */
    public final boolean isScanning()
    {
        return isAny(SCANNING, SCANNING_PAUSED, BOOST_SCANNING, STARTING_SCAN);
    }

    public final boolean isLocationEnabledForScanning()
    {
        return managerLayer().isLocationEnabledForScanning();
    }

    public final boolean isLocationEnabledForScanning_byManifestPermissions()
    {
        return Utils.isLocationEnabledForScanning_byManifestPermissions(getApplicationContext());
    }

    public final boolean isLocationEnabledForScanning_byRuntimePermissions()
    {
        return managerLayer().isLocationEnabledForScanning_byRuntimePermissions();
    }

    public final boolean areBluetoothPermissionsEnabled()
    {
        BleManagerConfig cfg = getConfigClone();
        return Utils.areBluetoothPermissionsGranted(getApplicationContext(), cfg.requestBackgroundOperation, cfg.requestAdvertisePermission);
    }

    public final boolean isLocationEnabledForScanning_byOsServices()
    {
        return managerLayer().isLocationEnabledForScanning_byOsServices();
    }

    /**
     * Convenience method to request your user to enable ble in a "standard" way
     * with an {@link android.content.Intent} instead of using {@link #turnOn()} directly.
     * Result will be posted as normal to {@link android.app.Activity#onActivityResult(int, int, Intent)}.
     * If current state is {@link BleManagerState#ON} or {@link BleManagerState#TURNING_ON}
     * this method early outs and does nothing.
     *
     * @see com.idevicesinc.sweetblue.utils.BluetoothEnabler
     */
    public final void turnOnWithIntent(Activity callingActivity, int requestCode)
    {
        if (isAny(ON, TURNING_ON)) return;

        final Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        callingActivity.startActivityForResult(enableBtIntent, requestCode);
    }

    /**
     * Opposite of {@link #onPause()}, to be called from your override of {@link android.app.Activity#onResume()} for each {@link android.app.Activity}
     * in your application. See comment for {@link #onPause()} for a similar explanation for why you should call this method.
     */
    public final void onResume()
    {
        m_isForegrounded = true;
        m_timeForegrounded = 0.0;

        m_scanManager.onResume();
    }

    /**
     * It's generally recommended to call this in your override of {@link android.app.Activity#onPause()} for each {@link android.app.Activity}
     * in your application. This doesn't do much for now, just a little bookkeeping and stops scan automatically if
     * {@link BleManagerConfig#stopScanOnPause} is <code>true</code>. Strictly speaking you don't *have* to call this method,
     * but another good reason is for future-proofing. Later releases of this library may do other more important things
     * in this method so it's good to have it being called just in case.
     */
    public final void onPause()
    {
        m_isForegrounded = false;
        m_timeForegrounded = 0.0;
        m_scanManager.onPause();
    }

    /**
     * Disconnects all devices, shuts down the BleManager, and it's backing thread, and unregisters any receivers that may be in use.
     * This also clears out it's static instance. This is meant to be called upon application exit. However, to use it again,
     * just call {@link BleManager#get(Context)}, or {@link BleManager#get(Context, BleManagerConfig)} again.
     *
     * @see BleManagerConfig#blockingShutdown
     */
    public final void shutdown()
    {
        m_logger.e("Received shutdown call, shutting down BleManager...");
        clearListeners();
        m_scanManager.stopScan();
        if (m_config.blockingShutdown)
        {
            // No sense to disconnect if there are no connected devices. Because of the semaphore now,
            // doing this when no devices are connected causes a lock up.
            // TODO - Look into why the lockup happens, and fix it, so we're safe to just try to disconnect without having to check for connected devices first
            if (getConnectedNativeDevices() > 0)
            {
                disconnectAll();
                // Add the shutdown task which should be executed after all disconnects. This needs to be posted to the update thread
                // to make sure it gets added after all the disconnect tasks from the call above
                m_postManager.postToUpdateThread(() -> m_taskManager.add(new P_Task_Shutdown(this, null)));


                // Hold the semaphore to ensure all devices are disconnected before returning from the method
                try
                {
                    m_shutdownSemaphore.acquire();
                } catch (InterruptedException e)
                {
                    e.printStackTrace();
                    // Probably not going to do anything here. If we're interrupted, then just let the rest
                    // continue, as this is typically used when shutting an app down.
                }
            }
        }
        else
        {
            disconnectAll();
        }

        clearQueue();
        m_uhOhThrottler.shutdown();
        m_updateRunnable.m_shutdown = true;
        ((Application) m_context).unregisterActivityLifecycleCallbacks(m_activityCallbacks);
        m_postManager.removeUpdateCallbacks(m_updateRunnable);
        m_postManager.quit();
        m_wakeLockMngr.clear();
        m_nativeManager.shutdown();
    }

    private int getConnectedNativeDevices()
    {
        return getDevices_List(BleDeviceState.BLE_CONNECTED).size();
    }

    public final void clearShutdownSemaphore()
    {
        m_shutdownSemaphore.release();
    }

    private void clearListeners()
    {
        m_defaultDeviceStateListener = null;
        m_stateTracker.setListener(null);
        m_historicalDataLoadListener = null;
        m_discoveryListener = null;
        m_ephemeralDiscoveryListener = null;
        m_resetListeners = null;
        m_assertionListener = null;
        m_defaultDeviceReconnectFilter = null;
        m_defaultServerReconnectFilter = null;
        m_defaultServerConnectFilter = null;
        m_defaultDeviceConnectListener = null;
        m_defaultBondListener = null;
        m_defaultReadWriteListener = null;
        m_defaultNotificationListener = null;
        m_defaultServerStateListener = null;
        m_defaultServerOutgoingListener = null;
        m_defaultServerIncomingListener = null;
        m_serviceAddListener = null;
        m_advertisingListener = null;
        m_uhOhThrottler.setListener(null);
        m_deviceMngr.clearDeviceListeners();
        if (m_server != null)
            m_server.clearListeners();
    }

    /**
     * Returns the {@link android.app.Application} provided to the constructor.
     */
    public final Context getApplicationContext()
    {
        return (Application) m_context;
    }

    public final void stopScan()
    {
        m_postManager.runOrPostToUpdateThread(() -> {
            if (!m_scanManager.isPeriodicScan())
                m_ephemeralDiscoveryListener = null;

            m_scanManager.resetOptions();
            m_scanManager.setInfiniteScan(false, false);

            stopScan_private(E_Intent.INTENTIONAL);
        });
    }

    public final void stopScan(ScanFilter filter)
    {
        m_filterMngr.clearEphemeralFilter();

        stopScan();
    }

    public final void stopScan(PendingIntent pendingIntent)
    {
        m_postManager.runOrPostToUpdateThread(() ->
        {
            m_scanManager.stopPendingIntentScan(pendingIntent);
            // Call normal stopScan to remove any scan task that may be in the queue
            stopScan();
        });
    }

    /**
     * Gets a known {@link BleDeviceState#DISCOVERED} device by MAC address, or {@link BleDevice#NULL} if there is no such device.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) IBleDevice getDevice(final String macAddress)
    {
        final String macAddress_normalized = normalizeMacAddress(macAddress);

        final IBleDevice device = m_deviceMngr.get(macAddress_normalized);

        if (device != null) return device;

        return P_BleDeviceImpl.NULL;
    }

    /**
     * Shortcut for checking if {@link #getDevice(String)} returns {@link BleDevice#NULL}.
     */
    public final boolean hasDevice(final String macAddress)
    {
        return !getDevice(macAddress).isNull();
    }

    /**
     * Calls {@link #hasDevice(String)}.
     */
    public final boolean hasDevice(final IBleDevice device)
    {
        return hasDevice(device.getMacAddress());
    }

    /**
     * Returns the first device that is in the given state, or {@link BleDevice#NULL} if no match is found.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) IBleDevice getDevice(BleDeviceState state)
    {
        return m_deviceMngr.getDevice(state);
    }

    /**
     * Returns the first device that matches the query, or {@link BleDevice#NULL} if no match is found.
     * See {@link BleDevice#is(Object...)} for the query format.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) IBleDevice getDevice(Object... query)
    {
        return m_deviceMngr.getDevice(query);
    }

    /**
     * Returns true if we have a device that matches the given query.
     * See {@link BleDevice#is(Object...)} for the query format.
     */
    public final boolean hasDevice(Object... query)
    {
        return !getDevice(query).isNull();
    }

    /**
     * Returns the first device which returns <code>true</code> for {@link BleDevice#isAny(int)}, or {@link BleDevice#NULL} if no such device is found.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) IBleDevice getDevice(final int mask_BleDeviceState)
    {
        return m_deviceMngr.getDevice(mask_BleDeviceState);
    }

    public final List<BleDevice> getDevices(final Intent intentFromScan)
    {
        if (Utils.isLollipop())
        {
            return L_Util.getBleDeviceListFromScanIntent(intentFromScan, this);
        }
        return P_Const.EMPTY_BLEDEVICE_LIST;
    }

    public final void getDevices(final ForEach_Void<BleDevice> forEach)
    {
        m_deviceMngr.forEach(forEach);
    }

    /**
     * Same as {@link #getDevices(ForEach_Void)} but will only return devices
     * in the given state provided.
     */
    public final void getDevices(final ForEach_Void<BleDevice> forEach, final BleDeviceState state)
    {
        m_deviceMngr.forEach(forEach, state, true);
    }

    /**
     * Overload of {@link #getDevices(ForEach_Void)}
     * if you need to break out of the iteration at any point.
     */
    public final void getDevices(final ForEach_Breakable<BleDevice> forEach)
    {
        m_deviceMngr.forEach(forEach);
    }

    /**
     * Overload of {@link #getDevices(ForEach_Void, BleDeviceState)}
     * if you need to break out of the iteration at any point.
     */
    public final void getDevices(final ForEach_Breakable<BleDevice> forEach, final BleDeviceState state)
    {
        m_deviceMngr.forEach(forEach, state, true);
    }

    /**
     * Returns the mac addresses of all devices that we know about from both current and previous
     * app sessions.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) Iterator<String> getDevices_previouslyConnected()
    {
        return m_diskOptionsMngr.getPreviouslyConnectedDevices();
    }


    /**
     * Convenience method to return a {@link Set} of currently bonded devices. This simply calls
     * {@link BluetoothAdapter#getBondedDevices()}, and wraps all bonded devices into separate
     * {@link BleDevice} classes.
     * <p>
     * NOTE: If the Bluetooth radio is turned off, some android devices return <code>null</code>. In this case,
     * SweetBlue will just return an empty list.
     */
    public final Set<IBleDevice> getDevices_bonded()
    {
        Set<P_DeviceHolder> native_bonded_devices = managerLayer().getBondedDevices();
        // The native system can return null from the above call if the bluetooth radio is
        // turned off, so if that's the case, just return an empty Set.
        if (native_bonded_devices == null)
            return new HashSet<>(0);

        Set<IBleDevice> bonded_devices = new HashSet<>(native_bonded_devices.size());
        IBleDevice device;
        for (P_DeviceHolder d : native_bonded_devices)
        {
            device = getDevice(d.getAddress());
            if (device.isNull())
                device = newDevice(d.getAddress());
            bonded_devices.add(device);
        }
        return bonded_devices;
    }

    public final @Nullable(Nullable.Prevalence.NEVER) List<IBleDevice> getDevices_List()
    {
        return m_deviceMngr.getList();
    }

    /**
     * Same as {@link #getDevices_List()}, but sorts the list using {@link BleManagerConfig#defaultListComparator}.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) List<IBleDevice> getDevices_List_sorted()
    {
        return m_deviceMngr.getList_sorted();
    }

    /**
     * Returns the total number of devices this manager is...managing.
     * This includes all devices that are {@link BleDeviceState#DISCOVERED}.
     */
    public final int getDeviceCount()
    {
        return m_deviceMngr.getCount();
    }

    /**
     * Returns the number of devices that are in the current state.
     */
    public final int getDeviceCount(BleDeviceState state)
    {
        return m_deviceMngr.getCount(state);
    }

    /**
     * Returns the number of devices that match the given query.
     * See {@link BleDevice#is(Object...)} for the query format.
     */
    public final int getDeviceCount(Object... query)
    {
        return m_deviceMngr.getCount(query);
    }

    public final boolean hasDevices()
    {
        return m_deviceMngr.getCount() > 0;
    }

    public final @Nullable(Nullable.Prevalence.NEVER) List<IBleDevice> getDevices_List(final BleDeviceState state)
    {
        return m_deviceMngr.getDevices_List(false, state);
    }

    /**
     * Same as {@link #getDevices_List(BleDeviceState)} except the list is sorted using {@link BleManagerConfig#defaultListComparator}.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) List<IBleDevice> getDevices_List_sorted(final BleDeviceState state)
    {
        return m_deviceMngr.getDevices_List(true, state);
    }

    public final @Nullable(Nullable.Prevalence.NEVER) List<IBleDevice> getDevices_List(final Object... query)
    {
        return m_deviceMngr.getDevices_List(false, query);
    }

    /**
     * Same as {@link #getDevices_List(Object...)} except the list is sorted using {@link BleManagerConfig#defaultListComparator}.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) List<IBleDevice> getDevices_List_sorted(final Object... query)
    {
        return m_deviceMngr.getDevices_List(true, query);
    }

    public final @Nullable(Nullable.Prevalence.NEVER) List<IBleDevice> getDevices_List(final int mask_BleDeviceState)
    {
        return m_deviceMngr.getDevices_List(false, mask_BleDeviceState);
    }

    /**
     * Same as {@link #getDevices_List(int)} except the list is sorted using {@link BleManagerConfig#defaultListComparator}.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) List<IBleDevice> getDevices_List_sorted(final int mask_BleDeviceState)
    {
        return m_deviceMngr.getDevices_List(true, mask_BleDeviceState);
    }

    /**
     * Removes the given {@link BleDevice} from SweetBlue's internal device cache list. You should never have to call this
     * yourself (and probably shouldn't), but it's here for flexibility.
     */
    @Advanced
    public final void removeDeviceFromCache(IBleDevice device)
    {
        m_deviceMngr.remove(device, m_deviceMngr_cache);
    }

    /**
     * Removes all {@link BleDevice}s from SweetBlue's internal device cache list. You should never have to call this
     * yourself (and probably shouldn't), but it's here for flexibility.
     */
    @Advanced
    public final void removeAllDevicesFromCache()
    {
        m_deviceMngr.removeAll(m_deviceMngr_cache);
    }

    /**
     * Returns a new {@link HistoricalData} instance using
     * {@link BleDeviceConfig#historicalDataFactory} if available.
     */
    public final HistoricalData newHistoricalData(final byte[] data, final EpochTime epochTime)
    {
        final BleDeviceConfig.HistoricalDataFactory factory = m_config.historicalDataFactory;

        if (m_config.historicalDataFactory != null)
        {
            return m_config.historicalDataFactory.newHistoricalData(data, epochTime);
        }
        else
        {
            return new HistoricalData(data, epochTime);
        }
    }

    /**
     * Same as {@link #newHistoricalData(byte[], EpochTime)} but tries to use
     * {@link BleDevice#newHistoricalData(byte[], EpochTime)} if we have a device
     * matching the given mac address.
     */
    public final HistoricalData newHistoricalData(final byte[] data, final EpochTime epochTime, final String macAddress)
    {
        final IBleDevice device = getDevice(macAddress);

        if (device.isNull())
        {
            return newHistoricalData(data, epochTime);
        }
        else
        {
            return device.newHistoricalData(data, epochTime);
        }
    }

    /**
     * Overload of {@link BleManager#getServer(IncomingListener)} without any initial set-up parameters.
     */
    public final IBleServer getServer()
    {
        return getServer((IncomingListener) null);
    }

    /**
     * Returns a {@link BleServer} instance. which for now at least is a singleton.
     */
    public final IBleServer getServer(final IncomingListener incomingListener)
    {
        return getServer(incomingListener, null, null);
    }

    /**
     * Overload of {@link BleManager#getServer(GattDatabase, AddServiceListener)}, with no {@link AddServiceListener} set.
     */
    public final IBleServer getServer(final GattDatabase gattDatabase)
    {
        return getServer(gattDatabase, null);
    }

    /**
     * Overload of {@link BleManager#getServer(IncomingListener, GattDatabase, AddServiceListener)}, with no {@link IncomingListener} set.
     */
    public final IBleServer getServer(final GattDatabase gattDatabase, AddServiceListener addServiceListener)
    {
        return getServer(null, gattDatabase, addServiceListener);
    }

    /**
     * Returns a {@link BleServer} instance. This is now the preferred method to retrieve the server instance.
     */
    public final IBleServer getServer(final IncomingListener incomingListener, final GattDatabase gattDatabase, final AddServiceListener addServiceListener)
    {
        if (m_server == null)
        {
            m_server = IBleServer.DEFAULT_FACTORY.newInstance(this, /*isNull*/false);
            if (gattDatabase != null)
            {
                for (BleService service : gattDatabase.getServiceList())
                {
                    m_server.addService(service, addServiceListener);
                }
            }
        }
        if (incomingListener != null)
            m_server.setListener_Incoming(incomingListener);
        return m_server;
    }

    public final boolean hasServerInstance()
    {
        // Return true if m_server is NOT null
        return m_server != null;
    }

    /**
     * Same as {@link #newDevice(String, String, BleDeviceConfig)} but uses an empty string for the name
     * and passes a <code>null</code> {@link BleDeviceConfig}, which results in inherited options from {@link BleManagerConfig}.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) IBleDevice newDevice(String macAddress)
    {
        return newDevice(macAddress, null, null);
    }

    /**
     * Same as {@link #newDevice(String)} but allows a custom name also.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) IBleDevice newDevice(final String macAddress, final String name)
    {
        return newDevice(macAddress, name, null);
    }

    /**
     * Same as {@link #newDevice(String)} but passes a {@link BleDeviceConfig} to be used as well.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) IBleDevice newDevice(final String macAddress, final BleDeviceConfig config)
    {
        return newDevice(macAddress, null, config);
    }

    /**
     * Creates a new {@link BleDevice} or returns an existing one if the macAddress matches.
     * {@link DiscoveryListener#onEvent(Event)} will be called if a new device
     * is created.
     * <br><br>
     * NOTE: You should always do a {@link BleDevice#isNull()} check on this method's return value just in case. Android
     * documentation says that underlying stack will always return a valid {@link android.bluetooth.BluetoothDevice}
     * instance (which is required to create a valid {@link BleDevice} instance), but you really never know.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) IBleDevice newDevice(final String macAddress, final String name, final BleDeviceConfig config)
    {
        return newDevice(macAddress, name, null, config);
    }

    public final @Nullable(Nullable.Prevalence.NEVER) IBleDevice newDevice(final String macAddress, final String name, final byte[] scanRecord, final BleDeviceConfig config)
    {
        final String macAddress_normalized = normalizeMacAddress(macAddress);

        final IBleDevice existingDevice = this.getDevice(macAddress_normalized);

        if (!existingDevice.isNull())
        {
            if (config != null)
            {
                existingDevice.setConfig(config);
            }

            if (name != null)
            {
                existingDevice.setName(name, null, null);
            }

            return existingDevice;
        }

        final IBluetoothDevice device_native = newNativeDevice(macAddress_normalized);

        if (device_native.isDeviceNull() && scanRecord == null) //--- DRK > API says this should never happen...not trusting it! Only returning null instance if scanRecord is null
        {
            return P_BleDeviceImpl.NULL;
        }

        final String name_normalized = Utils_String.normalizeDeviceName(name);

        final IBleDevice newDevice = newDevice_private(device_native, name_normalized, name != null ? name : "", BleDeviceOrigin.EXPLICIT, config);

        if (name != null)
        {
            newDevice.setName(name, null, null);
        }

        onDiscovered_wrapItUp(newDevice, device_native, /*newlyDiscovered=*/true, /*scanRecord=*/scanRecord, 0, BleDeviceOrigin.EXPLICIT, /*scanEvent=*/null);

        return newDevice;
    }

    public final boolean undiscover(final IBleDevice device)
    {
        if (device == null) return false;
        if (device.isNull()) return false;
        if (!hasDevice(device)) return false;
        if (device.is(BleDeviceState.UNDISCOVERED)) return false;

        if (device.isAny(BleDeviceState.BLE_CONNECTED, BleDeviceState.BLE_CONNECTING, BleDeviceState.CONNECTING_OVERALL))
            device.disconnectAndUndiscover();
        else
            m_deviceMngr.undiscoverAndRemove(device, m_discoveryListener, m_deviceMngr_cache, E_Intent.INTENTIONAL);

        return true;
    }

    /**
     * This method will clear the task queue of all tasks.
     * NOTE: This can really mess things up, especially if you're currently trying to connect to a device. Only use this if you absolutely have to!
     */
    @Advanced
    public final void clearQueue()
    {
        m_taskManager.clearQueueOfAll();
    }

    /**
     * Convenience forwarding of {@link #clearSharedPreferences(String)}.
     *
     * @see #clearSharedPreferences(String)
     */
    public final void clearSharedPreferences(final BleDevice device)
    {
        clearSharedPreferences(device.getMacAddress());
    }

    /**
     * Clears all data currently being held in {@link android.content.SharedPreferences} for a particular device.
     *
     * @see BleDeviceConfig#manageLastDisconnectOnDisk
     * @see BleDeviceConfig#tryBondingWhileDisconnected_manageOnDisk
     * @see BleDeviceConfig#saveNameChangesToDisk
     * @see #clearSharedPreferences()
     */
    public final void clearSharedPreferences(final String macAddress)
    {
        final String macAddress_normalized = normalizeMacAddress(macAddress);

        m_diskOptionsMngr.clear(macAddress_normalized);
    }

    /**
     * Clears all data currently being held in {@link android.content.SharedPreferences} for all devices.
     *
     * @see BleDeviceConfig#manageLastDisconnectOnDisk
     * @see BleDeviceConfig#tryBondingWhileDisconnected_manageOnDisk
     * @see BleDeviceConfig#saveNameChangesToDisk
     * @see #clearSharedPreferences(String)
     */
    public final void clearSharedPreferences()
    {
        m_diskOptionsMngr.clear();
    }

    /**
     * This method is made public in case you want to tie the library in to an update loop
     * from another codebase. Generally you should leave {@link BleManagerConfig#autoUpdateRate}
     * alone and let the library handle the calling of this method.
     */
    @Advanced
    public final void update(final double timeStep_seconds, final long currentTime)
    {
        TimeTracker tt = TimeTracker.getInstance();

        tt.start("BleManager_Update");

        m_currentTick = currentTime;

        tt.start("BleManager_Update_Native_Manager");
        m_nativeManager.update(timeStep_seconds);

        tt.transition("BleManager_Update_Native_Manager", "BleManager_Update_UhOhThrottler");

        m_uhOhThrottler.update(timeStep_seconds);

        tt.transition("BleManager_Update_UhOhThrottler", "BleManager_Update_TaskManager");

        if (m_taskManager.update(timeStep_seconds, currentTime))
        {
            m_lastTaskExecution = currentTime;
            checkIdleStatus();
        }

        tt.stop("BleManager_Update_TaskManager");

        if( m_isForegrounded )
        {
            m_timeForegrounded += timeStep_seconds;
        }
        else
        {
            m_timeForegrounded = 0.0;
        }

        tt.start("BleManager_Update_DeviceMngr");

        m_deviceMngr.update(timeStep_seconds);

        if ( m_timeTurnedOn == 0 && is(ON) )
        {
            m_timeTurnedOn = currentTime;
        }

        tt.transition("BleManager_Update_DeviceMngr", "BleManager_Update_ScanManager");
        boolean dontDoMoreStuff = m_scanManager.update(timeStep_seconds, currentTime);
        tt.stop("BleManager_Update_ScanManager");
        if(!dontDoMoreStuff)
        {
            tt.start("BleManager_Update_ScanManager_MoreStuff");
            if (Interval.isEnabled(m_config.minTimeToIdle))
            {
                if (!is(IDLE) && m_lastTaskExecution + m_config.minTimeToIdle.millis() < currentTime)
                {
                    m_updateRunnable.setUpdateRate(m_config.idleUpdateRate.millis());
                    m_stateTracker.update(E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, IDLE, true);
                    getLogger().i("Update loop has entered IDLE state.");
                }
            }
            tt.stop("BleManager_Update_ScanManager_MoreStuff");
        }

        tt.start("BleManager_Update_UpdateLoopCallback");

        if( m_config.updateLoopCallback != null )
        {
            m_config.updateLoopCallback.onUpdate(timeStep_seconds);
        }

        tt.stop("BleManager_Update_UpdateLoopCallback");

        // Commenting this out for now, as it's not really helpful for our customers.
//        if (!is(IDLE) && m_config.autoUpdateRate.millis() < (System.currentTimeMillis() - m_currentTick) && (m_lastUpdateLoopWarning + UPDATE_LOOP_WARNING_DELAY <= m_currentTick))
//        {
//            m_lastUpdateLoopWarning = m_currentTick;
//            getLogger().w("BleManager", String.format("Update loop took longer to run than the current interval of %dms", m_config.autoUpdateRate.millis()));
//        }

        tt.stop("BleManager_Update");

        tt.print();
    }

    /**
     * Returns this manager's knowledge of the app's foreground state.
     */
    public final boolean isForegrounded()
    {
        return m_isForegrounded;
    }

    @Override public final String toString()
    {
        return m_stateTracker.toString();
    }

    public final P_BluetoothCrashResolver getCrashResolver()
    {
        return m_crashResolver;
    }

    public final P_ManagerStateTracker getStateTracker()
    {
        return m_stateTracker;
    }

    public final P_Logger getLogger()
    {
        return m_logger;
    }

    public final P_TaskManager getTaskManager()
    {
        return m_taskManager;
    }

    public final void tryPurgingStaleDevices(final double scanTime)
    {
        m_deviceMngr.requestPurge(scanTime, m_deviceMngr_cache, m_discoveryListener);
    }

    public final long timeTurnedOn()
    {
        return m_timeTurnedOn;
    }

    public final void clearTimeTurnedOn()
    {
        m_timeTurnedOn = 0;
    }

    public final double timeForegrounded()
    {
        return m_timeForegrounded;
    }

    public final synchronized BleDevice getBleDevice(IBleDevice device)
    {
        if (device == null || device.isNull())
            return BleDevice.NULL;

        BleDevice d = m_deviceMap.get(device.getMacAddress());
        if (d == null)
        {
            d = P_Bridge_User.newDevice(device);
            m_deviceMap.put(device.getMacAddress(), d);
        }

        return d;
    }

    public final synchronized BleNode getBleNode(IBleNode node)
    {
        if (node instanceof IBleServer)
            return getBleServer((IBleServer) node);
        else
            return getBleDevice((IBleDevice) node);
    }

    public final synchronized BleServer getBleServer(IBleServer server)
    {
        if (server == null || server.isNull())
            return BleServer.NULL;

        BleServer s = m_serverMap.get(server);
        if (s == null)
        {
            s = P_Bridge_User.newServer(server);
            m_serverMap.put(server, s);
        }

        return s;
    }

    public final void checkIdleStatus()
    {
        if (is(IDLE))
        {
            // To ensure we get back up to speed as soon as possible, we'll remove the callbacks, set the new update rate
            // then post the runnable again. This avoids waiting for the idle time before the speed bumps back up.
            getLogger().i("Update loop is no longer in the IDLE state.");
            getPostManager().removeUpdateCallbacks(m_updateRunnable);
            m_updateRunnable.setUpdateRate(m_config.autoUpdateRate.millis());
            m_stateTracker.update(E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, IDLE, false);
            getPostManager().postToUpdateThread(m_updateRunnable);
        }
    }

    public final void stopScan(E_Intent intent)
    {
        m_scanManager.resetTimeNotScanning();

        if (!m_taskManager.succeed(P_Task_Scan.class, this))
        {
            // I don't think this is needed, if we're clearing it from the queue below.
//			P_Task_Scan scanTask = m_taskQueue.get(P_Task_Scan.class, BleManager.this);
//			if (scanTask != null)
//				scanTask.succeed();
            m_logger.i("Clearing queue of any scan tasks...");
            m_taskManager.clearQueueOf(P_Task_Scan.class, P_BleManagerImpl.this);
        }

        m_stateTracker.remove(BleManagerState.STARTING_SCAN, intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
        m_stateTracker.remove(BleManagerState.SCANNING, intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
        m_stateTracker.remove(BleManagerState.SCANNING_PAUSED, intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
        m_stateTracker.remove(BleManagerState.BOOST_SCANNING, intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
    }

    public final synchronized void onDiscoveredFromNativeStack(List<P_ScanManager.DiscoveryEntry> entries)
    {
        //--- DRK > Protects against fringe case where scan task is executing and app calls turnOff().
        //---		Here the scan task will be interrupted but still potentially has enough time to
        //---		discover another device or two. We're checking the enum state as opposed to the native
        //---		integer state because in this case the "turn off ble" task hasn't started yet and thus
        //---		hasn't called down into native code and thus the native state hasn't changed.
        if( false == is(ON) )  return;

        //--- DRK > Not sure if queued up messages to library's thread can sneak in a device discovery event
        //---		after user called stopScan(), so just a check to prevent unexpected callbacks to the user.
        if( false == isScanning() )  return;

        final List<P_ScanManager.DiscoveryEntry> list = new ArrayList<>();

        for (P_ScanManager.DiscoveryEntry entry : entries)
        {

            final String macAddress = entry.device().getAddress();
            IBleDevice device_sweetblue = m_deviceMngr.get(macAddress);

            if (device_sweetblue != null)
            {
                if (!device_sweetblue.nativeManager().getDeviceLayer().equals(entry.device()))
                {
                    ASSERT(false, "Discovered device " + entry.device().getName() + " " + macAddress + " already in list but with new native device instance.");
                }
            }

            final ScanFilter.Please please;
            final boolean newlyDiscovered;
            final ScanFilter.ScanEvent scanEvent_nullable;
            final boolean stopScan;

            if (device_sweetblue == null)
            {
                final String rawDeviceName;

                try
                {
                    rawDeviceName = getDeviceName(entry.device(), entry.record());
                }

                //--- DRK > Can occasionally catch a DeadObjectException or NullPointerException here...nothing we can do about it.
                catch (Exception e)
                {
                    m_logger.e(Arrays.toString(e.getStackTrace()));

                    //--- DRK > Can't actually catch the DeadObjectException itself.
                    if (e instanceof DeadObjectException)
                    {
                        uhOh(UhOhListener.UhOh.DEAD_OBJECT_EXCEPTION);
                    }
                    else
                    {
                        uhOh(UhOhListener.UhOh.RANDOM_EXCEPTION);
                    }

                    continue;
                }

                final String normalizedDeviceName = Utils_String.normalizeDeviceName(rawDeviceName);

                final boolean hitDisk = P_Bridge_User.boolOrDefault(m_config.manageLastDisconnectOnDisk);
                final State.ChangeIntent lastDisconnectIntent = m_diskOptionsMngr.loadLastDisconnect(macAddress, hitDisk);
                scanEvent_nullable = m_filterMngr.makeEvent() ? P_Bridge_User.newScanEventFromRecord(entry.device().getNativeDevice(), rawDeviceName, normalizedDeviceName, entry.rssi(), lastDisconnectIntent, entry.record()) : null;

                please = m_filterMngr.allow(m_logger, scanEvent_nullable);

                if (please != null && false == P_Bridge_User.ack(please)) continue;

                stopScan = P_Bridge_User.stopScan(please);

                final String name_native = rawDeviceName;

                final BleDeviceConfig config_nullable = P_Bridge_User.fromPlease(please);
                device_sweetblue = newDevice_private(entry.device(), normalizedDeviceName, name_native, BleDeviceOrigin.FROM_DISCOVERY, config_nullable);
                newlyDiscovered = true;
            }
            else
            {
                scanEvent_nullable = null;
                newlyDiscovered = false;
                stopScan = false;
            }

            entry.m_stopScan = stopScan;
            entry.m_newlyDiscovered = newlyDiscovered;
            entry.m_bleDevice = device_sweetblue;
            entry.m_origin = BleDeviceOrigin.FROM_DISCOVERY;
            entry.m_scanEvent = scanEvent_nullable;
            list.add(entry);
        }

        onDiscovered_wrapItUp(list);
    }

    public final IBluetoothManager managerLayer()
    {
        return m_config.bluetoothManagerImplementation;
    }

    public final boolean canPerformAutoScan()
    {
        return is(ON) && (m_config.autoScanDuringOta || !m_deviceMngr.hasDevice(BleDeviceState.PERFORMING_OTA));
    }

    public final void uhOh(UhOhListener.UhOh reason)
    {
//		if( reason == UhOh.UNKNOWN_CONNECTION_ERROR )
//		{
//			m_connectionFailTracker = 0;
//		}

        m_uhOhThrottler.uhOh(reason);
    }

    public final void postEvent(final GenericListener_Void listener, final Event event)
    {
        if (listener != null)
        {
            if (listener instanceof PA_CallbackWrapper)
            {
                m_postManager.runOrPostToUpdateThread(() -> {
                    if (listener != null)
                    {
                        listener.onEvent(event);
                    }
                });
            }
            else
            {
                m_postManager.postCallback(() -> {
                    if (listener != null)
                    {
                        listener.onEvent(event);
                    }
                });
            }
        }
    }

    public final P_ScanManager getScanManager()
    {
        return m_scanManager;
    }

    public final P_PostManager getPostManager()
    {
        return m_postManager;
    }

    public final P_BleManagerNativeManager getNativeManager()
    {
        return m_nativeManager;
    }

    public final P_WakeLockManager getWakeLockManager()
    {
        return m_wakeLockMngr;
    }

    public final P_DeviceManager getDeviceManager_cache()
    {
        return m_deviceMngr_cache;
    }

    public final P_DeviceManager getDeviceManager()
    {
        return m_deviceMngr;
    }

    public final void clearScanningRelatedMembers(final E_Intent intent)
    {
//		m_filterMngr.clear();

        m_scanManager.resetTimeNotScanning();

        m_stateTracker.remove(BleManagerState.SCANNING, intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
    }

    public final boolean ready() {
        if (!m_ready)
        {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            {
                m_ready = is(ON);
            }
            else
            {
                m_ready = is(ON) && isLocationEnabledForScanning_byRuntimePermissions() && isLocationEnabledForScanning_byOsServices();
            }
        }
        if (m_ready && !is(BLE_SCAN_READY))
        {
            setBleScanReady();
        }
        return m_ready;
    }

    public final IBluetoothDevice newNativeDevice(final String macAddress)
    {
        P_DeviceHolder deviceHolder = P_DeviceHolder.newHolder(managerLayer().getRemoteDevice(macAddress), macAddress);
        IBluetoothDevice layer = P_Bridge_User.newDeviceLayer(this, P_BleDeviceImpl.EMPTY_DEVICE(this));
        layer.setNativeDevice(deviceHolder.getDevice(), deviceHolder);
        return layer;
    }

    public OutgoingListener getListener_Outgoing()
    {
        return m_defaultServerOutgoingListener;
    }

    public final AdvertisingListener getListener_Advertising()
    {
        return m_advertisingListener;
    }

    public final Backend_HistoricalDatabase getHistoricalDatabase()
    {
        return m_historicalDatabase;
    }

    public final HistoricalDataLoadListener getHistoricalDataLoadListener()
    {
        return m_historicalDataLoadListener;
    }

    public final P_DiskOptionsManager getDiskOptionsManager()
    {
        return m_diskOptionsMngr;
    }

    public final ReadWriteListener getDefaultReadWriteListener()
    {
        return m_defaultReadWriteListener;
    }

    public final NotificationListener getDefaultNotificationListener()
    {
        return m_defaultNotificationListener;
    }

    public final ServerStateListener getDefaultServerStateListener()
    {
        return m_defaultServerStateListener;
    }

    public final ServerConnectListener getDefaultServerConnectListener()
    {
        return m_defaultServerConnectFilter;
    }

    public final void onDiscovered_fromRogueAutoConnect(final IBleDevice device, final boolean newlyDiscovered, final List<UUID> services_nullable, final byte[] scanRecord_nullable, final int rssi)
    {
        m_deviceMngr.add(device);

        onDiscovered_wrapItUp(device, device.nativeManager().getDeviceLayer(), newlyDiscovered, scanRecord_nullable, rssi, BleDeviceOrigin.FROM_DISCOVERY, /*scanEvent=*/null);
    }

    public final DeviceConnectListener getDefaultDeviceConnectListener()
    {
        return m_defaultDeviceConnectListener;
    }

    public final DeviceReconnectFilter getDefaultDeviceReconnectFilter()
    {
        return m_defaultDeviceReconnectFilter;
    }

    public final BondListener getDefaultBondListener()
    {
        return m_defaultBondListener;
    }

    public final String getDeviceName(IBluetoothDevice device, byte[] scanRecord) throws Exception
    {
        final String nameFromDevice;
        final String nameFromRecord;
        nameFromDevice = device.getName();
        nameFromRecord = Utils_ScanRecord.parseName(scanRecord);
        if (isDeviceThatReturnsShortName())
        {
            if (!TextUtils.isEmpty(nameFromRecord))
            {
                return nameFromRecord;
            }
            else
            {
                m_logger.w("Unable to get complete name from scan record! Defaulting to the short name given from BluetoothDevice.");
            }
        }
        if (TextUtils.isEmpty(nameFromDevice))
            return nameFromRecord;
        else
        {
            if (nameFromDevice.equals(P_Const.NULL_STRING))
                return nameFromRecord;
            else
                return nameFromDevice;
        }
    }

    public final DeviceStateListener getDefaultDeviceStateListener()
    {
        return m_defaultDeviceStateListener;
    }

    public final IncomingListener getDefaultServerIncomingListener()
    {
        return m_defaultServerIncomingListener;
    }

    public final long currentTime()
    {
        return m_currentTick;
    }

    public IDeviceListener.Factory getDeviceListenerFactory()
    {
        return m_deviceListenerFactory;
    }

    public IServerListener.Factory getServerListenerFactory()
    {
        return m_serverListenerFactory;
    }

    public IManagerListener.Factory getManagerListenerFactory()
    {
        return m_managerListenerFactory;
    }

    public IManagerListener getInternalListener()
    {
        return m_nativeManager.getListenerProcessor().getInternalListener();
    }










    private final String normalizeMacAddress(final String macAddress)
    {
        final String macAddress_normalized = Utils_String.normalizeMacAddress(macAddress);

        if( macAddress == macAddress_normalized )
        {
            return macAddress;
        }
        else if( macAddress.equals(macAddress_normalized) )
        {
            return macAddress;
        }
        else
        {
            getLogger().w("Given mac address " + macAddress + " has been auto-normalized to " + macAddress_normalized);

            return macAddress_normalized;
        }
    }

    final void setBleScanReady()
    {
        m_stateTracker.update(E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BLE_SCAN_READY, true);
    }

    final void forceOn()
    {
        m_stateTracker.update(E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, ON, true, OFF, false);
    }

    final void forceOff()
    {
        m_stateTracker.update(E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, ON, false, OFF, true);
    }

    /**
     * Call this from your app's {@link android.app.Activity#onDestroy()} method.
     * NOTE: Apparently no good way to know when app as a whole is being destroyed
     * and not individual Activities, so keeping this package-private for now.
     */
    final void onDestroy()
    {
        m_wakeLockMngr.clear();
        m_nativeManager.shutdown();
    }

    final boolean isBluetoothEnabled()
    {
        return managerLayer().isBluetoothEnabled();
    }

    public final long getUpdateRate()
    {
        return m_updateRunnable.getUpdateRate();
    }

    final <T extends Event> void postEvents(final GenericListener_Void listener, final List<T> events)
    {
        if (listener != null)
        {
            if (listener instanceof PA_CallbackWrapper)
            {
                m_postManager.runOrPostToUpdateThread(() -> {
                    if (listener != null)
                    {
                        for (Event e : events)
                        {
                            listener.onEvent(e);
                        }
                    }
                });
            }
            else
            {
                m_postManager.postCallback(() -> {
                    if (listener != null)
                    {
                        for (Event e : events)
                        {
                            listener.onEvent(e);
                        }
                    }
                });
            }
        }
    }

    public final ServerReconnectFilter getDefaultServerReconnectFilter()
    {
        return m_defaultServerReconnectFilter;
    }

    public final AddServiceListener getDefaultAddServiceListener()
    {
        return m_serviceAddListener;
    }













    private void stopScan_private(E_Intent intent)
    {
        m_scanManager.resetTimeNotScanning();

        if (!getTaskManager().succeed(P_Task_Scan.class, this))
        {
            m_logger.i("Clearing queue of any scan tasks...");
            m_taskManager.clearQueueOf(P_Task_Scan.class, this);
        }

        m_stateTracker.remove(BleManagerState.STARTING_SCAN, intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
        m_stateTracker.remove(BleManagerState.SCANNING, intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
        m_stateTracker.remove(BleManagerState.SCANNING_PAUSED, intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
        m_stateTracker.remove(BleManagerState.BOOST_SCANNING, intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
    }

    private Application.ActivityLifecycleCallbacks newLifecycleCallbacks()
    {
        return new Application.ActivityLifecycleCallbacks()
        {
            @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState){}
            @Override public void onActivityStarted(Activity activity){}
            @Override public void onActivityStopped(Activity activity){}
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState){}
            @Override public void onActivityDestroyed(Activity activity){}

            @Override public void onActivityPaused(Activity activity)
            {
                if( m_config.autoPauseResumeDetection )
                {
                    onPause();
                }
            }

            @Override public void onActivityResumed(Activity activity)
            {
                if( m_config.autoPauseResumeDetection )
                {
                    onResume();
                }
            }
        };
    }

    private void addLifecycleCallbacks()
    {
        if( getApplicationContext() instanceof Application )
        {
            final Application application = (Application) getApplicationContext();
            m_activityCallbacks = m_activityCallbacks == null ? newLifecycleCallbacks() : m_activityCallbacks;

            application.registerActivityLifecycleCallbacks(m_activityCallbacks);
        }
        else
        {
            //--- DRK > Not sure if this is practically possible but nothing we can do here I suppose.
        }
    }


    private final class UpdateRunnable implements Runnable
    {

        private Long m_lastAutoUpdateTime;
        private long m_autoUpdateRate = -1;
        private boolean m_shutdown = false;


        public UpdateRunnable(long updateRate)
        {
            m_autoUpdateRate = updateRate;
        }

        public UpdateRunnable()
        {
        }

        public void setUpdateRate(long rate)
        {
            m_autoUpdateRate = rate;
        }

        public long getUpdateRate()
        {
            return m_autoUpdateRate;
        }

        @Override public void run()
        {
            long currentTime = System.currentTimeMillis();
            if (m_lastAutoUpdateTime == null)
            {
                m_lastAutoUpdateTime = currentTime;
            }
            double timeStep = ((double) currentTime - m_lastAutoUpdateTime)/1000.0;

            timeStep = timeStep <= 0.0 ? .00001 : timeStep;
            //--- RB > Not sure why this was put here. If the tick is over a second, we still want to know that, otherwise tasks will end up running longer
            // 			than expected.
//			timeStep = timeStep > 1.0 ? 1.0 : timeStep;


            update(timeStep, currentTime);

            m_lastAutoUpdateTime = currentTime;

            if (!m_shutdown)
            {
                m_postManager.postToUpdateThreadDelayed(this, m_autoUpdateRate);
            }
        }
    }

    private void onDiscovered_wrapItUp(final IBleDevice device, final IBluetoothDevice device_native, final boolean newlyDiscovered, final byte[] scanRecord_nullable, final int rssi, final BleDeviceOrigin origin, ScanFilter.ScanEvent scanEvent_nullable)
    {
        if( newlyDiscovered )
        {
            device.onNewlyDiscovered(device_native, scanEvent_nullable, rssi, scanRecord_nullable, origin);

            if( m_discoveryListener != null )
            {
                final DiscoveryListener.DiscoveryEvent event = P_Bridge_User.newDiscoveryEvent(getBleDevice(device), DiscoveryListener.LifeCycle.DISCOVERED);
                postEvent(m_discoveryListener, event);
            }
        }
        else
        {
            device.onRediscovered(device_native, scanEvent_nullable, rssi, scanRecord_nullable, BleDeviceOrigin.FROM_DISCOVERY);

            if( m_discoveryListener != null )
            {
                final DiscoveryListener.DiscoveryEvent event = P_Bridge_User.newDiscoveryEvent(getBleDevice(device), DiscoveryListener.LifeCycle.REDISCOVERED);
                postEvent(m_discoveryListener, event);
            }
        }
    }

    private void onDiscovered_wrapItUp(List<P_ScanManager.DiscoveryEntry> entries)
    {
        final List<DiscoveryListener.DiscoveryEvent> events = new ArrayList<>(entries.size());

        boolean stopScan = false;

        for (P_ScanManager.DiscoveryEntry e : entries)
        {
            if (e.m_newlyDiscovered)
            {
                m_logger.i("BleManager", e.device().getAddress(), Utils_String.makeString("Discovered new BleDevice ", e.device().getName()));
                e.m_bleDevice.onNewlyDiscovered(e.device(), e.m_scanEvent, e.rssi(), e.record(), e.m_origin);
                final DiscoveryListener.DiscoveryEvent event = P_Bridge_User.newDiscoveryEvent(getBleDevice(e.m_bleDevice), DiscoveryListener.LifeCycle.DISCOVERED);
                events.add(event);
            }
            else
            {
                m_logger.d("BleManager", e.device().getAddress(), Utils_String.makeString("Re-discovered BleDevice ", e.device().getName()));
                e.m_bleDevice.onRediscovered(e.device(), e.m_scanEvent, e.rssi(), e.record(), e.m_origin);
                final DiscoveryListener.DiscoveryEvent event = P_Bridge_User.newDiscoveryEvent(getBleDevice(e.m_bleDevice), DiscoveryListener.LifeCycle.REDISCOVERED);
                events.add(event);
            }
            if (e.m_stopScan)
            {
                stopScan = true;
                break;
            }
        }
        DiscoveryListener listener = m_ephemeralDiscoveryListener;
        if (listener != null)
        {
            postEvents(listener, events);
        }
        listener = m_discoveryListener;
        if (listener != null)
        {
            postEvents(listener, events);
        }
        if (stopScan)
            stopScan();
    }

    private void reset_private(boolean nuclear, ResetListener listener)
    {
        if( listener != null )
        {
            if( m_resetListeners != null )
            {
                m_resetListeners.addListener(listener);
            }
            else
            {
                m_resetListeners = new P_WrappingResetListener(listener, m_postManager.getUIHandler(), m_config.postCallbacksToMainThread);
            }
        }

        if( is(BleManagerState.RESETTING) )
        {
            return;
        }

        m_stateTracker.append(RESETTING, E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);

        if( m_config.enableCrashResolverForReset )
        {
            m_taskManager.add(new P_Task_CrashResolver(P_BleManagerImpl.this, m_crashResolver, /*partOfReset=*/true));
        }

        turnOff_private(/*removeAllBonds=*/true);

        if (nuclear)
        {
            P_Task_FactoryReset reset = new P_Task_FactoryReset(this, null);
            m_taskManager.add(reset);
        }

        m_taskManager.add(new P_Task_TurnBleOn(this, /*implicit=*/false, (taskClass, state) -> {
            if (state.isEndingState())
            {
                ResetListener nukeListeners = m_resetListeners;
                m_resetListeners = null;
                m_stateTracker.remove(RESETTING, E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);

                if (nukeListeners != null)
                {
                    ResetListener.ResetEvent event = P_Bridge_User.newResetEvent(BleManager.get(getApplicationContext()), ResetListener.Progress.COMPLETED);
                    postEvent(nukeListeners, event);
                }
            }
        }));
    }

    private IBleDevice newDevice_private(final IBluetoothDevice device_native, final String name_normalized, final String name_native, final BleDeviceOrigin origin, final BleDeviceConfig config_nullable)
    {
        // TODO: for now always true...should these be behind a config option?
        final boolean hitCache = true;

        final IBleDevice device_cached;

        if( hitCache )
        {
            device_cached = m_deviceMngr_cache.get(device_native.getAddress());

            if( device_cached != null )
            {
                m_deviceMngr_cache.remove(device_cached, null);
                device_cached.setConfig(config_nullable);
            }
        }
        else
        {
            device_cached = null;
        }

        final IBleDevice device = device_cached != null ? device_cached : IBleDevice.DEFAULT_FACTORY.newInstance(this, device_native, name_normalized, name_native, origin, config_nullable, /*isNull=*/false);

        m_deviceMngr.add(device);

        return device;
    }

    private boolean isDeviceThatReturnsShortName()
    {
        //--- > RB  Right now, this is the only device we're aware of that returns the short name from BluetoothDevice.getName(). This may grow in the future.
        if (Build.MANUFACTURER.equalsIgnoreCase("amobile") && Build.PRODUCT.equalsIgnoreCase("full_amobile2601_wp_l") && Build.MODEL.equalsIgnoreCase("iot-500"))
        {
            return true;
        }
        return false;
    }

    private void turnOff_private(final boolean removeAllBonds)
    {
        if( isAny(TURNING_OFF, OFF) )  return;

        if( is(ON) )
        {
            m_stateTracker.update(E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, TURNING_OFF, true, ON, false);
        }

        m_deviceMngr.disconnectAllForTurnOff(PE_TaskPriority.CRITICAL);

        if( removeAllBonds )
        {
            m_deviceMngr.unbondAll(PE_TaskPriority.CRITICAL, BondListener.Status.CANCELLED_FROM_BLE_TURNING_OFF);
        }

        if( m_server != null )
        {
            m_server.disconnect_internal(AddServiceListener.Status.CANCELLED_FROM_BLE_TURNING_OFF, ServerReconnectFilter.Status.CANCELLED_FROM_BLE_TURNING_OFF, State.ChangeIntent.INTENTIONAL);
        }

        final P_Task_TurnBleOff task = new P_Task_TurnBleOff(this, /*implicit=*/false, (taskClass, state) -> {
            if (state == PE_TaskState.EXECUTING)
                m_deviceMngr.undiscoverAllForTurnOff(m_deviceMngr_cache, E_Intent.INTENTIONAL);
        });

        m_taskManager.add(task);
    }

    /**
     * Might not be useful to outside world. Used for sanity/early-out checks internally. Keeping private for now.
     * Does referential equality check.
     */
    private final boolean hasDevice_private(IBleDevice device)
    {
        return m_deviceMngr.has(device);
    }

    private void showScanWarningIfNeeded() {
        boolean hasAndroid12Permissions = true;
        if (Utils.isAndroid12())
        {
            hasAndroid12Permissions = areBluetoothPermissionsEnabled();
            if (!hasAndroid12Permissions) {
                m_logger.w("As of Android 12, you must request the following two permissions. They must be in your" +
                        " AndroidManifest.xml file, and you must request them at runtime. See https://developer.android.com/guide/topics/connectivity/bluetooth/permissions" +
                        " for more information on how to request permissions (alternatively, you can use the BleSetupHelper.");
            }
            if (getConfigClone().doNotRequestLocation)
            {
                return;
            }
            else
            {
                m_logger.d("If you do not wish to use location services/permissions for Android 12+, you must make sure to set" +
                        " BleManagerConfig.doNotRequestLocation to true, and update the permissions in your manifest.");
            }
        }

        if (false == isLocationEnabledForScanning())
        {
            final String ENABLED = "enabled";
            final String DISABLED = "disabled";

            final boolean reasonA = isLocationEnabledForScanning_byManifestPermissions();
            final boolean reasonB = isLocationEnabledForScanning_byRuntimePermissions();
            final boolean reasonC = isLocationEnabledForScanning_byOsServices();
            final String enabledA = reasonA ? ENABLED : DISABLED;
            final String enabledB = reasonB ? ENABLED : DISABLED;
            final String enabledC = reasonC ? ENABLED : DISABLED;

            m_logger.w
                    (
                        "As of Android M, in order for low energy scan results to return you must have the following:\n" +
                        "(A) " + Manifest.permission.ACCESS_COARSE_LOCATION + " or " + Manifest.permission.ACCESS_FINE_LOCATION + " in your AndroidManifest.xml.\n" +
                        "(B) Runtime permissions for aforementioned location permissions as described at https://developer.android.com/training/permissions/requesting.html.\n" +
                        "(C) Location services enabled, the same as if you go to OS settings App and enable Location.\n" +
                        "It looks like (A) is " + enabledA + ", (B) is " + enabledB + ", and (C) is " + enabledC + ".\n" +
                        "Various methods like BleManager.isLocationEnabledForScanning*() overloads and BleManager.turnOnLocationWithIntent*() overloads can help with this painful process.\n" +
                        "Good luck!"
                    );
        }
    }

    final boolean startScan_private(ScanOptions opts)
    {
        if (m_taskManager.isInQueue(P_Task_Scan.class, this))
        {
            getLogger().w("A startScan method was called when there's already a scan task in the queue!");
            return false;
        }

        if (!isBluetoothEnabled())
        {
            m_logger.e(BleManager.class.getSimpleName() + " is not " + ON + "! Please use the turnOn() method first.");

            return false;
        }

        final ScanOptions options;
        if (opts == null)
        {
            options = new ScanOptions();
            m_logger.e("Start scan was called with a null instance of ScanOptions! A new instance has been generated.");
        }
        else
            options = opts;

        if (options.getPendingIntent() != null && !Utils.isOreo())
        {
            m_logger.e("Start scan via PendingIntent was called on a device not running at least Oreo.");
            return false;
        }

        m_postManager.runOrPostToUpdateThread(() -> {
            final P_Task_Scan scanTask = m_taskManager.get(P_Task_Scan.class, P_BleManagerImpl.this);

            if (scanTask != null)
            {
                scanTask.resetTimeout(options.getScanTime().secs());
            }
            else
            {
                ASSERT(!m_taskManager.isCurrentOrInQueue(P_Task_Scan.class, P_BleManagerImpl.this));

                m_stateTracker.append(BleManagerState.STARTING_SCAN, E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);

                m_scanManager.resetTimeNotScanning();
                m_scanManager.setInfiniteScan(options.getScanTime().equals(Interval.INFINITE), options.isForceIndefinite());

                DiscoveryListener listener = options.getDiscoveryListener();
                if (listener != null)
                    m_ephemeralDiscoveryListener = listener;

                if (options.getScanFilter() != null)
                    m_filterMngr.setEphemeralFilter(options.getScanFilter(), options.getApplyMode());

                PE_TaskPriority pri = options.isPriorityScan() ? PE_TaskPriority.CRITICAL : null;

                boolean startScan = true;

                if (options.isPeriodic())
                {
                    if (!canPerformAutoScan())
                        startScan = false;
                }

                if (startScan)
                    m_taskManager.add(new P_Task_Scan(P_BleManagerImpl.this, m_nativeManager.getScanTaskListener(), options, pri));
            }
        });

        return true;
    }

    private void updateTimeTracker()
    {
        TimeTracker.getInstance().setTimeTrackerSetting(m_config.timeTrackerSetting);
    }

    private void updateLogger()
    {
        m_logger.updateInstance(P_Const.debugThreadNames, m_config.loggingOptions, m_config.logger);
    }

    private void initConfigDependentMembers()
    {
        // Check if one of the classes from the unit test module can be loaded. If so, we're running in a
        // unit test. Otherwise, we aren't. Only check this if the unitTest boolean is null
        if (P_Bridge_User.isUnitTest(m_config) == null)
        {
            try
            {
                Class.forName("com.idevicesinc.sweetblue.UnitTestLogger");
                P_Bridge_User.setUnitTest(m_config, true);
            } catch (ClassNotFoundException e)
            {
                P_Bridge_User.setUnitTest(m_config, false);
            }
        }

        m_filterMngr.setDefaultFilter(m_config.defaultScanFilter);

        m_config.bluetoothManagerImplementation.setIBleManager(this);

        if (m_config.bluetoothManagerImplementation.isManagerNull())
        {
            m_config.bluetoothManagerImplementation.resetManager(m_context);
        }

        m_nativeManager.init(m_config.bluetoothManagerImplementation);

        boolean startUpdate = true;

        if (m_updateRunnable != null)
        {
            m_updateRunnable.m_shutdown = false;
            m_postManager.removeUpdateCallbacks(m_updateRunnable);
        }
        else
        {
            if (Interval.isEnabled(m_config.autoUpdateRate))
            {
                m_updateRunnable = new UpdateRunnable(m_config.autoUpdateRate.millis());
            }
            else
            {
                startUpdate = false;
                m_updateRunnable = new UpdateRunnable();
            }
        }

        m_uhOhThrottler = new P_UhOhThrottler(this, Interval.secs(m_config.uhOhCallbackThrottle));

        if (m_wakeLockMngr == null)
        {
            m_wakeLockMngr = new P_WakeLockManager(this, m_config.manageCpuWakeLock);
        }
        else if (m_wakeLockMngr != null && m_config.manageCpuWakeLock == false)
        {
            m_wakeLockMngr.clear();
            m_wakeLockMngr = new P_WakeLockManager(this, m_config.manageCpuWakeLock);
        }

        if (m_config.defaultDiscoveryListener != null)
        {
            setListener_Discovery(m_config.defaultDiscoveryListener);
        }

        initPostManager();

        if (startUpdate)
        {
            m_postManager.postToUpdateThreadDelayed(m_updateRunnable, m_config.autoUpdateRate.millis());
        }

        if (m_config.reconnectFilter instanceof DeviceReconnectFilter)
            m_defaultDeviceReconnectFilter = (DeviceReconnectFilter) m_config.reconnectFilter;

        // We may have to do a more thorough check at some point. For now, I think just doing a reference check on the default option
        // should be sufficient.
        if (m_config.defaultNativeScanFilterList != BleManagerConfig.EMPTY_NATIVE_FILTER)
        {
            m_logger.w("BleManager", "Detected non-default native scan filter list. Setting scanApi option to POST_LOLLIPOP.");
            m_config.scanApi = BleScanApi.POST_LOLLIPOP;
        }
    }

    private void initPostManager()
    {
        if (m_postManager != null)
            m_postManager.quit();

        final P_SweetHandler update;
        P_SweetHandler ui;
        UpdateThreadType type = m_config.updateThreadType;

        switch (type)
        {
            case MAIN:
                update = new P_SweetUIHandler(this);
                ui = update;
                break;
            case USER_CUSTOM:
                ui = new P_SweetUIHandler(this);
                ThreadHandler handler = m_config.updateHandler;
                // If the thread handler is null, then we'll fall back on the default option.
                if (handler == null)
                {
                    m_logger.e("updateThreadType is set to " + UpdateThreadType.USER_CUSTOM.name() + ", but updateHandler instance is null! Defaulting to our own thread...");
                    update = new P_SweetBlueThread();
                }
                else
                    update = handler;

                update.post(() -> m_logger.setUpdateThread(android.os.Process.myTid()));
                break;
            case HANDLER_THREAD:
                ui = new P_SweetUIHandler(this);
                update = new P_SweetBlueAndroidHandlerThread();
                update.post(() -> m_logger.setUpdateThread(android.os.Process.myTid()));
                break;
            default:
                ui = new P_SweetUIHandler(this);
                update = new P_SweetBlueThread();
                update.post(() -> m_logger.setUpdateThread(android.os.Process.myTid()));
                break;
        }

        ui.post(() -> m_logger.setMainThread(android.os.Process.myTid()));
        if (m_postManager != null)
            m_postManager.quit();
        m_postManager = new P_PostManager(this, ui, update);
    }

}
