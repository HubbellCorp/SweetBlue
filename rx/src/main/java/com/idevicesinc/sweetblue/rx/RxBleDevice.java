package com.idevicesinc.sweetblue.rx;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

import com.idevicesinc.sweetblue.BleConnectionPriority;
import com.idevicesinc.sweetblue.BleDescriptorRead;
import com.idevicesinc.sweetblue.BleDescriptorWrite;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDeviceConfig;
import com.idevicesinc.sweetblue.BleDeviceOrigin;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.BleManagerState;
import com.idevicesinc.sweetblue.BleNodeConfig;
import com.idevicesinc.sweetblue.BleNotify;
import com.idevicesinc.sweetblue.BleRead;
import com.idevicesinc.sweetblue.BleTransaction;
import com.idevicesinc.sweetblue.BleWrite;
import com.idevicesinc.sweetblue.BondListener;
import com.idevicesinc.sweetblue.DescriptorFilter;
import com.idevicesinc.sweetblue.DeviceConnectListener;
import com.idevicesinc.sweetblue.DeviceReconnectFilter;
import com.idevicesinc.sweetblue.DeviceStateListener;
import com.idevicesinc.sweetblue.DiscoveryListener;
import com.idevicesinc.sweetblue.HistoricalDataLoadListener;
import com.idevicesinc.sweetblue.NotificationListener;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.ReadWriteListener;
import com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent;
import com.idevicesinc.sweetblue.ReconnectFilter;
import com.idevicesinc.sweetblue.ScanOptions;
import com.idevicesinc.sweetblue.annotations.Advanced;
import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.internal.IBleDevice;
import com.idevicesinc.sweetblue.internal.IBleTransaction;
import com.idevicesinc.sweetblue.rx.annotations.HotObservable;
import com.idevicesinc.sweetblue.rx.exception.ConnectException;
import com.idevicesinc.sweetblue.rx.exception.BondException;
import com.idevicesinc.sweetblue.rx.exception.HistoricalDataLoadException;
import com.idevicesinc.sweetblue.rx.exception.NotifyEnableException;
import com.idevicesinc.sweetblue.rx.exception.ReadWriteException;
import com.idevicesinc.sweetblue.rx.schedulers.SweetBlueSchedulers;
import com.idevicesinc.sweetblue.utils.BleScanRecord;
import com.idevicesinc.sweetblue.utils.Distance;
import com.idevicesinc.sweetblue.utils.EpochTime;
import com.idevicesinc.sweetblue.utils.EpochTimeRange;
import com.idevicesinc.sweetblue.utils.ForEach_Returning;
import com.idevicesinc.sweetblue.utils.HistoricalData;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Percent;
import com.idevicesinc.sweetblue.utils.Phy;
import com.idevicesinc.sweetblue.utils.State;
import com.idevicesinc.sweetblue.utils.TimeEstimator;
import com.idevicesinc.sweetblue.utils.Uuids;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;


public class RxBleDevice
{

    private final BleDevice m_device;

    private RxBleDeviceConfig m_config;

    private Flowable<RxDeviceStateEvent> m_stateFlowable;
    private Flowable<RxNotificationEvent> m_notifyFlowable;
    private Flowable<RxBondEvent> m_bondFlowable;
    private Flowable<RxReadWriteEvent> m_readWriteFlowable;
    private Flowable<RxHistoricalDataLoadEvent> m_historicalDataLoadFlowable;


    private RxBleDevice(BleDevice device)
    {
        m_device = device;
    }


    /**
     * Returns a {@link Flowable} which emits {@link RxDeviceStateEvent} when this {@link RxBleDevice} changes
     * state.
     */
    public final @HotObservable @Nullable(Nullable.Prevalence.NEVER) Flowable<RxDeviceStateEvent> observeStateEvents()
    {
        if (m_stateFlowable == null)
        {
            m_stateFlowable = Flowable.create((FlowableOnSubscribe<DeviceStateListener.StateEvent>) emitter ->
            {
                if (emitter.isCancelled()) return;

                m_device.setListener_State(e ->
                {
                    if (emitter.isCancelled()) return;

                    emitter.onNext(e);
                });

                emitter.setCancellable(() ->
                {
                    m_device.setListener_State(null);
                    m_stateFlowable = null;
                });
            }, BackpressureStrategy.BUFFER).map(RxDeviceStateEvent::new).share();
        }

        return m_stateFlowable.share();
    }

    /**
     * Returns a {@link Flowable} which emits {@link RxNotificationEvent} when any notifications are received for this
     * {@link RxBleDevice}.
     */
    public final @HotObservable @Nullable(Nullable.Prevalence.NEVER) Flowable<RxNotificationEvent> observeNotifyEvents()
    {
        if (m_notifyFlowable == null)
        {
            m_notifyFlowable = Flowable.create((FlowableOnSubscribe<NotificationListener.NotificationEvent>) emitter ->
            {
                if (emitter.isCancelled()) return;

                m_device.setListener_Notification(e ->
                {
                    if (emitter.isCancelled()) return;

                    emitter.onNext(e);
                });

                emitter.setCancellable(() ->
                {
                    m_device.setListener_Notification(null);
                    m_notifyFlowable = null;
                });
            }, BackpressureStrategy.BUFFER).map(RxNotificationEvent::new).share();
        }

        return m_notifyFlowable.share();
    }

    /**
     * Returns a {@link Flowable} which emits {@link RxBondEvent} when any bonding events happen for this device.
     */
    public final @HotObservable @Nullable(Nullable.Prevalence.NEVER) Flowable<RxBondEvent> observeBondEvents()
    {
        if (m_bondFlowable == null)
        {
            m_bondFlowable = Flowable.create((FlowableOnSubscribe<BondListener.BondEvent>) emitter ->
            {
                if (emitter.isCancelled()) return;

                m_device.setListener_Bond(e ->
                {
                    if (emitter.isCancelled()) return;

                    emitter.onNext(e);
                });

                emitter.setCancellable(() ->
                {
                    m_device.setListener_Bond(null);
                    m_bondFlowable = null;
                });
            }, BackpressureStrategy.BUFFER).map(RxBondEvent::new).share();
        }

        return m_bondFlowable.share();
    }

    /**
     * Returns a {@link Flowable} which emits {@link RxReadWriteEvent} when any read, or write operation completes for this device.
     */
    public final @HotObservable @Nullable(Nullable.Prevalence.NEVER) Flowable<RxReadWriteEvent> observeReadWriteEvents()
    {
        if (m_readWriteFlowable == null)
        {
            m_readWriteFlowable = Flowable.create((FlowableOnSubscribe<ReadWriteEvent>) emitter ->
            {
                if (emitter.isCancelled()) return;

                m_device.setListener_ReadWrite(e ->
                {
                    if (emitter.isCancelled()) return;

                    emitter.onNext(e);
                });

                emitter.setCancellable(() ->
                {
                    m_device.setListener_ReadWrite(null);
                    m_readWriteFlowable = null;
                });
            }, BackpressureStrategy.BUFFER).map(RxReadWriteEvent::new).share();
        }

        return m_readWriteFlowable.share();
    }

    /**
     * Returns a {@link Flowable} which emits {@link RxHistoricalDataLoadEvent} when any {@link com.idevicesinc.sweetblue.HistoricalDataLoadListener.HistoricalDataLoadEvent} is posted
     * for this device.
     */
    public final @HotObservable @Nullable(Nullable.Prevalence.NEVER) Flowable<RxHistoricalDataLoadEvent> observeHistoricalDataLoadEvents()
    {
        if (m_historicalDataLoadFlowable == null)
        {
            m_historicalDataLoadFlowable = Flowable.create((FlowableOnSubscribe<HistoricalDataLoadListener.HistoricalDataLoadEvent>) emitter ->
            {
                if (emitter.isCancelled()) return;

                m_device.setListener_HistoricalDataLoad(e ->
                {
                    if (emitter.isCancelled()) return;

                    emitter.onNext(e);
                });

                emitter.setCancellable(() ->
                {
                    m_device.setListener_HistoricalDataLoad(null);
                    m_historicalDataLoadFlowable = null;
                });
            }, BackpressureStrategy.BUFFER).map(RxHistoricalDataLoadEvent::new).share();
        }

        return m_historicalDataLoadFlowable.share();
    }

    /**
     * Set a listener here to be notified whenever a connection fails and to
     * have control over retry behavior. NOTE: This will clear the stack of {@link DeviceReconnectFilter}s, and set
     * the one provided here to be the only one in the stack. If the provided listener is <code>null</code>, then the stack of listeners will be cleared.
     */
    public final void setListener_Reconnect(@Nullable(Nullable.Prevalence.NORMAL) DeviceReconnectFilter listener_nullable)
    {
        m_device.setListener_Reconnect(listener_nullable);
    }

    /**
     * Pushes the provided {@link DeviceReconnectFilter} on to the top of the stack of listeners.
     * This method will early-out if the provided listener is <code>null</code>
     */
    public final void pushListener_Reconnect(@Nullable(Nullable.Prevalence.NEVER) DeviceReconnectFilter listener)
    {
        m_device.pushListener_Reconnect(listener);
    }

    /**
     * Pops the current {@link DeviceReconnectFilter} off the stack of listeners.
     * Returns <code>true</code> if a listener was actually removed from the stack (it will only be false if the stack is already empty).
     */
    public final boolean popListener_Reconnect()
    {
        return m_device.popListener_Reconnect();
    }

    /**
     * Wrapper for {@link BluetoothGatt#beginReliableWrite()} - will return an {@link Observable} which emits {@link RxReadWriteEvent}s.
     * After calling this you should do your {@link RxBleDevice#write(BleWrite)} calls then call {@link #reliableWrite_execute()}.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) Observable<RxReadWriteEvent> reliableWrite_begin()
    {
        return Observable.create((ObservableEmitter<ReadWriteEvent> emitter) ->
        {
            if (emitter.isDisposed()) return;

            ReadWriteEvent event = m_device.reliableWrite_begin(e ->
            {
                if (emitter.isDisposed()) return;

                emitter.onNext(e);
            });
            if (!event.isNull())
            {
                emitter.onError(new ReadWriteException(new RxReadWriteEvent(event)));
            }
        }).subscribeOn(SweetBlueSchedulers.sweetBlueThread()).map(RxReadWriteEvent::new);
    }

    /**
     * Wrapper for {@link BluetoothGatt#abortReliableWrite()} - will return an event such that {@link RxReadWriteEvent#isNull()} will
     * return <code>false</code> if there are no problems. This call requires a previous call to {@link #reliableWrite_begin()}.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) RxReadWriteEvent reliableWrite_abort()
    {

        return new RxReadWriteEvent(m_device.reliableWrite_abort());
    }

    /**
     * Wrapper for {@link BluetoothGatt#abortReliableWrite()} - will return an event such that {@link RxReadWriteEvent#isNull()} will
     * return <code>false</code> if there are no problems. This call requires a previous call to {@link #reliableWrite_begin()}.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) RxReadWriteEvent reliableWrite_execute()
    {

        return new RxReadWriteEvent(m_device.reliableWrite_execute());
    }

    /**
     * Returns a string of all the states this {@link RxBleDevice} is currently in.
     */
    public final String printState()
    {
        return m_device.printState();
    }

    /**
     * Optionally sets overrides for any custom options given to {@link RxBleManager#get(android.content.Context, RxBleManagerConfig)}
     * for this individual device.
     */
    public final void setConfig(@Nullable(Nullable.Prevalence.RARE) RxBleDeviceConfig config_nullable)
    {
        if (config_nullable != null && (config_nullable.defaultAuthFactory != null || config_nullable.defaultInitFactory != null))
            throw new RuntimeException("Please do not set defaultAuthFactory, or defaultInitFactory! Use defaultRxAuthFactory, or defaultRxInitFactory instead.");

        m_config = config_nullable;

        if (config_nullable != null)
        {
            if (config_nullable.defaultRxAuthFactory != null)
                config_nullable.defaultAuthFactory = () -> config_nullable.defaultRxAuthFactory.newAuthTxn().getWrappedTxn();

            if (config_nullable.defaultRxInitFactory != null)
                config_nullable.defaultInitFactory = () -> config_nullable.defaultRxInitFactory.newInitTxn().getWrappedTxn();
        }
        m_device.setConfig(m_config);
    }

