package com.idevicesinc.sweetblue.rx.exception;


import com.idevicesinc.sweetblue.rx.RxEvent;
import com.idevicesinc.sweetblue.utils.Event;

/**
 * Base Exception class for holding event instance when an error occurs (a read/write/connect/bond fails)
 */
public abstract class EventException extends Exception
{

    private final Event m_event;
    private final RxEvent m_rxEvent;


    public <T extends Event> EventException(T event)
    {
        super();
        m_event = event;
        m_rxEvent = null;
    }

    public <T extends RxEvent> EventException(T rxEvent)
    {
        super();
        m_rxEvent = rxEvent;
        m_event = null;
    }

    /**
     * Returns the event instance holding information as to what went wrong.
     */
    public <T extends Event> T getEvent()
    {
        return (T) m_event;
    }

    public <T extends RxEvent> T getRxEvent()
    {
        return (T) m_rxEvent;
    }

}
