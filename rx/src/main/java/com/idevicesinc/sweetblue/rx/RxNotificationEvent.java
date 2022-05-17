package com.idevicesinc.sweetblue.rx;


import com.idevicesinc.sweetblue.BleCharacteristic;
import com.idevicesinc.sweetblue.BleService;
import com.idevicesinc.sweetblue.NotificationListener;
import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.utils.Interval;
import java.util.UUID;


public final class RxNotificationEvent extends RxDeviceEvent<NotificationListener.NotificationEvent>
{

    RxNotificationEvent(NotificationListener.NotificationEvent event)
    {
        super(event);
    }


    public final boolean wasSuccess()
    {
        return m_event.wasSuccess();
    }

    public final RxBleDevice device()
    {
        return RxBleManager.getOrCreateDevice(m_event.device());
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.NotificationListener.NotificationEvent#macAddress()}
     */
    public final String macAddress()
    {
        return event().macAddress();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.NotificationListener.NotificationEvent#macAddress()}
     */
    public final NotificationListener.Type type()
    {
        return event().type();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.NotificationListener.NotificationEvent#macAddress()}
     */
    public final UUID serviceUuid()
    {
        return event().serviceUuid();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.NotificationListener.NotificationEvent#macAddress()}
     */
    public final UUID charUuid()
    {
        return event().charUuid();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.NotificationListener.NotificationEvent#macAddress()}
     */
    public final @Nullable(Nullable.Prevalence.NEVER) byte[] data()
    {
        return event().data();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.NotificationListener.NotificationEvent#macAddress()}
     */
    public final NotificationListener.Status status()
    {
        return event().status();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.NotificationListener.NotificationEvent#macAddress()}
     */
    public final Interval time_ota()
    {
        return event().time_ota();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.NotificationListener.NotificationEvent#macAddress()}
     */
    public final Interval time_total()
    {
        return event().time_total();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.NotificationListener.NotificationEvent#macAddress()}
     */
    public final int gattStatus()
    {
        return event().gattStatus();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.NotificationListener.NotificationEvent#macAddress()}
     */
    public final boolean solicited()
    {
        return event().solicited();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.NotificationListener.NotificationEvent#macAddress()}
     */
    public final byte data_byte()
    {
        return event().data_byte();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.NotificationListener.NotificationEvent#macAddress()}
     */
    public final @Nullable(Nullable.Prevalence.NEVER) String data_utf8()
    {
        return event().data_utf8();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.NotificationListener.NotificationEvent#macAddress()}
     */
    public final @Nullable(Nullable.Prevalence.NEVER) String data_string()
    {
        return event().data_string();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.NotificationListener.NotificationEvent#macAddress()}
     */
    public final @Nullable(Nullable.Prevalence.NEVER) String data_string(final String charset)
    {
        return event().data_string(charset);
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.NotificationListener.NotificationEvent#macAddress()}
     */
    public final @Nullable(Nullable.Prevalence.NEVER) int data_int(boolean reverse)
    {
        return event().data_int(reverse);
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.NotificationListener.NotificationEvent#macAddress()}
     */
    public final @Nullable(Nullable.Prevalence.NEVER) short data_short(boolean reverse)
    {
        return event().data_short(reverse);
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.NotificationListener.NotificationEvent#data_long(boolean)}
     */
    public final @Nullable(Nullable.Prevalence.NEVER) long data_long(boolean reverse)
    {
        return event().data_long(reverse);
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.NotificationListener.NotificationEvent#characteristic()}
     */
    public final BleCharacteristic characteristic()
    {
        return event().characteristic();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.NotificationListener.NotificationEvent#service()}
     */
    public final BleService service()
    {
        return event().service();
    }

}
