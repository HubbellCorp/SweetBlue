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


import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.utils.FutureData;
import com.idevicesinc.sweetblue.utils.P_Const;
import com.idevicesinc.sweetblue.utils.PresentData;
import com.idevicesinc.sweetblue.utils.Uuids;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/**
 * Base class for basic BLE operations (read, write, notify).
 *
 * This class is parameterized so that the shared methods of this class get auto casted to the parent class (eg. BleWrite, BleRead, BleNotify), so when chaining,
 * you have access to the parent's methods that aren't contained in this one.
 */
public abstract class BleOp<T extends BleOp>
{

    final static String TAG = BleOp.class.getSimpleName();

    private List<T> opList = new ArrayList<>();

    private UUID serviceUuid = null;
    private UUID charUuid = null;
    private ReadWriteListener readWriteListener = null;
    private DescriptorFilter descriptorFilter = null;
    private FutureData m_data = P_Const.EMPTY_FUTURE_DATA;



    public BleOp()
    {
    }

    public BleOp(UUID serviceUuid, UUID characteristicUuid)
    {
        this.serviceUuid = serviceUuid;
        this.charUuid = characteristicUuid;
    }

    public BleOp(UUID characteristicUuid)
    {
        this(null, characteristicUuid);
    }


    /**
     * Returns <code>true</code> if the minimum values have been set for this operation
     */
    public abstract boolean isValid();

    abstract T createDuplicate();

    abstract T createNewOp();



    /**
     * Set the service UUID for this operation. This is only needed when you have characteristics with identical uuids under different services.
     */
    public final T setServiceUUID(UUID uuid)
    {
        serviceUuid = uuid;
        return (T) this;
    }

    /**
     * Returns the {@link UUID} of the service for this operation. This may return <code>null</code>.
     */
    public final @Nullable(Nullable.Prevalence.NORMAL) UUID getServiceUuid()
    {
        return serviceUuid;
    }

    /**
     * Set the characteristic UUID.
     */
    public final T setCharacteristicUUID(UUID uuid)
    {
        charUuid = uuid;
        return (T) this;
    }

    /**
     * Returns the characteristic UUID of this operation.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) UUID getCharacteristicUuid()
    {
        return charUuid;
    }

    /**
     * Set the {@link ReadWriteListener} for listening to the callback of the operation you wish to perform.
     *
     * <b>NOTE: For {@link BleNotify}, use {@link BleNotify#setNotificationListener(NotificationListener)} instead of this method.</b>
     */
    public final T setReadWriteListener(final ReadWriteListener listener)
    {
        readWriteListener = listener;
        return (T) this;
    }

    /**
     * Returns the {@link ReadWriteListener} set for this operation. This can be <code>null</code>.
     */
    public final @Nullable(Nullable.Prevalence.NORMAL) ReadWriteListener getReadWriteListener()
    {
        return readWriteListener;
    }

    /**
     * Set the {@link DescriptorFilter} to determine which characteristic to operate on, if there are multiple with the same {@link UUID} in the same
     * {@link android.bluetooth.BluetoothGattService}.
     */
    public final T setDescriptorFilter(DescriptorFilter filter)
    {
        descriptorFilter = filter;
        return (T) this;
    }

    /**
     * Returns the {@link DescriptorFilter} set for this operation. This can be <code>null</code>.
     */
    public final @Nullable(Nullable.Prevalence.NORMAL) DescriptorFilter getDescriptorFilter()
    {
        return descriptorFilter;
    }

    public final T setData(FutureData data)
    {
        m_data = data;
        return (T) this;
    }

    public final @Nullable(Nullable.Prevalence.NEVER) FutureData getData()
    {
        return m_data;
    }



    public final boolean isRead()
    {
        return this instanceof BleRead;
    }

    public final boolean isWrite()
    {
        return this instanceof BleWrite;
    }

    public final boolean isNotify()
    {
        return this instanceof BleNotify;
    }

