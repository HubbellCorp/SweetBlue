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

package com.idevicesinc.sweetblue.toolbox.device;


import androidx.annotation.NonNull;

import com.idevicesinc.sweetblue.BleDevice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class BleDeviceLogManager
{
    private static BleDeviceLogManager sInstance = null;

    public static BleDeviceLogManager getInstance()
    {
        if (sInstance == null)
            sInstance = new BleDeviceLogManager();
        return sInstance;
    }

    public enum EventType
    {
        Read,
        Notify
    };

    // An individual log entry
    public static class BleDeviceLogEntry implements Comparable<BleDeviceLogEntry>
    {
        long mTimestamp;
        byte[] mValue;

        BleDeviceLogEntry(long timestamp, byte[] event)
        {
            mTimestamp = timestamp;
            mValue = event;
        }

        public long getTimestamp()
        {
            return mTimestamp;
        }

        public byte[] getValue()
        {
            return mValue;
        }

        @Override
        public int compareTo(@NonNull BleDeviceLogEntry bleDeviceLogEntry)
        {
            long cmp = mTimestamp - bleDeviceLogEntry.mTimestamp;
            return cmp < 0 ? -1 : (cmp > 0 ? 1 : 0);
        }
    }

    // Class that holds lists of log entries
    private static class BleDeviceLog
    {
        private static int sLogSizeLimit = 10;  // Max entries before we eject

        Map<EventType, Map<String, List<BleDeviceLogEntry>>> mEventLog = new HashMap<>();

        // Add an entry to the log
        void addEntry(EventType et, UUID charUuid, byte[] value)
        {
            List<BleDeviceLogEntry> list = getListForEventType(et, charUuid.toString());
            while (list.size() >= sLogSizeLimit && sLogSizeLimit > 0)
            {
                list.remove(0);
            }

            list.add(new BleDeviceLogEntry(System.currentTimeMillis(), value));
        }

        List<BleDeviceLogEntry> getListForEventType(EventType et, String uuid)
        {
            uuid = uuid.toLowerCase(Locale.US);

            // Get appropriate map for event type
            Map<String, List<BleDeviceLogEntry>> m = mEventLog.get(et);
            if (m == null)
            {
                m = new HashMap<>();
                mEventLog.put(et, m);
            }

            // Get list for given char uuid
            List<BleDeviceLogEntry> list = m.get(uuid);
            if (list == null)
            {
                list = new ArrayList<>();
                m.put(uuid, list);
            }

            return list;
        }
    }

    protected Map<String, BleDeviceLog> mLogMap = new HashMap<>();

    private BleDeviceLogManager()
    {
    }

    public BleDeviceLogEntry getLatestEntryForDevice(String macAddress, EventType et, String uuid)
    {
        macAddress = macAddress.toLowerCase(Locale.US);
        BleDeviceLog log = mLogMap.get(macAddress);
        if (log == null)
            return null;
        List<BleDeviceLogEntry> list = log.getListForEventType(et, uuid);
        return list.size() > 0 ? list.get(list.size() - 1) : null;
    }

    public BleDeviceLogEntry getLatestEntryForDevice(BleDevice device, EventType et, String uuid)
    {
        return device != null ? getLatestEntryForDevice(device.getMacAddress(), et, uuid) : null;
    }

    public BleDeviceLogEntry getLatestEntryForDevice(String macAddress, String uuid)
    {
        BleDeviceLogEntry best = null;
        for (EventType et : EventType.values())
        {
            BleDeviceLogEntry candidate = getLatestEntryForDevice(macAddress, et, uuid);

            if (candidate == null)
                continue;

            if (best == null || best.compareTo(candidate) < 0)
                best = candidate;
        }

        return best;
    }

    public BleDeviceLogEntry getLatestEntryForDevice(BleDevice device, String uuid)
    {
        return getLatestEntryForDevice(device.getMacAddress(), uuid);
    }

    public List<BleDeviceLogEntry> getEntryListForDevice(String macAddress, EventType et, String uuid)
    {
        BleDeviceLog log = mLogMap.get(macAddress);
        if (log == null)
            return null;

        // Return a defensive copy
        List<BleDeviceLogEntry> list = new ArrayList<>();
        list.addAll(log.getListForEventType(et, uuid));
        return list;
    }

    public List<BleDeviceLogEntry> getEntryListForDevice(BleDevice device, EventType et, String uuid)
    {
        return device != null ? getEntryListForDevice(device.getMacAddress(), et, uuid) : null;
    }

    public void addEntryForDevice(String macAddress, EventType et, UUID charUuid, byte[] value)
    {
        macAddress = macAddress.toLowerCase(Locale.US);
        BleDeviceLog log = mLogMap.get(macAddress);
        if (log == null)
        {
            log = new BleDeviceLog();
            mLogMap.put(macAddress, log);
        }
        log.addEntry(et, charUuid, value);
    }

    public void addEntryForDevice(BleDevice device, EventType et, UUID charUuid, byte[] value)
    {
        if (device != null)
            addEntryForDevice(device.getMacAddress(), et, charUuid, value);
    }

}
