


# Scanning #

There are many ways to perform a scan, not all are available on all OS versions of Android. In a case where a scan method is called on a device that doesn't support it, SweetBlue will automatically fallback to a supported option **(with the exception of a pending intent scan)**.

Just running a scan while your app is in the foreground is easy, once you've requested all necessary permissions (see [BleSetupHelper doc](BleSetupHelper) for more info).

<pre>
<code>import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.DiscoveryListener.LifeCycle;

{
   ...
   BleManagerConfig config = new BleManagerConfig();
   BleManager manager = BleManager.get(this, config);
   manager.setListener_Discovery(this::onBleDeviceDiscovered);
   ScanOptions scanOptions = new ScanOptions();
   manager.startScan(scanOptions);
}

public void onBleDeviceDiscovered(DiscoveryListener.DiscoveryEvent event)
{
   BleDevice device = event.device();
   switch(event.lifeCycle())
   {
      case LifeCycle.DISCOVERED:
         // Device was newly discovered. An android BLE scan will return the same
         // devices multiple times. Looking for the DISCOVERED lifecycle ensures you only
         // react to a device discovery once.
      case LifeCycle.REDISCOVERED:
         // If you want to do something each time the device is discovered in a scan, you can
         // do so here.
      case LifeCycle.UNDISCOVERED:
         // Undiscovery is something that is built into SweetBlue, but not on by default. This is
         // because it's arbitrary, and based on not seeing a device within a certain amount of
         // time of scanning.
   }
}
</code>
</pre>

## Background Scanning on Android 10+ ##

Android 10 and up require requesting background location for BLE scans to work when your app is in the background. You will need to add the following to your app's AndroidManifest.xml file:
<pre>
<code>&lt;uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" /&gt;
</code>
</pre>

Also, please see [BleSetupHelper doc](BleSetupHelper) for how to request the permission (it's not on by default).

## Scanning APIs ##

In Lollipop, a new scanning API was introduced. It was found that you could get different results with the 2 APIs, with some devices seemingly performing better with the old API, and some performing better with the new API. SweetBlue by default, will switch between the 2 different APIs, if possible (on devices running an OS lower than Lollipop, it will only run the 1 available API).
Another issue with scanning is that if you just keep a scan running, after about 10 seconds or so (again, depends on the device, and OS level), you stop getting results. We've found it's better to scan in pulses (scan for an amount of time, then stop for a short period, and resume again). For these reasons, by default, SweetBlue will scan for 10 seconds, then rest for half a second (these defaults can be changed via [infiniteScanInterval](https://api.sweetblue.io/com/idevicesinc/sweetblue/BleManagerConfig.html#infiniteScanInterval) and [infinitePauseInterval](https://api.sweetblue.io/com/idevicesinc/sweetblue/BleManagerConfig.html#infinitePauseInterval)). You can always force a true indefinite scan, if you want, just know that you're very likely to stop getting results after about 10 seconds or so.
You can also choose to do a Bluetooth Classic scan, though you won't get any extra data (scan record). SweetBlue falls back to a classic scan if the other APIs aren't available.
No matter which API you use to scan for Bluetooth devices, listening for them is the same (using a [DiscoveryListener](https://api.sweetblue.io/com/idevicesinc/sweetblue/DiscoveryListener.html)). Of course, there's always an exception (see the PendingIntent Scan section in this document).


## PendingIntent Scan ##

This type of scan is very useful if you need to have a scan running pretty much at all times, including when your app is not running. This will wake up your app when the android device has found bluetooth device(s) in range.

**PLEASE NOTE THAT THIS TYPE OF SCAN IS ONLY AVAILABLE IN ANDROID 8 AND UP**

To use this type of scan, it will require a bit more work than a typical scan.

First, you need to create a class which extends BroadcastReceiver.

<pre>
<code>public class MyScanReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        // Get the instance of BleManager
        BleManager mgr = BleManager.get(context);
        // SweetBlue provides a convenience method to convert the Intent instance to a list of BleDevice instances.
        List<BleDevice> deviceList = mgr.getDevices(intent);
        // Now do what you need to do with the discovered devices
    }
}
</code>
</pre>

This receiver class must also be registered within your AndroidManifest.xml file like so:

<pre>
<code>&lt;application&gt;
    &lt;receiver android:name="com.myapp.MyScanReceiver" &gt;
        &lt;intent-filter&gt;
            &lt;action android:name="com.myapp.ACTION_FOUND" /&gt;
        &lt;/intent-filter&gt;
    &lt;/receiver&gt;
&lt;/application&gt;
</code>
</pre>


You will need a PendingIntent instance to start and stop the scan. You can create it like so:

<pre>
<code>PendingIntent pIntent = PendingIntent.getBroadcast(activity, 42, new Intent(activity, MyScanReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);
</code>
</pre>

NOTE: You will need this PendingIntent instance to stop the scan.
NOTE 2: Using this type of scan will leave a scan task in SweetBlue's task queue. This won't affect other operations, it's there basically so the system knows a "scan" is in progress.

Now, to execute the scan, just do the following:

<pre>
<code>ScanOptions options = new ScanOptions();
options.withPendingIntent(pIntent);
// You may want to also set the defaultNativeScanFilterList option in BleManagerConfig to have android filter the
// devices for you, as SweetBlue won't have access to the devices to filter them for you.
bleManager.startScan(options);
</code>
</pre>

To stop the scan, simply do:
<pre>
<code>bleManager.stopScan(pIntent);
</code>
</pre>