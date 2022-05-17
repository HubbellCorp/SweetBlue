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

package com.idevicesinc.sweetblue;

import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Context;
import android.text.TextUtils;
import com.idevicesinc.sweetblue.compat.L_Util;
import com.idevicesinc.sweetblue.internal.IBleDevice;
import com.idevicesinc.sweetblue.internal.IBleManager;
import com.idevicesinc.sweetblue.internal.P_Bridge_BleManager;
import com.idevicesinc.sweetblue.internal.android.IServerListener;
import com.idevicesinc.sweetblue.internal.android.IBluetoothDevice;
import com.idevicesinc.sweetblue.internal.android.IBluetoothManager;
import com.idevicesinc.sweetblue.internal.android.P_DeviceHolder;
import com.idevicesinc.sweetblue.internal.android.P_ServerHolder;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Util_Unit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * Base class used to mock the bluetooth manager instance (at the android layer). This class is setup to always work.
 */
public class UnitTestBluetoothManager implements IBluetoothManager
{

    private int m_nativeState = BleStatuses.STATE_ON;
    private String m_address;
    private String m_name = "MockedDevice";
    private Map<String, Integer> deviceStates = new HashMap<>();
    private Set<P_DeviceHolder> bondedDevices = new HashSet<>();
    private IBleManager m_manager;
    private BleManager m_userManager;


    /**
     * Called internally to set the {@link IBleManager} instance. There should be no reason to override this method.
     */
    @Override
    public void setIBleManager(IBleManager mgr)
    {
        m_manager = mgr;
        m_userManager = BleManager.s_instance;
    }

    /**
     * Returns the connection state of this device. You shouldn't need to override this method.
     */
    @Override
    public int getConnectionState(IBluetoothDevice device, int profile)
    {
        Integer state = deviceStates.get(device.getAddress());
        return state == null ? 0 : state;
    }

    /**
     * Called by the library when it wants to start bluetooth classic discovery. This does nothing in unit testing.
     */
    @Override
    public boolean startDiscovery()
    {
        return true;
    }

    /**
     * Called internally when the system wants to stop a classic scan.
     */
    @Override
    public boolean cancelDiscovery()
    {
        P_Bridge_BleManager.onClassicDiscoveryFinished(getManager());
        return true;
    }

    /**
     * Returns <code>true</code> if the manager instance is "null" (this is all just mocked)
     */
    @Override
    public boolean isManagerNull()
    {
        return false;
    }

    /**
     * Called internally to update the state of a {@link BleDevice}.
     */
    public void updateDeviceState(IBleDevice device, int state)
    {
        deviceStates.put(device.getMacAddress(), state);
    }

    /**
     * Called by the library when attempting to turn off the Bluetooth radio. This calls {@link #setToTurningOff()} and {@link #setToOff()}, which are
     * the methods you should override to change behavior.
     */
    @Override
    public boolean disable()
    {
        if (BleManager.s_instance != null)
        {
            P_Bridge_BleManager.postUpdateDelayed(getManager(), this::setToTurningOff, 50);
            P_Bridge_BleManager.postUpdateDelayed(getManager(), this::setToOff, 150);
        }
        else
            m_nativeState = BleStatuses.STATE_OFF;
        return true;
    }

    /**
     * Called by the system when the library wants to turn the Bluetooth radio on. This calls {@link #setToTurningOn()} and {@link #setToOn()},
     * which are the methods you should override to change behavior.
     */
    @Override
    public boolean enable()
    {
        if (BleManager.s_instance != null)
        {
            P_Bridge_BleManager.postUpdateDelayed(getManager(), this::setToTurningOn, 50);
            P_Bridge_BleManager.postUpdateDelayed(getManager(), this::setToOn, 150);
        }
        else
            m_nativeState = BleStatuses.STATE_ON;
        return true;
    }

    /**
     * Returns <code>true</code> if multiple advertisement is supported (using an android device to advertise to other bluetooth controllers).
     * Default is <code>true</code>
     */
    @Override
    public boolean isMultipleAdvertisementSupported()
    {
        return true;
    }

    /**
     * Returns <code>true</code> if bluetooth 5 long range is supported.
     * Default is <code>true</code>
     */
    @Override
    public boolean isBluetooth5LongRangeSupported()
    {
        return true;
    }

    /**
     * Returns <code>true</code> if bluetooth 5 high speed is supported.
     * Default is <code>true</code>
     */
    @Override
    public boolean isBluetooth5HighSpeedSupported()
    {
        return true;
    }

    /**
     * Called by the library when it needs to instantiate the bluetooth manager. As in unit tests this is all
     * mocked out, this method doesn't do anything.
     */
    @Override
    public void resetManager(Context context)
    {
    }

    /**
     * Returns the native state of the Bluetooth Adaptor
     */
    @Override
    public int getState()
    {
        return m_nativeState;
    }

    /**
     * Returns the same as {@link #getState()}
     */
    @Override
    public int getBleState()
    {
        return m_nativeState;
    }

    /**
     * Returns the name of this manager/adaptor. This is the name shown if you want to use BleServer to advertise a device.
     */
    @Override
    public String getName()
    {
        return m_name;
    }

    /**
     * Sets the name of the bluetooth manager/adaptor.
     */
    @Override
    public boolean setName(String name)
    {
        m_name = name;
        return true;
    }

    /**
     * Returns the mac address of the "controller" (the phone/tablet).
     */
    @Override
    public String getAddress()
    {
        if (TextUtils.isEmpty(m_address))
            m_address = Util_Unit.randomMacAddress();
        return m_address;
    }

