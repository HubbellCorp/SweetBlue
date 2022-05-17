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

import java.util.UUID;

import com.idevicesinc.sweetblue.BleCharacteristic;
import com.idevicesinc.sweetblue.BleDescriptor;
import com.idevicesinc.sweetblue.BleDeviceConfig;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleManagerState;
import com.idevicesinc.sweetblue.BleOp;
import com.idevicesinc.sweetblue.BleStatuses;
import com.idevicesinc.sweetblue.BondListener;
import com.idevicesinc.sweetblue.DeviceReconnectFilter;
import com.idevicesinc.sweetblue.LogOptions;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.ReadWriteListener;
import com.idevicesinc.sweetblue.ReconnectFilter;
import com.idevicesinc.sweetblue.ReconnectFilter.AutoConnectUsage;
import com.idevicesinc.sweetblue.internal.android.IDeviceListener;
import com.idevicesinc.sweetblue.internal.android.P_Bridge_Native;
import com.idevicesinc.sweetblue.internal.android.P_GattHolder;
import com.idevicesinc.sweetblue.utils.CodeHelper;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.P_Const;
import com.idevicesinc.sweetblue.utils.Utils;

/**
 * This class has to deal with 3 different scenarios.<br>
 * 1. The current task makes a native call, then we receive a native callback, and then fail/succeed the task
 * 2. We receive an unexpected native callback (so the current task doesn't match the callback received)
 * 3. A task finishes without getting a native callback (timeouts, cancellations, etc).
 * <br>
 * So far, this only concerns connect/disconnect, and bonding.
 */
final class P_BleDevice_ListenerProcessor implements IDeviceListener.Callback
{

    private final IBleDevice m_device;
    private final P_Logger m_logger;
    private final P_TaskManager m_queue;
    private final IDeviceListener m_nativeListener;


    final PA_Task.I_StateListener m_taskStateListener = P_BleDevice_ListenerProcessor.this::taskStateChanged;


    public P_BleDevice_ListenerProcessor(IBleDevice device)
    {
        m_device = device;
        m_logger = m_device.getIManager().getLogger();
        m_queue = m_device.getIManager().getTaskManager();
        m_nativeListener = device.getIManager().getDeviceListenerFactory().newInstance(this);
    }


    public final IDeviceListener getNativeCallback()
    {
        return m_nativeListener;
    }


    private void taskStateChanged(PA_Task task, PE_TaskState state)
    {
        // TODO - The goal here is to only handle cases in which we DON'T receive a native callback (timeouts, cancels, and the such).
        if (task.getClass() == P_Task_Connect.class)
            onConnectTaskStateChange((P_Task_Connect) task, state);

        else if (task.getClass() == P_Task_Disconnect.class)
            onDisconnectTaskStateChange((P_Task_Disconnect) task, state);

        else if (task.getClass() == P_Task_DiscoverServices.class)
            onDiscoverServicesTaskStateChange((P_Task_DiscoverServices) task, state);

        else if (task.getClass() == P_Task_Bond.class || task.getClass() == P_Task_Unbond.class)
            m_device.getBondManager().onBondTaskStateChange(task, state);

    }

    private void onDiscoverServicesTaskStateChange(P_Task_DiscoverServices task, PE_TaskState state)
    {
        if (state.isEndingState() && state != PE_TaskState.SUCCEEDED && state != PE_TaskState.FAILED)
        {
            final P_DisconnectReason disconnectReason = new P_DisconnectReason(task.getGattStatus())
                    .setConnectFailReason(DeviceReconnectFilter.Status.DISCOVERING_SERVICES_FAILED)
                    .setTxnFailReason(P_Bridge_User.newReadWriteEventNULL(m_device.getBleDevice()));

            if (state == PE_TaskState.FAILED_IMMEDIATELY)
            {
                disconnectReason.setTiming(DeviceReconnectFilter.Timing.IMMEDIATELY);
                m_device.disconnectWithReason(disconnectReason);
            }
            else if (state == PE_TaskState.TIMED_OUT)
            {
                disconnectReason.setTiming(DeviceReconnectFilter.Timing.TIMED_OUT);
                m_device.disconnectWithReason(disconnectReason);
            }
            else
            {
                // If an explicit disconnect() was called while discovering services, we do NOT want to throw another disconnectWithReason (the task will do it when it executes)
                if (!m_device.getIManager().getTaskManager().isInQueue(P_Task_Disconnect.class, m_device))
                {
                    disconnectReason.setTiming(DeviceReconnectFilter.Timing.EVENTUALLY);
                    m_device.disconnectWithReason(disconnectReason);
                }
            }
        }
    }

