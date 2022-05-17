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

import com.idevicesinc.sweetblue.annotations.Extendable;

import java.util.List;

/**
 * Convenience implementation that wraps a {@link java.util.List} of other {@link UuidNameMap} instances.
 */
@Extendable
public class UuidNameMap_ListWrapper implements UuidNameMap
{
	private final List<UuidNameMap> m_maps;

	public UuidNameMap_ListWrapper(final List<UuidNameMap> maps)
	{
		m_maps = maps;
	}

	public UuidNameMap_ListWrapper()
	{
		m_maps = null;
	}

	@Override public String getUuidName(String uuid)
	{
		String debugName = null;

		if( m_maps != null )
		{
			for(int i = 0; i < m_maps.size(); i++ )
			{
				String actualDebugName = m_maps.get(i).getUuidName(uuid);
				debugName = actualDebugName != null ? actualDebugName : debugName;
			}
		}

		debugName = debugName == null ? uuid : debugName;
		debugName = debugName == null ? "" : debugName;

		return debugName;
	}
}
