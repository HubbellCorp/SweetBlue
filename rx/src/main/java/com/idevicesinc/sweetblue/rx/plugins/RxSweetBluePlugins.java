package com.idevicesinc.sweetblue.rx.plugins;


import com.idevicesinc.sweetblue.rx.schedulers.SweetBlueSchedulers;
import java.util.concurrent.Callable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Function;


public final class RxSweetBluePlugins
{

    private static volatile Function<Callable<Scheduler>, Scheduler> onInitSweetBlueThreadHandler;
    private static volatile Function<Scheduler, Scheduler> onSweetBlueThreadHandler;


    public static void setInitSweetBlueThreadHandler(Function<Callable<Scheduler>, Scheduler> handler)
    {
        onInitSweetBlueThreadHandler = handler;
    }

    public static Scheduler initSweetBlueThreadScheduler(Callable<Scheduler> scheduler)
    {
        if (scheduler == null) throw new NullPointerException("scheduler == null");

        Function<Callable<Scheduler>, Scheduler> f = onInitSweetBlueThreadHandler;

        if (f == null)
            return callRequireNonNull(scheduler);

        return applyRequireNonNull(f, scheduler);
    }

    public static void setSweetBlueThreadHandler(Function<Scheduler, Scheduler> handler)
    {
        onSweetBlueThreadHandler = handler;
    }

    public static Scheduler onSweetBlueThreadScheduler(Scheduler scheduler)
    {
        if (scheduler == null) throw new NullPointerException("scheduler == null");

        Function<Scheduler, Scheduler> f = onSweetBlueThreadHandler;

        if (f == null)
            return scheduler;

        return apply(f, scheduler);
    }

    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    public static Function<Callable<Scheduler>, Scheduler> getInitSweetBlueThreadHandler()
    {
        return onInitSweetBlueThreadHandler;
    }


    /**
     * Returns the current hook function.
     * @return the hook function, may be null
     */
    public static Function<Scheduler, Scheduler> getOnSweetBlueThreadSchedulerHandler()
    {
        return onSweetBlueThreadHandler;
    }

    /**
     * Removes all handlers and resets the default behavior.
     */
    public static void reset() {
        setInitSweetBlueThreadHandler(null);
        setSweetBlueThreadHandler(null);
        SweetBlueSchedulers.reset();
    }


    static Scheduler callRequireNonNull(Callable<Scheduler> s) {
        try {
            Scheduler scheduler = s.call();
            if (scheduler == null) {
                throw new NullPointerException("Scheduler Callable returned null");
            }
            return scheduler;
        } catch (Throwable ex) {
            throw Exceptions.propagate(ex);
        }
    }

    static Scheduler applyRequireNonNull(Function<Callable<Scheduler>, Scheduler> f, Callable<Scheduler> s) {
        Scheduler scheduler = apply(f,s);
        if (scheduler == null) {
            throw new NullPointerException("Scheduler Callable returned null");
        }
        return scheduler;
    }

    static <T, R> R apply(Function<T, R> f, T t) {
        try {
            return f.apply(t);
        } catch (Throwable ex) {
            throw Exceptions.propagate(ex);
        }
    }

    private RxSweetBluePlugins() {
        throw new AssertionError("No instances.");
    }

}
