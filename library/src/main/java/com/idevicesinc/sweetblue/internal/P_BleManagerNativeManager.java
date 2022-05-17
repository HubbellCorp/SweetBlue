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


import com.idevicesinc.sweetblue.internal.android.IBluetoothManager;


final class P_BleManagerNativeManager
{

    private final IBleManager m_manager;
    private final P_BleManager_ListenerProcessor m_listenerProcessor;
    private IBluetoothManager m_nativeManager;


    P_BleManagerNativeManager(IBleManager mgr)
    {
        m_manager = mgr;
        m_listenerProcessor = new P_BleManager_ListenerProcessor(mgr);
    }


    final void init(IBluetoothManager mgrLayer)
    {
        m_nativeManager = mgrLayer;
        m_listenerProcessor.updatePollRate(m_manager.getConfigClone().defaultStatePollRate);
    }

    final void shutdown()
    {
        m_listenerProcessor.onDestroy();
    }

    final void update(double timeStep)
    {
        m_listenerProcessor.update(timeStep);
    }

    final PA_Task.I_StateListener getScanTaskListener()
    {
        return m_listenerProcessor.getScanTaskListener();
    }

    final P_BleManager_ListenerProcessor getListenerProcessor()
    {
        return m_listenerProcessor;
    }

}
