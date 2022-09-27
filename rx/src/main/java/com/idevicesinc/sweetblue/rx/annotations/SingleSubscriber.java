package com.idevicesinc.sweetblue.rx.annotations;


import com.idevicesinc.sweetblue.ScanOptions;
import com.idevicesinc.sweetblue.rx.RxBleManager;
import io.reactivex.rxjava3.core.Observable;

/**
 * Annotation that dictates that an {@link Observable} should only ever have a single subscription.
 * For example, {@link RxBleManager#scan(ScanOptions)}, or {@link RxBleManager#scan_onlyNew(ScanOptions)}.
 */
public @interface SingleSubscriber
{
}
