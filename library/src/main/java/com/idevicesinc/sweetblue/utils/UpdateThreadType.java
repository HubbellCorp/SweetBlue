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

/**
 * Enumeration used to strictly type the different options for SweetBlue's update logic.
 */
public enum UpdateThreadType
{
    /**
     * This option creates a new thread for all of SweetBlue's update logic. Only 1 thread is created and used. This is the default behavior.
     */
    THREAD,

    /**
     * Run SweetBlue's update logic on the main thread. This is still here as a legacy option, and/or as a last resort should
     * you run into issues running SweetBlue on a background thread. So far, it's been unfounded that ble operations have to run
     * on the main thread.
     */
    MAIN,

    /**
     * If you wish to have SweetBlue run it's logic on a thread you've already created, you must use this option. You'll also have to sub-class
     * {@link com.idevicesinc.sweetblue.internal.ThreadHandler}, and pass in your thread instance in the constructor.
     */
    USER_CUSTOM,

    /**
     * Similar to {@link #THREAD}, only that this uses a {@link android.os.HandlerThread}, rather than a regular Java thread.
     */
    HANDLER_THREAD;

}
