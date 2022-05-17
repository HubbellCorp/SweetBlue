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

import android.annotation.TargetApi;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.os.Build;
import android.os.ParcelUuid;
import com.idevicesinc.sweetblue.BleAdvertisingSettings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


/**
 * Class used to store information from a BLE scan record. This class can also be used to create a scan record.
 */
public final class BleScanRecord implements UsesCustomNull
{

    public final static BleScanRecord NULL = new BleScanRecord();

    private Short m_manufactuerId;
    private byte[] m_manufacturerData;
    private List<ManufacturerData> m_manufacturerDataList;
    private Pointer<Integer> m_advFlags;
    private Pointer<Integer> m_txPower;
    private final List<BleUuid> m_serviceUuids;
    private final Map<UUID, byte[]> m_serviceData;
    private int m_options;
    private boolean m_completeUuidList;
    private String m_localName;
    private boolean m_shortName;

    /**
     * Basic constructor to use if you are building a scan record to advertise.
     */
    public BleScanRecord()
    {
        m_advFlags = new Pointer<>(0);
        m_txPower = new Pointer<>(0);
        m_serviceUuids = new ArrayList<>();
        m_serviceData = new HashMap<>();
        m_completeUuidList = false;
        m_options = Option.CONNECTABLE.or(Option.INCLUDE_NAME);
        m_manufacturerDataList = new ArrayList<>();
    }

    public BleScanRecord(UUID serviceUuid)
    {
        this();
        addServiceUuid(serviceUuid);
    }

    public BleScanRecord(UUID serviceUuid, byte[] serviceData)
    {
        this(serviceUuid, serviceData, Option.CONNECTABLE, Option.INCLUDE_NAME);
    }

    public BleScanRecord(UUID serviceUuid, byte[] serviceData, Option... options)
    {
        this();
        addServiceData(serviceUuid, serviceData);
        setOptions(options);
    }

    public BleScanRecord(UUID serviceUuid, Option... options)
    {
        this(serviceUuid);
        setOptions(options);
    }

    public BleScanRecord(UUID[] serviceUuids, Option... options)
    {
        this();
        for (UUID id : serviceUuids)
        {
            addServiceUuid(id);
        }
        setOptions(options);
    }

    /**
     * Old Constructor used to be used internally when a {@link com.idevicesinc.sweetblue.BleDevice} is discovered. Now, {@link #BleScanRecord(Pointer, Pointer, List, boolean, List, Map, String, boolean)} is
     * used instead.
     */
    public BleScanRecord(Pointer<Integer> advFlags, Pointer<Integer> txPower, List<UUID> serviceUuids, boolean uuidCompleteList, short mfgId, byte[] mfgData, Map<UUID, byte[]> serviceData, String localName, boolean shortName)
    {
        m_advFlags = advFlags;
        m_txPower = txPower;
        m_serviceUuids = new ArrayList<>();
        addServiceUUIDs(serviceUuids);
        m_manufactuerId = mfgId;
        m_manufacturerData = mfgData;
        m_manufacturerDataList = new ArrayList<>();
        ManufacturerData mdata = new ManufacturerData();
        mdata.m_id = m_manufactuerId;
        mdata.m_data = m_manufacturerData;
        m_manufacturerDataList.add(mdata);
        if (serviceData == null)
        {
            m_serviceData = new HashMap<>(0);
        }
        else
        {
            m_serviceData = serviceData;
        }
        m_localName = localName;
        m_shortName = shortName;
        m_completeUuidList = uuidCompleteList;
        m_options = Option.CONNECTABLE.or(Option.INCLUDE_NAME);
    }

    /**
     * Constructor used internally when a {@link com.idevicesinc.sweetblue.BleDevice} is discovered.
     */
    public BleScanRecord(Pointer<Integer> advFlags, Pointer<Integer> txPower, List<UUID> serviceUuids, boolean uuidCompleteList, List<ManufacturerData> mfgData, Map<UUID, byte[]> serviceData, String localName, boolean shortName)
    {
        m_advFlags = advFlags;
        m_txPower = txPower;
        m_serviceUuids = new ArrayList<>();
        addServiceUUIDs(serviceUuids);
        m_manufacturerDataList = mfgData;
        if (m_manufacturerDataList != null && m_manufacturerDataList.size() > 0)
        {
            ManufacturerData data = m_manufacturerDataList.get(0);
            m_manufactuerId = data.m_id;
            m_manufacturerData = data.m_data;
        }
        if (serviceData == null)
        {
            m_serviceData = new HashMap<>(0);
        }
        else
        {
            m_serviceData = serviceData;
        }
        m_localName = localName;
        m_shortName = shortName;
        m_completeUuidList = uuidCompleteList;
        m_options = Option.CONNECTABLE.or(Option.INCLUDE_NAME);
    }

