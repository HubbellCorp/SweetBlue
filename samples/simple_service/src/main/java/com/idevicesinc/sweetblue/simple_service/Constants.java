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

package com.idevicesinc.sweetblue.simple_service;

class Constants
{
    // Keys for the signals being sent to the service
    static final String ACTION_START = "ACTION_START";
    static final String ACTION_STOP = "ACTION_STOP";
    static final String ACTION_CONNECT = "ACTION_CONNECT";
    static final String ACTION_DISCONNECT = "ACTION_DISCONNECT";

    // Key for the signal indicating how the app was launched.
    static final String FROM_NOTIFICATION = "FROM_NOTIFICATION";

    // Key for extras being sent to the service
    static final String EXTRA_MAC_ADDRESS = "MAC_ADDRESS";
}
