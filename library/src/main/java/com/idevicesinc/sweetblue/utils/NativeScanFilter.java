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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanFilter;
import android.os.ParcelUuid;

import com.idevicesinc.sweetblue.compat.L_Util;

import java.util.Arrays;
import java.util.Objects;

/**
 *
 * This class is essentially a clone of {@link android.bluetooth.le.ScanFilter}. However, that class is not
 * available on devices running an OS lower than Lollipop. Using this class means you don't have to check whether the
 * device is running lollipop in order to use the class.
 *
 * Criteria for filtering result from Bluetooth LE scans. A {@link ScanFilter} allows clients to
 * restrict scan results to only those that are of interest to them.
 * <p>
 * Current filtering on the following fields are supported:
 * <li>Service UUIDs which identify the bluetooth gatt services running on the device.
 * <li>Name of remote Bluetooth LE device.
 * <li>Mac address of the remote device.
 * <li>Service data which is the data associated with a service.
 * <li>Manufacturer specific data which is the data associated with a particular manufacturer.
 *
 * @see android.bluetooth.le.ScanResult
 * @see BluetoothLeScanner
 */
public final class NativeScanFilter
{

    private final String m_deviceName;
    private final String m_deviceAddress;
    private final ParcelUuid m_serviceUuid;
    private final ParcelUuid m_serviceUuidMask;
    private final ParcelUuid m_serviceDataUuid;
    private final byte[] m_serviceData;
    private final byte[] m_serviceDataMask;
    private final int m_manufacturerId;
    private final byte[] m_manufacturerData;
    private final byte[] m_manufacturerDataMask;


    public static final NativeScanFilter EMPTY = new NativeScanFilter.Builder().build();


    private NativeScanFilter(String name, String deviceAddress, ParcelUuid uuid,
                             ParcelUuid uuidMask, ParcelUuid serviceDataUuid,
                             byte[] serviceData, byte[] serviceDataMask,
                             int manufacturerId, byte[] manufacturerData, byte[] manufacturerDataMask) {
        m_deviceName = name;
        m_serviceUuid = uuid;
        m_serviceUuidMask = uuidMask;
        m_deviceAddress = deviceAddress;
        m_serviceDataUuid = serviceDataUuid;
        m_serviceData = serviceData;
        m_serviceDataMask = serviceDataMask;
        m_manufacturerId = manufacturerId;
        m_manufacturerData = manufacturerData;
        m_manufacturerDataMask = manufacturerDataMask;
    }


    /**
     * Returns the filter set the device name field of Bluetooth advertisement data.
     */
    public String getDeviceName() {
        return m_deviceName;
    }

    /**
     * Returns the filter set on the service uuid.
     */
    public ParcelUuid getServiceUuid() {
        return m_serviceUuid;
    }

    public ParcelUuid getServiceUuidMask() {
        return m_serviceUuidMask;
    }

    public String getDeviceAddress() {
        return m_deviceAddress;
    }

    public byte[] getServiceData() {
        return m_serviceData;
    }

    public byte[] getServiceDataMask() {
        return m_serviceDataMask;
    }

    public ParcelUuid getServiceDataUuid() {
        return m_serviceDataUuid;
    }

    /**
     * Returns the manufacturer id. -1 if the manufacturer filter is not set.
     */
    public int getManufacturerId() {
        return m_manufacturerId;
    }

    public byte[] getManufacturerData() {
        return m_manufacturerData;
    }

    public byte[] getManufacturerDataMask() {
        return m_manufacturerDataMask;
    }


