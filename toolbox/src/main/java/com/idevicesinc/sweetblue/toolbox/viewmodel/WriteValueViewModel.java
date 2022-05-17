package com.idevicesinc.sweetblue.toolbox.viewmodel;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.idevicesinc.sweetblue.BleCharacteristic;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleService;
import com.idevicesinc.sweetblue.BleWrite;
import com.idevicesinc.sweetblue.ReadWriteListener;
import com.idevicesinc.sweetblue.toolbox.util.BleHelper;
import com.idevicesinc.sweetblue.toolbox.util.UuidUtil;
import com.idevicesinc.sweetblue.utils.Uuids;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class WriteValueViewModel extends AndroidViewModel
{
    private BleManager mBleManager;
    private BleDevice mDevice;
    private BleService mService;
    private BleCharacteristic mCharacteristic;

    private final String SHARED_PREFERENCES_FILE_NAME = "SAVED_VALUES";
    private List<SavedValue> mSavedValueList = new ArrayList<>();
    private boolean mSavedValueListDirty = false;

    public WriteValueViewModel(@NonNull Application application)
    {
        super(application);
    }

    public void init(String mac, String serviceUUID, String characteristicUUID)
    {
        mBleManager = BleHelper.get().getMgr();
        mDevice = mBleManager.getDevice(mac);
        mService = mDevice.getNativeBleService(UUID.fromString(serviceUUID));

        List<BleCharacteristic> charList = mService.getCharacteristics();
        if (characteristicUUID != null)
        {
            for (BleCharacteristic bgc : charList)
            {
                if (characteristicUUID.equals(bgc.getUuid().toString()))
                {
                    mCharacteristic = bgc;
                    break;
                }
            }
        }
    }

    public void saveSavedValues()
    {
        if (!mSavedValueListDirty)
            return;

        SharedPreferences sp = getApplication().getSharedPreferences(SHARED_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        for (SavedValue sv : mSavedValueList)
            sv.writePreference(editor);

        editor.commit();
    }


    public void loadSavedValues()
    {
        mSavedValueList.clear();

        SharedPreferences sp = getApplication().getSharedPreferences(SHARED_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);
        Set<String> keySet = sp.getAll().keySet();

        for (String key : keySet)
        {
            String s = sp.getString(key, null);

            SavedValue sv = SavedValue.parse(key, s);

            mSavedValueList.add(sv);
        }

        Collections.sort(mSavedValueList);
    }

    public List<SavedValue> getSavedValues()
    {
        // Return a defensive copy
        List<SavedValue> l = new ArrayList<>(mSavedValueList);
        return l;
    }

    public void addSavedValue(String saveAsName, String valueString, Uuids.GATTFormatType gft)
    {
        SavedValue sv = new SavedValue(saveAsName, valueString, gft);
        // Remove then add so we replace identically named values
        mSavedValueList.remove(sv);
        mSavedValueList.add(sv);
        mSavedValueListDirty = true;
    }

    public void deleteSavedValue(WriteValueViewModel.SavedValue sv)
    {
        mSavedValueList.remove(sv);
        mSavedValueListDirty = true;
    }

    public void writeValue(String valueString, final Uuids.GATTFormatType gattFormatType, ReadWriteListener readWriteListener) throws Uuids.GATTCharacteristicFormatTypeConversionException
    {
        final byte[] rawVal = gattFormatType.stringToByteArray(valueString);
        mDevice.write(new BleWrite(mCharacteristic.getUuid()).setBytes(rawVal), readWriteListener);
    }

    public boolean deviceNativeServicesListEmpty()
    {
        return mDevice.getNativeServices_List() == null || mDevice.getNativeServices_List().size() == 0;
    }

    public boolean isServiceNull()
    {
        return mService == null;
    }

    public boolean isCharacteristicNull()
    {
        return mCharacteristic == null;
    }

    public String getCharacteristicName()
    {
        return UuidUtil.getCharacteristicName(mDevice, mCharacteristic).name;
    }

    public BleDevice device()
    {
        return mDevice;
    }

    public static class SavedValue implements Comparable<SavedValue>
    {
        String mName;
        String mValueString;
        Uuids.GATTFormatType mGattFormatType;

        SavedValue(String name, String value, Uuids.GATTFormatType gft)
        {
            mName = name;
            mValueString = value;
            mGattFormatType = gft;
        }

        public String getName()
        {
            return mName;
        }

        public String getValueString()
        {
            return mValueString;
        }

        public Uuids.GATTFormatType getGATTFormatType()
        {
            return mGattFormatType;
        }

        static SavedValue parse(String name, String packed)
        {
            String splits[] = packed.split("\\|", 2);
            String lenString = splits[0];
            String leftover = splits[1];

            Integer len = Integer.parseInt(lenString);

            String value = leftover.substring(0, len);
            String typeString = leftover.substring(len);
            Uuids.GATTFormatType gft = Uuids.GATTFormatType.valueOf(typeString);

            SavedValue sv = new SavedValue(name, value, gft);
            return sv;
        }

        void writePreference(SharedPreferences.Editor editor)
        {
            StringBuilder sb = new StringBuilder();

            sb.append("" + mValueString.length());
            sb.append("|");
            sb.append(mValueString);
            sb.append(mGattFormatType.name());

            editor.putString(mName, sb.toString());
        }

        @Override
        public int compareTo(@NonNull SavedValue o)
        {
            return mName.compareTo(o.mName);
        }
    }
}
