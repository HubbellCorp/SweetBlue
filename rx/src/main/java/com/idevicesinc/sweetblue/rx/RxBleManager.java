package com.idevicesinc.sweetblue.rx;


import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import com.idevicesinc.sweetblue.AddServiceListener;
import com.idevicesinc.sweetblue.AdvertisingListener;
import com.idevicesinc.sweetblue.AssertListener;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDeviceConfig;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.BleManagerState;
import com.idevicesinc.sweetblue.BleServer;
import com.idevicesinc.sweetblue.BondListener;
import com.idevicesinc.sweetblue.DeviceConnectListener;
import com.idevicesinc.sweetblue.DeviceStateListener;
import com.idevicesinc.sweetblue.DiscoveryListener;
import com.idevicesinc.sweetblue.HistoricalDataLoadListener;
import com.idevicesinc.sweetblue.IncomingListener;
import com.idevicesinc.sweetblue.LogOptions;
import com.idevicesinc.sweetblue.NotificationListener;
import com.idevicesinc.sweetblue.OutgoingListener;
import com.idevicesinc.sweetblue.ReadWriteListener;
import com.idevicesinc.sweetblue.ResetListener;
import com.idevicesinc.sweetblue.ScanFilter;
import com.idevicesinc.sweetblue.ScanOptions;
import com.idevicesinc.sweetblue.DiscoveryListener.DiscoveryEvent;
import com.idevicesinc.sweetblue.ServerStateListener;
import com.idevicesinc.sweetblue.annotations.Advanced;
import com.idevicesinc.sweetblue.annotations.Experimental;
import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.rx.annotations.HotObservable;
import com.idevicesinc.sweetblue.DiscoveryListener.LifeCycle;
import com.idevicesinc.sweetblue.ManagerStateListener.StateEvent;
import com.idevicesinc.sweetblue.UhOhListener.UhOhEvent;
import com.idevicesinc.sweetblue.rx.plugins.RxSweetBluePlugins;
import com.idevicesinc.sweetblue.rx.schedulers.SweetBlueSchedulers;
import com.idevicesinc.sweetblue.utils.EpochTime;
import com.idevicesinc.sweetblue.utils.Event;
import com.idevicesinc.sweetblue.utils.GattDatabase;
import com.idevicesinc.sweetblue.utils.HistoricalData;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Pointer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.disposables.Disposable;


/**
 * Main entry point for the Rx module. You should use this class, rather than {@link BleManager} directly. The observe methods in this class
 * set listeners in the {@link BleManager} instance, so it's best not to use it as much as possible.
 */
public final class RxBleManager
{
    /**
     * Get the instance of {@link RxBleManager}. This class holds the instance of {@link BleManager}
     */
    public static @Nullable(Nullable.Prevalence.NEVER) RxBleManager get(Context context, RxBleManagerConfig config)
    {
        if (s_instance == null)
            s_instance = new RxBleManager(context, config);
        return s_instance;
    }

    /**
     * Get the instance of {@link RxBleManager}. If the instance needs to be created, a default
     * {@link BleManagerConfig} will be used.
     */
    public static @Nullable(Nullable.Prevalence.NEVER) RxBleManager get(Context context)
    {
        if (s_instance == null)
            s_instance = new RxBleManager(context, new RxBleManagerConfig());
        return s_instance;
    }


    /**
     * Convenience class for converting a {@link BleDevice} instance to an instance of {@link RxBleDevice} via RxJava
     */
    public static final RxBleDeviceTransformer BLE_DEVICE_TRANSFORMER = new RxBleDeviceTransformer();

    /**
     * Convenience class for converting a {@link com.idevicesinc.sweetblue.BleServer} instance to an instance of {@link RxBleServer} via RxJava
     */
    public static final RxBleServerTransformer BLE_SERVER_TRANSFORMER = new RxBleServerTransformer();

    // Map to hold instances of RxBleDevice. This is to avoid creating multiple instances of RxBleDevice for a single instance of BleDevice
    private static final Map<String, RxBleDevice> m_deviceMap = new HashMap<>();

    // This should arguably not be a map, as you can only have one BleServer going at once, but leaving it in case we figure something out in
    // the future where you can have many instances
    private static final Map<BleServer, RxBleServer> m_serverMap = new HashMap<>();


    private static RxBleManager s_instance;
    private final BleManager m_mgr;

    private RxBleManagerConfig m_config;

    private Flowable<RxUhOhEvent> m_uhOhFlowable;
    private Flowable<RxDeviceStateEvent> m_deviceStateFlowable;
    private Flowable<RxDeviceConnectEvent> m_deviceConnectFlowable;
    private Flowable<RxManagerStateEvent> m_mgrStateFlowable;
    private Flowable<RxAssertEvent> m_assertFlowable;
    private Flowable<RxServerStateEvent> m_serverStateFlowable;
    private Flowable<RxBondEvent> m_bondFlowable;
    private Flowable<RxReadWriteEvent> m_readWriteFlowable;
    private Flowable<RxNotificationEvent> m_notifyEventFlowable;
    private Flowable<RxHistoricalDataLoadEvent> m_historicalDataLoadFlowable;
    private Flowable<RxDiscoveryEvent> m_discoveryFlowable;
    private Flowable<RxOutgoingEvent> m_outgoingEventFlowable;
    private Flowable<RxServiceAddEvent> m_serviceAddEventFlowable;
    private Flowable<RxAdvertisingEvent> m_advertisingEventFlowable;


    private RxBleManager(Context context, RxBleManagerConfig config)
    {
        m_mgr = BleManager.get(context, config);
        m_config = config;
    }


    public final BleManager getBleManager()
    {
        return m_mgr;
    }


