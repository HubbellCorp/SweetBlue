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


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.internal.P_Bridge_Internal;
import com.idevicesinc.sweetblue.utils.CodeHelper;
import com.idevicesinc.sweetblue.utils.Event;
import com.idevicesinc.sweetblue.utils.GenericListener_Void;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.P_Const;
import com.idevicesinc.sweetblue.utils.PresentData;
import com.idevicesinc.sweetblue.utils.UsesCustomNull;
import com.idevicesinc.sweetblue.utils.Utils_Byte;
import com.idevicesinc.sweetblue.utils.Utils_String;
import com.idevicesinc.sweetblue.utils.Uuids;
import java.util.Arrays;
import java.util.UUID;

/**
 * Convenience interface for listening for Notifications/Indications only.
 */
public interface NotificationListener extends GenericListener_Void<NotificationListener.NotificationEvent>
{

    /**
     * A value returned to {@link NotificationListener#onEvent(Event)}
     * by way of {@link NotificationListener.NotificationEvent#status} that indicates success of the
     * operation or the reason for its failure. This enum is <i>not</i>
     * meant to match up with {@link BluetoothGatt}.GATT_* values in any way.
     *
     * @see NotificationListener.NotificationEvent#status()
     */
    enum Status implements UsesCustomNull
    {
        /**
         * As of now, not used.
         */
        NULL,

        /**
         * This is used to indicate that toggling a notification/indication was successful.
         */
        SUCCESS,

        /**
         * {@link BleDevice#read(UUID, ReadWriteListener)}, {@link BleDevice#write(UUID, byte[])},
         * {@link BleDevice#enableNotify(UUID, ReadWriteListener)}, etc. was called on {@link BleDevice#NULL}.
         */
        NULL_DEVICE,

        /**
         * Device is not {@link BleDeviceState#BLE_CONNECTED}.
         */
        NOT_CONNECTED,

        /**
         * Couldn't find a matching {@link ReadWriteListener.ReadWriteEvent#target} for the {@link ReadWriteListener.ReadWriteEvent#charUuid} (or
         * {@link ReadWriteListener.ReadWriteEvent#descUuid} if {@link ReadWriteListener.ReadWriteEvent#target} is {@link ReadWriteListener.Target#DESCRIPTOR}) which was given to
         * {@link BleDevice#read(UUID, ReadWriteListener)}, {@link BleDevice#write(UUID, byte[])}, etc. This most likely
         * means that the internal call to {@link BluetoothGatt#discoverServices()} didn't find any
         * {@link BluetoothGattService} that contained a {@link BluetoothGattCharacteristic} for {@link ReadWriteListener.ReadWriteEvent#charUuid()}.
         * This can also happen if the internal call to get a BluetoothService(s) causes an exception (seen on some phones).
         */
        NO_MATCHING_TARGET,

        /**
         * You tried to do a read on a characteristic that is write-only, or
         * vice-versa, or tried to read a notify-only characteristic, or etc., etc.
         */
        OPERATION_NOT_SUPPORTED,

        /**
         * The android api level doesn't support the lower level API call in the native stack. For example if you try to use
         * {@link BleDevice#negotiateMtu(int, ReadWriteListener)}, which requires API level 21, and you are at level 18.
         */
        ANDROID_VERSION_NOT_SUPPORTED,

        /**
         * The operation was cancelled by the device becoming {@link BleDeviceState#BLE_DISCONNECTED}.
         */
        CANCELLED_FROM_DISCONNECT,

        /**
         * The operation was cancelled because {@link BleManager} went {@link BleManagerState#TURNING_OFF} and/or
         * {@link BleManagerState#OFF}. Note that if the user turns off BLE from their OS settings (airplane mode, etc.) then
         * {@link ReadWriteListener.ReadWriteEvent#status} could potentially be {@link #CANCELLED_FROM_DISCONNECT} because SweetBlue might get
         * the disconnect callback before the turning off callback. Basic testing has revealed that this is *not* the case, but you never know.
         * <br><br>
         * Either way, the device was or will be disconnected.
         */
        CANCELLED_FROM_BLE_TURNING_OFF,

