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

import java.util.List;
import java.util.UUID;

import com.idevicesinc.sweetblue.BleCharacteristic;
import com.idevicesinc.sweetblue.BleDescriptor;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleOp;
import com.idevicesinc.sweetblue.BleService;
import com.idevicesinc.sweetblue.BleStatuses;
import com.idevicesinc.sweetblue.NotificationListener;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.ReadWriteListener;
import com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent;
import com.idevicesinc.sweetblue.ReadWriteListener.Status;
import com.idevicesinc.sweetblue.ReadWriteListener.Target;
import com.idevicesinc.sweetblue.ReadWriteListener.Type;
import com.idevicesinc.sweetblue.UhOhListener;
import com.idevicesinc.sweetblue.utils.P_Const;


final class P_DeviceServiceManager extends PA_ServiceManager
{

    private final IBleDevice m_device;


    public P_DeviceServiceManager(IBleDevice device)
    {
        m_device = device;
    }


    private ReadWriteEvent newNoMatchingTargetEvent(Type type, Target target, BleOp bleOp)
    {
        final int gattStatus = BleStatuses.GATT_STATUS_NOT_APPLICABLE;

        return P_Bridge_User.newReadWriteEvent(m_device.getBleDevice(), bleOp, type, target, Status.NO_MATCHING_TARGET, gattStatus, 0.0, 0.0, /*solicited=*/true);
    }

    private ReadWriteEvent newExceptionEvent(Type type, Target target, BleOp bleOp, UhOhListener.UhOh uhoh)
    {
        final int gattStatus = BleStatuses.GATT_STATUS_NOT_APPLICABLE;

        final Status status;

        if (uhoh == UhOhListener.UhOh.CONCURRENT_EXCEPTION)
        {
            status = Status.GATT_CONCURRENT_EXCEPTION;
        }
        else
        {
            status = Status.GATT_RANDOM_EXCEPTION;
        }

        return P_Bridge_User.newReadWriteEvent(m_device.getBleDevice(), bleOp, type, target, status, gattStatus, 0.0, 0.0, /*solicited=*/true);
    }

    public final ReadWriteEvent getEarlyOutEvent(BleOp bleOp, Type type, final Target target)
    {
        final int gattStatus = BleStatuses.GATT_STATUS_NOT_APPLICABLE;

        if (m_device.isNull())
        {
            return P_Bridge_User.newReadWriteEvent(m_device.getBleDevice(), bleOp, type, target, Status.NULL_DEVICE, gattStatus, 0.0, 0.0, /*solicited=*/true);
        }

        if (!m_device.is(BleDeviceState.BLE_CONNECTED))
        {
            if (type != ReadWriteListener.Type.ENABLING_NOTIFICATION && type != ReadWriteListener.Type.DISABLING_NOTIFICATION)
                return P_Bridge_User.newReadWriteEvent(m_device.getBleDevice(), bleOp, type, target, Status.NOT_CONNECTED, gattStatus, 0.0, 0.0, /*solicited=*/true);
            else
                return null;
        }
//
//		P_TaskManager deviceTask = m_device.getIManager().getTaskManager();
//
//		// check if a disconnect task exists as current or in queue
//		if( deviceTask.isCurrentOrInQueue(P_Task_Disconnect.class, m_device) )
//		{
//			// check to see if the disconnect task is explicit ( called by the user )
//			if( deviceTask.get(P_Task_Disconnect.class, m_device.getIManager()).isExplicit() )
//			{
//				return P_Bridge_User.newReadWriteEvent(m_device.getBleDevice(), bleOp, type, target, Status.DISCONNECTING, gattStatus, 0.0, 0.0, /*solicited=*/true);
//			}
//		}
//
        if (target == Target.RSSI || target == Target.MTU || target == Target.CONNECTION_PRIORITY || target == Target.PHYSICAL_LAYER)
            return null;
//
		final BleCharacteristic characteristic = m_device.getNativeBleCharacteristic(bleOp.getServiceUuid(), bleOp.getCharacteristicUuid());
        final BleDescriptor descriptor = m_device.getNativeBleDescriptor(bleOp.getServiceUuid(), bleOp.getCharacteristicUuid(), bleOp.getDescriptorUuid());

        if (target == Target.CHARACTERISTIC && characteristic.isNull() || target == Target.DESCRIPTOR && descriptor.isNull())
        {
            if (characteristic.hasUhOh() || descriptor.hasUhOh())
            {
                UhOhListener.UhOh uhoh;
                if (characteristic.hasUhOh())
                    uhoh = characteristic.getUhOh();
                else
                    uhoh = descriptor.getUhOh();
                return newExceptionEvent(type, target, bleOp, uhoh);
            }
            else
                return newNoMatchingTargetEvent(type, target, bleOp);
        }

        if (target == Target.CHARACTERISTIC)
            type = modifyResultType(characteristic, type);

        if (type != null && type.isWrite())
        {
            if (bleOp.getData() == null || bleOp.getData().getData().length < 1)
                return P_Bridge_User.newReadWriteEvent(m_device.getBleDevice(), bleOp, type, target, Status.NULL_DATA, gattStatus, 0.0, 0.0, /*solicited=*/true);
        }

        if (target == Target.CHARACTERISTIC)
        {
            int property = getProperty(type);

            if ((characteristic.getCharacteristic().getProperties() & property) == 0x0)
            {
                //TODO: Use correct gatt status even though we never reach gatt layer?
                ReadWriteEvent result = P_Bridge_User.newReadWriteEvent(m_device.getBleDevice(), bleOp, type, target, Status.OPERATION_NOT_SUPPORTED, gattStatus, 0.0, 0.0, /*solicited=*/true);

                return result;
            }
        }

        IBleTransaction trans = m_device.getThreadLocalTransaction();
        if (trans != null && (!trans.isRunning() || m_device.getTxnManager().getCurrent() != trans))
        {
            // Oops, transaction is not null but is either not started or not the current
            ReadWriteEvent result = P_Bridge_User.newReadWriteEvent(m_device.getBleDevice(), bleOp, type, target, Status.INVALID_TRANSACTION, gattStatus, 0.0, 0.0, /*solicited=*/true);
            return result;
        }

        return null;
    }


