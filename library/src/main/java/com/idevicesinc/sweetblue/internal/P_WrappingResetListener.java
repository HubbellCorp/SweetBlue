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

import com.idevicesinc.sweetblue.ResetListener;

import java.util.ArrayList;

/**
 * 
 * 
 *
 */
final class P_WrappingResetListener extends PA_CallbackWrapper implements ResetListener
{
	private final ArrayList<ResetListener> m_listeners = new ArrayList<>();
	
	P_WrappingResetListener(ResetListener listener, P_SweetHandler handler, boolean postToMain)
	{
		super(handler, postToMain);

		m_listeners.add(listener);
	}
	
	public void addListener(ResetListener listener)
	{
		m_listeners.add(listener);
	}

	@Override public void onEvent(final ResetEvent event)
	{
		final Runnable runnable = () -> {
            for( int i = 0; i < m_listeners.size(); i++ )
            {
                m_listeners.get(i).onEvent(event);
            }
        };
		
		if( postToMain() )
		{
			m_handler.post(runnable);
		}
		else
		{
			runnable.run();
		}
	}
}
