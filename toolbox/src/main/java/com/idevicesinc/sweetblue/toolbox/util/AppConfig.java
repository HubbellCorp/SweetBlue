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

import com.idevicesinc.sweetblue.toolbox.viewmodel.DashboardViewModel;

import java.util.Map;
import java.util.Set;

public class AppConfig
{
    public enum ConfigurationOption
    {
        BleConfigJSON(null),
        ScanApi(null),
        SortOption(DashboardViewModel.SortBy.Unsorted),
        ScanFilter(null),
        LogLevel(null); //TODO

        Object mDefaultValue;

        ConfigurationOption(Object defaultValue)
        {
            mDefaultValue = defaultValue;
        }

        public Object getDefaultValue()
        {
            return mDefaultValue;
        }

    }

    private static AppConfig sInstance = null;
    private final static String kSharedPreferencesFilename = "SBTB_PREFS";

    protected Context mContext;
    protected SharedPreferences mSharedPreferences;

    public static AppConfig getInstance()
    {
        return sInstance;
    }

    public static AppConfig getInstance(Context context)
    {
        if (sInstance == null && context != null)
            sInstance = new AppConfig(context);
        return sInstance;
    }

    public static void startup(Context context)
    {
        sInstance = new AppConfig(context);
    }

    private AppConfig(Context context)
    {
        mContext = context;
        mSharedPreferences = mContext.getSharedPreferences(kSharedPreferencesFilename, Context.MODE_PRIVATE);
        mContext = null;  // Don't hold ref, might leak memory.  Look into this more
    }

    public <T extends Object> T getConfigurationOption(ConfigurationOption co)
    {
        if (co == null)
            return null;

        Map<String, ?> m = mSharedPreferences.getAll();

        Object val = m.get(co.name());

        return (T)(val != null ? val : co.getDefaultValue());
    }

    public boolean getConfigurationOptionBoolean(ConfigurationOption co)
    {
        return (Boolean)getConfigurationOption(co);
    }

    public int getConfigurationOptionInt(ConfigurationOption co)
    {
        return (Integer)getConfigurationOption(co);
    }

    public long getConfigurationOptionLong(ConfigurationOption co)
    {
        return (Long)getConfigurationOption(co);
    }

    public float getConfigurationOptionFloat(ConfigurationOption co)
    {
        return (Float)getConfigurationOption(co);
    }

    public String getConfigurationOptionString(ConfigurationOption co)
    {
        return (String)getConfigurationOption(co);
    }

    public Set<String> getConfigurationOptionStringSet(ConfigurationOption co)
    {
        return (Set<String>)getConfigurationOption(co);
    }

    public void setConfigurationOption(ConfigurationOption co, Object value)
    {
        if (co == null)
            return;

        SharedPreferences.Editor ed = mSharedPreferences.edit();

        if (value instanceof Boolean)
            ed.putBoolean(co.name(), (Boolean)value);
        else if (value instanceof Integer)
            ed.putInt(co.name(), (Integer)value);
        else if (value instanceof Long)
            ed.putLong(co.name(), (Long)value);
        else if (value instanceof Float)
            ed.putFloat(co.name(), (Float)value);
        else if (value instanceof String)
            ed.putString(co.name(), (String)value);
        else if (value instanceof Set)  // Lets hope it's a string set...  Should we check?
            ed.putStringSet(co.name(), (Set<String>)value);
        else
        {
            //TODO:  Assert?  Something went wrong if we didn't get any supported type
            return;
        }

        ed.commit();
    }

}
