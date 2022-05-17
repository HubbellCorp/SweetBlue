package com.idevicesinc.sweetblue.rx;


import com.idevicesinc.sweetblue.HistoricalDataLoadListener;
import com.idevicesinc.sweetblue.utils.EpochTimeRange;
import java.util.UUID;


public final class RxHistoricalDataLoadEvent extends RxDeviceEvent<HistoricalDataLoadListener.HistoricalDataLoadEvent>
{


    RxHistoricalDataLoadEvent(HistoricalDataLoadListener.HistoricalDataLoadEvent event)
    {
        super(event);
    }


    /**
     * Forwards {@link com.idevicesinc.sweetblue.HistoricalDataLoadListener.HistoricalDataLoadEvent#wasSuccess()}
     */
    public final boolean wasSuccess()
    {
        return m_event.wasSuccess();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.HistoricalDataLoadListener.HistoricalDataLoadEvent#range()}
     */
    public final EpochTimeRange range()
    {
        return m_event.range();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.HistoricalDataLoadListener.HistoricalDataLoadEvent#uuid()}
     */
    public final UUID uuid()
    {
        return m_event.uuid();
    }


}
