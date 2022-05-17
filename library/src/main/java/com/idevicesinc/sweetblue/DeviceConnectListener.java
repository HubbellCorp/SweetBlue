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


import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.utils.Event;
import com.idevicesinc.sweetblue.utils.GenericListener_Void;

/**
 * Convenience listener to know if a connect call was successful or not.
 *
 * This listener will be called when<br>
 * A) the {@link BleDevice} enters the {@link BleDeviceState#INITIALIZED} state -- indicating the device is ready
 * to handle any bluetooth operations.
 * <br>
 * or
 * <br>
 * B) the {@link BleDevice} failed to connect. Note that this may fire multiple times. Basically, it's called anytime
 * a connect failure happens. In this case, you should check {@link ConnectEvent#isRetrying()} to see if SweetBlue is
 * still trying to get the {@link BleDevice} connected.
 */
public interface DeviceConnectListener extends GenericListener_Void<DeviceConnectListener.ConnectEvent>
{

    class ConnectEvent extends Event
    {

        private final BleDevice m_device;
        private final DeviceReconnectFilter.ConnectFailEvent m_failEvent;
        private final boolean m_willRetry;


        ConnectEvent(BleDevice device, DeviceReconnectFilter.ConnectFailEvent failEvent, boolean willRetry)
        {
            m_device = device;
            m_failEvent = failEvent;
            m_willRetry = willRetry;
        }


        public final @Nullable(Nullable.Prevalence.NEVER) BleDevice device()
        {
            return m_device;
        }

        /**
         * Returns the {@link com.idevicesinc.sweetblue.DeviceReconnectFilter.ConnectFailEvent} instance. While this should never return
         * <code>null</code>, the event can "be" null. Call {@link DeviceReconnectFilter.ConnectFailEvent#isNull()} to check. If that reports
         * <code>true</code>, it means the connection was successful, thus no real fail event.
         */
        public final @Nullable(Nullable.Prevalence.NEVER) DeviceReconnectFilter.ConnectFailEvent failEvent()
        {
            return m_failEvent;
        }

        /**
         * Returns <code>true</code> if a connection was established to the {@link #device()}. At this point, the device is connected,
         * and in the {@link BleDeviceState#INITIALIZED} state. If this returns <code>false</code>, you can retrieve failure information
         * by calling {@link #failEvent()}.
         */
        public final boolean wasSuccess()
        {
            return m_device.is(BleDeviceState.INITIALIZED) && !m_device.is(BleDeviceState.RECONNECTING_SHORT_TERM);
        }

        /**
         * Returns <code>true</code> if the library will be retrying the connection after this failure.
         */
        public final boolean isRetrying()
        {
            return m_willRetry;
        }
    }

}
