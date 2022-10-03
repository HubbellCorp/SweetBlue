package com.idevicesinc.sweetblue.framework;

import org.hamcrest.Matcher;
import org.junit.Assert;
import java.util.concurrent.Semaphore;

/**
 * Base class to help in writing android tests. This class allows you to easily test asynchronous operations (but you can also do synchronous tests with it).
 * You should make sure to call the assert methods baked into this class when running asynchronous tests, otherwise if a test fails, it will
 * keep spinning forever -- because when you call {@link #startAsyncTest()}, we acquire a {@link Semaphore}, which locks the thread. You must
 * also remember to call {@link #succeed()}, again when calling {@link #startAsyncTest()} to release the semaphore and complete the test.
 *
 * @see #startAsyncTest()
 * @see #startSynchronousTest()
 * @see #succeed()
 */
@SuppressWarnings("squid:S1181")
public abstract class AbstractTestClass
{

    private Semaphore s;
    private String className;
    private String methodName;
    private Error exception;
    private boolean useSemaphore;



    private void startTest(boolean useSemaphore) throws Exception
    {
        exception = null;
        className = this.getClass().getName();
        methodName = getSoonestTrace().getMethodName();
        this.useSemaphore = useSemaphore;
        if (useSemaphore)
        {
            s = new Semaphore(0);
            s.acquire();

            if (exception != null)
                throw exception;
        }
    }

    /**
     * Call this method if the test is synchronous. This should be the first method called in the test method. You should also call {@link #succeed()} when the test is
     * done, but it's not necessary for the test to complete.
     */
    public void startSynchronousTest() throws Exception
    {
        startTest(false);
    }

    /**
     * Call this method if the test is asynchronous. This should be the last method in the test method, as it acquires a {@link Semaphore}.
     * You then need to call {@link #succeed()} to, well, succeed the test. Otherwise, you should be using any of the assert methods in this class
     * to catch errors (and will throw exceptions if there are -- and properly kill the test if this is the case)
     */
    public void startAsyncTest() throws Exception
    {
        startTest(true);
    }

    /**
     * This will re-acquire the semaphore to lock the current thread. In most cases, you shouldn't need to use this, but it's here for flexibility.
     */
    public void reacquire() throws Exception
    {
        useSemaphore = true;
        s = new Semaphore(0);
        s.acquire();
    }

    /**
     * Releases the semaphore, if it's being held. You should not call this directly, but instead use {@link #succeed()}, unless you are doing
     * something that requires re-aquiring the semaphore.
     */
    public void release()
    {
        if (s != null)
            s.release();
    }

    /**
     * Releases the semphore held by this class (if applicable), and prints a message to the console about the test completing successfully.
     */
    public void succeed()
    {
        System.out.println(methodName + " completed successfully.");
        release();
    }

    /**
     * Forwards {@link Assert#assertTrue(boolean)}, and wraps it in a try/catch so that we can properly release the semaphore when a test
     * fails (so it doesn't lock up).
     */
    public void assertTrue(boolean condition)
    {
        try
        {
            Assert.assertTrue(condition);
        }
        catch (Error e)
        {
            handleException(e);
        }
    }

    /**
     * Forwards {@link Assert#assertTrue(String, boolean)}, and wraps it in a try/catch so that we can properly release the semaphore when a test
     * fails (so it doesn't lock up).
     */
    public void assertTrue(String msg, boolean condition)
    {
        try
        {
            Assert.assertTrue(msg, condition);
        }
        catch (Error e)
        {
            handleException(e);
        }
    }

    /**
     * Forwards {@link Assert#assertFalse(boolean)}, and wraps it in a try/catch so that we can properly release the semaphore when a test
     * fails (so it doesn't lock up).
     */
    public void assertFalse(boolean condition)
    {
        try
        {
            Assert.assertFalse(condition);
        }
        catch (Error e)
        {
            handleException(e);
        }
    }

    /**
     * Forwards {@link Assert#assertFalse(String, boolean)}, and wraps it in a try/catch so that we can properly release the semaphore when a test
     * fails (so it doesn't lock up).
     */
    public void assertFalse(String msg, boolean condition)
    {
        try
        {
            Assert.assertFalse(msg, condition);
        }
        catch (Error e)
        {
            handleException(e);
        }
    }

