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

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

import com.idevicesinc.sweetblue.BleCharacteristic;
import com.idevicesinc.sweetblue.BleDescriptor;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleNotify;
import com.idevicesinc.sweetblue.BleOp;
import com.idevicesinc.sweetblue.BleStatuses;
import com.idevicesinc.sweetblue.BleTask;
import com.idevicesinc.sweetblue.NotificationListener;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.ReadWriteListener;
import com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent;
import com.idevicesinc.sweetblue.ReadWriteListener.Status;
import com.idevicesinc.sweetblue.ReadWriteListener.Target;
import com.idevicesinc.sweetblue.internal.android.P_GattHolder;
import com.idevicesinc.sweetblue.utils.P_Const;
import com.idevicesinc.sweetblue.utils.PresentData;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.UhOhListener.UhOh;
import com.idevicesinc.sweetblue.utils.Uuids;


final class P_Task_ToggleNotify extends PA_Task_ReadOrWrite implements PA_Task.I_StateListener
{
    private static int Type_NOTIFY = 0;
    private static int Type_INDICATE = 1;

    private final boolean m_enable;

    private byte[] m_writeValue = null;


    public P_Task_ToggleNotify(IBleDevice device, BleNotify notify, boolean enable, IBleTransaction txn, PE_TaskPriority priority)
    {
        super(device, notify, false, txn, priority);

        if (!Uuids.isValid(notify.getDescriptorUuid()))
            notify.setDescriptorUUID(Uuids.CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR_UUID);

        m_enable = enable;
    }

    private byte[] getWriteValue()
    {
        return m_writeValue != null ? m_writeValue : P_Const.EMPTY_BYTE_ARRAY;
    }