        /**
         * Used either when {@link ReadWriteListener.ReadWriteEvent#type()} {@link ReadWriteListener.Type#isRead()} and the stack returned a <code>null</code>
         * value for {@link BluetoothGattCharacteristic#getValue()} despite the operation being otherwise "successful", <i>or</i>
         * {@link BleDevice#write(UUID, byte[])} (or overload(s) ) were called with a null data parameter. For the read case, the library
         * will throw an {@link UhOhListener.UhOh#READ_RETURNED_NULL}, but hopefully it was just a temporary glitch. If the problem persists try {@link BleManager#reset()}.
         */
        NULL_DATA,

        /**
         * Used either when {@link ReadWriteListener.ReadWriteEvent#type} {@link ReadWriteListener.Type#isRead()} and the operation was "successful" but
         * returned a zero-length array for {@link ReadWriteListener.ReadWriteEvent#data}, <i>or</i> {@link BleDevice#write(UUID, byte[])} (or overload(s) )
         * was called with a non-null but zero-length data parameter. Note that {@link ReadWriteListener.ReadWriteEvent#data} will be a zero-length array for
         * all other error statuses as well, for example {@link #NO_MATCHING_TARGET}, {@link #NOT_CONNECTED}, etc. In other words it's never null.
         */
        EMPTY_DATA,

        /**
         * For now only used when giving a negative or zero value to {@link BleDevice#negotiateMtu(int, ReadWriteListener)}.
         */
        INVALID_DATA,

        /**
         * {@link BluetoothGatt#setCharacteristicNotification(BluetoothGattCharacteristic, boolean)}
         * returned <code>false</code> for an unknown reason.
         */
        FAILED_TO_TOGGLE_NOTIFICATION,

        /**
         * The operation failed in a "normal" fashion, at least relative to all the other strange ways an operation can fail. This means for
         * example that {@link BluetoothGattCallback#onCharacteristicRead(BluetoothGatt, BluetoothGattCharacteristic, int)}
         * returned a status code that was not zero. This could mean the device went out of range, was turned off, signal was disrupted,
         * whatever. Often this means that the device is about to become {@link BleDeviceState#BLE_DISCONNECTED}. {@link ReadWriteListener.ReadWriteEvent#gattStatus()}
         * will most likely be non-zero, and you can check against the static fields in {@link BleStatuses} for more information.
         *
         * @see ReadWriteListener.ReadWriteEvent#gattStatus()
         */
        REMOTE_GATT_FAILURE,

        /**
         * This is a generic error state which means something went wrong, and we're not sure why. In theory, this should never be the status of a Notification/Indication.
         * If you DO see this status, please let us know at SweetBlue@idevicesinc.com know how you produce it.
         */
        UNKNOWN_ERROR;

        @Override public boolean isNull()
        {
            return this == NULL;
        }

        public static Status fromReadWriteStatus(ReadWriteListener.Status status)
        {
            switch (status)
            {
                case NULL:
                    return NULL;
                case SUCCESS:
                    return SUCCESS;
                case ANDROID_VERSION_NOT_SUPPORTED:
                    return ANDROID_VERSION_NOT_SUPPORTED;
                case CANCELLED_FROM_BLE_TURNING_OFF:
                    return CANCELLED_FROM_BLE_TURNING_OFF;
                case CANCELLED_FROM_DISCONNECT:
                    return CANCELLED_FROM_DISCONNECT;
                case EMPTY_DATA:
                    return EMPTY_DATA;
                case NULL_DATA:
                    return NULL_DATA;
                case INVALID_DATA:
                    return INVALID_DATA;
                case FAILED_TO_TOGGLE_NOTIFICATION:
                    return FAILED_TO_TOGGLE_NOTIFICATION;
                case NO_MATCHING_TARGET:
                    return NO_MATCHING_TARGET;
                case NOT_CONNECTED:
                    return NOT_CONNECTED;
                case OPERATION_NOT_SUPPORTED:
                    return OPERATION_NOT_SUPPORTED;
                case REMOTE_GATT_FAILURE:
                    return REMOTE_GATT_FAILURE;
                default:
                    return UNKNOWN_ERROR;
            }
        }
    }

