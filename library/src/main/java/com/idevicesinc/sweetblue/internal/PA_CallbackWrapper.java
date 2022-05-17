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


import com.idevicesinc.sweetblue.utils.Utils;

/**
 * 
 * 
 */
abstract class PA_CallbackWrapper
{
	protected final P_SweetHandler m_handler;
	protected final boolean m_forcePostToMain;
	
	PA_CallbackWrapper(P_SweetHandler handler, boolean postToMain)
	{
		m_handler = handler;
		m_forcePostToMain = postToMain;
	}
	
	protected boolean postToMain()
	{
		return m_forcePostToMain && !Utils.isOnMainThread();
	}
}
