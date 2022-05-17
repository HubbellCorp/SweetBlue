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
 *
 */
public interface ForEach_Breakable<T>
{
	public static class Please
	{
		private static final Please CONTINUE = new Please(true);
		private static final Please BREAK = new Please(false);

		private final boolean m_continue;

		private Please(final boolean doContinue)
		{
			m_continue = doContinue;
		}

		public boolean shouldContinue()
		{
			return m_continue;
		}

		public boolean shouldBreak()
		{
			return !shouldContinue();
		}

		public static Please doContinue()
		{
			return CONTINUE;
		}

		public static Please doBreak()
		{
			return BREAK;
		}
	}

	Please next(T next);
}
