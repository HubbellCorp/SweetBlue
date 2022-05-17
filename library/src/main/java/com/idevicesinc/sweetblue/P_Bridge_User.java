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

package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.database.Cursor;

import com.idevicesinc.sweetblue.internal.IBleDevice;
import com.idevicesinc.sweetblue.internal.IBleManager;
import com.idevicesinc.sweetblue.internal.IBleServer;
import com.idevicesinc.sweetblue.internal.IBleTransaction;
import com.idevicesinc.sweetblue.internal.P_ConnectFailPlease;
import com.idevicesinc.sweetblue.internal.P_DisconnectReason;
import com.idevicesinc.sweetblue.internal.android.IBluetoothGatt;
import com.idevicesinc.sweetblue.internal.android.IBluetoothDevice;
import com.idevicesinc.sweetblue.internal.android.P_DeviceHolder;
import com.idevicesinc.sweetblue.utils.BleScanRecord;
import com.idevicesinc.sweetblue.utils.EpochTime;
import com.idevicesinc.sweetblue.utils.EpochTimeRange;
import com.idevicesinc.sweetblue.utils.FutureData;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Phy;
import com.idevicesinc.sweetblue.utils.State;
import com.idevicesinc.sweetblue.utils.Utils_Config;

import java.util.ArrayList;
import java.util.UUID;

public final class P_Bridge_User
{


    private P_Bridge_User()
    {
        throw new RuntimeException("No instances!");
    }
    //==================================================//
    //                                                  //
    // Internal layer -> User layer                     //
    //                                                  //
    //==================================================//

    public static void initTaskTimeoutRequestEvent(TaskTimeoutRequestFilter.TaskTimeoutRequestEvent event, BleManager manager, BleDevice device, BleServer server, BleTask task, UUID charUuid, UUID descUuid)
    {
        event.init(manager, device, server, task, charUuid, descUuid);
    }

    public static boolean ack(ScanFilter.Please please)
    {
        return please.ack();
    }

    @Deprecated
    public static boolean stopPeriodicScan(ScanFilter.Please please)
    {
        return (please.m_stopScanOptions & ScanFilter.Please.STOP_PERIODIC_SCAN) != 0x0;
    }

    public static boolean stopScan(ScanFilter.Please please)
    {
        return (please.m_stopScanOptions & ScanFilter.Please.STOP_SCAN) != 0x0;
    }


    public static IBluetoothDevice newDeviceLayer(IBleManager mgr, IBleDevice device)
    {
        return mgr.getConfigClone().newDeviceLayer(device);
    }


    public static BleManagerState getState(int nativeStateInt)
    {
        return BleManagerState.get(nativeStateInt);
    }


    public static Boolean isUnitTest(BleManagerConfig config)
    {
        return config.unitTest;
    }

    public static void setUnitTest(BleManagerConfig config, boolean isUnitTest)
    {
        config.unitTest = isUnitTest;
    }


    // ***********************
    // Event helper methods
    // ***********************

    public static UhOhListener.UhOhEvent newUhOhEvent(BleManager mgr, UhOhListener.UhOh uhOh)
    {
        return new UhOhListener.UhOhEvent(mgr, uhOh);
    }

    public static ManagerStateListener.StateEvent newManagerStateEvent(final BleManager manager, final int oldStateBits, final int newStateBits, final int intentMask)
    {
        return new ManagerStateListener.StateEvent(manager, oldStateBits, newStateBits, intentMask);
    }

    public static BleNodeConfig.HistoricalDataLogFilter.HistoricalDataLogEvent newHistoricalDataLogEvent(BleNode node, String macAddress, UUID charUuid, byte[] data, EpochTime epochTime, BleNodeConfig.HistoricalDataLogFilter.Source source)
    {
        return new BleNodeConfig.HistoricalDataLogFilter.HistoricalDataLogEvent(node, macAddress, charUuid, data, epochTime, source);
    }

    public static AssertListener.AssertEvent newAssertEvent(IBleManager mgr, String msg, StackTraceElement[] stackTrace)
    {
        return new AssertListener.AssertEvent(BleManager.get(mgr.getApplicationContext()), msg, stackTrace);
    }

    public static DiscoveryListener.DiscoveryEvent newDiscoveryEvent(BleDevice device, DiscoveryListener.LifeCycle lifeCycle)
    {
        return new DiscoveryListener.DiscoveryEvent(device, lifeCycle);
    }

