/*

  Copyright 2022 Hubbell Incorporated

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.

  You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.

 */

package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.internal.P_Bridge_BleManager;
import com.idevicesinc.sweetblue.internal.TestTask;
import com.idevicesinc.sweetblue.internal.TestTaskA;
import com.idevicesinc.sweetblue.internal.TestTaskB;
import com.idevicesinc.sweetblue.utils.Interval;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;
import java.util.Random;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class QueueTest extends BaseBleUnitTest
{
    private Integer mRemainingTasks = null;
    private long mStartTimestamp;
    private Long mLastTimestamp;
    private Long mRequiredDelayTime = null;

    private void onExecute(TestTask tt)
    {
        long timeNow = System.currentTimeMillis();
        if (mRemainingTasks == null)
        {
            // Fail
        }
        else
        {
            mRemainingTasks--;

            System.out.println("Executing task of type " + tt.getClass().getSimpleName() + " with priority " + tt.getPriority() + " and metadata " + tt.getMetadata() + ".  There are " + mRemainingTasks + " remaining tasks");

            // Make sure the task wasn't executed too soon
            if (mLastTimestamp != null && mRequiredDelayTime != null)
            {
                long dt = timeNow - mLastTimestamp;
                System.out.println("Task took " + dt + " ms to execute, and delay time was " + mRequiredDelayTime);
                assertTrue(dt >= mRequiredDelayTime);
            }

            if (mRemainingTasks == 0)
            {
                long dt = timeNow - mStartTimestamp;
                System.out.println("All tasks complete after " + dt + "ms");
                succeed();
            }
        }

        mLastTimestamp = timeNow;
    }

    private int populateQueue(int numTasks, double distributionRatio, int startingOrdinal)
    {
        final Random r = new Random();

        int countA = 0;
        int countB = 0;

        for (int i = 0; i < numTasks; ++i)
        {
            TestTask tt = null;

            if (r.nextDouble() <= distributionRatio)
            {
                tt = new TestTaskA(m_manager.getIBleManager(), startingOrdinal + i, QueueTest.this::onExecute);
                ++countA;
            }
            else
            {
                tt = new TestTaskB(m_manager.getIBleManager(), startingOrdinal + i, QueueTest.this::onExecute);
                ++countB;
            }

            // Assign a random priority to force the inserts to walk the list
            tt.setPriority(P_Bridge_BleManager.randomPriority(r));

            P_Bridge_BleManager.addTask(m_manager.getIBleManager(), tt);
        }

        return countA;
    }

    @Test(timeout = 30000)
    public void queueTest() throws Exception
    {
        startSynchronousTest();
        m_config.loggingOptions = LogOptions.ON;
        m_manager.setConfig(m_config);

        mRequiredDelayTime = null;

        // Suspend the queue so we can cram it full of stuff w/o it doing anything just yet
        P_Bridge_BleManager.suspendQueue(m_manager.getIBleManager());

        final int kTaskCount = 1000;

        mStartTimestamp = System.currentTimeMillis();
        System.out.println("Starting queue operations at " + mStartTimestamp);

        int countA = populateQueue(kTaskCount, .5, 0);
        int countB = kTaskCount - countA;

        long dt = System.currentTimeMillis() - mStartTimestamp;
        mStartTimestamp = System.currentTimeMillis();
        System.out.println("Queue adds complete after " + dt + " ms.  Queue size is now " + P_Bridge_BleManager.getQueueSize(m_manager.getIBleManager()) + " with " + countA + " task A and " + countB + " task B.  Starting queue clear at " + mStartTimestamp);

        P_Bridge_BleManager.clearQueueOf(m_manager.getIBleManager(), TestTaskA.class);

        dt = System.currentTimeMillis() - mStartTimestamp;
        mStartTimestamp = System.currentTimeMillis();
        System.out.println("Queue operations complete after " + dt + " ms.  Queue size is now " + P_Bridge_BleManager.getQueueSize(m_manager.getIBleManager()) + ".  Starting task processing at " + mStartTimestamp);

        // Add some more tasks, favoring A more
        countA = populateQueue(kTaskCount, .75, kTaskCount);
        countB += kTaskCount - countA;

        // Now, verify that there are the correct number of each task
        int observedCountA = 0;
        int observedCountB = 0;
        List<TestTask> l = P_Bridge_BleManager.getFromQueue(m_manager.getIBleManager(), TestTask.class);

        Integer prevPriority = null;
        int prevOrdinal = 0;

        for (TestTask t : l)
        {
            if (t instanceof TestTaskA)
                ++observedCountA;
            if (t instanceof TestTaskB)
                ++observedCountB;

            int priority = P_Bridge_BleManager.getPriority(t);
            int currentOrdinal = (Integer)t.getMetadata();

            // Make sure the priorities aren't broken
            if (prevPriority != null)
            {
                if (prevPriority < priority)
                    assertTrue(false);

                if (prevPriority == priority)
                {
                    boolean expr = currentOrdinal > prevOrdinal;
                    if (!expr)
                    {
                        System.out.println("oops");
                    }
                    assertTrue(expr);
                }
            }

            prevOrdinal = currentOrdinal;
            prevPriority = priority;
        }

        assertTrue(observedCountA == countA);
        assertTrue(observedCountB == countB);

        succeed();
    }

    @Test(timeout = 30000)
    public void queueProcessTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;
        m_manager.setConfig(m_config);

        mRequiredDelayTime = null;

        // Suspend the queue so we can cram it full of stuff w/o it doing anything just yet
        P_Bridge_BleManager.suspendQueue(m_manager.getIBleManager());

        final int kTaskCount = 1000;
        populateQueue(kTaskCount, .35, 0);

        mRemainingTasks = P_Bridge_BleManager.getQueueSize(m_manager.getIBleManager());

        P_Bridge_BleManager.unsuspendQueue(m_manager.getIBleManager());

        startAsyncTest();
    }

    private void onDelayExecuted(TestTask tt)
    {
        /*if (mRemainingTasks == null)
        {
            // Fail
        }
        else
        {
            mRemainingTasks--;

            System.out.println("Executing task of type " + tt.getClass().getSimpleName() + " with priority " + tt.getPriority() + " and metadata " + tt.getMetadata() + ".  There are " + mRemainingTasks + " remaining tasks");

            if (mRemainingTasks == 0)
            {
                long dt = System.currentTimeMillis() - mStartTimestamp;
                System.out.println("All tasks complete after " + dt + "ms");
                succeed();
            }
        }*/
    }

    @Test(timeout = 30000)
    public void queueDelayTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;
        m_config.delayBetweenTasks = Interval.ONE_SEC;
        m_manager.setConfig(m_config);

        mRequiredDelayTime = 500L;

        P_Bridge_BleManager.suspendQueue(m_manager.getIBleManager());
        mRemainingTasks = 10;
        populateQueue(10, 1, 0);

        P_Bridge_BleManager.unsuspendQueue(m_manager.getIBleManager());

        startAsyncTest();

        //tt = new TestTaskB(m_manager.getIBleManager(), startingOrdinal + i, this::onExecute);
    }

}
