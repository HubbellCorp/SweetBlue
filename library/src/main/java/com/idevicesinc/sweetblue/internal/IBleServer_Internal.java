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


import com.idevicesinc.sweetblue.AddServiceListener;
import com.idevicesinc.sweetblue.AdvertisingListener;
import com.idevicesinc.sweetblue.BleServer;
import com.idevicesinc.sweetblue.OutgoingListener;
import com.idevicesinc.sweetblue.ServerConnectListener;
import com.idevicesinc.sweetblue.ServerReconnectFilter;
import com.idevicesinc.sweetblue.internal.android.IServerListener;
import com.idevicesinc.sweetblue.internal.android.P_DeviceHolder;
import com.idevicesinc.sweetblue.utils.BleScanRecord;
import com.idevicesinc.sweetblue.utils.State;


interface IBleServer_Internal
{

    BleServer getBleServer();
    void onAdvertiseStarted(BleScanRecord packet, AdvertisingListener listener);
    void onAdvertiseStartFailed(final AdvertisingListener.Status status, final AdvertisingListener listener);
    void disconnect_internal(final AddServiceListener.Status status_serviceAdd, final ServerReconnectFilter.Status status_connectionFail, final State.ChangeIntent intent);
    void onNativeConnectFail(final P_DeviceHolder nativeDevice, final ServerReconnectFilter.Status status, final int gattStatus);
    void onNativeDisconnect( final String macAddress, final boolean explicit, final int gattStatus);
    void onNativeConnect(final String macAddress, final boolean explicit);
    void onNativeConnecting_implicit(final String macAddress);
    void resetAdaptorName();
    P_ServerServiceManager getServerServiceManager();
    void invokeOutgoingListeners(final OutgoingListener.OutgoingEvent e, final OutgoingListener listener_specific_nullable);
    void invokeConnectListeners(final ServerConnectListener.ConnectEvent e);
    ServerReconnectFilter.ConnectFailEvent connect_internal(final P_DeviceHolder nativeDevice, boolean isRetrying);
    IServerListener getInternalListener();
    void clearListeners();

}
