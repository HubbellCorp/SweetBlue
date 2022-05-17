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

package com.idevicesinc.sweetblue.internal;


import com.idevicesinc.sweetblue.BleCharacteristic;
import com.idevicesinc.sweetblue.BleService;
import com.idevicesinc.sweetblue.internal.android.P_DeviceHolder;


public final class P_Bridge_BleServer
{

    private P_Bridge_BleServer() {}


    public static void onConnectionStateChange(IBleServer server, P_DeviceHolder holder, int gattStatus, int newState)
    {
        server.getNativeManager().getNativeListener().onConnectionStateChange(holder, gattStatus, newState);
    }

    public static void onCharacteristicWriteRequest(IBleServer server, final P_DeviceHolder device, final int requestId, final BleCharacteristic characteristic, final boolean preparedWrite, final boolean responseNeeded, final int offset, final byte[] value)
    {
        server.getNativeManager().getNativeListener().onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
    }

    public static void onCharacteristicReadRequesst(IBleServer server, final P_DeviceHolder device, final int requestId, final int offSet, final BleCharacteristic characteristic)
    {
        server.getNativeManager().getNativeListener().onCharacteristicReadRequest(device, requestId, offSet, characteristic);
    }

    public static void onServiceAdded(IBleServer server, final int gattStatus, final BleService service)
    {
        server.getNativeManager().getNativeListener().onServiceAdded(gattStatus, service);
    }

}
