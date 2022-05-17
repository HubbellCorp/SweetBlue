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

import com.idevicesinc.sweetblue.framework.AbstractTestClass;
import com.idevicesinc.sweetblue.utils.Utils_Byte;

import org.junit.Test;


public class Utils_ByteTest extends AbstractTestClass
{

    @Test(timeout = 500)
    public void booleanTest()
    {
        byte b = Utils_Byte.boolToByte(true);
        assertEquals(1, b);
        assertTrue(Utils_Byte.byteToBool(b));
        b = Utils_Byte.boolToByte(false);
        assertEquals(0, b);
        assertFalse(Utils_Byte.byteToBool(b));
    }

    @Test(timeout = 500)
    public void booleanOffsetTest()
    {
        byte[] bytes = new byte[] { 0x44, 0x55, 0x66, 0x01, 0x00, (byte) 0xBB };
        Boolean bool = Utils_Byte.byteToBool(bytes, 3);
        assertNotNull(bool);
        assertTrue(bool);
        bool = Utils_Byte.byteToBool(bytes, 4);
        assertNotNull(bool);
        assertFalse(bool);
    }

    @Test(timeout = 500)
    public void shortTest()
    {
        short value = 24123;
        byte[] determinedValue = new byte[] { 0x5E, 0x3B };
        assertArrayEquals(determinedValue, Utils_Byte.shortToBytes(value));
        assertEquals(value, Utils_Byte.bytesToShort(determinedValue));
    }

    @Test(timeout = 500)
    public void shortOffsetTest()
    {
        byte[] bytes = new byte[] { 0x25, 0x44, 0x5E, 0x3B, 0x66, 0x71 };
        Short value = Utils_Byte.bytesToShort(bytes, 2);
        assertNotNull(value);
        assertEquals(24123, value.shortValue());
    }

    @Test(timeout = 500)
    public void intTest()
    {
        int value = 424242;
        byte[] determinedValue = new byte[] { 0x0, 0x6, 0x79, 0x32 };
        assertArrayEquals(determinedValue, Utils_Byte.intToBytes(value));
        assertEquals(value, Utils_Byte.bytesToInt(determinedValue));
    }

    @Test(timeout = 500)
    public void intOffsetTest()
    {
        byte[] bytes = new byte[] { 0x22, 0x29, 0x44, 0x0, 0x6, 0x79, 0x32, (byte) 0xBB, (byte) 0xA4 };
        Integer value = Utils_Byte.bytesToInt(bytes, 3);
        assertNotNull(value);
        assertEquals(424242, value.intValue());
    }

    @Test(timeout = 500)
    public void longTest()
    {
        long value = 12345678901L;
        byte[] determinedValue = new byte[] { 0x00, 0x00, 0x00, 0x02, (byte) 0xDF, (byte) 0xDC, 0x1C, 0x35 };
        assertArrayEquals(determinedValue, Utils_Byte.longToBytes(value));
        assertEquals(value, Utils_Byte.bytesToLong(determinedValue));
    }

    @Test(timeout = 500)
    public void longOffsetTest()
    {
        byte[] bytes = new byte[] { 0x66, (byte) 0x88, 0x77, 0x00, 0x00, 0x00, 0x02, (byte) 0xDF, (byte) 0xDC, 0x1C, 0x35, (byte) 0x99, 0x12 };
        Long value = Utils_Byte.bytesToLong(bytes, 3);
        assertNotNull(value);
        assertEquals(12345678901L, value.longValue());
    }

    @Test(timeout = 500)
    public void floatTest()
    {
        float value = 123.123f;
        byte[] determinedValue = new byte[] { (byte) 0x42, (byte) 0xF6, 0x3E, (byte) 0xFA };
        assertArrayEquals(determinedValue, Utils_Byte.floatToBytes(value));
        assertEquals(value, Utils_Byte.bytesToFloat(determinedValue), 0);
    }

    @Test(timeout = 500)
    public void floatOffsetTest()
    {
        byte[] bytes = new byte[] { 0x11, 0x22, 0x33, (byte) 0x42, (byte) 0xF6, 0x3E, (byte) 0xFA, 0x40, 0x60 };
        Float value = Utils_Byte.bytesToFloat(bytes, 3);
        assertNotNull(value);
        assertEquals(123.123f, value.floatValue(), .0);
    }

