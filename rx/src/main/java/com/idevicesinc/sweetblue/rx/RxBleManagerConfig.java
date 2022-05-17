package com.idevicesinc.sweetblue.rx;

import com.idevicesinc.sweetblue.BleManagerConfig;

import java.lang.reflect.Field;


@SuppressWarnings("squid:ClassVariableVisibilityCheck")
public class RxBleManagerConfig extends BleManagerConfig
{

    public RxAuthFactory defaultRxAuthFactory           = null;

    public RxInitFactory defaultRxInitFactory           = null;

    public RxBleDeviceConfig toDeviceConfig()
    {
        return cloneFromManager(this);
    }

    private static RxBleDeviceConfig cloneFromManager(RxBleManagerConfig mgrConfig)
    {
        RxBleDeviceConfig deviceConfig = new RxBleDeviceConfig();
        Field[] mgrFields = mgrConfig.getClass().getFields();
        for (Field field : mgrFields)
        {
            try
            {
                Field dField = deviceConfig.getClass().getField(field.getName());
                field.setAccessible(true);
                dField.set(deviceConfig, field.get(mgrConfig));
            } catch (Exception e)
            {
            }
        }
        return deviceConfig;
    }

    @Override public RxBleManagerConfig clone()
    {
        return (RxBleManagerConfig) super.clone();
    }
}
