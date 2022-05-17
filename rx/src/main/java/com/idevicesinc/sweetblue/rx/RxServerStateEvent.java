package com.idevicesinc.sweetblue.rx;


import com.idevicesinc.sweetblue.BleServerState;
import com.idevicesinc.sweetblue.ServerStateListener;
import com.idevicesinc.sweetblue.utils.State;


public final class RxServerStateEvent extends RxServerEvent<ServerStateListener.StateEvent>
{

    RxServerStateEvent(ServerStateListener.StateEvent event)
    {
        super(event);
    }


    public final RxBleServer server()
    {
        return RxBleManager.getOrCreateServer(m_event.server());
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.ServerStateListener.StateEvent#didEnter(State)}
     */
    public final boolean didEnter(BleServerState state)
    {
        return m_event.didEnter(state);
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.ServerStateListener.StateEvent#didEnterAny(State[])}
     */
    public final boolean didEnterAny(BleServerState... states)
    {
        return m_event.didEnterAny(states);
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.ServerStateListener.StateEvent#didEnterAll(State[])}
     */
    public final boolean didEnterAll(BleServerState... states)
    {
        return m_event.didEnterAll(states);
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.ServerStateListener.StateEvent#didExit(State)}
     */
    public final boolean didExit(BleServerState state)
    {
        return m_event.didExit(state);
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.ServerStateListener.StateEvent#didExitAny(State[])}
     */
    public final boolean didExitAny(BleServerState... states)
    {
        return m_event.didExitAny(states);
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.ServerStateListener.StateEvent#didExitAll(State[])}
     */
    public final boolean didExitAll(BleServerState... states)
    {
        return m_event.didExitAll(states);
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.ServerStateListener.StateEvent#macAddress()}
     */
    public final String macAddress()
    {
        return m_event.macAddress();
    }
}
