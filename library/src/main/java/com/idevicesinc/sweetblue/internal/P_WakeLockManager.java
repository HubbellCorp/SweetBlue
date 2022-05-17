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

import android.Manifest.permission;
import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.utils.Utils;

final class P_WakeLockManager
{
	private static final String WAKE_LOCK_TAG = "%s:SWEETBLUE_WAKE_LOCK";
	
	private int m_count;
	private final WakeLock m_wakeLock;
	private final IBleManager m_mngr;
	
	public P_WakeLockManager(IBleManager mngr, boolean enabled)
	{
		m_mngr = mngr;
		
		if( enabled )
		{
			if( !Utils.hasPermission(mngr.getApplicationContext(), permission.WAKE_LOCK) )
			{
				Log.e(P_WakeLockManager.class.getSimpleName(), "PERMISSION REQUIRED: " + permission.WAKE_LOCK + ". Or set BleManagerConfig#manageCpuWakeLock to false to disable wake lock management.");
				
				m_wakeLock = null;
				
				return;
			}
			
			final PowerManager powerMngr = (PowerManager) m_mngr.getApplicationContext().getSystemService(Context.POWER_SERVICE);

			m_wakeLock = powerMngr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, String.format(WAKE_LOCK_TAG, mngr.getApplicationContext().getPackageName()));
		}
		else
		{
			m_wakeLock = null;
		}
	}
	
	public void clear()
	{
		if( m_count >= 1 )
		{
			releaseLock();
		}
		
		m_count = 0;
	}
	
	public void push()
	{
		m_count++;
		
		if( m_count == 1 )
		{
			if( m_wakeLock != null )
			{
				m_wakeLock.acquire();
			}
		}
	}
	
	private void releaseLock()
	{
		if( m_wakeLock == null )  return;
		
		try
		{
			m_wakeLock.release();
		}
		
		//--- DRK > Just looking at the source for release(), it can throw a RuntimeException if it's somehow
		//---		overreleased, like maybe app mismanages it. Just being defensive here.
		catch(RuntimeException e)
		{
			m_mngr.ASSERT(false, e.getMessage());
		}
	}
	
	public void pop()
	{
		m_count--;
		
		if( m_count == 0 )
		{
			releaseLock();
		}
		else if( m_count < 0 )
		{
			m_count = 0;
		}
	}
}
