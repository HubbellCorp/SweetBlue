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

import com.idevicesinc.sweetblue.utils.Event;
import com.idevicesinc.sweetblue.utils.GenericListener_Void;
import com.idevicesinc.sweetblue.utils.UsesCustomNull;
import com.idevicesinc.sweetblue.utils.Utils_String;
import com.idevicesinc.sweetblue.utils.BleScanRecord;

/**
 * Provide an implementation to {@link BleServer#setListener_Advertising(AdvertisingListener)}, and
 * {@link BleManager#setListener_Advertising(AdvertisingListener)} to receive a callback
 * when using {@link BleServer#startAdvertising(BleScanRecord)}.
 */
public interface AdvertisingListener extends GenericListener_Void<AdvertisingListener.AdvertisingEvent>
{

    /**
     * Enumeration describing the m_status of calling {@link BleServer#startAdvertising(BleScanRecord)}.
     */
    enum Status implements UsesCustomNull
    {
        SUCCESS(BleStatuses.ADVERTISE_SUCCESS),
        DATA_TOO_LARGE(BleStatuses.ADVERTISE_FAILED_DATA_TOO_LARGE),
        TOO_MANY_ADVERTISERS(BleStatuses.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS),
        ALREADY_STARTED(BleStatuses.ADVERTISE_FAILED_ALREADY_STARTED),
        INTERNAL_ERROR(BleStatuses.ADVERTISE_FAILED_INTERNAL_ERROR),
        ANDROID_VERSION_NOT_SUPPORTED(BleStatuses.ADVERTISE_ANDROID_VERSION_NOT_SUPPORTED),
        CHIPSET_NOT_SUPPORTED(BleStatuses.ADVERTISE_FAILED_FEATURE_UNSUPPORTED),
        BLE_NOT_ON(-1),
        NULL_SERVER(-2),
        NULL(-3);

        private final int m_nativeStatus;

        Status(int nativeStatus)
        {
            m_nativeStatus = nativeStatus;
        }

        public final int getNativeStatus()
        {
            return m_nativeStatus;
        }

        public static Status fromNativeStatus(int bit)
        {
            for (Status res : values())
            {
                if (res.m_nativeStatus == bit)
                {
                    return res;
                }
            }
            return SUCCESS;
        }

        @Override
        public final boolean isNull() {
            return this == NULL;
        }
    }

    /**
     * Sub class representing the Advertising Event
     */
    class AdvertisingEvent extends Event implements UsesCustomNull
    {
        private final BleServer m_server;
        private final Status m_status;

        AdvertisingEvent(BleServer server, Status m_status)
        {
            m_server = server;
            this.m_status = m_status;
        }

        /**
         * The backing {@link BleManager} which is attempting to start advertising.
         */
        public final BleServer server()
        {
            return m_server;
        }

        /**
         * Whether or not {@link BleServer#startAdvertising(BleScanRecord)} was successful or not. If false,
         * then call {@link #m_status} to get the error code.
         */
        public final boolean wasSuccess()
        {
            return m_status == Status.SUCCESS;
        }

        /**
         * Returns {@link Status} describing
         * the m_status of calling {@link BleServer#startAdvertising(BleScanRecord)}
         */
        public final Status status()
        {
            return m_status;
        }

        @Override
        public final boolean isNull() {
            return status() == Status.NULL;
        }

        @Override
        public final String toString() {
            return Utils_String.toString(this.getClass(),
                    "server", server().getClass().getSimpleName(),
                    "status", status());
        }
    }

    /**
     * Called upon the m_status of calling {@link BleServer#startAdvertising(BleScanRecord)}
     */
    void onEvent(AdvertisingEvent e);

}
