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


public enum AdvertisingFlag implements Flag
{

    Limited_Discoverable_Mode(0x0),
    General_Discoverable_Mode(1 << 1),
    BR_EDR_Not_Supported(1 << 2),
    LE_And_EDR_Supported_Controller(1 << 3),
    LE_And_EDR_Supported_Host(1 << 4),
    Unknown(1 << 5);


    private final int bit;

    AdvertisingFlag(int bit)
    {
        this.bit = bit;
    }

    @Override
    public int bit()
    {
        return bit;
    }

    public boolean overlaps(int mask)
    {
        return (mask & bit) != 0;
    }

    public static AdvertisingFlag fromBit(int bit)
    {
        for (AdvertisingFlag f : values())
        {
            if (f.bit == bit)
            {
                return f;
            }
        }
        return Unknown;
    }

}
