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
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Context;
import android.os.Build;
import android.os.DeadObjectException;
import com.idevicesinc.sweetblue.BleStatuses;
import com.idevicesinc.sweetblue.UhOhListener;
import com.idevicesinc.sweetblue.compat.L_Util;
import com.idevicesinc.sweetblue.compat.M_Util;
import com.idevicesinc.sweetblue.compat.O_Util;
import com.idevicesinc.sweetblue.internal.IBleManager;
import com.idevicesinc.sweetblue.internal.P_Bridge_Internal;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.P_Const;
import com.idevicesinc.sweetblue.utils.Utils;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import static com.idevicesinc.sweetblue.BleManagerState.OFF;
import static com.idevicesinc.sweetblue.BleManagerState.ON;


/**
 * Default implementation of {@link IBluetoothManager}, and wraps {@link BluetoothManager} and {@link BluetoothAdapter}. This class is used by default
 * by the library, and the only time it should NOT be used, is when unit testing.
 *
 * @see com.idevicesinc.sweetblue.BleManagerConfig#bluetoothManagerImplementation
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public final class AndroidBluetoothManager implements IBluetoothManager
{

    private BluetoothManager m_manager;
    private BluetoothAdapter m_adaptor;
    private IBleManager m_bleManager;
    private static Method m_getLeState_marshmallow;
    private static Integer m_refState;
    private static Integer m_state;


    public final void setIBleManager(IBleManager mgr)
    {
        m_bleManager = mgr;
    }


    @SuppressLint("MissingPermission")
    @Override
    public final int getConnectionState(IBluetoothDevice device, int profile)
    {
        if (m_manager != null)
        {
            return m_manager.getConnectionState(device.getNativeDevice(), BluetoothGatt.GATT_SERVER);
        }
        return 0;
    }

    @SuppressLint("MissingPermission")
    @Override
    public final boolean startDiscovery()
    {
        if (m_adaptor != null)
        {
            return m_adaptor.startDiscovery();
        }
        return false;
    }

    @Override
    public final boolean isManagerNull()
    {
        return m_manager == null || m_adaptor == null;
    }

    @Override
    public final void resetManager(Context context)
    {
        m_manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        m_adaptor = m_manager.getAdapter();
    }

    @SuppressLint("MissingPermission")
    @Override
    public final boolean disable()
    {
        if (m_adaptor != null)
            return m_adaptor.disable();

        return false;
    }

    @SuppressLint("MissingPermission")
    @Override
    public final boolean enable()
    {
        if (m_adaptor != null)
            return m_adaptor.enable();

        return false;
    }

    @Override
    public final boolean isMultipleAdvertisementSupported()
    {
        return L_Util.isAdvertisingSupportedByChipset(m_adaptor);
    }

    @Override
    public final boolean isBluetooth5LongRangeSupported()
    {
        if (m_adaptor != null)
            return O_Util.isLongRangeSupported(m_adaptor);

        return false;
    }

    @Override
    public final boolean isBluetooth5HighSpeedSupported()
    {
        if (m_adaptor != null)
            return O_Util.isHighSpeedSupported(m_adaptor);

        return false;
    }

    @Override
    public final int getState()
    {
        if (m_adaptor != null)
            return m_adaptor.getState();

        return BleStatuses.STATE_OFF;
    }

    @SuppressLint("DiscouragedPrivateApi")
    @Override
    public final int getBleState()
    {
        try
        {
            if (m_getLeState_marshmallow == null)
            {
                m_getLeState_marshmallow = BluetoothAdapter.class.getDeclaredMethod("getLeState");
            }
            m_refState = (Integer) m_getLeState_marshmallow.invoke(m_adaptor);
            m_state = m_adaptor.getState();
            // This is to fix an issue on the S7 (and perhaps other phones as well), where the OFF
            // state is never returned from the getLeState method. This is because the BLE_ states represent if LE only mode is on/off. This does NOT
            // relate to the Bluetooth radio being on/off. So, we check if STATE_BLE_ON, and the normal getState() method returns OFF, we
            // will return a state of OFF here.
            if (m_refState == BleStatuses.STATE_BLE_ON && m_state == OFF.getNativeCode())
            {
                return m_state;
            }
            else
            {
                // --- RB  > Not sure why this was setting to off, when the above handles the exception we want to handle. Commenting out for now.
//                m_refState = BleStatuses.STATE_OFF;
            }
            return m_refState;
        } catch (Exception e)
        {
            if (e instanceof DeadObjectException)
            {
                UhOhListener.UhOh uhoh = UhOhListener.UhOh.DEAD_OBJECT_EXCEPTION;
                m_bleManager.uhOh(uhoh);
            }
            return m_adaptor.getState();
        }
    }

    @SuppressLint({"MissingPermission", "HardwareIds"})
    @Override
    public final String getAddress()
    {
        if (m_adaptor != null)
            return m_adaptor.getAddress();

        return P_Const.NULL_MAC;
    }

    @SuppressLint("MissingPermission")
    @Override
    public String getName()
    {
        if (m_adaptor != null)
            return m_adaptor.getName();
        return null;
    }

    @SuppressLint("MissingPermission")
    @Override
    public boolean setName(String name)
    {
        if (m_adaptor != null)
            return m_adaptor.setName(name);
        else
            return false;
    }

    @SuppressLint("MissingPermission")
    @Override
    public final P_ServerHolder openGattServer(Context context, IServerListener listeners)
    {
        return P_ServerHolder.newHolder(m_manager.openGattServer(context, listeners.getCallback()));
    }

    @Override
    public final void startAdvertising(AdvertiseSettings settings, AdvertiseData adData, L_Util.AdvertisingCallback callback)
    {
        if (!L_Util.startAdvertising(m_adaptor, settings, adData, callback))
        {
            m_bleManager.ASSERT(false, "Unable to start advertising!");
            P_Bridge_Internal.logE(m_bleManager, "Failed to start advertising!");
        }
    }

    @Override
    public final void stopAdvertising()
    {
        L_Util.stopAdvertising(m_adaptor);
    }

    @SuppressLint("MissingPermission")
    @Override
    public final Set<P_DeviceHolder> getBondedDevices()
    {
        Set<P_DeviceHolder> set = new HashSet<>();
        if (m_adaptor != null)
        {
            Set<BluetoothDevice> bonded = m_adaptor.getBondedDevices();
            if (bonded != null && !bonded.isEmpty())
            {
                for (BluetoothDevice device : bonded)
                    set.add(P_DeviceHolder.newHolder(device));
            }
        }

        return set;
    }

    @SuppressLint("MissingPermission")
    @Override
    public final boolean cancelDiscovery()
    {
        if (m_adaptor != null)
        {
            return m_adaptor.cancelDiscovery();
        }
        return false;
    }

    @Override
    public final BluetoothDevice getRemoteDevice(String macAddress)
    {
        if (m_adaptor != null)
            return m_adaptor.getRemoteDevice(macAddress);

        return null;
    }

    @Override
    public final BluetoothAdapter getNativeAdaptor()
    {
        return m_adaptor;
    }

    @Override
    public final BluetoothManager getNativeManager()
    {
        return m_manager;
    }

    @Override
    public final boolean isLocationEnabledForScanning_byOsServices()
    {
        return Utils.isLocationEnabledForScanning_byOsServices(m_bleManager.getApplicationContext());
    }

    @Override
    public final boolean isLocationEnabledForScanning_byRuntimePermissions()
    {
        return Utils.isLocationEnabledForScanning_byRuntimePermissions(m_bleManager.getApplicationContext(), m_bleManager.getConfigClone().requestBackgroundOperation);
    }

    @Override
    public final boolean isLocationEnabledForScanning()
    {
        return Utils.isLocationEnabledForScanning(m_bleManager.getApplicationContext(), m_bleManager.getConfigClone().requestBackgroundOperation);
    }

    @Override
    public final boolean isBluetoothEnabled()
    {
        if (m_adaptor != null)
        {
            return m_adaptor.isEnabled();
        }
        else
        {
            // If the BleManager instance is somehow null here, we'll bomb out and say BLE isnt enabled
            if (m_bleManager == null)
            {
                return false;
            }
            return m_bleManager.is(ON);
        }
    }

    @Override
    public final void startLScan(int scanMode, Interval delay, L_Util.ScanCallback callback)
    {
        L_Util.startNativeScan(m_adaptor, scanMode, delay, m_bleManager.getConfigClone().defaultNativeScanFilterList, callback);
    }

    @Override
    public final void startMScan(int scanMode, int matchMode, int matchNum, Interval delay, L_Util.ScanCallback callback)
    {
        M_Util.startNativeScan(m_adaptor, scanMode, matchMode, matchNum, delay, m_bleManager.getConfigClone().defaultNativeScanFilterList, callback);
    }

    @SuppressLint("MissingPermission")
    @Override
    public final boolean startLeScan(BluetoothAdapter.LeScanCallback callback)
    {
        if (m_adaptor != null)
            return m_adaptor.startLeScan(callback);

        return false;
    }

    @Override
    public boolean startPendingIntentScan(int scanMode, int matchMode, int matchNum, Interval delay, PendingIntent pendingIntent)
    {
        return O_Util.startScan(m_adaptor, scanMode, matchMode, matchNum, delay, m_bleManager.getConfigClone().defaultNativeScanFilterList, pendingIntent);
    }

    @SuppressLint("MissingPermission")
    @Override
    public final void stopLeScan(BluetoothAdapter.LeScanCallback callback)
    {
        if (m_adaptor != null)
        {
            if (P_Bridge_Internal.isPostLollipopScan(m_bleManager.getScanManager()))
            {
                L_Util.stopNativeScan(m_adaptor);
            }
            else
            {
                m_adaptor.stopLeScan(callback);
            }
        }
        else
            P_Bridge_Internal.logE(m_bleManager, "Tried to stop scan (if it's even running), but the Bluetooth Adaptor is null!");
    }

    @Override
    public void stopPendingIntentScan(PendingIntent pendingIntent)
    {
        if (m_adaptor != null)
        {
            if (!O_Util.stopScan(m_adaptor, pendingIntent))
            {
                m_bleManager.ASSERT(false, "Something went wrong when trying to stop the PendingIntent scan!");
            }
        }
        else
            P_Bridge_Internal.logE(m_bleManager, "Tried to stop scan (if it's even running), but the Bluetooth Adaptor is null!");
    }
}
