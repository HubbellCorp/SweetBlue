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

import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;

import com.idevicesinc.sweetblue.annotations.Advanced;
import com.idevicesinc.sweetblue.DiscoveryListener.LifeCycle;
import com.idevicesinc.sweetblue.annotations.Extendable;
import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.annotations.Immutable;
import com.idevicesinc.sweetblue.annotations.Nullable.Prevalence;
import com.idevicesinc.sweetblue.annotations.UnitTest;
import com.idevicesinc.sweetblue.defaults.DefaultDeviceReconnectFilter;
import com.idevicesinc.sweetblue.defaults.DefaultReconnectFilter;
import com.idevicesinc.sweetblue.internal.IBleDevice;
import com.idevicesinc.sweetblue.internal.android.IBluetoothDevice;
import com.idevicesinc.sweetblue.internal.android.IBluetoothGatt;
import com.idevicesinc.sweetblue.utils.*;

import org.json.JSONObject;

/**
 * Provides a number of options to (optionally) pass to {@link BleDevice#setConfig(BleDeviceConfig)}.
 * This class is also a super class of {@link BleManagerConfig}, which you can pass
 * to {@link BleManager#get(Context, BleManagerConfig)} or {@link BleManager#setConfig(BleManagerConfig)} to set default base options for all devices at once.
 * For all options in this class, you may set the value to <code>null</code> when passed to {@link BleDevice#setConfig(BleDeviceConfig)}
 * and the value will then be inherited from the {@link BleManagerConfig} passed to {@link BleManager#get(Context, BleManagerConfig)}.
 * Otherwise, if the value is not <code>null</code> it will override any option in the {@link BleManagerConfig}.
 * If an option is ultimately <code>null</code> (<code>null</code> when passed to {@link BleDevice#setConfig(BleDeviceConfig)}
 * *and* {@link BleManager#get(Context, BleManagerConfig)}) then it is interpreted as <code>false</code> or {@link Interval#DISABLED}.
 * <br><br>
 * TIP: You can use {@link Interval#DISABLED} instead of <code>null</code> to disable any keepalive-based options, for code readability's sake.
 * <br><br>
 * TIP: You can use {@link #newNulled()} (or {@link #nullOut()}) then only set the few options you want for {@link BleDevice#setConfig(BleDeviceConfig)}.
 */
@Extendable
@SuppressWarnings("squid:ClassVariableVisibilityCheck")
public class BleDeviceConfig extends BleNodeConfig implements Cloneable
{
    /**
     * Default value for {@link #minScanTimeNeededForUndiscovery}.
     */
    public static final double DEFAULT_MINIMUM_SCAN_TIME = 5.0;

    /**
     * Default value for {@link #nForAverageRunningReadTime} and {@link #nForAverageRunningWriteTime}.
     */
    public static final int DEFAULT_RUNNING_AVERAGE_N = 10;

    /**
     * This is a good default value for {@link #undiscoveryKeepAlive}. By default {@link #undiscoveryKeepAlive} is {@link Interval#DISABLED}.
     */
    public static final double DEFAULT_SCAN_KEEP_ALIVE = DEFAULT_MINIMUM_SCAN_TIME * 2.5;

    /**
     * Default value for {@link #rssiAutoPollRate}.
     */
    public static final double DEFAULT_RSSI_AUTO_POLL_RATE = 10.0;

    /**
     * Default fallback value for {@link #rssi_min}.
     */
    public static final int DEFAULT_RSSI_MIN = -120;

    /**
     * Default fallback value for {@link #rssi_max}.
     */
    public static final int DEFAULT_RSSI_MAX = -30;

    /**
     * Default value for {@link #defaultTxPower}.
     */
    public static final int DEFAULT_TX_POWER = -50;

    /**
     * The default value of {@link #maxConnectionFailHistorySize}, the size of the list that keeps track of a {@link BleNode}'s connection failure history.
     * This is to prevent the list from growing too large, if the device is unable to connect, and you have a large long term reconnect time set
     * with {@link #reconnectFilter}.
     */
    public static final int DEFAULT_MAX_CONNECTION_FAIL_HISTORY_SIZE = 25;

