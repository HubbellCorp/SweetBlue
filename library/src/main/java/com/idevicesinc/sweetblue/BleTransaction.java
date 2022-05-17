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

package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.ReadWriteListener.Status;
import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.internal.IBleTransaction;
import com.idevicesinc.sweetblue.utils.Event;
import com.idevicesinc.sweetblue.utils.Phy;
import com.idevicesinc.sweetblue.utils.Utils;


/**
 * Abstract base class for transactions passed to various methods of {@link BleDevice}. Transactions provide a convenient way
 * to encapsulate a series of reads and writes for things like authentication handshakes, firmware updates, etc. You optionally
 * provide subclass instances to the various overloads of {@link BleDevice#connect()}. Normally in your {@link #start()}
 * method you then do some reads/writes and call {@link #succeed()} or {@link #fail()} depending on the {@link Status} returned.
 * <br><br>
 * NOTE: Nested subclasses here are only meant for tagging to enforce type-correctness and don't yet provide any differing contracts or implementations.
 * 
 * @see BleDevice#performOta(BleTransaction.Ota)
 * @see BleDevice#connect(BleTransaction.Auth)
 * @see BleDevice#connect(BleTransaction.Init)
 * @see BleDevice#connect(BleTransaction.Auth, BleTransaction.Init)
 * @see BleDevice#performTransaction(BleTransaction)
 */
public abstract class BleTransaction implements P_ITransaction
{
	/**
	 * Tagging subclass to force type-discrepancy for various {@link BleDevice#connect()} overloads.
	 */
	public abstract static class Init extends BleTransaction {

		public Init()
		{
		}

		Init(IBleTransaction txn)
		{
			super(txn);
		}

		public final Type getTransactionType()
		{
			return Type.INIT;
		}
	}
	
	/**
	 * Tagging subclass to force type-discrepancy for various {@link BleDevice#connect()} overloads.
	 */
	public abstract static class Auth extends BleTransaction {

		public Auth()
		{
		}

		Auth(IBleTransaction txn)
		{
			super(txn);
		}

		public final Type getTransactionType()
		{
			return Type.AUTH;
		}
	}
	
	/**
	 * Tagging subclass to force type-correctness for {@link BleDevice#performOta(BleTransaction.Ota)}.
	 */
	public abstract static class Ota extends BleTransaction {

		public Ota()
		{
		}

		Ota(IBleTransaction txn)
		{
			super(txn);
		}

		public final Type getTransactionType()
		{
			return Type.OTA;
		}

		@Override
		public boolean succeed()
		{
			if (getDevice().getConfig().clearGattOnOtaSuccess)
				Utils.refreshGatt(getDevice().getNativeGatt());
			return super.succeed();
		}
	}

	@Override
	public Type getTransactionType()
	{
		return Type.OTHER;
	}

	/**
	 * Values are passed to {@link BleTransaction#onEnd(EndReason)}.
	 */
	public enum EndReason
	{
		/**
		 * {@link BleTransaction#succeed()} was called.
		 */
		SUCCEEDED,
		
		/**
		 * The transaction's {@link BleDevice} became {@link BleDeviceState#BLE_DISCONNECTED}
		 * or/and {@link BleManager} went {@link BleManagerState#OFF}.
		 */
		CANCELLED,
		
		/**
		 * {@link BleTransaction#fail()} was called.
		 */
		FAILED
	}

	public enum Atomicity
	{
		/**
		 * The transaction is not atomic (it will not block any other tasks from running)
		 */
		NOT_ATOMIC,

		/**
		 * The transaction will block other transactionable tasks from running on the same device until it finishes
		 */
		DEVICE_ATOMIC,

		/**
		 * The transaction will block the entire queue from running other tasks until it finishes
		 */
		QUEUE_ATOMIC
	};

	private final IBleTransaction m_transactionImpl;
	private final IBleTransaction.Callback m_callback = new IBleTransaction.Callback()
	{
		@Override
		public void updateTxn(double timeStep)
		{
			update(timeStep);
		}

		@Override
		public void startTxn(BleDevice device)
		{
			// remove device from callback
			start();
		}

		@Override
		public void onEndTxn(BleDevice device, EndReason reason)
		{
			onEnd(reason);
		}

		@Override
		public Atomicity getAtomicity()
		{
			return BleTransaction.this.getAtomicity();
		}
	};

	
	public BleTransaction()
	{
		m_transactionImpl = IBleTransaction.DEFAULT_FACTORY.newInstance(m_callback);
	}

