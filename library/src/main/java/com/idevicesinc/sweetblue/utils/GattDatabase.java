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
import com.idevicesinc.sweetblue.BleService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/**
 * Use this class to build out a GATT database for your simulated devices when unit testing. It contains builder classes to make it easier
 * to build out the database. Start by calling {@link #addService(UUID)}.
 */
public class GattDatabase
{

    private final List<BleService> m_services;


    public GattDatabase()
    {
        m_services = new ArrayList<>();
    }

    /**
     * Add a new service to the database.
     */
    public final ServiceBuilder addService(UUID serviceUuid)
    {
        return new ServiceBuilder(this, serviceUuid);
    }

    /**
     * Return the list of {@link BleService}s in this {@link GattDatabase}.
     */
    public final List<BleService> getServiceList()
    {
        return m_services;
    }

    /**
     * Return the list of native {@link BluetoothGattService}s in this {@link GattDatabase}.
     */
    public final List<BluetoothGattService> getNativeServiceList()
    {
        int size = m_services.size();
        List<BluetoothGattService> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++)
        {
            list.add(m_services.get(i).getService());
        }
        return list;
    }

    public final List<BleService> getNativeBleServiceList()
    {
        return new ArrayList<>(m_services);
    }

    void addService(BleService service)
    {
        m_services.add(service);
    }


}