    /**
     * This only applies when {@link #useGattRefresh} is <code>true</code>. This is the default amount of time to delay after
     * refreshing the gatt database before actually performing the discover services operation. It has been observed that this delay
     * alleviates some instability when {@link #useGattRefresh} is <code>true</code>.
     */
    public static final int DEFAULT_GATT_REFRESH_DELAY = 500;

    /**
     * The default value used for {@link BondRetryFilter.DefaultBondRetryFilter}. Bond retries only apply when calling {@link BleDevice#bond()}, or {@link BleDevice#bond(BondListener)}.
     * Like connecting, sometimes in order to get bonding to work, you just have to try multiple times. If you require bonding for the device you're connecting
     * to, it's recommended to use one of the bond methods.
     */
    public static final int DEFAULT_MAX_BOND_RETRIES = 3;

    /**
     * The default value used for {@link #connectionBugFixTimeout}. Android leaves a connection open to a device on it's first bond. As the fix for this is rather
     * convoluted, the library has a timeout to forget about the fix if it hits this timeout value. This value is in seconds.
     */
    public static final int DEFAULT_CONNECTION_BUG_FIX_TIMEOUT = 60;


    /**
     * Default is {@link #DEFAULT_CONNECTION_BUG_FIX_TIMEOUT}. This sets the timeout length for the connection open bug.
     *
     * @see #DEFAULT_CONNECTION_BUG_FIX_TIMEOUT
     */
    public Interval connectionBugFixTimeout = Interval.secs(DEFAULT_CONNECTION_BUG_FIX_TIMEOUT);

    /**
     * Default is {@link Phy#DEFAULT}. This setting is only really used for Bluetooth 5 devices. The android device needs to be running Oreo for any option other than
     * {@link Phy#DEFAULT} to work (if not, SweetBlue will fall back to the default option). This allows you to specify if SweetBlue should use the high speed,
     * long range, or default features. Set it here to be applied to the device after connecting, or you can also call {@link BleDevice#setPhyOptions(Phy)}
     */
    public Phy phyOptions = Phy.DEFAULT;

    /**
     * Default is <code>false</code>. If the bluetooth device you are trying to connect to requires a pairing dialog to show up, you should
     * set this to <code>true</code>. Android will do one of two things when you try to pair to the device. It will either A) show the pairing dialog, or
     * B) show a notification in the notification area. When B happens, most people probably won't notice it, and think your app can't connect to the device.
     * This uses an ugly hack to get the dialog to always display...it starts a CLASSIC bluetooth scan for a second, then stops it, and starts the bond. As crazy
     * as it sounds, it works. Note that no devices will be discovered during this one second scan.
     */
    public boolean forceBondDialog = false;

    /**
     * Default is {@link Interval#ONE_SEC}. This setting only applies if {@link #forceBondDialog} is <code>true</code>. This sets the amount of time to run the classic
     * scan for before attempting to bond. If this is set to {@link Interval#DISABLED}, or is <code>null</code>, and {@link #forceBondDialog} is set to <code>true</code>,
     * then the default value will be used.
     *
     * @see #forceBondDialog
     */
    public Interval forceBondHackInterval = Interval.ONE_SEC;

    /**
     * Default is {@link #DEFAULT_GATT_REFRESH_DELAY}. This only applies when {@link #useGattRefresh} is <code>true</code>. This is the amount of time to delay after
     * refreshing the gatt database before actually performing the discover services operation. It has been observed that this delay
     * alleviates some instability when {@link #useGattRefresh} is <code>true</code>.
     */
    public Interval gattRefreshDelay = Interval.millis(DEFAULT_GATT_REFRESH_DELAY);

    /**
     * Default is {@link Interval#DISABLED}. This option adds a delay between establishing a BLE connection, and service discovery, if {@link #autoGetServices} is
     * <code>true</code>. This value will be ignored if {@link #useGattRefresh} is <code>true</code>, as the library will use {@link #gattRefreshDelay} instead.
     */
    public Interval serviceDiscoveryDelay = Interval.DISABLED;