    @Override
    public String toString() {
        return "BluetoothLeScanFilter [m_deviceName=" + m_deviceName + ", m_deviceAddress="
                + m_deviceAddress
                + ", mUuid=" + m_serviceUuid + ", m_uuidMask=" + m_serviceUuidMask
                + ", m_serviceDataUuid=" + m_serviceDataUuid + ", m_serviceData="
                + Arrays.toString(m_serviceData) + ", m_serviceDataMask="
                + Arrays.toString(m_serviceDataMask) + ", m_manufacturerId=" + m_manufacturerId
                + ", m_manufacturerData=" + Arrays.toString(m_manufacturerData)
                + ", m_manufacturerDataMask=" + Arrays.toString(m_manufacturerDataMask) + "]";
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[] { m_deviceName, m_deviceAddress, m_manufacturerId,
                Arrays.hashCode(m_manufacturerData),
                Arrays.hashCode(m_manufacturerDataMask),
                m_serviceDataUuid,
                Arrays.hashCode(m_serviceData),
                Arrays.hashCode(m_serviceDataMask),
                m_serviceUuid, m_serviceUuidMask });
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        NativeScanFilter other = (NativeScanFilter) obj;
        return equals(m_deviceName, other.m_deviceName)
                && equals(m_deviceAddress, other.m_deviceAddress)
                && m_manufacturerId == other.m_manufacturerId
                && Arrays.equals(m_manufacturerData, other.m_manufacturerData)
                && Arrays.equals(m_manufacturerDataMask, other.m_manufacturerDataMask)
                && equals(m_serviceDataUuid, other.m_serviceDataUuid)
                && Arrays.equals(m_serviceData, other.m_serviceData)
                && Arrays.equals(m_serviceDataMask, other.m_serviceDataMask)
                && equals(m_serviceUuid, other.m_serviceUuid)
                && equals(m_serviceUuidMask, other.m_serviceUuidMask);
    }

    public static boolean equals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }


    /**
     * Builder class for {@link NativeScanFilter}.
     */
    public static final class Builder {

        private String m_deviceName;
        private String m_deviceAddress;

        private ParcelUuid m_serviceUuid;
        private ParcelUuid m_uuidMask;

        private ParcelUuid m_serviceDataUuid;
        private byte[] m_serviceData;
        private byte[] m_serviceDataMask;

        private int m_manufacturerId = -1;
        private byte[] m_manufacturerData;
        private byte[] m_manufacturerDataMask;

        /**
         * Set filter on device name.
         */
        public Builder setDeviceName(String deviceName) {
            m_deviceName = deviceName;
            return this;
        }

        /**
         * Set filter on device address.
         *
         * @param deviceAddress The device Bluetooth address for the filter. It needs to be in the
         * format of "01:02:03:AB:CD:EF". The device address can be validated using {@link
         * BluetoothAdapter#checkBluetoothAddress}.
         * @throws IllegalArgumentException If the {@code deviceAddress} is invalid.
         */
        public Builder setDeviceAddress(String deviceAddress) {
            if (deviceAddress != null && !BluetoothAdapter.checkBluetoothAddress(deviceAddress)) {
                throw new IllegalArgumentException("invalid device address " + deviceAddress);
            }
            m_deviceAddress = deviceAddress;
            return this;
        }

        /**
         * Set filter on service uuid.
         */
        public Builder setServiceUuid(ParcelUuid serviceUuid) {
            m_serviceUuid = serviceUuid;
            m_uuidMask = null; // clear uuid mask
            return this;
        }

        /**
         * Set filter on partial service uuid. The {@code uuidMask} is the bit mask for the
         * {@code serviceUuid}. Set any bit in the mask to 1 to indicate a match is needed for the
         * bit in {@code serviceUuid}, and 0 to ignore that bit.
         *
         * @throws IllegalArgumentException If {@code serviceUuid} is {@code null} but {@code
         * uuidMask} is not {@code null}.
         */
        public Builder setServiceUuid(ParcelUuid serviceUuid, ParcelUuid uuidMask) {
            if (m_uuidMask != null && m_serviceUuid == null) {
                throw new IllegalArgumentException("uuid is null while uuidMask is not null!");
            }
            m_serviceUuid = serviceUuid;
            m_uuidMask = uuidMask;
            return this;
        }

        /**
         * Set filtering on service data.
         *
         * @throws IllegalArgumentException If {@code serviceDataUuid} is null.
         */
        public Builder setServiceData(ParcelUuid serviceDataUuid, byte[] serviceData) {
            if (serviceDataUuid == null) {
                throw new IllegalArgumentException("serviceDataUuid is null");
            }
            m_serviceDataUuid = serviceDataUuid;
            m_serviceData = serviceData;
            m_serviceDataMask = null; // clear service data mask
            return this;
        }

        /**
         * Set partial filter on service data. For any bit in the mask, set it to 1 if it needs to
         * match the one in service data, otherwise set it to 0 to ignore that bit.
         * <p>
         * The {@code serviceDataMask} must have the same length of the {@code serviceData}.
         *
         * @throws IllegalArgumentException If {@code serviceDataUuid} is null or {@code
         * serviceDataMask} is {@code null} while {@code serviceData} is not or {@code
         * serviceDataMask} and {@code serviceData} has different length.
         */
        public Builder setServiceData(ParcelUuid serviceDataUuid,
                                      byte[] serviceData, byte[] serviceDataMask) {
            if (serviceDataUuid == null) {
                throw new IllegalArgumentException("serviceDataUuid is null");
            }
            if (m_serviceDataMask != null) {
                if (m_serviceData == null) {
                    throw new IllegalArgumentException(
                            "serviceData is null while serviceDataMask is not null");
                }
                // Since the m_serviceDataMask is a bit mask for m_serviceData, the lengths of the two
                // byte array need to be the same.
                if (m_serviceData.length != m_serviceDataMask.length) {
                    throw new IllegalArgumentException(
                            "size mismatch for service data and service data mask");
                }
            }
            m_serviceDataUuid = serviceDataUuid;
            m_serviceData = serviceData;
            m_serviceDataMask = serviceDataMask;
            return this;
        }

        /**
         * Set filter on on manufacturerData. A negative manufacturerId is considered as invalid id.
         * <p>
         * Note the first two bytes of the {@code manufacturerData} is the manufacturerId.
         *
         * @throws IllegalArgumentException If the {@code manufacturerId} is invalid.
         */
        public Builder setManufacturerData(int manufacturerId, byte[] manufacturerData) {
            if (manufacturerData != null && manufacturerId < 0) {
                throw new IllegalArgumentException("invalid manufacture id");
            }
            m_manufacturerId = manufacturerId;
            m_manufacturerData = manufacturerData;
            m_manufacturerDataMask = null; // clear manufacturer data mask
            return this;
        }

        /**
         * Set filter on partial manufacture data. For any bit in the mask, set it the 1 if it needs
         * to match the one in manufacturer data, otherwise set it to 0.
         * <p>
         * The {@code manufacturerDataMask} must have the same length of {@code manufacturerData}.
         *
         * @throws IllegalArgumentException If the {@code manufacturerId} is invalid, or {@code
         * manufacturerData} is null while {@code manufacturerDataMask} is not, or {@code
         * manufacturerData} and {@code manufacturerDataMask} have different length.
         */
        public Builder setManufacturerData(int manufacturerId, byte[] manufacturerData,
                                           byte[] manufacturerDataMask) {
            if (manufacturerData != null && manufacturerId < 0) {
                throw new IllegalArgumentException("invalid manufacture id");
            }
            if (m_manufacturerDataMask != null) {
                if (m_manufacturerData == null) {
                    throw new IllegalArgumentException(
                            "manufacturerData is null while manufacturerDataMask is not null");
                }
                // Since the m_manufacturerDataMask is a bit mask for m_manufacturerData, the lengths
                // of the two byte array need to be the same.
                if (m_manufacturerData.length != m_manufacturerDataMask.length) {
                    throw new IllegalArgumentException(
                            "size mismatch for manufacturerData and manufacturerDataMask");
                }
            }
            m_manufacturerId = manufacturerId;
            m_manufacturerData = manufacturerData;
            m_manufacturerDataMask = manufacturerDataMask;
            return this;
        }

        /**
         * Build {@link NativeScanFilter}.
         *
         * @throws IllegalArgumentException If the filter cannot be built.
         */
        public NativeScanFilter build() {
            return new NativeScanFilter(m_deviceName, m_deviceAddress,
                    m_serviceUuid, m_uuidMask,
                    m_serviceDataUuid, m_serviceData, m_serviceDataMask,
                    m_manufacturerId, m_manufacturerData, m_manufacturerDataMask);
        }
    }

}
