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
import android.bluetooth.BluetoothGattService;

import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.internal.P_NativeGattObject;
import com.idevicesinc.sweetblue.utils.UsesInstanceId;
import com.idevicesinc.sweetblue.utils.Uuids;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/**
 * Wrapper class which holds an instance of {@link BluetoothGattService}. You should always check {@link #isNull()} before
 * doing anything with the {@link BluetoothGattService} returned from {@link #getService()}.
 */
public final class BleService extends P_NativeGattObject<BluetoothGattService> implements UsesInstanceId
{

    private int m_instanceId;

    private BleService()
    {
        super();
    }

    public BleService(UhOhListener.UhOh uhoh)
    {
        this(null, uhoh);
    }

    public BleService(BluetoothGattService service)
    {
        this(service, null);
    }

    BleService(BluetoothGattService service, UhOhListener.UhOh uhoh)
    {
        super(service, uhoh);
        
        if (service != null)
            m_instanceId = service.getInstanceId();
    }

    /**
     * Returns the instance of {@link BluetoothGattService} held in this class.
     */
    public final BluetoothGattService getService()
    {
        return getGattObject();
    }

    /**
     * Returns a list of {@link BleCharacteristic}s held by this service.
     */
    public final List<BleCharacteristic> getCharacteristics()
    {
        final List<BleCharacteristic> chars = new ArrayList<>();
        final BluetoothGattService service = getService();
        final List<BluetoothGattCharacteristic> nChars = service != null ? service.getCharacteristics() : new ArrayList<>(0);
        if (nChars != null && nChars.size() > 0)
        {
            for (BluetoothGattCharacteristic ch : nChars)
                chars.add(new BleCharacteristic(ch));
        }
        return chars;
    }

    /**
     * Returns the {@link UUID} of thi service. If the service is <code>null</code> {@link Uuids#INVALID} will be returned.
     */
    public final @Nullable(Nullable.Prevalence.NEVER) UUID getUuid()
    {
        return getService() != null ? getService().getUuid() : Uuids.INVALID;
    }


    public final static BleService NULL = new BleService();

    @Override
    public void setInstanceId(int id)
    {
        m_instanceId = id;
    }

    @Override
    public int getInstanceId()
    {
        if (m_instanceId == 0 && getService() != null)
            m_instanceId = getService().getInstanceId();

        return m_instanceId;
    }

    @Override public boolean equals(Object obj)
    {
        if (obj instanceof BleService)
        {
            BleService other = (BleService) obj;
            if (other.isNull() && isNull()) return true;

            if (other.isNull()) return false;

            return other.getService().getUuid().equals(getService().getUuid());
        }
        return false;
    }
}
