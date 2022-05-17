package com.idevicesinc.sweetblue.rx;


import com.idevicesinc.sweetblue.utils.Event;


public abstract class RxManagerEvent<T extends Event> extends RxEvent<T>
{

    private final RxBleManager m_manager;


    protected RxManagerEvent(RxBleManager mgr, T event)
    {
        super(event);
        m_manager = mgr;
    }


    public final RxBleManager manager()
    {
        return m_manager;
    }

}
