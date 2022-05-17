package com.idevicesinc.sweetblue.rx.exception;


import com.idevicesinc.sweetblue.AddServiceListener;


public final class ServiceAddException extends EventException
{

    public ServiceAddException(AddServiceListener.ServiceAddEvent event)
    {
        super(event);
    }

    @Override
    public AddServiceListener.ServiceAddEvent getEvent()
    {
        return super.getEvent();
    }

}
