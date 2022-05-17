
SweetBlue has three kinds of logging capabilities. 

### Logcat ###

The first type is specific to the internals of the library and primarily logs events related to the priority job queue that’s used to serialize all interaction with the native stack. Currently this is piped to Android’s "logcat", which is a circular in-memory buffer on the OS which is basically only retrievable through a command line interface called ADB while the device is plugged in. SweetBlue’s logging can be piped anywhere though via the [SweetLogger](https://api.sweetblue.io/com/idevicesinc/sweetblue/SweetLogger.html) interface.

The log entries posted also tell you helpful information, such as the thread they are posted on, along with whether the log represents a native callback, versus a SweetBlue specific log entry.

### UhOhs ###

The second type are affectionately called ["UhOhs"](https://api.sweetblue.io/com/idevicesinc/sweetblue/UhOhListener.UhOh.html) in SweetBlue lingo, and are events that are thrown whenever SweetBlue detects an anomaly in the underlying stack, with a suggestion for what can be done about it (either nothing, BLE restart, or phone restart). Currently app-land is responsible for logging these only if it wants to, but it’s very easy to convert the event to a string and pipe it wherever you want.

### Event structs ###

The third type are just general events dispatched by SweetBlue, ConnectionFail, StateChange (both per device, and for ble as a whole), discovery change, etc. Again app-land is responsible for listening to these and logging them only if it wants to, but again very easy to convert to string and pipe wherever.

In general SweetBlue is very helpful about converting the "alphabet soup" that usually coincides with BLE development into human-readable strings. Things like UUIDs, Thread IDs, status codes, and states are all converted (or easily convertible) to names.