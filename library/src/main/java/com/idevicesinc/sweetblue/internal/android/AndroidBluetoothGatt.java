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

package com.idevicesinc.sweetblue.internal.android;


import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Build;
import android.os.DeadObjectException;
import android.util.Log;

import com.idevicesinc.sweetblue.BleCharacteristic;
import com.idevicesinc.sweetblue.BleConnectionPriority;
import com.idevicesinc.sweetblue.BleDescriptor;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleService;
import com.idevicesinc.sweetblue.BleStatuses;
import com.idevicesinc.sweetblue.UhOhListener;
import com.idevicesinc.sweetblue.compat.K_Util;
import com.idevicesinc.sweetblue.compat.L_Util;
import com.idevicesinc.sweetblue.compat.O_Util;
import com.idevicesinc.sweetblue.internal.IBleDevice;
import com.idevicesinc.sweetblue.internal.IBleManager;
import com.idevicesinc.sweetblue.internal.P_Bridge_Internal;
import com.idevicesinc.sweetblue.utils.LogFunction;
import com.idevicesinc.sweetblue.utils.Phy;
import com.idevicesinc.sweetblue.utils.Utils;
import java.lang.reflect.Field;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.UUID;


/**
 * Default implementation of {@link IBluetoothGatt}, and wraps {@link BluetoothGatt}. This class is used by default
 * by the library, and the only time it should NOT be used, is when unit testing.
 *
 * @see com.idevicesinc.sweetblue.BleManagerConfig#gattFactory
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public final class AndroidBluetoothGatt implements IBluetoothGatt
{

    private static final String FIELD_NAME_AUTH_RETRY = "mAuthRetry";
    private static final String FIELD_NAME_NOUGAT_AUTH_RETRY_STATE = "mAuthRetryState";

    private static String s_authRetryFieldName;
    private static boolean s_nougatMr2 = false;

    private Field m_authRetryField = null;

    private BluetoothGatt m_gatt;
    private final IBleDevice m_device;


    public AndroidBluetoothGatt(IBleDevice device)
    {
        m_device = device;
    }


    @Override
    public final BleDevice getBleDevice()
    {
        return m_device.getBleDevice();
    }

    @Override
    public final void setGatt(BluetoothGatt gatt)
    {
        m_gatt = gatt;
    }

    @Override
    public final BluetoothGatt getGatt()
    {
        return m_gatt;
    }

    @Override
    public final Boolean getAuthRetryValue()
    {
        if (m_gatt != null)
        {
            final Boolean result;
            if (m_authRetryField == null)
            {
                if (s_authRetryFieldName == null)
                {
                    s_authRetryFieldName = getAuthRetryName();
                    if (s_authRetryFieldName == null)
                    {
                        // The field no longer exists as of Android 10 (api 29), so don't bother showing this assertion
                        if (Build.VERSION.SDK_INT < 29)
                            getManager().ASSERT(false, "Unable to get auth retry field! Neither mAuthRetry, or mAuthRetryState exist!");
                        return null;
                    }
                }
                try
                {
                    m_authRetryField = m_gatt.getClass().getDeclaredField(s_authRetryFieldName);
                } catch (NoSuchFieldException e1)
                {
                    getManager().ASSERT(false, "Problem getting field " + m_gatt.getClass().getSimpleName() + "." + s_authRetryFieldName);
                }
            }
            try
            {
                final boolean isAccessible_saved = m_authRetryField.isAccessible();
                m_authRetryField.setAccessible(true);
                if (s_nougatMr2)
                {
                    int state = m_authRetryField.getInt(m_gatt);

                    // TODO - Refactor this to pass along with auth retry state it is, so we know how better to deal with it. (Is simply bonding enough?).
                    result = state != BleStatuses.AUTH_RETRY_STATE_IDLE;
                }
                else
                {
                    result = m_authRetryField.getBoolean(m_gatt);
                }
                m_authRetryField.setAccessible(isAccessible_saved);

                return result;
            } catch (Exception e)
            {
                getManager().ASSERT(false, "Unable to get value of " + m_gatt.getClass().getSimpleName() + "." + m_authRetryField.getName());
            }
        }
        else
        {
            getManager().ASSERT(false, "Expected gatt object to be not null");
        }
        return null;
    }

    @Override
    public boolean setPhy(Phy options)
    {
        if (m_gatt != null)
        {
            O_Util.setPhyLayer(m_gatt, options.getTxMask(), options.getRxMask(), options.getPhyOptions());
            return true;
        }
        return false;
    }

    @Override
    public boolean readPhy()
    {
        if (m_gatt != null)
        {
            O_Util.readPhyLayer(m_gatt);
            return true;
        }
        return false;
    }

    private String getAuthRetryName()
    {
        Field[] fields = m_gatt.getClass().getDeclaredFields();
        for (Field f : fields)
        {
            if (f.getName().equals(FIELD_NAME_AUTH_RETRY))
            {
                return FIELD_NAME_AUTH_RETRY;
            }
            else if (f.getName().equals(FIELD_NAME_NOUGAT_AUTH_RETRY_STATE))
            {
                s_nougatMr2 = true;
                return FIELD_NAME_NOUGAT_AUTH_RETRY_STATE;
            }
        }
        return null;
    }

    @Override
    public final boolean equals(P_GattHolder gatt)
    {
        return gatt.getGatt() == m_gatt;
    }

    private IBleManager getManager()
    {
        return m_device.getIManager();
    }

    @SuppressLint("MissingPermission")
    @Override
    public final UhOhListener.UhOh closeGatt() {
        UhOhListener.UhOh uhoh = null;
        if( m_gatt == null )  return uhoh;

        //--- DRK > Tried this to see if it would kill autoConnect, but alas it does not, at least on S5.
        //---		Don't want to keep it here because I'm afraid it has a better chance to do bad than good.
//			if( disconnectAlso )
//			{
//				m_gatt.disconnect();
//			}

        //--- DRK > This can randomly throw an NPE down stream...NOT from m_gatt being null, but a few methods downstream.
        //---		See below for more info.
        try
        {
            m_gatt.close();
        } catch (Exception e)
        {
            if (e instanceof DeadObjectException)
            {
                //--- RB > It has been observed by some customers that a DeadObjectException can happen here. Nothing we can do about it, just
                // checking for it, and throwing to the UhOh Listener as a DeadObjectException

//				android.os.DeadObjectException
//				at android.os.BinderProxy.transactNative(Native Method)
//				at android.os.BinderProxy.transact(Binder.java:503)
//				at android.bluetooth.IBluetoothGatt$Stub$Proxy.unregisterClient(IBluetoothGatt.java:1009)
//				at android.bluetooth.BluetoothGatt.unregisterApp(BluetoothGatt.java:820)
//				at android.bluetooth.BluetoothGatt.close(BluetoothGatt.java:759)
//				at com.idevicesinc.sweetblue.internal.P_NativeDeviceWrapper.closeGatt(P_NativeDeviceWrapper.java:319)
//				at com.idevicesinc.sweetblue.internal.P_NativeDeviceWrapper.closeGattIfNeeded(P_NativeDeviceWrapper.java:301)
//				at com.idevicesinc.sweetblue.BleDevice.onNativeConnectFail(BleDevice.java:5782)
//				at com.idevicesinc.sweetblue.internal.P_BleDevice_Listeners$1.onStateChange(P_BleDevice_Listeners.java:51)
//				at com.idevicesinc.sweetblue.internal.PA_Task.setState(PA_Task.java:148)
//				at com.idevicesinc.sweetblue.internal.PA_Task.setEndingState(PA_Task.java:288)
//				at com.idevicesinc.sweetblue.internal.P_TaskManager.endCurrentTask(P_TaskManager.java:288)
//				at com.idevicesinc.sweetblue.internal.P_TaskManager.tryEndingTask_mainThread(P_TaskManager.java:395)
//				at com.idevicesinc.sweetblue.internal.P_TaskManager.tryEndingTask(P_TaskManager.java:387)
//				at com.idevicesinc.sweetblue.internal.PA_Task.timeout(PA_Task.java:183)
//				at com.idevicesinc.sweetblue.internal.PA_Task.update_internal(PA_Task.java:354)
//				at com.idevicesinc.sweetblue.internal.P_TaskManager.update(P_TaskManager.java:236)
//				at com.idevicesinc.sweetblue.BleManager.update(BleManager.java:3245)
//				at com.idevicesinc.sweetblue.BleManager$1.onUpdate(BleManager.java:746)
//				at com.idevicesinc.sweetblue.utils.UpdateLoop$1.run(UpdateLoop.java:24)
//				at android.os.Handler.handleCallback(Handler.java:739)
//				at android.os.Handler.dispatchMessage(Handler.java:95)
//				at android.os.Looper.loop(Looper.java:148)
//				at android.app.ActivityThread.main(ActivityThread.java:7303)
//				at java.lang.reflect.Method.invoke(Native Method)
//				at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:1230)
//				at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:1120)
                uhoh = UhOhListener.UhOh.DEAD_OBJECT_EXCEPTION;
            }
            else
            {
                //--- DRK > From Flurry crash reports...happened several times on S4 running 4.4.4 but was not able to reproduce.
//				This error occurred: java.lang.NullPointerException
//				android.os.Parcel.readException(Parcel.java:1546)
//				android.os.Parcel.readException(Parcel.java:1493)
//				android.bluetooth.IBluetoothGatt$Stub$Proxy.unregisterClient(IBluetoothGatt.java:905)
//				android.bluetooth.BluetoothGatt.unregisterApp(BluetoothGatt.java:710)
//				android.bluetooth.BluetoothGatt.close(BluetoothGatt.java:649)
//				com.idevicesinc.sweetblue.internal.P_NativeDeviceWrapper.closeGatt(P_NativeDeviceWrapper.java:238)
//				com.idevicesinc.sweetblue.internal.P_NativeDeviceWrapper.closeGattIfNeeded(P_NativeDeviceWrapper.java:221)
//				com.idevicesinc.sweetblue.BleDevice.onNativeConnectFail(BleDevice.java:2193)
//				com.idevicesinc.sweetblue.internal.P_BleDevice_Listeners$1.onStateChange_synchronized(P_BleDevice_Listeners.java:78)
//				com.idevicesinc.sweetblue.internal.P_BleDevice_Listeners$1.onStateChange(P_BleDevice_Listeners.java:49)
//				com.idevicesinc.sweetblue.internal.PA_Task.setState(PA_Task.java:118)
//				com.idevicesinc.sweetblue.internal.PA_Task.setEndingState(PA_Task.java:242)
//				com.idevicesinc.sweetblue.internal.P_TaskManager.endCurrentTask(P_TaskManager.java:220)
//				com.idevicesinc.sweetblue.internal.P_TaskManager.tryEndingTask(P_TaskManager.java:267)
//				com.idevicesinc.sweetblue.internal.P_TaskManager.fail(P_TaskManager.java:260)
//				com.idevicesinc.sweetblue.internal.P_BleDevice_Listeners.onConnectionStateChange_synchronized(P_BleDevice_Listeners.java:168)
                uhoh = UhOhListener.UhOh.RANDOM_EXCEPTION;
            }
        }
        m_gatt = null;
        return uhoh;
    }

    @Override
    public final List<BleService> getNativeServiceList(LogFunction logger)
    {
        if (m_gatt == null)
        {
            return null;
        }
        List<BluetoothGattService> list_native = null;

        try
        {
            list_native = m_gatt.getServices();
        } catch (Exception e)
        {
            UhOhListener.UhOh uhoh;
            if (e instanceof ConcurrentModificationException)
            {
                uhoh = UhOhListener.UhOh.CONCURRENT_EXCEPTION;
            }
            else
            {
                uhoh = UhOhListener.UhOh.RANDOM_EXCEPTION;
            }
            getManager().uhOh(uhoh);
            if (logger != null)
                logger.onLog(Log.ERROR, "Got a " + e.getClass().getSimpleName() + " with a message of " + e.getMessage() + " when trying to get the list of native services!");
        }
        final List<BleService> list = P_Bridge_Internal.fromNativeServiceList(list_native);
        return list;
    }


    @Override
    public BleService getBleService(UUID serviceUuid, LogFunction logger)
    {
        BleService wService;
        final BluetoothGattService service;
        try
        {
            service = m_gatt.getService(serviceUuid);
            wService = new BleService(service);
        }
        catch (Exception e)
        {
            UhOhListener.UhOh uhoh;
            if (e instanceof ConcurrentModificationException)
            {
                uhoh = UhOhListener.UhOh.CONCURRENT_EXCEPTION;
            }
            else
            {
                uhoh = UhOhListener.UhOh.RANDOM_EXCEPTION;
            }
            getManager().uhOh(uhoh);
            wService = new BleService(uhoh);
            if (logger != null)
                logger.onLog(Log.ERROR, "Got a " + e.getClass().getSimpleName() + " with a message of " + e.getMessage() + " when trying to get the native service!");
        }
        return wService;
    }

    @Override
    public final boolean isGattNull()
    {
        return m_gatt == null;
    }

    @Override
    public final void connect(IBluetoothDevice device, Context context, boolean useAutoConnect, BluetoothGattCallback callback)
    {
        m_gatt = device.connect(context, useAutoConnect, callback);
    }

    @Override
    public final boolean requestMtu(int mtu)
    {
        if (m_gatt != null)
        {
            return L_Util.requestMtu(m_gatt, mtu);
        }
        return false;
    }

    @Override
    public final boolean refreshGatt()
    {
        if (m_gatt != null)
        {
            Utils.refreshGatt(m_gatt);
        }
        return false;
    }

    @Override
    public final boolean requestConnectionPriority(BleConnectionPriority priority)
    {
        if (m_gatt != null)
        {
            return L_Util.requestConnectionPriority(m_gatt, priority.getNativeMode());
        }
        return false;
    }

    @SuppressLint("MissingPermission")
    @Override
    public final void disconnect()
    {
        if (m_gatt != null)
        {
            m_gatt.disconnect();
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public final boolean readCharacteristic(BleCharacteristic characteristic)
    {
        if (m_gatt != null && characteristic != null && !characteristic.isNull())
        {
            return m_gatt.readCharacteristic(characteristic.getCharacteristic());
        }
        return false;
    }

    @Override
    public final boolean setCharValue(BleCharacteristic characteristic, byte[] data)
    {
        if (characteristic != null && !characteristic.isNull())
        {
            return characteristic.getCharacteristic().setValue(data);
        }
        return false;
    }

    @SuppressLint("MissingPermission")
    @Override
    public final boolean writeCharacteristic(BleCharacteristic characteristic)
    {
        if (m_gatt != null && characteristic != null && !characteristic.isNull())
        {
            return m_gatt.writeCharacteristic(characteristic.getCharacteristic());
        }
        return false;
    }

    @SuppressLint("MissingPermission")
    @Override
    public final boolean setCharacteristicNotification(BleCharacteristic characteristic, boolean enable)
    {
        if (m_gatt != null && characteristic != null && !characteristic.isNull())
        {
            return m_gatt.setCharacteristicNotification(characteristic.getCharacteristic(), enable);
        }
        return false;
    }

    @SuppressLint("MissingPermission")
    @Override
    public final boolean readDescriptor(BleDescriptor descriptor)
    {
        if (m_gatt != null && descriptor != null && !descriptor.isNull())
        {
            return m_gatt.readDescriptor(descriptor.getDescriptor());
        }
        return false;
    }

    @Override
    public final boolean setDescValue(BleDescriptor descriptor, byte[] data)
    {
        if (descriptor != null && !descriptor.isNull())
        {
            return descriptor.getDescriptor().setValue(data);
        }
        return false;
    }

    @SuppressLint("MissingPermission")
    @Override
    public final boolean writeDescriptor(BleDescriptor descriptor)
    {
        if (m_gatt != null && descriptor != null && !descriptor.isNull())
        {
            if (!Utils.isNougat())
                return preNougatWriteDescriptor(descriptor);
            else
                return m_gatt.writeDescriptor(descriptor.getDescriptor());
        }
        return false;
    }

    private boolean preNougatWriteDescriptor(BleDescriptor descriptor)
    {
        // On Android 4.3 -> 6.0, there was a bug where descriptors would use the parent characteristic's write type. Most people
        // won't notice the difference, as most use write type default. However, if the parent's write type is WRITE_NO_RESPONSE,
        // then the descriptor won't properly get written, and notifications won't work on those OS levels. This is to make sure
        // it always works regardless of OS level
        final int originalWriteType;
        BluetoothGattCharacteristic ch = descriptor.getCharacteristic().getCharacteristic();
        originalWriteType = ch.getWriteType();
        ch.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

        @SuppressLint("MissingPermission") boolean success = m_gatt.writeDescriptor(descriptor.getDescriptor());

        // Set the write type back to what it was originally
        ch.setWriteType(originalWriteType);

        return success;
    }

    @SuppressLint("MissingPermission")
    @Override
    public final boolean discoverServices()
    {
        if (m_gatt != null)
            return m_gatt.discoverServices();
        return false;
    }

    @SuppressLint("MissingPermission")
    @Override
    public final boolean executeReliableWrite()
    {
        if (m_gatt != null)
            return m_gatt.executeReliableWrite();
        return false;
    }

    @SuppressLint("MissingPermission")
    @Override
    public final boolean beginReliableWrite()
    {
        if (m_gatt != null)
            return m_gatt.beginReliableWrite();
        return false;
    }

    @SuppressLint("MissingPermission")
    @Override
    public final void abortReliableWrite(BluetoothDevice device)
    {
        if (m_gatt != null)
        {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR2)
            {
                m_gatt.abortReliableWrite(device);
            }
            else
            {
                K_Util.abortReliableWrite(m_gatt);
            }
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public final boolean readRemoteRssi()
    {
        if (m_gatt != null)
            return m_gatt.readRemoteRssi();
        return false;
    }

}
