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


import android.bluetooth.BluetoothGattServer;
import com.idevicesinc.sweetblue.BleCharacteristic;
import com.idevicesinc.sweetblue.BleService;
import com.idevicesinc.sweetblue.internal.IBleManager;

import java.util.List;
import java.util.UUID;


/**
 * Interface used to abstract away the android class {@link BluetoothGattServer}.
 *
 * @see AndroidBluetoothServer for default implementation
 */
public interface IBluetoothServer
{

    boolean isServerNull();
    boolean addService(BleService service);
    void cancelConnection(P_DeviceHolder device);
    void clearServices();
    void close();
    boolean connect(P_DeviceHolder device, boolean autoConnect);
    BleService getService(UUID uuid);
    List<BleService> getServices();
    boolean notifyCharacteristicChanged(P_DeviceHolder device, BleCharacteristic characteristic, boolean confirm);
    boolean removeService(BleService service);
    boolean sendResponse(P_DeviceHolder device, int requestId, int status, int offset, byte[] value);
    BluetoothGattServer getNativeServer();


    /**
     * Interface used by the library to instantiate a new instance of {@link IBluetoothServer}
     */
    interface Factory
    {
        IBluetoothServer newInstance(IBleManager manager, P_ServerHolder server);
    }

    /**
     * An instance of {@link DefaultFactory} used by the library, unless {@link com.idevicesinc.sweetblue.BleManagerConfig#serverFactory} is changed.
     */
    Factory DEFAULT_FACTORY = new DefaultFactory();

    /**
     * Default implementation of {@link Factory}.
     */
    class DefaultFactory implements Factory
    {
        @Override
        public IBluetoothServer newInstance(IBleManager manager, P_ServerHolder server)
        {
            return new AndroidBluetoothServer(manager, server);
        }
    }

}
