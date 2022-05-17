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
import com.idevicesinc.sweetblue.utils.State;
import com.idevicesinc.sweetblue.utils.Utils_Byte;

/**
 * An enumeration of the various states that a {@link BleServer} can be in on a per-client (mac address) basis.
 * Note that unlike a {@link BleDevice}, a {@link BleServer} can only be in one state at a time for a given client
 * Use {@link BleServer#setListener_State(ServerStateListener)} to be notified of state changes.
 * 
 * @see ServerStateListener
 */
public enum BleServerState implements State
{
	/**
	 * Dummy value returned from any method that would otherwise return Java's built-in <code>null</code>.
	 * A normal {@link BleDevice} will never be in this state, but this will be the sole state of {@link BleServer#NULL}.
	 */
	NULL,

	/**
	 * SweetBlue equivalent of {@link android.bluetooth.BluetoothGattServer#STATE_DISCONNECTED}.
	 */
	DISCONNECTED,

	/**
	 * SweetBlue equivalent of {@link android.bluetooth.BluetoothGattServer#STATE_CONNECTING}.
	 */
	CONNECTING,

	/**
	 * SweetBlue equivalent of {@link android.bluetooth.BluetoothGattServer#STATE_CONNECTED}.
	 */
	CONNECTED,

	/**
	 * This is a state which is a SweetBlue construct. If the server/client connection fails, but is retrying, this will be it's state.
	 */
	RETRYING_CONNECTION;


	public static int toBits(BleServerState ... states)
	{
		return Utils_Byte.toBits(states);
	}
	
	@Override public boolean overlaps(int mask)
	{
		return (bit() & mask) != 0x0;
	}
	
	@Override public int bit()
	{
		return 0x1 << ordinal();
	}
	
	@Override public boolean didEnter(int oldStateBits, int newStateBits)
	{
		return !this.overlaps(oldStateBits) && this.overlaps(newStateBits);
	}
	
	@Override public boolean didExit(int oldStateBits, int newStateBits)
	{
		return this.overlaps(oldStateBits) && !this.overlaps(newStateBits);
	}
	
	@Override public int or(BitwiseEnum state)
	{
		return this.bit() | state.bit();
	}
	
	@Override public int or(int bits)
	{
		return this.bit() | bits;
	}
	
	static BleServerState[] VALUES()
	{
		s_values = s_values != null ? s_values : values();
		
		return s_values;
	}
	private static BleServerState[] s_values = null;
	
	/**
	 * Full bitwise mask made by ORing all {@link BleServerState} instances together.
	 */
	public static final int FULL_MASK = Utils_Byte.toBits(VALUES());

	@Override public boolean isNull()
	{
		return this == NULL;
	}
}
