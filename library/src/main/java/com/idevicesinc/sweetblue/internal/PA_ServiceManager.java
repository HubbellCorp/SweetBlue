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

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import com.idevicesinc.sweetblue.BleCharacteristic;
import com.idevicesinc.sweetblue.BleDescriptor;
import com.idevicesinc.sweetblue.BleService;
import com.idevicesinc.sweetblue.DescriptorFilter;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.utils.P_Const;
import com.idevicesinc.sweetblue.utils.PresentData;
import com.idevicesinc.sweetblue.utils.Utils;
import com.idevicesinc.sweetblue.utils.Uuids;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;


abstract class PA_ServiceManager
{

    PA_ServiceManager()
    {
    }

    public abstract BleService getServiceDirectlyFromNativeNode(final UUID uuid);

    protected abstract List<BleService> getNativeServiceList_original();


    public BleCharacteristic getCharacteristic(final UUID serviceUuid_nullable, final UUID charUuid)
    {
        if (serviceUuid_nullable == null || serviceUuid_nullable.equals(Uuids.INVALID))
        {
            final List<BleService> serviceList_native = getNativeServiceList_original();

            for (int i = 0; i < serviceList_native.size(); i++)
            {
                final BleService service_ith = serviceList_native.get(i);
                final BleCharacteristic characteristic = getCharacteristic(service_ith, charUuid);

                if (!characteristic.isNull())
                {
                    return characteristic;
                }
            }

            return BleCharacteristic.NULL;
        }
        else
        {
            final BleService service_nullable = getServiceDirectlyFromNativeNode(serviceUuid_nullable);
            if (service_nullable.hasUhOh())
                return new BleCharacteristic(service_nullable.getUhOh());
            else
                return getCharacteristic(service_nullable, charUuid);
        }
    }

    public BleCharacteristic getCharacteristic(final UUID serviceUuid_nullable, final UUID charUuid, final DescriptorFilter filter)
    {
        if (serviceUuid_nullable == null || serviceUuid_nullable.equals(Uuids.INVALID))
        {
            final List<BleService> serviceList_native = getNativeServiceList_original();

            for (int i = 0; i < serviceList_native.size(); i++)
            {
                final BleService service_ith = serviceList_native.get(i);
                final BleCharacteristic characteristic = getCharacteristic(service_ith, charUuid, filter);

                if (!characteristic.isNull())
                {
                    return characteristic;
                }
            }

            return BleCharacteristic.NULL;
        }
        else
        {
            final BleService service_nullable = getServiceDirectlyFromNativeNode(serviceUuid_nullable);

            if (service_nullable.hasUhOh())
                return new BleCharacteristic(service_nullable.getUhOh());
            else
                return getCharacteristic(service_nullable, charUuid, filter);
        }
    }

    private BleCharacteristic getCharacteristic(final BleService service, final UUID charUuid)
    {
        if (!service.isNull())
        {
            final List<BleCharacteristic> charList_native = getNativeCharacteristicList_original(service);

            for (int j = 0; j < charList_native.size(); j++)
            {
                final BleCharacteristic char_jth = charList_native.get(j);

                if (char_jth.getUuid().equals(charUuid))
                {
                    return char_jth;
                }
            }
        }

        return BleCharacteristic.NULL;
    }

    private BleCharacteristic getCharacteristic(final BleService service, final UUID charUuid, DescriptorFilter filter)
    {
        if (!service.isNull())
        {
            final List<BleCharacteristic> charList_native = getNativeCharacteristicList_original(service);

            for (int j = 0; j < charList_native.size(); j++)
            {
                final BleCharacteristic char_jth = charList_native.get(j);

                if (char_jth.getUuid().equals(charUuid))
                {

                    if (filter == null)
                    {
                        return char_jth;
                    }
                    else
                    {
                        final UUID descUuid = filter.descriptorUuid();
                        if (descUuid != null)
                        {
                            final BleDescriptor desc = char_jth.getDescriptor(filter.descriptorUuid());
                            if (desc != null)
                            {
                                final DescriptorFilter.DescriptorEvent event = P_Bridge_User.newDescriptorEvent(service.getService(), char_jth.getCharacteristic(), desc.getDescriptor(), new PresentData(desc.getValue()));
                                final DescriptorFilter.Please please = filter.onEvent(event);
                                if (P_Bridge_User.accepted(please))
                                {
                                    return char_jth;
                                }
                            }
                        }
                        else
                        {
                            final DescriptorFilter.DescriptorEvent event = P_Bridge_User.newDescriptorEvent(service.getService(), char_jth.getCharacteristic(), null, P_Const.EMPTY_FUTURE_DATA);
                            final DescriptorFilter.Please please = filter.onEvent(event);
                            if (P_Bridge_User.accepted(please))
                            {
                                return char_jth;
                            }
                        }
                    }
                }
            }
            return BleCharacteristic.NULL;
        }
        else
        {
            return BleCharacteristic.NULL;
        }
    }

