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


public final class DescriptorPermissions extends Permissions<DescriptorPermissions>
{

    private final DescriptorBuilder m_descBuilder;


    DescriptorPermissions(DescriptorBuilder builder)
    {
        m_descBuilder = builder;
    }


    public final DescriptorBuilder build()
    {
        m_descBuilder.setPermissions(getPermissions());
        return m_descBuilder;
    }

    public final CharacteristicBuilder completeDesc()
    {
        return build().build();
    }

    public final ServiceBuilder completeChar()
    {
        return build().completeChar();
    }

    public final GattDatabase completeService()
    {
        return build().completeService();
    }

}