    /**
     * Returns a {@link Set} of {@link P_DeviceHolder} representing the devices that are currently bonded to this manager/adaptor.
     */
    @Override
    public Set<P_DeviceHolder> getBondedDevices()
    {
        return bondedDevices;
    }

    /**
     * Called when the system wants to track a new device as being bonded.
     */
    public void addBondedDevice(BleDevice device)
    {
        bondedDevices.add(P_DeviceHolder.newHolder(device.getNative(), device.getMacAddress()));
    }

    /**
     * Called when a device is no longer bonded.
     */
    public void removeBondedDevice(BleDevice device)
    {
        bondedDevices.remove(P_DeviceHolder.newHolder(device.getNative(), device.getMacAddress()));
    }

    /**
     * Returns <code>null</code> as the adaptor is mocked.
     */
    @Override
    public BluetoothAdapter getNativeAdaptor()
    {
        return null;
    }

    /**
     * Returns <code>null</code> as the manager is mocked.
     */
    @Override
    public BluetoothManager getNativeManager()
    {
        return null;
    }

    /**
     * Called by the library when a {@link BleServer} is getting created.
     */
    @Override
    public P_ServerHolder openGattServer(Context context, IServerListener listeners)
    {
        return P_ServerHolder.NULL;
    }

    /**
     * Called by the system when the library wants the manager/adaptor to advertise over bluetooth.
     */
    @Override
    public void startAdvertising(AdvertiseSettings settings, AdvertiseData adData, L_Util.AdvertisingCallback callback)
    {
        Util_Native.setToAdvertising(m_userManager, settings, callback);
    }

    /**
     * Called by the library when it wants to stop the {@link BleServer} from advertising.
     */
    @Override
    public void stopAdvertising()
    {
    }

    /**
     * Returns <code>true</code> if location services are supported by the OS.
     * Default is <code>true</code>
     */
    @Override
    public boolean isLocationEnabledForScanning_byOsServices()
    {
        return true;
    }

    /**
     * Returns <code>true</code> if location service permissions have been granted.
     * Default is <code>true</code>
     */
    @Override
    public boolean isLocationEnabledForScanning_byRuntimePermissions()
    {
        return true;
    }

    /**
     * Returns <code>true</code> if location services have been enabled.
     * Default is <code>true</code>
     */
    @Override
    public boolean isLocationEnabledForScanning()
    {
        return true;
    }

    /**
     * Returns <code>true</code> if the bluetooth radio is on.
     */
    @Override
    public boolean isBluetoothEnabled()
    {
        return m_nativeState == BleStatuses.STATE_ON;
    }

    /**
     * Called when the library wants to start a post-lollipop scan (Lollipop only). This method does nothing.
     */
    @Override
    public void startLScan(int scanMode, Interval delay, L_Util.ScanCallback callback)
    {
    }

    /**
     * Called when the library wants to start a post-lollipop scan on marshmallow or above. This method does nothing.
     */
    @Override
    public void startMScan(int scanMode, int matchMode, int matchNum, Interval delay, L_Util.ScanCallback callback)
    {
    }

    /**
     * Called when the library wants to start a pre-lollipop scan. This method does nothing other than returning
     * <code>true</code>.
     */
    @Override
    public boolean startLeScan(BluetoothAdapter.LeScanCallback callback)
    {
        return true;
    }

    /**
     * Called when the system wants to stop an LE scan.
     */
    @Override
    public void stopLeScan(BluetoothAdapter.LeScanCallback callback)
    {
    }

    /**
     * Called when the system wants to stop a PendingIntent scan. This only applies for devices on Oreo or above.
     */
    @Override
    public void stopPendingIntentScan(PendingIntent pendingIntent)
    {
    }

    /**
     * Called when the library wants to start a BLE scan using a PendingIntent to deliver scan results. This method
     * does nothing other than returning <code>true</code>.
     */
    @Override
    public boolean startPendingIntentScan(int scanMode, int matchMode, int matchNum, Interval delay, PendingIntent pendingIntent)
    {
        return true;
    }

    /**
     * Default returns <code>null</code>.
     */
    @Override
    public BluetoothDevice getRemoteDevice(String macAddress)
    {
        return null;
    }

    /**
     * Allows you to manually set the native state of this manager/adaptor.
     */
    protected void manuallySetState(int newState)
    {
        m_nativeState = newState;
    }

    /**
     * Called by {@link #disable()}. This sets the state to TURNING_OFF.
     */
    protected void setToTurningOff()
    {
        Util_Native.sendBluetoothStateChange(m_userManager, m_nativeState, BluetoothAdapter.STATE_TURNING_OFF);
        m_nativeState = BluetoothAdapter.STATE_TURNING_OFF;
    }

    /**
     * Called by {@link #disable()}. This sets the state to OFF.
     */
    protected void setToOff()
    {
        Util_Native.sendBluetoothStateChange(m_userManager, m_nativeState, BluetoothAdapter.STATE_OFF);
        m_nativeState = BluetoothAdapter.STATE_OFF;
    }

    /**
     * Called by {@link #enable()}. This sets the state to TURNING_ON.
     */
    protected void setToTurningOn()
    {
        Util_Native.sendBluetoothStateChange(m_userManager, m_nativeState, BluetoothAdapter.STATE_TURNING_ON);
        m_nativeState = BluetoothAdapter.STATE_TURNING_ON;
    }

    /**
     * Called by {@link #enable()}. This sets the state to ON.
     */
    protected void setToOn()
    {
        Util_Native.sendBluetoothStateChange(m_userManager, m_nativeState, BluetoothAdapter.STATE_ON);
        m_nativeState = BluetoothAdapter.STATE_ON;
    }


    private IBleManager getManager()
    {
        return m_manager;
    }
}
