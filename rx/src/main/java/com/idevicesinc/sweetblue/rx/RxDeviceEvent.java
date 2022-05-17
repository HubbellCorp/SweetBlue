package com.idevicesinc.sweetblue.rx;


import com.idevicesinc.sweetblue.utils.Event;


public abstract class RxDeviceEvent<T extends Event> extends RxEvent<T>
{

    protected RxDeviceEvent(T event)
    {
        super(event);
    }

}
