package com.idevicesinc.sweetblue.rx;


import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import com.idevicesinc.sweetblue.AddServiceListener;
import com.idevicesinc.sweetblue.AdvertisingListener;
import com.idevicesinc.sweetblue.BleNodeConfig;
import com.idevicesinc.sweetblue.BleServer;
import com.idevicesinc.sweetblue.BleServerState;
import com.idevicesinc.sweetblue.BleService;
import com.idevicesinc.sweetblue.IncomingListener;
import com.idevicesinc.sweetblue.OutgoingListener;
import com.idevicesinc.sweetblue.ServerConnectListener;
import com.idevicesinc.sweetblue.ServerReconnectFilter;
import com.idevicesinc.sweetblue.ServerStateListener;
import com.idevicesinc.sweetblue.annotations.Advanced;
import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.internal.android.IBluetoothServer;
import com.idevicesinc.sweetblue.rx.annotations.HotObservable;
import com.idevicesinc.sweetblue.rx.exception.AdvertisingException;
import com.idevicesinc.sweetblue.rx.exception.OutgoingException;
import com.idevicesinc.sweetblue.rx.exception.ServerConnectException;
import com.idevicesinc.sweetblue.rx.exception.ServiceAddException;
import com.idevicesinc.sweetblue.rx.schedulers.SweetBlueSchedulers;
import com.idevicesinc.sweetblue.utils.BleScanRecord;

import java.util.UUID;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;



public final class RxBleServer
{

    private final BleServer m_server;

    private Flowable<RxServerStateEvent> m_stateFlowable;
    private Flowable<RxOutgoingEvent> m_outgoingFlowable;
    private Flowable<RxServiceAddEvent> m_serviceAddFlowable;
    private Flowable<RxAdvertisingEvent> m_advertisingFlowable;


    private RxBleServer(BleServer server)
    {
        m_server = server;
    }


    public final BleServer getBleServer()
    {
        return m_server;
    }


    public final @HotObservable @Nullable(Nullable.Prevalence.NEVER) Flowable<RxServerStateEvent> observeStateEvents()
    {
        if (m_stateFlowable == null)
        {
            m_stateFlowable = Flowable.create((FlowableOnSubscribe<ServerStateListener.StateEvent>) emitter ->
            {
                if (emitter.isCancelled()) return;

                m_server.setListener_State(e ->
                {
                    if (emitter.isCancelled()) return;

                    emitter.onNext(e);
                });

                emitter.setCancellable(() ->
                {
                    m_server.setListener_State(null);
                    m_stateFlowable = null;
                });
            }, BackpressureStrategy.BUFFER).subscribeOn(SweetBlueSchedulers.sweetBlueThread()).map(RxServerStateEvent::new).share();
        }

        return m_stateFlowable.share();
    }

    public final @HotObservable @Nullable(Nullable.Prevalence.NEVER) Flowable<RxOutgoingEvent> observeOutgoingEvents()
    {
        if (m_outgoingFlowable == null)
        {
            m_outgoingFlowable = Flowable.create((FlowableOnSubscribe<OutgoingListener.OutgoingEvent>) emitter ->
            {
                if (emitter.isCancelled()) return;

                m_server.setListener_Outgoing(e ->
                {
                    if (emitter.isCancelled()) return;

                    emitter.onNext(e);
                });

                emitter.setCancellable(() ->
                {
                    m_server.setListener_Outgoing(null);
                    m_outgoingFlowable = null;
                });
            }, BackpressureStrategy.BUFFER).subscribeOn(SweetBlueSchedulers.sweetBlueThread()).map(RxOutgoingEvent::new).share();
        }

        return m_outgoingFlowable.share();
    }

    public final @HotObservable @Nullable(Nullable.Prevalence.NEVER) Flowable<RxServiceAddEvent> observeServiceAddEvents()
    {
        if (m_serviceAddFlowable == null)
        {
            m_serviceAddFlowable = Flowable.create((FlowableOnSubscribe<AddServiceListener.ServiceAddEvent>) emitter ->
            {
                if (emitter.isCancelled()) return;

                m_server.setListener_ServiceAdd(e ->
                {
                    if (emitter.isCancelled()) return;

                    emitter.onNext(e);
                });

                emitter.setCancellable(() ->
                {
                    m_server.setListener_ServiceAdd(null);
                    m_serviceAddFlowable = null;
                });
            }, BackpressureStrategy.BUFFER).subscribeOn(SweetBlueSchedulers.sweetBlueThread()).map(RxServiceAddEvent::new).share();
        }

        return m_serviceAddFlowable.share();
    }

