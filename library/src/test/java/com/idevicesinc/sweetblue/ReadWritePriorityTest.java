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


import android.bluetooth.BluetoothGattCallback;
import android.content.Context;
import com.idevicesinc.sweetblue.internal.IBleDevice;
import com.idevicesinc.sweetblue.internal.android.IBluetoothDevice;
import com.idevicesinc.sweetblue.utils.GattDatabase;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Util_Unit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.UUID;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class ReadWritePriorityTest extends BaseBleUnitTest
{

    private final static UUID firstServiceUuid = UUID.randomUUID();
    private final static UUID secondSeviceUuid = UUID.randomUUID();
    private final static UUID thirdServiceUuid = UUID.randomUUID();

    private final static UUID firstCharUuid = UUID.randomUUID();
    private final static UUID secondCharUuid = UUID.randomUUID();
    private final static UUID thirdCharUuid = UUID.randomUUID();
    private final static UUID fourthCharUuid = UUID.randomUUID();


    private GattDatabase db =
            new GattDatabase().addService(firstServiceUuid).addCharacteristic(firstCharUuid).setProperties().readWrite().setPermissions().readWrite().completeService()
                    .addService(secondSeviceUuid).addCharacteristic(secondCharUuid).setProperties().readWrite().setPermissions().readWrite().completeService()
                    .addService(thirdServiceUuid).addCharacteristic(thirdCharUuid).setProperties().readWrite().setPermissions().readWrite().completeChar()
                    .addCharacteristic(fourthCharUuid).setProperties().readWrite().setPermissions().readWrite().completeService();


    @Test(timeout = 45000L)
    public void readStompTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;
        m_config.equalOpportunityReadsWrites = true;
        // TODO - Fix the task timeout for the connect task. Once fixed, this test shouldn't take as long to complete
        m_config.taskTimeoutRequestFilter = new BleNodeConfig.DefaultTaskTimeoutRequestFilter()
        {
            @Override
            public Please onEvent(TaskTimeoutRequestEvent e)
            {
                if (e.task() == BleTask.CONNECT)
                    return Please.setTimeoutFor(Interval.secs(1));
                else
                    return super.onEvent(e);
            }
        };

        m_config.gattFactory = device -> new UnitTestBluetoothGatt(device, db);

        m_manager.setConfig(m_config);

        final BleDevice goodDevice = m_manager.newDevice(Util_Unit.randomMacAddress(), "MyGoodDevice");

        final BleDeviceConfig badConfig = new BleDeviceConfig_UnitTest();
        badConfig.gattFactory = UnconnectableGatt::new;

        final BleDevice badDevice1 = m_manager.newDevice(Util_Unit.randomMacAddress(), "BadDevice1", badConfig);
        final BleDevice badDevice2 = m_manager.newDevice(Util_Unit.randomMacAddress(), "BadDevice2", badConfig);

        // Connect to the good device
        goodDevice.connect(e ->
        {
            if (e.wasSuccess())
            {
                // Now that our good device is connected, let's get the connect logic going for our bad devices
                badDevice1.connect();
                badDevice2.connect();
                BleRead read = new BleRead(firstServiceUuid, firstCharUuid);
                read.setReadWriteListener(e1 ->
                {
                    assertTrue("Bad devices are no longer connecting.", badDevice1.is(BleDeviceState.CONNECTING_OVERALL) && badDevice2.is(BleDeviceState.CONNECTING_OVERALL));
                    succeed();
                });
                goodDevice.read(read);
            }
        });

        startAsyncTest();
    }


    private final class UnconnectableGatt extends UnitTestBluetoothGatt
    {

        public UnconnectableGatt(IBleDevice device)
        {
            super(device);
        }

        @Override
        public void connect(IBluetoothDevice device, Context context, boolean useAutoConnect, BluetoothGattCallback callback)
        {
            setGattNull(false);
        }

    }


}