    @Test(timeout = 500)
    public void doubleTest()
    {
        double value = 71676287.813292399d;
        byte[] determinedValue = new byte[] { 0x41, (byte) 0x91, 0x16, (byte) 0xC5, (byte) 0xFF, 0x40, (byte) 0xCF, (byte) 0xB9 };
        assertArrayEquals(determinedValue, Utils_Byte.doubleToBytes(value));
        assertEquals(value, Utils_Byte.bytesToDouble(determinedValue), 0);
    }

    @Test(timeout = 500)
    public void doubleOffsetTest()
    {
        byte[] bytes = new byte[] { 0x22, 0x33, 0x44, 0x55, 0x41, (byte) 0x91, 0x16, (byte) 0xC5, (byte) 0xFF, 0x40, (byte) 0xCF, (byte) 0xB9, (byte) 0xBB, 0x66, 0x21 };
        Double value = Utils_Byte.bytesToDouble(bytes, 4);
        assertNotNull(value);
        assertEquals(71676287.813292399d, value.doubleValue(), 0);
    }

    @Test(timeout = 500)
    public void reverseTest()
    {
        byte[] bytes = new byte[] { 0x01, 0x02, 0x03, 0x04 };
        byte[] revBytes = new byte[] { 0x04, 0x03, 0x02, 0x01 };
        Utils_Byte.reverseBytes(bytes);
        assertArrayEquals(bytes, revBytes);
    }

    @Test(timeout = 500)
    public void subBytesTest()
    {
        byte[] bytes = new byte[] { 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9, 0xA };
        byte[] subBytes = Utils_Byte.subBytes(bytes, 3);
        assertArrayEquals(new byte[] { 0x4, 0x5, 0x6, 0x7, 0x8, 0x9, 0xA }, subBytes);
        subBytes = Utils_Byte.subBytes(bytes, 0, 3);
        assertArrayEquals(new byte[] { 0x1, 0x2, 0x3 }, subBytes);
    }

    @Test(timeout = 500)
    public void memCopyTest()
    {
        byte[] bytes = new byte[] { 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8 };
        byte[] copy = new byte[bytes.length];
        Utils_Byte.memcpy(copy, bytes, bytes.length);
        assertArrayEquals(bytes, copy);
    }

    @Test(timeout = 500)
    public void memCopyOffSetTest()
    {
        byte[] bytes = new byte[] { 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8 };
        byte[] copy = new byte[2];
        Utils_Byte.memcpy(copy, bytes, 1, 1, 1);
        assertArrayEquals(new byte[] { 0x0, 0x2 }, copy);
    }

    @Test(timeout = 500)
    public void memSetTest()
    {
        byte[] bytes = new byte[4];
        Utils_Byte.memset(bytes, (byte) 2, 4);
        assertArrayEquals(new byte[] { 0x2, 0x2, 0x2, 0x2 }, bytes);
    }

    @Test(timeout = 500)
    public void memSetOffsetTest()
    {
        byte[] bytes = new byte[4];
        Utils_Byte.memset(bytes, (byte) 2, 1, 3);
        assertArrayEquals(new byte[] { 0x0, 0x2, 0x2, 0x2 }, bytes);
    }

    @Test(timeout = 5000000)
    public void memCmpTest()
    {
        byte[] bytes = new byte[] { 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8 };
        byte[] bytes2 = new byte[] { 0x1, 0x2, 0x3, 0x4, 0x5, 0x8, 0x9, 0xA };
        int wtf = Utils_Byte.memcmp(bytes, bytes2, 8);
        assertTrue(wtf < 0);
    }

    @Test(timeout = 500)
    public void emptyByteArrayTest()
    {
        byte[] nullArray = null;
        byte[] empty = new byte[0];
        byte[] notempty = new byte[] { 0x69 };
        assertTrue(Utils_Byte.isEmpty(nullArray));
        assertTrue(Utils_Byte.isEmpty(empty));
        assertFalse(Utils_Byte.isEmpty(notempty));
    }

}
