# This is a short list of issues you will encounter if you try to use the native Android BLE stack directly… #

## Android 10 Issues ##
* In order to continue background scanning on Android 10, you need to add the new permission "android.permission.ACCESS_BACKGROUND_LOCATION" to your AndroidManifest.xml file. This permission will not be added automatically like most others because of this [Google Play Location Policy](https://play.google.com/about/privacy-security-deception/permissions/#!?zippy_activeEl=location-permissions#location-permissions). You also will have to set [BleManagerConfig.requestBackgroundOperation](https://api.sweetblue.io/com/idevicesinc/sweetblue/BleManagerConfig.html#requestBackgroundOperation) to true (if using [BleSetupHelper](https://api.sweetblue.io/com/idevicesinc/sweetblue/utils/BleSetupHelper.html))

## Android 8.1 Issues ##
* Android 8.1 brought about an undocumented "feature" for battery saving. If you have a scan running without their built-in ScanFilter class, you will not get any scan results if the screen is off (regardless if you have a foreground service running). SweetBlue has bypassed using the built-in ScanFilter class, as originally we found it to not work very well, and was very restrictive.
* Bug report: https://issuetracker.google.com/issues/70619940 (Marked won't fix)
* See this commit: https://android.googlesource.com/platform/packages/apps/Bluetooth/+/319aeae6f4ebd13678b4f77375d1804978c4a1e1
* We've created a feature request to allow for wildcards in the built-in ScanFilter class, to allow for more flexible scanning. https://issuetracker.google.com/issues/77544858. Unfortunately, Google closed it as working as designed.
* We have added a hack to get around this problem. However, it's possible the hack will stop working in future OS releases.
* An option was added to the BleManagerConfig to allow setting android's native ScanFilter class, in the case that you have to get scan results when the screen is off on 8.1+ devices (also requires the scan to be run from a foreground service). See [BleManagerConfig.defaultNativeScanFilterList](https://api.sweetblue.io/com/idevicesinc/sweetblue/BleManagerConfig.html#defaultNativeScanFilterList)

## Android M (6) Issues ##
* Every major OS release introduces new challenges with implementing BLE but M is particularly problematic...
* Low-Energy scanning only returns results if (A) Location permissions are in your manifest, (B) new so-called "runtime permissions" are enabled (https://developer.android.com/training/permissions/index.html), and (C) Location services are on. (B) is a new requirement in Android M that forces a user to confirm "dangerous" permissions granted by (A). If (A) and (B) are fulfilled but (C) is not, SweetBlue falls back to classic discovery mode, which only returns device name and mac address as opposed to full advertising packets, but is better than nothing (for example if your device's name never changes and is somewhat unique, it may be enough to filter on - alternatively if you somehow know all the possible mac addresses of your devices or some pattern you could filter on that too). If either (A) or (B) are not enabled no scanning will work at all. As for why location is needed, technically your location could be ascertained by receiving advertising packets from bluetooth devices with known geographic locations. See [BleManager.startScan()](https://api.sweetblue.io/com/idevicesinc/sweetblue/BleManager.html#startScan--) for more details. Anyway, [BleSetupHelper](https://api.sweetblue.io/com/idevicesinc/sweetblue/utils/BleSetupHelper.html) guides you effortlessly through this painful process of making sure everything's enabled.
* BluetoothAdapter now has 3 private states in addition to the public states STATE_ON, STATE_TURNING_OFF, STATE_OFF, and STATE_TURNING_ON. They are called STATE_BLE_TURNING_ON, STATE_BLE_ON, and STATE_BLE_TURNING_OFF. You might think they are private so not a problem, don't have to worry about it. Wrong. These states can randomly slip in through your BroadcastReceiver listening for BluetoothAdapter.ACTION_STATE_CHANGED so you must be aware of them. The further problem is the random part. So in addition to keying off BroadcastReceiver, you must manually poll BluetoothAdapter.getLeState() (which is a private method so you have to use reflection) in order to normalize the order and timing for these hidden state transitions. Needless to say, there are a lot of tricky edge cases here.

## Connection Issues ##
* Random disconnects happen. They happen a lot. In some situations you can expect a disconnect every few minutes regardless of signal strength. You need to have very robust reconnect logic for user experience not to suffer.
* When connecting to a device you must pass a mysterious Boolean flag called "autoConnect". For some phone and ble device combinations this Boolean has to be true, for others false. For others true is more consistently successful but slower to connect. For robust connection flow you have to basically experiment at runtime, on the fly, trying different combinations until you get successful connection, and then remember that setting for subsequent connection attempts.
* If you end up using "autoConnect" set to true you have to be careful because the native stack can very aggressively try to connect forever after, even forgoing being discovered first.
* Even in the best of conditions connecting and getting services can both fail on the first try but succeed without issue the second or third time around. For the best user experience you have to transparently retry each one several times. It’s sort of like starting a lawn mower…sometimes you just need to crank it a few times.
* You can get a connection success callback but device isn’t actually connected, and vice versa for disconnect.
* All state (connection status, bond status, ble stack status) tracked by the underlying stack can be stale or otherwise inaccurate at any point in time. Frequently you’ll get the callback for a particular state change but that state isn’t reflected when querying the object in your callback method. You have to maintain parallel state yourself and sometimes just take a best guess of what the actual native state is. These are unintentional race conditions but you’re left to decide who the winner is.
* Native "reconnect" method doesn’t really work at all on any device for any real-world scenario. Have to manage your own reconnect loop and your own CPU wake-lock.
* On the first bond to a device, android leaves the connection open, although the reported state tells you it's disconnected (verified via ble sniffer, and other methods). We've added an UhOh to detect when this happens - UhOh.CONNECTION_STILL_ALIVE. There is also the isConnectedBug() method in BondEvent.

## Scanning Issues ##
* See [Android M Issues](#android-m-issues) above for M-specific scanning challenges.
* BLE scanning pre-Lollipop and post-Lollipop are entirely different APIs that you have to manage differently, gating lots of logic by runtime checks on the Android build version.
* BLE scanning can fail for no reason and you’re left without the ability to discover advertising devices unless you back-up to using classic discovery, which is a different and harder-to-manage API.
* When BLE scanning fails once it can still succeed a short time afterwards so it makes sense to put this on a retry loop separated by a few hundred milliseconds on a separate thread.
* Built-in scan filtering, at least pre-lollipop, doesn’t work. You have to scan for all devices and do filtering yourself.
* Built-in undiscovery doesn’t work and even if it did it is limited to when you're scanning using classic discovery.
* You cannot continuously scan. Attempt to do so and you will eventually stop discovering devices entirely. It’s also a battery drain and destabilizes other operations. You have to simulate continuous scanning using intermittent scan pulses.

## Bonding-Pairing Issues ##
* Encrypted characteristics cause a whole host of issues around bonding state on some devices.
* If you try to read or write an encrypted characteristic and you're not bonded (even if the API says you’re bonded), the characteristic operation can silently fail and implicitly kick off a bonding operation without retrying the operation. Sometimes it does automatically retry the operation though. Managing this is a huge headache.
* Bonding state on some Android devices can’t be reused across multiple connection sessions. Every time you reconnect you have to clear bonding state programmatically and re-bond.
* Some phones, namely the LG G4 and Samsung S6, have subjective issues with bonding if you try to use the public BluetoothDevice.createBond() method. Instead you should use a private equivalent that must be called through reflection that forces low-energy transport mode.

## Threading Issues ##
* It’s more stable to kick off most operations from the main thread, but certain operations can randomly block for several seconds so it’s important to know which ones do so and when and how to handle that by splitting off to separate threads as needed.
* The native stack uses multiple different threads for asynchronous callback invocations, most of the time on threads different from the initial request. Routing all these callbacks back to the main thread for eventual UI updating is a pain.

## Read/Write/Notify Issues ##
* You cannot perform multiple operations at once. If you want to perform two writes, you have to wait for the first write to come back before performing the second. For non-trivial use cases you have to implement your own job queue, which can get pretty hairy with multiple devices and different operation priorities.
* Sometimes notify characteristics stop working despite still being able to still read them. A back-up change-tracking read poll to simulate notifications is needed to prevent disruptions to the user experience.
* There are also limitations to the number of notify characteristics you can register simultaneously. For some phones it’s 4, others 7. Again you need a back-up change-tracking poll to ensure smooth user experience.
* Characteristic reads and notifications can return "successfully" but with null values.
* RSSI readings vary from device to device, and aren't given to you in convenient formats like distance and percentage. You will have to do a fair amount of research to both normalize RSSI readings and convert them to usable values.
* Service discovery can sometimes erroneously return duplicated service object instances across multiple connection attempts. Use the wrong one and you’re in trouble.

## General Issues ##
* All operations - connecting, disconnecting, reading, writing, etc. - can sometimes never return with a callback. You have to handle timeout tracking yourself.
* Many undocumented status codes returned from various operations.
* Some critical methods can only be accessed through reflection hacks.
* Device name format can randomly change based on who knows what. Spaces and underscores are interchangeable, sometimes camel-cased, sometimes lowercased, sometimes with last four of MAC address appended with a hyphen. Need to normalize device name yourself for filtering and UI purposes.
* A number of calls to the native stack can throw mysterious undocumented exceptions, including DeadObjectExceptions. This is an undocumented behavior and happens rarely enough that you might not discover it while testing but it could bite your users out in the wild.
* The native stack's bluetooth service can sometimes get into states where it can't be turned on, or even can't be turned off. SweetBlue cannot fix this, but is does detect and report this issue through [UhOhListener](https://api.sweetblue.io/com/idevicesinc/sweetblue/UhOhListener.html). Usually this means a phone restart is required.

# Device Specific Issues #

## Samsung Galaxy S7 running Marshmallow ##

 The S7 has a new feature called "Nearby device scanning". If this is on, it uses BLE to scan for nearby devices. This uses BLE **even if bluetooth is turned off in settings**. It interferes with scan results in unpredictable ways when using the pre-lollipop scanning API (which is what SweetBlue currently uses by default, or if you use BleScanMode.AUTO). To get around the issue, either turn off the feature in the phone's device settings, or use BleScanMode.POST_LOLLIPOP for the S7.

## OnePlus OnePlus2 and Motorola Moto X Pure ##

 While these devices will allow you to set a larger MTU size, when you try to write a larger payload, it will result in a write time out. From testing, 50 is the largest size that will reliably work.