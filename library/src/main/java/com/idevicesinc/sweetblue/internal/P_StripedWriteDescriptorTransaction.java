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


import com.idevicesinc.sweetblue.BleDescriptorWrite;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleTransaction;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.ReadWriteListener;
import com.idevicesinc.sweetblue.utils.FutureData;
import com.idevicesinc.sweetblue.utils.PresentData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


final class P_StripedWriteDescriptorTransaction extends BleTransaction
{

    private final boolean m_requiresBonding;
    private final List<P_Task_WriteDescriptor> m_writeList;
    private final WriteListener m_internalListener;
    private final BleDescriptorWrite m_write;


    P_StripedWriteDescriptorTransaction(BleDescriptorWrite write, boolean requiresBonding)
    {
        m_write = write;
        m_requiresBonding = requiresBonding;
        m_writeList = new ArrayList<>();
        m_internalListener = new WriteListener();
    }


    @Override protected final void start()
    {
        final byte[] allData = m_write.getData().getData();
        int curIndex = 0;
        FutureData curData;
        final IBleDevice idevice = getIDevice();
        while (curIndex < allData.length)
        {
            int end = Math.min(allData.length, curIndex + getDevice().getEffectiveWriteMtuSize());
            curData = new PresentData(Arrays.copyOfRange(allData, curIndex, end));
            final BleDescriptorWrite write = P_Bridge_User.createDuplicate(m_write)
                    .setData(curData)
                    .setReadWriteListener(m_internalListener);
            m_writeList.add(new P_Task_WriteDescriptor(idevice, write, m_requiresBonding, P_Bridge_User.getIBleTransaction(this), idevice.getOverrideReadWritePriority()));
            curIndex = end;
        }
        idevice.getIManager().getTaskManager().add(m_writeList.remove(0));
    }

    private IBleDevice getIDevice()
    {
        return P_Bridge_User.getIBleDevice(getDevice());
    }

    private final class WriteListener implements ReadWriteListener
    {

        @Override public final void onEvent(ReadWriteListener.ReadWriteEvent e)
        {
            if (e.wasSuccess())
            {
                if (m_writeList.size() > 0)
                {
                    getIDevice().getIManager().getTaskManager().add(m_writeList.remove(0));
                }
                else
                {
                    succeed();
                    final ReadWriteListener listener = m_write.getReadWriteListener();
                    if (listener != null)
                        listener.onEvent(e);
                }
            }
            else
            {
                fail();
                final ReadWriteListener listener = m_write.getReadWriteListener();
                if (listener != null)
                    listener.onEvent(e);
            }
        }
    }
}
