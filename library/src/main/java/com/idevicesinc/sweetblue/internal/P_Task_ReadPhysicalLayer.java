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


import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleTask;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.ReadWriteListener;
import com.idevicesinc.sweetblue.utils.Phy;
import com.idevicesinc.sweetblue.utils.Utils;


final class P_Task_ReadPhysicalLayer extends PA_Task_RequiresConnection
{

    private final ReadWriteListener m_readWriteListener;


    public P_Task_ReadPhysicalLayer(IBleDevice device, I_StateListener listener, ReadWriteListener readWriteListener)
    {
        super(device, listener);

        m_readWriteListener = readWriteListener;
    }

    @Override
    protected BleTask getTaskType()
    {
        return BleTask.READ_PHYSICAL_LAYER;
    }

    @Override
    void execute()
    {
        if (Utils.isOreo())
            if (getDevice().nativeManager().getGattLayer().readPhy())
            {
                // success so far...
            }
            else
                fail();
        else
            fail();
    }

    protected void succeed(int gattStatus, int txPhy, int rxPhy)
    {
        super.succeed();

        Phy options = Phy.fromMasks(txPhy, rxPhy);

        getDevice().setPhy_private(options);

        final ReadWriteListener.ReadWriteEvent event = newEvent(ReadWriteListener.Status.SUCCESS, gattStatus, options);

        getDevice().invokeReadWriteCallback(m_readWriteListener, event);
    }

    void fail(ReadWriteListener.Status status, int gattStatus)
    {
        this.fail();

        getDevice().getStateTracker().update(PA_StateTracker.E_Intent.INTENTIONAL, gattStatus, BleDeviceState.REQUESTING_PHY, false);

        getDevice().invokeReadWriteCallback(m_readWriteListener, newEvent(status, gattStatus, null));
    }

    private ReadWriteListener.ReadWriteEvent newEvent(ReadWriteListener.Status status, int gattStatus, Phy options)
    {
        return P_Bridge_User.newReadWriteEventPhy(getDevice().getBleDevice(), status, gattStatus, options, getTotalTime(), getTotalTimeExecuting(), /*solicited=*/true);
    }

    @Override
    public PE_TaskPriority getPriority()
    {
        return PE_TaskPriority.MEDIUM;
    }
}
