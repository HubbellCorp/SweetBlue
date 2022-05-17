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


import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import com.idevicesinc.sweetblue.annotations.Advanced;
import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.internal.IBleNode;
import com.idevicesinc.sweetblue.utils.EpochTime;
import com.idevicesinc.sweetblue.utils.ForEach_Breakable;
import com.idevicesinc.sweetblue.utils.ForEach_Void;
import com.idevicesinc.sweetblue.utils.HistoricalData;
import com.idevicesinc.sweetblue.utils.HistoricalDataColumn;
import com.idevicesinc.sweetblue.utils.HistoricalDataQuery;
import com.idevicesinc.sweetblue.utils.UsesCustomNull;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;


/**
 * Abstract base class for {@link BleDevice} and {@link BleServer}, mostly just to statically tie their APIs together
 * wherever possible. That is, not much actual shared implementation exists in this class as of this writing.
 */
public abstract class BleNode implements UsesCustomNull
{
    /**
     * Field for app to associate any data it wants with instances of this class
     * instead of having to subclass or manage associative hash maps or something.
     * The library does not touch or interact with this data in any way.
     *
     * @see BleManager#appData
     * @see BleServer#appData
     */
    @SuppressWarnings("squid:ClassVariableVisibilityCheck")
    public Object appData;

    private final IBleNode m_nodeImpl;


    BleNode(IBleNode impl)
    {
        m_nodeImpl = impl;
    }


    final IBleNode getIBleNode()
    {
        return m_nodeImpl;
    }


    /**
     * Overload of {@link #getNativeBleDescriptor(UUID, UUID, UUID)} that will return the first descriptor we find
     * matching the given {@link UUID}.
     * <p>
     * Note that this will never return a <code>null</code> instance. You need to call {@link BleDescriptor#isNull()} to check if the {@link BluetoothGattDescriptor}
     * actually exists (in other words, it will return <code>true</code> if we were unable to find the requested descriptor).
     */
    public @Nullable(Nullable.Prevalence.NEVER) BleDescriptor getNativeBleDescriptor(final UUID descUuid)
    {
        return getNativeBleDescriptor(null, null, descUuid);
    }

    /**
     * Overload of {@link #getNativeBleDescriptor(UUID, UUID, UUID)} that will return the first descriptor we find
     * inside the given characteristic matching the given {@link UUID}.
     * <p>
     * Note that this will never return a <code>null</code> instance. You need to call {@link BleDescriptor#isNull()} to check if the {@link BluetoothGattDescriptor}
     * actually exists (in other words, it will return <code>true</code> if we were unable to find the requested descriptor).
     */
    public @Nullable(Nullable.Prevalence.NEVER) BleDescriptor getNativeBleDescriptor_inChar(final UUID charUuid, final UUID descUuid)
    {
        return getNativeBleDescriptor(null, charUuid, descUuid);
    }

    /**
     * Overload of {@link #getNativeBleDescriptor(UUID, UUID, UUID)} that will return the first descriptor we find
     * inside the given service matching the given {@link UUID}.
     * <p>
     * Note that this will never return a <code>null</code> instance. You need to call {@link BleDescriptor#isNull()} to check if the {@link BluetoothGattDescriptor}
     * actually exists (in other words, it will return <code>true</code> if we were unable to find the requested descriptor).
     */
    public @Nullable(Nullable.Prevalence.NEVER) BleDescriptor getNativeBleDescriptor_inService(final UUID serviceUuid, final UUID descUuid)
    {
        return getNativeBleDescriptor(serviceUuid, null, descUuid);
    }

    /**
     * Returns the {@link BleDescriptor} for the given UUID in case you need lower-level access.
     * <p>
     * Note that this will never return a <code>null</code> instance. You need to call {@link BleDescriptor#isNull()} to check if the {@link BluetoothGattDescriptor}
     * actually exists (in other words, it will return <code>true</code> if we were unable to find the requested descriptor).
     */
    public @Nullable(Nullable.Prevalence.NEVER) BleDescriptor getNativeBleDescriptor(final UUID serviceUuid, final UUID charUuid, final UUID descUuid)
    {
        return m_nodeImpl.getNativeBleDescriptor(serviceUuid, charUuid, descUuid);
    }