    /**
     * Overload of {@link #scan(ScanOptions)}, which uses a default instance of {@link ScanOptions}.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) Observable<RxDiscoveryEvent> scan()
    {
        return scan(new ScanOptions());
    }

    /**
     * Returns an {@link Observable} which kicks off a scan using the provided {@link ScanOptions} once subscribed to. The observable returns an {@link RxDiscoveryEvent}, so
     * that you can see if the device was {@link LifeCycle#DISCOVERED}, {@link LifeCycle#REDISCOVERED}, or {@link LifeCycle#UNDISCOVERED}. Be aware that the
     * {@link LifeCycle#REDISCOVERED} state can be emitted many times in a single scan. In most cases, {@link #scan_onlyNew(ScanOptions)} will suffice, as it only emits
     * when a {@link BleDevice} is {@link LifeCycle#DISCOVERED}.
     * <p>
     * NOTE: This ignores any {@link DiscoveryListener} that is set within the {@link ScanOptions} instance passed into this method.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) Observable<RxDiscoveryEvent> scan(@Nullable(Nullable.Prevalence.NORMAL) ScanOptions options)
    {
        return Observable.create((ObservableEmitter<DiscoveryEvent> emitter) ->
        {
            if (emitter.isDisposed()) return;

            options.withDiscoveryListener((DiscoveryEvent e) ->
            {
                if (emitter.isDisposed()) return;

                emitter.onNext(e);
            });

            m_mgr.startScan(options);

            emitter.setCancellable(m_mgr::stopScan);

        }).map((DiscoveryEvent ev) -> new RxDiscoveryEvent(RxBleManager.this, ev));
    }

    /**
     * Returns a {@link Flowable} which kicks off a scan using the provided {@link ScanOptions} once subscribed to. The observable returns a {@link BleDevice}, as this
     * method will only ever emit devices that were {@link LifeCycle#DISCOVERED}, as opposed to being {@link LifeCycle#REDISCOVERED}, or {@link LifeCycle#UNDISCOVERED}. If
     * you care about those other states, then you should use {@link #scan(ScanOptions)} instead.
     * <p>
     * NOTE: This ignores any {@link DiscoveryListener} that is set within the {@link ScanOptions} instance passed into this method.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) Observable<RxBleDevice> scan_onlyNew(@Nullable(Nullable.Prevalence.NORMAL) ScanOptions options)
    {
        // Filter out anything other than DISCOVERED devices (ignoring rediscovery, and undiscovery)
        return scan(options).filter(RxDiscoveryEvent::wasDiscovered).map(RxDiscoveryEvent::device);
    }

    /**
     * Returns a {@link Flowable} which emits {@link RxDiscoveryEvent} whenever a device is {@link LifeCycle#DISCOVERED}, {@link LifeCycle#REDISCOVERED},
     * or {@link LifeCycle#UNDISCOVERED}.
     */
    public @HotObservable @Nullable(Nullable.Prevalence.NEVER) Flowable<RxDiscoveryEvent> observeDiscoveryEvents()
    {
        if (m_discoveryFlowable == null)
        {
            m_discoveryFlowable = Flowable.create((FlowableOnSubscribe<DiscoveryEvent>) emitter ->
            {
                if (emitter.isCancelled()) return;

                m_mgr.setListener_Discovery(e ->
                {
                    if (emitter.isCancelled()) return;

                    emitter.onNext(e);
                });

                emitter.setCancellable(() ->
                {
                    m_mgr.setListener_Discovery(null);
                    m_discoveryFlowable = null;
                });
            }, BackpressureStrategy.BUFFER).map(discoveryEvent -> new RxDiscoveryEvent(RxBleManager.this, discoveryEvent)).share();
        }
        return m_discoveryFlowable.share();
    }

    /**
     * Returns a {@link Flowable} which emits {@link com.idevicesinc.sweetblue.ManagerStateListener.StateEvent} when {@link RxBleManager}'s state
     * changes.
     */
    public @HotObservable @Nullable(Nullable.Prevalence.NEVER) Flowable<RxManagerStateEvent> observeMgrStateEvents()
    {
        if (m_mgrStateFlowable == null)
        {
            m_mgrStateFlowable = Flowable.create((FlowableOnSubscribe<StateEvent>) emitter ->
            {
                if (emitter.isCancelled()) return;

                m_mgr.setListener_State(e ->
                {
                    if (emitter.isCancelled()) return;

                    emitter.onNext(e);
                });

                emitter.setCancellable(() ->
                {
                    m_mgr.setListener_State(null);
                    m_mgrStateFlowable = null;
                });
            }, BackpressureStrategy.BUFFER).map(stateEvent -> new RxManagerStateEvent(RxBleManager.this, stateEvent)).share();
        }

        return m_mgrStateFlowable.share();
    }

    /**
     * Returns a {@link Flowable} which emits {@link com.idevicesinc.sweetblue.DeviceStateListener.StateEvent} when any {@link RxBleDevice}'s state
     * changes.
     */
    public @HotObservable @Nullable(Nullable.Prevalence.NEVER) Flowable<RxDeviceStateEvent> observeDeviceStateEvents()
    {
        if (m_deviceStateFlowable == null)
        {
            m_deviceStateFlowable = Flowable.create((FlowableOnSubscribe<DeviceStateListener.StateEvent>) emitter ->
            {
                if (emitter.isCancelled()) return;

                m_mgr.setListener_DeviceState(e ->
                {
                    if (emitter.isCancelled()) return;

                    emitter.onNext(e);
                });

                emitter.setCancellable(() ->
                {
                    m_mgr.setListener_DeviceState(null);
                    m_deviceStateFlowable = null;
                });
            }, BackpressureStrategy.BUFFER).map(RxDeviceStateEvent::new).share();
        }

        return m_deviceStateFlowable.share();
    }

    /**
     * Returns a {@link Flowable} which emits {@link RxDeviceConnectEvent} when any {@link RxBleDevice} gets connected (or fails to).
     */
    public @HotObservable @Nullable(Nullable.Prevalence.NEVER) Flowable<RxDeviceConnectEvent> observeDeviceConnectEvents()
    {
        if (m_deviceConnectFlowable == null)
        {
            m_deviceConnectFlowable = Flowable.create((FlowableOnSubscribe<DeviceConnectListener.ConnectEvent>) emitter ->
            {
                if (emitter.isCancelled()) return;

                m_mgr.setListener_DeviceConnect(e ->
                {
                    if (emitter.isCancelled()) return;

                    emitter.onNext(e);
                });

                emitter.setCancellable(() ->
                {
                    m_mgr.setListener_DeviceConnect(null);
                    m_deviceConnectFlowable = null;
                });

            }, BackpressureStrategy.BUFFER).map(RxDeviceConnectEvent::new).share();
        }

        return m_deviceConnectFlowable.share();
    }

    /**
     * Returns a {@link Flowable} which emits {@link RxUhOhEvent} when any {@link com.idevicesinc.sweetblue.UhOhListener.UhOh}s
     * are posted by the library.
     */
    public @HotObservable @Nullable(Nullable.Prevalence.NEVER) Flowable<RxUhOhEvent> observeUhOhEvents()
    {
        if (m_uhOhFlowable == null)
        {
            m_uhOhFlowable = Flowable.create((FlowableOnSubscribe<UhOhEvent>) emitter ->
            {
                if (emitter.isCancelled()) return;

                m_mgr.setListener_UhOh(e ->
                {
                    if (emitter.isCancelled()) return;

                    emitter.onNext(e);
                });

                emitter.setCancellable(() ->
                {
                    m_mgr.setListener_UhOh(null);
                    m_uhOhFlowable = null;
                });
            }, BackpressureStrategy.BUFFER).map(uhOhEvent -> new RxUhOhEvent(RxBleManager.this, uhOhEvent)).share();
        }

        return m_uhOhFlowable.share();
    }

