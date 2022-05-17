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

import static com.idevicesinc.sweetblue.IncomingListener.*;
import static com.idevicesinc.sweetblue.OutgoingListener.*;

import com.idevicesinc.sweetblue.AddServiceListener;
import com.idevicesinc.sweetblue.BleCharacteristic;
import com.idevicesinc.sweetblue.BleDescriptor;
import com.idevicesinc.sweetblue.BleService;
import com.idevicesinc.sweetblue.BleStatuses;
import com.idevicesinc.sweetblue.ExchangeListener;
import com.idevicesinc.sweetblue.IncomingListener;
import com.idevicesinc.sweetblue.OutgoingListener;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.ServerReconnectFilter;
import com.idevicesinc.sweetblue.internal.android.IServerListener;
import com.idevicesinc.sweetblue.internal.android.P_DeviceHolder;
import com.idevicesinc.sweetblue.internal.android.ProfileConst;
import com.idevicesinc.sweetblue.utils.CodeHelper;
import com.idevicesinc.sweetblue.utils.P_Const;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.Uuids;


final class P_BleServer_ListenerProcessor implements IServerListener.Callback
{
    private final IBleServer m_server;
    private final P_Logger m_logger;
    private final P_TaskManager m_queue;
    private final IServerListener m_nativeListener;

    final PA_Task.I_StateListener m_taskStateListener = new TaskStateListener();


    P_BleServer_ListenerProcessor(IBleServer server)
    {
        m_server = server;
        m_logger = m_server.getIManager().getLogger();
        m_queue = m_server.getIManager().getTaskManager();
        m_nativeListener = server.getIManager().getServerListenerFactory().newInstance(this);
    }


    public final IServerListener getInternalListener()
    {
        return m_nativeListener;
    }


    private void hasCurrentDisconnectTaskFor(final P_DeviceHolder device, HasTaskListener<P_Task_DisconnectServer> function)
    {
        final P_Task_DisconnectServer disconnectTask = m_queue.getCurrent(P_Task_DisconnectServer.class, m_server);

        if (function != null)
            function.gotTask(disconnectTask, disconnectTask != null && disconnectTask.isFor(m_server, device.getAddress()));
    }

    private boolean hasCurrentConnectTaskFor(final P_DeviceHolder device, HasTaskListener<P_Task_ConnectServer> function)
    {
        final P_Task_ConnectServer connectTask = m_queue.getCurrent(P_Task_ConnectServer.class, m_server);

        boolean isFor = connectTask != null && connectTask.isFor(m_server, device.getAddress());

        if (function != null)
            function.gotTask(connectTask, isFor);

        return isFor;
    }

    private void failDisconnectTaskIfPossibleFor(final P_DeviceHolder device)
    {
        final P_Task_DisconnectServer disconnectTask = m_queue.getCurrent(P_Task_DisconnectServer.class, m_server);

        if (disconnectTask != null && disconnectTask.isFor(m_server, device.getAddress()))
            m_queue.fail(P_Task_DisconnectServer.class, m_server);
    }

    private boolean failConnectTaskIfPossibleFor(final P_DeviceHolder device, final int gattStatus)
    {
        return hasCurrentConnectTaskFor(device, (task, isTheTask) -> {
            if (isTheTask)
                task.onNativeFail(gattStatus);
        });
    }

    private void onNativeConnectFail(final P_DeviceHolder nativeDevice, final int gattStatus)
    {
        //--- DRK > NOTE: Making an assumption that the underlying stack agrees that the connection state is STATE_DISCONNECTED.
        //---				This is backed up by basic testing, but even if the underlying stack uses a different value, it can probably
        //---				be assumed that it will eventually go to STATE_DISCONNECTED, so SweetBlue library logic is sounder "living under the lie" for a bit regardless.
        m_server.getNativeManager().updateNativeConnectionState(nativeDevice.getAddress(), ProfileConst.STATE_DISCONNECTED);

        hasCurrentConnectTaskFor(nativeDevice, (task, isTheTask) -> {
            if (isTheTask)
                task.onNativeFail(gattStatus);
            else
                m_server.onNativeConnectFail(nativeDevice, ServerReconnectFilter.Status.NATIVE_CONNECTION_FAILED_EVENTUALLY, gattStatus);
        });
    }