    /**
     * Default is <code>true</code> - some devices can only reliably become {@link BleDeviceState#BONDED} while {@link BleDeviceState#BLE_DISCONNECTED},
     * so this option controls whether the library will internally change any bonding flow dictated by {@link #bondFilter} when a bond fails and try
     * to bond again the next time the device is {@link BleDeviceState#BLE_DISCONNECTED}.
     * <br><br>
     * NOTE: This option was added after noticing this behavior with the Samsung Tab 4 running 4.4.4.
     */
    @Nullable(Prevalence.NORMAL)
    public Boolean tryBondingWhileDisconnected = true;

    /**
     * Default is <code>false</code> - Controls whether SweetBlue will automatically bond when connecting to a peripheral (rather than letting Android do it itself).
     * If the device is already bonded, this will do nothing. In most cases, it's best to bond <i>before</i> connecting, but there are rare devices which work better
     * to bond <i>after</i> becoming connected. To adjust this behavior, adjust {@link #tryBondingWhileDisconnected} (if it's <code>true</code>, then the bond will happen
     * before connecting, otherwise it will happen after).
     */
    @Advanced
    public boolean alwaysBondOnConnect = false;

    /**
     * Default is {@link BleDeviceState#DEFAULT_STATES}. This specifies which {@link BleDeviceState}s should be posted to the {@link DeviceStateListener}. The default contains
     * all the states an app should care about. If you want more fine grained state changes (for debugging for instance), then you can specify which states you want to know about
     * here.
     */
    @Advanced
    public BleDeviceState[] defaultDeviceStates = BleDeviceState.DEFAULT_STATES;

    /**
     * Default is <code>true</code> - controls whether changes to a device's name through {@link BleDevice#setName(String)} are remembered on disk through
     * {@link SharedPreferences}. If true, this means calls to {@link com.idevicesinc.sweetblue.BleDevice#getName_override()} will return the same thing
     * even across app restarts.
     */
    @Nullable(Prevalence.NORMAL)
    public Boolean saveNameChangesToDisk = true;

    /**
     * Default is an instance of {@link DefaultReconnectFilter} using the timings that are <code>public static final</code> members thereof - set your own implementation here to
     * have fine-grain control over reconnect behavior while a device is {@link BleDeviceState#RECONNECTING_LONG_TERM} or {@link BleDeviceState#RECONNECTING_SHORT_TERM}.
     * This is basically how often and how long the library attempts to reconnect to a device that for example may have gone out of range. Set this variable to
     * <code>null</code> if reconnect behavior isn't desired. If not <code>null</code>, your app may find
     * {@link BleManagerConfig#manageCpuWakeLock} useful in order to force the app/phone to stay awake while attempting a reconnect.
     *
     * @see BleManagerConfig#manageCpuWakeLock
     * @see ReconnectFilter
     * @see DefaultReconnectFilter
     */
    @Nullable(Nullable.Prevalence.NORMAL)
    public ReconnectFilter<?> reconnectFilter = new DefaultDeviceReconnectFilter();

    /**
     * Default is <code>true</code> - whether to automatically get services immediately after a {@link BleDevice} is
     * {@link BleDeviceState#BLE_CONNECTED}. Currently this is the only way to get a device's services.
     */
    @Nullable(Prevalence.NORMAL)
    public Boolean autoGetServices = true;

    /**
     * Default is <code>true</code> - whether to automatically enable notifications that were enabled via a call to any of the enableNotify() methods
     * in {@link BleDevice} upon device reconnection. Basically, if you enable notifications in an {@link com.idevicesinc.sweetblue.BleTransaction.Init} transaction,
     * then set this to <code>false</code>, as the transaction will run on reconnection.
     */
    public boolean autoEnableNotifiesOnReconnect = true;

    /**
     * Default is <code>true</code> - whether to automatically renegotiate the MTU size that was set via {@link BleDevice#negotiateMtu(int, ReadWriteListener)}, or
     * {@link BleDevice#negotiateMtu(int)}. If you use either of those methods in a {@link com.idevicesinc.sweetblue.BleTransaction.Init} transaction, you should set
     * this to <code>false</code>, as the transaction will run on reconnection.
     */
    public boolean autoNegotiateMtuOnReconnect = true;

