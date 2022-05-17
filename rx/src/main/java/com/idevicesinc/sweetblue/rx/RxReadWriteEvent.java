package com.idevicesinc.sweetblue.rx;


import com.idevicesinc.sweetblue.BleCharacteristic;
import com.idevicesinc.sweetblue.BleConnectionPriority;
import com.idevicesinc.sweetblue.BleDescriptor;
import com.idevicesinc.sweetblue.BleService;
import com.idevicesinc.sweetblue.DescriptorFilter;
import com.idevicesinc.sweetblue.ReadWriteListener;
import com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent;
import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Phy;
import java.util.UUID;


public final class RxReadWriteEvent extends RxDeviceEvent<ReadWriteListener.ReadWriteEvent>
{

    RxReadWriteEvent(ReadWriteListener.ReadWriteEvent event)
    {
        super(event);
    }


    /**
     * Forwards {@link com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent#wasSuccess()}
     */
    public final boolean wasSuccess()
    {
        return m_event.wasSuccess();
    }

    public final RxBleDevice device()
    {
        return RxBleManager.getOrCreateDevice(m_event.device());
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent#isNull()}
     */
    public final boolean isNull()
    {
        return m_event.isNull();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent#isRead()}
     */
    public final boolean isRead()
    {
        return m_event.isRead();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent#isWrite()}
     */
    public final boolean isWrite()
    {
        return m_event.isWrite();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent#wasCancelled()}
     */
    public final boolean wasCancelled()
    {
        return m_event.wasCancelled();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent#solicited()}
     */
    public final boolean solicited()
    {
        return m_event.solicited();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent#characteristic()}
     */
    public final BleCharacteristic characteristic()
    {
        return m_event.characteristic();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent#connectionPriority()}
     */
    public final BleConnectionPriority connectionPriority()
    {
        return m_event.connectionPriority();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent#serviceUuid()}
     */
    public final UUID serviceUuid()
    {
        return m_event.serviceUuid();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent#charUuid()}
     */
    public final UUID charUuid()
    {
        return m_event.charUuid();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent#descUuid()}
     */
    public final UUID descUuid()
    {
        return m_event.descUuid();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent#data()}
     */
    public final @Nullable(Nullable.Prevalence.NEVER) byte[] data()
    {
        return m_event.data();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent#phy()}
     */
    public final @Nullable(Nullable.Prevalence.NEVER) Phy phy()
    {
        return m_event.phy();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent#rssi()}
     */
    public final int rssi()
    {
        return m_event.rssi();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent#mtu()}
     */
    public final int mtu()
    {
        return m_event.mtu();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent#status()}
     */
    public final ReadWriteListener.Status status()
    {
        return m_event.status();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent#time_ota()}
     */
    public final Interval time_ota()
    {
        return m_event.time_ota();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent#time_total()}
     */
    public final Interval time_total()
    {
        return m_event.time_total();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent#gattStatus()}
     */
    public final int gattStatus()
    {
        return m_event.gattStatus();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent#descriptorFilter()}
     */
    public final @Nullable(Nullable.Prevalence.NORMAL) DescriptorFilter descriptorFilter()
    {
        return m_event.descriptorFilter();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent#service()}
     */
    public final @Nullable(Nullable.Prevalence.NORMAL) BleService service()
    {
        return m_event.service();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent#descriptor()}
     */
    public final @Nullable(Nullable.Prevalence.NORMAL) BleDescriptor descriptor()
    {
        return m_event.descriptor();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent#data_byte()}
     */
    public final byte data_byte()
    {
        return m_event.data_byte();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent#data_utf8()}
     */
    public final @Nullable(Nullable.Prevalence.NEVER) String data_utf8()
    {
        return m_event.data_utf8();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent#data_string()}
     */
    public final @Nullable(Nullable.Prevalence.NEVER) String data_string()
    {
        return m_event.data_string();
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent#data_string(String)}
     */
    public final @Nullable(Nullable.Prevalence.NEVER) String data_string(final String charset)
    {
        return m_event.data_string(charset);
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent#data_int(boolean)}
     */
    public final @Nullable(Nullable.Prevalence.NEVER) int data_int(boolean reverse)
    {
        return m_event.data_int(reverse);
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent#data_short(boolean)}
     */
    public final @Nullable(Nullable.Prevalence.NEVER) short data_short(boolean reverse)
    {
        return m_event.data_short(reverse);
    }

    /**
     * Forwards {@link com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent#data_long(boolean)}
     */
    public final @Nullable(Nullable.Prevalence.NEVER) long data_long(boolean reverse)
    {
        return m_event.data_long(reverse);
    }

}
