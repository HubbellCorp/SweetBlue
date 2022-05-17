package com.idevicesinc.sweetblue.rx;


import com.idevicesinc.sweetblue.BleManagerState;
import com.idevicesinc.sweetblue.ManagerStateListener;
import com.idevicesinc.sweetblue.utils.State;


public final class RxManagerStateEvent extends RxManagerEvent<ManagerStateListener.StateEvent>
{


    RxManagerStateEvent(RxBleManager mgr, ManagerStateListener.StateEvent event)
    {
        super(mgr, event);
    }



    /**
     * Forwards {@link com.idevicesinc.sweetblue.ManagerStateListener.StateEvent#didEnter(State)}
     */
    public final boolean didEnter(BleManagerState state)
    {
        return m_event.didEnter(state);
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.ManagerStateListener.StateEvent#didEnterAny(State[])}
     */
    public final boolean didEnterAny(BleManagerState... states)
    {
        return m_event.didEnterAny(states);
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.ManagerStateListener.StateEvent#didEnterAll(State[])}
     */
    public final boolean didEnterAll(BleManagerState... states)
    {
        return m_event.didEnterAll(states);
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.ManagerStateListener.StateEvent#didExit(State)}
     */
    public final boolean didExit(BleManagerState state)
    {
        return m_event.didExit(state);
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.ManagerStateListener.StateEvent#didExitAny(State[])}
     */
    public final boolean didExitAny(BleManagerState... states)
    {
        return m_event.didExitAny(states);
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.ManagerStateListener.StateEvent#didExitAll(State[])}
     */
    public final boolean didExitAll(BleManagerState... states)
    {
        return m_event.didExitAll(states);
    }

}
