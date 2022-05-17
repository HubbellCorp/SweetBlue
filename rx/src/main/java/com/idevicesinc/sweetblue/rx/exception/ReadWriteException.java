package com.idevicesinc.sweetblue.rx.exception;


import com.idevicesinc.sweetblue.BleRead;
import com.idevicesinc.sweetblue.BleWrite;
import com.idevicesinc.sweetblue.rx.RxReadWriteEvent;

/**
 * Exception class which holds an instance of {@link com.idevicesinc.sweetblue.ReadWriteListener.ReadWriteEvent}, which gives more information about what went wrong
 * with a read/write. This gets passed into the onError method when using {@link com.idevicesinc.sweetblue.rx.RxBleDevice#read(BleRead)},
 * or {@link com.idevicesinc.sweetblue.rx.RxBleDevice#write(BleWrite)}.
 */
public class ReadWriteException extends EventException
{

    public ReadWriteException(RxReadWriteEvent event)
    {
        super(event);
    }

    @Override
    public RxReadWriteEvent getRxEvent()
    {
        return super.getRxEvent();
    }
}