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

import com.idevicesinc.sweetblue.utils.Uuids;
import com.idevicesinc.sweetblue.framework.AbstractTestClass;

import org.junit.Test;
import java.util.List;


public class BleOpBuilderTest extends AbstractTestClass
{

    @Test
    public void bleReadMultipleBuilderTest() throws Exception
    {
        startSynchronousTest();
        List<BleRead> reads = new BleRead.Builder().setServiceUUID(Uuids.BATTERY_SERVICE_UUID).setCharacteristicUUID(Uuids.BATTERY_LEVEL).
                nextNew().setServiceUUID(Uuids.GLUCOSE_SERVICE_UUID).setCharacteristicUUID(Uuids.GLUCOSE_MEASUREMENT).
                next().setCharacteristicUUID(Uuids.GLUCOSE_FEATURE).
                build();
        assertNotNull(reads);
        assertEquals(3, reads.size());
        assertEquals(Uuids.BATTERY_LEVEL.toString(), reads.get(0).getCharacteristicUuid().toString());
        assertEquals(Uuids.BATTERY_SERVICE_UUID.toString(), reads.get(0).getServiceUuid().toString());
        assertEquals(Uuids.GLUCOSE_MEASUREMENT.toString(), reads.get(1).getCharacteristicUuid().toString());
        assertEquals(Uuids.GLUCOSE_SERVICE_UUID.toString(), reads.get(1).getServiceUuid().toString());
        assertEquals(Uuids.GLUCOSE_FEATURE.toString(), reads.get(2).getCharacteristicUuid().toString());
        assertEquals(Uuids.GLUCOSE_SERVICE_UUID.toString(), reads.get(2).getServiceUuid().toString());
        succeed();
    }

    @Test
    public void bleReadMultipleArrayTest() throws Exception
    {
        startSynchronousTest();
        BleRead[] reads = new BleRead.Builder().setServiceUUID(Uuids.BATTERY_SERVICE_UUID).setCharacteristicUUID(Uuids.BATTERY_LEVEL).
                nextNew().setServiceUUID(Uuids.GLUCOSE_SERVICE_UUID).setCharacteristicUUID(Uuids.GLUCOSE_MEASUREMENT).
                next().setCharacteristicUUID(Uuids.GLUCOSE_FEATURE).
                buildArray();
        assertNotNull(reads);
        assertEquals(3, reads.length);
        assertEquals(Uuids.BATTERY_LEVEL.toString(), reads[0].getCharacteristicUuid().toString());
        assertEquals(Uuids.BATTERY_SERVICE_UUID.toString(), reads[0].getServiceUuid().toString());
        assertEquals(Uuids.GLUCOSE_MEASUREMENT.toString(), reads[1].getCharacteristicUuid().toString());
        assertEquals(Uuids.GLUCOSE_SERVICE_UUID.toString(), reads[1].getServiceUuid().toString());
        assertEquals(Uuids.GLUCOSE_FEATURE.toString(), reads[2].getCharacteristicUuid().toString());
        assertEquals(Uuids.GLUCOSE_SERVICE_UUID.toString(), reads[2].getServiceUuid().toString());
        succeed();
    }

    @Test
    public void bleWriteMultipleBuilderTest() throws Exception
    {
        startSynchronousTest();
        List<BleWrite> writes = new BleWrite.Builder().setServiceUUID(Uuids.BATTERY_SERVICE_UUID).setCharacteristicUUID(Uuids.BATTERY_LEVEL).
                nextNew().setServiceUUID(Uuids.GLUCOSE_SERVICE_UUID).setCharacteristicUUID(Uuids.GLUCOSE_MEASUREMENT).
                next().setCharacteristicUUID(Uuids.GLUCOSE_FEATURE).
                build();
        assertNotNull(writes);
        assertEquals(3, writes.size());
        assertEquals(Uuids.BATTERY_LEVEL.toString(), writes.get(0).getCharacteristicUuid().toString());
        assertEquals(Uuids.BATTERY_SERVICE_UUID.toString(), writes.get(0).getServiceUuid().toString());
        assertEquals(Uuids.GLUCOSE_MEASUREMENT.toString(), writes.get(1).getCharacteristicUuid().toString());
        assertEquals(Uuids.GLUCOSE_SERVICE_UUID.toString(), writes.get(1).getServiceUuid().toString());
        assertEquals(Uuids.GLUCOSE_FEATURE.toString(), writes.get(2).getCharacteristicUuid().toString());
        assertEquals(Uuids.GLUCOSE_SERVICE_UUID.toString(), writes.get(2).getServiceUuid().toString());
        succeed();
    }