    public final @HotObservable @Nullable(Nullable.Prevalence.NEVER) Flowable<RxAdvertisingEvent> observeAdvertisingEvents()
    {
        if (m_advertisingFlowable == null)
        {
            m_advertisingFlowable = Flowable.create((FlowableOnSubscribe<AdvertisingListener.AdvertisingEvent>) emitter ->
            {
                if (emitter.isCancelled()) return;

                m_server.setListener_Advertising(e ->
                {
                    if (emitter.isCancelled()) return;

                    emitter.onNext(e);
                });

                emitter.setCancellable(() ->
                {
                    m_server.setListener_Advertising(null);
                    m_advertisingFlowable = null;
                });
            }, BackpressureStrategy.BUFFER).subscribeOn(SweetBlueSchedulers.sweetBlueThread()).map(RxAdvertisingEvent::new).share();
        }

        return m_advertisingFlowable.share();
    }

    /**
     * Set a listener here to override any listener provided previously.
     */
    public final void setListener_Incoming(@Nullable(Nullable.Prevalence.NORMAL) final IncomingListener listener_nullable)
    {
        m_server.setListener_Incoming(listener_nullable);
    }

    /**
     * Set a listener here to override any listener provided previously.
     */
    public final void setListener_ReconnectFilter(final ServerReconnectFilter listener)
    {
        m_server.setListener_ReconnectFilter(listener);
    }

    public final void setConfig(BleNodeConfig config)
    {
        m_server.setConfig(config);
    }

    public final @Nullable(Nullable.Prevalence.NEVER) Single<RxOutgoingEvent> sendIndication(final String macAddress, final UUID serviceUuid, final UUID charUuid, final byte[] data)
    {
        return Single.create((SingleOnSubscribe<OutgoingListener.OutgoingEvent>) emitter ->
        {
            if (emitter.isDisposed()) return;

            m_server.sendIndication(macAddress, serviceUuid, charUuid, data, e ->
            {
                if (emitter.isDisposed()) return;

                if (e.wasSuccess())
                    emitter.onSuccess(e);
                else
                    emitter.onError(new OutgoingException(e));
            });
        }).subscribeOn(SweetBlueSchedulers.sweetBlueThread()).map(RxOutgoingEvent::new);
    }

    public final @Nullable(Nullable.Prevalence.NEVER) Single<RxOutgoingEvent> sendNotification(final String macAddress, final UUID serviceUuid, final UUID charUuid, final byte[] data)
    {
        return Single.create((SingleOnSubscribe<OutgoingListener.OutgoingEvent>) emitter ->
        {
            if (emitter.isDisposed()) return;

            m_server.sendNotification(macAddress, serviceUuid, charUuid, data, e ->
            {
                if (emitter.isDisposed()) return;

                if (e.wasSuccess())
                    emitter.onSuccess(e);
                else
                    emitter.onError(new OutgoingException(e));
            });
        }).subscribeOn(SweetBlueSchedulers.sweetBlueThread()).map(RxOutgoingEvent::new);
    }

    public final @Nullable(Nullable.Prevalence.NEVER) Completable connect(final String macAddress)
    {
        return connect(macAddress, null);
    }

    public final @Nullable(Nullable.Prevalence.NEVER) Completable connect(final String macAddress, final ServerReconnectFilter failListener)
    {
        return Completable.create(emitter ->
        {
            if (emitter.isDisposed()) return;

            m_server.connect(macAddress, e ->
            {
                if (emitter.isDisposed()) return;

                if (e.wasSuccess())
                    emitter.onComplete();
                else if (!e.isRetrying())
                    emitter.onError(new ServerConnectException(e.failEvent()));

                // As the connection is being retried, we don't do anything here because we'll get another event in whether it's successful or not

            }, failListener);
        }).subscribeOn(SweetBlueSchedulers.sweetBlueThread());
    }