    /**
     * Returns a {@link Flowable} which emits {@link RxAssertEvent} when any {@link com.idevicesinc.sweetblue.AssertListener.AssertEvent}s
     * are posted by the library.
     */
    public final @HotObservable @Nullable(Nullable.Prevalence.NEVER) Flowable<RxAssertEvent> observeAssertEvents()
    {
        if (m_assertFlowable == null)
        {
            m_assertFlowable = Flowable.create((FlowableOnSubscribe<AssertListener.AssertEvent>) emitter ->
            {
                if (emitter.isCancelled()) return;

                m_mgr.setListener_Assert(e ->
                {
                    if (emitter.isCancelled()) return;

                    emitter.onNext(e);
                });

                emitter.setCancellable(() ->
                {
                    m_mgr.setListener_Assert(null);
                    m_assertFlowable = null;
                });
            }, BackpressureStrategy.BUFFER).map(assertEvent -> new RxAssertEvent(RxBleManager.this, assertEvent)).share();
        }

        return m_assertFlowable.share();
    }

    /**
     * Returns a {@link Flowable} which emits {@link RxServerStateEvent} when any {@link com.idevicesinc.sweetblue.ServerStateListener.StateEvent}s
     * are posted by the library.
     */
    public final @HotObservable @Nullable(Nullable.Prevalence.NEVER) Flowable<RxServerStateEvent> observeServerStateEvents()
    {
        if (m_serverStateFlowable == null)
        {
            m_serverStateFlowable = Flowable.create((FlowableOnSubscribe<ServerStateListener.StateEvent>) emitter ->
            {
                if (emitter.isCancelled()) return;

                m_mgr.setListener_ServerState(e ->
                {
                    if (emitter.isCancelled()) return;

                    emitter.onNext(e);
                });

                emitter.setCancellable(() ->
                {
                    m_mgr.setListener_ServerState(null);
                    m_serverStateFlowable = null;
                });
            }, BackpressureStrategy.BUFFER).map(RxServerStateEvent::new).share();
        }

        return m_serverStateFlowable.share();
    }

    /**
     * Returns a {@link Flowable} which emits {@link RxBondEvent} when any {@link com.idevicesinc.sweetblue.BondListener.BondEvent}s
     * for all {@link RxBleDevice}s are posted by the library.
     */
    public final @HotObservable @Nullable(Nullable.Prevalence.NEVER) Flowable<RxBondEvent> observeBondEvents()
    {
        if (m_bondFlowable == null)
        {
            m_bondFlowable = Flowable.create((FlowableOnSubscribe<BondListener.BondEvent>) emitter ->
            {
                if (emitter.isCancelled()) return;

                m_mgr.setListener_Bond(e ->
                {
                    if (emitter.isCancelled()) return;

                    emitter.onNext(e);
                });

                emitter.setCancellable(() ->
                {
                    m_mgr.setListener_Bond(null);
                    m_bondFlowable = null;
                });
            }, BackpressureStrategy.BUFFER).map(RxBondEvent::new).share();
        }

        return m_bondFlowable.share();
    }

    /**
     * Returns a {@link Flowable} which emits {@link RxReadWriteEvent} when any {@link com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent}s
     * for all {@link RxBleDevice}s are posted by the library.
     */
    public final @HotObservable @Nullable(Nullable.Prevalence.NEVER) Flowable<RxReadWriteEvent> observeReadWriteEvents()
    {
        if (m_readWriteFlowable == null)
        {
            m_readWriteFlowable = Flowable.create((FlowableOnSubscribe<ReadWriteListener.ReadWriteEvent>) emitter ->
            {
                if (emitter.isCancelled()) return;

                m_mgr.setListener_Read_Write(e ->
                {
                    if (emitter.isCancelled()) return;

                    emitter.onNext(e);
                });

                emitter.setCancellable(() ->
                {
                    m_mgr.setListener_Read_Write(null);
                    m_readWriteFlowable = null;
                });
            }, BackpressureStrategy.BUFFER).map(RxReadWriteEvent::new).share();
        }

        return m_readWriteFlowable.share();
    }

    /**
     * Returns a {@link Flowable} which emits {@link RxNotificationEvent} when any {@link com.idevicesinc.sweetblue.NotificationListener.NotificationEvent}s
     * for all {@link RxBleDevice}s are posted by the library.
     */
    public final @HotObservable @Nullable(Nullable.Prevalence.NEVER) Flowable<RxNotificationEvent> observeNotificationEvents()
    {
        if (m_notifyEventFlowable == null)
        {
            m_notifyEventFlowable = Flowable.create((FlowableOnSubscribe<NotificationListener.NotificationEvent>) emitter ->
            {
                if (emitter.isCancelled()) return;

                m_mgr.setListener_Notification(e ->
                {
                    if (emitter.isCancelled()) return;

                    emitter.onNext(e);
                });

                emitter.setCancellable(() ->
                {
                    m_mgr.setListener_Notification(null);
                    m_notifyEventFlowable = null;
                });
            }, BackpressureStrategy.BUFFER).map(RxNotificationEvent::new).share();
        }

        return m_notifyEventFlowable.share();
    }

    /**
     * Returns a {@link Flowable} which emits {@link RxHistoricalDataLoadEvent} when any {@link com.idevicesinc.sweetblue.HistoricalDataLoadListener.HistoricalDataLoadEvent}s
     * for all {@link RxBleDevice}s are posted by the library.
     */
    public final @HotObservable @Nullable(Nullable.Prevalence.NEVER) Flowable<RxHistoricalDataLoadEvent> observeHistoricalDataLoadEvents()
    {
        if (m_historicalDataLoadFlowable == null)
        {
            m_historicalDataLoadFlowable = Flowable.create((FlowableOnSubscribe<HistoricalDataLoadListener.HistoricalDataLoadEvent>) emitter ->
            {
                if (emitter.isCancelled()) return;

                m_mgr.setListener_HistoricalDataLoad(e ->
                {
                    if (emitter.isCancelled()) return;

                    emitter.onNext(e);
                });

                emitter.setCancellable(() ->
                {
                    m_mgr.setListener_HistoricalDataLoad(null);
                    m_historicalDataLoadFlowable = null;
                });
            }, BackpressureStrategy.BUFFER).map(RxHistoricalDataLoadEvent::new).share();
        }

        return m_historicalDataLoadFlowable.share();
    }

