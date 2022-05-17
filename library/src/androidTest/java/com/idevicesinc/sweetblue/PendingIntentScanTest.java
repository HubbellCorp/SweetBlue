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

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.test.filters.FlakyTest;

import com.idevicesinc.sweetblue.utils.Utils;
import org.junit.Test;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;


public class PendingIntentScanTest extends BaseTester
{


    private static ReceiverCallback mCallback = null;


    // This test is kind of flaky. It won't work if there are no bluetooth devices around. It may not work if there are a lot
    // of bluetooth devices around. The MAGIC_TIME_MS field may have to be tweaked in certain cases.
    @FlakyTest
    @Test(timeout = 30000L)
    public void pendingIntentScanAndStopTest() throws Exception
    {
        boolean isOreo = Utils.isOreo();
        m_config.loggingOptions = LogOptions.ON;

        mgr.setConfig(m_config);

        final Timer timer = new Timer();
        StopScanTimer timerTask = new StopScanTimer();

        // Create our pending intent
        final PendingIntent pIntent = PendingIntent.getBroadcast(activity, /* \m/ */666, new Intent(activity, TestReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);

        mgr.reset(e1 ->
        {
            // Sometimes it can take a little bit for the manager to enter the ON state after the ResetListener gets dispatched
            while (!mgr.is(BleManagerState.ON))
            {
                try
                {
                    Thread.sleep(100);
                }
                catch (Exception e)
                {
                }
            }
            ScanOptions options = new ScanOptions();
            options.withPendingIntent(pIntent);

            final AtomicBoolean stoppedScan = new AtomicBoolean(false);

            mCallback = (context, intent) ->
            {
                // Update the last time device seen field
                timerTask.mLastTimeDeviceSeen = System.currentTimeMillis();

                if (stoppedScan.compareAndSet(false, true))
                {
                    if (mgr == null)
                        instantiateManagerInstance();

                    List<BleDevice> devicesFound = mgr.getDevices(intent);
                    // Make sure list isn't null (it should never be, if so, it's a SweetBlue bug)
                    assertNotNull("Devices found list null!", devicesFound);
                    // Make sure list isn't empty (if it is, then that is most likely an Android bug)
                    assertFalse("Device found list is empty!", devicesFound.isEmpty());


                    mgr.stopScan(pIntent);
                }

            };

            mgr.setListener_State(e ->
            {
                if (e.didEnter(BleManagerState.SCANNING))
                {
                    mgr.shutdown();
                    mgr = null;
                    activity.finish();
                }
            });

            boolean scanStarted = mgr.startScan(options);

            if (!scanStarted)
            {
                if (!isOreo)
                    succeed();
                else
                    assertTrue("Oreo compatible device failed to start the PendingIntent scan", scanStarted);
            }
            else
            {
                if (isOreo)
                {
                    timer.scheduleAtFixedRate(timerTask, 100, 500);
                }
                if (!isOreo)
                    assertFalse("Non-oreo device succeeded a PendingIntent scan", scanStarted);
            }
        });
        startAsyncTest();
    }

    private class StopScanTimer extends TimerTask
    {

        // Magic time to determine the scan has been stopped. This is a guess, and may need some adjustment if the test keeps failing
        final static int MAGIC_TIME_MS = 10000;

        long mLastTimeDeviceSeen;

        public StopScanTimer()
        {
            mLastTimeDeviceSeen = System.currentTimeMillis();
        }

        @Override
        public void run()
        {
            if (mLastTimeDeviceSeen + MAGIC_TIME_MS < System.currentTimeMillis())
            {
                // We haven't seen any devices within the magic time, so we assume success here
                cancel();
                succeed();
            }
        }
    }

    @Override
    public void shutdown()
    {
        super.shutdown();
        // Make sure the callback instance is cleared in case of errors
        mCallback = null;
    }

    // Interface used to deliver results from the broadcast receiver
    private interface ReceiverCallback
    {
        void gotResults(Context context, Intent intent);
    }


    public static class TestReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (mCallback != null)
                mCallback.gotResults(context, intent);
        }
    }
}
