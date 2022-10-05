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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDeviceOrigin;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleStatuses;
import com.idevicesinc.sweetblue.BondListener;
import com.idevicesinc.sweetblue.DeviceReconnectFilter;
import com.idevicesinc.sweetblue.DiscoveryListener;
import com.idevicesinc.sweetblue.DiscoveryListener.DiscoveryEvent;
import com.idevicesinc.sweetblue.DiscoveryListener.LifeCycle;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.utils.ForEach_Breakable;
import com.idevicesinc.sweetblue.utils.ForEach_Void;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Utils_Config;


final class P_DeviceManager
{
    private final Object m_lock = new Object();

    // Map that holds all of our devices (and preserves insertion order)
    private final LinkedHashMap<String, IBleDevice> m_map = new LinkedHashMap<>();

    private final IBleManager m_mngr;

    private boolean m_updating = false;
    private boolean m_requestPurge = false;
    private Double m_purgeScanTime = 0.0;

    private P_DeviceManager m_deviceManagerCache;
    private DiscoveryListener m_discoveryListener;


    P_DeviceManager(IBleManager mngr)
    {
        m_mngr = mngr;
    }

    private P_Logger logger()
    {
        return m_mngr.getLogger();
    }

    public ArrayList<IBleDevice> getList()
    {
        return getList_private(false);
    }

    public ArrayList<IBleDevice> getList_sorted()
    {
        return getList_private(true);
    }

    private ArrayList<IBleDevice> getList_private(boolean sort)
    {
        ArrayList<IBleDevice> deviceList;
        synchronized (m_lock)
        {
            deviceList = new ArrayList<>(m_map.values());
        }
        if (sort && m_mngr.getConfigClone().defaultListComparator != null)
            Collections.sort(deviceList, wrapComparator(m_mngr.getConfigClone().defaultListComparator));
        return deviceList;
    }

    private Comparator<IBleDevice> wrapComparator(final Comparator<BleDevice> comparator)
    {
        return (o1, o2) -> comparator.compare(o1.getBleDevice(), o2.getBleDevice());
    }

    void forEach(final Object forEach, final Object... query)
    {
        final boolean isQueryValid = query != null && query.length > 0;

        // Copy the list so we don't have to lock while iterating
        List<IBleDevice> list = getList();

        // Call the forEach on every device
        for (IBleDevice device : list)
        {
            // Don't execute if we have a valid query and the device doesn't match it
            if (isQueryValid && !device.is(query))
                continue;

            if (!forEach_invoke(forEach, m_mngr.getBleDevice(device)))
                break;
        }
    }

    private boolean forEach_invoke(final Object forEach, final BleDevice device)
    {
        if (forEach instanceof ForEach_Breakable)
        {
            ForEach_Breakable<BleDevice> forEach_cast = (ForEach_Breakable<BleDevice>) forEach;

            final ForEach_Breakable.Please please = forEach_cast.next(device);

            return please.shouldContinue();
        }
        else if (forEach instanceof ForEach_Void)
        {
            ForEach_Void<BleDevice> forEach_cast = (ForEach_Void<BleDevice>) forEach;

            forEach_cast.next(device);

            return true;
        }

        return false;
    }

    // Helper method for completing iteration after we discover the starting position in the list
    private IBleDevice iterateStage2(List<IBleDevice> list, int startingIndex, int delta, Object... query)
    {
        // See if the query is actually valid.  If not, we don't attempt to check it
        final boolean queryValid = query != null && query.length > 0;

        // Walk the list from the given index
        for (int i = 1; i <= list.size(); ++i)
        {
            int index = startingIndex;
            if (delta > 0)
                index += i;
            else
                index -= i;

            // Handle wraparound
            if (index < 0)
                index += list.size();
            if (index >= list.size())
                index -= list.size();

            IBleDevice candidate = list.get(index);

            if (queryValid)
            {
                if (candidate.is(query))
                    return candidate;
            }
            else  // If no query, just return candidate
                return candidate;

            // Due to the way we set up the loop, we can never iterate over the entire list more than once
            // If nothing in the list matches the criteria, we will drop out of the loop and return BleDevice.NULL
        }

        // If we got here, nothing matched the query
        return P_BleDeviceImpl.NULL;
    }

    private IBleDevice iterate(final IBleDevice device, int delta, Object... query)
    {
        // Grab a snapshot of the list to avoid any issues with concurrency
        List<IBleDevice> list = getList();

        for (int i = 0; i < list.size(); ++i)
        {
            IBleDevice candidate = list.get(i);

            if (candidate.equals(device))
            {
                // We have identified the starting index.   Now try to find the next device in the given direction matching the query (if provided)
                return iterateStage2(list, i, delta, query);
            }
        }

        // If we got here, there starting device wasn't found.  In this case, we have to just iterate forward from the start of the list and return the first device matching the criteria
        return iterateStage2(list, 0, 1, query);
    }

