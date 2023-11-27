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

import static com.idevicesinc.sweetblue.BleManagerState.BLE_SCAN_READY;
import static com.idevicesinc.sweetblue.BleManagerState.OFF;
import static com.idevicesinc.sweetblue.BleManagerState.ON;
import static com.idevicesinc.sweetblue.BleManagerState.SCANNING;
import static com.idevicesinc.sweetblue.BleManagerState.TURNING_OFF;
import static com.idevicesinc.sweetblue.BleManagerState.TURNING_ON;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import com.idevicesinc.sweetblue.AddServiceListener;
import com.idevicesinc.sweetblue.BleManagerState;
import com.idevicesinc.sweetblue.BleScanApi;
import com.idevicesinc.sweetblue.BleStatuses;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.ServerReconnectFilter;
import com.idevicesinc.sweetblue.UhOhListener.UhOh;
import com.idevicesinc.sweetblue.compat.L_Util;
import com.idevicesinc.sweetblue.compat.O_Util;
import com.idevicesinc.sweetblue.compat.T_Util;
import com.idevicesinc.sweetblue.internal.android.AdapterConst;
import com.idevicesinc.sweetblue.internal.android.DeviceConst;
import com.idevicesinc.sweetblue.internal.android.IManagerListener;
import com.idevicesinc.sweetblue.internal.android.IBluetoothDevice;
import com.idevicesinc.sweetblue.internal.android.P_DeviceHolder;
import com.idevicesinc.sweetblue.utils.CodeHelper;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.State;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.Utils_String;

import java.util.ArrayList;
import java.util.List;


final class P_BleManager_ListenerProcessor implements IManagerListener.Callback
{

    // Callbacks/Listeners
    private final PA_Task.I_StateListener m_scanTaskListener = new ScanTaskListener();
    private final BroadcastReceiver m_receiver = new BluetoothReceiver();
    private final IManagerListener m_nativeListener;

    private final IBleManager m_mngr;
    private Interval m_pollRate;
    private double m_timeSinceLastPoll;
    private boolean m_checkingState;
    private int m_nativeState;



    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    P_BleManager_ListenerProcessor(IBleManager bleMngr)
    {
        m_mngr = bleMngr;

        if (Utils.isAndroid14()) {
            // Context.RECEIVER_NOT_EXPORTED was added in Android 13. However, passing this int
            // was not necessary for 13. In 14, if you do not pass this int, then the receiver won't
            // get registered, and end up crashing the application.
            T_Util.registerReceiver(m_mngr.getApplicationContext(), m_receiver, newIntentFilter());
        } else {
            m_mngr.getApplicationContext().registerReceiver(m_receiver, newIntentFilter());
        }


        m_pollRate = m_mngr.getConfigClone().defaultStatePollRate;

        m_nativeListener = m_mngr.getManagerListenerFactory().newInstance(this);
    }



    final IManagerListener getInternalListener()
    {
        return m_nativeListener;
    }

    final void updatePollRate(Interval rate)
    {
        m_pollRate = rate;
    }

