package com.idevicesinc.sweetblue.compat;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.TIRAMISU)
public final class T_Util
{
    private T_Util() {}

    public static Intent registerReceiver(Context context, BroadcastReceiver broadcastReceiver, IntentFilter intentFilter)
    {
        return O_Util.registerReceiver(context, broadcastReceiver, intentFilter, Context.RECEIVER_EXPORTED);
    }
}
