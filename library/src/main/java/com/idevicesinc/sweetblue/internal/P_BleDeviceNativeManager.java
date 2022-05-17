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

package com.idevicesinc.sweetblue.internal;


import android.text.TextUtils;
import com.idevicesinc.sweetblue.BleCharacteristic;
import com.idevicesinc.sweetblue.BleConnectionPriority;
import com.idevicesinc.sweetblue.BleDescriptor;
import com.idevicesinc.sweetblue.BleService;
import com.idevicesinc.sweetblue.BleStatuses;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.UhOhListener;
import com.idevicesinc.sweetblue.internal.android.GattConst;
import com.idevicesinc.sweetblue.internal.android.IBluetoothGatt;
import com.idevicesinc.sweetblue.internal.android.IBluetoothDevice;
import com.idevicesinc.sweetblue.internal.android.IBluetoothManager;
import com.idevicesinc.sweetblue.internal.android.P_Bridge_Native;
import com.idevicesinc.sweetblue.internal.android.P_DeviceHolder;
import com.idevicesinc.sweetblue.internal.android.P_GattHolder;
import com.idevicesinc.sweetblue.utils.CodeHelper;
import com.idevicesinc.sweetblue.utils.LogFunction;
import com.idevicesinc.sweetblue.utils.P_Const;
import com.idevicesinc.sweetblue.utils.Utils_Config;
import com.idevicesinc.sweetblue.utils.Utils_String;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import static com.idevicesinc.sweetblue.BleManagerState.ON;


final class P_BleDeviceNativeManager
{

    private final IBleDevice m_device;
    private final IBluetoothDevice m_deviceLayer;
    private final P_BleDevice_ListenerProcessor m_nativeListener;

    private /*final-ish*/ IBluetoothManager m_managerLayer;
    private /*final-ish*/ IBluetoothGatt m_gattLayer;
    private /*final-ish*/ String m_address;

    private String m_name_native;
    private String m_name_normalized;
    private String m_name_override;

    private int m_bondState_cached = BleStatuses.DEVICE_BOND_UNBONDED;

    //--- DRK > We have to track connection state ourselves because using
    //---		BluetoothManager.getConnectionState() is slightly out of date
    //---		in some cases. Tracking ourselves from callbacks seems accurate.
    private AtomicInteger m_nativeConnectionState = null;


    P_BleDeviceNativeManager(IBleDevice device, IBluetoothDevice deviceLayer, String name_normalized, String name_native)
    {
        m_device = device;
        m_deviceLayer = deviceLayer;
        m_address = m_deviceLayer.getAddress() == null || m_device.isNull() ? P_Const.NULL_MAC : m_deviceLayer.getAddress();

        m_nativeListener = device.isNull() ? null : new P_BleDevice_ListenerProcessor(device);

        m_nativeConnectionState = new AtomicInteger(-1);

        updateName(name_native, name_normalized);

        //--- DRK > Manager can be null for BleDevice.NULL.
        final boolean hitDiskForOverrideName = true;
        final String name_disk = getManager() != null ? getManager().getDiskOptionsManager().loadName(m_address, hitDiskForOverrideName) : null;

        if( name_disk != null )
        {
            setName_override(name_disk);
        }
        else
        {
            if (!m_device.isNull())
            {
                setName_override(m_name_native);
                final boolean saveToDisk = Utils_Config.bool(m_device.conf_device().saveNameChangesToDisk, m_device.conf_mngr().saveNameChangesToDisk);
                getManager().getDiskOptionsManager().saveName(m_address, m_name_native, saveToDisk);
            }
        }
    }



    public final String getAddress()
    {
        if( m_device != null )
        {
            m_device.getIManager().ASSERT(m_address.equals(m_deviceLayer.getAddress()), "");
        }

        return m_address;
    }

    public final BleService getService(UUID serviceUuid)
    {
        return m_gattLayer.getBleService(serviceUuid, (level, msg) -> getLogger().log(level, getAddress(), msg));
    }

    public final void connect(boolean useAutoConnect)
    {
        m_gattLayer.connect(m_deviceLayer, m_device.getIManager().getApplicationContext(), useAutoConnect, m_device.getInternalListener().getNativeCallback());
    }

    public final void disconnect()
    {
        m_gattLayer.disconnect();
    }

    public final boolean readDescriptor(BleDescriptor descriptor)
    {
        return m_gattLayer.readDescriptor(descriptor);
    }

    public final boolean writeDescriptor(BleDescriptor descriptor)
    {
        return m_gattLayer.writeDescriptor(descriptor);
    }

    public final boolean createBond()
    {
        return m_deviceLayer.createBond();
    }

    public final IBluetoothGatt getGattLayer()
    {
        return m_gattLayer;
    }

