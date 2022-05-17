/**
 *
 * Copyright 2022 Hubbell Incorporated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.idevicesinc.sweetblue.utils;


/**
 * Some helper utilities for dealing with {@link State} implementors.
 */
public final class Utils_State extends Utils
{
	private Utils_State(){super();}

	public static int[] getModifiedStateBits(int trackedStateBits, int oldStateBits, int newStateBits)
	{
		if (oldStateBits == newStateBits)
			return null;

		// AND the old and new state bits with the states the user cares about
		int oldAnd = oldStateBits & trackedStateBits;
		int newAnd = newStateBits & trackedStateBits;

		// If both old and new are now equal, it means no state change happened that the user cares about
		if (oldAnd == newAnd)
			return null;

		// Otherwise, we return the old, and new ANDed state bits
		return new int[] { oldAnd, newAnd };
	}

	public static boolean query(final int stateMask, Object... query)
	{
		if (query == null || query.length == 0)  return false;

		final boolean internal = false;

		for (int i = 0; i < query.length; i += 2)
		{
			final Object first = query[i];
			final Object second = i + 1 < query.length ? query[i + 1] : null;

			if (first == null && second == null)
			{
				return false;
			}
			else if( first != null && second != null )
			{
				if( (first instanceof State) && (second instanceof Boolean) )
				{
					final State state = (State) first;
					final Boolean value = (Boolean) second;
					final boolean overlap = state.overlaps(stateMask);

					if (value && !overlap)					return false;
					else if (!value && overlap)				return false;
				}
				else if( (first instanceof State) && (second instanceof State) )
				{
					final State state_first = (State) first;
					final State state_second = (State) second;
					final boolean overlap_first = state_first.overlaps(stateMask);
					final boolean overlap_second = state_second.overlaps(stateMask);

					if( overlap_first == false && overlap_second == false )
					{
						return false;
					}
				}
				else
				{
					return false;
				}
			}
			else if( first != null && second == null )
			{
				final State state = (State) first;
				final boolean overlap = state.overlaps(stateMask);

				if( false == overlap )
				{
					return false;
				}
			}
			else
			{
				return false;
			}
		}

		return true;
	}
}
