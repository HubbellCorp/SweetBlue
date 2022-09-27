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


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import com.idevicesinc.sweetblue.BondListener.BondEvent;
import com.idevicesinc.sweetblue.DiscoveryListener.LifeCycle;
import com.idevicesinc.sweetblue.annotations.Advanced;
import com.idevicesinc.sweetblue.annotations.Experimental;
import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.annotations.Nullable.Prevalence;
import com.idevicesinc.sweetblue.compat.L_Util;
import com.idevicesinc.sweetblue.internal.IBleDevice;
import com.idevicesinc.sweetblue.internal.IBleManager;
import com.idevicesinc.sweetblue.internal.P_BleManagerImpl;
import com.idevicesinc.sweetblue.internal.P_Bridge_Internal;
import com.idevicesinc.sweetblue.utils.EpochTime;
import com.idevicesinc.sweetblue.utils.Event;
import com.idevicesinc.sweetblue.utils.ForEach_Breakable;
import com.idevicesinc.sweetblue.utils.ForEach_Void;
import com.idevicesinc.sweetblue.utils.GattDatabase;
import com.idevicesinc.sweetblue.utils.HistoricalData;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.P_Const;
import com.idevicesinc.sweetblue.utils.Utils;

/**
 * The entry point to the library. Get a singleton instance using {@link #get(android.content.Context, BleManagerConfig)} or its overloads. Make sure
 * to hook up this manager to lifecycle events for your app as a whole: {@link #onPause()} and {@link #onResume()}.
 * <br><br>
 * Also put the following entries (or something similar) in the root of your AndroidManifest.xml:
 * <br><br>
 * {@code <uses-sdk android:minSdkVersion="18" android:targetSdkVersion="23" />}<br>
 * {@code <uses-permission android:name="android.permission.BLUETOOTH" /> }<br>
 * {@code <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" /> }<br>
 * {@code <uses-permission android:name="android.permission.BLUETOOTH_PRIVILEGED" /> }<br>
 * {@code <uses-permission android:name="android.permission.WAKE_LOCK" /> } <br>
 * {@code <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" /> } <br>
 * {@code <uses-feature android:name="android.hardware.bluetooth_le" android:required="true" /> }<br>
 * <br><br>
 * {@link android.Manifest.permission#WAKE_LOCK} is recommended but optional, needed if {@link BleManagerConfig#manageCpuWakeLock} is enabled to aid with reconnect loops.
 * As of now it's enabled by default.
 * <br><br>
 * {@link android.Manifest.permission#ACCESS_COARSE_LOCATION} (or {@link android.Manifest.permission#ACCESS_FINE_LOCATION})
 * is also strongly recommended but optional. Without it, {@link BleManager#startScan()} and overloads will not properly return results in {@link android.os.Build.VERSION_CODES#M} and above.
 * See {@link #startScan(Interval, ScanFilter, DiscoveryListener)} for more information.
 * <br><br>
 * Now here is a simple example usage:<pre><code>
 * public class MyActivity extends Activity
 * {
 *     {@literal @}Override protected void onCreate(Bundle savedInstanceState)
 *      {
 *          // A ScanFilter decides whether a BleDevice instance will be created
 *          // and passed to the DiscoveryListener implementation below.
 *         final ScanFilter scanFilter = new ScanFilter()
 *         {
 *            {@literal @}Override public Please onEvent(ScanEvent e)
 *             {
 *                 return Please.acknowledgeIf(e.name_normalized().contains("my_device"))
 *                              .thenStopScan();
 *             }
 *         };
 *
 *         // New BleDevice instances are provided through this listener.
 *         // Nested listeners then listen for connection and read results.
 *         final DiscoveryListener discoveryListener = new DiscoveryListener()
 *         {
 *            {@literal @}Override public void onEvent(DiscoveryEvent e)
 *             {
 *                 if( e.was(LifeCycle.DISCOVERED) )
 *                 {
 *                     e.device().connect(new StateListener()
 *                     {
 *                        {@literal @}Override public void onEvent(StateEvent e)
 *                         {
 *                             if( e.didEnter(BleDeviceState.INITIALIZED) )
 *                             {
 *                                 e.device().read(Uuids.BATTERY_LEVEL, new ReadWriteListener()
 *                                 {
 *                                    {@literal @}Override public void onEvent(ReadWriteEvent e)
 *                                     {
 *                                         if( e.wasSuccess() )
 *                                         {
 *                                             Log.i("", "Battery level is " + e.data_byte() + "%");
 *                                         }
 *                                     }
 *                                 });
 *                             }
 *                         }
 *                     });
 *                 }
 *             }
 *         };
 *
 *         // Helps you navigate the treacherous waters of Android M Location requirements for scanning.
 *         BleSetupHelper.start(this, new DefaultBleSetupHelperFilter()
 *         {
 *            {@literal @}Override public Please onEvent(BleSetupHelperEvent e)
 *             {
 *                 if( e.isDone() )
 *                 {
 *                     e.bleManager().startScan(scanFilter, discoveryListener);
 *                 }
 *
 *                 return super.onEvent(e);
 *             }
 *         });
 *    }
 * </code>
 * </pre>
 */
public final class BleManager
{


	/**
	 * Create the singleton instance or retrieve the already-created singleton instance with default configuration options set.
	 * If you call this after you call {@link #get(android.content.Context, BleManagerConfig)} (for example in another
	 * {@link android.app.Activity}), the {@link BleManagerConfig} originally passed in will be used.
	 * Otherwise, if a new instance is to be created, this calls {@link #get(android.content.Context, BleManagerConfig)} with a {@link BleManagerConfig}
	 * instance created using the default constructor {@link BleManagerConfig#BleManagerConfig()}.
	 *
	 */
	public static BleManager get(Context context)
	{
		if( s_instance == null )
			return get(context, new BleManagerConfig());
		else
			return s_instance;
	}

	/**
	 * Create the singleton instance or retrieve the already-created singleton instance with custom configuration options set.
	 * If you call this more than once (for example from a different {@link android.app.Activity}
	 * with different {@link BleManagerConfig} options set then the newer options overwrite the older options.
	 *
	 */
	public static BleManager get(Context context, BleManagerConfig config)
	{
		if( s_instance == null )
			s_instance = new BleManager(context, config);
		else
			s_instance.setConfig(config);

		return s_instance;
	}

	static BleManager s_instance;

	private final P_BleManagerImpl m_managerImpl;


	/**
	 * Field for app to associate any data it wants with the singleton instance of this class
	 * instead of having to subclass or manage associative hash maps or something.
	 * The library does not touch or interact with this data in any way.
	 *
	 * @see BleDevice#appData
	 * @see BleServer#appData
	 */
	@SuppressWarnings("squid:ClassVariableVisibilityCheck")
	public Object appData;



	private BleManager(Context context, BleManagerConfig config)
	{
		m_managerImpl = P_Bridge_Internal.newManager(context, config);
	}


	/**
	 * Updates the config options for this instance after calling {@link #get(android.content.Context)} or {@link #get(android.content.Context, BleManagerConfig)}.
	 * Providing a <code>null</code> value will set everything back to default values.
	 */
	public final void setConfig(@Nullable(Prevalence.RARE) BleManagerConfig config_nullable)
	{
		m_managerImpl.setConfig(config_nullable);
	}

	public final BleManagerConfig getConfigClone()
	{
		return m_managerImpl.getConfigClone();
	}


	/**
	 * Returns whether the manager is in any of the provided states.
	 */
	public final boolean isAny(BleManagerState ... states)
	{
		return m_managerImpl.isAny(states);
	}

