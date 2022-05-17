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

package com.idevicesinc.sweetblue.annotations;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation denotes things that will change in the future. This is usually used on configuration options in
 * {@link com.idevicesinc.sweetblue.BleManagerConfig}, or {@link com.idevicesinc.sweetblue.BleDeviceConfig}. It's
 * a convenient way to show that something will change in a future version.
 */
@Retention(RetentionPolicy.CLASS)
public @interface FutureChange
{
    /**
     * The estimated release version the change is expected to happen
     */
    String value();

    /**
     * Optional value used to provide any additonal info (such as what will be changing).
     */
    String message();
}
