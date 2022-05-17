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

package com.idevicesinc.sweetblue.internal;


import com.idevicesinc.sweetblue.annotations.Advanced;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Class used by the library to post {@link Runnable}s. You shouldn't ever need to subclass this, as it's all implemented for you.
 * However, if you want SweetBlue to run it's update logic on an already created {@link Thread} in your app (rather than creating a new Thread),
 * then you will need to use this class. You <b>must</b> make sure to call {@link #loop()} within the existing Thread's runnable, otherwise
 * the class won't execute it's commands.
 */
@Advanced
public class ThreadHandler implements P_SweetHandler
{

    private final LinkedBlockingQueue<SweetRunnable> m_runnables;
    private /*final-ish*/ Thread m_thread = null;
    protected final AtomicBoolean m_running;


    /**
     * Create a new instance of ThreadHandler. If you are using this class to run SweetBlue's update logic in a thread you already
     * have in your app, <b>you must remember to call {@link #loop()} in it's Runnable</b>. This is setup as an update loop, so if your
     * Thread's runnable doesn't loop, this class won't help much.
     */
    public ThreadHandler()
    {
        m_runnables = new LinkedBlockingQueue<>();
        m_running = new AtomicBoolean(true);
    }



    /**
     * Only used by {@link P_SweetBlueThread}, as we can't instantiate the Thread's runnable until after the constructor calls it's super() method.
     */
    void init(Thread thread)
    {
        m_thread = thread;
    }



    /**
     * If you are running SweetBlue's update logic in your own thread, you must remember to call this method inside of the Runnable
     * for the your Thread.
     */
    @Advanced
    public final void loop()
    {
        final Thread current = Thread.currentThread();
        if (m_thread == null || m_thread != current)
            m_thread = current;
        processRunnables();
    }


    /**
     * Post a {@link Runnable} to be executed by the {@link Thread} backing this handler.
     */
    @Override
    public final void post(Runnable action)
    {
        m_runnables.add(new SweetRunnable(action, System.currentTimeMillis()));
    }

    /**
     * Post a {@link Runnable} to be executed by the {@link Thread} backing this handler, delayed by the amount given.
     */
    @Override
    public final void postDelayed(Runnable action, long delay)
    {
        postDelayed(action, delay, null);
    }

    /**
     * Post a {@link Runnable} to be executed by the {@link Thread} backing this handler, delayed by the amount given, with the given tag to identify this
     * action.
     *
     * @see #removeCallbacks(Object)
     */
    @Override
    public void postDelayed(Runnable action, long delay, Object tag)
    {
        m_runnables.add(new SweetRunnable(action, System.currentTimeMillis(), delay, tag));
    }

    /**
     * Remove a {@link Runnable} from the handler. If the runnable is currently executing, it will not be canceled.
     */
    @Override
    public final void removeCallbacks(Runnable action)
    {
        Iterator<SweetRunnable> it = m_runnables.iterator();
        while (it.hasNext())
        {
            SweetRunnable run = it.next();
            if (run.m_runnable == action)
            {
                run.cancel();
                it.remove();
            }
        }
    }

    @Override
    public void removeCallbacks(Object tag)
    {
        Iterator<SweetRunnable> it = m_runnables.iterator();
        while (it.hasNext())
        {
            SweetRunnable run = it.next();
            if (run.m_tag != null && run.m_tag.equals(tag))
            {
                run.cancel();
                it.remove();
            }
        }
    }

    @Override
    public void quit()
    {
        m_running.set(false);
        m_thread.interrupt();
        if (Thread.currentThread() != m_thread)
        {
            try
            {
                m_thread.join();
            } catch (Exception e) {}
        }
    }

    @Override
    public Thread getThread()
    {
        return m_thread;
    }


    private final static class SweetRunnable
    {
        private final Runnable m_runnable;
        private final long m_postedTime;
        private final Long m_delay;
        private final Object m_tag;
        private boolean m_canceled = false;


        SweetRunnable(Runnable action, long postedTime)
        {
            this(action, postedTime, null, null);
        }

        SweetRunnable(Runnable action, long postedTime, Long delay, Object tag)
        {
            m_runnable = action;
            m_postedTime = postedTime;
            m_delay = delay;
            m_tag = tag;
        }


        public final void run()
        {
            if (m_canceled)
                return;
            m_runnable.run();
        }

        public final void cancel()
        {
            m_canceled = true;
        }

        public final boolean getCanceled()
        {
            return m_canceled;
        }

        public final boolean ready(long curTime)
        {
            if (m_canceled || m_delay == null)
                return true;
            return (curTime - m_postedTime) > m_delay;
        }

        public final boolean ready()
        {
            return ready(System.currentTimeMillis());
        }
    }

    private void processRunnables()
    {
        if (m_running.get() && !m_runnables.isEmpty())
        {
            if (m_thread.isInterrupted())
                return;
            final Iterator<SweetRunnable> it = m_runnables.iterator();
            while (it.hasNext())
            {
                if (!m_running.get())
                    break;

                if (m_thread.isInterrupted())
                    return;

                SweetRunnable run = it.next();

                // Skip any tasks that aren't ready
                if (!run.ready())
                    continue;

                // Run the task then remove it.  If the task was already canceled, it will not perform it's run operation
                run.run();
                it.remove();
            }
        }
    }

}
