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

import com.idevicesinc.sweetblue.di.SweetDIManager;
import com.idevicesinc.sweetblue.internal.IBleDevice;
import com.idevicesinc.sweetblue.internal.android.IBluetoothDevice;
import com.idevicesinc.sweetblue.internal.android.IBluetoothGatt;
import com.idevicesinc.sweetblue.utils.GattDatabase;


/**
 * Convenience config class which sets {@link #bluetoothManagerImplementation}, {@link #gattFactory}, {@link #bluetoothDeviceFactory}, {@link #serverFactory},
 * and {@link #logger} to the default unit testing equivalents. This provides implementations that will "just work", with the exception of reads/writes, as no
 * {@link com.idevicesinc.sweetblue.utils.GattDatabase} is implemented.
 *
 * @see UnitTestBluetoothManager
 * @see UnitTestBluetoothDevice
 * @see UnitTestBluetoothGatt
 * @see UnitTestBluetoothServer
 * @see UnitTestLogger
 */
@SuppressWarnings("squid:ClassVariableVisibilityCheck")
public class BleManagerConfig_UnitTest extends BleManagerConfig
{

    public GattDatabase m_gattDatabase;


    public BleManagerConfig_UnitTest()
    {
        super();
        populateUnitTestItems();
    }

    /**
     * This constructor allows you to pass in a {@link GattDatabase} instance to be used for the default {@link #gattFactory} instance. If you are overriding
     * the default, then this constructor doesn't provide anything special. This is just a convenience if you are using the default gatt implementation.
     */
    public BleManagerConfig_UnitTest(GattDatabase gattDatabase)
    {
        super();
        m_gattDatabase = gattDatabase;
        populateUnitTestItems();
    }


    private void populateUnitTestItems()
    {
        bluetoothManagerImplementation = new UnitTestBluetoothManager();
        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, inputs ->
        {
            if (m_gattDatabase == null)
                return new UnitTestBluetoothGatt(inputs.get(0));
            else
                return new UnitTestBluetoothGatt(inputs.get(0), m_gattDatabase);
        });

        SweetDIManager.getInstance().registerTransient(IBluetoothDevice.class, UnitTestBluetoothDevice.class);
        serverFactory = (manager, server) -> new UnitTestBluetoothServer(manager);
        logger = new UnitTestLogger();
    }

}