    public static ResetListener.ResetEvent newResetEvent(BleManager mgr, ResetListener.Progress progress)
    {
        return new ResetListener.ResetEvent(mgr, progress);
    }

    public static ScanFilter.ScanEvent newScanEventFromRecord(final BluetoothDevice device_native, final String rawDeviceName, final String normalizedDeviceName, final int rssi, final State.ChangeIntent lastDisconnectIntent, final byte[] scanRecord)
    {
        return ScanFilter.ScanEvent.fromScanRecord(device_native, rawDeviceName, normalizedDeviceName, rssi, lastDisconnectIntent, scanRecord);
    }

    public static BondListener.BondEvent newBondEvent(BleDevice device, BondListener.BondEvent.Type bondType, BondListener.Status status, int failReason, State.ChangeIntent intent)
    {
        return new BondListener.BondEvent(device, bondType, status, failReason, intent);
    }

    public static BondListener.BondEvent newBondEventNULL(BleDevice device)
    {
        return BondListener.BondEvent.NULL(device);
    }

    public static BondFilter.CharacteristicEvent newBondCharEvent(BleDevice device, UUID uuid, BondFilter.CharacteristicEventType charType)
    {
        return new BondFilter.CharacteristicEvent(device, uuid, charType);
    }

    public static BondRetryFilter.RetryEvent newBondRetryEvent(BleDevice device, int failCode, int retryAttempts, boolean direct, boolean userPrompted)
    {
        return new BondRetryFilter.RetryEvent(device, failCode, retryAttempts, direct, userPrompted);
    }

    public static BondFilter.ConnectionBugEvent newBondConnectionBugEvent(BleDevice device)
    {
        return new BondFilter.ConnectionBugEvent(device);
    }

    public static ReadWriteListener.ReadWriteEvent newReadWriteEvent(BleDevice device, BleOp bleOp, ReadWriteListener.Type type, ReadWriteListener.Target target, ReadWriteListener.Status status, int gattStatus, double totalTime, double transitTime, boolean solicited)
    {
        return new ReadWriteListener.ReadWriteEvent(device, bleOp, type, target, status, gattStatus, totalTime, transitTime, solicited);
    }

    public static ReadWriteListener.ReadWriteEvent newReadWriteEventRssi(BleDevice device, ReadWriteListener.Type type, int rssi, ReadWriteListener.Status status, int gattStatus, double totalTime, double transitTime, boolean solicited)
    {
        return new ReadWriteListener.ReadWriteEvent(device, type, rssi, status, gattStatus, totalTime, transitTime, solicited);
    }

    public static ReadWriteListener.ReadWriteEvent newReadWriteEventMtu(BleDevice device, int mtu, ReadWriteListener.Status status, int gattStatus, double totalTime, double transitTime, boolean solicited)
    {
        return new ReadWriteListener.ReadWriteEvent(device, mtu, status, gattStatus, totalTime, transitTime, solicited);
    }

    public static ReadWriteListener.ReadWriteEvent newReadWriteEventConnectionPriority(BleDevice device, BleConnectionPriority connectionPriority, ReadWriteListener.Status status, int gattStatus, double totalTime, double transitTime, boolean solicited)
    {
        return new ReadWriteListener.ReadWriteEvent(device, connectionPriority, status, gattStatus, totalTime, transitTime, solicited);
    }

    public static ReadWriteListener.ReadWriteEvent newReadWriteEventPhy(BleDevice device, ReadWriteListener.Status status, int gattStatus, Phy options, double totalTime, double transitTime, boolean solicited)
    {
        return new ReadWriteListener.ReadWriteEvent(device, status, gattStatus, options, totalTime, transitTime, solicited);
    }

    public static ReadWriteListener.ReadWriteEvent newReadWriteEventNULL(BleDevice device)
    {
        return ReadWriteListener.ReadWriteEvent.NULL(device);
    }

    public static NotificationListener.NotificationEvent newNotificationEvent(BleDevice device, BleNotify notify, NotificationListener.Type type, NotificationListener.Status status, int gattStatus, double totalTime, double transitTime, boolean solicited)
    {
        return new NotificationListener.NotificationEvent(device, notify.getServiceUuid(), notify.getCharacteristicUuid(), type, notify.getData().getData(), status, gattStatus, totalTime, transitTime, solicited);
    }