    /**
     * Returns the native characteristic for the given UUID in case you need lower-level access.
     * <p>
     * Note that this will never return a <code>null</code> instance. You need to call {@link BleCharacteristic#isNull()} to check if the {@link BluetoothGattCharacteristic}
     * actually exists (in other words, it will return <code>true</code> if we were unable to find the requested characteristic).
     */
    @com.idevicesinc.sweetblue.annotations.Advanced public @Nullable(Nullable.Prevalence.NEVER) BleCharacteristic getNativeBleCharacteristic(final UUID charUuid)
    {
        return getNativeBleCharacteristic(null, charUuid);
    }

    /**
     * Overload of {@link #getNativeBleCharacteristic(UUID)} for when you have characteristics with identical uuids under different services.
     * <p>
     * Note that this will never return a <code>null</code> instance. You need to call {@link BleCharacteristic#isNull()} to check if the {@link BluetoothGattCharacteristic}
     * actually exists (in other words, it will return <code>true</code> if we were unable to find the requested characteristic).
     */
    public @Nullable(Nullable.Prevalence.NEVER) BleCharacteristic getNativeBleCharacteristic(final UUID serviceUuid, final UUID charUuid)
    {
        return m_nodeImpl.getNativeBleCharacteristic(serviceUuid, charUuid);
    }

    /**
     * Overload of {@link #getNativeBleCharacteristic(UUID, UUID)} for when you have characteristics with identical uuids within the same service.
     */
    public @Nullable(Nullable.Prevalence.NORMAL) BleCharacteristic getNativeCharacteristic(final UUID serviceUuid, final UUID charUuid, final DescriptorFilter descriptorFilter)
    {
        return m_nodeImpl.getNativeBleCharacteristic(serviceUuid, charUuid, descriptorFilter);
    }

    /**
     * Returns the native service for the given UUID in case you need lower-level access.
     * <p>
     * Note that this will never return a <code>null</code> instance. You need to call {@link BleService#isNull()} to check if the {@link BluetoothGattService}
     * actually exists (in other words, it will return <code>true</code> if we were unable to find the requested service).
     */
    @Advanced
    public @Nullable(Nullable.Prevalence.NEVER) BleService getNativeBleService(final UUID serviceUuid)
    {
        return m_nodeImpl.getNativeBleService(serviceUuid);
    }

    /**
     * Returns all {@link BluetoothGattService} instances.
     */
    @Advanced public @Nullable(Nullable.Prevalence.NEVER) Iterator<BleService> getNativeServices()
    {
        return m_nodeImpl.getNativeServices();
    }

    /**
     * Convenience overload of {@link #getNativeServices()} that returns a {@link List}.
     */
    @Advanced public @Nullable(Nullable.Prevalence.NEVER) List<BleService> getNativeServices_List()
    {
        return m_nodeImpl.getNativeServices_List();
    }

    /**
     * Overload of {@link #getNativeServices()} that uses a for each construct instead of returning an iterator.
     */
    public void getNativeServices(final ForEach_Void<BleService> forEach)
    {
        m_nodeImpl.getNativeServices(forEach);
    }

    /**
     * Overload of {@link #getNativeServices()} that uses a for each construct instead of returning an iterator.
     */
    public void getNativeServices(final ForEach_Breakable<BleService> forEach)
    {
        m_nodeImpl.getNativeServices(forEach);
    }

    /**
     * Overload of {@link #getNativeCharacteristics()} that uses a for each construct instead of returning an iterator.
     */
    public void getNativeCharacteristics(final ForEach_Void<BleCharacteristic> forEach)
    {
        getNativeCharacteristics(null, forEach);
    }

    /**
     * Overload of {@link #getNativeCharacteristics()} that uses a for each construct instead of returning an iterator.
     */
    public void getNativeCharacteristics(final ForEach_Breakable<BleCharacteristic> forEach)
    {
        getNativeCharacteristics(null, forEach);
    }

    /**
     * Overload of {@link #getNativeCharacteristics(UUID)} that uses a for each construct instead of returning an iterator.
     */
    public void getNativeCharacteristics(final UUID serviceUuid, final ForEach_Void<BleCharacteristic> forEach)
    {
        m_nodeImpl.getNativeCharacteristics(serviceUuid, forEach);
    }

    /**
     * Overload of {@link #getNativeCharacteristics(UUID)} that uses a for each construct instead of returning an iterator.
     */
    public void getNativeCharacteristics(final UUID serviceUuid, final ForEach_Breakable<BleCharacteristic> forEach)
    {
        m_nodeImpl.getNativeCharacteristics(serviceUuid, forEach);
    }

    /**
     * Returns all {@link BluetoothGattCharacteristic} instances.
     */
    public @Nullable(Nullable.Prevalence.NEVER) Iterator<BleCharacteristic> getNativeCharacteristics()
    {
        return m_nodeImpl.getNativeCharacteristics(null);
    }

