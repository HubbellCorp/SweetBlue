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


public class Permissions<T extends Permissions>
{

    private int m_permissions;


    public final T read()
    {
        m_permissions |= BluetoothGattCharacteristic.PERMISSION_READ;
        return (T) this;
    }

    public final T read_encrypted()
    {
        m_permissions |= BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED;
        return (T) this;
    }

    public final T read_encrypted_mitm()
    {
        m_permissions |= BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM;
        return (T) this;
    }

    public final T write()
    {
        m_permissions |= BluetoothGattCharacteristic.PERMISSION_WRITE;
        return (T) this;
    }

    public final T readWrite()
    {
        return (T) read().write();
    }

    public final T signed_write()
    {
        m_permissions |= BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED;
        return (T) this;
    }

    public final T signed_write_mitm()
    {
        m_permissions |= BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED_MITM;
        return (T) this;
    }

    public final T write_encrypted()
    {
        m_permissions |= BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED;
        return (T) this;
    }

    public final T write_encrypted_mitm()
    {
        m_permissions |= BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM;
        return (T) this;
    }


    final int getPermissions()
    {
        return m_permissions;
    }

}
