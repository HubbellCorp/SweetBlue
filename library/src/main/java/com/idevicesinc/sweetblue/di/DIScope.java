package com.idevicesinc.sweetblue.di;

import com.idevicesinc.sweetblue.utils.FunctionO;

/**
 * Enumeration of the different scope types used within the DI system.
 */
public enum DIScope {
    /**
     * The object will be created every time {@link SweetDIManager#get(Class, Object...)} is called.
     *
     * @see SweetDIManager#registerTransient(Class)
     * @see SweetDIManager#registerTransient(Class, Class)
     * @see SweetDIManager#registerTransient(Class, FunctionO)
     */
    Transient,

    /**
     * The object will be scoped to the calling class. Each calling class will have it own instance
     * when {@link SweetDIManager#get(Class, Object...)} is called.
     *
     * @see SweetDIManager#registerScoped(Class)
     * @see SweetDIManager#registerScoped(Class, Class)
     * @see SweetDIManager#registerScoped(Class, FunctionO)
     */
    Scoped,

    /**
     * Only 1 instance of the object will ever be created. The same instance will always be returned
     * when calling {@link SweetDIManager#get(Class, Object...)}.
     *
     * @see SweetDIManager#registerSingleton(Class)
     * @see SweetDIManager#registerSingleton(Class, Class)
     * @see SweetDIManager#registerSingleton(Class, FunctionO)
     */
    Singleton
}
