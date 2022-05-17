## Version 3 Changes ##

Version 3 focused on making the library more user friendly, and to remove ambiguity whenever possible. Thread safety was another big thing that was focused on for this major release.

## API Key ##

SweetBlue now requires an API key to operate. You will receive an API key upon purchasing a license. You can always email sweetblue@idevicesinc.com to obtain your key, or get a new key, if it's needed. You have two ways in which you can implement the key. The easiest, and recommended way, is to put it into a file called sweetblue_api_key.txt, in your app's assets folder. The other way, is to use the new createInstance static method on BleManager. This method is similar to the get() method, only it allows you to pass in the api key.


## Refactors ##

A large part of the changes that will be seen app-side are the refactors of listeners and states.

### Listeners/Filters ###
|**Old Listener**|**New Listener**| 
|---|---| 
| BleDevice.ReadWriteListener | ReadWriteListener |
| BleDevice.StateListener | DeviceStateListener |
| BleDevice.ConnectionFailListener | DeviceReconnectFilter |
| BleDevice.BondListener | BondListener |
| BleManager.DiscoveryListener | DiscoveryListener |
| BleManager.StateListener | ManagerStateListener |
| BleManager.NativeStateListener | NativeManagerStateListener |
| BleManager.UhOhListener | UhOhListener |
| BleManager.ResetListener | ResetListener |
| BleManager.AssertListener | AssertListener |
| BleServer.ServiceAddListener | AddServiceListener |
| BleServer.ExchangeListener | ExchangeListener |
| BleServer.IncomingListener | IncomingListener |
| BleServer.OutgoingListener | OutgoingListener |
| BleNode.HistoricalDataLoadListener | HistoricalDataLoadListener |
| BleNode.HistoricalDataQueryListener      | HistoricalDataQueryListener |
| BleManagerConfig.ScanFilter | ScanFilter |

**Note 1: ReadWriteListener will no longer fire its onEvent() method for Notifications. Use the NotificationListener instead.**

**Note 2: BondListener is now ephemeral when passing in an instance to the BleDevice.bond() method. This means it will only report the result for that method call (in v2, it would be set as the new BondListener, and report results for all bond events). You can also now pass a BondListener into the BleDevice.unbond() method.**

### BleDevice Listeners ###