    /**
     * See the copy/pasted log statements in {@link BleStatuses} for an example of how the state changes
     * occur over the course of a few seconds in Android M.
     */
    final void update(double time_Step)
    {
        if (Utils.isMarshmallow() && Interval.isEnabled(m_pollRate) && m_timeSinceLastPoll >= m_pollRate.secs())
        {
            m_checkingState = true;

            m_timeSinceLastPoll = 0.0;

            final int oldState = m_nativeState;
            final int newState = getBleState();

            if (oldState != newState)
            {
                m_nativeState = newState;

                if (oldState == BleStatuses.STATE_ON)
                {
                    if (newState == BleStatuses.STATE_TURNING_OFF || newState == BleStatuses.STATE_BLE_TURNING_OFF)
                    {
                        onNativeBleStateChange_fromPolling(BleStatuses.STATE_ON, BleStatuses.STATE_TURNING_OFF);
                    }
                    else if (newState == BleStatuses.STATE_OFF)
                    {
                        onNativeBleStateChange_fromPolling(BleStatuses.STATE_ON, BleStatuses.STATE_OFF);
                    }
                    else
                    {
                        assertOnWeirdStateChange(oldState, newState);
                    }
                }
                else if (oldState == BleStatuses.STATE_TURNING_OFF)
                {
                    if (newState == BleStatuses.STATE_ON)
                    {
                        //--- DRK > This is a "valid" case observed in pre-Android-M BroadcastReceiver callbacks.
                        //---		Down the line this will result in an UhOh and log errors and whatnot but we
                        //---		let it pass just because we did previously.
                        onNativeBleStateChange_fromPolling(BleStatuses.STATE_TURNING_OFF, BleStatuses.STATE_ON);
                    }
                    else if (newState == BleStatuses.STATE_OFF)
                    {
                        //--- DRK > Based on limited testing, we *should* get STATE_TURNING_OFF->STATE_BLE_TURNING_OFF->STATE_OFF
                        //---		but it's possible we missed STATE_BLE_TURNING_OFF so no problem, behaves just like pre-M.
                        onNativeBleStateChange_fromPolling(BleStatuses.STATE_TURNING_OFF, BleStatuses.STATE_OFF);
                    }
                    else if (newState == BleStatuses.STATE_BLE_TURNING_OFF)
                    {
                        //--- DRK > We skip this case cause we consider STATE_TURNING_OFF to be the "start"
                        //---		of the turning off process, and STATE_TURNING_OFF->STATE_BLE_TURNING_OFF to just be the "continuation".
                    }
                    else if (newState == BleStatuses.STATE_BLE_ON)
                    {
                        //--- DRK > Ignoring this because even though oddly enough it's an observed state transition, it doesn't make
                        //---		sense from the perspective of onNativeBleStateChange(). Note that it happens pretty fast so sometimes we miss it, but no big deal.
                    }
                    else if (newState == BleStatuses.STATE_BLE_TURNING_ON)
                    {
                        //--- RB > Adding this missing state. Due to the IDLE state, it's possible we miss other states in between update loops, so we must make sure
                        //--- 		to account for all states in this check. This case is a bit more odd, as we went from TURNING_OFF, to BLE_TURNING_ON. Will treat this as
                        //---		STATE_TURNING_ON (usually, that state comes before this one)

                        // Fail the Turn off task, as we're now turning on

                        m_mngr.getTaskManager().fail(P_Task_TurnBleOff.class, m_mngr);

                        onNativeBleStateChange_fromPolling(BleStatuses.STATE_TURNING_OFF, BleStatuses.STATE_TURNING_ON);
                    }
                    else if (newState == BleStatuses.STATE_TURNING_ON)
                    {
                        //--- RB > Adding this missing state. Due to the IDLE state, it's possible we miss other states in between update loops, so we must make sure
                        //--- 		to account for all states in this check.

                        // Fail the Turn off task, as we're now turning on

                        m_mngr.getTaskManager().fail(P_Task_TurnBleOff.class, m_mngr);

                        onNativeBleStateChange_fromPolling(BleStatuses.STATE_TURNING_OFF, BleStatuses.STATE_TURNING_ON);
                    }
                    else
                    {
                        assertOnWeirdStateChange(oldState, newState);
                    }
                }
                else if (oldState == BleStatuses.STATE_BLE_TURNING_OFF)
                {
                    if (newState == BleStatuses.STATE_OFF)
                    {
                        onNativeBleStateChange_fromPolling(BleStatuses.STATE_TURNING_OFF, BleStatuses.STATE_OFF);
                    }
                    else
                    {
                        assertOnWeirdStateChange(oldState, newState);
                    }
                }
                else if (oldState == BleStatuses.STATE_OFF)
                {
                    if (newState == BleStatuses.STATE_ON)
                    {
                        onNativeBleStateChange_fromPolling(BleStatuses.STATE_OFF, BleStatuses.STATE_ON);
                    }
                    else if (newState == BleStatuses.STATE_BLE_TURNING_ON || newState == BleStatuses.STATE_TURNING_ON)
                    {
                        onNativeBleStateChange_fromPolling(BleStatuses.STATE_OFF, BleStatuses.STATE_TURNING_ON);
                    }
                    else
                    {
                        assertOnWeirdStateChange(oldState, newState);
                    }
                }
                else if (oldState == BleStatuses.STATE_BLE_TURNING_ON)
                {
                    if (newState == BleStatuses.STATE_ON)
                    {
                        onNativeBleStateChange_fromPolling(BleStatuses.STATE_TURNING_ON, BleStatuses.STATE_ON);
                    }
                    else if (newState == BleStatuses.STATE_OFF)
                    {
                        //--- DRK > Have never seen this case directly but *have* seen STATE_TURNING_ON->STATE_OFF so have UhOh/logging-logic
                        //---		in place to handle it.
                        onNativeBleStateChange_fromPolling(BleStatuses.STATE_TURNING_ON, BleStatuses.STATE_OFF);
                    }
                    else if (newState == BleStatuses.STATE_TURNING_ON)
                    {
                        //--- DRK > We skip this case cause we consider STATE_BLE_TURNING_ON to be the "start"
                        //---		of the turning on process, and STATE_BLE_TURNING_ON->STATE_TURNING_ON to just be the "continuation".
                    }
                    else if (newState == BleStatuses.STATE_BLE_ON)
                    {
                        //--- DRK > Also skipping this transition because we consider it the continuation of bluetooth turning on.
                        //---		Next state should be STATE_TURNING_ON.
                    }
                    else
                    {
                        assertOnWeirdStateChange(oldState, newState);
                    }
                }
                else if (oldState == BleStatuses.STATE_TURNING_ON)
                {
                    if (newState == BleStatuses.STATE_ON)
                    {
                        onNativeBleStateChange_fromPolling(BleStatuses.STATE_TURNING_ON, BleStatuses.STATE_ON);
                    }
                    else if (newState == BleStatuses.STATE_OFF)
                    {
                        //--- DRK > "Valid" case seen in the wild pre-M. UhOhs/logging are in place to catch it.
                        onNativeBleStateChange_fromPolling(BleStatuses.STATE_TURNING_ON, BleStatuses.STATE_OFF);
                    }
                    else
                    {
                        assertOnWeirdStateChange(oldState, newState);
                    }
                }


                //--- DRK > I've put line breaks before this else-if case to emphasize how it doesn't
                //---		fit in nicely with the rest and should be looked down upon and even ridiculed.
                else if (oldState == BleStatuses.STATE_BLE_ON)
                {
                    if (newState == BleStatuses.STATE_OFF)
                    {
                        //--- DRK > This is to cover the case of STATE_ON->STATE_TURNING_OFF->STATE_BLE_ON->STATE_BLE_TURNING_OFF->STATE_OFF (see logcat in BleStatuses)
                        //---		but STATE_BLE_TURNING_OFF gets skipped for whatever reason because the timestep is large.
                        onNativeBleStateChange_fromPolling(BleStatuses.STATE_TURNING_OFF, BleStatuses.STATE_OFF);
                    }
                    else if (newState == BleStatuses.STATE_BLE_TURNING_OFF)
                    {
                        //--- DRK > Skipping because this is just the continuation of the turning off process that should have been caught earlier.
                    }
                    else if (newState == BleStatuses.STATE_TURNING_ON)
                    {
                        //--- DRK > Skipping because this is just the continuation of the bluetooth turning on process.
                    }
                    else
                    {
                        assertOnWeirdStateChange(oldState, newState);
                    }
                }
            }
            m_checkingState = false;
        }
        else if (Interval.isEnabled(m_pollRate) && m_timeSinceLastPoll < m_pollRate.secs())
        {
            m_timeSinceLastPoll += time_Step;
        }
    }

