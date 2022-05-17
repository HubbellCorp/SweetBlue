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

import android.bluetooth.BluetoothGattCharacteristic;


public class Properties
{

    private final CharacteristicBuilder m_charBuilder;
    private int m_properties;


    Properties(CharacteristicBuilder builder)
    {
        m_charBuilder = builder;
    }


    public final Properties read()
    {
        m_properties |= BluetoothGattCharacteristic.PROPERTY_READ;
        return this;
    }

    public final Properties write()
    {
        m_properties |= BluetoothGattCharacteristic.PROPERTY_WRITE;
        return this;
    }

    public final Properties readWrite()
    {
        return read().write();
    }

    public final Properties readWriteNotify()
    {
        return readWrite().notify_prop();
    }

    public final Properties readWriteIndicate()
    {
        return readWrite().indicate();
    }

    public final Properties signed_write()
    {
        m_properties |= BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE;
        return this;
    }

    public final Properties write_no_response()
    {
        m_properties |= BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE;
        return this;
    }

    public final Properties notify_prop()
    {
        m_properties |= BluetoothGattCharacteristic.PROPERTY_NOTIFY;
        return this;
    }

    public final Properties indicate()
    {
        m_properties |= BluetoothGattCharacteristic.PROPERTY_INDICATE;
        return this;
    }

    public final Properties broadcast()
    {
        m_properties |= BluetoothGattCharacteristic.PROPERTY_BROADCAST;
        return this;
    }

    public final Properties extended_props()
    {
        m_properties |= BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS;
        return this;
    }

    public final CharacteristicBuilder build()
    {
        m_charBuilder.setProperties(m_properties);
        return m_charBuilder;
    }

    public final CharacteristicPermissions setPermissions()
    {
        return new CharacteristicPermissions(build());
    }

    public final GattDatabase completeService()
    {
        return build().completeService();
    }
}
