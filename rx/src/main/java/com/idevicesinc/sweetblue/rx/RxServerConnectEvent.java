package com.idevicesinc.sweetblue.rx;


import com.idevicesinc.sweetblue.ServerConnectListener;
import com.idevicesinc.sweetblue.ServerReconnectFilter;


public final class RxServerConnectEvent extends RxServerEvent<ServerConnectListener.ConnectEvent>
{

    protected RxServerConnectEvent(ServerConnectListener.ConnectEvent event)
    {
        super(event);
    }


    /**
     * Forwards {@link com.idevicesinc.sweetblue.ServerConnectListener.ConnectEvent#wasSuccess()}.
     */
    public final boolean wasSuccess()
    {
        return m_event.wasSuccess();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.ServerConnectListener.ConnectEvent#isRetrying()}.
     */
    public final boolean isRetrying()
    {
        return m_event.isRetrying();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.ServerConnectListener.ConnectEvent#failEvent()}.
     */
    public final ServerReconnectFilter.ConnectFailEvent failEvent()
    {
        return m_event.failEvent();
    }
}