	/**
	 * Returns whether the manager is in all of the provided states.
	 *
	 * @see #isAny(BleManagerState...)
	 */
	public final boolean isAll(BleManagerState... states)
	{
		return m_managerImpl.isAll(states);
	}

	/**
	 * Returns whether the manager is in the provided state.
	 *
	 * @see #isAny(BleManagerState...)
	 */
	public final boolean is(final BleManagerState state)
	{
		return m_managerImpl.is(state);
	}

	/**
	 * Returns <code>true</code> if there is partial bitwise overlap between the provided value and {@link #getStateMask()}.
	 *
	 * @see #isAll(int)
	 */
	public final boolean isAny(final int mask_BleManagerState)
	{
		return m_managerImpl.isAny(mask_BleManagerState);
	}

	/**
	 * Returns <code>true</code> if there is complete bitwise overlap between the provided value and {@link #getStateMask()}.
	 *
	 * @see #isAny(int)
	 */
	public final boolean isAll(final int mask_BleManagerState)
	{
		return m_managerImpl.isAll(mask_BleManagerState);
	}

	/**
	 * See similar comment for {@link BleDevice#getTimeInState(BleDeviceState)}.
	 *
	 * @see BleDevice#getTimeInState(BleDeviceState)
	 */
	public final Interval getTimeInState(BleManagerState state)
	{
		return m_managerImpl.getTimeInState(state);
	}

	/**
	 * Checks the underlying stack to see if BLE is supported on the phone.
	 */
	public final boolean isBleSupported()
	{
		return m_managerImpl.isBleSupported();
	}

	/**
	 * Checks to see if the device is running an Android OS which supports
	 * advertising.
	 */
	public final boolean isAdvertisingSupportedByAndroidVersion()
	{
		return m_managerImpl.isAdvertisingSupportedByAndroidVersion();
	}

	/**
	 * Checks to see if the device supports advertising.
	 */
	public final boolean isAdvertisingSupportedByChipset()
	{
		return m_managerImpl.isAdvertisingSupportedByChipset();
	}

	/**
	 * Checks to see if the device supports advertising BLE services.
	 */
	public final boolean isAdvertisingSupported()
	{
		return isAdvertisingSupportedByAndroidVersion() && isAdvertisingSupportedByChipset();
	}

	/**
	 * Returns <code>true</code> if the android device is running an android OS version which supports Bluetooth 5 features.
	 */
	public final boolean isBluetooth5SupportedByAndroidVersion()
	{
		return m_managerImpl.isAdvertisingSupportedByAndroidVersion();
	}

	/**
	 * Returns <code>true</code> if the android device supports the bluetooth 5 feature of long range (up to 4x the range of Bluetooth 4.x).
	 *
	 * It's possible for this to return <code>true</code>, and {@link #isBluetooth5HighSpeedSupported()} to return <code>false</code>.
	 */
	public final boolean isBluetooth5LongRangeSupported()
	{
		return m_managerImpl.isBluetooth5LongRangeSupported();
	}

	/**
	 * Returns <code>true</code> if the android device supports the high speed feature of Bluetooth 5 (2x the speed of Bluetooth 4.x).
	 */
	public final boolean isBluetooth5HighSpeedSupported()
	{
		return m_managerImpl.isBluetooth5HighSpeedSupported();
	}

	/**
	 * Convenience method to check if the android device supports bluetooth 5 in any way. This just calls {@link #isBluetooth5SupportedByAndroidVersion()},
	 *  {@link #isBluetooth5HighSpeedSupported()} and {@link #isBluetooth5LongRangeSupported()}.
	 */
	public final boolean isBluetooth5Supported()
	{
		return isBluetooth5SupportedByAndroidVersion() && (isBluetooth5LongRangeSupported() || isBluetooth5HighSpeedSupported());
	}

	/**
	 * Disables BLE if manager is {@link BleManagerState#ON}. This disconnects all current
	 * connections, stops scanning, and forgets all discovered devices.
	 */
	public final void turnOff()
	{
		m_managerImpl.turnOff();
	}

	/**
	 * Returns the native manager.
	 */
	@Advanced
	public final BluetoothManager getNative()
	{
		return m_managerImpl.getNative();
	}

	/**
	 * Returns the native bluetooth adapter.
	 */
	@Advanced
	public final BluetoothAdapter getNativeAdapter()
	{
		return m_managerImpl.getNativeAdapter();
	}

	/**
	 * Sets a default backup {@link HistoricalDataLoadListener} that will be invoked
	 * for all historical data loads to memory for all uuids for all devices.
	 */
	public final void setListener_HistoricalDataLoad(@Nullable(Prevalence.NORMAL) final HistoricalDataLoadListener listener_nullable)
	{
		m_managerImpl.setListener_HistoricalDataLoad(listener_nullable);
	}

	/**
	 * Set a listener here to be notified whenever we encounter an {@link UhOhListener.UhOh}.
	 */
	public final void setListener_UhOh(@Nullable(Prevalence.NORMAL) UhOhListener listener_nullable)
	{
		m_managerImpl.setListener_UhOh(listener_nullable);
	}

	/**
	 * Set a listener here to be notified whenever {@link #ASSERT(boolean)} fails.
	 * Mostly for use by internal library developers.
	 */
	public final void setListener_Assert(@Nullable(Prevalence.NORMAL) AssertListener listener_nullable)
	{
		m_managerImpl.setListener_Assert(listener_nullable);
	}

	/**
	 * Set a listener here to be notified whenever a {@link BleDevice} is discovered, rediscovered, or undiscovered.
	 */
	public final void setListener_Discovery(@Nullable(Prevalence.NORMAL) DiscoveryListener listener_nullable)
	{
		m_managerImpl.setListener_Discovery(listener_nullable);
	}

	/**
	 * Returns the discovery listener set with {@link #setListener_Discovery(DiscoveryListener)} or
	 * {@link BleManagerConfig#defaultDiscoveryListener}, or <code>null</code> if not set.
	 */
	public final DiscoveryListener getListener_Discovery()
	{
		return m_managerImpl.getListener_Discovery();
	}

	/**
	 * Set a listener here to be notified whenever this manager's {@link BleManagerState} changes.
	 */
	public final void setListener_State(@Nullable(Prevalence.NORMAL) ManagerStateListener listener_nullable)
	{
		m_managerImpl.setListener_State(listener_nullable);
	}

