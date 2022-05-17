package com.idevicesinc.sweetblue.rx;


import com.idevicesinc.sweetblue.DeviceConnectListener;
import com.idevicesinc.sweetblue.DeviceReconnectFilter;


public final class RxDeviceConnectEvent extends RxDeviceEvent<DeviceConnectListener.ConnectEvent>
{


    RxDeviceConnectEvent(DeviceConnectListener.ConnectEvent event)
    {
        super(event);
    }


    public final RxBleDevice device()
    {
        return RxBleManager.getOrCreateDevice(m_event.device());
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.DeviceConnectListener.ConnectEvent#wasSuccess()}
     */
    public final boolean wasSuccess()
    {
        return m_event.wasSuccess();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.DeviceConnectListener.ConnectEvent#isRetrying()}
     */
    public final boolean isRetrying()
    {
        return m_event.isRetrying();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.DeviceConnectListener.ConnectEvent#failEvent()}
     */
    public final DeviceReconnectFilter.ConnectFailEvent failEvent()
    {
        return m_event.failEvent();
    }


}