    private void onDisconnectTaskStateChange(P_Task_Disconnect task, PE_TaskState state)
    {
        // Only add the disconnect task if it's not already in the queue

        // This doesn't make much sense. If the disconnect task is the current, it will NOT be in the queue. Further disconnect tasks in the queue will
        // be tagged as redundant, so why care if it's in the queue or not?
        if (state == PE_TaskState.REDUNDANT && !m_device.getIManager().getTaskManager().isInQueue(P_Task_Disconnect.class, m_device))
            onConnectionStateChange(P_Bridge_Native.newGattHolder(m_device.getNativeGatt().getGatt()), BleStatuses.GATT_STATUS_NOT_APPLICABLE, BleStatuses.DEVICE_DISCONNECTED, false, task.isExplicit());
    }

    private void onConnectTaskStateChange(P_Task_Connect task, PE_TaskState state)
    {
        if (state.isEndingState())
        {
            // This is here to catch the case where the task has ended, but we didn't get a native callback.
            // As this is a failure, we just directly call onNativeConnectFail here, rather than pipe it back through onConnectionStateChange
            if (state != PE_TaskState.SUCCEEDED && state != PE_TaskState.REDUNDANT && state != PE_TaskState.FAILED)
                m_device.onNativeConnectFail(state, task.getGattStatus(), task.getAutoConnectUsage());

                // As redundant means we didn't even make a native request, there will be no callback, so this is here to capture this scenario
            else if (state == PE_TaskState.REDUNDANT)
                onConnectionStateChange(P_Bridge_Native.newGattHolder(m_device.getNativeGatt().getGatt()), BleStatuses.GATT_STATUS_NOT_APPLICABLE, BleStatuses.DEVICE_CONNECTED, false, task.isExplicit());
        }
    }


    @Override
    public final void onConnectionStateChange(final P_GattHolder gatt, final int gattStatus, final int newState, boolean isNativeCallback, final Boolean isExplicit)
    {
        if (isNativeCallback)
            m_logger.log_status_native(m_device.getMacAddress(), gattStatus, CodeHelper.gattConn(newState, m_logger.isEnabled()));
        else
            m_logger.d("onConnectionStateChange()", m_device.getMacAddress(), "Got connection state change from task ending. New state: " + CodeHelper.gattConn(newState, m_logger.isEnabled()));

        m_device.getIManager().getPostManager().runOrPostToUpdateThread(() -> onConnectionStateChange_updateThread(gatt, gattStatus, newState, isExplicit));
    }

    private void onConnectionStateChange_updateThread(final P_GattHolder gatt, final int gattStatus, final int newState, Boolean explicit)
    {
        //--- DRK > NOTE: For some devices disconnecting by turning off the peripheral comes back with a status of 8, which is BluetoothGatt.GATT_SERVER.
        //---				For that same device disconnecting from the app the status is 0. Just an FYI to future developers in case they want to distinguish
        //---				between the two as far as user intent or something.

        //--- RB > NOTE: Regarding the above comment, 8 is actually BleStatuses.CONN_TIMEOUT -- it seems connection status codes have different variables
        //---				associated to them. Some of them share the same value as BluetoothGatt status codes. This is fixed now with the new log_conn_status_native
        //---				logger method (and the values are in BleStatuses)

        if (newState == BleStatuses.DEVICE_DISCONNECTED)
            onNativeDisconnect(gatt, gattStatus, newState, explicit);

        else if (newState == BleStatuses.DEVICE_CONNECTING)
            onNativeConnecting(gatt, gattStatus, newState);

        else if (newState == BleStatuses.DEVICE_CONNECTED)
            onNativeConnect(gatt, gattStatus, newState);

            //--- DRK > NOTE: never seen this case happen.
        else if (newState == BleStatuses.DEVICE_DISCONNECTING)
            onNativeDisconnecting(gatt, newState);

        else
        {
            m_device.getIManager().ASSERT(false, "Got a new device state that is not connected, connecting, disconnected, or disconnecting!");
            m_device.getNativeManager().updateNativeConnectionState(gatt);
        }
    }

