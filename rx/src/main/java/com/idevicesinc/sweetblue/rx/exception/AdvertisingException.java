package com.idevicesinc.sweetblue.rx.exception;


import com.idevicesinc.sweetblue.AdvertisingListener;


public final class AdvertisingException extends EventException
{

    public AdvertisingException(AdvertisingListener.AdvertisingEvent event)
    {
        super(event);
    }

    @Override
    public AdvertisingListener.AdvertisingEvent getEvent()
    {
        return super.getEvent();
    }

}