    @Test
    public void bleWriteMultipleArrayTest() throws Exception
    {
        startSynchronousTest();
        BleWrite[] writes = new BleWrite.Builder().setServiceUUID(Uuids.BATTERY_SERVICE_UUID).setCharacteristicUUID(Uuids.BATTERY_LEVEL).
                nextNew().setServiceUUID(Uuids.GLUCOSE_SERVICE_UUID).setCharacteristicUUID(Uuids.GLUCOSE_MEASUREMENT).
                next().setCharacteristicUUID(Uuids.GLUCOSE_FEATURE).
                buildArray();
        assertNotNull(writes);
        assertEquals(3, writes.length);
        assertEquals(Uuids.BATTERY_LEVEL.toString(), writes[0].getCharacteristicUuid().toString());
        assertEquals(Uuids.BATTERY_SERVICE_UUID.toString(), writes[0].getServiceUuid().toString());
        assertEquals(Uuids.GLUCOSE_MEASUREMENT.toString(), writes[1].getCharacteristicUuid().toString());
        assertEquals(Uuids.GLUCOSE_SERVICE_UUID.toString(), writes[1].getServiceUuid().toString());
        assertEquals(Uuids.GLUCOSE_FEATURE.toString(), writes[2].getCharacteristicUuid().toString());
        assertEquals(Uuids.GLUCOSE_SERVICE_UUID.toString(), writes[2].getServiceUuid().toString());
        succeed();
    }

    @Test
    public void bleNotifyMultipleBuilderTest() throws Exception
    {
        startSynchronousTest();
        List<BleNotify> notifies = new BleNotify.Builder().setServiceUUID(Uuids.BATTERY_SERVICE_UUID).setCharacteristicUUID(Uuids.BATTERY_LEVEL).
                nextNew().setServiceUUID(Uuids.GLUCOSE_SERVICE_UUID).setCharacteristicUUID(Uuids.GLUCOSE_MEASUREMENT).
                next().setCharacteristicUUID(Uuids.GLUCOSE_FEATURE).
                build();
        assertNotNull(notifies);
        assertEquals(3, notifies.size());
        assertEquals(Uuids.BATTERY_LEVEL.toString(), notifies.get(0).getCharacteristicUuid().toString());
        assertEquals(Uuids.BATTERY_SERVICE_UUID.toString(), notifies.get(0).getServiceUuid().toString());
        assertEquals(Uuids.GLUCOSE_MEASUREMENT.toString(), notifies.get(1).getCharacteristicUuid().toString());
        assertEquals(Uuids.GLUCOSE_SERVICE_UUID.toString(), notifies.get(1).getServiceUuid().toString());
        assertEquals(Uuids.GLUCOSE_FEATURE.toString(), notifies.get(2).getCharacteristicUuid().toString());
        assertEquals(Uuids.GLUCOSE_SERVICE_UUID.toString(), notifies.get(2).getServiceUuid().toString());
        succeed();
    }

    @Test
    public void bleNotifyMultipleArrayTest() throws Exception
    {
        startSynchronousTest();
        BleNotify[] notifies = new BleNotify.Builder().setServiceUUID(Uuids.BATTERY_SERVICE_UUID).setCharacteristicUUID(Uuids.BATTERY_LEVEL).
                nextNew().setServiceUUID(Uuids.GLUCOSE_SERVICE_UUID).setCharacteristicUUID(Uuids.GLUCOSE_MEASUREMENT).
                next().setCharacteristicUUID(Uuids.GLUCOSE_FEATURE).
                buildArray();
        assertNotNull(notifies);
        assertEquals(3, notifies.length);
        assertEquals(Uuids.BATTERY_LEVEL.toString(), notifies[0].getCharacteristicUuid().toString());
        assertEquals(Uuids.BATTERY_SERVICE_UUID.toString(), notifies[0].getServiceUuid().toString());
        assertEquals(Uuids.GLUCOSE_MEASUREMENT.toString(), notifies[1].getCharacteristicUuid().toString());
        assertEquals(Uuids.GLUCOSE_SERVICE_UUID.toString(), notifies[1].getServiceUuid().toString());
        assertEquals(Uuids.GLUCOSE_FEATURE.toString(), notifies[2].getCharacteristicUuid().toString());
        assertEquals(Uuids.GLUCOSE_SERVICE_UUID.toString(), notifies[2].getServiceUuid().toString());
        succeed();
    }

