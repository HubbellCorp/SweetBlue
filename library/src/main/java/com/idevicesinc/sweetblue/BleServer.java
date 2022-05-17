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

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattServer;
import com.idevicesinc.sweetblue.annotations.Advanced;
import com.idevicesinc.sweetblue.annotations.Immutable;
import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.internal.IBleServer;
import com.idevicesinc.sweetblue.internal.P_BleServerImpl;
import com.idevicesinc.sweetblue.internal.P_Bridge_Internal;
import com.idevicesinc.sweetblue.internal.android.IBluetoothServer;
import com.idevicesinc.sweetblue.utils.BleScanRecord;
import com.idevicesinc.sweetblue.utils.ForEach_Breakable;
import com.idevicesinc.sweetblue.utils.ForEach_Void;
import com.idevicesinc.sweetblue.utils.FutureData;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.PresentData;
import com.idevicesinc.sweetblue.BleAdvertisingSettings.BleTransmissionPower;
import com.idevicesinc.sweetblue.BleAdvertisingSettings.BleAdvertisingMode;


/**
 * Get an instance from {@link BleManager#getServer()}. Wrapper for functionality exposed by {@link BluetoothGattServer}. For OS levels less than 5.0, this
 * is only useful by piggybacking on an existing {@link BleDevice} that is currently {@link BleDeviceState#BLE_CONNECTED}.
 * For OS levels 5.0 and up a {@link BleServer} is capable of acting as an independent peripheral.
 */
public final class BleServer extends BleNode
{
	/**
	 * Special value that is used in place of Java's built-in <code>null</code>.
	 */
	@Immutable
	public static final BleServer NULL = new BleServer(P_Bridge_Internal.NULL_SERVER());

	static final OutgoingListener NULL_OUTGOING_LISTENER = e -> {
    };


	private final P_BleServerImpl m_serverImpl;


	/*package*/ BleServer(final IBleServer serverImpl)
	{
		super(serverImpl);
		m_serverImpl = (P_BleServerImpl) serverImpl;
	}


	final IBleServer getIBleServer()
	{
		return m_serverImpl;
	}


	/**
	 * Optionally sets overrides for any custom options given to {@link BleManager#get(android.content.Context, BleManagerConfig)}
	 * for this individual server.
	 */
	public final void setConfig(final BleNodeConfig config_nullable)
	{
		m_serverImpl.setConfig(config_nullable);
	}

	/**
	 * Set a listener here to be notified whenever this server's state changes in relation to a specific client.
	 */
	public final void setListener_State(@Nullable(Nullable.Prevalence.NORMAL) final ServerStateListener listener_nullable)
	{
		m_serverImpl.setListener_State(listener_nullable);
	}

	/**
	 * Set a listener here to override any listener provided previously.
	 */
	public final void setListener_Incoming(@Nullable(Nullable.Prevalence.NORMAL) final IncomingListener listener_nullable)
	{
		m_serverImpl.setListener_Incoming(listener_nullable);
	}

	/**
	 * Set a listener here to override any listener provided previously and provide a default backup that will be called
	 * after any listener provided to {@link #addService(BleService, AddServiceListener)}.
	 */
	public final void setListener_ServiceAdd(@Nullable(Nullable.Prevalence.NORMAL) final AddServiceListener listener_nullable)
	{
		m_serverImpl.setListener_ServiceAdd(listener_nullable);
	}

	public final void setListener_Advertising(@Nullable(Nullable.Prevalence.NORMAL) final AdvertisingListener listener_nullable)
	{
		m_serverImpl.setListener_Advertising(listener_nullable);
	}

	public final @Nullable(Nullable.Prevalence.RARE)
	AdvertisingListener getListener_Advertise()
	{
		return m_serverImpl.getListener_Advertise();
	}

	/**
	 * Returns the listener provided to {@link #setListener_Incoming(IncomingListener)}.
	 */
	public final @Nullable(Nullable.Prevalence.RARE) IncomingListener getListener_Incoming()
	{
		return m_serverImpl.getListener_Incoming();
	}

	/**
	 * This is a default catch-all convenience listener that will be called after any listener provided through
	 * the static methods of {@link IncomingListener.Please} such as {@link IncomingListener.Please#respondWithSuccess(OutgoingListener)}.
	 *
	 * @see BleManager#setListener_Outgoing(OutgoingListener)
	 */
	public final void setListener_Outgoing(final OutgoingListener listener)
	{
		m_serverImpl.setListener_Outgoing(listener);
	}