    /**
     * The type of operation for a {@link ReadWriteListener.ReadWriteEvent} - read, write, poll, etc.
     */
    enum Type implements UsesCustomNull
    {
        /**
         * As of now, only used for {@link DeviceReconnectFilter.ConnectFailEvent#txnFailReason()} in some cases.
         */
        NULL,

        /**
         * Associated with {@link BleDevice#enableNotify(UUID, ReadWriteListener)} when we  actually get a notification.
         */
        NOTIFICATION,

        /**
         * Similar to {@link #NOTIFICATION}, kicked off from {@link BleDevice#enableNotify(UUID, ReadWriteListener)}, but
         * under the hood this is treated slightly differently.
         */
        INDICATION,

        /**
         * Associated with {@link BleDevice#startChangeTrackingPoll(UUID, Interval, ReadWriteListener)}
         * or {@link BleDevice#enableNotify(UUID, Interval, ReadWriteListener)} where a force-read timeout is invoked.
         */
        PSEUDO_NOTIFICATION,

        /**
         * Associated with {@link BleDevice#enableNotify(UUID, ReadWriteListener)} and called when enabling the notification completes by writing to the
         * Descriptor of the given {@link UUID}. {@link ReadWriteListener.Status#SUCCESS} doesn't <i>necessarily</i> mean that notifications will
         * definitely now work (there may be other issues in the underlying stack), but it's a reasonable guarantee.
         */
        ENABLING_NOTIFICATION,

        /**
         * Opposite of {@link #ENABLING_NOTIFICATION}.
         */
        DISABLING_NOTIFICATION;


        /**
         * Returns <code>true</code> only for {@link #NOTIFICATION} and {@link #INDICATION}, i.e. only
         * notifications whose origin is an *actual* notification (or indication) sent from the remote BLE device (as opposed to
         * a {@link #PSEUDO_NOTIFICATION}).
         */
        public boolean isNativeNotification()
        {
            return this == NOTIFICATION || this == INDICATION;
        }

        /**
         * Returns the {@link BleNodeConfig.HistoricalDataLogFilter.Source} equivalent
         * for this {@link NotificationListener.Type}, or {@link BleNodeConfig.HistoricalDataLogFilter.Source#NULL}.
         */
        public BleNodeConfig.HistoricalDataLogFilter.Source toHistoricalDataSource()
        {
            switch (this)
            {
                case NOTIFICATION:
                    return BleNodeConfig.HistoricalDataLogFilter.Source.NOTIFICATION;
                case INDICATION:
                    return BleNodeConfig.HistoricalDataLogFilter.Source.INDICATION;
                case PSEUDO_NOTIFICATION:
                    return BleNodeConfig.HistoricalDataLogFilter.Source.PSUEDO_NOTIFICATION;
            }

            return BleNodeConfig.HistoricalDataLogFilter.Source.NULL;
        }

        @Override public boolean isNull()
        {
            return this == NULL;
        }
    }

    /**
     * Provides a bunch of information about a notification.
     */
    @com.idevicesinc.sweetblue.annotations.Immutable
    class NotificationEvent extends com.idevicesinc.sweetblue.utils.Event implements com.idevicesinc.sweetblue.utils.UsesCustomNull
    {

        /**
         * Value used in place of <code>null</code>.
         */
        public static final UUID NON_APPLICABLE_UUID = Uuids.INVALID;

        /**
         * The {@link BleDevice} this {@link NotificationEvent} is for.
         */
        public BleDevice device()
        {
            return m_device;
        }

        private final BleDevice m_device;

        /**
         * Convience to return the mac address of {@link #device()}.
         */
        public String macAddress()
        {
            return m_device.getMacAddress();
        }

        /**
         * The type of operation, read, write, etc.
         */
        public Type type()
        {
            return m_type;
        }

