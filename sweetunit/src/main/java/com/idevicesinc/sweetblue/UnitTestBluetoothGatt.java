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
import com.idevicesinc.sweetblue.internal.IBleDevice;
import com.idevicesinc.sweetblue.internal.P_Bridge_BleDevice;
import com.idevicesinc.sweetblue.internal.P_Bridge_BleManager;
import com.idevicesinc.sweetblue.internal.android.IBluetoothGatt;
import com.idevicesinc.sweetblue.internal.android.IBluetoothDevice;
import com.idevicesinc.sweetblue.internal.android.P_GattHolder;
import com.idevicesinc.sweetblue.utils.GattDatabase;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.LogFunction;
import com.idevicesinc.sweetblue.utils.P_Const;
import com.idevicesinc.sweetblue.utils.Phy;
import com.idevicesinc.sweetblue.utils.Util_Unit;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Base class used to mock a bluetooth gatt instance (at the android layer). This class is setup to always work.
 */
public class UnitTestBluetoothGatt implements IBluetoothGatt
{


    public static int MIN_DELAY_TIME = 1;
    public static int MAX_DELAY_TIME = 500;

    private Interval m_delayTime;
    private boolean m_gattIsNull = true;
    private final IBleDevice m_device;
    private final BleDevice m_userDevice;
    private boolean m_explicitDisconnect = false;

    private List<BleService> m_services;


    /**
     * Default constructor to use when not providing a {@link GattDatabase} instance.
     */
    public UnitTestBluetoothGatt(IBleDevice device)
    {
        m_device = device;
        m_userDevice = m_device.getIManager().getBleDevice(m_device);
    }

    /**
     * Default constructor to use when providing a {@link GattDatabase} instance.
     */
    public UnitTestBluetoothGatt(IBleDevice device, GattDatabase gattDb)
    {
        this(device);
        if (gattDb != null)
            m_services = gattDb.getNativeBleServiceList();
    }

    /**
     * Set the {@link GattDatabase} instance this gatt instance will use.
     */
    public void setDatabase(GattDatabase database)
    {
        if (database != null)
            m_services = database.getNativeBleServiceList();
    }

    /**
     * Internal method used by the library. Being that there is no actual gatt instance when unit testing, this method does nothing.
     */
    @Override
    public final void setGatt(BluetoothGatt gatt)
    {

    }

    /**
     * Another internal method that does nothing when unit testing.
     */
    @Override
    public final UhOhListener.UhOh closeGatt()
    {
        return null;
    }

    /**
     * Internal method which does nothing, and has no value when unit testing.
     */
    @Override
    public BluetoothGatt getGatt()
    {
        return null;
    }

    @Override
    public Boolean getAuthRetryValue()
    {
        return true;
    }

    @Override
    public boolean equals(P_GattHolder gatt)
    {
        return false;
    }

    /**
     * Returns a List of {@link BleService}s that represent the backing gatt database. This is managed for you, so there should be no reason
     * to override this method.
     */
    @Override
    public List<BleService> getNativeServiceList(LogFunction logger)
    {
        return m_services == null ? P_Const.EMPTY_BLESERVICE_LIST : m_services;
    }

    /**
     * Method used internally to retrieve a {@link BleService} instance. This is managed for you, so there should be no reason to override this
     * method.
     */
    @Override
    public BleService getBleService(UUID serviceUuid, LogFunction logger)
    {
        if (m_services != null && m_services.size() > 0)
        {
            for (BleService service : m_services)
            {
                if (service.getUuid().equals(serviceUuid))
                    return service;
            }
        }
        return BleService.NULL;
    }

    /**
     * Returns <code>true</code> if the gatt instance is "null". This is all faked, as we will never have a valid gatt instance during unit testing.
     */
    @Override
    public boolean isGattNull()
    {
        return m_gattIsNull;
    }

    /**
     * Connects to the device. This method ends up calling {@link #setToConnecting()}, and {@link #setToConnected()}. You should override
     * those methods, instead of this one.
     *
     * @see #setToConnecting()
     * @see #setToConnected()
     */
    @Override
    public void connect(IBluetoothDevice device, Context context, boolean useAutoConnect, BluetoothGattCallback callback)
    {
        m_gattIsNull = false;
        m_explicitDisconnect = false;
        P_Bridge_BleManager.postUpdateDelayed(m_device.getIManager(), () -> {

            if (!m_explicitDisconnect)
                setToConnecting();

        }, 100);
        P_Bridge_BleManager.postUpdateDelayed(m_device.getIManager(), () -> {

            if (!m_explicitDisconnect)
                setToConnected();

        }, 250);
        device.connect(context, useAutoConnect, callback);
    }

