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


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattServer;

import com.idevicesinc.sweetblue.AddServiceListener;
import com.idevicesinc.sweetblue.AdvertisingListener;
import com.idevicesinc.sweetblue.BleAdvertisingSettings;
import com.idevicesinc.sweetblue.BleCharacteristic;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.BleNodeConfig;
import com.idevicesinc.sweetblue.BleServer;
import com.idevicesinc.sweetblue.BleServerState;
import com.idevicesinc.sweetblue.BleService;
import com.idevicesinc.sweetblue.BleStatuses;
import com.idevicesinc.sweetblue.IncomingListener;
import com.idevicesinc.sweetblue.OutgoingListener;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.ServerConnectListener;
import com.idevicesinc.sweetblue.ServerReconnectFilter;
import com.idevicesinc.sweetblue.ServerStateListener;
import com.idevicesinc.sweetblue.annotations.Advanced;
import com.idevicesinc.sweetblue.annotations.Immutable;
import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.defaults.DefaultServerReconnectFilter;
import com.idevicesinc.sweetblue.internal.android.IServerListener;
import com.idevicesinc.sweetblue.internal.android.IBluetoothDevice;
import com.idevicesinc.sweetblue.internal.android.IBluetoothServer;
import com.idevicesinc.sweetblue.internal.android.P_DeviceHolder;
import com.idevicesinc.sweetblue.utils.BleScanRecord;
import com.idevicesinc.sweetblue.utils.ForEach_Breakable;
import com.idevicesinc.sweetblue.utils.ForEach_Void;
import com.idevicesinc.sweetblue.utils.FutureData;
import com.idevicesinc.sweetblue.utils.PresentData;
import com.idevicesinc.sweetblue.utils.State;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.Utils_String;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static com.idevicesinc.sweetblue.BleServerState.CONNECTED;
import static com.idevicesinc.sweetblue.BleServerState.CONNECTING;
import static com.idevicesinc.sweetblue.BleServerState.DISCONNECTED;
import static com.idevicesinc.sweetblue.BleServerState.RETRYING_CONNECTION;


public final class P_BleServerImpl extends BleNodeImpl implements IBleServer
{

    /**
     * Special value that is used in place of Java's built-in <code>null</code>.
     */
    @Immutable
    public static final P_BleServerImpl NULL = new P_BleServerImpl(null, /*isNull=*/true);


    private final P_ServerStateTracker m_stateTracker;
    final P_BleServerNativeManager m_nativeManager;
    private IncomingListener m_incomingListener;
    private OutgoingListener m_outgoingListener_default;
    private final boolean m_isNull;
    private BleNodeConfig m_config = null;
    private final P_ServerConnectionFailManager m_connectionFailMngr;
    private final P_ClientManager m_clientMngr;
    private final P_AdvertisementManager m_advManager;
    private ServerConnectListener m_connectListener;
    private Map<String, ServerConnectListener> m_ephemeralConnectListenerMap;


    /*package*/ P_BleServerImpl(final IBleManager mngr, final boolean isNull)
    {
        super(mngr);

        m_isNull = isNull;

        m_advManager = new P_AdvertisementManager(this);
        m_stateTracker = new P_ServerStateTracker(this);
        m_nativeManager = new P_BleServerNativeManager(this);
        m_connectionFailMngr = new P_ServerConnectionFailManager(this);
        m_clientMngr = new P_ClientManager(this);
        m_ephemeralConnectListenerMap = new HashMap<>();
        if (mngr != null)
        {
            m_config = mngr.getConfigClone();
            m_config.reconnectFilter = new DefaultServerReconnectFilter();
        }

    }

    @Override
    PA_ServiceManager newServiceManager()
    {
        return new P_ServerServiceManager(this);
    }

    @Override
    public BleNodeConfig conf_node()
    {
        return m_config != null ? m_config : conf_mngr();
    }

    @Override
    public P_BleServerNativeManager getNativeManager()
    {
        return m_nativeManager;
    }

    /**
     * Optionally sets overrides for any custom options given to {@link BleManager#get(android.content.Context, BleManagerConfig)}
     * for this individual server.
     */
    public final void setConfig(final BleNodeConfig config_nullable)
    {
        m_config = config_nullable == null ? null : config_nullable.clone();
    }

    /**
     * Set a listener here to be notified whenever this server's state changes in relation to a specific client.
     */
    public final void setListener_State(@Nullable(Nullable.Prevalence.NORMAL) final ServerStateListener listener_nullable)
    {
        m_stateTracker.setListener(listener_nullable);
    }