    private void onNativeConnecting(final P_GattHolder gatt, final int gattStatus, final int newState)
    {
        if (Utils.isSuccess(gattStatus))
        {
            m_device.getNativeManager().updateNativeConnectionState(gatt, newState);

            m_device.onConnecting(false, false, P_BondManager.OVERRIDE_EMPTY_STATES, true);

            if (!m_queue.isCurrent(P_Task_Connect.class, m_device))
            {
                P_Task_Connect task = new P_Task_Connect(m_device, m_taskStateListener, false, PE_TaskPriority.FOR_IMPLICIT_BONDING_AND_CONNECTING);
                m_queue.add(task);
            }

            m_queue.fail(P_Task_Disconnect.class, m_device);
        }
        else
        {
            onNativeConnectFail(gatt, gattStatus);
        }
    }

    private void onNativeConnect(final P_GattHolder gatt, final int gattStatus, final int newState)
    {
        if (Utils.isSuccess(gattStatus))
        {
            m_device.getNativeManager().updateNativeConnectionState(gatt, newState);

            m_queue.fail(P_Task_Disconnect.class, m_device);

            P_Task_Connect connectTask = m_queue.getCurrent(P_Task_Connect.class, m_device);

            boolean explicit = connectTask != null && connectTask.isExplicit();
            if (connectTask != null)
            {
                connectTask.succeed();
                m_device.setToAlwaysUseAutoConnectIfItWorked();
            }
            m_device.onNativeConnect(explicit);
        }
        else
        {
            onNativeConnectFail(gatt, gattStatus);
        }
    }

    private void onNativeDisconnecting(final P_GattHolder gatt, final int newState)
    {
        m_logger.e("Actually natively disconnecting!"); // DRK > error level just so it's noticeable...never seen this.

        m_device.getNativeManager().updateNativeConnectionState(gatt, newState);

        //			m_device.onDisconnecting();

        if (!m_queue.isCurrent(P_Task_Disconnect.class, m_device))
        {
            P_Task_Disconnect task = new P_Task_Disconnect(m_device, m_taskStateListener, false, PE_TaskPriority.FOR_IMPLICIT_BONDING_AND_CONNECTING, true);
            m_queue.add(task);
        }

        m_queue.fail(P_Task_Connect.class, m_device);
    }

    private void onNativeDisconnect(final P_GattHolder gatt, final int gattStatus, final int newState, Boolean explicit)
    {
        m_device.getNativeManager().updateNativeConnectionState(gatt, newState);

        final P_Task_Connect connectTask = m_queue.getCurrent(P_Task_Connect.class, m_device);

        if (connectTask != null)
        {
            connectTask.fail();
            m_device.onNativeConnectFail(PE_TaskState.FAILED, connectTask.getGattStatus(), connectTask.getAutoConnectUsage());
        }
        else
        {
            final P_Task_Disconnect disconnectTask = m_queue.getCurrent(P_Task_Disconnect.class, m_device);

            m_device.getNativeManager().closeGattIfNeeded(false);

            final BleDeviceConfig.RefreshOption option = m_device.conf_device().gattRefreshOption != null ? m_device.conf_device().gattRefreshOption : m_device.conf_mngr().gattRefreshOption;

            if (option == BleDeviceConfig.RefreshOption.AFTER_DISCONNECTING)
            {
                if (!Utils.refreshGatt(m_device.getNativeGatt().getGatt()))
                {
                    m_logger.e("Unable to refresh gatt. Gatt is null = " + (m_device.getNativeGatt().getGatt() == null));
                }
            }

            if (disconnectTask != null)
            {
                disconnectTask.onNativeSuccess(gattStatus);
                // attemptShortTermReconnect should only be set to true when the disconnect was not explicit.
                m_device.onNativeDisconnect(disconnectTask.isExplicit(), gattStatus, !disconnectTask.isExplicit(), disconnectTask.shouldSaveLastDisconnect());
            }
            else
            {
                boolean doShortTermReconnect = m_device.getIManager().is(BleManagerState.ON) && !m_device.getIManager().is(BleManagerState.TURNING_OFF);
                DeviceReconnectFilter filter = m_device.getListener_Reconnect();
                if (filter == null)
                    filter = m_device.getIManager().getDefaultDeviceReconnectFilter();
                boolean wasConnected = m_device.is(BleDeviceState.CONNECTED);
                if (filter != null)
                {
                    if (wasConnected)
                    {
                        DeviceReconnectFilter.ConnectFailEvent failevent = P_Bridge_User.newConnectFailNULL(m_device);
                        // Make sure to use the short term reconnect manager, as this is a lost connection (so next step would be short term, before going into long term)
                        ReconnectFilter.ConnectionLostPlease please = m_device.getConnectionManager().m_reconnectMngr_shortTerm.onConnectionLost(failevent);
                        doShortTermReconnect = P_Bridge_User.shouldPersist(please);
                    }
                }
                m_device.onNativeDisconnect(explicit == null ? false : explicit, gattStatus, doShortTermReconnect, true);
            }
        }
    }

