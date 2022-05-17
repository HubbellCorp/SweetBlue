package com.idevicesinc.sweetblue.rx;


import com.idevicesinc.sweetblue.UhOhListener;


public final class RxUhOhEvent extends RxManagerEvent<UhOhListener.UhOhEvent>
{


    RxUhOhEvent(RxBleManager mgr, UhOhListener.UhOhEvent event)
    {
        super(mgr, event);
    }


    /**
     * Forwards {@link com.idevicesinc.sweetblue.UhOhListener.UhOhEvent#remedy()}
     */
    public final UhOhListener.Remedy remedy()
    {
        return m_event.remedy();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.UhOhListener.UhOhEvent#uhOh()}
     */
    public final UhOhListener.UhOh uhOh()
    {
        return m_event.uhOh();
    }
}