	/**
	 * Set a listener here to override any listener provided previously.
	 */
	public final void setListener_ReconnectFilter(final ServerReconnectFilter listener)
	{
		m_serverImpl.setListener_ReconnectFilter(listener);
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
		return m_serverImpl.sendIndication(macAddress, serviceUuid, charUuid, futureData, listener);
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

	/**
	 * Use this method to send a notification to the client device with the given mac address to the given characteristic {@link UUID}.
	 * If there is any kind of "early-out" issue then this method will return a {@link OutgoingListener.OutgoingEvent} in addition
	 * to passing it through the listener. Otherwise this method will return an instance with {@link OutgoingListener.OutgoingEvent#isNull()} being
	 * <code>true</code>.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) OutgoingListener.OutgoingEvent sendNotification(final String macAddress, UUID serviceUuid, UUID charUuid, final FutureData futureData, OutgoingListener listener)
	{
		return m_serverImpl.sendNotification(macAddress, serviceUuid, charUuid, futureData, listener);
	}

	/**
	 * Checks to see if the device is running an Android OS which supports
	 * advertising. This is forwarded from {@link BleManager#isAdvertisingSupportedByAndroidVersion()}.
	 */
	public final boolean isAdvertisingSupportedByAndroidVersion()
	{
		return m_serverImpl.isAdvertisingSupportedByAndroidVersion();
	}

	/**
	 * Checks to see if the device supports advertising. This is forwarded from {@link BleManager#isAdvertisingSupportedByChipset()}.
	 */
	public final boolean isAdvertisingSupportedByChipset()
	{
		return m_serverImpl.isAdvertisingSupportedByChipset();
	}

	/**
	 * Checks to see if the device supports advertising BLE services. This is forwarded from {@link BleManager#isAdvertisingSupported()}.
	 */
	public final boolean isAdvertisingSupported()
	{
		return m_serverImpl.isAdvertisingSupported();
	}

	/**
	 * Checks to see if the device is currently advertising.
	 */
	public final boolean isAdvertising()
	{
		return m_serverImpl.isAdvertising();
	}

	/**
	 * Checks to see if the device is currently advertising the given {@link UUID}.
	 */
	public final boolean isAdvertising(UUID serviceUuid)
	{
		return m_serverImpl.isAdvertising(serviceUuid);
	}

	/**
	 * Overload of {@link #startAdvertising(BleScanRecord)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) AdvertisingListener.AdvertisingEvent startAdvertising(UUID serviceUuid)
	{
		return startAdvertising(new BleScanRecord(serviceUuid));
	}

	/**
	 * Overload of {@link #startAdvertising(BleScanRecord, AdvertisingListener)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) AdvertisingListener.AdvertisingEvent startAdvertising(UUID serviceUuid, AdvertisingListener listener)
	{
		return startAdvertising(new BleScanRecord(serviceUuid), listener);
	}

	/**
	 * Overload of {@link #startAdvertising(BleScanRecord)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) AdvertisingListener.AdvertisingEvent startAdvertising(UUID[] serviceUuids)
	{
		return startAdvertising(new BleScanRecord(serviceUuids));
	}

	/**
	 * Overload of {@link #startAdvertising(BleScanRecord, AdvertisingListener)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) AdvertisingListener.AdvertisingEvent startAdvertising(UUID[] serviceUuids, AdvertisingListener listener)
	{
		return startAdvertising(new BleScanRecord(serviceUuids), listener);
	}

	/**
	 * Overload of {@link #startAdvertising(BleScanRecord)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) AdvertisingListener.AdvertisingEvent startAdvertising(UUID serviceUuid, byte[] serviceData)
	{
		return startAdvertising(new BleScanRecord(serviceUuid, serviceData));
	}

	/**
	 * Overload of {@link #startAdvertising(BleScanRecord)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) AdvertisingListener.AdvertisingEvent startAdvertising(UUID serviceUuid, byte[] serviceData, BleScanRecord.Option... options)
	{
		return startAdvertising(new BleScanRecord(serviceUuid, serviceData, options));
	}

	/**
	 * Overload of {@link #startAdvertising(BleScanRecord)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) AdvertisingListener.AdvertisingEvent startAdvertising(UUID serviceUuid, BleScanRecord.Option... options)
	{
		return startAdvertising(new BleScanRecord(serviceUuid, options));
	}

	/**
	 * Overload of {@link #startAdvertising(BleScanRecord)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) AdvertisingListener.AdvertisingEvent startAdvertising(UUID[] serviceUuids, BleScanRecord.Option... options)
	{
		return startAdvertising(new BleScanRecord(serviceUuids, options));
	}

	/**
	 * Overload of {@link #startAdvertising(BleScanRecord, BleAdvertisingSettings, AdvertisingListener)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) AdvertisingListener.AdvertisingEvent startAdvertising(UUID serviceUuid, BleAdvertisingSettings settings, AdvertisingListener listener)
	{
		return startAdvertising(new BleScanRecord(serviceUuid), settings, listener);
	}

	/**
	 * Overload of {@link #startAdvertising(BleScanRecord, BleAdvertisingSettings, AdvertisingListener)}.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) AdvertisingListener.AdvertisingEvent startAdvertising(UUID[] serviceUuids, BleAdvertisingSettings settings, AdvertisingListener listener)
	{
		return startAdvertising(new BleScanRecord(serviceUuids), settings, listener);
	}

	/**
	 * Overload of {@link #startAdvertising(BleScanRecord, BleAdvertisingSettings, AdvertisingListener)}. This sets
	 * the {@link BleAdvertisingMode} to {@link BleAdvertisingMode#AUTO}, and {@link BleTransmissionPower} to {@link BleTransmissionPower#MEDIUM}, and
	 * no timeout for the advertisement.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) AdvertisingListener.AdvertisingEvent startAdvertising(BleScanRecord advPacket)
	{
		return startAdvertising(advPacket, null);
	}

	/**
	 * Overload of {@link #startAdvertising(BleScanRecord, BleAdvertisingSettings, AdvertisingListener)}. This sets
	 * the {@link BleAdvertisingMode} to {@link BleAdvertisingMode#AUTO}, and {@link BleTransmissionPower} to {@link BleTransmissionPower#MEDIUM}, and
	 * no timeout for the advertisement.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) AdvertisingListener.AdvertisingEvent startAdvertising(BleScanRecord advPacket, AdvertisingListener listener)
	{
		return startAdvertising(advPacket, new BleAdvertisingSettings(BleAdvertisingMode.AUTO, BleTransmissionPower.MEDIUM, Interval.ZERO), listener);
	}

	/**
	 * Starts advertising serviceUuids with the information supplied in {@link BleScanRecord}. Note that this will
	 * only work for devices on Lollipop, or above. Even then, not every device supports advertising. Use
	 * {@link BleManager#isAdvertisingSupported()} to check to see if the phone supports it.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) AdvertisingListener.AdvertisingEvent startAdvertising(BleScanRecord advertisePacket, BleAdvertisingSettings settings, AdvertisingListener listener)
	{
		return m_serverImpl.startAdvertising(advertisePacket, settings, listener);
	}

	/**
	 * Stops the server from advertising.
	 */
	public final void stopAdvertising()
	{
		m_serverImpl.stopAdvertising();
	}

	/**
	 * Returns the name this {@link BleServer} is using (and will be advertised as, if applicable).
	 */
	public final String getName()
	{
		return m_serverImpl.getName();
	}

	/**
	 * Set the name you wish this {@link BleServer} to be known as. This will affect how other devices see this server, and sets the name
	 * on the lower level {@link BluetoothAdapter}. If you DO change this, please be aware this will affect everything, including apps outside
	 * of your own. It's probably best NOT to use this, but it's here for flexibility.
	 */
	@Advanced
	public final void setName(String name)
	{
		m_serverImpl.setName(name);
	}

    /**
     * Provides just-in-case lower-level access to the native server instance.
     * See similar warning for {@link BleDevice#getNative()}.
     */
    @Advanced
    public final @Nullable(Nullable.Prevalence.RARE) BluetoothGattServer getNative()
    {
        return m_serverImpl.getNative();
    }

	/**
	 * Provides just-in-case access to the abstracted server instance.
	 * See similar warning for {@link BleDevice#getNative()}.
	 */
	@Advanced
	public final @Nullable(Nullable.Prevalence.RARE) IBluetoothServer getNativeLayer()
	{
		return m_serverImpl.getNativeLayer();
	}

	/**
	 * Returns the bitwise state mask representation of {@link BleServerState} for the given client mac address.
	 *
	 * @see BleServerState
	 */
	@Advanced
	public final int getStateMask(final String macAddress)
	{
		return m_serverImpl.getStateMask(macAddress);
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
	 * Overload of {@link #connect(String, ServerConnectListener, ServerReconnectFilter)} with no listeners.
	 */
	public final ServerReconnectFilter.ConnectFailEvent connect(final String macAddress)
	{
		return connect(macAddress, null, null);
	}

	/**
	 * Overload of {@link #connect(String, ServerConnectListener, ServerReconnectFilter)} with only one listener.
	 */
	public final ServerReconnectFilter.ConnectFailEvent connect(final String macAddress, final ServerConnectListener connectListener)
	{
		return connect(macAddress, connectListener, null);
	}

	/**
	 * Overload of {@link #connect(String, ServerConnectListener, ServerReconnectFilter)} with only one listener.
	 */
	public final ServerReconnectFilter.ConnectFailEvent connect(final String macAddress, final ServerReconnectFilter connectionFailListener)
	{
		return connect(macAddress, null, connectionFailListener);
	}

	/**
	 * Connect to the given client mac address and provided listeners that are shorthand for calling {@link #setListener_State(ServerStateListener)}
	 * {@link #setListener_ReconnectFilter(ServerReconnectFilter)}.
	 */
	public final ServerReconnectFilter.ConnectFailEvent connect(final String macAddress, final ServerConnectListener connectListener, final ServerReconnectFilter connectionFailListener)
	{
		return m_serverImpl.connect(macAddress, connectListener, connectionFailListener);
	}

	public final boolean disconnect(final String macAddress)
	{
		return m_serverImpl.disconnect(macAddress);
	}

	/**
	 * Disconnects this server completely, disconnecting all connected clients and shutting things down.
	 * To disconnect individual clients use {@link #disconnect(String)}.
	 */
	public final void disconnect()
	{
		m_serverImpl.disconnect();
	}

	@Override public final boolean isNull()
	{
		return m_serverImpl.isNull();
	}

	/**
	 * Does a referential equality check on the two servers.
	 */
	public final boolean equals(@Nullable(Nullable.Prevalence.NORMAL) final BleServer server_nullable)
	{
		return m_serverImpl.equals(server_nullable.m_serverImpl);
	}

	/**
	 * Returns {@link #equals(BleServer)} if object is an instance of {@link BleServer}. Otherwise calls super.
	 *
	 * @see BleServer#equals(BleServer)
	 */
	@Override public final boolean equals(@Nullable(Nullable.Prevalence.NORMAL) final Object object_nullable)
	{
		return m_serverImpl.equals(object_nullable);
	}

	/**
	 * Overload of {@link #addService(BleService, AddServiceListener)} without the listener.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) AddServiceListener.ServiceAddEvent addService(final BleService service)
	{
		return this.addService(service, null);
	}

	/**
	 * Starts the process of adding a service to this server. The provided listener will be called when the service is added or there is a problem.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) AddServiceListener.ServiceAddEvent addService(final BleService service, final AddServiceListener listener)
	{
		return m_serverImpl.addService(service, listener);
	}

	/**
	 * Remove any service previously provided to {@link #addService(BleService, AddServiceListener)} or overloads. This can be safely called
	 * even if the call to {@link #addService(BleService, AddServiceListener)} hasn't resulted in a callback to the provided listener yet, in which
	 * case it will be called with {@link AddServiceListener.Status#CANCELLED_FROM_REMOVAL}.
	 */
	public final @Nullable(Nullable.Prevalence.NORMAL) BleService removeService(final UUID serviceUuid)
	{
		return m_serverImpl.removeService(serviceUuid);
	}

	/**
	 * Convenience to remove all services previously added with {@link #addService(BleService, AddServiceListener)} (or overloads). This is slightly more performant too.
	 */
	public final void removeAllServices()
	{
		m_serverImpl.removeAllServices();
	}

	/**
	 * Offers a more "functional" means of iterating through the internal list of clients instead of
	 * using {@link #getClients()} or {@link #getClients_List()}.
	 */
	public final void getClients(final ForEach_Void<String> forEach)
	{
		m_serverImpl.getClients(forEach);
	}

	/**
	 * Same as {@link #getClients(ForEach_Void)} but will only return clients
	 * in the given state provided.
	 */
	public final void getClients(final ForEach_Void<String> forEach, final BleServerState state)
	{
		m_serverImpl.getClients(forEach, state);
	}

	/**
	 * Same as {@link #getClients(ForEach_Void)} but will only return clients
	 * in any of the given states provided.
	 */
	public final void getClients(final ForEach_Void<String> forEach, final BleServerState ... states)
	{
		m_serverImpl.getClients(forEach, states);
	}

	/**
	 * Overload of {@link #getClients(ForEach_Void)}
	 * if you need to break out of the iteration at any point.
	 */
	public final void getClients(final ForEach_Breakable<String> forEach)
	{
		m_serverImpl.getClients(forEach);
	}

	/**
	 * Overload of {@link #getClients(ForEach_Void, BleServerState)}
	 * if you need to break out of the iteration at any point.
	 */
	public final void getClients(final ForEach_Breakable<String> forEach, final BleServerState state)
	{
		m_serverImpl.getClients(forEach, state);
	}

	/**
	 * Same as {@link #getClients(ForEach_Breakable)} but will only return clients
	 * in any of the given states provided.
	 */
	public final void getClients(final ForEach_Breakable<String> forEach, final BleServerState ... states)
	{
		m_serverImpl.getClients(forEach, states);
	}

	/**
	 * Returns all the clients connected or connecting (or previously so) to this server.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) Iterator<String> getClients()
	{
		return m_serverImpl.getClients();
	}

	/**
	 * Returns all the clients connected or connecting (or previously so) to this server.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) Iterator<String> getClients(final BleServerState state)
	{
		return m_serverImpl.getClients(state);
	}

	/**
	 * Returns all the clients connected or connecting (or previously so) to this server.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) Iterator<String> getClients(final BleServerState ... states)
	{
		return m_serverImpl.getClients(states);
	}

	/**
	 * Overload of {@link #getClients()} that returns a {@link java.util.List} for you.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) List<String> getClients_List()
	{
		return m_serverImpl.getClients_List();
	}

	/**
	 * Overload of {@link #getClients(BleServerState)} that returns a {@link java.util.List} for you.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) List<String> getClients_List(final BleServerState state)
	{
		return m_serverImpl.getClients_List(state);
	}

	/**
	 * Overload of {@link #getClients(BleServerState[])} that returns a {@link java.util.List} for you.
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) List<String> getClients_List(final BleServerState ... states)
	{
		return m_serverImpl.getClients_List(states);
	}

	/**
	 * Returns the total number of clients this server is connecting or connected to (or previously so).
	 */
	public final int getClientCount()
	{
		return m_serverImpl.getClientCount();
	}

	/**
	 * Returns the number of clients that are in the current state.
	 */
	public final int getClientCount(final BleServerState state)
	{
		return m_serverImpl.getClientCount(state);
	}

	/**
	 * Returns the number of clients that are in any of the given states.
	 */
	public final int getClientCount(final BleServerState ... states)
	{
		return m_serverImpl.getClientCount(states);
	}

	/**
	 * Returns <code>true</code> if this server has any connected or connecting clients (or previously so).
	 */
	public final boolean hasClients()
	{
		return getClientCount() > 0;
	}

	/**
	 * Returns <code>true</code> if this server has any clients in the given state.
	 */
	public final boolean hasClient(final BleServerState state)
	{
		return getClientCount(state) > 0;
	}

	/**
	 * Returns <code>true</code> if this server has any clients in any of the given states.
	 */
	public final boolean hasClient(final BleServerState ... states)
	{
		return getClientCount(states) > 0;
	}

	/**
	 * Pretty-prints the list of connecting or connected clients.
	 */
	public final String toString()
	{
		return m_serverImpl.toString();
	}

	/**
	 * Returns the local mac address provided by {@link BluetoothAdapter#getAddress()}.
	 */
	@Override public final @Nullable(Nullable.Prevalence.NEVER) String getMacAddress()
	{
		return m_serverImpl.getMacAddress();
	}
}
