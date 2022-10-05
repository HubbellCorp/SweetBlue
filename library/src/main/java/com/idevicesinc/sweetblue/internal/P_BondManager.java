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


import android.bluetooth.BluetoothDevice;
import static com.idevicesinc.sweetblue.BleDeviceState.BONDED;
import static com.idevicesinc.sweetblue.BleDeviceState.BONDING;
import static com.idevicesinc.sweetblue.BleDeviceState.UNBONDED;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleStatuses;
import com.idevicesinc.sweetblue.BondFilter;
import com.idevicesinc.sweetblue.BondListener;
import com.idevicesinc.sweetblue.BondListener.Status;
import com.idevicesinc.sweetblue.BondRetryFilter;
import com.idevicesinc.sweetblue.DeviceReconnectFilter;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.UhOhListener.UhOh;
import com.idevicesinc.sweetblue.utils.CodeHelper;
import com.idevicesinc.sweetblue.utils.State;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.Utils_Config;
import java.util.UUID;


final class P_BondManager
{

    static final Object[] OVERRIDE_UNBONDED_STATES = {UNBONDED, true, BONDING, false, BONDED, false};
    static final Object[] OVERRIDE_BONDING_STATES = {UNBONDED, false, BONDING, true, BONDED, false};
    static final Object[] OVERRIDE_EMPTY_STATES = {};

    private final IBleDevice m_device;

    private int m_bondRetries = 0;

    private BondListener m_listener;
    private BondListener m_ephemeralListener;

    private boolean m_bondRequested;
    private final ConnectionBugHack mConnectBugHack;



    P_BondManager(IBleDevice device)
    {
        m_device = device;
        mConnectBugHack = new ConnectionBugHack(m_device);
    }


    public void setListener(BondListener listener_nullable)
    {
        m_listener = listener_nullable;
    }

    public void setEphemeralListener(BondListener listener)
    {
        m_ephemeralListener = listener;
    }

    public void resetBondRetryCount()
    {
        m_bondRetries = 0;
        m_bondRequested = false;
    }

    void onBondTaskStateChange(final PA_Task task, final PE_TaskState state)
    {
        final PA_StateTracker.E_Intent intent = task.isExplicit() ? PA_StateTracker.E_Intent.INTENTIONAL : PA_StateTracker.E_Intent.UNINTENTIONAL;

        if (task.getClass() == P_Task_Bond.class)
        {
            final P_Task_Bond bondTask = (P_Task_Bond) task;

            if (state.isEndingState())
            {
                if (state == PE_TaskState.SUCCEEDED || state == PE_TaskState.REDUNDANT)
                    onNativeBond(intent);
                else if (state == PE_TaskState.SOFTLY_CANCELLED)
                {
                }
                else
                {
                    final int failReason = bondTask.getFailReason();
                    final boolean wasDirect = bondTask.isDirect();
                    final BondListener.Status status;

                    if (state == PE_TaskState.TIMED_OUT)
                        status = Status.TIMED_OUT;
                    else if (state == PE_TaskState.FAILED_IMMEDIATELY)
                        status = Status.FAILED_IMMEDIATELY;
                    else
                        status = Status.FAILED_EVENTUALLY;

                    onNativeBondFailed(intent, status, failReason, wasDirect);
                }
            }
        }
        else if (task.getClass() == P_Task_Unbond.class)
        {
            if (state == PE_TaskState.SUCCEEDED || state == PE_TaskState.REDUNDANT)
                onNativeUnbond(intent);
            else
            {
                // not sure what to do here, if anything
            }
        }
    }

    void onNativeUnbond(final PA_StateTracker.E_Intent intent)
    {
        final boolean wasAlreadyUnbonded = m_device.is(UNBONDED);

        m_device.getStateTracker().update(intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BONDED, false, BONDING, false, UNBONDED, true);
        m_device.getStateTracker().updateBondState(UNBONDED);

        if (intent == PA_StateTracker.E_Intent.INTENTIONAL)
        {
            m_device.getIManager().getDiskOptionsManager().clearNeedsBonding(m_device.getMacAddress());
        }

        if (!wasAlreadyUnbonded)
            invokeCallback(Status.SUCCESS, BondListener.BondEvent.Type.UNBOND, BleStatuses.BOND_FAIL_REASON_NOT_APPLICABLE, intent.convert());
    }

    void onNativeBonding(final PA_StateTracker.E_Intent intent)
    {
        m_device.getStateTracker().update(intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BONDED, false, BONDING, true, UNBONDED, false);
        m_device.getStateTracker().updateBondState(BONDING);
    }