	/**
	 * Convenience method to listen for all changes in {@link BleDeviceState} for all devices.
	 * The listener provided will get called in addition to and after the listener, if any, provided
	 * to {@link BleDevice#setListener_State(DeviceStateListener)}.
	 *
	 * @see BleDevice#setListener_State(DeviceStateListener)
	 */
	public final void setListener_DeviceState(@Nullable(Prevalence.NORMAL) DeviceStateListener listener_nullable)
	{
		m_managerImpl.setListener_DeviceState(listener_nullable);
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
	public final void setListener_ServerReconnectFilter(@Nullable(Prevalence.NORMAL) ServerReconnectFilter listener_nullable)
	{
		m_managerImpl.setListener_ServerReconnectFilter(listener_nullable);
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
	public final void setListener_Incoming(@Nullable(Prevalence.NORMAL) IncomingListener listener_nullable)
	{
		m_managerImpl.setListener_Incoming(listener_nullable);
	}

	/**
	 * Convenience method to listen for all service addition events for all servers.
	 * The listener provided will get called in addition to and after the listener, if any, provided
	 * to {@link BleServer#setListener_ServiceAdd(AddServiceListener)}.
	 *
	 * @see BleServer#setListener_ServiceAdd(AddServiceListener)
	 */
	public final void setListener_ServiceAdd(@Nullable(Prevalence.NORMAL) AddServiceListener listener_nullable)
	{
		m_managerImpl.setListener_ServiceAdd(listener_nullable);
	}

	/**
	 * Convenience method to listen for all changes in {@link BleServerState} for all servers.
	 * The listener provided will get called in addition to and after the listener, if any, provided
	 * to {@link BleServer#setListener_State(ServerStateListener)}.
	 *
	 * @see BleServer#setListener_State(ServerStateListener)
	 */
	public final void setListener_ServerState(@Nullable(Prevalence.NORMAL) ServerStateListener listener_nullable)
	{
		m_managerImpl.setListener_ServerState(listener_nullable);
	}

	/**
	 * Convenience method to listen for completion of all outgoing messages from
	 * {@link BleServer} instances. The listener provided will get called in addition to and after the listener, if any, provided
	 * to {@link BleServer#setListener_Outgoing(OutgoingListener)}.
	 *
	 * @see BleServer#setListener_Outgoing(OutgoingListener)
	 */
	public final void setListener_Outgoing(@Nullable(Prevalence.NORMAL) OutgoingListener listener_nullable)
	{
		m_managerImpl.setListener_Outgoing(listener_nullable);
	}

	/**
	 * Convenience method to handle connection fail events at the manager level. You should be aware that if there is a listener
	 * set via {@link BleDevice#setListener_Reconnect(DeviceReconnectFilter)}, the return value of that one will be used
	 * internally by SweetBlue for that {@link BleDevice} (but the event will still be pumped to the listener provided here).
	 *
	 * @see BleDevice#setListener_Reconnect(DeviceReconnectFilter)
	 */
	public final void setListener_DeviceReconnect(@Nullable(Prevalence.NORMAL) DeviceReconnectFilter listener_nullable)
	{
		m_managerImpl.setListener_DeviceReconnect(listener_nullable);
	}

	/**
	 * Convenience method to handle {@link BleDevice} connect events at a manager level. This listener simply reports when
	 * a device is connected, or has failed to connect -- {@link DeviceConnectListener#onEvent(Event)} may fire multiple times
	 * when {@link DeviceConnectListener.ConnectEvent#wasSuccess()} is <code>false</code>, as SweetBlue may be retrying the connection
	 * in the background, remember to check {@link DeviceConnectListener.ConnectEvent#isRetrying()}.
	 */
	public final void setListener_DeviceConnect(@Nullable(Prevalence.NORMAL) DeviceConnectListener listener_nullable)
	{
		m_managerImpl.setListener_DeviceConnect(listener_nullable);
	}

	/**
	 * Convenience method to set a default back up listener for all {@link BondEvent}s across all {@link BleDevice} instances.
	 */
	public final void setListener_Bond(@Nullable(Prevalence.NORMAL) BondListener listener_nullable)
	{
		m_managerImpl.setListener_Bond(listener_nullable);
	}

	/**
	 * Sets a default backup {@link ReadWriteListener} that will be called for all {@link BleDevice} instances.
	 * <br><br>
	 * TIP: Place some analytics code in the listener here.
	 */
	public final void setListener_Read_Write(@Nullable(Prevalence.NORMAL) ReadWriteListener listener_nullable)
	{
		m_managerImpl.setListener_Read_Write(listener_nullable);
	}

	public final void setListener_Notification(@Nullable(Prevalence.NORMAL) NotificationListener listener_nullable)
	{
		m_managerImpl.setListener_Notification(listener_nullable);
	}

	/**
	 * Set a listener here to be notified of the result of starting to advertise.
	 */
	public final void setListener_Advertising(AdvertisingListener listener)
	{
		m_managerImpl.setListener_Advertising(listener);
	}

	/**
	 * @deprecated use {@link #startScan(ScanOptions)} instead.
	 * This method will be removed in v3.1.
	 *
	 * Manually starts a periodic scan.
	 *
	 * @see com.idevicesinc.sweetblue.utils.BleSetupHelper
	 */
	@Deprecated
	public final void startPeriodicScan(Interval scanActiveTime, Interval scanPauseTime)
	{
		startScan(new ScanOptions().scanPeriodically(scanActiveTime, scanPauseTime));
	}

	/**
	 * @deprecated use {@link #startScan(ScanOptions)} instead.
	 * This method will be removed in v3.1.
	 *
	 * Same as {@link #startPeriodicScan(Interval, Interval)} but calls {@link #setListener_Discovery(DiscoveryListener)} for you too.
	 *
	 * @see com.idevicesinc.sweetblue.utils.BleSetupHelper
	 */
	@Deprecated
	public final void startPeriodicScan(Interval scanActiveTime, Interval scanPauseTime, DiscoveryListener discoveryListener)
	{
		startScan(new ScanOptions(discoveryListener).scanPeriodically(scanActiveTime, scanPauseTime));
	}

	/**
	 * @deprecated use {@link #startScan(ScanOptions)} instead.
	 * This method will be removed in v3.1.
	 *
	 * Same as {@link #startPeriodicScan(Interval, Interval)} but adds a filter too.
	 *
	 * @see com.idevicesinc.sweetblue.utils.BleSetupHelper
	 */
	@Deprecated
	public final void startPeriodicScan(Interval scanActiveTime, Interval scanPauseTime, ScanFilter filter)
	{
		startScan(new ScanOptions(filter).scanPeriodically(scanActiveTime, scanPauseTime));
	}

	/**
	 * @deprecated use {@link #startScan(ScanOptions)} instead.
	 * This method will be removed in v3.1.
	 *
	 * Same as {@link #startPeriodicScan(Interval, Interval)} but calls {@link #setListener_Discovery(DiscoveryListener)} for you too and adds a filter.
	 *
	 * @see com.idevicesinc.sweetblue.utils.BleSetupHelper
	 */
	@Deprecated
	public final void startPeriodicScan(Interval scanActiveTime, Interval scanPauseTime, ScanFilter filter, DiscoveryListener discoveryListener)
	{
		startScan(new ScanOptions(filter, discoveryListener)
				.withScanFilterApplyMode(ScanFilter.ApplyMode.CombineEither)
				.scanPeriodically(scanActiveTime, scanPauseTime));
	}

	/**
	 * @deprecated use {@link #startScan(ScanOptions)} instead.
	 * This method will be removed in v3.1.
	 *
	 * Same as {@link #startPeriodicScan(Interval, Interval)} but calls {@link #setListener_Discovery(DiscoveryListener)} for you too and adds a filter and a filter mode.
	 *
	 * @see com.idevicesinc.sweetblue.utils.BleSetupHelper
	 */
	@Deprecated
	public final void startPeriodicScan(Interval scanActiveTime, Interval scanPauseTime, ScanFilter filter, ScanFilter.ApplyMode filterApplyMode, DiscoveryListener discoveryListener)
	{
		m_managerImpl.startScan(new ScanOptions(filter, discoveryListener)
				.withScanFilterApplyMode(filterApplyMode)
				.scanPeriodically(scanActiveTime, scanPauseTime));
	}

	/**
	 * @deprecated use {@link #stopScan()} or {@link #stopScan(ScanFilter)} instead.
	 * This method will be removed in v3.1.
	 *
	 * Same as {@link #stopPeriodicScan()} but will also unregister any {@link ScanFilter} provided
	 * through {@link #startPeriodicScan(Interval, Interval, ScanFilter)} or other overloads.
	 */
	@Deprecated
	public final void stopPeriodicScan(final ScanFilter filter)
	{
		m_managerImpl.stopScan(filter);
	}

	/**
	 * @deprecated use {@link #stopScan()} or {@link #stopScan(ScanFilter)} instead.
	 * This method will be removed in v3.1.
	 *
	 * Stops a periodic scan previously started explicitly with {@link #startPeriodicScan(Interval, Interval)}.
	 */
	@Deprecated
	public final void stopPeriodicScan()
	{
		m_managerImpl.stopScan();
	}

	/**
	 * Starts a scan that will continue indefinitely until {@link #stopScan()} is called.
	 *
	 * @see com.idevicesinc.sweetblue.utils.BleSetupHelper
	 *
	 * @return <code>true</code> if scan started, <code>false</code> otherwise - usually this means this manager is not {@link BleManagerState#ON}.
	 */
	public final boolean startScan()
	{
		return startScan(Interval.INFINITE);
	}

	/**
	 * Calls {@link #startScan(Interval, ScanFilter)} with {@link Interval#INFINITE}.
	 *
	 * @see com.idevicesinc.sweetblue.utils.BleSetupHelper
	 *
	 * @return <code>true</code> if scan started, <code>false</code> otherwise - usually this means this manager is not {@link BleManagerState#ON}.
	 */
	public final boolean startScan(ScanFilter filter)
	{
		return startScan(Interval.INFINITE, filter, null);
	}

	/**
	 * Same as {@link #startScan()} but also calls {@link #setListener_Discovery(DiscoveryListener)} for you.
	 *
	 * @see com.idevicesinc.sweetblue.utils.BleSetupHelper
	 *
	 * @return <code>true</code> if scan started, <code>false</code> otherwise - usually this means this manager is not {@link BleManagerState#ON}.
	 */
	public final boolean startScan(DiscoveryListener discoveryListener)
	{
		return startScan(Interval.INFINITE, null, discoveryListener);
	}

	/**
	 * Overload of {@link #startScan(Interval, ScanFilter, DiscoveryListener)}
	 *
	 * @see com.idevicesinc.sweetblue.utils.BleSetupHelper
	 *
	 * @return <code>true</code> if scan started, <code>false</code> otherwise - usually this means this manager is not {@link BleManagerState#ON}.
	 */
	public final boolean startScan(Interval scanTime, ScanFilter filter)
	{
		return startScan(scanTime, filter, null);
	}

	/**
	 * Overload of {@link #startScan(Interval, ScanFilter, DiscoveryListener)}
	 *
	 * @see com.idevicesinc.sweetblue.utils.BleSetupHelper
	 *
	 * @return <code>true</code> if scan started, <code>false</code> otherwise - usually this means this manager is not {@link BleManagerState#ON}.
	 */
	public final boolean startScan(Interval scanTime, DiscoveryListener discoveryListener)
	{
		return startScan(scanTime, null, discoveryListener);
	}

	/**
	 * Same as {@link #startScan()} but also calls {@link #setListener_Discovery(DiscoveryListener)} for you.
	 *
	 * @see com.idevicesinc.sweetblue.utils.BleSetupHelper
	 *
	 * @return <code>true</code> if scan started, <code>false</code> otherwise - usually this means this manager is not {@link BleManagerState#ON}.
	 */
	public final boolean startScan(ScanFilter filter, DiscoveryListener discoveryListener)
	{
		return startScan(Interval.INFINITE, filter, discoveryListener);
	}

	/**
	 * Starts a scan that will generally last for the given time (roughly).
	 *
	 * @see com.idevicesinc.sweetblue.utils.BleSetupHelper
	 *
	 * @return <code>true</code> if scan started, <code>false</code> otherwise - usually this means this manager is not {@link BleManagerState#ON}.
	 */
	public final boolean startScan(Interval scanTime)
	{
		return startScan(scanTime, null, null);
	}

	/**
	 * Same as {@link #startScan(Interval)} but sets an ephemeral {@link #setListener_Discovery(DiscoveryListener)} that you provide.
	 * <br><br>
	 * WARNING: For {@link android.os.Build.VERSION_CODES#M} and up, in order for this method to return scan events
	 * through {@link ScanFilter} you must have {@link android.Manifest.permission#ACCESS_COARSE_LOCATION} or {@link android.Manifest.permission#ACCESS_FINE_LOCATION}
	 * in your AndroidManifest.xml, AND enabled at runtime (see {@link #isLocationEnabledForScanning_byRuntimePermissions()} and {@link #turnOnLocationWithIntent_forPermissions(Activity, int)}),
	 * AND location services should be enabled (see {@link #isLocationEnabledForScanning_byOsServices()} and {@link #isLocationEnabledForScanning_byOsServices()}).
	 * <br><br>
	 * The assumed reason why location must be enabled is that an app might scan for bluetooth devices like iBeacons with known physical locations and unique advertisement packets.
	 * Knowing the physical locations, the app could report back that you're definitely within ~50 ft. of a given longitude and latitude. With multiple beacons involved and/or fine-tuned RSSI-based
	 * distance calculations the location could get pretty accurate. For example a department store app could sprinkle a few dozen beacons throughout its store and
	 * if you had their app running they would know exactly where you are. Not an everyday concern, and it makes BLE even more annoying to implement on Android,
	 * but Google is understandably erring on the side of privacy and security for its users.
	 *
	 * @see com.idevicesinc.sweetblue.utils.BleSetupHelper
	 *
	 * @return <code>true</code> if scan started, <code>false</code> otherwise - usually this means this manager is not {@link BleManagerState#ON}.
	 */
	public final boolean startScan(Interval scanTime, ScanFilter filter, DiscoveryListener discoveryListener)
	{
		return startScan(new ScanOptions().scanFor(scanTime).withScanFilter(filter).withDiscoveryListener(discoveryListener));
	}

	public final boolean startScan(ScanOptions options)
	{
		return m_managerImpl.startScan(options);
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
		m_managerImpl.pushWakeLock();
	}

	/**
	 * Opposite of {@link #pushWakeLock()}, eventually calls {@link android.os.PowerManager.WakeLock#release()}.
	 */
	@Advanced
	public final void popWakeLock()
	{
		m_managerImpl.popWakeLock();
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
		return m_managerImpl.ASSERT(condition, message);
	}

	/**
	 * Returns the abstracted bitwise state mask representation of {@link BleManagerState} for the manager instance.
	 *
	 * @see BleManagerState
	 */
	public final int getStateMask()
	{
		return m_managerImpl.getStateMask();
	}

	/**
	 * Enables BLE if manager is currently {@link BleManagerState#OFF} or {@link BleManagerState#TURNING_OFF}, otherwise does nothing.
	 * For a convenient way to ask your user first see {@link #turnOnWithIntent(android.app.Activity, int)}.
	 */
	public final void turnOn()
	{
		m_managerImpl.turnOn();
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
	 *  @see BleManagerState#RESETTING
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
		m_managerImpl.reset(listener);
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
		m_managerImpl.nukeBle(resetListener);
	}

	/**
	 * Removes bonds for all devices that are {@link BleDeviceState#BONDED}.
	 * Essentially a convenience method for calling {@link BleDevice#unbond()},
	 * on each device individually.
	 */
	public final void unbondAll()
	{
		m_managerImpl.unbondAll();
	}

	/**
	 * Disconnects all devices that are {@link BleDeviceState#BLE_CONNECTED}.
	 * Essentially a convenience method for calling {@link BleDevice#disconnect()},
	 * on each device individually.
	 */
	public final void disconnectAll()
	{
		m_managerImpl.disconnectAll();
	}

	/**
	 * Same as {@link #disconnectAll()} but drills down to {@link BleDevice#disconnect_remote()} instead.
	 */
	public final void disconnectAll_remote()
	{
		m_managerImpl.disconnectAll_remote();
	}

	/**
	 * Undiscovers all devices that are {@link BleDeviceState#DISCOVERED}.
	 * Essentially a convenience method for calling {@link BleDevice#undiscover()},
	 * on each device individually.
	 */
	public final void undiscoverAll()
	{
		m_managerImpl.undiscoverAll();
	}

	/**
	 * If {@link #isLocationEnabledForScanning_byOsServices()} returns <code>false</code>, you can use this method to allow the user to enable location services.
	 * <br><br>
	 * NOTE: If {@link #isLocationEnabledForScanning_byOsServices()} returns <code>false</code> but all other overloads of {@link #isLocationEnabledForScanning()} return <code>true</code> then
	 * SweetBlue will fall back to classic discovery through {@link BluetoothAdapter#startDiscovery()} when you call {@link #startScan()} or overloads, so you may not have to use this.
	 *
	 * @see #isLocationEnabledForScanning_byOsServices()
	 * @see com.idevicesinc.sweetblue.utils.BleSetupHelper
	 */
	public final void turnOnLocationWithIntent_forOsServices(final Activity callingActivity, int requestCode)
	{
		m_managerImpl.turnOnLocationWithIntent_forOsServices(callingActivity, requestCode);
	}

	/**
	 * Overload of {@link #turnOnLocationWithIntent_forOsServices(Activity, int)} if you don't care about result.
	 *
	 * @see com.idevicesinc.sweetblue.utils.BleSetupHelper
	 */
	public final void turnOnLocationWithIntent_forOsServices(final Activity callingActivity)
	{
		m_managerImpl.turnOnLocationWithIntent_forOsServices(callingActivity);
	}

	/**
	 * Returns <code>true</code> if {@link #turnOnLocationWithIntent_forPermissions(Activity, int)} will pop a system dialog, <code>false</code> if it will bring
	 * you to the OS's Application Settings. The <code>true</code> case happens if the app has never shown a request Location Permissions dialog or has shown a request Location Permission dialog and the user has yet to select "Never ask again". This method is used to weed out the false
	 * negative from {@link Activity#shouldShowRequestPermissionRationale(String)} when the Location Permission has never been requested. Make sure to use this in conjunction with {@link #isLocationEnabledForScanning_byRuntimePermissions()}
	 * which will tell you if permissions are already enabled.
	 *
	 * @see com.idevicesinc.sweetblue.utils.BleSetupHelper
	 */
	public final boolean willLocationPermissionSystemDialogBeShown(Activity callingActivity)
	{
		return m_managerImpl.willLocationPermissionSystemDialogBeShown(callingActivity);
	}

	/**
	 * If {@link #isLocationEnabledForScanning_byOsServices()} returns <code>false</code>, you can use this method to allow the user to enable location
	 * through an OS intent. The result of the request (i.e. what the user chose) is passed back through {@link Activity#onRequestPermissionsResult(int, String[], int[])}
	 * with the requestCode provided as the second parameter to this method. If the user selected "Never ask again" the function will open up the app settings screen where the
	 * user can navigate to enable the permissions.
	 *
	 * @see #isLocationEnabledForScanning_byRuntimePermissions()
	 * @see com.idevicesinc.sweetblue.utils.BleSetupHelper
	 */
	public final void turnOnLocationWithIntent_forPermissions(final Activity callingActivity, int requestCode)
	{
		m_managerImpl.turnOnLocationWithIntent_forPermissions(callingActivity, requestCode);
	}

	/**
	 * This method will only do anything on devices running Android 12 or higher. This just makes the initial
	 * request for the necessary permissions depending on {@link BleManagerConfig#requestBackgroundOperation},
	 * and {@link BleManagerConfig#requestAdvertisePermission}. You will have to handle the result yourself, if
	 * you call this method. This method is used by the {@link com.idevicesinc.sweetblue.utils.BleSetupHelper},
	 * so if you are using that class, you don't need to call this method at all.
	 *
	 * @see com.idevicesinc.sweetblue.utils.BleSetupHelper
	 * @see <a href="https://developer.android.com/guide/topics/connectivity/bluetooth/permissions"></a>
	 */
	public final void requestBluetoothPermissions(final Activity callingActivity, int requestCode)
	{
		m_managerImpl.requestBluetoothPermissions(callingActivity, requestCode);
	}

	/**
	 * Tells you whether a call to {@link #startScan()} (or overloads), will succeed or not. Basically a convenience for checking if both
	 * {@link #isLocationEnabledForScanning()} and {@link #is(BleManagerState)} with {@link BleManagerState#SCANNING} return <code>true</code>.
	 */
	public final boolean isScanningReady()
	{
		return m_managerImpl.isScanningReady();
	}

	/**
	 * Convenience method which reports <code>true</code> if the {@link BleManager} is in any of the following states: <br></br>
	 * {@link BleManagerState#SCANNING}, {@link BleManagerState#SCANNING_PAUSED}, {@link BleManagerState#BOOST_SCANNING}, or {@link BleManagerState#STARTING_SCAN}
	 */
	public final boolean isScanning()
	{
		return m_managerImpl.isScanning();
	}

	/**
	 * Returns <code>true</code> if location is enabled to a degree that allows scanning on {@link android.os.Build.VERSION_CODES#M} and above.
	 * If this returns <code>false</code> it means you're on Android M and you either (A) do not have {@link android.Manifest.permission#ACCESS_COARSE_LOCATION}
	 * (or {@link android.Manifest.permission#ACCESS_FINE_LOCATION} in your AndroidManifest.xml, see {@link #isLocationEnabledForScanning_byManifestPermissions()}), or (B)
	 * runtime permissions for aformentioned location permissions are off (see {@link #isLocationEnabledForScanning_byRuntimePermissions()} and
	 * https://developer.android.com/training/permissions/index.html), or (C) location services on the phone are disabled (see {@link #isLocationEnabledForScanning_byOsServices()}).
	 * <br><br>
	 * If this returns <code>true</code> then you are good to go for calling {@link #startScan()}.
	 *
	 * @see #startScan(Interval, ScanFilter, DiscoveryListener)
	 *
	 * @see #turnOnLocationWithIntent_forPermissions(Activity, int)
	 * @see #turnOnLocationWithIntent_forOsServices(Activity)
	 * @see #turnOnLocationWithIntent_forOsServices(Activity, int)
	 *
	 * @see com.idevicesinc.sweetblue.utils.BleSetupHelper
	 */
	public final boolean isLocationEnabledForScanning()
	{
		return m_managerImpl.isLocationEnabledForScanning();
	}

	/**
	 * Returns <code>true</code> if you're either pre-Android-M, or app has permission for either {@link android.Manifest.permission#ACCESS_COARSE_LOCATION}
	 * or {@link android.Manifest.permission#ACCESS_FINE_LOCATION} in your AndroidManifest.xml, <code>false</code> otherwise.
	 *
	 * @see #startScan(Interval, ScanFilter, DiscoveryListener)
	 *
	 * @see com.idevicesinc.sweetblue.utils.BleSetupHelper
	 */
	public final boolean isLocationEnabledForScanning_byManifestPermissions()
	{
		return m_managerImpl.isLocationEnabledForScanning_byManifestPermissions();
	}

	/**
	 * Returns <code>true</code> if you're either pre-Android-M, or app has runtime permissions enabled by checking
	 * <a href="https://developer.android.com/reference/android/support/v4/content/ContextCompat.html#checkSelfPermission(android.content.Context, java.lang.String)"></a>
	 * See more information at https://developer.android.com/training/permissions/index.html.
	 *
	 * @see #startScan(Interval, ScanFilter, DiscoveryListener)
	 *
	 * @see #turnOnLocationWithIntent_forPermissions(Activity, int)
	 *
	 * @see com.idevicesinc.sweetblue.utils.BleSetupHelper
	 */
	public final boolean isLocationEnabledForScanning_byRuntimePermissions()
	{
		return m_managerImpl.isLocationEnabledForScanning_byRuntimePermissions();
	}

	/**
	 * Returns <code>true</code> if running on Android 12 or higher, and the necessary Bluetooth permissions have
	 * been granted. This takes into account {@link BleManagerConfig#requestBackgroundOperation}, and
	 * {@link BleManagerConfig#requestAdvertisePermission}.
	 *
	 * @see #requestBluetoothPermissions(Activity, int)
	 */
	public final boolean areBluetoothPermissionsEnabled()
	{
		return m_managerImpl.areBluetoothPermissionsEnabled();
	}

	/**
	 * Returns <code>true</code> if you're either pre-Android-M, or location services are enabled, the same is if you go to the Android Settings app
	 * and manually toggle Location ON/OFF.
	 * <br><br>
	 * NOTE: If this returns <code>false</code> but all other overloads of {@link #isLocationEnabledForScanning()} return <code>true</code> then
	 * SweetBlue will fall back to classic discovery through {@link BluetoothAdapter#startDiscovery()} when you call {@link #startScan()} or overloads.
	 *
	 * @see #startScan(Interval, ScanFilter, DiscoveryListener)
	 *
	 * @see #turnOnLocationWithIntent_forOsServices(Activity)
	 * @see #turnOnLocationWithIntent_forOsServices(Activity, int)
	 *
	 * @see com.idevicesinc.sweetblue.utils.BleSetupHelper
	 */
	public final boolean isLocationEnabledForScanning_byOsServices()
	{
		return m_managerImpl.isLocationEnabledForScanning_byOsServices();
	}

	/**
	 * Convenience method to request your user to enable ble in a "standard" way
	 * with an {@link android.content.Intent} instead of using {@link #turnOn()} directly.
	 * Result will be posted as normal to {@link android.app.Activity#onActivityResult(int, int, Intent)}.
	 * If current state is {@link BleManagerState#ON} or {@link BleManagerState#TURNING_ON}
	 * this method early outs and does nothing.
	 *
	 * @see com.idevicesinc.sweetblue.utils.BleSetupHelper
	 */
	public final void turnOnWithIntent(Activity callingActivity, int requestCode)
	{
		m_managerImpl.turnOnWithIntent(callingActivity, requestCode);
	}

	/**
	 * This method is automatically called for you, if {@link BleManagerConfig#autoPauseResumeDetection} is set to <code>true</code>, which is the default.
	 * The below comment only applies if the autoPauseResumeDetection is set to <code>false</code>.
	 *
	 * Opposite of {@link #onPause()}, to be called from your override of {@link android.app.Activity#onResume()} for each {@link android.app.Activity}
	 * in your application. See comment for {@link #onPause()} for a similar explanation for why you should call this method.
	 */
	public final void onResume()
	{
		m_managerImpl.onResume();
	}

	/**
	 * This method is automatically called for you, if {@link BleManagerConfig#autoPauseResumeDetection} is set to <code>true</code>, which is the default.
	 * The below comment only applies if the autoPauseResumeDetection is set to <code>false</code>.
	 *
	 * It's generally recommended to call this in your override of {@link android.app.Activity#onPause()} for each {@link android.app.Activity}
	 * in your application. This doesn't do much for now, just a little bookkeeping and stops scan automatically if
	 * {@link BleManagerConfig#stopScanOnPause} is <code>true</code>. Strictly speaking you don't *have* to call this method,
	 * but another good reason is for future-proofing. Later releases of this library may do other more important things
	 * in this method so it's good to have it being called just in case.
	 */
	public final void onPause()
	{
		m_managerImpl.onPause();
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
		m_managerImpl.shutdown();
		s_instance = null;
	}

	/**
	 * Returns the {@link android.app.Application} provided to the constructor.
	 */
	public final Context getApplicationContext()
	{
		return m_managerImpl.getApplicationContext();
	}

	/**
	 * @deprecated use {@link #stopScan()} or {@link #stopScan(ScanFilter)} instead.
	 * This method will be removed in v3.1.
	 *
	 * Convenience that will call both {@link #stopPeriodicScan()} and {@link #stopScan()} for you.
	 */
	@Deprecated
	public final void stopAllScanning()
	{
		stopPeriodicScan();
		stopScan();
	}

	/**
	 * Stops any scans previously started by {@link #startScan()}.
	 */
	public final void stopScan()
	{
		m_managerImpl.stopScan();
	}

	/**
	 * Same as {@link #stopScan()} but also unregisters any filter supplied to various overloads of {@link #startScan()}.
	 *
	 * Calling {@link #stopScan()} alone will keep any previously registered filters active.
	 */
	public final void stopScan(ScanFilter filter)
	{
		m_managerImpl.stopScan(filter);
	}

	/**
	 * Stops a {@link PendingIntent} scan; you can initiate one by supplying your PendingIntent instance to the
	 * {@link ScanOptions} class via {@link ScanOptions#withPendingIntent(PendingIntent)}.
	 *
	 * NOTE: You may still receive some callbacks after calling this method, as it seems the android stack batches
	 * these results, and calls them in order.
	 */
	public final void stopScan(PendingIntent pendingIntent)
	{
		m_managerImpl.stopScan(pendingIntent);
	}

	/**
	 * Gets a known {@link BleDeviceState#DISCOVERED} device by MAC address, or {@link BleDevice#NULL} if there is no such device.
	 */
	public final @Nullable(Prevalence.NEVER) BleDevice getDevice(final String macAddress)
	{
		return convert(m_managerImpl.getDevice(macAddress));
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
	public final boolean hasDevice(final BleDevice device)
	{
		return hasDevice(device.getMacAddress());
	}

	/**
	 * Returns the first device that is in the given state, or {@link BleDevice#NULL} if no match is found.
	 */
	public final @Nullable(Prevalence.NEVER) BleDevice getDevice(BleDeviceState state)
	{
		return convert(m_managerImpl.getDevice(state));
	}

	/**
	 * Returns true if we have a device in the given state.
	 */
	public final boolean hasDevice(BleDeviceState state)
	{
		return !getDevice(state).isNull();
	}

	/**
	 * Returns the first device that matches the query, or {@link BleDevice#NULL} if no match is found.
	 * See {@link BleDevice#is(Object...)} for the query format.
	 */
	public final @Nullable(Prevalence.NEVER) BleDevice getDevice(Object ... query)
	{
		return convert(m_managerImpl.getDevice(query));
	}

	/**
	 * Returns true if we have a device that matches the given query.
	 * See {@link BleDevice#is(Object...)} for the query format.
	 */
	public final boolean hasDevice(Object ... query)
	{
		return !getDevice(query).isNull();
	}

	/**
	 * Returns the first device which returns <code>true</code> for {@link BleDevice#isAny(int)}, or {@link BleDevice#NULL} if no such device is found.
	 */
	public final @Nullable(Prevalence.NEVER) BleDevice getDevice(final int mask_BleDeviceState)
	{
		return convert(m_managerImpl.getDevice(mask_BleDeviceState));
	}

	/**
	 * Returns <code>true</code> if there is any {@link BleDevice} for which {@link BleDevice#isAny(int)} with the given mask returns <code>true</code>.
	 */
	public final boolean hasDevice(final int mask_BleDeviceState)
	{
		return !getDevice(mask_BleDeviceState).isNull();
	}

	/**
	 * This is a convenience method to get an {@link ArrayList} of {@link BleDevice}s from the Intent
	 * which was delivered as a result of calling {@link #startScan(ScanOptions)} when using a
	 * {@link android.app.PendingIntent}.
	 */
	public final List<BleDevice> getDevices(final Intent intentFromScan)
	{
		return m_managerImpl.getDevices(intentFromScan);
	}

	/**
	 * Offers a more "functional" means of iterating through the internal list of devices instead of
	 * using {@link #getDevices()} or {@link #getDevices_List()}.
	 */
	public final void getDevices(final ForEach_Void<BleDevice> forEach)
	{
		m_managerImpl.getDevices(forEach);
	}

	/**
	 * Same as {@link #getDevices(ForEach_Void)} but will only return devices
	 * in the given state provided.
	 */
	public final void getDevices(final ForEach_Void<BleDevice> forEach, final BleDeviceState state)
	{
		m_managerImpl.getDevices(forEach, state);
	}

	/**
	 * Overload of {@link #getDevices(ForEach_Void)}
	 * if you need to break out of the iteration at any point.
	 */
	public final void getDevices(final ForEach_Breakable<BleDevice> forEach)
	{
		m_managerImpl.getDevices(forEach);
	}

	/**
	 * Overload of {@link #getDevices(ForEach_Void, BleDeviceState)}
	 * if you need to break out of the iteration at any point.
	 */
	public final void getDevices(final ForEach_Breakable<BleDevice> forEach, final BleDeviceState state)
	{
		m_managerImpl.getDevices(forEach, state);
	}

	/**
	 * Returns the mac addresses of all devices that we know about from both current and previous
	 * app sessions.
	 */
	public final @Nullable(Prevalence.NEVER) Iterator<String> getDevices_previouslyConnected()
	{
		return m_managerImpl.getDevices_previouslyConnected();
	}

	/**
	 * Convenience method to return a {@link Set} of currently bonded devices. This simply calls
	 * {@link BluetoothAdapter#getBondedDevices()}, and wraps all bonded devices into separate
	 * {@link BleDevice} classes.
	 *
	 * NOTE: If the Bluetooth radio is turned off, some android devices return <code>null</code>. In this case,
	 * SweetBlue will just return an empty list.
     */
	public final Set<BleDevice> getDevices_bonded()
	{
		return convertSet(m_managerImpl, m_managerImpl.getDevices_bonded());
	}

	/**
	 * Returns all the devices managed by this class. This generally includes all devices that are either.
	 * {@link BleDeviceState#ADVERTISING} or {@link BleDeviceState#BLE_CONNECTED}.
	 */
	public final @Nullable(Prevalence.NEVER) BleDeviceIterator getDevices()
	{
		return new BleDeviceIterator(getDevices_List());
	}

	/**
	 * Same as {@link #getDevices()}, but with the devices sorted using {@link BleManagerConfig#defaultListComparator}, which
	 * by default sorts by {@link BleDevice#getName_debug()}.
	 */
	public final @Nullable(Prevalence.NEVER) BleDeviceIterator getDevices_sorted()
	{
		return new BleDeviceIterator(getDevices_List_sorted());
	}

	/**
	 * Overload of {@link #getDevices()} that returns a {@link java.util.List} for you.
	 */
	public final @Nullable(Prevalence.NEVER) List<BleDevice> getDevices_List()
	{
		return convertList(m_managerImpl, m_managerImpl.getDevices_List());
	}

	/**
	 * Same as {@link #getDevices_List()}, but sorts the list using {@link BleManagerConfig#defaultListComparator}.
	 */
	public final @Nullable(Prevalence.NEVER) List<BleDevice> getDevices_List_sorted()
	{
		return convertList(m_managerImpl, m_managerImpl.getDevices_List_sorted());
	}

	/**
	 * Returns the total number of devices this manager is...managing.
	 * This includes all devices that are {@link BleDeviceState#DISCOVERED}.
	 */
	public final int getDeviceCount()
	{
		return m_managerImpl.getDeviceCount();
	}

	/**
	 * Returns the number of devices that are in the current state.
	 */
	public final int getDeviceCount(BleDeviceState state)
	{
		return m_managerImpl.getDeviceCount(state);
	}

	/**
	 * Returns the number of devices that match the given query.
	 * See {@link BleDevice#is(Object...)} for the query format.
	 */
	public final int getDeviceCount(Object ... query)
	{
		return m_managerImpl.getDeviceCount(query);
	}

	/**
	 * Returns whether we have any devices. For example if you have never called {@link #startScan()}
	 * or {@link #newDevice(String)} (or overloads) then this will return false.
	 */
	public final boolean hasDevices()
	{
		return m_managerImpl.hasDevices();
	}

	/**
	 * Same as {@link #getDevice(BleDeviceState)} except returns all matching devices.
	 */
	public final @Nullable(Prevalence.NEVER) BleDeviceIterator getDevices(final BleDeviceState state)
	{
		return new BleDeviceIterator(getDevices_List(), state, true);
	}

	/**
	 * Overload of {@link #getDevices(BleDeviceState)} that returns a {@link java.util.List} for you.
	 */
	public final @Nullable(Prevalence.NEVER) List<BleDevice> getDevices_List(final BleDeviceState state)
	{
		return convertList(m_managerImpl, m_managerImpl.getDevices_List(state));
	}

	/**
	 * Same as {@link #getDevices_List(BleDeviceState)} except the list is sorted using {@link BleManagerConfig#defaultListComparator}.
	 */
	public final @Nullable(Prevalence.NEVER) List<BleDevice> getDevices_List_sorted(final BleDeviceState state)
	{
		return convertList(m_managerImpl, m_managerImpl.getDevices_List_sorted(state));
	}

	/**
	 * Same as {@link #getDevice(Object...)} except returns all matching devices.
	 * See {@link BleDevice#is(Object...)} for the query format.
	 */
	public final @Nullable(Prevalence.NEVER) BleDeviceIterator getDevices(final Object ... query)
	{
		return new BleDeviceIterator(getDevices_List(), query);
	}

	/**
	 * Overload of {@link #getDevices(Object...)} that returns a {@link java.util.List} for you.
	 */
	public final @Nullable(Prevalence.NEVER) List<BleDevice> getDevices_List(final Object ... query)
	{
		return convertList(m_managerImpl, m_managerImpl.getDevices_List(query));
	}

	/**
	 * Same as {@link #getDevices_List(Object...)} except the list is sorted using {@link BleManagerConfig#defaultListComparator}.
	 */
	public final @Nullable(Prevalence.NEVER) List<BleDevice> getDevices_List_sorted(final Object ... query)
	{
		return convertList(m_managerImpl, m_managerImpl.getDevices_List_sorted(query));
	}

	/**
	 * Same as {@link #getDevices()} except filters using {@link BleDevice#isAny(int)}.
	 */
	public final @Nullable(Prevalence.NEVER) BleDeviceIterator getDevices(final int mask_BleDeviceState)
	{
		return new BleDeviceIterator(getDevices_List(), mask_BleDeviceState);
	}

	/**
	 * Overload of {@link #getDevices(int)} that returns a {@link java.util.List} for you.
	 */
	public final @Nullable(Prevalence.NEVER) List<BleDevice> getDevices_List(final int mask_BleDeviceState)
	{
		return convertList(m_managerImpl, m_managerImpl.getDevices_List(mask_BleDeviceState));
	}

	/**
	 * Same as {@link #getDevices_List(int)} except the list is sorted using {@link BleManagerConfig#defaultListComparator}.
	 */
	public final @Nullable(Prevalence.NEVER) List<BleDevice> getDevices_List_sorted(final int mask_BleDeviceState)
	{
		return convertList(m_managerImpl, m_managerImpl.getDevices_List_sorted(mask_BleDeviceState));
	}

	/**
	 * Removes the given {@link BleDevice} from SweetBlue's internal device cache list. You should never have to call this
	 * yourself (and probably shouldn't), but it's here for flexibility.
     */
	@Advanced
	public final void removeDeviceFromCache(BleDevice device)
	{
		m_managerImpl.removeDeviceFromCache(device.getIBleDevice());
	}

	/**
	 * Removes all {@link BleDevice}s from SweetBlue's internal device cache list. You should never have to call this
	 * yourself (and probably shouldn't), but it's here for flexibility.
	 */
	@Advanced
	public final void removeAllDevicesFromCache()
	{
		m_managerImpl.removeAllDevicesFromCache();
	}

	/**
	 * Returns a new {@link HistoricalData} instance using
	 * {@link BleDeviceConfig#historicalDataFactory} if available.
	 */
	public final HistoricalData newHistoricalData(final byte[] data, final EpochTime epochTime)
	{
		return m_managerImpl.newHistoricalData(data, epochTime);
	}

	/**
	 * Same as {@link #newHistoricalData(byte[], EpochTime)} but tries to use
	 * {@link BleDevice#newHistoricalData(byte[], EpochTime)} if we have a device
	 * matching the given mac address.
	 */
	public final HistoricalData newHistoricalData(final byte[] data, final EpochTime epochTime, final String macAddress)
	{
		return m_managerImpl.newHistoricalData(data, epochTime, macAddress);
	}

	/**
	 * Overload of {@link BleManager#getServer(IncomingListener)} without any initial set-up parameters.
	 */
	public final BleServer getServer()
	{
		return getServer((IncomingListener) null);
	}

	/**
	 * Returns a {@link BleServer} instance. which for now at least is a singleton.
	 */
	public final BleServer getServer(final IncomingListener incomingListener)
	{
		return m_managerImpl.getBleServer(m_managerImpl.getServer(incomingListener));
	}

	/**
	 * Overload of {@link BleManager#getServer(GattDatabase, AddServiceListener)}, with no {@link AddServiceListener} set.
	 */
	public final BleServer getServer(final GattDatabase gattDatabase)
	{
		return getServer(gattDatabase, null);
	}

	/**
	 * Overload of {@link BleManager#getServer(IncomingListener, GattDatabase, AddServiceListener)}, with no {@link IncomingListener} set.
	 */
	public final BleServer getServer(final GattDatabase gattDatabase, AddServiceListener addServiceListener)
	{
		return getServer(null, gattDatabase, addServiceListener);
	}

	/**
	 * Returns a {@link BleServer} instance. This is now the preferred method to retrieve the server instance.
	 */
	public final BleServer getServer(final IncomingListener incomingListener, final GattDatabase gattDatabase, final AddServiceListener addServiceListener)
	{
		return m_managerImpl.getBleServer(m_managerImpl.getServer(incomingListener, gattDatabase, addServiceListener));
	}

	/**
	 * Same as {@link #newDevice(String, String, BleDeviceConfig)} but uses an empty string for the name
	 * and passes a <code>null</code> {@link BleDeviceConfig}, which results in inherited options from {@link BleManagerConfig}.
	 */
	public final @Nullable(Prevalence.NEVER) BleDevice newDevice(String macAddress)
	{
		return newDevice(macAddress, null, null);
	}

	/**
	 * Similar to {@link #newDevice(String)}, but this method accepts a native {@link BluetoothDevice} as an argument. This should only
	 * ever really be used when using a {@link android.app.PendingIntent} scan.
	 */
	public final @Nullable(Prevalence.NEVER) BleDevice newDevice(BluetoothDevice nativeDevice)
	{
		return newDevice(nativeDevice, null);
	}

	/**
	 * Same as {@link #newDevice(BluetoothDevice)}, but allows passing in a {@link BleDeviceConfig} as well.
	 */
	@SuppressLint("MissingPermission")
	public final @Nullable(Prevalence.NEVER) BleDevice newDevice(BluetoothDevice nativeDevice, BleDeviceConfig config)
	{
		return newDevice(nativeDevice.getAddress(), nativeDevice.getName(), config);
	}

	/**
	 * Same as {@link #newDevice(String)} but allows a custom name also.
	 */
	public final @Nullable(Prevalence.NEVER) BleDevice newDevice(final String macAddress, final String name)
	{
		return newDevice(macAddress, name, null);
	}

	/**
	 * Same as {@link #newDevice(String)} but passes a {@link BleDeviceConfig} to be used as well.
	 */
	public final @Nullable(Prevalence.NEVER) BleDevice newDevice(final String macAddress, final BleDeviceConfig config)
	{
		return newDevice(macAddress, null, config);
	}

	/**
	 * Overload of {@link #newDevice(String, String, byte[], BleDeviceConfig)}.
	 */
	public final @Nullable(Prevalence.NEVER) BleDevice newDevice(final String macAddress, final String name, final BleDeviceConfig config)
	{
		return newDevice(macAddress, name, null, config);
	}

	/**
	 * Creates a new {@link BleDevice} or returns an existing one if the macAddress matches.
	 * {@link DiscoveryListener#onEvent(DiscoveryListener.DiscoveryEvent)} will be called if a new device
	 * is created.
	 * <br><br>
	 * NOTE: You should always do a {@link BleDevice#isNull()} check on this method's return value just in case. Android
	 * documentation says that underlying stack will always return a valid {@link android.bluetooth.BluetoothDevice}
	 * instance (which is required to create a valid {@link BleDevice} instance), but you really never know.
	 */
	public final @Nullable(Prevalence.NEVER) BleDevice newDevice(final String macAddress, final String name, byte[] scanRecord, final BleDeviceConfig config)
	{
		return m_managerImpl.getBleDevice(m_managerImpl.newDevice(macAddress, name, scanRecord, config));
	}

	/**
	 * Forcefully undiscovers a device, disconnecting it first if needed and removing it from this manager's internal list.
	 * {@link DiscoveryListener#onEvent(DiscoveryListener.DiscoveryEvent)} with {@link LifeCycle#UNDISCOVERED} will be called.
	 * No clear use case has been thought of but the method is here just in case anyway.
	 *
	 * @return	<code>true</code> if the device was undiscovered, <code>false</code> if device is already {@link BleDeviceState#UNDISCOVERED} or manager
	 * 			doesn't contain an instance, checked referentially, not through {@link BleDevice#equals(BleDevice)} (i.e. by mac address).
	 */
	public final boolean undiscover(final BleDevice device)
	{
		return m_managerImpl.undiscover(device.getIBleDevice());
	}

	/**
	 * This method will clear the task queue of all tasks.
	 * NOTE: This can really mess things up, especially if you're currently trying to connect to a device. Only use this if you absolutely have to!
	 */
	@Advanced
	public final void clearQueue()
	{
		m_managerImpl.clearQueue();
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
		m_managerImpl.clearSharedPreferences(macAddress);
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
		m_managerImpl.clearSharedPreferences();
	}

	/**
	 * Returns this manager's knowledge of the app's foreground state.
	 */
	public final boolean isForegrounded()
	{
		return m_managerImpl.isForegrounded();
	}

	@Override public final String toString()
	{
		return m_managerImpl.toString();
	}




	IBleManager getIBleManager()
	{
		return m_managerImpl;
	}




	private BleDevice convert(IBleDevice device)
	{
		return m_managerImpl.getBleDevice(device);
	}

	private static List<BleDevice> convertList(IBleManager mgr, List<IBleDevice> internalList)
	{
		final List<BleDevice> list = new ArrayList<>(internalList.size());
		for (int i = 0; i < internalList.size(); i++)
		{
			list.add(mgr.getBleDevice(internalList.get(i)));
		}
		return list;
	}

	private static Set<BleDevice> convertSet(IBleManager mgr, Set<IBleDevice> deviceSet)
	{
		final Set<BleDevice> set = new HashSet<>(deviceSet.size());
		for (IBleDevice d : deviceSet)
		{
			set.add(mgr.getBleDevice(d));
		}
		return set;
	}

}
