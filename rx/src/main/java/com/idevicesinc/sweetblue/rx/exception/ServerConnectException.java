package com.idevicesinc.sweetblue.rx.exception;


import com.idevicesinc.sweetblue.ServerReconnectFilter;


/**
 * Exception class used to indicate a connection failure, which holds an instance of {@link com.idevicesinc.sweetblue.ServerReconnectFilter.ConnectFailEvent},
 * when using {@link com.idevicesinc.sweetblue.rx.RxBleServer#connect(String, ServerReconnectFilter)}.
 */
public final class ServerConnectException extends EventException
{
    public ServerConnectException(ServerReconnectFilter.ConnectFailEvent rxEvent)
    {
        super(rxEvent);
    }

    @Override public ServerReconnectFilter.ConnectFailEvent getEvent()
    {
        return super.getEvent();
    }
}