    static byte[] getWriteValue(BleCharacteristic char_native, boolean enable)
    {
        final int type;

        if ((P_Bridge_User.getProperties(char_native) & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0x0)
        {
            type = Type_NOTIFY;
        }
        else if ((P_Bridge_User.getProperties(char_native) & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0x0)
        {
            type = Type_INDICATE;
        }
        else
        {
            type = Type_NOTIFY;
        }

        final byte[] enableValue = type == Type_NOTIFY ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.ENABLE_INDICATION_VALUE;
        final byte[] disableValue = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;

        return enable ? enableValue : disableValue;
    }

    @Override
    protected void executeReadOrWrite()
    {
        final BleCharacteristic char_native = getFilteredCharacteristic() != null ? getFilteredCharacteristic() : getDevice().getNativeBleCharacteristic(getServiceUuid(), getCharUuid(), m_bleOp.getDescriptorFilter());

        if (char_native == null || char_native.isNull())
        {
            this.fail(Status.NO_MATCHING_TARGET, BleStatuses.GATT_STATUS_NOT_APPLICABLE, Target.CHARACTERISTIC, getCharUuid(), ReadWriteEvent.NON_APPLICABLE_UUID);
        }
        else if (false == getDevice().nativeManager().getGattLayer().setCharacteristicNotification(char_native, m_enable))
        {
            this.fail(Status.FAILED_TO_TOGGLE_NOTIFICATION, BleStatuses.GATT_STATUS_NOT_APPLICABLE, Target.CHARACTERISTIC, getCharUuid(), ReadWriteEvent.NON_APPLICABLE_UUID);
        }
        else
        {
            final BleDescriptor descriptor = char_native.getDescriptor(m_bleOp.getDescriptorUuid());

            if (descriptor == null || descriptor.isNull())
            {
                //--- DRK > Previously we were failing the task if the descriptor came up null. It was assumed that writing the descriptor
                //---		was a requirement. It turns out that, at least sometimes, simply calling setCharacteristicNotification(true) is enough.
                succeed();

                // this.fail(Status.NO_MATCHING_TARGET, BleStatuses.GATT_STATUS_NOT_APPLICABLE, Target.DESCRIPTOR, m_uuid, m_descUuid);
            }
            else
            {
                if (!m_bleOp.isServiceUuidValid())
                    m_bleOp.setServiceUUID(char_native.getService().getUuid());

                m_writeValue = getWriteValue(char_native, m_enable);

                if (false == getDevice().nativeManager().getGattLayer().setDescValue(descriptor, getWriteValue()))
                {
                    this.fail(Status.FAILED_TO_SET_VALUE_ON_TARGET, BleStatuses.GATT_STATUS_NOT_APPLICABLE, Target.DESCRIPTOR, getCharUuid(), m_bleOp.getDescriptorUuid());
                }
                else if (false == getDevice().nativeManager().getGattLayer().writeDescriptor(descriptor))
                {
                    this.fail(Status.FAILED_TO_SEND_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, Target.DESCRIPTOR, getCharUuid(), m_bleOp.getDescriptorUuid());
                }
                else
                {
                    // SUCCESS, so far...
                }
            }
        }
    }

    @Override
    protected void fail(Status status, int gattStatus, Target target, UUID charUuid, UUID descUuid)
    {
        super.fail(status, gattStatus, target, charUuid, descUuid);

        if (m_enable)
        {
            getDevice().getPollManager().onNotifyStateChange(getServiceUuid(), charUuid, P_PollManager.E_NotifyState__NOT_ENABLED);
        }

        final NotificationListener.NotificationEvent nEvent = newNotifyEvent(NotificationListener.Status.fromReadWriteStatus(status), gattStatus, m_bleOp);
        getDevice().invokeNotificationCallback(getNotifyListener(), nEvent);
    }

    @Override
    protected void succeed()
    {
//		getDevice().addWriteTime(result.totalTime);

        if (m_enable)
        {
            getDevice().getPollManager().onNotifyStateChange(getServiceUuid(), getCharUuid(), P_PollManager.E_NotifyState__ENABLED);
        }
        else
        {
            getDevice().getPollManager().onNotifyStateChange(getServiceUuid(), getCharUuid(), P_PollManager.E_NotifyState__NOT_ENABLED);
        }

        super.succeed();

        final NotificationListener.NotificationEvent nEvent = newNotifyEvent(NotificationListener.Status.SUCCESS, BleStatuses.GATT_SUCCESS, m_bleOp);
        getDevice().invokeNotificationCallback(getNotifyListener(), nEvent);
    }

    private NotificationListener getNotifyListener()
    {
        return ((BleNotify) m_bleOp).getNotificationListener();
    }

    public void onDescriptorWrite(P_GattHolder gatt, UUID descUuid, int status)
    {
        getManager().ASSERT(getDevice().nativeManager().gattEquals(gatt), "");

        if (!descUuid.equals(m_bleOp.getDescriptorUuid())) return;

        final boolean isConnected = getDevice().is_internal(BleDeviceState.BLE_CONNECTED);

        if (isConnected && Utils.isSuccess(status))
        {
            succeed();
        }
        else
        {
            if (!isConnected && Utils.isSuccess(status))
            {
                //--- DRK > Trying to catch a case that I currently can't explain any other way.
                //--- DRK > UPDATE: Nevermind, must have been tired when I wrote this assert, device can be
                //---			explicitly disconnected while the notify enable write is out and this can get tripped.
//				getIManager().ASSERT(false, "Successfully enabled notification but device isn't connected.");

                fail(Status.CANCELLED_FROM_DISCONNECT, status, Target.DESCRIPTOR, getCharUuid(), descUuid);
            }
            else
            {
                fail(Status.REMOTE_GATT_FAILURE, status, Target.DESCRIPTOR, getCharUuid(), descUuid);
            }
        }
    }

    @Override
    public void onStateChange(PA_Task task, PE_TaskState state)
    {
        super.onStateChange(task, state);

        if (state == PE_TaskState.TIMED_OUT)
        {
            getLogger().w(getLogger().charName(getCharUuid()) + " descriptor write timed out!");

            final ReadWriteEvent event = newReadWriteEvent(Status.TIMED_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, Target.DESCRIPTOR, m_bleOp);

            getDevice().invokeReadWriteCallback(m_bleOp.getReadWriteListener(), event);

            getManager().uhOh(UhOh.WRITE_TIMED_OUT);
        }
        else if (state == PE_TaskState.SOFTLY_CANCELLED)
        {
            final Target target = this.getState() == PE_TaskState.EXECUTING ? Target.DESCRIPTOR : Target.CHARACTERISTIC;
            final UUID descUuid = target == Target.DESCRIPTOR ? m_bleOp.getDescriptorUuid() : ReadWriteEvent.NON_APPLICABLE_UUID;
            final ReadWriteEvent event = newReadWriteEvent(getCancelType(), BleStatuses.GATT_STATUS_NOT_APPLICABLE, target, m_bleOp);

            getDevice().invokeReadWriteCallback(m_bleOp.getReadWriteListener(), event);
        }
    }

    @Override
    protected UUID getDescUuid()
    {
        return m_bleOp.getDescriptorUuid();
    }

    @Override
    public boolean isMoreImportantThan(PA_Task task)
    {
        return isMoreImportantThan_default(task);
    }

    private NotificationListener.Type getNotifyType()
    {
        return m_enable ? NotificationListener.Type.ENABLING_NOTIFICATION : NotificationListener.Type.DISABLING_NOTIFICATION;
    }

    @Override
    protected ReadWriteEvent newReadWriteEvent(Status status, int gattStatus, Target target, BleOp bleOp)
    {
        BleNotify notify = new BleNotify(bleOp.getServiceUuid(), bleOp.getCharacteristicUuid()).setDescriptorUUID(bleOp.getDescriptorUuid()).setDescriptorFilter(bleOp.getDescriptorFilter());
        notify.setData(new PresentData(getWriteValue()));
        return P_Bridge_User.newReadWriteEvent(getDevice().getBleDevice(), notify, ReadWriteListener.Type.READ, target, status, gattStatus, getTotalTime(), getTotalTimeExecuting(), /*solicited=*/true);
    }

    private NotificationListener.NotificationEvent newNotifyEvent(NotificationListener.Status status, int gattStatus, BleOp bleOp)
    {
        BleNotify notify = new BleNotify(bleOp.getServiceUuid(), bleOp.getCharacteristicUuid()).setDescriptorUUID(bleOp.getDescriptorUuid()).setDescriptorFilter(bleOp.getDescriptorFilter())
                .setData(bleOp.getData());
        return P_Bridge_User.newNotificationEvent(getDevice().getBleDevice(), notify, getNotifyType(), status, gattStatus, getTotalTime(), getTotalTimeExecuting(), true);

    }

    @Override
    protected BleTask getTaskType()
    {
        return BleTask.TOGGLE_NOTIFY;
    }
}
