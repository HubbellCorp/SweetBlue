package com.idevicesinc.sweetblue.rx;


import com.idevicesinc.sweetblue.utils.Event;

/**
 * Base interface used for all Rx event classes.
 */
public abstract class RxEvent<T extends Event>
{

    final T m_event;


    protected RxEvent(T event)
    {
        m_event = event;
    }


    /**
     * Returns the {@link Event} instance being held in this class.
     */
    public final T event()
    {
        return m_event;
    }

    /**
     * Forwards {@link Event#isFor(Object)}
     */
    public final boolean isFor(Object value)
    {
        return m_event.isFor(value);
    }

    /**
     * Forwards {@link Event#isForAll(Object...)}
     */
    public final boolean isForAll(Object... values)
    {
        return m_event.isForAll(values);
    }

    /**
     * Forwards {@link Event#isForAny(Object...)}
     */
    public final boolean isForAny(Object... values)
    {
        return m_event.isForAny(values);
    }


    @Override
    public final String toString()
    {
        return m_event.toString();
    }
}
