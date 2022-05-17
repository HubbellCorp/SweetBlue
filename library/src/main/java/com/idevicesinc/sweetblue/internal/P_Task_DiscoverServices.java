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
import com.idevicesinc.sweetblue.BleTask;
import com.idevicesinc.sweetblue.UhOhListener.UhOh;
import com.idevicesinc.sweetblue.utils.Interval;


final class P_Task_DiscoverServices extends PA_Task_RequiresConnection
{

	private int m_gattStatus = BleStatuses.GATT_STATUS_NOT_APPLICABLE;
	private boolean m_gattRefresh;
	private boolean m_useDelay;
	private double m_curGattDelay;
	private double m_gattDelayTarget;
	private boolean m_discoverAttempted;

	
	public P_Task_DiscoverServices(IBleDevice bleDevice, I_StateListener listener, boolean gattRefresh, boolean useDelay, Interval gattDelay)
	{
		super(bleDevice, listener);
		m_gattRefresh = gattRefresh;
		m_useDelay = useDelay;
		m_gattDelayTarget = Interval.isDisabled(gattDelay) || gattDelay == Interval.INFINITE ? 0.0 : gattDelay.secs();
	}

	@Override public void execute()
	{
		if( m_gattRefresh )
		{
			getDevice().nativeManager().refreshGatt();
		}

		if (m_useDelay)
		{
			getLogger().i("Delaying service discovery by " + m_gattDelayTarget + " seconds.");
			return;
		}

		if( !getDevice().nativeManager().discoverServices() )
		{
			failImmediately();
			
			getManager().uhOh(UhOh.SERVICE_DISCOVERY_IMMEDIATELY_FAILED);
		}
		m_discoverAttempted = true;
	}

	@Override protected void update(double timeStep)
	{
		if (m_useDelay && !m_discoverAttempted)
		{
			m_curGattDelay += timeStep;
			if (m_curGattDelay >= m_gattDelayTarget)
			{
				m_discoverAttempted = true;
				if( !getDevice().nativeManager().discoverServices() )
				{
					failImmediately();

					getManager().uhOh(UhOh.SERVICE_DISCOVERY_IMMEDIATELY_FAILED);
				}
			}
		}
	}

	@Override public PE_TaskPriority getPriority()
	{
		return PE_TaskPriority.MEDIUM;
	}
	
	public void onNativeFail(int gattStatus)
	{
		m_gattStatus = gattStatus;

//		if (getDevice().is(BleDeviceState.BLE_CONNECTED))
//		{
//			getDevice().disconnectWithReason(BleDevice.ConnectionFailListener.Status.DISCOVERING_SERVICES_FAILED, BleDevice.ConnectionFailListener.Timing.EVENTUALLY, gattStatus, BleStatuses.BOND_FAIL_REASON_NOT_APPLICABLE, BleDevice.ReadWriteListener.ReadWriteEvent.NULL(getDevice()));
//		}
		
		this.fail();
	}
	
	public int getGattStatus()
	{
		return m_gattStatus;
	}
	
	@Override protected BleTask getTaskType()
	{
		return BleTask.DISCOVER_SERVICES;
	}
}