        private final Type m_type;

        /**
         * The {@link UUID} of the service associated with this {@link NotificationEvent}. This will always be a non-null {@link UUID}.
         */
        public UUID serviceUuid()
        {
            return m_serviceUuid;
        }

        private final UUID m_serviceUuid;

        /**
         * The {@link UUID} of the characteristic associated with this {@link NotificationEvent}. This will always be a non-null {@link UUID}.
         */
        public UUID charUuid()
        {
            return m_charUuid;
        }

        private final UUID m_charUuid;

        /**
         * The data received from the peripheral. This will never be <code>null</code>. For error cases it will be a
         * zero-length array.
         */
        public @Nullable(Nullable.Prevalence.NEVER) byte[] data()
        {
            return m_data;
        }

        private final byte[] m_data;

        /**
         * Indicates either success or the type of failure.
         */
        public Status status()
        {
            return m_status;
        }

        private final Status m_status;

        /**
         * Time spent "over the air" - so in the native stack, processing in
         * the peripheral's embedded software, what have you. This will
         * always be slightly less than {@link #time_total()}.
         */
        public Interval time_ota()
        {
            return m_transitTime;
        }

        private final Interval m_transitTime;

        /**
         * Total time it took for the operation to complete, whether success
         * or failure. This mainly includes time spent in the internal job
         * queue plus {@link ReadWriteListener.ReadWriteEvent#time_ota()}. This will always be
         * longer than {@link #time_ota()}, though usually only slightly so.
         */
        public Interval time_total()
        {
            return m_totalTime;
        }

        private final Interval m_totalTime;

        /**
         * The native gatt status returned from the stack, if applicable. If the {@link #status} returned is, for example,
         * {@link ReadWriteListener.Status#NO_MATCHING_TARGET}, then the operation didn't even reach the point where a gatt status is
         * provided, in which case this member is set to {@link BleStatuses#GATT_STATUS_NOT_APPLICABLE} (value of
         * {@value com.idevicesinc.sweetblue.BleStatuses#GATT_STATUS_NOT_APPLICABLE}). Otherwise it will be <code>0</code> for success or greater than
         * <code>0</code> when there's an issue. <i>Generally</i> this value will only be meaningful when {@link #status} is
         * {@link ReadWriteListener.Status#SUCCESS} or {@link ReadWriteListener.Status#REMOTE_GATT_FAILURE}. There are
         * also some cases where this will be 0 for success but {@link #status} is for example
         * {@link ReadWriteListener.Status#NULL_DATA} - in other words the underlying stack deemed the operation a success but SweetBlue
         * disagreed. For this reason it's recommended to treat this value as a debugging tool and use {@link #status} for actual
         * application logic if possible.
         * <br><br>
         * See {@link BluetoothGatt} for its static <code>GATT_*</code> status code members. Also see the source code of
         * {@link BleStatuses} for SweetBlue's more comprehensive internal reference list of gatt status values. This list may not be
         * totally accurate or up-to-date, nor may it match GATT_ values used by the bluetooth stack on your phone.
         */
        public int gattStatus()
        {
            return m_gattStatus;
        }

        private final int m_gattStatus;

        /**
         * This returns <code>true</code> if this event was the result of an explicit call through SweetBlue, e.g. through
         * {@link BleDevice#read(UUID)}, {@link BleDevice#write(UUID, byte[])}, etc. It will return <code>false</code> otherwise,
         * which can happen if for example you use {@link BleDevice#getNativeGatt()} to bypass SweetBlue for whatever reason.
         * Another theoretical case is if you make an explicit call through SweetBlue, then you get {@link com.idevicesinc.sweetblue.ReadWriteListener.Status#TIMED_OUT},
         * but then the native stack eventually *does* come back with something - this has never been observed, but it is possible.
         */
        public boolean solicited()
        {
            return m_solicited;
        }

        private final boolean m_solicited;


