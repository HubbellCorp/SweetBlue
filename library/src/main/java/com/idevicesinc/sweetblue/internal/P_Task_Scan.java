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


import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.BleScanApi;
import com.idevicesinc.sweetblue.BleTask;
import com.idevicesinc.sweetblue.ScanOptions;
import com.idevicesinc.sweetblue.UhOhListener;
import com.idevicesinc.sweetblue.utils.Interval;


final class P_Task_Scan extends PA_Task_RequiresBleOn
{

    private final boolean m_explicit = true;
    private boolean m_stoppedBecauseOfBleTurnOff = false;
    private final PE_TaskPriority m_priority;
    private final ScanOptions m_scanOptions;


    public P_Task_Scan(IBleManager manager, I_StateListener listener, ScanOptions scanOptions, PE_TaskPriority priority)
    {
        super(manager, listener);

        m_priority = priority == null ? PE_TaskPriority.TRIVIAL : priority;

        m_scanOptions = scanOptions;
    }

    public PA_StateTracker.E_Intent getIntent()
    {
        return m_explicit ? PA_StateTracker.E_Intent.INTENTIONAL : PA_StateTracker.E_Intent.UNINTENTIONAL;
    }

    @Override protected double getInitialTimeout()
    {
        // Account for the classic scan boost here, we don't want to count the time doing the classic boost towards the timeout of the BLE scan
        if (isClassicBoosted())
        {
            return m_scanOptions.getScanTime().secs() + getManager().getConfigClone().scanClassicBoostLength.secs();
        }
        return m_scanOptions.getScanTime().secs();
    }

    @Override public void execute()
    {
        //--- DRK > Because scanning has to be done on a separate thread, isExecutable() can return true
        //---		but then by the time we get here it can be false. isExecutable() is currently not thread-safe
        //---		either, thus we're doing the manual check in the native stack. Before 5.0 the scan would just fail
        //---		so we'd fail as we do below, but Android 5.0 makes this an exception for at least some phones (OnePlus One (A0001)).
        if (false == isBluetoothEnabled())
        {
            fail();
        }
        else
        {

            if (isClassicBoosted())
            {
                if (!getManager().getScanManager().classicBoost(getManager().getConfigClone().scanClassicBoostLength.secs()))
                {
                    fail();

                    getManager().uhOh(UhOhListener.UhOh.CLASSIC_DISCOVERY_FAILED);
                }
            }
            else
            {
                if (!getManager().getScanManager().startScan(getIntent(), m_scanOptions))
                {
                    fail();

                    getManager().uhOh(UhOhListener.UhOh.START_BLE_SCAN_FAILED);
                }
                else
                {
                    // Success for now
                }
            }
        }
    }

    void stopForBleTurnOff()
    {
        m_stoppedBecauseOfBleTurnOff = true;
        succeed();
    }

    boolean wasStoppedForBleTurnOff()
    {
        return m_stoppedBecauseOfBleTurnOff;
    }

    boolean isClassicBoosted()
    {
        boolean isClassicScan = getManager().getConfigClone().scanApi == BleScanApi.CLASSIC;
        return !isClassicScan && Interval.isEnabled(getManager().getConfigClone().scanClassicBoostLength);
    }

    void onClassicBoostFinished()
    {
        if (!getManager().getScanManager().startScan(getIntent(), m_scanOptions))
        {
            fail();

            getManager().uhOh(UhOhListener.UhOh.START_BLE_SCAN_FAILED);
        }
    }

    private boolean isBluetoothEnabled()
    {
        return getManager().managerLayer().isBluetoothEnabled();
    }

    private double getMinimumScanTime()
    {
        return Interval.secs(getManager().getConfigClone().idealMinScanTime);
    }

    @Override protected void update(double timeStep)
    {
        if (this.getState() == PE_TaskState.EXECUTING)
        {
            if (getTotalTimeExecuting(getManager().currentTime()) >= getMinimumScanTime() && (getQueue().getSize() > 0 && isSelfInterruptableBy(getQueue().peek())))
            {
                selfInterrupt();
            }
            else if (getManager().getScanManager().isClassicScan() && getTotalTimeExecuting(getManager().currentTime()) >= BleManagerConfig.MAX_CLASSIC_SCAN_TIME)
            {
                selfInterrupt();
            }
        }
    }

    @Override public PE_TaskPriority getPriority()
    {
        return m_priority;
    }

    private boolean isSelfInterruptableBy(final PA_Task otherTask)
    {
//        if (otherTask.getPriority().ordinal() > PE_TaskPriority.TRIVIAL.ordinal())
        if (otherTask.getPriority().ordinal() > m_priority.ordinal())
        {
            return true;
        }
//        else if (otherTask.getPriority().ordinal() >= this.getPriority().ordinal())
        else if (otherTask.getPriority().ordinal() == m_priority.ordinal())
        {
            //--- DRK > Not sure infinite timeout check really matters here.
            return this.getTotalTimeExecuting() >= getMinimumScanTime();
//				return getTimeout() == TIMEOUT_INFINITE && this.getTotalTimeExecuting() >= getIManager().m_config.minimumScanTime;
        }
        else
        {
            return false;
        }
    }

    @Override public boolean isCancellableBy(PA_Task task)
    {
        boolean gettingCanceled = super.isCancellableBy(task);
        if (gettingCanceled && task instanceof P_Task_TurnBleOff)
            m_stoppedBecauseOfBleTurnOff = true;
        return gettingCanceled;
    }

    @Override public boolean isInterruptableBy(PA_Task otherTask)
    {
        return otherTask.getPriority().ordinal() > m_priority.ordinal();
//        return otherTask.getPriority().ordinal() >= PE_TaskPriority.FOR_EXPLICIT_BONDING_AND_CONNECTING.ordinal();
    }

    @Override public boolean isExplicit()
    {
        return m_explicit;
    }

    @Override protected BleTask getTaskType()
    {
        return null;
    }
}