    @Override
    public final void onConnectionStateChange(final P_DeviceHolder device, final int gattStatus, final int newState)
    {
        m_server.getIManager().getPostManager().runOrPostToUpdateThread(() -> onConnectionStateChange_updateThread(device, gattStatus, newState));
    }

    @Override
    public final void onMtuChanged(P_DeviceHolder device, int mtu)
    {
    }

    @Override
    public final void onPhyUpdate(P_DeviceHolder device, int txPhy, int rxPhy, int status)
    {
    }

    @Override
    public final void onPhyRead(P_DeviceHolder device, int txPhy, int rxPhy, int status)
    {
    }

    private void onConnectionStateChange_updateThread(final P_DeviceHolder device, final int gattStatus, final int newState)
    {
        final String macAddress = device != null ? device.getAddress() : null;
        m_logger.log_conn_status_native(macAddress, gattStatus, CodeHelper.gattConn(newState, m_logger.isEnabled()));

        if (newState == ProfileConst.STATE_DISCONNECTED)
        {
            m_server.getNativeManager().updateNativeConnectionState(macAddress, newState);

            if (!failConnectTaskIfPossibleFor(device, gattStatus))
            {
                hasCurrentDisconnectTaskFor(device, (task, isTheTask) -> {
                    if (isTheTask)
                        task.onNativeSuccess(gattStatus);
                    else
                        m_server.onNativeDisconnect(device.getAddress(), /*explicit=*/false, gattStatus);
                });
            }
        }
        else if (newState == ProfileConst.STATE_CONNECTING)
        {
            if (Utils.isSuccess(gattStatus))
            {
                m_server.getNativeManager().updateNativeConnectionState(macAddress, newState);

                failDisconnectTaskIfPossibleFor(device);

                hasCurrentConnectTaskFor(device, (connectTask, isTheTask) -> {
                    if (!isTheTask)
                    {
                        final P_Task_ConnectServer task = new P_Task_ConnectServer(m_server, device, m_taskStateListener, /*explicit=*/false, PE_TaskPriority.FOR_IMPLICIT_BONDING_AND_CONNECTING);

                        m_queue.add(task);
                    }
                    else
                        m_server.onNativeConnecting_implicit(macAddress);
                });
            }
            else
            {
                onNativeConnectFail(device, gattStatus);
            }
        }
        else if (newState == ProfileConst.STATE_CONNECTED)
        {
            if (Utils.isSuccess(gattStatus))
            {
                m_server.getNativeManager().updateNativeConnectionState(macAddress, newState);

                failDisconnectTaskIfPossibleFor(device);

                hasCurrentConnectTaskFor(device, (task, isTheTask) -> {
                    if (isTheTask)
                        task.succeed();
                    else
                        m_server.onNativeConnect(macAddress, /*explicit=*/false);
                });
            }
            else
            {
                onNativeConnectFail(device, gattStatus);
            }
        }
        //--- DRK > NOTE: never seen this case happen with BleDevice, we'll see if it happens with the server.
        else if (newState == ProfileConst.STATE_DISCONNECTING)
        {
            m_server.getNativeManager().updateNativeConnectionState(macAddress, newState);

            //--- DRK > error level just so it's noticeable...never seen this with client connections so we'll see if it hits with server ones.
            m_logger.e("Actually natively disconnecting server!");

            hasCurrentDisconnectTaskFor(device, (disconnectTask, isTheTask) -> {
                if (!isTheTask)
                {
                    P_Task_DisconnectServer task = new P_Task_DisconnectServer(m_server, device, m_taskStateListener, /*explicit=*/false, PE_TaskPriority.FOR_IMPLICIT_BONDING_AND_CONNECTING);
                    m_queue.add(task);
                }
            });

            failConnectTaskIfPossibleFor(device, gattStatus);
        }
        else
        {
            m_server.getNativeManager().updateNativeConnectionState(device);
        }
    }

    @Override
    public final void onServiceAdded(final int gattStatus, final BleService service)
    {
        m_server.getIManager().getPostManager().runOrPostToUpdateThread(() -> onServiceAdded_updateThread(gattStatus, service));
    }

