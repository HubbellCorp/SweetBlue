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
import android.bluetooth.BluetoothGatt;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDeviceConfig;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.BleManagerState;
import com.idevicesinc.sweetblue.BleStatuses;
import com.idevicesinc.sweetblue.DeviceConnectListener;
import com.idevicesinc.sweetblue.DeviceReconnectFilter;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.ReadWriteListener;
import com.idevicesinc.sweetblue.ReconnectFilter;
import com.idevicesinc.sweetblue.UhOhListener;
import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.internal.android.P_DeviceHolder;
import com.idevicesinc.sweetblue.internal.android.P_GattHolder;
import com.idevicesinc.sweetblue.utils.CodeHelper;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Phy;
import com.idevicesinc.sweetblue.utils.State;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.Utils_Config;

import static com.idevicesinc.sweetblue.BleDeviceState.BONDED;
import static com.idevicesinc.sweetblue.BleDeviceState.REQUESTING_PHY;
import static com.idevicesinc.sweetblue.internal.P_ConnectFailPlease.*;
import java.util.ArrayList;
import java.util.Stack;
import static com.idevicesinc.sweetblue.BleDeviceState.ADVERTISING;
import static com.idevicesinc.sweetblue.BleDeviceState.AUTHENTICATED;
import static com.idevicesinc.sweetblue.BleDeviceState.AUTHENTICATING;
import static com.idevicesinc.sweetblue.BleDeviceState.BLE_CONNECTED;
import static com.idevicesinc.sweetblue.BleDeviceState.BLE_CONNECTING;
import static com.idevicesinc.sweetblue.BleDeviceState.CONNECTED;
import static com.idevicesinc.sweetblue.BleDeviceState.CONNECTING;
import static com.idevicesinc.sweetblue.BleDeviceState.CONNECTING_OVERALL;
import static com.idevicesinc.sweetblue.BleDeviceState.BLE_DISCONNECTED;
import static com.idevicesinc.sweetblue.BleDeviceState.DISCONNECTED;
import static com.idevicesinc.sweetblue.BleDeviceState.INITIALIZED;
import static com.idevicesinc.sweetblue.BleDeviceState.INITIALIZING;
import static com.idevicesinc.sweetblue.BleDeviceState.RECONNECTING_LONG_TERM;
import static com.idevicesinc.sweetblue.BleDeviceState.RECONNECTING_SHORT_TERM;
import static com.idevicesinc.sweetblue.BleDeviceState.RETRYING_BLE_CONNECTION;
import static com.idevicesinc.sweetblue.BleDeviceState.UNBONDED;


class P_DeviceConnectionManager
{

    private final IBleDevice m_device;
    final P_ReconnectManager m_reconnectMngr_longTerm;
    final P_ReconnectManager m_reconnectMngr_shortTerm;
    final P_ConnectionFailManager m_connectionFailMngr;


    private final Stack<DeviceConnectListener> m_connectListenerStack;
    private DeviceConnectListener m_ephemeralConnectListener;
    private DeviceReconnectFilter.ConnectFailEvent m_nullConnectionFailEvent = null;


    private boolean m_underwentPossibleImplicitBondingAttempt = false;
    private boolean m_lastDisconnectWasBecauseOfBleTurnOff = false;
    private Boolean m_lastConnectOrDisconnectWasUserExplicit = null;
    private boolean m_useAutoConnect = false;
    private boolean m_alwaysUseAutoConnect = false;


    P_DeviceConnectionManager(IBleDevice device)
    {
        m_device = device;
        m_connectListenerStack = new Stack<>();
        m_connectionFailMngr = new P_ConnectionFailManager(device);

        if (m_device.isNull())
        {
            m_reconnectMngr_shortTerm = null;
            m_reconnectMngr_longTerm = null;
        }
        else
        {
            m_reconnectMngr_shortTerm = new P_ReconnectManager(m_device, NULL_CONNECTIONFAIL_INFO(), true);
            m_reconnectMngr_longTerm = new P_ReconnectManager(m_device, NULL_CONNECTIONFAIL_INFO(), false);
        }
    }


//    ********************************
//    **  Callback related methods  **
//    ********************************

    /**
     * This gets called when we notice a native connection state change in {@link P_BleDevice_ListenerProcessor#onConnectionStateChange_updateThread(P_GattHolder, int, int, Boolean)},
     * and within {@link #connect_private(IBleTransaction, IBleTransaction, boolean)}.
     */
    final void onConnecting(boolean definitelyExplicit, boolean isReconnect, final Object[] extraBondingStates, final boolean bleConnect)
    {
        m_lastConnectOrDisconnectWasUserExplicit = definitelyExplicit;

        if (bleConnect && m_device.is_internal(/* already */BLE_CONNECTING))
        {
            P_Task_Connect task = taskManager().getCurrent(P_Task_Connect.class, m_device);
            boolean mostDefinitelyExplicit = task != null && task.isExplicit();

            //--- DRK > Not positive about this assert...we'll see if it trips.
            getManager().ASSERT(definitelyExplicit || mostDefinitelyExplicit, "");

            m_device.updateBondStates(extraBondingStates);
        }
        else
        {
            final PA_StateTracker.E_Intent intent;

            if (definitelyExplicit && !isReconnect)
            {
                //--- DRK > We're stopping the reconnect process (if it's running) because the user has decided to explicitly connect
                //--- for whatever reason. Making a judgement call that the user would then expect reconnect to stop.
                //--- In other words it's not stopped for any hard technical reasons...it could go on.
                m_reconnectMngr_longTerm.stop();
                intent = PA_StateTracker.E_Intent.INTENTIONAL;
                m_device.getStateTracker().update(intent, BluetoothGatt.GATT_SUCCESS, RECONNECTING_LONG_TERM, false, CONNECTING, true, BLE_CONNECTING, bleConnect, CONNECTING_OVERALL, true, BLE_DISCONNECTED, false, DISCONNECTED, false, ADVERTISING, false, extraBondingStates);
            }
            else
            {
                intent = lastConnectDisconnectIntent();
                m_device.getStateTracker().update(intent, BluetoothGatt.GATT_SUCCESS, BLE_CONNECTING, bleConnect, CONNECTING_OVERALL, true, BLE_DISCONNECTED, false, ADVERTISING, false, extraBondingStates);
                if (bleConnect)
                    m_device.getStateTracker().update_native(BLE_CONNECTING);
            }
        }
    }

