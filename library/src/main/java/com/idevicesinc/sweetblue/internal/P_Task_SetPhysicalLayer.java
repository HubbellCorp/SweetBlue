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
import com.idevicesinc.sweetblue.utils.Utils_String;


final class P_Task_SetPhysicalLayer extends PA_Task_Transactionable
{

    private final Phy m_phy;
    private final ReadWriteListener m_readWriteListener;


    public P_Task_SetPhysicalLayer(IBleDevice device, Phy options, IBleTransaction txn_nullable, PE_TaskPriority priority, ReadWriteListener readWriteListener)
    {
        super(device, txn_nullable, false, priority);
        m_phy = options;
        m_readWriteListener = readWriteListener;
    }


    @Override
    void execute()
    {
        switch (m_phy)
        {
            case HIGH_SPEED:
                if (!getManager().isBluetooth5HighSpeedSupported())
                {
                    getLogger().e("Tried to set bluetooth 5 high speed feature, but that feature is not available on this android device!");
                    fail();
                    return;
                }
                else
                    getLogger().d(Utils_String.concatStrings("Setting bluetooth 5 high speed option on device ", getDevice().getName_debug(), " (", getDevice().getMacAddress(), ")."));
                break;
            case LONG_RANGE_2X:
            case LONG_RANGE_4X:
                if (!getManager().isBluetooth5LongRangeSupported())
                {
                    getLogger().e("Tried to set bluetooth 5 long range feature, but that feature is not available on this android device!");
                    fail();
                    return;
                }
                else
                    getLogger().d(Utils_String.concatStrings("Setting bluetooth 5 long range option on device ", getDevice().getName_debug(), " (", getDevice().getMacAddress(), ")."));
                break;
        }
        if (getDevice().nativeManager().getGattLayer().setPhy(m_phy))
        {
            // success so far...waiting on callback
        }
        else
            fail();
    }


    protected void succeed(int gattStatus, int txPhy, int rxPhy)
    {

        getManager().ASSERT(m_phy.getTxMask() == txPhy && m_phy.getRxMask() == rxPhy, "Set Physical layer succeeded, but tx, and/or rx masks don't match" +
                " the requested parameters!");

        // TODO - Decide what to do here, if what's returned doesn't match what was requested.
//        dfgsdfg

        super.succeed();

        getDevice().setPhy_private(m_phy);

        final ReadWriteListener.ReadWriteEvent event = newEvent(ReadWriteListener.Status.SUCCESS, gattStatus, m_phy);

        getDevice().invokeReadWriteCallback(m_readWriteListener, event);
    }

    void fail(ReadWriteListener.Status status, int gattStatus)
    {
        this.fail();

        getDevice().getStateTracker().update(PA_StateTracker.E_Intent.INTENTIONAL, gattStatus, BleDeviceState.REQUESTING_PHY, false);

        getDevice().invokeReadWriteCallback(m_readWriteListener, newEvent(status, gattStatus, m_phy));
    }

    private ReadWriteListener.ReadWriteEvent newEvent(ReadWriteListener.Status status, int gattStatus, Phy options)
    {
        return P_Bridge_User.newReadWriteEventPhy(getDevice().getBleDevice(), status, gattStatus, options, getTotalTime(), getTotalTimeExecuting(), /*solicited=*/true);
    }

    @Override
    protected BleTask getTaskType()
    {
        return BleTask.SET_PHYSICAL_LAYER;
    }

    @Override
    public PE_TaskPriority getPriority()
    {
        return PE_TaskPriority.MEDIUM;
    }
}
