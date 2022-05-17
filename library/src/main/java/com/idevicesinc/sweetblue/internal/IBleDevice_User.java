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


import com.idevicesinc.sweetblue.BleConnectionPriority;
import com.idevicesinc.sweetblue.BleDescriptorRead;
import com.idevicesinc.sweetblue.BleDescriptorWrite;
import com.idevicesinc.sweetblue.BleDeviceConfig;
import com.idevicesinc.sweetblue.BleDeviceOrigin;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleNotify;
import com.idevicesinc.sweetblue.BleOp;
import com.idevicesinc.sweetblue.BleRead;
import com.idevicesinc.sweetblue.BleTransaction;
import com.idevicesinc.sweetblue.BleWrite;
import com.idevicesinc.sweetblue.BondListener;
import com.idevicesinc.sweetblue.DeviceConnectListener;
import com.idevicesinc.sweetblue.DeviceReconnectFilter;
import com.idevicesinc.sweetblue.DeviceStateListener;
import com.idevicesinc.sweetblue.HistoricalDataLoadListener;
import com.idevicesinc.sweetblue.NotificationListener;
import com.idevicesinc.sweetblue.ReadWriteListener;
import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.internal.android.IBluetoothDevice;
import com.idevicesinc.sweetblue.internal.android.IBluetoothGatt;
import com.idevicesinc.sweetblue.utils.BleScanRecord;
import com.idevicesinc.sweetblue.utils.Distance;
import com.idevicesinc.sweetblue.utils.EpochTime;
import com.idevicesinc.sweetblue.utils.EpochTimeRange;
import com.idevicesinc.sweetblue.utils.ForEach_Breakable;
import com.idevicesinc.sweetblue.utils.ForEach_Returning;
import com.idevicesinc.sweetblue.utils.ForEach_Void;
import com.idevicesinc.sweetblue.utils.HistoricalData;
import com.idevicesinc.sweetblue.utils.HistoricalDataCursor;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Percent;
import com.idevicesinc.sweetblue.utils.Phy;
import com.idevicesinc.sweetblue.utils.State;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;


interface IBleDevice_User
{

