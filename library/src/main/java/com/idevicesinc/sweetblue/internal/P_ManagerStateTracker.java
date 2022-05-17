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


import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerState;
import com.idevicesinc.sweetblue.ManagerStateListener;
import com.idevicesinc.sweetblue.ManagerStateListener.StateEvent;
import com.idevicesinc.sweetblue.P_Bridge_User;


final class P_ManagerStateTracker extends PA_StateTracker<BleManagerState>
{

	private ManagerStateListener m_stateListener;

	
	P_ManagerStateTracker(IBleManager mngr)
	{
		super(mngr, BleManagerState.VALUES());
		
	}

	@Override
	public final boolean is_native(BleManagerState state)
	{
		if (state.getNativeCode() == -1)
			return false;

		return state.getNativeCode() == getState_native();
	}

	public final void update_native(BleManagerState state)
	{
		if (state.getNativeCode() != -1)
		{
			update_native(state.getNativeCode());
		}
	}

	@Override
	final int trackedStates()
	{
		BleManagerState[] states = getManager().getConfigClone().defaultManagerStates;
		if (states == null)
			states = BleManagerState.VALUES();
		int mask = 0;
		for (BleManagerState state : states)
			mask |= state.bit();
		return mask;
	}

	public final void setListener(ManagerStateListener listener)
	{
		m_stateListener = listener;
	}

	@Override
	protected final void onStateChange(final int oldStateBits, final int newStateBits, final int intentMask, final int status)
	{
		int[] moddedBits = getModifiedStateBits(oldStateBits, newStateBits);
		if( moddedBits != null && m_stateListener != null )
		{
			final StateEvent event = P_Bridge_User.newManagerStateEvent(BleManager.get(getManager().getApplicationContext()), moddedBits[0], moddedBits[1], intentMask);
			getManager().postEvent(m_stateListener, event);
		}
	}
	
	@Override
	public final String toString()
	{
		return super.toString(BleManagerState.VALUES());
	}
}