    /**
     * Clear all service data that may be in this {@link BleScanRecord} instance.
     * See also {@link #clearServiceUUIDs()}.
     */
    public final BleScanRecord clearServiceData()
    {
        m_serviceData.clear();
        return this;
    }

    /**
     * Set the service data for this {@link BleScanRecord} instance.
     */
    public final BleScanRecord addServiceData(Map<UUID, byte[]> data)
    {
        m_serviceData.putAll(data);
        return this;
    }

    /**
     * Set the options for this scan record. This only applies when this class is used with {@link com.idevicesinc.sweetblue.BleServer} to advertise itself.
     *
     * @see Option
     */
    public final BleScanRecord setOptions(Option... options)
    {
        if (options != null)
            m_options = Option.getFlags(options);
        return this;
    }

    /**
     * Returns whether the resulting advertising packet will have the connectable flag set or not.
     */
    public final boolean isConnectable()
    {
        return (m_options & Option.CONNECTABLE.bit()) == Option.CONNECTABLE.bit();
    }

    /**
     * Returns whether the resulting advertising packet will have the TX Power level included or not.
     */
    public final boolean includesTxPowerLevel()
    {
        return (m_options & Option.INCLUDE_TX_POWER.bit()) == Option.INCLUDE_TX_POWER.bit();
    }

    /**
     * Returns whether the resulting advertising packet will have the device name included or not.
     */
    public final boolean includesDeviceName()
    {
        return (m_options & Option.INCLUDE_NAME.bit()) == Option.INCLUDE_NAME.bit();
    }

    /**
     * Clears any service UUIDs in this instance.
     */
    public final BleScanRecord clearServiceUUIDs()
    {
        m_serviceUuids.clear();
        return this;
    }

    /**
     * Add the given List of {@link UUID}s to this instance's UUID list.
     */
    public final BleScanRecord addServiceUUIDs(List<UUID> uuids)
    {
        if (uuids != null)
        {
            for (UUID u : uuids)
            {
                BleUuid.UuidSize size = shortUuid(u) ? BleUuid.UuidSize.SHORT : BleUuid.UuidSize.FULL;
                m_serviceUuids.add(new BleUuid(u, size));
            }
        }
        return this;
    }

    /**
     * Add the given {@link UUID} and data to this instance's service data map.
     */
    public final BleScanRecord addServiceData(UUID uuid, byte[] data)
    {
        m_serviceData.put(uuid, data);
        return this;
    }

    /**
     * Add a {@link UUID} with the given {@link BleUuid.UuidSize} to this instance's {@link UUID} list.
     */
    public final BleScanRecord addServiceUuid(UUID uuid, BleUuid.UuidSize size)
    {
        m_serviceUuids.add(new BleUuid(uuid, size));
        return this;
    }

    /**
     * Overload of {@link #addServiceUuid(UUID, BleUuid.UuidSize)}, which sets the size to {@link BleUuid.UuidSize#SHORT}, if it can fit, otherwise it will
     * default to {@link BleUuid.UuidSize#FULL}
     */
    public final BleScanRecord addServiceUuid(UUID uuid)
    {
        final BleUuid.UuidSize size = shortUuid(uuid) ? BleUuid.UuidSize.SHORT : BleUuid.UuidSize.FULL;
        return addServiceUuid(uuid, size);
    }

    /**
     * Add some manufacturer data, along with the given manufacturer id to the backing {@link List}.
     */
    public final BleScanRecord addManufacturerData(short manId, byte[] data)
    {
        if (m_manufacturerDataList.size() == 0)
        {
            m_manufactuerId = manId;
            m_manufacturerData = data;
        }
        final ManufacturerData mdata = new ManufacturerData();
        mdata.m_id = manId;
        mdata.m_data = data;
        m_manufacturerDataList.add(mdata);
        return this;
    }