    @Test
    public void bleDescriptorReadMultipleBuilderTest() throws Exception
    {
        startSynchronousTest();
        List<BleDescriptorRead> reads = new BleDescriptorRead.Builder().setServiceUUID(Uuids.BATTERY_SERVICE_UUID).setCharacteristicUUID(Uuids.BATTERY_LEVEL).setDescriptorUUID(Uuids.CHARACTERISTIC_USER_DESCRIPTION_DESCRIPTOR_UUID).
                nextNew().setServiceUUID(Uuids.GLUCOSE_SERVICE_UUID).setCharacteristicUUID(Uuids.GLUCOSE_MEASUREMENT).setDescriptorUUID(Uuids.CHARACTERISTIC_USER_DESCRIPTION_DESCRIPTOR_UUID).
                next().setCharacteristicUUID(Uuids.GLUCOSE_FEATURE).
                build();
        assertNotNull(reads);
        assertEquals(3, reads.size());
        assertEquals(Uuids.BATTERY_LEVEL.toString(), reads.get(0).getCharacteristicUuid().toString());
        assertEquals(Uuids.BATTERY_SERVICE_UUID.toString(), reads.get(0).getServiceUuid().toString());
        assertEquals(Uuids.CHARACTERISTIC_USER_DESCRIPTION_DESCRIPTOR_UUID.toString(), reads.get(0).getDescriptorUuid().toString());
        assertEquals(Uuids.GLUCOSE_MEASUREMENT.toString(), reads.get(1).getCharacteristicUuid().toString());
        assertEquals(Uuids.GLUCOSE_SERVICE_UUID.toString(), reads.get(1).getServiceUuid().toString());
        assertEquals(Uuids.CHARACTERISTIC_USER_DESCRIPTION_DESCRIPTOR_UUID.toString(), reads.get(1).getDescriptorUuid().toString());
        assertEquals(Uuids.GLUCOSE_FEATURE.toString(), reads.get(2).getCharacteristicUuid().toString());
        assertEquals(Uuids.GLUCOSE_SERVICE_UUID.toString(), reads.get(2).getServiceUuid().toString());
        assertEquals(Uuids.CHARACTERISTIC_USER_DESCRIPTION_DESCRIPTOR_UUID.toString(), reads.get(2).getDescriptorUuid().toString());
        succeed();
    }

    @Test
    public void bleDescriptorReadMultipleArrayTest() throws Exception
    {
        startSynchronousTest();
        BleDescriptorRead[] reads = new BleDescriptorRead.Builder().setServiceUUID(Uuids.BATTERY_SERVICE_UUID).setCharacteristicUUID(Uuids.BATTERY_LEVEL).setDescriptorUUID(Uuids.CHARACTERISTIC_USER_DESCRIPTION_DESCRIPTOR_UUID).
                nextNew().setServiceUUID(Uuids.GLUCOSE_SERVICE_UUID).setCharacteristicUUID(Uuids.GLUCOSE_MEASUREMENT).setDescriptorUUID(Uuids.CHARACTERISTIC_USER_DESCRIPTION_DESCRIPTOR_UUID).
                next().setCharacteristicUUID(Uuids.GLUCOSE_FEATURE).
                buildArray();
        assertNotNull(reads);
        assertEquals(3, reads.length);
        assertEquals(Uuids.BATTERY_LEVEL.toString(), reads[0].getCharacteristicUuid().toString());
        assertEquals(Uuids.BATTERY_SERVICE_UUID.toString(), reads[0].getServiceUuid().toString());
        assertEquals(Uuids.CHARACTERISTIC_USER_DESCRIPTION_DESCRIPTOR_UUID.toString(), reads[0].getDescriptorUuid().toString());
        assertEquals(Uuids.GLUCOSE_MEASUREMENT.toString(), reads[1].getCharacteristicUuid().toString());
        assertEquals(Uuids.GLUCOSE_SERVICE_UUID.toString(), reads[1].getServiceUuid().toString());
        assertEquals(Uuids.CHARACTERISTIC_USER_DESCRIPTION_DESCRIPTOR_UUID.toString(), reads[1].getDescriptorUuid().toString());
        assertEquals(Uuids.GLUCOSE_FEATURE.toString(), reads[2].getCharacteristicUuid().toString());
        assertEquals(Uuids.GLUCOSE_SERVICE_UUID.toString(), reads[2].getServiceUuid().toString());
        assertEquals(Uuids.CHARACTERISTIC_USER_DESCRIPTION_DESCRIPTOR_UUID.toString(), reads[2].getDescriptorUuid().toString());
        succeed();
    }

