<b>|</b>&nbsp;<a href='#why'>Why?</a>
<b>|</b>&nbsp;<a href='#features'>Features</a>
<b>|</b>&nbsp;<a href="https://sweetblue.io/docs">Docs</a>
<b>|</b>&nbsp;<a href="https://play.google.com/store/apps/details?id=com.idevicesinc.sweetblue.toolbox">Toolbox</a>
<p align="right">
[![SweetBlue-CI](https://github.com/HubbellCorp/SweetBlue/actions/workflows/build_and_test.yml/badge.svg)](https://github.com/HubbellCorp/SweetBlue/actions/workflows/build_and_test.yml)
</p>
<img align="right" src="https://img.shields.io/badge/version-4.0.0-blue.svg" />

<p align="center">
  <br>
  <a href="https://sweetblue.io">
    <img src="https://idevicesinc.com/sweetblue/downloads/sweetblue_logo.png" />
  </a>
</p>

Why?
----

Android's BLE stack has some...issues...

* [https://github.com/iDevicesInc/SweetBlue/wiki/Android-BLE-Issues](https://github.com/iDevicesInc/SweetBlue/wiki/Android-BLE-Issues)
* [https://code.google.com/p/android/issues/detail?id=58381](https://code.google.com/p/android/issues/detail?id=58381)
* [http://androidcommunity.com/nike-blames-ble-for-their-shunning-of-android-20131202/](http://androidcommunity.com/nike-blames-ble-for-their-shunning-of-android-20131202/)
* [http://stackoverflow.com/questions/17870189/android-4-3-bluetooth-low-energy-unstable](http://stackoverflow.com/questions/17870189/android-4-3-bluetooth-low-energy-unstable)

SweetBlue is a blanket abstraction that shoves all that troublesome behavior behind a clean interface 
and gracefully degrades when the underlying stack becomes too unstable for even it to handle.

It’s built on the hard-earned experience of several commercial BLE projects and provides so many transparent 
workarounds to issues both annoying and fatal that it’s frankly impossible to imagine writing an app without
 it. It also supports many higher-level constructs, things like atomic transactions for coordinating 
 authentication handshakes and firmware updates, flexible scanning configurations, read polling, transparent
  retries for transient failure conditions, and, well, the list goes on. The API is dead simple, with usage
   dependence on a few plain old Java objects and link dependence on standard Android classes. It offers
    conveniences for debugging and analytics and error handling that will save you months of work - last
     mile stuff you didn't even know you had to worry about.

Features
========

*	Full-coverage API documentation: [https://api.sweetblue.io](https://api.sweetblue.io)
*	Sample applications.
*	Battle-tested in commercial apps.
*	Plain old Java with zero API-level dependencies.
*	Rich, queryable state tracking that makes UI integration a breeze.
*	Automatic service discovery.
*	Full support for server role including advertising.
*	Easy RSSI tracking with built-in polling and caching, including distance and friendly signal strength calculations.
*	Highly configurable scanning with min/max time limits, periodic bursts, advanced filtering, and more.
*	Continuous scanning mode that saves battery and defers to more important operations by stopping and starting as needed under the hood.
*	Atomic transactions for easily coordinating authentication handshakes, initialization, and firmware updates.
* 	Automatic striping of characteristic writes greater than [MTU](http://en.wikipedia.org/wiki/Maximum_transmission_unit) size of 20 bytes.
*	Undiscovery based on last time seen.
*	Clean leakage of underlying native stack objects in case of emergency.
*	Wraps Android API level checks that gate certain methods.
*	Verbose [logging](https://github.com/iDevicesInc/SweetBlue/wiki/Logging) that outputs human-readable thread IDs, UUIDs, status codes and states instead of alphabet soup.
*	Wrangles a big bowl of thread spaghetti behind a nice asynchronous API - make a call on main thread, get a callback on main thread a short time later.
*	Internal priority job queue that ensures serialization of all operations so native stack doesn’t get overloaded and important stuff gets done first.
*	Optimal coordination of the BLE stack when connected to multiple devices.
*	Detection and correction of dozens of BLE failure conditions.
*	Numerous manufacturer-specific workarounds and hacks all hidden from you.
*	Built-in polling for read characteristics with optional change-tracking to simulate notifications.
*	Transparent retries for transient failure conditions related to connecting, getting services, and scanning.
*	Comprehensive callback system with clear enumerated reasons when something goes wrong like connection or read/write failures.
*	Distills dozens of lines of boilerplate, booby-trapped, native API usages into single method calls.
*	Transparently falls back to Bluetooth Classic for certain BLE failure conditions.
*	On-the-fly-configurable reconnection loops started automatically when random disconnects occur, e.g. from going out of range.
*	Retention and automatic reconnection of devices after BLE off->on cycle or even complete app reboot.
*	One convenient method to completely unwind and reset the Bluetooth stack.
*	Detection and reporting of BLE failure conditions that user should take action on, such as restarting the Bluetooth stack or even the entire phone.
*	Runtime analytics for tracking average operation times, total elapsed times, and time estimates for long-running operations like firmware updates.