    /**
     * Sets this {@link BleScanRecord}'s manufacturer data list. This creates a new list from the one given.
     */
    public final BleScanRecord setManufacturerDataList(List<ManufacturerData> list)
    {
        m_manufacturerDataList = new ArrayList<>(list);
        return this;
    }

    /**
     * Overload of {@link #setName(String, boolean)}, which defaults to a complete name (not short).
     */
    public final BleScanRecord setName(String name)
    {
        return setName(name, false);
    }

    /**
     * Set the device name, and if it's a shortened name or not.
     */
    public final BleScanRecord setName(String name, boolean shortName)
    {
        m_localName = name;
        m_shortName = shortName;
        return this;
    }

    /**
     * Get the manufacturer Id from this {@link BleScanRecord} instance. This is a convenience method. This simply returns the first item in the {@link List} holding
     * all manufacturer data.
     *
     * @see #getManufacturerDataList()
     */
    public final short getManufacturerId()
    {
        if (m_manufactuerId == null)
        {
            return -1;
        }
        return m_manufactuerId;
    }

    /**
     * Get the manufacturer data from this instance. This is a convenience method. This simply returns the first item in the {@link List} holding
     * all manufacturer data.
     *
     * @see #getManufacturerDataList()
     */
    public final byte[] getManufacturerData()
    {
        if (m_manufacturerData == null)
        {
            return P_Const.EMPTY_BYTE_ARRAY;
        }
        return m_manufacturerData;
    }

    /**
     * Returns a {@link List} of all manufacturer data parsed in the scan record.
     */
    public final List<ManufacturerData> getManufacturerDataList()
    {
        return m_manufacturerDataList;
    }

    /**
     * Set the advertising flags. This method expects a byte bitmask (so all flags are already OR'd).
     */
    public final BleScanRecord setAdvFlags(byte mask)
    {
        if (m_advFlags == null)
        {
            m_advFlags = new Pointer<>((int) mask);
        }
        else
        {
            m_advFlags.value = (int) mask;
        }
        return this;
    }

    /**
     * Convenience method to set the advertising flags, which allows you to pass in every flag you want, and this
     * method will OR them together for you.
     */
    public final BleScanRecord setAdvFlags(byte... flags)
    {
        if (flags == null || flags.length == 0)
        {
            return this;
        }
        if (m_advFlags == null)
        {
            m_advFlags = new Pointer<>(0);
        }
        for (byte b : flags)
        {
            m_advFlags.value |= b;
        }
        return this;
    }

    /**
     * Get the advertising flags for this instance.
     */
    public final Pointer<Integer> getAdvFlags()
    {
        if (m_advFlags == null)
        {
            return new Pointer<>(0);
        }
        return m_advFlags;
    }

    /**
     * Set the TX power
     */
    public final BleScanRecord setTxPower(byte power)
    {
        if (m_txPower == null)
        {
            m_txPower = new Pointer<>((int) power);
        }
        else
        {
            m_txPower.value = (int) power;
        }
        return this;
    }

    /**
     * Gets the Tx power
     */
    public final Pointer<Integer> getTxPower()
    {
        if (m_txPower == null)
        {
            return new Pointer<>(0);
        }
        return m_txPower;
    }

    /**
     * Returns a list of service {@link UUID}s. This ONLY includes {@link UUID}s that do NOT have any data associated with them.
     *
     * See also {@link #getServiceData()}.
     */
    public final List<UUID> getServiceUUIDS()
    {
        List<UUID> list = new ArrayList<>();
        if (m_serviceUuids != null)
        {
            for (BleUuid u : m_serviceUuids)
            {
                list.add(u.uuid());
            }
        }
        return list;
    }

    /**
     * Returns a {@link Map} of the service data in this instance.
     *
     * See also {@link #getServiceUUIDS()}.
     */
    public final Map<UUID, byte[]> getServiceData()
    {
        return m_serviceData;
    }

    /**
     * Returns the device name
     */
    public final String getName()
    {
        if (m_localName == null)
        {
            return "";
        }
        return m_localName;
    }

    /**
     * Returns whether the name is a shortened version or not.
     */
    public final boolean isShortName()
    {
        return m_shortName;
    }