    final void onDestroy()
    {
        try
        {
            m_mngr.getApplicationContext().unregisterReceiver(m_receiver);
        } catch (Exception e)
        {
            m_mngr.getLogger().w("Tried to unregister ble listeners failed with message: " + e.getMessage());
        }
    }

    final PA_Task.I_StateListener getScanTaskListener()
    {
        return m_scanTaskListener;
    }

    final void onClassicDiscoveryFinished()
    {
        P_Task_Scan scanTask = m_mngr.getTaskManager().getCurrent(P_Task_Scan.class, m_mngr);
        boolean interrupt = false;
        if (scanTask != null)
        {
            if (scanTask.isClassicBoosted())
            {
                return;
            }
            if (m_mngr.getScanManager().isPeriodicScan())
                interrupt = true;

        }
        if (interrupt)
            m_mngr.getTaskManager().interrupt(P_Task_Scan.class, m_mngr);
        else
            // Try to succeed the task, if that fails, it means it's no longer current, so we'll clear the queue of the scan task, just to be sure it doesn't
            // start up again
            if (!m_mngr.getTaskManager().succeed(P_Task_Scan.class, m_mngr))
            {
                m_mngr.getTaskManager().clearQueueOf(P_Task_Scan.class, m_mngr);
            }
    }

    final void onNativeBleStateChangeFromBroadcastReceiver(Context context, Intent intent)
    {
        final int previousNativeState = intent.getExtras().getInt(AdapterConst.EXTRA_PREVIOUS_STATE);
        final int newNativeState = intent.getIntExtra(AdapterConst.EXTRA_STATE, AdapterConst.ERROR);

        int logLevel = newNativeState == AdapterConst.ERROR || previousNativeState == AdapterConst.ERROR ? Log.WARN : Log.INFO;
        m_mngr.getLogger().log_native(logLevel, null, Utils_String.makeString("previous=", CodeHelper.gattBleState(previousNativeState, true), " new=", CodeHelper.gattBleState(newNativeState, true)));

        if (Utils.isMarshmallow())
        {
            //--- > RB Commenting all this logic out. It's my opinion that we should never ignore native callbacks. Granted, we also poll the native
            //---       state on devices running Marshmallow or higher, but there are times when polling is insufficient (we could potentially get several
            //---       broadcasts between update ticks). So we'll just check if we're already checking the state, if not, we filter this broadcast through
            //---       to the system.
            // If in the IDLE state, we will pass the state change through as if pre 6.0, and bump out of the IDLE state
            // to catch any other changes that may happen in polling.
//            if (m_mngr.is(IDLE))
//            {
            // If we're checking the state in the update method, don't bother posting this here. We'll fall out of IDLE state, so any further changes
            // will be caught soon in polling.
            if (!m_checkingState)
            {
                onNativeBleStateChange(previousNativeState, newNativeState);
            }
//                m_mngr.m_stateTracker.update(E_Intent.INTENTIONAL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, IDLE, false);
//                m_mngr.checkIdleStatus();
//            }

            //--- > RB The below is commented out because above we are handling all states from the native callback, unless polling is currently checking
            //---       the state.
//            /*else*/ if (previousNativeState == BleStatuses.STATE_ON && newNativeState == BleStatuses.STATE_TURNING_OFF)
//            {
//                if (m_nativeState == BleStatuses.STATE_ON)
//                {
//                    m_nativeState = BleStatuses.STATE_TURNING_OFF;
//
//                    //--- DRK > We allow this code path in this particular case in marshmallow because STATE_TURNING_OFF is only active
//                    //---		for a very short time, so polling might miss it. If polling detects it before this, fine, because we
//                    //---		early-out above and never call this method. If afterwards, it skips it because m_nativeState is identical
//                    //---		to what's reported from the native stack.
//                    onNativeBleStateChange(previousNativeState, newNativeState);
//                }
//            }
        }
        else
        {
            onNativeBleStateChange(previousNativeState, newNativeState);
        }
    }