    /**
     * Return the {@link RxBleDeviceConfig} this device is set to use. If none has been set explicitly, then the instance
     * of {@link RxBleManagerConfig} is returned.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) RxBleDeviceConfig getConfig()
    {
        if (m_config != null)
            return m_config;
        return RxBleManager.get(m_device.getManager().getApplicationContext()).getConfigClone().toDeviceConfig();
    }

    /**
     * How the device was originally created, either from scanning or explicit creation.
     * <br><br>
     * NOTE: That devices for which this returns {@link BleDeviceOrigin#EXPLICIT} may still be
     * {@link com.idevicesinc.sweetblue.DiscoveryListener.LifeCycle#REDISCOVERED} through {@link RxBleManager#scan(ScanOptions)}.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) BleDeviceOrigin getOrigin()
    {
        return m_device.getOrigin();
    }

    /**
     * Returns the last time the device was {@link com.idevicesinc.sweetblue.DiscoveryListener.LifeCycle#DISCOVERED}
     * or {@link com.idevicesinc.sweetblue.DiscoveryListener.LifeCycle#REDISCOVERED}. If {@link #getOrigin()} returns
     * {@link BleDeviceOrigin#EXPLICIT} then this will return {@link EpochTime#NULL} unless or until
     * the device is {@link com.idevicesinc.sweetblue.DiscoveryListener.LifeCycle#REDISCOVERED}.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) EpochTime getLastDiscoveryTime()
    {
        return m_device.getLastDiscoveryTime();
    }

    /**
     * This enum gives you an indication of the last interaction with a device across app sessions or in-app BLE
     * {@link BleManagerState#OFF}-&gt;{@link BleManagerState#ON} cycles or undiscovery-&gt;rediscovery, which
     * basically means how it was last {@link BleDeviceState#BLE_DISCONNECTED}.
     * <br><br>
     * If {@link com.idevicesinc.sweetblue.utils.State.ChangeIntent#NULL}, then the last disconnect is unknown because
     * (a) device has never been seen before,
     * (b) reason for disconnect was app being killed and {@link BleDeviceConfig#manageLastDisconnectOnDisk} was <code>false</code>,
     * (c) app user cleared app data between app sessions or reinstalled the app.
     * <br><br>
     * If {@link com.idevicesinc.sweetblue.utils.State.ChangeIntent#UNINTENTIONAL}, then from a user experience perspective, the user may not have wanted
     * the disconnect to happen, and thus *probably* would want to be automatically connected again as soon as the device is discovered.
     * <br><br>
     * If {@link com.idevicesinc.sweetblue.utils.State.ChangeIntent#INTENTIONAL}, then the last reason the device was {@link BleDeviceState#BLE_DISCONNECTED} was because
     * {@link RxBleDevice#disconnect()} was called, which most-likely means the user doesn't want to automatically connect to this device again.
     * <br><br>
     * See further explanation at {@link BleDeviceConfig#manageLastDisconnectOnDisk}.
     */
    @Advanced
    public final @Nullable(Nullable.Prevalence.NEVER) State.ChangeIntent getLastDisconnectIntent()
    {
        return m_device.getLastDisconnectIntent();
    }

    /**
     * Returns the connection failure retry count during a retry loop. Basic example use case is to provide a callback to
     * {@link #setListener_Reconnect(DeviceReconnectFilter)} and update your application's UI with this method's return value downstream of your
     * {@link DeviceReconnectFilter#onConnectFailed(ReconnectFilter.ConnectFailEvent)} override.
     */
    public final int getConnectionRetryCount()
    {
        return m_device.getConnectionRetryCount();
    }

    /**
     * Returns the bitwise state mask representation of {@link BleDeviceState} for this device.
     *
     * @see BleDeviceState
     */
    @Advanced
    public final int getStateMask()
    {
        return m_device.getStateMask();
    }

    /**
     * Returns the actual native state mask representation of the {@link BleDeviceState} for this device.
     * The main purpose of this is to reflect what's going on under the hood while {@link RxBleDevice#is(BleDeviceState)}
     * with {@link BleDeviceState#RECONNECTING_SHORT_TERM} is <code>true</code>.
     */
    @Advanced
    public final int getNativeStateMask()
    {
        return m_device.getNativeStateMask();
    }

    /**
     * See similar explanation for {@link #getAverageWriteTime()}.
     *
     * @see #getAverageWriteTime()
     * @see BleManagerConfig#nForAverageRunningReadTime
     */
    @Advanced
    public final @Nullable(Nullable.Prevalence.NEVER) Interval getAverageReadTime()
    {
        return m_device.getAverageReadTime();
    }

    /**
     * Returns the average round trip time in seconds for all write operations started with {@link #write(BleWrite)}.
     * This is a running average with N being defined by {@link BleManagerConfig#nForAverageRunningWriteTime}. This may
     * be useful for estimating how long a series of reads and/or writes will take. For example for displaying the estimated
     * time remaining for a firmware update.
     */
    @Advanced
    public final @Nullable(Nullable.Prevalence.NEVER) Interval getAverageWriteTime()
    {
        return m_device.getAverageWriteTime();
    }

    /**
     * Returns the raw RSSI retrieved from when the device was discovered,
     * rediscovered, or when you call {@link #readRssi()} or {@link #startRssiPoll(Interval)}.
     *
     * @see #getDistance()
     */
    public final int getRssi()
    {
        return m_device.getRssi();
    }

    /**
     * Raw RSSI from {@link #getRssi()} is a little cryptic, so this gives you a friendly 0%-100% value for signal strength.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) Percent getRssiPercent()
    {
        return m_device.getRssiPercent();
    }

    /**
     * Returns the approximate distance in meters based on {@link #getRssi()} and
     * {@link #getTxPower()}. NOTE: the higher the distance, the less the accuracy.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) Distance getDistance()
    {
        return m_device.getDistance();
    }

    /**
     * Returns the calibrated transmission power of the device. If this can't be
     * figured out from the device itself then it backs up to the value provided
     * in {@link BleDeviceConfig#defaultTxPower}.
     *
     * @see BleDeviceConfig#defaultTxPower
     */
    @Advanced
    public final int getTxPower()
    {
        return m_device.getTxPower();
    }

    /**
     * Returns the manufacturer data, if any, parsed from {@link #getScanRecord()}. May be empty but never <code>null</code>.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) byte[] getManufacturerData()
    {
        return m_device.getManufacturerData();
    }

    /**
     * Returns the manufacturer id, if any, parsed from {@link #getScanRecord()} }. May be -1 if not set
     */
    public final int getManufacturerId()
    {
        return m_device.getManufacturerId();
    }

    /**
     * Returns the scan record from when we discovered the device. May be empty but never <code>null</code>.
     */
    @Advanced
    public final @Nullable(Nullable.Prevalence.NEVER) byte[] getScanRecord()
    {
        return m_device.getScanRecord();
    }

    /**
     * Returns the {@link BleScanRecord} instance held by this {@link RxBleDevice}.
     */
    public final @Nullable(Nullable.Prevalence.NEVER)
    BleScanRecord getScanInfo()
    {
        return m_device.getScanInfo();
    }

    /**
     * Returns the advertising flags, if any, parsed from {@link #getScanRecord()}.
     */
    public final int getAdvertisingFlags()
    {
        return m_device.getAdvertisingFlags();
    }

    /**
     * Loads all historical data to memory for this device.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) Observable<RxHistoricalDataLoadEvent> loadHistoricalData()
    {
        return loadHistoricalData(null);
    }

    /**
     * Loads all historical data to memory for this device for the given {@link UUID}.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) Observable<RxHistoricalDataLoadEvent> loadHistoricalData(UUID uuid)
    {
        return Observable.create((ObservableEmitter<HistoricalDataLoadListener.HistoricalDataLoadEvent> emitter) ->
        {
            if (emitter.isDisposed()) return;

            m_device.loadHistoricalData(uuid, e ->
            {
                if (emitter.isDisposed()) return;

                if (e.wasSuccess())
                {
                    emitter.onNext(e);
                }
                else
                {
                    emitter.onError(new HistoricalDataLoadException(new RxHistoricalDataLoadEvent(e)));
                }
            });
        }).subscribeOn(SweetBlueSchedulers.sweetBlueThread()).map(RxHistoricalDataLoadEvent::new);
    }

    /**
     * Returns whether the device is currently loading any historical data to memory, either through
     * {@link #loadHistoricalData()} (or overloads) or {@link #getHistoricalData(UUID)} (or overloads).
     */
    @Advanced
    public final boolean isHistoricalDataLoading()
    {
        return isHistoricalDataLoading(null);
    }

    /**
     * Returns whether the device is currently loading any historical data to memory for the given uuid, either through
     * {@link #loadHistoricalData()} (or overloads) or {@link #getHistoricalData(UUID)} (or overloads).
     */
    @Advanced
    public final boolean isHistoricalDataLoading(final UUID uuid)
    {
        return m_device.isHistoricalDataLoading(uuid);
    }

    /**
     * Returns <code>true</code> if the historical data for all historical data for
     * this device is loaded into memory.
     * Use {@link HistoricalDataLoadListener}
     * to listen for when the load actually completes. If {@link #hasHistoricalData(UUID)}
     * returns <code>false</code> then this will also always return <code>false</code>.
     */
    @Advanced
    public final boolean isHistoricalDataLoaded()
    {
        return isHistoricalDataLoaded(null);
    }

    /**
     * Returns <code>true</code> if the historical data for a given uuid is loaded into memory.
     * Use {@link HistoricalDataLoadListener}
     * to listen for when the load actually completes. If {@link #hasHistoricalData(UUID)}
     * returns <code>false</code> then this will also always return <code>false</code>.
     */
    @Advanced
    public final boolean isHistoricalDataLoaded(final UUID uuid)
    {
        return m_device.isHistoricalDataLoaded(uuid);
    }