    private List<BleService> getNativeServiceList_cloned()
    {
        final List<BleService> list_native = getNativeServiceList_original();

        return list_native == P_Const.EMPTY_BLESERVICE_LIST ? list_native : new ArrayList<>(list_native);
    }

    private List<BleCharacteristic> getNativeCharacteristicList_original(final BleService service)
    {
        if (!service.isNull())
        {
            final List<BleCharacteristic> list_native = P_Bridge_Internal.fromBleService(service);

            return list_native == null ? P_Const.EMPTY_BLECHARACTERISTIC_LIST : list_native;
        }
        else
            return P_Const.EMPTY_BLECHARACTERISTIC_LIST;
    }

    private List<BleCharacteristic> getNativeCharacteristicList_cloned(final BleService service)
    {
        if (!service.isNull())
        {
            final List<BleCharacteristic> list_native = getNativeCharacteristicList_original(service);

            return list_native == P_Const.EMPTY_BLECHARACTERISTIC_LIST ? list_native : new ArrayList<>(list_native);
        }
        else
            return P_Const.EMPTY_BLECHARACTERISTIC_LIST;
    }

    private List<BleDescriptor> getNativeDescriptorList_original(final BleCharacteristic characteristic)
    {
        if (!characteristic.isNull())
        {
            final List<BleDescriptor> list_native = P_Bridge_Internal.fromBleCharacteristic(characteristic);

            return list_native == null ? P_Const.EMPTY_BLEDESCRIPTOR_LIST : list_native;
        }
        else
            return P_Const.EMPTY_BLEDESCRIPTOR_LIST;

    }

    private List<BleDescriptor> getNativeDescriptorList_cloned(final BleCharacteristic characteristic)
    {
        if (!characteristic.isNull())
        {
            final List<BleDescriptor> list_native = getNativeDescriptorList_original(characteristic);

            return list_native == P_Const.EMPTY_BLEDESCRIPTOR_LIST ? list_native : new ArrayList<>(list_native);
        }
        else
            return P_Const.EMPTY_BLEDESCRIPTOR_LIST;
    }

    private List<BleCharacteristic> collectAllNativeCharacteristics(final UUID serviceUuid_nullable, final Object forEach_nullable)
    {
        final ArrayList<BleCharacteristic> characteristics = forEach_nullable == null ? new ArrayList<>() : null;
        final List<BleService> serviceList_native = getNativeServiceList_original();

        for (int i = 0; i < serviceList_native.size(); i++)
        {
            final BleService service_ith = serviceList_native.get(i);

            if (serviceUuid_nullable == null || !service_ith.isNull() && serviceUuid_nullable.equals(service_ith.getService().getUuid()))
            {
                final List<BleCharacteristic> nativeChars = getNativeCharacteristicList_original(service_ith);

                if (forEach_nullable != null)
                {
                    if (Utils.doForEach_break(forEach_nullable, nativeChars))
                    {
                        return P_Const.EMPTY_BLECHARACTERISTIC_LIST;
                    }
                }
                else
                {
                    characteristics.addAll(nativeChars);
                }
            }
        }

        return characteristics;
    }



    private List<BleDescriptor> collectAllNativeDescriptors(
            final UUID serviceUuid_nullable, final UUID charUuid_nullable, final Object forEach_nullable)
    {
        final ArrayList<BleDescriptor> toReturn = forEach_nullable == null ? new ArrayList<>() : null;
        final List<BleService> serviceList_native = getNativeServiceList_original();

        for (int i = 0; i < serviceList_native.size(); i++)
        {
            final BleService service_ith = serviceList_native.get(i);

            if (serviceUuid_nullable == null || !service_ith.isNull() && serviceUuid_nullable.equals(service_ith.getService().getUuid()))
            {
                final List<BleCharacteristic> charList_native = getNativeCharacteristicList_original(service_ith);

                for (int j = 0; j < charList_native.size(); j++)
                {
                    final BleCharacteristic char_jth = charList_native.get(j);

                    if (charUuid_nullable == null || !char_jth.isNull() && charUuid_nullable.equals(char_jth.getCharacteristic().getUuid()))
                    {
                        final List<BleDescriptor> descriptors = getNativeDescriptorList_original(char_jth);

                        if (forEach_nullable != null)
                        {
                            if (Utils.doForEach_break(forEach_nullable, descriptors))
                            {
                                return P_Const.EMPTY_BLEDESCRIPTOR_LIST;
                            }
                        }
                        else
                        {
                            toReturn.addAll(descriptors);
                        }
                    }
                }
            }
        }

        return toReturn;
    }

