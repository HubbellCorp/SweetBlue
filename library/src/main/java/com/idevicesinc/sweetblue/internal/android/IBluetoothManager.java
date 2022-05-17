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


import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Context;

import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.compat.L_Util;
import com.idevicesinc.sweetblue.internal.IBleDevice;
import com.idevicesinc.sweetblue.internal.IBleManager;
import com.idevicesinc.sweetblue.utils.Interval;
import java.util.Set;

/**
 * Interface used to abstract away the android classes {@link BluetoothManager} and {@link BluetoothAdapter}.
 *
 * @see AndroidBluetoothManager for the default implementation
 */
public interface IBluetoothManager
{

    /**
     * This gets called when the {@link IBleManager} calls it's initConfigDependentMembers method. This is called
     * upon first instantiation of the manager class, and whenever {@link com.idevicesinc.sweetblue.BleManager#setConfig(BleManagerConfig)} is
     * called.
     */
    void setIBleManager(IBleManager mgr);
    int getConnectionState(IBluetoothDevice device, int profile);
    boolean startDiscovery();
    boolean cancelDiscovery();
    boolean isManagerNull();
    boolean disable();
    boolean enable();
    boolean isMultipleAdvertisementSupported();
    boolean isBluetooth5LongRangeSupported();
    boolean isBluetooth5HighSpeedSupported();
    void resetManager(Context context);
    int getState();
    int getBleState();
    String getAddress();
    String getName();
    boolean setName(String name);
    Set<P_DeviceHolder> getBondedDevices();
    BluetoothAdapter getNativeAdaptor();
    BluetoothManager getNativeManager();
    P_ServerHolder openGattServer(Context context, IServerListener listener);
    void startAdvertising(AdvertiseSettings settings, AdvertiseData adData, L_Util.AdvertisingCallback callback);
    void stopAdvertising();
    boolean isLocationEnabledForScanning_byOsServices();
    boolean isLocationEnabledForScanning_byRuntimePermissions();
    boolean isLocationEnabledForScanning();
    boolean isBluetoothEnabled();
    void startLScan(int scanMode, Interval delay, L_Util.ScanCallback callback);
    void startMScan(int scanMode, int matchMode, int matchNum, Interval delay, L_Util.ScanCallback callback);
    boolean startLeScan(BluetoothAdapter.LeScanCallback callback);
    void stopLeScan(BluetoothAdapter.LeScanCallback callback);
    void stopPendingIntentScan(PendingIntent pendingIntent);
    boolean startPendingIntentScan(int scanMode, int matchMode, int matchNum, Interval delay, PendingIntent pendingIntent);
    BluetoothDevice getRemoteDevice(String macAddress);

    /**
     * Interface used by the library to instantiate a new instance of {@link IBluetoothManager}
     */
    interface Factory
    {
        IBluetoothManager newInstance();
    }

    /**
     * An instance of {@link DefaultFactory} used by the library, unless {@link com.idevicesinc.sweetblue.BleManagerConfig#bluetoothManagerImplementation} is changed.
     */
    Factory DEFAULT_FACTORY = new DefaultFactory();

    /**
     * Default instance of {@link IBluetoothManager} used by the library, unless {@link com.idevicesinc.sweetblue.BleManagerConfig#bluetoothManagerImplementation} is changed.
     */
    IBluetoothManager DEFAULT_INSTANCE = DEFAULT_FACTORY.newInstance();

    /**
     * Default implementation of {@link Factory}.
     */
    class DefaultFactory implements Factory
    {
        @Override
        public IBluetoothManager newInstance()
        {
            return new AndroidBluetoothManager();
        }
    }

}
