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


import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;

import com.idevicesinc.sweetblue.AddServiceListener;
import com.idevicesinc.sweetblue.AdvertisingListener;
import com.idevicesinc.sweetblue.AssertListener;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDeviceConfig;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.BleManagerState;
import com.idevicesinc.sweetblue.BondListener;
import com.idevicesinc.sweetblue.DeviceConnectListener;
import com.idevicesinc.sweetblue.DeviceReconnectFilter;
import com.idevicesinc.sweetblue.DeviceStateListener;
import com.idevicesinc.sweetblue.DiscoveryListener;
import com.idevicesinc.sweetblue.HistoricalDataLoadListener;
import com.idevicesinc.sweetblue.IncomingListener;
import com.idevicesinc.sweetblue.ManagerStateListener;
import com.idevicesinc.sweetblue.NotificationListener;
import com.idevicesinc.sweetblue.OutgoingListener;
import com.idevicesinc.sweetblue.ReadWriteListener;
import com.idevicesinc.sweetblue.ResetListener;
import com.idevicesinc.sweetblue.ScanFilter;
import com.idevicesinc.sweetblue.ScanOptions;
import com.idevicesinc.sweetblue.ServerReconnectFilter;
import com.idevicesinc.sweetblue.ServerStateListener;
import com.idevicesinc.sweetblue.UhOhListener;
import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.internal.android.IBluetoothDevice;
import com.idevicesinc.sweetblue.utils.EpochTime;
import com.idevicesinc.sweetblue.utils.ForEach_Breakable;
import com.idevicesinc.sweetblue.utils.ForEach_Void;
import com.idevicesinc.sweetblue.utils.GattDatabase;
import com.idevicesinc.sweetblue.utils.HistoricalData;
import com.idevicesinc.sweetblue.utils.Interval;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


interface IBleManager_User
{

