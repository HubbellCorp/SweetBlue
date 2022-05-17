# Reconnection  
  
  
In version 3, all reconnection logic is now handled via the [ReconnectFilter](https://api.sweetblue.io/com/idevicesinc/sweetblue/ReconnectFilter.html) interface. There are a few default implementations already provided, and most of the time you shouldn't have to adjust these.  
  
  
When dealing with reconnections, it's important to note the distinction between the two methods in the [ReconnectFilter](https://api.sweetblue.io/com/idevicesinc/sweetblue/ReconnectFilter.html) interface.  
  
`onConnectFailed(ConnectFailEvent)` - This method is called when the library fails to establish a connection to a [BleDevice](https://api.sweetblue.io/com/idevicesinc/sweetblue/BleDevice.html) when it was not connected prior.  This will only be called if the BleDevice is **not** in either the [RECONNECTING_SHORT_TERM](https://api.sweetblue.io/com/idevicesinc/sweetblue/BleDeviceState.html#RECONNECTING_SHORT_TERM) or [RECONNECTING_LONG_TERM](https://api.sweetblue.io/com/idevicesinc/sweetblue/BleDeviceState.html#RECONNECTING_LONG_TERM)  state.
  
[onConnectionLost()](https://api.sweetblue.io/com/idevicesinc/sweetblue/ReconnectFilter.html#onConnectionLost-com.idevicesinc.sweetblue.ReconnectFilter.ConnectionLostEvent-) - The method is called by the library when we previously had a connection to the device (it was in the `CONNECTED` state), but have since lost that connection. This method is related to the states [RECONNECTING_SHORT_TERM](https://api.sweetblue.io/com/idevicesinc/sweetblue/BleDeviceState.html#RECONNECTING_SHORT_TERM) and [RECONNECTING_LONG_TERM](https://api.sweetblue.io/com/idevicesinc/sweetblue/BleDeviceState.html#RECONNECTING_LONG_TERM)  
  
Both [BleDevice](https://api.sweetblue.io/com/idevicesinc/sweetblue/BleDevice.html) and [BleServer](https://api.sweetblue.io/com/idevicesinc/sweetblue/BleServer.html) have default implementations of the [ReconnectFilter](https://api.sweetblue.io/com/idevicesinc/sweetblue/ReconnectFilter.html) interface.  
  
## BleDevice ##  
  
[DefaultDeviceReconnectFilter](https://api.sweetblue.io/com/idevicesinc/sweetblue/defaults/DefaultDeviceReconnectFilter.html)<br>  
    * Best practice is to extend the DefaultDeviceReconnectFilter for handling reconnection logic for your [BleDevice](https://api.sweetblue.io/com/idevicesinc/sweetblue/BleDevice.html)(s).
    
  Default implementation of [DeviceReconnectFilter](https://api.sweetblue.io/com/idevicesinc/sweetblue/DeviceReconnectFilter.html) (which extends `ReconnectFilter`). This class has a few constructors which allow you to change the default settings for the filter (how many attempts to retry a connection, how long the library should try to establish a reconnection, etc.). The defaults are:  
 * Retry failed connection attempts twice (for a total of 3 connection attempts)  
 * Retry connecting lost connections  
 * Short term reconnect runs a total of 5 seconds, with 1 second between each retry  
 * Long term reconnect runs a total of 5 minutes, with 5 seconds of delay between each retry  
  
## BleServer ##  
  
[DefaultServerReconnectFilter](https://api.sweetblue.io/com/idevicesinc/sweetblue/defaults/DefaultServerReconnectFilter.html)<br>  
* Best practice - Like above, it's best practice to extend the DefaultServerReconnectFilter for handling reconnection logic.
  
 Default implementation of [ServerReconnectFilter](https://api.sweetblue.io/com/idevicesinc/sweetblue/ServerReconnectFilter.html). This implementation uses the same default values as [DefaultDeviceReconnectFilter](#bledevice)


## Custom Filters ##

If you are going to use a custom filter, then you should understand how the [onConnectionLost()](https://api.sweetblue.io/com/idevicesinc/sweetblue/ReconnectFilter.html#onConnectionLost-com.idevicesinc.sweetblue.ReconnectFilter.ConnectionLostEvent-)  method is used. (onConnectFailed() is pretty straightforward, and everything you need to know about it is listed in the beginning of this document).

When a device loses its established connection, [onConnectionLost()](https://api.sweetblue.io/com/idevicesinc/sweetblue/ReconnectFilter.html#onConnectionLost-com.idevicesinc.sweetblue.ReconnectFilter.ConnectionLostEvent-) is called. The library first needs to know if it should continue reconnecting. What you return here will affect how the method gets called again. If you return with either [ConnectionLostPlease.retryWithTimeout](https://api.sweetblue.io/com/idevicesinc/sweetblue/ReconnectFilter.ConnectionLostPlease.html#retryWithTimeout-com.idevicesinc.sweetblue.utils.Interval-com.idevicesinc.sweetblue.utils.Interval-) or [ConnectionLostPlease.retryInstantlyWithTimeout](https://api.sweetblue.io/com/idevicesinc/sweetblue/ReconnectFilter.ConnectionLostPlease.html#retryInstantlyWithTimeout-com.idevicesinc.sweetblue.utils.Interval-), [onConnectionLost()](https://api.sweetblue.io/com/idevicesinc/sweetblue/ReconnectFilter.html#onConnectionLost-com.idevicesinc.sweetblue.ReconnectFilter.ConnectionLostEvent-) will not be called again (until you get a connection established, and then drops again, forcing another reconnection attempt). Otherwise, if you tell the library to continue retrying, the method will be called again to get the delay time before trying to reconnect. Also in this case, the method will get called each time it needs to try to reconnect.