    public static DescriptorFilter.DescriptorEvent newDescriptorEvent(BluetoothGattService service, BluetoothGattCharacteristic characteristic, BluetoothGattDescriptor descriptor, FutureData data)
    {
        return new DescriptorFilter.DescriptorEvent(service, characteristic, descriptor, data);
    }

    public static DeviceConnectListener.ConnectEvent newConnectEvent(BleDevice device, DeviceReconnectFilter.ConnectFailEvent failEvent, boolean willRetry)
    {
        return new DeviceConnectListener.ConnectEvent(device, failEvent, willRetry);
    }

    public static DeviceConnectListener.ConnectEvent newConnectEvent(BleDevice device)
    {
        return new DeviceConnectListener.ConnectEvent(device, DeviceReconnectFilter.ConnectFailEvent.NULL(device.getIBleDevice()), false);
    }

    public static DeviceReconnectFilter.ConnectFailEvent newConnectFailEvent(BleDevice device, int failureCountSoFar, P_DisconnectReason reason, BleDeviceState highestStateReached_total, Interval latestAttemptTime, Interval totalAttemptTime)
    {
        return new DeviceReconnectFilter.ConnectFailEvent(device, failureCountSoFar, reason, highestStateReached_total, latestAttemptTime, totalAttemptTime);
    }

    public static DeviceReconnectFilter.ConnectFailEvent newConnectFailEARLYOUT(BleDevice device, DeviceReconnectFilter.Status status)
    {
        return DeviceReconnectFilter.ConnectFailEvent.EARLY_OUT(device, status);
    }

    public static DeviceReconnectFilter.ConnectFailEvent newConnectFailNULL(IBleDevice device)
    {
        return DeviceReconnectFilter.ConnectFailEvent.NULL(device);
    }

    public static ReconnectFilter.ConnectionLostEvent newConnectLostEvent(BleNode node, String macAddress, int failureCount, Interval totalTimeReconnecting, Interval previousDelay, ReconnectFilter.ConnectFailEvent connectionFailEvent, ReconnectFilter.Type type)
    {
        return new ReconnectFilter.ConnectionLostEvent(node, macAddress, failureCount, totalTimeReconnecting, previousDelay, connectionFailEvent, type);
    }

    public static DeviceStateListener.StateEvent newDeviceStateEvent(BleDevice device, int oldStateBits, int newStateBits, int intentMask, int gattStatus)
    {
        return new DeviceStateListener.StateEvent(device, oldStateBits, newStateBits, intentMask, gattStatus);
    }

    public static BondFilter.StateChangeEvent newStateChangeEvent(BleDevice device, int oldStateBits, int newStateBits, int intentMask, int gattStatus)
    {
        return new BondFilter.StateChangeEvent(device, oldStateBits, newStateBits, intentMask, gattStatus);
    }

    // ***********************
    // BleServer related helper methods
    // ***********************

    public static ServerReconnectFilter.ConnectFailEvent newServerConnectFailEvent(BleServer server, BluetoothDevice nativeDevice, ServerReconnectFilter.Status status, int failureCountSoFar, Interval lastAttemptTime, Interval totalAttemptTime, int gattStatus, ReconnectFilter.AutoConnectUsage autoConnectUsage, ArrayList<ServerReconnectFilter.ConnectFailEvent> history)
    {
        return new ServerReconnectFilter.ConnectFailEvent(server, nativeDevice, status, failureCountSoFar, lastAttemptTime, totalAttemptTime, gattStatus, autoConnectUsage, history);
    }

    public static AdvertisingListener.AdvertisingEvent newAdvertisingEvent(BleServer server, AdvertisingListener.Status status)
    {
        return new AdvertisingListener.AdvertisingEvent(server, status);
    }

    public static AddServiceListener.ServiceAddEvent newServiceAddEvent(BleServer server, BluetoothGattService service, AddServiceListener.Status status, int gattStatus, boolean solicited)
    {
        return new AddServiceListener.ServiceAddEvent(server, service, status, gattStatus, solicited);
    }

    public static AddServiceListener.ServiceAddEvent newServiceAddEvent_NULL(BleServer server, BluetoothGattService service)
    {
        return AddServiceListener.ServiceAddEvent.NULL(server, service);
    }

