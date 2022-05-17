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

import com.idevicesinc.sweetblue.utils.BleScanRecord;
import com.idevicesinc.sweetblue.utils.BleUuid;
import com.idevicesinc.sweetblue.utils.Utils_ScanRecord;
import com.idevicesinc.sweetblue.utils.Utils_String;
import com.idevicesinc.sweetblue.utils.Uuids;
import com.idevicesinc.sweetblue.framework.AbstractTestClass;

import org.junit.Test;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class BleScanRecordTest extends AbstractTestClass
{


    @Test(timeout = 5000)
    public void scanRecordTestWithServiceData() throws Exception
    {
        startSynchronousTest();
        final UUID uuid = Uuids.BATTERY_SERVICE_UUID;
        final short manId = (short) 16454;
        final byte[] manData = new byte[] { 0x5,(byte) 0xAA, 0x44, (byte) 0xB3, 0x66 };
        BleScanRecord bleRecord = new BleScanRecord()
                .setName("Johnny 5")
                .setAdvFlags((byte) 1, (byte) 0x2)
                .setTxPower((byte) 10)
                .addServiceData(uuid, new byte[] { 100 })
                 .addManufacturerData(manId, manData);
        byte[] record = bleRecord.buildPacket();
        BleScanRecord info = Utils_ScanRecord.parseScanRecord(record);
        assertEquals("Johnny 5", info.getName());
        assertEquals(3, info.getAdvFlags().value);
        assertEquals(manId, info.getManufacturerId());
        assertArrayEquals(manData, info.getManufacturerData());
        assertEquals(10, info.getTxPower().value);
        Map<UUID, byte[]> services = info.getServiceData();
        assertEquals(1, services.size());
        assertArrayEquals(new byte[] {100}, services.get(uuid));
        succeed();
    }

    @Test(timeout = 5000)
    public void scanRecordTestWithShort() throws Exception
    {
        startSynchronousTest();
        final UUID uuid = Uuids.BATTERY_SERVICE_UUID;
        final short manId = (short) 16454;
        final byte[] manData = new byte[] { 0x5,(byte) 0xAA, 0x44, (byte) 0xB3, 0x66 };
        BleScanRecord bleRecord = new BleScanRecord()
                .setName("Johnny 5")
                .setAdvFlags((byte) 0x3)
                .setTxPower((byte) 10)
                .addServiceUuid(uuid, BleUuid.UuidSize.SHORT)
                .addManufacturerData(manId, manData);
        byte[] record = bleRecord.buildPacket();
        BleScanRecord info = Utils_ScanRecord.parseScanRecord(record);
        assertEquals("Johnny 5", info.getName());
        assertEquals(3, info.getAdvFlags().value);
        assertEquals(manId, info.getManufacturerId());
        assertArrayEquals(manData, info.getManufacturerData());
        assertEquals(10, info.getTxPower().value);
        List<UUID> services = info.getServiceUUIDS();
        assertEquals(1, services.size());
        assertTrue(services.get(0).equals(uuid));
        succeed();
    }

    @Test(timeout = 5000)
    public void scanRecordTestWithFull() throws Exception
    {
        startSynchronousTest();
        final short manId = (short) 16454;
        final byte[] manData = new byte[] { 0x5,(byte) 0xAA, 0x44, (byte) 0xB3, 0x66 };
        BleScanRecord bleRecord = new BleScanRecord()
                .setName("Johnny 5")
                .setAdvFlags((byte) 1, (byte) 0x2)
                .setTxPower((byte) 10)
                .addServiceUuid(Uuids.BATTERY_SERVICE_UUID)
                .addServiceUuid(Uuids.DEVICE_INFORMATION_SERVICE_UUID)
                .addManufacturerData(manId, manData);
        byte[] record = bleRecord.buildPacket();
        BleScanRecord info = Utils_ScanRecord.parseScanRecord(record);
        assertEquals("Johnny 5", info.getName());
        assertEquals(3, info.getAdvFlags().value);
        assertEquals(manId, info.getManufacturerId());
        assertArrayEquals(manData, info.getManufacturerData());
        assertEquals(10, info.getTxPower().value);
        List<UUID> services = info.getServiceUUIDS();
        assertEquals(2, services.size());
        assertTrue(services.contains(Uuids.BATTERY_SERVICE_UUID));
        assertTrue(services.contains(Uuids.DEVICE_INFORMATION_SERVICE_UUID));
        succeed();
    }

    @Test(timeout = 5000)
    public void scanRecordTestWithMedium() throws Exception
    {
        startSynchronousTest();
        UUID myUuid = Uuids.fromInt("ABABCDCD");
        final short manId = (short) 16454;
        final byte[] manData = new byte[] { 0x5,(byte) 0xAA, 0x44, (byte) 0xB3, 0x66 };
        BleScanRecord bleRecord = new BleScanRecord()
                .setName("Johnny 5")
                .setAdvFlags((byte) 1, (byte) 0x2)
                .setTxPower((byte) 10)
                .addServiceUuid(Uuids.CURRENT_TIME_SERVICE, BleUuid.UuidSize.MEDIUM)
                .addServiceUuid(Uuids.CURRENT_TIME_SERVICE__CURRENT_TIME, BleUuid.UuidSize.MEDIUM)
                .addServiceUuid(myUuid, BleUuid.UuidSize.MEDIUM)
                .addManufacturerData(manId, manData);
        byte[] record = bleRecord.buildPacket();
        BleScanRecord info = Utils_ScanRecord.parseScanRecord(record);
        assertEquals("Johnny 5", info.getName());
        assertEquals(3, info.getAdvFlags().value);
        assertEquals(manId, info.getManufacturerId());
        assertArrayEquals(manData, info.getManufacturerData());
        assertEquals(10, info.getTxPower().value);
        List<UUID> services = info.getServiceUUIDS();
        assertEquals(3, services.size());
        assertTrue(services.contains(Uuids.CURRENT_TIME_SERVICE));
        assertTrue(services.contains(Uuids.CURRENT_TIME_SERVICE__CURRENT_TIME));
        assertTrue(services.contains(myUuid));
        succeed();
    }

    @Test
    public void multipleMfgDataTest() throws Exception
    {
        startSynchronousTest();
        BleScanRecord info = new BleScanRecord();
        info.addManufacturerData((short) 14, new byte[] { 0x0, 0x1, 0x2 });
        info.addManufacturerData((short) 14, new byte[] { 0x3, 0x4, 0x5 });

        byte[] record = info.buildPacket();

        BleScanRecord info2 = Utils_ScanRecord.parseScanRecord(record);
        assertEquals(2, info2.getManufacturerDataList().size());
        succeed();
    }

    @Test
    public void serviceUUID128BitTest() throws Exception
    {
        startSynchronousTest();
        // This is a sample raw scan record which contains a 128bit service uuid with data.
        byte[] rawRecord = Utils_String.hexStringToBytes("0201020709363534333231020AF11821024DE6A9087CC2831D48D87196E28455303132333435360000000000000000000000000000000000000000000000");
        BleScanRecord record = Utils_ScanRecord.parseScanRecord(rawRecord);
        assertTrue(record.getServiceData().size() > 0);
        byte[] data = null;
        for (UUID id : record.getServiceData().keySet())
        {
            data = record.getServiceData().get(id);
            break;
        }
        assertTrue(data != null && data.length > 0);
        succeed();
    }

}