    /**
     * Convenience overload of {@link #getNativeCharacteristics()} that returns a {@link List}.
     */
    public @Nullable(Nullable.Prevalence.NEVER) List<BleCharacteristic> getNativeCharacteristics_List()
    {
        return m_nodeImpl.getNativeCharacteristics_List(null);
    }

    /**
     * Same as {@link #getNativeCharacteristics()} but you can filter on the service {@link UUID}.
     */
    public @Nullable(Nullable.Prevalence.NEVER) Iterator<BleCharacteristic> getNativeCharacteristics(UUID serviceUuid)
    {
        return m_nodeImpl.getNativeCharacteristics(serviceUuid);
    }

    /**
     * Convenience overload of {@link #getNativeCharacteristics(UUID)} that returns a {@link List}.
     */
    public @Nullable(Nullable.Prevalence.NEVER) List<BleCharacteristic> getNativeCharacteristics_List(UUID serviceUuid)
    {
        return m_nodeImpl.getNativeCharacteristics_List(serviceUuid);
    }

    /**
     * Returns all descriptors on this node.
     */
    public @Nullable(Nullable.Prevalence.NEVER) Iterator<BleDescriptor> getNativeDescriptors()
    {
        return m_nodeImpl.getNativeDescriptors(null, null);
    }

    /**
     * Returns all descriptors on this node as a list.
     */
    public @Nullable(Nullable.Prevalence.NEVER) List<BleDescriptor> getNativeDescriptors_List()
    {
        return m_nodeImpl.getNativeDescriptors_List(null, null);
    }

    /**
     * Returns all descriptors on this node in the given service.
     */
    public @Nullable(Nullable.Prevalence.NEVER) Iterator<BleDescriptor> getNativeDescriptors_inService(final UUID serviceUuid)
    {
        return m_nodeImpl.getNativeDescriptors(serviceUuid, null);
    }

    /**
     * Returns all descriptors on this node in the given service as a list.
     */
    public @Nullable(Nullable.Prevalence.NEVER) List<BleDescriptor> getNativeDescriptors_inService_List(final UUID serviceUuid)
    {
        return m_nodeImpl.getNativeDescriptors_List(serviceUuid, null);
    }

    /**
     * Returns all descriptors on this node in the given characteristic.
     */
    public @Nullable(Nullable.Prevalence.NEVER) Iterator<BleDescriptor> getNativeDescriptors_inChar(final UUID charUuid)
    {
        return m_nodeImpl.getNativeDescriptors(null, charUuid);
    }

    /**
     * Returns all descriptors on this node in the given characteristic as a list.
     */
    public @Nullable(Nullable.Prevalence.NEVER) List<BleDescriptor> getNativeDescriptors_inChar_List(final UUID charUuid)
    {
        return m_nodeImpl.getNativeDescriptors_List(null, charUuid);
    }

    /**
     * Returns all descriptors on this node in the given characteristic.
     */
    public @Nullable(Nullable.Prevalence.NEVER) Iterator<BleDescriptor> getNativeDescriptors(final UUID serviceUuid, final UUID charUuid)
    {
        return m_nodeImpl.getNativeDescriptors(serviceUuid, charUuid);
    }

    /**
     * Returns all descriptors on this node in the given characteristic as a list.
     */
    public @Nullable(Nullable.Prevalence.NEVER) List<BleDescriptor> getNativeDescriptors_List(final UUID serviceUuid, final UUID charUuid)
    {
        return m_nodeImpl.getNativeDescriptors_List(serviceUuid, charUuid);
    }

    /**
     * Overload of {@link BleNode#getNativeDescriptors()} using a for each construct.
     */
    public void getNativeDescriptors(final ForEach_Void<BleDescriptor> forEach)
    {
        getNativeDescriptors(null, null, forEach);
    }

    /**
     * Overload of {@link BleNode#getNativeDescriptors()} using a for each construct.
     */
    public void getNativeDescriptors(final ForEach_Breakable<BleDescriptor> forEach)
    {
        getNativeDescriptors(null, null, forEach);
    }

    /**
     * Overload of {@link BleNode#getNativeDescriptors_inService(UUID)} using a for each construct.
     */
    public void getNativeDescriptors_inService(final UUID serviceUuid, final ForEach_Void<BleDescriptor> forEach)
    {
        getNativeDescriptors(serviceUuid, null, forEach);
    }