BleDevice listeners have been changed to a Stack of listeners. This allows you to push a new listener onto the stack, then
pop it off once you're done. This is convenient if you want to handle things a certain way temporarily (for instance you may want
to do something different with states during an OTA).<br><br> 
**Note: If [BondFilter.onEvent(BondFilter.ConnectionBugEvent)](https://api.sweetblue.io/com/idevicesinc/sweetblue/BondFilter.html#onEvent-com.idevicesinc.sweetblue.BondFilter.ConnectionBugEvent-) returns
[BondFilter.ConnectionBugEvent.Please.tryFix()](https://api.sweetblue.io/com/idevicesinc/sweetblue/BondFilter.ConnectionBugEvent.Please.html#tryFix--), then pushing/setting/popping listeners will be ignored during the fixing
process.**

### States (BleDevice only): ###


| **Old State** | **New State** |
|---|---|
| CONNECTED | BLE_CONNECTED |
| CONNECTING | BLE_CONNECTING |
| DISCONNECTED   | BLE_DISCONNECTED |


## BleDevice State Changes ##

As you can see above, CONNECTED, CONNECTING, and DISCONNECTED have been refactored. The idea behind this change was to make it more apparent when a device is connected and ready to use.

So, we've added 3 new "simple" states to watch for on BleDevices. They are CONNECTED, CONNECTING, and DISCONNECTED. To better understand the flow, see below (simple states in bold).

* This flow assumes no errors were encountered along the way.

| **Step  ** | **Device Operation** | **States entered** | **Notes** |
|---|---|---|---|
| **1** | Device is discovered | BLE_DISCONNECTED, **DISCONNECTED** |   |
| **2** | connect() is called on the device | BLE_CONNECTING, CONNECTING_OVERALL, **CONNECTING** | The first of the simple states is set here |
| **3** | Established BLE connection | BLE_CONNECTED, DISCOVERING_SERVICES | Now that the device is connected, we try to discover its services |
| **4** | Service discovered | SERVICES_DISCOVERED, AUTHENTICATING | Runs any Auth transaction for this device |
| **5** | Auth transaction succeeded | AUTHENTICATED, INITIALIZING | Runs any Init transaction for this device |
| **6** | Init transaction succeeded | INITIALIZED, **CONNECTED** | Device is connected, and ready to use now |

Most of the time, you only care about when a device gets connected, is connecting, or is disconnected. As there are a few things done for the connection process, it made sense to come up with the new "simple" states. All states are delivered via the DeviceStateListener. The StateEvent class that is passed in this listener contains the method isSimple() to make it easier to know if you should really care about the particular state being dispatched. 

The above state flow hasn't changed from v2, other than the addition of the new "simple" states.

## DeviceStateListener Changes ##

With the addition of the 3 new simple states (CONNECTED, DISCONNECTED, CONNECTING), there is a new option in BleDeviceConfig called defaultDeviceStates. This option allows you to specify the states that will make the DeviceStateListener fire. By default, those states are DISCONNECTED, CONNECTING, CONNECTED, UNBONDED, BONDING, and BONDED. If you want to listen for ALL state changes, then set defaultDeviceStates = BleDeviceState.VALUES();


## Connection Changes ##

### DeviceConnectionListener ###

Many overloaded connect() methods were removed. It was kind of overwhelming to see 15 different connect() methods. This has been paired down to 6 different connect() methods, depending on what you want to pass in for arguments. You can no longer pass in a DeviceStateListener, or ConnectionFailListener. Instead, we added a new interface for convenience purposes

This interface is tied to the new "simple" states introduced in v3. There is a single method which passes in a ConnectEvent instance. You then call wasSuccess() on this instance. If it reports true, then the device is connected, and ready to use.

**NOTE:** This will also get called if the connect attempt failed. Because SweetBlue can be trying to reconnect for you, there is also the isRetrying() method available on the ConnectEvent instance, so you know if the device may get connected soon. You can also call the failEvent() method to see what the failure was.

**NOTE2:** This listener will ONLY get called when connecting to a device from a disconnected state (in other words, if you GET connected to a device, then it drops the connection, this listener will not be invoked).

### Reconnection ###

This was another source of confusion, so we tried to make it more clear in v3.

The source of most confusion was the fact that there are 2 areas where reconnection can happen.

1. When trying to connect to a device. On android sometimes, this just fails for whatever reason, and all thats needed is to try again. (ConnectionFailListener)
1. When a device is connected, but loses its connection. (ReconnectFilter)


These 2 cases are now merged into a single interface.

#### ReconnectFilter ####

The ReconnectFilter is the base interface used. You do not need to manually implement this interface, as there are default implementations provided in SweetBlue to handle the majority of cases.

The interface has two methods to implement, each representing the 2 cases pointed out above.

* onConnectFailed(ConnectFailEvent) - this is the case where the device failed to connect
* onConnectionLost(ConnectionLostEvent) - this is the case where the device was connected, but lost its connection


DefaultReconnectFilter is an interface which extends ReconnectFilter, which implements most of the onConnectionLost logic for both BleDevice, and BleServer. You should never have to extend this class yourself.


**BleDevice**

DeviceReconnectFilter - base interface used for BleDevice, which extends DefaultReconnectFilter

DefaultDeviceReconnectFilter - default implementation of DeviceReconnectFilter

This class sets up the defaults as such:

* Retry failed connections twice (for a total of 3 connection attempts)
* Retry connecting lost connections 
* Short term reconnect runs a total of 5 seconds, with 1 second between each try
* Long term reconnect runs a total of 5 minutes, with 5 seconds of delay between each try


**BleServer**

ServerReconnectFilter - base interface used for BleServer, which extends DefaultReconnectFilter

DefaultServerReconnectFilter - this is setup with the same default values as DefaultDeviceReconnectFilter