    /**
     * Default is <code>null</code> - This callback is used after calling {@link BleDevice#negotiateMtu(int, ReadWriteListener)} or {@link BleDevice#negotiateMtu(int)}, if the
     * negotiation was successful, and provides a way to know if the test failed/succeeded. If the MTU test fails, SweetBlue will disconnect the device, as it won't work beyond
     * that point anyway (depending on your settings, SweetBlue may reconnect automatically for you).
     */
    public MtuTestCallback mtuTestCallback = null;

    /**
     * Default is <code>false</code> - if <code>true</code> and you call {@link BleDevice#startPoll(UUID, Interval, ReadWriteListener)}
     * or {@link BleDevice#startChangeTrackingPoll(UUID, Interval, ReadWriteListener)} with identical
     * parameters then two identical polls would run which would probably be wasteful and unintentional.
     * This option provides a defense against that situation.
     */
    @Nullable(Prevalence.NORMAL)
    public Boolean allowDuplicatePollEntries = false;

    /**
     * Default is <code>false</code> - {@link BleDevice#getAverageReadTime()} and {@link BleDevice#getAverageWriteTime()} can be
     * skewed if the peripheral you are connecting to adjusts its maximum throughput for OTA firmware updates and the like.
     * Use this option to let the library know whether you want read/writes to factor in while {@link BleDeviceState#PERFORMING_OTA}.
     *
     * @see BleDevice#getAverageReadTime()
     * @see BleDevice#getAverageWriteTime()
     */
    @Nullable(Prevalence.NORMAL)
    public Boolean includeOtaReadWriteTimesInAverage = false;

    /**
     * Default is <code>true</code> - controls whether {@link BleManager} will keep a device in active memory when it goes {@link BleManagerState#OFF}.
     * If <code>false</code> then a device will be purged and you'll have to do {@link BleManager#startScan()} again to discover devices
     * if/when {@link BleManager} goes back {@link BleManagerState#ON}.
     * <br><br>
     * NOTE: if this flag is true for {@link BleManagerConfig} passed to {@link BleManager#get(Context, BleManagerConfig)} then this
     * applies to all devices.
     */
    @com.idevicesinc.sweetblue.annotations.Advanced
    @Nullable(Prevalence.NORMAL)
    public Boolean retainDeviceWhenBleTurnsOff = true;

    /**
     * Default is <code>true</code> - only applicable if {@link #retainDeviceWhenBleTurnsOff} is also true. If {@link #retainDeviceWhenBleTurnsOff}
     * is false then devices will be undiscovered when {@link BleManager} goes {@link BleManagerState#OFF} regardless.
     * <br><br>
     * NOTE: See NOTE for {@link #retainDeviceWhenBleTurnsOff} for how this applies to {@link BleManagerConfig}.
     *
     * @see #retainDeviceWhenBleTurnsOff
     * @see #autoReconnectDeviceWhenBleTurnsBackOn
     */
    @com.idevicesinc.sweetblue.annotations.Advanced
    @Nullable(Prevalence.NORMAL)
    public Boolean undiscoverDeviceWhenBleTurnsOff = true;

    /**
     * Default is <code>true</code> - if devices are kept in memory for a {@link BleManager#turnOff()}/{@link BleManager#turnOn()} cycle
     * (or a {@link BleManager#reset()}) because {@link #retainDeviceWhenBleTurnsOff} is <code>true</code>, then a {@link BleDevice#connect()}
     * will be attempted for any devices that were previously {@link BleDeviceState#BLE_CONNECTED}.
     * <br><br>
     * NOTE: See NOTE for {@link #retainDeviceWhenBleTurnsOff} for how this applies to {@link BleManagerConfig}.
     *
     * @see #retainDeviceWhenBleTurnsOff
     */
    @com.idevicesinc.sweetblue.annotations.Advanced
    @Nullable(Prevalence.NORMAL)
    public Boolean autoReconnectDeviceWhenBleTurnsBackOn = true;

