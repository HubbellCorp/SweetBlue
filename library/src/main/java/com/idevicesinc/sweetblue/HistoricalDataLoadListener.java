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


import com.idevicesinc.sweetblue.internal.P_Bridge_Internal;
import com.idevicesinc.sweetblue.utils.EpochTimeRange;
import com.idevicesinc.sweetblue.utils.Event;
import com.idevicesinc.sweetblue.utils.GenericListener_Void;
import com.idevicesinc.sweetblue.utils.UsesCustomNull;
import com.idevicesinc.sweetblue.utils.Utils_String;

import java.util.UUID;

/**
 * A callback that is used by various overloads of {@link BleDevice#loadHistoricalData()} that accept instances hereof.
 * You can also set default listeners on {@link BleDevice#setListener_HistoricalDataLoad(HistoricalDataLoadListener)}
 * and {@link BleManager#setListener_HistoricalDataLoad(HistoricalDataLoadListener)}. The {@link HistoricalDataLoadListener#onEvent(Event)} method
 * is called when the historical data for a given characteristic {@link UUID} is done loading from disk.
 */
@com.idevicesinc.sweetblue.annotations.Lambda
public interface HistoricalDataLoadListener extends GenericListener_Void<HistoricalDataLoadListener.HistoricalDataLoadEvent>
{
    /**
     * Enumerates the status codes for operations kicked off from {@link BleDevice#loadHistoricalData()} (or overloads).
     */
    public static enum Status implements UsesCustomNull
    {
        /**
         * Fulfills soft contract of {@link UsesCustomNull}.
         */
        NULL,

        /**
         * Historical data is fully loaded to memory and ready to access synchronously (without blocking current thread)
         * through {@link BleDevice#getHistoricalData_iterator(UUID)} (or overloads).
         */
        LOADED,

        /**
         * {@link BleDevice#loadHistoricalData()} (or overloads) was called but the data was already loaded to memory.
         */
        ALREADY_LOADED,

        /**
         * {@link BleDevice#loadHistoricalData()} (or overloads) was called but there was no data available to load to memory.
         */
        NOTHING_TO_LOAD,

        /**
         * {@link BleDevice#loadHistoricalData()} (or overloads) was called and the operation was successfully started -
         * expect another {@link HistoricalDataLoadEvent} with {@link HistoricalDataLoadEvent#status()} being {@link #LOADED} shortly.
         */
        STARTED_LOADING,

        /**
         * Same idea as {@link #STARTED_LOADING}, not an error status, but letting you know that the load was already in progress
         * when {@link BleDevice#loadHistoricalData()} (or overloads) was called a second time. This doesn't
         * affect the actual loading process at all, and {@link #LOADED} will eventually be returned for both callbacks.
         */
        ALREADY_LOADING;

        /**
         * Returns true if <code>this==</code> {@link #NULL}.
         */
        @Override public boolean isNull()
        {
            return this == NULL;
        }
    }

    /**
     * Event struct passed to {@link HistoricalDataLoadListener#onEvent(Event)} that provides
     * further information about the status of a historical data load to memory using {@link BleDevice#loadHistoricalData()}
     * (or overloads).
     */
    @com.idevicesinc.sweetblue.annotations.Immutable
    public static class HistoricalDataLoadEvent extends Event
    {
        /**
         * The mac address that the data is being queried for.
         */
        public String macAddress() {  return m_macAddress; }
        private final String m_macAddress;

        /**
         * The {@link UUID} that the data is being loaded for.
         */
        public UUID uuid() {  return m_uuid;  }
        private final UUID m_uuid;

        /**
         * The resulting time range spanning all of the data loaded to memory, or {@link EpochTimeRange#NULL} if not applicable.
         */
        public EpochTimeRange range() {  return m_range; }
        private final EpochTimeRange m_range;

        /**
         * The general status of the load operation.
         */
        public Status status() {  return m_status; }
        private final Status m_status;

        private final BleNode m_endpoint;

        HistoricalDataLoadEvent(final BleNode endpoint, final String macAddress, final UUID uuid, final EpochTimeRange range, final Status status)
        {
            m_endpoint = endpoint;
            m_macAddress = macAddress;
            m_uuid = uuid;
            m_range = range;
            m_status = status;
        }

        /**
         * Returns <code>true</code> if {@link #status()} is either {@link Status#LOADED} or
         *  {@link Status#ALREADY_LOADED}.
         */
        public boolean wasSuccess()
        {
            return status() == Status.LOADED || status() == Status.ALREADY_LOADED;
        }

        @Override public String toString()
        {
            return Utils_String.toString
            (
                this.getClass(),
                "macAddress", macAddress(),
                "uuid", P_Bridge_Internal.uuidName(m_endpoint.getIBleNode().getIManager(), uuid()),
                "status", status()
            );
        }
    }

}