    /**
     * This gets called from {@link P_BleDevice_ListenerProcessor#onConnectionStateChange_updateThread(P_GattHolder, int, int, Boolean)} when we notice the state changed from the native
     * stack, and when a {@link P_Task_Connect} task succeeds (or is redundant) for the {@link #m_device}.
     */
    final void onConnected(boolean explicit)
    {
        m_lastDisconnectWasBecauseOfBleTurnOff = false; // DRK > Just being anal.

        PA_StateTracker.E_Intent intent = explicit && !m_device.is(RECONNECTING_LONG_TERM) ? PA_StateTracker.E_Intent.INTENTIONAL : PA_StateTracker.E_Intent.UNINTENTIONAL;
        m_lastConnectOrDisconnectWasUserExplicit = intent == PA_StateTracker.E_Intent.INTENTIONAL;

        if (m_device.is_internal(/*already*/BLE_CONNECTED))
        {
            //--- DRK > Possible to get here when implicit tasks are involved I think. Not sure if assertion should be here,
            //--- and if it should it perhaps should be keyed off whether the task is implicit or something.
            //--- Also possible to get here for example on connection fail retries, where we queue a disconnect
            //--- but that gets immediately soft-cancelled by what will be a redundant connect task.
            //--- OVERALL, This assert is here because I'm just curious how it hits (it does).
            String message = "nativelyConnected=" + CodeHelper.gattConn(m_device.getNativeManager().getConnectionState(), logger().isEnabled()) + " gatt==" + m_device.getNativeManager().getGattLayer().getGatt();
            // getIManager().ASSERT(false, message);
            getManager().ASSERT(m_device.getNativeManager().isNativelyConnected(), message);

            return;
        }

        getManager().ASSERT(!m_device.nativeManager().getGattLayer().isGattNull(), "");

        //--- DRK > There exists a fringe case like this: You try to connect with autoConnect==true in the gatt object.
        //--- The connection fails, so you stop trying. Then you turn off the remote device. Device gets "undiscovered".
        //--- You turn the device back on, and apparently underneath the hood, this whole time, the stack has been trying
        //--- to reconnect, and now it does, *without* (re)discovering the device first, or even discovering it at all.
        //--- So as usual, here's another gnarly workaround to ensure a consistent API experience through SweetBlue.
        //
        //--- NOTE: We do explicitly disconnect after a connection failure if we're using autoConnect, so this
        //--- case shouldn't really come up much or at all with that in place.
        if (!getManager().hasDevice(m_device.getMacAddress()))
        {
            getManager().onDiscovered_fromRogueAutoConnect(m_device, /*newlyDiscovered=*/true, m_device.getScanInfo().getServiceUUIDS(), m_device.getScanRecord(), m_device.getRssi());
        }

        //--- DRK > Some trapdoor logic for bad android ble bug.
        int nativeBondState = m_device.getNativeManager().getNativeBondState();
        if (nativeBondState == BluetoothDevice.BOND_BONDED)
        {
            //--- DRK > Trying to catch fringe condition here of stack lying to
            // us about bonded state.
            //--- This is not about finding a logic error in my code.
            // --- RB > Not sure this is even valid anymore. It looks like we're asserting everytime we get connected when there is a bond? Seems pretty silly to me. I'm commenting it out for now
            // so people don't think something is wrong
//            getManager().ASSERT(getManager().managerLayer().getBondedDevices().contains(P_DeviceHolder.newHolder(m_device.getNativeManager().getDeviceLayer().getNativeDevice())), "");
        }

        logger().d(CodeHelper.gattBondState(m_device.getNativeManager().getNativeBondState(), logger().isEnabled()));

        Phy phy = conf_device().phyOptions != null ? conf_device().phyOptions : conf_mngr().phyOptions != null ? conf_mngr().phyOptions : Phy.DEFAULT;

        if (phy != Phy.DEFAULT)
        {
            m_device.getStateTracker().update(PA_StateTracker.E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, REQUESTING_PHY, true, BLE_CONNECTING, false, BLE_CONNECTED, true);
            m_device.setPhyOptions(phy, e -> {
                if (e.wasSuccess() == false)
                    getManager().uhOh(UhOhListener.UhOh.PHYSICAL_LAYER_FAILURE);

                handleAfterBleConnect();
            });
        }
        else
            handleAfterBleConnect();

    }

    private void handleAfterBleConnect()
    {
        // First check if we should bond, and add the task if it needs to be done, if tryBondingWhileDisconnected is false, and alwaysBondOnConnect is true
        boolean tryBondingWhileConnected = !Utils_Config.bool(m_device.conf_device().tryBondingWhileDisconnected, m_device.conf_mngr().tryBondingWhileDisconnected);
        boolean alwaysBondOnConnect = Utils_Config.bool(m_device.conf_device().alwaysBondOnConnect, m_device.conf_mngr().alwaysBondOnConnect);
        boolean bonded = m_device.is(BONDED);
        if (!bonded && alwaysBondOnConnect && tryBondingWhileConnected)
            m_device.bond_justAddTheTask(P_Task_Bond.E_TransactionLockBehavior.PASSES, false);

        // Discover services, unless the autoGetServices flag is false, in which case, we move on to any Auth/Init transactions
        boolean autoGetServices = Utils_Config.bool(conf_device().autoGetServices, conf_mngr().autoGetServices);
        if (autoGetServices)
        {
            m_device.getServices(BLE_DISCONNECTED, false, CONNECTING_OVERALL, true, BLE_CONNECTING, false, BLE_CONNECTED, true, ADVERTISING, false);
        }
        else
        {
            m_device.getTxnManager().runAuthOrInitTxnIfNeeded(BluetoothGatt.GATT_SUCCESS, BLE_DISCONNECTED, false, CONNECTING_OVERALL, true, BLE_CONNECTING, false, BLE_CONNECTED, true, ADVERTISING, false);
        }
    }