    /**
     * Default is <code>true</code> - controls whether the {@link com.idevicesinc.sweetblue.utils.State.ChangeIntent} behind a device going {@link BleDeviceState#BLE_DISCONNECTED}
     * is saved to and loaded from disk so that it can be restored across app sessions, undiscoveries, and BLE
     * {@link BleManagerState#OFF}-&gt;{@link BleManagerState#ON} cycles. This uses Android's {@link SharedPreferences} so does not require
     * any extra permissions. The main advantage of this is the following scenario: User connects to a device through your app,
     * does what they want, kills the app, then opens the app sometime later. {@link BleDevice#getLastDisconnectIntent()} returns
     * {@link com.idevicesinc.sweetblue.utils.State.ChangeIntent#UNINTENTIONAL}, which lets you know that you can probably automatically connect to this device without user confirmation.
     */
    @com.idevicesinc.sweetblue.annotations.Advanced
    @Nullable(Prevalence.NORMAL)
    public Boolean manageLastDisconnectOnDisk = true;

    /**
     * Default is <code>true</code> - controls whether a {@link BleDevice} is placed into an in-memory cache when it becomes {@link BleDeviceState#UNDISCOVERED}.
     * If <code>true</code>, subsequent calls to {@link DiscoveryListener#onEvent(com.idevicesinc.sweetblue.DiscoveryListener.DiscoveryEvent)} with
     * {@link LifeCycle#DISCOVERED} (or calls to {@link BleManager#newDevice(String)}) will return the cached {@link BleDevice} instead of creating a new one.
     * <br><br>
     * The advantages of caching are:<br>
     * <ul>
     * <li>Slightly better performance at the cost of some retained memory, especially in situations where you're frequently discovering and undiscovering many devices.
     * <li>Resistance to future stack failures that would otherwise mean missing data like {@link BleDevice#getAdvertisedServices()} for future discovery events.
     * <li>More resistant to potential "user error" of retaining devices in app-land after BleManager undiscovery.
     * <ul><br>
     * This is kept as an option in case there's some unforeseen problem with devices being cached for a certain application.
     * <p>
     * See also {@link #minScanTimeNeededForUndiscovery}.
     */
    @com.idevicesinc.sweetblue.annotations.Advanced
    @Nullable(Prevalence.NORMAL)
    public Boolean cacheDeviceOnUndiscovery = true;

    /**
     * Default is <code>true</code> - controls whether {@link DeviceReconnectFilter.Status#BONDING_FAILED} is capable of
     * inducing {@link DeviceReconnectFilter#onConnectFailed(ReconnectFilter.ConnectFailEvent)}
     * while a device is {@link BleDeviceState#CONNECTING_OVERALL}.
     */
    @Nullable(Prevalence.NORMAL)
    public Boolean bondingFailFailsConnection = true;

    /**
     * Default is <code>false</code> - whether to use <code>BluetoothGatt.refresh()</code> right before service discovery.
     * This method is not in the public Android API, so its use is disabled by default. You may find it useful to enable
     * if your remote device is routinely changing its gatt service profile. This method call supposedly clears a cache
     * that would otherwise prevent changes from being discovered.
     */
    @com.idevicesinc.sweetblue.annotations.Advanced
    @Nullable(Prevalence.NORMAL)
    public Boolean useGattRefresh = false;

    /**
     * Default is {@link RefreshOption#BEFORE_SERVICE_DISCOVERY} - This determines when SweetBlue will refresh the gatt database.
     * This only applies if you have set {@link #useGattRefresh} to <code>true</code>.
     */
    public RefreshOption gattRefreshOption = RefreshOption.BEFORE_SERVICE_DISCOVERY;

    /**
     * Default is <code>false</code>. When set to <code>true</code>, this will clear the gatt database that is cached after
     * {@link BleTransaction.Ota#succeed()} is called. This is useful when you are performing a firmware update using the
     * {@link com.idevicesinc.sweetblue.BleTransaction.Ota} class, and the gatt database has changed as a result.
     */
    public boolean clearGattOnOtaSuccess = false;


    /**
     * Default is <code>true</code> - whether SweetBlue should retry a connect <i>after</i> successfully connecting via
     * BLE. This means that if discovering services, or {@link com.idevicesinc.sweetblue.BleTransaction.Init}, or {@link com.idevicesinc.sweetblue.BleTransaction.Auth}
     * fail for any reason, SweetBlue will disconnect, then retry the connection.
     */
    public Boolean connectFailRetryConnectingOverall = true;