    public static AddServiceListener.ServiceAddEvent newServiceAddEvent_EARLY_OUT(BleServer server, BluetoothGattService service, AddServiceListener.Status status)
    {
        return AddServiceListener.ServiceAddEvent.EARLY_OUT(server, service, status);
    }

    public static ServerConnectListener.ConnectEvent newServerConnectEvent(BleServer server, String macAddress, ServerReconnectFilter.ConnectFailEvent failEvent)
    {
        return new ServerConnectListener.ConnectEvent(server, macAddress, failEvent);
    }

    public static ServerStateListener.StateEvent newServerStateEvent(BleServer server, String macAddress, int oldBits, int newBits, int intentMask, int gattStatus)
    {
        return new ServerStateListener.StateEvent(server, macAddress, oldBits, newBits, intentMask, gattStatus);
    }

    public static ServerReconnectFilter.ConnectFailEvent newServerEarlyOut(BleServer server, BluetoothDevice device, ServerReconnectFilter.Status status)
    {
        return ServerReconnectFilter.ConnectFailEvent.EARLY_OUT(server, device, status);
    }

    public static ServerReconnectFilter.ConnectFailEvent serverNULL(BleServer server, BluetoothDevice device)
    {
        return ServerReconnectFilter.ConnectFailEvent.NULL(server, device);
    }

    public static IncomingListener.IncomingEvent newIncomingEvent(BleServer server, P_DeviceHolder nativeDevice, UUID serviceUuid, UUID charUuid, UUID descUuid, ExchangeListener.Type type, ExchangeListener.Target target, byte[] data_in, int requestId, int offSet, boolean responseNeeded)
    {
        return new IncomingListener.IncomingEvent(server, nativeDevice, serviceUuid, charUuid, descUuid, type, target, data_in, requestId, offSet, responseNeeded);
    }

    public static OutgoingListener.OutgoingEvent newOutgoingEvent(BleServer server, P_DeviceHolder nativeDevice, UUID serviceUuid, UUID charUuid, UUID descUuid, ExchangeListener.Type type, ExchangeListener.Target target, byte[] data_received, byte[] data_sent, int requestId, int offset, boolean responseNeeded, OutgoingListener.Status status, int gattStatus_sent, int gattStatus_received, boolean solicited)
    {
        return new OutgoingListener.OutgoingEvent(server, nativeDevice, serviceUuid, charUuid, descUuid, type, target, data_received, data_sent, requestId, offset, responseNeeded, status, gattStatus_sent, gattStatus_received, solicited);
    }

    public static OutgoingListener.OutgoingEvent newOutgoingEvent(final IncomingListener.IncomingEvent e, final byte[] data_sent, final OutgoingListener.Status status, final int gattStatus_sent, final int gattStatus_received)
    {
        return new OutgoingListener.OutgoingEvent(e, data_sent, status, gattStatus_sent, gattStatus_received);
    }

    public static OutgoingListener.OutgoingEvent newOutgoingEarlyOut(BleServer server, P_DeviceHolder device, UUID serviceUuid, UUID charUuid, FutureData data, OutgoingListener.Status status)
    {
        return OutgoingListener.OutgoingEvent.EARLY_OUT__NOTIFICATION(server, device, serviceUuid, charUuid, data, status);
    }

    public static OutgoingListener.OutgoingEvent outgoingNULL(BleServer server, P_DeviceHolder device, UUID serviceUuid, UUID charUuid)
    {
        return OutgoingListener.OutgoingEvent.NULL__NOTIFICATION(server, device, serviceUuid, charUuid);
    }

    public static HistoricalDataLoadListener.HistoricalDataLoadEvent newHistoricalDataLoadEvent(BleNode node, String macAddress, UUID uuid, EpochTimeRange range, HistoricalDataLoadListener.Status status)
    {
        return new HistoricalDataLoadListener.HistoricalDataLoadEvent(node, macAddress, uuid, range, status);
    }

    public static HistoricalDataQueryListener.HistoricalDataQueryEvent newHistoricalDataQueryEvent(BleNode node, UUID uuid, Cursor cursor, HistoricalDataQueryListener.Status status, String rawQuery)
    {
        return new HistoricalDataQueryListener.HistoricalDataQueryEvent(node, uuid, cursor, status, rawQuery);
    }

    public static MtuTestCallback.MtuTestEvent newMtuTestEvent(BleDevice device, int mtu)
    {
        return new MtuTestCallback.MtuTestEvent(device, mtu);
    }

