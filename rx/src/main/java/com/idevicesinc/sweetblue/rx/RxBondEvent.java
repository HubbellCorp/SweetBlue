package com.idevicesinc.sweetblue.rx;


import com.idevicesinc.sweetblue.BondListener;
import com.idevicesinc.sweetblue.utils.State;


public final class RxBondEvent extends RxDeviceEvent<BondListener.BondEvent>
{

    RxBondEvent(BondListener.BondEvent event)
    {
        super(event);
    }


    /**
     * Forwards {@link com.idevicesinc.sweetblue.BondListener.BondEvent#wasSuccess()}
     */
    public final boolean wasSuccess()
    {
        return m_event.wasSuccess();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.BondListener.BondEvent#failReason()}
     */
    public final int failReason()
    {
        return m_event.failReason();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.BondListener.BondEvent#intent()}
     */
    public final State.ChangeIntent intent()
    {
        return m_event.intent();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.BondListener.BondEvent#macAddress()}
     */
    public final String macAddress()
    {
        return m_event.macAddress();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.BondListener.BondEvent#wasCancelled()}
     */
    public final boolean wasCancelled()
    {
        return m_event.wasCancelled();
    }


    public final RxBleDevice device()
    {
        return RxBleManager.getOrCreateDevice(m_event.device());
    }

}