    final void onInitialized(final int gattStatus, Object... extraFlags)
    {
        m_reconnectMngr_longTerm.stop();
        m_reconnectMngr_shortTerm.stop();
        m_connectionFailMngr.onFullyInitialized();

        //--- DRK > Saving last disconnect as unintentional here in case for some
        //--- reason app is hard killed or something and we never get a disconnect callback.
        final boolean hitDisk = Utils_Config.bool(conf_device().manageLastDisconnectOnDisk, conf_mngr().manageLastDisconnectOnDisk);
        getManager().getDiskOptionsManager().saveLastDisconnect(m_device.getMacAddress(), State.ChangeIntent.UNINTENTIONAL, hitDisk);

        m_device.getStateTracker().update(lastConnectDisconnectIntent(), gattStatus, extraFlags,
                RECONNECTING_LONG_TERM, false, CONNECTING_OVERALL, false, RECONNECTING_SHORT_TERM, false,
                CONNECTED, true, CONNECTING, false, AUTHENTICATING, false, AUTHENTICATED, true, INITIALIZING, false,
                INITIALIZED, true, RETRYING_BLE_CONNECTION, false, DISCONNECTED, false);

        final DeviceConnectListener.ConnectEvent event = P_Bridge_User.newConnectEvent(m_device.getBleDevice());

        invokeConnectCallbacks(event);

        m_ephemeralConnectListener = null;
    }

    /**
     * This gets called for a couple of reasons. The most common would be {@link P_BleDevice_ListenerProcessor#taskStateChanged(PA_Task, PE_TaskState)} when a
     * {@link P_Task_Connect} fails for the {@link #m_device}. It can also be called from {@link P_BleDevice_ListenerProcessor#onConnectionStateChange_updateThread(P_GattHolder, int, int, Boolean)}
     * if changing to another task ended up with a gattStatus of anything other than {@link BluetoothGatt#GATT_SUCCESS}.
     */
    final void onConnectFail(PE_TaskState state, int gattStatus, ReconnectFilter.AutoConnectUsage autoConnectUsage)
    {
        m_device.getNativeManager().closeGattIfNeeded(/* disconnectAlso= */true);

        if (state == PE_TaskState.SOFTLY_CANCELLED) return;

        boolean attemptingReconnect = m_device.is(RECONNECTING_LONG_TERM);
        BleDeviceState highestState = BleDeviceState.getTransitoryConnectionState(getStateMask());

        final boolean wasConnecting = m_device.is_internal(CONNECTING_OVERALL);
        final DeviceReconnectFilter.Status connectionFailStatus = DeviceReconnectFilter.Status.NATIVE_CONNECTION_FAILED;

        m_device.getTxnManager().cancelAllTransactions();

        if (Utils.phoneHasBondingIssues())
        {
            taskManager().clearQueueOf(P_Task_Bond.class, m_device, taskManager().getSize());
        }

        boolean retryingConnection = false;

        if (wasConnecting)
        {
            P_DisconnectReason disconnectReason = new P_DisconnectReason(gattStatus)
                    .setAttemptingLongTermReconnect(attemptingReconnect)
                    .setConnectFailReason(connectionFailStatus)
                    .setHighestState(highestState)
                    .setAutoConnectUsage(autoConnectUsage)
                    .setTxnFailReason(P_Bridge_User.newReadWriteEventNULL(m_device.getBleDevice()));

            dispatchFailEventOnConnecting(state, disconnectReason);
        }
        else
        {
            // This was moved into the onConnectionFailed method of the connectionfaillistener, so we can add the RETRYING_BLE_CONNECTION state while
            // setting the state to disconnected. This way, both states are set at the same time, eliminating any race conditions between those 2
            // states. This is here now in the case the connectionfaillistener doesn't get called, but the device is still in any connected/ing state
            if (m_device.isAny_internal(BLE_CONNECTED, BLE_CONNECTING, CONNECTING_OVERALL))
            {
                m_device.setStateToDisconnected(attemptingReconnect, retryingConnection, PA_StateTracker.E_Intent.UNINTENTIONAL, gattStatus);
            }
        }
    }

