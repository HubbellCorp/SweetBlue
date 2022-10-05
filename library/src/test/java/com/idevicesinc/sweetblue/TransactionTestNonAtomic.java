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
public class TransactionTestNonAtomic extends BaseBleUnitTest
{
    @Test(timeout = 40000)
    public void TransNonAtomicQueueTest() throws Exception
    {
        reset();

        m_config.updateThreadType = UpdateThreadType.THREAD;
        m_config.loggingOptions = LogOptions.ON;
        m_config.postCallbacksToMainThread = false;
        m_manager.setConfig(m_config);

        mDevice = m_manager.newDevice(Util_Unit.randomMacAddress());

        // To start, lets add some tasks,
        mDevice.connect(e -> {
            assertTrue(e.wasSuccess());

            P_Bridge_BleManager.suspendQueue(m_manager.getIBleManager());

            // Shove some junk into the queue
            addTestReadsToQueue(2);

            // Start a transaction
            mDevice.performTransaction(new BleTransaction()
            {
                @Override
                protected void start()
                {
                    // Add a test task
                    final BleRead read = new BleRead(mAuthServiceUuid, mAuthCharUuid).setReadWriteListener(e12 -> {
                        assertTrue(e12.wasSuccess());
                        assertTrue(mAuthCount == 0);
                        assertTrue(mInitCount == 2);

                        // Increase ordinal
                        ++mAuthCount;

                        // Do a second read now
                        final BleRead read2 = new BleRead(mAuthServiceUuid, mAuthCharUuid).setReadWriteListener(e1 -> {
                            assertTrue(e1.wasSuccess());
                            assertTrue(mAuthCount == 1);
                            assertTrue(mInitCount == 4);

                            // Succeed both the transaction and the test
                            succeed();
                            TransactionTestNonAtomic.this.succeed();
                        });

                        read(read2);
                    });

                    read(read);
                }
            });

            // Start the transaction
            addTestReadsToQueue(2);

            P_Bridge_BleManager.unsuspendQueue(m_manager.getIBleManager());
        });

        startAsyncTest();
    }

    private BleDevice mDevice;
    private int mAuthCount = 0;
    private int mInitCount = 0;

    private void reset()
    {
        mDevice = null;
        mAuthCount = mInitCount = 0;
        m_config.defaultTransactionAtomicity = BleTransaction.Atomicity.NOT_ATOMIC;
    }

    private void addTestReadsToQueue(int numTasks)
    {
        // Add however many tasks were requested into the queue
        for (int i = 0; i < numTasks; ++i)
        {
            // Add a test task
            final BleRead read = new BleRead(mInitServiceUuid, mInitCharUuid).setReadWriteListener(e -> {
                // Increase count
                ++mInitCount;
            });

            mDevice.read(read);
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
    public void postSetup()
    {
        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, inputs -> new UnitTestBluetoothGatt(inputs.get(0), db));
    }
}