    private void onNativeConnectFail(final P_GattHolder gatt, final int gattStatus)
    {
        //--- DRK > NOTE: Making an assumption that the underlying stack agrees that the connection state is STATE_DISCONNECTED.
        //---				This is backed up by basic testing, but even if the underlying stack uses a different value, it can probably
        //---				be assumed that it will eventually go to STATE_DISCONNECTED, so SweetBlue library logic is sounder "living under the lie" for a bit regardless.
        m_device.getNativeManager().updateNativeConnectionState(gatt, BleStatuses.DEVICE_DISCONNECTED);

        final P_Task_Connect connectTask = m_queue.getCurrent(P_Task_Connect.class, m_device);

        if (connectTask != null)
        {
            connectTask.onNativeFail(gattStatus);
        }
        else
        {
            m_device.onNativeConnectFail(null, gattStatus, AutoConnectUsage.UNKNOWN);
        }
    }

    @Override
    public final void onServicesDiscovered(final P_GattHolder gatt, final int gattStatus)
    {
        m_logger.log_status_native(m_device.getMacAddress(), gattStatus);

        m_device.getIManager().getPostManager().runOrPostToUpdateThread(() -> onServicesDiscovered_updateThread(gatt, gattStatus));
    }

    private void onServicesDiscovered_updateThread(final P_GattHolder gatt, final int gattStatus)
    {
        final P_Task_DiscoverServices task = m_queue.getCurrent(P_Task_DiscoverServices.class, m_device);

        if (Utils.isSuccess(gattStatus))
        {
            if (task != null)
                task.succeed();
            m_device.onServicesDiscovered();
        }
        else
        {
            if (task != null)
            {
                task.fail();
                final P_DisconnectReason disconnectReason = new P_DisconnectReason(gattStatus)
                        .setConnectFailReason(DeviceReconnectFilter.Status.DISCOVERING_SERVICES_FAILED)
                        .setTxnFailReason(P_Bridge_User.newReadWriteEventNULL(m_device.getBleDevice()));
                disconnectReason.setTiming(DeviceReconnectFilter.Timing.EVENTUALLY);
                m_device.disconnectWithReason(disconnectReason);
            }
            // TODO - Does anything need to be done here? It shouldn't really be possible to get this callback without a discover services task
            m_device.getIManager().ASSERT(task == null, "Got onServicesDiscovered when there is no discover services task in the queue!");
        }
    }

    @Override
    public final void onCharacteristicRead(final P_GattHolder gatt, final BleCharacteristic characteristic, final int gattStatus)
    {
        final byte[] value = characteristic.getValue() == null ? null : characteristic.getValue().clone();

        final UUID uuid = characteristic.getCharacteristic().getUuid();
        m_logger.log_status_native(m_device.getMacAddress(), gattStatus, m_logger.charName(uuid));

        m_device.getIManager().getPostManager().runOrPostToUpdateThread(() -> onCharacteristicRead_updateThread(gatt, characteristic, gattStatus, value));
    }

    private void onCharacteristicRead_updateThread(final P_GattHolder gatt, final BleCharacteristic characteristic, final int gattStatus, final byte[] value)
    {
        final P_Task_Read readTask = m_queue.getCurrent(P_Task_Read.class, m_device);

        if (readTask != null && readTask.isFor(characteristic))
        {
            readTask.onCharacteristicRead(gatt, characteristic.getUuid(), value, gattStatus);
        }
        else
        {
            fireUnsolicitedEvent(characteristic, BleDescriptor.NULL, ReadWriteListener.Type.READ, ReadWriteListener.Target.CHARACTERISTIC, value, gattStatus);
        }
    }

