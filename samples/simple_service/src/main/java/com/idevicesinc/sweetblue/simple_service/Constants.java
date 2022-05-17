package com.idevicesinc.sweetblue.simple_service;

class Constants
{
    // Keys for the signals being sent to the service
    static final String ACTION_START = "ACTION_START";
    static final String ACTION_STOP = "ACTION_STOP";
    static final String ACTION_CONNECT = "ACTION_CONNECT";
    static final String ACTION_DISCONNECT = "ACTION_DISCONNECT";

    // Key for the signal indicating how the app was launched.
    static final String FROM_NOTIFICATION = "FROM_NOTIFICATION";

    // Key for extras being sent to the service
    static final String EXTRA_MAC_ADDRESS = "MAC_ADDRESS";
}