    public final IBluetoothManager getManagerLayer()
    {
        return m_managerLayer;
    }

    public final boolean startDiscovery()
    {
        return m_managerLayer.startDiscovery();
    }

    public final int getBondState()
    {
        return m_deviceLayer.getBondState();
    }





    final P_BleDevice_ListenerProcessor getNativeListener()
    {
        return m_nativeListener;
    }

    final PA_Task.I_StateListener getTaskListener()
    {
        return m_nativeListener.m_taskStateListener;
    }

    final String getNormalizedName()
    {
        return m_name_normalized;
    }

    final String getNativeName()
    {
        return m_name_native;
    }

    final String getName_override()
    {
        return m_name_override;
    }

    final String getDebugName()
    {
        String lastFourOfMac = "XXXX";
        if (m_address != null && !m_address.isEmpty())
        {
            String[] address_split = m_address.split(":");
            lastFourOfMac = address_split[address_split.length - 2] + address_split[address_split.length - 1];
        }
        String debugName = m_name_normalized.length() == 0 ? "<no_name>" : m_name_normalized;
        String debug = m_device != null ? String.format("%s%s%s", debugName, "_", lastFourOfMac) : debugName;
        return debug;
    }

    final List<BleService> getNativeServiceList()
    {
        return m_gattLayer.getNativeServiceList((level, msg) -> getLogger().log(level, getAddress(), msg));
    }

    final boolean isGattNull()
    {
        return m_gattLayer.isGattNull();
    }

    final boolean readCharacteristic(BleCharacteristic characteristic)
    {
        return m_gattLayer.readCharacteristic(characteristic);
    }

    final boolean writeCharacteristic(BleCharacteristic characteristic)
    {
        return m_gattLayer.writeCharacteristic(characteristic);
    }

    final boolean createBondSneaky(String methodName)
    {
        return m_deviceLayer.createBondSneaky(methodName, m_device.getIManager().getLogger().isEnabled());
    }

    final boolean cancelDiscovery()
    {
        return m_managerLayer.cancelDiscovery();
    }

    final boolean refreshGatt()
    {
        return m_gattLayer.refreshGatt();
    }

    final boolean discoverServices()
    {
        return m_gattLayer.discoverServices();
    }

    final boolean executeReliableWrite()
    {
        return m_gattLayer.executeReliableWrite();
    }

    final boolean readRemoteRssi()
    {
        return m_gattLayer.readRemoteRssi();
    }

    final boolean requestConnectionPriority(BleConnectionPriority priority)
    {
        return m_gattLayer.requestConnectionPriority(priority);
    }

    final boolean requestMtu(int mtu)
    {
        return m_gattLayer.requestMtu(mtu);
    }

    final boolean setCharValue(BleCharacteristic characteristic, byte[] data)
    {
        return m_gattLayer.setCharValue(characteristic, data);
    }

    final boolean setDescValue(BleDescriptor descriptor, byte[] data)
    {
        return m_gattLayer.setDescValue(descriptor, data);
    }

    final boolean gattEquals(P_GattHolder gatt)
    {
        return m_gattLayer.equals(gatt);
    }

    final IBluetoothDevice getDeviceLayer()
    {
        return m_deviceLayer;
    }

    final int getNativeBondState()
    {
        int bondState_native;
        //--- > RB  If Bluetooth is not on, then we can't get the current bond state. This is here to catch the times when the system
        // 			decides to turn BT off/on. This prevents spamming the logs with a bunch of messages about not being able to get the
        // 			bond state
        if (m_device != null && getManager().is(ON))
        {
            bondState_native = m_device.getNative().getBondState();
            m_bondState_cached = bondState_native;
        }
        else
        {
            bondState_native = m_bondState_cached;
        }

        return bondState_native;
    }

    final int getNativeConnectionState()
    {
        //--- > RB If BT has been turned off quickly (for instance, there are many devices connected, then you go into BT settings and run a scan), then
        // 			we obviously won't be able to get a state. We return BLE_DISCONNECTED here for obvious reasons.
        if (m_managerLayer.isBluetoothEnabled())
        {
            return m_managerLayer.getConnectionState(m_device.getNative(), GattConst.GATT_SERVER);
        }
        else
        {
            return GattConst.STATE_DISCONNECTED;
        }
    }

    final int getConnectionState()
    {
        return performGetNativeState(null, null);
        // It was thought that getting the state from the UI thread would alleviate some issues. It wasn't found to be the case
        // I'm leaving it here just in case we need to switch back.
//		if (Utils.isOnMainThread())
//		{
//			return performGetNativeState(null, null);
//		}
//		else
//		{
//			// > RB - This may not be necessary anymore. It was thought that the check of the native state had to be made on the UI thread,
//			// so this is here to block the current thread until it gets the result back.
//			final CountDownLatch latch = new CountDownLatch(1);
//			final AtomicInteger state = new AtomicInteger(-1);
//			getIManager().getPostManager().postToMain(new Runnable()
//			{
//				@Override public void run()
//				{
//					performGetNativeState(state, latch);
//				}
//			});
//			try
//			{
//				latch.await();
//			} catch (Exception e)
//			{
//				e.printStackTrace();
//			}
//			return state.get();
//		}
    }