    public final @Nullable(Nullable.Prevalence.NEVER) Observable<RxServerConnectEvent> connect_withRetries(final String macAddress)
    {
        return connect_withRetries(macAddress, null);
    }

    public final @Nullable(Nullable.Prevalence.NEVER) Observable<RxServerConnectEvent> connect_withRetries(final String macAddress, final ServerReconnectFilter reconnectFilter)
    {
        return Observable.create((ObservableEmitter<ServerConnectListener.ConnectEvent> emitter) ->
        {
            if (emitter.isDisposed()) return;

            m_server.connect(macAddress, e ->
            {
                if (emitter.isDisposed()) return;

                emitter.onNext(e);

            }, reconnectFilter);
        }).subscribeOn(SweetBlueSchedulers.sweetBlueThread()).map(RxServerConnectEvent::new);
    }

    public final boolean disconnect(String macAddress)
    {
        return m_server.disconnect(macAddress);
    }

    public final void disconnect()
    {
        m_server.disconnect();
    }

    public final @Nullable(Nullable.Prevalence.NEVER) Single<RxServiceAddEvent> addService(final BleService service)
    {
        return Single.create((SingleOnSubscribe<AddServiceListener.ServiceAddEvent>) emitter ->
        {
            if (emitter.isDisposed()) return;

            m_server.addService(service, e ->
            {
                if (emitter.isDisposed()) return;

                if (e.wasSuccess())
                    emitter.onSuccess(e);
                else
                    emitter.onError(new ServiceAddException(e));
            });
        }).subscribeOn(SweetBlueSchedulers.sweetBlueThread()).map(RxServiceAddEvent::new);
    }

    public final @Nullable(Nullable.Prevalence.NEVER) BleService removeBleService(UUID serviceUuid)
    {
        return m_server.removeService(serviceUuid);
    }

    public final @Nullable(Nullable.Prevalence.RARE) BluetoothGattService removeService(UUID serviceUuid)
    {
        return m_server.removeService(serviceUuid).getService();
    }

    public final void removeAllServices()
    {
        m_server.removeAllServices();
    }

    public final String getMacAddress()
    {
        return m_server.getMacAddress();
    }

    public final String getName()
    {
        return m_server.getName();
    }

    @Advanced
    public final void setName(String name)
    {
        m_server.setName(name);
    }

    /**
     * Checks to see if the device is running an Android OS which supports
     * advertising. This is forwarded from {@link RxBleManager#isAdvertisingSupportedByAndroidVersion()}.
     */
    public final boolean isAdvertisingSupportedByAndroidVersion()
    {
        return m_server.isAdvertisingSupportedByAndroidVersion();
    }

    /**
     * Checks to see if the device supports advertising. This is forwarded from {@link RxBleManager#isAdvertisingSupportedByChipset()}.
     */
    public final boolean isAdvertisingSupportedByChipset()
    {
        return m_server.isAdvertisingSupportedByChipset();
    }

    /**
     * Checks to see if the device supports advertising BLE services. This is forwarded from {@link RxBleManager#isAdvertisingSupported()}.
     */
    public final boolean isAdvertisingSupported()
    {
        return m_server.isAdvertisingSupported();
    }

    public final boolean isAdvertising()
    {
        return m_server.isAdvertising();
    }

    /**
     * Checks to see if the device is currently advertising the given {@link UUID}.
     */
    public final boolean isAdvertising(UUID serviceUuid)
    {
        return m_server.isAdvertising(serviceUuid);
    }

    public final @Nullable(Nullable.Prevalence.NEVER) Observable<String> getClients()
    {
        return Observable.fromIterable(m_server.getClients_List());
    }

    /**
     * Returns the total number of clients this server is connecting or connected to (or previously so).
     */
    public final int getClientCount()
    {
        return m_server.getClientCount();
    }

    /**
     * Returns the number of clients that are in the current state.
     */
    public final int getClientCount(final BleServerState state)
    {
        return m_server.getClientCount(state);
    }

    /**
     * Returns the number of clients that are in any of the given states.
     */
    public final int getClientCount(final BleServerState... states)
    {
        return m_server.getClientCount(states);
    }