    public static MtuTestCallback.TestResult newTestResult(BleDevice device, MtuTestCallback.TestResult.Result result, ReadWriteListener.Status writeStatus)
    {
        return new MtuTestCallback.TestResult(device, result, writeStatus);
    }


    public static BleOp createBleOp(UUID serviceUuid, UUID charUuid, UUID descUuid, DescriptorFilter filter, byte[] data, ReadWriteListener.Type type)
    {
        return BleOp.createReadWriteOp(serviceUuid, charUuid, descUuid, filter, data, type);
    }


    public static BleDevice newDevice(IBleDevice deviceImpl)
    {
        return new BleDevice(deviceImpl);
    }

    public static BleServer newServer(IBleServer serverImpl)
    {
        return new BleServer(serverImpl);
    }


    public static IBluetoothGatt newGattLayer(BleDeviceConfig config, BleDevice device)
    {
        return config.newGattLayer(device.getIBleDevice());
    }


    public static boolean boolOrDefault(Boolean bool)
    {
        return Utils_Config.boolOrDefault(bool);
    }

    public static boolean bool(Boolean bool1, Boolean bool2)
    {
        return Utils_Config.bool(bool1, bool2);
    }

    public static BleOp createNewOp(BleOp op)
    {
        return op.createNewOp();
    }

    public static <T extends BleOp> T createDuplicate(T op)
    {
        return (T) op.createDuplicate();
    }

    public static BleManagerConfig nullConfig()
    {
        return BleManagerConfig.NULL;
    }

    public static int bleDeviceStatePurgeableMask()
    {
        return BleDeviceState.PURGEABLE_MASK;
    }

    public static IBleTransaction getIBleTransaction(BleTransaction txn)
    {
        if (txn == null)
            return null;
        return txn.getIBleTransaction();
    }

    public interface TransactionHolder
    {
        void start();

        void update(double timeStep);

        void onEnd(BleTransaction.EndReason endReason);

        BleTransaction.Atomicity getAtomicity();

    }

    public static BleTransaction newTransaction(IBleTransaction txn, P_ITransaction.Type type, final TransactionHolder holder)
    {
        switch (type)
        {
            case AUTH:
                return new BleTransaction.Auth(txn)
                {
                    @Override
                    protected final void start()
                    {
                        if (holder != null)
                            holder.start();
                    }

                    @Override
                    protected final void update(double timeStep)
                    {
                        if (holder != null)
                            holder.update(timeStep);
                    }

                    @Override
                    protected final void onEnd(EndReason reason)
                    {
                        if (holder != null)
                            holder.onEnd(reason);
                    }

                    @Override
                    protected final Atomicity getAtomicity()
                    {
                        if (holder != null)
                            return holder.getAtomicity();
                        else
                            return super.getAtomicity();
                    }
                };
            case INIT:
                return new BleTransaction.Init(txn)
                {
                    @Override
                    protected final void start()
                    {
                        if (holder != null)
                            holder.start();
                    }

                    @Override
                    protected final void update(double timeStep)
                    {
                        if (holder != null)
                            holder.update(timeStep);
                    }

                    @Override
                    protected final void onEnd(EndReason reason)
                    {
                        if (holder != null)
                            holder.onEnd(reason);
                    }

                    @Override
                    protected final Atomicity getAtomicity()
                    {
                        if (holder != null)
                            return holder.getAtomicity();
                        else
                            return super.getAtomicity();
                    }
                };
            case OTA:
                return new BleTransaction.Ota(txn)
                {
                    @Override
                    protected final void start()
                    {
                        if (holder != null)
                            holder.start();
                    }

                    @Override
                    protected final void update(double timeStep)
                    {
                        if (holder != null)
                            holder.update(timeStep);
                    }

                    @Override
                    protected final void onEnd(EndReason reason)
                    {
                        if (holder != null)
                            holder.onEnd(reason);
                    }

                    @Override
                    protected final Atomicity getAtomicity()
                    {
                        if (holder != null)
                            return holder.getAtomicity();
                        else
                            return super.getAtomicity();
                    }
                };
            default:
                return new BleTransaction(txn)
                {
                    @Override
                    protected final void start()
                    {
                        if (holder != null)
                            holder.start();
                    }

                    @Override
                    protected final void update(double timeStep)
                    {
                        if (holder != null)
                            holder.update(timeStep);
                    }

                    @Override
                    protected final void onEnd(EndReason reason)
                    {
                        if (holder != null)
                            holder.onEnd(reason);
                    }

                    @Override
                    protected final Atomicity getAtomicity()
                    {
                        if (holder != null)
                            return holder.getAtomicity();
                        else
                            return super.getAtomicity();
                    }
                };
        }
    }

