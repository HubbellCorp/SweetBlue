## Getting Started ##

**Recommended Step:**

Use the Toolbox to find the settings that work best for the device you're trying to connect to. Once you find settings that work well for you, you can export them to a json file, with which you can then import into SweetBlue (Using `new BleManagerConfig(jsonData)`).

* Modify your app's build.gradle file to pull SweetBlue down from our maven server, and add the dependency to your project:

<pre>
<code>
repositories {
    ...
    maven {
        url "https://artifacts.sweetblue.io"
    }
}

dependencies {
    ...
    implementation "com.idevicesinc:sweetblue:4.0.0.5-SNAPSHOT"
}
</pre>
</code>


* SweetBlue provides a helper class to handle all the permissions, and turning things on for you. If you decide to use this class, you must use it in an Activity, as it shows dialogs.
* The following example shows how to use the [BleSetupHelper](https://api.sweetblue.io/com/idevicesinc/sweetblue/utils/BleSetupHelper.html) class, along with starting a scan, connecting to the first device seen, then reading it's battery level.

<pre>
<code>
BleManagerConfig config = new BleManagerConfig();
BleManager manager = BleManager.get(this, config);
BleSetupHelper.runEnabler(manager, this, result -> 
{
    if (result.getSuccessful()) 
    {
        // Do whatever you want to do with Bluetooth now.
        scanThenConnectThenRead(manager);
    }
});
</pre>
</code>
<pre>
<code>
private void scanThenConnectThenRead(BleManager manager)
{
    // Set a discovery listener to be notified when a device is discovered
    manager.setListener(discoveryEvent ->
    {
        if (discoveryEvent.was(LifeCycle.DISCOVERED)
        {
            // Stop scanning now, so we don't end up connecting to every device we see.
            manager.stopScan();
            discoveryEvent.device().connect(connectEvent ->
            {
                if (connectEvent.wasSuccess())
                {
                    BleRead read = new BleRead(Uuids.BATTERY_SERVICE, Uuids.BATTERY_LEVEL);
                    read.setReadWriteListener(readEvent -> 
                    {
                        if (readEvent.wasSuccess())
                            Log.i("", "Battery level is " + readEvent.data_byte() + "%");
                    });
                    connectEvent.device().read(read);
                }
                else
                {
                    if (!connectEvent.isRetrying())
                    {
                        Log.e("MyApp", "Device failed to connect. Fail Event: " + connectEvent.failEvent());
                    }
                }
            }
        }
    }
    // Start a scan
    manager.startScan();
}
</pre>
</code>


### Proguard ###

If you're using proguard to optimize/obfuscate your app, then we suggest you add the following lines to your proguard file.

<pre>
<code>
-keepattributes SourceFile,LineNumberTable
-keep class com.idevicesinc.sweetblue.\*\*                                    { \*; }
-dontwarn com.idevicesinc.sweetblue.\*\*
</pre>
</code>

The only one of the above lines which is even near required is the last line to stop warnings. The other 2 lines are to make
debugging of crash logs easier for us if/when you report them to us.