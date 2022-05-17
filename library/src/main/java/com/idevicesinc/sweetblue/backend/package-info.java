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

/**
 * Contains specification and default implementation of "backend" modules for SweetBlue. You never use anything in these subpackages
 * directly - rather they are the implementations for various official public methods of classes like {@link com.idevicesinc.sweetblue.BleDevice}
 * and {@link com.idevicesinc.sweetblue.BleManager}. Implementations released with the open source
 * GPLv3 code are restricted to give you a sample of the functionality but are most-likely not useful for production purposes.
 * Please contact sweetblue@idevicesinc.com to discuss upgrade options.
 * <br><br>
 * The current back-end modules are as follows:
 * <p><ul>
 * <li>Historical Data for tracking past results of reads and notifications.</li>
 * </ul></p>
 * <br><br>
 * In varying stages of development are:
 * <p><ul>
 * <li>iBeacon/AltBeacon support.</li>
 * <li>BLE Server support (phone acts as the server).</li>
 * </ul></p>
 * <br><br>
 * NOTE: In general nothing in these subpackages is subject to backwards-compatible guarantees like the rest of the core of the library is.
 * Method and interface signatures may change without warning, which should be fine because the average person using SweetBlue doesn't
 * need to worry about how the back-end is implemented.
 */
package com.idevicesinc.sweetblue.backend;