    final void onDisconnected(final boolean wasExplicit, final int gattStatus, final boolean attemptShortTermReconnect, final boolean saveLastDisconnect)
    {
        logger().w("Disconnected " + (wasExplicit ? "explicitly " : "implicitly ") + "and attemptShortTermReconnect=" + attemptShortTermReconnect);

        m_lastDisconnectWasBecauseOfBleTurnOff = getManager().isAny(BleManagerState.TURNING_OFF, BleManagerState.OFF);
        m_lastConnectOrDisconnectWasUserExplicit = wasExplicit;

        final BleDeviceState highestState = BleDeviceState.getTransitoryConnectionState(getStateMask());

        if (saveLastDisconnect)
            saveLastDisconnect(wasExplicit);

        m_device.getPollManager().resetNotifyStates();
        m_device.getNativeManager().closeGattIfNeeded(/* disconnectAlso= */false);

        final int overrideOrdinal = getManager().getTaskManager().getCurrentOrdinal();

        final boolean wasSimpleConnected = m_device.is(CONNECTED);

        if (attemptShortTermReconnect)
        {
            // If this was an implicit disconnect, and the device was initialized at one point, and the reconnect short term
            // manager isn't running, start it up now.
            if (!wasExplicit && wasSimpleConnected && !m_reconnectMngr_shortTerm.isRunning())
            {
                // Make sure to interrupt any task that is currently executing for this device
                getManager().getTaskManager().interrupt(m_device);
                
                m_reconnectMngr_shortTerm.attemptStart(gattStatus);

                if (!m_device.is(RECONNECTING_SHORT_TERM))
                {
                    // This call simply sets the RECONNECTING_SHORT_TERM state
                    m_device.onReconnectingShortTerm();
                }
            }
            // If the reconnect manager is running, then we let it handle the reconnect process. State changes have been handled above.
            if (m_reconnectMngr_shortTerm.isRunning())
                return;
        }
        else if (m_device.is(RECONNECTING_SHORT_TERM) && !m_device.reconnectMngr().isRunning())
            taskManager().fail(P_Task_Connect.class, m_device);

        final boolean isDisconnectedAfterReconnectingShortTermStateCallback = m_device.is(DISCONNECTED) && m_device.is(RECONNECTING_SHORT_TERM);
        final boolean isConnectingBle = m_device.is(BLE_CONNECTING);
        final boolean explicitDisconnectWhenConnecting = isConnectingBle && wasExplicit;
        final boolean cancelTasks;

        if (!explicitDisconnectWhenConnecting)
        {
            if (isDisconnectedAfterReconnectingShortTermStateCallback/* || wasExplicit*/)
            {
                m_connectionFailMngr.onExplicitDisconnect();

                cancelTasks = false;
            }
            else
                cancelTasks = true;
        }
        else
            cancelTasks = false;

        //--- DRK > This was originally where cancelTasks = true; is now placed, before disconnected state change. Putting it after because of the following scenario:
        //---		(1) Write task takes a long time (timeout scenario). Specifically, tab 4 onCharacteristicWrite gets called only internally (doesn't make it to callback) then keeps spinning.
        //---		(2) An unsolicited disconnect comes in but we don't get a callback for the write.
        //---		(3) Before, the soft cancellation was done before the state change, which made the connection failure reason due to authentication failing, not an unsolicited disconnect like it should be.
        //--- RB > Moved this back up, as we're getting a race condition where a disconnect comes in, and connect() is called in the callback. Sometimes, the connect task can get put into the queue
        //          before we cancel tasks, so the new connect task gets cleared when it shouldn't. We will have to monitor this if the above case happens to other devices (or still happens due to the
        //          threading changes in v3)
        if (cancelTasks)
            m_device.softlyCancelTasks(overrideOrdinal);

        //--- DRK > Fringe case bail out in case user calls disconnect() in state change for short term reconnect.
        if (isDisconnectedAfterReconnectingShortTermStateCallback)
        {
            m_device.getTxnManager().cancelAllTransactions();

            return;
        }

        final boolean isAttemptingReconnect_longTerm = m_device.is_internal(RECONNECTING_LONG_TERM);
        final boolean wasConnectingOverall = m_device.is(CONNECTING_OVERALL);

        if (explicitDisconnectWhenConnecting)
        {
            m_device.getTxnManager().cancelAllTransactions();

            // We want to make sure that we update the state here. If you call disconnect() when currently connecting, the state won't get updated, unless this is here
            m_device.setStateToDisconnected(isAttemptingReconnect_longTerm, false, PA_StateTracker.E_Intent.INTENTIONAL, gattStatus);

            return;
        }

        // BEGIN CALLBACKS TO USER

        final PA_StateTracker.E_Intent intent = wasExplicit ? PA_StateTracker.E_Intent.INTENTIONAL : PA_StateTracker.E_Intent.UNINTENTIONAL;
        m_device.setStateToDisconnected(isAttemptingReconnect_longTerm, false, intent, gattStatus);

        //--- DRK > Technically user could have called connect() in callbacks above....bad form but we need to account for it.
        //--- RB >  This shouldn't really happen anymore, as we post to the main thread. BUT, we still allow running on the main thread
        //          so we still need to account for this case.
        final boolean isConnectingOverall_1 = m_device.is_internal(CONNECTING_OVERALL);
        final boolean isStillAttemptingReconnect_longTerm = m_device.is_internal(RECONNECTING_LONG_TERM);
        final DeviceReconnectFilter.Status connectionFailReason_nullable;
        if (!m_reconnectMngr_shortTerm.isRunning() && wasConnectingOverall && !wasExplicit)
        {
            if (getManager().isAny(BleManagerState.TURNING_OFF, BleManagerState.OFF))
                connectionFailReason_nullable = DeviceReconnectFilter.Status.BLE_TURNING_OFF;
            else
                connectionFailReason_nullable = DeviceReconnectFilter.Status.ROGUE_DISCONNECT;
        }
        else
            connectionFailReason_nullable = null;

        //--- DRK > Originally had is(BLE_DISCONNECTED) here, changed to is_internal, but then realized
        //---		you probably want to (and it's safe to ) cancel all transactions all the time.
        //---		I think the original intent was to account for the faulty assumption that someone
        //---		would call connect again themselves in the state callback and somehow cancel the
        //---		new transaction passed to connect()...BUT this can't happen cause the actual connect
        //---		task has to run (even if it's redundant), and services have to be discovered.
//						if (is_internal(BLE_DISCONNECTED))
        m_device.getTxnManager().cancelAllTransactions();

        final P_ConnectFailPlease retrying__PE_Please;
        final P_DisconnectReason disconnectReason = new P_DisconnectReason(gattStatus, DeviceReconnectFilter.Timing.NOT_APPLICABLE)
                .setConnectFailReason(connectionFailReason_nullable)
                .setAttemptingLongTermReconnect(isStillAttemptingReconnect_longTerm)
                .setBondFailReason(BleStatuses.BOND_FAIL_REASON_NOT_APPLICABLE)
                .setHighestState(highestState)
                .setAutoConnectUsage(ReconnectFilter.AutoConnectUsage.NOT_APPLICABLE)
                .setTxnFailReason(P_Bridge_User.newReadWriteEventNULL(m_device.getBleDevice()));

        if (!isConnectingOverall_1 && !m_reconnectMngr_shortTerm.isRunning())
        {
            if (connectionFailReason_nullable != null && wasExplicit)
            {
                // If we're already disconnected, then this is the native callback coming back, and we've already sent the connectionfail event back to the user
                // So we don't need to post the event again here.
                if (!isDisconnectedAfterReconnectingShortTermStateCallback)
                    retrying__PE_Please = m_connectionFailMngr.onConnectionFailed(disconnectReason);
                else
                    retrying__PE_Please = DO_NOT_RETRY;
            }
            else
            {
                if (m_connectionFailMngr.hasPendingConnectionFailEvent())
                {
                    retrying__PE_Please = m_connectionFailMngr.getPendingConnectionFailRetry();
                    m_connectionFailMngr.clearPendingRetry();
                }
                else
                {
                    retrying__PE_Please = m_connectionFailMngr.onConnectionFailed(disconnectReason);
//                    retrying__PE_Please = ConnectionFailListener.Please.PE_Please_DO_NOT_RETRY;PE_Please_DO_NOT_RETRY
                }
            }
        }
        else
            retrying__PE_Please = DO_NOT_RETRY;

        //--- DRK > Again, technically user could have called connect() in callbacks above....bad form but we need to account for it.
        final boolean isConnectingOverall_2 = m_device.is_internal(CONNECTING_OVERALL);

        if (!m_reconnectMngr_shortTerm.isRunning() && !m_reconnectMngr_longTerm.isRunning() && !wasExplicit && wasSimpleConnected && !isConnectingOverall_2)
        {
            // Make sure to call onConnectionLost, so the timeout values get set properly
            ReconnectFilter.ConnectFailEvent event = P_Bridge_User.newConnectFailEvent(m_device.getBleDevice(), 0, disconnectReason, highestState, Interval.ZERO, Interval.ZERO);
            m_reconnectMngr_longTerm.onConnectionLost(event);
            m_reconnectMngr_longTerm.attemptStart(gattStatus);

            if (m_reconnectMngr_longTerm.isRunning())
                m_device.onReconnectingLongTerm();
        }

        //--- DRK > Throwing in one last disconnect if it looks like we just came out of a short term reconnect
        //---		that was connected and (e.g.) getting services and so this method was called but no long term reconnect was started
        //---		so we have to bail out.
        if (m_device.is(BLE_DISCONNECTED) && !m_device.is(RECONNECTING_LONG_TERM) && m_reconnectMngr_longTerm.isRunning() == false && m_reconnectMngr_shortTerm.isRunning() == false)
        {
            if (m_device.getNativeManager().isNativelyConnectingOrConnected())
                taskManager().add(new P_Task_Disconnect(m_device, m_device.getListener_TaskState(), /*explicit=*/false, null, /*cancellable=*/true));
        }

        //--- DRK > Not actually entirely sure how, it may be legitimate, but a connect task can still be
        //--- hanging out in the queue at this point, so we just make sure to clear the queue as a failsafe.
        //--- TODO: Understand the conditions under which a connect task can still be queued...might be a bug upstream.
        if (!isConnectingOverall_2 && retrying__PE_Please == DO_NOT_RETRY)
            taskManager().clearQueueOf(P_Task_Connect.class, m_device, -1);

        boolean doReconnectForConnectingOverall = Utils_Config.bool(conf_device().connectFailRetryConnectingOverall, conf_mngr().connectFailRetryConnectingOverall);

        if (doReconnectForConnectingOverall && !wasExplicit && !wasSimpleConnected && retrying__PE_Please != DO_NOT_RETRY)
            attemptReconnect();
    }

