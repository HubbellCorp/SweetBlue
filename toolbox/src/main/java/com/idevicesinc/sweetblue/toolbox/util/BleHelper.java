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

package com.idevicesinc.sweetblue.toolbox.util;


import android.content.Context;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.BleNodeConfig;
import com.idevicesinc.sweetblue.BleTask;
import com.idevicesinc.sweetblue.LogOptions;
import com.idevicesinc.sweetblue.defaults.DefaultDeviceReconnectFilter;
import com.idevicesinc.sweetblue.utils.Interval;


public class BleHelper
{

    private static final BleHelper ourInstance = new BleHelper();


    public static BleHelper get()
    {
        return ourInstance;
    }


    private BleDevice m_device;
    private IBleConfigFactory m_configFactory = new BleConfigFactory();
    private BleManager m_manager;



    private BleHelper()
    {
    }

    public void init(Context context)
    {
        m_manager = BleManager.get(context, getInitialConfig());
    }

    private void checkManager(Context context)
    {
        if (m_manager == null)
            init(context);
    }

    public BleManager getMgr()
    {
        return m_manager;
    }

    public BleManager getMgr(Context context)
    {
        checkManager(context);
        return m_manager;
    }

    public BleManager getMgr(BleManagerConfig config, Context context)
    {
        checkManager(context);
        
        if (config != null)
            m_manager.setConfig(config);

        return m_manager;
    }

    public void setDevice(BleDevice device)
    {
        m_device = device;
    }

    public BleDevice getDevice()
    {
        return m_device;
    }

    public void setConfigFactory(IBleConfigFactory factory)
    {
        m_configFactory = factory;
    }

    public BleManagerConfig getInitialConfig()
    {
        return m_configFactory.getInitialConfig();
    }


    public interface IBleConfigFactory
    {
        BleManagerConfig getInitialConfig();
    }


    public static final class BleConfigFactory implements IBleConfigFactory
    {
        @Override
        public BleManagerConfig getInitialConfig()
        {
            BleManagerConfig cfg = new BleManagerConfig();
            cfg.defaultDeviceStates = BleDeviceState.VALUES();
            cfg.connectFailRetryConnectingOverall = true;
//            cfg.logger = DebugLog.getDebugger();
            cfg.loggingOptions = LogOptions.ON;
            cfg.alwaysUseAutoConnect = true;
            cfg.reconnectFilter = new DefaultDeviceReconnectFilter(5, 1, Interval.millis(100), Interval.secs(10), Interval.mins(15), Interval.mins(60));
            cfg.manageCpuWakeLock = true;
            cfg.maxConnectionFailHistorySize = 100;
            cfg.forceBondDialog = true;
            cfg.alwaysBondOnConnect = false;
            cfg.tryBondingWhileDisconnected = false;
            cfg.autoBondFixes = false;
            cfg.cacheDeviceOnUndiscovery = true;
            cfg.doNotRequestLocation = true;
            cfg.connectionBugFixTimeout = Interval.secs(30);
            return cfg;
        }
    }
}
