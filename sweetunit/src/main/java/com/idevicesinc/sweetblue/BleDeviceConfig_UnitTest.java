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
 * Convenience config class which sets {@link #gattFactory}, and {@link #bluetoothDeviceFactory} to the default unit testing equivalents.
 * This provides implementations that will "just work", with the exception of reads/writes, as no
 * {@link com.idevicesinc.sweetblue.utils.GattDatabase} is implemented.
 *
 * @see UnitTestBluetoothDevice
 * @see UnitTestBluetoothGatt
 */
@SuppressWarnings("squid:ClassVariableVisibilityCheck")
public class BleDeviceConfig_UnitTest extends BleDeviceConfig
{

    public GattDatabase m_gattDatabase;

    public BleDeviceConfig_UnitTest()
    {
        super();
        populateUnitTestItems();
    }

    /**
     * This constructor allows you to pass in a {@link GattDatabase} instance to be used for the default {@link #gattFactory} instance. If you are overriding
     * the default, then this constructor doesn't provide anything special. This is just a convenience if you are using the default gatt implementation.
     */
    public BleDeviceConfig_UnitTest(GattDatabase gattDatabase)
    {
        super();
        m_gattDatabase = gattDatabase;
        populateUnitTestItems();
    }

    private void populateUnitTestItems()
    {
        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, (args) ->
        {
            if (m_gattDatabase == null)
                return new UnitTestBluetoothGatt((IBleDevice) args[0]);
            else
                return new UnitTestBluetoothGatt((IBleDevice) args[0], m_gattDatabase);
        });
        SweetDIManager.getInstance().registerTransient(IBluetoothDevice.class, UnitTestBluetoothDevice.class);
    }

}
