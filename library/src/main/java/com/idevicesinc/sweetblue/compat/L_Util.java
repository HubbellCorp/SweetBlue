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
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import com.idevicesinc.sweetblue.BleAdvertisingSettings;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.internal.IBleDevice;
import com.idevicesinc.sweetblue.internal.IBleManager;
import com.idevicesinc.sweetblue.internal.P_Bridge_Internal;
import com.idevicesinc.sweetblue.internal.android.P_DeviceHolder;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.NativeScanFilter;
import com.idevicesinc.sweetblue.utils.Pointer;

import java.util.ArrayList;
import java.util.List;


@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class L_Util
{

    private L_Util()
    {
    }


    private static final String INTENT_EXTRA_SCAN_RESULTS = "android.bluetooth.le.extra.LIST_SCAN_RESULT";


    public interface ScanCallback
    {
        void onScanResult(int callbackType, ScanResult result);

        void onBatchScanResults(List<ScanResult> results);

        void onScanFailed(int errorCode);
    }

    public interface AdvertisingCallback
    {
        void onStartSuccess(BleAdvertisingSettings settings);

        void onStartFailure(int errorCode);
    }

    public final static class ScanResult
    {
        private P_DeviceHolder device;
        private int rssi;
        private byte[] record;


        public ScanResult()
        {
        }

        public ScanResult(P_DeviceHolder device, int rssi, byte[] record)
        {
            this.device = device;
            this.rssi = rssi;
            this.record = record;
        }

        public final P_DeviceHolder getDevice()
        {
            return device;
        }

        public final int getRssi()
        {
            return rssi;
        }

        public final byte[] getRecord()
        {
            return record;
        }
    }

    private static ScanCallback m_UserScanCallback;
    private static AdvertisingCallback m_userAdvCallback;

    private final static android.bluetooth.le.ScanCallback m_callback = new android.bluetooth.le.ScanCallback()
    {
        @Override
        public void onScanResult(int callbackType, android.bluetooth.le.ScanResult result)
        {
            if (m_UserScanCallback != null)
            {
                m_UserScanCallback.onScanResult(callbackType, toLScanResult(result));
            }
        }

        @Override
        public void onBatchScanResults(List<android.bluetooth.le.ScanResult> results)
        {
            if (m_UserScanCallback != null)
            {
                m_UserScanCallback.onBatchScanResults(toLScanResults(results));
            }
        }

        @Override
        public void onScanFailed(int errorCode)
        {
            if (m_UserScanCallback != null)
            {
                m_UserScanCallback.onScanFailed(errorCode);
            }
        }
    };

    private static final AdvertiseCallback m_nativeAdvertiseCallback = new AdvertiseCallback()
    {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect)
        {
            if (m_userAdvCallback != null)
            {
                m_userAdvCallback.onStartSuccess(fromNativeSettings(settingsInEffect));
            }
        }

        @Override
        public void onStartFailure(int errorCode)
        {
            if (m_userAdvCallback != null)
            {
                m_userAdvCallback.onStartFailure(errorCode);
            }
        }
    };

    public static android.bluetooth.le.ScanCallback getNativeScanCallback()
    {
        return m_callback;
    }

    public static AdvertiseCallback getNativeAdvertisingCallback()
    {
        return m_nativeAdvertiseCallback;
    }

    static void setAdvCallback(AdvertisingCallback callback)
    {
        m_userAdvCallback = callback;
    }


    public static BleAdvertisingSettings fromNativeSettings(AdvertiseSettings settings)
    {
        return new BleAdvertisingSettings(BleAdvertisingSettings.BleAdvertisingMode.fromNative(settings.getMode()),
                BleAdvertisingSettings.BleTransmissionPower.fromNative(settings.getTxPowerLevel()),
                Interval.millis(settings.getTimeout()));
    }


    @SuppressLint("MissingPermission")
    public static boolean requestMtu(BluetoothGatt gatt, int mtu)
    {
        return gatt.requestMtu(mtu);
    }

    public static boolean isAdvertisingSupportedByChipset(BluetoothAdapter adapter)
    {
        return Build.PRODUCT.equals("iot_rpi3") || adapter.isMultipleAdvertisementSupported();
    }

    public static BluetoothLeAdvertiser getBluetoothLeAdvertiser(BluetoothAdapter adapter)
    {
        return adapter.getBluetoothLeAdvertiser();
    }

    @SuppressLint("MissingPermission")
    public static void stopNativeScan(BluetoothAdapter adapter)
    {
        if (adapter == null)
        {
            Log.e("ScanManager", "Tried to stop the scan, but the Bluetooth Adapter instance was null!");
            return;
        }

        final BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();
        if (scanner != null)
        {
            try
            {
                // A customer wrote in about this crashing when BlueTooth was turned off and then attempting to stop the scan
                // we weren't able to reproduce the crash so wrapping in a try/catch to be safe
                scanner.stopScan(m_callback);
            }
            catch (Exception e)
            {
                Log.e("ScanManager", "Exception thrown when attempting to stop the scan. Exception Message: " + e.getMessage());
            }
        }
        else
            Log.w("ScanManager", "Tried to stop the scan, but the BluetoothLeScanner instance was null. This implies the scanning has stopped already.");
    }


    @SuppressLint("MissingPermission")
    public static boolean requestConnectionPriority(BluetoothGatt gatt, int mode)
    {
        return gatt.requestConnectionPriority(mode);
    }

    public static void startNativeScan(BluetoothAdapter adapter, int scanMode, Interval scanReportDelay, List<NativeScanFilter> filterList, ScanCallback listener)
    {
        final ScanSettings settings = buildSettings(adapter, scanMode, scanReportDelay).build();

        startScan(adapter, settings, filterList, listener);
    }

    static ScanSettings.Builder buildSettings(BluetoothAdapter adapter, int scanMode, Interval scanReportDelay)
    {
        final ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(scanMode);

        if (adapter.isOffloadedScanBatchingSupported())
        {
            final long scanReportDelay_millis = Interval.isEnabled(scanReportDelay) ? scanReportDelay.millis() : 0;
            builder.setReportDelay(scanReportDelay_millis);
        }
        else
        {
            builder.setReportDelay(0);
        }
        return builder;
    }

    @SuppressLint("MissingPermission")
    static void flushPendingScanResults(BluetoothAdapter adapter)
    {
        if (adapter != null)
        {
            adapter.getBluetoothLeScanner().flushPendingScanResults(m_callback);
        }
    }

    @SuppressLint("MissingPermission")
    static void startScan(BluetoothAdapter adapter, ScanSettings scanSettings, List<NativeScanFilter> filterList, ScanCallback listener)
    {
        m_UserScanCallback = listener;
        // Add a last ditch check to make sure the adapter isn't null before trying to start the scan.
        // We check in the task, but by the time we reach this method, it could have been shut off
        // Either the adapter, or the scanner object may be null, so we check it here
        if (adapter == null || adapter.getBluetoothLeScanner() == null)
        {
            m_callback.onScanFailed(android.bluetooth.le.ScanCallback.SCAN_FAILED_INTERNAL_ERROR);
            return;
        }

        final List<ScanFilter> list = convertNativeFilterList(filterList);

        adapter.getBluetoothLeScanner().startScan(list, scanSettings, m_callback);
    }

    @SuppressLint("MissingPermission")
    public static boolean startAdvertising(BluetoothAdapter adapter, AdvertiseSettings settings, AdvertiseData adData, AdvertisingCallback callback)
    {
        final BluetoothLeAdvertiser adv = adapter.getBluetoothLeAdvertiser();
        if (adv == null)
            return false;

        m_userAdvCallback = callback;
        adv.startAdvertising(settings, adData, m_nativeAdvertiseCallback);
        return true;
    }

    @SuppressLint("MissingPermission")
    public static void stopAdvertising(BluetoothAdapter adapter)
    {
        if (adapter != null)
        {
            final BluetoothLeAdvertiser adv = adapter.getBluetoothLeAdvertiser();
            if (adv != null)
            {
                adv.stopAdvertising(m_nativeAdvertiseCallback);
            }
        }
    }

    public static List<ScanFilter> newEmptyFilterList()
    {
        List<ScanFilter> list = new ArrayList<>();
        list.add(new ScanFilter.Builder().build());
        return list;
    }

    public static ScanResult toLScanResult(P_DeviceHolder device, int rssi, byte[] scanRecord)
    {
        ScanResult res = new ScanResult();
        res.device = device;
        res.rssi = rssi;
        res.record = scanRecord;
        return res;
    }

    public static List<ScanFilter> convertNativeFilterList(List<NativeScanFilter> filters)
    {
        List<ScanFilter> list = new ArrayList<>(filters.size());
        for (NativeScanFilter filter : filters)
        {
            ScanFilter.Builder b = new ScanFilter.Builder();
            if (filter.getServiceUuidMask() != null)
                b.setServiceUuid(filter.getServiceUuid(), filter.getServiceUuidMask());
            else if (filter.getServiceUuid() != null)
                b.setServiceUuid(filter.getServiceUuid());

            if (filter.getServiceDataMask() != null)
                b.setServiceData(filter.getServiceDataUuid(), filter.getServiceData(), filter.getServiceDataMask());
            else if (filter.getServiceData() != null)
                b.setServiceData(filter.getServiceDataUuid(), filter.getServiceData());

            if (filter.getDeviceAddress() != null)
                b.setDeviceAddress(filter.getDeviceAddress());

            if (filter.getDeviceName() != null)
                b.setDeviceName(filter.getDeviceName());

            if (filter.getManufacturerDataMask() != null)
                b.setManufacturerData(filter.getManufacturerId(), filter.getManufacturerData(), filter.getManufacturerDataMask());
            else if (filter.getManufacturerData() != null)
                b.setManufacturerData(filter.getManufacturerId(), filter.getManufacturerData());

            list.add(b.build());
        }

        return list;
    }

    @SuppressLint("MissingPermission")
    public static List<BleDevice> getBleDeviceListFromScanIntent(Intent intentFromScan, IBleManager mgr)
    {
        List<BleDevice> list = new ArrayList<>();
        if (intentFromScan != null && intentFromScan.hasExtra(INTENT_EXTRA_SCAN_RESULTS))
        {
            List<android.bluetooth.le.ScanResult> bundlelist = intentFromScan.getParcelableArrayListExtra(INTENT_EXTRA_SCAN_RESULTS);
            if (bundlelist != null)
            {
                for (android.bluetooth.le.ScanResult result : bundlelist)
                {
                    byte[] scanRecord = result.getScanRecord() != null ? result.getScanRecord().getBytes() : null;
                    IBleDevice idevice = mgr.newDevice(result.getDevice().getAddress(), result.getDevice().getName(), scanRecord, null);
                    idevice.updateRssi(result.getRssi(), true);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        idevice.updateKnownTxPower(result.getTxPower());
                    list.add(P_Bridge_User.newDevice(idevice));
                }
            }
        }
        return list;
    }

    private static ScanResult toLScanResult(android.bluetooth.le.ScanResult result)
    {
        ScanResult res = new ScanResult();
        res.device = P_DeviceHolder.newHolder(result.getDevice());
        res.rssi = result.getRssi();
        res.record = result.getScanRecord().getBytes();
        return res;
    }

    private static List<ScanResult> toLScanResults(List<android.bluetooth.le.ScanResult> results)
    {
        int size = results.size();
        List<ScanResult> res = new ArrayList<>(size);
        for (int i = 0; i < size; i++)
        {
            res.add(toLScanResult(results.get(i)));
        }
        return res;
    }

}