    /**
     * Returns the cached data from the latest successful read or notify received for a given uuid.
     * Basically if you receive a {@link com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent} for which
     * {@link com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent#isRead()} and
     * {@link com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent#wasSuccess()} both return
     * <code>true</code> then {@link com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent#data()},
     * will be cached and is retrievable by this method.
     *
     * @return The cached value from a previous read or notify, or {@link HistoricalData#NULL} otherwise.
     * @see com.idevicesinc.sweetblue.BleNodeConfig.HistoricalDataLogFilter
     * @see com.idevicesinc.sweetblue.BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final @Nullable(Nullable.Prevalence.NEVER) Single<HistoricalData> getHistoricalData_latest(final UUID uuid)
    {
        return Single.create((SingleEmitter<HistoricalData> emitter) ->
        {
            if (emitter.isDisposed()) return;

            emitter.onSuccess(m_device.getHistoricalData_latest(uuid));
        }).subscribeOn(SweetBlueSchedulers.sweetBlueThread());
    }

    /**
     * Overload of {@link #getHistoricalData(UUID, EpochTimeRange)}, which uses a range of {@link EpochTimeRange#FROM_MIN_TO_MAX}.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) Observable<HistoricalData> getHistoricalData(UUID uuid)
    {
        return getHistoricalData(uuid, EpochTimeRange.FROM_MIN_TO_MAX);
    }

    /**
     * Returns an {@link Observable} which emits {@link HistoricalData} for each data item for the UUID given, and within the specified range.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) Observable<HistoricalData> getHistoricalData(UUID uuid, EpochTimeRange range)
    {
        return Observable.create((ObservableEmitter<HistoricalData> emitter) ->
        {
            if (emitter.isDisposed()) return;

            Iterator<HistoricalData> it = m_device.getHistoricalData_iterator(uuid, range);

            while (it.hasNext())
            {
                if (emitter.isDisposed()) return;

                emitter.onNext(it.next());
            }

            emitter.onComplete();
        }).subscribeOn(SweetBlueSchedulers.sweetBlueThread());
    }

    public final @Nullable(Nullable.Prevalence.NEVER) Single<HistoricalData> getHistoricalData_atOffset(final UUID uuid, final int offsetFromStart)
    {
        return getHistoricalData_atOffset(uuid, EpochTimeRange.FROM_MIN_TO_MAX, offsetFromStart);
    }

    public final @Nullable(Nullable.Prevalence.NEVER) Single<HistoricalData> getHistoricalData_atOffset(final UUID uuid, final EpochTimeRange range, final int offsetFromStart)
    {
        return Single.create((SingleEmitter<HistoricalData> emitter) ->
        {
            if (emitter.isDisposed()) return;

            emitter.onSuccess(m_device.getHistoricalData_atOffset(uuid, range, offsetFromStart));
        }).subscribeOn(SweetBlueSchedulers.sweetBlueThread());
    }

    /**
     * Returns the number of historical data entries that have been logged for the device's given characteristic.
     *
     * @see com.idevicesinc.sweetblue.BleNodeConfig.HistoricalDataLogFilter
     * @see com.idevicesinc.sweetblue.BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final int getHistoricalDataCount(final UUID uuid)
    {
        return getHistoricalDataCount(uuid, EpochTimeRange.FROM_MIN_TO_MAX);
    }

    /**
     * Returns the number of historical data entries that have been logged
     * for the device's given characteristic within the range provided.
     *
     * @see com.idevicesinc.sweetblue.BleNodeConfig.HistoricalDataLogFilter
     * @see com.idevicesinc.sweetblue.BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final int getHistoricalDataCount(final UUID uuid, final EpochTimeRange range)
    {
        return m_device.getHistoricalDataCount(uuid, range);
    }

    /**
     * Returns <code>true</code> if there is any historical data at all for this device.
     *
     * @see com.idevicesinc.sweetblue.BleNodeConfig.HistoricalDataLogFilter
     * @see com.idevicesinc.sweetblue.BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final boolean hasHistoricalData()
    {
        return hasHistoricalData(EpochTimeRange.FROM_MIN_TO_MAX);
    }

    /**
     * Returns <code>true</code> if there is any historical data at all for this device within the given range.
     *
     * @see com.idevicesinc.sweetblue.BleNodeConfig.HistoricalDataLogFilter
     * @see com.idevicesinc.sweetblue.BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final boolean hasHistoricalData(final EpochTimeRange range)
    {
        return hasHistoricalData(null, range);
    }

    /**
     * Returns <code>true</code> if there is any historical data for the given uuid.
     *
     * @see com.idevicesinc.sweetblue.BleNodeConfig.HistoricalDataLogFilter
     * @see com.idevicesinc.sweetblue.BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final boolean hasHistoricalData(final UUID uuid)
    {
        return hasHistoricalData(uuid, EpochTimeRange.FROM_MIN_TO_MAX);
    }

    /**
     * Returns <code>true</code> if there is any historical data for any of the given uuids.
     *
     * @see com.idevicesinc.sweetblue.BleNodeConfig.HistoricalDataLogFilter
     * @see com.idevicesinc.sweetblue.BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final boolean hasHistoricalData(final UUID[] uuids)
    {
        for (int i = 0; i < uuids.length; i++)
        {
            if (hasHistoricalData(uuids[i], EpochTimeRange.FROM_MIN_TO_MAX))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns <code>true</code> if there is any historical data for the given uuid within the given range.
     *
     * @see com.idevicesinc.sweetblue.BleNodeConfig.HistoricalDataLogFilter
     * @see com.idevicesinc.sweetblue.BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final boolean hasHistoricalData(final UUID uuid, final EpochTimeRange range)
    {
        return m_device.hasHistoricalData(uuid, range);
    }

    /**
     * Manual way to add data to the historical data list managed by this device. You may want to use this if,
     * for example, your remote BLE device is capable of taking and caching independent readings while not connected.
     * After you connect with this device and download the log you can add it manually here.
     * Really you can use this for any arbitrary historical data though, even if it's not associated with a characteristic.
     *
     * @see com.idevicesinc.sweetblue.BleNodeConfig.HistoricalDataLogFilter
     * @see com.idevicesinc.sweetblue.BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final void addHistoricalData(final UUID uuid, final byte[] data, final EpochTime epochTime)
    {
        m_device.addHistoricalData(uuid, data, epochTime);
    }

    /**
     * Just an overload of {@link #addHistoricalData(UUID, byte[], EpochTime)} with the data and epochTime parameters switched around.
     */
    @Advanced
    public final void addHistoricalData(final UUID uuid, final EpochTime epochTime, final byte[] data)
    {
        addHistoricalData(uuid, data, epochTime);
    }

    /**
     * Same as {@link #addHistoricalData(UUID, byte[], EpochTime)} but uses {@link System#currentTimeMillis()} for the timestamp.
     *
     * @see com.idevicesinc.sweetblue.BleNodeConfig.HistoricalDataLogFilter
     * @see com.idevicesinc.sweetblue.BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final void addHistoricalData(final UUID uuid, final byte[] data)
    {
        addHistoricalData(uuid, data, new EpochTime());
    }

    /**
     * Same as {@link #addHistoricalData(UUID, byte[], EpochTime)}.
     *
     * @see com.idevicesinc.sweetblue.BleNodeConfig.HistoricalDataLogFilter
     * @see com.idevicesinc.sweetblue.BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final void addHistoricalData(final UUID uuid, final HistoricalData historicalData)
    {
        m_device.addHistoricalData(uuid, historicalData);
    }

    /**
     * Same as {@link #addHistoricalData(UUID, byte[], EpochTime)} but for large datasets this is more efficient when writing to disk.
     *
     * @see com.idevicesinc.sweetblue.BleNodeConfig.HistoricalDataLogFilter
     * @see com.idevicesinc.sweetblue.BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final void addHistoricalData(final UUID uuid, final Iterator<HistoricalData> historicalData)
    {
        m_device.addHistoricalData(uuid, historicalData);
    }

    /**
     * Same as {@link #addHistoricalData(UUID, byte[], EpochTime)} but for large datasets this is more efficient when writing to disk.
     *
     * @see com.idevicesinc.sweetblue.BleNodeConfig.HistoricalDataLogFilter
     * @see com.idevicesinc.sweetblue.BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final void addHistoricalData(final UUID uuid, final List<HistoricalData> historicalData)
    {
        addHistoricalData(uuid, historicalData.iterator());
    }

    /**
     * Same as {@link #addHistoricalData(UUID, byte[], EpochTime)} but for large datasets this is more efficient when writing to disk.
     *
     * @see com.idevicesinc.sweetblue.BleNodeConfig.HistoricalDataLogFilter
     * @see com.idevicesinc.sweetblue.BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final void addHistoricalData(final UUID uuid, final ForEach_Returning<HistoricalData> historicalData)
    {
        m_device.addHistoricalData(uuid, historicalData);
    }

    /**
     * Returns whether the device is in any of the provided states.
     *
     * @see #is(BleDeviceState)
     */
    public final boolean isAny(BleDeviceState... states)
    {
        return m_device.isAny(states);
    }

    /**
     * Returns whether the device is in all of the provided states.
     *
     * @see #isAny(BleDeviceState...)
     */
    public final boolean isAll(BleDeviceState... states)
    {
        return m_device.isAll(states);
    }

    /**
     * Convenience method to tell you whether a call to {@link #connect()} (or overloads) has a chance of succeeding.
     * For example if the device is {@link BleDeviceState#CONNECTING_OVERALL} or {@link BleDeviceState#INITIALIZED}
     * then this will return <code>false</code>.
     */
    public final boolean isConnectable()
    {
        return m_device.isConnectable();
    }

    /**
     * Returns whether the device is in the provided state.
     *
     * @see #isAny(BleDeviceState...)
     */
    public final boolean is(final BleDeviceState state)
    {
        return m_device.is(state);
    }

    /**
     * Returns <code>true</code> if there is any bitwise overlap between the provided value and {@link #getStateMask()}.
     *
     * @see #isAll(int)
     */
    public final boolean isAny(final int mask_BleDeviceState)
    {
        return m_device.isAny(mask_BleDeviceState);
    }

    /**
     * Returns <code>true</code> if there is complete bitwise overlap between the provided value and {@link #getStateMask()}.
     *
     * @see #isAny(int)
     */
    public final boolean isAll(final int mask_BleDeviceState)
    {
        return m_device.isAll(mask_BleDeviceState);
    }

    /**
     * Similar to {@link #is(BleDeviceState)} and {@link #isAny(BleDeviceState...)} but allows you to give a simple query
     * made up of {@link BleDeviceState} and {@link Boolean} pairs. So an example would be
     * <code>myDevice.is({@link BleDeviceState#BLE_CONNECTING}, true, {@link BleDeviceState#RECONNECTING_LONG_TERM}, false)</code>.
     */
    public final boolean is(Object... query)
    {
        return m_device.is(query);
    }


    /**
     * If {@link #is(BleDeviceState)} returns true for the given state (i.e. if
     * the device is in the given state) then this method will (a) return the
     * amount of time that the device has been in the state. Otherwise, this
     * will (b) return the amount of time that the device was *previously* in
     * that state. Otherwise, if the device has never been in the state, it will
     * (c) return 0.0 seconds. Case (b) might be useful for example for checking
     * how long you <i>were</i> connected for after becoming
     * {@link BleDeviceState#BLE_DISCONNECTED}, for analytics purposes or whatever.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) Interval getTimeInState(BleDeviceState state)
    {
        return m_device.getTimeInState(state);
    }

    /**
     * Overload of {@link #refreshGattDatabase(Interval)} which uses the default gatt refresh delay of {@link BleDeviceConfig#DEFAULT_GATT_REFRESH_DELAY}.
     */
    public final void refreshGattDatabase()
    {
        refreshGattDatabase(Interval.millis(BleDeviceConfig.DEFAULT_GATT_REFRESH_DELAY));
    }

    /**
     * This only applies to a device which is {@link BleDeviceState#BLE_CONNECTED}. This is meant to be used mainly after performing a
     * firmware update, and the Gatt database has changed. This will clear the device's gatt cache, and perform discover services again.
     * The device will drop out of {@link BleDeviceState#SERVICES_DISCOVERED}, and enter {@link BleDeviceState#DISCOVERING_SERVICES}. So,
     * you can listen in your device's {@link DeviceStateListener} for when it enters {@link BleDeviceState#SERVICES_DISCOVERED} to know
     * when the operation is complete.
     */
    public final void refreshGattDatabase(Interval gattPause)
    {
        m_device.refreshGattDatabase(gattPause);
    }

