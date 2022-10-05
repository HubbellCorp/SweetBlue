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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import com.idevicesinc.sweetblue.utils.EmptyIterator;
import com.idevicesinc.sweetblue.utils.State;


// Adding this suppresslint annotation as we always want to make sure we save to disk immediately.
@SuppressLint("ApplySharedPref")
final class P_DiskOptionsManager
{
    private static final int ACCESS_MODE = Context.MODE_PRIVATE;
    private static final String PHONE_NAME_KEY = "Phone_Advertising_Name";

    //--- DRK > Just adding some salt to these to mitigate any possible conflict.
    private enum E_Namespace
    {
        LAST_DISCONNECT("sweetblue_16l@{&a}"),
        NEEDS_BONDING("sweetblue_p59=F%k"),
        DEVICE_NAME("sweetblue_qurhzpoc"),
        ADAPTOR_NAME("sweetblue_9nc@x82kg_4");


        private final String m_key;

        E_Namespace(final String key)
        {
            m_key = key;
        }

        public String key()
        {
            return m_key;
        }
    }

    private final IBleManager m_manager;

    private final HashMap<String, Integer> m_inMemoryDb_lastDisconnect = new HashMap<>();
    private final HashMap<String, Boolean> m_inMemoryDb_needsBonding = new HashMap<>();
    private final HashMap<String, String> m_inMemoryDb_name = new HashMap<>();
    private final HashMap<Object, String> m_inMemoryDb_adaptorName = new HashMap<>();

    private final HashMap[] m_inMemoryDbs = new HashMap[E_Namespace.values().length];

    private final SharedPreferences[] m_prefsInstances = new SharedPreferences[E_Namespace.values().length];


    P_DiskOptionsManager(P_BleManagerImpl manager)
    {
        m_manager = manager;

        m_inMemoryDbs[E_Namespace.LAST_DISCONNECT.ordinal()] = m_inMemoryDb_lastDisconnect;
        m_inMemoryDbs[E_Namespace.NEEDS_BONDING.ordinal()] = m_inMemoryDb_needsBonding;
        m_inMemoryDbs[E_Namespace.DEVICE_NAME.ordinal()] = m_inMemoryDb_name;
        m_inMemoryDbs[E_Namespace.ADAPTOR_NAME.ordinal()] = m_inMemoryDb_adaptorName;

        final E_Namespace[] values = E_Namespace.values();

        for (int i = 0; i < values.length; i++)
        {
            Object ith = m_inMemoryDbs[i];

            if (ith == null)
                throw new Error("Expected in-memory DB to be not null");
        }
    }

    private SharedPreferences prefs(E_Namespace namespace)
    {
        SharedPreferences prefs = m_prefsInstances[namespace.ordinal()];
        if (prefs == null)
        {
            prefs = m_manager.getApplicationContext().getSharedPreferences(namespace.key(), ACCESS_MODE);
            m_prefsInstances[namespace.ordinal()] = prefs;
        }

        return prefs;
    }


    void saveLastDisconnect(final String mac, final State.ChangeIntent changeIntent, final boolean hitDisk)
    {
        final int diskValue = State.ChangeIntent.toDiskValue(changeIntent);
        m_inMemoryDb_lastDisconnect.put(mac, diskValue);

        if (!hitDisk) return;

        prefs(E_Namespace.LAST_DISCONNECT).edit().putInt(mac, diskValue).commit();
    }

    State.ChangeIntent loadLastDisconnect(final String mac, final boolean hitDisk)
    {
        final Integer value_memory = m_inMemoryDb_lastDisconnect.get(mac);

        if (value_memory != null)
        {
            final State.ChangeIntent lastDisconnect_memory = State.ChangeIntent.fromDiskValue(value_memory);

            return lastDisconnect_memory;
        }

        if (!hitDisk) return State.ChangeIntent.NULL;

        final SharedPreferences prefs = prefs(E_Namespace.LAST_DISCONNECT);

        final int value_disk = prefs.getInt(mac, State.ChangeIntent.NULL.toDiskValue());

        final State.ChangeIntent lastDisconnect = State.ChangeIntent.fromDiskValue(value_disk);

        return lastDisconnect;
    }