    @Test
    public void bleDescriptorWriteMultipleBuilderTest() throws Exception
    {
        startSynchronousTest();
        List<BleDescriptorWrite> writes = new BleDescriptorWrite.Builder().setServiceUUID(Uuids.BATTERY_SERVICE_UUID).setCharacteristicUUID(Uuids.BATTERY_LEVEL).setDescriptorUUID(Uuids.CHARACTERISTIC_USER_DESCRIPTION_DESCRIPTOR_UUID).
                nextNew().setServiceUUID(Uuids.GLUCOSE_SERVICE_UUID).setCharacteristicUUID(Uuids.GLUCOSE_MEASUREMENT).setDescriptorUUID(Uuids.CHARACTERISTIC_USER_DESCRIPTION_DESCRIPTOR_UUID).
                next().setCharacteristicUUID(Uuids.GLUCOSE_FEATURE).
                build();
        assertNotNull(writes);
        assertEquals(3, writes.size());
        assertEquals(Uuids.BATTERY_LEVEL.toString(), writes.get(0).getCharacteristicUuid().toString());
        assertEquals(Uuids.BATTERY_SERVICE_UUID.toString(), writes.get(0).getServiceUuid().toString());
        assertEquals(Uuids.CHARACTERISTIC_USER_DESCRIPTION_DESCRIPTOR_UUID.toString(), writes.get(0).getDescriptorUuid().toString());
        assertEquals(Uuids.GLUCOSE_MEASUREMENT.toString(), writes.get(1).getCharacteristicUuid().toString());
        assertEquals(Uuids.GLUCOSE_SERVICE_UUID.toString(), writes.get(1).getServiceUuid().toString());
        assertEquals(Uuids.CHARACTERISTIC_USER_DESCRIPTION_DESCRIPTOR_UUID.toString(), writes.get(1).getDescriptorUuid().toString());
        assertEquals(Uuids.GLUCOSE_FEATURE.toString(), writes.get(2).getCharacteristicUuid().toString());
        assertEquals(Uuids.GLUCOSE_SERVICE_UUID.toString(), writes.get(2).getServiceUuid().toString());
        assertEquals(Uuids.CHARACTERISTIC_USER_DESCRIPTION_DESCRIPTOR_UUID.toString(), writes.get(2).getDescriptorUuid().toString());
        succeed();
    }

    @Test
    public void bleDescriptorWriteMultipleArrayTest() throws Exception
    {
        startSynchronousTest();
        BleDescriptorWrite[] writes = new BleDescriptorWrite.Builder().setServiceUUID(Uuids.BATTERY_SERVICE_UUID).setCharacteristicUUID(Uuids.BATTERY_LEVEL).setDescriptorUUID(Uuids.CHARACTERISTIC_USER_DESCRIPTION_DESCRIPTOR_UUID).
                nextNew().setServiceUUID(Uuids.GLUCOSE_SERVICE_UUID).setCharacteristicUUID(Uuids.GLUCOSE_MEASUREMENT).setDescriptorUUID(Uuids.CHARACTERISTIC_USER_DESCRIPTION_DESCRIPTOR_UUID).
                next().setCharacteristicUUID(Uuids.GLUCOSE_FEATURE).
                buildArray();
        assertNotNull(writes);
        assertEquals(3, writes.length);
        assertEquals(Uuids.BATTERY_LEVEL.toString(), writes[0].getCharacteristicUuid().toString());
        assertEquals(Uuids.BATTERY_SERVICE_UUID.toString(), writes[0].getServiceUuid().toString());
        assertEquals(Uuids.CHARACTERISTIC_USER_DESCRIPTION_DESCRIPTOR_UUID.toString(), writes[0].getDescriptorUuid().toString());
        assertEquals(Uuids.GLUCOSE_MEASUREMENT.toString(), writes[1].getCharacteristicUuid().toString());
        assertEquals(Uuids.GLUCOSE_SERVICE_UUID.toString(), writes[1].getServiceUuid().toString());
        assertEquals(Uuids.CHARACTERISTIC_USER_DESCRIPTION_DESCRIPTOR_UUID.toString(), writes[1].getDescriptorUuid().toString());
        assertEquals(Uuids.GLUCOSE_FEATURE.toString(), writes[2].getCharacteristicUuid().toString());
        assertEquals(Uuids.GLUCOSE_SERVICE_UUID.toString(), writes[2].getServiceUuid().toString());
        assertEquals(Uuids.CHARACTERISTIC_USER_DESCRIPTION_DESCRIPTOR_UUID.toString(), writes[2].getDescriptorUuid().toString());
        succeed();
    }

}
