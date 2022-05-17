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

package com.idevicesinc.sweetblue.utils;

import android.bluetooth.BluetoothGattService;
import com.idevicesinc.sweetblue.BleCharacteristic;
import com.idevicesinc.sweetblue.BleService;
import com.idevicesinc.sweetblue.annotations.Advanced;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Builder class used to help building out {@link BleService}s.
 */
public final class ServiceBuilder
{

    private final GattDatabase m_database;
    private final List<BleCharacteristic> m_characteristics;
    private final UUID m_serviceUuid;

    private BleService m_service;

    private boolean m_isPrimary = true;


    ServiceBuilder(GattDatabase database, UUID serviceUuid)
    {
        m_database = database;
        m_serviceUuid = serviceUuid;
        m_characteristics = new ArrayList<>();
    }

    ServiceBuilder(UUID serviceUuid)
    {
        m_database = null;
        m_serviceUuid = serviceUuid;
        m_characteristics = new ArrayList<>();
    }

    /**
     * Set this service to the type {@link BluetoothGattService#SERVICE_TYPE_PRIMARY}. This is the default, so it shouldn't be necessary
     * to call this, but leaving it here for explicitness.
     */
    public final ServiceBuilder primary()
    {
        m_isPrimary = true;
        return this;
    }

    /**
     * Set this service to the type {@link BluetoothGattService#SERVICE_TYPE_SECONDARY}.
     */
    public final ServiceBuilder secondary()
    {
        m_isPrimary = false;
        return this;
    }

    /**
     * Add a new {@link BleCharacteristic} to this service.
     */
    public final CharacteristicBuilder addCharacteristic(UUID charUuid)
    {
        return new CharacteristicBuilder(this, charUuid);
    }

    /**
     * Complete this service, and create a new one to be entered into the database.
     */
    public final ServiceBuilder newService(UUID serviceUuid)
    {
        build();
        return new ServiceBuilder(m_database, serviceUuid);
    }

    /**
     * Builds the current {@link BleService}, and returns the parent {@link GattDatabase}.
     */
    public final GattDatabase build()
    {
        BluetoothGattService service = new BluetoothGattService(m_serviceUuid, m_isPrimary ? BluetoothGattService.SERVICE_TYPE_PRIMARY : BluetoothGattService.SERVICE_TYPE_SECONDARY);
        for (BleCharacteristic ch : m_characteristics)
        {
            service.addCharacteristic(ch.getCharacteristic());
        }
        m_service = new BleService(service);
        m_database.addService(m_service);
        return m_database;
    }

    /**
     * Builds and returns the current {@link BleService}. Use this method if you are building out a {@link BleService}, not to be built into a {@link GattDatabase}.
     */
    @Advanced
    public final BleService buildService()
    {
        BluetoothGattService service = new BluetoothGattService(m_serviceUuid, m_isPrimary ? BluetoothGattService.SERVICE_TYPE_PRIMARY : BluetoothGattService.SERVICE_TYPE_SECONDARY);
        for (BleCharacteristic ch : m_characteristics)
        {
            service.addCharacteristic(ch.getCharacteristic());
        }
        m_service = new BleService(service);
        return m_service;
    }


    private GattDatabase getDatabase()
    {
        return m_database;
    }

    void addCharacteristic(BleCharacteristic characteristic)
    {
        m_characteristics.add(characteristic);
    }
}
