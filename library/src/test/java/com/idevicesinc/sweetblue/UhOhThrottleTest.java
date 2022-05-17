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


import com.idevicesinc.sweetblue.utils.Interval;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class UhOhThrottleTest extends BaseBleUnitTest
{

    @Test(timeout = 20000)
    public void uhOhThrottleTest() throws Exception
    {
        m_config.updateLoopCallback = new UpdateCallback()
        {
            private double time;

            @Override
            public void onUpdate(double timestep_seconds)
            {
                time += timestep_seconds;
                if (time > 5)
                {
                    succeed();
                }
            }
        };

        m_manager.setConfig(m_config);
        m_manager.setListener_UhOh(new UhOhListener()
        {
            private long m_lastEvent;

            @Override
            public void onEvent(UhOhEvent e)
            {
                long now = System.currentTimeMillis();
                if (m_lastEvent == 0)
                {
                    m_lastEvent = now;
                }
                else if (now - m_lastEvent < 2500)
                {
                    assertFalse("Didn't honor throttle time! Time diff: " + (now - m_lastEvent) + "ms", true);
                }
            }
        });
        m_manager.getIBleManager().uhOh(UhOhListener.UhOh.CANNOT_ENABLE_BLUETOOTH);
        m_manager.getIBleManager().uhOh(UhOhListener.UhOh.CANNOT_ENABLE_BLUETOOTH);
        startAsyncTest();
    }

    @Test(timeout = 30000)
    public void uhOhThrottleShutdownTest() throws Exception
    {
        final UhOhCallback callback = new UhOhCallback();
        m_config.updateLoopCallback = callback;
        m_config.uhOhCallbackThrottle = Interval.secs(20);

        m_manager.setConfig(m_config);
        m_manager.setListener_UhOh(new UhOhListener()
        {
            private long m_lastEvent;

            @Override
            public void onEvent(UhOhEvent e)
            {
                long now = System.currentTimeMillis();
                if (m_lastEvent == 0)
                {
                    m_lastEvent = now;
                }
                else if (now - m_lastEvent < 2500)
                {
                    assertFalse("Didn't honor throttle time! Time diff: " + (now - m_lastEvent) + "ms", true);
                }
            }
        });
        m_manager.getIBleManager().uhOh(UhOhListener.UhOh.CANNOT_ENABLE_BLUETOOTH);
        m_manager.getIBleManager().uhOh(UhOhListener.UhOh.CANNOT_ENABLE_BLUETOOTH);
        startAsyncTest();
    }

    private final class UhOhCallback implements UpdateCallback
    {

        private double time;
        private boolean secondTime;


        @Override
        public void onUpdate(double timestep_seconds)
        {
            time += timestep_seconds;
            if (time > 5 && time < 10 && !secondTime)
            {
                secondTime = true;
                m_manager.shutdown();

                new Thread(() -> {
                    System.out.println("Restarting BleManager...");
                    m_manager = initManager(m_config);
                    m_manager.getIBleManager().uhOh(UhOhListener.UhOh.CANNOT_ENABLE_BLUETOOTH);
                    m_manager.getIBleManager().uhOh(UhOhListener.UhOh.CANNOT_ENABLE_BLUETOOTH);
                    m_manager.getIBleManager().uhOh(UhOhListener.UhOh.CANNOT_ENABLE_BLUETOOTH);
                    m_manager.getIBleManager().uhOh(UhOhListener.UhOh.CANNOT_ENABLE_BLUETOOTH);
                }).start();

            }
            else if (time >= 10d)
            {
                System.out.println("Time steps at release: " + time);
                succeed();
            }
        }
    }


    @Override
    public BleManagerConfig getConfig()
    {
        BleManagerConfig config = super.getConfig();
        m_config.loggingOptions = LogOptions.ON;
        config.uhOhCallbackThrottle = Interval.secs(2.5);
        return config;
    }
}
