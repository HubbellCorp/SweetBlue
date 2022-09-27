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


import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import androidx.test.rule.ActivityTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import android.view.WindowManager;
import com.idevicesinc.sweetblue.framework.AbstractTestClass;
import com.idevicesinc.sweetblue.internal.P_Bridge_BleManager;
import com.idevicesinc.sweetblue.utils.BleSetupHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import java.util.concurrent.atomic.AtomicBoolean;


@RunWith(AndroidJUnit4.class)
public abstract class BaseTester extends AbstractTestClass
{

    private final static int SETUP_HELPER_TIMEOUT = 30000;

    Activity activity;
    BleManager mgr;
    BleManagerConfig m_config;
    private PowerManager.WakeLock m_screenLock;
    private AtomicBoolean setupHelperLock = new AtomicBoolean(true);

    @Rule
    public ActivityTestRule<Activity> mRule = new ActivityTestRule<>(Activity.class);

    // Override this in a subclass to change the initial config
    BleManagerConfig getInitialConfig()
    {
        return null;
    }

    @Before
    public void setup()
    {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        activity = mRule.launchActivity(intent);
        instantiateManagerInstance();
        if (useSetupHelper())
        {
            long m_setupHelperStart = System.currentTimeMillis();

            // Start loop to check on helper dialogs
            P_Bridge_BleManager.postUpdateDelayed(mgr.getIBleManager(), this::setupHelperDialogLoop, 350);

            // Make sure the screen is on
            activity.runOnUiThread(this::makeSureScreenIsOn);

            // Start the helper process
            P_Bridge_BleManager.postMainDelayed(mgr.getIBleManager(), this::startHelper, 250);

            // Wait on this thread for the helper to complete
            while (setupHelperLock.get())
            {
                if (System.currentTimeMillis() - m_setupHelperStart > SETUP_HELPER_TIMEOUT)
                    throw new RuntimeException("Timeout while running Setup Helper!");
                try
                {
                    Thread.sleep(25);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
        additionalSetup();
    }

    /**
     * Only use this method when you have called {@link BleManager#shutdown()} to create a new instance. Only in the rarest of cases should you
     * be using this method (for instance, when testing PendingIntentScans because they run even when SweetBlue itself isn't instantiated)
     */
    protected void instantiateManagerInstance()
    {
        m_config = getConfig();
        mgr = BleManager.get(activity, m_config);
    }

    @After
    public void shutdown()
    {
        if (m_screenLock != null && m_screenLock.isHeld())
            m_screenLock.release();
        if (mgr != null)
        {
            mgr.shutdown();
            mgr = null;
        }
    }

    public void handleBluetoothEnabling(Runnable testAction) throws Exception
    {
        BleSetupHelper.runEnabler(mgr, activity, result ->
        {
            if (result.getSuccessful())
            {
                if (testAction != null)
                    testAction.run();
            }
        });
        Thread.sleep(1000);
        UIUtil.handleBluetoothEnablerDialogs(activity);
    }

    /**
     * Method to override if you need to perform some additional logic when the test is "setting up". The method gets called within {@link #setup()}, as the last
     * call in the method.
     */
    public void additionalSetup()
    {
    }

    /**
     * This tells the base test class to use the BleSetupHelper (it will be handled automatically, so that by the time your test actually runs, this will
     * be taken care of for you). The only time this should ever return <code>false</code> is when testing the Helper itself.
     */
    public boolean useSetupHelper()
    {
        return true;
    }

    public BleScanApi getScanApi()
    {
        return P_Bridge_BleManager.getScanApi(mgr.getIBleManager());
    }

    public BleScanPower getScanPower()
    {
        return P_Bridge_BleManager.getScanPower(mgr.getIBleManager());
    }

    private BleManagerConfig getConfig()
    {
        BleManagerConfig config = getInitialConfig();
        if (config == null)
        {
            config = new BleManagerConfig();
            config.loggingOptions = LogOptions.ON;
            config.unitTest = false;
        }
        return config;
    }

    private void setupHelperDialogLoop()
    {
        UIUtil.handleBluetoothEnablerDialogs(activity);
        if (setupHelperLock.get())
            P_Bridge_BleManager.postUpdateDelayed(mgr.getIBleManager(), this::setupHelperDialogLoop, 20);
    }

    private void makeSureScreenIsOn()
    {
        KeyguardManager km = (KeyguardManager) activity.getSystemService(Context.KEYGUARD_SERVICE);
        if (km.inKeyguardRestrictedInputMode())
        {
            m_screenLock = ((PowerManager)activity.getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "tag");
            m_screenLock.acquire();
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        }
    }

    private void startHelper()
    {
        BleSetupHelper.runEnabler(mgr, activity, result ->
        {
            if (result.getSuccessful())
                setupHelperLock.set(false);
        });
    }


}