    final void onNativeBondStateChanged(final IBluetoothDevice device_native, final int previousState, final int newState, final int failReason)
    {
        m_mngr.getPostManager().runOrPostToUpdateThread(() -> {
            final IBleDevice device = getDeviceFromNativeDevice(device_native);

            if (device != null)
            {
                //--- DRK > Got an NPE here when restarting the app through the debugger. Pretty sure it's an impossible case
                //---		for actual app usage cause the listeners member of the device is final. So some memory corruption issue
                //---		associated with debugging most likely...still gating it for the hell of it.
                if (device.getListeners() != null)
                {
                    device.getListeners().onNativeBondStateChanged_updateThread(previousState, newState, failReason);
                }
            }
        });
    }


    private void onBondRequest(Intent intent)
    {
        final IBluetoothDevice layer = P_Bridge_User.newDeviceLayer(m_mngr, P_BleDeviceImpl.EMPTY_DEVICE(m_mngr));

        final P_DeviceHolder deviceHolder = P_DeviceHolder.newHolder(intent);

        layer.setNativeDevice(deviceHolder.getDevice(), deviceHolder);

        onNativeBondRequest(layer);
    }

    private void onNativeBondRequest(final IBluetoothDevice device_native)
    {
        final String macAddress = device_native != null && device_native.getAddress() != null ? device_native.getAddress() : null;
        m_mngr.getLogger().log_native(Log.INFO, macAddress, "Bond request served.");
        m_mngr.getPostManager().runOrPostToUpdateThread(() -> {
            final IBleDevice device = getDeviceFromNativeDevice(device_native);

            if (device != null)
            {
                if (device.getListeners() != null)
                {
                    device.getListeners().onNativeBondRequest_updateThread(device);
                }
            }
        });
    }