    private void onServiceAdded_updateThread(final int gattStatus, final BleService service)
    {
        final P_Task_AddService task = m_queue.getCurrent(P_Task_AddService.class, m_server);

        if (task != null && task.getService().equals(service))
        {
            task.onServiceAdded(gattStatus, service);
        }
        else
        {
            final AddServiceListener.Status status = Utils.isSuccess(gattStatus) ? AddServiceListener.Status.SUCCESS : AddServiceListener.Status.FAILED_EVENTUALLY;
            final AddServiceListener.ServiceAddEvent e = P_Bridge_User.newServiceAddEvent(m_server.getBleServer(), service.getService(), status, gattStatus, /*solicited=*/false);

            m_server.getServerServiceManager().invokeListeners(e, null);
        }
    }

    private OutgoingEvent newEarlyOutResponse_Read(final P_DeviceHolder device, final UUID serviceUuid, final UUID charUuid, final UUID descUuid_nullable, final int requestId, final int offset, final Status status)
    {
        final Target target = descUuid_nullable == null ? Target.CHARACTERISTIC : Target.DESCRIPTOR;

        final OutgoingEvent e = P_Bridge_User.newOutgoingEvent(
                m_server.getBleServer(), device, serviceUuid, charUuid, descUuid_nullable, Type.READ, target, P_Const.EMPTY_BYTE_ARRAY, P_Const.EMPTY_BYTE_ARRAY,
                requestId, offset, /*responseNeeded=*/true, status, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BleStatuses.GATT_STATUS_NOT_APPLICABLE, /*solicited=*/true
        );

        return e;
    }

    private void onReadRequest_updateThread(final P_DeviceHolder device, final int requestId, final int offset, final UUID serviceUuid, final UUID charUuid, final UUID descUuid_nullable)
    {
        final Target target = descUuid_nullable == null ? Target.CHARACTERISTIC : Target.DESCRIPTOR;

        final IncomingListener listener = m_server.getListener_Incoming() != null ? m_server.getListener_Incoming() : m_server.getIManager().getDefaultServerIncomingListener();

        if (listener == null)
        {
            m_server.invokeOutgoingListeners(newEarlyOutResponse_Read(device, serviceUuid, charUuid, /*descUuid=*/null, requestId, offset, Status.NO_REQUEST_LISTENER_SET), null);
        }
        else
        {
            final IncomingEvent requestEvent = P_Bridge_User.newIncomingEvent(
                    m_server.getBleServer(), device, serviceUuid, charUuid, descUuid_nullable, Type.READ, target, P_Const.EMPTY_BYTE_ARRAY, requestId, offset, /*responseNeeded=*/true
            );

            final IncomingListener.Please please = listener.onEvent(requestEvent);

            if (please == null)
            {
                m_server.invokeOutgoingListeners(newEarlyOutResponse_Read(device, serviceUuid, charUuid, descUuid_nullable, requestId, offset, Status.NO_RESPONSE_ATTEMPTED), null);
            }
            else
            {
                final boolean attemptResponse = P_Bridge_User.respond(please);

                if (attemptResponse)
                {
                    final P_Task_SendReadWriteResponse responseTask = new P_Task_SendReadWriteResponse(m_server, requestEvent, please);

                    m_queue.add(responseTask);
                }
                else
                {
                    m_server.invokeOutgoingListeners(newEarlyOutResponse_Read(device, serviceUuid, charUuid, descUuid_nullable, requestId, offset, Status.NO_RESPONSE_ATTEMPTED), P_Bridge_User.getOutgoingListener(please));
                }
            }
        }
    }

    @Override
    public final void onCharacteristicReadRequest(final P_DeviceHolder device, final int requestId, final int offset, final BleCharacteristic characteristic)
    {
        m_server.getIManager().getPostManager().runOrPostToUpdateThread(() -> onReadRequest_updateThread(device, requestId, offset, characteristic.getService().getUuid(), characteristic.getUuid(), /*descUuid=*/null));
    }

    @Override
    public final void onDescriptorReadRequest(final P_DeviceHolder device, final int requestId, final int offset, final BleDescriptor descriptor)
    {
        m_server.getIManager().getPostManager().runOrPostToUpdateThread(() -> onReadRequest_updateThread(device, requestId, offset, descriptor.getCharacteristic().getService().getUuid(), descriptor.getCharacteristic().getUuid(), descriptor.getUuid()));
    }

