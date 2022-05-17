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
import com.idevicesinc.sweetblue.utils.Utils_String;

import org.junit.Test;


public class Utils_StringTest extends AbstractTestClass
{

    @Test(timeout = 5000)
    public void getStringValueTest() throws Exception
    {
        startSynchronousTest();
        String s = "This is just a test";
        byte[] sb = s.getBytes();
        assertEquals(s, Utils_String.getStringValue(sb));
        assertEquals(s, Utils_String.getStringValue(sb, "UTF-8"));
        succeed();
    }

    @Test(timeout = 5000)
    public void normalizeNameTest() throws Exception
    {
        startSynchronousTest();
        String name = "This Is A Test-To make sure it works";
        assertEquals("this_is_a_test", Utils_String.normalizeDeviceName(name));
        succeed();
    }

    @Test(timeout = 500)
    public void emptyStringTest()
    {
        String nullString = null;
        String empty = "";
        String notempty = "notempty";
        assertTrue(Utils_String.isEmpty(nullString));
        assertTrue(Utils_String.isEmpty(empty));
        assertFalse(Utils_String.isEmpty(notempty));
    }

    @Test(timeout = 500)
    public void hexTest()
    {
        String hex = "A7B244FFBA5C";
        byte[] bytes = Utils_String.hexStringToBytes(hex);
        assertArrayEquals(new byte[] { (byte) 0xA7, (byte) 0xB2, 0x44, (byte) 0xFF, (byte) 0xBA, 0x5C }, bytes);
        String reHex = Utils_String.bytesToHexString(bytes);
        assertTrue(hex.equals(reHex));
    }

    @Test(timeout = 500000)
    public void hexLowerCaseTest()
    {
        String hex = "a7b244ffba5c";
        byte[] bytes = Utils_String.hexStringToBytes(hex);
        assertArrayEquals(new byte[] { (byte) 0xA7, (byte) 0xB2, 0x44, (byte) 0xFF, (byte) 0xBA, 0x5C }, bytes);
        String reHex = Utils_String.bytesToHexString(bytes);
        assertTrue(hex.equals(reHex.toLowerCase()));
        bytes = Utils_String.hexStringToBytes(hex, (Utils_String.HexOption[]) null);
        assertNull(bytes);
    }

    @Test(timeout = 500)
    public void hexWithSpacesTest()
    {
        String hex = "a7b244ff 15c6671";
        byte[] bytes = Utils_String.hexStringToBytes(hex);
        assertArrayEquals(new byte[] { (byte) 0xA7, (byte) 0xB2, 0x44, (byte) 0xFF, (byte) 0x01, 0x5C, 0x66, 0x71 }, bytes);
        bytes = Utils_String.hexStringToBytes(hex, (Utils_String.HexOption[]) null);
        assertNull(bytes);

        hex = "0xa7b244ff 0x15c6671";
        bytes = Utils_String.hexStringToBytes(hex);
        assertArrayEquals(new byte[] { (byte) 0xA7, (byte) 0xB2, 0x44, (byte) 0xFF, (byte) 0x01, 0x5C, 0x66, 0x71 }, bytes);
    }

    @Test(timeout = 500)
    public void hexWithPrefixTest()
    {
        String hex = "0xABCDEF";
        byte[] bytes = Utils_String.hexStringToBytes(hex);
        assertArrayEquals(new byte[] { (byte) 0xAB, (byte) 0xCD, (byte) 0xEF }, bytes);

        bytes = Utils_String.hexStringToBytes(hex, (Utils_String.HexOption[]) null);
        assertNull(bytes);
    }

    @Test(timeout = 500)
    public void binaryStringTest() throws Exception
    {
        String binary = "00000000 00000110 01111001 00110010";
        byte[] bytes = Utils_String.binaryStringToBytes(binary);
        byte[] determinedValue = new byte[] { 0x00, 0x06, 0x79, 0x32 };
        assertArrayEquals(determinedValue, bytes);
        String reBinary = Utils_String.bytesToBinaryString(bytes, 1);
        assertEquals(reBinary, binary);
        binary = binary.replaceAll(" ", "");
        reBinary = Utils_String.bytesToBinaryString(determinedValue);
        assertEquals(binary, reBinary);
    }

}
