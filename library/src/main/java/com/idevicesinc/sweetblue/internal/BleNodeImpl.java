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


import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.database.Cursor;
import com.idevicesinc.sweetblue.BleCharacteristic;
import com.idevicesinc.sweetblue.BleDescriptor;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDeviceConfig;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.BleNode;
import com.idevicesinc.sweetblue.BleServer;
import com.idevicesinc.sweetblue.BleService;
import com.idevicesinc.sweetblue.DescriptorFilter;
import com.idevicesinc.sweetblue.HistoricalDataQueryListener;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.utils.EmptyCursor;
import com.idevicesinc.sweetblue.utils.EpochTime;
import com.idevicesinc.sweetblue.utils.ForEach_Breakable;
import com.idevicesinc.sweetblue.utils.ForEach_Void;
import com.idevicesinc.sweetblue.utils.HistoricalData;
import com.idevicesinc.sweetblue.utils.HistoricalDataQuery;
import com.idevicesinc.sweetblue.utils.UsesCustomNull;
import com.idevicesinc.sweetblue.utils.Uuids;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;


abstract class BleNodeImpl implements IBleNode, UsesCustomNull
{
    private final IBleManager m_manager;

    //--- DRK > Can't be final cause can't reference subclass 'this' while calling super() constructor.
    private PA_ServiceManager m_serviceMngr;

    public BleNodeImpl(final IBleManager manager)
    {
        m_manager = manager;
        m_serviceMngr = newServiceManager();
    }

    public <T extends PA_ServiceManager> T getServiceManager()
    {
        return (T) m_serviceMngr;
    }

    abstract PA_ServiceManager newServiceManager();

    /**
     * Returns the {@link BleDescriptor} for the given UUID in case you need lower-level access.
     *
     * Note that this will never return a <code>null</code> instance. You need to call {@link BleDescriptor#isNull()} to check if the {@link BluetoothGattDescriptor}
     * actually exists (in other words, it will return <code>true</code> if we were unable to find the requested descriptor).
     */
    public @Nullable(Nullable.Prevalence.NEVER)
    BleDescriptor getNativeBleDescriptor(final UUID serviceUuid, final UUID charUuid, final UUID descUuid)
    {
        return m_serviceMngr.getDescriptor(serviceUuid, charUuid, descUuid);
    }

    /**
     * Returns the native characteristic for the given UUID for when you have characteristics with identical uuids under different services.
     *
     * Note that this will never return a <code>null</code> instance. You need to call {@link BleCharacteristic#isNull()} to check if the {@link BluetoothGattCharacteristic}
     * actually exists (in other words, it will return <code>true</code> if we were unable to find the requested characteristic).
     */
    public @Nullable(Nullable.Prevalence.NEVER) BleCharacteristic getNativeBleCharacteristic(final UUID serviceUuid, final UUID charUuid)
    {
        return m_serviceMngr.getCharacteristic(serviceUuid, charUuid);
    }

    public @Nullable(Nullable.Prevalence.NORMAL) BleCharacteristic getNativeBleCharacteristic(final UUID serviceUuid, final UUID charUuid, final DescriptorFilter descriptorFilter)
    {
        return m_serviceMngr.getCharacteristic(serviceUuid, charUuid, descriptorFilter);
    }

    /**
     * Returns the native service for the given UUID in case you need lower-level access.
     *
     * Note that this will never return a <code>null</code> instance. You need to call {@link BleService#isNull()} to check if the {@link BluetoothGattService}
     * actually exists (in other words, it will return <code>true</code> if we were unable to find the requested service).
     */
    @com.idevicesinc.sweetblue.annotations.Advanced
    public @Nullable(Nullable.Prevalence.NEVER) BleService getNativeBleService(final UUID serviceUuid)
    {
        return m_serviceMngr.getServiceDirectlyFromNativeNode(serviceUuid);
    }

    /**
     * Returns all {@link BluetoothGattService} instances.
     */
    @com.idevicesinc.sweetblue.annotations.Advanced
    public @Nullable(Nullable.Prevalence.NEVER) Iterator<BleService> getNativeServices()
    {
        return m_serviceMngr.getServices();
    }

