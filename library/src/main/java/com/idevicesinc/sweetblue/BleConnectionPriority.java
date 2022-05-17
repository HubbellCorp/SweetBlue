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

import android.bluetooth.BluetoothGatt;

/**
 * This enum enforces compile-time constraints over various public static int CONNECTION_PRIORITY_* members
 * of {@link android.bluetooth.BluetoothGatt} and is passed to {@link BleDevice#setConnectionPriority(BleConnectionPriority, ReadWriteListener)}
 * and returned from {@link BleDevice#getConnectionPriority()}.
 */
public enum BleConnectionPriority
{
	/**
	 * Strict typing for {@link BluetoothGatt#CONNECTION_PRIORITY_LOW_POWER}.
	 */
	LOW(2 /*BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER*/),

	/**
	 * Strict typing for {@link BluetoothGatt#CONNECTION_PRIORITY_BALANCED}.
	 */
	MEDIUM(0 /*BluetoothGatt.CONNECTION_PRIORITY_BALANCED*/),

	/**
	 * Strict typing for {@link BluetoothGatt#CONNECTION_PRIORITY_HIGH}.
	 */
	HIGH(1 /*BluetoothGatt.CONNECTION_PRIORITY_HIGH*/);

	private final int m_nativeMode;

	BleConnectionPriority(final int nativeMode)
	{
		m_nativeMode = nativeMode;
	}

	/**
	 * Returns one of the static final int members of {@link BleConnectionPriority} whose name starts with CONNECTION_PRIORITY_.
	 */
	public int getNativeMode()
	{
		return m_nativeMode;
	}
}