    /**
     * Returns <code>true</code> if this instance is considered null.
     */
    @Override public final boolean isNull()
    {
        return this == NULL;
    }

    /**
     * Build a byte[] scan record from the data stored in this instance.
     */
    public final byte[] buildPacket()
    {
        Map<BleUuid, byte[]> map = new HashMap<>(m_serviceUuids.size() + m_serviceData.size());
        if (m_serviceUuids.size() > 0)
        {
            for (BleUuid u : m_serviceUuids)
            {
                map.put(u, null);
            }
        }
        if (m_serviceData.size() > 0)
        {
            for (UUID u : m_serviceData.keySet())
            {
                map.put(new BleUuid(u, BleUuid.UuidSize.SHORT), m_serviceData.get(u));
            }
        }
        byte flags = m_advFlags.value != null ? m_advFlags.value.byteValue() : 0;
        byte tx = m_txPower.value != null ? m_txPower.value.byteValue() : 0;
        return Utils_ScanRecord.newScanRecord(flags, map, m_completeUuidList, m_localName, m_shortName, tx, m_manufacturerDataList);
    }

    /**
     * Returns true if this advertising packet contains the uuid given.
     */
    public boolean hasUuid(UUID uuid)
    {
        if (m_serviceUuids != null && m_serviceUuids.size() > 0)
        {
            for (BleUuid id : m_serviceUuids)
            {
                if (id.uuid().equals(uuid))
                {
                    return true;
                }
            }
        }
        if (m_serviceData != null && m_serviceData.size() > 0)
        {
            for (UUID id : m_serviceData.keySet())
            {
                if (id.equals(uuid))
                {
                    return true;
                }
            }
        }
        return false;
    }




    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    final AdvertiseSettings getNativeSettings(BleAdvertisingSettings.BleAdvertisingMode mode, BleAdvertisingSettings.BleTransmissionPower power, Interval timeout)
    {
        AdvertiseSettings.Builder settings = new AdvertiseSettings.Builder();
        settings.setAdvertiseMode(mode.getNativeMode());
        settings.setTxPowerLevel(power.getNativeMode());
        settings.setConnectable(isConnectable());
        settings.setTimeout((int) timeout.millis());
        return settings.build();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    final AdvertiseData getNativeData()
    {
        AdvertiseData.Builder data = new AdvertiseData.Builder();
        for (BleUuid id : m_serviceUuids)
        {
            data.addServiceUuid(new ParcelUuid(id.uuid()));
        }
        if (m_manufacturerDataList != null && m_manufacturerDataList.size() > 0)
        {
            for (ManufacturerData mdata : m_manufacturerDataList)
            {
                data.addManufacturerData(mdata.m_id, mdata.m_data);
            }
        }
        if (m_serviceData != null && m_serviceData.size() > 0)
        {
            for (UUID dataUuid : m_serviceData.keySet())
            {
                data.addServiceData(new ParcelUuid(dataUuid), m_serviceData.get(dataUuid));
            }
        }
        data.setIncludeDeviceName(includesDeviceName());
        data.setIncludeTxPowerLevel(includesTxPowerLevel());
        return data.build();
    }




    /**
     * Enumeration for advertising options
     */
    public enum Option implements BitwiseEnum
    {

        CONNECTABLE(1),
        INCLUDE_NAME(2),
        INCLUDE_TX_POWER(4);

        private final int m_bit;

        private Option(int bit)
        {
            m_bit = bit;
        }

        public static int getFlags(Option[] options) {
            if (options == null || options.length == 0) {
                return 0;
            }
            int flags = 0;
            for (Option o : options) {
                flags |= o.bit();
            }
            return flags;
        }

        @Override public int or(BitwiseEnum state)
        {
            return m_bit | state.bit();
        }

        @Override public int or(int bits)
        {
            return m_bit | bits;
        }

        @Override public int bit()
        {
            return m_bit;
        }

        @Override public boolean overlaps(int mask)
        {
            return (m_bit & mask) != 0x0;
        }
    }


    private static boolean shortUuid(UUID u)
    {
        long msb = u.getMostSignificantBits();
        short m = (short) (msb >>> 32);
        UUID test = Uuids.fromShort(Utils_String.bytesToHexString(Utils_Byte.shortToBytes(m)));
        return test.equals(u);
    }
}