    /**
     * Convenience overload of {@link #getNativeServices()} that returns a {@link List}.
     */
    @com.idevicesinc.sweetblue.annotations.Advanced
    public @Nullable(Nullable.Prevalence.NEVER) List<BleService> getNativeServices_List()
    {
        return m_serviceMngr.getServices_List();
    }

    /**
     * Overload of {@link #getNativeServices()} that uses a for each construct instead of returning an iterator.
     */
    public void getNativeServices(final ForEach_Void<BleService> forEach)
    {
        m_serviceMngr.getServices(forEach);
    }

    /**
     * Overload of {@link #getNativeServices()} that uses a for each construct instead of returning an iterator.
     */
    public void getNativeServices(final ForEach_Breakable<BleService> forEach)
    {
        m_serviceMngr.getServices(forEach);
    }

    /**
     * Overload of {@link #getNativeCharacteristics(UUID)} that uses a for each construct instead of returning an iterator.
     */
    public void getNativeCharacteristics(final UUID serviceUuid, final ForEach_Void<BleCharacteristic> forEach)
    {
        m_serviceMngr.getCharacteristics(serviceUuid, forEach);
    }

    /**
     * Overload of {@link #getNativeCharacteristics(UUID)} that uses a for each construct instead of returning an iterator.
     */
    public void getNativeCharacteristics(final UUID serviceUuid, final ForEach_Breakable<BleCharacteristic> forEach)
    {
        m_serviceMngr.getCharacteristics(serviceUuid, forEach);
    }

    /**
     * Same as {@link #getNativeCharacteristics(UUID)} but you can filter on the service {@link UUID}.
     */
    public @Nullable(Nullable.Prevalence.NEVER) Iterator<BleCharacteristic> getNativeCharacteristics(UUID serviceUuid)
    {
        return m_serviceMngr.getCharacteristics(serviceUuid);
    }

    /**
     * Convenience overload of {@link #getNativeCharacteristics(UUID)} that returns a {@link List}.
     */
    public @Nullable(Nullable.Prevalence.NEVER) List<BleCharacteristic> getNativeCharacteristics_List(UUID serviceUuid)
    {
        return m_serviceMngr.getCharacteristics_List(serviceUuid);
    }

    /**
     * Returns all descriptors on this node in the given characteristic.
     */
    public @Nullable(Nullable.Prevalence.NEVER) Iterator<BleDescriptor> getNativeDescriptors(final UUID serviceUuid, final UUID charUuid)
    {
        return m_serviceMngr.getDescriptors(serviceUuid, charUuid);
    }

    /**
     * Returns all descriptors on this node in the given characteristic as a list.
     */
    public @Nullable(Nullable.Prevalence.NEVER) List<BleDescriptor> getNativeDescriptors_List(final UUID serviceUuid, final UUID charUuid)
    {
        return m_serviceMngr.getDescriptors_List(serviceUuid, charUuid);
    }

    /**
     * Overload of {@link BleNode#getNativeDescriptors(UUID, UUID)} using a for each construct.
     */
    public void getNativeDescriptors(final UUID serviceUuid, final UUID charUuid, final ForEach_Void<BleDescriptor> forEach)
    {
        m_serviceMngr.getDescriptors(serviceUuid, charUuid, forEach);
    }

    /**
     * Overload of {@link BleNode#getNativeDescriptors(UUID, UUID)} using a for each construct.
     */
    public void getNativeDescriptors(final UUID serviceUuid, final UUID charUuid, final ForEach_Breakable<BleDescriptor> forEach)
    {
        m_serviceMngr.getDescriptors(serviceUuid, charUuid, forEach);
    }

    /**
     * Returns a new {@link com.idevicesinc.sweetblue.utils.HistoricalData} instance using
     * {@link com.idevicesinc.sweetblue.BleDeviceConfig#historicalDataFactory} if available.
     */
    @com.idevicesinc.sweetblue.annotations.Advanced
    public HistoricalData newHistoricalData(final byte[] data, final EpochTime epochTime)
    {
        final BleDeviceConfig.HistoricalDataFactory factory_device = conf_node().historicalDataFactory;
        final BleDeviceConfig.HistoricalDataFactory factory_mngr = conf_mngr().historicalDataFactory;
        final BleDeviceConfig.HistoricalDataFactory factory = factory_device != null ? factory_device : factory_mngr;

        if( factory != null )
        {
            return factory.newHistoricalData(data, epochTime);
        }
        else
        {
            return new HistoricalData(data, epochTime);
        }
    }

