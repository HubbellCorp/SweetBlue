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

package com.idevicesinc.sweetblue.internal;


import com.idevicesinc.sweetblue.BleCharacteristic;
import com.idevicesinc.sweetblue.BleDescriptor;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.BleNodeConfig;
import com.idevicesinc.sweetblue.BleService;
import com.idevicesinc.sweetblue.DescriptorFilter;
import com.idevicesinc.sweetblue.HistoricalDataQueryListener;
import com.idevicesinc.sweetblue.utils.EpochTime;
import com.idevicesinc.sweetblue.utils.ForEach_Breakable;
import com.idevicesinc.sweetblue.utils.ForEach_Void;
import com.idevicesinc.sweetblue.utils.HistoricalData;
import com.idevicesinc.sweetblue.utils.HistoricalDataQuery;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Interface to define shared methods between classes which extend the node class (BleDevice, BleServer)
 */
public interface IBleNode
{

    BleDescriptor getNativeBleDescriptor(final UUID serviceUuid, final UUID charUuid, final UUID descUuid);
    BleCharacteristic getNativeBleCharacteristic(final UUID serviceUuid, final UUID charUuid);
    BleCharacteristic getNativeBleCharacteristic(final UUID serviceUuid, final UUID charUuid, final DescriptorFilter descriptorFilter);
    BleService getNativeBleService(final UUID serviceUuid);
    Iterator<BleService> getNativeServices();
    List<BleService> getNativeServices_List();
    void getNativeServices(final ForEach_Void<BleService> forEach);
    void getNativeServices(final ForEach_Breakable<BleService> forEach);
    void getNativeCharacteristics(final UUID serviceUuid, final ForEach_Void<BleCharacteristic> forEach);
    void getNativeCharacteristics(final UUID serviceUuid, final ForEach_Breakable<BleCharacteristic> forEach);
    Iterator<BleCharacteristic> getNativeCharacteristics(UUID serviceUuid);
    List<BleCharacteristic> getNativeCharacteristics_List(UUID serviceUuid);
    Iterator<BleDescriptor> getNativeDescriptors(final UUID serviceUuid, final UUID charUuid);
    List<BleDescriptor> getNativeDescriptors_List(final UUID serviceUuid, final UUID charUuid);
    void getNativeDescriptors(final UUID serviceUuid, final UUID charUuid, final ForEach_Void<BleDescriptor> forEach);
    void getNativeDescriptors(final UUID serviceUuid, final UUID charUuid, final ForEach_Breakable<BleDescriptor> forEach);
    HistoricalData newHistoricalData(final byte[] data, final EpochTime epochTime);
    IBleManager getIManager();
    HistoricalDataQueryListener.HistoricalDataQueryEvent queryHistoricalData(final String query);
    void queryHistoricalData(final String query, final HistoricalDataQueryListener listener);
    HistoricalDataQuery.Part_Select select();
    BleNodeConfig conf_node();
    BleManagerConfig conf_mngr();

}