    /**
     * This gets called when {@link #m_device} has been deemed to be {@link BleDeviceState#UNDISCOVERED}, either through lack of it being seen
     * via scanning, {@link BleDevice#undiscover()} was called, or BLE is being turned off.
     */
    final void onUndiscovered()
    {
        if (m_reconnectMngr_longTerm != null) m_reconnectMngr_longTerm.stop();
        if (m_reconnectMngr_shortTerm != null) m_reconnectMngr_shortTerm.stop();
    }

    /**
     * Part of {@link BleManager}'s update thread. This updates the reconnect managers held in this instance for {@link #m_device}.
     */
    final void update(double timeStep)
    {
        m_reconnectMngr_longTerm.update(timeStep);
        m_reconnectMngr_shortTerm.update(timeStep);
    }


//    **************************************************
//    **  Methods to perform connect related actions  **
//    **************************************************

    final void disconnectWithReason(final P_DisconnectReason disconnectReason)
    {
        // If there is already a disconnect task in the queue, then simply ignore this (in the case an explicit disconnect comes in while
        // a transaction is processing...the transaction will call this method as well)
        boolean inQueue = taskManager().isInQueue(P_Task_Disconnect.class, m_device);

        if (!inQueue)
        {
            if (m_device.isNull()) return;

            final boolean cancelled = disconnectReason.isCanceled();
            final boolean explicit = disconnectReason.isExplicit() || disconnectReason.getIsForcedRemoteDisconnect();
            final BleDeviceState highestState = BleDeviceState.getTransitoryConnectionState(getStateMask());

            if (explicit)
            {
                m_reconnectMngr_shortTerm.stop();
            }

            if (cancelled)
            {
                m_useAutoConnect = m_alwaysUseAutoConnect;

                m_connectionFailMngr.onExplicitDisconnect();
            }

            final boolean wasConnecting = m_device.is_internal(CONNECTING_OVERALL);
            final boolean attemptingReconnect_shortTerm = m_device.is(RECONNECTING_SHORT_TERM);
            final boolean attemptingReconnect_longTerm = cancelled ? false : m_device.is(RECONNECTING_LONG_TERM);

            PA_StateTracker.E_Intent intent = cancelled ? PA_StateTracker.E_Intent.INTENTIONAL : PA_StateTracker.E_Intent.UNINTENTIONAL;
            m_lastConnectOrDisconnectWasUserExplicit = intent == PA_StateTracker.E_Intent.INTENTIONAL;

            final boolean cancellableFromConnect = Utils_Config.bool(conf_device().disconnectIsCancellable, conf_mngr().disconnectIsCancellable);
            final boolean tryBondingWhileDisconnected = disconnectReason.shouldTryBondingWhileDisconnected(m_device.getBleDevice());
            final boolean underwentPossibleImplicitBondingAttempt = m_device.getNativeManager().isNativelyUnbonded() && m_underwentPossibleImplicitBondingAttempt == true;
            final boolean taskIsCancellable = cancellableFromConnect == true && tryBondingWhileDisconnected == false && underwentPossibleImplicitBondingAttempt == false;

            saveLastDisconnect(disconnectReason.isExplicit());

            final boolean saveLastDisconnectAfterTaskCompletes = !disconnectReason.isRogueDisconnect();

            final int taskOrdinal;
            final boolean clearQueue;

            if (m_device.isAny_internal(BLE_CONNECTED, BLE_CONNECTING, CONNECTING_OVERALL, INITIALIZED))
            {
                final P_Task_Disconnect disconnectTask = new P_Task_Disconnect(m_device, m_device.getListener_TaskState(), /*explicit=*/explicit, disconnectReason.getPriority(), taskIsCancellable, saveLastDisconnectAfterTaskCompletes);
                taskManager().add(disconnectTask);

                // If it's currently connecting, then cancel the task
                P_Task_Connect connectTask = taskManager().getCurrent(P_Task_Connect.class, m_device);
                if (connectTask != null)
                    connectTask.softlyCancel();

                taskOrdinal = disconnectTask.getOrdinal();
                clearQueue = true;
            }
            else
            {
                taskOrdinal = -1;
                clearQueue = false;
            }

            m_device.getTxnManager().cancelAllTransactions();

            if (clearQueue)
            {
                taskManager().clearQueueOf(P_Task_Connect.class, m_device, -1);
                taskManager().clearQueueOf(PA_Task_RequiresConnection.class, m_device, taskOrdinal);
            }

            if (!attemptingReconnect_longTerm)
            {
                m_reconnectMngr_longTerm.stop();
            }

            if (wasConnecting || attemptingReconnect_shortTerm)
            {
                if (getManager().ASSERT(disconnectReason.getConnectFailReason() != null, ""))
                {
                    disconnectReason.setAttemptingLongTermReconnect(attemptingReconnect_longTerm)
                            .setHighestState(highestState)
                            .setAutoConnectUsage(ReconnectFilter.AutoConnectUsage.NOT_APPLICABLE);
                    m_connectionFailMngr.onConnectionFailed(disconnectReason);
                }
            }
        }

        if (disconnectReason.getIsUndiscoverAfter())
            getManager().getDeviceManager().undiscoverAndRemove(m_device, getManager().getListener_Discovery(), getManager().getDeviceManager_cache(), PA_StateTracker.E_Intent.INTENTIONAL);
    }

