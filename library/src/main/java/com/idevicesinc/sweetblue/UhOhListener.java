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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;

import com.idevicesinc.sweetblue.utils.GenericListener_Void;
import com.idevicesinc.sweetblue.utils.Phy;
import com.idevicesinc.sweetblue.utils.Utils_String;

/**
 * Provide an implementation to {@link BleManager#setListener_UhOh(UhOhListener)}
 * to receive a callback when an {@link UhOhListener.UhOh} occurs.
 *
 * @see UhOhListener.UhOh
 */
@com.idevicesinc.sweetblue.annotations.Lambda
public interface UhOhListener extends GenericListener_Void<UhOhListener.UhOhEvent>
{
    /**
     * An UhOh is a warning about an exceptional (in the bad sense) and unfixable problem with the underlying stack that
     * the app can warn its user about. It's kind of like an {@link Exception} but they can be so common
     * that using {@link Exception} would render this library unusable without a rat's nest of try/catches.
     * Instead you implement {@link UhOhListener} to receive them. Each {@link UhOhListener.UhOh} has a {@link UhOhListener.UhOh#getRemedy()}
     * that suggests what might be done about it.
     *
     * @see UhOhListener
     * @see BleManager#setListener_UhOh(UhOhListener)
     */
    public static enum UhOh
    {
        /**
         * This happens when you first bond to a peripheral. It seems Android leaves the connection open, but will report it's state as being
         * disconnected. Though if you use a BLE sniffer, you will see that the connection is in fact, still open. Another easier way to check this
         * is to have another phone scanning. It will not see the peripheral, because it's still connected to the first phone.
         *
         * @see Remedy#RECYCLE_CONNECTION
         */
        CONNECTION_STILL_ALIVE,

        /**
         * A {@link BleTask#BOND} operation timed out. This can happen a lot with the Galaxy Tab 4, and doing {@link BleManager#reset()} seems to fix it.
         * SweetBlue does as much as it can to work around the issue that causes bond timeouts, but some might still slip through.
         */
        BOND_TIMED_OUT,

        /**
         * A {@link BleDevice#read(java.util.UUID, ReadWriteListener)}
         * took longer than timeout set by {@link BleDeviceConfig#taskTimeoutRequestFilter}.
         * You will also get a {@link ReadWriteListener.ReadWriteEvent} with {@link ReadWriteListener.Status#TIMED_OUT}
         * but a timeout is a sort of fringe case that should not regularly happen.
         */
        READ_TIMED_OUT,

        /**
         * A {@link BleDevice#read(java.util.UUID, ReadWriteListener)} returned with a <code>null</code>
         * characteristic value. The <code>null</code> value will end up as an empty array in {@link ReadWriteListener.ReadWriteEvent#data}
         * so app-land doesn't have to do any special <code>null</code> handling.
         */
        READ_RETURNED_NULL,

        /**
         * Similar to {@link #READ_TIMED_OUT} but for {@link BleDevice#write(java.util.UUID, byte[])}.
         */
        WRITE_TIMED_OUT,

        /**
         * Similar to {@link #WRITE_TIMED_OUT}, only used to signify when testing a new MTU size, and it times out. This usually means the android device
         * has a bug where it says the MTU size has changed, but can't write the MTU size amount (OnePlus2, Moto X Pure). In this case, SweetBlue will disconnect
         * the device, as no other reads/writes will work once this happens.
         */
        WRITE_MTU_TEST_TIMED_OUT,

        /**
         * When the underlying stack meets a race condition where {@link android.bluetooth.BluetoothAdapter#getState()} does not
         * match the value provided through {@link android.bluetooth.BluetoothAdapter#ACTION_STATE_CHANGED} with {@link android.bluetooth.BluetoothAdapter#EXTRA_STATE}.
         *
         */
        INCONSISTENT_NATIVE_BLE_STATE,

        /**
         * A {@link BleDevice} went from {@link BleDeviceState#BONDING} to {@link BleDeviceState#UNBONDED}.
         * UPDATE: This can happen under normal circumstances, so not listing it as an uh oh for now.
         */
//			WENT_FROM_BONDING_TO_UNBONDED,