    @Override
    public final void onCharacteristicWrite(final P_GattHolder gatt, final BleCharacteristic characteristic, final int gattStatus)
    {
        final byte[] data = characteristic.getValue();

        final UUID uuid = characteristic.getUuid();
        m_logger.log_status_native(m_device.getMacAddress(), gattStatus, m_logger.charName(uuid));

        m_device.getIManager().getPostManager().runOrPostToUpdateThread(() -> onCharacteristicWrite_updateThread(gatt, characteristic, data, gattStatus));
    }

    private void onCharacteristicWrite_updateThread(final P_GattHolder gatt, final BleCharacteristic characteristic, final byte[] data, final int gattStatus)
    {
        final P_Task_Write task = m_queue.getCurrent(P_Task_Write.class, m_device);

        if (task != null && task.isFor(characteristic))
        {
            task.onCharacteristicWrite(gatt, characteristic.getUuid(), gattStatus);
        }
        else
        {
            final P_Task_TestMtu testTask = m_queue.getCurrent(P_Task_TestMtu.class, m_device);
            if (testTask != null && testTask.isFor(characteristic))
                testTask.onCharacteristicWrite(gatt, characteristic.getUuid(), gattStatus);
            else
                fireUnsolicitedEvent(characteristic, BleDescriptor.NULL, ReadWriteListener.Type.WRITE, ReadWriteListener.Target.CHARACTERISTIC, data, gattStatus);

        }
    }

    private void fireUnsolicitedEvent(final BleCharacteristic bleChar, final BleDescriptor descriptor, ReadWriteListener.Type type, final ReadWriteListener.Target target, final byte[] data, final int gattStatus)
    {
        final BleCharacteristic characteristic = bleChar == null ? BleCharacteristic.NULL : bleChar;
        final ReadWriteListener.Type type_modified = !characteristic.isNull() ? P_DeviceServiceManager.modifyResultType(characteristic, type) : type;
        final ReadWriteListener.Status status = Utils.isSuccess(gattStatus) ? ReadWriteListener.Status.SUCCESS : ReadWriteListener.Status.REMOTE_GATT_FAILURE;
        final UUID serviceUuid = !characteristic.isNull() ? characteristic.getCharacteristic().getService().getUuid() : ReadWriteListener.ReadWriteEvent.NON_APPLICABLE_UUID;

        final UUID characteristicUuid = !characteristic.isNull() ? characteristic.getCharacteristic().getUuid() : ReadWriteListener.ReadWriteEvent.NON_APPLICABLE_UUID;
        final UUID descriptorUuid = !descriptor.isNull() ? descriptor.getDescriptor().getUuid() : ReadWriteListener.ReadWriteEvent.NON_APPLICABLE_UUID;

        final double time = Interval.DISABLED.secs();
        final boolean solicited = false;

        final ReadWriteListener.ReadWriteEvent e;

        final BleOp op = P_Bridge_User.createBleOp(serviceUuid, characteristicUuid, descriptorUuid, null, data, type);

        if (target == ReadWriteListener.Target.CHARACTERISTIC || target == ReadWriteListener.Target.DESCRIPTOR)
        {
            e = P_Bridge_User.newReadWriteEvent(m_device.getBleDevice(), op, type_modified, target, status, gattStatus, time, time, solicited);
        }
        else if (target == ReadWriteListener.Target.RSSI)
        {
            e = P_Bridge_User.newReadWriteEventRssi(m_device.getBleDevice(), type, m_device.getRssi(), status, gattStatus, time, time, solicited);
        }
        else if (target == ReadWriteListener.Target.MTU)
        {
            e = P_Bridge_User.newReadWriteEventMtu(m_device.getBleDevice(), m_device.getMtu(), status, gattStatus, time, time, solicited);
        }
        else if (target == ReadWriteListener.Target.PHYSICAL_LAYER)
        {
            e = P_Bridge_User.newReadWriteEventPhy(m_device.getBleDevice(), status, gattStatus, m_device.getPhy_private(), time, time, solicited);
        }
        else
        {
            return;
        }

        m_device.invokeReadWriteCallback(null, e);
    }

