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

package com.idevicesinc.sweetblue.toolbox.viewmodel;


import androidx.lifecycle.ViewModel;
import com.idevicesinc.sweetblue.BleCharacteristic;
import com.idevicesinc.sweetblue.BleDescriptor;
import com.idevicesinc.sweetblue.BleDescriptorRead;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleRead;
import com.idevicesinc.sweetblue.BleService;
import com.idevicesinc.sweetblue.BleTransaction;
import com.idevicesinc.sweetblue.NotificationListener;
import com.idevicesinc.sweetblue.ReadWriteListener;
import com.idevicesinc.sweetblue.toolbox.device.BleDeviceLogManager;
import com.idevicesinc.sweetblue.toolbox.util.MutablePostLiveData;
import com.idevicesinc.sweetblue.toolbox.util.UuidUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;


public class CharacteristicsViewModel extends ViewModel implements ReadWriteListener
{

    private BleDevice m_device;
    private BleService m_service;
    private List<BleCharacteristic> m_characteristicList = new ArrayList<>();

    private ReadTransaction mTransaction = null;

    private MutablePostLiveData<ReadWriteEvent> m_readWriteEvent = new MutablePostLiveData<>();
    private MutablePostLiveData<NotificationListener.NotificationEvent> m_notifyEvent = new MutablePostLiveData<>();


    public void init(BleDevice device, UUID charUuid)
    {
        if (m_device == null)
        {
            m_device = device;

            m_device.setListener_ReadWrite(this);

            m_device.setListener_Notification(e ->
            {
                if (e.type().isNativeNotification())
                {
                    m_notifyEvent.setValue(e);
                    BleDeviceLogManager.getInstance().addEntryForDevice(m_device, BleDeviceLogManager.EventType.Notify, e.charUuid(), e.data());
                }
            });

            m_service = m_device.getNativeBleService(charUuid);

            m_characteristicList = m_service.getCharacteristics();

            mTransaction = new ReadTransaction(null, m_characteristicList);
            m_device.performTransaction(mTransaction);
        }
        else if (mTransaction.getDevice() == null)
            m_device.performTransaction(mTransaction);

    }

    public MutablePostLiveData<ReadWriteEvent> getReadWriteEvent()
    {
        return m_readWriteEvent;
    }

    public MutablePostLiveData<NotificationListener.NotificationEvent> getNotifyEvent()
    {
        return m_notifyEvent;
    }

    public boolean isQueueEmpty()
    {
        if (mTransaction != null)
        {
            return mTransaction.getQueueSize() <= 0;
        }
        return true;
    }

    public boolean isServiceNull()
    {
        return m_service == null;
    }

    public String getServiceName()
    {
        return UuidUtil.getServiceName(m_device, m_service).name;
    }

    public BleDevice device()
    {
        return m_device;
    }

    public boolean hasCharacteristics()
    {
        return !m_characteristicList.isEmpty();
    }

    public BleService getService()
    {
        return m_service;
    }

    public List<BleCharacteristic> getCharacteristicList()
    {
        return m_characteristicList;
    }

    public void refresh()
    {
        mTransaction.refreshAll(m_characteristicList);
    }


    @Override
    public void onEvent(ReadWriteEvent e)
    {
        m_readWriteEvent.setValue(e);

        BleDeviceLogManager.getInstance().addEntryForDevice(m_device, BleDeviceLogManager.EventType.Read, e.charUuid(), e.data());
    }

    @Override
    protected void onCleared()
    {
        if (mTransaction != null)
            mTransaction.finishUp();
    }

    public final class ReadTransaction extends BleTransaction
    {
        private volatile List<Object> mPendingReadQueue = new ArrayList<>();
        private AtomicBoolean mTransactionLock = new AtomicBoolean(false);
        private ReadWriteListener mListener = null;

        ReadTransaction(ReadWriteListener listener, List<BleCharacteristic> characteristicList)
        {
            mListener = listener;

            buildQueue(characteristicList, false);
        }

        public void refreshAll(List<BleCharacteristic> list)
        {
            mPendingReadQueue.clear();  //FIXME:  synchronize
            buildQueue(list, true);
        }

        private void buildQueue(List<BleCharacteristic> list, boolean wipeValues)
        {
            // Populate initial queue
            for (BleCharacteristic bgc : list)
            {
                // TODO - Figure out if this is needed. Ideally, app's should never have to set values on
                // the characteristics/descriptors themselves.
//                if (wipeValues)
//                    bgc.setValue((byte[])null);
                mPendingReadQueue.add(bgc);
                List<BleDescriptor> descriptorList = bgc.getDescriptors();
                for (BleDescriptor bgd : descriptorList)
                {
                    // TODO - Figure out if this is needed. Ideally, app's should never have to set values on
                    // the characteristics/descriptors themselves.
//                    if (wipeValues)
//                        bgd.setValue(null);
                    mPendingReadQueue.add(bgd);
                }
            }
        }

        @Override
        protected void start()
        {
            // Do nothing, we handle everything in the update
        }

        @Override
        protected void update(double timeStep)
        {
            if (!mTransactionLock.compareAndSet(false, true))
                return;

            if (mPendingReadQueue.size() < 1)
            {
                mTransactionLock.set(false);
                return;
            }

            Object next = mPendingReadQueue.remove(0);

            // Send a transaction to read next
            if (next instanceof BleCharacteristic)
            {
                BleCharacteristic bgc = (BleCharacteristic) next;
                BleRead read = new BleRead(bgc.getUuid())
                        .setReadWriteListener(e ->
                        {
                            mTransactionLock.set(false);

                            if (mListener != null)
                                mListener.onEvent(e);
                        });
                read(read);
            }
            else if (next instanceof BleDescriptor)
            {
                BleDescriptor bgd = (BleDescriptor) next;
                BleDescriptorRead read = new BleDescriptorRead()
                        .setDescriptorUUID(bgd.getUuid())
                        .setReadWriteListener(e ->
                        {
                            mTransactionLock.set(false);

                            if (mListener != null)
                                mListener.onEvent(e);
                        });
                read(read);
            }
            else
            {
                // Hmm, a bad object was queued?  Unlock our boolean
                mTransactionLock.set(false);
            }
        }

        public int getQueueSize()
        {
            return mPendingReadQueue.size() + (mTransactionLock.get() ? 1 : 0);
        }

        public void finishUp()
        {
            if (getDevice() != null)
                cancel();
        }
    }

}
