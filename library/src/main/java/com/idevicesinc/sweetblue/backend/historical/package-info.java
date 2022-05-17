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
 * Contains specification and default implementation of a "backend" for instances of {@link com.idevicesinc.sweetblue.BleDevice}
 * that stores and manages historical data. The default implementation released with the open source GPLv3 code can only
 * track one piece of historical data per UUID at a time, and only to memory.
 * <br><br>
 * Please contact sweetblue@idevicesinc.com to discuss upgrade options.
 */
package com.idevicesinc.sweetblue.backend.historical;