    final DeviceReconnectFilter.ConnectFailEvent connect(IBleTransaction authenticationTxn, IBleTransaction initTxn, DeviceConnectListener connectionListener)
    {
        if (connectionListener != null)
            m_ephemeralConnectListener = connectionListener;

        final DeviceReconnectFilter.ConnectFailEvent info_earlyOut = connect_earlyOut();

        if (info_earlyOut != null) return info_earlyOut;

        m_lastConnectOrDisconnectWasUserExplicit = true;

        if (m_device.isAny(BLE_CONNECTED, BLE_CONNECTING, CONNECTING_OVERALL))
        {
            //--- DRK > Making a judgement call that an explicit connect call here means we bail out of the long term reconnect state.
            m_device.dropReconnectingLongTermState();

            final DeviceReconnectFilter.ConnectFailEvent info_alreadyConnected = P_Bridge_User.newConnectFailEARLYOUT(m_device.getBleDevice(), DeviceReconnectFilter.Status.ALREADY_CONNECTING_OR_CONNECTED);

            m_connectionFailMngr.invokeCallback(info_alreadyConnected, true);

            return info_alreadyConnected;
        }

        connect_private(authenticationTxn, initTxn, /* isReconnect= */false);

        return NULL_CONNECTIONFAIL_INFO();
    }

    final void attemptReconnect()
    {
        if (connect_earlyOut() != null) return;

        m_lastConnectOrDisconnectWasUserExplicit = true;

        if (m_device.isAny_internal(BLE_CONNECTED, BLE_CONNECTING, CONNECTING_OVERALL))
        {
            final DeviceReconnectFilter.ConnectFailEvent info = P_Bridge_User.newConnectFailEARLYOUT(m_device.getBleDevice(), DeviceReconnectFilter.Status.ALREADY_CONNECTING_OR_CONNECTED);

            m_connectionFailMngr.invokeCallback(info, false);

            return;
        }

        connect_private(m_device.getTxnManager().m_authTxn, m_device.getTxnManager().m_initTxn, /*isReconnect=*/true);
    }

