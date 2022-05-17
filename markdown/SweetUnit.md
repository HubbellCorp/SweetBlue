# SweetUnit Testing Module #

SweetUnit allows you to unit test your BleDevices in your app. The module provides pre-built configurations for "happy path" tests. It is up to you to implement failure conditions.

**Prerequisite**
The module was built with the assumption that [Robolectric](http://robolectric.org) would be used. See
[Getting-Started-Sweetunit](Getting-Started_Sweetunit)

It is recommended to create a base test class from which all of your test classes will extend.

```java

// Parameterize the Test class with the Activity you want to test against
@RunWith(RobolectricTestRunner.class)
public class MyBaseTest extends SweetUnitTest<MyActivity>
{

    static final String KEY = "YOUR_API_KEY_HERE";


    @Override
    protected Activity createActivity()
    {
        return Robolectric.setupActivity(MyActivity.class);
    }

    @Override
    protected String getKey()
    {
        return KEY;
    }

}

```


And now an example of reading the battery level of a device

```java


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class BatteryTest extends MyBaseTest
{

    // The database of the device we're going to interact with.
    GattDatabase db = new GattDatabase()
            .addService(Uuids.BATTERY_SERVICE_UUID)
            .addCharacteristic(Uuids.BATTERY_LEVEL)
            .setValue(new byte[] { (byte) 100 })
            .setPermissions().read()
            .setProperties().read()
            .completeService();



    @Test
    public void readBatteryTest() throws Exception
    {
        // Logging is off by default. Turning it on for this test
        mConfig.loggingOptions = LogOptions.ON;

        // Now we can just apply mConfig to the manager instance, and logs will be turned on
        mManager.setConfig(mConfig);

        // Create a new BleDevice, with a random mac address, and a name
        final BleDevice device = m_mgr.newDevice(Util_Unit.randomMacAddress(), "ABatteryPoweredDevice");

        // Now connect to the device, and read it's battery level
        device.connect(connectEvent -> {
            assertTrue(connectEvent.wasSuccess());
            BleRead read = new BleRead(Uuids.BATTERY_SERVICE_UUID, Uuids.BATTERY_LEVEL).setReadWriteListener(readEvent -> {
                assertTrue(readEvent.wasSuccess());
                assertNotNull(readEvent.data());
                // You must call succeed when the test should be ended successfully. If any of the assert methods throw an
                // exception, it will end the test.
                succeed();
            });
            device.read(read);
        });

        // This method blocks the test thread (different from the UI thread). You MUST call succeed() when the test is to exit after
        // a success.
        startAsyncTest();
    }

}
```