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

package com.idevicesinc.sweetblue.toolbox.util;


import android.content.Context;
import android.content.SharedPreferences;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.utils.BleScanRecord;
import com.idevicesinc.sweetblue.utils.Utils_Byte;
import com.idevicesinc.sweetblue.utils.Utils_String;

import java.util.List;
import java.util.UUID;


public class NameManager
{

    private static NameManager s_instance;


    public static NameManager get(Context context)
    {
        if (s_instance == null)
            s_instance = new NameManager(context);
        return s_instance;
    }

    public static NameManager get()
    {
        return s_instance;
    }


    private Context m_context;


    private NameManager(Context context)
    {
        m_context = context.getApplicationContext();
    }

    public String getName(BleDevice device, UUID uuid, String defaultName)
    {
        SharedPreferences prefs = getPrefs(device);
        String name = prefs.getString(uuid.toString(), null);
        if (name == null)
            return defaultName;
        return name;
    }

    public void saveName(BleDevice device, UUID uuid, String name)
    {
        SharedPreferences prefs = getPrefs(device);
        prefs.edit().putString(uuid.toString(), name).commit();
    }

    private SharedPreferences getPrefs(BleDevice device)
    {
        BleScanRecord info = device.getScanInfo();
        List<UUID> serviceUuids = info.getServiceUUIDS();
        if (serviceUuids.size() == 1)
        {
            return m_context.getSharedPreferences(serviceUuids.get(0).toString(), Context.MODE_PRIVATE);
        }
        else
        {
            short id = info.getManufacturerId();
            byte[] data = info.getManufacturerData();
            if (id == -1 && data == null || data.length == 0)
            {
                return m_context.getSharedPreferences(device.getMacAddress(), Context.MODE_PRIVATE);
            }
            String dataHex = Utils_String.bytesToHexString(data);
            if (id != -1)
                dataHex = Utils_String.bytesToHexString(Utils_Byte.shortToBytes(id)) + dataHex;
            return m_context.getSharedPreferences(dataHex, Context.MODE_PRIVATE);
        }
    }


}