    public final boolean isServiceUuidValid()
    {
        return serviceUuid != null && !serviceUuid.equals(Uuids.INVALID);
    }

    public final boolean isCharUuidValid()
    {
        return charUuid != null && !charUuid.equals(Uuids.INVALID);
    }

    T getDuplicateOp()
    {
        BleOp op = createNewOp();
        op.charUuid = charUuid;
        op.serviceUuid = serviceUuid;
        op.readWriteListener = readWriteListener;
        op.descriptorFilter = descriptorFilter;
        op.opList = opList;
        return (T) op;
    }

    /**
     * Returns the UUID of the descriptor for this operation. This can be <code>null</code>.
     */
    public final UUID getDescriptorUuid()
    {
        if (this instanceof BleDescriptorOp)
            return ((BleDescriptorOp) this).m_descriptorUuid;
        return Uuids.INVALID;
    }


    static BleOp createReadWriteOp(UUID serviceUuid, UUID charUuid, UUID descUuid, DescriptorFilter filter, byte[] data, ReadWriteListener.Type type)
    {
        BleOp op;
        boolean descriptorOp = Uuids.isValid(descUuid);
        switch (type)
        {
            case WRITE:
            case WRITE_NO_RESPONSE:
            case WRITE_SIGNED:
                if (descriptorOp)
                    op = new BleDescriptorWrite(serviceUuid, charUuid, descUuid);
                else
                    op = new BleWrite(serviceUuid, charUuid).setWriteType(type);
                break;
            default:
                if (descriptorOp)
                    op = new BleDescriptorRead(serviceUuid, charUuid, descUuid);
                else
                    op = new BleRead(serviceUuid, charUuid);
        }
        op.setData(new PresentData(data)).setDescriptorFilter(filter);
        return op;
    }



    static abstract class Builder<B extends Builder, T extends BleOp>
    {
        private List<T> opList = new ArrayList<>();

        T currentOp;
        private T lastOp = null;


        /**
         * Set the service UUID for this operation. This is only needed when you have characteristics with identical uuids under different services.
         */
        public final B setServiceUUID(UUID uuid)
        {
            currentOp.setServiceUUID(uuid);
            return (B) this;
        }

        /**
         * Set the characteristic UUID.
         */
        public final B setCharacteristicUUID(UUID uuid)
        {
            currentOp.setCharacteristicUUID(uuid);
            return (B) this;
        }

        /**
         * Set the {@link ReadWriteListener} for listening to the callback of the operation you wish to perform.
         */
        public final B setReadWriteListener(final ReadWriteListener listener)
        {
            currentOp.setReadWriteListener(listener);
            return (B) this;
        }

        /**
         * Set the {@link DescriptorFilter} to determine which characteristic to operate on, if there are multiple with the same {@link UUID} in the same
         * {@link android.bluetooth.BluetoothGattService}.
         */
        public final B setDescriptorFilter(DescriptorFilter filter)
        {
            currentOp.setDescriptorFilter(filter);
            return (B) this;
        }

        /**
         * Move on to another {@link BleRead} instance, based on the last one (so if you only need to change the char UUID, you don't have to set all
         * other fields again).
         */
        public final B next()
        {
            if (currentOp != lastOp)
            {
                opList.add(currentOp);
                lastOp = currentOp;
                currentOp = (T) currentOp.createDuplicate();
            }
            return (B) this;
        }

        /**
         * Move on to another {@link BleRead} instance, with the new instance having no fields set.
         */
        public final B nextNew()
        {
            if (currentOp != lastOp)
            {
                opList.add(currentOp);
                lastOp = currentOp;
                currentOp = (T) currentOp.createNewOp();
            }
            return (B) this;
        }

        /**
         * Builds, and returns the list of {@link BleRead}s.
         */
        public final List<T> build()
        {
            if (currentOp != lastOp)
            {
                opList.add(currentOp);
                lastOp = currentOp;
            }
            return opList;
        }

        /*
         * Same as {@link #build()}, only returns an array instead of a list.
         */
        public abstract T[] buildArray();

    }

}
