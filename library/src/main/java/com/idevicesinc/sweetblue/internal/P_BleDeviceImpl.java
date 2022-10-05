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


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

import com.idevicesinc.sweetblue.BleCharacteristic;
import com.idevicesinc.sweetblue.BleConnectionPriority;
import com.idevicesinc.sweetblue.BleDescriptorRead;
import com.idevicesinc.sweetblue.BleDescriptorWrite;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDeviceConfig;
import com.idevicesinc.sweetblue.BleDeviceOrigin;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.BleNodeConfig;
import com.idevicesinc.sweetblue.BleNotify;
import com.idevicesinc.sweetblue.BleOp;
import com.idevicesinc.sweetblue.BleRead;
import com.idevicesinc.sweetblue.BleStatuses;
import com.idevicesinc.sweetblue.BleTransaction;
import com.idevicesinc.sweetblue.BleWrite;
import com.idevicesinc.sweetblue.BondFilter;
import com.idevicesinc.sweetblue.BondListener;
import com.idevicesinc.sweetblue.DeviceConnectListener;
import com.idevicesinc.sweetblue.DeviceReconnectFilter;
import com.idevicesinc.sweetblue.DeviceStateListener;
import com.idevicesinc.sweetblue.HistoricalDataLoadListener;
import com.idevicesinc.sweetblue.MtuTestCallback;
import com.idevicesinc.sweetblue.NotificationListener;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.di.SweetDIManager;
import com.idevicesinc.sweetblue.internal.PA_StateTracker.E_Intent;
import com.idevicesinc.sweetblue.ReadWriteListener;
import com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent;
import com.idevicesinc.sweetblue.ReconnectFilter;
import com.idevicesinc.sweetblue.ScanFilter;
import com.idevicesinc.sweetblue.annotations.Immutable;
import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.defaults.DefaultDeviceReconnectFilter;
import com.idevicesinc.sweetblue.internal.android.IDeviceListener;
import com.idevicesinc.sweetblue.internal.android.IBluetoothDevice;
import com.idevicesinc.sweetblue.internal.android.IBluetoothGatt;
import com.idevicesinc.sweetblue.utils.BleScanRecord;
import com.idevicesinc.sweetblue.utils.Distance;
import com.idevicesinc.sweetblue.utils.EmptyIterator;
import com.idevicesinc.sweetblue.utils.EpochTime;
import com.idevicesinc.sweetblue.utils.EpochTimeRange;
import com.idevicesinc.sweetblue.utils.Event;
import com.idevicesinc.sweetblue.utils.ForEach_Breakable;
import com.idevicesinc.sweetblue.utils.ForEach_Returning;
import com.idevicesinc.sweetblue.utils.ForEach_Void;
import com.idevicesinc.sweetblue.utils.GenericListener_Void;
import com.idevicesinc.sweetblue.utils.HistoricalData;
import com.idevicesinc.sweetblue.utils.HistoricalDataCursor;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.P_Const;
import com.idevicesinc.sweetblue.utils.Percent;
import com.idevicesinc.sweetblue.utils.Phy;
import com.idevicesinc.sweetblue.utils.PresentData;
import com.idevicesinc.sweetblue.utils.State;
import com.idevicesinc.sweetblue.utils.TimeEstimator;
import com.idevicesinc.sweetblue.utils.TimeTracker;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.Utils_Config;
import com.idevicesinc.sweetblue.utils.Utils_Rssi;
import com.idevicesinc.sweetblue.utils.Utils_ScanRecord;
import com.idevicesinc.sweetblue.utils.Utils_State;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.idevicesinc.sweetblue.BleDeviceState.*;


public final class P_BleDeviceImpl extends BleNodeImpl implements IBleDevice
{

    /**
     * Special value that is used in place of Java's built-in <code>null</code>.
     */
    @Immutable
    public static final P_BleDeviceImpl NULL = new P_BleDeviceImpl(null, IBluetoothDevice.NULL, P_Const.NULL_STRING, P_Const.NULL_STRING, BleDeviceOrigin.EXPLICIT, null, /*isNull=*/true);

    static DeviceReconnectFilter DEFAULT_CONNECTION_FAIL_LISTENER = new DefaultDeviceReconnectFilter();

//    private final P_NativeDeviceWrapper m_nativeWrapper;

    private double m_timeSinceLastDiscovery;
    private EpochTime m_lastDiscoveryTime = EpochTime.NULL;

    private final P_DeviceStateTracker m_stateTracker;
    private final P_PollManager m_pollMngr;

    private final P_TransactionManager m_txnMngr;
    private final ThreadLocal<IBleTransaction> m_threadLocalTransaction = new ThreadLocal<>();

    private final P_DeviceConnectionManager m_connectionMgr;
    private final P_RssiPollManager m_rssiPollMngr;
    private final P_RssiPollManager m_rssiPollMngr_auto;
    private final P_Task_Disconnect m_dummyDisconnectTask;
    private final P_HistoricalDataManager m_historicalDataMngr;
    private final P_BondManager m_bondMngr;

    private final Stack<ReadWriteListener> m_readWriteListenerStack;
    private final Stack<NotificationListener> m_notificationListenerStack;

    // These two fields are for when we're running the ConnectionBugFix. If the user tries to push a state or connect listener
    // we ignore it.
    private AtomicBoolean m_stateStackPaused = new AtomicBoolean(false);
    private AtomicBoolean m_connectStackPaused = new AtomicBoolean(false);

    private TimeEstimator m_writeTimeEstimator;
    private TimeEstimator m_readTimeEstimator;

    private final PA_Task.I_StateListener m_taskStateListener;

    private final BleDeviceOrigin m_origin;
    private BleDeviceOrigin m_origin_latest;

    private BleConnectionPriority m_connectionPriority = BleConnectionPriority.MEDIUM;
    private int m_mtu = 0;
    private int m_rssi = 0;
    private Integer m_knownTxPower = null;
    private byte[] m_scanRecord = P_Const.EMPTY_BYTE_ARRAY;
    private Boolean m_hasMtuBug = null;

    private BleScanRecord m_scanInfo = new BleScanRecord();

    private BleDeviceConfig m_config = null;
    private P_BleDeviceNativeManager m_nativeManager;
    private IBluetoothDevice m_deviceLayer;

    private BondListener.BondEvent m_nullBondEvent = null;
    private ReadWriteListener.ReadWriteEvent m_nullReadWriteEvent = null;
    private Phy m_phyOptions = Phy.DEFAULT;

    private final boolean m_isNull;

    private final P_ReliableWriteManager m_reliableWriteMngr;


    P_BleDeviceImpl(IBleManager mngr, IBluetoothDevice device_native, String name_normalized, String name_native, BleDeviceOrigin origin, BleDeviceConfig config_nullable, boolean isNull)
    {
        super(mngr);

        m_origin = origin;
        m_origin_latest = m_origin;
        m_isNull = isNull;

        m_readWriteListenerStack = new Stack<>();
        m_notificationListenerStack = new Stack<>();

        m_deviceLayer = device_native;


        if (isNull)
        {

            m_nativeManager = new P_BleDeviceNativeManager(this, m_deviceLayer, name_normalized, name_native);
            m_rssiPollMngr = null;
            m_rssiPollMngr_auto = null;
            // setConfig(config_nullable);
            m_stateTracker = new P_DeviceStateTracker(this);
            m_bondMngr = new P_BondManager(this);
            m_pollMngr = new P_PollManager(this);
            m_txnMngr = new P_TransactionManager(this);
            m_taskStateListener = null;
            m_dummyDisconnectTask = null;
            m_historicalDataMngr = null;
            m_reliableWriteMngr = null;
            stateTracker().set_noCallback(E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BleDeviceState.NULL, true);
            m_connectionMgr = new P_DeviceConnectionManager(this);
        }
        else
        {

            m_nativeManager = new P_BleDeviceNativeManager(this, m_deviceLayer, name_normalized, name_native);
            m_deviceLayer.updateBleDevice(this);
            m_rssiPollMngr = new P_RssiPollManager(this);
            m_rssiPollMngr_auto = new P_RssiPollManager(this);
            m_stateTracker = new P_DeviceStateTracker(this);
            m_bondMngr = new P_BondManager(this);
            m_pollMngr = new P_PollManager(this);
            m_txnMngr = new P_TransactionManager(this);
            m_taskStateListener = m_nativeManager.getTaskListener();
            m_dummyDisconnectTask = new P_Task_Disconnect(this, null, /*explicit=*/false, PE_TaskPriority.FOR_EXPLICIT_BONDING_AND_CONNECTING, /*cancellable=*/true);
            m_historicalDataMngr = new P_HistoricalDataManager(this, getMacAddress());
            m_reliableWriteMngr = new P_ReliableWriteManager(this);
            final Object[] bondStates = m_bondMngr.getNativeBondingStateOverrides();
            stateTracker().set_noCallback(E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, UNDISCOVERED, true, BleDeviceState.BLE_DISCONNECTED, true, DISCONNECTED, true, bondStates);
            m_connectionMgr = new P_DeviceConnectionManager(this);
            setConfig(config_nullable);
            setListener_Reconnect(config_nullable == null ? null : (DeviceReconnectFilter) config_nullable.reconnectFilter);
        }

    }


    @Override
    public ReadWriteListener.ReadWriteEvent reliableWrite_begin(ReadWriteListener listener)
    {
        return m_reliableWriteMngr.begin(listener);
    }

    @Override
    public ReadWriteListener.ReadWriteEvent reliableWrite_abort()
    {
        return m_reliableWriteMngr.abort();
    }

    @Override
    public ReadWriteListener.ReadWriteEvent reliableWrite_execute()
    {
        return m_reliableWriteMngr.execute();
    }

    @Override
    public String printState()
    {
        return stateTracker().toString();
    }

    @Override
    public void setConfig(BleDeviceConfig config_nullable)
    {
        if (isNull()) return;

        m_config = config_nullable == null ? null : config_nullable.clone();

        if (m_nativeManager.needsInit())
        {
            m_nativeManager.init(SweetDIManager.getInstance().get(IBluetoothGatt.class, this), getIManager().managerLayer());
        }

        initEstimators();

        //--- DRK > Not really sure how this config option should be
        // interpreted, but here's a first stab for now.
        //--- Fringe enough use case that I don't think it's really a big deal.
        boolean alwaysUseAutoConnect = Utils_Config.bool(conf_device().alwaysUseAutoConnect, conf_mngr().alwaysUseAutoConnect);
        m_connectionMgr.setAutoConnectFromConfig(alwaysUseAutoConnect);


        final Interval autoRssiPollRate = Utils_Config.interval(conf_device().rssiAutoPollRate, conf_mngr().rssiAutoPollRate);

        if (!m_rssiPollMngr.isRunning() && !Interval.isDisabled(autoRssiPollRate))
        {
            m_rssiPollMngr_auto.start(autoRssiPollRate.secs(), null);
        }
        else
        {
            m_rssiPollMngr_auto.stop();
        }
    }

