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


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import com.idevicesinc.sweetblue.annotations.Immutable;
import com.idevicesinc.sweetblue.internal.IBleDevice;
import com.idevicesinc.sweetblue.internal.P_DisconnectReason;
import com.idevicesinc.sweetblue.internal.P_Bridge_Internal;
import com.idevicesinc.sweetblue.utils.CodeHelper;
import com.idevicesinc.sweetblue.utils.Event;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.UsesCustomNull;
import com.idevicesinc.sweetblue.utils.Utils_String;
import java.util.ArrayList;


public interface DeviceReconnectFilter extends ReconnectFilter<DeviceReconnectFilter.ConnectFailEvent>
{

    /**
     * The reason for the connection failure.
     */
    enum Status implements UsesCustomNull
    {
        /**
         * Used in place of Java's built-in <code>null</code> wherever needed. As of now, the {@link ConnectFailEvent#status()} given
         * to {@link #onConnectFailed(ReconnectFilter.ConnectFailEvent)} will *never* be {@link Status#NULL}.
         */
        NULL,

        /**
         * A call was made to {@link BleDevice#connect()} or its overloads
         * but {@link ConnectFailEvent#device()} is already
         * {@link BleDeviceState#BLE_CONNECTING} or {@link BleDeviceState#BLE_CONNECTED}.
         */
        ALREADY_CONNECTING_OR_CONNECTED,

        /**
         * {@link BleDevice#connect()} (or various overloads) was called on {@link BleDevice#NULL}.
         */
        NULL_DEVICE,

        /**
         * Couldn't connect through {@link BluetoothDevice#connectGatt(android.content.Context, boolean, BluetoothGattCallback)}
         * because it (a) {@link Timing#IMMEDIATELY} returned <code>null</code>, (b) {@link Timing#EVENTUALLY} returned a bad
         * {@link ConnectFailEvent#gattStatus()}, or (c) {@link Timing#TIMED_OUT}.
         */
        NATIVE_CONNECTION_FAILED,

        /**
         * {@link BluetoothGatt#discoverServices()} either (a) {@link Timing#IMMEDIATELY} returned <code>false</code>,
         * (b) {@link Timing#EVENTUALLY} returned a bad {@link ConnectFailEvent#gattStatus()}, or (c) {@link Timing#TIMED_OUT}.
         */
        DISCOVERING_SERVICES_FAILED,

        /**
         * {@link BluetoothDevice#createBond()} either (a) {@link Timing#IMMEDIATELY} returned <code>false</code>,
         * (b) {@link Timing#EVENTUALLY} returned a bad {@link ConnectFailEvent#bondFailReason()}, or (c) {@link Timing#TIMED_OUT}.
         * <br><br>
         * NOTE: {@link BleDeviceConfig#bondingFailFailsConnection} must be <code>true</code> for this {@link Status} to be applicable.
         *
         * @see BondListener
         */
        BONDING_FAILED,

        /**
         * The {@link BleTransaction} instance passed to {@link BleDevice#connect(BleTransaction.Auth)} or
         * {@link BleDevice#connect(BleTransaction.Auth, BleTransaction.Init)} failed through {@link BleTransaction#fail()}.
         */
        AUTHENTICATION_FAILED,

        /**
         * {@link BleTransaction} instance passed to {@link BleDevice#connect(BleTransaction.Init)} or
         * {@link BleDevice#connect(BleTransaction.Auth, BleTransaction.Init)} failed through {@link BleTransaction#fail()}.
         */
        INITIALIZATION_FAILED,

        /**
         * Remote peripheral randomly disconnected sometime during the connection process. Similar to {@link #NATIVE_CONNECTION_FAILED}
         * but only occurs after the device is {@link BleDeviceState#BLE_CONNECTED} and we're going through
         * {@link BleDeviceState#DISCOVERING_SERVICES}, or {@link BleDeviceState#AUTHENTICATING}, or what have you. It might
         * be from the device turning off, or going out of range, or any other random reason.
         */
        ROGUE_DISCONNECT,

        /**
         * This status can get set if an implicit disconnect happened -- for example, {@link BleDevice#unbond()} was called while the device
         * was connected, or the MTU write test failed.
         */
        IMPLICIT_DISCONNECT,

