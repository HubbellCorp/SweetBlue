package com.idevicesinc.sweetblue.toolbox.util;

import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.MutableLiveData;


public class MutablePostLiveData<T> extends MutableLiveData<T>
{

    private static Handler m_handler = new Handler(Looper.getMainLooper());


    @Override
    public void setValue(final T value)
    {
        if (Looper.myLooper() != Looper.getMainLooper())
        {
            m_handler.post(() -> MutablePostLiveData.super.setValue(value));
        }
        else
            super.setValue(value);
    }
}
