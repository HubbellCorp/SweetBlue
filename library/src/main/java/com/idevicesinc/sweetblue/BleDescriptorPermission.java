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

import com.idevicesinc.sweetblue.utils.BitwiseEnum;
import android.bluetooth.BluetoothGattDescriptor;

/**
 * This enum enforces compile-time constraints over various public static int PERMISSION_ members
 * of {@link BluetoothGattDescriptor}.
 */
public enum BleDescriptorPermission implements BitwiseEnum
{
	/**
	 * Strict typing for {@link BluetoothGattDescriptor#PERMISSION_READ}.
	 */
	READ(BluetoothGattDescriptor.PERMISSION_READ),

	/**
	 * Strict typing for {@link BluetoothGattDescriptor#PERMISSION_READ_ENCRYPTED}.
	 */
	READ_ENCRYPTED(BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED),

	/**
	 * Strict typing for {@link BluetoothGattDescriptor#PERMISSION_READ_ENCRYPTED_MITM}.
	 */
	READ_ENCRYPTED_MITM(BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM),

	/**
	 * Strict typing for {@link BluetoothGattDescriptor#PERMISSION_WRITE}.
	 */
	WRITE(BluetoothGattDescriptor.PERMISSION_WRITE),

	/**
	 * Strict typing for {@link BluetoothGattDescriptor#PERMISSION_WRITE_ENCRYPTED}.
	 */
	WRITE_ENCRYPTED(BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED),

	/**
	 * Strict typing for {@link BluetoothGattDescriptor#PERMISSION_WRITE_ENCRYPTED_MITM}.
	 */
	WRITE_ENCRYPTED_MITM(BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED_MITM),

	/**
	 * Strict typing for {@link BluetoothGattDescriptor#PERMISSION_WRITE_SIGNED}.
	 */
	WRITE_SIGNED(BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED),

	/**
	 * Strict typing for {@link BluetoothGattDescriptor#PERMISSION_WRITE_SIGNED_MITM}.
	 */
	WRITE_SIGNED_MITM(BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED_MITM);

	private final int m_bit;

	private BleDescriptorPermission(final int bit)
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
