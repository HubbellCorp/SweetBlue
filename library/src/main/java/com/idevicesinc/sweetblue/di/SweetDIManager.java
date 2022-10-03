package com.idevicesinc.sweetblue.di;

import com.idevicesinc.sweetblue.utils.FunctionO;

import java.util.HashMap;
import java.util.Map;

/**
 * Dependency injection manager for SweetBlue. This is used internally to inject internal
 * dependencies into the system. This provides a better way to swap out internal classes for testing.
 * You are welcome to use this for your app as well, if you so feel inclined. Note that this is not
 * process aware, and you may get strange results if your app uses multiple processes.
 * <p>
 * You first must remember to register your object (it's best practice to create interfaces for
 * everything, and then register your implementation classes with that interface. This makes it so
 * you can easily swap out the implementation class when testing):
 * <pre>
 *
 *     <code>SweetDIManager mgr = SweetDIManager().getInstance();
 *     mgr.registerSingleton(IMyClass.class, MyClassImpl.class);
 *     </code>
 * </pre>
 * <p>
 * Then you simply call @{link {@link #get(Class, Object...)} to retrieve the instance of your class:
 * <pre>
 *
 *     <code>IMyClass myClassInstance = mgr.get(IMyClass.class);
 *
 *     </code>
 * </pre>
 *
 * @see DIScope for more info on the registration types
 */
public class SweetDIManager
{
    private static SweetDIManager s_managerInstance = null;

    private final Map<Class<?>, ObjectHolder<?, ?>> m_registeredObjects;


    private SweetDIManager()
    {
        m_registeredObjects = new HashMap<>();
    }

    /**
     * Get the singleton instance of the {@link SweetDIManager}.
     */
    public static SweetDIManager getInstance()
    {
        if (s_managerInstance == null)
        {
            synchronized (SweetDIManager.class)
            {
                s_managerInstance = new SweetDIManager();
            }
        }
        return s_managerInstance;
    }

    /**
     * Register a transient object into the DI system.
     *
     * @see DIScope#Transient
     */
    public final <B> void registerTransient(Class<B> objectClass)
    {
        registerTransient(objectClass, objectClass);
    }

    /**
     * Register a transient object into the DI system, with the given {@link FunctionO}. This
     * function will get called every time {@link #get(Class, Object...)} is called.
     *
     * @see DIScope#Transient
     */
    public final <T> void registerTransient(Class<T> objectClass, FunctionO<T> constructorFunc)
    {
        registerObject(objectClass, DIScope.Transient, constructorFunc);
    }

    /**
     * Register a transient object into the DI system with the given implementation class
     * i.e. <pre><code>mgr.registerTransient(IMyClass.class, MyClassImpl.class);</code></pre>
     *
     * @see DIScope#Transient
     */
    public final <B, I extends B> void registerTransient(Class<B> baseClass, Class<I> implementationClass)
    {
        registerObject(baseClass, implementationClass, DIScope.Transient);
    }

    /**
     * Register a scoped object into the DI system.
     * <p>
     * This scopes an instance of the given objectClass to the class calling the method {@link #get(Class, Object...)}.
     *
     * @see DIScope#Scoped
     */
    public final <T> void registerScoped(Class<T> objectClass)
    {
        registerScoped(objectClass, objectClass);
    }

    /**
     * Register a scoped object into the DI system, with the given {@link FunctionO}. This
     * function will get called any time a new instance of your class is created for the scope.
     * <p>
     * This scopes an instance of the given objectClass to the class calling the method {@link #get(Class, Object...)}.
     *
     * @see DIScope#Scoped
     */
    public final <T> void registerScoped(Class<T> objectClass, FunctionO<T> constructorFunction)
    {
        registerObject(objectClass, DIScope.Scoped, constructorFunction);
    }

    /**
     * Register a scoped object into the DI system with the given implementation class
     * i.e. <pre><code>mgr.registerScoped(IMyClass.class, MyClassImpl.class);</code></pre>
     * <p>
     * This scopes an instance of the given objectClass to the class calling the method {@link #get(Class, Object...)}.
     *
     * @see DIScope#Scoped
     */
    public final <B, I extends B> void registerScoped(Class<B> baseClass, Class<I> implementationClass)
    {
        registerObject(baseClass, implementationClass, DIScope.Scoped);
    }