    /**
     * Returns a {@link Flowable} which emits {@link RxOutgoingEvent} when any {@link com.idevicesinc.sweetblue.OutgoingListener.OutgoingEvent}s
     * are posted by the library.
     */
    public final @HotObservable @Nullable(Nullable.Prevalence.NEVER) Flowable<RxOutgoingEvent> observeOutgoingEvents()
    {
        if (m_outgoingEventFlowable == null)
        {
            m_outgoingEventFlowable = Flowable.create((FlowableOnSubscribe<OutgoingListener.OutgoingEvent>) emitter ->
            {
                if (emitter.isCancelled()) return;

                m_mgr.setListener_Outgoing(e ->
                {
                    if (emitter.isCancelled()) return;

                    emitter.onNext(e);
                });

                emitter.setCancellable(() ->
                {
                    m_mgr.setListener_Outgoing(null);
                    m_outgoingEventFlowable = null;
                });
            }, BackpressureStrategy.BUFFER).map(RxOutgoingEvent::new).share();
        }

        return m_outgoingEventFlowable.share();
    }

    /**
     * Returns a {@link Flowable} which emits {@link RxServiceAddEvent} when any {@link com.idevicesinc.sweetblue.AddServiceListener.ServiceAddEvent}s
     * are posted by the library.
     */
    public final @HotObservable @Nullable(Nullable.Prevalence.NEVER) Flowable<RxServiceAddEvent> observeServiceAddEvents()
    {
        if (m_serviceAddEventFlowable == null)
        {
            m_serviceAddEventFlowable = Flowable.create((FlowableOnSubscribe<AddServiceListener.ServiceAddEvent>) emitter ->
            {
                if (emitter.isCancelled()) return;

                m_mgr.setListener_ServiceAdd(e ->
                {
                    if (emitter.isCancelled()) return;

                    emitter.onNext(e);
                });

                emitter.setCancellable(() ->
                {
                    m_mgr.setListener_ServiceAdd(null);
                    m_serviceAddEventFlowable = null;
                });
            }, BackpressureStrategy.BUFFER).map(RxServiceAddEvent::new).share();
        }

        return m_serviceAddEventFlowable.share();
    }

    /**
     * Returns a {@link Flowable} which emits {@link RxAdvertisingEvent} when any {@link com.idevicesinc.sweetblue.AdvertisingListener.AdvertisingEvent}s
     * are posted by the library.
     */
    public final @HotObservable @Nullable(Nullable.Prevalence.NEVER) Flowable<RxAdvertisingEvent> observeAdvertisingEvents()
    {
        if (m_advertisingEventFlowable == null)
        {
            m_advertisingEventFlowable = Flowable.create((FlowableOnSubscribe<AdvertisingListener.AdvertisingEvent>) emitter ->
            {
                if (emitter.isCancelled()) return;

                m_mgr.setListener_Advertising(e ->
                {
                    if (emitter.isCancelled()) return;

                    emitter.onNext(e);
                });

                emitter.setCancellable(() ->
                {
                    m_mgr.setListener_Advertising(null);
                    m_advertisingEventFlowable = null;
                });
            }, BackpressureStrategy.BUFFER).map(RxAdvertisingEvent::new).share();
        }

        return m_advertisingEventFlowable.share();
    }

    /**
     * Rx-ified version of {@link BleManager#getDevice(BleDeviceState)}.
     * <p>
     * Returns an instance of {@link RxBleDevice}.
     */
    public @Nullable(Nullable.Prevalence.NEVER) RxBleDevice getDevice(@Nullable(Nullable.Prevalence.NEVER) BleDeviceState state)
    {
        return Single.create((SingleEmitter<BleDevice> emitter) ->
        {
            if (emitter.isDisposed()) return;

            emitter.onSuccess(m_mgr.getDevice(state));
        }).subscribeOn(SweetBlueSchedulers.sweetBlueThread()).map(BLE_DEVICE_TRANSFORMER).blockingGet();
    }

    /**
     * Rx-ified version of {@link BleManager#getDevice(String)}.
     * <p>
     * Returns an instance of {@link RxBleDevice}.
     */
    public @Nullable(Nullable.Prevalence.NEVER) RxBleDevice getDevice(@Nullable(Nullable.Prevalence.NEVER) String macAddress)
    {
        return Single.create((SingleEmitter<BleDevice> emitter) ->
        {
            if (emitter.isDisposed()) return;

            emitter.onSuccess(m_mgr.getDevice(macAddress));
        }).subscribeOn(SweetBlueSchedulers.sweetBlueThread()).map(BLE_DEVICE_TRANSFORMER).blockingGet();
    }

    /**
     * Rx-ified version of {@link BleManager#getDevice(Object...)}.
     * <p>
     * Returns an instance of {@link RxBleDevice}.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) BleDevice getDevice(@Nullable(Nullable.Prevalence.NEVER) Object... query)
    {
        return Single.create((SingleEmitter<BleDevice> emitter) ->
        {
            if (emitter.isDisposed()) return;

            emitter.onSuccess(m_mgr.getDevice(query));
        }).subscribeOn(SweetBlueSchedulers.sweetBlueThread()).blockingGet();
    }

    /**
     * Rx-ified version of {@link BleManager#getDevice(int)}.
     * <p>
     * Returns an instance of {@link RxBleDevice}.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) BleDevice getDevice(final int mask_BleDeviceState)
    {
        return Single.create((SingleEmitter<BleDevice> emitter) ->
        {
            if (emitter.isDisposed()) return;

            emitter.onSuccess(m_mgr.getDevice(mask_BleDeviceState));
        }).subscribeOn(SweetBlueSchedulers.sweetBlueThread()).blockingGet();
    }

    /**
     * Shortcut for checking if {@link #getDevice(String)} returns {@link BleDevice#NULL}.
     */
    public final boolean hasDevice(final String macAddress)
    {
        return m_mgr.hasDevice(macAddress);
    }

    /**
     * Calls {@link #hasDevice(String)}.
     */
    public final boolean hasDevice(final BleDevice device)
    {
        return m_mgr.hasDevice(device);
    }

    /**
     * Returns true if we have a device in the given state.
     */
    public final boolean hasDevice(BleDeviceState state)
    {
        return m_mgr.hasDevice(state);
    }

    /**
     * Returns true if we have a device that matches the given query.
     * See {@link BleDevice#is(Object...)} for the query format.
     */
    public final boolean hasDevice(Object... query)
    {
        return m_mgr.hasDevice(query);
    }

    /**
     * Returns <code>true</code> if there is any {@link BleDevice} for which {@link BleDevice#isAny(int)} with the given mask returns <code>true</code>.
     */
    public final boolean hasDevice(final int mask_BleDeviceState)
    {
        return m_mgr.hasDevice(mask_BleDeviceState);
    }

    /**
     * Rx-ified version of {@link BleManager#getDevices_bonded()}.
     * <p>
     * Returns an {@link Observable} to cycle through all the devices returned. They will all be instances of {@link RxBleDevice}.
     */
    public @Nullable(Nullable.Prevalence.NEVER) Observable<RxBleDevice> getDevices_bonded()
    {
        return Observable.create((ObservableEmitter<BleDevice> emitter) ->
        {
            if (emitter.isDisposed()) return;

            Set<BleDevice> devices = m_mgr.getDevices_bonded();

            for (BleDevice device : devices)
            {
                if (emitter.isDisposed()) return;

                emitter.onNext(device);
            }

            emitter.onComplete();
        }).subscribeOn(SweetBlueSchedulers.sweetBlueThread()).map(BLE_DEVICE_TRANSFORMER);
    }

