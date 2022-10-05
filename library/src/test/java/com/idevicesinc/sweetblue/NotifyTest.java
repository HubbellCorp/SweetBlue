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


import android.bluetooth.BluetoothGattCharacteristic;

import com.idevicesinc.sweetblue.di.SweetDIManager;
import com.idevicesinc.sweetblue.internal.IBleDevice;
import com.idevicesinc.sweetblue.internal.android.IBluetoothGatt;
import com.idevicesinc.sweetblue.utils.GattDatabase;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Util_Unit;
import com.idevicesinc.sweetblue.utils.Uuids;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class NotifyTest extends BaseBleUnitTest
{

    private static final UUID mTestService = Uuids.fromShort("12BA");
    private static final UUID mTestChar = Uuids.fromShort("12BC");
    private static final UUID mTest2Char = Uuids.fromShort("12BD");
    private static final UUID mTestDesc = Uuids.CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR_UUID;


    private BleDevice m_device;
    private GattDatabase dbNotifyWithDesc = new GattDatabase().addService(mTestService)
            .addCharacteristic(mTestChar).setProperties().readWriteNotify().setPermissions().read().build()
            .addDescriptor(mTestDesc).setPermissions().read().completeService();
    private GattDatabase dbNotify = new GattDatabase().addService(mTestService)
            .addCharacteristic(mTestChar).setProperties().readWriteNotify().setPermissions().read().completeChar()
            .addCharacteristic(mTest2Char).setProperties().readWriteNotify().setPermissions().read().completeService();
    private GattDatabase dbIndicateNoDesc = new GattDatabase().addService(mTestService)
            .addCharacteristic(mTestChar).setProperties().readWriteIndicate().setPermissions().read().completeService();


    @Test(timeout = 15000)
    public void enableNotifyTest() throws Exception
    {
        m_device = null;

        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, inputs -> new UnitTestBluetoothGatt(inputs.get(0), dbNotifyWithDesc));

        m_config.loggingOptions = LogOptions.ON;

        m_manager.setConfig(m_config);

        m_manager.setListener_Discovery(e -> {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                NotifyTest.this.m_device = e.device();
                NotifyTest.this.m_device.connect(new BleTransaction.Init()
                {
                    @Override
                    protected void start()
                    {
                        BleNotify notify = new BleNotify(mTestChar).setNotificationListener(e1 -> {
                            assertTrue("Enabling notification failed with status " + e1.status(), e1.wasSuccess());
                            assertTrue(NotifyTest.this.m_device.isNotifyEnabled(mTestChar));
                            succeed();
                            NotifyTest.this.succeed();
                        });
                        enableNotify(notify);
                    }
                });
            }
        });

        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device");

        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void enableNotifyDuplicateEnablingTest() throws Exception
    {
        m_device = null;

        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, inputs -> new UnitTestBluetoothGatt(inputs.get(0), dbNotifyWithDesc));

        m_config.loggingOptions = LogOptions.ON;

        m_manager.setConfig(m_config);

        final AtomicBoolean gotEnabling = new AtomicBoolean(false);

        m_manager.setListener_Notification(e ->
        {
            if (e.type() == NotificationListener.Type.ENABLING_NOTIFICATION)
            {
                assertTrue("Enabling notification failed with status " + e.status(), e.wasSuccess());
                assertTrue(NotifyTest.this.m_device.isNotifyEnabled(mTestChar));
                assertFalse("Got the enabling notification callback twice!", gotEnabling.get());
                gotEnabling.set(true);
                Util_Native.sendNotification(m_device, dbNotifyWithDesc.getServiceList().get(0).getCharacteristics().get(0), new byte[] { 0 }, Interval.millis(250));
            }
            else if (e.type() == NotificationListener.Type.NOTIFICATION)
            {
                succeed();
            }
        });

        m_manager.setListener_Discovery(e -> {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                NotifyTest.this.m_device = e.device();
                NotifyTest.this.m_device.connect(new BleTransaction.Init()
                {
                    @Override
                    protected void start()
                    {
                        BleNotify notify = new BleNotify(mTestChar);
                        enableNotify(notify);
                    }
                });
            }
        });

        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device");

        startAsyncTest();
    }

    @Test(timeout = 10000)
    public void enableNotifyEarlyOutTest() throws Exception
    {
        m_device = null;

        m_config.loggingOptions = LogOptions.ON;

        m_manager.setConfig(m_config);

        m_manager.setListener_Discovery(e -> {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                m_device = e.device();
                m_device.connect(new BleTransaction.Init()
                {
                    @Override
                    protected void start()
                    {
                        BleNotify notify = new BleNotify(mTestChar).setNotificationListener(e1 -> {
                            NotificationListener.Type type = e1.type();
                            assertTrue("Expected ENABLING_NOTIFICATION, but got " + type + " instead.",type == NotificationListener.Type.ENABLING_NOTIFICATION);
                            assertFalse(e1.wasSuccess());
                            // Succeed the init transaction
                            succeed();
                            NotifyTest.this.succeed();
                        });
                        enableNotify(notify);
                    }
                });
            }
        });

        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device");

        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void enableNotifyNoDescriptorTest() throws Exception
    {
        m_device = null;

        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, inputs -> new UnitTestBluetoothGatt(inputs.get(0), dbNotify));

        m_config.loggingOptions = LogOptions.ON;

        m_manager.setConfig(m_config);

        m_manager.setListener_Discovery(e -> {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                NotifyTest.this.m_device = e.device();
                NotifyTest.this.m_device.connect(new BleTransaction.Init()
                {
                    @Override
                    protected void start()
                    {
                        BleNotify notify = new BleNotify(mTestChar).setNotificationListener(e1 -> {
                            if (e1.type() == NotificationListener.Type.ENABLING_NOTIFICATION)
                            {
                                assertTrue("Enabling notification failed with status " + e1.status(), e1.wasSuccess());
                                assertTrue(NotifyTest.this.m_device.isNotifyEnabled(mTestChar));
                                succeed();
                                NotifyTest.this.succeed();
                            }
                        });
                        enableNotify(notify);
                    }
                });
            }
        });

        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device");

        startAsyncTest();
    }

    @Test(timeout = 18000)
    public void enableNotifyWithNoDefaultListenerTest() throws Exception
    {
        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, inputs -> new UnitTestBluetoothGatt(inputs.get(0), dbNotify));
        m_config.loggingOptions = LogOptions.ON;

        m_manager.setConfig(m_config);

        final BleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "Florence Testingdale");

        device.connect(e -> {
            assertTrue(e.wasSuccess());
            BleNotify notify = new BleNotify(mTestService, mTestChar).setNotificationListener(e1 -> {
                assertTrue(e1.wasSuccess());
                if (e1.type() == NotificationListener.Type.ENABLING_NOTIFICATION)
                {
                    BleCharacteristic blechar = device.getNativeBleCharacteristic(mTestService, mTestChar);
                    Util_Native.sendNotification(device, blechar, new byte[]{0x1, 0x2, 0x3, 0x4});
                }
                else
                {
                    assertArrayEquals(new byte[]{0x1, 0x2, 0x3, 0x4}, e1.data());
                    succeed();
                }
            });
            device.enableNotify(notify);
        });

        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void multiNotifyTest() throws Exception
    {
        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, inputs -> new UnitTestBluetoothGatt(inputs.get(0), dbNotify));

        m_config.loggingOptions = LogOptions.ON;

        m_manager.setConfig(m_config);

        final BleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "NotifyTesterererer");

        final boolean[] notifies = new boolean[2];

        device.connect(e -> {
            NotifyTest.this.assertTrue(e.wasSuccess());
            BleNotify.Builder builder = new BleNotify.Builder(mTestService, mTestChar).setNotificationListener(e1 -> {
                if (e1.type() == NotificationListener.Type.ENABLING_NOTIFICATION)
                {
                    NotifyTest.this.assertTrue(e1.wasSuccess());
                    notifies[0] = true;
                }
            });
            builder.next().setCharacteristicUUID(mTest2Char).setNotificationListener(e1 -> {
                if (e1.type() == NotificationListener.Type.ENABLING_NOTIFICATION)
                {
                    NotifyTest.this.assertTrue(e1.wasSuccess());
                    NotifyTest.this.succeed();
                }
            });
            device.enableNotifies(builder.build());
        });

        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void disableNotifyTest() throws Exception
    {
        m_device = null;

        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, inputs -> new UnitTestBluetoothGatt(inputs.get(0), dbNotify));

        m_manager.setConfig(m_config);

        m_manager.setListener_Discovery(e -> {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                // Set a notification listener to avoid multiple events being emitted in the same listener (which messes up the test)
                e.device().setListener_Notification(ne -> {});
                m_device = e.device();
                m_device.connect(new BleTransaction.Init()
                {
                    @Override
                    protected void start()
                    {
                        final BleNotify notify = new BleNotify(mTestChar);
                        notify.setNotificationListener(e1 -> {
                            if (e1.type() == NotificationListener.Type.ENABLING_NOTIFICATION)
                            {
                                assertTrue("Enabling notification failed with status " + e1.status(), e1.wasSuccess());
                                assertTrue(NotifyTest.this.m_device.isNotifyEnabled(mTestChar));
                                notify.setNotificationListener(e11 -> {
                                    if (e11.type() == NotificationListener.Type.DISABLING_NOTIFICATION)
                                    {
                                        assertTrue("Disabling notification failed with status " + e11.status(), e11.wasSuccess());
                                        assertFalse(NotifyTest.this.m_device.isNotifyEnabled(mTestChar));
                                        succeed();
                                        NotifyTest.this.succeed();
                                    }
                                });
                                disableNotify(notify);
                            }
                        });
                        enableNotify(notify);
                    }
                });
            }
        });

        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device");

        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void disableNotifyNoDescriptorTest() throws Exception
    {
        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, inputs -> new UnitTestBluetoothGatt(inputs.get(0), dbNotify));

        m_manager.setConfig(m_config);

        m_manager.setListener_Discovery(e -> {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                // Set a notification listener to avoid multiple events being emitted in the same listener (which messes up the test)
                e.device().setListener_Notification(ne -> {});
                e.device().connect(new BleTransaction.Init()
                {
                    @Override
                    protected void start()
                    {
                        final BleNotify notify = new BleNotify(mTestChar);
                        notify.setNotificationListener(e1 -> {
                            if (e1.type() == NotificationListener.Type.ENABLING_NOTIFICATION)
                            {
                                assertTrue("Enabling notification failed with status " + e1.status(), e1.wasSuccess());
                                assertTrue(e1.device().isNotifyEnabled(mTestChar));
                                notify.setNotificationListener(e11 -> {
                                    if (e11.type() == NotificationListener.Type.DISABLING_NOTIFICATION)
                                    {
                                        assertTrue("Disabling notification failed with status " + e11.status(), e11.wasSuccess());
                                        assertFalse(e11.device().isNotifyEnabled(mTestChar));
                                        succeed();
                                        NotifyTest.this.succeed();
                                    }
                                });
                                disableNotify(notify);
                            }
                        });
                        enableNotify(notify);
                    }
                });
            }
        });

        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device");

        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void enableAndReceiveNotifyUsingReadWriteListenerTest() throws Exception
    {
        m_device = null;

        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, inputs -> new UnitTestBluetoothGatt(inputs.get(0), dbNotifyWithDesc));

        m_config.loggingOptions = LogOptions.ON;

        m_manager.setConfig(m_config);

        final byte[] notifyData = new byte[20];
        new Random().nextBytes(notifyData);

        m_manager.setListener_Discovery(e -> {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                m_device = e.device();
                m_device.connect(new BleTransaction.Init()
                {
                    @Override
                    protected void start()
                    {
                        m_device.setListener_Notification(e1 -> {
                            if (e1.type() == NotificationListener.Type.ENABLING_NOTIFICATION)
                            {
                                assertTrue("Enabling notification failed with status " + e1.status(), e1.wasSuccess());
                                assertTrue(NotifyTest.this.m_device.isNotifyEnabled(mTestChar));
                                succeed();
                                Util_Native.sendNotification(NotifyTest.this.m_device, e1.characteristic(), notifyData, Interval.millis(500));
                            }
                            else if (e1.type() == NotificationListener.Type.NOTIFICATION)
                            {
                                assertArrayEquals(notifyData, e1.data());
                                NotifyTest.this.succeed();
                            }
                        });
                        final BleNotify notify = new BleNotify(mTestChar);
                        enableNotify(notify);
                    }
                });
            }
        });

        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device");

        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void enableAndReceiveNotifyUsingNotificationListenerTest() throws Exception
    {
        m_device = null;

        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, inputs -> new UnitTestBluetoothGatt(inputs.get(0), dbNotifyWithDesc));

        m_config.loggingOptions = LogOptions.ON;

        m_manager.setConfig(m_config);

        final byte[] notifyData = new byte[20];
        new Random().nextBytes(notifyData);

        m_manager.setListener_Discovery(e -> {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                NotifyTest.this.m_device = e.device();
                NotifyTest.this.m_device.setListener_Notification(e1 -> {
                    if (e1.type() == NotificationListener.Type.ENABLING_NOTIFICATION)
                    {
                        NotifyTest.this.assertTrue("Enabling failed with error " + e1.status(), e1.wasSuccess());
                        NotifyTest.this.assertTrue(NotifyTest.this.m_device.isNotifyEnabled(mTestChar));
                        if (e1.wasSuccess())
                        {
                            Util_Native.sendNotification(NotifyTest.this.m_device, e1.characteristic(), notifyData, Interval.millis(500));
                        }
                    }
                    else if (e1.type() == NotificationListener.Type.NOTIFICATION)
                    {
                        NotifyTest.this.assertArrayEquals(notifyData, e1.data());
                        NotifyTest.this.succeed();
                    }
                });
                NotifyTest.this.m_device.connect(new BleTransaction.Init()
                {
                    @Override
                    protected void start()
                    {
                        BleNotify notify = new BleNotify(mTestChar);
                        enableNotify(notify);
                    }
                });
            }
        });

        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device");

        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void enableAndReceiveIndicateTest() throws Exception
    {
        m_device = null;

        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, inputs -> new UnitTestBluetoothGatt(inputs.get(0), dbIndicateNoDesc));

        m_config.loggingOptions = LogOptions.ON;

        m_manager.setConfig(m_config);

        final byte[] notifyData = new byte[20];
        new Random().nextBytes(notifyData);

        m_manager.setListener_Discovery(e -> {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                NotifyTest.this.m_device = e.device();
                NotifyTest.this.m_device.connect(new BleTransaction.Init()
                {
                    @Override
                    protected void start()
                    {
                        NotifyTest.this.m_device.setListener_Notification(e1 -> {
                            if (e1.type() == NotificationListener.Type.ENABLING_NOTIFICATION)
                            {
                                assertTrue("Enabling indication failed with status " + e1.status(), e1.wasSuccess());
                                assertTrue(NotifyTest.this.m_device.isNotifyEnabled(mTestChar));
                                succeed();
                                Util_Native.sendNotification(NotifyTest.this.m_device, e1.characteristic(), notifyData, Interval.millis(500));
                            }
                            else if (e1.type() == NotificationListener.Type.INDICATION)
                            {
                                assertArrayEquals(notifyData, e1.data());
                                NotifyTest.this.succeed();
                            }
                        });
                        BleNotify notify = new BleNotify(mTestChar);
                        enableNotify(notify);
                    }
                });
            }
        });

        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device");

        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void pseudoNotifyTest() throws Exception
    {
        m_device = null;

        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, inputs -> new PollNotifyBluetoothGatt(inputs.get(0), dbNotifyWithDesc));

        m_config.loggingOptions = LogOptions.ON;

        m_manager.setConfig(m_config);

        final byte[] notifyData = new byte[20];
        new Random().nextBytes(notifyData);

        m_manager.setListener_Discovery(e -> {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                NotifyTest.this.m_device = e.device();
                NotifyTest.this.m_device.setListener_Notification(e1 -> {
                    if (e1.type() == NotificationListener.Type.ENABLING_NOTIFICATION)
                    {
                        NotifyTest.this.assertTrue("Enabling failed with error " + e1.status(), e1.wasSuccess());
                        NotifyTest.this.assertTrue(NotifyTest.this.m_device.isNotifyEnabled(mTestChar));
                    }
                    else
                    {
                        NotifyTest.this.assertTrue(e1.type() == NotificationListener.Type.PSEUDO_NOTIFICATION);
                        NotifyTest.this.assertNotNull(e1.data());
                        NotifyTest.this.succeed();
                    }
                });
                NotifyTest.this.m_device.connect(new BleTransaction.Init()
                {
                    @Override
                    protected void start()
                    {
                        BleNotify notify = new BleNotify(mTestChar).setForceReadTimeout(Interval.secs(2.0)).setNotificationListener(e12 -> {
                            succeed(); //Succeed transaction.
                        });
                        enableNotify(notify);
                    }
                });
            }
        });

        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device");

        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void notifyStackTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;

        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, inputs -> new UnitTestBluetoothGatt(inputs.get(0), dbNotify));

        final byte[] first = Util_Unit.randomBytes(20);
        final byte[] second = Util_Unit.randomBytes(20);

        m_config.defaultInitFactory = () -> new BleTransaction.Init()
        {
            @Override
            protected void start()
            {
                BleNotify notify = new BleNotify(mTestService, mTestChar)
                        .setNotificationListener(e -> {
                            NotifyTest.this.assertTrue(e.wasSuccess());
                            succeed();
                        });
                enableNotify(notify);
            }
        };

        m_manager.setConfig(m_config);

        final BleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "NotifMotif");

        final BleCharacteristic ch = device.getNativeCharacteristic(mTestService, mTestChar, null);

        device.setListener_Notification(e -> {
            if (e.type() == NotificationListener.Type.NOTIFICATION)
            {
                NotifyTest.this.assertTrue(Arrays.equals(e.data(), first));
                device.pushListener_Notification(e1 -> {
                    NotifyTest.this.assertTrue(Arrays.equals(e1.data(), second));
                    NotifyTest.this.succeed();
                });
                Util_Native.sendNotification(device, ch, second);
            }
        });

        device.connect(e -> {
            NotifyTest.this.assertTrue(e.wasSuccess());
            Util_Native.sendNotification(device, ch, first);
        });

        startAsyncTest();
    }


    private class PollNotifyBluetoothGatt extends UnitTestBluetoothGatt
    {

        private byte m_value = 0;

        public PollNotifyBluetoothGatt(IBleDevice device, GattDatabase gattDb)
        {
            super(device, gattDb);
        }

        @Override
        public void sendReadResponse(BleCharacteristic characteristic, byte[] data)
        {
            m_value++;
            super.sendReadResponse(characteristic, new byte[]{m_value});
        }
    }

}
