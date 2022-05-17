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

package com.idevicesinc.sweetblue.internal.android;


import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGattServer;
import com.idevicesinc.sweetblue.BleCharacteristic;
import com.idevicesinc.sweetblue.BleService;
import com.idevicesinc.sweetblue.internal.IBleManager;
import com.idevicesinc.sweetblue.internal.P_Bridge_Internal;
import com.idevicesinc.sweetblue.utils.P_Const;
import java.util.List;
import java.util.UUID;


/**
 * Default implementation of {@link IBluetoothServer}, and wraps {@link BluetoothGattServer}. This class is used by default
 * by the library, and the only time it should NOT be used, is when unit testing.
 *
 * @see com.idevicesinc.sweetblue.BleManagerConfig#serverFactory
 */
public final class AndroidBluetoothServer implements IBluetoothServer
{

    public final static AndroidBluetoothServer NULL = new AndroidBluetoothServer(null, P_ServerHolder.NULL);

    private final BluetoothGattServer m_server;


    AndroidBluetoothServer(IBleManager manager, P_ServerHolder server)
    {
        m_server = server.getServer();
    }


    @Override
    public final boolean isServerNull()
    {
        return m_server == null;
    }

    @SuppressLint("MissingPermission")
    @Override
    public final boolean addService(BleService service)
    {
        if (m_server != null && !service.isNull())
        {
            return m_server.addService(service.getService());
        }

        return false;
    }

    @SuppressLint("MissingPermission")
    @Override
    public final void cancelConnection(P_DeviceHolder device)
    {
        if (m_server != null && !device.isNull())
        {
            m_server.cancelConnection(device.getDevice());
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public final void clearServices()
    {
        if (m_server != null)
        {
            m_server.clearServices();
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public final void close()
    {
        if (m_server != null)
        {
            m_server.close();
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public final boolean connect(P_DeviceHolder device, boolean autoConnect)
    {
        if (m_server != null && !device.isNull())
        {
            return m_server.connect(device.getDevice(), autoConnect);
        }
        return false;
    }

    @Override
    public final BleService getService(UUID uuid)
    {
        if (m_server != null)
        {
            return new BleService(m_server.getService(uuid));
        }
        return null;
    }

    @Override
    public final List<BleService> getServices()
    {
        if (m_server != null)
        {
            return P_Bridge_Internal.fromNativeServiceList(m_server.getServices());
        }
        return P_Const.EMPTY_BLESERVICE_LIST;
    }

    @SuppressLint("MissingPermission")
    @Override
    public final boolean notifyCharacteristicChanged(P_DeviceHolder device, BleCharacteristic characteristic, boolean confirm)
    {
        if (m_server != null && !device.isNull() && !characteristic.isNull())
        {
            return m_server.notifyCharacteristicChanged(device.getDevice(), characteristic.getCharacteristic(), confirm);
        }
        return false;
    }

    @SuppressLint("MissingPermission")
    @Override
    public final boolean removeService(BleService service)
    {
        if (m_server != null && !service.isNull())
        {
            return m_server.removeService(service.getService());
        }
        return false;
    }

    @SuppressLint("MissingPermission")
    @Override
    public final boolean sendResponse(P_DeviceHolder device, int requestId, int status, int offset, byte[] value)
    {
        if (m_server != null && !device.isNull())
        {
            return m_server.sendResponse(device.getDevice(), requestId, status, offset, value);
        }
        return false;
    }

    @Override
    public final BluetoothGattServer getNativeServer()
    {
        return m_server;
    }
}