    /**
     * Sets the gatt instance to be <code>null</code>
     */
    public void setGattNull(boolean isNull)
    {
        m_gattIsNull = isNull;
    }

    /**
     * This sets the device state to CONNECTING, and posts the "native" callback
     */
    public void setToConnecting()
    {
        Util_Native.setToConnecting(m_userDevice, BleStatuses.GATT_SUCCESS, true, Interval.millis(0));
    }

    /**
     * This sets the device state to CONNECTED, and posts the "native" callback
     */
    public void setToConnected()
    {
        Util_Native.setToConnected(m_userDevice, BleStatuses.GATT_SUCCESS, Interval.millis(0));
    }

    /**
     * Disconnects the device. Sets the gatt to null, and sets the state to DISCONNECTED. This calls
     * {@link #preDisconnect()} to set the actual state.
     *
     * @see #preDisconnect()
     */
    @Override
    public void disconnect()
    {
        m_gattIsNull = true;
        m_explicitDisconnect = true;
        preDisconnect();
    }

    /**
     * Sends the "native" callback to set the state to DISCONNECTED
     */
    public void preDisconnect()
    {
        Util_Native.setToDisconnected(m_userDevice, BleStatuses.GATT_SUCCESS);
    }

    /**
     * Called when the system wants to request a new MTU size.
     */
    @Override
    public boolean requestMtu(int mtu)
    {
        Util_Native.requestMTUSuccess(m_userDevice, mtu, getDelayTime());
        return true;
    }

    /**
     * This method doesn't really make any sense in a unit testing environment as everything is mocked.
     */
    @Override
    public boolean refreshGatt()
    {
        return true;
    }

    /**
     * Sets the PHY of the device.
     */
    @Override
    public boolean setPhy(Phy options)
    {
        Util_Native.setPhySuccess(m_userDevice, options, getDelayTime());
        return true;
    }

    /**
     * Read the PHY of the device.
     */
    @Override
    public boolean readPhy()
    {
        Util_Native.readPhySuccess(m_userDevice, m_device.getPhy_private(), getDelayTime());
        return true;
    }

    /**
     * Called when the system is trying to read a characteristic. This method calls {@link #sendReadResponse(BleCharacteristic, byte[])}, which is the method
     * you should override to change behavior.
     *
     * @see #sendReadResponse(BleCharacteristic, byte[])
     */
    @Override
    public boolean readCharacteristic(BleCharacteristic characteristic)
    {
        sendReadResponse(characteristic, characteristic.getValue());
        return true;
    }

    /**
     * Called by {@link #readCharacteristic(BleCharacteristic)} to send the response of the read.
     */
    public void sendReadResponse(BleCharacteristic characteristic, byte[] data)
    {
        Util_Native.readSuccess(m_userDevice, characteristic, data, getDelayTime());
    }

    /**
     * Used internally when writing to a characteristic. You shouldn't need to override this method.
     */
    @Override
    public boolean setCharValue(BleCharacteristic characteristic, byte[] data)
    {
        characteristic.setValue(data);
        return true;
    }

    /**
     * Called when the library tries to write a characteristic. This calls {@link #sendWriteResponse(BleCharacteristic)}, which is the method to overload to
     * change behavior.
     */
    @Override
    public boolean writeCharacteristic(BleCharacteristic characteristic)
    {
        sendWriteResponse(characteristic);
        return true;
    }

    /**
     * Sends the response of writing to a characteristic.
     */
    public void sendWriteResponse(BleCharacteristic characteristic)
    {
        Util_Native.writeSuccess(m_userDevice, characteristic, getDelayTime());
    }

    /**
     * Called internally when enabling/disabling a notification on a characteristic. This method doesn't really do anything.
     */
    @Override
    public boolean setCharacteristicNotification(BleCharacteristic characteristic, boolean enable)
    {
        return true;
    }