    /**
     * Returns an {@link Observable} that emits all {@link RxBleDevice}s managed by the library.
     */
    public @Nullable(Nullable.Prevalence.NEVER) Observable<RxBleDevice> getDevices()
    {
        return Observable.create((ObservableOnSubscribe<BleDevice>) emitter ->
        {
            if (emitter.isDisposed()) return;

            List<BleDevice> deviceList = m_mgr.getDevices_List();

            for (BleDevice d : deviceList)
            {
                if (emitter.isDisposed()) return;

                emitter.onNext(d);
            }

            emitter.onComplete();
        }).subscribeOn(SweetBlueSchedulers.sweetBlueThread()).map(BLE_DEVICE_TRANSFORMER);
    }

    /**
     * Returns an {@link Observable} that emits all {@link RxBleDevice}s managed by the library. Devices are emitted in a sorted
     * order, which is defined by {@link BleManagerConfig#defaultListComparator}.
     */
    public @Nullable(Nullable.Prevalence.NEVER) Observable<RxBleDevice> getDevices_sorted()
    {
        return Observable.create((ObservableOnSubscribe<BleDevice>) emitter ->
        {
            if (emitter.isDisposed()) return;

            List<BleDevice> deviceList = m_mgr.getDevices_List_sorted();

            for (BleDevice d : deviceList)
            {
                if (emitter.isDisposed()) return;

                emitter.onNext(d);
            }

            emitter.onComplete();
        }).subscribeOn(SweetBlueSchedulers.sweetBlueThread()).map(BLE_DEVICE_TRANSFORMER);
    }

    /**
     * Returns an {@link Observable} which emits the macAddresses of all devices that we know about from both
     * current and previous app sessions.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) Observable<String> getDevices_previouslyConnected()
    {
        return Observable.create((ObservableOnSubscribe<String>) emitter ->
        {
            if (emitter.isDisposed()) return;

            final Iterator<String> it = m_mgr.getDevices_previouslyConnected();

            while (it.hasNext())
            {
                if (emitter.isDisposed()) return;

                emitter.onNext(it.next());
            }

            emitter.onComplete();
        }).subscribeOn(SweetBlueSchedulers.sweetBlueThread());
    }

    /**
     * Rx-ified version of {@link BleManager#newDevice(String)}.
     */
    public @Nullable(Nullable.Prevalence.NEVER) RxBleDevice newDevice(@Nullable(Nullable.Prevalence.NEVER) final String macAddress)
    {
        return newDevice(macAddress, null, null);
    }

    /**
     * Rx-ified version of {@link BleManager#newDevice(String, String)}.
     */
    public @Nullable(Nullable.Prevalence.NEVER) RxBleDevice newDevice(@Nullable(Nullable.Prevalence.NEVER) final String macAddress, final String name)
    {
        return newDevice(macAddress, name, null);
    }

    /**
     * Rx-ified version of {@link BleManager#newDevice(String, BleDeviceConfig)}.
     */
    public @Nullable(Nullable.Prevalence.NEVER) RxBleDevice newDevice(@Nullable(Nullable.Prevalence.NEVER) final String macAddress, final BleManagerConfig config)
    {
        return newDevice(macAddress, null, config);
    }

    /**
     * Rx-ified version of {@link BleManager#newDevice(String, String, BleDeviceConfig)}.
     */
    public @Nullable(Nullable.Prevalence.NEVER) RxBleDevice newDevice(@Nullable(Nullable.Prevalence.NEVER) final String macAddress, final String name, final BleDeviceConfig config)
    {
        return Single.create((SingleEmitter<BleDevice> emitter) ->
        {
            if (emitter.isDisposed()) return;

            emitter.onSuccess(m_mgr.newDevice(macAddress, name, config));
        }).subscribeOn(SweetBlueSchedulers.sweetBlueThread()).map(BLE_DEVICE_TRANSFORMER).blockingGet();
    }

    /**
     * Returns the total number of devices this manager is...managing.
     * This includes all devices that are {@link BleDeviceState#DISCOVERED}.
     */
    public final int getDeviceCount()
    {
        return m_mgr.getDeviceCount();
    }

    /**
     * Returns the number of devices that are in the current state.
     */
    public final int getDeviceCount(BleDeviceState state)
    {
        return m_mgr.getDeviceCount(state);
    }

    /**
     * Returns the number of devices that match the given query.
     * See {@link BleDevice#is(Object...)} for the query format.
     */
    public final int getDeviceCount(Object... query)
    {
        return m_mgr.getDeviceCount(query);
    }

    /**
     * Returns whether we have any devices. For example if you have never called {@link #scan(ScanOptions)}
     * or {@link #newDevice(String)} (or overloads) then this will return false.
     */
    public final boolean hasDevices()
    {
        return m_mgr.hasDevices();
    }

    /**
     * Removes the given {@link RxBleDevice} from SweetBlue's internal device cache list. You should never have to call this
     * yourself (and probably shouldn't), but it's here for flexibility.
     */
    @Advanced
    public final void removeDeviceFromCache(RxBleDevice device)
    {
        m_mgr.removeDeviceFromCache(device.getBleDevice());
    }

    /**
     * Removes all {@link RxBleDevice}s from SweetBlue's internal device cache list. You should never have to call this
     * yourself (and probably shouldn't), but it's here for flexibility.
     */
    @Advanced
    public final void removeAllDevicesFromCache()
    {
        m_mgr.removeAllDevicesFromCache();
    }

