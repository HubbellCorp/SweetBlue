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

import com.idevicesinc.sweetblue.BleServer;
import com.idevicesinc.sweetblue.BleService;

/**
 * Static-only class with some common BLE services for {@link BleServer#addService(BleService)}.
 */
public final class BleServices
{
	private BleServices(){}

	/**
	 * Returns a new service conforming to the "Current Time Service" specification.
	 */
	public static BleService currentTime()
	{
		ServiceBuilder builder = new ServiceBuilder(Uuids.CURRENT_TIME_SERVICE_UUID)

				.addCharacteristic(Uuids.CURRENT_TIME).setProperties().read().setPermissions().read().completeChar()

				.addCharacteristic(Uuids.LOCAL_TIME_INFORMATION).setProperties().read().notify_prop().setPermissions().read().build()

				.addDescriptor(Uuids.CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR_UUID).setPermissions().readWrite().completeChar();

		return builder.buildService();
	}
}
