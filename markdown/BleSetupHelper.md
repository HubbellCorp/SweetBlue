

# BleSetupHelper #

The [BleSetupHelper](https://api.sweetblue.io/com/idevicesinc/sweetblue/utils/BleSetupHelper.html) class is a convenience class to effortlessly handle getting the necessary permissions for BLE scans to work on Android 6 and up. This can be quite an annoying experience, and we've chewed this piece of annoyance up for you. Of course, you can always ignore it, and roll your own implementation.

The helper will show dialogs to your user explaining what's needed and being requested. There's even pre formatted text for each dialog, which has multiple languages available.

## How many languages does the helper come with? ##

18\. English is the default, which is hardcoded (if you use the jar, this is all you will get without implementing your own), and 17 additional language codes.

The language codes are:

1. English (en) [default]
1. Czech (cs)
1. Danish (da)
1. German (de)
1. Spanish (es)
1. French (fr)
1. French - Canada (fr-rCA)
1. Italian (it)
1. Japanese (ja)
1. Korean (ko)
1. Dutch (nl)
1. Norwegian nn
1. Polish (pl)
1. Portuguese - Brazil (pt-rBR)
1. Russian (ru)
1. Swedish (sv)
1. Chinese (China) zh-rCN
1. Chinese (Taiwan) zh-rTW


## What exactly is needed to successfully run a BLE scan on Android Marshmallow (6.0) and higher? ##

**Common requirements:**
1. Bluetooth must be on

**For Android 6-11:**

2. Location services permission
    a. You must declare `android.permission.ACCESS_COARSE_LOCATION` in your manifest file _(for Android Q and beyond, you will have to use `android.permission.ACCESS_FINE_LOCATION`)_
    b. You must request user permission at runtime
3. Location services must be on

**For Android 12+:**

* Location services and permissions are still needed, unless strongly asserted that location is not used
4. Bluetooth (scan, connect, and advertise) permission
    a. You must declare `android.permission.BLUETOOTH_SCAN` and `android.permission.BLUETOOTH_CONNECT` in your manifest file
    b. If you plan to have the android device advertise itself, you also need to add `android.permission.BLUETOOTH_ADVERTISE` to the manifest file
    b. You must request these permissions at runtime

The helper will turn the bluetooth radio on, if it is not already. Next, it will request location permission, if they have not been granted. Then it will turn on location services, if they are not on. Then it will request the android 12 permissions. Finally, any custom permissions are handled.

**Android 12 Location Note**
* If your app does not need location services/permissions for any reason (even outside of BLE), then you can set the option `BleManagerConfig.doNotRequestLocation` to `true`. If you do this, you must also make sure to strongly assert you do not need location in the bluetooth permissions:
```xml
<uses-permission android:name="android.permission.BLUETOOTH_SCAN"
        android:usesPermissionFlags="neverForLocation" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT"
    android:usesPermissionFlags="neverForLocation"/>
```


## Simple Example Usage ##

```java
BleManagerConfig config = new BleManagerConfig();
BleManager manager = BleManager.get(this, config);
BleSetupHelper.runEnabler(manager, this, result ->
{
    if (result.getSuccessful())
    {
        // Do whatever you want to do with Bluetooth now.
        scanThenConnectThenRead(manager);
    }
    else
    {
        // You may want to show something to the user here to explain
        // that bluetooth scanning will not work without all the requested
        // permissions and services are turned on
    }
});
```

## Can I use the helper class to handle my own custom permissions? ##

Yes! The helper was built with the idea that if you wanted to roll extra permissions into the flow, it would be easy to do.

### Custom permission example ###

```java

@Override
protected void onCreate(Bundle savedInstanceState)
{
   ...
   BleManagerConfig config = new BleManagerConfig();
   BleManager mgr = BleManager.get(this, config);
   BleSetupHelper helper = new BleSetupHelper(mgr, this, result ->
   {
      if (result.getSuccessful())
         // TODO BLE is fully enabled, and all permissions including custom are granted
  });
   helper.addRequiredPermissions();
   helper.addCustomPermission("My custom permission");
   helper.setImpl(new MyPermissionHelper());
   helper.start();
}


private static class MyPermissionHelper extends BleSetupHelper.BluetoothEnablerImpl
{
  boolean m_permissionGranted = false;

  @Override
  public void requestCustomPermission(Object metadata)
  {   // TODO - Implement code to actually request the permission
  m_permissionGranted = true;
  }

  @Override
  public boolean checkIsCustomPermissionEnabled(Object metadata)
  {   // TODO - Actually check if the permission has been granted
 return m_permissionGranted;
  }

  @Override
  public boolean checkIsCustomPermissionRequired(Object metadata)
  {   // We're just going to say that this permission is always required
     return true;
  }
}
```

## What about background scanning on Android 10+? ##
Android 10 brought about more permission changes, in that if you would like your app to actually get any results from a BLE scan while in the background, you have to request background location. This requires adding this to your app's AndroidManifest.xml file:
```java
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
```
You should also set the option [requestBackgroundOperation](https://api.sweetblue.io/com/idevicesinc/sweetblue/BleManagerConfig.html#requestBackgroundOperation) to true before running the BleSetupHelper:

```java
BleManagerConfig config = new BleManagerConfig();
config.requestBackgroundOperation = true;
BleManager mgr = BleManager.get(this, config);
BleSetupHelper.runEnabler(mgr, this, result ->
{
	if  (result.getSuccessful())
	    // Permissions have all been granted.
};
```