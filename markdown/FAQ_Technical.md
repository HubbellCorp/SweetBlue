### Do you have a general FAQ? This stuff is too technical.
[Yes!](FAQ_General)

### SweetBlue seems great and all but I'm still having trouble connecting and staying connected...what can I do?
There are dozens of variables involved with achieving and maintaining a stable connection, many of which are outside of SweetBlue's control. Your best best is to go through our [Troubleshooting](Troubleshooting) steps and see if anything helps.

### Are Java Docs hosted somewhere?
Yes, https://api.sweetblue.io

### Does SweetBlue cleanly handle multiple device connections simultaneously?
Yes, this was one of the primary purposes for its creation.

### What happens when my app is backgrounded?
If you're connected to a device the Android operating system seems to hold some kind of implicit wake lock that prevents it from going to sleep and disconnecting your peripheral. This is automatic and doesn't require any permissions or programmatic intervention.

If you're *not* connected to a device, the operating system will treat your app like any other. If you need your app to stay awake in order to scan then you may need to manage your own wake lock. See https://api.sweetblue.io/com/idevicesinc/sweetblue/BleManager.html#pushWakeLock-- and https://api.sweetblue.io/com/idevicesinc/sweetblue/BleManager.html#popWakeLock-- for helper methods that can aid you here.

### What is SweetBlue's threading model?
Default behavior:

SweetBlue runs its operations on a background thread. In version 2, it was thought that bluetooth operations would be more reliable when called from the main thread. After a year or so of running in a background thread, we now realize that's not the case. Plus, now that SweetBlue's logic is all handled in a background thread, animations, and the UI in general are much more responsive.

All user-land callbacks are posted to the main thread by default, so you don't have to worry about posting them yourself when updating UI components from these callbacks. This can be changed of course, if you wish to handle making sure you update your views on the main thread.


### How is reconnection handled?
In order to understand some of the decisions behind reconnect logic, one must first understand that disconnects can happen *very* frequently on Android even under the best of conditions. You may sometimes gets disconnects every few seconds even though you can trivially reconnect immediately afterwards.