    private OutgoingEvent newEarlyOutResponse_Write(final P_DeviceHolder device, final Type type, final UUID serviceUuid, final UUID charUuid, final UUID descUuid_nullable, final int requestId, final int offset, final Status status)
    {
        final Target target = descUuid_nullable == null ? Target.CHARACTERISTIC : Target.DESCRIPTOR;

        final OutgoingEvent e = P_Bridge_User.newOutgoingEvent(
                m_server.getBleServer(), device, serviceUuid, charUuid, descUuid_nullable, type, target, P_Const.EMPTY_BYTE_ARRAY, P_Const.EMPTY_BYTE_ARRAY,
                requestId, offset, /*responseNeeded=*/true, status, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BleStatuses.GATT_STATUS_NOT_APPLICABLE, /*solicited=*/true
        );

        return e;
    }

    private void onWriteRequest_updateThread(final P_DeviceHolder device, final byte[] data, final int requestId, final int offset, final boolean preparedWrite, final boolean responseNeeded, final UUID serviceUuid, final UUID charUuid, final UUID descUuid_nullable)
    {
        final Target target = descUuid_nullable == null ? Target.CHARACTERISTIC : Target.DESCRIPTOR;
        final Type type = preparedWrite ? Type.PREPARED_WRITE : Type.WRITE;

        final IncomingListener listener = m_server.getListener_Incoming() != null ? m_server.getListener_Incoming() : m_server.getIManager().getDefaultServerIncomingListener();

        if (listener == null)
        {
            m_server.invokeOutgoingListeners(newEarlyOutResponse_Write(device, type, serviceUuid, charUuid, /*descUuid=*/null, requestId, offset, Status.NO_REQUEST_LISTENER_SET), null);
        }
        else
        {
            final IncomingEvent requestEvent = P_Bridge_User.newIncomingEvent(
                    m_server.getBleServer(), device, serviceUuid, charUuid, descUuid_nullable, type, target, data, requestId, offset, responseNeeded
            );

            final IncomingListener.Please please = listener.onEvent(requestEvent);

            if (please == null)
            {
                m_server.invokeOutgoingListeners(newEarlyOutResponse_Write(device, type, serviceUuid, charUuid, descUuid_nullable, requestId, offset, Status.NO_RESPONSE_ATTEMPTED), null);
            }
            else
            {
                final boolean attemptResponse = P_Bridge_User.respond(please);

                if (attemptResponse)
                {
                    final P_Task_SendReadWriteResponse responseTask = new P_Task_SendReadWriteResponse(m_server, requestEvent, please);

                    m_queue.add(responseTask);
                }
                else
                {
                    m_server.invokeOutgoingListeners(newEarlyOutResponse_Write(device, type, serviceUuid, charUuid, descUuid_nullable, requestId, offset, Status.NO_RESPONSE_ATTEMPTED), P_Bridge_User.getOutgoingListener(please));
                }
            }
        }
    }

    @Override
    public final void onCharacteristicWriteRequest(final P_DeviceHolder device, final int requestId, final BleCharacteristic characteristic, final boolean preparedWrite, final boolean responseNeeded, final int offset, final byte[] value)
    {
        m_server.getIManager().getPostManager().runOrPostToUpdateThread(() -> onWriteRequest_updateThread(device, value, requestId, offset, preparedWrite, responseNeeded, characteristic.getService().getUuid(), characteristic.getUuid(), /*descUuid=*/null));
    }

    @Override
    public final void onDescriptorWriteRequest(final P_DeviceHolder device, final int requestId, final BleDescriptor descriptor, final boolean preparedWrite, final boolean responseNeeded, final int offset, final byte[] value)
    {
        m_server.getIManager().getPostManager().runOrPostToUpdateThread(() -> onWriteRequest_updateThread(device, value, requestId, offset, preparedWrite, responseNeeded, descriptor.getCharacteristic().getService().getUuid(), descriptor.getCharacteristic().getUuid(), descriptor.getUuid()));
    }

    @Override
    public final void onExecuteWrite(P_DeviceHolder device, int requestId, boolean execute)
    {
    }

    @Override
    public final void onNotificationSent(final P_DeviceHolder device, final int gattStatus)
    {
        m_server.getIManager().getPostManager().runOrPostToUpdateThread(() -> onNotificationSent_updateThread(device, gattStatus));
    }

