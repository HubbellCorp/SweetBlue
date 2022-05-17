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

package com.idevicesinc.sweetblue.utils;


/**
 * Contract to force <code>enum</code> implementors to comply to common bitwise operations.
 */
public interface BitwiseEnum extends Flag
{
	/**
	 * Does a bitwise OR for this state and the given state.
	 */
	int or(BitwiseEnum state);

	/**
	 * Does a bitwise OR for this state and the given bits.
	 */
	int or(int bits);

	/**
	 * Convenience method for checking if <code>({@link #bit()} &amp; mask) != 0x0</code>.
	 */
	boolean overlaps(int mask);

	/**
	 * Same as {@link Enum#name()}.
	 */
	String name();
}