So, when a disconnect occurs a BleDevice enters a state called [RECONNECTING_SHORT_TERM](https://api.sweetblue.io/com/idevicesinc/sweetblue/BleDeviceState.html#RECONNECTING_SHORT_TERM). What this means is that SweetBlue temporarily hides the fact that the device actually disconnected while it tries to silently reconnect for several seconds. If this silent reconnection fails, a BleDevice notifies your app code of the disconnect and enters a state called [RECONNECTING_LONG_TERM](https://api.sweetblue.io/com/idevicesinc/sweetblue/BleDeviceState.html#RECONNECTING_LONG_TERM). This means the device will automatically try connecting again and again until, e.g. it comes back into range. Overall by default SweetBlue is pretty aggressive about reconnecting due to how often random disconnects can occur even under the best of conditions. The short term reconnect is meant to hide the annoying transient disconnects from you, while the long term is a convenience to re-establish connection after legitimate disconnects (e.g. from going out of range) as soon as possible.

You can control the reconnect behavior through a [ReconnectFilter](https://api.sweetblue.io/com/idevicesinc/sweetblue/BleNodeConfig.html#reconnectFilter) if the defaults don’t work for your situation.

### If I have a known MAC address can I avoid scanning and just connect directly?
Yes, see https://api.sweetblue.io/com/idevicesinc/sweetblue/BleManager.html#newDevice-java.lang.String-.

### What's the maximum amount of data you can write to a characteristic at a time?
Generally 20 bytes. This is a limitation imposed by the BLE specification. However, SweetBlue also has support for BLE's reliable write mechanism, which essentially breaks up writes greater than 20 bytes into individual packets where they're reassembled on the peripheral side and treated as one atomic chunk. In practice this is of limited use though because (a) it requires the firmware to implement such, and (b) doesn't usually work anyway. It's there to try though!

The other thing SweetBlue supports (on Lollipop and above), is changing the MTU (maximum transmission unit) size. This also depends on the peripheral side implementing support for this, but you can get up to around 512 bytes per packet. 

### What's up with [BleTransaction](https://api.sweetblue.io/com/idevicesinc/sweetblue/BleTransaction.html)? When would I need to use this?
To be clear, there is no similar concept of a transaction in the actual underlying BLE specification - this is a SweetBlue-only construct. Transactions are simply a convenient and totally optional way to bundle a series of specialized reads and writes into one nice unit of code, and also gives a signal to SweetBlue that these reads and writes are probably important, thus giving some amount of priority to them. You typically fail a transaction (`BleTransaction.fail()`) when one of your reads or writes fail. On the flip side, you typically call `BleTransaction.succeed()` when your series of reads and/or writes complete successfully. You override [BleTransaction.onEnd()](https://api.sweetblue.io/com/idevicesinc/sweetblue/BleTransaction.html#onEnd-com.idevicesinc.sweetblue.BleTransaction.EndReason-) to know when/how your transaction completes.
Some examples of when you would use transactions: 
 * Some companies like to make sure that *only* their app can connect to their peripheral. To do this you can implement an application-specific authentication handshake using a few reads and writes inside of a subclass of [BleTransaction.Auth](https://api.sweetblue.io/com/idevicesinc/sweetblue/BleTransaction.Auth.html) and pass an instance of this subclass to various overloads of `BleDevice.connect()` such as [BleDevice.connect(BleTransaction.Auth)](https://api.sweetblue.io/com/idevicesinc/sweetblue/BleDevice.html#connect-com.idevicesinc.sweetblue.BleTransaction.Auth-).
 * Most devices need to have a characteristic or two read right after connection for user-experience purposes. For example if you're connecting to a heart-rate monitor you probably want to read the heart-rate right after connection so it appears as quickly as possible in the application's UI. SweetBlue acknowledges this common use case through [BleTransaction.Init](https://api.sweetblue.io/com/idevicesinc/sweetblue/BleTransaction.Init.html). Pass a subclass of this to various overloads of `BleDevice.connect()` such as [BleDevice.connect(BleTransaction.Init)](https://api.sweetblue.io/com/idevicesinc/sweetblue/BleDevice.html#connect-com.idevicesinc.sweetblue.BleTransaction.Init-) and the library will treat this transaction as part of the overall connection process, which conceptually makes a lot of sense for most application UI flows - that is, you're not "fully" connected until you've read some initial data from the peripheral.
 * Most devices also have a firmware update protocol that involves a bunch of writes over the course of several minutes or even hours depending on transfer speed and file size. Doing these writes inside of a subclass of [BleTransaction.Ota](https://api.sweetblue.io/com/idevicesinc/sweetblue/BleTransaction.Ota.html) is recommended so that SweetBlue knows a long-running transfer is occurring and can temporarily reassign resources accordingly. Use [BleDevice.performOta(BleTransaction.Ota)](https://api.sweetblue.io/com/idevicesinc/sweetblue/BleDevice.html#performOta-com.idevicesinc.sweetblue.BleTransaction.Ota-) to accomplish this.

### Is BleManager a singleton?
Funny you should ask. Why yes it is. When you call [BleManager.get(Context)](https://api.sweetblue.io/com/idevicesinc/sweetblue/BleManager.html#get-android.content.Context-) for the first time, this is when the BleManager is actually created. Subsequent calls simply retrieve this already-created instance, no matter what Context you pass. In other words, even if you call this getter from one Activity then another, you're still getting the same actual BleManager instance. We weren't ecstatic about making this a singleton, but *not* doing so just clashed too much with Android's API underneath which also uses singletons.

### What is the difference between [CONNECTING_OVERALL](https://api.sweetblue.io/com/idevicesinc/sweetblue/BleDeviceState.html#CONNECTING_OVERALL) and [CONNECTING](https://api.sweetblue.io/com/idevicesinc/sweetblue/BleDeviceState.html#CONNECTING)?
[CONNECTING_OVERALL](https://api.sweetblue.io/com/idevicesinc/sweetblue/BleDeviceState.html#CONNECTING_OVERALL) is the "master" state that encompasses [CONNECTING](https://api.sweetblue.io/com/idevicesinc/sweetblue/BleDeviceState.html#CONNECTING), [DISCOVERING_SERVICES](https://api.sweetblue.io/com/idevicesinc/sweetblue/BleDeviceState.html#DISCOVERING_SERVICES), [AUTHENTICATING](https://api.sweetblue.io/com/idevicesinc/sweetblue/BleDeviceState.html#AUTHENTICATING), and [INITIALIZING](https://api.sweetblue.io/com/idevicesinc/sweetblue/BleDeviceState.html#INITIALIZING) (and sometimes [BONDING](https://api.sweetblue.io/com/idevicesinc/sweetblue/BleDeviceState.html#BONDING)). In other words if you wrote a simple app that showed a spinner for the connection process, [CONNECTING_OVERALL](https://api.sweetblue.io/com/idevicesinc/sweetblue/BleDeviceState.html#CONNECTING_OVERALL) would probably be what you would use to show and hide the spinner. [CONNECTING](https://api.sweetblue.io/com/idevicesinc/sweetblue/BleDeviceState.html#CONNECTING) would probably be renamed to CONNECTING_BLE if we didn't care about backwards compatibility.