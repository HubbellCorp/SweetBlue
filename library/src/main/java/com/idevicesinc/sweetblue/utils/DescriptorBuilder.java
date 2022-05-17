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

import android.bluetooth.BluetoothGattDescriptor;

import com.idevicesinc.sweetblue.BleCharacteristic;
import com.idevicesinc.sweetblue.BleDescriptor;
import com.idevicesinc.sweetblue.BleService;

import java.util.UUID;

/**
 * Builder class used to create and configure a {@link BleDescriptor} to be added to a {@link BleCharacteristic}.
 */
public final class DescriptorBuilder
{

    private final UUID m_descUuid;
    private final CharacteristicBuilder m_charBuilder;

    private BleDescriptor m_descriptor;

    private int m_permissions;
    private byte[] m_value;


    DescriptorBuilder(CharacteristicBuilder charBuilder, UUID descUuid)
    {
        m_descUuid = descUuid;
        m_charBuilder = charBuilder;
    }

    /**
     * Set this {@link BleDescriptor}'s permissions. You can also use {@link #setPermissions(int...)}, but it's recommended
     * you use {@link #setPermissions()} instead.
     */
    public final DescriptorBuilder setPermissions(int permissions)
    {
        m_permissions = permissions;
        return this;
    }

    /**
     * Set this {@link BleDescriptor}'s permissions. You can also use {@link #setPermissions(int)}, but it's recommended
     * you use {@link #setPermissions()} instead.
     */
    public final DescriptorBuilder setPermissions(int... permissions)
    {
        m_permissions = 0;
        if (permissions != null && permissions.length > 0)
        {
            for (int perm : permissions)
            {
                m_permissions |= perm;
            }
        }
        return this;
    }

    /**
     * Set the value held in this {@link BleDescriptor}.
     */
    public final DescriptorBuilder setValue(byte[] value)
    {
        m_value = value;
        return this;
    }

    /**
     * Set the permissions for this {@link BleDescriptor}.
     */
    public final DescriptorPermissions setPermissions()
    {
        return new DescriptorPermissions(this);
    }

    /**
     * Build this {@link BleDescriptor}, and add it to its parent {@link BleCharacteristic}.
     */
    public final CharacteristicBuilder build()
    {
        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(m_descUuid, m_permissions);
        descriptor.setValue(m_value);
        m_descriptor = new BleDescriptor(descriptor);
        m_charBuilder.addDescriptor(m_descriptor);
        return m_charBuilder;
    }

    /**
     * Calls {@link #build()}, then creates a new {@link BleDescriptor} to add to the same {@link BleCharacteristic}.
     */
    public final DescriptorBuilder newDescriptor(UUID descriptorUuid)
    {
        build();
        return new DescriptorBuilder(m_charBuilder, descriptorUuid);
    }

    /**
     * Calls {@link #build()}, then builds the parent {@link BleCharacteristic}.
     */
    public final ServiceBuilder completeChar()
    {
        return build().build();
    }

    /**
     * Calls {@link #build()}, then builds the parent {@link BleCharacteristic}, then builds the parent {@link BleService}.
     */
    public final GattDatabase completeService()
    {
        return build().completeService();
    }

}
