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


import com.idevicesinc.sweetblue.ReadWriteListener;

/**
 * 
 * 
 */
class P_WrappingReadWriteListener extends PA_CallbackWrapper implements ReadWriteListener
{
	private final ReadWriteListener m_listener;
	
	P_WrappingReadWriteListener(ReadWriteListener listener, P_SweetHandler handler, boolean postToMain)
	{
		super(handler, postToMain);
		
		m_listener = listener;
	}
	
	protected void onEvent(final ReadWriteListener listener, final ReadWriteEvent result)
	{
		if( listener == null )  return;
		
		if( postToMain() )
		{
			m_handler.post(() -> listener.onEvent(result));
		}
		else
		{
			listener.onEvent(result);
		}
	}
	
	@Override public void onEvent(final ReadWriteEvent result)
	{
		onEvent(m_listener, result);
	}
}
