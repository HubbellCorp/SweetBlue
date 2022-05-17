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


import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Handler;

import com.idevicesinc.sweetblue.BleCharacteristic;
import com.idevicesinc.sweetblue.BleDescriptor;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.BleService;
import com.idevicesinc.sweetblue.DeviceReconnectFilter;
import com.idevicesinc.sweetblue.NotificationListener;
import com.idevicesinc.sweetblue.utils.CodeHelper;
import com.idevicesinc.sweetblue.utils.P_Const;
import com.idevicesinc.sweetblue.utils.Uuids;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class P_Bridge_Internal
{

    private P_Bridge_Internal()
    {
        throw new RuntimeException("No instances!");
    }



    public static NotificationListener.Type getProperNotificationType(BleCharacteristic char_native, NotificationListener.Type type)
    {
        return P_DeviceServiceManager.getProperNotificationType(char_native, type);
    }

    public static IBleDevice NULL_DEVICE()
    {
        return P_BleDeviceImpl.NULL;
    }

    public static IBleServer NULL_SERVER()
    {
        return P_BleServerImpl.NULL;
    }

    public static P_BleManagerImpl newManager(Context context, BleManagerConfig config)
    {
        return new P_BleManagerImpl(context, config);
    }

    public static String charName(IBleManager mgr, UUID uuid)
    {
        return mgr.getLogger().charName(uuid);
    }

    public static <T> void checkPlease(IBleManager mgr, final T please_nullable, final Class<T> please_class)
    {
        mgr.getLogger().checkPlease(please_nullable, please_class);
    }

    public static void setBleScanReady(IBleManager mgr)
    {
        ((P_BleManagerImpl) mgr).setBleScanReady();
    }

    public static String uuidName(IBleManager mgr, UUID uuid)
    {
        return mgr.getLogger().uuidName(uuid);
    }

    public static String gattStatus(IBleManager mgr, int status)
    {
        return CodeHelper.gattStatus(status, mgr.getLogger().isEnabled());
    }

    public static String serviceName(IBleManager mgr, UUID serviceUuid)
    {
        return mgr.getLogger().serviceName(serviceUuid);
    }

    public static ArrayList<DeviceReconnectFilter.ConnectFailEvent> getConnectionFailHistory(IBleDevice device)
    {
        return device.getConnectionManager().getConnectionFailHistory();
    }

    public static P_DisconnectReason newDisconnectReason(int gattStatus, DeviceReconnectFilter.Timing timing)
    {
        return new P_DisconnectReason(gattStatus, timing);
    }

    public static boolean loggingEnabled(IBleManager mgr)
    {
        return mgr.getLogger().isEnabled();
    }

    public static void logE(IBleManager mgr, String msg)
    {
        mgr.getLogger().e(msg);
    }

    public static IBleDevice newDevice_NULL()
    {
        return P_BleDeviceImpl.NULL;
    }

    public static boolean isPostLollipopScan(P_ScanManager scanMgr)
    {
        return scanMgr.isPostLollipopScan();
    }

    public static List<BleCharacteristic> fromBleService(BleService bleService)
    {
        final BleService service = bleService;

        if (service == null || service.isNull())
            return P_Const.EMPTY_BLECHARACTERISTIC_LIST;

        final List<BluetoothGattCharacteristic> nativeList = service.getService().getCharacteristics();
        final List<BleCharacteristic> list = fromNativeCharList(nativeList);

        return list;
    }

    public static List<BleDescriptor> fromBleCharacteristic(BleCharacteristic bleChar)
    {
        final BleCharacteristic ch = bleChar;

        if (ch == null || ch.isNull())
            return P_Const.EMPTY_BLEDESCRIPTOR_LIST;

        final List<BluetoothGattDescriptor> nativeList = ch.getCharacteristic().getDescriptors();
        final List<BleDescriptor> list = fromNativeDescList(nativeList);

        return list;
    }

    public static BleDescriptor getFromBleCharacteristic(BleCharacteristic characteristic, UUID descUuid)
    {
        final BleCharacteristic ch = characteristic;
        if (ch == null || ch.isNull() || descUuid == null || Uuids.INVALID.equals(descUuid))
            return BleDescriptor.NULL;

        final BluetoothGattDescriptor desc = ch.getCharacteristic().getDescriptor(descUuid);
        if (desc == null)
            return BleDescriptor.NULL;

        return new BleDescriptor(desc);
    }

    public static List<BleService> fromNativeServiceList(List<BluetoothGattService> nativeList)
    {
        if (nativeList == null)
            nativeList = new ArrayList<>(0);

        final List<BleService> list = new ArrayList<>(nativeList.size());
        for (BluetoothGattService s : nativeList)
        {
            list.add(new BleService(s));
        }
        return list;
    }

    public static P_SweetHandler getUpdateHandler(IBleManager mgr)
    {
        return mgr.getPostManager().getUpdateHandler();
    }

    public static Handler getUpdateHandler_Android(IBleManager mgr)
    {
        return mgr.getPostManager().getSweetBlueThreadHandler();
    }




    private static List<BleCharacteristic> fromNativeCharList(List<BluetoothGattCharacteristic> nativeList)
    {
        if (nativeList == null)
            nativeList = new ArrayList<>(0);

        final List<BleCharacteristic> list = new ArrayList<>(nativeList.size());
        for (BluetoothGattCharacteristic bc : nativeList)
        {
            list.add(new BleCharacteristic(bc));
        }
        return list;
    }

    private static List<BleDescriptor> fromNativeDescList(List<BluetoothGattDescriptor> nativeList)
    {
        if (nativeList == null)
            nativeList = new ArrayList<>(0);

        final List<BleDescriptor> list = new ArrayList<>(nativeList.size());
        for (BluetoothGattDescriptor bc : nativeList)
        {
            list.add(new BleDescriptor(bc));
        }
        return list;
    }

    public static void postCallback(IBleManager mgr, Runnable callback)
    {
        mgr.getPostManager().postCallback(callback);
    }

    public static void postUpdateDelayed(IBleManager mgr, Runnable runnable, long delay)
    {
        mgr.getPostManager().postToUpdateThreadDelayed(runnable, delay);
    }
}