    void onNativeBond(final PA_StateTracker.E_Intent intent)
    {
        final boolean wasAlreadyBonded = m_device.is(BONDED);

        m_device.getStateTracker().update(intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BONDED, true, BONDING, false, UNBONDED, false);

        m_device.getStateTracker().updateBondState(BONDED);

        // TODO - To be removed in V3.1
        saveNeedsBondingIfDesired();

        if (!wasAlreadyBonded)
            invokeCallback(Status.SUCCESS, BondListener.BondEvent.Type.BOND, BleStatuses.BOND_FAIL_REASON_NOT_APPLICABLE, intent.convert());

        mConnectBugHack.checkAndHandleConnectionBug();
    }

    private boolean failConnection(final BondListener.Status status)
    {
        if (P_Bridge_User.canFailConnection(status))
        {
            if (m_device.is_internal(BleDeviceState.CONNECTING_OVERALL))
            {
                final boolean bondingFailFailsConnection = Utils_Config.bool(m_device.conf_device().bondingFailFailsConnection, m_device.conf_mngr().bondingFailFailsConnection);

                return bondingFailFailsConnection;
            }
        }

        return false;
    }

    Object[] getOverrideBondStatesForDisconnect(DeviceReconnectFilter.Status connectionFailReasonIfConnecting)
    {
        final Object[] overrideBondingStates;

        if (connectionFailReasonIfConnecting == DeviceReconnectFilter.Status.BONDING_FAILED)
            overrideBondingStates = OVERRIDE_UNBONDED_STATES;
        else
            overrideBondingStates = OVERRIDE_EMPTY_STATES;

        return overrideBondingStates;
    }

    void onNativeBondRequest()
    {
        m_bondRequested = true;
    }

    void onNativeBondFailed(final PA_StateTracker.E_Intent intent, final BondListener.Status status, final int failReason, final boolean wasDirect)
    {
        if (isNativelyBondingOrBonded())
        {
            //--- DRK > This is for cases where the bond task has timed out,
            //--- or otherwise failed without actually resetting internal bond state.
            m_device.unbond_justAddTheTask();
        }

        // Determine if we need to retry the bond.
        if (getFilter() != null)
        {
            final BondRetryFilter.RetryEvent event = P_Bridge_User.newBondRetryEvent(m_device.getBleDevice(), failReason, m_bondRetries, wasDirect, m_bondRequested);
            final BondRetryFilter.Please please = m_device.getIManager().getConfigClone().bondRetryFilter.onEvent(event);
            if (P_Bridge_User.shouldRetry(please))
            {
                m_device.getIManager().getLogger().w("Bond failed with failReason of " + CodeHelper.gattUnbondReason(failReason, m_device.getIManager().getLogger().isEnabled()) + ". Retrying bond...");
                m_bondRetries++;
                m_device.getStateTracker().update(intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BONDING, false, UNBONDED, true);
                m_device.bond_private(wasDirect, false, m_ephemeralListener);
                return;
            }
        }
        resetBondRetryCount();


        // TODO - Completely remove this section in V3.1
        // Not really sure why this was saving this here. It doesn't make much sense, especially if a device doesn't support bonding,
        // and fails, it will just keep failing, and we'll keep trying to bond. Moving this check into the successful bond method
//        if (m_device.is(BleDeviceState.BLE_CONNECTED) || m_device.is(BleDeviceState.BLE_CONNECTING))
//        {
//            saveNeedsBondingIfDesired();
//        }

        if (failConnection(status))
        {
            final boolean doingReconnect_shortTerm = m_device.is(BleDeviceState.RECONNECTING_SHORT_TERM);

            final P_DisconnectReason disconnectReason = new P_DisconnectReason(BleStatuses.GATT_STATUS_NOT_APPLICABLE, P_Bridge_User.bondTiming(status));
            disconnectReason.setConnectFailReason(DeviceReconnectFilter.Status.BONDING_FAILED)
                    .setBondFailReason(failReason)
                    .setTxnFailReason(P_Bridge_User.newReadWriteEventNULL(m_device.getBleDevice()));

            m_device.disconnectWithReason(disconnectReason);
        }
        else
            onNativeBondFailed_common(intent);

        invokeCallback(status, BondListener.BondEvent.Type.BOND, failReason, intent.convert());

        if (status == Status.TIMED_OUT)
            m_device.getIManager().uhOh(UhOh.BOND_TIMED_OUT);
    }

    private BondRetryFilter getFilter()
    {
        if (m_device.conf_device().bondRetryFilter != null)
            return m_device.conf_device().bondRetryFilter;
        else
            return m_device.conf_mngr().bondRetryFilter;
    }