    /**
     * Forwards {@link Assert#assertNotNull(Object)}, and wraps it in a try/catch so that we can properly release the semaphore when a test
     * fails (so it doesn't lock up).
     */
    public void assertNotNull(Object object)
    {
        try
        {
            Assert.assertNotNull(object);
        }
        catch (Error e)
        {
            handleException(e);
        }
    }

    /**
     * Forwards {@link Assert#assertNotNull(String, Object)}, and wraps it in a try/catch so that we can properly release the semaphore when a test
     * fails (so it doesn't lock up).
     */
    public void assertNotNull(String msg, Object object)
    {
        try
        {
            Assert.assertNotNull(msg, object);
        }
        catch (Error e)
        {
            handleException(e);
        }
    }

    /**
     * Forwards {@link Assert#assertNull(Object)}, and wraps it in a try/catch so that we can properly release the semaphore when a test
     * fails (so it doesn't lock up).
     */
    public void assertNull(Object object)
    {
        try
        {
            Assert.assertNull(object);
        }
        catch (Error e)
        {
            handleException(e);
        }
    }

    /**
     * Forwards {@link Assert#assertNull(String, Object)}, and wraps it in a try/catch so that we can properly release the semaphore when a test
     * fails (so it doesn't lock up).
     */
    public void assertNull(String msg, Object object)
    {
        try
        {
            Assert.assertNull(msg, object);
        }
        catch (Error e)
        {
            handleException(e);
        }
    }

    /**
     * Forwards {@link Assert#assertEquals(long, long)}, and wraps it in a try/catch so that we can properly release the semaphore when a test
     * fails (so it doesn't lock up).
     */
    public void assertEquals(long expected, long actual)
    {
        try
        {
            Assert.assertEquals(expected, actual);
        }
        catch (Error e)
        {
            handleException(e);
        }
    }

    /**
     * Forwards {@link Assert#assertEquals(String, long, long)}, and wraps it in a try/catch so that we can properly release the semaphore when a test
     * fails (so it doesn't lock up).
     */
    public void assertEquals(String failMsg, long expected, long actual)
    {
        try
        {
            Assert.assertEquals(failMsg, expected, actual);
        }
        catch (Error e)
        {
            handleException(e);
        }
    }

    /**
     * Forwards {@link Assert#assertEquals(double, double, double)}, and wraps it in a try/catch so that we can properly release the semaphore when a test
     * fails (so it doesn't lock up).
     */
    public void assertEquals(double expected, double actual, double delta)
    {
        try
        {
            Assert.assertEquals(expected, actual, delta);
        }
        catch (Error e)
        {
            handleException(e);
        }
    }

    /**
     * Forwards {@link Assert#assertEquals(String, double, double, double)}, and wraps it in a try/catch so that we can properly release the semaphore when a test
     * fails (so it doesn't lock up).
     */
    public void assertEquals(String failMsg, double expected, double actual, double delta)
    {
        try
        {
            Assert.assertEquals(failMsg, expected, actual, delta);
        }
        catch (Error e)
        {
            handleException(e);
        }
    }

    /**
     * Forwards {@link Assert#assertEquals(Object, Object)}, and wraps it in a try/catch so that we can properly release the semaphore when a test
     * fails (so it doesn't lock up).
     */
    public void assertEquals(String expected, String actual)
    {
        try
        {
            Assert.assertEquals(expected, actual);
        }
        catch (Error e)
        {
            handleException(e);
        }
    }

    /**
     * Forwards {@link Assert#assertEquals(String, Object, Object)}, and wraps it in a try/catch so that we can properly release the semaphore when a test
     * fails (so it doesn't lock up).
     */
    public void assertEquals(String failMsg, String expected, String actual)
    {
        try
        {
            Assert.assertEquals(failMsg, expected, actual);
        }
        catch (Error e)
        {
            handleException(e);
        }
    }

    /**
     * Forwards {@link Assert#assertNotEquals(long, long)}, and wraps it in a try/catch so that we can properly release the semaphore when a test
     * fails (so it doesn't lock up).
     */
    public void assertNotEquals(long expected, long actual)
    {
        try
        {
            Assert.assertNotEquals(expected, actual);
        }
        catch (Error e)
        {
            handleException(e);
        }
    }

    /**
     * Forwards {@link Assert#assertNotEquals(String, long, long)}, and wraps it in a try/catch so that we can properly release the semaphore when a test
     * fails (so it doesn't lock up).
     */
    public void assertNotEquals(String failMsg, long expected, long actual)
    {
        try
        {
            Assert.assertNotEquals(failMsg, expected, actual);
        }
        catch (Error e)
        {
            handleException(e);
        }
    }