    /**
     * The below explanation is wrong, only in that the default is now <code>false</code>. This is for backwards
     * compatibility, as a customer noted bonding not working after this change. This will most likely go back to being
     * <code>true</code> when version 3 comes out.
     * <p>
     * Default is <code>true</code> - The normal way to bond in the native API is to use {@link BluetoothDevice#createBond()}.
     * There is however also a overload method that's made invisible using the "hide" annotation that takes an int
     * representing the desired transport mode. The default for {@link BluetoothDevice#createBond()} is {@link BluetoothDevice#TRANSPORT_AUTO}.
     * You can look at the source to see that this is the case. The thing is, you *never* want the Android stack to automatically decide something.
     * So if you set <code>useLeTransportForBonding</code> to true then SweetBlue will use the "private" overloaded method with
     * {@link BluetoothDevice#TRANSPORT_LE}. This workaround anecdotally fixed bonding issues with LG G4 and Samsung S6 phones.
     * Anecdotally because the public {@link BluetoothDevice#createBond()} was not working, tried the private one, it worked,
     * but then the public {@link BluetoothDevice#createBond()} also worked flawlessly after that.
     * But again, regardless, you should always choose explicit behavior over automatic when dealing with Android BLE.
     */
    @com.idevicesinc.sweetblue.annotations.Advanced
    @Nullable(Prevalence.NORMAL)
    public Boolean useLeTransportForBonding = false;

    /**
     * Default is {@link BondRetryFilter.DefaultBondRetryFilter} - This allows to you implement your own logic on whether or not SweetBlue should
     * retry a failed bond.
     */
    @Advanced
    public BondRetryFilter bondRetryFilter = new BondRetryFilter.DefaultBondRetryFilter();

    /**
     * Default is <code>true</code> - By default SweetBlue will force a bond/unbond for certain phones (mostly Sony, Motorola) because it has been found to
     * improve connection rates with them, see {@link BondFilter} docs. This option is here in the case you don't want this behavior (for instance, the BLE
     * device you're connecting to needs a pairing dialog to come up). However, you should use this at your own risk because it may make further connections
     * to the device less reliable.
     */
    @Advanced
    public Boolean autoBondFixes = true;

    /**
     * Default is <code>false</code> - SweetBlue tries to manage task priority for best performance. It's best to leave this alone, unless you absolutely need to change
     * it. The use case for this is if you always connect to several devices, and one or more of them could be unreachable. In this case, the connecting task(s) will
     * block any read/write calls to connected devices. Setting this option to <code>true</code> will set reads/writes to the same priority as connect tasks, which means
     * reads, writes, and connect calls will be executed in the order they were called.
     */
    @Advanced
    public boolean equalOpportunityReadsWrites = false;

    /**
     * Default is {@link #DEFAULT_MINIMUM_SCAN_TIME} seconds - Undiscovery of devices must be
     * approximated by checking when the last time was that we discovered a device,
     * and if this time is greater than {@link #undiscoveryKeepAlive} then the device is undiscovered. However a scan
     * operation must be allowed a certain amount of time to make sure it discovers all nearby devices that are
     * still advertising. This is that time in seconds.
     * <br><br>
     * Use {@link Interval#DISABLED} to disable undiscovery altogether.
     *
     * @see DiscoveryListener#onEvent(DiscoveryListener.DiscoveryEvent)
     * @see #undiscoveryKeepAlive
     */
    @Nullable(Prevalence.NORMAL)
    public Interval minScanTimeNeededForUndiscovery = Interval.secs(DEFAULT_MINIMUM_SCAN_TIME);

    /**
     * Default is disabled - If a device exceeds this amount of time since its
     * last discovery then it is a candidate for being undiscovered.
     * The default for this option attempts to accommodate the worst Android phones (BLE-wise), which may make it seem
     * like it takes a long time to undiscover a device. You may want to configure this number based on the phone or
     * manufacturer. For example, based on testing, in order to make undiscovery snappier the Galaxy S5 could use lower times.
     * <br><br>
     * A decent default time to start with is {@link #DEFAULT_SCAN_KEEP_ALIVE }.
     * Use {@link Interval#DISABLED} to disable undiscovery altogether.
     *
     * @see DiscoveryListener#onEvent(DiscoveryListener.DiscoveryEvent)
     * @see #minScanTimeNeededForUndiscovery
     */
    @Nullable(Prevalence.NORMAL)
    public Interval undiscoveryKeepAlive = Interval.DISABLED;