    /**
     * Register a singleton object into the DI system.
     * <p>
     * This will only ever create a single instance of the given itemClass
     *
     * @see DIScope#Singleton
     */
    public final <T> void registerSingleton(Class<T> itemClass)
    {
        registerSingleton(itemClass, itemClass);
    }

    /**
     * Register a singleton object into the DI system, with the given {@link FunctionO}. This
     * function will get called once, the first time {@link #get(Class, Object...)} is called for this
     * objectClass.
     * <p>
     * This will only ever create a single instance of the given objectClass
     *
     * @see DIScope#Singleton
     */
    public final <T> void registerSingleton(Class<T> objectClass, FunctionO<T> constructorFunction)
    {
        registerObject(objectClass, DIScope.Singleton, constructorFunction);
    }

    /**
     * Register a singleton object into the DI system with the given implementation class
     * i.e. <pre><code>mgr.registerSingleton(IMyClass.class, MyClassImpl.class);</code></pre>
     * <p>
     * This will only ever create a single instance of the given objectClass
     *
     * @see DIScope#Singleton
     */
    public final <B, I extends B> void registerSingleton(Class<B> baseClass, Class<I> implementationClass)
    {
        registerObject(baseClass, implementationClass, DIScope.Singleton);
    }

    /**
     * Get an instance of a registered class in the DI system. You must first register the class
     * using one of the register methods in this class.
     *
     * @param constructorArgs - Used to pass in the constructor args to use if the instance is going
     *                        to be instantiated (this depends on how the object was registered). If
     *                        the instance already exists (singleton, or scoped), then that instance
     *                        will be returned, and the constructor args will be ignored.
     * @throws IllegalStateException if the requested class is not registered in the DI system
     */
    public final <B, I extends B> I get(Class<B> objectClass, Object... constructorArgs)
    {
        return get_private(objectClass, true, constructorArgs);
    }

    /**
     * Same as {@link #get(Class, Object...)}, only this will return <code>null</code> if the class isn't
     * registered in the system.
     *
     * @param constructorArgs - Used to pass in the constructor args to use if the instance is going
     *                        to be instantiated (this depends on how the object was registered). If
     *                        the instance already exists (singleton, or scoped), then that instance
     *                        will be returned, and the constructor args will be ignored.
     * @throws IllegalStateException if the requested class is not registered in the DI system
     */
    public final <B, I extends B> I safeGet(Class<B> objectClass, Object... constructorArgs)
    {
        return get_private(objectClass, false, constructorArgs);
    }

    /**
     * Returns true if the given class is registered in the DI system.
     */
    public final boolean isRegistered(Class<?> classToCheck)
    {
        synchronized (SweetDIManager.class)
        {
            return m_registeredObjects.containsKey(classToCheck);
        }
    }

    /**
     * Releases all registered objects, and any instances that may be cached in memory. The static
     * manager instance is nulled out. Make sure to call {@link #getInstance()} before calling
     * any other methods on the manager.
     */
    public final void dispose()
    {
        synchronized (SweetDIManager.class)
        {
            m_registeredObjects.clear();
            s_managerInstance = null;
        }
    }


    private <B, I extends B> I get_private(Class<B> objectClass, boolean throwExceptionOnNull, Object... constructorArgs)
    {
        ObjectHolder<?, ?> objectHolder;
        synchronized (m_registeredObjects)
        {
            objectHolder = m_registeredObjects.get(objectClass);
        }
        if (objectHolder == null)
        {
            if (throwExceptionOnNull)
            {
                throw new IllegalStateException("Unable to locate object holder for object class " + objectClass);
            }
            return null;
        }
        return (I) objectHolder.getInstance(constructorArgs);
    }

    private <B, I extends B> void registerObject(Class<B> baseClass, Class<I> implementationClass, DIScope scopeType)
    {
        ObjectHolder<B, I> holder = new ObjectHolder<>(baseClass, implementationClass, scopeType);
        synchronized (m_registeredObjects)
        {
            m_registeredObjects.put(baseClass, holder);
        }
    }

    private <B> void registerObject(Class<B> baseClass, DIScope scopeType, FunctionO<B> constructorFunction)
    {
        ObjectHolder<B, B> holder = new ObjectHolder<>(baseClass, scopeType, constructorFunction);
        synchronized (m_registeredObjects)
        {
            m_registeredObjects.put(baseClass, holder);
        }
    }
}