    /**
     * Same as {@link #setName(String, UUID)} but will not attempt to propagate the
     * name change to the remote device. Only {@link #getName_override()} will be affected by this.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) Single<RxReadWriteEvent> setName(final String name)
    {
        return setName(name, null);
    }

    /**
     * Sets the local name of the device and also attempts a {@link #write(BleWrite)}
     * using the given {@link UUID}. Further calls to {@link #getName_override()} will immediately reflect the name given here.
     * Further calls to {@link #getName_native()}, {@link #getName_debug()} and {@link #getName_normalized()} will only reflect
     * the name given here if the write is successful. It is somewhat assumed that doing this write will cause the remote device
     * to use the new name given here for its device information service {@link Uuids#DEVICE_NAME}.
     * If {@link BleDeviceConfig#saveNameChangesToDisk} is <code>true</code> then this name
     * will always be returned for {@link #getName_override()}, even if you kill/restart the app.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) Single<RxReadWriteEvent> setName(final String name, final UUID characteristicUuid)
    {
        return Single.create((SingleEmitter<ReadWriteEvent> emitter) ->
        {
            if (emitter.isDisposed()) return;

            m_device.setName(name, characteristicUuid, e ->
            {
                if (emitter.isDisposed()) return;

                if (e.wasSuccess())
                {
                    emitter.onSuccess(e);
                }
                else
                    emitter.onError(new ReadWriteException(new RxReadWriteEvent(e)));
            });
        }).subscribeOn(SweetBlueSchedulers.sweetBlueThread()).map(RxReadWriteEvent::new);
    }

    /**
     * Clears any name previously provided through {@link #setName(String)} or overloads.
     */
    public final void clearName()
    {
        m_device.clearName();
    }

    /**
     * By default returns the same value as {@link #getName_native()}.
     * If you call {@link #setName(String)} (or overloads)
     * then calling this will return the same string provided in that setter.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) String getName_override()
    {
        return m_device.getName_override();
    }

    /**
     * Returns the raw, unmodified device name retrieved from the stack.
     * Equivalent to {@link BluetoothDevice#getName()}. It's suggested to use
     * {@link #getName_normalized()} if you're using the name to match/filter
     * against something, e.g. an entry in a config file or for advertising
     * filtering.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) String getName_native()
    {
        return m_device.getName_native();
    }

    /**
     * The name retrieved from {@link #getName_native()} can change arbitrarily,
     * like the last 4 of the MAC address can get appended sometimes, and spaces
     * might get changed to underscores or vice-versa, caps to lowercase, etc.
     * This may somehow be standard, to-the-spec behavior but to the newcomer
     * it's confusing and potentially time-bomb-bug-inducing, like if you're
     * using device name as a filter for something and everything's working
     * until one day your app is suddenly broken and you don't know why. This
     * method is an attempt to normalize name behavior and always return the
     * same name regardless of the underlying stack's whimsy. The target format
     * is all lowercase and underscore-delimited with no trailing MAC address.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) String getName_normalized()
    {
        return m_device.getName_normalized();
    }

    /**
     * Returns a name useful for logging and debugging. As of this writing it is
     * {@link #getName_normalized()} plus the last four digits of the device's
     * MAC address from {@link #getMacAddress()}. {@link BleDevice#toString()}
     * uses this.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) String getName_debug()
    {
        return m_device.getName_debug();
    }

    /**
     * Provides just-in-case lower-level access to the native device instance.
     * <br><br>
     * WARNING: Be careful with this. It generally should not be needed. Only
     * invoke "mutators" of this object in times of extreme need.
     * <br><br>
     * NOTE: If you are forced to use this please contact library developers to
     * discuss possible feature addition or report bugs.
     */
    @Advanced
    public final @Nullable(Nullable.Prevalence.RARE) BluetoothDevice getNative()
    {
        return m_device.getNative();
    }

    /**
     * See pertinent warning for {@link #getNative()}. Generally speaking, this
     * will return <code>null</code> if the BleDevice is {@link BleDeviceState#BLE_DISCONNECTED}.
     * <br><br>
     * NOTE: If you are forced to use this please contact library developers to
     * discuss possible feature addition or report bugs.
     */
    @Advanced
    public final @Nullable(Nullable.Prevalence.NORMAL) BluetoothGatt getNativeGatt()
    {
        return m_device.getNativeGatt();
    }

    /**
     * Returns the MAC address of this device, as retrieved from the native stack or provided through {@link RxBleManager#newDevice(String)} (or overloads thereof).
     * You may treat this as the unique ID of the device, suitable as a key in a {@link java.util.HashMap}, {@link android.content.SharedPreferences}, etc.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) String getMacAddress()
    {
        return m_device.getMacAddress();
    }

    /**
     * Disconnects from a connected device or does nothing if already {@link BleDeviceState#BLE_DISCONNECTED}. You can call this at any point
     * during the connection process as a whole, during reads and writes, during transactions, whenever, and the device will cleanly cancel all ongoing
     * operations. This method will also bring the device out of the {@link BleDeviceState#RECONNECTING_LONG_TERM} state.
     *
     * @return <code>true</code> if this call "had an effect", such as if the device was previously {@link BleDeviceState#RECONNECTING_LONG_TERM},
     * {@link BleDeviceState#CONNECTING_OVERALL}, or {@link BleDeviceState#INITIALIZED}
     * @see com.idevicesinc.sweetblue.DeviceReconnectFilter.Status#EXPLICIT_DISCONNECT
     */
    public final boolean disconnect()
    {
        return m_device.disconnect();
    }

    /**
     * Similar to {@link #disconnect()} with the difference being the disconnect task is set to a low priority. This allows all current calls to finish
     * executing before finally disconnecting. Note that this can cause issues if you keep executing reads/writes, as they have a higher priority.
     *
     * @return <code>true</code> if this call "had an effect", such as if the device was previously {@link BleDeviceState#RECONNECTING_LONG_TERM},
     * {@link BleDeviceState#CONNECTING_OVERALL}, or {@link BleDeviceState#INITIALIZED}
     * @see com.idevicesinc.sweetblue.DeviceReconnectFilter.Status#EXPLICIT_DISCONNECT
     */
    public final boolean disconnectWhenReady()
    {
        return m_device.disconnectWhenReady();
    }

    /**
     * Same as {@link #disconnect()} but this call roughly simulates the disconnect as if it's because of the remote device going down, going out of range, etc.
     * For example {@link #getLastDisconnectIntent()} will be {@link com.idevicesinc.sweetblue.utils.State.ChangeIntent#UNINTENTIONAL} instead of
     * {@link com.idevicesinc.sweetblue.utils.State.ChangeIntent#INTENTIONAL}.
     * <br><br>
     * If the device is currently {@link BleDeviceState#CONNECTING_OVERALL} then your
     * {@link DeviceReconnectFilter#onConnectFailed(ReconnectFilter.ConnectFailEvent)}
     * implementation will be called with {@link com.idevicesinc.sweetblue.DeviceReconnectFilter.Status#ROGUE_DISCONNECT}.
     * <br><br>
     * NOTE: One major difference between this and an actual remote disconnect is that this will not cause the device to enter
     * {@link BleDeviceState#RECONNECTING_SHORT_TERM} or {@link BleDeviceState#RECONNECTING_LONG_TERM}.
     */
    public final boolean disconnect_remote()
    {
        return m_device.disconnect_remote();
    }

    /**
     * Convenience method that calls {@link RxBleManager#undiscover(RxBleDevice)}.
     *
     * @return <code>true</code> if the device was successfully {@link BleDeviceState#UNDISCOVERED}, <code>false</code> if BleDevice isn't known to the {@link BleManager}.
     * @see BleManager#undiscover(BleDevice)
     */
    public final boolean undiscover()
    {
        return m_device.undiscover();
    }

    /**
     * Convenience forwarding of {@link RxBleManager#clearSharedPreferences(String)}.
     *
     * @see RxBleManager#clearSharedPreferences(RxBleDevice)
     */
    public final void clearSharedPreferences()
    {
        m_device.clearSharedPreferences();
    }

    /**
     * First checks referential equality and if <code>false</code> checks
     * equality of {@link #getMacAddress()}. Note that ideally this method isn't
     * useful to you and never returns true (besides the identity case, which
     * isn't useful to you). Otherwise it probably means your app is holding on
     * to old references that have been undiscovered, and this may be a bug or
     * bad design decision in your code. This library will (well, should) never
     * hold references to two devices such that this method returns true for them.
     */
    public final boolean equals(@Nullable(Nullable.Prevalence.NORMAL) final RxBleDevice device_nullable)
    {
        if (device_nullable == null)
            return false;
        return m_device.equals(device_nullable.getBleDevice());
    }

    /**
     * Returns {@link #equals(RxBleDevice)} if object is an instance of {@link RxBleDevice}. Otherwise calls super.
     *
     * @see RxBleDevice#equals(RxBleDevice)
     */
    @Override public final boolean equals(@Nullable(Nullable.Prevalence.NORMAL) final Object object_nullable)
    {
        return m_device.equals(object_nullable);
    }

    public final @Nullable(Nullable.Prevalence.NEVER) Observable<RxReadWriteEvent> startPoll(final UUID characteristicUuid, Interval interval)
    {
        return startPoll(null, characteristicUuid, null, interval);
    }

    public final @Nullable(Nullable.Prevalence.NEVER) Observable<RxReadWriteEvent> startPoll(final UUID serviceUuid, final UUID characteristicUuid, Interval interval)
    {
        return startPoll(serviceUuid, characteristicUuid, null, interval);
    }

    public final @Nullable(Nullable.Prevalence.NEVER) Observable<RxReadWriteEvent> startPoll(UUID[] uuids, Interval interval)
    {
        uuids = uuids != null ? uuids : new UUID[0];

        Observable<RxReadWriteEvent> o = null;

        for (int i = 0; i < uuids.length; i++)
        {
            Observable<RxReadWriteEvent> ob = startPoll(uuids[i], interval);
            if (i == 0)
                o = ob;
            else
                o = o.mergeWith(ob);
        }
        return o;
    }

    public final @Nullable(Nullable.Prevalence.NEVER) Observable<RxReadWriteEvent> startPoll(Iterable<UUID> uuids, Interval interval)
    {
        if (uuids == null)
            return Observable.fromArray();

        Observable<RxReadWriteEvent> o = null;

        final Iterator<UUID> it = uuids.iterator();

        while (it.hasNext())
        {
            if (o == null)
                o = startPoll(it.next(), interval);
            else
                o = o.mergeWith(startPoll(it.next(), interval));
        }

        return o;
    }

    /**
     * Starts a periodic read of a particular characteristic. Use this wherever you can in place of {@link #enableNotify(BleNotify)}. One
     * use case would be to periodically read wind speed from a weather device. You *could* develop your device firmware to send notifications to the app
     * only when the wind speed changes, but Android has observed stability issues with notifications, so use them only when needed.
     * <br><br>
     * TIP: You can call this method when the device is in any {@link BleDeviceState}, even {@link BleDeviceState#BLE_DISCONNECTED}. However, you will not receive
     * any events until the device is connected.
     *
     * @see #startChangeTrackingPoll(UUID, Interval)
     * @see #enableNotify(BleNotify)
     */
    public final @Nullable(Nullable.Prevalence.NEVER) Observable<RxReadWriteEvent> startPoll(final UUID serviceUuid, final UUID characteristicUuid, final DescriptorFilter descriptorFilter, final Interval interval)
    {
        return Observable.create((ObservableOnSubscribe<ReadWriteEvent>) emitter ->
        {
            if (emitter.isDisposed()) return;

            final ReadWriteListener listener = e ->
            {
                if (emitter.isDisposed()) return;

                emitter.onNext(e);
            };

            m_device.startPoll(serviceUuid, characteristicUuid, descriptorFilter, interval, listener);

            emitter.setCancellable(() -> m_device.stopPoll(serviceUuid, characteristicUuid, descriptorFilter, interval, listener));
        }).subscribeOn(SweetBlueSchedulers.sweetBlueThread()).map(RxReadWriteEvent::new);
    }

    public final @Nullable(Nullable.Prevalence.NEVER) Observable<RxReadWriteEvent> startChangeTrackingPoll(UUID characteristicUuid, Interval interval)
    {
        return startChangeTrackingPoll(null, characteristicUuid, null, interval);
    }

    public final @Nullable(Nullable.Prevalence.NEVER) Observable<RxReadWriteEvent> startChangeTrackingPoll(UUID serviceUuid, UUID characteristicUuid, Interval interval)
    {
        return startChangeTrackingPoll(serviceUuid, characteristicUuid, null, interval);
    }