        /**
         * A {@link android.bluetooth.BluetoothGatt#discoverServices()} operation returned two duplicate services. Not the same instance
         * necessarily but the same UUID.
         */
        DUPLICATE_SERVICE_FOUND,

        /**
         * A {@link android.bluetooth.BluetoothGatt#discoverServices()} operation returned a service instance that we already received before
         * after disconnecting and reconnecting.
         */
        OLD_DUPLICATE_SERVICE_FOUND,

        /**
         * {@link android.bluetooth.BluetoothAdapter#startLeScan(BluetoothAdapter.LeScanCallback)} failed for an unknown reason. The library is now using
         * {@link android.bluetooth.BluetoothAdapter#startDiscovery()} instead.
         *
         * @see BleManagerConfig#revertToClassicDiscoveryIfNeeded
         */
        START_BLE_SCAN_FAILED__USING_CLASSIC,

        /**
         * {@link android.bluetooth.BluetoothGatt#getConnectionState(BluetoothDevice)} says we're connected but we never tried to connect in the first place.
         * My theory is that this can happen on some phones when you quickly restart the app and the stack doesn't have
         * a chance to disconnect from the device entirely.
         */
        CONNECTED_WITHOUT_EVER_CONNECTING,

        /**
         * Similar in concept to {@link UhOhListener.UhOh#RANDOM_EXCEPTION} but used when {@link android.os.DeadObjectException} is thrown.
         */
        DEAD_OBJECT_EXCEPTION,

        /**
         * The underlying native BLE stack enjoys surprising you with random exceptions. Every time a new one is discovered
         * it is wrapped in a try/catch and this {@link UhOhListener.UhOh} is dispatched.
         */
        RANDOM_EXCEPTION,

        /**
         * Occasionally, when trying to get the native GattService, android will throw a ConcurrentModificationException. This can happen
         * when trying to perform any read or write. Usually, you simply have to just try again.
         */
        CONCURRENT_EXCEPTION,

        /**
         * This happens if setting the physical layer fails while connecting (bluetooth 5 features). This only applies when the library does it automatically
         * on connections. This doesn't apply if you call {@link BleDevice#setPhyOptions(Phy, ReadWriteListener)}, or {@link BleDevice#readPhyOptions(ReadWriteListener)}
         * manually.
         */
        PHYSICAL_LAYER_FAILURE,

        /**
         * {@link android.bluetooth.BluetoothAdapter#startLeScan(BluetoothAdapter.LeScanCallback)} failed and {@link BleManagerConfig#revertToClassicDiscoveryIfNeeded} is <code>false</code>.
         *
         * @see BleManagerConfig#revertToClassicDiscoveryIfNeeded
         */
        START_BLE_SCAN_FAILED,

        /**
         * {@link android.bluetooth.BluetoothAdapter#startLeScan(BluetoothAdapter.LeScanCallback)} failed and {@link BleManagerConfig#revertToClassicDiscoveryIfNeeded} is <code>true</code>
         * so we try {@link android.bluetooth.BluetoothAdapter#startDiscovery()} but that also fails...fun!
         */
        CLASSIC_DISCOVERY_FAILED,

        /**
         * {@link android.bluetooth.BluetoothGatt#discoverServices()} failed right off the bat and returned false.
         */
        SERVICE_DISCOVERY_IMMEDIATELY_FAILED,

        /**
         * {@link android.bluetooth.BluetoothAdapter#disable()}, through {@link BleManager#turnOff()}, is failing to complete.
         * We always end up back at {@link android.bluetooth.BluetoothAdapter#STATE_ON}.
         */
        CANNOT_DISABLE_BLUETOOTH,

        /**
         * {@link android.bluetooth.BluetoothAdapter#enable()}, through {@link BleManager#turnOn()}, is failing to complete.
         * We always end up back at {@link android.bluetooth.BluetoothAdapter#STATE_OFF}. Opposite problem of {@link #CANNOT_DISABLE_BLUETOOTH}
         */
        CANNOT_ENABLE_BLUETOOTH,

