package com.idevicesinc.sweetblue.rx.exception;


import com.idevicesinc.sweetblue.DeviceReconnectFilter.ConnectFailEvent;
import com.idevicesinc.sweetblue.rx.RxBleDevice;

/**
 * Exception class used to indicate a connection failure, which holds an instance of {@link ConnectFailEvent},
 * when using {@link RxBleDevice#connect()}.
 */
public class ConnectException extends EventException
{
    public ConnectException(ConnectFailEvent event)
    {
        super(event);
    }

    @Override
    public ConnectFailEvent getEvent()
    {
        return super.getEvent();
    }
}