    /**
     * Called by the system when trying to read a descriptor. This calls {@link #sendReadDescriptorResponse(BleDescriptor, byte[])}, which is the method
     * you should override to change behavior.
     */
    @Override
    public boolean readDescriptor(BleDescriptor descriptor)
    {
        sendReadDescriptorResponse(descriptor, descriptor.getValue());
        return true;
    }

    /**
     * Called by {@link #readDescriptor(BleDescriptor)} to send the response of the read back through the "native" callback.
     */
    public void sendReadDescriptorResponse(BleDescriptor descriptor, byte[] data)
    {
        Util_Native.readDescSuccess(m_userDevice, descriptor, data, getDelayTime());
    }

    /**
     * Called internally when trying to write to a descriptor. You should have no reason to override this method.
     */
    @Override
    public boolean setDescValue(BleDescriptor descriptor, byte[] data)
    {
        descriptor.setValue(data);
        return true;
    }

    /**
     * Called when the system tries to write to a descriptor. This calls {@link #sendWriteDescResponse(BleDescriptor)}, which is the method to
     * override if you wish to change behavior.
     */
    @Override
    public boolean writeDescriptor(BleDescriptor descriptor)
    {
        sendWriteDescResponse(descriptor);
        return true;
    }

    /**
     * Called by {@link #writeDescriptor(BleDescriptor)} to send the response of writing to the descriptor to the native callback.
     */
    public void sendWriteDescResponse(BleDescriptor descriptor)
    {
        Util_Native.writeDescSuccess(m_userDevice, descriptor, getDelayTime());
    }

    /**
     * Called by the system when trying to request a change in connection priority. There isn't much to do in this method when unit testing.
     */
    @Override
    public boolean requestConnectionPriority(BleConnectionPriority priority)
    {
        return true;
    }

    /**
     * Called by the system when trying to discover it's services (gatt database). This just calls {@link #setServicesDiscovered()}, which is the
     * method to override to change behavior.
     */
    @Override
    public boolean discoverServices()
    {
        P_Bridge_BleManager.postUpdateDelayed(m_device.getIManager(), () -> {

            if (!m_explicitDisconnect)
                setServicesDiscovered();

        }, getDelayTime().millis());
        return true;
    }

    /**
     * Posts the response of discovering services to the native callback.
     */
    public void setServicesDiscovered()
    {
        Util_Native.succeedDiscoverServices(m_userDevice);
    }

    /**
     * Not hooked up in this class.
     */
    @Override
    public boolean executeReliableWrite()
    {
        return true;
    }

    /**
     * Not hooked up in this class.
     */
    @Override
    public boolean beginReliableWrite()
    {
        return true;
    }

    /**
     * Not hooked up in this class.
     */
    @Override
    public void abortReliableWrite(BluetoothDevice device)
    {

    }

    /**
     * Called by the system when trying to read the rssi from the device.
     */
    @Override
    public boolean readRemoteRssi()
    {
        P_Bridge_BleManager.postUpdateDelayed(m_device.getIManager(), () ->
                        P_Bridge_BleDevice.onReadRemoteRssi(m_device, null, getRssiValue(), BleStatuses.GATT_SUCCESS)
                , getDelayTime().millis());
        return true;
    }

    /**
     * Returns the {@link BleDevice} instance held in this class.
     */
    @Override
    public final BleDevice getBleDevice()
    {
        return m_userDevice;
    }

    /**
     * Sets the amount of time to delay each native callback (to simulate a real world scenario where callbacks aren't instant)
     */
    public void setDelayTime(Interval delay)
    {
        m_delayTime = delay;
    }

    /**
     * Returns the delay set via {@link #setDelayTime(Interval)}, or a random time range from {@link #MIN_DELAY_TIME} to {@link #MAX_DELAY_TIME}.
     */
    public Interval getDelayTime()
    {
        if (Interval.isDisabled(m_delayTime))
            return Util_Unit.getRandomTime(MIN_DELAY_TIME, MAX_DELAY_TIME);
        else
            return m_delayTime;
    }

    /**
     * Returns a random rssi value based off {@link BleManagerConfig#rssi_min} and {@link BleManagerConfig#rssi_max}.
     */
    public int getRssiValue()
    {
        int diff = Math.abs(m_device.conf_mngr().rssi_min) - Math.abs(m_device.conf_mngr().rssi_max);
        Random r = new Random();
        return -(r.nextInt(diff) + Math.abs(m_device.conf_mngr().rssi_max));
    }
}
