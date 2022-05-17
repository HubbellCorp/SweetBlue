package com.idevicesinc.sweetblue.rx.schedulers;


import com.idevicesinc.sweetblue.internal.P_SweetHandler;

import java.util.concurrent.TimeUnit;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.plugins.RxJavaPlugins;


public class SweetBlueScheduler extends Scheduler
{

    private final P_SweetHandler m_handler;


    SweetBlueScheduler(P_SweetHandler handler)
    {
        m_handler = handler;
    }


    @Override
    public Disposable scheduleDirect(Runnable run, long delay, TimeUnit unit)
    {
        return super.scheduleDirect(run, delay, unit);
    }

    @Override
    public Worker createWorker()
    {
        return new SweetWorker(m_handler);
    }

    @Override public void shutdown()
    {
        m_handler.quit();
    }

    private static final class SweetWorker extends Worker
    {

        private final P_SweetHandler m_handler;

        private volatile boolean m_disposed;


        SweetWorker(P_SweetHandler handler)
        {
            m_handler = handler;
        }


        @Override
        public Disposable schedule(Runnable run, long delay, TimeUnit unit)
        {
            if (run == null) throw new NullPointerException("run == null");
            if (unit == null) throw new NullPointerException("unit == null");

            if (m_disposed)
                return Disposables.disposed();

            run = RxJavaPlugins.onSchedule(run);

            SweetScheduledRunnable scheduled = new SweetScheduledRunnable(m_handler, run);

            // this is used as token for batch disposal of this worker's runnables.
            m_handler.postDelayed(scheduled, Math.max(0L, unit.toMillis(delay)));

            // Re-check disposed state for removing in case we were racing a call to dispose().
            if (m_disposed)
            {
                m_handler.removeCallbacks(scheduled);
                return Disposables.disposed();
            }

            return scheduled;
        }

        @Override
        public void dispose()
        {
            m_disposed = true;
            m_handler.removeCallbacks(this);
        }

        @Override
        public boolean isDisposed()
        {
            return m_disposed;
        }
    }


    private static final class SweetScheduledRunnable implements Runnable, Disposable
    {

        private final P_SweetHandler m_handler;
        private final Runnable m_runner;

        private volatile boolean m_disposed;


        SweetScheduledRunnable(P_SweetHandler handler, Runnable runner)
        {
            m_handler = handler;
            m_runner = runner;
        }


        @Override
        public void run()
        {
            try
            {
                m_runner.run();
            }
            catch (Throwable t)
            {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void dispose()
        {
            m_disposed = true;
            m_handler.removeCallbacks(this);
        }

        @Override
        public boolean isDisposed()
        {
            return m_disposed;
        }

    }
}
