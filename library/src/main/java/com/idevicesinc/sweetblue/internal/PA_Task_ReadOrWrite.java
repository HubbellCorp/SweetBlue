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
import java.util.UUID;
import android.bluetooth.BluetoothGatt;
import com.idevicesinc.sweetblue.BleCharacteristic;
import com.idevicesinc.sweetblue.BleDescriptor;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleOp;
import com.idevicesinc.sweetblue.BleStatuses;
import com.idevicesinc.sweetblue.DescriptorFilter;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.ReadWriteListener;
import com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent;
import com.idevicesinc.sweetblue.ReadWriteListener.Status;
import com.idevicesinc.sweetblue.ReadWriteListener.Target;
import com.idevicesinc.sweetblue.UhOhListener;
import com.idevicesinc.sweetblue.internal.android.P_GattHolder;
import com.idevicesinc.sweetblue.utils.P_Const;
import com.idevicesinc.sweetblue.utils.PresentData;
import com.idevicesinc.sweetblue.utils.Utils;


abstract class PA_Task_ReadOrWrite extends PA_Task_Transactionable implements PA_Task.I_StateListener
{

	final BleOp m_bleOp;

	private Boolean m_authRetryValue_onExecute = null;
	private boolean m_triedToKickOffBond = false;

	private BleCharacteristic m_filteredCharacteristic;
	private List<BleCharacteristic> m_characteristicList;


	PA_Task_ReadOrWrite(IBleDevice device, BleOp bleOp, boolean requiresBonding, IBleTransaction txn_nullable, PE_TaskPriority priority)
	{
		super(device, txn_nullable, requiresBonding, priority);

		m_bleOp = bleOp;

		m_characteristicList = new ArrayList<>();
	}

	
	protected abstract ReadWriteEvent newReadWriteEvent(Status status, int gattStatus, Target target, BleOp bleOp);
	protected abstract void executeReadOrWrite();


	protected UUID getActualDescUuid(UUID descUuid)
	{
		return descUuid != null ? descUuid : m_bleOp.getDescriptorFilter() != null ? m_bleOp.getDescriptorFilter().descriptorUuid() : null;
	}

	protected Target getDefaultTarget()
	{
		return Target.CHARACTERISTIC;
	}
	
	protected void fail(Status status, int gattStatus, Target target, UUID charUuid, UUID descUuid)
	{
		this.fail();

		getDevice().invokeReadWriteCallback(m_bleOp.getReadWriteListener(), newReadWriteEvent(status, gattStatus, target, m_bleOp));
	}

	@Override protected void onNotExecutable()
	{
		super.onNotExecutable();

		final ReadWriteEvent event = newReadWriteEvent(Status.NOT_CONNECTED, BleStatuses.GATT_STATUS_NOT_APPLICABLE, getDefaultTarget(), m_bleOp);

		getDevice().invokeReadWriteCallback(m_bleOp.getReadWriteListener(), event);
	}
	
	protected boolean acknowledgeCallback(int status)
	{
		 //--- DRK > As of now, on the nexus 7, if a write requires authentication, it kicks off a bonding process
		 //---		 and we don't get a callback for the write (android bug), so we let this write task be interruptible
		 //---		 by an implicit bond task. If on other devices we *do* get a callback, we ignore it so that this
		 //---		 library's logic always follows the lowest common denominator that is the nexus 7.
		//---		NOTE: Also happens with tab 4, same thing as nexus 7.
		 if( status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION || status == BleStatuses.GATT_AUTH_FAIL )
		 {
			 return false;
		 }
		 
		 return true;
	}

	protected BleCharacteristic getFilteredCharacteristic()
	{
		return m_filteredCharacteristic;
	}
	
	private void checkIfBondingKickedOff()
	{
		if( getState() == PE_TaskState.EXECUTING )
		{
			if( m_triedToKickOffBond == false )
			{
				final Boolean authRetryValue_now = getAuthRetryValue();
				
				if( m_authRetryValue_onExecute != null && authRetryValue_now != null )
				{
					if( m_authRetryValue_onExecute == false && authRetryValue_now == true )
					{
						m_triedToKickOffBond = true;
						
						getManager().getLogger().i("Kicked off bond!");
					}
				}
			}
		}
	}
	
