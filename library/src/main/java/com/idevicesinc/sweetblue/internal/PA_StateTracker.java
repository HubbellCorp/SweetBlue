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


import com.idevicesinc.sweetblue.BleStatuses;
import com.idevicesinc.sweetblue.utils.State;
import com.idevicesinc.sweetblue.utils.Utils_State;
import com.idevicesinc.sweetblue.utils.Utils_String;
import java.util.concurrent.atomic.AtomicInteger;


abstract class PA_StateTracker<T extends State>
{
	enum E_Intent
	{
		INTENTIONAL, UNINTENTIONAL;
		
		public int getMask()
		{
			return this == INTENTIONAL ? 0xFFFFFFFF : 0x0;
		}
		
		public State.ChangeIntent convert()
		{
			switch(this)
			{
				case INTENTIONAL:	  return State.ChangeIntent.INTENTIONAL;
				case UNINTENTIONAL:	  return State.ChangeIntent.UNINTENTIONAL;
			}
			
			return State.ChangeIntent.NULL;
		}
	}
	
	private int m_stateMask = 0x0;

	private final long[] m_timesInState;
	private final int m_stateCount;

	private final AtomicInteger m_nativeState;
	private final IBleManager m_manager;

	
	PA_StateTracker(final IBleManager manager, final State[] enums, final boolean trackTimes)
	{
		m_manager = manager;
		m_stateCount = enums.length;
		m_timesInState = trackTimes ? new long[m_stateCount] : null;
		m_nativeState = new AtomicInteger(0);
	}
	
	PA_StateTracker(final IBleManager manager, final State[] enums)
	{
		this(manager, enums, /*trackTimes=*/true);
	}
	
	public boolean is(State state)
	{
		return checkBitMatch(state, true);
	}

	public abstract boolean is_native(T state);
	
	public int getState()
	{
		return m_stateMask;
	}

	public int getState_native()
	{
		return m_nativeState.get();
	}
	
	boolean checkBitMatch(State flag, boolean value)
	{
		return ((flag.bit() & m_stateMask) != 0) == value;
	}
	
	private int getMask(final int currentStateMask, final Object[] statesAndValues)
	{
		int newStateBits = currentStateMask;
		
		for( int i = 0; i < statesAndValues.length; i++ )
		{
			Object ithValue = statesAndValues[i];
			
			if( ithValue instanceof Object[] )
			{
				newStateBits = getMask(newStateBits, (Object[])ithValue);
				
				continue;
			}
			
			State state = (State) statesAndValues[i];
			boolean append = true;
			
			if( statesAndValues[i+1] instanceof Boolean )
			{
				append = (Boolean) statesAndValues[i+1];
				i++;
			}

			// TODO - Investigate this further to attempt to find the root cause
			// Sometimes we get a weird BLE state back from the native stack. For now, we'll just ignore it.
			if (state != null)
			{
				if (append)
				{
					append_assert(state);

					newStateBits |= state.bit();
				}
				else
				{
					newStateBits &= ~state.bit();
				}
			}
		}
		
		return newStateBits;
	}
	
	void append(State newState, E_Intent intent, int status)
	{
		if( newState./*already*/overlaps(m_stateMask) )
		{
			return;
		}

		append_assert(newState);

		setStateMask(m_stateMask | newState.bit(), intent == E_Intent.INTENTIONAL ? newState.bit() : 0x0, status, true);
	}
	
	void remove(State state, E_Intent intent, int status)
	{
		setStateMask(m_stateMask & ~state.bit(), intent == E_Intent.INTENTIONAL ? state.bit() : 0x0, status, true);
	}
	
	protected void append_assert(State newState){}

	void set(final E_Intent intent, final int status, final Object ... statesAndValues)
	{
		set(intent.getMask(), status, statesAndValues);
	}

	void set_noCallback(final E_Intent intent, final int status, final Object... statesAndValues)
	{
		final int newStateBits = getMask(0x0, statesAndValues);

		setStateMask(newStateBits, intent.getMask(), status, false);
	}
	
	private void set(final int intentMask, final int status, final Object ... statesAndValues)
	{
		final int newStateBits = getMask(0x0, statesAndValues);

		setStateMask(newStateBits, intentMask, status, true);
	}
	
	void update(E_Intent intent, int status, Object ... statesAndValues)
	{
		update(intent.getMask(), status, statesAndValues);
	}

	void update_native(int nativeState)
	{
		m_nativeState.set(nativeState);
	}
	
	private void update(int intentMask, int status, Object ... statesAndValues)
	{
		int newStateBits = getMask(m_stateMask, statesAndValues);

		setStateMask(newStateBits, intentMask, status, true);
	}
	
	long getTimeInState(int stateOrdinal)
	{
		if( m_timesInState == null )  return 0;
		
		int bit = (0x1 << stateOrdinal);
		
		if( (bit & m_stateMask) != 0x0 )
		{
			return m_manager.currentTime() - m_timesInState[stateOrdinal];
		}
		else
		{
			return m_timesInState[stateOrdinal];
		}
	}

	abstract int trackedStates();

	int[] getModifiedStateBits(int oldStateBits, int newStateBits)
	{
		return Utils_State.getModifiedStateBits(trackedStates(), oldStateBits, newStateBits);
	}

	int[] getModifiedStateBits(int trackedBits, int oldStateBits, int newStateBits)
	{
		return Utils_State.getModifiedStateBits(trackedBits, oldStateBits, newStateBits);
	}
	
	protected void copy(PA_StateTracker stateTracker)
	{
		this.setStateMask(stateTracker.getState(), 0x0, BleStatuses.GATT_STATUS_NOT_APPLICABLE, true);
	}

	protected IBleManager getManager()
	{
		return m_manager;
	}

	private void setStateMask(final int newStateBits, int intentMask, final int status, final boolean fireChange)
	{
		int oldStateBits = m_stateMask;
		m_stateMask = newStateBits;

		final long currentTime = m_manager != null ? m_manager.currentTime() : System.currentTimeMillis();
		
		//--- DRK > Minor skip optimization...shouldn't actually skip (too much) in practice
		//---		if other parts of the library are handling their state tracking sanely.
		if( oldStateBits != newStateBits )
		{
			for( int i = 0, bit = 0x1; i < m_stateCount; i++, bit <<= 0x1 )
			{
				//--- DRK > State exited...
				if( (oldStateBits & bit) != 0x0 && (newStateBits & bit) == 0x0 )
				{
					if( m_timesInState != null )
					{
						m_timesInState[i] = currentTime - m_timesInState[i];
					}
				}
				//--- DRK > State entered...
				else if( (oldStateBits & bit) == 0x0 && (newStateBits & bit) != 0x0 )
				{
					if( m_timesInState != null )
					{
						m_timesInState[i] = currentTime;
					}
				}
				else
				{
					intentMask &= ~bit;
				}
			}
		}
		else
		{
			intentMask = 0x0;
		}

		if (fireChange)
			fireStateChange(oldStateBits, newStateBits, intentMask, status);
	}
	
	protected abstract void onStateChange(int oldStateBits, int newStateBits, int intentMask, int status);
	
	private void fireStateChange(int oldStateBits, int newStateBits, int intentMask, int status)
	{
		if( oldStateBits == newStateBits )
		{
			return;
		}
		
		onStateChange(oldStateBits, newStateBits, intentMask, status);
	}
	
	protected String toString(State[] enums)
	{
		return Utils_String.toString(m_stateMask, enums);
	}
}
