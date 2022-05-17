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


public final class Utils_Config
{

    // No instances
    private Utils_Config() {}


    public static boolean boolOrDefault(Boolean bool_nullable)
    {
        return bool_nullable == null ? false : bool_nullable;
    }

    public static Interval intervalOrDefault(Interval value_nullable)
    {
        return value_nullable == null ? Interval.DISABLED : value_nullable;
    }

    public static boolean bool(Boolean bool_device_nullable, Boolean bool_mngr_nullable)
    {
        return bool_device_nullable != null ? bool_device_nullable : boolOrDefault(bool_mngr_nullable);
    }

    public static Interval interval(Interval interval_device_nullable, Interval interval_mngr_nullable)
    {
        return interval_device_nullable != null ? interval_device_nullable : intervalOrDefault(interval_mngr_nullable);
    }

    public static Integer integer(Integer int_device_nullable, Integer int_mngr_nullable)
    {
        return int_device_nullable != null ? int_device_nullable : int_mngr_nullable;
    }

    public static Integer integer(Integer int_device_nullable, Integer int_mngr_nullable, int defaultValue)
    {
        return integerOrDefault(integer(int_device_nullable, int_mngr_nullable), defaultValue);
    }

    public static int integerOrZero(Integer value_nullable)
    {
        return integerOrDefault(value_nullable, 0);
    }

    public static int integerOrDefault(Integer value_nullable, int defaultValue)
    {
        return value_nullable != null ? value_nullable : defaultValue;
    }

    public static <T> T filter(T filter_device, T filter_mngr)
    {
        return filter_device != null ? filter_device : filter_mngr;
    }



}