    IBleDevice getDevice_offset(final IBleDevice device, final int offset, Object... query)
    {
        return iterate(device, offset, query);
    }

    public IBleDevice getDevice(final int mask_BleDeviceState)
    {
        synchronized (m_lock)
        {
            for (IBleDevice device : m_map.values())
            {
                if (device.isAny(mask_BleDeviceState))
                    return device;
            }
        }

        return P_BleDeviceImpl.NULL;
    }

    public IBleDevice getDevice(BleDeviceState state)
    {
        synchronized (m_lock)
        {
            for (IBleDevice device : m_map.values())
            {
                if (device.is(state))
                    return device;
            }
        }

        return P_BleDeviceImpl.NULL;
    }

    public IBleDevice getDevice(Object ... query)
    {
        synchronized (m_lock)
        {
            for (IBleDevice device : m_map.values())
            {
                if (device.is(query))
                    return device;
            }
        }

        return P_BleDeviceImpl.NULL;
    }

    public List<IBleDevice> getDevices_List(boolean sort, Object... query)
    {
        final List<IBleDevice> list = sort ? getList_sorted() : getList();

        // Remove anything from the cloned list that shouldn't be there
        Iterator<IBleDevice> it = list.iterator();
        while (it.hasNext())
        {
            IBleDevice device = it.next();
            if (!device.is(query))
                it.remove();
        }

        return list;
    }

    public List<IBleDevice> getDevices_List(boolean sort, final BleDeviceState state)
    {
        final List<IBleDevice> list = sort ? getList_sorted() : getList();

        // Remove anything from the cloned list that shouldn't be there
        Iterator<IBleDevice> it = list.iterator();
        while (it.hasNext())
        {
            IBleDevice device = it.next();
            if (!device.is(state))
                it.remove();
        }

        return list;
    }

    public List<IBleDevice> getDevices_List(boolean sort, final int mask_BleDeviceState)
    {
        final List<IBleDevice> list = sort ? getList_sorted() : getList();

        // Remove anything from the cloned list that shouldn't be there
        Iterator<IBleDevice> it = list.iterator();
        while (it.hasNext())
        {
            IBleDevice device = it.next();
            if (!device.isAny(mask_BleDeviceState))
                it.remove();
        }

        return list;
    }

    public boolean has(IBleDevice device)
    {
        return device != null && m_map.containsKey(device.getMacAddress());
        // Uncomment below to get old functionality back where we don't rely on equals, but rather check for the exact instance
        //return m_map.get(device.getMacAddress()) == device;
    }

    //TODO:  Audit usage of this and see if we can get rid of it.  Random access is very slow
    public IBleDevice get(int i)
    {
        List<IBleDevice> list = getList();
        if (i < 0 || i >= list.size())
            return null;
        return list.get(i);
    }

    public int getDeviceIndex(final IBleDevice device)
    {
        synchronized (m_lock)
        {
            int count = 0;
            for (IBleDevice candidate : m_map.values())
            {
                if (candidate.equals(device))
                    return count;
                count++;
            }
        }

        return -1;
    }

    int getCount(Object[] query)
    {
        int count = 0;

        // Don't bother cloning here since the check is so trivial
        synchronized (m_lock)
        {
            for (IBleDevice device : m_map.values())
            {
                if (device.is(query))
                    ++count;
            }
        }

        return count;
    }

    int getCount(BleDeviceState state)
    {
        int count = 0;

        // Don't bother cloning here since the check is so trivial
        synchronized (m_lock)
        {
            for (IBleDevice device : m_map.values())
            {
                if (device.is(state))
                    ++count;
            }
        }

        return count;
    }

    int getCount()
    {
        synchronized (m_lock)
        {
            return m_map.size();
        }
    }

    public IBleDevice get(String uniqueId)
    {
        // Need to synchronize gets because modifications to the table can cause get to crash
        synchronized (m_lock)
        {
            return m_map.get(uniqueId);
        }
    }

    void add(final IBleDevice device)
    {
        if (device == null)
            return;

        synchronized (m_lock)
        {
            if (m_map.containsKey(device.getMacAddress()))
            {
                logger().e("Already registered device " + device.getMacAddress());
                return;
            }

            m_map.put(device.getMacAddress(), device);
        }
    }

    void remove(final IBleDevice device, final P_DeviceManager cache)
    {
        synchronized (m_lock)
        {
            doRemoval(device, cache);
        }
    }

