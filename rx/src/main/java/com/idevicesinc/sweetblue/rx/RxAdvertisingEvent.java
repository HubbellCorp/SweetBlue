package com.idevicesinc.sweetblue.rx;


import com.idevicesinc.sweetblue.AdvertisingListener;


public final class RxAdvertisingEvent extends RxServerEvent<AdvertisingListener.AdvertisingEvent>
{


    RxAdvertisingEvent(AdvertisingListener.AdvertisingEvent event)
    {
        super(event);
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.AdvertisingListener.AdvertisingEvent#wasSuccess()}
     */
    public final boolean wasSuccess()
    {
        return m_event.wasSuccess();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.AdvertisingListener.AdvertisingEvent#status()}
     */
    public final AdvertisingListener.Status status()
    {
        return m_event.status();
    }

    public final RxBleServer server()
    {
        return RxBleManager.getOrCreateServer(m_event.server());
    }


}
