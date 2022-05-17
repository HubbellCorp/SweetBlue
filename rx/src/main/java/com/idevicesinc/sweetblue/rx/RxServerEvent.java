package com.idevicesinc.sweetblue.rx;


import com.idevicesinc.sweetblue.utils.Event;


public abstract class RxServerEvent<T extends Event> extends RxEvent<T>
{

    protected RxServerEvent(T event)
    {
        super(event);
    }


}