    @Override
    public BleDeviceConfig getConfig()
    {
        return conf_device();
    }

    @Override
    public BleDeviceOrigin getOrigin()
    {
        return m_origin;
    }

    @Override
    public EpochTime getLastDiscoveryTime()
    {
        return m_lastDiscoveryTime;
    }

    @Override
    public State.ChangeIntent getLastDisconnectIntent()
    {
        if (isNull()) return State.ChangeIntent.NULL;

        boolean hitDisk = Utils_Config.bool(conf_device().manageLastDisconnectOnDisk, conf_mngr().manageLastDisconnectOnDisk);
        State.ChangeIntent lastDisconnect = getIManager().getDiskOptionsManager().loadLastDisconnect(getMacAddress(), hitDisk);

        return lastDisconnect;
    }

    @Override
    public boolean setListener_State(DeviceStateListener listener_nullable)
    {
        if (isNull() || listener_nullable == null || m_stateStackPaused.get()) return false;

        stateTracker().clearListenerStack();

        return stateTracker().setListener(listener_nullable);
    }

    @Override
    public boolean pushListener_State(DeviceStateListener listener)
    {
        return !isNull() && listener != null && !m_stateStackPaused.get() && stateTracker().pushListener(listener);

    }

    @Override
    public boolean popListener_State()
    {
        return !isNull() && !m_stateStackPaused.get() && stateTracker().popListener();

    }

    @Override
    public boolean popListener_State(DeviceStateListener listener)
    {
        return !isNull() && listener != null && !m_stateStackPaused.get() && stateTracker().popListener(listener);

    }

    @Override
    public DeviceStateListener getListener_State()
    {
        return stateTracker().getListener();
    }

    @Override
    public void pauseOrResumeStateStack(boolean shouldPause)
    {
        m_stateStackPaused.set(shouldPause);
    }

    @Override
    public void pauseOrResumeConnectStack(boolean shouldPause)
    {
        m_connectStackPaused.set(shouldPause);
    }

    @Override
    public boolean setListener_Connect(DeviceConnectListener listener)
    {
        return !isNull() && !m_connectStackPaused.get() && m_connectionMgr.setListener_Connect(listener);
    }

    @Override
    public boolean pushListener_Connect(DeviceConnectListener listener)
    {
        return !isNull() && listener != null && !m_connectStackPaused.get() && m_connectionMgr.pushListener_Connect(listener);

    }

    @Override
    public boolean popListener_Connect()
    {
        if (isNull() || m_connectStackPaused.get()) return false;

        return m_connectionMgr.popListener_Connect();
    }

    @Override
    public boolean popListener_Connect(DeviceConnectListener listener)
    {
        if (isNull() || listener == null || m_connectStackPaused.get()) return false;

        return m_connectionMgr.popListener_Connect(listener);
    }

    @Override
    public DeviceConnectListener getListener_Connect()
    {
        return m_connectionMgr.getListener_Connect();
    }

    @Override
    public void setListener_Reconnect(DeviceReconnectFilter listener_nullable)
    {
        m_connectionMgr.setListener_Reconnect(listener_nullable);
    }

    @Override
    public void pushListener_Reconnect(DeviceReconnectFilter listener)
    {
        m_connectionMgr.pushListener_Reconnect(listener);
    }

    @Override
    public boolean popListener_Reconnect()
    {
        return m_connectionMgr.popListener_Reconnect();
    }

    @Override
    public boolean popListener_Reconnect(DeviceReconnectFilter listener)
    {
        return m_connectionMgr.popListener_Reconnect(listener);
    }

    @Override
    public DeviceReconnectFilter getListener_Reconnect()
    {
        return m_connectionMgr.getListener_Reconnect();
    }

    @Override
    public void setListener_Bond(BondListener listener_nullable)
    {
        if (isNull()) return;

        m_bondMngr.setListener(listener_nullable);
    }

    @Override
    public void setListener_ReadWrite(ReadWriteListener listener_nullable)
    {
        if (isNull()) return;

        m_readWriteListenerStack.clear();

        if (listener_nullable != null)
            m_readWriteListenerStack.push(listener_nullable);
    }

    @Override
    public void pushListener_ReadWrite(ReadWriteListener listener)
    {
        if (isNull()) return;

        if (listener == null) return;

        m_readWriteListenerStack.push(listener);
    }

    @Override
    public boolean popListener_ReadWrite()
    {
        if (isNull()) return false;

        if (m_readWriteListenerStack.empty())
            return false;

        m_readWriteListenerStack.pop();
        return true;
    }

    @Override
    public boolean popListener_ReadWrite(ReadWriteListener listener)
    {
        return !isNull() && !m_readWriteListenerStack.empty() && m_readWriteListenerStack.remove(listener);
    }

    @Override
    public ReadWriteListener getListener_ReadWrite()
    {
        if (isNull() || m_readWriteListenerStack.empty())
            return null;
        return m_readWriteListenerStack.peek();
    }

    @Override
    public void setListener_Notification(NotificationListener listener_nullable)
    {
        if (isNull()) return;

        m_notificationListenerStack.clear();

        if (listener_nullable != null)
            m_notificationListenerStack.push(listener_nullable);
    }

    @Override
    public void pushListener_Notification(NotificationListener listener)
    {
        if (isNull()) return;

        if (listener == null) return;

        m_notificationListenerStack.push(listener);
    }

    @Override
    public boolean popListener_Notification()
    {
        if (isNull() || m_notificationListenerStack.empty()) return false;

        m_notificationListenerStack.pop();
        return true;
    }

    @Override
    public boolean popListener_Notification(NotificationListener listener)
    {
        return !isNull() && !m_notificationListenerStack.empty() && m_notificationListenerStack.remove(listener);
    }

    @Override
    public NotificationListener getListener_Notification()
    {
        if (isNull() || m_notificationListenerStack.empty()) return null;

        return m_notificationListenerStack.peek();
    }

    @Override
    public void setListener_HistoricalDataLoad(HistoricalDataLoadListener listener_nullable)
    {
        if (isNull()) return;

        m_historicalDataMngr.setListener(listener_nullable);
    }

    @Override
    public int getConnectionRetryCount()
    {
        if (isNull()) return 0;

        return m_connectionMgr.getConnectionRetryCount();
    }

    @Override
    public int getStateMask()
    {
        return stateTracker().getState();
    }

    @Override
    public int getNativeStateMask()
    {
        return stateTracker().getState_native();
    }

    @Override
    public Interval getAverageReadTime()
    {
        return m_readTimeEstimator != null ? Interval.secs(m_readTimeEstimator.getRunningAverage()) : Interval.ZERO;
    }

    @Override
    public Interval getAverageWriteTime()
    {
        return m_writeTimeEstimator != null ? Interval.secs(m_writeTimeEstimator.getRunningAverage()) : Interval.ZERO;
    }

    @Override
    public int getRssi()
    {
        return m_rssi;
    }

    @Override
    public Percent getRssiPercent()
    {
        if (isNull())
        {
            return Percent.ZERO;
        }
        else
        {
            final int rssi_min = Utils_Config.integer(conf_device().rssi_min, conf_mngr().rssi_min, BleDeviceConfig.DEFAULT_RSSI_MIN);
            final int rssi_max = Utils_Config.integer(conf_device().rssi_max, conf_mngr().rssi_max, BleDeviceConfig.DEFAULT_RSSI_MAX);
            final double percent = Utils_Rssi.percent(getRssi(), rssi_min, rssi_max);

            return Percent.fromDouble_clamped(percent);
        }
    }

    @Override
    public Distance getDistance()
    {
        if (isNull())
        {
            return Distance.INVALID;
        }
        else
        {
            return Distance.meters(Utils_Rssi.distance(getTxPower(), getRssi()));
        }
    }

    @Override
    public int getTxPower()
    {
        if (isNull())
        {
            return BleNodeConfig.INVALID_TX_POWER;
        }
        else
        {
            if (m_knownTxPower != null)
            {
                return m_knownTxPower;
            }
            else
            {
                final Integer defaultTxPower = Utils_Config.integer(conf_device().defaultTxPower, conf_mngr().defaultTxPower);
                final int toReturn = defaultTxPower == null || defaultTxPower == BleNodeConfig.INVALID_TX_POWER ? BleDeviceConfig.DEFAULT_TX_POWER : defaultTxPower;

                return toReturn;
            }
        }
    }

    @Override
    public byte[] getScanRecord()
    {
        return m_scanRecord;
    }

    @Override
    public BleScanRecord getScanInfo()
    {
        return m_scanInfo;
    }

    @Override
    public int getAdvertisingFlags()
    {
        final int flags = (m_scanInfo != null && m_scanInfo.getAdvFlags() != null) ? m_scanInfo.getAdvFlags().value : 0;
        return flags;
    }

    @Override
    public UUID[] getAdvertisedServices()
    {
        final UUID[] toReturn = m_scanInfo.getServiceUUIDS().size() > 0 ? new UUID[m_scanInfo.getServiceUUIDS().size()] : P_Const.EMPTY_UUID_ARRAY;
        return m_scanInfo.getServiceUUIDS().toArray(toReturn);
    }

    @Override
    public byte[] getManufacturerData()
    {
        final byte[] toReturn = m_scanInfo.getManufacturerData() != null ? m_scanInfo.getManufacturerData().clone() : P_Const.EMPTY_BYTE_ARRAY;

        return toReturn;
    }

    @Override
    public int getManufacturerId()
    {
        final int toReturn = m_scanInfo.getManufacturerId();

        return toReturn;
    }

    @Override
    public Map<UUID, byte[]> getAdvertisedServiceData()
    {
        final Map<UUID, byte[]> toReturn = new HashMap<>();

        toReturn.putAll(m_scanInfo.getServiceData());

        return toReturn;
    }

    @Override
    public String getHistoricalDataTableName(UUID uuid)
    {
        return getIManager().getHistoricalDatabase().getTableName(getMacAddress(), uuid);
    }

    @Override
    public HistoricalDataCursor getHistoricalData_cursor(UUID uuid, EpochTimeRange range)
    {
        return m_historicalDataMngr.getCursor(uuid, range);
    }

    @Override
    public void loadHistoricalData(UUID uuid, HistoricalDataLoadListener listener)
    {
        if (isNull()) return;

        m_historicalDataMngr.load(uuid, listener);
    }

    @Override
    public boolean isHistoricalDataLoading(UUID uuid)
    {
        return m_historicalDataMngr.isLoading(uuid);
    }

