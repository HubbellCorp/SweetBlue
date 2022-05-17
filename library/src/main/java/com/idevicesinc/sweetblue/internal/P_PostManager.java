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

import com.idevicesinc.sweetblue.utils.UpdateThreadType;
import com.idevicesinc.sweetblue.utils.Utils;


final class P_PostManager
{

    private final P_SweetHandler m_uiHandler;
    private final P_SweetHandler m_updateHandler;
    private final IBleManager m_manager;


    P_PostManager(IBleManager mgr, P_SweetHandler uiHandler, P_SweetHandler updateHandler)
    {
        m_uiHandler = uiHandler;
        m_updateHandler = updateHandler;
        m_manager = mgr;
    }

    public final void postToMain(Runnable action)
    {
        if (Utils.isOnMainThread())
        {
            action.run();
        }
        else
        {
            m_uiHandler.post(action);
        }
    }

    public final void post(Runnable action)
    {
        if (m_manager.getConfigClone().updateThreadType == UpdateThreadType.MAIN)
        {
            if (Utils.isOnMainThread())
            {
                action.run();
            }
            else
            {
                m_uiHandler.post(action);
            }
        }
        else
        {
            if (isOnSweetBlueThread())
            {
                action.run();
            }
            else
            {
                m_updateHandler.post(action);
            }
        }
    }

    public final void postCallback(Runnable action)
    {
        if (m_manager.getConfigClone().postCallbacksToMainThread)
        {
            postToMain(action);
        }
        else
        {
            if (isOnSweetBlueThread())
            {
                action.run();
            }
            else
            {
                m_updateHandler.post(action);
            }
        }
    }

    public final void postToUpdateThread(Runnable action)
    {
        m_updateHandler.post(action);
    }

    public final void runOrPostToUpdateThread(Runnable action)
    {
        if (isOnSweetBlueThread())
        {
            action.run();
        }
        else
        {
            m_updateHandler.post(action);
        }
    }

    public final void forcePostToUpdate(Runnable action)
    {
        m_updateHandler.post(action);
    }

    public final void postToMainDelayed(Runnable action, long delay)
    {
        m_uiHandler.postDelayed(action, delay);
    }

    public final void postDelayed(Runnable action, long delay)
    {
        if (m_manager.getConfigClone().updateThreadType == UpdateThreadType.MAIN)
        {
            m_uiHandler.postDelayed(action, delay);
        }
        else
        {
            m_updateHandler.postDelayed(action, delay);
        }
    }

    public final void postCallbackDelayed(Runnable action, long delay)
    {
        if (m_manager.getConfigClone().postCallbacksToMainThread)
        {
            m_uiHandler.postDelayed(action, delay);
        }
        else
        {
            m_updateHandler.postDelayed(action, delay);
        }
    }

    public final void postToUpdateThreadDelayed(Runnable action, long delay)
    {
        m_updateHandler.postDelayed(action, delay);
    }

    public final void removeUICallbacks(Runnable uiRunnable)
    {
        m_uiHandler.removeCallbacks(uiRunnable);
    }

    public final void removeUpdateCallbacks(Runnable updateRunnable)
    {
        m_updateHandler.removeCallbacks(updateRunnable);
    }

    final Handler getSweetBlueThreadHandler() {
        if (m_updateHandler instanceof P_SweetBlueAndroidHandlerThread)
            return ((P_SweetBlueAndroidHandlerThread) m_updateHandler).getAndroidHandler();
        else
            return null;
    }

    final P_SweetHandler getUIHandler()
    {
        return m_uiHandler;
    }

    final P_SweetHandler getUpdateHandler()
    {
        return m_updateHandler;
    }

    final boolean isOnSweetBlueThread()
    {
        return Thread.currentThread() == m_updateHandler.getThread();
    }

    final void quit()
    {
        m_updateHandler.quit();
        m_uiHandler.quit();
    }

}
