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

import com.idevicesinc.sweetblue.annotations.Advanced;
import com.idevicesinc.sweetblue.utils.FutureData;
import com.idevicesinc.sweetblue.utils.Phy;

import java.util.UUID;

/**
 * Under the hood, SweetBlue uses a priority task queue to serialize all interaction with the native BLE stack.
 * This enumeration represents all the tasks that are used and lets you control various timing options in
 * {@link BleDeviceConfig} and {@link BleManagerConfig}, for example {@link BleDeviceConfig#taskTimeoutRequestFilter}.
 */
@Advanced
public enum BleTask
{
	/**
	 * Associated with {@link BleManager#turnOff()}
	 */
	TURN_BLE_OFF,
	
	/**
	 * Associated with {@link BleManager#turnOn()}
	 */
	TURN_BLE_ON,

	/**
	 * Associated with {@link BleManager#nukeBle()}
	 */
 	NUKE_BLE_STACK,

	/**
	 * Associated with {@link BleManagerConfig#enableCrashResolver}.
	 */
	RESOLVE_CRASHES,
	
	/**
	 * Associated with {@link BleDevice#connect()} and its several overloads.
	 */
	CONNECT,
	
	/**
	 * Associated with {@link BleDevice#disconnect()}.
	 */
	DISCONNECT,
	
	/**
	 * Associated with {@link BleDevice#bond()} and {@link BleDeviceState#BONDING}.
	 */
	BOND,
	
	/**
	 * Associated with {@link BleDevice#unbond()}.
	 */
	UNBOND,
	
	/**
	 * Associated with {@link BleDevice#read(java.util.UUID, com.idevicesinc.sweetblue.ReadWriteListener)}.
	 */
	READ,
	
	/**
	 * Associated with {@link BleDevice#write(java.util.UUID, byte[], com.idevicesinc.sweetblue.ReadWriteListener)}.
	 */
	WRITE,
	
	/**
	 * Associated with {@link BleDevice#enableNotify(java.util.UUID, com.idevicesinc.sweetblue.ReadWriteListener)} and
	 * {@link BleDevice#disableNotify(java.util.UUID, com.idevicesinc.sweetblue.ReadWriteListener)}.
	 */
	TOGGLE_NOTIFY,
	
	/**
	 * Associated with {@link BleDevice#readRssi()} and {@link BleDevice#startRssiPoll(com.idevicesinc.sweetblue.utils.Interval)} (and overloads thereof).
	 */
	READ_RSSI,
	
	/**
	 * Associated with discovering services after a {@link BleDevice} becomes {@link BleDeviceState#BLE_CONNECTED}.
	 */
	DISCOVER_SERVICES,

	/**
	 * Associated with sending a notification to a remote client through {@link BleServer#sendNotification(String, UUID, UUID, FutureData, OutgoingListener)}
	 * or {@link BleServer#sendIndication(String, UUID, UUID, FutureData, OutgoingListener)} overloads.
	 */
	SEND_NOTIFICATION,

	/**
	 * Associated with {@link BleServer#connect(String)} or overloads.
	 */
	CONNECT_SERVER,

	/**
	 * Associated with {@link BleServer#disconnect(String)}.
	 */
	DISCONNECT_SERVER,

	/**
	 * Associated with {@link IncomingListener.Please#respondWithSuccess()}, {@link IncomingListener.Please#respondWithError(int)},
	 * or various other static methods of {@link IncomingListener.Please}.
	 */
	SEND_READ_WRITE_RESPONSE,

	/**
	 * Associated with {@link BleServer#addService(BleService, AddServiceListener)} or overloads.
	 */
	ADD_SERVICE,

	/**
	 * Associated with {@link BleDevice#negotiateMtu(int)} or overloads.
	 */
	SET_MTU,

	/**
	 * Associated with {@link BleDevice#setConnectionPriority(BleConnectionPriority, ReadWriteListener)} or overloads.
	 */
	SET_CONNECTION_PRIORITY,

	/**
	 * Associated with {@link BleDevice#setPhyOptions(Phy)}, which sets Bluetooth 5 specific features.
	 */
	SET_PHYSICAL_LAYER,

	/**
	 * Gets the value set via {@link BleDevice#setPhyOptions(Phy, ReadWriteListener)}, which dictates Bluetooth 5 specific features.
	 */
	READ_PHYSICAL_LAYER,

	/**
	 * Associated with {@link BleDevice#readDescriptor(UUID, UUID, ReadWriteListener)} or overloads.
	 */
	READ_DESCRIPTOR,

	/**
	 * Associated with {@link BleDevice#writeDescriptor(UUID, UUID, byte[], ReadWriteListener)} or overloads.
	 */
	WRITE_DESCRIPTOR,

	/**
	 * Associated with {@link BleDevice#reliableWrite_execute()}.
	 */
	RELIABLE_WRITE,

	/**
	 * Associated with {@link BleServer#startAdvertising(com.idevicesinc.sweetblue.utils.BleScanRecord)}, {@link BleServer#startAdvertising(com.idevicesinc.sweetblue.utils.BleScanRecord, AdvertisingListener)}
	 */
	START_ADVERTISING,

	/**
	 * This is used to add a delay between tasks. Right now, this is not currently used.
	 */
	DELAY,

	/**
	 * This is used when calling {@link BleManager#shutdown()}. This task ensures that all devices have actually been disconnected.
	 */
	SHUTDOWN;

	/**
	 * Returns whether <code>this</code> is associated with a {@link BleDevice}.
	 */
	public boolean isDeviceSpecific()
	{
		switch(this)
		{
			//--- DRK > Server-specific.
			case CONNECT_SERVER:
			case DISCONNECT_SERVER:
			case SEND_NOTIFICATION:
			case SEND_READ_WRITE_RESPONSE:
			case ADD_SERVICE:

			//--- DRK > Manager-specific.
			case TURN_BLE_OFF:
			case TURN_BLE_ON:
			case NUKE_BLE_STACK:
			case RESOLVE_CRASHES:	return false;

			default:				return true;
		}
	}

	/**
	 * Returns whether <code>this</code> is associated with {@link BleManager}.
	 */
	public boolean isManagerSpecific()
	{
		switch(this)
		{
			case TURN_BLE_OFF:
			case TURN_BLE_ON:
			case NUKE_BLE_STACK:
			case RESOLVE_CRASHES:	return true;

			default:				return false;
		}
	}

	/**
	 * Returns whether <code>this</code> is associated with a {@link BleServer}.
	 */
	public boolean isServerSpecific()
	{
		return !isDeviceSpecific() && !isManagerSpecific();
	}
	
	/**
	 * Returns <code>true</code> if the task can have a characteristic UUID associated with it - for now {@link #READ}, {@link #WRITE}, {@link #TOGGLE_NOTIFY}, {@link #READ_DESCRIPTOR} and {@link #WRITE_DESCRIPTOR}.
	 */
	public boolean usesCharUuid()
	{
		return this == READ || this == WRITE || this == TOGGLE_NOTIFY || this == READ_DESCRIPTOR || this == WRITE_DESCRIPTOR;
	}
}
