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

package com.idevicesinc.sweetblue.defaults;


import com.idevicesinc.sweetblue.ReconnectFilter;
import com.idevicesinc.sweetblue.utils.Interval;

/**
 * Default implementation of {@link ReconnectFilter} that uses {@link com.idevicesinc.sweetblue.ReconnectFilter.ConnectionLostPlease#retryInstantly()} for the
 * first reconnect attempt, and from then on uses the {@link Interval} rate passed to the constructor. An instance of this class is held
 * in {@link DefaultDeviceReconnectFilter}, and {@link DefaultServerReconnectFilter}, to handle connection lost events ONLY. The logic is the same
 * between the two, hence this class exists for this purpose, and to adhere to the DRY principle. This class is not meant for public consumption.
 *
 */
public class DefaultReconnectFilter implements ReconnectFilter
{
    public static final Interval LONG_TERM_ATTEMPT_RATE			= Interval.secs(3.0);
    public static final Interval SHORT_TERM_ATTEMPT_RATE		= Interval.secs(1.0);

    public static final Interval SHORT_TERM_TIMEOUT				= Interval.FIVE_SECS;
    public static final Interval LONG_TERM_TIMEOUT				= Interval.mins(5);

    private final ConnectionLostPlease m_please__SHORT_TERM;
    private final ConnectionLostPlease m_please__LONG_TERM;


    public DefaultReconnectFilter()
    {
        this
                (
                        DefaultReconnectFilter.SHORT_TERM_ATTEMPT_RATE,
                        DefaultReconnectFilter.LONG_TERM_ATTEMPT_RATE,
                        DefaultReconnectFilter.SHORT_TERM_TIMEOUT,
                        DefaultReconnectFilter.LONG_TERM_TIMEOUT
                );
    }

    public DefaultReconnectFilter(final Interval reconnectRate__SHORT_TERM, final Interval reconnectRate__LONG_TERM, final Interval timeout__SHORT_TERM, final Interval timeout__LONG_TERM)
    {
        m_please__SHORT_TERM = ConnectionLostPlease.retryWithTimeout(reconnectRate__SHORT_TERM, timeout__SHORT_TERM);
        m_please__LONG_TERM = ConnectionLostPlease.retryWithTimeout(reconnectRate__LONG_TERM, timeout__LONG_TERM);
    }


    @Override
    public ConnectFailPlease onConnectFailed(ConnectFailEvent event)
    {
        throw new RuntimeException("Stub!");
    }

    @Override
    public ConnectionLostPlease onConnectionLost(ConnectionLostEvent e)
    {
        if( e.type().isShortTerm() )
        {
            return m_please__SHORT_TERM;
        }
        else
        {
            return m_please__LONG_TERM;
        }
    }
}
