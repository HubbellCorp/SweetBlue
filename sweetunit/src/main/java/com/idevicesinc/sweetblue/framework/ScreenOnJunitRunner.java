package com.idevicesinc.sweetblue.framework;

import static android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD;
import static android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
import static android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;
import android.os.Bundle;
import androidx.test.runner.AndroidJUnitRunner;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;


public class ScreenOnJunitRunner extends AndroidJUnitRunner
{

    @Override
    public void onCreate(Bundle arguments)
    {
        super.onCreate(arguments);

        // Add flags to the window to force the screen to be on, and unlocked, otherwise UI tests
        // will fail
        ActivityLifecycleMonitorRegistry.getInstance().addLifecycleCallback((activity, stage) -> {
            if (stage == Stage.PRE_ON_CREATE)
                activity.getWindow().addFlags(FLAG_DISMISS_KEYGUARD | FLAG_TURN_SCREEN_ON | FLAG_KEEP_SCREEN_ON);
        });
    }
}