        /**
         * {@link BleDevice#disconnect()} was called sometime during the connection process.
         */
        EXPLICIT_DISCONNECT,

        /**
         * {@link BleManager#reset()} or {@link BleManager#turnOff()} (or
         * overloads) were called sometime during the connection process.
         * Basic testing reveals that this value will also be used when a
         * user turns off BLE by going through their OS settings, airplane
         * mode, etc., but it's not absolutely *certain* that this behavior
         * is consistent across phones. For example there might be a phone
         * that kills all connections before going through the ble turn-off
         * process, in which case SweetBlue doesn't know the difference and
         * {@link #ROGUE_DISCONNECT} will be used.
         */
        BLE_TURNING_OFF;

        /**
         * Returns true for {@link #EXPLICIT_DISCONNECT} or {@link #BLE_TURNING_OFF}.
         */
        public final boolean wasCancelled()
        {
            return this == EXPLICIT_DISCONNECT || this == BLE_TURNING_OFF;
        }

        /**
         * Same as {@link #wasCancelled()}, at least for now, but just being more "explicit", no pun intended.
         */
        final boolean wasExplicit()
        {
            return wasCancelled();
        }

        /**
         * Whether this status honors a {@link com.idevicesinc.sweetblue.ReconnectFilter.ConnectFailPlease#isRetry()}. Returns <code>false</code> if {@link #wasCancelled()} or
         * <code>this</code> is {@link #ALREADY_CONNECTING_OR_CONNECTED}.
         */
        public final boolean allowsRetry()
        {
            return !this.wasCancelled() && this != ALREADY_CONNECTING_OR_CONNECTED;
        }

        @Override public final boolean isNull()
        {
            return this == NULL;
        }

        /**
         * Convenience method that returns whether this status is something that your app user would usually care about.
         * If this returns <code>true</code> then perhaps you should pop up a {@link android.widget.Toast} or something of that nature.
         */
        public final boolean shouldBeReportedToUser()
        {
            return this == NATIVE_CONNECTION_FAILED ||
                    this == DISCOVERING_SERVICES_FAILED ||
                    this == BONDING_FAILED ||
                    this == AUTHENTICATION_FAILED ||
                    this == INITIALIZATION_FAILED ||
                    this == ROGUE_DISCONNECT;
        }
    }

    /**
     * For {@link Status#NATIVE_CONNECTION_FAILED}, {@link Status#DISCOVERING_SERVICES_FAILED}, and
     * {@link Status#BONDING_FAILED}, gives further timing information on when the failure took place.
     * For all other reasons, {@link ConnectFailEvent#timing()} will be {@link #NOT_APPLICABLE}.
     */
    enum Timing
    {
        /**
         * For reasons like {@link Status#BLE_TURNING_OFF}, {@link Status#AUTHENTICATION_FAILED}, etc.
         */
        NOT_APPLICABLE,

        /**
         * The operation failed immediately, for example by the native stack method returning <code>false</code> from a method call.
         */
        IMMEDIATELY,

        /**
         * The operation failed in the native stack. {@link ConnectFailEvent#gattStatus()}
         * will probably be a positive number if {@link ConnectFailEvent#status()} is
         * {@link Status#NATIVE_CONNECTION_FAILED} or {@link Status#DISCOVERING_SERVICES_FAILED}.
         * {@link ConnectFailEvent#bondFailReason()} will probably be a positive number if
         * {@link ConnectFailEvent#status()} is {@link Status#BONDING_FAILED}.
         */
        EVENTUALLY,

        /**
         * The operation took longer than the time dictated by {@link BleDeviceConfig#taskTimeoutRequestFilter}.
         */
        TIMED_OUT;
    }

    /**
     * Structure passed to {@link #onConnectFailed(ReconnectFilter.ConnectFailEvent)} to provide more info about how/why the connection failed.
     */
    @Immutable
    final class ConnectFailEvent extends ReconnectFilter.ConnectFailEvent implements UsesCustomNull
    {
        /**
         * The {@link BleDevice} this {@link ConnectFailEvent} is for.
         */
        public final BleDevice device()
        {
            return m_device;
        }

        private final BleDevice m_device;

