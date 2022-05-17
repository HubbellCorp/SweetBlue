package com.idevicesinc.sweetblue.rx;


public interface RxAuthFactory<T extends RxBleTransaction.RxAuth>
{
    T newAuthTxn();
}
