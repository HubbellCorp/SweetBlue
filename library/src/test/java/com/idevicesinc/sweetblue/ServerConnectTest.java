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



import com.idevicesinc.sweetblue.internal.IBleManager;
import com.idevicesinc.sweetblue.internal.android.P_DeviceHolder;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Util_Unit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class ServerConnectTest extends BaseBleUnitTest
{

    @Test(timeout = 15000)
    public void connectTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;

        final String deviceMac = Util_Unit.randomMacAddress();

        m_manager.setConfig(m_config);

        BleServer server = m_manager.getServer();

        server.connect(deviceMac, e -> ServerConnectTest.this.succeed());

        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void connectMultipleTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;

        m_manager.setConfig(m_config);

        final String mac1 = Util_Unit.randomMacAddress();
        final String mac2 = Util_Unit.randomMacAddress();
        final String mac3 = Util_Unit.randomMacAddress();

        BleServer server = m_manager.getServer();

        server.connect(mac1);
        server.connect(mac2);
        server.connect(mac3, e -> ServerConnectTest.this.succeed());

        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void connectThenDisconnectTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;

        final String deviceMac = Util_Unit.randomMacAddress();

        m_manager.setConfig(m_config);

        final BleServer server = m_manager.getServer();

        server.setListener_State(e -> {
            if (e.didEnter(BleServerState.CONNECTED))
                server.disconnect();
            else if (e.didEnter(BleServerState.DISCONNECTED))
                ServerConnectTest.this.succeed();
        });

        server.connect(deviceMac);

        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void failConnectThenConnectTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;
        m_config.serverFactory = (manager, server) -> new FailConnectOnceServer(manager);

        final String deviceMac = Util_Unit.randomMacAddress();

        m_manager.setConfig(m_config);

        final BleServer server = m_manager.getServer();

        server.connect(deviceMac, e -> {
            if (e.failEvent() != null && e.failEvent().failureCountSoFar() == 0)
            {
                ServerConnectTest.this.assertFalse(e.wasSuccess());
                ServerConnectTest.this.assertTrue(e.isRetrying());
                ServerConnectTest.this.assertTrue(server.is(deviceMac, BleServerState.RETRYING_CONNECTION));
            }
            else
            {
                ServerConnectTest.this.assertTrue(e.wasSuccess());
                ServerConnectTest.this.succeed();
            }
        });

        startAsyncTest();
    }


    private final static class FailConnectOnceServer extends UnitTestBluetoothServer
    {

        private boolean m_failedOnce = false;

        public FailConnectOnceServer(IBleManager mgr)
        {
            super(mgr);
        }

        @Override public boolean connect(P_DeviceHolder device, boolean autoConnect)
        {
            if (m_failedOnce)
                return super.connect(device, autoConnect);
            else
            {
                m_failedOnce = true;
                Util_Native.setToDisconnected(getManager().getServer().getBleServer(), device.getAddress(), BleStatuses.GATT_ERROR, Interval.millis(1050));
            }
            return true;
        }
    }
}
