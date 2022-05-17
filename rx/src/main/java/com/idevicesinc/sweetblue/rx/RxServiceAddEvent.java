package com.idevicesinc.sweetblue.rx;


import com.idevicesinc.sweetblue.AddServiceListener;


public final class RxServiceAddEvent extends RxServerEvent<AddServiceListener.ServiceAddEvent>
{

    RxServiceAddEvent(AddServiceListener.ServiceAddEvent event)
    {
        super(event);
    }



    public final boolean wasSuccess()
    {
        return m_event.wasSuccess();
    }

    public final RxBleServer server()
    {
        return RxBleManager.getOrCreateServer(m_event.server());
    }

}
