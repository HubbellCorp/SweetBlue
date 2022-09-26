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

package com.idevicesinc.sweetblue.toolbox.viewmodel;

import android.app.Activity;
import android.content.Context;
import android.os.Looper;
import android.text.TextUtils;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.BleManagerState;
import com.idevicesinc.sweetblue.BleScanApi;
import com.idevicesinc.sweetblue.DiscoveryListener;
import com.idevicesinc.sweetblue.ScanFilter;
import com.idevicesinc.sweetblue.ScanOptions;
import com.idevicesinc.sweetblue.toolbox.R;
import com.idevicesinc.sweetblue.toolbox.util.AppConfig;
import com.idevicesinc.sweetblue.toolbox.util.BleHelper;
import com.idevicesinc.sweetblue.toolbox.util.MutablePostLiveData;
import com.idevicesinc.sweetblue.toolbox.util.UpdateManager;
import com.idevicesinc.sweetblue.utils.Utils;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import android.os.Handler;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;


public class DashboardViewModel extends ViewModel
{
    public enum SortBy
    {
        Unsorted(R.string.unsorted),
        Name(R.string.name),
        SignalStrength(R.string.signal_strength);

        int mStringResId;

        SortBy(int stringResId)
        {
            mStringResId = stringResId;
        }

        public int getStringResId()
        {
            return mStringResId;
        }

        public static SortBy fromString(Context c, String s)
        {
            if (c == null)
                return null;

            for (SortBy sb : values())
            {
                String compare = c.getString(sb.getStringResId());
                if (compare != null && compare.equals(s))
                    return sb;
            }
            return null;
        }

    };

    private BleManager m_manager;
    private ArrayList<BleDevice> m_deviceList;
    private MutablePostLiveData<ArrayList<BleDevice>> m_displayList;

    private MutablePostLiveData<Integer> m_scanImageRes;
    private MutablePostLiveData<Integer> m_scanTextRes;

    private RssiComparator rssiComparator = new RssiComparator();
    private NameComparator nameComparator = new NameComparator();

    private Comparator<BleDevice> m_currentComparator = null;
    private NameScanFilter m_nameScanFilter = new NameScanFilter("");
    private MutablePostLiveData<String> m_queryString;
    private MutablePostLiveData<Boolean> m_isScanning;


    private Handler m_handler;


    public DashboardViewModel()
    {
        m_deviceList = new ArrayList<>();
        m_displayList = new MutablePostLiveData<>();
        m_displayList.setValue(new ArrayList<>());
        m_scanImageRes = new MutablePostLiveData<>();
        m_scanImageRes.setValue(R.drawable.icon_alert);
        m_scanTextRes = new MutablePostLiveData<>();
        m_scanTextRes.setValue(R.string.not_ready_to_scan);
        m_queryString = new MutablePostLiveData<>();
        m_queryString.setValue("");
        m_isScanning = new MutablePostLiveData<>();
        m_handler = new Handler(Looper.getMainLooper());
    }

