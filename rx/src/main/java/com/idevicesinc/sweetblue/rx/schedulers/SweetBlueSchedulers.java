package com.idevicesinc.sweetblue.rx.schedulers;


import com.idevicesinc.sweetblue.P_Bridge;
import com.idevicesinc.sweetblue.rx.plugins.RxSweetBluePlugins;
import io.reactivex.Scheduler;

/**
 * SweetBlue specific schedulers.
 */
public final class SweetBlueSchedulers
{


    private static Scheduler SWEETBLUE_SCHEDULER_INSTANCE = null;

    private static Scheduler SWEETBLUE_THREAD = null;


    /** A {@link Scheduler} which executes actions on SweetBlue's update thread. */
    public static Scheduler sweetBlueThread()
    {
        // Make sure to re-initialize the holder and scheduler instances if they are null to avoid holding onto a stale
        // sweetApi handler.
        if (SWEETBLUE_SCHEDULER_INSTANCE == null)
            SWEETBLUE_SCHEDULER_INSTANCE = new SweetBlueScheduler(P_Bridge.getUpdateHandler());
        if (SWEETBLUE_THREAD == null)
            SWEETBLUE_THREAD = RxSweetBluePlugins.initSweetBlueThreadScheduler(() -> SWEETBLUE_SCHEDULER_INSTANCE);
        return RxSweetBluePlugins.onSweetBlueThreadScheduler(SWEETBLUE_THREAD);
    }

    /**
     * Reset the current handler/scheduler instances to null.
     */
    public static void reset()
    {
        if (SWEETBLUE_THREAD != null)
            SWEETBLUE_THREAD.shutdown();
        SWEETBLUE_THREAD = null;
        if (SWEETBLUE_SCHEDULER_INSTANCE != null)
            SWEETBLUE_SCHEDULER_INSTANCE.shutdown();
        SWEETBLUE_SCHEDULER_INSTANCE = null;
    }


    private SweetBlueSchedulers()
    {
        throw new AssertionError("No instances.");
    }

}