    @Override
    public boolean isHistoricalDataLoaded(UUID uuid)
    {
        return m_historicalDataMngr.isLoaded(uuid);
    }

    @Override
    public Iterator<HistoricalData> getHistoricalData_iterator(UUID uuid, EpochTimeRange range)
    {
        if (isNull()) return new EmptyIterator<>();

        return m_historicalDataMngr.getIterator(uuid, EpochTimeRange.denull(range));
    }

    @Override
    public boolean getHistoricalData_forEach(UUID uuid, EpochTimeRange range, ForEach_Void<HistoricalData> forEach)
    {
        if (isNull()) return false;

        return m_historicalDataMngr.doForEach(uuid, EpochTimeRange.denull(range), forEach);
    }

    @Override
    public boolean getHistoricalData_forEach(UUID uuid, EpochTimeRange range, ForEach_Breakable<HistoricalData> forEach)
    {
        if (isNull()) return false;

        return m_historicalDataMngr.doForEach(uuid, EpochTimeRange.denull(range), forEach);
    }

    @Override
    public HistoricalData getHistoricalData_atOffset(UUID uuid, EpochTimeRange range, int offsetFromStart)
    {
        if (isNull()) return HistoricalData.NULL;

        return m_historicalDataMngr.getWithOffset(uuid, EpochTimeRange.denull(range), offsetFromStart);
    }

    @Override
    public int getHistoricalDataCount(UUID uuid, EpochTimeRange range)
    {
        if (isNull()) return 0;

        return m_historicalDataMngr.getCount(uuid, EpochTimeRange.denull(range));
    }

    @Override
    public boolean hasHistoricalData(UUID uuid, EpochTimeRange range)
    {
        if (isNull()) return false;

        return m_historicalDataMngr.hasHistoricalData(range);
    }

    @Override
    public void addHistoricalData(UUID uuid, HistoricalData historicalData)
    {
        if (isNull()) return;

        m_historicalDataMngr.add_single(uuid, historicalData, BleNodeConfig.HistoricalDataLogFilter.Source.SINGLE_MANUAL_ADDITION);
    }

    @Override
    public void addHistoricalData(UUID uuid, Iterator<HistoricalData> historicalData)
    {
        if (isNull()) return;

        m_historicalDataMngr.add_multiple(uuid, historicalData);
    }

    @Override
    public void addHistoricalData(UUID uuid, ForEach_Returning<HistoricalData> historicalData)
    {
        if (isNull()) return;

        m_historicalDataMngr.add_multiple(uuid, historicalData);
    }

    @Override
    public boolean isAny(BleDeviceState... states)
    {
        for (int i = 0; i < states.length; i++)
        {
            if (is(states[i])) return true;
        }

        return false;
    }

    @Override
    public boolean isAll(BleDeviceState... states)
    {
        for (int i = 0; i < states.length; i++)
        {
            if (!is(states[i])) return false;
        }
        return true;
    }

