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


import android.bluetooth.BluetoothGattServer;
import com.idevicesinc.sweetblue.internal.IBleManager;
import com.idevicesinc.sweetblue.internal.android.IBluetoothServer;
import com.idevicesinc.sweetblue.internal.android.P_DeviceHolder;
import com.idevicesinc.sweetblue.utils.Interval;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Base class used to mock a bluetooth server (at the android layer). This class is setup to basically always work.
 */
public class UnitTestBluetoothServer implements IBluetoothServer
{

    private final IBleManager m_manager;
    private final List<BleService> m_serviceList;


    /**
     * Default constructor.
     */
    public UnitTestBluetoothServer(IBleManager mgr)
    {
        m_manager = mgr;
        m_serviceList = new ArrayList<>();
    }

    /**
     * Returns <code>true</code> if the server instance is <code>null</code>. This method for now, always returns <code>false</code>.
     */
    @Override
    public boolean isServerNull()
    {
        return false;
    }

    /**
     * Called by the system when it wants to add a new {@link BleService} to the {@link BleServer} instance.
     */
    @Override
    public boolean addService(BleService service)
    {
        if (!m_serviceList.contains(service))
            m_serviceList.add(service);

        Util_Native.addServiceSuccess(m_manager.getServer().getBleServer(), service, Interval.millis(150));

        return true;
    }

    /**
     * Called when attempting to disconnect a device from a {@link BleServer} instance.
     */
    @Override
    public void cancelConnection(P_DeviceHolder device)
    {
    }

    /**
     * Called by the library when it wants to remove all {@link BleService}s from this {@link BleServer} instance.
     */
    @Override
    public void clearServices()
    {
    }

    /**
     * Called when the system wants to shutdown the {@link BleServer} instance.
     */
    @Override
    public void close()
    {
    }

    /**
     * Called when the system wants to connect the {@link BleServer} instance to a device.
     */
    @Override
    public boolean connect(P_DeviceHolder device, boolean autoConnect)
    {
        Util_Native.setToConnected(m_manager.getServer().getBleServer(), device.getAddress(), Interval.millis(150));
        return true;
    }

    /**
     * Returns a {@link BleService} instance which has the given {@link UUID}. If none are found, {@link BleService#NULL} is
     * returned.
     */
    @Override
    public BleService getService(UUID uuid)
    {
        for (BleService service : m_serviceList)
        {
            if (service.getUuid().equals(uuid))
                return service;
        }
        return BleService.NULL;
    }

    /**
     * Returns a List of {@link BleService}s served by the {@link BleServer} instance.
     */
    @Override
    public List<BleService> getServices()
    {
        return m_serviceList;
    }

    /**
     * Called by the system to say that a notification has changed.
     */
    @Override
    public boolean notifyCharacteristicChanged(P_DeviceHolder device, BleCharacteristic characteristic, boolean confirm)
    {
        return true;
    }

    /**
     * Called by the library when removing a {@link BleService}.
     */
    @Override
    public boolean removeService(BleService service)
    {
        return true;
    }

    /**
     * Called by the system to send a response to a request from a connected device.
     */
    @Override
    public boolean sendResponse(P_DeviceHolder device, int requestId, int status, int offset, byte[] value)
    {
        return true;
    }

    /**
     * Returns <code>null</code>, as in unit tests it's mocked.
     */
    @Override
    public BluetoothGattServer getNativeServer()
    {
        return null;
    }

    /**
     * Returns the {@link IBleManager} instance held by this class.
     */
    public IBleManager getManager()
    {
        return m_manager;
    }
}
