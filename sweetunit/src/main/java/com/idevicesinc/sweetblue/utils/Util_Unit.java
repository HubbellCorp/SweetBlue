package com.idevicesinc.sweetblue.utils;


import com.idevicesinc.sweetblue.BleManagerConfig;

import java.util.Random;


/**
 * Utility class to handle various things when unit testing, such as generating random mac addresses, random byte arrays. For generating a scan record, you
 * should use the {@link BleScanRecord} class.
 */
public final class Util_Unit
{

    private Util_Unit() {}


    /**
     * Returns a random mac address
     */
    public static String randomMacAddress()
    {
        byte[] add = new byte[6];
        new Random().nextBytes(add);
        return Utils_String.bytesToMacAddress(add);
    }

    /**
     * Returns a random byte array of the given size
     */
    public static byte[] randomBytes(int size)
    {
        byte[] bytes = new byte[size];
        new Random().nextBytes(bytes);
        return bytes;
    }

    /**
     * Returns a random amount of time wrapped in an {@link Interval} instance from the given range.
     */
    public static Interval getRandomTime(int from, int to)
    {
        Random r = new Random();
        int ms = r.nextInt(to - from) + from;
        return Interval.millis(ms);
    }

    /**
     * Returns a random RSSI value within the range {@link BleManagerConfig#DEFAULT_RSSI_MIN} and {@link BleManagerConfig#DEFAULT_RSSI_MAX}
     */
    public static int randomRssi()
    {
        return new Random().nextInt((BleManagerConfig.DEFAULT_RSSI_MAX - BleManagerConfig.DEFAULT_RSSI_MIN) + 1) + BleManagerConfig.DEFAULT_RSSI_MIN;
    }


    /**
     * Overloads {@link #getRandomTime()}, using the range 50-500.
     */
    public static Interval getRandomTime()
    {
        return getRandomTime(50, 500);
    }


}
