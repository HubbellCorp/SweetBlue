## Getting Started (Sweetunit)

With the unit test module for SweetBlue, it allows you to unit test your application using SweetBlue. All Bluetooth operations are mocked and provide best-case use out-of-the box. All operations can also be overriden to provide failure cases as well. The library requires robolectric to mock out the needed Context (you can use Mockito, or another mock library, but they are not tested, and require more work).

* Modify your app's build.gradle file to pull SweetBlue down from our maven server, and add the dependency to your project, and to allow unit tests to have access to the app's resources (so it can read the API key in the assets folder):

```groovy
android {
    testOptions {
        unitTests.includeAndroidResources = true
    }
}

repositories {
    maven {
        url "https://artifacts.sweetblue.io"
    }
}
 
dependencies {
    implementation "com.idevicesinc:sweetblue:4.0.0"
    // Make sure to only include the dependency on the test variant
    testImplementation "com.idevicesinc:sweetunit:4.0.0"
    // Robolectric is used to mock an Activity instance. It's possible to use another library for this such as Mockito,
    // but it's untested, and would require more work.
    testImplementation "org.robolectric:robolectric:4.2.1"
}
```

### Sample Unit Test ###

```java
// If you wish to mock certain SDK versions, you can set them with the Config annotation
@Config(sdk = 25)
@RunWith(RobolectricTestRunner.class)
/**
* Extend the convenience class SweetUnitTest to quickly setup a Bluetooth testing environment.
*/
public class MyTestClass extends SweetUnitTest<Activity> 
{
    // -------------
    // First, a couple of setup methods to make the test class do prep work for you
    // -------------
    
    /**
    *  Return an instance of the Activity you wish to test. This example shows using Robolectric,
    *  however, you could use something like Mockito, with a little effort.
    */
    @Override
    protected Activity createActivity()
    {
        return Robolectric.setupActivity(Activity.class);
    }
    
    /**
    *  This method should return your valid SweetBlue API Key. 
    */  
    @Override
    protected String getKey()
    {
        return "REPLACEWITHYOURSWEETBLUEAPIKEY";
    }
    
    // -------------
    // Now declare a few fields for use throughout any tests in this class
    // -------------
        
    // The UUID of the service which contains the characteristic we want to read and write
    private final static UUID myServiceUuid = Uuids.fromShort("AB01");
    
    // The UUID of the characteristic we want to read and write
    private final static UUID muCharUuid = Uuids.fromShort("AB02");
 
    // Create the database of the product under test
    private GattDatabase m_db = new GattDatabase().addService(myServiceUuid)
                                     .addCharacteristic(myCharUuid).
                                     setProperties().readWrite().
                                     setPermissions().readWrite().completeService();
 
    
    // -------------
    // We're ready to write any and all tests here. Writing a test using SweetBlue is no different than using it
    // in an app. The native bluetooth code is mocked out for you to give you happy path results (connections always
    // connect, reads/writes always succeed, bonding always succeeds, etc). If you wish to change any of this behavior,
    // you will have to override the appropriate method and provide your mocked instance. See the links in the 
    // comment on the test below
    // -------------
    
    
    /**
    *  A simple test which just writes garbage data to a characteristic, then reads it back, and verifies
    *  the data is the same.
    *  
    * @see com.idevicesinc.sweetblue.SweetUnitTest#getGattLayer(com.idevicesinc.sweetblue.internal.IBleDevice)
    * @see com.idevicesinc.sweetblue.SweetUnitTest#getDeviceLayer(com.idevicesinc.sweetblue.internal.IBleDevice) 
    * @see com.idevicesinc.sweetblue.SweetUnitTest#getManagerLayer() 
    * @see com.idevicesinc.sweetblue.SweetUnitTest#getServerLayer(com.idevicesinc.sweetblue.internal.IBleManager, com.idevicesinc.sweetblue.internal.android.P_ServerHolder) 
    */
    @Test
    public void simpleWriteAndReadTest() throws Exception
    {
        // Logging is off by default. Turning it on for this test
        m_config.loggingOptions = LogOptions.ON;
        
        // Now we can just apply mConfig to the manager instance, and logs will be turned on
        m_manager.setConfig(m_config);
        
        // Create a new BleDevice, with a random mac address, and a name
        final BleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "ABatteryPoweredDevice");
        
        // Now connect to the device, and read it's battery level
        device.connect(connectEvent -> {
            assertTrue(connectEvent.wasSuccess());
            BleRead read = new BleRead(Uuids.BATTERY_SERVICE_UUID, Uuids.BATTERY_LEVEL).setReadWriteListener(readEvent -> {
                assertTrue(readEvent.wasSuccess());
                assertNotNull(readEvent.data());
                succeed();
            });
            device.read(read);
        });
        
        startAsyncTest();
    }
    
    /**
    *  Here we use an alternate constructor for UnitTestBluetoothGatt, which allows us to set the GattDatabase.
    *  Note: You can also call UnitTestBluetoothGatt.setDatabase if you already have an instance of the
    *  mocked gatt (UnitTestBluetoothGatt)
    */
    @Override
    public IBluetoothGatt getGattLayer(IBleDevice device)
    {
        return new UnitTestBluetoothGatt(device, db);
    }
 
 
}
```