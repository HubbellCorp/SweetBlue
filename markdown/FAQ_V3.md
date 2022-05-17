### How long are you going to support version 2? ###
We plan on supporting version 2 with critical bug fixes until December 31 2018.

### Does my current license cover upgrading to version 3? ###
As long as your license is valid by the time version 3 is officially released, then yes it does.

### Will it cost me more? ###
No, version 3 will be the same price.

### Will version 3 remain open source? ###
No, with version 3, we decided to go to a closed source system. One of the main reasons it was open sourced was to get contributions from the community. In the 3+ years SweetBlue has been on github, we only received a few pull requests.

### What's the meaning of life? ###
42

### Will version 3 still support Android 4.3 & 4.4? ###
Yes.

### What's changed in version 3? ###
You can see the major changes [here](Version-3-Changes).

### Why aren't I seeing all device state changes? ###
To cut down on thread switching, version 3 has an option called defaultDeviceStates in BleDeviceConfig. This dictates which states will fire the DeviceStateListener. By default, it will only fire when CONNECTED, CONNECTING, DISCONNECTED, BONDED, UNBONDED, and BONDING change.