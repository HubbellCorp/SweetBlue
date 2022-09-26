## Getting Started (Rx module) ##

**Recommended Step:**

Download the SweetBlue Toolbox app [here](https://play.google.com/store/apps/details?id=com.idevicesinc.sweetblue.toolbox.v3). Use the Toolbox to find the settings that work best for the device you're trying to connect to. Once you find settings that work well for you, you can export them to a json file, with which you can then import into SweetBlue (Using `new RxBleManagerConfig(jsonData)`).

**Also recommended:**

Do not use any non-Rx prefixed class, unless there is no other alternative. This mainly applies to BleManager, BleDevice, and BleServer. The Rx wrapper classes set listeners on the core classes, so using them directly can cause unintended things to happen (mainly, things will probably stop working). So don't do it.

* Modify your app's build.gradle file to pull SweetBlue down from our maven server, and add the dependency to your project:


<pre><code>
repositories {
    ...
    maven {
        url "https://artifacts.sweetblue.io"
    }
}
 
dependencies {
    ...
    implementation "com.idevicesinc:sweetblue:4.0.0"
    implementation "com.idevicesinc:sweetbluerx:4.0.0"
}
</pre></code>


* SweetBlue provides a helper class to handle all the permissions, and turning things on for you. If you decide to use this class, you must use it in an Activity, as it shows dialogs.
* The following example shows how to use the [BleSetupHelper](https://api.sweetblue.io/com/idevicesinc/sweetblue/utils/BleSetupHelper.html) class, along with starting a scan, connecting to the first device seen, then reading it's battery level.

<pre><code>
RxBleManagerConfig config = new RxBleManagerConfig();
RxBleManager manager = RxBleManager.get(this, config);
BleSetupHelper.runEnabler(manager.getBleManager(), this, result ->
{
    if (result.getSuccessful())
    {
        // Do whatever you want to do with Bluetooth now.
        scanThenConnectThenRead(manager);
    }
});
</pre></code>
<pre><code>
// This must be a class field (as opposed to a local variable) for the below to work (calling dispose()).
Disposable scanDisposable;
 
private void scanThenConnectThenRead(RxBleManager manager)
{
    ScanOptions options = new ScanOptions().scanPeriodically(Interval.TEN_SECS, Interval.ONE_SEC);
    scanDisposable = manager.scan_onlyNew(options).subscribe(rxBleDevice ->
    {
        // Stop scanning now, so we don't end up connecting to every device we see.
        // You can also just call RxBleManager.stopScan() if you don't want to hold the Disposable
        // instance in your class.
        scanDisposable.dispose();
        rxBleDevice.connect().subscribe(() ->
            {
                BleRead read = new BleRead(Uuids.BATTERY_SERVICE, Uuids.BATTERY_LEVEL);
                rxBleDevice.read(read).subscribe(readEvent ->
                {
                    if (readEvent.wasSuccess())
                        Log.i("", "Battery level is " + readEvent.data_byte() + "%");
                });
            }, throwable ->
            {
                ConnectException cex = (ConnectException) throwable;
                Log.e("MyApp", "Device failed to connect. Fail Event: " + cex.getEvent());
            }
        );
    }
}
</pre></code>

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