    public Iterator<BleService> getServices()
    {
        return getServices_List().iterator();
    }

    public List<BleService> getServices_List()
    {
        return getNativeServiceList_cloned();
    }

    public Iterator<BleCharacteristic> getCharacteristics(
            final UUID serviceUuid_nullable)
    {
        return getCharacteristics_List(serviceUuid_nullable).iterator();
    }

    public List<BleCharacteristic> getCharacteristics_List(
            final UUID serviceUuid_nullable)
    {
        return collectAllNativeCharacteristics(serviceUuid_nullable, /*forEach=*/null);
    }

    private BleDescriptor getDescriptor(final BleCharacteristic characteristic, final UUID descUuid)
    {
        if (!characteristic.isNull())
        {
            final List<BleDescriptor> list_native = getNativeDescriptorList_original(characteristic);

            for (int i = 0; i < list_native.size(); i++)
            {
                final BleDescriptor ith = list_native.get(i);

                if (ith.getUuid().equals(descUuid))
                {
                    return ith;
                }
            }
        }

        return BleDescriptor.NULL;
    }

    private BleDescriptor getDescriptor(final BleService service, final UUID charUuid_nullable, final UUID descUuid)
    {
        if (!service.isNull())
        {
            final List<BleCharacteristic> charList = getNativeCharacteristicList_original(service);

            for (int j = 0; j < charList.size(); j++)
            {
                final BleCharacteristic char_jth = charList.get(j);

                if (charUuid_nullable == null || !char_jth.isNull() && charUuid_nullable.equals(char_jth.getCharacteristic().getUuid()))
                {
                    final BleDescriptor descriptor = getDescriptor(char_jth, descUuid);

                    return descriptor;
                }
            }
        }

        return BleDescriptor.NULL;
    }

    public Iterator<BleDescriptor> getDescriptors(final UUID serviceUuid_nullable, final UUID charUuid_nullable)
    {
        return getDescriptors_List(serviceUuid_nullable, charUuid_nullable).iterator();
    }

    public List<BleDescriptor> getDescriptors_List(final UUID serviceUuid_nullable, final UUID charUuid_nullable)
    {
        return collectAllNativeDescriptors(serviceUuid_nullable, charUuid_nullable, null);
    }

    public BleDescriptor getDescriptor(final UUID serviceUuid_nullable, final UUID charUuid_nullable, final UUID descUuid)
    {
        BleDescriptor descriptor = BleDescriptor.NULL;
        if (serviceUuid_nullable == null)
        {
            final List<BleService> serviceList = getNativeServiceList_original();

            for (int i = 0; i < serviceList.size(); i++)
            {
                final BleService service_ith = serviceList.get(i);
                descriptor = getDescriptor(service_ith, charUuid_nullable, descUuid);
                if (!descriptor.isNull())
                    break;
            }
        }
        else
        {
            final BleService service = getServiceDirectlyFromNativeNode(serviceUuid_nullable);

            if (service.hasUhOh())
                descriptor = new BleDescriptor(service.getUhOh());
            else
                descriptor = getDescriptor(service, charUuid_nullable, descUuid);
        }

        return descriptor;
    }

    public void getServices(final Object forEach)
    {
        Utils.doForEach_break(forEach, getNativeServiceList_original());
    }

    public void getCharacteristics(final UUID serviceUuid, final Object forEach)
    {
        collectAllNativeCharacteristics(serviceUuid, forEach);
    }

    public void getDescriptors(final UUID serviceUuid, final UUID charUuid, final Object forEach)
    {
        collectAllNativeDescriptors(serviceUuid, charUuid, forEach);
    }

    protected static boolean equals(final BluetoothGattService one, final BluetoothGattService another)
    {
        if (one == another)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    protected static boolean equals(final BleService one, final BleService another)
    {
        if (one.getService() == another.getService())
        {
            return true;
        }
        else
        {
            return false;
        }
    }

}
