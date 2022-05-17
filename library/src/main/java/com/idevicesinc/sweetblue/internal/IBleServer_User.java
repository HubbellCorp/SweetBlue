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


import android.bluetooth.BluetoothGattServer;

import com.idevicesinc.sweetblue.AddServiceListener;
import com.idevicesinc.sweetblue.AdvertisingListener;
import com.idevicesinc.sweetblue.BleAdvertisingSettings;
import com.idevicesinc.sweetblue.BleNodeConfig;
import com.idevicesinc.sweetblue.BleServerState;
import com.idevicesinc.sweetblue.BleService;
import com.idevicesinc.sweetblue.IncomingListener;
import com.idevicesinc.sweetblue.OutgoingListener;
import com.idevicesinc.sweetblue.ServerConnectListener;
import com.idevicesinc.sweetblue.ServerReconnectFilter;
import com.idevicesinc.sweetblue.ServerStateListener;
import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.internal.android.IBluetoothServer;
import com.idevicesinc.sweetblue.utils.BleScanRecord;
import com.idevicesinc.sweetblue.utils.ForEach_Breakable;
import com.idevicesinc.sweetblue.utils.ForEach_Void;
import com.idevicesinc.sweetblue.utils.FutureData;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;


interface IBleServer_User
{

    void setConfig(final BleNodeConfig config_nullable);
    void setListener_State(@Nullable(Nullable.Prevalence.NORMAL) final ServerStateListener listener_nullable);
    void setListener_Incoming(@Nullable(Nullable.Prevalence.NORMAL) final IncomingListener listener_nullable);
    void setListener_ServiceAdd(@Nullable(Nullable.Prevalence.NORMAL) final AddServiceListener listener_nullable);
    void setListener_Advertising(@Nullable(Nullable.Prevalence.NORMAL) final AdvertisingListener listener_nullable);
    AdvertisingListener getListener_Advertise();
    IncomingListener getListener_Incoming();
    void setListener_Outgoing(final OutgoingListener listener);
    void setListener_ReconnectFilter(final ServerReconnectFilter listener);
    OutgoingListener.OutgoingEvent sendIndication(final String macAddress, UUID serviceUuid, UUID charUuid, final FutureData futureData, OutgoingListener listener);
    OutgoingListener.OutgoingEvent sendNotification(final String macAddress, UUID serviceUuid, UUID charUuid, final FutureData futureData, OutgoingListener listener);
    boolean isAdvertisingSupportedByAndroidVersion();
    boolean isAdvertisingSupportedByChipset();
    boolean isAdvertisingSupported();
    boolean isAdvertising();
    boolean isAdvertising(UUID serviceUuid);
    AdvertisingListener.AdvertisingEvent startAdvertising(BleScanRecord advertisePacket, BleAdvertisingSettings settings, AdvertisingListener listener);
    void stopAdvertising();
    String getName();
    boolean setName(String name);
    BluetoothGattServer getNative();
    IBluetoothServer getNativeLayer();
    int getStateMask(final String macAddress);
    boolean isAny(String string, int mask);
    ServerReconnectFilter.ConnectFailEvent connect(final String macAddress, final ServerConnectListener connectListener, final ServerReconnectFilter connectionFailListener);
    P_BleServerNativeManager getNativeManager();
    boolean disconnect(final String macAddress);
    void disconnect();
    AddServiceListener.ServiceAddEvent addService(final BleService service, final AddServiceListener listener);
    BleService removeService(final UUID serviceUuid);
    void removeAllServices();
    void getClients(final ForEach_Void<String> forEach);
    void getClients(final ForEach_Void<String> forEach, final BleServerState state);
    void getClients(final ForEach_Void<String> forEach, final BleServerState ... states);
    void getClients(final ForEach_Breakable<String> forEach);
    void getClients(final ForEach_Breakable<String> forEach, final BleServerState state);
    void getClients(final ForEach_Breakable<String> forEach, final BleServerState ... states);
    Iterator<String> getClients();
    Iterator<String> getClients(final BleServerState state);
    Iterator<String> getClients(final BleServerState ... states);
    List<String> getClients_List();
    List<String> getClients_List(final BleServerState state);
    List<String> getClients_List(final BleServerState ... states);
    int getClientCount();
    int getClientCount(final BleServerState state);
    int getClientCount(final BleServerState ... states);
    String toString();
    String getMacAddress();
    boolean equals(@Nullable(Nullable.Prevalence.NORMAL) final IBleServer server_nullable);
    boolean equals(@Nullable(Nullable.Prevalence.NORMAL) final Object object_nullable);

}