    void setConfig(@Nullable(Nullable.Prevalence.RARE) BleManagerConfig config_nullable);
    BleManagerConfig getConfigClone();
    boolean isAny(BleManagerState... states);
    boolean isAll(BleManagerState... states);
    boolean is(final BleManagerState state);
    boolean isAny(final int mask_BleManagerState);
    boolean isAll(final int mask_BleManagerState);
    Interval getTimeInState(BleManagerState state);
    boolean isBleSupported();
    boolean isAdvertisingSupportedByAndroidVersion();
    boolean isAdvertisingSupportedByChipset();
    boolean isBluetooth5SupportedByAndroidVersion();
    boolean isBluetooth5LongRangeSupported();
    boolean isBluetooth5HighSpeedSupported();
    void turnOff();
    BluetoothManager getNative();
    BluetoothAdapter getNativeAdapter();
    void setListener_HistoricalDataLoad(@Nullable(Nullable.Prevalence.NORMAL) final HistoricalDataLoadListener listener_nullable);
    void setListener_UhOh(@Nullable(Nullable.Prevalence.NORMAL) UhOhListener listener_nullable);
    void setListener_Assert(@Nullable(Nullable.Prevalence.NORMAL) AssertListener listener_nullable);
    void setListener_Discovery(@Nullable(Nullable.Prevalence.NORMAL) DiscoveryListener listener_nullable);
    DiscoveryListener getListener_Discovery();
    void setListener_State(@Nullable(Nullable.Prevalence.NORMAL) ManagerStateListener listener_nullable);
    void setListener_DeviceState(@Nullable(Nullable.Prevalence.NORMAL) DeviceStateListener listener_nullable);
    void setListener_ServerReconnectFilter(@Nullable(Nullable.Prevalence.NORMAL) ServerReconnectFilter listener_nullable);
    void setListener_Incoming(@Nullable(Nullable.Prevalence.NORMAL) IncomingListener listener_nullable);
    void setListener_ServiceAdd(@Nullable(Nullable.Prevalence.NORMAL) AddServiceListener listener_nullable);
    void setListener_ServerState(@Nullable(Nullable.Prevalence.NORMAL) ServerStateListener listener_nullable);
    void setListener_Outgoing(@Nullable(Nullable.Prevalence.NORMAL) OutgoingListener listener_nullable);
    void setListener_DeviceReconnect(@Nullable(Nullable.Prevalence.NORMAL) DeviceReconnectFilter listener_nullable);
    void setListener_DeviceConnect(@Nullable(Nullable.Prevalence.NORMAL) DeviceConnectListener listener_nullable);
    void setListener_Bond(@Nullable(Nullable.Prevalence.NORMAL) BondListener listener_nullable);
    void setListener_Read_Write(@Nullable(Nullable.Prevalence.NORMAL) ReadWriteListener listener_nullable);
    void setListener_Notification(@Nullable(Nullable.Prevalence.NORMAL) NotificationListener listener_nullable);
    void setListener_Advertising(AdvertisingListener listener);
    boolean startScan(ScanOptions options);
    void pushWakeLock();
    void popWakeLock();
    boolean ASSERT(boolean condition, String message);
    int getStateMask();
    void turnOn();
    void reset(ResetListener listener);
    void nukeBle(ResetListener resetListener);
    void unbondAll();
    void disconnectAll();
    void disconnectAll_remote();
    void undiscoverAll();
    void turnOnLocationWithIntent_forOsServices(final Activity callingActivity, int requestCode);
    void turnOnLocationWithIntent_forOsServices(final Activity callingActivity);
    boolean willLocationPermissionSystemDialogBeShown(Activity callingActivity);
    void turnOnLocationWithIntent_forPermissions(final Activity callingActivity, int requestCode);
    void requestBluetoothPermissions(final Activity callingActivity, int requestCode);
    boolean isScanningReady();
    boolean isScanning();
    boolean isLocationEnabledForScanning();
    boolean isLocationEnabledForScanning_byManifestPermissions();
    boolean isLocationEnabledForScanning_byRuntimePermissions();
    boolean isLocationEnabledForScanning_byOsServices();
    boolean areBluetoothPermissionsEnabled();
    void turnOnWithIntent(Activity callingActivity, int requestCode);
    void onResume();
    void onPause();
    void shutdown();
    Context getApplicationContext();
    void stopScan();
    void stopScan(ScanFilter filter);
    void stopScan(PA_StateTracker.E_Intent intent);
    void stopScan(PendingIntent pendingIntent);
    IBleDevice getDevice(final String macAddress);
    IBleDevice getDevice(BleDeviceState state);
    IBleDevice getDevice(Object ... query);
    IBleDevice getDevice(final int mask_BleDeviceState);
    List<BleDevice> getDevices(final Intent intentFromScan);
    void getDevices(final ForEach_Void<BleDevice> forEach);
    void getDevices(final ForEach_Void<BleDevice> forEach, final BleDeviceState state);
    void getDevices(final ForEach_Breakable<BleDevice> forEach);
    void getDevices(final ForEach_Breakable<BleDevice> forEach, final BleDeviceState state);
    boolean hasDevice(String macAddress);
    Iterator<String> getDevices_previouslyConnected();
    Set<IBleDevice> getDevices_bonded();
    List<IBleDevice> getDevices_List();
    List<IBleDevice> getDevices_List_sorted();
    int getDeviceCount();
    int getDeviceCount(BleDeviceState state);
    int getDeviceCount(Object ... query);
    boolean hasDevices();
    List<IBleDevice> getDevices_List(final BleDeviceState state);
    List<IBleDevice> getDevices_List_sorted(final BleDeviceState state);
    List<IBleDevice> getDevices_List(final Object ... query);
    List<IBleDevice> getDevices_List_sorted(final Object ... query);
    List<IBleDevice> getDevices_List(final int mask_BleDeviceState);
    List<IBleDevice> getDevices_List_sorted(final int mask_BleDeviceState);
    void removeDeviceFromCache(IBleDevice device);
    void removeAllDevicesFromCache();
    HistoricalData newHistoricalData(final byte[] data, final EpochTime epochTime);
    HistoricalData newHistoricalData(final byte[] data, final EpochTime epochTime, final String macAddress);
    IBleServer getServer(final IncomingListener incomingListener);
    IBleServer getServer(final IncomingListener incomingListener, final GattDatabase gattDatabase, final AddServiceListener addServiceListener);
    IBleDevice newDevice(final String macAddress, final String name, final byte[] scanRecord, final BleDeviceConfig config);
    IBluetoothDevice newNativeDevice(final String macAddress);
    boolean undiscover(final IBleDevice device);
    void clearQueue();
    void clearSharedPreferences(final String macAddress);
    void clearSharedPreferences();
    boolean isForegrounded();
    String toString();

}
