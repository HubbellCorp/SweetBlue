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
