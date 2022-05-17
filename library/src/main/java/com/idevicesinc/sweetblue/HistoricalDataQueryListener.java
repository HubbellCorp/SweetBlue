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


import android.database.Cursor;
import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.internal.P_Bridge_Internal;
import com.idevicesinc.sweetblue.utils.Event;
import com.idevicesinc.sweetblue.utils.GenericListener_Void;
import com.idevicesinc.sweetblue.utils.UsesCustomNull;
import com.idevicesinc.sweetblue.utils.Utils_String;
import java.util.UUID;

/**
 * A callback that is used by {@link BleDevice#select()} to listen for when a database query is done processing. The
 * {@link HistoricalDataQueryListener#onEvent(Event)} method is called when the historical data for a given characteristic {@link UUID} is done querying.
 */
@com.idevicesinc.sweetblue.annotations.Alpha
@com.idevicesinc.sweetblue.annotations.Lambda
public interface HistoricalDataQueryListener extends GenericListener_Void<HistoricalDataQueryListener.HistoricalDataQueryEvent>
{
    /**
     * Enumerates the status codes for operations kicked off from {@link BleDevice#select()}.
     */
    public static enum Status implements UsesCustomNull
    {
        /**
         * Fulfills soft contract of {@link UsesCustomNull}.
         */
        NULL,

        /**
         * Tried to query historical data on {@link BleDevice#NULL} or {@link BleServer#NULL}.
         */
        NULL_ENDPOINT,

        /**
         * Query completed successfully - {@link HistoricalDataQueryEvent#cursor()} may be empty but there were no exceptions or anything.
         */
        SUCCESS,

        /**
         * There is no backing table for the given {@link UUID}.
         */
        NO_TABLE,

        /**
         * General failure - this feature is still in {@link com.idevicesinc.sweetblue.annotations.Alpha} so expect more detailed error statuses in the future.
         */
        ERROR;

        /**
         * Returns true if <code>this==</code> {@link #NULL}.
         */
        @Override public boolean isNull()
        {
            return this == NULL;
        }
    }

    /**
     * Event struct passed to {@link HistoricalDataQueryListener#onEvent(Event)} that provides
     * further information about the status of a historical data load to memory using {@link BleDevice#loadHistoricalData()}
     * (or overloads).
     */
    @com.idevicesinc.sweetblue.annotations.Immutable
    public static class HistoricalDataQueryEvent extends Event
    {
        /**
         * The {@link UUID} that the data is being queried for.
         */
        public UUID uuid() {  return m_uuid;  }
        private final UUID m_uuid;

        /**
         * The general status of the query operation.
         */
        public Status status() {  return m_status; }
        private final Status m_status;

        /**
         * The resulting {@link Cursor} from the database query. This will never be null, just an empty cursor if anything goes wrong.
         */
        public @Nullable(Nullable.Prevalence.NEVER) Cursor cursor() {  return m_cursor; }
        private final Cursor m_cursor;

        /**
         * The raw query given to the database.
         */
        public @Nullable(Nullable.Prevalence.NEVER) String rawQuery() {  return m_rawQuery; }
        private final String m_rawQuery;

        private final BleNode m_endpoint;

        public HistoricalDataQueryEvent(final BleNode endpoint, final UUID uuid, final Cursor cursor, final Status status, final String rawQuery)
        {
            m_endpoint = endpoint;
            m_uuid = uuid;
            m_cursor = cursor;
            m_status = status;
            m_rawQuery = rawQuery;
        }

        /**
         * Returns <code>true</code> if {@link #status()} is {@link Status#SUCCESS}.
         */
        public boolean wasSuccess()
        {
            return status() == Status.SUCCESS;
        }

        @Override public String toString()
        {
            return Utils_String.toString
            (
                this.getClass(),
                "uuid",     P_Bridge_Internal.uuidName(m_endpoint.getIBleNode().getIManager(), uuid()),
                "status",			status()
            );
        }
    }

}
