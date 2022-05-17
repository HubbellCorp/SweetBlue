package com.idevicesinc.sweetblue.rx;


import com.idevicesinc.sweetblue.OutgoingListener;


public final class RxOutgoingEvent extends RxServerEvent<OutgoingListener.OutgoingEvent>
{


    RxOutgoingEvent(OutgoingListener.OutgoingEvent event)
    {
        super(event);
    }


    public final RxBleServer server()
    {
        return RxBleManager.getOrCreateServer(m_event.server());
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.OutgoingListener.OutgoingEvent#macAddress()}
     */
    public final String macAddress()
    {
        return m_event.macAddress();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.OutgoingListener.OutgoingEvent#wasSuccess()}
     */
    public final boolean wasSuccess()
    {
        return m_event.wasSuccess();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.OutgoingListener.OutgoingEvent#data_sent()}}
     */
    public final byte[] data_sent()
    {
        return m_event.data_sent();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.OutgoingListener.OutgoingEvent#data_received()}
     */
    public final byte[] data_received()
    {
        return m_event.data_received();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.OutgoingListener.OutgoingEvent#gattStatus_received()}
     */
    public final int gattStatus_received()
    {
        return m_event.gattStatus_received();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.OutgoingListener.OutgoingEvent#gattStatus_sent()}
     */
    public final int gattStatus_sent()
    {
        return m_event.gattStatus_sent();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.OutgoingListener.OutgoingEvent#solicited()}
     */
    public final boolean solicited()
    {
        return m_event.solicited();
    }

}
