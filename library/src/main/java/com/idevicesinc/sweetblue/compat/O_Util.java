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

package com.idevicesinc.sweetblue.compat;


import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.NativeScanFilter;
import java.util.List;


@TargetApi(Build.VERSION_CODES.O)
public class O_Util
{


    public static Intent registerReceiver(Context context, BroadcastReceiver broadcastReceiver, IntentFilter intentFilter, int rec)
    {
        return context.registerReceiver(broadcastReceiver, intentFilter, rec);
    }

    public static boolean isHighSpeedSupported(BluetoothAdapter adapter)
    {
        if (adapter != null)
            return adapter.isLe2MPhySupported();

        return false;
    }

    public static boolean isLongRangeSupported(BluetoothAdapter adapter)
    {
        if (adapter != null)
            return adapter.isLeCodedPhySupported();

        return false;
    }

    @SuppressLint("MissingPermission")
    public static void setPhyLayer(BluetoothGatt gatt, int txPhy, int rxPhy, int phyOptions)
    {
        if (gatt != null)
            gatt.setPreferredPhy(txPhy, rxPhy, phyOptions);
    }

    @SuppressLint("MissingPermission")
    public static void readPhyLayer(BluetoothGatt gatt)
    {
        if (gatt != null)
            gatt.readPhy();
    }

    @SuppressLint("MissingPermission")
    public static boolean startScan(BluetoothAdapter adapter, int scanMode, int matchMode, int matchNum, Interval scanReportDelay, List<NativeScanFilter> filterList, PendingIntent pendingIntent)
    {
        final ScanSettings settings = L_Util.buildSettings(adapter, scanMode, scanReportDelay)
                .setMatchMode(matchMode)
                .setNumOfMatches(matchNum)
                .build();

        // Add a last ditch check to make sure the adapter isn't null before trying to start the scan.
        // We check in the task, but by the time we reach this method, it could have been shut off
        // Either the adapter, or the scanner object may be null, so we check it here
        if (adapter == null)
            return false;

        final List<ScanFilter> list = L_Util.convertNativeFilterList(filterList);

        final BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();

        if (scanner == null)
            return false;

        return scanner.startScan(list, settings, pendingIntent) == 0;
    }

    @SuppressLint("MissingPermission")
    public static boolean stopScan(BluetoothAdapter adapter, PendingIntent pendingIntent)
    {
        boolean success = false;
        if (adapter == null)
        {
            Log.e("ScanManager", "Tried to stop the scan, but the Bluetooth Adapter instance was null!");
            // This should be treated as a success for now...it basically means there is no scan if we have no adaptor
            return true;
        }

        final BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();
        if (scanner != null)
        {
            try
            {
                scanner.stopScan(pendingIntent);
                L_Util.flushPendingScanResults(adapter);
                success = true;
            }
            catch (Exception e)
            {
                Log.e("ScanManager", "Exception thrown when attempting to stop the scan. Exception Message: " + e.getMessage());
            }
        }
        else
            Log.w("ScanManager", "Tried to stop the scan, but the BluetoothLeScanner instance was null. This implies the scanning has stopped already.");

        return success;
    }

    public static BluetoothGatt connect(BluetoothDevice device, boolean autoConnect, Context context, BluetoothGattCallback callback)
    {
        return M_Util.connect(device, autoConnect, context, callback);
    }

    @SuppressLint("MissingPermission")
    public static BluetoothGatt connect(BluetoothDevice device, boolean autoConnect, Context context, BluetoothGattCallback callback, Handler handler)
    {
        return device.connectGatt(context, autoConnect, callback, BluetoothDevice.TRANSPORT_LE, BluetoothDevice.PHY_LE_1M_MASK, handler);
    }

}