    /**
     * Returns a new {@link HistoricalData} instance using
     * {@link BleDeviceConfig#historicalDataFactory} if available.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) HistoricalData newHistoricalData(final byte[] data, final EpochTime epochTime)
    {
        return m_mgr.newHistoricalData(data, epochTime);
    }

    /**
     * Same as {@link #newHistoricalData(byte[], EpochTime)} but tries to use
     * {@link BleDevice#newHistoricalData(byte[], EpochTime)} if we have a device
     * matching the given mac address.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) HistoricalData newHistoricalData(final byte[] data, final EpochTime epochTime, final String macAddress)
    {
        return m_mgr.newHistoricalData(data, epochTime, macAddress);
    }

    /**
     * Rx-ified version of {@link BleManager#getServer(IncomingListener, GattDatabase, AddServiceListener)}.
     * <p>
     * NOTE: The device creation is performed on the thread which SweetBlue is using, and this method is blocking.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) RxBleServer getServer(final IncomingListener listener, final GattDatabase db, final AddServiceListener addListener)
    {
        return Single.create((SingleOnSubscribe<BleServer>) emitter ->
        {
            if (emitter.isDisposed()) return;

            emitter.onSuccess(m_mgr.getServer(listener, db, addListener));
        }).map(BLE_SERVER_TRANSFORMER).subscribeOn(SweetBlueSchedulers.sweetBlueThread()).blockingGet();
    }

    /**
     * Same as {@link #getServer(IncomingListener, GattDatabase, AddServiceListener)}, which passes null for all arguments.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) RxBleServer getServer()
    {
        return getServer(null, null, null);
    }

    /**
     * Forcefully undiscovers a device, disconnecting it first if needed and removing it from this manager's internal list.
     * {@link DiscoveryListener#onEvent(Event)} with {@link LifeCycle#UNDISCOVERED} will be called.
     * No clear use case has been thought of but the method is here just in case anyway.
     *
     * @return <code>true</code> if the device was undiscovered, <code>false</code> if device is already {@link BleDeviceState#UNDISCOVERED} or manager
     * doesn't contain an instance, checked referentially, not through {@link RxBleDevice#equals(RxBleDevice)} (i.e. by mac address).
     */
    public final boolean undiscover(final RxBleDevice device)
    {
        return m_mgr.undiscover(device.getBleDevice());
    }

    /**
     * This method will clear the task queue of all tasks.
     * NOTE: This can really mess things up, especially if you're currently trying to connect to a device. Only use this if you absolutely have to!
     */
    @Advanced
    public final void clearQueue()
    {
        m_mgr.clearQueue();
    }