	BleTransaction(IBleTransaction impl) {
		m_transactionImpl = impl;
	}


	IBleTransaction getIBleTransaction()
	{
		return m_transactionImpl;
	}


	/**
	 * Implement this method to kick off your transaction. Usually you kick off some reads/writes inside
	 * your override and call {@link #succeed()} or {@link #fail()} depending on how things went.
	 */
	protected abstract void start();

	/**
	 * Optional convenience method to override if you want to do periodic updates or time-based calculations.
	 */
	protected void update(double timeStep) {}

	/**
	 * Called when a transaction ends, either due to the transaction itself finishing itself
	 * through {@link #fail()} or {@link #succeed()}, or from the library implicitly ending
	 * the transaction, for example if {@link #getDevice()} becomes {@link BleDeviceState#BLE_DISCONNECTED}.
	 *
	 * Override this method to wrap up any loose ends or notify UI or what have you.
	 */
	protected void onEnd(EndReason reason) {}

	/**
	 * Default is {@link Boolean#FALSE}. Optionally override if you want your transaction's reads/writes to execute "atomically".
	 * This means that if you're connected to multiple devices only the reads/writes of this transaction's device
	 * will be executed until this transaction is finished.
	 */
	protected Atomicity getAtomicity()
	{
		try
		{
			return getDevice().getConfig().defaultTransactionAtomicity;
		}
		catch (Exception e)
		{
			return Atomicity.NOT_ATOMIC;
		}
	}

	/**
	 * Returns the device this transaction is running on.
	 */
	public BleDevice getDevice()
	{
		return m_transactionImpl.getDevice().getBleDevice();
	}

	/**
	 * Returns whether the transaction is currently running.
	 */
	public boolean isRunning()
	{
		return m_transactionImpl.isRunning();
	}

	public void cancel()
	{
		m_transactionImpl.cancel();
	}

	/**
	 * Call this from subclasses to indicate that the transaction has failed. Usually you call this in your
	 * {@link ReadWriteListener#onEvent(Event)} when {@link ReadWriteListener.Status} is something other than {@link ReadWriteListener.Status#SUCCESS}. If you do so,
	 * {@link DeviceReconnectFilter.ConnectFailEvent#txnFailReason()} will be set.
	 *
	 * @return <code>false</code> if the transaction wasn't running to begin with.
	 */
	public boolean fail()
	{
		return m_transactionImpl.fail();
	}

	/**
	 * Call this from subclasses to indicate that the transaction has succeeded.
	 *
	 * @return {@link Boolean#FALSE} if the transaction wasn't running to begin with.
	 */
	public boolean succeed()
	{
		return m_transactionImpl.succeed();
	}

	/**
	 * Returns the total time that this transaction has been running. You can use this in {@link #update(double)}
	 * for example to {@link #fail()} or {@link #succeed()} a transaction that has taken longer than a certain
	 * amount of time.
	 *
	 * @see BleTransaction#update(double)
	 */
	public double getTime()
	{
		return m_transactionImpl.getTime();
	}

	// Begin wrapper methods