    /**
     * Returns <code>true</code> if this server has any connected or connecting clients (or previously so).
     */
    public final boolean hasClients()
    {
        return m_server.hasClients();
    }

    /**
     * Returns <code>true</code> if this server has any clients in the given state.
     */
    public final boolean hasClient(final BleServerState state)
    {
        return m_server.hasClient(state);
    }

    /**
     * Returns <code>true</code> if this server has any clients in any of the given states.
     */
    public final boolean hasClient(final BleServerState... states)
    {
        return m_server.hasClient(states);
    }

    public final @Nullable(Nullable.Prevalence.NEVER) Single<RxAdvertisingEvent> startAdvertising(final BleScanRecord advPacket)
    {
        return Single.create((SingleOnSubscribe<AdvertisingListener.AdvertisingEvent>) emitter ->
        {
            if (emitter.isDisposed()) return;

            m_server.startAdvertising(advPacket, e ->
            {
                if (emitter.isDisposed()) return;

                if (e.wasSuccess())
                    emitter.onSuccess(e);
                else
                    emitter.onError(new AdvertisingException(e));
            });
        }).subscribeOn(SweetBlueSchedulers.sweetBlueThread()).map(RxAdvertisingEvent::new);
    }

    public final void stopAdvertising()
    {
        m_server.stopAdvertising();
    }

    /**
     * Provides just-in-case lower-level access to the native server instance.
     * See similar warning for {@link RxBleDevice#getNative()}.
     */
    @Advanced
    public final @Nullable(Nullable.Prevalence.RARE) BluetoothGattServer getNative()
    {
        return m_server.getNative();
    }

    /**
     * Provides just-in-case access to the abstracted server instance.
     * See similar warning for {@link RxBleDevice#getNative()}.
     */
    @Advanced
    public final @Nullable(Nullable.Prevalence.RARE) IBluetoothServer getNativeLayer()
    {
        return m_server.getNativeLayer();
    }

    /**
     * Returns the bitwise state mask representation of {@link BleServerState} for the given client mac address.
     *
     * @see BleServerState
     */
    @Advanced
    public final int getStateMask(final String macAddress)
    {
        return m_server.getStateMask(macAddress);
    }

    /**
     * Returns <code>true</code> if there is any bitwise overlap between the provided value and {@link #getStateMask(String)}.
     *
     * @see #isAll(String, int)
     */
    public final boolean isAny(final String macAddress, final int mask_BleServerState)
    {
        return m_server.isAny(macAddress, mask_BleServerState);
    }

    /**
     * Returns <code>true</code> if there is complete bitwise overlap between the provided value and {@link #getStateMask(String)}.
     *
     * @see #isAny(String, int)
     */
    public final boolean isAll(final String macAddress, final int mask_BleServerState)
    {
        return m_server.isAll(macAddress, mask_BleServerState);
    }

    /**
     * Returns true if the given client is in the state provided.
     */
    public final boolean is(final String macAddress, final BleServerState state)
    {
        return m_server.is(macAddress, state);
    }

    /**
     * Returns true if the given client is in any of the states provided.
     */
    public final boolean isAny(final String macAddress, final BleServerState... states)
    {
        return m_server.isAny(macAddress, states);
    }

    public final boolean isNull()
    {
        return m_server.isNull();
    }

    /**
     * Does a referential equality check on the two servers.
     */
    public final boolean equals(@Nullable(Nullable.Prevalence.NORMAL) final RxBleServer server_nullable)
    {
        return m_server.equals(server_nullable.getBleServer());
    }

    /**
     * Returns {@link #equals(RxBleServer)} if object is an instance of {@link RxBleServer}. Otherwise calls super.
     *
     * @see RxBleServer#equals(RxBleServer)
     */
    @Override
    public final boolean equals(@Nullable(Nullable.Prevalence.NORMAL) final Object object_nullable)
    {
        return m_server.equals(object_nullable);
    }

    /**
     * Pretty-prints the list of connecting or connected clients.
     */
    public final String toString()
    {
        return m_server.toString();
    }


    static RxBleServer create(BleServer server)
    {
        return new RxBleServer(server);
    }
}