    final boolean needsInit()
    {
        return m_managerLayer == null || m_gattLayer == null;
    }

    final void init(IBluetoothGatt gattLayer, IBluetoothManager managerLayer)
    {
        m_gattLayer = gattLayer;
        m_managerLayer = managerLayer;
    }

    final void setName_override(final String name)
    {
        m_name_override = name != null ? name : "";
    }

    final void updateNativeName(final String name_native)
    {
        final String name_native_override;

        if( name_native != null )
        {
            name_native_override = name_native;
        }
        else
        {
            //--- DRK > After a ble reset using cached devices, calling updateNativeDevice with the old native BluetoothDevice
            //---		instance gives you a null name...not sure how long this has been the case, but I think only > 5.0
            name_native_override = m_name_native;
        }

        final String name_normalized = Utils_String.normalizeDeviceName(name_native_override);

        updateName(name_native_override, name_normalized);
    }

    final void clearName_override()
    {
        setName_override(m_name_native);
    }

    final void updateNativeConnectionState(IBluetoothGatt gatt)
    {
        updateNativeConnectionState(P_Bridge_Native.newGattHolder(gatt.getGatt()));
    }

    final void updateNativeConnectionState(P_GattHolder gatt)
    {
        updateNativeConnectionState(gatt, null);
    }

    final void updateNativeConnectionState(P_GattHolder gatt, Integer state)
    {
        if( state == null )
        {
            m_nativeConnectionState.set(getNativeConnectionState());
        }
        else
        {
            m_nativeConnectionState.set(state);
        }

        updateGattFromCallback(gatt);

        getLogger().i(CodeHelper.gattConn(m_nativeConnectionState.get(), getLogger().isEnabled()));
    }

    final boolean isNativelyBonding()
    {
        return getNativeBondState() == BleStatuses.DEVICE_BOND_BONDING;
    }

    final boolean isNativelyBondedOrBonding()
    {
        int bondState = getNativeBondState();
        return bondState == BleStatuses.DEVICE_BOND_BONDED || bondState == BleStatuses.DEVICE_BOND_BONDING;
    }

    final boolean isNativelyBonding(int bondState)
    {
        return bondState == BleStatuses.DEVICE_BOND_BONDING;
    }

    final boolean isNativelyBonded()
    {
        return getNativeBondState() == BleStatuses.DEVICE_BOND_BONDED;
    }

    final boolean isNativelyBonded(int bondState)
    {
        return bondState == BleStatuses.DEVICE_BOND_BONDED;
    }

    final boolean isNativelyUnbonded()
    {
        return getNativeBondState() == BleStatuses.DEVICE_BOND_UNBONDED;
    }

    final boolean isNativelyUnbonded(int bondState)
    {
        return bondState == BleStatuses.DEVICE_BOND_UNBONDED;
    }

    final boolean isNativelyConnected()
    {
        synchronized (this)
        {
            return getConnectionState() == BleStatuses.DEVICE_CONNECTED;
        }
    }

    final boolean isNativelyConnecting()
    {
        return getConnectionState() == BleStatuses.DEVICE_CONNECTING;
    }

    final boolean isNativelyDisconnecting()
    {
        return getConnectionState() == BleStatuses.DEVICE_DISCONNECTING;
    }

    final boolean isNativelyConnectingOrConnected()
    {
        int state = getConnectionState();
        return state == BleStatuses.DEVICE_CONNECTED|| state == BleStatuses.DEVICE_CONNECTING;
    }

    final void closeGattIfNeeded(boolean disconnectAlso)
    {
        m_device.getReliableWriteManager().onDisconnect();

        if( gattLayer() == null || gattLayer().isGattNull() )  return;

        closeGatt(disconnectAlso);
    }

    final void updateNativeDeviceOnly(final IBluetoothDevice device_native)
    {
        m_deviceLayer.setNativeDevice(device_native.getNativeDevice(), P_DeviceHolder.newHolder(device_native.getNativeDevice(), device_native.getAddress()));
    }

    final void updateNativeDevice(final IBluetoothDevice device_native, final byte[] scanRecord_nullable, boolean isSameScanRecord)
    {
        if (!isSameScanRecord)
        {
            String name_native;
            try
            {
                name_native = getManager().getDeviceName(device_native, scanRecord_nullable);
            } catch (Exception e)
            {
                getLogger().e("Failed to parse name, returning what BluetoothDevice returns.");
                name_native = device_native.getName();
            }

            if (!TextUtils.equals(name_native, m_name_native))
            {
                updateNativeName(name_native);
            }
        }

        m_deviceLayer.setNativeDevice(device_native.getNativeDevice(), P_DeviceHolder.NULL);

    }