    /**
     * Forwards {@link Assert#assertNotEquals(Object, Object)}, and wraps it in a try/catch so that we can properly release the semaphore when a test
     * fails (so it doesn't lock up).
     */
    public void assertNotEquals(Object expected, Object actual)
    {
        try
        {
            Assert.assertNotEquals(expected, actual);
        }
        catch (Error e)
        {
            handleException(e);
        }
    }

    /**
     * Forwards {@link Assert#assertNotEquals(String, Object, Object)}, and wraps it in a try/catch so that we can properly release the semaphore when a test
     * fails (so it doesn't lock up).
     */
    public void assertNotEquals(String failMsg, Object expected, Object actual)
    {
        try
        {
            Assert.assertNotEquals(failMsg, expected, actual);
        }
        catch (Error e)
        {
            handleException(e);
        }
    }

    /**
     * Forwards {@link Assert#assertNotEquals(float, float, float)}, and wraps it in a try/catch so that we can properly release the semaphore when a test
     * fails (so it doesn't lock up).
     */
    public void assertNotEquals(float expected, float actual, float delta)
    {
        try
        {
            Assert.assertNotEquals(expected, actual, delta);
        }
        catch (Error e)
        {
            handleException(e);
        }
    }

    /**
     * Forwards {@link Assert#assertNotEquals(String, float, float, float)}, and wraps it in a try/catch so that we can properly release the semaphore when a test
     * fails (so it doesn't lock up).
     */
    public void assertNotEquals(String failMsg, float expected, float actual, float delta)
    {
        try
        {
            Assert.assertNotEquals(failMsg, expected, actual, delta);
        }
        catch (Error e)
        {
            handleException(e);
        }
    }

    /**
     * Forwards {@link Assert#assertNotEquals(double, double, double)}, and wraps it in a try/catch so that we can properly release the semaphore when a test
     * fails (so it doesn't lock up).
     */
    public void assertNotEquals(double expected, double actual, double delta)
    {
        try
        {
            Assert.assertNotEquals(expected, actual, delta);
        }
        catch (Error e)
        {
            handleException(e);
        }
    }

    /**
     * Forwards {@link Assert#assertNotEquals(String, double, double, double)}, and wraps it in a try/catch so that we can properly release the semaphore when a test
     * fails (so it doesn't lock up).
     */
    public void assertNotEquals(String failMsg, double expected, double actual, double delta)
    {
        try
        {
            Assert.assertNotEquals(failMsg, expected, actual, delta);
        }
        catch (Error e)
        {
            handleException(e);
        }
    }

    /**
     * Forwards {@link Assert#assertArrayEquals(byte[], byte[])}, and wraps it in a try/catch so that we can properly release the semaphore when a test
     * fails (so it doesn't lock up).
     */
    public void assertArrayEquals(byte[] expected, byte[] actual)
    {
        try
        {
            Assert.assertArrayEquals(expected, actual);
        }
        catch (Error e)
        {
            handleException(e);
        }
    }

    /**
     * Forwards {@link Assert#assertArrayEquals(String, byte[], byte[])}, and wraps it in a try/catch so that we can properly release the semaphore when a test
     * fails (so it doesn't lock up).
     */
    public void assertArrayEquals(String msg, byte[] expected, byte[] actual)
    {
        try
        {
            Assert.assertArrayEquals(msg, expected, actual);
        }
        catch (Error e)
        {
            handleException(e);
        }
    }

    /**
     * Forwards {@link Assert#assertArrayEquals(int[], int[])}, and wraps it in a try/catch so that we can properly release the semaphore when a test
     * fails (so it doesn't lock up).
     */
    public void assertArrayEquals(int[] expected, int[] actual)
    {
        try
        {
            Assert.assertArrayEquals(expected, actual);
        }
        catch (Error e)
        {
            handleException(e);
        }
    }

    /**
     * Forwards {@link Assert#assertArrayEquals(String, int[], int[])}, and wraps it in a try/catch so that we can properly release the semaphore when a test
     * fails (so it doesn't lock up).
     */
    public void assertArrayEquals(String msg, int[] expected, int[] actual)
    {
        try
        {
            Assert.assertArrayEquals(msg, expected, actual);
        }
        catch (Error e)
        {
            handleException(e);
        }
    }

