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

import static com.idevicesinc.sweetblue.BleDeviceState.AUTHENTICATED;
import static com.idevicesinc.sweetblue.BleDeviceState.AUTHENTICATING;
import static com.idevicesinc.sweetblue.BleDeviceState.INITIALIZING;
import static com.idevicesinc.sweetblue.BleDeviceState.PERFORMING_OTA;

import android.bluetooth.BluetoothGatt;

import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleStatuses;
import com.idevicesinc.sweetblue.BleTransaction;
import com.idevicesinc.sweetblue.DeviceReconnectFilter.Status;
import com.idevicesinc.sweetblue.BleTransaction.EndReason;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.ReadWriteListener;
import com.idevicesinc.sweetblue.utils.CodeHelper;

final class P_TransactionManager
{
    final PI_EndListener m_txnEndListener = P_TransactionManager.this::transactionEnded;

    private final IBleDevice m_device;

    IBleTransaction m_authTxn;
    IBleTransaction m_initTxn;
    IBleTransaction m_otaTxn;
    IBleTransaction m_anonTxn;

    IBleTransaction m_current;

    ReadWriteListener.ReadWriteEvent m_failReason;

    P_TransactionManager(IBleDevice device)
    {
        m_device = device;

        if (!m_device.isNull())
            resetReadWriteResult();
    }

    void start(final IBleTransaction txn)
    {
        if (m_current != null)
        {
            m_device.getIManager().ASSERT(false, "Old: " + m_current.getClass().getSimpleName() + " New: " + txn.getClass().getSimpleName());
        }

        m_current = txn;

        start_common(m_device, txn);
    }

    static void start_common(final IBleDevice device, final IBleTransaction txn)
    {
        if (txn.getAtomicity() == BleTransaction.Atomicity.QUEUE_ATOMIC)
        {
            device.getIManager().getTaskManager().add(new P_Task_TxnLock(device, txn));
        }

        txn.start_internal();
    }

    IBleTransaction getCurrent()
    {
        return m_current;
    }

    void clearQueueLock()
    {
        m_device.getIManager().getPostManager().runOrPostToUpdateThread(this::clearQueueLock_updateThread);
    }

    private void clearQueueLock_updateThread()
    {
        if (!m_device.getIManager().getTaskManager().succeed(P_Task_TxnLock.class, m_device))
        {
            m_device.getIManager().getTaskManager().clearQueueOf(P_Task_TxnLock.class, m_device, -1);
        }
    }

    void cancelOtaTransaction()
    {
        if (m_otaTxn != null && m_otaTxn.isRunning())
        {
            m_otaTxn.cancel();
        }
    }

    void cancelAllTransactions()
    {
        if (m_authTxn != null && m_authTxn.isRunning())
        {
            m_authTxn.cancel();
        }

        if (m_initTxn != null && m_initTxn.isRunning())
        {
            m_initTxn.cancel();
        }

        cancelOtaTransaction();

        if (m_anonTxn != null && m_anonTxn.isRunning())
        {
            m_anonTxn.cancel();
            m_anonTxn = null;
        }

        IBleTransaction txn = m_current;
        if (txn != null)
        {
            m_device.getIManager().ASSERT(false, "Expected current transaction to be null.");

            txn.cancel();
            m_current = null;
        }
    }

    void update(double timeStep)
    {
        if (m_authTxn != null && m_authTxn.isRunning())
        {
            m_authTxn.update_internal(timeStep);
        }

        if (m_initTxn != null && m_initTxn.isRunning())
        {
            m_initTxn.update_internal(timeStep);
        }

        if (m_otaTxn != null && m_otaTxn.isRunning())
        {
            m_otaTxn.update_internal(timeStep);
        }

        if (m_anonTxn != null && m_anonTxn.isRunning())
        {
            m_anonTxn.update_internal(timeStep);
        }
    }

    void onConnect(IBleTransaction authenticationTxn, IBleTransaction initTxn)
    {
        m_authTxn = authenticationTxn;
        m_initTxn = initTxn;

        if (m_authTxn != null)
        {
            m_authTxn.init(m_device, m_txnEndListener);
        }

        if (m_initTxn != null)
        {
            m_initTxn.init(m_device, m_txnEndListener);
        }
    }

    private void resetReadWriteResult()
    {
        m_failReason = P_Bridge_User.newReadWriteEventNULL(m_device.getBleDevice());
    }