    /**
     * Convenience forwarding of {@link #clearSharedPreferences(String)}.
     *
     * @see #clearSharedPreferences(String)
     */
    public final void clearSharedPreferences(final RxBleDevice device)
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
        m_mgr.clearSharedPreferences(macAddress);
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
        m_mgr.clearSharedPreferences();
    }

    /**
     * Returns this manager's knowledge of the app's foreground state.
     */
    public final boolean isForegrounded()
    {
        return m_mgr.isForegrounded();
    }

    @Override
    public final String toString()
    {
        return m_mgr.toString();
    }

    /**
     * Disconnects all devices, shuts down the BleManager, and it's backing thread, and unregisters any receivers that may be in use.
     * This also clears out the {@link BleManager}, and {@link RxBleManager} static instances. This is meant to be called upon application exit. However, to use it again,
     * just call {@link BleManager#get(Context)}, or {@link BleManager#get(Context, BleManagerConfig)} again.
     */
    public final void shutdown()
    {
        cleanUpFlowables();
        RxSweetBluePlugins.reset();
        m_mgr.shutdown();
        m_deviceMap.clear();
        s_instance = null;
    }

    private void cleanUpFlowables()
    {
        m_uhOhFlowable = null;
        m_deviceStateFlowable = null;
        m_deviceConnectFlowable = null;
        m_mgrStateFlowable = null;
        m_assertFlowable = null;
        m_serverStateFlowable = null;
        m_bondFlowable = null;
        m_readWriteFlowable = null;
        m_notifyEventFlowable = null;
        m_historicalDataLoadFlowable = null;
        m_discoveryFlowable = null;
        m_outgoingEventFlowable = null;
        m_serviceAddEventFlowable = null;
        m_advertisingEventFlowable = null;
    }

    public final void setConfig(RxBleManagerConfig config)
    {
        if (config != null && (config.defaultAuthFactory != null || config.defaultInitFactory != null))
            throw new RuntimeException("Please do not set defaultAuthFactory, or defaultInitFactory! Use defaultRxAuthFactory, or defaultRxInitFactory instead.");

        m_config = config;

        if (config != null)
        {
            if (config.defaultRxAuthFactory != null)
                config.defaultAuthFactory = () -> config.defaultRxAuthFactory.newAuthTxn().getWrappedTxn();

            if (config.defaultRxInitFactory != null)
                config.defaultInitFactory = () -> config.defaultRxInitFactory.newInitTxn().getWrappedTxn();
        }
        m_mgr.setConfig(m_config);
    }

    public final @Nullable(Nullable.Prevalence.NEVER) RxBleManagerConfig getConfigClone()
    {
        return m_config.clone();
    }

    /**
     * Returns whether the manager is in any of the provided states.
     */
    public final boolean isAny(BleManagerState... states)
    {
        return m_mgr.isAny(states);
    }

    /**
     * Returns whether the manager is in all of the provided states.
     *
     * @see #isAny(BleManagerState...)
     */
    public final boolean isAll(BleManagerState... states)
    {
        return m_mgr.isAll(states);
    }

    /**
     * Returns whether the manager is in the provided state.
     *
     * @see #isAny(BleManagerState...)
     */
    public final boolean is(final BleManagerState state)
    {
        return m_mgr.is(state);
    }

    /**
     * Returns <code>true</code> if there is partial bitwise overlap between the provided value and {@link #getStateMask()}.
     *
     * @see #isAll(int)
     */
    public final boolean isAny(final int mask_BleManagerState)
    {
        return m_mgr.isAny(mask_BleManagerState);
    }

    /**
     * Returns <code>true</code> if there is complete bitwise overlap between the provided value and {@link #getStateMask()}.
     *
     * @see #isAny(int)
     */
    public final boolean isAll(final int mask_BleManagerState)
    {
        return m_mgr.isAll(mask_BleManagerState);
    }

    /**
     * See similar comment for {@link RxBleDevice#getTimeInState(BleDeviceState)}.
     *
     * @see RxBleDevice#getTimeInState(BleDeviceState)
     */
    public final @Nullable(Nullable.Prevalence.NEVER) Interval getTimeInState(BleManagerState state)
    {
        return m_mgr.getTimeInState(state);
    }

    /**
     * Checks the underlying stack to see if BLE is supported on the phone.
     */
    public final boolean isBleSupported()
    {
        return m_mgr.isBleSupported();
    }

    /**
     * Checks to see if the device is running an Android OS which supports
     * advertising.
     */
    public final boolean isAdvertisingSupportedByAndroidVersion()
    {
        return m_mgr.isAdvertisingSupportedByAndroidVersion();
    }

    /**
     * Checks to see if the device supports advertising.
     */
    public final boolean isAdvertisingSupportedByChipset()
    {
        return m_mgr.isAdvertisingSupportedByChipset();
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
        return m_mgr.isAdvertisingSupportedByAndroidVersion();
    }

    /**
     * Returns <code>true</code> if the android device supports the bluetooth 5 feature of long range (up to 4x the range of Bluetooth 4.x).
     * <p>
     * It's possible for this to return <code>true</code>, and {@link #isBluetooth5HighSpeedSupported()} to return <code>false</code>.
     */
    public final boolean isBluetooth5LongRangeSupported()
    {
        return m_mgr.isBluetooth5LongRangeSupported();
    }

    /**
     * Returns <code>true</code> if the android device supports the high speed feature of Bluetooth 5 (2x the speed of Bluetooth 4.x).
     */
    public final boolean isBluetooth5HighSpeedSupported()
    {
        return m_mgr.isBluetooth5HighSpeedSupported();
    }

    /**
     * Convenience method to check if the android device supports bluetooth 5 in any way. This just calls {@link #isBluetooth5SupportedByAndroidVersion()},
     * {@link #isBluetooth5HighSpeedSupported()} and {@link #isBluetooth5LongRangeSupported()}.
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
        m_mgr.turnOff();
    }

    /**
     * Returns the native manager.
     */
    @Advanced
    public final @Nullable(Nullable.Prevalence.NORMAL) BluetoothManager getNative()
    {
        return m_mgr.getNative();
    }

    /**
     * Returns the native bluetooth adapter.
     */
    @Advanced
    public final @Nullable(Nullable.Prevalence.NORMAL) BluetoothAdapter getNativeAdapter()
    {
        return m_mgr.getNativeAdapter();
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
        m_mgr.pushWakeLock();
    }

    /**
     * Opposite of {@link #pushWakeLock()}, eventually calls {@link android.os.PowerManager.WakeLock#release()}.
     */
    @Advanced
    public final void popWakeLock()
    {
        m_mgr.popWakeLock();
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
        return m_mgr.ASSERT(condition, message);
    }

    /**
     * Returns the abstracted bitwise state mask representation of {@link BleManagerState} for the manager instance.
     *
     * @see BleManagerState
     */
    public final int getStateMask()
    {
        return m_mgr.getStateMask();
    }

    /**
     * Enables BLE if manager is currently {@link BleManagerState#OFF} or {@link BleManagerState#TURNING_OFF}, otherwise does nothing.
     * For a convenient way to ask your user first see {@link #turnOnWithIntent(android.app.Activity, int)}.
     */
    public final void turnOn()
    {
        m_mgr.turnOn();
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
        m_mgr.reset(listener);
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
        m_mgr.nukeBle(resetListener);
    }

    /**
     * Removes bonds for all devices that are {@link BleDeviceState#BONDED}.
     * Essentially a convenience method for calling {@link RxBleDevice#unbond()},
     * on each device individually.
     */
    public final void unbondAll()
    {
        m_mgr.unbondAll();
    }

    /**
     * Disconnects all devices that are {@link BleDeviceState#BLE_CONNECTED}.
     * Essentially a convenience method for calling {@link RxBleDevice#disconnect()},
     * on each device individually.
     */
    public final void disconnectAll()
    {
        m_mgr.disconnectAll();
    }

    /**
     * Same as {@link #disconnectAll()} but drills down to {@link RxBleDevice#disconnect_remote()} instead.
     */
    public final void disconnectAll_remote()
    {
        m_mgr.disconnectAll_remote();
    }

    /**
     * Undiscovers all devices that are {@link BleDeviceState#DISCOVERED}.
     * Essentially a convenience method for calling {@link RxBleDevice#undiscover()},
     * on each device individually.
     */
    public final void undiscoverAll()
    {
        m_mgr.undiscoverAll();
    }

    /**
     * If {@link #isLocationEnabledForScanning_byOsServices()} returns <code>false</code>, you can use this method to allow the user to enable location services.
     * <br><br>
     * NOTE: If {@link #isLocationEnabledForScanning_byOsServices()} returns <code>false</code> but all other overloads of {@link #isLocationEnabledForScanning()} return <code>true</code> then
     * SweetBlue will fall back to classic discovery through {@link BluetoothAdapter#startDiscovery()} when you call {@link #scan(ScanOptions)}, so you may not have to use this.
     *
     * @see #isLocationEnabledForScanning_byOsServices()
     * @see com.idevicesinc.sweetblue.utils.BluetoothEnabler
     */
    public final void turnOnLocationWithIntent_forOsServices(final Activity callingActivity, int requestCode)
    {
        m_mgr.turnOnLocationWithIntent_forOsServices(callingActivity, requestCode);
    }

    /**
     * Overload of {@link #turnOnLocationWithIntent_forOsServices(Activity, int)} if you don't care about result.
     *
     * @see com.idevicesinc.sweetblue.utils.BluetoothEnabler
     */
    public final void turnOnLocationWithIntent_forOsServices(final Activity callingActivity)
    {
        m_mgr.turnOnLocationWithIntent_forOsServices(callingActivity);
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
        return m_mgr.willLocationPermissionSystemDialogBeShown(callingActivity);
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
        m_mgr.turnOnLocationWithIntent_forPermissions(callingActivity, requestCode);
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
        m_mgr.requestBluetoothPermissions(callingActivity, requestCode);
    }

    /**
     * Tells you whether a call to {@link #scan(ScanOptions)}, will succeed or not. Basically a convenience for checking if both
     * {@link #isLocationEnabledForScanning()} and {@link #is(BleManagerState)} with {@link BleManagerState#SCANNING} return <code>true</code>.
     */
    public final boolean isScanningReady()
    {
        return m_mgr.isScanningReady();
    }

    /**
     * Convenience method which reports <code>true</code> if the {@link BleManager} is in any of the following states: <br><br>
     * {@link BleManagerState#SCANNING}, {@link BleManagerState#SCANNING_PAUSED}, {@link BleManagerState#BOOST_SCANNING}, or {@link BleManagerState#STARTING_SCAN}
     */
    public final boolean isScanning()
    {
        return m_mgr.isScanning();
    }

    /**
     * Returns <code>true</code> if location is enabled to a degree that allows scanning on {@link android.os.Build.VERSION_CODES#M} and above.
     * If this returns <code>false</code> it means you're on Android M and you either (A) do not have {@link android.Manifest.permission#ACCESS_COARSE_LOCATION}
     * (or {@link android.Manifest.permission#ACCESS_FINE_LOCATION} in your AndroidManifest.xml, see {@link #isLocationEnabledForScanning_byManifestPermissions()}), or (B)
     * runtime permissions for aformentioned location permissions are off (see {@link #isLocationEnabledForScanning_byRuntimePermissions()} and
     * https://developer.android.com/training/permissions/index.html), or (C) location services on the phone are disabled (see {@link #isLocationEnabledForScanning_byOsServices()}).
     * <br><br>
     * If this returns <code>true</code> then you are good to go for calling {@link #scan(ScanOptions)}.
     *
     * @see #scan(ScanOptions)
     * @see #turnOnLocationWithIntent_forPermissions(Activity, int)
     * @see #turnOnLocationWithIntent_forOsServices(Activity)
     * @see #turnOnLocationWithIntent_forOsServices(Activity, int)
     * @see com.idevicesinc.sweetblue.utils.BluetoothEnabler
     */
    public final boolean isLocationEnabledForScanning()
    {
        return m_mgr.isLocationEnabledForScanning();
    }

    /**
     * Returns <code>true</code> if you're either pre-Android-M, or app has permission for either {@link android.Manifest.permission#ACCESS_COARSE_LOCATION}
     * or {@link android.Manifest.permission#ACCESS_FINE_LOCATION} in your AndroidManifest.xml, <code>false</code> otherwise.
     *
     * @see #scan(ScanOptions)
     * @see com.idevicesinc.sweetblue.utils.BluetoothEnabler
     */
    public final boolean isLocationEnabledForScanning_byManifestPermissions()
    {
        return m_mgr.isLocationEnabledForScanning_byManifestPermissions();
    }

    /**
     * Returns <code>true</code> if you're either pre-Android-M, or app has runtime permissions enabled by checking
     * <a href="https://developer.android.com/reference/android/support/v4/content/ContextCompat#checkSelfPermission(android.content.Context,%20java.lang.String)">ContextCompat.checkSelfPermission(android.content.Context, java.lang.String)</a>
     * See more information at https://developer.android.com/training/permissions/index.html.
     *
     * @see #scan(ScanOptions)
     * @see #turnOnLocationWithIntent_forPermissions(Activity, int)
     * @see com.idevicesinc.sweetblue.utils.BluetoothEnabler
     */
    public final boolean isLocationEnabledForScanning_byRuntimePermissions()
    {
        return m_mgr.isLocationEnabledForScanning_byRuntimePermissions();
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
        return m_mgr.areBluetoothPermissionsEnabled();
    }

    /**
     * Returns <code>true</code> if you're either pre-Android-M, or location services are enabled, the same is if you go to the Android Settings app
     * and manually toggle Location ON/OFF.
     * <br><br>
     * NOTE: If this returns <code>false</code> but all other overloads of {@link #isLocationEnabledForScanning()} return <code>true</code> then
     * SweetBlue will fall back to classic discovery through {@link BluetoothAdapter#startDiscovery()} when you call {@link #scan(ScanOptions)}.
     *
     * @see #scan(ScanOptions)
     * @see #turnOnLocationWithIntent_forOsServices(Activity)
     * @see #turnOnLocationWithIntent_forOsServices(Activity, int)
     * @see com.idevicesinc.sweetblue.utils.BluetoothEnabler
     */
    public final boolean isLocationEnabledForScanning_byOsServices()
    {
        return m_mgr.isLocationEnabledForScanning_byOsServices();
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
        m_mgr.turnOnWithIntent(callingActivity, requestCode);
    }

    /**
     * Opposite of {@link #onPause()}, to be called from your override of {@link android.app.Activity#onResume()} for each {@link android.app.Activity}
     * in your application. See comment for {@link #onPause()} for a similar explanation for why you should call this method.
     */
    public final void onResume()
    {
        m_mgr.onResume();
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
        m_mgr.onPause();
    }

    /**
     * Returns the {@link android.app.Application} provided to the constructor.
     */
    public final @Nullable(Nullable.Prevalence.RARE) Context getApplicationContext()
    {
        return m_mgr.getApplicationContext();
    }

    /**
     * @deprecated use {@link #stopScan()} or {@link #stopScan(ScanFilter)} instead.
     * This method will be removed in v3.1.
     *
     * Convenience that will call both {@link BleManager#stopPeriodicScan()} and {@link BleManager#stopScan()} for you. This is here as a stop-gap
     * in the off chance the scan doesn't stop when you dispose the {@link Flowable} returned from {@link #scan(ScanOptions)}.
     */
    @Deprecated
    public final void stopAllScanning()
    {
        m_mgr.stopScan();
    }

    /**
     * Stops any scans previously started by {@link #scan()}, {@link #scan(ScanOptions)}, or {@link #scan_onlyNew(ScanOptions)}.
     */
    public final void stopScan()
    {
        m_mgr.stopScan();
    }

    /**
     * Same as {@link #stopScan()} but also unregisters any filter supplied to various overloads of {@link #scan()}.
     *
     * Calling {@link #stopScan()} alone will keep any previously registered filters active.
     */
    public final void stopScan(ScanFilter filter)
    {
        m_mgr.stopScan(filter);
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
        m_mgr.stopScan(pendingIntent);
    }

    private void checkDiscoveryFlowable(ScanOptions scanoptions)
    {
        final Pointer<ScanOptions> options = new Pointer<>(scanoptions == null ? new ScanOptions() : scanoptions);
        if (m_discoveryFlowable == null)
        {
            m_discoveryFlowable = Flowable.create(new FlowableOnSubscribe<DiscoveryEvent>()
            {
                private Disposable stateDisposable;

                @Override
                public void subscribe(final FlowableEmitter<DiscoveryEvent> emitter) throws Exception
                {
                    if (emitter.isCancelled()) return;

                    m_mgr.setListener_Discovery(e ->
                    {
                        if (emitter.isCancelled()) return;

                        emitter.onNext(e);
                    });

                    if (!options.value.isContinuous())
                    {
                        stateDisposable = observeMgrStateEvents().subscribe(stateEvent ->
                        {
                            if (stateEvent.didExit(BleManagerState.SCANNING) && !m_mgr.isScanning())
                            {
                                emitter.onComplete();
                            }
                        });
                    }

                    emitter.setCancellable(() ->
                    {
                        m_mgr.setListener_Discovery(null);
                        m_discoveryFlowable = null;
                        // Clean up the state disposable, if we have one, and it's not already disposed
                        if (stateDisposable != null && !stateDisposable.isDisposed())
                            stateDisposable.dispose();

                        // Stop all scanning
                        m_mgr.stopScan();
                    });
                }
            }, BackpressureStrategy.BUFFER).map(discoveryEvent -> new RxDiscoveryEvent(RxBleManager.this, discoveryEvent)).share();
        }
    }


    static com.idevicesinc.sweetblue.rx.RxBleDevice getOrCreateDevice(@Nullable(Nullable.Prevalence.NEVER) BleDevice device)
    {
        com.idevicesinc.sweetblue.rx.RxBleDevice rxDevice = m_deviceMap.get(device.getMacAddress());
        if (rxDevice == null)
        {
            rxDevice = com.idevicesinc.sweetblue.rx.RxBleDevice.create(device);
            m_deviceMap.put(device.getMacAddress(), rxDevice);
        }
        return rxDevice;
    }

    static RxBleServer getOrCreateServer(@Nullable(Nullable.Prevalence.NEVER) BleServer server)
    {
        RxBleServer rxServer = m_serverMap.get(server);
        if (rxServer == null)
        {
            rxServer = RxBleServer.create(server);
            m_serverMap.put(server, rxServer);
        }
        return rxServer;
    }

}
