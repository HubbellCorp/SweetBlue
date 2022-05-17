/*

  Copyright 2022 Hubbell Incorporated

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.

  You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.

 */

package com.idevicesinc.sweetblue.utils;


/**
 * Enumeration used to specify which Bluetooth 5 features to use. Each item here is mutually exclusive.
 */
public enum Phy
{

    /**
     * SweetBlue's default option. This is standard Bluetooth 4 option (no Bluetooth 5 specific features enabled)
     */
    DEFAULT(/*BluetoothDevice.PHY_LE_1M_MASK*/1, /*BluetoothDevice.PHY_LE_1M_MASK*/1, /*BluetoothDevice.PHY_OPTION_NO_PREFERRED*/0),

    /**
     * Use Bluetooth 5's high speed physical layer. (2x speed).
     */
    HIGH_SPEED(/*BluetoothDevice.PHY_LE_2M_MASK*/2, /*BluetoothDevice.PHY_LE_2M_MASK*/2, /*BluetoothDevice.PHY_OPTION_NO_PREFERRED*/0),

    /**
     * Use Bluetooth 5's long range support (2x range).
     */
    LONG_RANGE_2X(/*BluetoothDevice.PHY_LE_CODED_MASK*/4, /*BluetoothDevice.PHY_LE_CODED_MASK*/4, /*BluetoothDevice.PHY_OPTION_S2*/1),

    /**
     * Use Bluetooth 5's longer range support (4x range).
     */
    LONG_RANGE_4X(/*BluetoothDevice.PHY_LE_CODED_MASK*/4, /*BluetoothDevice.PHY_LE_CODED_MASK*/4, /*BluetoothDevice.PHY_OPTION_S8*/2);


    private final int txMask;
    private final int rxMask;
    private final int phyOptions;

    private static Phy[] VALUES;


    Phy(int txMask, int rxMask, int phyOptions)
    {
        this.txMask = txMask;
        this.rxMask = rxMask;
        this.phyOptions = phyOptions;
    }

    public int getTxMask()
    {
        return txMask;
    }

    public int getRxMask()
    {
        return rxMask;
    }

    public int getPhyOptions()
    {
        return phyOptions;
    }


    public static Phy fromMasks(int txMask, int rxMask, int phyOptions)
    {
        for (Phy op : VALUES())
        {
            if (op.txMask == txMask && op.rxMask == rxMask && op.phyOptions == phyOptions)
            {
                return op;
            }
        }
        return DEFAULT;
    }

    public static Phy fromMasks(int txMask, int rxMask)
    {
        for (Phy op : VALUES())
        {
            if (op.txMask == txMask && op.rxMask == rxMask)
            {
                return op;
            }
        }
        return DEFAULT;
    }


    private static Phy[] VALUES()
    {
        if (VALUES == null)
            VALUES = values();

        return VALUES;
    }

}