        /**
         * Convience to return the mac address of {@link #device()}.
         */
        public final String macAddress()
        {
            return m_device.getMacAddress();
        }

        /**
         * General reason why the connection failed.
         */
        public final Status status()
        {
            return m_status;
        }

        private final Status m_status;

        /**
         * See {@link BondListener.BondEvent#failReason()}.
         */
        public final int bondFailReason()
        {
            return m_bondFailReason;
        }

        private final int m_bondFailReason;

        /**
         * The highest state reached by the latest connection attempt.
         */
        public final BleDeviceState highestStateReached_latest()
        {
            return m_highestStateReached_latest;
        }

        private final BleDeviceState m_highestStateReached_latest;

        /**
         * The highest state reached during the whole connection attempt cycle.
         * <br><br>
         * TIP: You can use this to keep the visual feedback in your connection progress UI "bookmarked" while the connection retries
         * and goes through previous states again.
         */
        public final BleDeviceState highestStateReached_total()
        {
            return m_highestStateReached_total;
        }

        private final BleDeviceState m_highestStateReached_total;

        /**
         * Further timing information for {@link Status#NATIVE_CONNECTION_FAILED}, {@link Status#BONDING_FAILED}, and {@link Status#DISCOVERING_SERVICES_FAILED}.
         */
        public final Timing timing()
        {
            return m_timing;
        }

        private final Timing m_timing;

        /**
         * If {@link ConnectFailEvent#status()} is {@link Status#AUTHENTICATION_FAILED} or
         * {@link Status#INITIALIZATION_FAILED} and {@link BleTransaction#fail()} was called somewhere in or
         * downstream of {@link ReadWriteListener#onEvent(Event)}, then the {@link ReadWriteListener.ReadWriteEvent} passed there will be returned
         * here. Otherwise, this will return a {@link ReadWriteListener.ReadWriteEvent} for which {@link ReadWriteListener.ReadWriteEvent#isNull()} returns <code>true</code>.
         */
        public final ReadWriteListener.ReadWriteEvent txnFailReason()
        {
            return m_txnFailReason;
        }

        private final ReadWriteListener.ReadWriteEvent m_txnFailReason;

        /**
         * Returns a chronologically-ordered list of all {@link ConnectFailEvent} instances returned through
         * {@link #onConnectFailed(ReconnectFilter.ConnectFailEvent)} since the first call to {@link BleDevice#connect()},
         * including the current instance. Thus this list will always have at least a length of one (except if {@link #isNull()} is <code>true</code>).
         * The list length is "reset" back to one whenever a {@link BleDeviceState#CONNECTING_OVERALL} operation completes, either
         * through becoming {@link BleDeviceState#INITIALIZED}, or {@link BleDeviceState#BLE_DISCONNECTED} for good.
         */
        public final ConnectFailEvent[] history()
        {
            if (isNull())
            {
                return new ConnectFailEvent[0];
            }
            // We want to clear out any event after this one to prevent memory leaks from occurring. This doesn't affect the "main"
            // history, which is stored in P_ConnectionFailManager, so it's safe to do whatever we want to this list.
            ArrayList<ConnectFailEvent> history = P_Bridge_Internal.getConnectionFailHistory(m_device.getIBleDevice());
            int position = history.indexOf(this);
            if (position != -1)
            {
                ConnectFailEvent[] h = new ConnectFailEvent[position + 1];
                for (int i = 0; i <= position; i++)
                {
                    h[i] = history.get(i);
                }
                return h;
            }
            // If this event is not in the list, then this event must have been cached app-side. So, we simply return an array with this
            // event in it.
            ConnectFailEvent[] h = { this };
            return h;
        }