    /**
     * Set a listener here to be notified whenever this server's clients state changes eg. disconnected, connecting, and connected
     */
    public final void setListener_Connect(@Nullable(Nullable.Prevalence.NORMAL) final ServerConnectListener listener_nullable)
    {
        m_connectListener = listener_nullable;
    }

    /**
     * Set a listener here to override any listener provided previously.
     */
    public final void setListener_Incoming(@Nullable(Nullable.Prevalence.NORMAL) final IncomingListener listener_nullable)
    {
        m_incomingListener = listener_nullable;
    }

    /**
     * Set a listener here to override any listener provided previously and provide a default backup that will be called
     * after any listener provided to {@link #addService(BleService, AddServiceListener)}.
     */
    public final void setListener_ServiceAdd(@Nullable(Nullable.Prevalence.NORMAL) final AddServiceListener listener_nullable)
    {
        getServerServiceManager().setListener(listener_nullable);
    }

    public final void setListener_Advertising(@Nullable(Nullable.Prevalence.NORMAL) final AdvertisingListener listener_nullable)
    {
        m_advManager.setListener_Advertising(listener_nullable);
    }

    public final @Nullable(Nullable.Prevalence.RARE)
    AdvertisingListener getListener_Advertise()
    {
        return m_advManager.getListener_advertising();
    }

    /**
     * Returns the listener provided to {@link #setListener_Incoming(IncomingListener)}.
     */
    public final @Nullable(Nullable.Prevalence.RARE) IncomingListener getListener_Incoming()
    {
        return m_incomingListener;
    }

    public final void setListener_Outgoing(final OutgoingListener listener)
    {
        m_outgoingListener_default = listener;
    }

    /**
     * Set a listener here to override any listener provided previously.
     */
    public final void setListener_ReconnectFilter(final ServerReconnectFilter listener)
    {
        m_connectionFailMngr.setListener(listener);
    }

