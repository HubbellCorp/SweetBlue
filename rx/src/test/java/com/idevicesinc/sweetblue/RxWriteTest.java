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


import com.idevicesinc.sweetblue.di.SweetDIManager;
import com.idevicesinc.sweetblue.internal.IBleDevice;
import com.idevicesinc.sweetblue.internal.android.IBluetoothGatt;
import com.idevicesinc.sweetblue.rx.RxBleDevice;
import com.idevicesinc.sweetblue.utils.GattDatabase;
import com.idevicesinc.sweetblue.utils.Util_Unit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.UUID;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public final class RxWriteTest extends RxBaseBleUnitTest
{

    private final static UUID firstServiceUuid = UUID.randomUUID();
    private final static UUID secondSeviceUuid = UUID.randomUUID();
    private final static UUID thirdServiceUuid = UUID.randomUUID();

    private final static UUID firstCharUuid = UUID.randomUUID();
    private final static UUID secondCharUuid = UUID.randomUUID();
    private final static UUID thirdCharUuid = UUID.randomUUID();
    private final static UUID fourthCharUuid = UUID.randomUUID();


    private final GattDatabase db =
            new GattDatabase().addService(firstServiceUuid).addCharacteristic(firstCharUuid).setProperties().readWrite().setPermissions().readWrite().completeService()
                    .addService(secondSeviceUuid).addCharacteristic(secondCharUuid).setProperties().readWrite().setPermissions().readWrite().completeService()
                    .addService(thirdServiceUuid).addCharacteristic(thirdCharUuid).setProperties().readWrite().setPermissions().readWrite().completeChar()
                    .addCharacteristic(fourthCharUuid).setProperties().readWrite().setPermissions().readWrite().completeService();

    @Test(timeout = 15000)
    public void simpleWriteTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;

        m_manager.setConfig(m_config);

        final RxBleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "DeviceOfWrite-ness");

        m_disposables.add(device.connect().subscribe(() ->
        {
            BleWrite write = new BleWrite(firstServiceUuid, firstCharUuid).setBytes(Util_Unit.randomBytes(20));
            m_disposables.add(device.write(write).subscribe(r ->
            {
                assertTrue(r.status().name(), r.wasSuccess());
                assertNotNull(r.data());
                succeed();
            }));
        }, throwable -> {}));

        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void multiWriteTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;

        m_manager.setConfig(m_config);

        final RxBleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "DeviceOfRead-nes");

        final boolean[] writes = new boolean[4];

        m_disposables.add(device.connect().subscribe(() ->
        {
            BleWrite.Builder builder = new BleWrite.Builder(firstServiceUuid, firstCharUuid);
            builder.setBytes(Util_Unit.randomBytes(20))
                    .next().setServiceUUID(secondSeviceUuid).setCharacteristicUUID(secondCharUuid).setBytes(Util_Unit.randomBytes(20))
                    .next().setServiceUUID(thirdServiceUuid).setCharacteristicUUID(thirdCharUuid).setBytes(Util_Unit.randomBytes(20))
                    .next().setCharacteristicUUID(fourthCharUuid).setBytes(Util_Unit.randomBytes(20));
            m_disposables.add(device.writeMany(builder.build()).subscribe(r ->
            {
                assertTrue(r.status().name(), r.wasSuccess());
                assertNotNull(r.data());
                if (r.isFor(firstCharUuid))
                    writes[0] = true;
                else if (r.isFor(secondCharUuid))
                    writes[1] = true;
                else if (r.isFor(thirdCharUuid))
                    writes[2] = true;
                else if (r.isFor(fourthCharUuid))
                {
                    assertTrue(writes[0] && writes[1] && writes[2]);
                    succeed();
                }
                else
                    // We should never get to this option
                    assertTrue(false);
            }));
        }, throwable -> {}));

        startAsyncTest();
    }

    @Override
    public void postSetup()
    {
        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, args -> new UnitTestBluetoothGatt(args.get(0), db));
    }

}