    /**
     * Similar to {@link #startPoll(UUID, Interval)} but only
     * invokes a callback when a change in the characteristic value is detected.
     * Use this in preference to {@link #enableNotify(BleNotify)} if possible,
     * due to instability issues (rare, but still) with notifications on Android.
     * <br><br>
     * TIP: You can call this method when the device is in any {@link BleDeviceState}, even {@link BleDeviceState#BLE_DISCONNECTED}. However, you will not receive any
     * events until the device is connected.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) Observable<RxReadWriteEvent> startChangeTrackingPoll(final UUID serviceUuid, final UUID characteristicUuid, final DescriptorFilter descriptorFilter, final Interval interval)
    {
        return Observable.create((ObservableOnSubscribe<ReadWriteEvent>) emitter ->
        {
            if (emitter.isDisposed()) return;

            ReadWriteListener listener = e ->
            {
                if (emitter.isDisposed()) return;

                emitter.onNext(e);
            };
            m_device.startChangeTrackingPoll(serviceUuid, characteristicUuid, descriptorFilter, interval, listener);

            emitter.setCancellable(() -> m_device.stopPoll(serviceUuid, characteristicUuid, descriptorFilter, interval, listener));
        }).subscribeOn(SweetBlueSchedulers.sweetBlueThread()).map(RxReadWriteEvent::new);
    }


    /**
     * Connect to this BLE device.
     * <p>
     * Returns a {@link Completable} which holds nothing. {@link CompletableEmitter#onComplete()} will be called when connected, otherwise
     * a {@link ConnectException} will be returned in {@link CompletableEmitter#onError(Throwable)}, which contains the
     * {@link com.idevicesinc.sweetblue.DeviceReconnectFilter.ConnectFailEvent}. The onError method will only be called when SweetBlue
     * has run through all retries -- set via {@link DeviceReconnectFilter}.
     *
     * @see #connect(RxBleTransaction.RxAuth, RxBleTransaction.RxInit)
     * @see #connect_withRetries()
     */
    public @Nullable(Nullable.Prevalence.NEVER) Completable connect()
    {
        return Completable.create(emitter ->
        {
            if (emitter.isDisposed()) return;

            m_device.connect(e ->
            {
                if (emitter.isDisposed()) return;

                if (e.wasSuccess())
                    emitter.onComplete();
                else if (!e.isRetrying())
                    emitter.onError(new ConnectException(e.failEvent()));
            });
        });
    }

    /**
     * Similar to {@link #connect()}, only this method allows you to pass in {@link RxBleTransaction.RxAuth}, and {@link RxBleTransaction.RxInit}
     * instances for this specific device -- instead of using {@link BleDeviceConfig#defaultAuthFactory}, or {@link BleDeviceConfig#defaultInitFactory}.
     *
     * @see #connect()
     * @see #connect(RxBleTransaction.RxAuth)
     * @see #connect(RxBleTransaction.RxInit)
     */
    public @Nullable(Nullable.Prevalence.NEVER) Completable connect(RxBleTransaction.RxAuth authTxn, RxBleTransaction.RxInit initTxn)
    {
        return Completable.create(emitter ->
        {
            if (emitter.isDisposed()) return;

            BleTransaction.Auth aTxn = authTxn != null ? authTxn.getWrappedTxn() : null;
            BleTransaction.Init iTxn = initTxn != null ? initTxn.getWrappedTxn() : null;

            m_device.connect(aTxn, iTxn, e ->
            {
                if (emitter.isDisposed()) return;

                if (e.wasSuccess())
                    emitter.onComplete();
                else
                    emitter.onError(new ConnectException(e.failEvent()));
            });
        });
    }

    /**
     * Overload of {@link #connect(RxBleTransaction.RxAuth, RxBleTransaction.RxInit)}. This will use {@link BleDeviceConfig#defaultInitFactory} for the init transaction,
     * if one has been set. If you don't want this behavior, then call {@link #connect(RxBleTransaction.RxAuth, RxBleTransaction.RxInit)}, and pass in <code>null</code>
     * for the init transaction.
     *
     * @see #connect(RxBleTransaction.RxAuth, RxBleTransaction.RxInit)
     * @see #connect()
     */
    public @Nullable(Nullable.Prevalence.NEVER) Completable connect(RxBleTransaction.RxAuth authTxn)
    {
        RxBleTransaction.RxInit initTxn = null;
        if (getConfig().defaultRxInitFactory != null)
            initTxn = getConfig().defaultRxInitFactory.newInitTxn();
        return connect(authTxn, initTxn);
    }

    /**
     * Overload of {@link #connect(RxBleTransaction.RxAuth, RxBleTransaction.RxInit)}. This will use {@link BleDeviceConfig#defaultAuthFactory} for the auth transaction,
     * if one has been set. If you don't want this behavior, then call {@link #connect(RxBleTransaction.RxAuth, RxBleTransaction.RxInit)}, and pass in <code>null</code>
     * for the auth transaction.
     *
     * @see #connect(RxBleTransaction.RxAuth, RxBleTransaction.RxInit)
     * @see #connect()
     */
    public @Nullable(Nullable.Prevalence.NEVER) Completable connect(RxBleTransaction.RxInit initTxn)
    {
        RxBleTransaction.RxAuth authTxn = null;
        if (getConfig().defaultRxAuthFactory != null)
            authTxn = getConfig().defaultRxAuthFactory.newAuthTxn();
        return connect(authTxn, initTxn);
    }

    /**
     * Similar to {@link #connect()}, only the {@link ObservableEmitter#onNext(Object)} method can get called multiple times depending on what
     * logic is implemented in {@link BleManagerConfig#reconnectFilter}. {@link ObservableEmitter#onError(Throwable)} will be called on the last
     * failed connection attempt (ie the library has tried all retries, and will no longer be attempting to connect).
     *
     * It's best not to use this method, and to use {@link #connect()} instead.
     *
     * @see #connect()
     * @see RxConnectEvent#wasSuccess()
     * @see RxConnectEvent#isRetrying()
     */
    @Advanced
    public @Nullable(Nullable.Prevalence.NEVER) Observable<RxConnectEvent> connect_withRetries()
    {
        return Observable.create((ObservableEmitter<DeviceConnectListener.ConnectEvent> emitter) ->
        {
            if (emitter.isDisposed()) return;

            m_device.connect(e ->
            {
                if (emitter.isDisposed()) return;

                if (!e.wasSuccess() && !e.isRetrying())
                {
                    emitter.onError(new ConnectException(e.failEvent()));
                }
                else
                {
                    emitter.onNext(e);
                }
            });
        }).map(RxConnectEvent::new);
    }

    /**
     * Similar to {@link #connect_withRetries()}, only this method allows you to pass in {@link RxBleTransaction.RxAuth}, and {@link RxBleTransaction.RxInit}
     * instances for this specific device -- instead of using {@link BleDeviceConfig#defaultAuthFactory}, or {@link BleDeviceConfig#defaultInitFactory}.
     *
     * @see #connect_withRetries()
     * @see #connect_withRetries(RxBleTransaction.RxAuth)
     * @see #connect_withRetries(RxBleTransaction.RxInit)
     */
    @Advanced
    public @Nullable(Nullable.Prevalence.NEVER) Observable<RxConnectEvent> connect_withRetries(RxBleTransaction.RxAuth authTxn, RxBleTransaction.RxInit initTxn)
    {
        return Observable.create((ObservableOnSubscribe<DeviceConnectListener.ConnectEvent>) emitter ->
        {
            if (emitter.isDisposed()) return;

            m_device.connect(authTxn.getWrappedTxn(), initTxn.getWrappedTxn(), e ->
            {
                if (emitter.isDisposed()) return;

                if (!e.wasSuccess() && !e.isRetrying())
                    emitter.onError(new ConnectException(e.failEvent()));
                else
                    emitter.onNext(e);
            });
        }).map(RxConnectEvent::new);
    }

    /**
     * Overload of {@link #connect_withRetries(RxBleTransaction.RxAuth, RxBleTransaction.RxInit)}. This will use {@link BleDeviceConfig#defaultInitFactory} for the init transaction,
     * if one has been set. If you don't want this behavior, then call {@link #connect_withRetries(RxBleTransaction.RxAuth)}, and pass in <code>null</code>
     * for the init transaction.
     *
     * @see #connect_withRetries(RxBleTransaction.RxAuth, RxBleTransaction.RxInit)
     * @see #connect_withRetries()
     */
    @Advanced
    public @Nullable(Nullable.Prevalence.NEVER) Observable<RxConnectEvent> connect_withRetries(RxBleTransaction.RxAuth authTxn)
    {
        RxBleTransaction.RxInit initTxn = null;
        if (getConfig().defaultRxInitFactory != null)
            initTxn = getConfig().defaultRxInitFactory.newInitTxn();
        return connect_withRetries(authTxn, initTxn);
    }

    /**
     * Overload of {@link #connect_withRetries(RxBleTransaction.RxAuth, RxBleTransaction.RxInit)}. This will use {@link BleDeviceConfig#defaultAuthFactory} for the auth transaction,
     * if one has been set. If you don't want this behavior, then call {@link #connect(RxBleTransaction.RxAuth, RxBleTransaction.RxInit)}, and pass in <code>null</code>
     * for the auth transaction.
     *
     * @see #connect_withRetries(RxBleTransaction.RxAuth, RxBleTransaction.RxInit)
     * @see #connect_withRetries()
     * @see #connect_withRetries(RxBleTransaction.RxAuth)
     */
    @Advanced
    public @Nullable(Nullable.Prevalence.NEVER) Observable<RxConnectEvent> connect_withRetries(RxBleTransaction.RxInit initTxn)
    {
        RxBleTransaction.RxAuth authTxn = null;
        if (getConfig().defaultRxAuthFactory != null)
            authTxn = getConfig().defaultRxAuthFactory.newAuthTxn();
        return connect_withRetries(authTxn, initTxn);
    }



    /**
     * Bonds this device.
     * <p>
     * Returns a {@link Single} which holds an instance of {@link com.idevicesinc.sweetblue.BondListener.BondEvent}. If the bond fails,
     * {@link SingleEmitter#onError(Throwable)} will be called which holds an instance of {@link BondException}, which also holds an instance
     * of {@link com.idevicesinc.sweetblue.BondListener.BondEvent}, so you can get more information on what went wrong.
     */
    public @Nullable(Nullable.Prevalence.NEVER) Single<RxBondEvent> bond()
    {
        return Single.create((SingleEmitter<BondListener.BondEvent> emitter) ->
        {
            if (emitter.isDisposed()) return;

            m_device.bond(e ->
            {
                if (emitter.isDisposed()) return;

                if (e.wasSuccess())
                    emitter.onSuccess(e);
                else
                    emitter.onError(new BondException(e));
            });
        }).map(RxBondEvent::new);
    }

    /**
     * Forwards {@link BleDevice#unbond()}.
     */
    public void unbond()
    {
        m_device.unbond();
    }

    /**
     * Perform a BLE read on this device.
     * <p>
     * Returns a {@link Single} which holds an instance of {@link ReadWriteEvent}. If the bond fails,
     * {@link SingleEmitter#onError(Throwable)} will be called which holds an instance of {@link ReadWriteException}, which also holds an instance
     * of {@link ReadWriteEvent}, so you can get more information on what went wrong.
     */
    public @Nullable(Nullable.Prevalence.NEVER) Single<RxReadWriteEvent> read(final BleRead read)
    {
        return read_private(read, null);
    }

