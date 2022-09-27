package com.idevicesinc.sweetblue.rx;

import com.idevicesinc.sweetblue.BleDevice;
import io.reactivex.rxjava3.functions.Function;


/**
 * Built in transformation for converting a {@link BleDevice} to {@link RxBleDevice}.
 */
public final class RxBleDeviceTransformer implements Function<BleDevice, RxBleDevice>
{
    @Override
    public RxBleDevice apply(BleDevice bleDevice) throws Exception
    {
        return RxBleDevice.create(bleDevice);
    }
}
