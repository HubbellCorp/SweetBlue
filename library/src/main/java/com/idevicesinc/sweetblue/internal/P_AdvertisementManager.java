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


import android.text.TextUtils;

import com.idevicesinc.sweetblue.AdvertisingListener;
import com.idevicesinc.sweetblue.BleAdvertisingSettings;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerState;
import com.idevicesinc.sweetblue.BleServer;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.utils.BleScanRecord;
import com.idevicesinc.sweetblue.utils.Utils;
import java.util.UUID;
import static com.idevicesinc.sweetblue.BleManagerState.ON;


class P_AdvertisementManager
{

    private final IBleServer m_server;
    // Cached packet
    private BleScanRecord m_advPacket;
    private String m_customName;

    private boolean m_isAdvertising = false;

    private AdvertisingListener m_advertisingListener;


    P_AdvertisementManager(IBleServer server)
    {
        m_server = server;
    }


    final void setListener_Advertising(AdvertisingListener listener)
    {
        m_advertisingListener = listener;
    }

    final AdvertisingListener getListener_advertising()
    {
        return m_advertisingListener;
    }

    final boolean isAdvertising()
    {
        return m_isAdvertising;
    }

    final boolean isAdvertising(UUID serviceUuid)
    {
        if (Utils.isLollipop() && m_advPacket != null)
        {
            return m_advPacket.hasUuid(serviceUuid);
        }
        return false;
    }

    final AdvertisingListener.AdvertisingEvent startAdvertising(BleScanRecord advertisePacket, BleAdvertisingSettings settings, AdvertisingListener listener)
    {
        if (m_server.isNull())
        {
            getManager().getLogger().e(BleServer.class.getSimpleName() + " is null!");

            return P_Bridge_User.newAdvertisingEvent(getManager().getBleServer(m_server), AdvertisingListener.Status.NULL_SERVER);
        }

        if (!isAdvertisingSupportedByAndroidVersion())
        {
            getManager().getLogger().e("Advertising NOT supported on android OS's less than Lollipop!");

            return P_Bridge_User.newAdvertisingEvent(getManager().getBleServer(m_server), AdvertisingListener.Status.ANDROID_VERSION_NOT_SUPPORTED);
        }

        if (!isAdvertisingSupportedByChipset())
        {
            getManager().getLogger().e("Advertising NOT supported by current device's chipset!");

            return P_Bridge_User.newAdvertisingEvent(getManager().getBleServer(m_server), AdvertisingListener.Status.CHIPSET_NOT_SUPPORTED);
        }

        if (!getManager().is(BleManagerState.ON))
        {
            getManager().getLogger().e(BleManager.class.getSimpleName() + " is not " + ON + "! Please use the turnOn() method first.");

            return P_Bridge_User.newAdvertisingEvent(getManager().getBleServer(m_server), AdvertisingListener.Status.BLE_NOT_ON);
        }

        final String customName = m_server.getName();
        if (!TextUtils.isEmpty(customName))
        {
            m_customName = customName;
            m_server.setName(m_customName);
        }

        if (m_isAdvertising)
        {
            getManager().getLogger().w(BleServer.class.getSimpleName() + " is already advertising!");

            return P_Bridge_User.newAdvertisingEvent(getManager().getBleServer(m_server), AdvertisingListener.Status.ALREADY_STARTED);
        }
        else
        {
            getManager().ASSERT(!getManager().getTaskManager().isCurrentOrInQueue(P_Task_Advertise.class, getManager()), "");

            getManager().getTaskManager().add(new P_Task_Advertise(m_server, advertisePacket, settings, listener));
            return P_Bridge_User.newAdvertisingEvent(getManager().getBleServer(m_server), AdvertisingListener.Status.NULL);
        }
    }

    final void setCustomName(String name)
    {
        m_customName = name;
    }

    final void stopAdvertising()
    {
        if (Utils.isLollipop())
        {

            final P_Task_Advertise adTask = getManager().getTaskManager().get(P_Task_Advertise.class, getManager());
            if (adTask != null)
            {
                adTask.stopAdvertising();
                adTask.clearFromQueue();
            }
            else
            {
                // We don't leave the advertising task in the queue, so this is what's going to be called 99% of the time.
                getManager().managerLayer().stopAdvertising();
            }
            onAdvertiseStop();
        }
    }

    final void onAdvertiseStart(BleScanRecord packet)
    {
        getManager().getLogger().d("Advertising started successfully.");
        m_advPacket = packet;
        m_isAdvertising = true;
    }

    final void onAdvertiseStartFailed(AdvertisingListener.Status result)
    {
        getManager().getLogger().e("Failed to start advertising! Result: " + result);
        onAdvertiseStop();
    }

    final void onAdvertiseStop()
    {
        getManager().getLogger().d("Advertising stopped.");
        m_advPacket = null;
        m_isAdvertising = false;
        // Reset the adaptor name, if a custom name has been set.
        if (!TextUtils.isEmpty(m_customName))
            m_server.resetAdaptorName();
    }

    /**
     * Checks to see if the device is running an Android OS which supports
     * advertising. This is forwarded from {@link BleManager#isAdvertisingSupportedByAndroidVersion()}.
     */
    final boolean isAdvertisingSupportedByAndroidVersion()
    {
        return getManager().isAdvertisingSupportedByAndroidVersion();
    }

    /**
     * Checks to see if the device supports advertising. This is forwarded from {@link BleManager#isAdvertisingSupportedByChipset()}.
     */
    final boolean isAdvertisingSupportedByChipset()
    {
        return getManager().isAdvertisingSupportedByChipset();
    }

    /**
     * Checks to see if the device supports advertising BLE services. This is forwarded from {@link BleManager#isAdvertisingSupported()}.
     */
    final boolean isAdvertisingSupported()
    {
        return isAdvertisingSupportedByAndroidVersion() && isAdvertisingSupportedByChipset();
    }


    private IBleManager getManager()
    {
        return m_server.getIManager();
    }

}
