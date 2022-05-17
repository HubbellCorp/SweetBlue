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
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleNode;
import com.idevicesinc.sweetblue.BleServer;
import com.idevicesinc.sweetblue.BondListener;
import com.idevicesinc.sweetblue.DeviceConnectListener;
import com.idevicesinc.sweetblue.DeviceReconnectFilter;
import com.idevicesinc.sweetblue.DeviceStateListener;
import com.idevicesinc.sweetblue.HistoricalDataLoadListener;
import com.idevicesinc.sweetblue.IncomingListener;
import com.idevicesinc.sweetblue.NotificationListener;
import com.idevicesinc.sweetblue.OutgoingListener;
import com.idevicesinc.sweetblue.ReadWriteListener;
import com.idevicesinc.sweetblue.ServerConnectListener;
import com.idevicesinc.sweetblue.ServerReconnectFilter;
import com.idevicesinc.sweetblue.ServerStateListener;
import com.idevicesinc.sweetblue.UhOhListener;
import com.idevicesinc.sweetblue.backend.historical.Backend_HistoricalDatabase;
import com.idevicesinc.sweetblue.internal.android.IDeviceListener;
import com.idevicesinc.sweetblue.internal.android.IManagerListener;
import com.idevicesinc.sweetblue.internal.android.IServerListener;
import com.idevicesinc.sweetblue.internal.android.IBluetoothDevice;
import com.idevicesinc.sweetblue.internal.android.IBluetoothManager;
import com.idevicesinc.sweetblue.utils.Event;
import com.idevicesinc.sweetblue.utils.GenericListener_Void;
import java.util.List;
import java.util.UUID;


interface IBleManager_Internal
{

    P_Logger getLogger();
    void uhOh(UhOhListener.UhOh uhOh);
    void update(final double timeStep_seconds, final long currentTime);
    P_BluetoothCrashResolver getCrashResolver();
    P_ManagerStateTracker getStateTracker();
    boolean canPerformAutoScan();
    P_TaskManager getTaskManager();
    void tryPurgingStaleDevices(final double scanTime);
    boolean ready();
    long timeTurnedOn();
    void clearTimeTurnedOn();
    long getUpdateRate();
    double timeForegrounded();
    BleDevice getBleDevice(IBleDevice device);
    BleServer getBleServer(IBleServer server);
    BleNode getBleNode(IBleNode node);
    void checkIdleStatus();
    void onDiscoveredFromNativeStack(List<P_ScanManager.DiscoveryEntry> entries);
    IBluetoothManager managerLayer();
    void postEvent(final GenericListener_Void listener, final Event event);
    P_ScanManager getScanManager();
    P_PostManager getPostManager();
    P_WakeLockManager getWakeLockManager();
    P_DeviceManager getDeviceManager();
    P_DeviceManager getDeviceManager_cache();
    void clearScanningRelatedMembers(final PA_StateTracker.E_Intent intent);
    OutgoingListener getListener_Outgoing();
    AdvertisingListener getListener_Advertising();
    Backend_HistoricalDatabase getHistoricalDatabase();
    HistoricalDataLoadListener getHistoricalDataLoadListener();
    P_DiskOptionsManager getDiskOptionsManager();
    ReadWriteListener getDefaultReadWriteListener();
    NotificationListener getDefaultNotificationListener();
    ServerStateListener getDefaultServerStateListener();
    IncomingListener getDefaultServerIncomingListener();
    DeviceConnectListener getDefaultDeviceConnectListener();
    DeviceReconnectFilter getDefaultDeviceReconnectFilter();
    BondListener getDefaultBondListener();
    DeviceStateListener getDefaultDeviceStateListener();
    ServerReconnectFilter getDefaultServerReconnectFilter();
    ServerConnectListener getDefaultServerConnectListener();
    PA_Task.I_StateListener getDefaultTaskStateListener();
    void setListener_TaskState(PA_Task.I_StateListener listener);
    AddServiceListener getDefaultAddServiceListener();
    void onDiscovered_fromRogueAutoConnect(final IBleDevice device, final boolean newlyDiscovered, final List<UUID> services_nullable, final byte[] scanRecord_nullable, final int rssi);
    String getDeviceName(IBluetoothDevice device, byte[] scanRecord) throws Exception;
    long currentTime();
    IBleServer getServer();
    IManagerListener getInternalListener();
    IDeviceListener.Factory getDeviceListenerFactory();
    IServerListener.Factory getServerListenerFactory();
    IManagerListener.Factory getManagerListenerFactory();
    P_BleManagerNativeManager getNativeManager();
    boolean hasServerInstance();
    void clearShutdownSemaphore();
}
