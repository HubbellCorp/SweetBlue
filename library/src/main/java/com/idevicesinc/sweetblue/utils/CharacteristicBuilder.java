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
import android.bluetooth.BluetoothGattService;

import com.idevicesinc.sweetblue.BleCharacteristic;
import com.idevicesinc.sweetblue.BleDescriptor;
import com.idevicesinc.sweetblue.BleService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Builder class used to create and configure a {@link BluetoothGattCharacteristic} to be entered into a {@link BluetoothGattService}.
 */
public final class CharacteristicBuilder
{

    private final UUID m_charUuid;
    private final ServiceBuilder m_serviceBuilder;
    private final List<BleDescriptor> m_descriptors;

    private BleCharacteristic m_characteristic;

    private byte[] m_value;
    private int m_properties;
    private int m_permissions;


    CharacteristicBuilder(ServiceBuilder serviceBuilder, UUID charUuid)
    {
        m_serviceBuilder = serviceBuilder;
        m_charUuid = charUuid;
        m_descriptors = new ArrayList<>();
    }

    /**
     * Set this {@link BleCharacteristic}'s properties. You can also use {@link #setProperties(int...)}, but it's recommended
     * you use {@link #setProperties()} instead.
     */
    public final CharacteristicBuilder setProperties(int properties)
    {
        m_properties = properties;
        return this;
    }

    /**
     * Set this {@link BleCharacteristic}'s properties. You can also use {@link #setProperties(int)}, but it's recommended
     * you use {@link #setProperties()} instead.
     */
    public final CharacteristicBuilder setProperties(int... properties)
    {
        m_properties = 0;
        if (properties != null && properties.length > 0)
        {
            for (int prop : properties)
            {
                m_properties |= prop;
            }
        }
        return this;
    }

    /**
     * Set the properties for this {@link BleCharacteristic}.
     */
    public final Properties setProperties()
    {
        return new Properties(this);
    }

    /**
     * Set this {@link BleCharacteristic}'s permissions. You can also use {@link #setPermissions(int...)}, but it's recommended
     * you use {@link #setPermissions()} instead.
     */
    public final CharacteristicBuilder setPermissions(int permissions)
    {
        m_permissions = permissions;
        return this;
    }

    /**
     * Set this {@link BleCharacteristic}'s permissions. You can also use {@link #setPermissions(int)}, but it's recommended
     * you use {@link #setPermissions()} instead.
     */
    public final CharacteristicBuilder setPermissions(int... permissions)
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
     * Set the permissions for this {@link BleCharacteristic}.
     */
    public final CharacteristicPermissions setPermissions()
    {
        return new CharacteristicPermissions(this);
    }

    /**
     * Set the default value for this {@link BleCharacteristic}.
     */
    public final CharacteristicBuilder setValue(byte[] value)
    {
        m_value = value;
        return this;
    }

    /**
     * Add a new {@link BleDescriptor} to be added to this {@link BleCharacteristic}.
     */
    public final DescriptorBuilder addDescriptor(UUID descriptorUuid)
    {
        return new DescriptorBuilder(this, descriptorUuid);
    }

    /**
     * Build this {@link BleCharacteristic}, and add it to it's parent {@link BleService} via {@link ServiceBuilder}. This is meant to be used when building
     * a {@link GattDatabase}.
     */
    public final ServiceBuilder build()
    {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(m_charUuid, m_properties, m_permissions);
        characteristic.setValue(m_value);
        for (BleDescriptor desc : m_descriptors)
        {
            characteristic.addDescriptor(desc.getDescriptor());
        }
        m_characteristic = new BleCharacteristic(characteristic);
        m_serviceBuilder.addCharacteristic(m_characteristic);
        return m_serviceBuilder;
    }

    /**
     * Calls {@link #build()}, then creates a new {@link BleCharacteristic}.
     */
    public final CharacteristicBuilder newCharacteristic(UUID charUuid)
    {
        build();
        return new CharacteristicBuilder(m_serviceBuilder, charUuid);
    }

    /**
     * Calls {@link #build()}, then builds the parent {@link BleService}, and add it to the database.
     */
    public final GattDatabase completeService()
    {
        return build().build();
    }


    void addDescriptor(BleDescriptor descriptor)
    {
        m_descriptors.add(descriptor);
    }
}
