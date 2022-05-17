package com.idevicesinc.sweetblue.rx.exception;

import com.idevicesinc.sweetblue.BondListener;


public class BondException extends EventException
{

    public BondException(BondListener.BondEvent event)
    {
        super(event);
    }

    @Override
    public BondListener.BondEvent getEvent()
    {
        return super.getEvent();
    }
}