    private void onNotificationSent_updateThread(final P_DeviceHolder device, final int gattStatus)
    {
        final P_Task_SendNotification task = m_queue.getCurrent(P_Task_SendNotification.class, m_server);

        if (task != null && task.m_macAddress.equals(device.getAddress()))
        {
            task.onNotificationSent(device, gattStatus);
        }
        else
        {
            final OutgoingEvent e = P_Bridge_User.newOutgoingEvent(
                    m_server.getBleServer(), device, Uuids.INVALID, Uuids.INVALID, ExchangeListener.ExchangeEvent.NON_APPLICABLE_UUID, Type.NOTIFICATION,
                    ExchangeListener.Target.CHARACTERISTIC, P_Const.EMPTY_BYTE_ARRAY, P_Const.EMPTY_BYTE_ARRAY, ExchangeListener.ExchangeEvent.NON_APPLICABLE_REQUEST_ID,
                    /*offset=*/0, /*responseNeeded=*/false, OutgoingListener.Status.SUCCESS, BleStatuses.GATT_STATUS_NOT_APPLICABLE, gattStatus, /*solicited=*/false
            );

            m_server.invokeOutgoingListeners(e, null);
        }
    }

    private final class TaskStateListener implements PA_Task.I_StateListener
    {

        @Override
        public void onStateChange(PA_Task task, PE_TaskState state)
        {
            if (task.getClass() == P_Task_DisconnectServer.class)
            {
                if (state.isEndingState())
                {
                    if (state == PE_TaskState.SUCCEEDED)
                    {
                        final P_Task_DisconnectServer task_cast = (P_Task_DisconnectServer) task;

                        m_server.onNativeDisconnect(task_cast.m_nativeDevice.getAddress(), task_cast.isExplicit(), task_cast.getGattStatus());
                    }
                }
            }
            else if (task.getClass() == P_Task_ConnectServer.class)
            {
                if (state.isEndingState())
                {
                    final P_Task_ConnectServer task_cast = (P_Task_ConnectServer) task;

                    if (state == PE_TaskState.SUCCEEDED)
                    {
                        m_server.onNativeConnect(task_cast.m_nativeDevice.getAddress(), task_cast.isExplicit());
                    }
                    else if (state == PE_TaskState.REDUNDANT)
                    {
                        // nothing to do, but maybe should assert?
                    }
                    else if (state == PE_TaskState.FAILED_IMMEDIATELY)
                    {
                        final ServerReconnectFilter.Status status = task_cast.getStatus();

                        if (status == ServerReconnectFilter.Status.SERVER_OPENING_FAILED || status == ServerReconnectFilter.Status.NATIVE_CONNECTION_FAILED_IMMEDIATELY)
                        {
                            m_server.onNativeConnectFail(task_cast.m_nativeDevice, status, task_cast.getGattStatus());
                        }
                        else
                        {
                            m_server.getIManager().ASSERT(false, "Didn't expect server failed-immediately status to be something else.");

                            m_server.onNativeConnectFail(task_cast.m_nativeDevice, ServerReconnectFilter.Status.NATIVE_CONNECTION_FAILED_IMMEDIATELY, task_cast.getGattStatus());
                        }
                    }
                    else if (state == PE_TaskState.FAILED)
                    {
                        m_server.onNativeConnectFail(task_cast.m_nativeDevice, ServerReconnectFilter.Status.NATIVE_CONNECTION_FAILED_EVENTUALLY, task_cast.getGattStatus());
                    }
                    else if (state == PE_TaskState.TIMED_OUT)
                    {
                        m_server.onNativeConnectFail(task_cast.m_nativeDevice, ServerReconnectFilter.Status.TIMED_OUT, task_cast.getGattStatus());
                    }
                    else if (state == PE_TaskState.SOFTLY_CANCELLED)
                    {
                        // do nothing...this was handled upstream back in time
                    }
                    else
                    {
                        m_server.getIManager().ASSERT(false, "Did not expect ending state " + state + " for connect task failure.");

                        m_server.onNativeConnectFail(task_cast.m_nativeDevice, ServerReconnectFilter.Status.NATIVE_CONNECTION_FAILED_EVENTUALLY, task_cast.getGattStatus());
                    }
                }
            }
        }
    }


    private interface HasTaskListener<T extends PA_Task>
    {
        void gotTask(T task, boolean isTheTask);
    }
}
