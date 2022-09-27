package com.idevicesinc.sweetblue.rx;


import com.idevicesinc.sweetblue.BleConnectionPriority;
import com.idevicesinc.sweetblue.BleDescriptorRead;
import com.idevicesinc.sweetblue.BleDescriptorWrite;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleNotify;
import com.idevicesinc.sweetblue.BleRead;
import com.idevicesinc.sweetblue.BleTransaction;
import com.idevicesinc.sweetblue.BleWrite;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.P_ITransaction;
import com.idevicesinc.sweetblue.ReadWriteListener;
import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.internal.IBleTransaction;
import com.idevicesinc.sweetblue.utils.Event;
import com.idevicesinc.sweetblue.utils.Phy;
import java.util.List;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;


public abstract class RxBleTransaction implements P_ITransaction
{

    public abstract static class RxAuth extends RxBleTransaction
    {
        public RxAuth()
        {
            super();
        }

        public final Type getTransactionType()
        {
            return Type.AUTH;
        }
    }

    public abstract static class RxInit extends RxBleTransaction
    {
        public RxInit()
        {
            super();
        }

        public final Type getTransactionType()
        {
            return Type.INIT;
        }
    }

    public abstract static class RxOta extends RxBleTransaction
    {
        public RxOta()
        {
            super();
        }

        public final Type getTransactionType()
        {
            return Type.OTA;
        }
    }


    private final BleTransaction m_wrappedTxn;


    protected abstract void start();


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
        public void onEndTxn(BleDevice device, BleTransaction.EndReason reason)
        {
            onEnd(reason);
        }

