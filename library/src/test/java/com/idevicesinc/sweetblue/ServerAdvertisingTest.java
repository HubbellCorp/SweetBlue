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


import com.idevicesinc.sweetblue.utils.Uuids;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.concurrent.atomic.AtomicInteger;



@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class ServerAdvertisingTest extends BaseBleUnitTest
{

    @Test(timeout = 15000)
    public void startAdvertisingTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;

        m_manager.setConfig(m_config);

        final BleServer server = m_manager.getServer();
        server.setListener_Advertising(e -> {
            ServerAdvertisingTest.this.assertTrue(e.wasSuccess());
            ServerAdvertisingTest.this.assertTrue(server.isAdvertising());
            ServerAdvertisingTest.this.assertTrue(server.isAdvertising(Uuids.BATTERY_SERVICE_UUID));
            server.stopAdvertising();
            ServerAdvertisingTest.this.succeed();
        });

        server.startAdvertising(Uuids.BATTERY_SERVICE_UUID);

        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void stopAdvertisingTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;

        m_manager.setConfig(m_config);

        final BleServer server = m_manager.getServer();
        server.setListener_Advertising(e -> {
            ServerAdvertisingTest.this.assertTrue(e.wasSuccess());
            server.stopAdvertising();
            ServerAdvertisingTest.this.assertFalse(server.isAdvertising());
            ServerAdvertisingTest.this.assertFalse(server.isAdvertising(Uuids.BATTERY_SERVICE_UUID));
            ServerAdvertisingTest.this.succeed();
        });

        server.startAdvertising(Uuids.BATTERY_SERVICE_UUID);

        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void startStopStartAdvertisingTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;

        final AtomicInteger startCount = new AtomicInteger(0);

        m_manager.setConfig(m_config);

        final BleServer server = m_manager.getServer();
        server.setListener_Advertising(e -> {
            if (startCount.get() > 3)
            {
                server.stopAdvertising();
                ServerAdvertisingTest.this.succeed();
            }

            ServerAdvertisingTest.this.assertTrue(e.wasSuccess());
            startCount.incrementAndGet();

            server.stopAdvertising();
            ServerAdvertisingTest.this.assertFalse(server.isAdvertising());

            server.startAdvertising(Uuids.BATTERY_SERVICE_UUID);
        });

        server.startAdvertising(Uuids.BATTERY_SERVICE_UUID);

        startAsyncTest();
    }

}