    final void invokeConnectCallbacks(DeviceConnectListener.ConnectEvent event)
    {
        DeviceConnectListener listener = m_ephemeralConnectListener;
        // Post to the ephemeral listener first, if it's not null
        if (listener != null)
            getManager().postEvent(listener, event);

        // Now post to the default listener, if there is one
        listener = getListener_Connect();

        if (listener != null)
            getManager().postEvent(listener, event);

        // Now post to the manager's listener, if there is one
        listener = getManager().getDefaultDeviceConnectListener();
        if (listener != null)
            getManager().postEvent(listener, event);
    }


//    **************************************************
//    **  Miscellaneous package private methods       **
//    **************************************************


    final boolean setListener_Connect(@Nullable(Nullable.Prevalence.NORMAL) DeviceConnectListener listener)
    {
        if (m_device.isNull()) return false;

        m_connectListenerStack.clear();
        if (listener != null)
            return m_connectListenerStack.push(listener) != null;
        else
            return false;
    }

    final boolean pushListener_Connect(@Nullable(Nullable.Prevalence.NEVER) DeviceConnectListener listener)
    {
        if (m_device.isNull() || listener == null) return false;

        m_connectListenerStack.push(listener);
        return true;
    }

    final boolean popListener_Connect()
    {
        if (m_device.isNull()) return false;

        if (m_connectListenerStack.empty()) return false;

        m_connectListenerStack.pop();

        return true;
    }

    final boolean popListener_Connect(DeviceConnectListener listener)
    {
        if (m_device.isNull()) return false;

        return !m_connectListenerStack.empty() && m_connectListenerStack.remove(listener);
    }

    final @Nullable(Nullable.Prevalence.NORMAL) DeviceConnectListener getListener_Connect()
    {
        if (m_connectListenerStack.empty()) return null;

        return m_connectListenerStack.peek();
    }

    final void setListener_Reconnect(@Nullable(Nullable.Prevalence.NORMAL) DeviceReconnectFilter listener_nullable)
    {
        m_connectionFailMngr.clearListenerStack();
        if (listener_nullable != null)
            m_connectionFailMngr.setListener(listener_nullable);
    }

    final void pushListener_Reconnect(@Nullable(Nullable.Prevalence.NEVER) DeviceReconnectFilter listener)
    {
        if (listener == null) return;

        m_connectionFailMngr.pushListener(listener);
    }

    final boolean popListener_Reconnect()
    {
        return m_connectionFailMngr.popListener();
    }

    final boolean popListener_Reconnect(DeviceReconnectFilter listener)
    {
        return m_connectionFailMngr.popListener(listener);
    }

    final @Nullable(Nullable.Prevalence.NORMAL) DeviceReconnectFilter getListener_Reconnect()
    {
        return m_connectionFailMngr.getListener();
    }

    final ArrayList<DeviceReconnectFilter.ConnectFailEvent> getConnectionFailHistory()
    {
        return m_connectionFailMngr.getHistory();
    }

    final void notifyOfPossibleImplicitBondingAttempt()
    {
        m_underwentPossibleImplicitBondingAttempt = true;
    }

    final int getConnectionRetryCount()
    {
        return m_connectionFailMngr.getRetryCount();
    }

    final boolean lastDisconnectWasBecauseOfBleTurnOff()
    {
        return m_lastDisconnectWasBecauseOfBleTurnOff;
    }

    final PA_StateTracker.E_Intent lastConnectDisconnectIntent()
    {
        if (m_lastConnectOrDisconnectWasUserExplicit == null)
        {
            return PA_StateTracker.E_Intent.UNINTENTIONAL;
        }
        else
        {
            return m_lastConnectOrDisconnectWasUserExplicit ? PA_StateTracker.E_Intent.INTENTIONAL : PA_StateTracker.E_Intent.UNINTENTIONAL;
        }
    }

    final void setAutoConnectFromConfig(boolean useAutoConnect)
    {
        if (useAutoConnect)
        {
            m_alwaysUseAutoConnect = m_useAutoConnect = true;
        }
        else
        {
            m_alwaysUseAutoConnect = false;
        }
    }

    final void onLongTermReconnectTimeOut()
    {
        m_connectionFailMngr.onLongTermTimedOut();
    }

    final boolean shouldUseAutoConnect()
    {
        return m_useAutoConnect;
    }

    final void setToAlwaysUseAutoConnectIfItWorked()
    {
        m_alwaysUseAutoConnect = m_useAutoConnect;
    }

    final DeviceReconnectFilter.ConnectFailEvent NULL_CONNECTIONFAIL_INFO()
    {
        if (m_nullConnectionFailEvent != null)
        {
            return m_nullConnectionFailEvent;
        }

        m_nullConnectionFailEvent = P_Bridge_User.newConnectFailNULL(m_device);

        return m_nullConnectionFailEvent;
    }


//    **************************************************
//    **  Methods only used by this class             **
//    **************************************************

    private void connect_private(final IBleTransaction authenticationTxn, final IBleTransaction initTxn, final boolean isReconnect)
    {
        getManager().getPostManager().runOrPostToUpdateThread(() -> {
            if (m_device.is_internal(INITIALIZED))
            {
                getManager().ASSERT(false, "Device is initialized but not connected!");
                return;
            }

            IBleTransaction auth = getAuthTxn(authenticationTxn);
            IBleTransaction init = getInitTxn(initTxn);
            m_device.getTxnManager().onConnect(auth, init);

            final Object[] extraBondingStates;

            if (m_device.is(UNBONDED) && Utils.isKitKat())
            {
                final BondCheck check = doBondingChecks();
                extraBondingStates = check.extraBondingStates;
            }
            else
            {
                extraBondingStates = P_BondManager.OVERRIDE_EMPTY_STATES;
            }

            onConnecting(/* definitelyExplicit= */true, isReconnect, extraBondingStates, /*bleConnect=*/false);

            //--- DRK > Just accounting for technical possibility that user calls #disconnect() or something in the state change callback for connecting overall.
            if (!/*still*/m_device.is_internal(CONNECTING_OVERALL))
            {
                return;
            }

            taskManager().add(new P_Task_Connect(m_device, m_device.getListener_TaskState()));

            onConnecting(/* definitelyExplicit= */true, isReconnect, extraBondingStates, /*bleConnect=*/true);
        });
    }

