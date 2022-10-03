package com.idevicesinc.sweetblue;

public class TestDIClass2 implements ITestDIClass2 {
    final ITestDIClass m_testDiClass;

    public TestDIClass2(ITestDIClass testDIClass)
    {
        m_testDiClass = testDIClass;
    }
}
