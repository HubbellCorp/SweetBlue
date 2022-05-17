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

import java.util.UUID;

/**
 * A simple interface whose implementations should be passed to methods like {@link com.idevicesinc.sweetblue.BleDevice#write(UUID, FutureData)}
 * so you can provide time-sensitive data at the last possible moment. For example you may want to send the current time to a peripheral. If you provided
 * the time through {@link com.idevicesinc.sweetblue.BleDevice#write(UUID, byte[])} then the operation might spend a second or two in SweetBlue's internal job
 * queue, so that by the time the write was actually sent off, the time would be a few seconds behind. Using this class, the data will be requested right before
 * it actually gets sent off.
 */
public interface FutureData
{

	/**
	 * Return the data that should (for example) be sent to a peripheral through {@link com.idevicesinc.sweetblue.BleDevice#write(UUID, FutureData)}.
	 */
	byte[] getData();
}