    public static IBleDevice getIBleDevice(BleDevice device)
    {
        return device.getIBleDevice();
    }

    public static IBleManager getIBleManager(BleManager mgr)
    {
        return mgr.getIBleManager();
    }

    public static IBleServer getIBleServer(BleServer server)
    {
        return server.getIBleServer();
    }

    public static int getConnectionOrdinal(BleDeviceState state)
    {
        return state.getConnectionOrdinal();
    }

    public static boolean wasExplicit(DeviceReconnectFilter.Status status)
    {
        return status.wasExplicit();
    }

    public static boolean canFailConnection(BondListener.Status status)
    {
        return status.canFailConnection();
    }

    public static int getNativeBit(BleDeviceState state)
    {
        return state.getNativeBit();
    }

    public static boolean setCharValue(BleCharacteristic ch, byte[] value)
    {
        return ch.setValue(value);
    }

    public static boolean setDescValue(BleDescriptor desc, byte[] value)
    {
        return desc.setValue(value);
    }

    public static void setCharWriteType(BleCharacteristic ch, ReadWriteListener.Type type)
    {
        ch.setWriteType(type);
    }

    public static int getProperties(BleCharacteristic ch)
    {
        return ch.getProperties();
    }


    // ***********************
    // Please helper methods
    // ***********************

    public static boolean respond(IncomingListener.Please please)
    {
        return please.m_respond;
    }

    public static OutgoingListener getOutgoingListener(IncomingListener.Please please)
    {
        return please.m_outgoingListener;
    }

    public static int gattStatus(IncomingListener.Please please)
    {
        return please.m_gattStatus;
    }

    public static FutureData getFutureData(IncomingListener.Please please)
    {
        return please.m_futureData;
    }

    public static int getPersistanceLevel(BleNodeConfig.HistoricalDataLogFilter.Please please)
    {
        return please.m_persistenceLevel;
    }

    public static boolean shouldPersist(ReconnectFilter.ConnectionLostPlease please)
    {
        return please.shouldPersist();
    }

    public static Interval interval(ReconnectFilter.ConnectionLostPlease please)
    {
        return please.interval();
    }

    public static Interval timeout(ReconnectFilter.ConnectionLostPlease please)
    {
        return please.timeout();
    }

    public static boolean isRetry(ReconnectFilter.ConnectFailPlease please)
    {
        return please.isRetry();
    }

    public static boolean shouldRetry(BondRetryFilter.Please please)
    {
        return please.shouldRetry();
    }

    public static Boolean bond_private(BondFilter.Please please)
    {
        return please.bond_private();
    }

    public static BondListener bondListener(BondFilter.Please please)
    {
        return please.listener();
    }

    public static DeviceReconnectFilter.Timing bondTiming(BondListener.Status status)
    {
        return status.timing();
    }

    public static boolean shouldTryConnectionBugFix(BondFilter.ConnectionBugEvent.Please please)
    {
        return please.shouldTryFix();
    }

    public static P_ConnectFailPlease internalPlease(ReconnectFilter.ConnectFailPlease please)
    {
        return please.please();
    }

    public static boolean accepted(DescriptorFilter.Please please)
    {
        return please != null ? please.isAccepted() : false;
    }

    public static boolean doMtuTest(MtuTestCallback.Please please)
    {
        return please.doTest();
    }

    public static UUID getServiceUuid(MtuTestCallback.Please please)
    {
        return please.serviceUuid();
    }

    public static UUID getCharUuid(MtuTestCallback.Please please)
    {
        return please.charUuid();
    }

    public static ReadWriteListener.Type getWriteType(MtuTestCallback.Please please)
    {
        return please.writeType();
    }

    public static byte[] getData(MtuTestCallback.Please please)
    {
        return please.data();
    }

    public static BleDeviceConfig fromPlease(ScanFilter.Please please)
    {
        return please == null ? null : please.getConfig();
    }

}
