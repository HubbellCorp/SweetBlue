package com.idevicesinc.sweetblue.rx;


public interface RxInitFactory<T extends RxBleTransaction.RxInit>
{
    T newInitTxn();
}
