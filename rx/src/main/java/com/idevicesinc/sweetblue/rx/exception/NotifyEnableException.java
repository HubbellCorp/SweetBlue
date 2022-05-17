package com.idevicesinc.sweetblue.rx.exception;

import com.idevicesinc.sweetblue.BleNotify;
import com.idevicesinc.sweetblue.NotificationListener;

/**
 * Exception class which holds the {@link com.idevicesinc.sweetblue.NotificationListener.NotificationEvent} of the enable/disable notification operation for more
 * information about what went wrong. This class is the exception passed into the onError(e) method when calling either
 * {@link com.idevicesinc.sweetblue.rx.RxBleDevice#enableNotify(BleNotify)}, or {@link com.idevicesinc.sweetblue.rx.RxBleDevice#disableNotify(BleNotify)}.
 */
public final class NotifyEnableException extends EventException
{

    public NotifyEnableException(NotificationListener.NotificationEvent event)
    {
        super(event);
    }

    @Override
    public NotificationListener.NotificationEvent getEvent()
    {
        return super.getEvent();
    }
}