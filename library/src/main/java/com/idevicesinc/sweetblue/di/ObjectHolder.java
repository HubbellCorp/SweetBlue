package com.idevicesinc.sweetblue.di;

import com.idevicesinc.sweetblue.utils.FunctionO;
import com.idevicesinc.sweetblue.utils.FunctionVargs;
import com.idevicesinc.sweetblue.utils.Utils_Reflection;
import java.util.HashMap;
import java.util.Map;


final class ObjectHolder<B, I extends B>
{
    private final SweetDIManager m_manager;
    Class<I> m_implementationClass;
    Class<B> m_baseClass;
    DIScope m_scopeType;
    I m_instance;
    Map<Class<?>, I> m_scopedInstanceMap;
    FunctionVargs<I> m_constructorFunction;


    ObjectHolder(Class<B> baseClass, Class<I> implementationClass, DIScope scopeType)
    {
        m_manager = SweetDIManager.getInstance();
        m_implementationClass = implementationClass;
        m_baseClass = baseClass;
        m_scopeType = scopeType;
    }

    @SuppressWarnings("unchecked")
    ObjectHolder(Class<B> baseClass, DIScope scopeType, FunctionVargs<I> constructorFunction)
    {
        m_manager = SweetDIManager.getInstance();
        m_implementationClass = (Class<I>) baseClass;
        m_constructorFunction = constructorFunction;
        m_baseClass = baseClass;
        m_scopeType = scopeType;
    }

    private static boolean hasArgs(Object... args)
    {
        return args != null && args.length > 0;
    }

    public I getInstance(Object... args)
    {
        if (m_scopeType != null)
        {
            switch (m_scopeType)
            {
                case Scoped:
                    return getScopedInstance(args);
                case Singleton:
                    if (m_instance == null)
                    {
                        boolean hasArgs = hasArgs(args);
                        m_instance = hasArgs ? constructParameterInstance(args) : constructInstance();
                    }
                    return m_instance;
                case Transient:
                    return hasArgs(args) ? constructParameterInstance(args) : constructInstance();
            }
        }
        throw new RuntimeException("Unknown, or null scope type: " + m_scopeType);
    }



    private I constructParameterInstance(Object... constructorArgs)
    {
        if (m_constructorFunction != null)
            return m_constructorFunction.call(constructorArgs);

        if (constructorArgs != null)
        {
            I instance = Utils_Reflection.constructInstance(m_implementationClass, param -> m_manager.safeGet(param), constructorArgs);
            if (instance == null)
                throw new RuntimeException("Unable to instantiate dependency class");
            return instance;
        }
        else
        {
            return constructInstance();
        }
    }

    private I constructInstance()
    {
        if (m_constructorFunction != null) return m_constructorFunction.call();

        I instance = Utils_Reflection.constructInstance(m_implementationClass, param -> m_manager.safeGet(param));

        if (instance != null) return instance;

        throw new RuntimeException("Unable to instantiate class " + m_implementationClass);
    }

    private I getScopedInstance(Object... args)
    {
        I instance;
        Class<?> callingClass;
        try
        {
            StackTraceElement[] e = new Exception().getStackTrace();
            callingClass = Class.forName(e[4].getClassName());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        if (m_scopedInstanceMap == null)
        {
            m_scopedInstanceMap = new HashMap<>();
        }
        instance = m_scopedInstanceMap.get(callingClass);

        if (instance == null)
        {
            instance = hasArgs(args) ? constructParameterInstance(null, args) : constructInstance();
            m_scopedInstanceMap.put(callingClass, instance);
        }
        return instance;
    }
}
