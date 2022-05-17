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

/**
 * Enumeration signifying how a {@link BleDevice} instance was created.
 */
public enum BleDeviceOrigin
{
	/**
	 * Created from {@link BleManager#newDevice(String, String)} or overloads.
	 * This type of device can only be {@link BleDeviceState#UNDISCOVERED} by using
	 * {@link BleManager#undiscover(BleDevice)}.
	 */
	EXPLICIT,
	
	/**
	 * Created from an advertising discovery right before {@link DiscoveryListener#onEvent(com.idevicesinc.sweetblue.DiscoveryListener.DiscoveryEvent)} is called.
	 */
	FROM_DISCOVERY;
}