    private P_Logger getLogger()
    {
        return getManager().getLogger();
    }

    private void updateName(String name_native, String name_normalized)
    {
        name_native = name_native != null ? name_native : "";
        m_name_native = name_native;

        m_name_normalized = name_normalized;
    }

    private IBleManager getManager()
    {
        return m_device.getIManager();
    }

    private void updateGattFromCallback(P_GattHolder gatt)
    {
        if (gatt == null && !P_Bridge_User.isUnitTest(getManager().getConfigClone()))
        {
            getLogger().w("Gatt object from callback is null.");
        }
        else
        {
            setGatt(gatt);
        }
    }

    private int performGetNativeState(AtomicInteger state, CountDownLatch latch)
    {
        final int reportedNativeConnectionState = getNativeConnectionState();
        int connectedStateThatWeWillGoWith = reportedNativeConnectionState;

        if( m_nativeConnectionState != null && m_nativeConnectionState.get() != -1 )
        {
            if( m_nativeConnectionState.get() != reportedNativeConnectionState )
            {
                getLogger().e("Tracked native state " + CodeHelper.gattConn(m_nativeConnectionState.get(), getLogger().isEnabled()) + " doesn't match reported state " + CodeHelper.gattConn(reportedNativeConnectionState, getLogger().isEnabled()) + ".");
            }

            connectedStateThatWeWillGoWith = m_nativeConnectionState.get();
        }

        if( connectedStateThatWeWillGoWith != GattConst.STATE_DISCONNECTED )
        {
            if( gattLayer().isGattNull() )
            {
                //--- DRK > Can't assert here because gatt can legitmately be null even though we have a connecting/ed native state.
                //---		This was observed on the moto G right after app start up...getNativeConnectionState() reported connecting/ed
                //---		but we haven't called connect yet. Really rare...only seen once after 4 months.
                if( m_nativeConnectionState == null )
                {
                    getLogger().e("Gatt is null with " + CodeHelper.gattConn(connectedStateThatWeWillGoWith, getLogger().isEnabled()));

                    connectedStateThatWeWillGoWith = GattConst.STATE_DISCONNECTED;

                    getManager().uhOh(UhOhListener.UhOh.CONNECTED_WITHOUT_EVER_CONNECTING);
                }
                else
                {
                    getManager().ASSERT(false, "Gatt is null with tracked native state: " + CodeHelper.gattConn(connectedStateThatWeWillGoWith, getLogger().isEnabled()));
                }
            }
        }
        else
        {
            //--- DRK > Had this assert here but must have been a brain fart because we can be disconnected
            //---		but still have gatt be not null cause we're trying to reconnect.
            //			if( !m_mngr.ASSERT(m_gatt == null) )
            //			{
            //				m_logger.e(m_logger.gattConn(connectedStateThatWeWillGoWith));
            //			}
        }
        if (state != null)
        {
            state.set(connectedStateThatWeWillGoWith);
        }
        if (latch != null)
        {
            latch.countDown();
        }
        return connectedStateThatWeWillGoWith;
    }

    private void closeGatt(boolean disconnectAlso)
    {
        UhOhListener.UhOh uhoh = m_gattLayer.closeGatt();
        if (uhoh != null)
        {
            m_device.getIManager().uhOh(uhoh);
        }
        m_nativeConnectionState.set(GattConst.STATE_DISCONNECTED);
    }

    private IBluetoothGatt gattLayer()
    {
        return m_gattLayer;
    }

    private void setGatt(P_GattHolder gatt)
    {
        if( gattLayer() != null  && !gattLayer().isGattNull() && gatt != null)
        {
            //--- DRK > This tripped with an S5 and iGrillv2 with low battery (not sure that matters).
            //---		AV was able to replicate twice but was not attached to debugger and now can't replicate.
            //---		As a result of a brief audit, moved gatt object setting from the ending state
            //---		handler of the connect task in P_BleDevice_Listeners to the execute method of the connect task itself.
            //---		Doesn't solve any particular issue found, but seems more logical.
            getManager().ASSERT(gattLayer().getGatt() == gatt.getGatt(), "Different gatt object set.");

            if( gattLayer().getGatt() != gatt.getGatt() )
            {
                closeGatt(/*disconnectAlso=*/false);
            }
            else
            {
                return;
            }
        }

        if( gatt == null)
        {
            gattLayer().setGatt(null);
        }
        else
        {
            gattLayer().setGatt(gatt.getGatt());
        }
    }
}
