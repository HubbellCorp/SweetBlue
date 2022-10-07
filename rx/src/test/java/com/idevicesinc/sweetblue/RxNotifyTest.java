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
import com.idevicesinc.sweetblue.rx.RxBleTransaction;
import com.idevicesinc.sweetblue.utils.GattDatabase;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Util_Unit;
import com.idevicesinc.sweetblue.utils.Uuids;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.Random;
import java.util.UUID;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public final class RxNotifyTest extends RxBaseBleUnitTest
{

    private static final UUID mTestService = Uuids.fromShort("12BA");
    private static final UUID mTestChar = Uuids.fromShort("12BC");
    private static final UUID mTest2Char = Uuids.fromShort("12BD");
    private static final UUID mTestDesc = Uuids.CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR_UUID;


    private RxBleDevice m_device;
    private final GattDatabase dbNotifyWithDesc = new GattDatabase().addService(mTestService)
            .addCharacteristic(mTestChar).setProperties().readWriteNotify().setPermissions().read().build()
            .addDescriptor(mTestDesc).setPermissions().read().completeService();
    private final GattDatabase dbNotify = new GattDatabase().addService(mTestService)
            .addCharacteristic(mTestChar).setProperties().readWriteNotify().setPermissions().read().completeChar()
            .addCharacteristic(mTest2Char).setProperties().readWriteNotify().setPermissions().read().completeService();
    private final GattDatabase dbIndicateNoDesc = new GattDatabase().addService(mTestService)
            .addCharacteristic(mTestChar).setProperties().readWriteIndicate().setPermissions().read().completeService();


    @Test(timeout = 15000)
    public void enableNotifyTest() throws Exception
    {
        m_device = null;

        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, args -> new UnitTestBluetoothGatt(args.get(0), dbNotifyWithDesc));

        m_config.loggingOptions = LogOptions.ON;

        m_manager.setConfig(m_config);

        m_disposables.add(m_manager.observeDiscoveryEvents().subscribe(e ->
        {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                m_device = e.device();
                m_disposables.add(m_device.connect(new RxBleTransaction.RxInit()
                {
                    @Override
                    protected void start()
                    {
                        BleNotify notify = new BleNotify(mTestChar);
                        m_disposables.add(m_device.enableNotify(notify).subscribe((e1) ->
                        {
                            if (e1.type() == NotificationListener.Type.ENABLING_NOTIFICATION)
                            {
                                assertTrue("Enabling notification failed with status " + e1.status(), e1.wasSuccess());
                                assertTrue(m_device.isNotifyEnabled(mTestChar));
                                succeed();
                                RxNotifyTest.this.succeed();
                            }
                        }, throwable -> {}));
                    }
                }).subscribe(() -> {}, throwable -> {}));
            }
        }));

        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device");

        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void enableNotifyNoDescriptorTest() throws Exception
    {
        m_device = null;

        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, args -> new UnitTestBluetoothGatt(args.get(0), dbNotify));

        m_config.loggingOptions = LogOptions.ON;

        m_manager.setConfig(m_config);

        m_disposables.add(m_manager.observeDiscoveryEvents().subscribe(e ->
        {
            if (e.wasDiscovered())
            {
                m_device = e.device();
                m_disposables.add(m_device.connect(new RxBleTransaction.RxInit()
                {
                    @Override
                    protected void start()
                    {
                        BleNotify notify = new BleNotify(mTestChar);
                        m_disposables.add(m_device.enableNotify(notify).subscribe((e1) ->
                        {
                            if (e1.type() == NotificationListener.Type.ENABLING_NOTIFICATION)
                            {
                                assertTrue("Enabling notification failed with status " + e1.status(), e1.wasSuccess());
                                assertTrue(m_device.isNotifyEnabled(mTestChar));
                                succeed();
                                RxNotifyTest.this.succeed();
                            }
                        }, throwable -> {}));
                    }
                }).subscribe(() -> {}, throwable -> {}));
            }
        }));

        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device");

        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void multiNotifyTest() throws Exception
    {
        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, args -> new UnitTestBluetoothGatt(args.get(0), dbNotify));

        m_config.loggingOptions = LogOptions.ON;

        m_manager.setConfig(m_config);

        RxBleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "NotifyTesterererer");

        final boolean[] notifies = new boolean[2];

        m_disposables.add(device.connect().subscribe(() ->
        {
            BleNotify.Builder builder = new BleNotify.Builder(mTestService, mTestChar);
            builder.next().setCharacteristicUUID(mTest2Char);

            m_disposables.add(device.enableNotifies(builder.build()).subscribe((e1) ->
            {
                if (e1.type() == NotificationListener.Type.ENABLING_NOTIFICATION && e1.isFor(mTestChar))
                {
                    assertTrue(e1.wasSuccess());
                    notifies[0] = true;
                }
                else if (e1.type() == NotificationListener.Type.ENABLING_NOTIFICATION && e1.isFor(mTest2Char))
                {
                    assertTrue(e1.wasSuccess());
                    succeed();
                }
            }, throwable -> {}));
        }));

        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void disableNotifyTest() throws Exception
    {
        m_device = null;

        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, args -> new UnitTestBluetoothGatt(args.get(0), dbNotify));

        m_manager.setConfig(m_config);

        m_disposables.add(m_manager.observeDiscoveryEvents().subscribe(e ->
        {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                m_device = e.device();
                m_disposables.add(m_device.connect(new RxBleTransaction.RxInit()
                {
                    @Override
                    protected void start()
                    {
                        final BleNotify notify = new BleNotify(mTestChar);
                        m_disposables.add(m_device.enableNotify(notify).subscribe(e1 ->
                        {
                            if (e1.type() == NotificationListener.Type.ENABLING_NOTIFICATION)
                            {
                                assertTrue("Enabling notification failed with status " + e1.status(), e1.wasSuccess());
                                assertTrue(m_device.isNotifyEnabled(mTestChar));
                                m_disposables.add(m_device.disableNotify(notify).subscribe((e11) ->
                                {
                                    if (e11.type() == NotificationListener.Type.DISABLING_NOTIFICATION)
                                    {
                                        assertTrue("Disabling notification failed with status " + e11.status(), e11.wasSuccess());
                                        assertFalse(m_device.isNotifyEnabled(mTestChar));
                                        RxNotifyTest.this.succeed();
                                    }
                                }, throwable -> {}));
                            }
                        }, throwable -> {}));
                    }
                }).subscribe(() -> {}, throwable -> {}));
            }
        }));

        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device");

        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void disableNotifyNoDescriptorTest() throws Exception
    {
        m_device = null;

        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, args -> new UnitTestBluetoothGatt(args.get(0), dbNotify));

        m_manager.setConfig(m_config);

        m_disposables.add(m_manager.observeDiscoveryEvents().subscribe(e ->
        {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                m_device = e.device();
                m_disposables.add(m_device.connect(new RxBleTransaction.RxInit()
                {
                    @Override
                    protected void start()
                    {
                        final BleNotify notify = new BleNotify(mTestChar);
                        m_disposables.add(m_device.observeNotifyEvents().subscribe(e1 ->
                        {
                            if (e1.type() == NotificationListener.Type.ENABLING_NOTIFICATION)
                            {
                                assertTrue("Enabling notification failed with status " + e1.status(), e1.wasSuccess());
                                assertTrue(m_device.isNotifyEnabled(mTestChar));
                                m_disposables.add(m_device.disableNotify(notify).subscribe(e11 ->
                                {
                                    if (e11.type() == NotificationListener.Type.DISABLING_NOTIFICATION)
                                    {
                                        assertTrue("Disabling notification failed with status " + e11.status(), e11.wasSuccess());
                                        assertFalse(m_device.isNotifyEnabled(mTestChar));
                                        RxNotifyTest.this.succeed();
                                    }
                                }, throwable -> {}));
                            }
                        }));
                        m_disposables.add(m_device.enableNotify(notify).subscribe(e1 -> {}, throwable -> {}));
                    }
                }).subscribe(() -> {}, throwable -> {}));
            }
        }));

        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device");

        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void enableAndReceiveNotifyUsingReadWriteListenerTest() throws Exception
    {
        m_device = null;

        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, args -> new UnitTestBluetoothGatt(args.get(0), dbNotifyWithDesc));

        m_config.loggingOptions = LogOptions.ON;

        m_manager.setConfig(m_config);

        final byte[] notifyData = new byte[20];
        new Random().nextBytes(notifyData);

        m_disposables.add(m_manager.observeDiscoveryEvents().subscribe(e ->
        {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                m_device = e.device();
                m_disposables.add(m_device.connect(new RxBleTransaction.RxInit()
                {
                    @Override
                    protected void start()
                    {
                        final BleNotify notify = new BleNotify(mTestChar);
                        m_disposables.add(m_device.observeNotifyEvents().subscribe(e1 ->
                        {
                            if (e1.type() == NotificationListener.Type.ENABLING_NOTIFICATION)
                            {
                                assertTrue("Enabling notification failed with status " + e1.status(), e1.wasSuccess());
                                assertTrue(m_device.isNotifyEnabled(mTestChar));
                                succeed();
                                Util_Native.sendNotification(m_device.getBleDevice(), e1.characteristic(), notifyData, Interval.millis(500));
                            }
                            else if (e1.type() == NotificationListener.Type.NOTIFICATION)
                            {
                                assertArrayEquals(notifyData, e1.data());
                                RxNotifyTest.this.succeed();
                            }
                        }));
                        m_disposables.add(m_device.observeNotifyEvents().subscribe(e1 -> {}));
                        m_disposables.add(m_device.enableNotify(notify).subscribe(e1 -> {}, throwable -> {}));
                    }
                }).subscribe(() -> {}, throwable -> {}));
            }
        }));

        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device");

        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void enableAndReceiveNotifyUsingNotificationListenerTest() throws Exception
    {
        m_device = null;

        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, args -> new UnitTestBluetoothGatt(args.get(0), dbNotifyWithDesc));

        m_config.loggingOptions = LogOptions.ON;

        m_manager.setConfig(m_config);

        final byte[] notifyData = new byte[20];
        new Random().nextBytes(notifyData);

        m_disposables.add(m_manager.observeDiscoveryEvents().subscribe(e ->
        {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                m_device = e.device();
                m_disposables.add(m_device.observeNotifyEvents().subscribe(e1 ->
                {
                    if (e1.type() == NotificationListener.Type.ENABLING_NOTIFICATION)
                    {
                        assertTrue("Enabling failed with error " + e1.status(), e1.wasSuccess());
                        assertTrue(m_device.isNotifyEnabled(mTestChar));
                        if (e1.wasSuccess())
                        {
                            Util_Native.sendNotification(m_device.getBleDevice(), e1.characteristic(), notifyData, Interval.millis(500));
                        }
                    }
                    else if (e1.type() == NotificationListener.Type.NOTIFICATION)
                    {
                        assertArrayEquals(notifyData, e1.data());
                        succeed();
                    }
                }, throwable -> {}));

                m_disposables.add(m_device.connect(new RxBleTransaction.RxInit()
                {
                    @Override
                    protected void start()
                    {
                        BleNotify notify = new BleNotify(mTestChar);
                        m_disposables.add(m_device.enableNotify(notify).subscribe(e12 ->
                        {
                            if (e12.type() == NotificationListener.Type.ENABLING_NOTIFICATION)
                            {
//                                assertTrue("Enabling failed with error " + e12.status(), e12.wasSuccess());
//                                assertTrue(m_device.isNotifyEnabled(mTestChar));
//                                succeed();
                            }
                        }, throwable -> {}));
                    }
                }).subscribe(() -> {}, throwable -> {}));
            }
        }));

        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device");

        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void enableAndReceiveIndicateTest() throws Exception
    {
        m_device = null;

        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, args -> new UnitTestBluetoothGatt(args.get(0), dbIndicateNoDesc));

        m_config.loggingOptions = LogOptions.ON;

        m_manager.setConfig(m_config);

        final byte[] notifyData = new byte[20];
        new Random().nextBytes(notifyData);

        m_disposables.add(m_manager.observeDiscoveryEvents().subscribe(e ->
        {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                m_device = e.device();
                m_disposables.add(m_device.observeNotifyEvents().subscribe(e1 ->
                {
                    if (e1.type() == NotificationListener.Type.ENABLING_NOTIFICATION)
                    {
                        assertTrue("Enabling indication failed with status " + e1.status(), e1.wasSuccess());
                        assertTrue(m_device.isNotifyEnabled(mTestChar));
                        succeed();
                        Util_Native.sendNotification(m_device.getBleDevice(), e1.characteristic(), notifyData, Interval.millis(500));
                    }
                    else if (e1.type() == NotificationListener.Type.INDICATION)
                    {
                        assertArrayEquals(notifyData, e1.data());
                        RxNotifyTest.this.succeed();
                    }
                }));
                m_disposables.add(m_device.connect(new RxBleTransaction.RxInit()
                {
                    @Override
                    protected void start()
                    {
                        BleNotify notify = new BleNotify(mTestChar);
                        m_disposables.add(m_device.enableNotify(notify).subscribe(e -> {}, throwable -> {}));
                    }
                }).subscribe(() -> {}, throwable -> {}));
            }
        }));

        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device");

        startAsyncTest();
    }

    @Test(timeout = 15000)
    public void pseudoNotifyTest() throws Exception
    {
        m_device = null;

        SweetDIManager.getInstance().registerTransient(IBluetoothGatt.class, args -> new PollNotifyBluetoothGatt(args.get(0), dbNotifyWithDesc));

        m_config.loggingOptions = LogOptions.ON;

        m_manager.setConfig(m_config);

        final byte[] notifyData = new byte[20];
        new Random().nextBytes(notifyData);

        m_disposables.add(m_manager.observeDiscoveryEvents().subscribe(e ->
        {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                m_device = e.device();
                m_disposables.add(m_device.observeNotifyEvents().subscribe(e1 ->
                {
                    if (e1.type() == NotificationListener.Type.ENABLING_NOTIFICATION)
                    {
                        assertTrue("Enabling failed with error " + e1.status(), e1.wasSuccess());
                        assertTrue(m_device.isNotifyEnabled(mTestChar));
                    }
                    else
                    {
                        assertTrue(e1.type() == NotificationListener.Type.PSEUDO_NOTIFICATION);
                        assertNotNull(e1.data());
                        succeed();
                    }
                }));
                m_disposables.add(m_device.connect(new RxBleTransaction.RxInit()
                {
                    @Override
                    protected void start()
                    {
                        BleNotify notify = new BleNotify(mTestChar).setForceReadTimeout(Interval.secs(2.0));
                        m_disposables.add(m_device.enableNotify(notify).subscribe(e ->
                        {
                            succeed(); // Succeed the transaction
                        }, throwable -> {}));
                    }
                }).subscribe(() -> {}, throwable -> {}));
            }
        }));

        m_manager.newDevice(Util_Unit.randomMacAddress(), "Test Device");

        startAsyncTest();
    }



    private static class PollNotifyBluetoothGatt extends UnitTestBluetoothGatt
    {

        private byte m_value = 0;

        public PollNotifyBluetoothGatt(IBleDevice device, GattDatabase gattDb)
        {
            super(device, gattDb);
        }

        @Override public void sendReadResponse(BleCharacteristic characteristic, byte[] data)
        {
            m_value++;
            super.sendReadResponse(characteristic, new byte[] { m_value });
        }
    }

}
