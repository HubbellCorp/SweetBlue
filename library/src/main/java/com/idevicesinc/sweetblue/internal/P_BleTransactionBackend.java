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


import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleTransaction;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.ReadWriteListener;
import com.idevicesinc.sweetblue.utils.Utils;


/**
 * Backend class for BleTransactions.
 */
public final class P_BleTransactionBackend implements IBleTransaction
{

    private final double m_timeout;
    private double m_timeTracker;
    private boolean m_isRunning;
    private IBleDevice m_device = null;
    private PI_EndListener m_listener;
    private final Callback m_callback;


    P_BleTransactionBackend(Callback callback)
    {
        m_timeout = 0.0;
        m_callback = callback;
    }


    public void init(IBleDevice device, PI_EndListener listener )
    {
        if( m_device != null )
        {
            if( m_device != device )
            {
                throw new Error("Cannot currently reuse transactions across devices.");
            }
        }

        m_device = device;
        m_listener = listener;
    }

    /**
     * Returns the device this transaction is running on.
     */
    public IBleDevice getDevice()
    {
        return m_device;
    }

    /**
     * Returns whether the transaction is currently running.
     */
    public boolean isRunning()
    {
        return m_isRunning;
    }

    public void start_internal()
    {
        m_isRunning = true;
        m_timeTracker = 0.0;

        start(m_device.getBleDevice());
    }

    public final void cancel()
    {
        end(BleTransaction.EndReason.CANCELLED, P_Bridge_User.newReadWriteEventNULL(m_device.getBleDevice()));
    }

    public final boolean fail()
    {
        final ReadWriteListener.ReadWriteEvent failReason = m_device.getTxnManager().m_failReason;

        return this.end(BleTransaction.EndReason.FAILED, failReason);
    }

    public final boolean succeed()
    {
        return end(BleTransaction.EndReason.SUCCEEDED, P_Bridge_User.newReadWriteEventNULL(m_device.getBleDevice()));
    }

    public void update_internal(double timeStep)
    {
        m_timeTracker += timeStep;

        if( m_timeout > 0.0 )
        {
            if( m_timeTracker >= m_timeout )
            {
            }
        }

        if (m_callback != null)
            m_callback.updateTxn(timeStep);
    }

    public double getTime()
    {
        return m_timeTracker;
    }

    public BleTransaction.Atomicity getAtomicity()
    {
        if (m_callback != null)
            return m_callback.getAtomicity();
        return BleTransaction.Atomicity.NOT_ATOMIC;
    }

    /**
     * Implement this method to kick off your transaction. Usually you kick off some reads/writes inside
     * your override and call {@link #succeed()} or {@link #fail()} depending on how things went.
     */
    void start(BleDevice device)
    {
        if (m_callback != null)
            m_callback.startTxn(device);
    }

    /**
     * Called when a transaction ends, either due to the transaction itself finishing itself
     * through {@link #fail()} or {@link #succeed()}, or from the library implicitly ending
     * the transaction, for example if {@link #getDevice()} becomes {@link BleDeviceState#BLE_DISCONNECTED}.
     *
     * Override this method to wrap up any loose ends or notify UI or what have you.
     */
    void onEnd(BleDevice device, BleTransaction.EndReason reason)
    {
        if (m_callback != null)
            m_callback.onEndTxn(device, reason);
    }





    private boolean end(final BleTransaction.EndReason reason, final ReadWriteListener.ReadWriteEvent failReason)
    {
        if( !m_isRunning )
        {
            //--- DRK > Can be due to a legitimate race condition, so warning might be a little much.
//				m_device.getIManager().getLogger().w("Transaction is already ended!");

            return false;
        }

        m_device.getIManager().getLogger().i("transaction " + reason.name());

        m_isRunning = false;

        if( m_listener != null )
        {
            m_listener.onTransactionEnd(this, reason, failReason);
        }

        if( m_device.getIManager().getConfigClone().postCallbacksToMainThread && !Utils.isOnMainThread() )
        {
            m_device.getIManager().getPostManager().postToMain(() -> onEnd(m_device.getBleDevice(), reason));
        }
        else
        {
            onEnd(m_device.getBleDevice(), reason);
        }

        return true;
    }
}
