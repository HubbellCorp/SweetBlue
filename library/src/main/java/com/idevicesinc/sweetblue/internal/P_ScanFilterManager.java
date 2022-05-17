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

import java.util.ArrayList;
import java.util.List;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.ScanFilter;
import com.idevicesinc.sweetblue.ScanFilter.Please;
import com.idevicesinc.sweetblue.ScanFilter.ScanEvent;


final class P_ScanFilterManager
{
	private ScanFilter m_default;  // Regular default scan filter
	private ScanFilter m_ephemeral = null;  // Temporary filter for just the current scan
	private ScanFilter.ApplyMode m_ephemeralApplyMode = ScanFilter.ApplyMode.CombineEither;  // How should the ephemeral filter be applied?
	private final IBleManager m_mngr;

	
	P_ScanFilterManager(final IBleManager mngr, final ScanFilter defaultFilter)
	{
		m_mngr = mngr;
		m_default = defaultFilter;
	}

	void setDefaultFilter(ScanFilter filter)
	{
		m_default = filter;
	}

	void setEphemeralFilter(ScanFilter ephemeral)
	{
		m_ephemeral = ephemeral;
	}

	void setEphemeralFilter(ScanFilter ephemeral, ScanFilter.ApplyMode applyMode)
	{
		m_ephemeral = ephemeral;
		m_ephemeralApplyMode = applyMode;
	}

	void setEphemeralFilterApplyMode(ScanFilter.ApplyMode applyMode)
	{
		m_ephemeralApplyMode = applyMode;
	}

	void clearEphemeralFilter()
	{
		m_ephemeral = null;
		m_ephemeralApplyMode = ScanFilter.ApplyMode.CombineEither;
	}

	private List<ScanFilter> activeFilters()
	{
		List<ScanFilter> l = new ArrayList<>();
		if (m_ephemeralApplyMode == ScanFilter.ApplyMode.Override)
		{
			if (m_ephemeral != null)
				l.add(m_ephemeral);
		}
		else
		{
			if (m_default != null)
				l.add(m_default);
			if (m_ephemeral != null)
				l.add(m_ephemeral);
		}
		return l;
	}

	public boolean makeEvent()
	{
		return activeFilters().size() > 0;
	}
	
	ScanFilter.Please allow(P_Logger logger, final ScanEvent e)
	{
		List<ScanFilter> activeFilters = activeFilters();

		if (activeFilters.isEmpty())
			return Please.acknowledge();

		Please yesPlease = null;
		for (ScanFilter sf : activeFilters)
		{
			final Please please = sf.onEvent(e);

			logger.checkPlease(please, Please.class);

			// Scanning is now stopped after the scan entries have been processed instead of here
			// This ensures the ephemeral discovery listener is fired before being cleared when a scan stops.
			/*stopScanningIfNeeded(sf, please);*/

			boolean accepted = (please != null && P_Bridge_User.ack(please));

			if (accepted && yesPlease == null)
				yesPlease = please;

			if (m_ephemeralApplyMode != ScanFilter.ApplyMode.CombineBoth && accepted)
				return please;

			if (m_ephemeralApplyMode == ScanFilter.ApplyMode.CombineBoth && !accepted)
				return ScanFilter.Please.ignore();
		}

		return m_ephemeralApplyMode == ScanFilter.ApplyMode.CombineBoth && yesPlease != null ? yesPlease : ScanFilter.Please.ignore();
	}

	private void stopScanningIfNeeded(final ScanFilter filter, final ScanFilter.Please please_nullable)
	{
		if( please_nullable != null )
		{
			if(P_Bridge_User.ack(please_nullable))
			{
				if( P_Bridge_User.stopScan(please_nullable) )
				{
					m_mngr.stopScan(filter);
				}
			}
		}
	}
}
