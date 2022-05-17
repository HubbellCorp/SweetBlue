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
import com.idevicesinc.sweetblue.internal.IBleDevice;
import com.idevicesinc.sweetblue.internal.IBleManager;
import com.idevicesinc.sweetblue.internal.P_Bridge_BleManager;
import com.idevicesinc.sweetblue.internal.android.IBluetoothDevice;
import com.idevicesinc.sweetblue.internal.android.IBluetoothGatt;
import com.idevicesinc.sweetblue.internal.android.IBluetoothManager;
import com.idevicesinc.sweetblue.internal.android.IBluetoothServer;
import com.idevicesinc.sweetblue.internal.android.P_ServerHolder;
import com.idevicesinc.sweetblue.rx.RxBleManager;
import com.idevicesinc.sweetblue.rx.RxBleManagerConfig;
import com.idevicesinc.sweetblue.framework.AbstractTestClass;

import org.junit.After;
import org.junit.Before;


/**
 * Convenience base class for running unit tests involving SweetBlue. Parameterize the sub-class with
 * the {@link Activity} class you are testing against. You will then have to implement {@link #createActivity()}
 * and pass in the newly created {@link Activity} instance.
 */
public abstract class RxSweetUnitTest<A extends Activity> extends AbstractTestClass
{

    public IBluetoothManager mBluetoothManager;
    public IBluetoothServer mBluetoothServer;


    public RxBleManager m_manager;
    public RxBleManagerConfig m_config;

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
     * Returns the current instance of {@link IBluetoothGatt}. If the instance is <code>null</code>, then
     * a new instance of {@link UnitTestBluetoothGatt} will be created and used.
     */
    public @Nullable(Nullable.Prevalence.NEVER) IBluetoothGatt getGattLayer(@Nullable(Nullable.Prevalence.NEVER) IBleDevice device)
    {
        return new UnitTestBluetoothGatt(device);
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
     * using {@link #initManager(RxBleManagerConfig)}, {@link #getConfig()}.
     */
    @Before
    public void setup() throws Exception
    {
        m_activity = createActivity();
        initManager(getConfig());
    }

    /**
     * Initializes the {@link RxBleManager} instance with the given {@link RxBleManagerConfig} instance. This also
     * calls {@link Util_Native#forceOn(BleManager)}, and onResume() on the {@link Activity} instance.
     *
     * @return the newly created {@link RxBleManager} instance
     */
    protected RxBleManager initManager(@Nullable(Nullable.Prevalence.RARE) RxBleManagerConfig config)
    {
        m_manager = RxBleManager.get(m_activity, config);
        Util_Native.forceOn(m_manager.getBleManager());
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
        m_manager.shutdown();
        m_activity.finish();
        m_manager = null;
        m_config = null;
        m_activity = null;
        mBluetoothServer = null;
        mBluetoothManager = null;
    }

    /**
     * Returns an instance of {@link BleManagerConfig}. Override this method to give a different instance
     * during test bring-up. This is called by {@link #setup()}, and the result is passed into {@link #initManager(RxBleManagerConfig)}.
     */
    public RxBleManagerConfig getConfig()
    {
        m_config = new RxBleManagerConfig();
        m_config.bluetoothManagerImplementation = getManagerLayer();
        m_config.gattFactory = this::getGattLayer;
        m_config.bluetoothDeviceFactory = this::getDeviceLayer;
        m_config.serverFactory = this::getServerLayer;
        m_config.logger = new UnitTestLogger();
        m_config.blockingShutdown = true;
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

    public BleScanApi getScanApi()
    {
        return P_Bridge_BleManager.getScanApi(m_manager.getBleManager().getIBleManager());
    }

    public BleScanPower getScanPower()
    {
        return P_Bridge_BleManager.getScanPower(m_manager.getBleManager().getIBleManager());
    }

}
