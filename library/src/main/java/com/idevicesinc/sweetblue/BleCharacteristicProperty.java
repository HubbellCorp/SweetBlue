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

import android.bluetooth.BluetoothGattCharacteristic;

import com.idevicesinc.sweetblue.utils.BitwiseEnum;

/**
 * This enum enforces compile-time constraints over various public static int PROPERTY_ members
 * of {@link android.bluetooth.BluetoothGattCharacteristic}.
 */
public enum BleCharacteristicProperty implements BitwiseEnum
{
	/**
	 * Strict typing for {@link BluetoothGattCharacteristic#PROPERTY_BROADCAST}.
	 */
	BROADCAST(BluetoothGattCharacteristic.PROPERTY_BROADCAST),

	/**
	 * Strict typing for {@link BluetoothGattCharacteristic#PROPERTY_READ}.
	 */
	READ(BluetoothGattCharacteristic.PROPERTY_READ),

	/**
	 * Strict typing for {@link BluetoothGattCharacteristic#PROPERTY_WRITE_NO_RESPONSE}.
	 */
	WRITE_NO_RESPONSE(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE),

	/**
	 * Strict typing for {@link BluetoothGattCharacteristic#PROPERTY_WRITE}.
	 */
	WRITE(BluetoothGattCharacteristic.PROPERTY_WRITE),

	/**
	 * Strict typing for {@link BluetoothGattCharacteristic#PROPERTY_NOTIFY}.
	 */
	NOTIFY(BluetoothGattCharacteristic.PROPERTY_NOTIFY),

	/**
	 * Strict typing for {@link BluetoothGattCharacteristic#PROPERTY_INDICATE}.
	 */
	INDICATE(BluetoothGattCharacteristic.PROPERTY_INDICATE),

	/**
	 * Strict typing for {@link BluetoothGattCharacteristic#PROPERTY_SIGNED_WRITE}.
	 */
	SIGNED_WRITE(BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE),

	/**
	 * Strict typing for {@link BluetoothGattCharacteristic#PROPERTY_EXTENDED_PROPS}.
	 */
	EXTENDED_PROPS(BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS);


	private final int m_bit;

	private BleCharacteristicProperty(final int bit)
	{
		m_bit = bit;
	}



	@Override public int or(BitwiseEnum state)
	{
		return 0;
	}

	@Override public int or(int bits)
	{
		return m_bit | bits;
	}

	@Override public int bit()
	{
		return m_bit;
	}

	@Override public boolean overlaps(int mask)
	{
		return (m_bit & mask) != 0x0;
	}
}
