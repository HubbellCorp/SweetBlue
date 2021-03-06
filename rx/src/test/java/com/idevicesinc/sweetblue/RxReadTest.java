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
public final class RxReadTest extends RxBaseBleUnitTest
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
    public void simpleReadTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;

        m_manager.setConfig(m_config);

        final RxBleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "DeviceOfRead-ness");

        m_disposables.add(device.connect().subscribe(() ->
        {
            BleRead read = new BleRead(firstServiceUuid, firstCharUuid);
            m_disposables.add(device.read(read).subscribe(r ->
            {
                assertTrue(r.wasSuccess());
                assertNotNull(r.data());
                succeed();
            }));
        }, throwable -> {}));

        startAsyncTest();
    }

    @Test(timeout = 20000)
    public void multiReadTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;

        m_manager.setConfig(m_config);

        final RxBleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "DeviceOfRead-nes");

        final boolean[] reads = new boolean[4];

        m_disposables.add(device.connect().subscribe(() ->
        {
            BleRead.Builder builder = new BleRead.Builder(firstServiceUuid, firstCharUuid);
            builder.next().setServiceUUID(secondSeviceUuid).setCharacteristicUUID(secondCharUuid)
                    .next().setServiceUUID(thirdServiceUuid).setCharacteristicUUID(thirdCharUuid)
                    .next().setCharacteristicUUID(fourthCharUuid);
            m_disposables.add(device.readMany(builder.build()).subscribe(r ->
            {
                assertTrue(r.status().name(), r.wasSuccess());
                assertNotNull(r.data());
                if (r.isFor(firstCharUuid))
                    reads[0] = true;
                else if (r.isFor(secondCharUuid))
                    reads[1] = true;
                else if (r.isFor(thirdCharUuid))
                    reads[2] = true;
                else if (r.isFor(fourthCharUuid))
                {
                    assertTrue(reads[0] && reads[1] && reads[2]);
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
    public IBluetoothGatt getGattLayer(IBleDevice device)
    {
        return new ReadBluetoothGatt(device, db);
    }

    private static class ReadBluetoothGatt extends UnitTestBluetoothGatt
    {

        public ReadBluetoothGatt(IBleDevice device, GattDatabase gattDb)
        {
            super(device, gattDb);
        }

        @Override
        public boolean readCharacteristic(BleCharacteristic characteristic)
        {
            characteristic.setValue(Util_Unit.randomBytes(20));
            return super.readCharacteristic(characteristic);
        }
    }


}
