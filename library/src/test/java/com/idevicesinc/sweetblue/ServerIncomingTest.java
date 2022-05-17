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


import com.idevicesinc.sweetblue.utils.GattDatabase;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Util_Unit;
import com.idevicesinc.sweetblue.utils.Uuids;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public final class ServerIncomingTest extends BaseBleUnitTest
{

    @Test(timeout = 15000)
    public void readServerTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;

        m_manager.setConfig(m_config);

        final GattDatabase db = new GattDatabase()
                .addService(Uuids.BATTERY_SERVICE_UUID)
                .addCharacteristic(Uuids.BATTERY_LEVEL).setPermissions().readWrite().setProperties().readWrite().completeService();

        final String macAddress = Util_Unit.randomMacAddress();

        final BleServer server = m_manager.getServer(e -> {
            // Incoming listener, listen for write request, and make sure we got the data expected
            if (e.type() == ExchangeListener.Type.READ && e.isFor(Uuids.BATTERY_LEVEL))
            {
                return IncomingListener.Please.respondWithSuccess(new byte[]{0x2, 0x3});
            }
            return IncomingListener.Please.respondWithSuccess();
        }, db, e -> e.server().connect(macAddress, e1 -> {
            ServerIncomingTest.this.assertTrue(e1.wasSuccess());
            // Now that we're connected, request a read from the server
            Util_Native.readFromServer(e1.server(), macAddress, Uuids.BATTERY_LEVEL, Interval.millis(150));
        }));

        server.setListener_Outgoing(e -> {
            if (e.macAddress().equals(macAddress))
            {
                ServerIncomingTest.this.assertTrue(e.wasSuccess());
                ServerIncomingTest.this.assertArrayEquals(e.data_sent(), new byte[]{0x2, 0x3});
                ServerIncomingTest.this.succeed();
            }
        });

        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void writeServerTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;

        m_manager.setConfig(m_config);

        final GattDatabase db = new GattDatabase()
                .addService(Uuids.BATTERY_SERVICE_UUID)
                .addCharacteristic(Uuids.BATTERY_LEVEL).setPermissions().readWrite().setProperties().readWrite().completeService();

        final String macAddress = Util_Unit.randomMacAddress();

        final BleServer server = m_manager.getServer(e -> {
            // Incoming listener, listen for write request, and make sure we got the data expected
            if (e.type() == ExchangeListener.Type.WRITE && e.isFor(Uuids.BATTERY_LEVEL))
            {
                ServerIncomingTest.this.assertArrayEquals(e.data_received(), new byte[]{0x1, 0x2});
            }
            return IncomingListener.Please.respondWithSuccess();
        }, db, e -> e.server().connect(macAddress, e1 -> {
            ServerIncomingTest.this.assertTrue(e1.wasSuccess());
            // Now that we're connected, send the write request to the server
            Util_Native.sendWriteToServer(e1.server(), macAddress, Uuids.BATTERY_LEVEL, new byte[]{0x1, 0x2}, Interval.millis(150));
        }));

        server.setListener_Outgoing(e -> {
            if (e.macAddress().equals(macAddress))
            {
                ServerIncomingTest.this.assertTrue(e.wasSuccess());
                ServerIncomingTest.this.succeed();
            }
        });

        startAsyncTest();
    }

}
