package com.idevicesinc.sweetblue.rx;


import com.idevicesinc.sweetblue.AssertListener;


public final class RxAssertEvent extends RxManagerEvent<AssertListener.AssertEvent>
{

    RxAssertEvent(RxBleManager mgr, AssertListener.AssertEvent event)
    {
        super(mgr, event);
    }


    /**
     * Forwards {@link com.idevicesinc.sweetblue.AssertListener.AssertEvent#message()}
     */
    public final String message()
    {
        return m_event.message();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.AssertListener.AssertEvent#stackTrace()}
     */
    public final StackTraceElement[] stackTrace()
    {
        return m_event.stackTrace();
    }
}
