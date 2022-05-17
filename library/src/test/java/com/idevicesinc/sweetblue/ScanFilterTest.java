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


import android.os.ParcelUuid;

import com.idevicesinc.sweetblue.compat.L_Util;
import com.idevicesinc.sweetblue.defaults.DefaultScanFilter;
import com.idevicesinc.sweetblue.utils.NativeScanFilter;
import com.idevicesinc.sweetblue.utils.State;
import com.idevicesinc.sweetblue.utils.Utils_ScanRecord;
import com.idevicesinc.sweetblue.utils.Uuids;
import com.idevicesinc.sweetblue.utils.Util_Unit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public class ScanFilterTest extends BaseBleUnitTest
{

    @Test(timeout = 5000)
    public void uuidFilterTest() throws Exception
    {
        byte[] record = Utils_ScanRecord.newScanRecord("FilterTesterer", Uuids.BATTERY_SERVICE_UUID);
        BleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "FilterTesterer", record, null);

        ScanFilter filter = new DefaultScanFilter(Uuids.BATTERY_SERVICE_UUID);
        ScanFilter.ScanEvent event = newEvent(device);
        ScanFilter.Please please = filter.onEvent(event);
        assertTrue(please.ack());

        filter = new DefaultScanFilter(Uuids.GLUCOSE_SERVICE_UUID);
        event = newEvent(device);
        please = filter.onEvent(event);
        assertFalse(please.ack());
    }

    @Test(timeout = 5000)
    public void uuidListTest() throws Exception
    {
        byte[] record = Utils_ScanRecord.newScanRecord("FilterTesterer", Uuids.BATTERY_SERVICE_UUID);
        BleDevice device1 = m_manager.newDevice(Util_Unit.randomMacAddress(), "FilterTesterer", record, null);

        byte[] record2 = Utils_ScanRecord.newScanRecord("Milano", Uuids.CURRENT_TIME_SERVICE);
        BleDevice device2 = m_manager.newDevice(Util_Unit.randomMacAddress(), "Milano", record2, null);

        byte[] record3 = Utils_ScanRecord.newScanRecord("Wretched", Uuids.BLOOD_PRESSURE_SERVICE_UUID);
        BleDevice device3 = m_manager.newDevice(Util_Unit.randomMacAddress(), "Wretched", record3, null);

        ArrayList<UUID> list = new ArrayList<>();
        list.add(Uuids.BATTERY_SERVICE_UUID);
        list.add(Uuids.CURRENT_TIME_SERVICE);
        ScanFilter filter = new DefaultScanFilter(list);
        ScanFilter.ScanEvent event = newEvent(device1);
        ScanFilter.Please please = filter.onEvent(event);
        assertTrue(please.ack());

        event = newEvent(device2);
        please = filter.onEvent(event);
        assertTrue(please.ack());

        event = newEvent(device3);
        please = filter.onEvent(event);
        assertFalse(please.ack());
    }

    @Test(timeout = 5000)
    public void nameFilterTest() throws Exception
    {
        byte[] record = Utils_ScanRecord.newScanRecord("FilterTesterer", Uuids.BATTERY_SERVICE_UUID);
        BleDevice device = m_manager.newDevice(Util_Unit.randomMacAddress(), "FilterTesterer", record, null);

        ScanFilter filter = new DefaultScanFilter("TERtes");
        ScanFilter.ScanEvent event = newEvent(device);
        ScanFilter.Please please = filter.onEvent(event);
        assertTrue(please.ack());

        filter = new DefaultScanFilter("testing");
        event = newEvent(device);
        please = filter.onEvent(event);
        assertFalse(please.ack());
    }

    @Test(timeout = 5000)
    public void nameListTest() throws Exception
    {
        BleDevice device1 = m_manager.newDevice(Util_Unit.randomMacAddress(), "FilterTesterer");
        BleDevice device2 = m_manager.newDevice(Util_Unit.randomMacAddress(), "Milano");
        BleDevice device3 = m_manager.newDevice(Util_Unit.randomMacAddress(), "Wretched");

        ScanFilter filter = new DefaultScanFilter("TESter", "laNo");
        ScanFilter.ScanEvent event = newEvent(device1);
        ScanFilter.Please please = filter.onEvent(event);
        assertTrue(please.ack());

        event = newEvent(device2);
        please = filter.onEvent(event);
        assertTrue(please.ack());

        event = newEvent(device3);
        please = filter.onEvent(event);
        assertFalse(please.ack());
    }

    @Test(timeout = 5000)
    public void uuidAndNameListTest() throws Exception
    {
        byte[] record = Utils_ScanRecord.newScanRecord("FilterTesterer", Uuids.BATTERY_SERVICE_UUID);
        BleDevice device1 = m_manager.newDevice(Util_Unit.randomMacAddress(), "FilterTesterer", record, null);

        byte[] record2 = Utils_ScanRecord.newScanRecord("Milano", Uuids.CURRENT_TIME_SERVICE);
        BleDevice device2 = m_manager.newDevice(Util_Unit.randomMacAddress(), "Milano", record2, null);

        byte[] record3 = Utils_ScanRecord.newScanRecord("Wretched", Uuids.BLOOD_PRESSURE_SERVICE_UUID);
        BleDevice device3 = m_manager.newDevice(Util_Unit.randomMacAddress(), "Wretched", record3, null);

        ArrayList<UUID> list = new ArrayList<>();
        list.add(Uuids.BATTERY_SERVICE_UUID);
        ScanFilter filter = new DefaultScanFilter(list, "ilano");
        ScanFilter.ScanEvent event = newEvent(device1);
        ScanFilter.Please please = filter.onEvent(event);
        assertTrue(please.ack());

        event = newEvent(device2);
        please = filter.onEvent(event);
        assertTrue(please.ack());

        event = newEvent(device3);
        please = filter.onEvent(event);
        assertFalse(please.ack());
    }

    @Test(timeout = 5000)
    public void nativeScanFilterConversionTest() throws Exception
    {
        startSynchronousTest();

        final byte[] manData = new byte[] {  0x0, 0x8, 0x1, 0x2, 0x3, 0x4, 0x5 };
        final byte[] manMask = new byte[] { 0x1, 0x1, 0x1, 0x1, 0x1, 0x0, 0x0 };
        final byte[] serviceData = new byte[] { 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9, 0xA };
        final byte[] serviceMask = new byte[] { 0x1, 0x1, 0x0, 0x0, 0x1, 0x1, 0x0, 0x1 };
        final String mac = "DE:CA:FF:C0:FF:EE";
        final String name = "Not Real Coffee";
        final int manId = 8;
        final ParcelUuid uuid = ParcelUuid.fromString(Uuids.INVALID.toString());
        final ParcelUuid uuidMask = ParcelUuid.fromString("00000000-1111-0000-1111-000000000000");
        List<NativeScanFilter> filterList = new ArrayList<>();
        NativeScanFilter.Builder b = new NativeScanFilter.Builder();
        b.setDeviceAddress(mac);
        b.setDeviceName(name);
        b.setManufacturerData(manId, manData, manMask);
        b.setServiceData(uuid, serviceData, serviceMask);
        b.setServiceUuid(uuid, uuidMask);
        filterList.add(b.build());

        List<android.bluetooth.le.ScanFilter> newList = L_Util.convertNativeFilterList(filterList);
        android.bluetooth.le.ScanFilter filter = newList.get(0);
        assertThat("Mac address doesn't match!", filter.getDeviceAddress(), is(equalTo(mac)));
        assertThat("Device name doesn't match!", filter.getDeviceName(), is(equalTo(name)));
        assertThat("Manufacturer Id doesn't match!", filter.getManufacturerId(), is(equalTo(manId)));
        assertThat("Manufacturer data doesn't match!", filter.getManufacturerData(), is(equalTo(manData)));
        assertThat("Manufacturer data mask doesn't match!", filter.getManufacturerDataMask(), is(equalTo(manMask)));
        assertThat("Service Uuid doesn't match!", filter.getServiceUuid(), is(equalTo(uuid)));
        assertThat("Service Uuid mask doesn't match!", filter.getServiceUuidMask(), is(equalTo(uuidMask)));
        assertThat("Service data doesn't match!", filter.getServiceData(), is(equalTo(serviceData)));
        assertThat("Service data mask doesn't match!", filter.getServiceDataMask(), is(equalTo(serviceMask)));

        succeed();
    }



    private ScanFilter.ScanEvent newEvent(BleDevice device)
    {
        return ScanFilter.ScanEvent.fromScanRecord(device.getNative(), device.getName_native(), device.getName_normalized(), device.getRssi(), State.ChangeIntent.INTENTIONAL, device.getScanRecord());
    }

}