	/**
	 * Forwards to {@link BleDevice#read(BleRead)}
	 */
	public final ReadWriteListener.ReadWriteEvent read(final BleRead read)
	{
		try
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(m_transactionImpl);
			return getDevice().read(read);
		}
		finally
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(null);
		}
	}

	/**
	 * Forwards to {@link BleDevice#readMany(BleRead[])}
	 */
	public final Void readMany(final BleRead[] bleReads)
	{
		try
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(m_transactionImpl);
			getDevice().readMany(bleReads);
		}
		finally
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(null);
		}
		return null;
	}

	/**
	 * Forwards to {@link BleDevice#readMany(Iterable)}
	 */
	public final Void readMany(final Iterable<BleRead> bleReads)
	{
		try
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(m_transactionImpl);
			getDevice().readMany(bleReads);
		}
		finally
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(null);
		}
		return null;
	}

	/**
	 * Forwards to {@link BleDevice#readBatteryLevel(ReadWriteListener)}
	 */
	public final ReadWriteListener.ReadWriteEvent readBatteryLevel(ReadWriteListener listener)
	{
		try
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(m_transactionImpl);
			return getDevice().readBatteryLevel(listener);
		}
		finally
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(null);
		}
	}

	/**
	 * Forwards to {@link BleDevice#enableNotifies(BleNotify[])}
	 */
	public final Void enableNotifies(BleNotify[] notifies)
	{
		try
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(m_transactionImpl);
			getDevice().enableNotifies(notifies);
		}
		finally
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(null);
		}
		return null;
	}

	/**
	 * Forwards to {@link BleDevice#enableNotifies(Iterable)}
	 */
	public final Void enableNotifies(final Iterable<BleNotify> notifies)
	{
		try
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(m_transactionImpl);
			getDevice().enableNotifies(notifies);
		}
		finally
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(null);
		}
		return null;
	}

	/**
	 * Forwards to {@link BleDevice#enableNotify(BleNotify)}
	 */
	public final ReadWriteListener.ReadWriteEvent enableNotify(BleNotify notify)
	{
		try
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(m_transactionImpl);
			return getDevice().enableNotify(notify);
		}
		finally
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(null);
		}
	}

	/**
	 * Forwards to {@link BleDevice#disableNotify(BleNotify)}
	 */
	public final ReadWriteListener.ReadWriteEvent disableNotify(BleNotify notify)
	{
		try
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(m_transactionImpl);
			return getDevice().disableNotify(notify);
		}
		finally
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(null);
		}
	}

	/**
	 * Forwards to {@link BleDevice#disableNotifies(BleNotify[])}
	 */
	public final Void disableNotifies(final BleNotify[] notifies)
	{
		try
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(m_transactionImpl);
			getDevice().disableNotifies(notifies);
		}
		finally
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(null);
		}
		return null;
	}

	/**
	 * Forwards to {@link BleDevice#disableNotifies(Iterable)}
	 */
	public final Void disableNotifies(final Iterable<BleNotify> notifies)
	{
		try
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(m_transactionImpl);
			getDevice().disableNotifies(notifies);
		}
		finally
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(null);
		}
		return null;
	}

	/**
	 * Forwards to {@link BleDevice#write(BleWrite)}
	 */
	public final ReadWriteListener.ReadWriteEvent write(final BleWrite write)
	{
		try
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(m_transactionImpl);
			return getDevice().write(write);
		}
		finally
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(null);
		}
	}

	/**
	 * Forwards to {@link BleDevice#write(BleWrite, ReadWriteListener)}
	 */
	public final ReadWriteListener.ReadWriteEvent write(BleWrite bleWrite, ReadWriteListener listener)
	{
		try
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(m_transactionImpl);
			return getDevice().write(bleWrite, listener);
		}
		finally
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(null);
		}
	}

	/**
	 * Forwards to {@link BleDevice#write(BleDescriptorWrite)}
	 */
	public final ReadWriteListener.ReadWriteEvent write(BleDescriptorWrite write)
	{
		try
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(m_transactionImpl);
			return getDevice().write(write);
		}
		finally
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(null);
		}
	}

	/**
	 * Forwards to {@link BleDevice#write(BleWrite, ReadWriteListener)}
	 */
	public final ReadWriteListener.ReadWriteEvent write(BleDescriptorWrite write, ReadWriteListener listener)
	{
		try
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(m_transactionImpl);
			return getDevice().write(write, listener);
		}
		finally
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(null);
		}
	}

	/**
	 * Forwards to {@link BleDevice#read(BleDescriptorRead)}
	 */
	public final ReadWriteListener.ReadWriteEvent read(final BleDescriptorRead read)
	{
		try
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(m_transactionImpl);
			return getDevice().read(read);
		}
		finally
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(null);
		}
	}

	/**
	 * Forwards to {@link BleDevice#readRssi()}
	 */
	public final ReadWriteListener.ReadWriteEvent readRssi()
	{
		try
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(m_transactionImpl);
			return getDevice().readRssi();
		}
		finally
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(null);
		}
	}

	/**
	 * Forwards to {@link BleDevice#readRssi(ReadWriteListener)}
	 */
	public final ReadWriteListener.ReadWriteEvent readRssi(final ReadWriteListener listener)
	{
		try
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(m_transactionImpl);
			return getDevice().readRssi(listener);
		}
		finally
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(null);
		}
	}

	/**
	 * Forwards to {@link BleDevice#setConnectionPriority(BleConnectionPriority)}
	 */
	public final ReadWriteListener.ReadWriteEvent setConnectionPriority(final BleConnectionPriority connectionPriority)
	{
		try
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(m_transactionImpl);
			return getDevice().setConnectionPriority(connectionPriority);
		}
		finally
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(null);
		}
	}

	/**
	 * Forwards to {@link BleDevice#setConnectionPriority(BleConnectionPriority, ReadWriteListener)}
	 */
	public final ReadWriteListener.ReadWriteEvent setConnectionPriority(final BleConnectionPriority connectionPriority, final ReadWriteListener listener)
	{
		try
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(m_transactionImpl);
			return getDevice().setConnectionPriority(connectionPriority);
		}
		finally
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(null);
		}
	}

	/**
	 * Forwards to {@link BleDevice#negotiateMtuToDefault()}
	 */
	public final @Nullable(Nullable.Prevalence.NEVER) ReadWriteListener.ReadWriteEvent negotiateMtuToDefault()
	{
		try
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(m_transactionImpl);
			return getDevice().negotiateMtuToDefault();
		}
		finally
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(null);
		}
	}

	/**
	 * Forwards to {@link BleDevice#negotiateMtuToDefault(ReadWriteListener)}
	 */
	public final ReadWriteListener.ReadWriteEvent negotiateMtuToDefault(final ReadWriteListener listener)
	{
		try
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(m_transactionImpl);
			return getDevice().negotiateMtuToDefault(listener);
		}
		finally
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(null);
		}
	}

	/**
	 * Forwards to {@link BleDevice#negotiateMtu(int)}
	 */
	public final ReadWriteListener.ReadWriteEvent negotiateMtu(final int mtu)
	{
		try
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(m_transactionImpl);
			return getDevice().negotiateMtu(mtu);
		}
		finally
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(null);
		}
	}

	/**
	 * Forwards to {@link BleDevice#negotiateMtu(int, ReadWriteListener)}
	 */
	public final ReadWriteListener.ReadWriteEvent negotiateMtu(final int mtu, final ReadWriteListener listener)
	{
		try
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(m_transactionImpl);
			return getDevice().negotiateMtu(mtu, listener);
		}
		finally
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(null);
		}
	}

	/**
	 * Forwards to {@link BleDevice#setPhyOptions(Phy)}
	 */
	public final ReadWriteListener.ReadWriteEvent setPhyOptions(Phy phyOptions)
	{
		try
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(m_transactionImpl);
			return getDevice().setPhyOptions(phyOptions);
		}
		finally
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(null);
		}
	}

	/**
	 * Forwards to {@link BleDevice#setPhyOptions(Phy, ReadWriteListener)}
	 */
	public final ReadWriteListener.ReadWriteEvent setPhyOptions(Phy phyOptions, ReadWriteListener listener)
	{
		try
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(m_transactionImpl);
			return getDevice().setPhyOptions(phyOptions, listener);
		}
		finally
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(null);
		}
	}

	/**
	 * Forwards to {@link BleDevice#readPhyOptions()}
	 */
	public final ReadWriteListener.ReadWriteEvent readPhyOptions()
	{
		try
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(m_transactionImpl);
			return getDevice().readPhyOptions();
		}
		finally
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(null);
		}
	}

	/**
	 * Forwards to {@link BleDevice#readPhyOptions(ReadWriteListener)}
	 */
	public final ReadWriteListener.ReadWriteEvent readPhyOptions(ReadWriteListener listener)
	{
		try
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(m_transactionImpl);
			return getDevice().readPhyOptions(listener);
		}
		finally
		{
			m_transactionImpl.getDevice().setThreadLocalTransaction(null);
		}
	}
}
