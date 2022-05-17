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


import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.NativeScanFilter;

import java.util.List;


@TargetApi(Build.VERSION_CODES.M)
public class M_Util
{

    private M_Util() {}

    public static boolean shouldShowRequestPermissionRationale(Activity context) {
        return context.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    public static void requestPermissions(Activity context, int requestCode) {
        context.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, requestCode);
    }

    public static void startNativeScan(BluetoothAdapter adapter, int scanMode, int matchMode, int machNum, Interval scanReportDelay, List<NativeScanFilter> filterList, L_Util.ScanCallback listener) {
        final ScanSettings.Builder builder = L_Util.buildSettings(adapter, scanMode, scanReportDelay);

        builder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
        builder.setNumOfMatches(machNum);
        builder.setMatchMode(matchMode);

        final ScanSettings scanSettings = builder.build();

        L_Util.startScan(adapter, scanSettings, filterList, listener);
    }

    @SuppressLint("MissingPermission")
    public static BluetoothGatt connect(BluetoothDevice device, boolean autoConnect, Context context, BluetoothGattCallback callback)
    {
        return device.connectGatt(context, autoConnect, callback, BluetoothDevice.TRANSPORT_LE);
    }

}
