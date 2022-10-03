package com.idevicesinc.sweetblue;

import com.idevicesinc.sweetblue.di.SweetDIManager;
import com.idevicesinc.sweetblue.framework.AbstractTestClass;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class SweetDITest extends AbstractTestClass {

    private SweetDIManager m_manager;

    @Before
    public void startUp()
    {
        m_manager = SweetDIManager.getInstance();
    }

    @After
    public void cleanUp()
    {
        m_manager = null;
        SweetDIManager.getInstance().dispose();
    }

    @Test(timeout = 3000)
    public void transientRuntimeConstructorArgsDIObjectTest() throws Exception
    {
        m_manager.registerTransient(HeyLookAnotherTestClass.class);

        HeyLookAnotherTestClass instance = m_manager.get(HeyLookAnotherTestClass.class, "Something", 42, new Object());
        assertNotNull(instance);

        HeyLookAnotherTestClass secondInstance = m_manager.get(HeyLookAnotherTestClass.class, "Something2", 87, new Object());
        assertNotNull(secondInstance);
        assertNotEquals(instance, secondInstance);
    }

    @Test(timeout = 3000)
    public void transientCustomConstructorDIObjectTest() throws Exception
    {
        m_manager.registerTransient(YetEvenAnotherTestClass.class, () -> new YetEvenAnotherTestClass("something here") {});

        YetEvenAnotherTestClass instance = m_manager.get(YetEvenAnotherTestClass.class);
        assertNotNull(instance);

        YetEvenAnotherTestClass secondInstance = m_manager.get(YetEvenAnotherTestClass.class);
        assertNotNull(secondInstance);
        assertNotEquals(instance.hashCode(), secondInstance.hashCode());
    }

    @Test(timeout = 3000)
    public void scopedCustomConstructorDIObjectTest() throws Exception
    {
        m_manager.registerScoped(YetEvenAnotherTestClass.class, () -> new YetEvenAnotherTestClass("something here") {});
        m_manager.registerScoped(WrapperTestClass.class);

        YetEvenAnotherTestClass class1 = m_manager.get(YetEvenAnotherTestClass.class);
        WrapperTestClass class2 = m_manager.get(WrapperTestClass.class);
        assertNotNull(class1.m_something);
        assertNotNull(class2);
        assertNotEquals(class1.hashCode(), class2.m_class.hashCode());
    }

    @Test(timeout = 3000)
    public void singletonCustomConstructorDIObjectTest() throws Exception
    {
        m_manager.registerSingleton(YetEvenAnotherTestClass.class, () -> new YetEvenAnotherTestClass("something here") {});
        m_manager.registerScoped(WrapperTestClass.class);

        YetEvenAnotherTestClass class1 = m_manager.get(YetEvenAnotherTestClass.class);
        WrapperTestClass class2 = m_manager.get(WrapperTestClass.class);
        assertNotNull(class1.m_something);
        assertNotNull(class2);
        assertEquals(class1.hashCode(), class2.m_class.hashCode());
    }

    @Test(timeout = 3000)
    public void transientDIObjectTest() throws Exception
    {
        m_manager.registerTransient(ITestDIClass.class, TestDIClass.class);

        TestDIClass instance = m_manager.get(ITestDIClass.class);
        assertNotNull(instance);

        TestDIClass secondInstance = m_manager.get(ITestDIClass.class);
        assertNotNull(secondInstance);
        assertNotEquals(instance.hashCode(), secondInstance.hashCode());
    }

    @Test(timeout = 3000)
    public void scopedDIObjectTest() throws Exception
    {
        m_manager.registerScoped(ITestDIClass.class, TestDIClass.class);
        m_manager.registerScoped(ITestDIClass2.class, TestDIClass2.class);

        TestDIClass2 class1 = m_manager.get(ITestDIClass2.class);
        TestDIClass class2 = m_manager.get(ITestDIClass.class);
        assertNotNull(class1.m_testDiClass);
        assertNotNull(class2);
        assertNotEquals(class1.m_testDiClass.hashCode(), class2.hashCode());
    }

    @Test(timeout = 3000)
    public void singletonDIObjectTest() throws Exception
    {
        m_manager.registerSingleton(ITestDIClass.class, TestDIClass.class);
        m_manager.registerScoped(ITestDIClass2.class, TestDIClass2.class);

        TestDIClass2 class1 = m_manager.get(ITestDIClass2.class);
        TestDIClass class2 = m_manager.get(ITestDIClass.class);
        assertNotNull(class1.m_testDiClass);
        assertNotNull(class2);
        assertEquals(class1.m_testDiClass.hashCode(), class2.hashCode());
    }

    @Test(timeout = 3000)
    public void innerDependencyTest() throws Exception
    {
        m_manager.registerScoped(ITestDIClass.class, TestDIClass.class);
        m_manager.registerScoped(ITestDIClass2.class, TestDIClass2.class);

        TestDIClass2 test2Class = m_manager.get(ITestDIClass2.class);
        assertNotNull(test2Class);
        assertNotNull(test2Class.m_testDiClass);
    }

    @Test(timeout = 3000)
    public void transientInnerClassDIObjectTest() throws Exception
    {
        m_manager.registerTransient(ITestClass.class, TestClass.class);

        TestClass instance = m_manager.get(ITestClass.class);
        assertNotNull(instance);

        TestClass secondInstance = m_manager.get(ITestClass.class);
        assertNotNull(secondInstance);
        assertNotEquals(instance.hashCode(), secondInstance.hashCode());
    }

    @Test(timeout = 3000)
    public void scopedInnerClassDIObjectTest() throws Exception
    {
        m_manager.registerScoped(ITestClass.class, TestClass.class);

        OtherTestClass class1 = new OtherTestClass();
        YetAnotherTestClass class2 = new YetAnotherTestClass();
        assertNotNull(class1.m_testClass);
        assertNotNull(class2.m_testClass);
        assertNotEquals(class1.m_testClass.hashCode(), class2.m_testClass.hashCode());
    }

    @Test(timeout = 3000)
    public void singletonInnerClassDIObjectTest() throws Exception
    {
        m_manager.registerSingleton(ITestClass.class, TestClass.class);

        OtherTestClass class1 = new OtherTestClass();
        YetAnotherTestClass class2 = new YetAnotherTestClass();
        assertNotNull(class1.m_testClass);
        assertNotNull(class2.m_testClass);
        assertEquals(class1.m_testClass.hashCode(), class2.m_testClass.hashCode());
    }

    @Test(timeout = 3000)
    public void innerDependencyInnerClassTest() throws Exception
    {
        m_manager.registerScoped(ITestClass.class, TestClass.class);
        m_manager.registerScoped(ITest2Class.class, Test2Class.class);

        Test2Class test2Class = m_manager.get(ITest2Class.class);
        assertNotNull(test2Class);
        assertNotNull(test2Class.m_testClass);
    }

    private interface ITestClass {}

    private static class TestClass implements ITestClass
    {
    }

    private interface ITest2Class {}

    private class Test2Class implements ITest2Class
    {
        final ITestClass m_testClass;

        Test2Class(ITestClass testClass)
        {
            m_testClass = testClass;
        }
    }

    private class OtherTestClass
    {
        final ITestClass m_testClass;

        public OtherTestClass() {
            m_testClass = SweetDIManager.getInstance().get(ITestClass.class);
        }
    }

    private class YetAnotherTestClass
    {
        final ITestClass m_testClass;

        public YetAnotherTestClass() {
            m_testClass = SweetDIManager.getInstance().get(ITestClass.class);
        }
    }

    private abstract class YetEvenAnotherTestClass {
        private String m_something;

        public YetEvenAnotherTestClass(String something)
        {
            m_something = something;
        }
    }

    private class WrapperTestClass {

        private final YetEvenAnotherTestClass m_class;

        public WrapperTestClass(YetEvenAnotherTestClass tClass) {
            m_class = tClass;
        }
    }

    private class HeyLookAnotherTestClass {
        private final String m_something;
        private final int m_someNumber;
        private Object m_someObject;

        HeyLookAnotherTestClass(String something, int number, Object someObject)
        {
            m_something = something;
            m_someNumber = number;
            m_someObject = someObject;
        }
    }
}