        NotificationEvent(BleDevice device, UUID serviceUuid, UUID charUuid, NotificationListener.Type type, byte[] data, NotificationListener.Status status, int gattStatus, double totalTime, double transitTime, boolean solicited)
        {
            this.m_device = device;
            this.m_serviceUuid = serviceUuid != null ? serviceUuid : NON_APPLICABLE_UUID;
            this.m_charUuid = charUuid != null ? charUuid : NON_APPLICABLE_UUID;
            this.m_type = type;
            this.m_status = status;
            this.m_gattStatus = gattStatus;
            this.m_totalTime = Interval.secs(totalTime);
            this.m_transitTime = Interval.secs(transitTime);
            this.m_data = data != null ? data : P_Const.EMPTY_BYTE_ARRAY;
            this.m_solicited = solicited;
        }


        static NotificationEvent NULL(BleDevice device)
        {
            return new NotificationEvent(device, NON_APPLICABLE_UUID, NON_APPLICABLE_UUID, NotificationListener.Type.NULL, P_Const.EMPTY_BYTE_ARRAY, NotificationListener.Status.NULL, BleStatuses.GATT_STATUS_NOT_APPLICABLE, Interval.ZERO.secs(), Interval.ZERO.secs(), /*solicited=*/true);
        }

        /**
         * Forwards {@link BleDevice#getNativeBleService(UUID)}.
         */
        public @Nullable(Nullable.Prevalence.NORMAL) BleService service()
        {
            return device().getNativeBleService(serviceUuid());
        }

        /**
         * Forwards {@link BleDevice#getNativeBleCharacteristic(UUID, UUID)}.
         */
        public @Nullable(Nullable.Prevalence.NORMAL) BleCharacteristic characteristic()
        {
            return device().getNativeBleCharacteristic(serviceUuid(), charUuid());
        }

        /**
         * Convenience method for checking if {@link NotificationListener.NotificationEvent#status} equals {@link NotificationListener.Status#SUCCESS}.
         */
        public boolean wasSuccess()
        {
            return status() == NotificationListener.Status.SUCCESS;
        }

        /**
         * Returns the first byte from {@link #data()}, or 0x0 if not available.
         */
        public byte data_byte()
        {
            return data().length > 0 ? data()[0] : 0x0;
        }

        /**
         * Convenience method that attempts to parse the data as a UTF-8 string.
         */
        public @Nullable(Nullable.Prevalence.NEVER) String data_utf8()
        {
            return data_string("UTF-8");
        }

        /**
         * Best effort parsing of {@link #data()} as a {@link String}. For now simply forwards {@link #data_utf8()}.
         * In the future may try to autodetect encoding first.
         */
        public @Nullable(Nullable.Prevalence.NEVER) String data_string()
        {
            return data_utf8();
        }

        /**
         * Convenience method that attempts to parse {@link #data()} as a {@link String} with the given charset, for example <code>"UTF-8"</code>.
         */
        public @Nullable(Nullable.Prevalence.NEVER) String data_string(final String charset)
        {
            return Utils_String.getStringValue(data(), charset);
        }

        /**
         * Convenience method that attempts to parse {@link #data()} as an int.
         *
         * @param reverse - Set to true if you are connecting to a device with {@link java.nio.ByteOrder#BIG_ENDIAN} byte order, to automatically reverse the bytes before conversion.
         */
        public @Nullable(Nullable.Prevalence.NEVER) int data_int(boolean reverse)
        {
            if (reverse)
            {
                byte[] data = data();
                Utils_Byte.reverseBytes(data);
                return Utils_Byte.bytesToInt(data);
            }
            else
            {
                return Utils_Byte.bytesToInt(data());
            }
        }

        /**
         * Convenience method that attempts to parse {@link #data()} as a short.
         *
         * @param reverse - Set to true if you are connecting to a device with {@link java.nio.ByteOrder#BIG_ENDIAN} byte order, to automatically reverse the bytes before conversion.
         */
        public @Nullable(Nullable.Prevalence.NEVER) short data_short(boolean reverse)
        {
            if (reverse)
            {
                byte[] data = data();
                Utils_Byte.reverseBytes(data);
                return Utils_Byte.bytesToShort(data);
            }
            else
            {
                return Utils_Byte.bytesToShort(data());
            }
        }