	private boolean triedToKickOffBond()
	{
		return m_triedToKickOffBond;
	}
	
	@Override public void execute()
	{
		m_authRetryValue_onExecute = getAuthRetryValue();

		if (m_bleOp.getDescriptorFilter() == null)
		{
			executeReadOrWrite();
		}
		else
		{
			// A descriptor filter has been set, so here we need to determine which characteristic to perform the read/write on.
			// Get the list of characteristics in the service of the Uuid provided
			List<BleCharacteristic> charList = getDevice().getNativeCharacteristics_List(getServiceUuid());
			if (charList != null)
			{
				// Now update the list of characteristics that match the given char Uuid
				int size = charList.size();
				for (int i = 0; i < size; i++)
				{
					BleCharacteristic ch = charList.get(i);
					if (ch.getUuid().equals(getCharUuid()))
					{
						m_characteristicList.add(ch);
					}
				}
				size = m_characteristicList.size();
				if (size == 0)
				{
					fail(ReadWriteListener.Status.NO_MATCHING_TARGET, BleStatuses.GATT_STATUS_NOT_APPLICABLE, ReadWriteListener.Target.CHARACTERISTIC, getCharUuid(), m_bleOp.getDescriptorFilter().descriptorUuid());
					return;
				}

				final UUID descUuid = m_bleOp.getDescriptorFilter().descriptorUuid();

				if (descUuid != null)
				{
					// Find the descriptor matching the given desc Uuid, and read it (the rest of the logic is handled in onDescriptorReadCallback)
					final BleCharacteristic m_char = m_characteristicList.get(0);
					final BleDescriptor m_desc = m_char.getDescriptor(descUuid);
					if (m_desc == null || m_desc.isNull())
					{
						fail(ReadWriteListener.Status.NO_MATCHING_TARGET, BleStatuses.GATT_STATUS_NOT_APPLICABLE, ReadWriteListener.Target.CHARACTERISTIC, getCharUuid(), m_bleOp.getDescriptorFilter().descriptorUuid());
					}
					else
					{
						if (false == getDevice().nativeManager().readDescriptor(m_desc))
						{
							fail(ReadWriteListener.Status.FAILED_TO_SEND_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, ReadWriteListener.Target.DESCRIPTOR, m_char.getUuid(), m_desc.getUuid());
						}
						else
						{
							// Wait for the descriptor read callback
						}
					}
				}
				else
				{
					// If the descriptor Uuid is null, then we'll forward all chars we find and let the app decide if it's the right one or not
					for (BleCharacteristic ch : m_characteristicList)
					{
						final DescriptorFilter.DescriptorEvent event = P_Bridge_User.newDescriptorEvent(ch.getService().getService(), ch.getCharacteristic(), null, P_Const.EMPTY_FUTURE_DATA);
						final DescriptorFilter.Please please = m_bleOp.getDescriptorFilter().onEvent(event);
						if (P_Bridge_User.accepted(please))
						{
							m_filteredCharacteristic = ch;
							if (!m_bleOp.isServiceUuidValid())
								m_bleOp.setServiceUUID(m_filteredCharacteristic.getService().getUuid());
							executeReadOrWrite();
							return;
						}
					}

					// If we got here, we couldn't find a valid char to write to
					fail(ReadWriteListener.Status.NO_MATCHING_TARGET, BleStatuses.GATT_STATUS_NOT_APPLICABLE, ReadWriteListener.Target.CHARACTERISTIC, getCharUuid(), m_bleOp.getDescriptorFilter().descriptorUuid());
				}
			}
			else
			{
				fail(ReadWriteListener.Status.NO_MATCHING_TARGET, BleStatuses.GATT_STATUS_NOT_APPLICABLE, ReadWriteListener.Target.CHARACTERISTIC, getCharUuid(), m_bleOp.getDescriptorFilter().descriptorUuid());
			}
		}
	}
	