    final Single<RxReadWriteEvent> read_private(final BleRead read, final IBleTransaction transaction)
    {
        return Single.create((SingleEmitter<ReadWriteEvent> emitter) ->
        {
            if (emitter.isDisposed()) return;

            read.setReadWriteListener(e ->
            {
                if (emitter.isDisposed()) return;

                if (e.wasSuccess())
                    emitter.onSuccess(e);
                else
                    emitter.onError(new ReadWriteException(new RxReadWriteEvent(e)));
            });

            if (transaction != null)
            {
                try
                {
                    transaction.getDevice().setThreadLocalTransaction(transaction);
                    m_device.read(read);
                }
                finally
                {
                    transaction.getDevice().setThreadLocalTransaction(null);
                }
            }
            else
                m_device.read(read);

        }).map(RxReadWriteEvent::new);
    }

    /**
     * Overload for {@link #read(BleRead)} for many writes. This calls {@link Observable#merge(Iterable)} after calling
     * {@link Single#toObservable()} to merge all Singles into one Observable.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) Observable<RxReadWriteEvent> readMany(final BleRead[] bleReads)
    {
        return readMany_private(bleReads, null);
    }

    final Observable<RxReadWriteEvent> readMany_private(final BleRead[] reads, IBleTransaction transaction)
    {
        if (reads == null || reads.length == 0)
            return Observable.fromArray();

        final List<Observable<RxReadWriteEvent>> list = new ArrayList<>();

        for (int i = 0; i < reads.length; i++)
        {
            final BleRead read = reads[i];

            Observable<RxReadWriteEvent> ob = read_private(read, transaction).toObservable();

            list.add(ob);

        }
        return Observable.merge(list);
    }


    /**
     * Overload for {@link #read(BleRead)} for many writes. This calls {@link Observable#mergeWith(ObservableSource)} after calling
     * {@link Single#toObservable()} to merge all Singles into one Observable.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) Observable<RxReadWriteEvent> readMany(final Iterable<BleRead> bleReads)
    {
        return readMany_private(bleReads, null);
    }

    final Observable<RxReadWriteEvent> readMany_private(Iterable<BleRead> reads, IBleTransaction transaction)
    {
        if (reads == null || !reads.iterator().hasNext())
            return Observable.fromArray();

        final Iterator<BleRead> iterator = reads.iterator();

        List<Observable<RxReadWriteEvent>> list = new ArrayList<>();

        while (iterator.hasNext())
        {
            final BleRead read = iterator.next();

            Observable<RxReadWriteEvent> ob = read_private(read, transaction).toObservable();

            list.add(ob);
        }

        return Observable.merge(list);
    }


    public final @Nullable(Nullable.Prevalence.NEVER) Single<RxReadWriteEvent> read(BleDescriptorRead read)
    {
        return read_private(read, null);
    }

    final Single<RxReadWriteEvent> read_private(BleDescriptorRead read, IBleTransaction transaction)
    {
        return Single.create((SingleOnSubscribe<ReadWriteEvent>) emitter ->
        {
            if (emitter.isDisposed()) return;

            read.setReadWriteListener(e ->
            {
                if (emitter.isDisposed()) return;

                if (e.wasSuccess())
                    emitter.onSuccess(e);
                else
                    emitter.onError(new ReadWriteException(new RxReadWriteEvent(e)));
            });

            if (transaction != null)
            {
                try
                {
                    transaction.getDevice().setThreadLocalTransaction(transaction);
                    m_device.read(read);
                }
                finally
                {
                    transaction.getDevice().setThreadLocalTransaction(null);
                }
            }
            else
                m_device.read(read);

        }).subscribeOn(SweetBlueSchedulers.sweetBlueThread()).map(RxReadWriteEvent::new);
    }

    /**
     * Performs a BLE write on this device.
     * <p>
     * Returns a {@link Single} which holds an instance of {@link ReadWriteEvent}. If the bond fails,
     * {@link SingleEmitter#onError(Throwable)} will be called which holds an instance of {@link ReadWriteException}, which also holds an instance
     * of {@link ReadWriteEvent}, so you can get more information on what went wrong.
     */
    public @Nullable(Nullable.Prevalence.NEVER) Single<RxReadWriteEvent> write(final BleWrite write)
    {
        return write_private(write, null);
    }

    final Single<RxReadWriteEvent> write_private(BleWrite write, IBleTransaction transaction)
    {
        return Single.create((SingleEmitter<ReadWriteEvent> emitter) ->
        {
            if (emitter.isDisposed()) return;

            write.setReadWriteListener((e) ->
            {

                if (emitter.isDisposed()) return;

                if (e.wasSuccess())
                    emitter.onSuccess(e);
                else
                    emitter.onError(new ReadWriteException(new RxReadWriteEvent(e)));
            });

            if (transaction != null)
            {
                try
                {
                    transaction.getDevice().setThreadLocalTransaction(transaction);
                    m_device.write(write);
                }
                finally
                {
                    transaction.getDevice().setThreadLocalTransaction(null);
                }
            }
            else
                m_device.write(write);

        }).map(RxReadWriteEvent::new);
    }

    /**
     * Overload for {@link #write(BleWrite)} for many writes. This calls {@link Observable#mergeWith(ObservableSource)} after calling
     * {@link Single#toObservable()} to merge all Singles into one Observable.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) Observable<RxReadWriteEvent> writeMany(BleWrite[] writes)
    {
        return writeMany_private(writes, null);
    }

    final Observable<RxReadWriteEvent> writeMany_private(BleWrite[] writes, IBleTransaction transaction)
    {
        if (writes == null || writes.length == 0)
            return Observable.fromArray();

        List<Observable<RxReadWriteEvent>> list = new ArrayList<>(writes.length);

        for (int i = 0; i < writes.length; i++)
        {
            final BleWrite write = writes[i];

            Observable<RxReadWriteEvent> ob = write_private(write, transaction).toObservable();
            list.add(ob);
        }
        return Observable.merge(list);
    }

    /**
     * Overload for {@link #write(BleWrite)} for many writes. This calls {@link Observable#mergeWith(ObservableSource)} after calling
     * {@link Single#toObservable()} to merge all Singles into one Observable.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) Observable<RxReadWriteEvent> writeMany(Iterable<BleWrite> writes)
    {
        return writeMany_private(writes, null);
    }

    final Observable<RxReadWriteEvent> writeMany_private(Iterable<BleWrite> writes, IBleTransaction transaction)
    {
        if (writes == null || !writes.iterator().hasNext())
            return Observable.fromArray();

        final Iterator<BleWrite> iterator = writes.iterator();

        List<Observable<RxReadWriteEvent>> list = new ArrayList<>();

        while (iterator.hasNext())
        {
            final BleWrite write = iterator.next();

            Observable<RxReadWriteEvent> ob = write_private(write, transaction).toObservable();
            list.add(ob);
        }

        return Observable.merge(list);
    }

    public final @Nullable(Nullable.Prevalence.NEVER) Single<RxReadWriteEvent> write(BleDescriptorWrite write)
    {
        return write_private(write, null);
    }

    final Single<RxReadWriteEvent> write_private(BleDescriptorWrite write, IBleTransaction transaction)
    {
        return Single.create((SingleOnSubscribe<ReadWriteEvent>) emitter ->
        {
            if (emitter.isDisposed()) return;

            write.setReadWriteListener(e ->
            {
                if (emitter.isDisposed()) return;

                if (e.wasSuccess())
                    emitter.onSuccess(e);
                else
                    emitter.onError(new ReadWriteException(new RxReadWriteEvent(e)));
            });

            if (transaction != null)
            {
                try
                {
                    transaction.getDevice().setThreadLocalTransaction(transaction);
                    m_device.write(write);
                }
                finally
                {
                    transaction.getDevice().setThreadLocalTransaction(null);
                }
            }
            else
                m_device.write(write);
        }).subscribeOn(SweetBlueSchedulers.sweetBlueThread()).map(RxReadWriteEvent::new);
    }

    /**
     * Enable notifications for a characteristic on this BLE device.
     * <p>
     * Returns a {@link Single} which holds an instance of {@link com.idevicesinc.sweetblue.NotificationListener.NotificationEvent}. If the bond fails,
     * {@link SingleEmitter#onError(Throwable)} will be called which holds an instance of {@link NotifyEnableException}, which also holds an instance
     * of {@link com.idevicesinc.sweetblue.NotificationListener.NotificationEvent}, so you can get more information on what went wrong.
     */
    public @Nullable(Nullable.Prevalence.NEVER) Single<RxNotificationEvent> enableNotify(final BleNotify notify)
    {
        return enableNotify_private(notify, null);
    }

    final Single<RxNotificationEvent> enableNotify_private(BleNotify notify, IBleTransaction transaction)
    {
        return Single.create((SingleEmitter<NotificationListener.NotificationEvent> emitter) ->
        {
            if (emitter.isDisposed()) return;

            notify.setNotificationListener((e) ->
            {
                if (emitter.isDisposed()) return;

                if (e.wasSuccess())
                    emitter.onSuccess(e);
                else
                    emitter.onError(new NotifyEnableException(e));
            });

            if (transaction != null)
            {
                try
                {
                    transaction.getDevice().setThreadLocalTransaction(transaction);
                    m_device.enableNotify(notify);
                }
                finally
                {
                    transaction.getDevice().setThreadLocalTransaction(null);
                }
            }
            else
                m_device.enableNotify(notify);

        }).map(RxNotificationEvent::new);
    }

    public @Nullable(Nullable.Prevalence.NEVER) Observable<RxNotificationEvent> enableNotifies(final List<BleNotify> notifies)
    {
        return enableNotifies_private(notifies, null);
    }

    final Observable<RxNotificationEvent> enableNotifies_private(List<BleNotify> notifies, IBleTransaction transaction)
    {
        return Observable.create((ObservableOnSubscribe<NotificationListener.NotificationEvent>) emitter ->
        {
            if (emitter.isDisposed()) return;

            NotificationListener listener = e ->
            {
                if (emitter.isDisposed()) return;

                emitter.onNext(e);
            };

            for (BleNotify n : notifies)
            {
                n.setNotificationListener(listener);
            }

            if (transaction != null)
            {
                try
                {
                    transaction.getDevice().setThreadLocalTransaction(transaction);
                    m_device.enableNotifies(notifies);
                }
                finally
                {
                    transaction.getDevice().setThreadLocalTransaction(null);
                }
            }
            else
                m_device.enableNotifies(notifies);

        }).map(RxNotificationEvent::new);
    }

    /**
     * Disable notifications for a characteristic on this BLE device.
     * <p>
     * Returns a {@link Single} which holds an instance of {@link com.idevicesinc.sweetblue.NotificationListener.NotificationEvent}. If the bond fails,
     * {@link SingleEmitter#onError(Throwable)} will be called which holds an instance of {@link NotifyEnableException}, which also holds an instance
     * of {@link com.idevicesinc.sweetblue.NotificationListener.NotificationEvent}, so you can get more information on what went wrong.
     */
    public @Nullable(Nullable.Prevalence.NEVER) Single<RxNotificationEvent> disableNotify(final BleNotify notify)
    {
        return disableNotify_private(notify, null);
    }

    final Single<RxNotificationEvent> disableNotify_private(BleNotify notify, IBleTransaction transaction)
    {
        return Single.create((SingleEmitter<NotificationListener.NotificationEvent> emitter) ->
        {
            if (emitter.isDisposed()) return;

            notify.setNotificationListener((e) ->
            {

                if (emitter.isDisposed()) return;

                if (e.wasSuccess())
                    emitter.onSuccess(e);
                else
                    emitter.onError(new NotifyEnableException(e));
            });

            if (transaction != null)
            {
                try
                {
                    transaction.getDevice().setThreadLocalTransaction(transaction);
                    m_device.disableNotify(notify);
                }
                finally
                {
                    transaction.getDevice().setThreadLocalTransaction(null);
                }
            }
            else
                m_device.disableNotify(notify);

        }).map(RxNotificationEvent::new);
    }