    void saveAdaptorAdvertisingName(String name)
    {
        String n = name;
        if (n == null)
            n = "";

        m_inMemoryDb_adaptorName.put(null, n);

        prefs(E_Namespace.ADAPTOR_NAME).edit().putString(PHONE_NAME_KEY, n).commit();
    }

    boolean hasAdaptorAdvertisingName()
    {
        final String value_memory = m_inMemoryDb_adaptorName.get(null);

        if (value_memory != null)   return true;

        final SharedPreferences prefs = prefs(E_Namespace.ADAPTOR_NAME);

        final String value_disk = prefs.getString(PHONE_NAME_KEY, null);

        return value_disk != null;
    }

    // Don't use this for checking the name for the first time.
    String getAdaptorAdvertisingName()
    {
        final String value_memory = m_inMemoryDb_adaptorName.get(null);

        if (value_memory != null)   return value_memory;

        final SharedPreferences prefs = prefs(E_Namespace.ADAPTOR_NAME);

        final String value_disk = prefs.getString(PHONE_NAME_KEY, "");

        return value_disk;
    }

    void saveNeedsBonding(final String mac)
    {
        m_inMemoryDb_needsBonding.put(mac, true);
    }

    void clearNeedsBonding(final String mac)
    {
        m_inMemoryDb_needsBonding.remove(mac);
    }

    boolean loadNeedsBonding(final String mac)
    {
        final Boolean value_memory = m_inMemoryDb_needsBonding.get(mac);

        if (value_memory != null)   return value_memory;

        return false;
    }

    void saveName(final String mac, final String name, final boolean hitDisk)
    {
        final String name_override = name != null ? name : "";

        m_inMemoryDb_name.put(mac, name_override);

        if (!hitDisk) return;

        prefs(E_Namespace.DEVICE_NAME).edit().putString(mac, name_override).commit();
    }

    String loadName(final String mac, final boolean hitDisk)
    {
        final String value_memory = m_inMemoryDb_name.get(mac);

        if (value_memory != null)   return value_memory;

        if (!hitDisk) return null;

        final SharedPreferences prefs = prefs(E_Namespace.DEVICE_NAME);

        final String value_disk = prefs.getString(mac, null);

        return value_disk;
    }

    void clear()
    {
        final E_Namespace[] values = E_Namespace.values();

        for (int i = 0; i < values.length; i++)
        {
            final SharedPreferences prefs = prefs(values[i]);
            prefs.edit().clear().commit();

            final HashMap ith = m_inMemoryDbs[i];

            if (ith != null)
                ith.clear();
        }
    }

    void clearName(final String macAddress)
    {
        final E_Namespace namespace = E_Namespace.DEVICE_NAME;

        clearNamespace(macAddress, namespace);
    }

    void clear(final String macAddress)
    {
        final E_Namespace[] values = E_Namespace.values();

        for (E_Namespace value : values)
            clearNamespace(macAddress, value);
    }

    Iterator<String> getPreviouslyConnectedDevices()
    {
        final SharedPreferences prefs = prefs(E_Namespace.LAST_DISCONNECT);

        Map<String, ?> map = prefs.getAll();

        if (map != null)
        {
            List<String> keys = new ArrayList<>(map.keySet());
            Collections.sort(keys);
            return keys.iterator();
        }
        else
            return new EmptyIterator<>();
    }


    private void clearNamespace(final String macAddress, final E_Namespace namespace)
    {
        final int ordinal = namespace.ordinal();
        final SharedPreferences prefs = prefs(namespace);
        prefs.edit().remove(macAddress).commit();

        final HashMap ith = m_inMemoryDbs[ordinal];

        if (ith != null)
            ith.remove(macAddress);
    }
}