    private void transactionEnded(IBleTransaction txn, EndReason reason, ReadWriteListener.ReadWriteEvent txnFailReason)
    {
        clearQueueLock();

        m_current = null;

        if (!m_device.is_internal(BleDeviceState.BLE_CONNECTED))
        {
            if (reason == EndReason.CANCELLED)
            {
                return;
            }
            else if (reason == EndReason.SUCCEEDED || reason == EndReason.FAILED)
            {
                m_device.getIManager().ASSERT(false, "nativelyConnected=" + CodeHelper.gattConn(m_device.getNativeManager().getConnectionState(), m_device.getIManager().getLogger().isEnabled()) + " gatt==" + m_device.getNativeManager().getGattLayer().getGatt());

                return;
            }
        }

        if (txn == m_authTxn)
        {
            if (reason == EndReason.SUCCEEDED)
            {
                if (m_initTxn != null)
                {
                    m_device.getStateTracker().update
                            (
                                    PA_StateTracker.E_Intent.INTENTIONAL,
                                    BleStatuses.GATT_STATUS_NOT_APPLICABLE,
                                    AUTHENTICATING, false, AUTHENTICATED, true, INITIALIZING, true
                            );

                    start(m_initTxn);

                    m_device.getPollManager().enableNotifications_assumesWeAreConnected();
                }
                else
                {
                    m_device.onFullyInitialized(BleStatuses.GATT_STATUS_NOT_APPLICABLE);
                }
            }
            else
            {
                final P_DisconnectReason disconnectReason = new P_DisconnectReason(BleStatuses.GATT_STATUS_NOT_APPLICABLE);
                disconnectReason.setConnectFailReason(Status.AUTHENTICATION_FAILED)
                        .setTxnFailReason(txnFailReason);
                m_device.disconnectWithReason(disconnectReason);
            }
        }
        else if (txn == m_initTxn)
        {
            if (reason == EndReason.SUCCEEDED)
            {
                m_device.onFullyInitialized(BleStatuses.GATT_STATUS_NOT_APPLICABLE);
            }
            else
            {
                final P_DisconnectReason disconnectReason = new P_DisconnectReason(BleStatuses.GATT_STATUS_NOT_APPLICABLE);
                disconnectReason.setConnectFailReason(Status.INITIALIZATION_FAILED)
                        .setTxnFailReason(txnFailReason);

                m_device.disconnectWithReason(disconnectReason);
            }
        }
        else if (txn == m_device.getTxnManager().m_otaTxn)
        {
//				m_device.m_txnMngr.clearFirmwareUpdateTxn();
            PA_StateTracker.E_Intent intent = PA_StateTracker.E_Intent.UNINTENTIONAL;
            m_device.getStateTracker().remove(PERFORMING_OTA, intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE);

            //--- DRK > As of now don't care whether this succeeded or failed.
            if (reason == EndReason.SUCCEEDED)
            {
            }
            else
            {
            }
        }
        else if (txn == m_anonTxn)
        {
            m_anonTxn = null;
        }
    }

    void onReadWriteResult(ReadWriteListener.ReadWriteEvent result)
    {
        resetReadWriteResult();

        if (!result.wasSuccess())
        {
            if (m_device.isAny_internal(AUTHENTICATING, INITIALIZING))
            {
                m_failReason = result;
            }
        }
    }

    void onReadWriteResultCallbacksCalled()
    {
        resetReadWriteResult();
    }

    void startOta(BleTransaction.Ota txn)
    {
//			m_device.getIManager().ASSERT(m_otaTxn == null);

        m_otaTxn = P_Bridge_User.getIBleTransaction(txn);
        m_otaTxn.init(m_device, m_txnEndListener);

        m_device.getStateTracker().append(PERFORMING_OTA, PA_StateTracker.E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE);

        start(m_otaTxn);
    }

    void performAnonTransaction(BleTransaction txn)
    {
        m_anonTxn = P_Bridge_User.getIBleTransaction(txn);

        m_anonTxn.init(m_device, m_txnEndListener);
        start(m_anonTxn);
    }

    void runAuthOrInitTxnIfNeeded(final int gattStatus, Object... extraFlags)
    {
        PA_StateTracker.E_Intent intent = m_device.lastConnectDisconnectIntent();
        if (m_authTxn == null && m_initTxn == null)
        {
            m_device.getPollManager().enableNotifications_assumesWeAreConnected();

            m_device.onFullyInitialized(gattStatus, extraFlags);
        }
        else if (m_authTxn != null)
        {
            m_device.getStateTracker().update(intent, BluetoothGatt.GATT_SUCCESS, extraFlags, AUTHENTICATING, true);

            start(m_authTxn);
        }
        else if (m_initTxn != null)
        {
            m_device.getPollManager().enableNotifications_assumesWeAreConnected();

            m_device.getStateTracker().update(intent, BluetoothGatt.GATT_SUCCESS, extraFlags, AUTHENTICATED, true, INITIALIZING, true);

            start(m_initTxn);
        }
    }
}