    /**
     * Default is {@link #DEFAULT_RSSI_AUTO_POLL_RATE} - The rate at which a {@link BleDevice} will automatically poll for its {@link BleDevice#getRssi()} value
     * after it's {@link BleDeviceState#BLE_CONNECTED}. You may also use {@link BleDevice#startRssiPoll(Interval, ReadWriteListener)} for more control and feedback.
     */
    @Nullable(Prevalence.NORMAL)
    public Interval rssiAutoPollRate = Interval.secs(DEFAULT_RSSI_AUTO_POLL_RATE);

    /**
     * Default is {@link #DEFAULT_RUNNING_AVERAGE_N} - The number of historical write times that the library should keep track of when calculating average time.
     *
     * @see BleDevice#getAverageWriteTime()
     * @see #nForAverageRunningReadTime
     */
    @com.idevicesinc.sweetblue.annotations.Advanced
    @Nullable(Prevalence.NORMAL)
    public Integer nForAverageRunningWriteTime = DEFAULT_RUNNING_AVERAGE_N;

    /**
     * Default is {@link #DEFAULT_RUNNING_AVERAGE_N} - Same thing as {@link #nForAverageRunningWriteTime} but for reads.
     *
     * @see BleDevice#getAverageWriteTime()
     * @see #nForAverageRunningWriteTime
     */
    @com.idevicesinc.sweetblue.annotations.Advanced
    @Nullable(Prevalence.NORMAL)
    public Integer nForAverageRunningReadTime = DEFAULT_RUNNING_AVERAGE_N;

    /**
     * Default is {@link #DEFAULT_TX_POWER} - this value is used if we can't establish a device's calibrated transmission power from the device itself,
     * either through its scan record or by reading the standard characteristic. To get a good value for this on a per-remote-device basis
     * experimentally, simply run a sample app and use {@link BleDevice#startRssiPoll(Interval, ReadWriteListener)} and spit {@link BleDevice#getRssi()}
     * to your log. The average value of {@link BleDevice#getRssi()} at one meter away is the value you should use for this config option.
     *
     * @see BleDevice#getTxPower()
     */
    @com.idevicesinc.sweetblue.annotations.Advanced
    @Nullable(Prevalence.NORMAL)
    public Integer defaultTxPower = DEFAULT_TX_POWER;

    /**
     * Default is {@link #DEFAULT_RSSI_MIN} - the estimated minimum value for {@link BleDevice#getRssi()}.
     */
    @Nullable(Prevalence.NORMAL)
    public Integer rssi_min = DEFAULT_RSSI_MIN;

    /**
     * Default is {@link #DEFAULT_RSSI_MAX} - the estimated maximum value for {@link BleDevice#getRssi()}.
     */
    @Nullable(Prevalence.NORMAL)
    public Integer rssi_max = DEFAULT_RSSI_MAX;

    /**
     * Default is instance of {@link DefaultBondFilter}.
     *
     * @see BondFilter
     */
    @Nullable(Prevalence.NORMAL)
    public BondFilter bondFilter = new DefaultBondFilter();

    /**
     * Set a default {@link com.idevicesinc.sweetblue.BleTransaction.Auth} factory which will be used to dispatch a new instance
     * of the transaction when connecting to a {@link BleDevice}. This transaction will also be called if the {@link BleDevice} has
     * to reconnect for any reason.
     */
    @Nullable(Prevalence.NORMAL)
    public AuthTransactionFactory<?> defaultAuthFactory = null;

    /**
     * Set a default {@link com.idevicesinc.sweetblue.BleTransaction.Init} factory which will be used to dispatch a new instance
     * of the transaction when connecting to a {@link BleDevice}. This transaction will also be called if the {@link BleDevice} has
     * to reconnect for any reason.
     */
    @Nullable(Prevalence.NORMAL)
    public InitTransactionFactory<?> defaultInitFactory = null;