    @Override
    public boolean isConnectable()
    {
        if (isAny(BleDeviceState.INITIALIZED, BleDeviceState.CONNECTING_OVERALL))
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    public final boolean is_internal(BleDeviceState state)
    {
        return state.overlaps(stateTracker().getState());
    }

    @Override
    public boolean is(BleDeviceState state)
    {
        return state.overlaps(getStateMask());
    }

    @Override
    public boolean isAny(int mask_BleDeviceState)
    {
        return (getStateMask() & mask_BleDeviceState) != 0x0;
    }

    @Override
    public boolean isAll(int mask_BleDeviceState)
    {
        return (getStateMask() & mask_BleDeviceState) == mask_BleDeviceState;
    }

    @Override
    public boolean is(Object... query)
    {
        return Utils_State.query(getStateMask(), query);
    }

    @Override
    public Interval getTimeInState(BleDeviceState state)
    {
        return Interval.millis(stateTracker().getTimeInState(state.ordinal()));
    }

    @Override
    public void refreshGattDatabase(Interval gattPause)
    {
        if (is(BLE_CONNECTED))
        {
            stateTracker().update(E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, SERVICES_DISCOVERED, false, DISCOVERING_SERVICES, true);
            P_Task_DiscoverServices discTask = new P_Task_DiscoverServices(this, (task, state) -> {
                if (task.getClass() == P_Task_DiscoverServices.class)
                {
                    if (state == PE_TaskState.SUCCEEDED)
                    {
                        m_stateTracker.update(E_Intent.INTENTIONAL, BleStatuses.GATT_SUCCESS, DISCOVERING_SERVICES, false, SERVICES_DISCOVERED, true);
                    }
                }
            }, true, true, gattPause);
            taskManager().add(discTask);
        }
    }

    @Override
    public ReadWriteListener.ReadWriteEvent setName(final String name, UUID characteristicUuid, final ReadWriteListener listener)
    {
        if (!isNull())
        {
            m_nativeManager.setName_override(name);

            final boolean saveToDisk = Utils_Config.bool(conf_device().saveNameChangesToDisk, conf_mngr().saveNameChangesToDisk);

            getIManager().getDiskOptionsManager().saveName(getMacAddress(), name, saveToDisk);
        }

        if (characteristicUuid != null)
        {
            final ReadWriteListener listener_wrapper = e -> {
                if (e.wasSuccess())
                {
                    m_nativeManager.updateNativeName(name);
                }

                invokeReadWriteCallback(listener, e);
            };

            final BleWrite write = new BleWrite(characteristicUuid)
                    .setReadWriteListener(listener_wrapper)
                    .setData(new PresentData(name.getBytes()));

            return write(write);
        }
        else
        {
            return NULL_READWRITE_EVENT();
        }
    }

    @Override
    public void clearName()
    {
        if (isNull()) return;

        m_nativeManager.clearName_override();
        getIManager().getDiskOptionsManager().clearName(getMacAddress());
    }

    @Override
    public String getName_override()
    {
        return m_nativeManager.getName_override();
    }

    @Override
    public String getName_native()
    {
        return m_nativeManager.getNativeName();
    }

    @Override
    public String getName_normalized()
    {
        return m_nativeManager.getNormalizedName();
    }

    @Override
    public String getName_debug()
    {
        return m_nativeManager.getDebugName();
    }

    @Override
    public IBluetoothDevice getNative()
    {
        return m_nativeManager.getDeviceLayer();
    }

    @Override
    public IBluetoothGatt getNativeGatt()
    {
        return m_nativeManager.getGattLayer();
    }

    @Override
    public String getMacAddress()
    {
        return m_nativeManager.getAddress();
    }

    @Override
    public BondListener.BondEvent bond(BondListener listener)
    {
        return bond_private(true, true, listener);
    }

    @Override
    public boolean unbond(BondListener listener)
    {
        final boolean alreadyUnbonded = is(UNBONDED);

        m_bondMngr.setEphemeralListener(listener);

        unbond_internal(null, BondListener.Status.CANCELLED_FROM_UNBOND);

        return !alreadyUnbonded;
    }

    @Override
    public DeviceReconnectFilter.ConnectFailEvent connect(BleTransaction.Auth authenticationTxn, BleTransaction.Init initTxn, DeviceConnectListener connectionListener)
    {
        return m_connectionMgr.connect(P_Bridge_User.getIBleTransaction(authenticationTxn), P_Bridge_User.getIBleTransaction(initTxn), connectionListener);
    }

    @Override
    public boolean disconnect()
    {
        return disconnect_private(null, DeviceReconnectFilter.Status.EXPLICIT_DISCONNECT, false, false);
    }

    @Override
    public boolean disconnectWhenReady()
    {
        return disconnect_private(PE_TaskPriority.LOW, DeviceReconnectFilter.Status.EXPLICIT_DISCONNECT, true, false);
    }

    @Override
    public boolean disconnect_remote()
    {
        return disconnect_private(null, DeviceReconnectFilter.Status.ROGUE_DISCONNECT, false, true);
    }

    @Override
    public boolean undiscover()
    {
        if (isNull()) return false;

        return getIManager().undiscover(this);
    }

    @Override
    public void clearSharedPreferences()
    {
        if (isNull()) return;

        getIManager().clearSharedPreferences(getMacAddress());
    }

    @Override
    public boolean equals(IBleDevice device_nullable)
    {
        if (device_nullable == null) return false;
        if (device_nullable == this) return true;
        if (device_nullable.nativeManager().getDeviceLayer().isDeviceNull() || this.nativeManager().getDeviceLayer().isDeviceNull())
            return false;
        if (this.isNull() && device_nullable.isNull()) return true;

        return device_nullable.nativeManager().getDeviceLayer().equals(getNative());
    }

    @Override
    public final boolean equals(Object object_nullable)
    {
        if (object_nullable == null) return false;

        if (object_nullable instanceof BleDevice)
        {
            BleDevice object_cast = (BleDevice) object_nullable;

            return this.equals(P_Bridge_User.getIBleDevice(object_cast));
        }

        if (object_nullable instanceof IBleDevice)
        {
            return this.equals((IBleDevice) object_nullable);
        }

        return false;
    }

    @Override
    public void startPoll(BleOp bleOp, Interval interval)
    {
        m_pollMngr.startPoll(bleOp, interval, false, false);
    }

    @Override
    public void startChangeTrackingPoll(BleOp bleOp, Interval interval)
    {
        m_pollMngr.startPoll(bleOp, interval, true, false);
    }

    @Override
    public void stopPoll(BleOp bleOp, Interval interval)
    {
        m_pollMngr.stopPoll(bleOp, interval.secs(), false);
    }

    @Override
    public ReadWriteListener.ReadWriteEvent write(BleWrite bleWrite)
    {
        return write_internal(bleWrite);
    }

    @Override
    public ReadWriteEvent write(BleDescriptorWrite descriptorWrite)
    {
        return write_internal(descriptorWrite);
    }

    @Override
    public ReadWriteListener.ReadWriteEvent readRssi(ReadWriteListener listener)
    {
        final ReadWriteListener.ReadWriteEvent earlyOutResult = getServiceManager().getEarlyOutEvent(BleRead.INVALID, ReadWriteListener.Type.READ, ReadWriteListener.Target.RSSI);

        if (earlyOutResult != null)
        {
            invokeReadWriteCallback(listener, earlyOutResult);

            return earlyOutResult;
        }

        readRssi_internal(ReadWriteListener.Type.READ, listener);

        return NULL_READWRITE_EVENT();
    }

    @Override
    public ReadWriteListener.ReadWriteEvent setConnectionPriority(BleConnectionPriority connectionPriority, ReadWriteListener listener)
    {
        return setConnectionPriority_private(connectionPriority, listener, getOverrideReadWritePriority());
    }

    @Override
    public BleConnectionPriority getConnectionPriority()
    {
        return m_connectionPriority;
    }

    @Override
    public int getMtu()
    {
        return m_mtu == 0 ? BleDeviceConfig.DEFAULT_MTU_SIZE : m_mtu;
    }

    @Override
    public ReadWriteListener.ReadWriteEvent negotiateMtuToDefault(ReadWriteListener listener)
    {
        if (is(BLE_CONNECTED))
        {
            return negotiateMtu(BleNodeConfig.DEFAULT_MTU_SIZE, listener);
        }
        else
        {
            clearMtu();

            final ReadWriteEvent e = P_Bridge_User.newReadWriteEventMtu(getBleDevice(), getMtu(), ReadWriteListener.Status.SUCCESS, BleStatuses.GATT_SUCCESS, 0.0, 0.0, /*solicited=*/true);

            invokeReadWriteCallback(listener, e);

            return e;
        }
    }

    @Override
    public ReadWriteListener.ReadWriteEvent negotiateMtu(int mtu, ReadWriteListener listener)
    {
        return negotiateMtu_private(mtu, listener, getOverrideReadWritePriority());
    }

    @Override
    public ReadWriteEvent setPhyOptions(Phy phy, ReadWriteListener listener)
    {
        ReadWriteEvent earlyOutEvent = getServiceManager().getEarlyOutEvent(BleWrite.INVALID, ReadWriteListener.Type.WRITE, ReadWriteListener.Target.PHYSICAL_LAYER);
        if (earlyOutEvent != null)
        {
            invokeReadWriteCallback(listener, earlyOutEvent);
            return earlyOutEvent;
        }
        // Initial check to make sure the phone supports BT 5 by OS level
        if (getIManager().isBluetooth5SupportedByAndroidVersion() == false)
        {
            ReadWriteEvent event = P_Bridge_User.newReadWriteEventPhy(getBleDevice(), ReadWriteListener.Status.ANDROID_VERSION_NOT_SUPPORTED, BleStatuses.GATT_STATUS_NOT_APPLICABLE, m_phyOptions, 0.0, 0.0, true);
            invokeReadWriteCallback(listener, event);
            return event;
        }

        // Now make sure the phone supports the specific BT 5 feature
        boolean bombOut = false;
        switch (phy)
        {
            case HIGH_SPEED:
                if (getIManager().isBluetooth5HighSpeedSupported() == false)
                    bombOut = true;
                break;
            case LONG_RANGE_2X:
            case LONG_RANGE_4X:
                if (getIManager().isBluetooth5LongRangeSupported() == false)
                    bombOut = true;
                break;
        }
        if (bombOut)
        {
            // early out
            ReadWriteEvent event = P_Bridge_User.newReadWriteEventPhy(getBleDevice(), ReadWriteListener.Status.DEVICE_CHIPSET_NOT_SUPPORTED, BleStatuses.GATT_STATUS_NOT_APPLICABLE, m_phyOptions, 0.0, 0.0, true);
            invokeReadWriteCallback(listener, event);
            return event;
        }

        P_Task_SetPhysicalLayer task = new P_Task_SetPhysicalLayer(this, phy, m_threadLocalTransaction.get(), getOverrideReadWritePriority(), listener);
        taskManager().add(task);

        return NULL_READWRITE_EVENT();
    }

    @Override
    public ReadWriteEvent readPhyOptions(ReadWriteListener listener)
    {
        ReadWriteEvent earlyOutEvent = getServiceManager().getEarlyOutEvent(BleWrite.INVALID, ReadWriteListener.Type.READ, ReadWriteListener.Target.PHYSICAL_LAYER);
        if (earlyOutEvent != null)
        {
            invokeReadWriteCallback(listener, earlyOutEvent);
            return earlyOutEvent;
        }
        // Initial check to make sure the phone supports BT 5 by OS level
        if (getIManager().isBluetooth5SupportedByAndroidVersion() == false)
        {
            ReadWriteEvent event = P_Bridge_User.newReadWriteEventPhy(getBleDevice(), ReadWriteListener.Status.ANDROID_VERSION_NOT_SUPPORTED, BleStatuses.GATT_STATUS_NOT_APPLICABLE, m_phyOptions, 0.0, 0.0, true);
            invokeReadWriteCallback(listener, event);
            return event;
        }

        P_Task_ReadPhysicalLayer task = new P_Task_ReadPhysicalLayer(this, null, listener);
        taskManager().add(task);

        return NULL_READWRITE_EVENT();
    }

    @Override
    public void setPhy_private(Phy phy)
    {
        m_phyOptions = phy;
        getStateTracker().update(E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, REQUESTING_PHY, false);
    }

    @Override
    public Phy getPhy_private()
    {
        return m_phyOptions;
    }

    @Override
    public void startRssiPoll(Interval interval, ReadWriteListener listener)
    {
        if (isNull()) return;

        m_rssiPollMngr.start(interval.secs(), listener);

        m_rssiPollMngr_auto.stop();
    }

    @Override
    public void stopRssiPoll()
    {
        if (isNull()) return;

        m_rssiPollMngr.stop();

        final Interval autoPollRate = Utils_Config.interval(conf_device().rssiAutoPollRate, conf_mngr().rssiAutoPollRate);

        if (!Interval.isDisabled(autoPollRate))
        {
            m_rssiPollMngr_auto.start(autoPollRate.secs(), null);
        }
    }

    @Override
    public void clearAllData()
    {
        clearName();
        clearHistoricalData();
        clearSharedPreferences();
    }

    @Override
    public void clearHistoricalData()
    {
        if (isNull()) return;

        m_historicalDataMngr.clearEverything();
    }

    @Override
    public void clearHistoricalData(EpochTimeRange range, long count)
    {
        if (isNull()) return;

        m_historicalDataMngr.delete_all(range, count, /*memoryOnly=*/false);
    }

    @Override
    public void clearHistoricalData(UUID uuid, EpochTimeRange range, long count)
    {
        if (isNull()) return;

        m_historicalDataMngr.delete(uuid, range, count, /*memoryOnly=*/false);
    }

    @Override
    public void clearHistoricalData_memoryOnly()
    {
        clearHistoricalData_memoryOnly(EpochTimeRange.FROM_MIN_TO_MAX, Long.MAX_VALUE);
    }

    @Override
    public void clearHistoricalData_memoryOnly(EpochTimeRange range, long count)
    {
        if (isNull()) return;

        m_historicalDataMngr.delete_all(range, count, /*memoryOnly=*/true);
    }

    @Override
    public void clearHistoricalData_memoryOnly(UUID characteristicUuid, EpochTimeRange range, long count)
    {
        if (isNull()) return;

        m_historicalDataMngr.delete(characteristicUuid, range, count, /*memoryOnly=*/true);
    }

    @Override
    public ReadWriteListener.ReadWriteEvent read(BleRead read)
    {
        return read_internal(ReadWriteListener.Type.READ, read);
    }

    @Override
    public ReadWriteEvent read(BleDescriptorRead descriptorRead)
    {
        return read_internal(ReadWriteListener.Type.READ, descriptorRead);
    }

    @Override
    public boolean isNotifyEnabled(UUID uuid)
    {
        if (isNull()) return false;

        final UUID serviceUuid = null;

        final int/*__E_NotifyState*/ notifyState = m_pollMngr.getNotifyState(serviceUuid, uuid);

        return notifyState == P_PollManager.E_NotifyState__ENABLED;
    }

    @Override
    public boolean isNotifyEnabling(UUID uuid)
    {
        if (isNull()) return false;

        final UUID serviceUuid = null;

        final int/*__E_NotifyState*/ notifyState = m_pollMngr.getNotifyState(serviceUuid, uuid);

        return notifyState == P_PollManager.E_NotifyState__ENABLING;
    }

    @Override
    public ReadWriteListener.ReadWriteEvent enableNotify(BleNotify notify)
    {
        return enableNotify_private(notify);
    }

    @Override
    public ReadWriteListener.ReadWriteEvent disableNotify(BleNotify notify)
    {
        return disableNotify_private(notify);
    }

    @Override
    public boolean performOta(BleTransaction.Ota txn)
    {
        if (performTransaction_earlyOut(txn)) return false;

        if (is(PERFORMING_OTA))
        {
            //--- DRK > The strictest and maybe best way to early out here, but as far as expected behavior this may be better.
            //---		In the end it's a judgement call, what's best API-wise with user expectations.
            m_txnMngr.cancelOtaTransaction();
        }

        m_txnMngr.startOta(txn);

        return true;
    }

    @Override
    public boolean performTransaction(BleTransaction txn)
    {
        if (performTransaction_earlyOut(txn)) return false;

        m_txnMngr.performAnonTransaction(txn);

        return true;
    }

    @Override
    public int getEffectiveWriteMtuSize()
    {
        return getMtu() - BleManagerConfig.GATT_WRITE_MTU_OVERHEAD;
    }

    @Override
    public final P_BleDeviceNativeManager nativeManager()
    {
        return m_nativeManager;
    }

    @Override
    public boolean isNull()
    {
        return m_isNull;
    }

    @Override
    public String toString()
    {
        if (isNull())
        {
            return P_Const.NULL_STRING;
        }
        else
        {
            return m_nativeManager.getDebugName() + " " + stateTracker().toString();
        }
    }

    public final boolean disconnectAndUndiscover()
    {
        return disconnect_private(null, DeviceReconnectFilter.Status.EXPLICIT_DISCONNECT, true, false);
    }

    public P_BondManager getBondManager()
    {
        return m_bondMngr;
    }

    public final PE_TaskPriority getOverrideReadWritePriority()
    {
        if (isAny(AUTHENTICATING, INITIALIZING) || getConfig().equalOpportunityReadsWrites)
        {
            getIManager().ASSERT(m_txnMngr.getCurrent() != null, "");

            return PE_TaskPriority.FOR_PRIORITY_READS_WRITES;
        }
        else
        {
            return PE_TaskPriority.FOR_NORMAL_READS_WRITES;
        }
    }

    public final void readRssi_internal(ReadWriteListener.Type type, ReadWriteListener listener)
    {
        taskManager().add(new P_Task_ReadRssi(this, listener, m_threadLocalTransaction.get(), getOverrideReadWritePriority(), type));
    }

    public final P_ReconnectManager reconnectMngr()
    {
        if (stateTracker().checkBitMatch(BleDeviceState.RECONNECTING_SHORT_TERM, true))
        {
            return m_connectionMgr.m_reconnectMngr_shortTerm;
        }
        else
        {
            return m_connectionMgr.m_reconnectMngr_longTerm;
        }
    }

    public final void setToAlwaysUseAutoConnectIfItWorked()
    {
        m_connectionMgr.setToAlwaysUseAutoConnectIfItWorked();
    }

    public final void onNativeConnect(boolean explicit)
    {
        m_connectionMgr.onConnected(explicit);
    }

    public final void onNativeConnectFail(PE_TaskState state, int gattStatus, ReconnectFilter.AutoConnectUsage autoConnectUsage)
    {
        m_connectionMgr.onConnectFail(state, gattStatus, autoConnectUsage);
    }

    public final void onNativeDisconnect(final boolean wasExplicit, final int gattStatus, final boolean attemptShortTermReconnect, final boolean saveLastDisconnect)
    {
        m_connectionMgr.onDisconnected(wasExplicit, gattStatus, attemptShortTermReconnect, saveLastDisconnect);
    }

    public final void onServicesDiscovered()
    {
        boolean autoNegotiateMtu = Utils_Config.bool(conf_device().autoNegotiateMtuOnReconnect, conf_mngr().autoNegotiateMtuOnReconnect);
        if (autoNegotiateMtu && m_mtu > BleNodeConfig.DEFAULT_MTU_SIZE)
        {
            if (isAny(RECONNECTING_SHORT_TERM, RECONNECTING_LONG_TERM))
            {
                negotiateMtu_private(m_mtu, null, PE_TaskPriority.FOR_PRIORITY_READS_WRITES);
            }
        }
        if (!autoNegotiateMtu)
            m_mtu = BleNodeConfig.DEFAULT_MTU_SIZE;

        if (m_connectionPriority != BleConnectionPriority.MEDIUM)
        {
            if (isAny(RECONNECTING_SHORT_TERM, RECONNECTING_LONG_TERM))
            {
                setConnectionPriority_private(m_connectionPriority, null, PE_TaskPriority.FOR_PRIORITY_READS_WRITES);
            }
        }

        m_txnMngr.runAuthOrInitTxnIfNeeded(BluetoothGatt.GATT_SUCCESS, DISCOVERING_SERVICES, false, SERVICES_DISCOVERED, true);
    }

    public final boolean shouldUseAutoConnect()
    {
        return m_connectionMgr.shouldUseAutoConnect();
    }

    public final void onConnecting(boolean definitelyExplicit, boolean isReconnect, final Object[] extraBondingStates, final boolean bleConnect)
    {
        m_connectionMgr.onConnecting(definitelyExplicit, isReconnect, extraBondingStates, bleConnect);
    }

    public final void updateRssi(final int rssi, boolean fromScan)
    {
        m_rssi = rssi;
        // If this update is from a scan, it will not call the event from the rssi poll (if running). So we have to manually
        // tell the poll manager that we got an rssi update.
        if (fromScan)
        {
            m_rssiPollMngr.onScanRssiUpdate(rssi);
        }
    }

    public final void updateMtu(final int mtu)
    {
        m_mtu = mtu;
    }

    public final P_ReliableWriteManager getReliableWriteManager()
    {
        return m_reliableWriteMngr;
    }

    public final P_BleDevice_ListenerProcessor getListeners()
    {
        return m_nativeManager.getNativeListener();
    }

    public final IDeviceListener getInternalListener()
    {
        return m_nativeManager.getNativeListener().getNativeCallback();
    }

    public final void onMtuChanged()
    {
        // At this point updateMtu() was already called, so we just need to check if we need to test the new MTU size or not
        final MtuTestCallback testCallback = conf_device().mtuTestCallback != null ? conf_device().mtuTestCallback : conf_mngr().mtuTestCallback;
        if (testCallback != null)
        {
            // If there's a callback set, then call the onTestRequest method to see if we need to perform a test
            MtuTestCallback.MtuTestEvent event = P_Bridge_User.newMtuTestEvent(getBleDevice(), m_mtu);
            MtuTestCallback.Please please = testCallback.onTestRequest(event);
            if (please != null && P_Bridge_User.doMtuTest(please))
            {
                final boolean requiresBonding = m_bondMngr.bondIfNeeded(P_Bridge_User.getCharUuid(please), BondFilter.CharacteristicEventType.WRITE);
                final BleWrite write = new BleWrite(P_Bridge_User.getServiceUuid(please), P_Bridge_User.getCharUuid(please))
                        .setWriteType(P_Bridge_User.getWriteType(please))
                        .setReadWriteListener(this::onMtuWriteComplete);


                final P_Task_TestMtu task = new P_Task_TestMtu(this, write, new PresentData(P_Bridge_User.getData(please)), requiresBonding, P_Bridge_User.getWriteType(please),
                        m_threadLocalTransaction.get(), PE_TaskPriority.CRITICAL);

                taskManager().add(task);
            }
            else
            {
                final MtuTestCallback.TestResult res = P_Bridge_User.newTestResult(getBleDevice(), MtuTestCallback.TestResult.Result.NO_OP, null);
                testCallback.onResult(res);
            }
        }
    }

    public final BleDeviceConfig conf_device()
    {
        return m_config != null ? m_config : conf_mngr();
    }

    @Override
    public final BleNodeConfig conf_node()
    {
        return conf_device();
    }

    public final P_PollManager getPollManager()
    {
        return m_pollMngr;
    }

    public final void onNewlyDiscovered(final IBluetoothDevice device_native, final ScanFilter.ScanEvent scanEvent_nullable, int rssi, byte[] scanRecord_nullable, final BleDeviceOrigin origin)
    {
        m_origin_latest = origin;

        clear_discovery();

        m_nativeManager.updateNativeDeviceOnly(device_native);

        onDiscovered_private(scanEvent_nullable, rssi, scanRecord_nullable);

        stateTracker().update(PA_StateTracker.E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, m_bondMngr.getNativeBondingStateOverrides(), UNDISCOVERED, false, DISCOVERED, true, ADVERTISING, origin == BleDeviceOrigin.FROM_DISCOVERY, BLE_DISCONNECTED, true);
    }

    public final void onRediscovered(final IBluetoothDevice device_native, final ScanFilter.ScanEvent scanEvent_nullable, int rssi, byte[] scanRecord_nullable, final BleDeviceOrigin origin)
    {
        m_origin_latest = origin;

        m_nativeManager.updateNativeDevice(device_native, scanRecord_nullable, Arrays.equals(m_scanRecord, scanRecord_nullable));

        onDiscovered_private(scanEvent_nullable, rssi, scanRecord_nullable);

        stateTracker().update(PA_StateTracker.E_Intent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, m_bondMngr.getNativeBondingStateOverrides(), ADVERTISING, true);
    }

    public final void updateConnectionPriority(final BleConnectionPriority connectionPriority)
    {
        m_connectionPriority = connectionPriority;
    }

    public final void invokeReadWriteCallback(final ReadWriteListener listener_nullable, final ReadWriteListener.ReadWriteEvent event)
    {
        if (event.wasSuccess() && event.isRead() && event.target() == ReadWriteListener.Target.CHARACTERISTIC)
        {
            final EpochTime timestamp = new EpochTime();
            final BleNodeConfig.HistoricalDataLogFilter.Source source = event.type().toHistoricalDataSource();

            m_historicalDataMngr.add_single(event.charUuid(), event.data(), timestamp, source);
        }

        m_txnMngr.onReadWriteResult(event);

        if (listener_nullable != null)
        {
            postEventAsCallback(listener_nullable, event);
        }

        if (getListener_ReadWrite() != null)
        {
            postEventAsCallback(getListener_ReadWrite(), event);
        }

        final ReadWriteListener rwListener = getIManager().getDefaultReadWriteListener();

        if (getIManager() != null && rwListener != null)
        {
            postEventAsCallback(rwListener, event);
        }

        m_txnMngr.onReadWriteResultCallbacksCalled();
    }

    @Override
    public void invokeNotificationCallback(NotificationListener nl, NotificationListener.NotificationEvent event)
    {
        if (event.wasSuccess())
        {
            final EpochTime timestamp = new EpochTime();
            final BleNodeConfig.HistoricalDataLogFilter.Source source = event.type().toHistoricalDataSource();

            m_historicalDataMngr.add_single(event.charUuid(), event.data(), timestamp, source);
        }

        NotificationListener listener = nl;
        if (listener != null)
        {
            postEventAsCallback(listener, event);
        }
        listener = getListener_Notification();
        // Make sure listener is not null, and it's not the same instance as the listener passed into this method to avoid duplicate events
        if (listener != null && listener != nl)
        {
            postEventAsCallback(listener, event);
        }

        listener = getIManager().getDefaultNotificationListener();

        if (getIManager() != null && listener != null)
        {
            postEventAsCallback(listener, event);
        }
    }

    public final void addReadTime(double timeStep)
    {
        if (!shouldAddOperationTime())
            return;

        if (m_readTimeEstimator != null)
        {
            m_readTimeEstimator.addTime(timeStep);
        }
    }

    public final void addWriteTime(double timeStep)
    {
        if (!shouldAddOperationTime()) return;

        if (m_writeTimeEstimator != null)
        {
            m_writeTimeEstimator.addTime(timeStep);
        }
    }

    public final void notifyOfPossibleImplicitBondingAttempt()
    {
        m_connectionMgr.notifyOfPossibleImplicitBondingAttempt();
    }

    public BleDevice getBleDevice()
    {
        return getIManager().getBleDevice(this);
    }

    public final P_DeviceServiceManager getServiceManager()
    {
        return (P_DeviceServiceManager) super.getServiceManager();
    }

    public final void unbond_internal(final PE_TaskPriority priority_nullable, final BondListener.Status status)
    {
        if (isNull()) return;

        // This fixes an android bug, where if you unbond while connected, it screws up the native bond state, so even though the unbond was
        // successful, if you query the bond state, it will still report as being bonded. The fix is to unbond when disconnected.
        if (isAny(BLE_CONNECTED, BLE_CONNECTING) && m_nativeManager.isNativelyConnectingOrConnected())
            disconnect_private(PE_TaskPriority.CRITICAL, DeviceReconnectFilter.Status.IMPLICIT_DISCONNECT, false, false);

        // If the unbond task is already in the queue, then do nothing
        if (!taskManager().isInQueue(P_Task_Unbond.class, this))
        {
            unbond_justAddTheTask(priority_nullable);

            final boolean wasBonding = is(BONDING);

            if (wasBonding)
            {
                m_bondMngr.invokeCallback(status, BondListener.BondEvent.Type.UNBOND, BleStatuses.BOND_FAIL_REASON_NOT_APPLICABLE, State.ChangeIntent.INTENTIONAL);
            }
        }
    }

    public final boolean lastDisconnectWasBecauseOfBleTurnOff()
    {
        return m_connectionMgr.lastDisconnectWasBecauseOfBleTurnOff();
    }

    public final void onUndiscovered(PA_StateTracker.E_Intent intent)
    {
        clear_undiscovery();

        m_connectionMgr.onUndiscovered();

        if (m_rssiPollMngr != null) m_rssiPollMngr.stop();
        if (m_rssiPollMngr_auto != null) m_rssiPollMngr_auto.stop();
        if (m_pollMngr != null) m_pollMngr.clear();

        stateTracker().set(intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE,
                UNDISCOVERED, true, DISCOVERED, false, ADVERTISING, false, m_bondMngr.getNativeBondingStateOverrides(),
                BLE_DISCONNECTED, true, DISCONNECTED, true);

        if (m_txnMngr != null)
        {
            m_txnMngr.cancelAllTransactions();
        }
    }

    public final double getTimeSinceLastDiscovery()
    {
        return m_timeSinceLastDiscovery;
    }

    public final P_TransactionManager getTxnManager()
    {
        return m_txnMngr;
    }

    public final P_DeviceStateTracker getStateTracker()
    {
        return m_stateTracker;
    }

    public final void onFullyInitialized(final int gattStatus, Object... extraFlags)
    {
        m_connectionMgr.onInitialized(gattStatus, extraFlags);
    }

    public final boolean isAny_internal(BleDeviceState... states)
    {
        for (int i = 0; i < states.length; i++)
        {
            if (is_internal(states[i]))
            {
                return true;
            }
        }

        return false;
    }

    public final PA_StateTracker.E_Intent lastConnectDisconnectIntent()
    {
        return m_connectionMgr.lastConnectDisconnectIntent();
    }

    public final P_DeviceConnectionManager getConnectionManager()
    {
        return m_connectionMgr;
    }

    public final void updateBondStates(Object[] extraBondingStates)
    {
        stateTracker().update(PA_StateTracker.E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, extraBondingStates);
    }

    public final void setStateToDisconnected(final boolean attemptingReconnect_longTerm, final boolean retryingConnection, final PA_StateTracker.E_Intent intent, final int gattStatus)
    {
        //--- DRK > Device probably wasn't advertising while connected so here we reset the timer to keep
        //--- it from being immediately undiscovered after disconnection.
        m_timeSinceLastDiscovery = 0.0;

        m_txnMngr.clearQueueLock();

        final P_DeviceStateTracker tracker = stateTracker();

        final int bondState;
        // If an unbond task is in the queue, then cache the bond state. It was found that if we don't, we'll still report the device as bonded because
        // the native side hasn't caught up yet (sometimes it posts the disconnect callback before the bond state has changed on the native side, even though
        // we already got the native bond state callback).

        if (taskManager().isInQueue(P_Task_Unbond.class, this))
        {
            if (is(BONDED))
                bondState = BluetoothDevice.BOND_BONDED;
            else if (is(BONDING))
                bondState = BluetoothDevice.BOND_BONDING;
            else
                bondState = BluetoothDevice.BOND_NONE;
        }
        else
            bondState = m_nativeManager.getNativeBondState();

        boolean reconnectingShortTerm = is(RECONNECTING_SHORT_TERM);

        Object[] simpleStates;
        if (reconnectingShortTerm || retryingConnection)
        {
            // Keep the current states as is
            simpleStates = new Object[6];
            simpleStates[0] = DISCONNECTED;
            simpleStates[1] = is(DISCONNECTED);
            simpleStates[2] = CONNECTING;
            simpleStates[3] = is(CONNECTING);
            simpleStates[4] = CONNECTED;
            simpleStates[5] = is(CONNECTED);
        }
        else
        {
            simpleStates = new Object[6];
            simpleStates[0] = DISCONNECTED;
            simpleStates[1] = true;
            simpleStates[2] = CONNECTING;
            simpleStates[3] = false;
            simpleStates[4] = CONNECTED;
            simpleStates[5] = false;
        }

        tracker.set
                (
                        intent,
                        gattStatus,
                        DISCOVERED, true,
                        simpleStates,
                        BLE_DISCONNECTED, true,
                        BONDING, m_nativeManager.isNativelyBonding(bondState),
                        BONDED, m_nativeManager.isNativelyBonded(bondState),
                        UNBONDED, m_nativeManager.isNativelyUnbonded(bondState),
                        RETRYING_BLE_CONNECTION, retryingConnection,
                        SERVICES_DISCOVERED, is(SERVICES_DISCOVERED),
                        RECONNECTING_SHORT_TERM, reconnectingShortTerm,
                        RECONNECTING_LONG_TERM, attemptingReconnect_longTerm,
                        ADVERTISING, !attemptingReconnect_longTerm && m_origin_latest == BleDeviceOrigin.FROM_DISCOVERY

                );

        tracker.update_native(BLE_DISCONNECTED);
    }

    public void getServices(Object... extraFlags)
    {
        if (!m_nativeManager.isNativelyConnected())
        {
            return;
        }

        boolean gattRefresh = Utils_Config.bool(conf_device().useGattRefresh, conf_mngr().useGattRefresh);
        BleDeviceConfig.RefreshOption option = conf_device().gattRefreshOption != null ? conf_device().gattRefreshOption : conf_mngr().gattRefreshOption;
        gattRefresh = gattRefresh && option == BleDeviceConfig.RefreshOption.BEFORE_SERVICE_DISCOVERY;
        Interval delay = Utils_Config.interval(conf_device().gattRefreshDelay, conf_mngr().gattRefreshDelay);
        boolean useDelay = gattRefresh;
        if (!gattRefresh)
        {
            Interval serviceDelay = Utils_Config.interval(conf_device().serviceDiscoveryDelay, conf_mngr().serviceDiscoveryDelay);
            if (Interval.isEnabled(serviceDelay))
            {
                useDelay = true;
                delay = serviceDelay;
            }
        }
        taskManager().add(new P_Task_DiscoverServices(this, m_taskStateListener, gattRefresh, useDelay, delay));

        //--- DRK > We check up top, but check again here cause we might have been disconnected on another thread in the mean time.
        //--- Even without this check the library should still be in a goodish state. Might send some weird state
        //--- callbacks to the app but eventually things settle down and we're good again.
        if (m_nativeManager.isNativelyConnected())
        {
            stateTracker().update(lastConnectDisconnectIntent(), BluetoothGatt.GATT_SUCCESS, extraFlags, DISCOVERING_SERVICES, true);
            stateTracker().update_native(BLE_CONNECTED);
        }
    }

    public final void onReconnectingShortTerm()
    {
        getIManager().getLogger().d("Entering RECONNECTING_SHORT_TERM state.");
        stateTracker().update(E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, RECONNECTING_SHORT_TERM, true, BLE_CONNECTED, false,
                BLE_DISCONNECTED, true, AUTHENTICATED, false, INITIALIZED, false, SERVICES_DISCOVERED, false);
    }

    public final void onReconnectingLongTerm()
    {
        getIManager().getLogger().d("Entering RECONNECTING_LONG_TERM state.");
        stateTracker().update(E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, RECONNECTING_LONG_TERM, true, RECONNECTING_SHORT_TERM, false, CONNECTED,
                false, CONNECTING, false, DISCONNECTED, true, SERVICES_DISCOVERED, false, ADVERTISING, true);
    }

    public final void dropReconnectingLongTermState()
    {
        getIManager().getLogger().d("Exiting RECONNECTING_LONG_TERM state.");
        stateTracker().update(E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, RECONNECTING_LONG_TERM, false, ADVERTISING, true);
    }

    public final void softlyCancelTasks(final int overrideOrdinal)
    {
        m_dummyDisconnectTask.setOverrideOrdinal(overrideOrdinal);
        taskManager().softlyCancelTasks(m_dummyDisconnectTask);
        taskManager().clearQueueOf(PA_Task_RequiresConnection.class, this, overrideOrdinal);
    }

    public final PA_Task.I_StateListener getListener_TaskState()
    {
        return m_taskStateListener;
    }

    public final void bond_justAddTheTask(P_Task_Bond.E_TransactionLockBehavior lockBehavior, boolean isDirect)
    {
        if (conf_device().forceBondDialog)
        {
            taskManager().add(new P_Task_BondPopupHack(this, null));
        }
        taskManager().add(new P_Task_Bond(this, /*isExplicit=*/true, isDirect, /*partOfConnection=*/false, m_taskStateListener, lockBehavior));
    }

    public final void unbond_justAddTheTask()
    {
        unbond_justAddTheTask(null);
    }

    public final BondListener.BondEvent bond_private(boolean isDirect, boolean userCalled, BondListener listener)
    {
        if (listener != null)
            m_bondMngr.setEphemeralListener(listener);

        if (isNull())
        {
            final BondListener.BondEvent event = m_bondMngr.invokeCallback(BondListener.Status.NULL_DEVICE, BondListener.BondEvent.Type.BOND, BleStatuses.BOND_FAIL_REASON_NOT_APPLICABLE, State.ChangeIntent.INTENTIONAL);

            return event;
        }

        if (isAny(BONDING, BONDED))
        {
            final BondListener.BondEvent event = m_bondMngr.invokeCallback(BondListener.Status.ALREADY_BONDING_OR_BONDED, BondListener.BondEvent.Type.BOND, BleStatuses.BOND_FAIL_REASON_NOT_APPLICABLE, State.ChangeIntent.INTENTIONAL);

            return event;
        }

        if (userCalled)
            m_bondMngr.resetBondRetryCount();

        bond_justAddTheTask(P_Task_Bond.E_TransactionLockBehavior.PASSES, isDirect);

        stateTracker_update(PA_StateTracker.E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BONDING, true, UNBONDED, false);
        stateTracker().updateBondState(BONDING);

        return NULL_BOND_EVENT();
    }

    public final void onLongTermReconnectTimeOut()
    {
        m_connectionMgr.onLongTermReconnectTimeOut();
    }

    public final void postEventAsCallback(final GenericListener_Void listener, final Event event)
    {
        if (listener != null)
        {
            if (listener instanceof PA_CallbackWrapper)
            {
                getIManager().getPostManager().runOrPostToUpdateThread(() -> {
                    if (listener != null)
                    {
                        listener.onEvent(event);
                    }
                });
            }
            else
            {
                getIManager().getPostManager().postCallback(() -> {
                    if (listener != null)
                    {
                        listener.onEvent(event);
                    }
                });
            }
        }
    }

    public final P_BleDeviceNativeManager getNativeManager()
    {
        return m_nativeManager;
    }

    public final void clearListeners()
    {
        setListener_State(null);
        m_connectionMgr.setListener_Connect(null);
        m_connectionMgr.setListener_Reconnect(null);
        m_bondMngr.setListener(null);
        setListener_ReadWrite(null);
        setListener_Notification(null);
        m_historicalDataMngr.setListener(null);
    }


    final void stateTracker_update(PA_StateTracker.E_Intent intent, int status, Object... statesAndValues)
    {
        stateTracker().update(intent, status, statesAndValues);
    }

    // TODO - This is redundant now (we have getStateTracker() that's public now). Looks like the only place that still
    // calls this method is within this class (we should change it to directly use the tracker instance).
    final P_DeviceStateTracker stateTracker()
    {
        return m_stateTracker;
    }

    private void clear_discovery()
    {
        // initEstimators();
    }

    private void clear_undiscovery()
    {
        m_lastDiscoveryTime = EpochTime.NULL;
    }

    private void initEstimators()
    {
        final Integer nForAverageRunningWriteTime = Utils_Config.integer(conf_device().nForAverageRunningWriteTime, conf_mngr().nForAverageRunningWriteTime);
        m_writeTimeEstimator = nForAverageRunningWriteTime == null ? null : new TimeEstimator(nForAverageRunningWriteTime);

        final Integer nForAverageRunningReadTime = Utils_Config.integer(conf_device().nForAverageRunningReadTime, conf_mngr().nForAverageRunningReadTime);
        m_readTimeEstimator = nForAverageRunningReadTime == null ? null : new TimeEstimator(nForAverageRunningReadTime);
    }

    private boolean disconnect_private(final PE_TaskPriority priority, final DeviceReconnectFilter.Status status, final boolean undiscoverAfter, final boolean forcedRemoteDisconnect)
    {
        if (isNull()) return false;

        final boolean alreadyDisconnected = is(BLE_DISCONNECTED);
        final boolean reconnecting_longTerm = is(RECONNECTING_LONG_TERM);
        final boolean alreadyQueuedToDisconnect = taskManager().isCurrentOrInQueue(P_Task_Disconnect.class, this);

        getIManager().getPostManager().runOrPostToUpdateThread(() -> {
            if (!alreadyQueuedToDisconnect)
            {
                if (status == DeviceReconnectFilter.Status.EXPLICIT_DISCONNECT)
                {
                    clearForExplicitDisconnect();
                }

                final P_DisconnectReason disconnectReason = new P_DisconnectReason(BleStatuses.GATT_STATUS_NOT_APPLICABLE, DeviceReconnectFilter.Timing.NOT_APPLICABLE)
                        .setPriority(priority)
                        .setBondFailReason(BleStatuses.BOND_FAIL_REASON_NOT_APPLICABLE)
                        .setConnectFailReason(status)
                        .setUndiscoverAfter(undiscoverAfter)
                        .setIsForcedRemoteDisconnect(forcedRemoteDisconnect)
                        .setTxnFailReason(NULL_READWRITE_EVENT());

                getIManager().getPostManager().runOrPostToUpdateThread(() -> disconnectWithReason(disconnectReason));
            }
        });
        return !alreadyDisconnected || reconnecting_longTerm || !alreadyQueuedToDisconnect;
    }

    private void clearForExplicitDisconnect()
    {
        m_pollMngr.clear();
        clearMtu();
    }

    private ReadWriteListener.ReadWriteEvent negotiateMtu_private(final int mtu, final ReadWriteListener listener, PE_TaskPriority priority)
    {
        if (false == Utils.isLollipop())
        {
            final ReadWriteListener.ReadWriteEvent e = P_Bridge_User.newReadWriteEventMtu(getBleDevice(), getMtu(), ReadWriteListener.Status.ANDROID_VERSION_NOT_SUPPORTED, BleStatuses.GATT_STATUS_NOT_APPLICABLE, 0.0, 0.0, /*solicited=*/true);

            invokeReadWriteCallback(listener, e);

            return e;
        }
        else
        {
            if (mtu <= 0)
            {
                final ReadWriteListener.ReadWriteEvent e = P_Bridge_User.newReadWriteEventMtu(getBleDevice(), getMtu(), ReadWriteListener.Status.INVALID_DATA, BleStatuses.GATT_STATUS_NOT_APPLICABLE, 0.0, 0.0, /*solicited=*/true);

                invokeReadWriteCallback(listener, e);

                return e;
            }
            else
            {
                final ReadWriteListener.ReadWriteEvent earlyOutResult = getServiceManager().getEarlyOutEvent(BleWrite.INVALID, ReadWriteListener.Type.WRITE, ReadWriteListener.Target.MTU);

                if (earlyOutResult != null)
                {
                    invokeReadWriteCallback(listener, earlyOutResult);

                    return earlyOutResult;
                }
                else
                {
                    getTaskManager().add(new P_Task_RequestMtu(this, listener, m_threadLocalTransaction.get(), priority, mtu));

                    return NULL_READWRITE_EVENT();
                }
            }
        }
    }

    private ReadWriteListener.ReadWriteEvent enableNotify_private(BleNotify notify)
    {
        final ReadWriteListener.ReadWriteEvent earlyOutResult = getServiceManager().getEarlyOutEvent(notify, ReadWriteListener.Type.ENABLING_NOTIFICATION, ReadWriteListener.Target.CHARACTERISTIC);

        if (earlyOutResult != null)
        {
            invokeReadWriteCallback(notify.getReadWriteListener(), earlyOutResult);

            invokeNotificationCallback(notify.getNotificationListener(), NotificationListener.NotificationEvent.fromReadWriteEvent(getBleDevice(), earlyOutResult));

            if (earlyOutResult.status() == ReadWriteListener.Status.NO_MATCHING_TARGET || (Interval.INFINITE.equals(notify.getForceReadTimeout()) || Interval.DISABLED.equals(notify.getForceReadTimeout())))
            {
                //--- DRK > No need to put this notify in the poll manager because either the characteristic wasn't found
                //--- or the notify (or indicate) property isn't supported and we're not doing a backing read poll.
                return earlyOutResult;
            }
        }

        final BleCharacteristic characteristic = getServiceManager().getCharacteristic(notify.getServiceUuid(), notify.getCharacteristicUuid());
        final int/*__E_NotifyState*/ notifyState = m_pollMngr.getNotifyState(notify.getServiceUuid(), notify.getCharacteristicUuid());

        final boolean shouldSendOutNotifyEnable = notifyState == P_PollManager.E_NotifyState__NOT_ENABLED && (earlyOutResult == null || earlyOutResult.status() != ReadWriteListener.Status.OPERATION_NOT_SUPPORTED);

        final ReadWriteListener.ReadWriteEvent result;
        final boolean isConnected = is(BLE_CONNECTED);

        if (shouldSendOutNotifyEnable && characteristic != null && isConnected)
        {
            m_bondMngr.bondIfNeeded(notify.getCharacteristicUuid(), BondFilter.CharacteristicEventType.ENABLE_NOTIFY);

            if (m_notificationListenerStack.isEmpty() && notify.getNotificationListener() != null)
                m_notificationListenerStack.add(notify.getNotificationListener());
            else if (notify.getNotificationListener() != null)
                getIManager().getLogger().w("BleDevice", "Enabling notification withOUT setting the given listener as the default, as there is already one set.");

            final P_Task_ToggleNotify task = new P_Task_ToggleNotify(this, notify, true, m_threadLocalTransaction.get(), getOverrideReadWritePriority());

            taskManager().add(task);

            m_pollMngr.onNotifyStateChange(notify.getServiceUuid(), notify.getCharacteristicUuid(), P_PollManager.E_NotifyState__ENABLING);

            result = NULL_READWRITE_EVENT();
        }
        else if (notifyState == P_PollManager.E_NotifyState__ENABLED)
        {
            if (notify.getReadWriteListener() != null && isConnected)
            {
                result = m_pollMngr.newAlreadyEnabledEvent(characteristic, notify.getServiceUuid(), notify.getCharacteristicUuid(), notify.getDescriptorFilter());

                invokeReadWriteCallback(notify.getReadWriteListener(), result);
            }
            else
                result = NULL_READWRITE_EVENT();

            if (!isConnected)
                getIManager().ASSERT(false, "Notification is enabled but we're not connected!");
        }
        else
            result = NULL_READWRITE_EVENT();

        m_pollMngr.startPoll(notify, notify.getForceReadTimeout(), true, true);

        return result;
    }

    private boolean performTransaction_earlyOut(final BleTransaction txn)
    {
        if (txn == null) return true;
        if (isNull()) return true;
        if (!is_internal(INITIALIZED)) return true;
        if (m_txnMngr.getCurrent() != null) return true;

        return false;
    }

    private boolean shouldAddOperationTime()
    {
        boolean includeFirmwareUpdateReadWriteTimesInAverage = Utils_Config.bool(conf_device().includeOtaReadWriteTimesInAverage, conf_mngr().includeOtaReadWriteTimesInAverage);

        return includeFirmwareUpdateReadWriteTimesInAverage || !is(PERFORMING_OTA);
    }

    final P_TaskManager getTaskManager()
    {
        return taskManager();
    }

    private void onDiscovered_private(final ScanFilter.ScanEvent scanEvent_nullable, final int rssi, byte[] scanRecord_nullable)
    {
        m_lastDiscoveryTime = EpochTime.now();
        m_timeSinceLastDiscovery = 0.0;
        updateRssi(rssi, true);

        if (scanEvent_nullable != null)
        {
            m_scanRecord = scanEvent_nullable.scanRecord();

            updateKnownTxPower(scanEvent_nullable.txPower());

            m_scanInfo.setAdvFlags((byte) scanEvent_nullable.advertisingFlags());
            m_scanInfo.setName(scanEvent_nullable.name_native());
            m_scanInfo.setTxPower((byte) scanEvent_nullable.txPower());

            m_scanInfo.clearServiceUUIDs();
            m_scanInfo.addServiceUUIDs(scanEvent_nullable.advertisedServices());

            m_scanInfo.setManufacturerDataList(scanEvent_nullable.manufacturerDataList());

            m_scanInfo.clearServiceData();
            m_scanInfo.addServiceData(scanEvent_nullable.serviceData());
        }
        else if (scanRecord_nullable != null)
        {
            m_scanRecord = scanRecord_nullable;

            m_scanInfo = Utils_ScanRecord.parseScanRecord(scanRecord_nullable);

            updateKnownTxPower(m_scanInfo.getTxPower().value);
        }
    }

    public final void updateKnownTxPower(final int txPower)
    {
        if (txPower != BleNodeConfig.INVALID_TX_POWER)
        {
            m_knownTxPower = txPower;
        }
    }

    private void onMtuWriteComplete(ReadWriteListener.ReadWriteEvent e)
    {
        // The callback shouldn't be null here if we're getting a MTU write callback, but checking it for null safety to be sure
        final MtuTestCallback testCallback = conf_device().mtuTestCallback != null ? conf_device().mtuTestCallback : conf_mngr().mtuTestCallback;
        if (testCallback != null)
        {
            // Filter the result back to the callback. If the write failed due to a time out, then we implicitly disconnect the device.
            MtuTestCallback.TestResult.Result res;
            ReadWriteListener.Status status = e.status();
            if (e.wasSuccess())
            {
                m_hasMtuBug = false;
                res = MtuTestCallback.TestResult.Result.SUCCESS;
            }
            else
            {
                if (status == ReadWriteListener.Status.TIMED_OUT)
                {
                    m_hasMtuBug = true;
                    res = MtuTestCallback.TestResult.Result.WRITE_TIMED_OUT;
                    clearMtu();
                    final P_DisconnectReason reason = new P_DisconnectReason(e.gattStatus(), DeviceReconnectFilter.Timing.EVENTUALLY)
                            .setConnectFailReason(DeviceReconnectFilter.Status.IMPLICIT_DISCONNECT)
                            .setTxnFailReason(e);
                    disconnectWithReason(reason);
                }
                else
                    res = MtuTestCallback.TestResult.Result.OTHER_FAILURE;
            }
            final MtuTestCallback.TestResult result = P_Bridge_User.newTestResult(getBleDevice(), res, status);
            testCallback.onResult(result);
        }
    }

    private void clearMtu()
    {
        updateMtu(0);
    }

    public final void update(double timeStep)
    {
        TimeTracker tt = TimeTracker.getInstance();
        tt.start("BleDevice_Update");

        m_timeSinceLastDiscovery += timeStep;

        tt.start("BleDevice_Update_PollMngr");
        m_pollMngr.update(timeStep);
        tt.transition("BleDevice_Update_PollMngr", "BleDevice_Update_TxnMngr");
        m_txnMngr.update(timeStep);
        tt.transition("BleDevice_Update_TxnMngr", "BleDevice_Update_ConnectionMgr");
        m_connectionMgr.update(timeStep);
        tt.transition("BleDevice_Update_ConnectionMgr", "BleDevice_Update_RssiPollMngr");
        m_rssiPollMngr.update(timeStep);
        tt.transition("BleDevice_UpdateRssiPollMngr", "BleDevice_Update_BondManager");
        m_bondMngr.update(timeStep);
        tt.stop("BleDevice_Update_BondManager");

        tt.stop("BleDevice_Update");
    }

    final void unbond_justAddTheTask(final PE_TaskPriority priority_nullable)
    {
        taskManager().add(new P_Task_Unbond(this, m_taskStateListener, priority_nullable));
    }

    public final void disconnectWithReason(final P_DisconnectReason disconnectReason)
    {
        m_connectionMgr.disconnectWithReason(disconnectReason);
    }

    public final ReadWriteListener.ReadWriteEvent read_internal(final ReadWriteListener.Type type, final BleOp read)
    {

        final ReadWriteListener.ReadWriteEvent earlyOutResult = getServiceManager().getEarlyOutEvent(read, type, ReadWriteListener.Target.CHARACTERISTIC);

        if (earlyOutResult != null)
        {
            invokeReadWriteCallback(read.getReadWriteListener(), earlyOutResult);

            return earlyOutResult;
        }

        if (read instanceof BleRead)
        {
            final boolean requiresBonding = m_bondMngr.bondIfNeeded(read.getCharacteristicUuid(), BondFilter.CharacteristicEventType.READ);
            final P_Task_Read task = new P_Task_Read(this, (BleRead) read, type, requiresBonding, m_threadLocalTransaction.get(), getOverrideReadWritePriority());
            taskManager().add(task);
        }
        else
        {
            final boolean requiresBonding = false;

            taskManager().add(new P_Task_ReadDescriptor(this, (BleDescriptorRead) read, type, requiresBonding, m_threadLocalTransaction.get(), getOverrideReadWritePriority()));
        }

        return NULL_READWRITE_EVENT();
    }

    final ReadWriteListener.ReadWriteEvent write_internal(final BleOp write)
    {
        final ReadWriteListener.ReadWriteEvent earlyOutResult = getServiceManager().getEarlyOutEvent(write, ReadWriteListener.Type.WRITE, ReadWriteListener.Target.CHARACTERISTIC);

        if (earlyOutResult != null)
        {
            invokeReadWriteCallback(write.getReadWriteListener(), earlyOutResult);

            return earlyOutResult;
        }

        if (write instanceof BleWrite)
        {
            final BleCharacteristic characteristic = getServiceManager().getCharacteristic(write.getServiceUuid(), write.getCharacteristicUuid());

            // I believe this can be null if the device gets disconnected right after the early out checks, in which case finding services/chars will fail.
            // TODO - Consider adding a new status to reflect when this happens.
            if (characteristic.isNull())
            {
                ReadWriteListener.Status status = is(BLE_DISCONNECTED) ? ReadWriteListener.Status.NOT_CONNECTED : ReadWriteListener.Status.NO_MATCHING_TARGET;
                ReadWriteEvent event = P_Bridge_User.newReadWriteEvent(getBleDevice(), write, ((BleWrite) write).getWriteType_safe(),
                        ReadWriteListener.Target.CHARACTERISTIC, status, BleStatuses.GATT_STATUS_NOT_APPLICABLE, 0, 0, true);
                invokeReadWriteCallback(write.getReadWriteListener(), event);
                return event;
            }

            final boolean requiresBonding = m_bondMngr.bondIfNeeded(characteristic.getCharacteristic().getUuid(), BondFilter.CharacteristicEventType.WRITE);

            addWriteTasks((BleWrite) write, requiresBonding);
        }
        else
        {
            final boolean requiresBonding = false;

            addWriteDescriptorTasks((BleDescriptorWrite) write, requiresBonding);
        }

        return NULL_READWRITE_EVENT();
    }

    private void addWriteDescriptorTasks(BleDescriptorWrite write, boolean requiresBonding)
    {
        int mtuSize = getEffectiveWriteMtuSize();
        if (!conf_device().autoStripeWrites || write.getData().getData().length < mtuSize)
        {
            taskManager().add(new P_Task_WriteDescriptor(this, write, requiresBonding, m_threadLocalTransaction.get(), getOverrideReadWritePriority()));
        }
        else
        {
            P_StripedWriteDescriptorTransaction descTxn = new P_StripedWriteDescriptorTransaction(write, requiresBonding);
            performTransaction(descTxn);
        }
    }

    private void addWriteTasks(BleWrite write, boolean requiresBonding)
    {
        int mtuSize = getEffectiveWriteMtuSize();

        if (!conf_device().autoStripeWrites || write.getData().getData().length <= mtuSize)
        {
            final P_Task_Write task_write = new P_Task_Write(this, write, requiresBonding, m_threadLocalTransaction.get(), getOverrideReadWritePriority());
            taskManager().add(task_write);
        }
        else
        {
            P_StripedWriteTransaction stripedTxn = new P_StripedWriteTransaction(write, requiresBonding, write.getWriteType());
            performTransaction(stripedTxn);
        }
    }

    private ReadWriteListener.ReadWriteEvent disableNotify_private(BleNotify notify)
    {

        final ReadWriteListener.ReadWriteEvent earlyOutResult = getServiceManager().getEarlyOutEvent(notify, ReadWriteListener.Type.DISABLING_NOTIFICATION, ReadWriteListener.Target.CHARACTERISTIC);

        if (earlyOutResult != null)
        {
            invokeReadWriteCallback(notify.getReadWriteListener(), earlyOutResult);

            invokeNotificationCallback(notify.getNotificationListener(), NotificationListener.NotificationEvent.fromReadWriteEvent(getBleDevice(), earlyOutResult));

            return earlyOutResult;
        }

        final BleCharacteristic characteristic = getServiceManager().getCharacteristic(notify.getServiceUuid(), notify.getCharacteristicUuid());

        if (!characteristic.isNull() && is(BLE_CONNECTED))
        {
            final P_Task_ToggleNotify task = new P_Task_ToggleNotify(this, notify, false, m_threadLocalTransaction.get(), getOverrideReadWritePriority());
            taskManager().add(task);
        }

        m_pollMngr.stopPoll(notify, Interval.secs(notify.getForceReadTimeout()), true);

        return NULL_READWRITE_EVENT();
    }

    private @Nullable(Nullable.Prevalence.NEVER)
    ReadWriteListener.ReadWriteEvent setConnectionPriority_private(final BleConnectionPriority connectionPriority, final ReadWriteListener listener, final PE_TaskPriority taskPriority)
    {
        if (false == Utils.isLollipop())
        {
            final ReadWriteListener.ReadWriteEvent e = P_Bridge_User.newReadWriteEventConnectionPriority(getBleDevice(), connectionPriority, ReadWriteListener.Status.ANDROID_VERSION_NOT_SUPPORTED, BleStatuses.GATT_STATUS_NOT_APPLICABLE, 0.0, 0.0, /*solicited=*/true);

            invokeReadWriteCallback(listener, e);

            return e;
        }
        else
        {
            final ReadWriteListener.ReadWriteEvent earlyOutResult = getServiceManager().getEarlyOutEvent(BleWrite.INVALID, ReadWriteListener.Type.WRITE, ReadWriteListener.Target.CONNECTION_PRIORITY);

            if (earlyOutResult != null)
            {
                invokeReadWriteCallback(listener, earlyOutResult);

                return earlyOutResult;
            }
            else
            {
                getTaskManager().add(new P_Task_RequestConnectionPriority(this, listener, m_threadLocalTransaction.get(), taskPriority, connectionPriority));

                return NULL_READWRITE_EVENT();
            }
        }
    }

    final ReadWriteListener.ReadWriteEvent NULL_READWRITE_EVENT()
    {
        if (m_nullReadWriteEvent != null)
        {
            return m_nullReadWriteEvent;
        }

        m_nullReadWriteEvent = P_Bridge_User.newReadWriteEventNULL(getBleDevice());

        return m_nullReadWriteEvent;
    }

    final BondListener.BondEvent NULL_BOND_EVENT()
    {
        if (m_nullBondEvent != null)
        {
            return m_nullBondEvent;
        }

        m_nullBondEvent = P_Bridge_User.newBondEventNULL(getBleDevice());

        return m_nullBondEvent;
    }

    @Override
    final PA_ServiceManager newServiceManager()
    {
        return new P_DeviceServiceManager(this);
    }

    static P_BleDeviceImpl EMPTY_DEVICE(IBleManager mgr)
    {
        return new P_BleDeviceImpl(mgr, IBluetoothDevice.NULL, P_Const.NULL_STRING, P_Const.NULL_STRING, BleDeviceOrigin.EXPLICIT, null, /*isNull=*/false);
    }

    @Override
    public void setThreadLocalTransaction(IBleTransaction transaction)
    {
        m_threadLocalTransaction.set(transaction);
    }

    @Override
    public IBleTransaction getThreadLocalTransaction()
    {
        return m_threadLocalTransaction.get();
    }

}
