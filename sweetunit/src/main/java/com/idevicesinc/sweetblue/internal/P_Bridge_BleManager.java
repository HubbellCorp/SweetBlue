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


import android.content.Context;
import android.content.Intent;

import com.idevicesinc.sweetblue.BleScanApi;
import com.idevicesinc.sweetblue.BleScanPower;
import com.idevicesinc.sweetblue.BleStatuses;
import com.idevicesinc.sweetblue.compat.L_Util;
import com.idevicesinc.sweetblue.internal.android.IBluetoothDevice;
import com.idevicesinc.sweetblue.internal.android.IBluetoothManager;
import com.idevicesinc.sweetblue.internal.android.P_DeviceHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.idevicesinc.sweetblue.BleManagerState.OFF;
import static com.idevicesinc.sweetblue.BleManagerState.ON;


public final class P_Bridge_BleManager
{

    private P_Bridge_BleManager() {}


    public static P_BleDevice_ListenerProcessor getNativeListener(IBleDevice device)
    {
        return device.getNativeManager().getNativeListener();
    }

    public static <T extends IBluetoothManager> T getManagerLayer(IBleDevice device)
    {
        return (T) device.getNativeManager().getManagerLayer();
    }

    public static void addScanResult(IBleManager mgr, P_DeviceHolder device, int rssi, byte[] scanRecord)
    {
        mgr.getScanManager().addScanResult(device, rssi, scanRecord);
    }

    public static void addBatchScanResults(IBleManager mgr, final List<L_Util.ScanResult> devices)
    {
        mgr.getScanManager().addBatchScanResults(devices);
    }

    public static void postUpdateDelayed(IBleManager mgr, Runnable run, long delay)
    {
        mgr.getPostManager().postToUpdateThreadDelayed(run, delay);
    }

    public static void postMainDelayed(IBleManager mgr, Runnable run, long delay)
    {
        mgr.getPostManager().postToMainDelayed(run, delay);
    }

    public static void onNativeBleStateChangeFromBroadcastReceiver(IBleManager mgr, Context context, Intent intent)
    {
        mgr.getNativeManager().getListenerProcessor().onNativeBleStateChangeFromBroadcastReceiver(context, intent);
    }

    public static void onNativeBondStateChanged(IBleManager mgr, final IBluetoothDevice device_native, final int previousState, final int newState, final int failReason)
    {
        mgr.getNativeManager().getListenerProcessor().onNativeBondStateChanged(device_native, previousState, newState, failReason);
    }

    public static void onClassicDiscoveryFinished(IBleManager mgr)
    {
        mgr.getNativeManager().getListenerProcessor().onClassicDiscoveryFinished();
    }

    public static void forceOn(IBleManager mgr)
    {
        mgr.getStateTracker().update(PA_StateTracker.E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, ON, true, OFF, false);
    }

    public static void forceOff(IBleManager mgr)
    {
        mgr.getStateTracker().update(PA_StateTracker.E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, OFF, true, ON, false);
    }

    public static BleScanApi getScanApi(IBleManager mgr)
    {
        return mgr.getScanManager().getCurrentApi();
    }

    public static BleScanPower getScanPower(IBleManager mgr)
    {
        return mgr.getScanManager().getCurrentPower();
    }

    public static long getUpdateRate(IBleManager mgr)
    {
        return mgr.getUpdateRate();
    }

    public static PE_TaskPriority randomPriority(Random r)
    {
        return PE_TaskPriority.values()[r.nextInt(PE_TaskPriority.values().length)];
    }

    public static void addTask(IBleManager mgr, PA_Task task)
    {
        mgr.getTaskManager().add(task);
    }

    public static void suspendQueue(IBleManager mgr)
    {
        mgr.getTaskManager().setSuspended(true);
    }

    public static void unsuspendQueue(IBleManager mgr)
    {
        mgr.getTaskManager().setSuspended(false);
    }

    public static int getQueueSize(IBleManager mgr)
    {
        return mgr.getTaskManager().getSize();
    }

    public static void clearQueueOf(IBleManager mgr, Class<? extends PA_Task> clazz)
    {
        mgr.getTaskManager().clearQueueOf(clazz, mgr);
    }

    public static <T extends PA_Task> List<T> getFromQueue(IBleManager mgr, Class<T> clazz)
    {
        List<PA_Task> l = mgr.getTaskManager().getRaw();
        List<T> list = new ArrayList<>();
        for (PA_Task t : l)
        {
            if (clazz.isAssignableFrom(t.getClass()))
            {
                list.add((T) t);
            }
        }
        return list;
    }

    public static boolean isInQueue(IBleManager mgr, String className)
    {
        final Class clazz;
        try
        {
            clazz = Class.forName(className);
            if (!PA_Task.class.isAssignableFrom(clazz))
                return false;
        }
        catch (Exception e)
        {
            return false;
        }
        return mgr.getTaskManager().isInQueue(clazz, mgr);
    }

    public static boolean isInQueue(IBleManager mgr, IBleDevice device, String className)
    {
        final Class clazz;
        try
        {
            clazz = Class.forName(className);
            if (!PA_Task.class.isAssignableFrom(clazz))
                return false;
        }
        catch (Exception e)
        {
            return false;
        }
        return mgr.getTaskManager().isInQueue(clazz, device);
    }

    public static int getPositionInQueue(IBleManager mgr, IBleDevice device, String className)
    {
        final Class clazz;
        try
        {
            clazz = Class.forName(className);
            if (!PA_Task.class.isAssignableFrom(clazz))
                return -1;
        }
        catch (Exception e)
        {
            return -1;
        }
        return mgr.getTaskManager().positionInQueue(clazz, device);
    }

    public static boolean isInQueue(IBleManager mgr, Class<? extends PA_Task> taskClass)
    {
        return mgr.getTaskManager().isInQueue(taskClass, mgr);
    }

    public static boolean isCurrent(IBleManager mgr, String className)
    {
        final Class clazz;
        try
        {
            clazz = Class.forName(className);
            if (!PA_Task.class.isAssignableFrom(clazz))
                return false;
        }
        catch (Exception e)
        {
            return false;
        }
        return mgr.getTaskManager().isCurrent(clazz, mgr);
    }

    public static boolean isCurrent(IBleManager mgr, Class<? extends PA_Task> taskClass)
    {
        return mgr.getTaskManager().isCurrent(taskClass, mgr);
    }

    public static int getPriority(PA_Task task)
    {
        return task.getPriority().ordinal();
    }

    public static boolean hasDevice(IBleManager mgr, IBleDevice device)
    {
        return mgr.getDeviceManager().has(device);
    }

}
