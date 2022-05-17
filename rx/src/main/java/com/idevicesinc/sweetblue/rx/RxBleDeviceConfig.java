package com.idevicesinc.sweetblue.rx;


import com.idevicesinc.sweetblue.BleDeviceConfig;


@SuppressWarnings("squid:ClassVariableVisibilityCheck")
public class RxBleDeviceConfig extends BleDeviceConfig
{

    public RxAuthFactory defaultRxAuthFactory           = null;

    public RxInitFactory defaultRxInitFactory           = null;

    @Override public RxBleDeviceConfig clone()
    {
        return (RxBleDeviceConfig) super.clone();
    }
}
