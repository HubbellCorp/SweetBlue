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


import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleTransaction;
import com.idevicesinc.sweetblue.BleWrite;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.ReadWriteListener;
import com.idevicesinc.sweetblue.utils.FutureData;
import com.idevicesinc.sweetblue.utils.PresentData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


final class P_StripedWriteTransaction extends BleTransaction
{

    private final boolean m_requiresBonding;
    private final ReadWriteListener.Type m_writeType;
    private final List<P_Task_Write> m_writeList;
    private final WriteListener m_internalListener;
    private BleWrite m_write;



    P_StripedWriteTransaction(BleWrite write, boolean requiresBonding, ReadWriteListener.Type writeType)
    {
        m_write = write;
        m_requiresBonding = requiresBonding;
        m_writeType = writeType;
        m_writeList = new ArrayList<>();
        m_internalListener = new WriteListener();
    }


    @Override protected final void start()
    {
        final byte[] allData = m_write.getData().getData();
        int curIndex = 0;
        FutureData curData;
        final IBleDevice idevice = P_Bridge_User.getIBleDevice(getDevice());
        while (curIndex < allData.length)
        {
            int end = Math.min(allData.length, curIndex + getDevice().getEffectiveWriteMtuSize());
            curData = new PresentData(Arrays.copyOfRange(allData, curIndex, end));
            final BleWrite write = P_Bridge_User.createDuplicate(m_write)
                    .setReadWriteListener(m_internalListener)
                    .setData(curData);
            final P_Task_Write task = new P_Task_Write(idevice, write, m_requiresBonding, P_Bridge_User.getIBleTransaction(this), idevice.getOverrideReadWritePriority());
            m_writeList.add(task);

            curIndex = end;
        }
        idevice.getIManager().getTaskManager().add(m_writeList.remove(0));
    }

    private final class WriteListener implements ReadWriteListener
    {

        @Override public final void onEvent(ReadWriteListener.ReadWriteEvent e)
        {
            if (e.wasSuccess())
            {
                if (m_writeList.size() > 0)
                {
                    P_Bridge_User.getIBleDevice(getDevice()).getIManager().getTaskManager().add(m_writeList.remove(0));
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
