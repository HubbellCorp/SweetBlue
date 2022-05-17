package com.idevicesinc.sweetblue.toolbox.util;


import android.content.Context;

import com.idevicesinc.sweetblue.BleCharacteristic;
import com.idevicesinc.sweetblue.BleDescriptor;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleService;
import com.idevicesinc.sweetblue.toolbox.R;
import com.idevicesinc.sweetblue.utils.Utils_Byte;
import com.idevicesinc.sweetblue.utils.Utils_String;
import com.idevicesinc.sweetblue.utils.Uuids;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public final class UuidUtil
{

    private static String CUSTOM_SERVICE = "CUSTOM SERVICE";
    private static String CUSTOM_CHARACTERISTIC = "CUSTOM CHARACTERISTIC";
    private static String CUSTOM_DESCRIPTOR = "CUSTOM DESCRIPTOR";

    private static Map<UUID, Field> uuidFields;

    private static NameManager nameManager;


    public static void makeStrings(Context context)
    {
        CUSTOM_SERVICE = context.getString(R.string.custom_service);

        CUSTOM_CHARACTERISTIC = context.getString(R.string.custom_characteristic);

        CUSTOM_DESCRIPTOR = context.getString(R.string.custom_descriptor);

        nameManager = NameManager.get(context);
    }


    public static Name getServiceName(BleDevice device, BleService service)
    {
        if (uuidFields == null)
        {
            uuidFields = getUuidFields();
        }
        Field field = uuidFields.get(service.getUuid());
        if (field == null)
        {
            checkNameManager(device);
            String name = nameManager.getName(device, service.getUuid(), CUSTOM_SERVICE);
            return new Name(name, true);
        }
        else
        {
            return new Name(field.getName().replace("_UUID", "").replace("_", " "), false);
        }
    }

    public static void saveUuidName(BleDevice device, UUID uuid, String name)
    {
        checkNameManager(device);
        nameManager.saveName(device, uuid, name);
    }

    public static Name getCharacteristicName(BleDevice device, BleCharacteristic characteristic)
    {
        if (uuidFields == null)
        {
            uuidFields = getUuidFields();
        }
        Field field = uuidFields.get(characteristic.getUuid());
        if (field == null)
        {
            checkNameManager(device);
            String name = nameManager.getName(device, characteristic.getUuid(), CUSTOM_CHARACTERISTIC);
            return new Name(name, true);
        }
        else
        {
            return new Name(field.getName().replace("_UUID", "").replace("_", " "), false);
        }
    }

    public static Name getDescriptorName(BleDevice device, BleDescriptor descriptor)
    {
        if (uuidFields == null)
        {
            uuidFields = getUuidFields();
        }
        Field field = uuidFields.get(descriptor.getUuid());
        if (field == null)
        {
            checkNameManager(device);
            String name = nameManager.getName(device, descriptor.getUuid(), CUSTOM_DESCRIPTOR);
            return new Name(name, true);
        }
        else
        {
            return new Name(field.getName().replace("_UUID", "").replace("_", " "), false);
        }
    }

    private static Map<UUID, Field> getUuidFields()
    {
        if (uuidFields == null)
        {
            try
            {
                Field[] fields = Uuids.class.getDeclaredFields();
                Map<UUID, Field> map = new HashMap<>(fields.length);
                for (Field f : fields)
                {
                    if (f.getType() == UUID.class)
                    {
                        map.put((UUID) f.get(f), f);
                    }
                }
                return map;
            } catch (Exception e)
            {
                e.printStackTrace();
                return new HashMap<>();
            }
        }
        else
        {
            return uuidFields;
        }
    }

    public static String getShortUuid(UUID uuid)
    {
        long msb = uuid.getMostSignificantBits();
        byte[] msbBytes = Utils_Byte.longToBytes(msb);
        String hex = Utils_String.bytesToHexString(msbBytes);
        hex = "0x" + hex.substring(4, 8);
        return hex;
    }



    private static void checkNameManager(BleDevice device)
    {
        if (nameManager == null)
            makeStrings(device.getManager().getApplicationContext());
    }




    public static final class Name
    {
        public final String name;
        public final boolean custom;

        Name(String name, boolean custom)
        {
            this.name = name;
            this.custom = custom;
        }
    }

}