        @Override
        public BleTransaction.Atomicity getAtomicity()
        {
            return RxBleTransaction.this.getAtomicity();
        }
    };

    // This forwards calls into BleTransaction from the internal library into this RxBleTransaction
    private P_Bridge_User.TransactionHolder m_holder = new P_Bridge_User.TransactionHolder()
    {
        @Override
        public void start()
        {
            RxBleTransaction.this.start();
        }

        @Override
        public void update(double timeStep)
        {
            RxBleTransaction.this.update(timeStep);
        }

        @Override
        public void onEnd(BleTransaction.EndReason endReason)
        {
            RxBleTransaction.this.onEnd(endReason);
        }

        @Override
        public BleTransaction.Atomicity getAtomicity()
        {
            return RxBleTransaction.this.getAtomicity();
        }
    };


    @Override
    public Type getTransactionType()
    {
        return Type.OTHER;
    }

    public RxBleTransaction()
    {
        m_transactionImpl = IBleTransaction.DEFAULT_FACTORY.newInstance(m_callback);

        // At the internal layer, it doesn't care that the txn is an "rx" txn or not, they all look the same, so this is here
        // to wrap the rx txn into a BleTransaction.
        m_wrappedTxn = P_Bridge_User.newTransaction(m_transactionImpl, getTransactionType(), m_holder);
    }

    <T extends BleTransaction> T getWrappedTxn()
    {
        return (T) m_wrappedTxn;
    }



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
    protected void onEnd(BleTransaction.EndReason reason) {}

    /**
     * Default is {@link Boolean#FALSE}. Optionally override if you want your transaction's reads/writes to execute "atomically".
     * This means that if you're connected to multiple devices only the reads/writes of this transaction's device
     * will be executed until this transaction is finished.
     */
    protected BleTransaction.Atomicity getAtomicity()
    {
        try
        {
            return getDevice().getConfig().defaultTransactionAtomicity;
        }
        catch (Exception e)
        {
            return BleTransaction.Atomicity.NOT_ATOMIC;
        }
    }

    /**
     * Returns the device this transaction is running on.
     */
    public RxBleDevice getDevice()
    {
        return RxBleManager.getOrCreateDevice(m_transactionImpl.getDevice().getBleDevice());
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
     * {@link ReadWriteListener#onEvent(Event)} when {@link com.idevicesinc.sweetblue.ReadWriteListener.Status} is something other
     * than {@link com.idevicesinc.sweetblue.ReadWriteListener.Status#SUCCESS}. If you do so,
     * {@link com.idevicesinc.sweetblue.DeviceReconnectFilter.ConnectFailEvent#txnFailReason()} will be set.
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
     * Forwards to {@link RxBleDevice#read(BleRead)}
     */
    public final Single<RxReadWriteEvent> read(final BleRead read)
    {
        return getDevice().read_private(read, m_transactionImpl);
    }

    /**
     * Forwards to {@link BleDevice#readMany(BleRead[])}
     */
    public final Observable<RxReadWriteEvent> readMany(final BleRead[] bleReads)
    {
        return getDevice().readMany_private(bleReads, m_transactionImpl);
    }

    /**
     * Forwards to {@link BleDevice#readMany(Iterable)}
     */
    public final Observable<RxReadWriteEvent> readMany(final Iterable<BleRead> bleReads)
    {
        return getDevice().readMany_private(bleReads, m_transactionImpl);
    }

    /**
     * Forwards to {@link RxBleDevice#enableNotifies(List)}
     */
    public final Observable<RxNotificationEvent> enableNotifies(final List<BleNotify> notifies)
    {
        return getDevice().enableNotifies_private(notifies, m_transactionImpl);
    }

    /**
     * Forwards to {@link RxBleDevice#enableNotify(BleNotify)}
     */
    public final Single<RxNotificationEvent> enableNotify(BleNotify notify)
    {
        return getDevice().enableNotify_private(notify, m_transactionImpl);
    }

    /**
     * Forwards to {@link RxBleDevice#disableNotify(BleNotify)}
     */
    public final Single<RxNotificationEvent> disableNotify(BleNotify notify)
    {
        return getDevice().disableNotify_private(notify, m_transactionImpl);
    }

    /**
     * Forwards to {@link RxBleDevice#disableNotifies(List)}
     */
    public final Observable<RxNotificationEvent> disableNotifies(final List<BleNotify> notifies)
    {
        return getDevice().disableNotifies_private(notifies, m_transactionImpl);
    }

    /**
     * Forwards to {@link RxBleDevice#write(BleWrite)}
     */
    public final Single<RxReadWriteEvent> write(final BleWrite write)
    {
        return getDevice().write_private(write, m_transactionImpl);
    }

    /**
     * Forwards to {@link RxBleDevice#write(BleDescriptorWrite)}
     */
    public final Single<RxReadWriteEvent> write(BleDescriptorWrite write)
    {
        return getDevice().write_private(write, m_transactionImpl);
    }

    /**
     * Forwards to {@link RxBleDevice#read(BleDescriptorRead)}
     */
    public final Single<RxReadWriteEvent> read(final BleDescriptorRead read)
    {
        return getDevice().read_private(read, m_transactionImpl);
    }

    /**
     * Forwards to {@link RxBleDevice#readRssi()}
     */
    public final Single<RxReadWriteEvent> readRssi()
    {
        return getDevice().readRssi_private(m_transactionImpl);
    }

    /**
     * Forwards to {@link RxBleDevice#setConnectionPriority(BleConnectionPriority)}
     */
    public final Single<RxReadWriteEvent> setConnectionPriority(final BleConnectionPriority connectionPriority)
    {
        return getDevice().setConnectionPriority_private(connectionPriority, m_transactionImpl);
    }

    /**
     * Forwards to {@link RxBleDevice#negotiateMtuToDefault()}
     */
    public final @Nullable(Nullable.Prevalence.NEVER) Single<RxReadWriteEvent> setMtuToDefault()
    {
        return getDevice().negotiateMtuToDefault_private(m_transactionImpl);
    }

    /**
     * Forwards to {@link RxBleDevice#negotiateMtu(int)}
     */
    public final Single<RxReadWriteEvent> setMtu(final int mtu)
    {
        return getDevice().negotiateMtu_private(mtu, m_transactionImpl);
    }

    /**
     * Forwards to {@link RxBleDevice#setPhyOptions(Phy)}
     */
    public final Single<RxReadWriteEvent> setPhyOptions(Phy phyOptions)
    {
        return getDevice().setPhyOptions_private(phyOptions, m_transactionImpl);
    }

    /**
     * Forwards to {@link RxBleDevice#readPhyOptions()}
     */
    public final Single<RxReadWriteEvent> readPhyOptions()
    {
        return getDevice().readPhyOptions(m_transactionImpl);
    }

}