    void removeAll(final P_DeviceManager cache)
    {
        synchronized (m_lock)
        {
            Iterator<Map.Entry<String, IBleDevice>> it = m_map.entrySet().iterator();

            while (it.hasNext())
            {
                Map.Entry<String, IBleDevice> entry = it.next();

                // Call the doRemove method, but tell it to not perform the actual removal itself...  We handle that here with the iterator
                doRemoval(entry.getValue(), cache, false);

                // Pull the entry out of the map
                it.remove();
            }
        }
    }

    private void doRemoval(IBleDevice device, P_DeviceManager cache)
    {
        doRemoval(device, cache, true);
    }

    private void doRemoval(IBleDevice device, P_DeviceManager cache, boolean actuallyRemove)
    {
        synchronized (m_lock)
        {
            m_mngr.ASSERT(m_map.containsKey(device.getMacAddress()), "");

            // Sometimes the caller may handle the actual removal (in an iterator, for example), so we only execute the remove here if told to
            if (actuallyRemove)
                m_map.remove(device.getMacAddress());

            final boolean cacheDevice = Utils_Config.bool(device.conf_device().cacheDeviceOnUndiscovery, device.conf_mngr().cacheDeviceOnUndiscovery);

            if (cacheDevice && cache != null)
                cache.add(device);
        }
    }

    void update(double timeStep)
    {
        if (m_purgeScanTime != null)
        {
            purgeStaleDevices();
            m_purgeScanTime = null;
            m_deviceManagerCache = null;
            m_discoveryListener = null;
        }

        //FIXME:  If we want to be super safe here, we should track removes that happen during the update loop, and not update devices that were removed
        // We can do this with a concurrenthashmap that is cleared here, populated when removes happen, and checked before calling update()
        // This will still allow us to do most of this process unlocked but also track removes in a safe way

        List<IBleDevice> updateList;

        synchronized (m_lock)
        {
            if (m_updating)
            {
                m_mngr.ASSERT(false, "Already updating.");
                return;
            }
            m_updating = true;

            // Clone the list so we don't have to worry about it changing mid iteration
            updateList = getList();
        }

        for (IBleDevice device : updateList)
            device.update(timeStep);

        synchronized (m_lock)
        {
            m_updating = false;
        }
    }

    void unbondAll(PE_TaskPriority priority, BondListener.Status status)
    {
        List<IBleDevice> list = getList();

        for (IBleDevice device : list)
        {
            if (device.getBondManager().isNativelyBondingOrBonded())
                device.unbond_internal(priority, status);

        }
    }

    void undiscoverAll()
    {
        List<IBleDevice> list = getList();

        for (IBleDevice device : list)
            device.undiscover();
    }

    void disconnectAll()
    {
        List<IBleDevice> list = getList();

        for (IBleDevice device : list)
            device.disconnect();
    }

    void disconnectAll_remote()
    {
        List<IBleDevice> list = getList();

        for (IBleDevice device : list)
            device.disconnect_remote();
    }

    void disconnectAllForTurnOff(PE_TaskPriority priority)
    {
        final P_DisconnectReason disconnectReason = new P_DisconnectReason(BleStatuses.GATT_STATUS_NOT_APPLICABLE)
                .setConnectFailReason(DeviceReconnectFilter.Status.BLE_TURNING_OFF)
                .setPriority(priority);
        List<IBleDevice> list = getList();

        for (IBleDevice device : list)
        {
            if (device.isAny(BleDeviceState.CONNECTING_OVERALL, BleDeviceState.BLE_CONNECTED))
            {
                disconnectReason.setTxnFailReason(P_Bridge_User.newReadWriteEventNULL(device.getBleDevice()));
                device.disconnectWithReason(disconnectReason);
            }
        }
    }

    void rediscoverDevicesAfterBleTurningBackOn()
    {
        List<IBleDevice> list = getList();

        for (IBleDevice device : list)
        {
            if (!device.is(BleDeviceState.DISCOVERED))
            {
                device.onNewlyDiscovered(device.nativeManager().getDeviceLayer(), null, device.getRssi(), device.getScanRecord(), device.getOrigin());

                final DiscoveryListener listener = m_mngr.getListener_Discovery();
                if (listener != null)
                {
                    DiscoveryEvent event = P_Bridge_User.newDiscoveryEvent(m_mngr.getBleDevice(device), LifeCycle.DISCOVERED);
                    m_mngr.postEvent(listener, event);
                }
            }
        }
    }

    void reconnectDevicesAfterBleTurningBackOn()
    {
        List<IBleDevice> list = getList();

        for (IBleDevice device : list)
        {
            final boolean autoReconnectDeviceWhenBleTurnsBackOn = Utils_Config.bool(device.conf_device().autoReconnectDeviceWhenBleTurnsBackOn, device.conf_mngr().autoReconnectDeviceWhenBleTurnsBackOn);

            if (autoReconnectDeviceWhenBleTurnsBackOn && device.lastDisconnectWasBecauseOfBleTurnOff())
                device.connect(null, null, null);
        }
    }

