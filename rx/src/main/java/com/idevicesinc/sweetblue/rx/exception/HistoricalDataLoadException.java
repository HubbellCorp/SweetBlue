package com.idevicesinc.sweetblue.rx.exception;


import com.idevicesinc.sweetblue.rx.RxEvent;
import com.idevicesinc.sweetblue.rx.RxHistoricalDataLoadEvent;


public final class HistoricalDataLoadException extends EventException
{

    public <T extends RxEvent> HistoricalDataLoadException(T rxEvent)
    {
        super(rxEvent);
    }


    @Override
    public RxHistoricalDataLoadEvent getRxEvent()
    {
        return super.getRxEvent();
    }
}