    static NotificationListener.Type getProperNotificationType(BleCharacteristic char_native, NotificationListener.Type type)
    {
        if (char_native != null && !char_native.isNull())
        {
            if (type == NotificationListener.Type.NOTIFICATION)
            {
                if ((char_native.getCharacteristic().getProperties() & BleCharacteristic.PROPERTY_NOTIFY) == 0x0)
                {
                    if ((char_native.getCharacteristic().getProperties() & BleCharacteristic.PROPERTY_INDICATE) != 0x0)
                    {
                        type = NotificationListener.Type.INDICATION;
                    }
                }
            }
        }
        return type;
    }

    static ReadWriteListener.Type modifyResultType(BleCharacteristic char_native, ReadWriteListener.Type type)
    {
        if (!char_native.isNull())
        {
            if (type == Type.WRITE)
            {
                if ((char_native.getCharacteristic().getProperties() & BleCharacteristic.PROPERTY_WRITE) == 0x0)
                {
                    if ((char_native.getCharacteristic().getProperties() & BleCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0x0)
                    {
                        type = Type.WRITE_NO_RESPONSE;
                    }
                    else if ((char_native.getCharacteristic().getProperties() & BleCharacteristic.PROPERTY_SIGNED_WRITE) != 0x0)
                    {
                        type = Type.WRITE_SIGNED;
                    }
                }
                //--- RB > Check the write type on the characteristic, in case this char has multiple write types
                int writeType = char_native.getCharacteristic().getWriteType();
                if (writeType == BleCharacteristic.WRITE_TYPE_NO_RESPONSE)
                {
                    type = Type.WRITE_NO_RESPONSE;
                }
                else if (writeType == BleCharacteristic.WRITE_TYPE_SIGNED)
                {
                    type = Type.WRITE_SIGNED;
                }
            }
        }

        return type;
    }

    private static int getProperty(ReadWriteListener.Type type)
    {
        switch (type)
        {
            case READ:
            case POLL:
            case PSUEDO_NOTIFICATION:
                return BleCharacteristic.PROPERTY_READ;

            case WRITE_NO_RESPONSE:
                return BleCharacteristic.PROPERTY_WRITE_NO_RESPONSE;
            case WRITE_SIGNED:
                return BleCharacteristic.PROPERTY_SIGNED_WRITE;
            case WRITE:
                return BleCharacteristic.PROPERTY_WRITE;

            case ENABLING_NOTIFICATION:
            case DISABLING_NOTIFICATION:
                return BleCharacteristic.PROPERTY_INDICATE |
                        BleCharacteristic.PROPERTY_NOTIFY;
        }

        return 0x0;
    }

    @Override
    public final BleService getServiceDirectlyFromNativeNode(UUID serviceUuid)
    {
        return m_device.nativeManager().getService(serviceUuid);
    }

    @Override
    protected final List<BleService> getNativeServiceList_original()
    {
        List<BleService> list_native = m_device.nativeManager().getNativeServiceList();
        return list_native == null ? P_Const.EMPTY_BLESERVICE_LIST : list_native;
    }
}