        /**
         * This can be thrown when the underlying state from {@link BluetoothManager#getConnectionState(BluetoothDevice, int)} does not match
         * the apparent condition of the device (for instance, you perform a scan, then try to connect to a device, but it reports as being connected...in this case, it cannot
         * be connected, AND advertising). It seems the values from this method are cached, so sometimes this cache gets "stuck" in the connected state. In this case, it may
         * be best to clear cache of the Bluetooth app (Sometimes called Bluetooth Cache).
         */
        INCONSISTENT_NATIVE_DEVICE_STATE,

        /**
         * Just a blanket case for when the library has to completely shrug its shoulders.
         */
        UNKNOWN_BLE_ERROR,

        /**
         * This can be thrown when there is a WindowManagerBadTokenException when trying to display a dialog
         * The exception is thrown when the activity has finished but the dialog is shown anyway. The only place
         * this would be thrown is in BleSetupHelper. That is the class which has the showDialog() method.
         */
        SETUP_HELPER_DIALOG_ERROR;

        /**
         * Returns the {@link UhOhListener.Remedy} for this {@link UhOhListener.UhOh}.
         */
        public UhOhListener.Remedy getRemedy()
        {
            if( this.ordinal() >= CANNOT_DISABLE_BLUETOOTH.ordinal() )
            {
                return UhOhListener.Remedy.RESTART_PHONE;
            }
            else if( this.ordinal() >= START_BLE_SCAN_FAILED.ordinal() )
            {
                return UhOhListener.Remedy.RESET_BLE;
            }
            else if (this.ordinal() == CONNECTION_STILL_ALIVE.ordinal())
            {
                return Remedy.RECYCLE_CONNECTION;
            }
            {
                return UhOhListener.Remedy.WAIT_AND_SEE;
            }
        }
    }

    /**
     * The suggested remedy for each {@link UhOhListener.UhOh}. This can be used as a proxy for the severity
     * of the issue.
     */
    enum Remedy
    {
        /**
         * This remedy only applies to {@link UhOh#CONNECTION_STILL_ALIVE}. When this happens, we've found that some phones simply need to connect,
         * then disconnect, and the connection gets closed. However, other phones require you to unbond, then bond again, THEN connect and disconnect.
         * A long, stupid process for sure, but we found it to work almost 100% of the time.
         */
        RECYCLE_CONNECTION,

        /**
         * Nothing you can really do, hopefully the library can soldier on.
         */
        WAIT_AND_SEE,

        /**
         * Calling {@link BleManager#reset()} is probably in order.
         *
         * @see BleManager#reset()
         */
        RESET_BLE,

        /**
         * Might want to notify your user that a phone restart is in order.
         */
        RESTART_PHONE;
    }

    /**
     * Struct passed to {@link UhOhListener#onEvent(UhOhListener.UhOhEvent)}.
     */
    @com.idevicesinc.sweetblue.annotations.Immutable
    public static class UhOhEvent extends com.idevicesinc.sweetblue.utils.Event
    {
        /**
         * The manager associated with the {@link UhOhListener.UhOhEvent}
         */
        public BleManager manager(){  return m_manager;  }
        private final BleManager m_manager;

        /**
         * Returns the type of {@link UhOhListener.UhOh} that occurred.
         */
        public UhOhListener.UhOh uhOh(){  return m_uhOh;  }
        private final UhOhListener.UhOh m_uhOh;

        /**
         * Forwards {@link UhOhListener.UhOh#getRemedy()}.
         */
        public UhOhListener.Remedy remedy(){  return uhOh().getRemedy();  };

        UhOhEvent(BleManager manager, UhOhListener.UhOh uhoh)
        {
            m_manager = manager;
            m_uhOh = uhoh;
        }

        @Override public String toString()
        {
            return Utils_String.toString
                    (
                            this.getClass(),
                            "uhOh",			uhOh(),
                            "remedy",		remedy()
                    );
        }
    }

    /**
     * This method is called when an {@link UhOh} happens. This can be anything from a read/write timing out, to more serious bluetooth
     * stack issues where a reboot is needed.
     */
    void onEvent(UhOhEvent e);
}