    /**
     * Overload of {@link BleNode#getNativeDescriptors_inService(UUID)} using a for each construct.
     */
    public void getNativeDescriptors_inService(final UUID serviceUuid, final ForEach_Breakable<BleDescriptor> forEach)
    {
        getNativeDescriptors(serviceUuid, null, forEach);
    }

    /**
     * Overload of {@link BleNode#getNativeDescriptors_inChar(UUID)} using a for each construct.
     */
    public void getNativeDescriptors_inChar(final UUID charUuid, final ForEach_Void<BleDescriptor> forEach)
    {
        getNativeDescriptors(null, charUuid, forEach);
    }

    /**
     * Overload of {@link BleNode#getNativeDescriptors_inChar(UUID)} using a for each construct.
     */
    public void getNativeDescriptors_inChar(final UUID charUuid, final ForEach_Breakable<BleDescriptor> forEach)
    {
        getNativeDescriptors(null, charUuid, forEach);
    }

    /**
     * Overload of {@link BleNode#getNativeDescriptors(UUID, UUID)} using a for each construct.
     */
    public void getNativeDescriptors(final UUID serviceUuid, final UUID charUuid, final ForEach_Void<BleDescriptor> forEach)
    {
        m_nodeImpl.getNativeDescriptors(serviceUuid, charUuid, forEach);
    }

    /**
     * Overload of {@link BleNode#getNativeDescriptors(UUID, UUID)} using a for each construct.
     */
    public void getNativeDescriptors(final UUID serviceUuid, final UUID charUuid, final ForEach_Breakable<BleDescriptor> forEach)
    {
        m_nodeImpl.getNativeDescriptors(serviceUuid, charUuid, forEach);
    }

    /**
     * Returns a new {@link com.idevicesinc.sweetblue.utils.HistoricalData} instance using
     * {@link com.idevicesinc.sweetblue.BleDeviceConfig#historicalDataFactory} if available.
     */
    @com.idevicesinc.sweetblue.annotations.Advanced public HistoricalData newHistoricalData(final byte[] data, final EpochTime epochTime)
    {
        return m_nodeImpl.newHistoricalData(data, epochTime);
    }

    /**
     * Returns this endpoint's manager.
     */
    public BleManager getManager()
    {
        return BleManager.s_instance;
    }

    /**
     * Convenience method that casts {@link #appData} for you.
     */
    public <T> T appData()
    {
        return (T) appData;
    }

    /**
     * Provides a means to perform a raw SQL query on the database storing the historical data for this node. Use {@link BleDevice#getHistoricalDataTableName(UUID)}
     * to generate table names and {@link HistoricalDataColumn} to get column names.
     */
    public @Nullable(Nullable.Prevalence.NEVER) HistoricalDataQueryListener.HistoricalDataQueryEvent queryHistoricalData(final String query)
    {
        return m_nodeImpl.queryHistoricalData(query);
    }

    /**
     * Same as {@link #queryHistoricalData(String)} but performs the query on a background thread and returns the result back on the main thread
     * through the provided {@link HistoricalDataQueryListener}.
     */
    public void queryHistoricalData(final String query, final HistoricalDataQueryListener listener)
    {
        m_nodeImpl.queryHistoricalData(query, listener);
    }

    /**
     * Provides a way to perform a statically checked SQL query by chaining method calls.
     */
    @com.idevicesinc.sweetblue.annotations.Advanced @com.idevicesinc.sweetblue.annotations.Alpha public @Nullable(Nullable.Prevalence.NEVER) HistoricalDataQuery.Part_Select select()
    {
        return m_nodeImpl.select();
    }

    /**
     * Just some sugar for casting to subclasses.
     */
    public <T extends BleNode> T cast()
    {
        return (T) this;
    }

    /**
     * Safer version of {@link #cast()} that will return {@link BleDevice#NULL} or {@link BleServer#NULL}
     * if the cast cannot be made.
     */
    public @Nullable(Nullable.Prevalence.NEVER) <T extends BleNode> T cast(final Class<T> type)
    {
        if( this instanceof BleDevice && type == BleServer.class )
        {
            return (T) BleServer.NULL;
        }
        else if( this instanceof BleServer && type == BleDevice.class )
        {
            return (T) BleDevice.NULL;
        }
        else
        {
            return cast();
        }
    }

    /**
     * Returns the MAC address of the remote {@link BleDevice} or local {@link BleServer}.
     */
    public abstract @Nullable(Nullable.Prevalence.NEVER) String getMacAddress();
}
