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

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BondFilter;
import com.idevicesinc.sweetblue.DeviceStateListener;
import com.idevicesinc.sweetblue.DeviceStateListener.StateEvent;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.utils.Utils_Config;

import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;


final class P_DeviceStateTracker extends PA_StateTracker<BleDeviceState>
{
	private final Stack<DeviceStateListener> m_stateListenerStack;
	private final P_BleDeviceImpl m_device;
	private final AtomicInteger m_nativeBondState;

	private boolean m_syncing = false;

	
	P_DeviceStateTracker(IBleDevice device)
	{
		super(device.getIManager(), BleDeviceState.VALUES(), /*trackTimes=*/true);

		m_nativeBondState = new AtomicInteger(0);

		m_stateListenerStack = new Stack<>();

		m_device = (P_BleDeviceImpl) device;
	}
	
	public final boolean setListener(DeviceStateListener listener)
	{
		m_stateListenerStack.clear();
		return m_stateListenerStack.push(listener) != null;
	}

	final boolean pushListener(DeviceStateListener listener)
	{
		return m_stateListenerStack.push(listener) != null;
	}

	final boolean popListener()
	{
		if (!m_stateListenerStack.empty())
        {
            m_stateListenerStack.pop();
            return true;
        }
        return false;
	}

	final boolean popListener(DeviceStateListener listener)
	{
		return !m_stateListenerStack.empty() && m_stateListenerStack.remove(listener);
	}

	final void update_native(BleDeviceState state)
	{
		final int bit = P_Bridge_User.getNativeBit(state);
		if (bit != -1)
		{
			update_native(bit);
		}
	}

	final void updateBondState(BleDeviceState state)
	{
		final int bit = P_Bridge_User.getNativeBit(state);
		if (bit != -1)
			m_nativeBondState.set(bit);
	}

	/**
	 * This returns a cached Bond state. If you want the one from the native stack, use {@link P_BleDeviceNativeManager#getNativeBondState()}.
	 */
	public int getBondState()
	{
		return m_nativeBondState.get();
	}

	/**
	 * Returns <code>true</code> if the native state matches the given {@link BleDeviceState}. This will ONLY check
	 * {@link BleDeviceState#BLE_CONNECTED}, {@link BleDeviceState#BLE_CONNECTING}, {@link BleDeviceState#BLE_DISCONNECTED}, {@link BleDeviceState#BONDED},
	 * {@link BleDeviceState#BONDING}, or {@link BleDeviceState#UNBONDED}. All other states will return <code>false</code>.
	 */
	@Override
	public boolean is_native(BleDeviceState state)
	{
		// Bond state is kept in a separate int natively, so here we need to figure out if it's a bond state, or otherwise so we can compare
		// the correct native state int
		int stateToCheck;
		boolean bondState = false;
		switch (state)
		{
			case BONDED:
				stateToCheck = BluetoothDevice.BOND_BONDED;
				bondState = true;
				break;
			case BONDING:
				stateToCheck = BluetoothDevice.BOND_BONDING;
				bondState = true;
				break;
			case UNBONDED:
				stateToCheck = BluetoothDevice.BOND_NONE;
				bondState = true;
				break;
			case BLE_CONNECTED:
				stateToCheck = BluetoothGatt.STATE_CONNECTED;
				break;
			case BLE_CONNECTING:
				stateToCheck = BluetoothGatt.STATE_CONNECTING;
				break;
			case BLE_DISCONNECTED:
				stateToCheck = BluetoothGatt.STATE_DISCONNECTED;
				break;
			default:
				return false;
		}
		return stateToCheck == (bondState ? m_nativeBondState.get() : getState_native());
	}

	final void clearListenerStack()
    {
        m_stateListenerStack.clear();
    }

	public final DeviceStateListener getListener()
	{
		if (m_stateListenerStack.empty())
			return null;
		return m_stateListenerStack.peek();
	}

    final void sync(P_DeviceStateTracker otherTracker)
	{
		m_syncing = true;
		
		this.copy(otherTracker);
		
		m_syncing = false;
	}

	@Override protected final void onStateChange(final int oldStateBits, final int newStateBits, final int intentMask, final int gattStatus)
	{
		if( m_device.isNull() )		return;
		if( m_syncing )				return;

		// Filter out state bits the user doesn't care about. If this returns null, it means no state change happened that the user cares about.
		int[] moddedBits = getModifiedStateBits(oldStateBits, newStateBits);

		if (moddedBits != null)
		{
			if (getListener() != null)
			{
				final StateEvent event = P_Bridge_User.newDeviceStateEvent(m_device.getBleDevice(), moddedBits[0], moddedBits[1], intentMask, gattStatus);
				m_device.postEventAsCallback(getListener(), event);
			}
		}

        // As the manager can have different tracked states, let's run the modified state bits against that
        moddedBits = getModifiedStateBits(getManagerStateMask(), oldStateBits, newStateBits);

        if (moddedBits != null)
        {
            final DeviceStateListener listener = m_device.getIManager().getDefaultDeviceStateListener();
            if (listener != null)
            {
                final StateEvent event = P_Bridge_User.newDeviceStateEvent(m_device.getBleDevice(), moddedBits[0], moddedBits[1], intentMask, gattStatus);
                m_device.postEventAsCallback(listener, event);
            }
        }

		final BondFilter bondFilter = Utils_Config.filter(m_device.conf_device().bondFilter, m_device.conf_mngr().bondFilter);
		
		if( bondFilter == null )  return;
		
		final BondFilter.StateChangeEvent bondStateChangeEvent = P_Bridge_User.newStateChangeEvent(m_device.getBleDevice(), oldStateBits, newStateBits, intentMask, gattStatus);
		
		final BondFilter.Please please = bondFilter.onEvent(bondStateChangeEvent);
		
		m_device.getIManager().getLogger().checkPlease(please, BondFilter.Please.class);
		
		m_device.getBondManager().applyPlease_BondFilter(please);
	}

	private int getManagerStateMask()
    {
        BleDeviceState[] states = getManager().getConfigClone().defaultDeviceStates;
        if (states == null) states = BleDeviceState.VALUES();

        int mask = 0;
        for (BleDeviceState state : states)
        {
            mask |= state.bit();
        }
        return mask;
    }

	@Override
	int trackedStates()
	{
		BleDeviceState[] statesTracked = m_device.conf_device().defaultDeviceStates;
		if (statesTracked == null)
			statesTracked = BleDeviceState.DEFAULT_STATES;
		int mask = 0;
		for (BleDeviceState state : statesTracked)
			mask |= state.bit();
		return mask;
	}

	@Override public final String toString()
	{
		return super.toString(BleDeviceState.VALUES());
	}
}
