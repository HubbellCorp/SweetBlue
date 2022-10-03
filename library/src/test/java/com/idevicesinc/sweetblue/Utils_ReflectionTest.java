package com.idevicesinc.sweetblue;

import com.idevicesinc.sweetblue.framework.AbstractTestClass;
import com.idevicesinc.sweetblue.utils.FunctionIO;
import com.idevicesinc.sweetblue.utils.Pointer;
import com.idevicesinc.sweetblue.utils.ReflectionTestClass1;
import com.idevicesinc.sweetblue.utils.ReflectionTestClass2DIParams;
import com.idevicesinc.sweetblue.utils.Utils_Reflection;
import org.junit.Test;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;


public class Utils_ReflectionTest extends AbstractTestClass
{

    @Test
    public void constructInstanceTest()
    {
        Utils_ReflectionTest thisClass = Utils_Reflection.constructInstance(Utils_ReflectionTest.class, null);
        assertNotNull(thisClass);
    }

    @Test
    public void constructWithParamsTest()
    {
        ReflectionTestClass1 theClass = Utils_Reflection.constructInstance(ReflectionTestClass1.class, null, new Object(), new Object());
        assertNotNull(theClass);
    }

    @Test
    public void constructWithSingleDIParamTest() throws Exception
    {
        Method method = Utils_Reflection.class.getDeclaredMethod("getConstructor", Class.class, Pointer.class, Pointer.class, FunctionIO.class);
        Object[] params = new Object[4];
        params[0] = ReflectionTestClass2DIParams.class;
        params[1] = new Pointer<>(new Class[0]);
        params[2] = new Pointer<>(new Object[0]);
        params[3] = null;
        boolean accessible = method.isAccessible();
        method.setAccessible(true);
        Constructor<ReflectionTestClass2DIParams> theClass = (Constructor<ReflectionTestClass2DIParams>) method.invoke(null, params);
        method.setAccessible(accessible);
        assertNotNull(theClass);
    }

    @Test
    public void constructWithSingleDIAndPassedParamTest() throws Exception
    {
        Method method = Utils_Reflection.class.getDeclaredMethod("getConstructor", Class.class, Pointer.class, Pointer.class, FunctionIO.class);
        Object[] params = new Object[4];
        params[0] = ReflectionTestClass2DIParams.class;
        params[1] = new Pointer<>( new Class<?>[] { String.class });
        params[2] = new Pointer<>(new Object[0]);
        params[3] = null;
        boolean accessible = method.isAccessible();
        method.setAccessible(true);
        Constructor<ReflectionTestClass2DIParams> theClass = (Constructor<ReflectionTestClass2DIParams>) method.invoke(null, params);
        method.setAccessible(accessible);
        assertNotNull(theClass);
    }

    @Test
    public void constructInnerClassTest()
    {
        TestClass test = Utils_Reflection.constructInstance(TestClass.class, null);
        assertNotNull(test);
    }

    @Test
    public void constructInnerStaticClassTest()
    {
        TestClassStatic test = Utils_Reflection.constructInstance(TestClassStatic.class, null);
        assertNotNull(test);
    }

    @Test
    public void constructInnerImplClassTest()
    {
        TestClassImplITest test = Utils_Reflection.constructInstance(TestClassImplITest.class, null);
        assertNotNull(test);
    }

    @Test
    public void constructInnerImplStaticClassTest()
    {
        TestClassImplITestStatic test = Utils_Reflection.constructInstance(TestClassImplITestStatic.class, null);
        assertNotNull(test);
    }


    class TestClass {}

    static class TestClassStatic {}

    class TestClassImplITest implements ITestDIClass {}

    static class TestClassImplITestStatic implements ITestDIClass {}
}
