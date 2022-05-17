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

package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.utils.FutureData;
import com.idevicesinc.sweetblue.utils.PresentData;
import com.idevicesinc.sweetblue.utils.Utils_Byte;

import java.io.UnsupportedEncodingException;
import java.util.UUID;


public class BleDescriptorWrite extends BleDescriptorOp<BleDescriptorWrite>
{

    boolean bigEndian = true;


    public BleDescriptorWrite()
    {
    }

    public BleDescriptorWrite(UUID serviceUuid, UUID characteristicUuid)
    {
        super(serviceUuid, characteristicUuid);
    }

    public BleDescriptorWrite(UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid)
    {
        super(serviceUuid, characteristicUuid, descriptorUuid);
    }

    public BleDescriptorWrite(UUID characteristicUuid)
    {
        super(characteristicUuid);
    }

    /**
     * Constructor which creates a new {@link BleDescriptorWrite} from the one given. This will only copy over the service, characteristic, and descriptor Uuids. It will
     * NOT copy over any listeners, or filters.
     */
    public BleDescriptorWrite(BleDescriptorWrite write)
    {
        super(write.getServiceUuid(), write.getCharacteristicUuid(), write.getDescriptorUuid());
    }


    /**
     * Tells SweetBlue the endianness of the BLE device you want to perform a write to. This value only matters if you are using any of the
     * convenience methods: <br><br>
     * {@link #setBoolean(boolean)} <br>
     * {@link #setInt(int)} <br>
     * {@link #setLong(long)} <br>
     * {@link #setShort(short)} <br>
     * {@link #setString(String)} <br>
     * {@link #setString(String, String)} <br>
     */
    public final BleDescriptorWrite setIsBigEndian(boolean isBigEndian)
    {
        bigEndian = isBigEndian;
        return this;
    }

    /**
     * Set the raw bytes to write.
     */
    public final BleDescriptorWrite setBytes(byte[] data)
    {
        setData(new PresentData(data));
        return this;
    }

    /**
     * Set the boolean to write.
     */
    public final BleDescriptorWrite setBoolean(boolean value)
    {
        setData(new PresentData(value ? new byte[]{0x1} : new byte[]{0x0}));
        return this;
    }

    /**
     * Set an int to be written.
     */
    public final BleDescriptorWrite setInt(int val)
    {
        final byte[] d = Utils_Byte.intToBytes(val);
        if (bigEndian)
        {
            Utils_Byte.reverseBytes(d);
        }
        setData(new PresentData(d));
        return this;
    }

    /**
     * Set a short to be written.
     */
    public final BleDescriptorWrite setShort(short val)
    {
        final byte[] d = Utils_Byte.shortToBytes(val);
        if (bigEndian)
        {
            Utils_Byte.reverseBytes(d);
        }
        setData(new PresentData(d));
        return this;
    }

    /**
     * Set a long to be written.
     */
    public final BleDescriptorWrite setLong(long val)
    {
        final byte[] d = Utils_Byte.longToBytes(val);
        if (bigEndian)
        {
            Utils_Byte.reverseBytes(d);
        }
        setData(new PresentData(d));
        return this;
    }

    /**
     * Set a string to be written. This method also allows you to specify the string encoding. If the encoding
     * fails, then {@link String#getBytes()} is used instead, which uses "UTF-8" by default.
     */
    public final BleDescriptorWrite setString(String value, String stringEncoding)
    {
        byte[] bytes;
        try
        {
            bytes = value.getBytes(stringEncoding);
        } catch (UnsupportedEncodingException e)
        {
            bytes = value.getBytes();
        }
        setData(new PresentData(bytes));
        return this;
    }

    /**
     * Set a string to be written. This defaults to "UTF-8" encoding.
     */
    public final BleDescriptorWrite setString(String value)
    {
        return setString(value, "UTF-8");
    }

    @Override BleDescriptorWrite createDuplicate()
    {
        return getDuplicateOp();
    }

    @Override BleDescriptorWrite createNewOp()
    {
        return new BleDescriptorWrite();
    }


    /**
     * Builder class to build out a list (or array) of {@link BleDescriptorWrite} instances.
     */
    public final static class Builder extends BleDescriptorOp.Builder<Builder, BleDescriptorWrite>
    {

        public Builder()
        {
            this(null, null);
        }

        public Builder(UUID characteristicUuid)
        {
            this(null, characteristicUuid);
        }

        public Builder(UUID serviceUuid, UUID characteristicUuid)
        {
            currentOp = new BleDescriptorWrite(serviceUuid, characteristicUuid);
        }

        public Builder(UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid)
        {
            currentOp = new BleDescriptorWrite(serviceUuid, characteristicUuid, descriptorUuid);
        }

        /**
         * Tells SweetBlue the endianness of the BLE device you want to perform a write to. This value only matters if you are using any of the
         * convenience methods: <br><br>
         * {@link #setBoolean(boolean)} <br>
         * {@link #setInt(int)} <br>
         * {@link #setLong(long)} <br>
         * {@link #setShort(short)} <br>
         * {@link #setString(String)} <br>
         * {@link #setString(String, String)} <br>
         */
        public final Builder setIsBigEndian(boolean isBigEndian)
        {
            currentOp.setIsBigEndian(isBigEndian);
            return this;
        }

        /**
         * Set the raw bytes to write.
         */
        public final Builder setBytes(byte[] data)
        {
            currentOp.setBytes(data);
            return this;
        }

        /**
         * Set the {@link FutureData} to write.
         */
        public final Builder setData(FutureData data)
        {
            currentOp.setData(data);
            return this;
        }

        /**
         * Set the boolean to write.
         */
        public final Builder setBoolean(boolean value)
        {
            currentOp.setBoolean(value);
            return this;
        }

        /**
         * Set an int to be written.
         */
        public final Builder setInt(int val)
        {
            currentOp.setInt(val);
            return this;
        }

        /**
         * Set a short to be written.
         */
        public final Builder setShort(short val)
        {
            currentOp.setShort(val);
            return this;
        }

        /**
         * Set a long to be written.
         */
        public final Builder setLong(long val)
        {
            currentOp.setLong(val);
            return this;
        }

        /**
         * Set a string to be written. This method also allows you to specify the string encoding. If the encoding
         * fails, then {@link String#getBytes()} is used instead, which uses "UTF-8" by default.
         */
        public final Builder setString(String value, String stringEncoding)
        {
            currentOp.setString(value, stringEncoding);
            return this;
        }

        /**
         * Set a string to be written. This defaults to "UTF-8" encoding.
         */
        public final Builder setString(String value)
        {
            return setString(value, "UTF-8");
        }

        @Override
        public final BleDescriptorWrite[] buildArray()
        {
            return build().toArray(new BleDescriptorWrite[0]);
        }
    }

}