    @Override
    public final void onReliableWriteCompleted(final P_GattHolder gatt, final int gattStatus)
    {
        m_logger.log_status_native(m_device.getMacAddress(), gattStatus);

        m_device.getIManager().getPostManager().runOrPostToUpdateThread(() -> onReliableWriteCompleted_updateThread(gatt, gattStatus));
    }

    private void onReliableWriteCompleted_updateThread(final P_GattHolder gatt, final int gattStatus)
    {

        final P_Task_ExecuteReliableWrite task = m_queue.getCurrent(P_Task_ExecuteReliableWrite.class, m_device);

        if (task != null)
        {
            task.onReliableWriteCompleted(gatt, gattStatus);
        }
        else
        {
            m_device.getReliableWriteManager().onReliableWriteCompleted_unsolicited(gatt, gattStatus);
        }
    }

    @Override
    public final void onReadRemoteRssi(final P_GattHolder gatt, final int rssi, final int gattStatus)
    {
        m_logger.log_status_native(m_device.getMacAddress(), gattStatus);

        m_device.getIManager().getPostManager().runOrPostToUpdateThread(() -> onReadRemoteRssi_updateThread(gatt, rssi, gattStatus));
    }

    private void onReadRemoteRssi_updateThread(final P_GattHolder gatt, final int rssi, final int gattStatus)
    {
        if (Utils.isSuccess(gattStatus))
        {
            m_device.updateRssi(rssi, false);
        }

        final P_Task_ReadRssi task = m_queue.getCurrent(P_Task_ReadRssi.class, m_device);

        if (task != null)
        {
            task.onReadRemoteRssi(gatt, rssi, gattStatus);
        }
        else
        {
            fireUnsolicitedEvent(BleCharacteristic.NULL, BleDescriptor.NULL, ReadWriteListener.Type.READ, ReadWriteListener.Target.RSSI, P_Const.EMPTY_BYTE_ARRAY, gattStatus);
        }
    }

    @Override
    public final void onDescriptorWrite(final P_GattHolder gatt, final BleDescriptor descriptor, final int gattStatus)
    {
        final UUID uuid = descriptor.getUuid();
        m_logger.i_native(m_logger.descriptorName(uuid));
        m_logger.log_status_native(m_device.getMacAddress(), gattStatus);

        final byte[] data = descriptor.getValue();

        m_device.getIManager().getPostManager().runOrPostToUpdateThread(() -> onDescriptorWrite_updateThread(gatt, descriptor, data, gattStatus));
    }

    private void onDescriptorWrite_updateThread(final P_GattHolder gatt, final BleDescriptor descriptor, final byte[] data, final int gattStatus)
    {
        final UUID uuid = descriptor.getUuid();

        final P_Task_WriteDescriptor task_write = m_queue.getCurrent(P_Task_WriteDescriptor.class, m_device);

        if (task_write != null && task_write.isFor(descriptor))
        {
            task_write.onDescriptorWrite(gatt, descriptor.getUuid(), gattStatus);
        }
        else
        {
            final P_Task_ToggleNotify task_toggleNotify = m_queue.getCurrent(P_Task_ToggleNotify.class, m_device);

            if (task_toggleNotify != null && task_toggleNotify.isFor(descriptor))
            {
                task_toggleNotify.onDescriptorWrite(gatt, uuid, gattStatus);
            }
            else
            {
                fireUnsolicitedEvent(descriptor.getCharacteristic(), descriptor, ReadWriteListener.Type.WRITE, ReadWriteListener.Target.DESCRIPTOR, data, gattStatus);
            }
        }
    }

    @Override
    public final void onDescriptorRead(final P_GattHolder gatt, final BleDescriptor descriptor, final int gattStatus)
    {
        final byte[] data = descriptor.getValue();
        final UUID uuid = descriptor.getUuid();
        m_logger.i_native(m_logger.descriptorName(uuid));
        m_logger.log_status_native(m_device.getMacAddress(), gattStatus);

        m_device.getIManager().getPostManager().runOrPostToUpdateThread(() -> onDescriptorRead_updateThread(gatt, descriptor, data, gattStatus));
    }