	@Override public void update(double timeStep)
	{
		if (getDevice().is(BleDeviceState.BLE_CONNECTED))
		{
			checkIfBondingKickedOff();
		}
	}
	
	private Boolean getAuthRetryValue()
	{
		return getDevice().nativeManager().getGattLayer().getAuthRetryValue();
	}
	
	@Override protected UUID getCharUuid()
	{
		return m_bleOp.getCharacteristicUuid();
	}

	protected UUID getServiceUuid()
	{
		return m_bleOp.getServiceUuid();
	}

	public boolean isFor(final BleCharacteristic characteristic)
	{
		return
				characteristic.getUuid().equals(getCharUuid()) &&
						characteristic.getService().getUuid().equals(getServiceUuid());
	}

	public boolean isFor(final BleDescriptor descriptor)
	{
		return descriptor.getUuid().equals(getDescUuid()) && isFor(descriptor.getCharacteristic());
	}
	
	protected boolean isForCharacteristic(UUID uuid)
	{
		return uuid.equals(getCharUuid());
	}
	
	@Override protected String getToStringAddition()
	{
		final String txn = getTxn() != null ? " txn!=null" : " txn==null";
		return getManager().getLogger().uuidName(getCharUuid()) + txn;
	}
	
	@Override public void onStateChange(PA_Task task, PE_TaskState state)
	{
		if( state == PE_TaskState.TIMED_OUT )
		{
			checkIfBondingKickedOff();
			
			if( triedToKickOffBond() )
			{
				getDevice().notifyOfPossibleImplicitBondingAttempt();
				getDevice().getBondManager().saveNeedsBondingIfDesired();
				
				getManager().getLogger().i("Kicked off bond and " + PE_TaskState.TIMED_OUT.name());
			}
		}
	}

	boolean descriptorMatches(BleDescriptor descriptor)
	{
		if (m_bleOp.getDescriptorFilter() == null)
		{
			return isFor(descriptor);
		}
		else
		{
			return descriptor.getUuid().equals(m_bleOp.getDescriptorFilter().descriptorUuid());
		}
	}

	void onDescriptorReadCallback(P_GattHolder gatt, BleDescriptor desc, byte[] value, int gattStatus)
	{
		if (m_bleOp.getDescriptorFilter() == null)
		{
			onDescriptorRead(gatt, desc.getDescriptor().getUuid(), value, gattStatus);
		}
		else
		{
			if (!m_characteristicList.contains(desc.getCharacteristic()))
			{
				return;
			}
			if( Utils.isSuccess(gattStatus))
			{
				final DescriptorFilter.DescriptorEvent event = P_Bridge_User.newDescriptorEvent(desc.getDescriptor().getCharacteristic().getService(), desc.getDescriptor().getCharacteristic(), desc.getDescriptor(), new PresentData(value));
				final DescriptorFilter.Please please = m_bleOp.getDescriptorFilter().onEvent(event);
				if (P_Bridge_User.accepted(please))
				{
					m_filteredCharacteristic = desc.getCharacteristic();
					executeReadOrWrite();
				}
				else
				{
					m_characteristicList.remove(desc.getCharacteristic());
					if (m_characteristicList.size() == 0)
					{
						fail(ReadWriteListener.Status.NO_MATCHING_TARGET, BleStatuses.GATT_STATUS_NOT_APPLICABLE, ReadWriteListener.Target.DESCRIPTOR, desc.getDescriptor().getCharacteristic().getUuid(), desc.getDescriptor().getUuid());
					}
					else
					{
						final BleCharacteristic ch = m_characteristicList.get(0);
						final BleDescriptor descr = ch.getDescriptor(m_bleOp.getDescriptorFilter().descriptorUuid());
						if (false == getDevice().nativeManager().readDescriptor(descr))
						{
							fail(ReadWriteListener.Status.FAILED_TO_SEND_OUT, BleStatuses.GATT_STATUS_NOT_APPLICABLE, ReadWriteListener.Target.DESCRIPTOR, ch.getUuid(), descr.getUuid());
						}
						else
						{
							// SUCCESS for now until the descriptor read comes back, and we can compare it to the given namespaceanddescription
						}
					}
				}
			}
			else
			{
				fail(ReadWriteListener.Status.REMOTE_GATT_FAILURE, gattStatus, getDefaultTarget(), getCharUuid(), getDescUuid());
			}
		}
	}


