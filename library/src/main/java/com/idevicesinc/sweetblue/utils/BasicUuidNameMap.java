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

import java.util.HashMap;
import java.util.UUID;

import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.annotations.Extendable;

/**
 * Manual convenience implementation of {@link UuidNameMap} that's basically just a {@link HashMap}.
 * 
 * Provide an instance to {@link BleManagerConfig#uuidNameMaps} if desired.
 * 
 * @see ReflectionUuidNameMap
 * @see BleManagerConfig#uuidNameMaps
 */
@Extendable
public class BasicUuidNameMap implements UuidNameMap
{
	private final HashMap<String, String> m_dict = new HashMap<>();
	
	/**
	 * Add a {@link UUID}-to-debug name entry.
	 */
	public void add(String uuid, String name)
	{
		m_dict.put(uuid, name);
	}
	
	@Override public String getUuidName(String uuid)
	{
		return m_dict.get(uuid);
	}
}