    private void onDescriptorRead_updateThread(final P_GattHolder gatt, final BleDescriptor descriptor, final byte[] data, final int gattStatus)
    {
        final P_Task_ReadDescriptor task_readDesc = m_queue.getCurrent(P_Task_ReadDescriptor.class, m_device);

        if (task_readDesc != null && task_readDesc.descriptorMatches(descriptor))
        {
            task_readDesc.onDescriptorRead(gatt, descriptor.getUuid(), data, gattStatus);
        }
        else
        {
            final PA_Task_ReadOrWrite task_read = m_queue.getCurrent(PA_Task_ReadOrWrite.class, m_device);

            if (task_read != null && task_read.descriptorMatches(descriptor))
                task_read.onDescriptorReadCallback(gatt, descriptor, data, gattStatus);
            else
                fireUnsolicitedEvent(descriptor.getCharacteristic(), descriptor, ReadWriteListener.Type.READ, ReadWriteListener.Target.DESCRIPTOR, data, gattStatus);
        }

    }

    @Override
    public final void onCharacteristicChanged(final P_GattHolder gatt, final BleCharacteristic characteristic)
    {
        final byte[] value = characteristic.getValue() == null ? null : characteristic.getValue().clone();

        final UUID characteristicUuid = characteristic.getUuid();
        m_logger.log_native(LogOptions.LogLevel.DEBUG.nativeBit(), m_device.getMacAddress(), "characteristic=" + characteristicUuid.toString());

        m_device.getIManager().getPostManager().runOrPostToUpdateThread(() -> onCharacteristicChanged_updateThread(gatt, characteristic, value));
    }

    private void onCharacteristicChanged_updateThread(final P_GattHolder gatt, final BleCharacteristic characteristic, final byte[] value)
    {
        final UUID characteristicUuid = characteristic.getUuid();
        final UUID serviceUuid = characteristic.getService().getUuid();

        m_device.getPollManager().onCharacteristicChangedFromNativeNotify(serviceUuid, characteristicUuid, value);
    }

    public final void onNativeBondRequest_updateThread(IBleDevice device)
    {
        device.getBondManager().onNativeBondRequest();
    }

    public final void onNativeBondStateChanged_updateThread(int previousState, int newState, int failReason)
    {
        if (newState == BleStatuses.DEVICE_ERROR)
        {
            P_TaskManager queue = m_device.getIManager().getTaskManager();
            queue.fail(P_Task_Bond.class, m_device);
            queue.fail(P_Task_Unbond.class, m_device);

            m_logger.e("newState for bond is BluetoothDevice.ERROR!(?)");
        }
        else if (newState == BleStatuses.DEVICE_BOND_UNBONDED)
        {
            final P_Task_Bond bondTask = m_queue.getCurrent(P_Task_Bond.class, m_device);
            final P_Task_Unbond unbondTask = m_queue.getCurrent(P_Task_Unbond.class, m_device);

            if (bondTask != null)
            {
                bondTask.onNativeFail(failReason);
            }
            else if (!m_queue.succeed(P_Task_Unbond.class, m_device))
            {
                if (previousState == BleStatuses.DEVICE_BOND_BONDING || previousState == BleStatuses.DEVICE_BOND_UNBONDED)
                {
                    m_device.getBondManager().onNativeBondFailed(PA_StateTracker.E_Intent.UNINTENTIONAL, BondListener.Status.FAILED_EVENTUALLY, failReason, false);
                }
                else
                {
                    m_device.getBondManager().onNativeUnbond(PA_StateTracker.E_Intent.UNINTENTIONAL);
                }
            }
            else
            {
                // The task succeeded. The BondManager monitors the task state, and will call onNativeUnbond when the Unbond task succeeds (or is redundant)
            }
        }
        else if (newState == BleStatuses.DEVICE_BOND_BONDING)
        {
            final P_Task_Bond task = m_queue.getCurrent(P_Task_Bond.class, m_device);
            PA_StateTracker.E_Intent intent = task != null && task.isExplicit() ? PA_StateTracker.E_Intent.INTENTIONAL : PA_StateTracker.E_Intent.UNINTENTIONAL;
            boolean isCurrent = task != null; // avoiding erroneous dead code warning from putting this directly in if-clause below.
            m_device.getBondManager().onNativeBonding(intent);

            if (!isCurrent)
            {
                m_queue.add(new P_Task_Bond(m_device, /*explicit=*/false, /*isDirect=*/false, /*partOfConnection=*/false, m_taskStateListener, PE_TaskPriority.FOR_IMPLICIT_BONDING_AND_CONNECTING, P_Task_Bond.E_TransactionLockBehavior.PASSES));
            }

            m_queue.fail(P_Task_Unbond.class, m_device);
        }
        else if (newState == BleStatuses.DEVICE_BOND_BONDED)
        {
            m_queue.fail(P_Task_Unbond.class, m_device);

            final P_Task_Bond task = m_queue.getCurrent(P_Task_Bond.class, m_device);

            if (task != null)
            {
                task.onNativeSuccess();
            }
            else
            {
                m_device.getBondManager().onNativeBond(PA_StateTracker.E_Intent.UNINTENTIONAL);
            }
        }
    }

