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


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.CallSuper;

import com.idevicesinc.sweetblue.internal.IBleDevice;
import com.idevicesinc.sweetblue.internal.P_Bridge_BleManager;
import com.idevicesinc.sweetblue.internal.android.IBluetoothDevice;
import com.idevicesinc.sweetblue.internal.android.P_DeviceHolder;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Util_Unit;


/**
 * Base class used to mock a bluetooth device (at the android layer). This class is setup to basically always work. Bonding and connecting will always work.
 */
public class UnitTestBluetoothDevice implements IBluetoothDevice
{


    private String m_address;
    private BleDevice m_device;
    private int m_bondState = BleStatuses.DEVICE_BOND_UNBONDED;
    private IBleDevice m_iBleDevice;


    public UnitTestBluetoothDevice(IBleDevice device)
    {
        m_iBleDevice = device;
        if (m_iBleDevice != null && !m_iBleDevice.isNull())
            m_address = device.getMacAddress();
    }

    /**
     * This method is called internally. You should have no reason to override this method. If you do, this class probably won't work right, unless you remember
     * to call super.init().
     */
    @CallSuper
    @Override
    public void init()
    {
        m_device = m_iBleDevice.getBleDevice();
    }

    /**
     * Internal method used to set the "native" device.
     */
    @Override
    public final void setNativeDevice(BluetoothDevice device, P_DeviceHolder deviceHolder)
    {
        m_address = deviceHolder.getAddress();
    }

    /**
     * Returns the current bond state.
     */
    @Override
    public int getBondState()
    {
        return m_bondState;
    }

    /**
     * Returns the mac address for this device
     */
    @Override
    public String getAddress()
    {
        if (TextUtils.isEmpty(m_address))
            m_address = Util_Unit.randomMacAddress();
        return m_address;
    }

    /**
     * Returns the name of the device
     */
    @Override
    public String getName()
    {
        return m_iBleDevice != null ? m_iBleDevice.getName_native() : "";
    }

    /**
     * Creates a bond with the device. This calls {@link #setToBonding()}, and {@link #setToBonded()}, in addition to updating
     * the bonded device list in the manager layer. You probably shouldn't override this method, but the setTo methods instead.
     */
    @Override
    public boolean createBond()
    {
        P_Bridge_BleManager.postUpdateDelayed(m_device.getIBleDevice().getIManager(), this::setToBonding, 100);
        P_Bridge_BleManager.postUpdateDelayed(m_device.getIBleDevice().getIManager(), this::setToBonded, 250);
        return true;
    }

    /**
     * Called by {@link #createBond()}, and {@link #createBondSneaky(String, boolean)}. This updates the device's bond state (to BONDING), and posts
     * the success as a "native" callback.
     */
    public void setToBonding()
    {
        m_bondState = BleStatuses.DEVICE_BOND_BONDING;
        Util_Native.setBondStatusSuccess(m_device, BleStatuses.DEVICE_BOND_BONDING, Interval.ZERO);
    }

    /**
     * Called by {@link #createBond()}, and {@link #createBondSneaky(String, boolean)}. This updates the device's bond state (to BONDED), and posts
     * the success as a "native" callback.
     */
    public void setToBonded()
    {
        m_bondState = BleStatuses.DEVICE_BOND_BONDED;
        ((UnitTestBluetoothManager) m_device.getManager().getIBleManager().managerLayer()).addBondedDevice(m_device);
        Util_Native.bondSuccess(m_device, Interval.ZERO);
    }

    /**
     * This is used internally as a "hack" fix.
     */
    @Override
    public boolean isConnected()
    {
        return m_device.is(BleDeviceState.BLE_CONNECTED);
    }

    /**
     * Returns <code>true</code> if the held device is null. By default, this just returns <code>false</code> as its assumed to be a valid device.
     */
    @Override
    public boolean isDeviceNull()
    {
        return false;
    }

    /**
     * Removes the bond to this device.
     */
    @Override
    public boolean removeBond()
    {
        P_Bridge_BleManager.postUpdateDelayed(m_device.getIBleDevice().getIManager(), this::setToUnBonded, 250);
        return true;
    }

    /**
     * Called by {@link #createBond()}, and {@link #createBondSneaky(String, boolean)}. This updates the device's bond state (to BONDED), and posts
     * the success as a "native" callback.
     */
    public void setToUnBonded()
    {
        ((UnitTestBluetoothManager) m_device.getManager().getIBleManager().managerLayer()).removeBondedDevice(m_device);
        m_bondState = BleStatuses.DEVICE_BOND_UNBONDED;
        Util_Native.unbondSuccess(getBleDevice(), Util_Unit.getRandomTime());
    }

    /**
     * Cancels an active bond attempt.
     */
    @Override
    public boolean cancelBond()
    {
        P_Bridge_BleManager.postUpdateDelayed(m_device.getIBleDevice().getIManager(), this::setToUnBonded, 250);
        return true;
    }

    /**
     * Referential equality check.
     */
    @Override
    public boolean equals(IBluetoothDevice device)
    {
        return device == this;
    }

    /**
     * This method is effectively the same as {@link #createBond()} when running in a unit test.
     */
    @Override
    public boolean createBondSneaky(String methodName, boolean loggingEnabled)
    {
        P_Bridge_BleManager.postUpdateDelayed(m_device.getIBleDevice().getIManager(), this::setToBonding, 100);
        P_Bridge_BleManager.postUpdateDelayed(m_device.getIBleDevice().getIManager(), this::setToBonded, 250);
        ((UnitTestBluetoothManager) m_device.getManager().getIBleManager().managerLayer()).addBondedDevice(m_device);
        return true;
    }

    /**
     * This will always return <code>null</code>, as when unit testing, we don't have any actual native devices.
     */
    @Override
    public BluetoothDevice getNativeDevice()
    {
        return null;
    }

    /**
     * This always returns <code>null</code>. This method also does nothing, as the connection logic is mainly handled
     * in {@link UnitTestBluetoothGatt}.
     */
    @Override
    public BluetoothGatt connect(Context context, boolean useAutoConnect, BluetoothGattCallback callback)
    {
        return null;
    }

    /**
     * Method called internally to update the instance of {@link IBleDevice} held by this class.
     */
    @Override
    public final void updateBleDevice(IBleDevice device)
    {
        m_device = device.getBleDevice();
    }

    /**
     * Returns the {@link BleDevice} instance held by this class.
     */
    @Override
    public final BleDevice getBleDevice()
    {
        return m_device;
    }

}