    public void init(Activity context)
    {
        AppConfig ac = AppConfig.getInstance();
        BleManagerConfig config = BleHelper.get().getInitialConfig();
        String configJSON = null;
        try
        {
            // Attempt to read in the saved config as JSON and toss it into the manager
            configJSON = ac.getConfigurationOption(AppConfig.ConfigurationOption.BleConfigJSON);
            if (configJSON != null)
            {
                JSONObject jo = new JSONObject(configJSON);
                if (jo != null)
                    config.readJSON(jo);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            // See if we have a scan API set
            String scanAPIString = ac.getConfigurationOption(AppConfig.ConfigurationOption.ScanApi);
            BleScanApi scanApi = scanAPIString != null ? BleScanApi.valueOf(scanAPIString) : null;
            if (scanApi != null)
                config.scanApi = scanApi;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            // See if we have a sort option
            String sortOptionString = ac.getConfigurationOptionString(AppConfig.ConfigurationOption.SortOption);
            SortBy sortBy = sortOptionString != null ? SortBy.valueOf(sortOptionString) : null;
            if (sortBy != null)
                setComparatorFromSortBy(sortBy);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            // Read in the scan filter
            String scanFilter = ac.getConfigurationOptionString(AppConfig.ConfigurationOption.ScanFilter);
            if (scanFilter != null)
                updateList(scanFilter);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        // Override scan API if we have one saved

        config.defaultScanFilter = m_nameScanFilter;

        m_manager = BleHelper.get().getMgr(config, context);

        m_manager.setListener_Discovery(new DeviceDiscovery());

        m_manager.setListener_State(e ->
        {
            if (e.didEnter(BleManagerState.SCANNING))
            {
                m_scanImageRes.setValue(R.drawable.icon_cancel);
            }
            else if (e.didExit(BleManagerState.SCANNING))
            {
                if (!m_manager.isScanning())
                {
                    m_scanImageRes.setValue(R.drawable.icon_scan);
                    m_scanTextRes.setValue(R.string.start_scan);
                }
            }
            else if (e.didEnter(BleManagerState.OFF))
            {
                m_scanImageRes.setValue(R.drawable.icon_alert);
                m_scanTextRes.setValue(R.string.not_ready_to_scan);
            }
            else if (e.didEnter(BleManagerState.ON))
            {
                if (m_manager.isScanningReady())
                {
                    m_scanImageRes.setValue(R.drawable.icon_scan);
                    m_scanTextRes.setValue(R.string.start_scan);
                }
            }
        });

    }

    public void enablerDone()
    {
        if (m_manager.isScanningReady())
        {
            // Not sure why yet, but we have to post the value here, otherwise the observer listener doesn't get called.
            m_scanTextRes.postValue(R.string.start_scan);
            m_scanImageRes.postValue(R.drawable.icon_scan);
        }
    }

    /**
     * This will only trigger the observable listeners if there is anything other than an empty string for the query
     */
    public void requestQueryString()
    {
        if (!TextUtils.isEmpty(m_nameScanFilter.query))
        {
            m_queryString.setValue(m_queryString.getValue());
        }
    }

    public int getCurrentComparatorIndex()
    {
        if (m_currentComparator == null)
            return 0;
        else if (m_currentComparator instanceof NameComparator)
            return 1;
        else
            return 2;
    }

    public void updateComparator(Context context, String choice)
    {
        SortBy sb = SortBy.fromString(context, choice);
        setComparatorFromSortBy(sb);

        // Apply the sort
        if (m_currentComparator != null)
        {
            updateDisplayList();
        }

        // Update shared preference
        try
        {
            AppConfig.getInstance().setConfigurationOption(AppConfig.ConfigurationOption.SortOption, sb.name());
        }
        catch (Exception e)
        {
            // Oops?  Nothing we can do here, just don't save
        }
    }

    private void setComparatorFromSortBy(SortBy sb)
    {
        if (sb == null)  // Default to signal strength
            sb = SortBy.SignalStrength;

        switch (sb)
        {
            case Unsorted:
                m_currentComparator = null;
                break;

            case Name:
                m_currentComparator = nameComparator;
                break;

            case SignalStrength:
                m_currentComparator = rssiComparator;
                break;
        }
    }

    public void updateScanApi(Context context, String choice)
    {
        updateScanApi(context, choice, true);
    }

    public void updateScanApi(Context context, String choice, boolean fromUser)
    {
        BleManagerConfig cfg = m_manager.getConfigClone();
        BleScanApi api;
        if (choice.equals(context.getString(R.string.scan_type_classic)))
        {
            api = BleScanApi.CLASSIC;
        }
        else if (choice.equals(context.getString(R.string.scan_type_pre_lollipop)))
        {
            api = BleScanApi.PRE_LOLLIPOP;
        }
        else if (choice.equals(context.getString(R.string.scan_type_post_lollipop)))
        {
            api = BleScanApi.POST_LOLLIPOP;
        }
        else
        {
            api = BleScanApi.AUTO;
        }

        cfg.scanApi = api;

        m_manager.setConfig(cfg);

        // Update shared preference
        try
        {
            AppConfig.getInstance().setConfigurationOption(AppConfig.ConfigurationOption.ScanApi, api.name());
        }
        catch (Exception e)
        {
            // Oops?  Nothing we can do here, just don't save
        }
    }

    public BleScanApi getCurrentScanApi()
    {
        return m_manager.getConfigClone().scanApi;
    }

    public MutableLiveData<String> getQueryString()
    {
        return m_queryString;
    }

    public MutableLiveData<ArrayList<BleDevice>> getDisplayList()
    {
        return m_displayList;
    }

    public MutableLiveData<Integer> getScanImageRes()
    {
        return m_scanImageRes;
    }

    public MutableLiveData<Integer> getScanTextRes()
    {
        return m_scanTextRes;
    }

    public MutableLiveData<Boolean> getIsScanning() {
        return m_isScanning;
    }

    public boolean requestScanViewsUpdate()
    {
        if (m_manager.isScanning())
        {
            m_manager.stopScan();
            updateDisplayList();
            m_scanTextRes.setValue(R.string.start_scan);
            m_scanImageRes.setValue(R.drawable.icon_scan);
            m_isScanning.setValue(false);
            return true;
        }
        else if (m_manager.isScanningReady())
        {
            ScanOptions options = new ScanOptions();
            options.withMatchMode(ScanOptions.MatchMode.STICKY);
            options.scanInfinitely();
            m_manager.startScan(options);
            m_scanTextRes.setValue(R.string.scanning);
            m_scanImageRes.setValue(R.drawable.icon_cancel);
            m_isScanning.setValue(true);
            return true;
        }
        else
        {
            return false;
        }
    }

    public void shutdownBle()
    {
        m_manager.shutdown();
    }


    public void updateList(String query)
    {
        boolean changed = !m_nameScanFilter.query.equals(query);

        if (!changed)
            return;

        boolean shorter = query.length() < m_nameScanFilter.query.length();
        m_nameScanFilter.query = query;
        m_queryString.setValue(query);

        String lcQuery = query != null ? query.toLowerCase(Locale.US) : null;

        if (changed && !shorter)
        {
            int size = m_deviceList.size();
            Iterator<BleDevice> it = m_displayList.getValue().iterator();
            while (it.hasNext())
            {
                final BleDevice device = it.next();
                if (!device.getName_native().toLowerCase(Locale.US).contains(lcQuery))
                {
                    it.remove();
                }
            }
            if (m_displayList.getValue().size() != size)
            {
                updateDisplayList();
            }
        }
        else
        {
            // Iterate through main device list to see if any new devices match the new query string
            boolean anyAdded = false;
            for (BleDevice device : m_deviceList)
            {
                if (device.getName_native().toLowerCase(Locale.US).contains(lcQuery) && !m_displayList.getValue().contains(device))
                {
                    anyAdded = true;
                    m_displayList.getValue().add(device);
                }
            }
            if (anyAdded)
            {
                updateDisplayList();
            }
        }

        // Save the query string
        if (query != null && query.length() < 1)
            query = null;
        AppConfig appConfig = AppConfig.getInstance();
        if (appConfig != null)
            appConfig.setConfigurationOption(AppConfig.ConfigurationOption.ScanFilter, query);
    }

    private void updateDisplayList()
    {
        if (m_currentComparator != null)
            Collections.sort(m_displayList.getValue(), m_currentComparator);
        m_displayList.setValue(m_displayList.getValue());
    }

    public void removeRow(int rowPosition)
    {
        ArrayList<BleDevice> list = m_displayList.getValue();
        if (list == null || list.isEmpty())
        {
            return;
        }

        if (rowPosition == -1 || rowPosition > list.size())
        {
            return;
        }

        // remove selected bluetooth device
        BleDevice selectedDevice = list.get(rowPosition);
        m_manager.removeDeviceFromCache(selectedDevice);

        m_displayList.getValue().remove(selectedDevice);
        updateDisplayList();
    }

    private final class DeviceDiscovery implements DiscoveryListener, UpdateManager.UpdateListener
    {
        ConcurrentHashMap<String, DiscoveryEvent> m_rediscoverMap = new ConcurrentHashMap<>();

        DeviceDiscovery()
        {
            // Register for updates every 3 seconds
            UpdateManager.getInstance().subscribe(this, 3.0);
        }

        @Override
        public void onEvent(final DiscoveryEvent de)
        {
            if (!Utils.isOnMainThread())
            {
                m_handler.post(() -> handleProcessEvent(de));
            }
            else
                handleProcessEvent(de);
        }

        private void handleProcessEvent(DiscoveryEvent de)
        {
            if (!processEvent(de, false))
                m_rediscoverMap.put(de.macAddress(), de);
        }

        @Override
        public void onUpdate()
        {
            if (m_rediscoverMap.size() > 0 && m_manager.isScanning())
            {
                m_rediscoverMap.clear();
                updateDisplayList();
            }
        }

        boolean processEvent(final DiscoveryEvent de, boolean processRediscovers)
        {
            if (de.was(LifeCycle.DISCOVERED))
            {
                m_deviceList.add(de.device());
                m_displayList.getValue().add(de.device());
                m_displayList.setValue(m_displayList.getValue());
            }
            else if (de.was(LifeCycle.REDISCOVERED))
            {
                if (!processRediscovers)
                    return false;

                // If the device was rediscovered, then we have an updated rssi value, so inform the adapter that the data has changed
                // for this device
                int index = m_displayList.getValue().indexOf(de.device());
                if (index != -1 && m_currentComparator != null)
                {
                    updateDisplayList();
                }
            }
            else if (de.was(DiscoveryListener.LifeCycle.UNDISCOVERED))
            {
                m_deviceList.remove(de.device());
                m_displayList.getValue().remove(de.device());
                m_displayList.setValue(m_displayList.getValue());
            }

            // True because we processed the event
            return true;
        }
    }

    private final class NameScanFilter implements ScanFilter
    {

        private String query;

        public NameScanFilter(String query)
        {
            this.query = query.toLowerCase(Locale.US);
        }

        @Override
        public Please onEvent(ScanEvent e)
        {
            return Please.acknowledgeIf(e.name_native().toLowerCase(Locale.US).contains(query));
        }
    }

    private final static class RssiComparator implements Comparator<BleDevice>
    {
        @Override
        public int compare(BleDevice o1, BleDevice o2)
        {
            return o2.getRssi() - o1.getRssi();
        }
    }

    private final static class NameComparator implements Comparator<BleDevice>
    {
        @Override
        public int compare(BleDevice o1, BleDevice o2)
        {
            return o1.getName_normalized().compareTo(o2.getName_normalized());
        }
    }

}
