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

package com.idevicesinc.sweetblue.utils;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothProfile;
import android.util.SparseArray;
import com.idevicesinc.sweetblue.BleDeviceConfig;
import com.idevicesinc.sweetblue.BleStatuses;
import java.lang.reflect.Field;
import java.util.Calendar;


public final class CodeHelper
{

    // No instances
    private CodeHelper() {}



    private static SparseArray<String> m_gattStatusCodes = null;
    private static SparseArray<String> m_gattConnStates = null;
    private static SparseArray<String> m_gattConnStatusCodes = null;
    private static SparseArray<String> m_gattBleStates = null;
    private static SparseArray<String> m_gattBondStates = null;
    private static SparseArray<String> m_unbondReasonCodes = null;





    public static String gattConn(int code, boolean enabled)
    {
        String name = "NO_NAME";

        if( m_gattConnStates == null && enabled )
        {
            initConnStates();
        }

        if( m_gattConnStates != null )
        {
            String actualName = m_gattConnStates.get(code);
            name = actualName != null ? actualName : name;
        }
        return Utils_String.makeString(name, "(", code, ")");
    }

    public static String gattStatus(int code, boolean enabled)
    {
        String errorName = "GATT_STATUS_NOT_APPLICABLE";

        if( m_gattStatusCodes == null && enabled )
            initGattStatusCodes();

        String actualErrorName = m_gattStatusCodes != null ? m_gattStatusCodes.get(code) : null;
        errorName = actualErrorName != null ? actualErrorName : errorName;

        return Utils_String.makeString(errorName, "(", code, ")");
    }

    public static String gattConnStatus(int code, boolean enabled)
    {
        String errorName = "GATT_STATUS_NOT_APPLICABLE";

        if( m_gattConnStatusCodes == null && enabled )
        {
            initGattConnStatusCodes();
        }

        String actualErrorName = m_gattConnStatusCodes != null ? m_gattConnStatusCodes.get(code) : null;
        // If we couldn't find a relevant gatt conn code, then we'll fall back to the old gattStatus checks
        if (actualErrorName == null)
            return gattStatus(code, enabled);
        errorName = "GATT_" + actualErrorName;

        return Utils_String.makeString(errorName, "(", code, ")");
    }

    public static String gattBleState(int code, boolean enabled)
    {
        String name = "NO_NAME";

        if( m_gattBleStates == null && enabled )
        {
            initGattBleStates();
        }

        if( m_gattBleStates != null )
        {
            String actualName = m_gattBleStates.get(code);
            name = actualName != null ? actualName : name;
        }

        return Utils_String.makeString(name, "(", code, ")");
    }

    public static String gattUnbondReason(int code, boolean enabled)
    {
        String name = "NO_NAME";

        if( m_unbondReasonCodes == null && enabled )
        {
            initUnbondReasonCodes();
        }

        if( m_unbondReasonCodes != null )
        {
            String actualName = m_unbondReasonCodes.get(code);
            name = actualName != null ? actualName : name;
        }

        return Utils_String.makeString(name, "(", code, ")");
    }

    public static String gattBondState(int code, boolean enabled)
    {
        String name = "NO_NAME";

        if( m_gattBondStates == null && enabled )
        {
            initGattBondStates();
        }

        if( m_gattBondStates != null )
        {
            String actualName = m_gattBondStates.get(code);
            name = actualName != null ? actualName : name;
        }

        return Utils_String.makeString(name, "(", code, ")");
    }

    public static void clearAll()
    {
        m_gattStatusCodes = null;
        m_gattConnStates = null;
        m_gattConnStatusCodes = null;
        m_gattBleStates = null;
        m_gattBondStates = null;
        m_unbondReasonCodes = null;
    }



    private static synchronized void initConnStates()
    {
        if( m_gattConnStates != null )  return;

        m_gattConnStates = new SparseArray<>();

        initFromReflection(BluetoothProfile.class, "STATE_", m_gattConnStates);
    }

    private static synchronized void initGattStatusCodes()
    {
        if( m_gattStatusCodes != null )  return;

        m_gattStatusCodes = new SparseArray<>();

        initFromReflection(BluetoothGatt.class, "GATT_", m_gattStatusCodes);
        initFromReflection(BleDeviceConfig.class, "GATT_", m_gattStatusCodes);
        initFromReflection(BleStatuses.class, "GATT_", m_gattStatusCodes);
    }

    private static synchronized void initGattConnStatusCodes()
    {
        if (m_gattConnStatusCodes != null)	return;

        m_gattConnStatusCodes = new SparseArray<>();

        initFromReflection(BleStatuses.class, "CONN_", m_gattConnStatusCodes);
    }

    private static synchronized void initGattBleStates()
    {
        if( m_gattBleStates != null )  return;

        m_gattBleStates = new SparseArray<>();

        initFromReflection(BluetoothAdapter.class, "STATE_", m_gattBleStates);

        m_gattBleStates.put(BluetoothAdapter.ERROR, "ERROR");
    }

    private static synchronized void initUnbondReasonCodes()
    {
        if( m_unbondReasonCodes != null )  return;

        m_unbondReasonCodes = new SparseArray<>();

        initFromReflection(BluetoothDevice.class, "UNBOND_REASON_", m_unbondReasonCodes);
        initFromReflection(BleStatuses.class, "BOND_FAIL_REASON", m_unbondReasonCodes);
    }

    private static synchronized void initGattBondStates()
    {
        if( m_gattBondStates != null )  return;

        m_gattBondStates = new SparseArray<>();

        initFromReflection(BluetoothDevice.class, "BOND_", m_gattBondStates);
    }

    private static void initFromReflection(Class<?> clazz, String fieldSuffix, SparseArray<String> map)
    {
        for( Field field : clazz.getFields() )
        {
            String fieldName = field.getName();

            if( !fieldName.contains(fieldSuffix) )  continue;

            Integer fieldValue = -1;

            try {
                fieldValue = field.getInt(null);
            } catch (IllegalAccessException e) {
//				e.printStackTrace();
            } catch (IllegalArgumentException e) {
//				e.printStackTrace();
            }

            if( map.indexOfKey(fieldValue) < 0 )
            {
                map.put(fieldValue, fieldName);
            }
        }
    }

}