    public @Nullable(Nullable.Prevalence.NEVER) Observable<RxNotificationEvent> disableNotifies(final List<BleNotify> notifies)
    {
        return disableNotifies_private(notifies, null);
    }

    final Observable<RxNotificationEvent> disableNotifies_private(List<BleNotify> notifies, IBleTransaction transaction)
    {
        return Observable.create((ObservableOnSubscribe<NotificationListener.NotificationEvent>) emitter ->
        {
            if (emitter.isDisposed()) return;

            NotificationListener listener = e ->
            {
                if (emitter.isDisposed()) return;

                emitter.onNext(e);
            };

            for (BleNotify n : notifies)
            {
                n.setNotificationListener(listener);
            }

            if (transaction != null)
            {
                try
                {
                    transaction.getDevice().setThreadLocalTransaction(transaction);
                    m_device.disableNotifies(notifies);
                }
                finally
                {
                    transaction.getDevice().setThreadLocalTransaction(null);
                }
            }
            else
                m_device.disableNotifies(notifies);

        }).map(RxNotificationEvent::new);
    }

    /**
     * Wrapper for {@link BluetoothGatt#readRemoteRssi()}. This will eventually update the value returned by {@link #getRssi()} but it is not
     * instantaneous. When a new RSSI is actually received the given listener will be called. The device must be {@link BleDeviceState#BLE_CONNECTED} for
     * this call to succeed. When the device is not {@link BleDeviceState#BLE_CONNECTED} then the value returned by
     * {@link #getRssi()} will be automatically updated every time this device is discovered (or rediscovered) by a scan operation.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) Single<RxReadWriteEvent> readRssi()
    {
        return readRssi_private(null);
    }

    final Single<RxReadWriteEvent> readRssi_private(IBleTransaction transaction)
    {
        return Single.create((SingleOnSubscribe<ReadWriteEvent>) emitter ->
        {
            if (emitter.isDisposed()) return;

            ReadWriteListener listener = e ->
            {
                if (e.wasSuccess())
                    emitter.onSuccess(e);
                else
                    emitter.onError(new ReadWriteException(new RxReadWriteEvent(e)));
            };

            if (transaction != null)
            {
                try
                {
                    transaction.getDevice().setThreadLocalTransaction(transaction);
                    m_device.readRssi(listener);
                }
                finally
                {
                    transaction.getDevice().setThreadLocalTransaction(null);
                }
            }
            else
                m_device.readRssi(listener);

        }).subscribeOn(SweetBlueSchedulers.sweetBlueThread()).map(RxReadWriteEvent::new);
    }

    /**
     * Wrapper for {@link BluetoothGatt#requestConnectionPriority(int)} which attempts to change the connection priority for a given connection.
     * This will eventually update the value returned by {@link #getConnectionPriority()} but it is not
     * instantaneous. When we receive confirmation from the native stack then this value will be updated. The device must be {@link BleDeviceState#BLE_CONNECTED} for
     * this call to succeed.
     *
     * @see #getConnectionPriority()
     */
    public final @Nullable(Nullable.Prevalence.NEVER) Single<RxReadWriteEvent> setConnectionPriority(BleConnectionPriority priority)
    {
        return setConnectionPriority_private(priority, null);
    }

    final Single<RxReadWriteEvent> setConnectionPriority_private(BleConnectionPriority priority, IBleTransaction transaction)
    {
        return Single.create((SingleOnSubscribe<ReadWriteEvent>) emitter ->
        {
            if (emitter.isDisposed()) return;

            ReadWriteListener listener = e ->
            {
                if (emitter.isDisposed()) return;

                if (e.wasSuccess())
                    emitter.onSuccess(e);
                else
                    emitter.onError(new ReadWriteException(new RxReadWriteEvent(e)));
            };

            if (transaction != null)
            {
                try
                {
                    transaction.getDevice().setThreadLocalTransaction(transaction);
                    m_device.setConnectionPriority(priority, listener);
                }
                finally
                {
                    transaction.getDevice().setThreadLocalTransaction(null);
                }
            }
            else
                m_device.setConnectionPriority(priority, listener);

        }).subscribeOn(SweetBlueSchedulers.sweetBlueThread()).map(RxReadWriteEvent::new);
    }

    /**
     * Returns the connection priority value set by {@link #setConnectionPriority(BleConnectionPriority)}, or {@link BleDeviceConfig#DEFAULT_MTU_SIZE} if
     * it was never set explicitly.
     */
    @Advanced
    public final @Nullable(Nullable.Prevalence.NEVER) BleConnectionPriority getConnectionPriority()
    {
        return m_device.getConnectionPriority();
    }

    /**
     * Returns the "maximum transmission unit" value set by {@link #negotiateMtu(int)}, or {@link BleDeviceConfig#DEFAULT_MTU_SIZE} if
     * it was never set explicitly.
     */
    @Advanced
    public final int getMtu()
    {
        return m_device.getMtu();
    }

    /**
     * Overload of {@link #negotiateMtu(int)} that returns the "maximum transmission unit" to the default.
     * Unlike {@link #negotiateMtu(int)}, this can be called when the device is {@link BleDeviceState#BLE_DISCONNECTED} in the event that you don't want the
     * MTU to be auto-set upon next reconnection.
     */
    @Advanced
    public final @Nullable(Nullable.Prevalence.NEVER) Single<RxReadWriteEvent> negotiateMtuToDefault()
    {
        return negotiateMtuToDefault_private(null);
    }

    final Single<RxReadWriteEvent> negotiateMtuToDefault_private(IBleTransaction transaction)
    {
        return Single.create((SingleOnSubscribe<ReadWriteEvent>) emitter ->
        {
            if (emitter.isDisposed()) return;

            ReadWriteListener listener = e ->
            {
                if (e.wasSuccess())
                    emitter.onSuccess(e);
                else
                    emitter.onError(new ReadWriteException(new RxReadWriteEvent(e)));
            };

            if (transaction != null)
            {
                try
                {
                    transaction.getDevice().setThreadLocalTransaction(transaction);
                    m_device.negotiateMtuToDefault(listener);
                }
                finally
                {
                    transaction.getDevice().setThreadLocalTransaction(null);
                }
            }
            else
                m_device.negotiateMtuToDefault(listener);

        }).subscribeOn(SweetBlueSchedulers.sweetBlueThread()).map(RxReadWriteEvent::new);
    }

    /**
     * Wrapper for {@link BluetoothGatt#requestMtu(int)} which attempts to change the "maximum transmission unit" for a given connection.
     * This will eventually update the value returned by {@link #getMtu()} but it is not
     * instantaneous. When we receive confirmation from the native stack then this value will be updated. The device must be {@link BleDeviceState#BLE_CONNECTED} for
     * this call to succeed.
     * <p>
     * <b>NOTE 1:</b> This will only work on devices running Android Lollipop (5.0) or higher. Otherwise it will be ignored.
     * <b>NOTE 2:</b> Some phones will request an MTU, and accept a higher number, but will fail (time out) when writing a characteristic with a large
     * payload. Namely, we've found the Moto Pure X, and the OnePlus OnePlus2 to have this behavior. For those phones any MTU above
     * 50 failed.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) Single<RxReadWriteEvent> negotiateMtu(int mtu)
    {
        return negotiateMtu_private(mtu, null);
    }

    final Single<RxReadWriteEvent> negotiateMtu_private(int mtu, IBleTransaction transaction)
    {
        return Single.create((SingleOnSubscribe<ReadWriteEvent>) emitter ->
        {
            if (emitter.isDisposed()) return;

            ReadWriteListener listener = e ->
            {
                if (e.wasSuccess())
                    emitter.onSuccess(e);
                else
                    emitter.onError(new ReadWriteException(new RxReadWriteEvent(e)));
            };

            if (transaction != null)
            {
                try
                {
                    transaction.getDevice().setThreadLocalTransaction(transaction);
                    m_device.negotiateMtu(mtu, listener);
                }
                finally
                {
                    transaction.getDevice().setThreadLocalTransaction(null);
                }
            }
            else
                m_device.negotiateMtu(mtu, listener);

        }).subscribeOn(SweetBlueSchedulers.sweetBlueThread()).map(RxReadWriteEvent::new);
    }

    /**
     * Use this method to manually set the bluetooth 5 physical layer to use a bluetooth 5 feature (high speed/long range).
     * <b>NOTE: This only works on Android Oreo and above, and not all devices on this OS have bluetooth 5 hardware.</b>
     *
     * @see RxBleManager#isBluetooth5Supported()
     * @see RxBleManager#isBluetooth5HighSpeedSupported()
     * @see RxBleManager#isBluetooth5LongRangeSupported()
     */
    public final @Nullable(Nullable.Prevalence.NEVER) Single<RxReadWriteEvent> setPhyOptions(Phy phyOption)
    {
        return setPhyOptions_private(phyOption, null);
    }

    final Single<RxReadWriteEvent> setPhyOptions_private(Phy phyOption, IBleTransaction transaction)
    {
        return Single.create((SingleOnSubscribe<ReadWriteEvent>) emitter ->
        {
            if (emitter.isDisposed()) return;

            ReadWriteListener listener = e ->
            {
                if (e.wasSuccess())
                    emitter.onSuccess(e);
                else
                    emitter.onError(new ReadWriteException(new RxReadWriteEvent(e)));
            };

            if (transaction != null)
            {
                try
                {
                    transaction.getDevice().setThreadLocalTransaction(transaction);
                    m_device.setPhyOptions(phyOption, listener);
                }
                finally
                {
                    transaction.getDevice().setThreadLocalTransaction(null);
                }
            }
            else
                m_device.setPhyOptions(phyOption, listener);

        }).subscribeOn(SweetBlueSchedulers.sweetBlueThread()).map(RxReadWriteEvent::new);
    }

    /**
     * Method to get the current "phy options" (physical layer), or current bluetooth 5 feature. This shouldn't need to be called, as SweetBlue caches this info for you, but it's here
     * for flexibility, and just-in-case scenarios.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) Single<RxReadWriteEvent> readPhyOptions()
    {
        return readPhyOptions(null);
    }

    final Single<RxReadWriteEvent> readPhyOptions(IBleTransaction transaction)
    {
        return Single.create((SingleOnSubscribe<ReadWriteEvent>) emitter ->
        {
            if (emitter.isDisposed()) return;

            ReadWriteListener listener = e ->
            {
                if (e.wasSuccess())
                    emitter.onSuccess(e);
                else
                    emitter.onError(new ReadWriteException(new RxReadWriteEvent(e)));
            };

            if (transaction != null)
            {
                try
                {
                    transaction.getDevice().setThreadLocalTransaction(transaction);
                    m_device.readPhyOptions(listener);
                }
                finally
                {
                    transaction.getDevice().setThreadLocalTransaction(null);
                }
            }
            else
                m_device.readPhyOptions(listener);

        }).subscribeOn(SweetBlueSchedulers.sweetBlueThread()).map(RxReadWriteEvent::new);
    }

    /**
     * Kicks off a poll that automatically calls {@link #readRssi()} at the {@link Interval} frequency
     * specified. This can be called before the device is actually {@link BleDeviceState#BLE_CONNECTED}. If you call this more than once in a
     * row then the most recent call's parameters will be respected.
     * <br><br>
     * TIP: You can call this method when the device is in any {@link BleDeviceState}, even {@link BleDeviceState#BLE_DISCONNECTED}, however, a scan must
     * be running in order to receive any updates (and of course, the device must be found in the scan).
     */
    public final @Nullable(Nullable.Prevalence.NEVER) Observable<RxReadWriteEvent> startRssiPoll(Interval interval)
    {
        return Observable.create((ObservableOnSubscribe<ReadWriteEvent>) emitter ->
        {
            if (emitter.isDisposed()) return;

            m_device.startRssiPoll(interval, e ->
            {
                if (emitter.isDisposed()) return;

                emitter.onNext(e);
            });

            emitter.setCancellable(m_device::stopRssiPoll);

        }).subscribeOn(SweetBlueSchedulers.sweetBlueThread()).map(RxReadWriteEvent::new);
    }

