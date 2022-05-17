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


import com.idevicesinc.sweetblue.internal.ThreadHandler;
import com.idevicesinc.sweetblue.utils.UpdateThreadType;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.concurrent.atomic.AtomicBoolean;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class ThreadHandlerTest extends BaseBleUnitTest
{

    @Test(timeout = 15000)
    public void customThreadHandlerTest() throws Exception
    {
        final AtomicBoolean running = new AtomicBoolean(true);
        final ThreadHandler myHandler = new ThreadHandler();

        final Thread myThread = new Thread(() -> {
            while (running.get())
            {
                myHandler.loop();
            }
        });

        myThread.start();

        m_config.updateHandler = myHandler;
        m_config.updateThreadType = UpdateThreadType.USER_CUSTOM;
        m_config.loggingOptions = LogOptions.ON;

        m_manager.setConfig(m_config);

        m_manager.setListener_State(e -> {
            if (e.didEnter(BleManagerState.OFF))
            {
                running.set(false);
                ThreadHandlerTest.this.succeed();
            }
        });

        m_manager.turnOff();

        startAsyncTest();
    }




}
