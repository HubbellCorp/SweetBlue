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
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDeviceConfig;
import com.idevicesinc.sweetblue.BleDeviceOrigin;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleOp;
import com.idevicesinc.sweetblue.BleRead;
import com.idevicesinc.sweetblue.BondListener;
import com.idevicesinc.sweetblue.NotificationListener;
import com.idevicesinc.sweetblue.ReadWriteListener;
import com.idevicesinc.sweetblue.ReconnectFilter;
import com.idevicesinc.sweetblue.ScanFilter;
import com.idevicesinc.sweetblue.internal.android.IDeviceListener;
import com.idevicesinc.sweetblue.internal.android.IBluetoothDevice;
import com.idevicesinc.sweetblue.utils.Event;
import com.idevicesinc.sweetblue.utils.GenericListener_Void;
import com.idevicesinc.sweetblue.utils.Phy;


interface IBleDevice_Internal
{

    BleDevice getBleDevice();
    P_BleDeviceNativeManager nativeManager();
    boolean disconnectAndUndiscover();
    void update(double timeStep);
    void onNewlyDiscovered(IBluetoothDevice device, ScanFilter.ScanEvent event, int rssi, byte[] scanRecord, BleDeviceOrigin origin);
    void onRediscovered(IBluetoothDevice device, ScanFilter.ScanEvent event, int rssi, byte[] scanRecord, BleDeviceOrigin origin);
    void invokeReadWriteCallback(ReadWriteListener listener, ReadWriteListener.ReadWriteEvent event);
    void invokeNotificationCallback(NotificationListener listener, NotificationListener.NotificationEvent event);
    ReadWriteListener.ReadWriteEvent read_internal(final ReadWriteListener.Type type, final BleOp read);
    BleDeviceConfig conf_device();
    P_BondManager getBondManager();
    P_DeviceServiceManager getServiceManager();
    P_DeviceConnectionManager getConnectionManager();
    PE_TaskPriority getOverrideReadWritePriority();
    P_PollManager getPollManager();
    P_TransactionManager getTxnManager();
    void readRssi_internal(ReadWriteListener.Type type, ReadWriteListener listener);
    P_BleDeviceNativeManager getNativeManager();
    P_ReconnectManager reconnectMngr();
    P_DeviceStateTracker getStateTracker();
    boolean is_internal(BleDeviceState state);
    boolean isAny_internal(BleDeviceState... states);
    void setToAlwaysUseAutoConnectIfItWorked();
    void onFullyInitialized(final int gattStatus, Object... extraFlags);
    void onNativeConnect(boolean explicit);
    void onNativeConnectFail(PE_TaskState state, int gattStatus, ReconnectFilter.AutoConnectUsage autoConnectUsage);
    void onNativeDisconnect(final boolean wasExplicit, final int gattStatus, final boolean attemptShortTermReconnect, final boolean saveLastDisconnect);
    void disconnectWithReason(final P_DisconnectReason disconnectReason);
    void onServicesDiscovered();
    boolean shouldUseAutoConnect();
    void onConnecting(boolean definitelyExplicit, boolean isReconnect, final Object[] extraBondingStates, final boolean bleConnect);
    void updateRssi(final int rssi, boolean fromScan);
    void updateMtu(final int mtu);
    void updateKnownTxPower(final int txPower);
    P_ReliableWriteManager getReliableWriteManager();
    P_BleDevice_ListenerProcessor getListeners();
    IDeviceListener getInternalListener();
    void onMtuChanged();
    void updateConnectionPriority(final BleConnectionPriority connectionPriority);
    void addReadTime(double timeStep);
    void addWriteTime(double timeStep);
    void notifyOfPossibleImplicitBondingAttempt();
    void unbond_internal(PE_TaskPriority priority, BondListener.Status status);
    boolean lastDisconnectWasBecauseOfBleTurnOff();
    void onUndiscovered(PA_StateTracker.E_Intent intent);
    double getTimeSinceLastDiscovery();
    PA_StateTracker.E_Intent lastConnectDisconnectIntent();
    void updateBondStates(Object[] extraBondingStates);
    void setStateToDisconnected(final boolean attemptingReconnect_longTerm, final boolean retryingConnection, final PA_StateTracker.E_Intent intent, final int gattStatus);
    void getServices(Object... extraFlags);
    void onReconnectingShortTerm();
    void onReconnectingLongTerm();
    void dropReconnectingLongTermState();
    void softlyCancelTasks(final int overrideOrdinal);
    PA_Task.I_StateListener getListener_TaskState();
    void bond_justAddTheTask(P_Task_Bond.E_TransactionLockBehavior lockBehavior, boolean isDirect);
    void unbond_justAddTheTask();
    BondListener.BondEvent bond_private(boolean isDirect, boolean userCalled, BondListener listener);
    void onLongTermReconnectTimeOut();
    void postEventAsCallback(final GenericListener_Void listener, final Event event);
    void setPhy_private(Phy phy);
    Phy getPhy_private();
    void setThreadLocalTransaction(IBleTransaction transaction);
    IBleTransaction getThreadLocalTransaction();
    void clearListeners();
    void pauseOrResumeStateStack(boolean shouldPause);
    void pauseOrResumeConnectStack(boolean shouldPause);
}