    private static IntentFilter newIntentFilter()
    {
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(AdapterConst.ACTION_STATE_CHANGED);
        intentFilter.addAction(DeviceConst.ACTION_BOND_STATE_CHANGED);

        intentFilter.addAction(DeviceConst.ACTION_ACL_CONNECTED);
        intentFilter.addAction(DeviceConst.ACTION_ACL_DISCONNECT_REQUESTED);
        intentFilter.addAction(DeviceConst.ACTION_ACL_DISCONNECTED);

        intentFilter.addAction(DeviceConst.ACTION_PAIRING_REQUEST);

        intentFilter.addAction(DeviceConst.ACTION_FOUND);
        intentFilter.addAction(AdapterConst.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(DeviceConst.ACTION_UUID);
        intentFilter.addAction(DeviceConst.ACTION_DISAPPEARED);

        return intentFilter;
    }

    private void onDeviceFound_classic(Context context, Intent intent)
    {
        // If this was discovered via the hack to show the bond popup, then do not propagate this
        // any further, as this scan is JUST to get the dialog to pop up (rather than show in the notification area)
        P_Task_BondPopupHack hack = m_mngr.getTaskManager().getCurrent(P_Task_BondPopupHack.class, m_mngr);

        // Only pipe discovery event if the scan task is running, and the manager says we're doing a classic scan
        P_Task_Scan scan = m_mngr.getTaskManager().getCurrent(P_Task_Scan.class, m_mngr);
        if (hack == null && scan != null && m_mngr.getConfigClone().scanApi == BleScanApi.CLASSIC)
        {
            final P_DeviceHolder deviceHolder = P_DeviceHolder.newHolder(intent);

            final String macAddress = deviceHolder.getAddress();
            m_mngr.getLogger().log_native(Log.VERBOSE, macAddress, "Discovered device via CLASSIC scan.");

            final int rssi = intent.getShortExtra(DeviceConst.EXTRA_RSSI, Short.MIN_VALUE);

            final IBluetoothDevice layer = P_Bridge_User.newDeviceLayer(m_mngr, P_BleDeviceImpl.EMPTY_DEVICE(m_mngr));
            layer.setNativeDevice(deviceHolder.getDevice(), deviceHolder);

            final List<P_ScanManager.DiscoveryEntry> entries = new ArrayList<>(1);
            entries.add(new P_ScanManager.DiscoveryEntry(layer, rssi, null));

            m_mngr.onDiscoveredFromNativeStack(entries);
        }
    }

    private void onNativeBleStateChange(int previousNativeState, int newNativeState)
    {
        //--- DRK > Checking for inconsistent state at this point (instead of at bottom of function),
        //---		simply because where this is where it was first observed. Checking at the bottom
        //---		may not work because maybe this bug relied on a race condition.
        //---		UPDATE: Not checking for inconsistent state anymore cause it can be legitimate due to native
        //---		state changing while call to this method is sitting on the update thread queue.
        final int adapterState = m_mngr.managerLayer().getState();

//		boolean inconsistentState = adapterState != newNativeState;
        PA_StateTracker.E_Intent intent = PA_StateTracker.E_Intent.INTENTIONAL;
        final boolean hitErrorState = newNativeState == AdapterConst.ERROR;

        if (hitErrorState)
        {
            newNativeState = adapterState;

            if (newNativeState /*still*/ == AdapterConst.ERROR)
            {
                return; // really not sure what we can do better here besides bailing out.
            }
        }
        else if (newNativeState == AdapterConst.STATE_OFF)
        {
            m_mngr.getWakeLockManager().clear();

            m_mngr.clearTimeTurnedOn();

            if (m_mngr.getTaskManager().isCurrent(P_Task_TurnBleOn.class, m_mngr))
            {
                return;
            }

            // If for some reason, we missed the turning off state before getting the off state, then make sure to call
            // the methods we usually do in the turning off state.
            if (previousNativeState != AdapterConst.STATE_TURNING_OFF)
                intent = handleBleTurningOff(intent);

            m_mngr.getTaskManager().fail(P_Task_TurnBleOn.class, m_mngr);
            P_Task_TurnBleOff turnOffTask = m_mngr.getTaskManager().getCurrent(P_Task_TurnBleOff.class, m_mngr);
            intent = turnOffTask == null || turnOffTask.isImplicit() ? PA_StateTracker.E_Intent.UNINTENTIONAL : intent;
            m_mngr.getTaskManager().succeed(P_Task_TurnBleOff.class, m_mngr);

            //--- DRK > Should have already been handled by the "turning off" event, but this is just to be
            //---		sure all devices are cleared in case something weird happens and we go straight
            //---		from ON to OFF or something.
            m_mngr.getDeviceManager().undiscoverAllForTurnOff(m_mngr.getDeviceManager_cache(), intent);

            // If there's a scan running when ble gets turned off, we set a flag here so we don't end up clearing
            // the current scan options
            P_Task_Scan scanTask = m_mngr.getTaskManager().getCurrent(P_Task_Scan.class, m_mngr);
            if (scanTask != null)
                scanTask.stopForBleTurnOff();
            else
                m_mngr.getTaskManager().clearQueueOf(P_Task_Scan.class, m_mngr);

            // We need to make sure to remove the transitory states, in case they were missed, and to enforce the ON/OFF state to get propagated app-side
            m_mngr.getStateTracker().update(intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE, TURNING_OFF, false, TURNING_ON, false, OFF, true, ON, false, BLE_SCAN_READY, false, SCANNING, false);
        }
        else if (newNativeState == AdapterConst.STATE_TURNING_ON)
        {
            if (!m_mngr.getTaskManager().isCurrent(P_Task_TurnBleOn.class, m_mngr))
            {
                m_mngr.getTaskManager().add(new P_Task_TurnBleOn(m_mngr, /*implicit=*/true));
                intent = PA_StateTracker.E_Intent.UNINTENTIONAL;
            }

            m_mngr.getTaskManager().fail(P_Task_TurnBleOff.class, m_mngr);
        }
        else if (newNativeState == AdapterConst.STATE_ON)
        {
            m_mngr.getTaskManager().fail(P_Task_TurnBleOff.class, m_mngr);
            P_Task_TurnBleOn turnOnTask = m_mngr.getTaskManager().getCurrent(P_Task_TurnBleOn.class, m_mngr);
            intent = turnOnTask == null || turnOnTask.isImplicit() ? PA_StateTracker.E_Intent.UNINTENTIONAL : intent;
            m_mngr.getTaskManager().succeed(P_Task_TurnBleOn.class, m_mngr);

            // We need to make sure to remove the transitory states, in case they were missed, and to enforce the ON/OFF state to get propagated app-side
            m_mngr.getStateTracker().update(intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE, TURNING_OFF, false, TURNING_ON, false, ON, true, OFF, false);
        }
        else if (newNativeState == AdapterConst.STATE_TURNING_OFF)
        {
            intent = handleBleTurningOff(intent);
        }

        //--- DRK > Can happen I suppose if newNativeState is an error and we revert to using the queried state and it's the same as previous state.
        //----		Below logic should still be resilient to this, but early-outing just in case.
        if (previousNativeState == newNativeState)
        {
            return;
        }

        BleManagerState previousState = P_Bridge_User.getState(previousNativeState);
        BleManagerState newState = P_Bridge_User.getState(newNativeState);

        m_mngr.getLogger().e(previousNativeState + " " + newNativeState + " " + previousState + " " + newState);

        m_mngr.getStateTracker().update(intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE, previousState, false, newState, true);
        m_mngr.getStateTracker().update_native(newNativeState);

        // If BT is now off, and the manager thinks it's still in the scan ready state, then remove the scan ready state.
        if (newNativeState == AdapterConst.STATE_OFF && m_mngr.is(BLE_SCAN_READY))
        {
            m_mngr.getStateTracker().update(intent, BleStatuses.GATT_STATUS_NOT_APPLICABLE, BLE_SCAN_READY, false);
        }

        if (previousNativeState != AdapterConst.STATE_ON && newNativeState == AdapterConst.STATE_ON)
        {
            m_mngr.getDeviceManager().rediscoverDevicesAfterBleTurningBackOn();
            m_mngr.getDeviceManager().reconnectDevicesAfterBleTurningBackOn();
        }

        if (hitErrorState)
        {
            m_mngr.uhOh(UhOh.UNKNOWN_BLE_ERROR);
        }

        if (previousNativeState == AdapterConst.STATE_TURNING_OFF && newNativeState == AdapterConst.STATE_ON)
        {
            m_mngr.uhOh(UhOh.CANNOT_DISABLE_BLUETOOTH);
        }
        else if (previousNativeState == AdapterConst.STATE_TURNING_ON && newNativeState == AdapterConst.STATE_OFF)
        {
            m_mngr.uhOh(UhOh.CANNOT_ENABLE_BLUETOOTH);
        }
//		else if( inconsistentState )
//		{
//			m_mngr.uhOh(UhOh.INCONSISTENT_NATIVE_BLE_STATE);
//			m_mngr.getLogger().w("adapterState=" + m_mngr.getLogger().gattBleState(adapterState) + " newState=" + m_mngr.getLogger().gattBleState(newNativeState));
//		}
    }

    private PA_StateTracker.E_Intent handleBleTurningOff(PA_StateTracker.E_Intent intent)
    {
        if (!m_mngr.getTaskManager().isCurrent(P_Task_TurnBleOff.class, m_mngr))
        {
            m_mngr.getDeviceManager().disconnectAllForTurnOff(PE_TaskPriority.CRITICAL);

//				m_mngr.m_deviceMngr.undiscoverAllForTurnOff(m_mngr.m_deviceMngr_cache, E_Intent.UNINTENTIONAL);
            m_mngr.getTaskManager().add(new P_Task_TurnBleOff(m_mngr, /*implicit=*/true));

            if (m_mngr.hasServerInstance())
            {
                m_mngr.getServer().disconnect_internal(AddServiceListener.Status.CANCELLED_FROM_BLE_TURNING_OFF, ServerReconnectFilter.Status.CANCELLED_FROM_BLE_TURNING_OFF, State.ChangeIntent.UNINTENTIONAL);
            }

            intent = PA_StateTracker.E_Intent.UNINTENTIONAL;
        }

        m_mngr.getTaskManager().fail(P_Task_TurnBleOn.class, m_mngr);

        return intent;
    }

    private void onNativeBondStateChanged(Context context, Intent intent)
    {
        final int previousState = intent.getIntExtra(DeviceConst.EXTRA_PREVIOUS_BOND_STATE, DeviceConst.ERROR);
        final int newState = intent.getIntExtra(DeviceConst.EXTRA_BOND_STATE, DeviceConst.ERROR);
        final P_DeviceHolder deviceHolder = P_DeviceHolder.newHolder(intent);
        int logLevel = newState == DeviceConst.ERROR || previousState == DeviceConst.ERROR ? Log.WARN : Log.INFO;
        final String macAddress = deviceHolder.getAddress();
        m_mngr.getLogger().log_native(logLevel, macAddress, Utils_String.makeString("previous=", CodeHelper.gattBondState(previousState, true), " new=", CodeHelper.gattBondState(newState, true)));

        final int failReason;

        if (newState == DeviceConst.BOND_NONE)
        {
            //--- DRK > Can't access BluetoothDevice.EXTRA_REASON cause of stupid @hide annotation, so hardcoding string here.
            failReason = intent.getIntExtra(DeviceConst.EXTRA_REASON, DeviceConst.ERROR);

            if (failReason != BleStatuses.BOND_SUCCESS)
            {
                m_mngr.getLogger().w_native(CodeHelper.gattUnbondReason(failReason, true));
            }
        }
        else
        {
            failReason = BleStatuses.BOND_FAIL_REASON_NOT_APPLICABLE;
        }

        final IBluetoothDevice layer = P_Bridge_User.newDeviceLayer(m_mngr, P_BleDeviceImpl.EMPTY_DEVICE(m_mngr));

        layer.setNativeDevice(deviceHolder.getDevice(), deviceHolder);

        onNativeBondStateChanged(layer, previousState, newState, failReason);
    }

    private IBleDevice getDeviceFromNativeDevice(final IBluetoothDevice device_native)
    {
        IBleDevice device = m_mngr.getDevice(device_native.getAddress());

        if (device == null)
        {
            final P_Task_Bond bondTask = m_mngr.getTaskManager().getCurrent(P_Task_Bond.class, m_mngr);

            if (bondTask != null)
            {
                if (bondTask.getDevice().getMacAddress().equals(device_native.getAddress()))
                {
                    device = bondTask.getDevice();
                }
            }
        }

        if (device /*still*/ == null)
        {
            final P_Task_Unbond unbondTask = m_mngr.getTaskManager().getCurrent(P_Task_Unbond.class, m_mngr);

            if (unbondTask != null)
            {
                if (unbondTask.getDevice().getMacAddress().equals(device_native.getAddress()))
                {
                    device = unbondTask.getDevice();
                }
            }
        }

        return device;
    }

    private static boolean isBleStateFromPreM(final int state)
    {
        return
                state == BleStatuses.STATE_ON ||
                        state == BleStatuses.STATE_TURNING_OFF ||
                        state == BleStatuses.STATE_OFF ||
                        state == BleStatuses.STATE_TURNING_ON;
    }

    private void assertOnWeirdStateChange(final int oldState, final int newState)
    {
        //--- DRK > Note this is not an assert SweetBlue-logic-wise...just want to call out attention to state changes that I assumed were impossible.
        //---		That said I will not be surprised if this trips.
        m_mngr.ASSERT(false, "Weird BLE state change detected from polling: " + CodeHelper.gattBleState(oldState, true) + " -> " + CodeHelper.gattBleState(newState, true));

        //--- RB > We still want to update the native state here, even if it's a state change we weren't expecting to get. This will probably happen more often now that the IDLE
        //---       state is in, which means it's possible we miss more state changes as a result
        onNativeBleStateChange_fromPolling(oldState, newState);
    }

    private void onNativeBleStateChange_fromPolling(final int oldState, final int newState)
    {
        if (false == isBleStateFromPreM(oldState) || false == isBleStateFromPreM(newState))
        {
            m_mngr.ASSERT(false, "Either " + CodeHelper.gattBleState(oldState, true) + " or " + CodeHelper.gattBleState(newState, true) + " are not valid pre-M BLE states!");
        }
        else
        {
            onNativeBleStateChange(oldState, newState);
        }
    }

    private int getBleState()
    {
        if (Utils.isMarshmallow() && m_mngr.getNativeAdapter() != null)
        {
            return m_mngr.managerLayer().getBleState();
        }
        else
        {
            return m_mngr.managerLayer().getState();
        }
    }

    @Override
    public final void onScanResult(int callbackType, L_Util.ScanResult result)
    {
        m_mngr.getScanManager().onScanResult(callbackType, result);
    }

    @Override
    public final void onBatchScanResult(int callbackType, List<L_Util.ScanResult> results)
    {
        m_mngr.getScanManager().onBatchScanResult(callbackType, results);
    }

    @Override
    public final void onScanFailed(int errorCode)
    {
        m_mngr.getScanManager().onScanFailed(errorCode);
    }

    private final class ScanTaskListener implements PA_Task.I_StateListener
    {
        @Override
        public final void onStateChange(PA_Task task, PE_TaskState state)
        {
            if (task.getState().ordinal() <= PE_TaskState.QUEUED.ordinal()) return;

            if (state.isEndingState())
            {
                final P_Task_Scan scanTask = (P_Task_Scan) task;
                final double totalTimeExecuting = scanTask.getTotalTimeExecuting();

                if (state == PE_TaskState.INTERRUPTED || state == PE_TaskState.TIMED_OUT || state == PE_TaskState.SUCCEEDED)
                {
                    if (state == PE_TaskState.INTERRUPTED)
                    {
                        m_mngr.getPostManager().runOrPostToUpdateThread(() -> m_mngr.tryPurgingStaleDevices(totalTimeExecuting));
                    }
                    else
                    {
                        m_mngr.tryPurgingStaleDevices(totalTimeExecuting);
                    }
                }

                // If the scan task was stopped due to ble turning off, return here so we don't reset the scan options in the scan manager.
                if (scanTask.wasStoppedForBleTurnOff())
                    return;

                m_mngr.getScanManager().stopNativeScan(scanTask);

                if (state == PE_TaskState.INTERRUPTED)
                {
                    // task will be put back onto the queue presently...nothing to do here
                }
                else
                {
                    m_mngr.clearScanningRelatedMembers(scanTask.isExplicit() ? PA_StateTracker.E_Intent.INTENTIONAL : PA_StateTracker.E_Intent.UNINTENTIONAL);
                }
            }
        }
    }

    private final class BluetoothReceiver extends BroadcastReceiver
    {
        @Override
        public final void onReceive(final Context context, final Intent intent)
        {
            final String action = intent.getAction();

            if (action.equals(AdapterConst.ACTION_STATE_CHANGED))
            {
                onNativeBleStateChangeFromBroadcastReceiver(context, intent);
            }
            else if (action.equals(DeviceConst.ACTION_BOND_STATE_CHANGED))
            {
                onNativeBondStateChanged(context, intent);
            }
            else if (action.equals(DeviceConst.ACTION_FOUND))
            {
                onDeviceFound_classic(context, intent);
            }
            else if (action.equals(AdapterConst.ACTION_DISCOVERY_FINISHED))
            {
                onClassicDiscoveryFinished();
            }
            else if (action.equals(DeviceConst.ACTION_PAIRING_REQUEST))
            {
                onBondRequest(intent);
            }

            //--- DRK > This block doesn't do anything...just wrote it to see how these other events work and if they're useful.
            //---		They don't seem to be but leaving it here for the future if needed anyway.
            else if (action.contains("ACL") || action.equals(DeviceConst.ACTION_UUID) || action.equals(DeviceConst.ACTION_DISAPPEARED))
            {
//                final BluetoothDevice device_native = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//
//                if (action.equals(BluetoothDevice.ACTION_FOUND))
//                {
////					device_native.fetchUuidsWithSdp();
//                }
//                else if (action.equals(BluetoothDevice.ACTION_UUID))
//                {
//                    m_mngr.getLogger().d("");
//                }

//                BleDevice device = m_mngr.getDevice(device_native.getAddress());
//                if (device != null)
//                {
//					m_mngr.getLogger().e("Known device " + device.getDebugName() + " " + action);
//                }
//                else
//                {
//					m_mngr.getLogger().e("Mystery device " + device_native.getName() + " " + device_native.getAddress() + " " + action);
//                }
            }
        }
    }
}
