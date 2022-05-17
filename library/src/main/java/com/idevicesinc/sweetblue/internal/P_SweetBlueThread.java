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

package com.idevicesinc.sweetblue.internal;


final class P_SweetBlueThread extends ThreadHandler
{

    P_SweetBlueThread()
    {
        this("SweetBlue Update Thread");
    }

    P_SweetBlueThread(String threadName)
    {
        super();
        final Thread t = new Thread(new HandlerRunner(), threadName);
        t.start();
        init(t);
    }


    private final class HandlerRunner implements Runnable
    {
        @Override
        public final void run()
        {
            while (m_running.get())
            {
                loop();

                // Sleep for a short period, so we don't hog the cpu
                try
                {
                    Thread.sleep(1);
                } catch (InterruptedException e) {}
            }
        }
    }

}