    @Override
    public final void onMtuChanged(final P_GattHolder gatt, final int mtu, final int gattStatus)
    {
        m_logger.log_status_native(m_device.getMacAddress(), gattStatus);

        m_device.getIManager().getPostManager().runOrPostToUpdateThread(() -> onMtuChanged_updateThread(gatt, mtu, gattStatus));
    }

    private void onMtuChanged_updateThread(P_GattHolder gatt, int mtu, int gattStatus)
    {
        if (Utils.isSuccess(gattStatus))
        {
            m_device.updateMtu(mtu);
        }

        final P_Task_RequestMtu task = m_queue.getCurrent(P_Task_RequestMtu.class, m_device);

        if (task != null)
        {
            task.onMtuChanged(gatt, mtu, gattStatus);
        }
        else
        {
            fireUnsolicitedEvent(BleCharacteristic.NULL, BleDescriptor.NULL, ReadWriteListener.Type.WRITE, ReadWriteListener.Target.MTU, P_Const.EMPTY_BYTE_ARRAY, gattStatus);
        }
    }

    @Override
    public void onPhyRead(final P_GattHolder gatt, final int txPhy, final int rxPhy, final int gattStatus)
    {
        m_logger.log_status_native(m_device.getMacAddress(), gattStatus);

        m_device.getIManager().getPostManager().runOrPostToUpdateThread(() -> onPhyRead_updateThread(gatt, txPhy, rxPhy, gattStatus));
    }

    private void onPhyRead_updateThread(P_GattHolder gatt, int txPhy, int rxPhy, int status)
    {
        final P_Task_ReadPhysicalLayer task = m_queue.getCurrent(P_Task_ReadPhysicalLayer.class, m_device);

        if (task != null)
        {
            if (Utils.isSuccess(status))
                task.succeed(status, txPhy, rxPhy);
            else
                task.fail(ReadWriteListener.Status.REMOTE_GATT_FAILURE, status);
        }
        else
            fireUnsolicitedEvent(BleCharacteristic.NULL, BleDescriptor.NULL, ReadWriteListener.Type.READ, ReadWriteListener.Target.PHYSICAL_LAYER, P_Const.EMPTY_BYTE_ARRAY, status);
    }

    @Override
    public void onPhyUpdate(final P_GattHolder gatt, final int txPhy, final int rxPhy, final int gattStatus)
    {
        m_logger.log_status_native(m_device.getMacAddress(), gattStatus);

        m_device.getIManager().getPostManager().runOrPostToUpdateThread(() -> onPhyUpdate_updateThread(gatt, txPhy, rxPhy, gattStatus));
    }

    private void onPhyUpdate_updateThread(P_GattHolder gatt, int txPhy, int rxPhy, int gattStatus)
    {
        final P_Task_SetPhysicalLayer task = m_queue.getCurrent(P_Task_SetPhysicalLayer.class, m_device);

        if (task != null)
        {
            if (Utils.isSuccess(gattStatus))
                task.succeed(gattStatus, txPhy, rxPhy);
            else
                task.fail(ReadWriteListener.Status.REMOTE_GATT_FAILURE, gattStatus);
        }
        else
            fireUnsolicitedEvent(BleCharacteristic.NULL, BleDescriptor.NULL, ReadWriteListener.Type.WRITE, ReadWriteListener.Target.PHYSICAL_LAYER, P_Const.EMPTY_BYTE_ARRAY, gattStatus);
    }
}