    /**
     * This is the default {@link BleTransaction.Atomicity} that will be used for all {@link BleTransaction}.  You can change
     * this on an individual transaction basis by overloading {@link BleTransaction#getAtomicity()}, or change it globally
     * with this setting
     */
    public BleTransaction.Atomicity defaultTransactionAtomicity = BleTransaction.Atomicity.NOT_ATOMIC;

    /**
     * Default is {@link #DEFAULT_MAX_CONNECTION_FAIL_HISTORY_SIZE} - This sets the size of the list that tracks the history
     * of {@link com.idevicesinc.sweetblue.ReconnectFilter.ConnectFailEvent}s. Note that this will always be
     * at least 1. If set to anything lower, it will be ignored, and the max size will be 1.
     */
    public int maxConnectionFailHistorySize = DEFAULT_MAX_CONNECTION_FAIL_HISTORY_SIZE;

    /**
     * Enumeration used with {@link #useGattRefresh}. This specifies where SweetBlue will refresh the gatt database for a device.
     */
    public enum RefreshOption
    {
        /**
         * The gatt database will be refreshed after connecting, and before service discovery. This is the original behavior (and current
         * default) of the library.
         */
        BEFORE_SERVICE_DISCOVERY,

        /**
         * The gatt database will be refreshed after disconnecting from a device. It's been found that at least some devices connect better
         * when the database is refreshed prior to connecting, if you have connected at least once already.
         */
        AFTER_DISCONNECTING
    }

    /**
     * Default implementation of {@link BondFilter} that unbonds for certain phone models upon discovery and disconnects.
     * See further explanation in documentation for {@link BondFilter}.
     */
    @com.idevicesinc.sweetblue.annotations.Advanced
    @Immutable
    public static class DefaultBondFilter implements BondFilter
    {
        /**
         * Forwards {@link Utils#phoneHasBondingIssues()}. Override to make this <code>true</code> for more (or fewer) phones.
         */
        public boolean phoneHasBondingIssues()
        {
            return Utils.phoneHasBondingIssues();
        }

        @Override
        public Please onEvent(StateChangeEvent e)
        {
            final boolean autoBondFix = Utils_Config.bool(e.device().getIBleDevice().conf_device().autoBondFixes, e.device().getIBleDevice().conf_mngr().autoBondFixes);
            if (autoBondFix && phoneHasBondingIssues())
            {
                if (!e.device().is(BleDeviceState.BONDING))
                    return Please.unbondIf(e.didEnterAny(BleDeviceState.DISCOVERED, BleDeviceState.BLE_DISCONNECTED));
            }

            return Please.doNothing();
        }

        @Override
        public Please onEvent(CharacteristicEvent e)
        {
            return Please.doNothing();
        }

        @Override
        public ConnectionBugEvent.Please onEvent(ConnectionBugEvent e)
        {
            return ConnectionBugEvent.Please.doNothing();
        }
    }

    public interface InitTransactionFactory<T extends BleTransaction.Init>
    {
        T newInitTxn();
    }

    public interface AuthTransactionFactory<T extends BleTransaction.Auth>
    {
        T newAuthTxn();
    }

    /**
     * Creates a {@link BleDeviceConfig} with all default options set. See each member of this class
     * for what the default options are set to. Consider using {@link #newNulled()} also.
     */
    public BleDeviceConfig()
    {
    }

    /**
     * Creates a {@link BleDeviceConfig} with all default options set. Then, any configuration options
     * specified in the given JSONObject will be applied over the defaults.  See {@link BleNodeConfig#writeJSON}
     * regarding the creation of the JSONObject
     */
    public BleDeviceConfig(JSONObject jo)
    {
        super();
        readJSON(jo);
    }

    /**
     * Convenience method that returns a nulled out {@link BleDeviceConfig}, which is useful
     * when using {@link BleDevice#setConfig(BleDeviceConfig)} to only override a few options
     * from {@link BleManagerConfig} passed to {@link BleManager#get(Context, BleManagerConfig)}
     * or {@link BleManager#setConfig(BleManagerConfig)}.
     */
    public static BleDeviceConfig newNulled()
    {
        final BleDeviceConfig config = new BleDeviceConfig();
        config.nullOut();

        return config;
    }

    @Override
    public BleDeviceConfig clone()
    {
        return (BleDeviceConfig) super.clone();
    }

}
