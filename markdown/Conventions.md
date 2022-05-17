This document enumerates some of the conventions this library follows. It's half meant as a reference for internal developers, half for users to better understand the design decisions made in the public API. Note that while the codebase uses many standard Object-Oriented and Functional techniques, these won't be mentioned because this isn't intended to be a regurgitation of best practices. Rather as a reference for any conventions that may lie outside the norm for the average Java/Android developer.

#### `null` Is Bad

For the most part SweetBlue will never return Java's built-in `null` and will accept `null` as method parameters or configuration options without issues. Instead of returning `null` we use empty strings, empty arrays, `enum` definitions with `NULL` entries, and special "dummy" singleton values whose only purpose is to stand in for what would otherwise be `null` references. Some classes implement an interface [UsesCustomNull](https://api.sweetblue.io/com/idevicesinc/sweetblue/utils/UsesCustomNull.html) to make this behavior more explicit.

Overall we believe our avoidance of `null` makes the API more explicit and self-documenting, nudging you to deal with edge cases more appropriately while cutting down on the dreaded `NullPointerException`.

#### Prefix Package-Private Classes

SweetBlue contains over 100 `.java` files, but only a small portion of these are meant for regular external consumption. To make the latter stand out we prefix the former with P_ for private. The goal is to make the library less scary looking when browsing through files in your IDE, especially for new users. The P_ prefix lets you know that you don't have to worry about it. If Java had more flexible access specifiers then these would all go into their own separate package, but alas we are left to dream.

#### Use Callback Event Structs

All callbacks to app-land provide an "event struct" that is a simple, read-only wrapper class for all the information you would want about what just happened. For an example, see [BondEvent](https://api.sweetblue.io/com/idevicesinc/sweetblue/BondListener.BondEvent.html). An event's getter methods are mostly direct, compiler-inline-able returners of private final member fields. For brevity's sake they thus forgo the standard `get*()` prefix that one usually uses for getters in Java. Structs provide the following advantages over having the same data as parameters of the callback method itself:
 * Backwards compatible changes can be made to the struct without affecting the callback method signature.
 * Structs can have convenience query methods that act on the data they contain.
 * Structs can be easily parcelled/unparcelled over networks, between apps, to/from disk, etc., if need be. 
 * Documentation tooling (javadoc, IDEs, etc.) works much better with `class` members compared to lone method parameters.
 * Easier for debugging if you can mouse over the struct and see its `toString()` output all at once.
 * Avoids huge method signatures that go past viewable screen area.

#### Say `Please`

Related to [Use Callback Structs](#use-callback-event-structs) and [Avoid Naked Primitives](#avoid-naked-primitives), any callback that requires a return value will use a peer static inner class called `Please` which will have clearly named static constructor methods, so for example `return Please.retry();` or `return Please.stop();` will be used instead of `return true;` or `return false;`. This has the following advantages:
 * Enhanced code readability.
 * Backwards compatible additions to the `Please` struct can be made, e.g. new methods or overloads of existing ones.
 * Methods can take further parameters for more flexibility.

#### Static Polymorphism

This is a fancy term for "naming stuff consistently", and makes us sound smart. SweetBlue strives to have very consistent naming and organizational conventions, such that once you have a bit of experience with some parts of the API, the rest of the API will come naturally without thinking about it. A prominent example of this is the various `*Listener` callbacks that are interfaces for [BleDevice](https://api.sweetblue.io/com/idevicesinc/sweetblue/BleDevice.html) and [BleManager](https://api.sweetblue.io/com/idevicesinc/sweetblue/BleManager.html), and [BleServer](https://api.sweetblue.io/com/idevicesinc/sweetblue/BleServer.html). These all have the format `<name>Listener` with one method called `onEvent()` that takes a `<name>Event` struct as the sole parameter. For an example see [https://api.sweetblue.io/com/idevicesinc/sweetblue/DeviceStateListener.html]().

#### Avoid Naked Primitives

Instead of `boolean` use an `enum` with two self-descriptive entries. Instead of `double` or `long` for time intervals, use a [wrapper class](https://api.sweetblue.io/com/idevicesinc/sweetblue/utils/Interval.html) that provides a certain level of type-safety and self-documentation.

#### Underscore Usage

Variable and method names sometimes contain an underscore followed by a suffix. This suffix is meant to convey pseudo-type information on "overloaded" variables and methods to help code readability. For example you might have two variables named `time_seconds` and `time_milliseconds`. So the underscore should always be seen as the delimiter for this soft type information.

#### Use Constructor Methods

You will notice that many of SweetBlue's classes have private constructors, forcing their creation through constructor methods, usually static ones. The benefits of this are:
 * Enhances code readability - an aptly named constructor method gives much more context over an actual constructor which only allows differentiation based on input parameters.
 * Allows for transparent pooling/caching/reuse of returned instances - especially for [@Immutable](https://api.sweetblue.io/com/idevicesinc/sweetblue/annotations/Immutable.html) classes. This can be done in a backwards compatible way as well, meaning a constructor method can heap-allocate in the original release and in the next release return a cached instance and app-code benefits without knowing or caring about the change under the hood.