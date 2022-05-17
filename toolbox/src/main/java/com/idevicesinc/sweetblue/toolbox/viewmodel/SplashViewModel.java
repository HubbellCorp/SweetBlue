package com.idevicesinc.sweetblue.toolbox.viewmodel;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.ViewModel;
import com.idevicesinc.sweetblue.toolbox.util.BleHelper;
import com.idevicesinc.sweetblue.toolbox.util.UuidUtil;


public class SplashViewModel extends ViewModel
{

    public void init(Context context)
    {
        UuidUtil.makeStrings(context.getApplicationContext());
        BleHelper.get().init(context);
        Log.e("SplashViewModel", "BleHelper init called.");
    }

}