    ReadWriteListener.ReadWriteEvent reliableWrite_begin(final ReadWriteListener listener);
    ReadWriteListener.ReadWriteEvent reliableWrite_abort();
    ReadWriteListener.ReadWriteEvent reliableWrite_execute();
    String printState();
    void setConfig(BleDeviceConfig config);
    BleDeviceConfig getConfig();
    BleDeviceOrigin getOrigin();
    EpochTime getLastDiscoveryTime();
    State.ChangeIntent getLastDisconnectIntent();
    boolean setListener_State(@Nullable(Nullable.Prevalence.NORMAL) DeviceStateListener listener_nullable);
    boolean pushListener_State(@Nullable(Nullable.Prevalence.NEVER) DeviceStateListener listener);
    boolean popListener_State();
    boolean popListener_State(DeviceStateListener listener);
    DeviceStateListener getListener_State();
    boolean setListener_Connect(@Nullable(Nullable.Prevalence.NORMAL) DeviceConnectListener listener);
    boolean pushListener_Connect(@Nullable(Nullable.Prevalence.NEVER) DeviceConnectListener listener);
    boolean popListener_Connect();
    boolean popListener_Connect(DeviceConnectListener listener);
    DeviceConnectListener getListener_Connect();
    void setListener_Reconnect(@Nullable(Nullable.Prevalence.NORMAL) DeviceReconnectFilter listener_nullable);
    void pushListener_Reconnect(@Nullable(Nullable.Prevalence.NEVER) DeviceReconnectFilter listener);
    boolean popListener_Reconnect();
    boolean popListener_Reconnect(DeviceReconnectFilter listener);
    DeviceReconnectFilter getListener_Reconnect();
    void setListener_Bond(@Nullable(Nullable.Prevalence.NORMAL) BondListener listener_nullable);
    void setListener_ReadWrite(@Nullable(Nullable.Prevalence.NORMAL) final ReadWriteListener listener_nullable);
    void pushListener_ReadWrite(@Nullable(Nullable.Prevalence.NEVER) ReadWriteListener listener);
    boolean popListener_ReadWrite();
    boolean popListener_ReadWrite(ReadWriteListener listener);
    ReadWriteListener getListener_ReadWrite();
    void setListener_Notification(@Nullable(Nullable.Prevalence.NORMAL) NotificationListener listener_nullable);
    void pushListener_Notification(@Nullable(Nullable.Prevalence.NEVER) NotificationListener listener);
    boolean popListener_Notification();
    boolean popListener_Notification(NotificationListener listener);
    NotificationListener getListener_Notification();
    void setListener_HistoricalDataLoad(@Nullable(Nullable.Prevalence.NORMAL) final HistoricalDataLoadListener listener_nullable);
    int getConnectionRetryCount();
    int getStateMask();
    int getNativeStateMask();
    Interval getAverageReadTime();
    Interval getAverageWriteTime();
    int getRssi();
    Percent getRssiPercent();
    Distance getDistance();
    int getTxPower();
    byte[] getScanRecord();
    BleScanRecord getScanInfo();
    int getAdvertisingFlags();
    UUID[] getAdvertisedServices();
    byte[] getManufacturerData();
    int getManufacturerId();
    Map<UUID, byte[]> getAdvertisedServiceData();
    String getHistoricalDataTableName(final UUID uuid);
    HistoricalDataCursor getHistoricalData_cursor(final UUID uuid, final EpochTimeRange range);
    void loadHistoricalData(final UUID uuid, final HistoricalDataLoadListener listener);
    boolean isHistoricalDataLoading(final UUID uuid);
    boolean isHistoricalDataLoaded(final UUID uuid);
    Iterator<HistoricalData> getHistoricalData_iterator(final UUID uuid, final EpochTimeRange range);
    boolean getHistoricalData_forEach(final UUID uuid, final EpochTimeRange range, final ForEach_Void<HistoricalData> forEach);
    boolean getHistoricalData_forEach(final UUID uuid, final EpochTimeRange range, final ForEach_Breakable<HistoricalData> forEach);
    HistoricalData getHistoricalData_atOffset(final UUID uuid, final EpochTimeRange range, final int offsetFromStart);
    int getHistoricalDataCount(final UUID uuid, final EpochTimeRange range);
    boolean hasHistoricalData(final UUID uuid, final EpochTimeRange range);
    void addHistoricalData(final UUID uuid, final HistoricalData historicalData);
    void addHistoricalData(final UUID uuid, Iterator<HistoricalData> historicalData);
    void addHistoricalData(final UUID uuid, ForEach_Returning<HistoricalData> historicalData);
    boolean isAny(BleDeviceState... states);
    boolean isAll(BleDeviceState... states);
    boolean isConnectable();
    boolean is(final BleDeviceState state);
    boolean isAny(final int mask_BleDeviceState);
    boolean isAll(final int mask_BleDeviceState);
    boolean is(Object... query);
    Interval getTimeInState(BleDeviceState state);
    void refreshGattDatabase(Interval gattPause);
    ReadWriteListener.ReadWriteEvent setName(final String name, final UUID characteristicUuid, final ReadWriteListener listener);
    void clearName();
    String getName_override();
    String getName_native();
    String getName_normalized();
    String getName_debug();
    IBluetoothDevice getNative();
    IBluetoothGatt getNativeGatt();
    String getMacAddress();
    BondListener.BondEvent bond(BondListener listener);
    boolean unbond(BondListener listener);
    DeviceReconnectFilter.ConnectFailEvent connect(BleTransaction.Auth authenticationTxn, BleTransaction.Init initTxn, DeviceConnectListener connectionListener);
    boolean disconnect();
    boolean disconnectWhenReady();
    boolean disconnect_remote();
    boolean undiscover();
    void clearSharedPreferences();
    boolean equals(@Nullable(Nullable.Prevalence.NORMAL) final IBleDevice device_nullable);
    boolean equals(@Nullable(Nullable.Prevalence.NORMAL) final Object object_nullable);
    void startPoll(BleOp bleOp, Interval interval);
    void startChangeTrackingPoll(BleOp bleOp, Interval interval);
    void stopPoll(BleOp bleOp, Interval interval);
    ReadWriteListener.ReadWriteEvent write(BleWrite bleWrite);
    ReadWriteListener.ReadWriteEvent write(BleDescriptorWrite descriptorWrite);
    ReadWriteListener.ReadWriteEvent readRssi(final ReadWriteListener listener);
    ReadWriteListener.ReadWriteEvent setConnectionPriority(final BleConnectionPriority connectionPriority, final ReadWriteListener listener);
    BleConnectionPriority getConnectionPriority();
    int getMtu();
    ReadWriteListener.ReadWriteEvent negotiateMtuToDefault(final ReadWriteListener listener);
    ReadWriteListener.ReadWriteEvent negotiateMtu(final int mtu, final ReadWriteListener listener);
    void startRssiPoll(final Interval interval, final ReadWriteListener listener);
    void stopRssiPoll();
    void clearAllData();
    void clearHistoricalData();
    void clearHistoricalData(final EpochTimeRange range, final long count);
    void clearHistoricalData(final UUID uuid, final EpochTimeRange range, final long count);
    void clearHistoricalData_memoryOnly();
    void clearHistoricalData_memoryOnly(final EpochTimeRange range, final long count);
    void clearHistoricalData_memoryOnly(final UUID characteristicUuid, final EpochTimeRange range, final long count);
    ReadWriteListener.ReadWriteEvent read(final BleRead read);
    ReadWriteListener.ReadWriteEvent read(final BleDescriptorRead descriptorRead);
    boolean isNotifyEnabled(final UUID uuid);
    boolean isNotifyEnabling(final UUID uuid);
    ReadWriteListener.ReadWriteEvent enableNotify(BleNotify notify);
    ReadWriteListener.ReadWriteEvent disableNotify(BleNotify notify);
    boolean performOta(final BleTransaction.Ota txn);
    boolean performTransaction(final BleTransaction txn);
    int getEffectiveWriteMtuSize();
    String toString();
    boolean isNull();
    ReadWriteListener.ReadWriteEvent setPhyOptions(Phy phy, ReadWriteListener listener);
    ReadWriteListener.ReadWriteEvent readPhyOptions(ReadWriteListener listener);

}
