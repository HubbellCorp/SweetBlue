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


import com.idevicesinc.sweetblue.BleCharacteristic;
import com.idevicesinc.sweetblue.BleOp;
import com.idevicesinc.sweetblue.BleStatuses;
import com.idevicesinc.sweetblue.BleTask;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.ReadWriteListener;
import com.idevicesinc.sweetblue.UhOhListener;
import com.idevicesinc.sweetblue.internal.android.P_GattHolder;
import com.idevicesinc.sweetblue.utils.FutureData;
import com.idevicesinc.sweetblue.utils.Utils;
import java.util.UUID;


class P_Task_TestMtu extends PA_Task_ReadOrWrite
{

    private final byte[] m_data;
    private final ReadWriteListener.Type m_writeType;


    P_Task_TestMtu(IBleDevice device, BleOp bleOp, final FutureData futureData, boolean requiresBonding, ReadWriteListener.Type writeType, IBleTransaction txn, PE_TaskPriority priority)
    {
        super(device, bleOp, requiresBonding, txn, priority);

        m_data = futureData.getData();

        m_writeType = writeType;
    }

    @Override
    protected ReadWriteListener.ReadWriteEvent newReadWriteEvent(ReadWriteListener.Status status, int gattStatus, ReadWriteListener.Target target, BleOp bleOp)
    {
        final BleCharacteristic char_native = getDevice().getNativeBleCharacteristic(bleOp.getServiceUuid(), bleOp.getCharacteristicUuid());
        final ReadWriteListener.Type type = P_DeviceServiceManager.modifyResultType(char_native, ReadWriteListener.Type.WRITE);
        BleOp op = P_Bridge_User.createNewOp(bleOp);
        return P_Bridge_User.newReadWriteEvent(getDevice().getBleDevice(), op, type, target, status, gattStatus, getTotalTime(), getTotalTimeExecuting(), true);
    }

    @Override protected void executeReadOrWrite()
    {
        if( false == write_earlyOut(m_data) )
        {
            final BleCharacteristic char_native = getFilteredCharacteristic() != null ? getFilteredCharacteristic() : getDevice().getNativeBleCharacteristic(getServiceUuid(), getCharUuid(), m_bleOp.getDescriptorFilter());

            if( char_native == null )
            {
                fail(ReadWriteListener.Status.NO_MATCHING_TARGET, BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), getCharUuid(), ReadWriteListener.ReadWriteEvent.NON_APPLICABLE_UUID);
            }
            else
            {
                // Set the write type now, if it is not null
                P_Bridge_User.setCharWriteType(char_native, m_writeType);

                if( false == getDevice().nativeManager().setCharValue(char_native, m_data) )
                {
                    fail(ReadWriteListener.Status.FAILED_TO_SET_VALUE_ON_TARGET, BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), getCharUuid(), ReadWriteListener.ReadWriteEvent.NON_APPLICABLE_UUID);
                }
                else
                {
                    if( false == getDevice().nativeManager().writeCharacteristic(char_native) )
                    {
                        fail(ReadWriteListener.Status.FAILED_TO_SEND_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), getCharUuid(), ReadWriteListener.ReadWriteEvent.NON_APPLICABLE_UUID);
                    }
                    else
                    {
                        // SUCCESS, for now... 
                    }
                }
            }
        }
    }

    public void onCharacteristicWrite(final P_GattHolder gatt, final UUID uuid, final int gattStatus)
    {
        getManager().ASSERT(getDevice().nativeManager().gattEquals(gatt), "");

        if( false == this.isForCharacteristic(uuid) )  return;

        if( false == acknowledgeCallback(gattStatus) )  return;

        if( Utils.isSuccess(gattStatus) )
        {
            succeedWrite();
        }
        else
        {
            fail(ReadWriteListener.Status.REMOTE_GATT_FAILURE, gattStatus, getDefaultTarget(), uuid, ReadWriteListener.ReadWriteEvent.NON_APPLICABLE_UUID);
        }
    }

    @Override public void onStateChange(final PA_Task task, final PE_TaskState state)
    {
        super.onStateChange(task, state);

        if( state == PE_TaskState.TIMED_OUT )
        {
            getLogger().w(getLogger().charName(getCharUuid()) + " write timed out!");

            getDevice().invokeReadWriteCallback(m_bleOp.getReadWriteListener(), newReadWriteEvent(ReadWriteListener.Status.TIMED_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), m_bleOp));

            getManager().uhOh(UhOhListener.UhOh.WRITE_MTU_TEST_TIMED_OUT);
        }
        else if( state == PE_TaskState.SOFTLY_CANCELLED )
        {
            getDevice().invokeReadWriteCallback(m_bleOp.getReadWriteListener(), newReadWriteEvent(getCancelType(), BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), m_bleOp));
        }
    }

    @Override protected BleTask getTaskType()
    {
        return BleTask.WRITE;
    }

    @Override
    protected ReadWriteListener.Target getDefaultTarget()
    {
        return ReadWriteListener.Target.CHARACTERISTIC_TEST_MTU;
    }
} 