	protected void onDescriptorRead(P_GattHolder gatt, UUID descriptorUuid, byte[] value, int gattStatus)
	{
	}

	protected void onCharacteristicOrDescriptorRead(P_GattHolder gatt, UUID uuid, byte[] value, int gattStatus, ReadWriteListener.Type type)
	{
		getManager().ASSERT(getDevice().nativeManager().getGattLayer().equals(gatt), "");

//		if( false == this.isForCharacteristic(uuid) )  return;

		if( false == acknowledgeCallback(gattStatus) )  return;

		if( Utils.isSuccess(gattStatus) )
		{
			if( value != null )
			{
				if( value.length == 0 )
				{
					fail(Status.EMPTY_DATA, gattStatus, getDefaultTarget(), getCharUuid(), getDescUuid());
				}
				else
				{
					succeedRead(value, getDefaultTarget(), type);
				}
			}
			else
			{
				fail(Status.NULL_DATA, gattStatus, getDefaultTarget(), getCharUuid(), getDescUuid());

				getManager().uhOh(UhOhListener.UhOh.READ_RETURNED_NULL);
			}
		}
		else
		{
			fail(Status.REMOTE_GATT_FAILURE, gattStatus, getDefaultTarget(), getCharUuid(), getDescUuid());
		}
	}

	private ReadWriteEvent newSuccessReadWriteEvent(byte[] data, Target target, ReadWriteListener.Type type, UUID charUuid, UUID descUuid, DescriptorFilter descriptorFilter)
	{
		BleOp op = P_Bridge_User.createBleOp(getServiceUuid(), charUuid, descUuid, descriptorFilter, data, type);
		op.setData(new PresentData(data));
		return P_Bridge_User.newReadWriteEvent(getDevice().getBleDevice(), op, type, target, Status.SUCCESS, BluetoothGatt.GATT_SUCCESS, getTotalTime(), getTotalTimeExecuting(), /*solicited=*/true);
	}

	private void succeedRead(byte[] value, Target target, ReadWriteListener.Type type)
	{
		super.succeed();

		final ReadWriteEvent event = newSuccessReadWriteEvent(value, target, type, getCharUuid(), getDescUuid(), m_bleOp.getDescriptorFilter());
		getDevice().addReadTime(event.time_total().secs());

		getDevice().invokeReadWriteCallback(m_bleOp.getReadWriteListener(), event);
	}

	protected void succeedWrite()
	{
		super.succeed();

		final ReadWriteEvent event = newReadWriteEvent(Status.SUCCESS, BluetoothGatt.GATT_SUCCESS, getDefaultTarget(), m_bleOp);
		getDevice().addWriteTime(event.time_total().secs());
		getDevice().invokeReadWriteCallback(m_bleOp.getReadWriteListener(), event);
	}

	protected boolean write_earlyOut(final byte[] data_nullable)
	{
		if( data_nullable == null )
		{
			fail(Status.NULL_DATA, BleStatuses.GATT_STATUS_NOT_APPLICABLE, Target.CHARACTERISTIC, getCharUuid(), getDescUuid());

			return true;
		}
		else if( data_nullable.length == 0 )
		{
			fail(Status.EMPTY_DATA, BleStatuses.GATT_STATUS_NOT_APPLICABLE, Target.CHARACTERISTIC, getCharUuid(), getDescUuid());

			return true;
		}
		else
		{
			return false;
		}
	}
}