    void saveNeedsBondingIfDesired()
    {
        final boolean tryBondingWhileDisconnected = Utils_Config.bool(m_device.conf_device().tryBondingWhileDisconnected, m_device.conf_mngr().tryBondingWhileDisconnected);

        if (tryBondingWhileDisconnected)
        {
            m_device.getIManager().getDiskOptionsManager().saveNeedsBonding(m_device.getMacAddress());
        }
    }

    private void onNativeBondFailed_common(final PA_StateTracker.E_Intent intent)
    {
        m_device.getStateTracker().update(intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BONDED, false, BONDING, false, UNBONDED, true);
    }

    boolean bondIfNeeded(final UUID charUuid, final BondFilter.CharacteristicEventType type)
    {
        final BondFilter bondFilter = m_device.conf_device().bondFilter != null ? m_device.conf_device().bondFilter : m_device.conf_mngr().bondFilter;

        if (bondFilter == null) return false;

        final BondFilter.CharacteristicEvent event = P_Bridge_User.newBondCharEvent(m_device.getBleDevice(), charUuid, type);

        final BondFilter.Please please = bondFilter.onEvent(event);

        return applyPlease_BondFilter(please);
    }

    boolean applyPlease_BondFilter(BondFilter.Please please_nullable)
    {
        if (please_nullable == null) return false;

        if (!Utils.isKitKat()) return false;

        final Boolean bond = P_Bridge_User.bond_private(please_nullable);

        if (bond == null) return false;

        if (bond)
            m_device.bond_private(/*isDirect=*/false, false, P_Bridge_User.bondListener(please_nullable));
        else
        {
            if (m_device.isAny(BONDING, BONDED) && m_device.getNativeManager().isNativelyBondedOrBonding())
                m_device.unbond_internal(PE_TaskPriority.HIGH, Status.CANCELLED_FROM_UNBOND);
        }

        return bond;
    }

    BondListener.BondEvent invokeCallback(Status status, BondListener.BondEvent.Type bondType, int failReason, State.ChangeIntent intent)
    {
        final BondListener.BondEvent event = P_Bridge_User.newBondEvent(m_device.getBleDevice(), bondType, status, failReason, intent);

        invokeCallback(event);

        return event;
    }

    void invokeCallback(final BondListener.BondEvent event)
    {
        if (m_ephemeralListener != null)
            m_device.getIManager().postEvent(m_ephemeralListener, event);

        // Since the listener had an event posted to it, we now clear out the ephemeral listener.
        m_ephemeralListener = null;

        if (m_listener != null)
            m_device.getIManager().postEvent(m_listener, event);

        final BondListener listener = m_device.getIManager().getDefaultBondListener();
        if (listener != null)
            m_device.getIManager().postEvent(listener, event);
    }

    Object[] getNativeBondingStateOverrides()
    {
        // Cut the 3 calls to jni layer down to one for efficiency (getting the native bond state calls a jni function)
        int bondState = m_device.getNativeManager().getNativeBondState();
        return new Object[]{BONDING, m_device.getNativeManager().isNativelyBonding(bondState), BONDED, m_device.getNativeManager().isNativelyBonded(bondState), UNBONDED, m_device.getNativeManager().isNativelyUnbonded(bondState)};
    }

    boolean isNativelyBondingOrBonded()
    {
        //--- DRK > These asserts are here because, as far as I could discern from logs, the abstracted
        //---		state for bonding/bonded was true, but when we did an encrypted write, it kicked
        //---		off a bonding operation, implying that natively the bonding state silently changed
        //---		since we discovered the device. I really don't know.
        //---		UPDATE: Nevermind, the reason bonding wasn't happening after connection was because
        //---				it was using the default config option of false. Leaving asserts here anywway
        //---				cause they can't hurt.
        //---		UPDATE AGAIN: Actually, these asserts can hit if you're connected to a device, you go
        //---		into OS settings, unbond, which kicks off an implicit disconnect which then kicks off
        //---		an implicit reconnect...race condition makes it so that you can query the bond state
        //---		and get its updated value before the bond state callback gets sent
        //---		UPDATE AGAIN AGAIN: Nevermind, it seems getBondState *can* actually lie, so original comment sorta stands...wow.
//		m_mngr.ASSERT(m_stateTracker.checkBitMatch(BONDED, isNativelyBonded()));
//		m_mngr.ASSERT(m_stateTracker.checkBitMatch(BONDING, isNativelyBonding()));
        int bondState = m_device.getNativeManager().getNativeBondState();

        return bondState == BluetoothDevice.BOND_BONDED || bondState == BluetoothDevice.BOND_BONDING;
    }

    void update(double timeStep)
    {
        mConnectBugHack.update(timeStep);
    }
}
