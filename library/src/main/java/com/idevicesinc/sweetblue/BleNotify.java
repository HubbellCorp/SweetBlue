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



import com.idevicesinc.sweetblue.utils.Interval;
import java.util.UUID;


public class BleNotify extends BleDescriptorOp<BleNotify>
{

    Interval m_forceReadTimeout = Interval.INFINITE;
    NotificationListener m_notificationListener;


    public BleNotify()
    {
    }

    public BleNotify(UUID serviceUuid, UUID characteristicUuid)
    {
        super(serviceUuid, characteristicUuid);
    }

    public BleNotify(UUID characteristicUuid)
    {
        super(characteristicUuid);
    }

    /**
     * Constructor which creates a new {@link BleNotify} from the one given. Note that this only copies over the service, and characteristic UUIDs, and the
     * forceReadTimeout value. No interfaces or filters are copied over.
     */
    public BleNotify(BleNotify notify)
    {
        super(notify.getServiceUuid(), notify.getCharacteristicUuid());
        m_forceReadTimeout = Interval.secs(notify.m_forceReadTimeout.secs());
    }


    @Override
    public final boolean isValid()
    {
        return getCharacteristicUuid() != null;
    }

    @Override
    final BleNotify createDuplicate()
    {
        BleNotify notify = getDuplicateOp();
        notify.m_forceReadTimeout = m_forceReadTimeout;
        return notify;
    }

    @Override
    final BleNotify createNewOp()
    {
        return new BleNotify();
    }

    /**
     * This allows you to set a forced read, simulating a notification. This can be useful if you want data back from
     * a characteristic on a consistent schedule. If the interval given here is passed before another notification is
     * received, a read will be performed.
     */
    public final BleNotify setForceReadTimeout(Interval timeout)
    {
        m_forceReadTimeout = timeout;
        return this;
    }

    /**
     * Get the forced read timeout set in this {@link BleNotify}.
     */
    public final Interval getForceReadTimeout()
    {
        return m_forceReadTimeout;
    }

    /**
     * Set a {@link NotificationListener} for this BleNotify. This listener will only be called when toggling
     * notifications/indications on/off. Use {@link BleDevice#setListener_Notification(NotificationListener)} to listen
     * for incoming notifications.
     */
    public final BleNotify setNotificationListener(NotificationListener listener)
    {
        m_notificationListener = listener;
        return this;
    }

    /**
     * Get the {@link NotificationListener} set in this instance of {@link BleNotify}.
     */
    public final NotificationListener getNotificationListener()
    {
        return m_notificationListener;
    }


    /**
     * Builder class to build out a list (or array) of {@link BleNotify} instances.
     */
    public final static class Builder extends BleOp.Builder<Builder, BleNotify>
    {

        public Builder()
        {
            this(null, null);
        }

        public Builder(UUID characteristicUuid)
        {
            this(null, characteristicUuid);
        }

        public Builder(UUID serviceUuid, UUID characteristicUuid)
        {
            currentOp = new BleNotify(serviceUuid, characteristicUuid);
        }


        public final Builder setForceReadTimeout(Interval timeout)
        {
            currentOp.setForceReadTimeout(timeout);
            return this;
        }

        public final Builder setNotificationListener(NotificationListener listener)
        {
            currentOp.setNotificationListener(listener);
            return this;
        }

        @Override
        public final BleNotify[] buildArray()
        {
            return build().toArray(new BleNotify[0]);
        }
    }
}
