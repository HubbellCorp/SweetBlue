package com.idevicesinc.sweetblue.rx;


import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.DeviceStateListener;
import com.idevicesinc.sweetblue.utils.State;


public final class RxDeviceStateEvent extends RxDeviceEvent<DeviceStateListener.StateEvent>
{

    RxDeviceStateEvent(DeviceStateListener.StateEvent event)
    {
        super(event);
    }


    /**
     * Forwards {@link com.idevicesinc.sweetblue.DeviceStateListener.StateEvent#didEnter(State)}
     */
    public final boolean didEnter(BleDeviceState state)
    {
        return m_event.didEnter(state);
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.DeviceStateListener.StateEvent#didEnterAny(State[])}
     */
    public final boolean didEnterAny(BleDeviceState... states)
    {
        return m_event.didEnterAny(states);
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.DeviceStateListener.StateEvent#didEnterAll(State[])}
     */
    public final boolean didEnterAll(BleDeviceState... states)
    {
        return m_event.didEnterAll(states);
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.DeviceStateListener.StateEvent#didExit(State)}
     */
    public final boolean didExit(BleDeviceState state)
    {
        return m_event.didExit(state);
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.DeviceStateListener.StateEvent#didExitAny(State[])}
     */
    public final boolean didExitAny(BleDeviceState... states)
    {
        return m_event.didExitAny(states);
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.DeviceStateListener.StateEvent#didExitAll(State[])}
     */
    public final boolean didExitAll(BleDeviceState... states)
    {
        return m_event.didExitAll(states);
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.DeviceStateListener.StateEvent#isSimple()}
     */
    public final boolean isSimple()
    {
        return m_event.isSimple();
    }




    public final RxBleDevice device()
    {
        return RxBleManager.getOrCreateDevice(m_event.device());
    }
}
