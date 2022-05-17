package com.idevicesinc.sweetblue.rx;


import com.idevicesinc.sweetblue.DiscoveryListener;
import com.idevicesinc.sweetblue.DiscoveryListener.DiscoveryEvent;
import com.idevicesinc.sweetblue.ScanOptions;
import com.idevicesinc.sweetblue.DiscoveryListener.LifeCycle;
import com.idevicesinc.sweetblue.utils.Percent;

/**
 * Convenience class used when scanning with {@link RxBleManager#scan(ScanOptions)}. This simply holds the {@link DiscoveryEvent}
 * returned from the scan with some convenience methods.
 */
public final class RxDiscoveryEvent extends RxManagerEvent<DiscoveryEvent>
{


    RxDiscoveryEvent(RxBleManager mgr, DiscoveryEvent event)
    {
        super(mgr, event);
    }


    /**
     * Returns <code>true</code> if the {@link RxDiscoveryEvent} was for the {@link RxBleDevice} being discovered for the first time.
     * This just calls {@link com.idevicesinc.sweetblue.DiscoveryListener.DiscoveryEvent#was(com.idevicesinc.sweetblue.DiscoveryListener.LifeCycle)} using {@link LifeCycle#DISCOVERED}
     */
    public final boolean wasDiscovered()
    {
        return m_event.was(DiscoveryListener.LifeCycle.DISCOVERED);
    }

    /**
     * Returns <code>true</code> if the {@link RxDiscoveryEvent} was for the {@link RxBleDevice} getting re-discovered.
     * This just calls {@link DiscoveryEvent#was(com.idevicesinc.sweetblue.DiscoveryListener.LifeCycle)} using {@link com.idevicesinc.sweetblue.DiscoveryListener.LifeCycle#REDISCOVERED}
     */
    public final boolean wasRediscovered()
    {
        return m_event.was(LifeCycle.REDISCOVERED);
    }

    /**
     *
     * This just calls {@link DiscoveryEvent#was(com.idevicesinc.sweetblue.DiscoveryListener.LifeCycle)} using {@link com.idevicesinc.sweetblue.DiscoveryListener.LifeCycle#UNDISCOVERED}
     */
    public final boolean wasUndiscovered()
    {
        return m_event.was(LifeCycle.UNDISCOVERED);
    }

    /**
     * Forwards {@link DiscoveryEvent#was(com.idevicesinc.sweetblue.DiscoveryListener.LifeCycle)}.
     */
    public final boolean was(LifeCycle lifecycle)
    {
        return m_event.was(lifecycle);
    }

    /**
     * Forwards {@link DiscoveryEvent#lifeCycle()}.
     */
    public final LifeCycle lifeCycle()
    {
        return m_event.lifeCycle();
    }

    /**
     * Forwards {@link DiscoveryEvent#rssi()}.
     */
    public final int rssi()
    {
        return m_event.rssi();
    }

    /**
     * Forwards {@link DiscoveryEvent#rssi_percent()}.
     */
    public final Percent rssi_percent()
    {
        return m_event.rssi_percent();
    }

    /**
     * Forwards {@link DiscoveryEvent#macAddress()}.
     */
    public final String macAddress()
    {
        return m_event.macAddress();
    }

    /**
     * Returns an instance of {@link RxBleDevice}
     */
    public final RxBleDevice device()
    {
        return RxBleManager.getOrCreateDevice(m_event.device());
    }
}
