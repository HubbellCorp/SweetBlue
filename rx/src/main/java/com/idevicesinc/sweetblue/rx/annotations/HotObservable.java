package com.idevicesinc.sweetblue.rx.annotations;

import io.reactivex.Observable;
import io.reactivex.Flowable;

/**
 * Annotation used to dictate if a particular {@link Observable}, or {@link Flowable} is considered
 * to be a hot observable.
 * <p>
 *     Note that there is no ColdObservable annotation. If an {@link Observable}/{@link Flowable} is not marked
 *     with an annotation, it can be assumed that it is a cold observable.
 * </p>
 */
public @interface HotObservable
{
}
