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

import android.annotation.TargetApi;
import android.os.Build;
import com.idevicesinc.sweetblue.AdvertisingListener;
import com.idevicesinc.sweetblue.BleAdvertisingSettings;
import com.idevicesinc.sweetblue.BleTask;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.compat.L_Util;
import com.idevicesinc.sweetblue.utils.BleScanRecord;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.P_Bridge_Utils;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.BleAdvertisingSettings.BleAdvertisingMode;
import com.idevicesinc.sweetblue.BleAdvertisingSettings.BleTransmissionPower;


@TargetApi(Build.VERSION_CODES.LOLLIPOP)
final class P_Task_Advertise extends PA_Task_RequiresBleOn {


    private final BleScanRecord m_packet;
    private final AdvertisingListener m_listener;
    private final BleAdvertisingMode m_mode;
    private final BleTransmissionPower m_power;
    private final Interval m_timeOut;


    private final L_Util.AdvertisingCallback adCallback = new L_Util.AdvertisingCallback()
    {
        @Override
        public void onStartSuccess(BleAdvertisingSettings settings)
        {
            getServer().onAdvertiseStarted(m_packet, m_listener);
            succeed();
        }

        @Override
        public void onStartFailure(int errorCode)
        {
            AdvertisingListener.Status result = AdvertisingListener.Status.fromNativeStatus(errorCode);
            getServer().onAdvertiseStartFailed(result, m_listener);
            fail();
        }
    };


    public P_Task_Advertise(IBleServer server, BleScanRecord info, AdvertisingListener listener, BleAdvertisingMode mode, BleTransmissionPower power, Interval timeout)
    {
        super(server, null);
        m_packet = info;
        m_listener = listener;
        m_mode = mode;
        m_power = power;
        m_timeOut = timeout;
    }

    public P_Task_Advertise(IBleServer server, BleScanRecord info, BleAdvertisingSettings settings, AdvertisingListener listener)
    {
        super(server, null);
        m_packet = info;
        m_listener = listener;
        m_mode = settings.getAdvertisingMode();
        m_power = settings.getTransmissionPower();
        m_timeOut = settings.getTimeout();
    }


    /*package*/ final BleScanRecord getPacket()
    {
        return m_packet;
    }

    @Override
    protected final BleTask getTaskType()
    {
        return BleTask.START_ADVERTISING;
    }

    @Override
    final void execute()
    {
        if (Utils.isLollipop())
        {
            invokeStartAdvertising();
        }
        else
        {
            fail();
        }
    }

    /*package*/ final void stopAdvertising()
    {
        getManager().managerLayer().stopAdvertising();
    }

    @Override
    public final PE_TaskPriority getPriority()
    {
        return PE_TaskPriority.TRIVIAL;
    }

    private void invokeStartAdvertising()
    {
        final BleAdvertisingMode mode = determineMode(m_mode, m_timeOut, getManager().isForegrounded());
        getManager().managerLayer().startAdvertising(P_Bridge_Utils.getNativeSettings(m_packet, mode, m_power, m_timeOut), P_Bridge_Utils.getNativeData(m_packet), adCallback);
    }

    private static BleAdvertisingMode determineMode(BleAdvertisingMode curMode, Interval timeout, boolean foregrounded)
    {
        if (curMode == BleAdvertisingMode.AUTO)
        {
            if (foregrounded)
            {
                if (timeout == Interval.ZERO)
                {
                    return BleAdvertisingMode.MEDIUM_FREQUENCY;
                }
                else
                {
                    return BleAdvertisingMode.HIGH_FREQUENCY;
                }
            }
            else
            {
                return BleAdvertisingMode.LOW_FREQUENCY;
            }
        }
        else
        {
            return curMode;
        }
    }

}