    /**
     * Returns this endpoint's manager.
     */
    public IBleManager getIManager()
    {
        return m_manager;
    }

    public BleManagerConfig conf_mngr()
    {
        if (getIManager() != null)
        {
            return getIManager().getConfigClone();
        }
        else
        {
            return P_Bridge_User.nullConfig();
        }
    }

    public P_TaskManager taskManager()
    {
        return getIManager().getTaskManager();
    }

    public P_Logger logger()
    {
        return getIManager().getLogger();
    }

    public @Nullable(Nullable.Prevalence.NEVER) HistoricalDataQueryListener.HistoricalDataQueryEvent queryHistoricalData(final String query)
    {
        if( this.isNull() )
        {
            BleNode node;
            if (this instanceof IBleDevice)
                node = ((IBleDevice) cast()).getBleDevice();
            else
                node = ((IBleServer) cast()).getBleServer();
            return P_Bridge_User.newHistoricalDataQueryEvent(node, Uuids.INVALID, new EmptyCursor(), HistoricalDataQueryListener.Status.NULL_ENDPOINT, query);
        }
        else
        {
            final Cursor cursor = getIManager().getHistoricalDatabase().query(query);
            BleNode node;
            if (this instanceof IBleDevice)
                node = ((IBleDevice) cast()).getBleDevice();
            else
                node = ((IBleServer) cast()).getBleServer();
            return P_Bridge_User.newHistoricalDataQueryEvent(node, Uuids.INVALID, cursor, HistoricalDataQueryListener.Status.SUCCESS, query);
        }
    }

    /**
     * Same as {@link #queryHistoricalData(String)} but performs the query on a background thread and returns the result back on the main thread
     * through the provided {@link HistoricalDataQueryListener}.
     */
    public void queryHistoricalData(final String query, final HistoricalDataQueryListener listener)
    {
        if( this.isNull() )
        {
            BleNode node;
            if (this instanceof IBleDevice)
                node = ((IBleDevice) cast()).getBleDevice();
            else
                node = ((IBleServer) cast()).getBleServer();
            HistoricalDataQueryListener.HistoricalDataQueryEvent event = P_Bridge_User.newHistoricalDataQueryEvent(node, Uuids.INVALID, new EmptyCursor(), HistoricalDataQueryListener.Status.NULL_ENDPOINT, query);
            listener.onEvent(event);
        }
        else
        {
            getIManager().getPostManager().postToUpdateThread(() -> {
                final HistoricalDataQueryListener.HistoricalDataQueryEvent e = queryHistoricalData(query);

                getIManager().postEvent(listener, e);
            });
        }
    }

    /**
     * Provides a way to perform a statically checked SQL query by chaining method calls.
     */
    @com.idevicesinc.sweetblue.annotations.Advanced
    @com.idevicesinc.sweetblue.annotations.Alpha
    public @Nullable(Nullable.Prevalence.NEVER) HistoricalDataQuery.Part_Select select()
    {
        final HistoricalDataQuery.Part_Select select = HistoricalDataQuery.select(this, getIManager().getHistoricalDatabase());

        return select;
    }

    /**
     * Just some sugar for casting to subclasses.
     */
    public <T extends BleNodeImpl> T cast()
    {
        return (T) this;
    }

    /**
     * Safer version of {@link BleNodeImpl#cast()} that will return {@link BleDevice#NULL} or {@link BleServer#NULL}
     * if the cast cannot be made.
     */
    public @Nullable(Nullable.Prevalence.NEVER) <T extends BleNodeImpl> T cast(final Class<T> type)
    {
        if( this instanceof P_BleDeviceImpl && type == P_BleServerImpl.class )
        {
            return (T) P_BleServerImpl.NULL;
        }
        else if( this instanceof P_BleServerImpl && type == P_BleDeviceImpl.class )
        {
            return (T) P_BleDeviceImpl.NULL;
        }
        else
        {
            return cast();
        }
    }

    @Override public boolean isNull()
    {
        return false;
    }

}