    private BondCheck doBondingChecks()
    {
        boolean needsBond;
        final BondCheck check = new BondCheck();
        // The following below is to determine if we should bond before connecting. It has been found that it improves connection rates on some phones
        // Not only that, but some devices require a PIN to pair(bond). If the device is already bonded, then no bonding will be done (unless it's one of these
        // troubled devices, as to get them to work more reliably, we have to remove the bond after every connect).
        final boolean tryBondingWhileDisconnected = Utils_Config.bool(conf_device().tryBondingWhileDisconnected, conf_mngr().tryBondingWhileDisconnected);

        final boolean autoBondFix = Utils_Config.bool(conf_device().autoBondFixes, conf_mngr().autoBondFixes) && Utils.phoneHasBondingIssues();

        final boolean alwaysBondOnConnect = Utils_Config.bool(conf_device().alwaysBondOnConnect, conf_mngr().alwaysBondOnConnect);

        needsBond = autoBondFix || alwaysBondOnConnect;

        final boolean doPreBond = getManager().getDiskOptionsManager().loadNeedsBonding(m_device.getMacAddress()) || needsBond;

        if (doPreBond && tryBondingWhileDisconnected)
        {
            needsBond = false;
            m_device.bond_justAddTheTask(P_Task_Bond.E_TransactionLockBehavior.PASSES, /*isDirect=*/false);

            check.extraBondingStates = P_BondManager.OVERRIDE_BONDING_STATES;
        }
        else
        {
            check.extraBondingStates = P_BondManager.OVERRIDE_EMPTY_STATES;
        }
        check.needsBonding = needsBond;
        return check;
    }

    private void dispatchFailEventOnConnecting(PE_TaskState state, P_DisconnectReason disconnectReason)
    {
        DeviceReconnectFilter.Timing timing = state == PE_TaskState.FAILED_IMMEDIATELY ? DeviceReconnectFilter.Timing.IMMEDIATELY : DeviceReconnectFilter.Timing.EVENTUALLY;

        if (state == PE_TaskState.TIMED_OUT)
        {
            timing = DeviceReconnectFilter.Timing.TIMED_OUT;
        }

        disconnectReason.setTiming(timing);

        final P_ConnectFailPlease retry__PE_Please = m_connectionFailMngr.onConnectionFailed(disconnectReason);

        if (!disconnectReason.getIsAttemptingLongTermReconnect() && retry__PE_Please == RETRY_WITH_AUTOCONNECT_TRUE)
        {
            m_useAutoConnect = true;
        }
        else if (!disconnectReason.getIsAttemptingLongTermReconnect() && retry__PE_Please == RETRY_WITH_AUTOCONNECT_FALSE)
        {
            m_useAutoConnect = false;
        }
        else
        {
            m_useAutoConnect = m_alwaysUseAutoConnect;
        }
    }

    private BleDeviceConfig conf_device()
    {
        return m_device.conf_device();
    }

    private BleManagerConfig conf_mngr()
    {
        return m_device.conf_mngr();
    }

    private DeviceReconnectFilter.ConnectFailEvent connect_earlyOut()
    {
        if (m_device.isNull())
        {
            final DeviceReconnectFilter.ConnectFailEvent e = P_Bridge_User.newConnectFailEARLYOUT(m_device.getBleDevice(), DeviceReconnectFilter.Status.NULL_DEVICE);

            m_connectionFailMngr.invokeCallback(e, true);

            return e;
        }

        return null;
    }

    private IBleTransaction getAuthTxn(IBleTransaction txn)
    {
        if (txn != null)
        {
            return txn;
        }
        if (conf_device().defaultAuthFactory != null)
        {
            return P_Bridge_User.getIBleTransaction(conf_device().defaultAuthFactory.newAuthTxn());
        }
        return null;
    }

    private IBleTransaction getInitTxn(IBleTransaction txn)
    {
        if (txn != null)
        {
            return txn;
        }
        if (conf_device().defaultInitFactory != null)
        {
            return P_Bridge_User.getIBleTransaction(conf_device().defaultInitFactory.newInitTxn());
        }
        return null;
    }

    private P_TaskManager taskManager()
    {
        return getManager().getTaskManager();
    }

    private IBleManager getManager()
    {
        return m_device.getIManager();
    }

    private P_Logger logger()
    {
        return getManager().getLogger();
    }

    private int getStateMask()
    {
        return m_device.getStateMask();
    }

    private void saveLastDisconnect(final boolean explicit)
    {
        if (!m_device.is(INITIALIZED)) return;

        final boolean hitDisk = Utils_Config.bool(conf_device().manageLastDisconnectOnDisk, conf_mngr().manageLastDisconnectOnDisk);

        if (explicit)
        {
            getManager().getDiskOptionsManager().saveLastDisconnect(m_device.getMacAddress(), State.ChangeIntent.INTENTIONAL, hitDisk);
        }
        else
        {
            getManager().getDiskOptionsManager().saveLastDisconnect(m_device.getMacAddress(), State.ChangeIntent.UNINTENTIONAL, hitDisk);
        }
    }


    private final static class BondCheck
    {
        // This may not be needed anymore, but leaving it here in case we find we need it in the future.
        private boolean needsBonding = false;
        private Object[] extraBondingStates;
    }

}