        ConnectFailEvent(BleDevice device, int failureCountSoFar, P_DisconnectReason reason, BleDeviceState highestStateReached_total, Interval latestAttemptTime, Interval totalAttemptTime)
        {
            super(failureCountSoFar, reason, latestAttemptTime, totalAttemptTime);

            m_device = device;
            m_status = reason.getConnectFailReason();
            m_timing = reason.getTiming();
            m_highestStateReached_latest = reason.getHighestState();
            m_highestStateReached_total = highestStateReached_total;
            m_bondFailReason = reason.getBondFailReason();
            m_txnFailReason = reason.getTxnFailReason();

            // A customer reported an NPE happening here. I would think it shouldn't be possible, as in order for SweetBlue to get native callbacks,
            // you need to have instantiated the BleManager. Perhaps this can happen when the BleManager is being instantiated, but hasn't yet finsihed
            // the initialization, so it would be null when calling BleManager.s_instance. <shrug>. Either way, null guarding this to not cause
            // an exception in future releases.
            BleManager mgr = m_device.getManager();
            if (mgr != null)
                m_device.getManager().ASSERT(highestStateReached_total != null, "highestState_total shouldn't be null.");
        }

        static ConnectFailEvent NULL(IBleDevice device)
        {
            P_DisconnectReason reason = P_Bridge_Internal.newDisconnectReason(BleStatuses.GATT_STATUS_NOT_APPLICABLE, Timing.NOT_APPLICABLE)
                    .setConnectFailReason(Status.NULL)
                    .setTxnFailReason(P_Bridge_User.newReadWriteEventNULL(device.getBleDevice()))
                    .setAutoConnectUsage(AutoConnectUsage.NOT_APPLICABLE)
                    .setBondFailReason(BleStatuses.BOND_FAIL_REASON_NOT_APPLICABLE)
                    .setHighestState(BleDeviceState.NULL);
            return new ConnectFailEvent(device.getBleDevice(), 0, reason, BleDeviceState.NULL, Interval.ZERO, Interval.ZERO);
        }

        static ConnectFailEvent EARLY_OUT(BleDevice device, Status reason)
        {
            P_DisconnectReason dReason = P_Bridge_Internal.newDisconnectReason(BleStatuses.GATT_STATUS_NOT_APPLICABLE, Timing.TIMED_OUT)
                    .setConnectFailReason(reason)
                    .setTxnFailReason(P_Bridge_User.newReadWriteEventNULL(device))
                    .setAutoConnectUsage(AutoConnectUsage.NOT_APPLICABLE)
                    .setBondFailReason(BleStatuses.BOND_FAIL_REASON_NOT_APPLICABLE)
                    .setHighestState(BleDeviceState.NULL);
            return new ConnectFailEvent(device, 0, dReason, BleDeviceState.NULL, Interval.ZERO, Interval.ZERO);
        }

        /**
         * Returns whether this {@link ConnectFailEvent} instance is a "dummy" value. For now used for
         * {@link ReconnectFilter.ConnectionLostEvent#connectionFailEvent()} in certain situations.
         */
        @Override public final boolean isNull()
        {
            return status().isNull();
        }

        /**
         * Forwards {@link Status#shouldBeReportedToUser()} using {@link #status()}.
         */
        public final boolean shouldBeReportedToUser()
        {
            return status().shouldBeReportedToUser();
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj != null && obj instanceof ConnectFailEvent)
            {
                ConnectFailEvent other = (ConnectFailEvent) obj;
                return m_device.equals(other.m_device) && m_status == other.m_status && m_timing == other.m_timing && m_highestStateReached_latest == other.m_highestStateReached_latest
                        && m_highestStateReached_total == other.m_highestStateReached_total && m_bondFailReason == other.m_bondFailReason && m_txnFailReason == other.m_txnFailReason
                        && failureCountSoFar() == other.failureCountSoFar();
            }
            return false;
        }

        @Override public final String toString()
        {
            if (isNull())
            {
                return Status.NULL.name();
            }
            else
            {
                if (status() == Status.BONDING_FAILED)
                {
                    return Utils_String.toString
                            (
                                    this.getClass(),
                                    "device", device().getName_debug(),
                                    "status", status(),
                                    "timing", timing(),
                                    "bondFailReason", CodeHelper.gattUnbondReason(bondFailReason(), true),
                                    "failureCountSoFar", failureCountSoFar()
                            );
                }
                else
                {
                    return Utils_String.toString
                            (
                                    this.getClass(),
                                    "device", device().getName_debug(),
                                    "status", status(),
                                    "timing", timing(),
                                    "gattStatus", CodeHelper.gattStatus(gattStatus(), true),
                                    "failureCountSoFar", failureCountSoFar()
                            );
                }
            }
        }
    }

}
