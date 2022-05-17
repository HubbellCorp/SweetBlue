package com.idevicesinc.sweetblue.toolbox.viewmodel;


import android.content.Context;
import android.net.Uri;

import androidx.lifecycle.ViewModel;

import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.toolbox.util.AppConfig;
import com.idevicesinc.sweetblue.toolbox.util.BleHelper;
import com.idevicesinc.sweetblue.toolbox.util.ExportResult;
import com.idevicesinc.sweetblue.toolbox.util.FileUtil;
import com.idevicesinc.sweetblue.utils.P_JSONUtil;
import org.json.JSONObject;


public class SettingsViewModel extends ViewModel
{

    private Object mSettingsObject = null;
    private BleManager m_manager;



    public void init(Context context)
    {
        if (m_manager == null)
        {
            m_manager = BleHelper.get().getMgr(context);
            mSettingsObject = m_manager.getConfigClone();
        }
    }

    public Object getSettingsObject()
    {
        return mSettingsObject;
    }

    public void resetSettings()
    {
        mSettingsObject = BleHelper.get().getInitialConfig();
    }

    public void importSettings(JSONObject json)
    {
        mSettingsObject = new BleManagerConfig(json);
    }

    public ExportResult exportSettingsToDisk()
    {
        String filenameFormat = "sweetblue%s.json";
        return FileUtil.writeUniqueFile(filenameFormat, getSettingsJSON());
    }

    public String getSettingsJSON()
    {
        // Update the ble manager config
        BleManagerConfig cfg = (BleManagerConfig) mSettingsObject;

        // Build a JSON object of the differences in the config from a standard config
        BleManagerConfig baseConfig = new BleManagerConfig();

        JSONObject jo1 = baseConfig.writeJSON();
        JSONObject jo2 = cfg.writeJSON();

        try
        {
            //TODO:  Find a way to move this into the library (the diff)
            JSONObject diff = P_JSONUtil.shallowDiffJSONObjects(jo1, jo2);
            String diffString = diff.toString();
            return diffString;
        }
        catch (Exception e)
        {

        }

        return null;
    }

    public boolean readAndImportFile(Context context, Uri selectedfile)
    {
        try
        {
            JSONObject jo = FileUtil.readJSONFromUri(context, selectedfile);
            importSettings(jo);
        }
        catch (Exception e)
        {
            return false;
        }
        return true;
    }

    public  void saveAndAppySettings()
    {
        // Apply to the BLE manager
        m_manager.setConfig((BleManagerConfig)getSettingsObject());

        try
        {
            AppConfig appConfig = AppConfig.getInstance();
            appConfig.setConfigurationOption(AppConfig.ConfigurationOption.BleConfigJSON, getSettingsJSON());
        }
        catch (Exception e)
        {

        }
    }
}
