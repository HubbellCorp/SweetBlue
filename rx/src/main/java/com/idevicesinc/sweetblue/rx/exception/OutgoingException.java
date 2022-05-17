package com.idevicesinc.sweetblue.rx.exception;


import com.idevicesinc.sweetblue.OutgoingListener;


public final class OutgoingException extends EventException
{

    public OutgoingException(OutgoingListener.OutgoingEvent event)
    {
        super(event);
    }

    @Override
    public OutgoingListener.OutgoingEvent getEvent()
    {
        return super.getEvent();
    }

}
