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
import com.idevicesinc.sweetblue.annotations.Nullable;
import com.idevicesinc.sweetblue.di.SweetDIManager;
import com.idevicesinc.sweetblue.internal.IBleDevice;
import com.idevicesinc.sweetblue.internal.IBleManager;
import com.idevicesinc.sweetblue.internal.android.IBluetoothDevice;
import com.idevicesinc.sweetblue.internal.android.IBluetoothGatt;
import com.idevicesinc.sweetblue.internal.android.IBluetoothManager;
import com.idevicesinc.sweetblue.internal.android.IBluetoothServer;
import com.idevicesinc.sweetblue.internal.android.P_ServerHolder;
import com.idevicesinc.sweetblue.framework.AbstractTestClass;

import org.junit.After;
import org.junit.Before;


/**
 * Convenience base class for running unit tests involving SweetBlue. Parameterize the sub-class with
 * the {@link Activity} class you are testing against. You will then have to implement {@link #createActivity()}
 * and pass in the newly created {@link Activity} instance.
 */
public abstract class SweetUnitTest<A extends Activity> extends AbstractTestClass
{

    public IBluetoothManager mBluetoothManager;
    public IBluetoothServer mBluetoothServer;


    public BleManager m_manager;
    public BleManagerConfig m_config;

    public A m_activity;


    /**
     * Subclasses must implement this method, and a valid, non-null Activity instance is expected.
     */
    protected abstract @Nullable(Nullable.Prevalence.NEVER) A createActivity();


    /**
     * Returns the current instance of {@link IBluetoothManager}. If the instance is <code>null</code>, then
     * a new instance of {@link UnitTestBluetoothManager} will be created and used.
     */
    public @Nullable(Nullable.Prevalence.NEVER) IBluetoothManager getManagerLayer()
    {
        if (mBluetoothManager == null)
            mBluetoothManager = new UnitTestBluetoothManager();
        return mBluetoothManager;
    }

    /**
     * Returns the current instance of {@link IBluetoothDevice}. If the instance is <code>null</code>, then
     * a new instance of {@link UnitTestBluetoothDevice} will be created and used.
     */
    public @Nullable(Nullable.Prevalence.NEVER) IBluetoothDevice getDeviceLayer(@Nullable(Nullable.Prevalence.NEVER) IBleDevice device)
    {
        return new UnitTestBluetoothDevice(device);
    }

    /**
     * Returns the current instance of {@link IBluetoothServer}. If the instance is <code>null</code>, then
     * a new instance of {@link UnitTestBluetoothServer} will be created and used.
     */
    public @Nullable(Nullable.Prevalence.NEVER) IBluetoothServer getServerLayer(@Nullable(Nullable.Prevalence.NEVER) IBleManager mgr, @Nullable(Nullable.Prevalence.NEVER) P_ServerHolder serverHolder)
    {
        if (mBluetoothServer == null)
            mBluetoothServer = new UnitTestBluetoothServer(mgr);
        return mBluetoothServer;
    }


    /**
     * This method is called before every test. This is where the {@link Activity} instance is created,
     * from calling {@link #createActivity()}. The {@link BleManager} instance is also initialized here
     * using {@link #initManager(BleManagerConfig)}, {@link #getConfig()}.
     */
    @Before
    public void setup()
    {
        if (m_activity == null)
            m_activity = createActivity();
        // bluetooth manager is a special case where it has to be registered before initializing the blemanager
        // whereas everything else should be regi
        SweetDIManager.getInstance().registerTransient(IBluetoothManager.class, UnitTestBluetoothManager.class);
        initManager(getConfig());
        SweetDIManager.getInstance().registerTransient(IBluetoothDevice.class, UnitTestBluetoothDevice.class);
        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, UnitTestBluetoothGatt.class);
        postSetup();
    }

    /**
     * Override this method if you need to perform additional setup, after the built-in setup is done
     */
    public void postSetup()
    {
    }

    /**
     * Initializes the {@link BleManager} instance with the given {@link BleManagerConfig} instance. This also
     * calls {@link Util_Native#forceOn(BleManager)}, and onResume() on the {@link Activity} instance.
     *
     * @return the newly created {@link BleManager} instance
     */
    protected BleManager initManager(@Nullable(Nullable.Prevalence.RARE) BleManagerConfig config)
    {
        m_manager = BleManager.get(m_activity, config);
        Util_Native.forceOn(m_manager);
        m_manager.onResume();
        return m_manager;
    }

    /**
     * This gets called after every test has run. It calls {@link BleManager#shutdown()}, and finish() on the
     * {@link Activity} instance, as well as null-ing out all instance fields.
     */
    @After
    public void tearDown() throws Exception
    {
        if (m_manager != null)
            m_manager.shutdown();
        m_activity.finish();
        SweetDIManager.getInstance().dispose();
        m_manager = null;
        m_config = null;
        m_activity = null;
        mBluetoothServer = null;
        mBluetoothManager = null;
    }

    /**
     * Returns an instance of {@link BleManagerConfig}. Override this method to give a different instance
     * during test bring-up. This is called by {@link #setup()}, and the result is passed into {@link #initManager(BleManagerConfig)}.
     */
    public BleManagerConfig getConfig()
    {
        m_config = new BleManagerConfig();
        m_config.serverFactory = this::getServerLayer;
        m_config.logger = new UnitTestLogger();
        return m_config;
    }

    /**
     * Should return the index of the current stack trace that contains the test method. Default is 2, but you need to add one for
     * each subclass layer.
     */
    @Override
    protected int getTraceIndex()
    {
        return super.getTraceIndex() + 1;
    }

}
