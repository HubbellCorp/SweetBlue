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
import com.idevicesinc.sweetblue.internal.android.IBluetoothGatt;
import com.idevicesinc.sweetblue.rx.RxBleDevice;
import com.idevicesinc.sweetblue.rx.RxBleManagerConfig;
import com.idevicesinc.sweetblue.rx.RxBleTransaction;
import com.idevicesinc.sweetblue.utils.GattDatabase;
import com.idevicesinc.sweetblue.utils.UpdateThreadType;
import com.idevicesinc.sweetblue.utils.Util_Unit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.UUID;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public final class RxOtaTest extends RxBaseBleUnitTest
{

    private final static UUID m_serviceUuid = UUID.randomUUID();
    private final static UUID m_charUuid = UUID.randomUUID();

    private final GattDatabase db = new GattDatabase().addService(m_serviceUuid)
            .addCharacteristic(m_charUuid).setValue(new byte[]{0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0}).setProperties().write().setPermissions().write().completeService();

    @Test(timeout = 20000)
    public void otaTest() throws Exception
    {
        final RxBleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "OtaTester");

        m_disposables.add(device.connect().subscribe(() -> device.performOta(new TestOta())));

        startAsyncTest();
    }


    private final class TestOta extends RxBleTransaction.RxOta
    {

        @Override
        protected void start()
        {
            final BleWrite bleWrite = new BleWrite(m_serviceUuid, m_charUuid).setBytes(Util_Unit.randomBytes(10));
            RxBleDevice rxDevice = m_manager.getDevice(getDevice().getMacAddress());
            m_disposables.add(rxDevice.write(bleWrite).subscribe(e -> {
                bleWrite.setBytes(Util_Unit.randomBytes(10));
                m_disposables.add(rxDevice.write(bleWrite).subscribe(e1 ->
                {
                    bleWrite.setBytes(Util_Unit.randomBytes(10));
                    m_disposables.add(rxDevice.write(bleWrite).subscribe(e2 -> RxOtaTest.this.succeed()));
                }));
            }));
        }
    }

    @Override
    public void postSetup()
    {
        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, args -> new UnitTestBluetoothGatt(args.get(0), db));
    }

    @Override
    public RxBleManagerConfig getConfig()
    {
        RxBleManagerConfig config = super.getConfig();
        config.loggingOptions = LogOptions.ON;
        m_config.updateThreadType = UpdateThreadType.THREAD;
        return config;
    }

}
