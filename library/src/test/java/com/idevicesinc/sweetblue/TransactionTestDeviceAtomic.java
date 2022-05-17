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
import com.idevicesinc.sweetblue.internal.P_Bridge_BleManager;
import com.idevicesinc.sweetblue.internal.android.IBluetoothGatt;
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
public class TransactionTestDeviceAtomic extends BaseBleUnitTest
{
    @Test//(timeout = 40000)
    public void TransDeviceAtomicQueueTest() throws Exception
    {
        reset();

        m_config.updateThreadType = UpdateThreadType.THREAD;
        m_config.loggingOptions = LogOptions.ON;
        m_config.postCallbacksToMainThread = false;
        m_manager.setConfig(m_config);

        m_deviceA = m_manager.newDevice(Util_Unit.randomMacAddress());
        m_deviceB = m_manager.newDevice(Util_Unit.randomMacAddress());

        DeviceConnectListener dcl = e -> {
            assertTrue(e.wasSuccess());
            if (m_deviceA.is(BleDeviceState.INITIALIZED) && m_deviceB.is(BleDeviceState.INITIALIZED))
                doTest();
        };

        // To start, lets add some tasks,
        m_deviceA.connect(dcl);
        m_deviceB.connect(dcl);

        startAsyncTest();
    }

    private void doTest()
    {
        P_Bridge_BleManager.suspendQueue(m_manager.getIBleManager());

        // Shove some junk into the queue, 2 tasks for each device
        addTestReadsToQueue(m_deviceA, 2);
        addTestReadsToQueue(m_deviceB, 2);

        // Adjust the config to make the transaction atomic
        m_config.defaultTransactionAtomicity = BleTransaction.Atomicity.DEVICE_ATOMIC;
        m_manager.setConfig(m_config);

        // Start a transaction
        m_deviceA.performTransaction(new BleTransaction()
        {
            @Override
            protected void start()
            {
                // Add a test task
                final BleRead read = new BleRead(mAuthServiceUuid, mAuthCharUuid).setReadWriteListener(e -> {
                    assertTrue(e.wasSuccess());
                    assertTrue(mAuthCount == 0);
                    assertTrue(mInitACount == 0);
                    assertTrue(mInitBCount == 2);

                    // Increase ordinal
                    ++mAuthCount;

                    // Do a second read now
                    final BleRead read2 = new BleRead(mAuthServiceUuid, mAuthCharUuid).setReadWriteListener(e2 -> {
                        assertTrue(e2.wasSuccess());
                        assertTrue(mAuthCount == 1);
                        assertTrue(mInitACount == 0);
                        assertTrue(mInitBCount == 4);

                        // Flag that the transaction ran to completion
                        mTransactionSuccess = true;
                        succeed();
                    });

                    read(read2);
                });

                read(read);
            }
        });

        // Add more test tasks
        addTestReadsToQueue(m_deviceA, 2);
        addTestReadsToQueue(m_deviceB, 2);

        // Start up the queue
        P_Bridge_BleManager.unsuspendQueue(m_manager.getIBleManager());
    }

    private BleDevice m_deviceA;
    private BleDevice m_deviceB;
    private int mAuthCount = 0;
    private int mInitACount = 0;
    private int mInitBCount = 0;
    private boolean mTransactionSuccess = false;

    private void reset()
    {
        m_deviceA = null;
        m_deviceB = null;
        mAuthCount = mInitACount = mInitBCount = 0;
        m_config.defaultTransactionAtomicity = BleTransaction.Atomicity.NOT_ATOMIC;
    }

    private void addTestReadsToQueue(final BleDevice device, int numTasks)
    {
        // Add however many tasks were requested into the queue
        for (int i = 0; i < numTasks; ++i)
        {
            // Add a test task
            final BleRead read = new BleRead(mInitServiceUuid, mInitCharUuid).setReadWriteListener(e -> {
                // Increase count
                if (device == m_deviceA)
                {
                    ++mInitACount;
                    TransactionTestDeviceAtomic.this.assertTrue(mInitBCount == 4);  // No random reads on device A should run until device B is done
                    TransactionTestDeviceAtomic.this.assertTrue(mTransactionSuccess);
                }
                else
                {
                    ++mInitBCount;
                    TransactionTestDeviceAtomic.this.assertTrue(mInitACount == 0);
                }

                if (mInitACount == 4)
                {
                    TransactionTestDeviceAtomic.this.assertTrue(mInitBCount == 4);  // Make sure the B transactions ran
                    TransactionTestDeviceAtomic.this.assertTrue(mTransactionSuccess);  // Make sure the transaction actually ran to completion
                    TransactionTestDeviceAtomic.this.succeed();
                }
            });

            device.read(read);
        }
    }

    private final static UUID mAuthServiceUuid = UUID.randomUUID();
    private final static UUID mAuthCharUuid = UUID.randomUUID();
    private final static UUID mInitServiceUuid = UUID.randomUUID();
    private final static UUID mInitCharUuid = UUID.randomUUID();

    private GattDatabase db = new GattDatabase().addService(mAuthServiceUuid)
            .addCharacteristic(mAuthCharUuid).setValue(new byte[]{0x2, 0x4}).setProperties().read().setPermissions().read().completeService()
            .addService(mInitServiceUuid)
            .addCharacteristic(mInitCharUuid).setValue(new byte[]{0x8, 0xA}).setProperties().read().setPermissions().read().completeService();

    @Override
    public IBluetoothGatt getGattLayer(IBleDevice device)
    {
        return new UnitTestBluetoothGatt(device, db);
    }
}