    /**
     * Overload of {@link #sendIndication(String, UUID, UUID, FutureData, OutgoingListener)}.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendIndication(final String macAddress, UUID charUuid, byte[] data)
    {
        return sendIndication(macAddress, null, charUuid, data, null);
    }

    /**
     * Overload of {@link #sendIndication(String, UUID, UUID, FutureData, OutgoingListener)}.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendIndication(final String macAddress, UUID charUuid, byte[] data, OutgoingListener listener)
    {
        return sendIndication(macAddress, null, charUuid, data, listener);
    }

    /**
     * Overload of {@link #sendIndication(String, UUID, UUID, FutureData, OutgoingListener)}.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendIndication(final String macAddress, UUID serviceUuid, UUID charUuid, byte[] data)
    {
        return sendIndication(macAddress, serviceUuid, charUuid, data, null);
    }

    /**
     * Overload of {@link #sendIndication(String, UUID, UUID, FutureData, OutgoingListener)}.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendIndication(final String macAddress, UUID serviceUuid, UUID charUuid, byte[] data, OutgoingListener listener)
    {
        return sendIndication(macAddress, serviceUuid, charUuid, new PresentData(data), listener);
    }

    /**
     * Overload of {@link #sendIndication(String, UUID, UUID, FutureData, OutgoingListener)}.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendIndication(final String macAddress, final UUID charUuid, final FutureData futureData)
    {
        return sendIndication(macAddress, null, charUuid, futureData, null);
    }

    /**
     * Overload of {@link #sendIndication(String, UUID, UUID, FutureData, OutgoingListener)}.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendIndication(final String macAddress, final UUID charUuid, final FutureData futureData, OutgoingListener listener)
    {
        return sendIndication(macAddress, null, charUuid, futureData, listener);
    }

    /**
     * Overload of {@link #sendIndication(String, UUID, UUID, FutureData, OutgoingListener)}.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendIndication(final String macAddress, final UUID serviceUuid, final UUID charUuid, final FutureData futureData)
    {
        return sendIndication(macAddress, serviceUuid, charUuid, futureData, null);
    }

    /**
     * Same as {@link #sendNotification(String, UUID, UUID, FutureData, OutgoingListener)} but sends an indication instead.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendIndication(final String macAddress, UUID serviceUuid, UUID charUuid, final FutureData futureData, OutgoingListener listener)
    {
        return sendNotification_private(macAddress, serviceUuid, charUuid, futureData, listener, /*isIndication=*/true);
    }

    /**
     * Overload of {@link #sendNotification(String, UUID, UUID, FutureData, OutgoingListener)}.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendNotification(final String macAddress, UUID charUuid, byte[] data)
    {
        return sendNotification(macAddress, null, charUuid, data, null);
    }

    /**
     * Overload of {@link #sendNotification(String, UUID, UUID, FutureData, OutgoingListener)}.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendNotification(final String macAddress, UUID charUuid, byte[] data, OutgoingListener listener)
    {
        return sendNotification(macAddress, null, charUuid, data, listener);
    }

    /**
     * Overload of {@link #sendNotification(String, UUID, UUID, FutureData, OutgoingListener)}.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendNotification(final String macAddress, UUID serviceUuid, UUID charUuid, byte[] data)
    {
        return sendNotification(macAddress, serviceUuid, charUuid, data, null);
    }

    /**
     * Overload of {@link #sendNotification(String, UUID, UUID, FutureData, OutgoingListener)}.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendNotification(final String macAddress, UUID serviceUuid, UUID charUuid, byte[] data, OutgoingListener listener)
    {
        return sendNotification(macAddress, serviceUuid, charUuid, new PresentData(data), listener);
    }

    /**
     * Overload of {@link #sendNotification(String, UUID, UUID, FutureData, OutgoingListener)}.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendNotification(final String macAddress, final UUID charUuid, final FutureData futureData)
    {
        return sendNotification(macAddress, null, charUuid, futureData, null);
    }

    /**
     * Overload of {@link #sendNotification(String, UUID, UUID, FutureData, OutgoingListener)}.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendNotification(final String macAddress, final UUID charUuid, final FutureData futureData, OutgoingListener listener)
    {
        return sendNotification(macAddress, null, charUuid, futureData, listener);
    }

    /**
     * Overload of {@link #sendNotification(String, UUID, UUID, FutureData, OutgoingListener)}.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendNotification(final String macAddress, final UUID serviceUuid, final UUID charUuid, final FutureData futureData)
    {
        return sendNotification(macAddress, serviceUuid, charUuid, futureData, null);
    }

    public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendNotification(final String macAddress, UUID serviceUuid, UUID charUuid, final FutureData futureData, OutgoingListener listener)
    {
        return sendNotification_private(macAddress, serviceUuid, charUuid, futureData, listener, /*isIndication=*/false);
    }

    public final boolean isAdvertisingSupportedByAndroidVersion()
    {
        return m_advManager.isAdvertisingSupportedByAndroidVersion();
    }

    /**
     * Checks to see if the device supports advertising. This is forwarded from {@link BleManager#isAdvertisingSupportedByChipset()}.
     */
    public final boolean isAdvertisingSupportedByChipset()
    {
        return m_advManager.isAdvertisingSupportedByChipset();
    }

    /**
     * Checks to see if the device supports advertising BLE services. This is forwarded from {@link BleManager#isAdvertisingSupported()}.
     */
    public final boolean isAdvertisingSupported()
    {
        return m_advManager.isAdvertisingSupported();
    }

    /**
     * Checks to see if the device is currently advertising.
     */
    public final boolean isAdvertising()
    {
        return m_advManager.isAdvertising();
    }

    /**
     * Checks to see if the device is currently advertising the given {@link UUID}.
     */
    public final boolean isAdvertising(UUID serviceUuid)
    {
        if (Utils.isLollipop())
        {
            return m_advManager.isAdvertising(serviceUuid);
        }
        return false;
    }

    /**
     * Starts advertising serviceUuids with the information supplied in {@link BleScanRecord}. Note that this will
     * only work for devices on Lollipop, or above. Even then, not every device supports advertising. Use
     * {@link BleManager#isAdvertisingSupported()} to check to see if the phone supports it.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) AdvertisingListener.AdvertisingEvent startAdvertising(BleScanRecord advertisePacket, BleAdvertisingSettings settings, AdvertisingListener listener)
    {
        return m_advManager.startAdvertising(advertisePacket, settings, listener);
    }

    /**
     * Stops the server from advertising.
     */
    public final void stopAdvertising()
    {
        if (Utils.isLollipop())
        {
            m_advManager.stopAdvertising();
        }
    }

    /**
     * Returns the name this {@link BleServer} is using (and will be advertised as, if applicable).
     */
    public final String getName()
    {
        final String name = getIManager().managerLayer().getName();
        return name != null ? name : "";
    }

    /**
     * Set the name you wish this {@link BleServer} to be known as. This will affect how other devices see this server, and sets the name
     * on the lower level {@link BluetoothAdapter}. If you DO change this, please be aware this will affect everything, including apps outside
     * of your own. It's probably best NOT to use this, but it's here for flexibility.
     *
     * @return <code>true</code> if the name was successfully set. This can return <code>false</code> if BLE is turned off -- so the underlying
     * {@link BluetoothAdapter} is <code>null</code>.
     */
    @Advanced
    public final boolean setName(String name)
    {
        final IBleManager mgr = getIManager();
        final P_DiskOptionsManager disk = mgr.getDiskOptionsManager();
        if (!disk.hasAdaptorAdvertisingName())
        {
            final String currentName = mgr.managerLayer().getName();
            if (currentName == null)
                return false;

            disk.saveAdaptorAdvertisingName(currentName);
        }
        m_advManager.setCustomName(name);
        return getIManager().managerLayer().setName(name);
    }

    @Override
    public final void resetAdaptorName()
    {
        P_DiskOptionsManager opts = getIManager().getDiskOptionsManager();
        if (opts.hasAdaptorAdvertisingName())
        {
            final String originalName = opts.getAdaptorAdvertisingName();
            // If the current name doesn't match the original, then set it to the original
            if (!getName().equals(originalName))
                getIManager().managerLayer().setName(originalName);
        }
    }

    /**
     * Provides just-in-case lower-level access to the native server instance.
     * See similar warning for {@link BleDevice#getNative()}.
     */
    @Advanced
    public final @Nullable(Nullable.Prevalence.RARE) BluetoothGattServer getNative()
    {
        return m_nativeManager.getNative().getNativeServer();
    }

    /**
     * Provides just-in-case access to the abstracted server instance.
     * See similar warning for {@link BleDevice#getNative()}.
     */
    @Advanced
    public final @Nullable(Nullable.Prevalence.RARE) IBluetoothServer getNativeLayer()
    {
        return m_nativeManager.getNative();
    }

    /**
     * Returns the bitwise state mask representation of {@link BleServerState} for the given client mac address.
     *
     * @see BleServerState
     */
    @Advanced
    public final int getStateMask(final String macAddress)
    {
        final String macAddress_normalized = Utils_String.normalizeMacAddress(macAddress);

        return m_stateTracker.getStateMask(macAddress_normalized);
    }

    /**
     * Returns <code>true</code> if there is any bitwise overlap between the provided value and {@link #getStateMask(String)}.
     *
     * @see #isAll(String, int)
     */
    public final boolean isAny(final String macAddress, final int mask_BleServerState)
    {
        return (getStateMask(macAddress) & mask_BleServerState) != 0x0;
    }

    /**
     * Returns <code>true</code> if there is complete bitwise overlap between the provided value and {@link #getStateMask(String)}.
     *
     * @see #isAny(String, int)
     *
     */
    public final boolean isAll(final String macAddress, final int mask_BleServerState)
    {
        return (getStateMask(macAddress) & mask_BleServerState) == mask_BleServerState;
    }

    /**
     * Returns true if the given client is in the state provided.
     */
    public final boolean is(final String macAddress, final BleServerState state)
    {
        return state.overlaps(getStateMask(macAddress));
    }

    /**
     * Returns true if the given client is in any of the states provided.
     */
    public final boolean isAny(final String macAddress, final BleServerState ... states )
    {
        final int stateMask = getStateMask(macAddress);

        for( int i = 0; i < states.length; i++ )
        {
            if( states[i].overlaps(stateMask) )  return true;
        }

        return false;
    }

    /**
     * Connect to the given client mac address and provided listeners that are shorthand for calling {@link #setListener_State(ServerStateListener)}
     * {@link #setListener_ReconnectFilter(ServerReconnectFilter)}.
     */
    public final ServerReconnectFilter.ConnectFailEvent connect(final String macAddress, final ServerConnectListener connectListener, final ServerReconnectFilter connectionFailListener)
    {
        final String macAddress_normalized = Utils_String.normalizeMacAddress(macAddress);

        P_DeviceHolder holder = P_DeviceHolder.newHolder(newNativeDevice(macAddress_normalized).getNativeDevice());

        // This is here purely for unit testing. We shouldn't ever see this in the wild.
        if (!holder.getAddress().equals(macAddress) && holder.isNull())
            holder = P_DeviceHolder.newNullHolder(macAddress);

        return connect_internal(holder, connectListener, connectionFailListener, false);
    }

    public final boolean disconnect(final String macAddress)
    {
        final String macAddress_normalized = Utils_String.normalizeMacAddress(macAddress);

        return disconnect_private(macAddress_normalized, ServerReconnectFilter.Status.CANCELLED_FROM_DISCONNECT, State.ChangeIntent.INTENTIONAL);
    }

    public final void disconnect()
    {
        disconnect_internal(AddServiceListener.Status.CANCELLED_FROM_DISCONNECT, ServerReconnectFilter.Status.CANCELLED_FROM_DISCONNECT, State.ChangeIntent.INTENTIONAL);
    }

    @Override public final boolean isNull()
    {
        return m_isNull;
    }

    /**
     * Does a referential equality check on the two servers.
     */
    public final boolean equals(@Nullable(Nullable.Prevalence.NORMAL) final IBleServer server_nullable)
    {
        if (server_nullable == null)												return false;
        if (server_nullable == this)												return true;
        if (server_nullable.getNativeLayer().isServerNull() || this.getNativeLayer().isServerNull() )		return false;
        if( this.isNull() && server_nullable.isNull() )								return true;

        return server_nullable == this;
    }

    /**
     * Returns {@link #equals(IBleServer)} if object is an instance of {@link BleServer}. Otherwise calls super.
     */
    @Override public final boolean equals(@Nullable(Nullable.Prevalence.NORMAL) final Object object_nullable)
    {
        if( object_nullable == null )  return false;

        if (object_nullable instanceof BleServer)
        {
            final BleServer object_cast = (BleServer) object_nullable;

            return this.equals(P_Bridge_User.getIBleServer(object_cast));
        }

        return false;
    }

    /**
     * Starts the process of adding a service to this server. The provided listener will be called when the service is added or there is a problem.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) AddServiceListener.ServiceAddEvent addService(final BleService service, final AddServiceListener listener)
    {
        return getServerServiceManager().addService(service, listener);
    }

    public final @Nullable(Nullable.Prevalence.NORMAL) BleService removeService(final UUID serviceUuid)
    {
        return getServerServiceManager().remove(serviceUuid);
    }

    /**
     * Convenience to remove all services previously added with {@link #addService(BleService, AddServiceListener)} (or overloads). This is slightly more performant too.
     */
    public final void removeAllServices()
    {
        getServerServiceManager().removeAll(AddServiceListener.Status.CANCELLED_FROM_REMOVAL);
    }

    /**
     * Offers a more "functional" means of iterating through the internal list of clients instead of
     * using {@link #getClients()} or {@link #getClients_List()}.
     */
    public final void getClients(final ForEach_Void<String> forEach)
    {
        m_clientMngr.getClients(forEach, 0x0);
    }

    /**
     * Same as {@link #getClients(ForEach_Void)} but will only return clients
     * in the given state provided.
     */
    public final void getClients(final ForEach_Void<String> forEach, final BleServerState state)
    {
        m_clientMngr.getClients(forEach, state.bit());
    }

    /**
     * Same as {@link #getClients(ForEach_Void)} but will only return clients
     * in any of the given states provided.
     */
    public final void getClients(final ForEach_Void<String> forEach, final BleServerState ... states)
    {
        m_clientMngr.getClients(forEach, BleServerState.toBits(states));
    }

    /**
     * Overload of {@link #getClients(ForEach_Void)}
     * if you need to break out of the iteration at any point.
     */
    public final void getClients(final ForEach_Breakable<String> forEach)
    {
        m_clientMngr.getClients(forEach, 0x0);
    }

    /**
     * Overload of {@link #getClients(ForEach_Void, BleServerState)}
     * if you need to break out of the iteration at any point.
     */
    public final void getClients(final ForEach_Breakable<String> forEach, final BleServerState state)
    {
        m_clientMngr.getClients(forEach, state.bit());
    }

    /**
     * Same as {@link #getClients(ForEach_Breakable)} but will only return clients
     * in any of the given states provided.
     */
    public final void getClients(final ForEach_Breakable<String> forEach, final BleServerState ... states)
    {
        m_clientMngr.getClients(forEach, BleServerState.toBits(states));
    }

    /**
     * Returns all the clients connected or connecting (or previously so) to this server.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) Iterator<String> getClients()
    {
        return m_clientMngr.getClients(0x0);
    }

    /**
     * Returns all the clients connected or connecting (or previously so) to this server.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) Iterator<String> getClients(final BleServerState state)
    {
        return m_clientMngr.getClients(state.bit());
    }

    /**
     * Returns all the clients connected or connecting (or previously so) to this server.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) Iterator<String> getClients(final BleServerState ... states)
    {
        return m_clientMngr.getClients(BleServerState.toBits(states));
    }

    /**
     * Overload of {@link #getClients()} that returns a {@link java.util.List} for you.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) List<String> getClients_List()
    {
        return m_clientMngr.getClients_List(0x0);
    }

    /**
     * Overload of {@link #getClients(BleServerState)} that returns a {@link java.util.List} for you.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) List<String> getClients_List(final BleServerState state)
    {
        return m_clientMngr.getClients_List(state.bit());
    }

    /**
     * Overload of {@link #getClients(BleServerState[])} that returns a {@link java.util.List} for you.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) List<String> getClients_List(final BleServerState ... states)
    {
        return m_clientMngr.getClients_List(BleServerState.toBits(states));
    }

    /**
     * Returns the total number of clients this server is connecting or connected to (or previously so).
     */
    public final int getClientCount()
    {
        return m_clientMngr.getClientCount();
    }

    /**
     * Returns the number of clients that are in the current state.
     */
    public final int getClientCount(final BleServerState state)
    {
        return m_clientMngr.getClientCount(state.bit());
    }

    /**
     * Returns the number of clients that are in any of the given states.
     */
    public final int getClientCount(final BleServerState ... states)
    {
        return m_clientMngr.getClientCount(BleServerState.toBits(states));
    }

    /**
     * Pretty-prints the list of connecting or connected clients.
     */
    public final String toString()
    {
        return this.getClass().getSimpleName() + " with " + m_clientMngr.getClientCount(BleServerState.toBits(CONNECTING, CONNECTED)) + " connected/ing clients.";
    }

    /**
     * Returns the local mac address provided by {@link BluetoothAdapter#getAddress()}.
     */
    @Override public final @Nullable(Nullable.Prevalence.NEVER) String getMacAddress()
    {
        return getIManager().managerLayer().getAddress();
    }

    public final void onAdvertiseStarted(BleScanRecord packet, AdvertisingListener listener)
    {
        m_advManager.onAdvertiseStart(packet);
        invokeAdvertiseListeners(AdvertisingListener.Status.SUCCESS, listener);
    }

    public final void onAdvertiseStartFailed(final AdvertisingListener.Status status, final AdvertisingListener listener)
    {
        m_advManager.onAdvertiseStartFailed(status);
        if (getIManager().getConfigClone().postCallbacksToMainThread)
        {
            getIManager().getPostManager().postToMain(() -> invokeAdvertiseListeners(status, listener));
        }
        else
            invokeAdvertiseListeners(status, listener);
    }

    public final void disconnect_internal(final AddServiceListener.Status status_serviceAdd, final ServerReconnectFilter.Status status_connectionFail, final State.ChangeIntent intent)
    {
        stopAdvertising();

        getClients(next -> {
            disconnect_private(next, status_connectionFail, intent);

            m_nativeManager.ignoreNextImplicitDisconnect(next);
        }, CONNECTING, CONNECTED);

        m_nativeManager.closeServer();

        getServerServiceManager().removeAll(status_serviceAdd);
    }

    public BleServer getBleServer()
    {
        return getIManager().getBleServer(this);
    }

    public final void onNativeConnectFail(final P_DeviceHolder nativeDevice, final ServerReconnectFilter.Status status, final int gattStatus)
    {
        if( status == ServerReconnectFilter.Status.TIMED_OUT )
        {
            final P_Task_DisconnectServer task = new P_Task_DisconnectServer(this, nativeDevice, m_nativeManager.getNativeListener().m_taskStateListener, /*explicit=*/true, PE_TaskPriority.FOR_EXPLICIT_BONDING_AND_CONNECTING);
            taskManager().add(task);
        }

        m_stateTracker.doStateTransition(nativeDevice.getAddress(), BleServerState.CONNECTING /* ==> */, BleServerState.DISCONNECTED, State.ChangeIntent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);

        m_connectionFailMngr.onNativeConnectFail(nativeDevice, status, gattStatus);

        // Invoking the ServerConnectListener is handled in the P_ServerConnectionFailEntry.onNativeConnectFail method, so we have access to the ConnectFailEvent
    }

    public final void onNativeDisconnect( final String macAddress, final boolean explicit, final int gattStatus)
    {
        final boolean ignore = m_nativeManager.shouldIgnoreImplicitDisconnect(macAddress);

        if( explicit == false && ignore == false )
        {
            m_stateTracker.doStateTransition(macAddress, BleServerState.CONNECTED /* ==> */, BleServerState.DISCONNECTED, State.ChangeIntent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
        }
        else
        {
            // explicit case gets handled immediately by the disconnect method.
        }
    }

    public final void onNativeConnect(final String macAddress, final boolean explicit)
    {
        m_clientMngr.onConnected(macAddress);

        final State.ChangeIntent intent = explicit ? State.ChangeIntent.INTENTIONAL : State.ChangeIntent.UNINTENTIONAL;

        //--- DRK > Testing and source code inspection reveals that it's impossible for the native stack to report server->client BLE_CONNECTING.
        //---		In other words for both implicit and explicit connects it always jumps from BLE_DISCONNECTED to BLE_CONNECTED.
        //---		For explicit connects through SweetBlue we can thus fake the BLE_CONNECTING state cause we know a task was in the queue, etc.
        //---		For implicit connects the decision is made here to reflect what happens in the native stack, cause as far as SweetBlue
        //---		is concerned we were never in the BLE_CONNECTING state either.
        final BleServerState previousState = explicit ? BleServerState.CONNECTING : BleServerState.DISCONNECTED;

        m_stateTracker.doStateTransition(macAddress, previousState /* ==> */, BleServerState.CONNECTED, intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE);

        ServerConnectListener.ConnectEvent event = P_Bridge_User.newServerConnectEvent(getBleServer(), macAddress, null);

        invokeConnectListeners(event);
    }

    public final void onNativeConnecting_implicit(final String macAddress)
    {
        m_clientMngr.onConnecting(macAddress);

        m_stateTracker.doStateTransition(macAddress, BleServerState.DISCONNECTED /* ==> */, BleServerState.CONNECTING, State.ChangeIntent.UNINTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
    }

    public P_ServerServiceManager getServerServiceManager()
    {
        return getServiceManager();
    }

    public final void invokeOutgoingListeners(final OutgoingListener.OutgoingEvent e, final OutgoingListener listener_specific_nullable)
    {
        if( listener_specific_nullable != null )
            getIManager().postEvent(listener_specific_nullable, e);

        if( m_outgoingListener_default != null )
            getIManager().postEvent(m_outgoingListener_default, e);

        final OutgoingListener listener = getIManager().getListener_Outgoing();
        if( listener != null )
            getIManager().postEvent(listener, e);
    }

    public final void invokeConnectListeners(ServerConnectListener.ConnectEvent event)
    {
        ServerConnectListener listener = m_ephemeralConnectListenerMap.remove(event.macAddress());
        if (listener != null)
            getIManager().postEvent(listener, event);

        listener = m_connectListener;
        if (listener != null)
            getIManager().postEvent(listener, event);

        listener = getIManager().getDefaultServerConnectListener();
        if (listener != null)
            getIManager().postEvent(listener, event);
    }

    public final ServerReconnectFilter.ConnectFailEvent connect_internal(final P_DeviceHolder nativeDevice, boolean isRetrying)
    {
        return connect_internal(nativeDevice, null, null, isRetrying);
    }

    public final IServerListener getInternalListener()
    {
        return m_nativeManager.getNativeListener().getInternalListener();
    }

    public final void clearListeners()
    {
        m_stateTracker.setListener(null);
        m_connectListener = null;
        m_incomingListener = null;
        getServerServiceManager().setListener(null);
        m_advManager.setListener_Advertising(null);
    }

















    final void invokeAdvertiseListeners(AdvertisingListener.Status result, AdvertisingListener listener)
    {
        final AdvertisingListener.AdvertisingEvent event = P_Bridge_User.newAdvertisingEvent(getBleServer(), result);
        if (listener != null)
        {
            getIManager().postEvent(listener, event);
        }
        final AdvertisingListener defaultListener = m_advManager.getListener_advertising();
        if (defaultListener != null)
        {
            getIManager().postEvent(defaultListener, event);
        }

        AdvertisingListener manlistener = getIManager().getListener_Advertising();
        if (manlistener != null)
        {
            getIManager().postEvent(manlistener, event);
        }
    }















    private boolean disconnect_private(final String macAddress, final ServerReconnectFilter.Status status_connectionFail, final State.ChangeIntent intent)
    {
        final boolean addTask = true;

        m_connectionFailMngr.onExplicitDisconnect(macAddress);

        if( is(macAddress, DISCONNECTED) )  return false;

        final BleServerState oldConnectionState = m_stateTracker.getOldConnectionState(macAddress);

        final IBluetoothDevice nativeDevice = newNativeDevice(macAddress);
        final P_DeviceHolder holder = P_DeviceHolder.newHolder(nativeDevice.getNativeDevice());

        if( addTask )
        {
            //--- DRK > Purposely doing explicit=true here without regarding the intent.
            final boolean explicit = true;
            final P_Task_DisconnectServer task = new P_Task_DisconnectServer(this, holder, m_nativeManager.getNativeListener().m_taskStateListener, /*explicit=*/true, PE_TaskPriority.FOR_EXPLICIT_BONDING_AND_CONNECTING);
            taskManager().add(task);
        }

        m_stateTracker.doStateTransition(macAddress, oldConnectionState /* ==> */, BleServerState.DISCONNECTED, intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE);

        if( oldConnectionState == CONNECTING )
        {
            m_connectionFailMngr.onNativeConnectFail(holder, status_connectionFail, BleStatuses.GATT_STATUS_NOT_APPLICABLE);
        }

        return true;
    }

    private final ServerReconnectFilter.ConnectFailEvent connect_internal(final P_DeviceHolder nativeDevice, final ServerConnectListener connectListener, final ServerReconnectFilter connectionFailListener, final boolean isRetrying)
    {
        m_nativeManager.clearImplicitDisconnectIgnoring(nativeDevice.getAddress());

        if( connectListener != null )
            m_ephemeralConnectListenerMap.put(nativeDevice.getAddress(), connectListener);

        if( connectionFailListener != null )
            setListener_ReconnectFilter(connectionFailListener);

        if( isNull() )
        {
            final ServerReconnectFilter.ConnectFailEvent e = P_Bridge_User.newServerEarlyOut(getBleServer(), nativeDevice.getDevice(), ServerReconnectFilter.Status.NULL_SERVER);

            m_connectionFailMngr.invokeCallback(e);

            return e;
        }

        m_connectionFailMngr.onExplicitConnectionStarted(nativeDevice.getAddress());

        if( isAny(nativeDevice.getAddress(), CONNECTING, CONNECTED) )
        {
            final ServerReconnectFilter.ConnectFailEvent e = P_Bridge_User.newServerEarlyOut(getBleServer(), nativeDevice.getDevice(), ServerReconnectFilter.Status.ALREADY_CONNECTING_OR_CONNECTED);

            m_connectionFailMngr.invokeCallback(e);

            return e;
        }

        m_clientMngr.onConnecting(nativeDevice.getAddress());

        final P_Task_ConnectServer task = new P_Task_ConnectServer(this, nativeDevice, m_nativeManager.getNativeListener().m_taskStateListener, /*explicit=*/true, PE_TaskPriority.FOR_EXPLICIT_BONDING_AND_CONNECTING);
        taskManager().add(task);

        final BleServerState newState = isRetrying ? RETRYING_CONNECTION : CONNECTING;
        m_stateTracker.doStateTransition(nativeDevice.getAddress(), BleServerState.DISCONNECTED /* ==> */, newState, State.ChangeIntent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);

        return P_Bridge_User.serverNULL(getBleServer(), nativeDevice.getDevice());
    }

    private IBluetoothDevice newNativeDevice(final String macAddress)
    {
        final IBleManager mngr = getIManager();

        return mngr == null ? IBluetoothDevice.NULL : mngr.newNativeDevice(macAddress);
    }

    private OutgoingListener.OutgoingEvent sendNotification_private(final String macAddress, final UUID serviceUuid, final UUID charUuid, final FutureData futureData, final OutgoingListener listener, final boolean isIndication)
    {
        final String macAddress_normalized = Utils_String.normalizeMacAddress(macAddress);

        final IBluetoothDevice nativeDevice = newNativeDevice(macAddress_normalized);

        if( isNull() )
        {
            final OutgoingListener.OutgoingEvent e = P_Bridge_User.newOutgoingEarlyOut(getBleServer(), P_DeviceHolder.newHolder(nativeDevice.getNativeDevice()), serviceUuid, charUuid, futureData, OutgoingListener.Status.NULL_SERVER);

            invokeOutgoingListeners(e, listener);

            return e;
        }

        if( !is(macAddress_normalized, CONNECTED ) )
        {
            final OutgoingListener.OutgoingEvent e = P_Bridge_User.newOutgoingEarlyOut(getBleServer(), P_DeviceHolder.newHolder(nativeDevice.getNativeDevice()), serviceUuid, charUuid, futureData, OutgoingListener.Status.NOT_CONNECTED);

            invokeOutgoingListeners(e, listener);

            return e;
        }

        final BleCharacteristic char_native = getNativeBleCharacteristic(serviceUuid, charUuid, null);

        if( char_native == null )
        {
            final OutgoingListener.OutgoingEvent e = P_Bridge_User.newOutgoingEarlyOut(getBleServer(), P_DeviceHolder.newHolder(nativeDevice.getNativeDevice()), serviceUuid, charUuid, futureData, OutgoingListener.Status.NO_MATCHING_TARGET);

            invokeOutgoingListeners(e, listener);

            return e;
        }

        final boolean confirm = isIndication;
        final P_Task_SendNotification task = new P_Task_SendNotification(this, nativeDevice, serviceUuid, charUuid, futureData, confirm, listener);
        taskManager().add(task);

        return P_Bridge_User.outgoingNULL(getBleServer(), P_DeviceHolder.newHolder(nativeDevice.getNativeDevice()), serviceUuid, charUuid);
    }
}
