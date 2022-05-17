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


import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.idevicesinc.sweetblue.P_Bridge_User;


final class P_SweetUIHandler implements P_SweetHandler
{

    private final Handler m_handler;
    private P_SweetBlueThread m_thread;


    public P_SweetUIHandler(IBleManager mgr)
    {
        boolean unitTest = true;
        if (P_Bridge_User.isUnitTest(mgr.getConfigClone()) == null)
        {
            try
            {
                Class.forName("org.junit.Assert");
            } catch (ClassNotFoundException e)
            {
                unitTest = false;
            }
        }
        else
            unitTest = P_Bridge_User.isUnitTest(mgr.getConfigClone());

        if (unitTest)
        {
            m_thread = new P_SweetBlueThread("Mocked UI Thread");
            m_handler = null;
        }
        else
            m_handler = new Handler(Looper.getMainLooper());
    }


    @Override public void post(Runnable action)
    {
        if (m_handler != null)
            m_handler.post(action);
        else
            m_thread.post(action);
    }

    @Override public void postDelayed(Runnable action, long delay)
    {
        if (m_handler != null)
            m_handler.postDelayed(action, delay);
        else
            m_thread.postDelayed(action, delay);
    }

    @Override
    public void postDelayed(Runnable action, long delay, Object tag)
    {
        if (m_handler != null)
        {
            Message msg = Message.obtain(m_handler, action);
            msg.obj = tag;

            m_handler.sendMessageDelayed(msg, Math.max(0L, delay));
        }
        else
            m_thread.postDelayed(action, delay, tag);
    }

    @Override public void removeCallbacks(Runnable action)
    {
        if (m_handler != null)
            m_handler.removeCallbacks(action);
        else
            m_thread.removeCallbacks(action);
    }

    @Override
    public void removeCallbacks(Object tag)
    {
        if (m_handler != null)
            m_handler.removeCallbacksAndMessages(tag);
        else
            m_thread.removeCallbacks(tag);
    }

    @Override public Thread getThread()
    {
        if (m_handler != null)
            return m_handler.getLooper().getThread();
        else
            return m_thread.getThread();
    }

    @Override public void quit()
    {
        if (m_thread != null)
            m_thread.quit();
    }
}
