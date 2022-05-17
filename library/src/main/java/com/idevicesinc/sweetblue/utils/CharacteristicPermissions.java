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


public final class CharacteristicPermissions extends Permissions<CharacteristicPermissions>
{

    private final CharacteristicBuilder m_charBuilder;


    CharacteristicPermissions(CharacteristicBuilder builder)
    {
        m_charBuilder = builder;
    }

    public final Properties setProperties()
    {
        return new Properties(build());
    }

    public final CharacteristicBuilder build()
    {
        m_charBuilder.setPermissions(getPermissions());
        return m_charBuilder;
    }

    public final ServiceBuilder completeChar()
    {
        return build().build();
    }

    public final GattDatabase completeService()
    {
        return build().completeService();
    }
}