    void clearDeviceListeners()
    {
        synchronized (m_lock)
        {
            List<IBleDevice> list = getList();
            for (IBleDevice d : list)
            {
                d.clearListeners();
            }
        }
    }

    void undiscoverAllForTurnOff(final P_DeviceManager cache, final PA_StateTracker.E_Intent intent)
    {
        List<IBleDevice> list;

        synchronized (m_lock)
        {
            //FIXME:  Why is this only an assert when in every other case we return?
            m_mngr.ASSERT(!m_updating, "Undiscovering devices while updating!");

            list = getList();
        }

        for (IBleDevice device : list)
        {
            if (device.is(BleDeviceState.BLE_CONNECTED))
            {
                device.getNativeManager().updateNativeConnectionState(device.getNativeBleGatt());
                device.onNativeDisconnect(false, BleStatuses.GATT_STATUS_NOT_APPLICABLE, false, true);
            }

            final boolean retainDeviceWhenBleTurnsOff = Utils_Config.bool(device.conf_device().retainDeviceWhenBleTurnsOff, device.conf_mngr().retainDeviceWhenBleTurnsOff);

            if (!retainDeviceWhenBleTurnsOff)
                undiscoverAndRemove(device, m_mngr.getListener_Discovery(), cache, intent);
            else
            {
                final boolean undiscoverDeviceWhenBleTurnsOff = Utils_Config.bool(device.conf_device().undiscoverDeviceWhenBleTurnsOff, device.conf_mngr().undiscoverDeviceWhenBleTurnsOff);

                if (undiscoverDeviceWhenBleTurnsOff)
                    undiscoverDevice(device, m_mngr.getListener_Discovery(), intent);
            }
        }
    }

    private static void undiscoverDevice(IBleDevice device, DiscoveryListener listener, PA_StateTracker.E_Intent intent)
    {
        if (device == null || !device.is(BleDeviceState.DISCOVERED))
            return;

        device.onUndiscovered(intent);

        if (listener != null)
        {
            DiscoveryEvent event = P_Bridge_User.newDiscoveryEvent(device.getBleDevice(), LifeCycle.UNDISCOVERED);
            device.getIManager().postEvent(listener, event);
        }
    }

    void undiscoverAndRemove(IBleDevice device, DiscoveryListener discoveryListener, P_DeviceManager cache, PA_StateTracker.E_Intent intent)
    {
        if (device == null)
            return;

        remove(device, cache);

        undiscoverDevice(device, discoveryListener, intent);
    }

    void requestPurge(final double scanTime, final P_DeviceManager cache, final DiscoveryListener listener)
    {
        m_purgeScanTime = scanTime;
        m_deviceManagerCache = cache;
        m_discoveryListener = listener;
    }

    void purgeStaleDevices()
    {
        List<IBleDevice> list;
        synchronized (m_lock)
        {
            list = getList();
        }

        for (IBleDevice device : list)
        {
            Interval minScanTimeToInvokeUndiscovery = Utils_Config.interval(device.conf_device().minScanTimeNeededForUndiscovery, device.conf_mngr().minScanTimeNeededForUndiscovery);
            if (Interval.isDisabled(minScanTimeToInvokeUndiscovery))
                continue;

            Interval scanKeepAlive_interval = Utils_Config.interval(device.conf_device().undiscoveryKeepAlive, device.conf_mngr().undiscoveryKeepAlive);
            if (Interval.isDisabled(scanKeepAlive_interval))
                continue;

            if (m_purgeScanTime < Interval.secs(minScanTimeToInvokeUndiscovery))
                continue;

            final boolean purgeable = device.getOrigin() != BleDeviceOrigin.EXPLICIT && ((device.getStateMask() & ~P_Bridge_User.bleDeviceStatePurgeableMask()) == 0x0);

            if (purgeable)
            {
                if (device.getTimeSinceLastDiscovery() > scanKeepAlive_interval.secs())
                    undiscoverAndRemove(device, m_discoveryListener, m_deviceManagerCache, PA_StateTracker.E_Intent.UNINTENTIONAL);
            }
        }
    }

    boolean hasDevice(BleDeviceState... filter)
    {
        // If the filter is null or empty, report true if we have any device at all
        if (filter == null || filter.length == 0)
            return getCount() > 0;

        // Don't bother cloning the list here since the isAny check is so quick and cannot possibly cascade back into other list operations
        synchronized (m_lock)
        {
            for (IBleDevice device : m_map.values())
            {
                if (device.isAny(filter))
                    return true;
            }
        }

        return false;
    }
}