    /**
     * Forwards {@link Assert#assertArrayEquals(short[], short[])}, and wraps it in a try/catch so that we can properly release the semaphore when a test
     * fails (so it doesn't lock up).
     */
    public void assertArrayEquals(short[] expected, short[] actual)
    {
        try
        {
            Assert.assertArrayEquals(expected, actual);
        }
        catch (Error e)
        {
            handleException(e);
        }
    }

    /**
     * Forwards {@link Assert#assertArrayEquals(String, short[], short[])}, and wraps it in a try/catch so that we can properly release the semaphore when a test
     * fails (so it doesn't lock up).
     */
    public void assertArrayEquals(String msg, short[] expected, short[] actual)
    {
        try
        {
            Assert.assertArrayEquals(msg, expected, actual);
        }
        catch (Error e)
        {
            handleException(e);
        }
    }

    /**
     * Forwards {@link Assert#assertArrayEquals(long[], long[])}, and wraps it in a try/catch so that we can properly release the semaphore when a test
     * fails (so it doesn't lock up).
     */
    public void assertArrayEquals(long[] expected, long[] actual)
    {
        try
        {
            Assert.assertArrayEquals(expected, actual);
        }
        catch (Error e)
        {
            handleException(e);
        }
    }

    /**
     * Forwards {@link Assert#assertArrayEquals(String, long[], long[])}, and wraps it in a try/catch so that we can properly release the semaphore when a test
     * fails (so it doesn't lock up).
     */
    public void assertArrayEquals(String msg, long[] expected, long[] actual)
    {
        try
        {
            Assert.assertArrayEquals(msg, expected, actual);
        }
        catch (Error e)
        {
            handleException(e);
        }
    }

    /**
     * Forwards {@link Assert#assertArrayEquals(Object[], Object[])}, and wraps it in a try/catch so that we can properly release the semaphore when a test
     * fails (so it doesn't lock up).
     */
    public void assertArrayEquals(Object[] expected, Object[] actual)
    {
        try
        {
            Assert.assertArrayEquals(expected, actual);
        }
        catch (Error e)
        {
            handleException(e);
        }
    }

    /**
     * Forwards {@link Assert#assertArrayEquals(String, Object[], Object[])}, and wraps it in a try/catch so that we can properly release the semaphore when a test
     * fails (so it doesn't lock up).
     */
    public void assertArrayEquals(String msg, Object[] expected, Object[] actual)
    {
        try
        {
            Assert.assertArrayEquals(msg, expected, actual);
        }
        catch (Error e)
        {
            handleException(e);
        }
    }

    /**
     * Forwards {@link Assert#assertThat(Object, Matcher)}, and wraps it in a try/catch so that we can properly release the semaphore when a test
     * fails (so it doesn't lock up).
     */
    public <T> void assertThat(T actual, Matcher<? super T> matcher)
    {
        try
        {
            Assert.assertThat(actual, matcher);
        }
        catch (Error e)
        {
            handleException(e);
        }
    }

    /**
     * Forwards {@link Assert#assertThat(String, Object, Matcher)}, and wraps it in a try/catch so that we can properly release the semaphore when a test
     * fails (so it doesn't lock up).
     */
    public <T> void assertThat(String failMessage, T actual, Matcher<? super T> matcher)
    {
        try
        {
            Assert.assertThat(failMessage, actual, matcher);
        }
        catch (Error e)
        {
            handleException(e);
        }
    }


    /**
     * You should never have to override this method. It is only exposed so we can test the framework.
     */
    void handleException(Error e)
    {
        exception = e;
        if (useSemaphore)
            release();
        else
            throw exception;
    }

    private StackTraceElement getSoonestTrace()
    {
        StackTraceElement[] trace = new Exception().getStackTrace();
        return getSoonestTrace(trace);
    }

    private StackTraceElement getSoonestTrace(StackTraceElement[] trace)
    {
        for (StackTraceElement stackTraceElement : trace) {
            if (stackTraceElement.getClassName().equals(className)) {
                return stackTraceElement;
            }
        }
        return trace[getTraceIndex()];
    }

    /**
     * Override this method if you find that the test name isn't correct when printing in the console. This class simply tries to deduce
     * the test name via the method that's called. The default index is 2, this shouldn't require too much, if any change.
     */
    protected int getTraceIndex()
    {
        return 2;
    }


}