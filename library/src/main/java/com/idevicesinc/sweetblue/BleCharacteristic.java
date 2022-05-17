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
import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.internal.P_Bridge_Internal;
import com.idevicesinc.sweetblue.internal.P_NativeGattObject;
import com.idevicesinc.sweetblue.utils.P_Const;
import com.idevicesinc.sweetblue.utils.UsesInstanceId;
import com.idevicesinc.sweetblue.utils.Uuids;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Wrapper class which holds an instance of {@link BluetoothGattCharacteristic}. You should always check {@link #isNull()} before
 * doing anything with the {@link BluetoothGattCharacteristic} returned from {@link #getCharacteristic()}.
 */
public final class BleCharacteristic extends P_NativeGattObject<BluetoothGattCharacteristic> implements UsesInstanceId
{

    public static final int PROPERTY_NOTIFY                 = BluetoothGattCharacteristic.PROPERTY_NOTIFY;
    public static final int PROPERTY_INDICATE               = BluetoothGattCharacteristic.PROPERTY_INDICATE;
    public static final int PROPERTY_WRITE                  = BluetoothGattCharacteristic.PROPERTY_WRITE;
    public static final int PROPERTY_WRITE_NO_RESPONSE      = BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE;
    public static final int PROPERTY_SIGNED_WRITE           = BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE;
    public static final int PROPERTY_READ                   = BluetoothGattCharacteristic.PROPERTY_READ;
    public static final int WRITE_TYPE_NO_RESPONSE          = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE;
    public static final int WRITE_TYPE_SIGNED               = BluetoothGattCharacteristic.WRITE_TYPE_SIGNED;


    private int m_instanceId;


    private BleCharacteristic()
    {
        super();
    }

    public BleCharacteristic(BluetoothGattCharacteristic characteristic)
    {
        this(characteristic, null);
    }

    public BleCharacteristic(UhOhListener.UhOh uhoh)
    {
        this(null, uhoh);
    }

    BleCharacteristic(BluetoothGattCharacteristic characteristic, UhOhListener.UhOh uhoh)
    {
        super(characteristic, uhoh);
        if (characteristic != null)
            m_instanceId = characteristic.getInstanceId();
    }

    /**
     * Returns the instance of {@link BluetoothGattCharacteristic} held in this class.
     */
    public final BluetoothGattCharacteristic getCharacteristic()
    {
        return getGattObject();
    }

    /**
     * Returns a list of {@link BleDescriptor}s held in this characteristic.
     */
    public final List<BleDescriptor> getDescriptors()
    {
        final List<BleDescriptor> descs = new ArrayList<>();
        final BluetoothGattCharacteristic ch = getCharacteristic();
        final List<BluetoothGattDescriptor> nDescs = ch != null ? ch.getDescriptors() : new ArrayList<>(0);
        if (nDescs != null && nDescs.size() > 0)
        {
            for (BluetoothGattDescriptor d : nDescs)
                descs.add(new BleDescriptor(d));
        }
        return descs;
    }

    /**
     * Forwards {@link BluetoothGattCharacteristic#getUuid()}. Will return {@link Uuids#INVALID} if the backing
     * characteristic is <code>null</code>.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) UUID getUuid()
    {
        return getCharacteristic() != null ? getCharacteristic().getUuid() : Uuids.INVALID;
    }

    /**
     * Forwards {@link BluetoothGattCharacteristic#getValue()}. Will return an empty array if the backing
     * characteristic is <code>null</code>.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) byte[] getValue()
    {
        return getCharacteristic() != null ? getCharacteristic().getValue() : P_Const.EMPTY_BYTE_ARRAY;
    }

    /**
     * Set the write type of this characteristic. This method is called automatically by the library.
     */
    final void setWriteType(ReadWriteListener.Type type)
    {
        if (type != null)
        {
            if (type == ReadWriteListener.Type.WRITE_NO_RESPONSE)
                getCharacteristic().setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            else if (type == ReadWriteListener.Type.WRITE_SIGNED)
                getCharacteristic().setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_SIGNED);
            else if (getCharacteristic().getWriteType() != BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                getCharacteristic().setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        }
    }

    /**
     * Set the value for this characteristic. This is used by the library, there's no need to call this app-side.
     */
    final boolean setValue(byte[] value)
    {
        return value != null && getCharacteristic() != null && getCharacteristic().setValue(value);

    }

    final int getProperties()
    {
        if (getCharacteristic() == null)
            return -1;

        return getCharacteristic().getProperties();
    }

    /**
     * Returns an instance of {@link BleService} that holds the native {@link android.bluetooth.BluetoothGattService} which this
     * characteristic belongs to. If the backing characteristic is <code>null</code>, {@link BleService#NULL} will be returned.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) BleService getService()
    {
        return getCharacteristic() == null ? BleService.NULL : new BleService(getCharacteristic().getService());
    }

    /**
     * Returns an instance of {@link BleDescriptor} that holds the native {@link android.bluetooth.BluetoothGattDescriptor} for the
     * {@link UUID} requested. If the descriptor is not found, {@link BleDescriptor#NULL} will be returned.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) BleDescriptor getDescriptor(UUID descriptorUuid)
    {
        return P_Bridge_Internal.getFromBleCharacteristic(this, descriptorUuid);
    }

    public final static BleCharacteristic NULL = new BleCharacteristic();

    @Override
    public void setInstanceId(int id)
    {
        m_instanceId = id;
    }

    @Override
    public int getInstanceId()
    {
        if (m_instanceId == 0 && getCharacteristic() != null)
            m_instanceId = getCharacteristic().getInstanceId();

        return m_instanceId;
    }
}
