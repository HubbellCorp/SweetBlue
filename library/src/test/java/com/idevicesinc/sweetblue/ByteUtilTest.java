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

import com.idevicesinc.sweetblue.utils.Utils_Byte;
import com.idevicesinc.sweetblue.utils.Utils_String;
import com.idevicesinc.sweetblue.framework.AbstractTestClass;

import org.junit.Test;
import java.util.Arrays;



public class ByteUtilTest extends AbstractTestClass
{

    @Test(timeout = 5000)
    public void intTest() throws Exception
    {
        startSynchronousTest();
        int value = 424242;
        byte[] bytes = Utils_Byte.intToBytes(value);
        assertArrayEquals(new byte[] { 0x0, 0x6, 0x79, 0x32 }, bytes);
        assertEquals(value, Utils_Byte.bytesToInt(bytes));
        byte[] newBytes = new byte[12];
        Utils_Byte.memset(newBytes, (byte) 0x44, 8);
        Utils_Byte.memcpy(newBytes, bytes, 4, 8, 0);
        assertEquals(value, Utils_Byte.bytesToInt(newBytes, 8));
        byte[] tmpBytes = bytes.clone();
        Utils_Byte.reverseBytes(bytes);
        assertFalse(Arrays.equals(bytes, tmpBytes));
        Utils_Byte.reverseBytes(bytes);
        assertArrayEquals(bytes, tmpBytes);
        succeed();
    }

    @Test(timeout = 5000)
    public void shortTest() throws Exception
    {
        startSynchronousTest();
        short value = 8724;
        byte[] bytes = Utils_Byte.shortToBytes(value);
        assertArrayEquals(new byte[] { 0x22, 0x14 }, bytes);
        assertEquals(value, Utils_Byte.bytesToShort(bytes));
        byte[] newBytes = new byte[12];
        Utils_Byte.memset(newBytes, (byte) 0x44, 10);
        Utils_Byte.memcpy(newBytes, bytes, 2, 10, 0);
        assertEquals(value, Utils_Byte.bytesToShort(newBytes, 10));
        byte[] tmpBytes = bytes.clone();
        Utils_Byte.reverseBytes(bytes);
        assertFalse(Arrays.equals(bytes, tmpBytes));
        Utils_Byte.reverseBytes(bytes);
        assertArrayEquals(bytes, tmpBytes);
        succeed();
    }

    @Test(timeout = 5000)
    public void longTest() throws Exception
    {
        startSynchronousTest();
        long value = 8279580351934L;
        byte[] bytes = Utils_Byte.longToBytes(value);
        assertArrayEquals(new byte[] { 0x0, 0x0, 0x7, (byte) 0x87, (byte) 0xBD, 0x72, 0x1D, (byte) 0xBE }, bytes);
        assertEquals(value, Utils_Byte.bytesToLong(bytes));
        byte[] newBytes = new byte[12];
        Utils_Byte.memset(newBytes, (byte) 0x44, 4);
        Utils_Byte.memcpy(newBytes, bytes, 8, 4, 0);
        assertEquals(value, Utils_Byte.bytesToLong(newBytes, 4));
        byte[] tmpBytes = bytes.clone();
        Utils_Byte.reverseBytes(bytes);
        assertFalse(Arrays.equals(bytes, tmpBytes));
        Utils_Byte.reverseBytes(bytes);
        assertArrayEquals(bytes, tmpBytes);
        succeed();
    }

    @Test(timeout = 5000)
    public void floatTest() throws Exception
    {
        startSynchronousTest();
        float value = 71.8781f;
        byte[] bytes = Utils_Byte.floatToBytes(value);
        assertArrayEquals(new byte[] { 0x42, (byte) 0x8f, (byte) 0xc1, (byte) 0x96 }, bytes);
        assertEquals(value, Utils_Byte.bytesToFloat(bytes), 0);
        byte[] newBytes = new byte[12];
        Utils_Byte.memset(newBytes, (byte) 0x44, 8);
        Utils_Byte.memcpy(newBytes, bytes, 4, 8, 0);
        assertEquals(value, Utils_Byte.bytesToFloat(newBytes, 8), 0);
        byte[] tmpBytes = bytes.clone();
        Utils_Byte.reverseBytes(bytes);
        assertFalse(Arrays.equals(bytes, tmpBytes));
        Utils_Byte.reverseBytes(bytes);
        assertArrayEquals(bytes, tmpBytes);
        succeed();
    }

    @Test(timeout = 5000)
    public void doubleTest() throws Exception
    {
        startSynchronousTest();
        double value = 7187.813292399d;
        byte[] bytes = Utils_Byte.doubleToBytes(value);
        assertArrayEquals(new byte[] { 0x40, (byte) 0xbc, (byte) 0x13, (byte) 0xd0, (byte) 0x33, (byte) 0xee, (byte) 0x3f, (byte) 0xca }, bytes);
        assertEquals(value, Utils_Byte.bytesToDouble(bytes), 0);
        byte[] newBytes = new byte[12];
        Utils_Byte.memset(newBytes, (byte) 0x44, 4);
        Utils_Byte.memcpy(newBytes, bytes, 8, 4, 0);
        assertEquals(value, Utils_Byte.bytesToDouble(newBytes, 4), 0);
        byte[] tmpBytes = bytes.clone();
        Utils_Byte.reverseBytes(bytes);
        assertFalse(Arrays.equals(bytes, tmpBytes));
        Utils_Byte.reverseBytes(bytes);
        assertArrayEquals(bytes, tmpBytes);
        succeed();
    }

    @Test(timeout = 5000)
    public void hexTest() throws Exception
    {
        startSynchronousTest();
        String hex = "A7B244FFBA5C";
        byte[] bytes = Utils_String.hexStringToBytes(hex);
        assertArrayEquals(new byte[] { (byte) 0xA7, (byte) 0xB2, 0x44, (byte) 0xFF, (byte) 0xBA, 0x5C }, bytes);
        String reHex = Utils_String.bytesToHexString(bytes);
        assertTrue(hex.equals(reHex));
        succeed();
    }

}