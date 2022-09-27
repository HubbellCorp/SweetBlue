package com.idevicesinc.sweetblue.rx;


import com.idevicesinc.sweetblue.BleServer;
import io.reactivex.rxjava3.functions.Function;


/**
 * Transformation class to convert a {@link BleServer} to an {@link RxBleServer}.
 */
public class RxBleServerTransformer implements Function<BleServer, RxBleServer>
{
    @Override
    public RxBleServer apply(BleServer server) throws Exception
    {
        return RxBleManager.getOrCreateServer(server);
    }
}
