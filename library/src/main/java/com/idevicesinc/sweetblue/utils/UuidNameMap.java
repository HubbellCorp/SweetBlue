/**
 *
 * Copyright 2022 Hubbell Incorporated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.idevicesinc.sweetblue.utils;

import java.util.UUID;

import com.idevicesinc.sweetblue.BleManagerConfig;

/**
 * Provide an implementation to {@link BleManagerConfig#uuidNameMaps}.
 * 
 * @see BleManagerConfig#uuidNameMaps
 */
public interface UuidNameMap
{
	/**
	 * Returns the name of the {@link UUID} to be used for logging/debugging purposes, for example "BATTERY_LEVEL".
	 */
	String getUuidName(String uuid);
}