    /**
     * One method to remove absolutely all "metadata" related to this device that is stored on disk and/or cached in memory in any way.
     * This method is useful if for example you have a "forget device" feature in your app.
     */
    public final void clearAllData()
    {
        m_device.clearAllData();
    }

    /**
     * Clears all {@link HistoricalData} tracked by this device.
     *
     * @see com.idevicesinc.sweetblue.BleNodeConfig.HistoricalDataLogFilter
     * @see com.idevicesinc.sweetblue.BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final void clearHistoricalData()
    {
        m_device.clearHistoricalData();
    }

    /**
     * Clears the first <code>count</code> number of {@link HistoricalData} tracked by this device.
     *
     * @see com.idevicesinc.sweetblue.BleNodeConfig.HistoricalDataLogFilter
     * @see com.idevicesinc.sweetblue.BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final void clearHistoricalData(final long count)
    {
        clearHistoricalData(EpochTimeRange.FROM_MIN_TO_MAX, count);
    }

    /**
     * Clears all {@link HistoricalData} tracked by this device within the given range.
     *
     * @see com.idevicesinc.sweetblue.BleNodeConfig.HistoricalDataLogFilter
     * @see com.idevicesinc.sweetblue.BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final void clearHistoricalData(final EpochTimeRange range)
    {
        clearHistoricalData(range, Long.MAX_VALUE);
    }

    /**
     * Clears the first <code>count</code> number of {@link HistoricalData} tracked by this device within the given range.
     *
     * @see com.idevicesinc.sweetblue.BleNodeConfig.HistoricalDataLogFilter
     * @see com.idevicesinc.sweetblue.BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final void clearHistoricalData(final EpochTimeRange range, final long count)
    {
        m_device.clearHistoricalData(range, count);
    }

    /**
     * Clears all {@link HistoricalData} tracked by this device for a particular
     * characteristic {@link java.util.UUID}.
     *
     * @see com.idevicesinc.sweetblue.BleNodeConfig.HistoricalDataLogFilter
     * @see com.idevicesinc.sweetblue.BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final void clearHistoricalData(final UUID uuid)
    {
        clearHistoricalData(uuid, EpochTimeRange.FROM_MIN_TO_MAX, Long.MAX_VALUE);
    }

    /**
     * Overload of {@link #clearHistoricalData(UUID)} that just calls that method multiple times.
     */
    public final void clearHistoricalData(final UUID... uuids)
    {
        for (int i = 0; i < uuids.length; i++)
        {
            final UUID ith = uuids[i];

            clearHistoricalData(ith);
        }
    }

    /**
     * Clears the first <code>count</code> number of {@link HistoricalData} tracked by this device for a particular
     * characteristic {@link java.util.UUID}.
     *
     * @see com.idevicesinc.sweetblue.BleNodeConfig.HistoricalDataLogFilter
     * @see com.idevicesinc.sweetblue.BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final void clearHistoricalData(final UUID uuid, final long count)
    {
        clearHistoricalData(uuid, EpochTimeRange.FROM_MIN_TO_MAX, count);
    }

    /**
     * Clears all {@link HistoricalData} tracked by this device for a particular
     * characteristic {@link java.util.UUID} within the given range.
     *
     * @see com.idevicesinc.sweetblue.BleNodeConfig.HistoricalDataLogFilter
     * @see com.idevicesinc.sweetblue.BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final void clearHistoricalData(final UUID uuid, final EpochTimeRange range)
    {
        clearHistoricalData(uuid, range, Long.MAX_VALUE);
    }

    /**
     * Clears the first <code>count</code> number of {@link HistoricalData} tracked by this device for a particular
     * characteristic {@link java.util.UUID} within the given range.
     *
     * @see com.idevicesinc.sweetblue.BleNodeConfig.HistoricalDataLogFilter
     * @see com.idevicesinc.sweetblue.BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final void clearHistoricalData(final UUID uuid, final EpochTimeRange range, final long count)
    {
        m_device.clearHistoricalData(uuid, range, count);
    }

    /**
     * Clears all {@link HistoricalData} tracked by this device.
     *
     * @see com.idevicesinc.sweetblue.BleNodeConfig.HistoricalDataLogFilter
     * @see com.idevicesinc.sweetblue.BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final void clearHistoricalData_memoryOnly()
    {
        clearHistoricalData_memoryOnly(EpochTimeRange.FROM_MIN_TO_MAX, Long.MAX_VALUE);
    }

    /**
     * Clears the first <code>count</code> number of {@link HistoricalData} tracked by this device.
     *
     * @see com.idevicesinc.sweetblue.BleNodeConfig.HistoricalDataLogFilter
     * @see com.idevicesinc.sweetblue.BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final void clearHistoricalData_memoryOnly(final long count)
    {
        clearHistoricalData_memoryOnly(EpochTimeRange.FROM_MIN_TO_MAX, count);
    }

    /**
     * Clears all {@link HistoricalData} tracked by this device within the given range.
     *
     * @see com.idevicesinc.sweetblue.BleNodeConfig.HistoricalDataLogFilter
     * @see com.idevicesinc.sweetblue.BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final void clearHistoricalData_memoryOnly(final EpochTimeRange range)
    {
        clearHistoricalData_memoryOnly(range, Long.MAX_VALUE);
    }

    /**
     * Clears the first <code>count</code> number of {@link HistoricalData} tracked by this device within the given range.
     *
     * @see com.idevicesinc.sweetblue.BleNodeConfig.HistoricalDataLogFilter
     * @see com.idevicesinc.sweetblue.BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final void clearHistoricalData_memoryOnly(final EpochTimeRange range, final long count)
    {
        m_device.clearHistoricalData_memoryOnly(range, count);
    }

    /**
     * Clears all {@link HistoricalData} tracked by this device for a particular
     * characteristic {@link java.util.UUID}.
     *
     * @see com.idevicesinc.sweetblue.BleNodeConfig.HistoricalDataLogFilter
     * @see com.idevicesinc.sweetblue.BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final void clearHistoricalData_memoryOnly(final UUID uuid)
    {
        clearHistoricalData_memoryOnly(uuid, EpochTimeRange.FROM_MIN_TO_MAX, Long.MAX_VALUE);
    }

    /**
     * Clears the first <code>count</code> number of {@link HistoricalData} tracked by this device for a particular
     * characteristic {@link java.util.UUID}.
     *
     * @see com.idevicesinc.sweetblue.BleNodeConfig.HistoricalDataLogFilter
     * @see com.idevicesinc.sweetblue.BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final void clearHistoricalData_memoryOnly(final UUID uuid, final long count)
    {
        clearHistoricalData_memoryOnly(uuid, EpochTimeRange.FROM_MIN_TO_MAX, count);
    }

    /**
     * Clears all {@link HistoricalData} tracked by this device for a particular
     * characteristic {@link java.util.UUID} within the given range.
     *
     * @see com.idevicesinc.sweetblue.BleNodeConfig.HistoricalDataLogFilter
     * @see com.idevicesinc.sweetblue.BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final void clearHistoricalData_memoryOnly(final UUID characteristicUuid, final EpochTimeRange range)
    {
        clearHistoricalData_memoryOnly(characteristicUuid, range, Long.MAX_VALUE);
    }

    /**
     * Clears the first <code>count</code> number of {@link HistoricalData} tracked by this device for a particular
     * characteristic {@link java.util.UUID} within the given range.
     *
     * @see com.idevicesinc.sweetblue.BleNodeConfig.HistoricalDataLogFilter
     * @see com.idevicesinc.sweetblue.BleNodeConfig.DefaultHistoricalDataLogFilter
     */
    @Advanced
    public final void clearHistoricalData_memoryOnly(final UUID characteristicUuid, final EpochTimeRange range, final long count)
    {
        m_device.clearHistoricalData_memoryOnly(characteristicUuid, range, count);
    }

    /**
     * Returns <code>true</code> if notifications are enabled for the given uuid.
     * NOTE: {@link #isNotifyEnabling(UUID)} may return true even if this method returns false.
     *
     * @see #isNotifyEnabling(UUID)
     */
    public final boolean isNotifyEnabled(final UUID uuid)
    {
        return m_device.isNotifyEnabled(uuid);
    }

    /**
     * Returns <code>true</code> if SweetBlue is in the process of enabling notifications for the given uuid.
     *
     * @see #isNotifyEnabled(UUID)
     */
    public final boolean isNotifyEnabling(final UUID uuid)
    {
        return m_device.isNotifyEnabling(uuid);
    }

    /**
     * Kicks off an "over the air" long-term transaction if it's not already
     * taking place and the device is {@link BleDeviceState#INITIALIZED}. This
     * will put the device into the {@link BleDeviceState#PERFORMING_OTA} state
     * if <code>true</code> is returned. You can use this to do firmware
     * updates, file transfers, etc.
     * <br><br>
     * TIP: Use the {@link TimeEstimator} class to let your users know roughly
     * how much time it will take for the ota to complete.
     * <br><br>
     * TIP: For shorter-running transactions consider using {@link #performTransaction(RxBleTransaction)}.
     *
     * @return <code>true</code> if OTA has started, otherwise <code>false</code> if device is either already
     * {@link BleDeviceState#PERFORMING_OTA} or is not {@link BleDeviceState#INITIALIZED}.
     *
     * @see BleManagerConfig#includeOtaReadWriteTimesInAverage
     * @see BleManagerConfig#autoScanDuringOta
     * @see #performTransaction(RxBleTransaction)
     */
    public final boolean performOta(final RxBleTransaction.RxOta txn)
    {
        return m_device.performOta(txn.getWrappedTxn());
    }

    /**
     * Allows you to perform an arbitrary transaction that is not associated with any {@link BleDeviceState} like
     * {@link BleDeviceState#PERFORMING_OTA}, {@link BleDeviceState#AUTHENTICATING} or {@link BleDeviceState#INITIALIZING}.
     * Generally this transaction should be short, several reads and writes. For longer-term transaction consider using
     * {@link #performOta(RxBleTransaction.RxOta)}.
     * <br><br>
     * The device must be {@link BleDeviceState#INITIALIZED}.
     * <br><br>
     * TIP: For long-term transactions consider using {@link #performOta(RxBleTransaction.RxOta)}.
     *
     * @return <code>true</code> if the transaction successfully started, <code>false</code> otherwise if device is not {@link BleDeviceState#INITIALIZED}.
     */
    public final boolean performTransaction(final RxBleTransaction txn)
    {
        return m_device.performTransaction(txn.getWrappedTxn());
    }

    /**
     * Returns the effective MTU size for a write. BLE has an overhead when reading and writing, so that eats out of the MTU size.
     * The write overhead is defined via {@link BleManagerConfig#GATT_WRITE_MTU_OVERHEAD}. The method simply returns the MTU size minus
     * the overhead. This is just used internally, but is exposed in case it's needed for some other use app-side.
     */
    public final int getEffectiveWriteMtuSize()
    {
        return m_device.getEffectiveWriteMtuSize();
    }


    /**
     * As you should never get an instance of this class which is <code>null</code>, use this method to see if the device is considered to be
     * <code>null</code> or not.
     */
    public boolean isNull()
    {
        return m_device == null || m_device.isNull();
    }

    public BleDevice getBleDevice()
    {
        return m_device;
    }

    @Override
    public int hashCode()
    {
        return m_device.hashCode();
    }

    @Override
    public String toString()
    {
        return m_device.toString();
    }


    IBleDevice getIBleDevice()
    {
        return P_Bridge_User.getIBleDevice(m_device);
    }

    static RxBleDevice create(BleDevice device)
    {
        return new RxBleDevice(device);
    }

}