        /**
         * Convenience method that attempts to parse {@link #data()} as a long.
         *
         * @param reverse - Set to true if you are connecting to a device with {@link java.nio.ByteOrder#BIG_ENDIAN} byte order, to automatically reverse the bytes before conversion.
         */
        public @Nullable(Nullable.Prevalence.NEVER) long data_long(boolean reverse)
        {
            if (reverse)
            {
                byte[] data = data();
                Utils_Byte.reverseBytes(data);
                return Utils_Byte.bytesToLong(data);
            }
            else
            {
                return Utils_Byte.bytesToLong(data());
            }
        }

        /**
         * Forwards {@link NotificationListener.Type#isNull()}.
         */
        @Override public boolean isNull()
        {
            return type().isNull();
        }

        @Override public String toString()
        {
            if (isNull())
            {
                return ReadWriteListener.Type.NULL.toString();
            }
            else
            {
                return Utils_String.toString
                        (
                                this.getClass(),
                                "status", status(),
                                "data", Arrays.toString(data()),
                                "type", type(),
                                "charUuid",     P_Bridge_Internal.uuidName(device().getIBleDevice().getIManager(), charUuid()),
                                "gattStatus",   CodeHelper.gattStatus(gattStatus(), true)
                        );

            }
        }

        public static NotificationListener.NotificationEvent fromReadWriteEvent(BleDevice device, ReadWriteListener.ReadWriteEvent event)
        {
            NotificationListener.Type type;
            switch (event.type())
            {
                case PSUEDO_NOTIFICATION:
                    type = Type.PSEUDO_NOTIFICATION;
                    break;
                case ENABLING_NOTIFICATION:
                    type = Type.ENABLING_NOTIFICATION;
                    break;
                case DISABLING_NOTIFICATION:
                    type = Type.DISABLING_NOTIFICATION;
                    break;
                default:
                    type = NotificationListener.Type.NOTIFICATION;
                    break;
            }
            type = P_Bridge_Internal.getProperNotificationType(event.characteristic(), type);
            NotificationListener.Status status;
            switch (event.status())
            {
                case SUCCESS:
                    status = NotificationListener.Status.SUCCESS;
                    break;
                case NULL:
                    status = NotificationListener.Status.NULL;
                    break;
                case ANDROID_VERSION_NOT_SUPPORTED:
                    status = NotificationListener.Status.ANDROID_VERSION_NOT_SUPPORTED;
                    break;
                case CANCELLED_FROM_BLE_TURNING_OFF:
                    status = NotificationListener.Status.CANCELLED_FROM_BLE_TURNING_OFF;
                    break;
                case CANCELLED_FROM_DISCONNECT:
                    status = NotificationListener.Status.CANCELLED_FROM_DISCONNECT;
                    break;
                case EMPTY_DATA:
                    status = NotificationListener.Status.EMPTY_DATA;
                    break;
                case INVALID_DATA:
                    status = NotificationListener.Status.INVALID_DATA;
                    break;
                case NULL_DATA:
                    status = NotificationListener.Status.NULL_DATA;
                    break;
                case NO_MATCHING_TARGET:
                    status = NotificationListener.Status.NO_MATCHING_TARGET;
                    break;
                case NOT_CONNECTED:
                    status = NotificationListener.Status.NOT_CONNECTED;
                    break;
                case REMOTE_GATT_FAILURE:
                    status = NotificationListener.Status.REMOTE_GATT_FAILURE;
                    break;
                default:
                    status = NotificationListener.Status.UNKNOWN_ERROR;
                    break;
            }
            return P_Bridge_User.newNotificationEvent(device, new BleNotify(event.serviceUuid(), event.charUuid()).setData(new PresentData(event.data())), type, status, event.gattStatus(), event.time_total().secs(), event.time_ota().secs(), event.solicited());
        }
    }

}