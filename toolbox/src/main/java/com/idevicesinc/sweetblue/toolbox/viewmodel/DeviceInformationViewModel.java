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


import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.toolbox.R;
import com.idevicesinc.sweetblue.toolbox.util.BleHelper;
import com.idevicesinc.sweetblue.toolbox.util.DeviceUtil;
import com.idevicesinc.sweetblue.toolbox.util.MutablePostLiveData;


public class DeviceInformationViewModel extends ViewModel
{

    private BleManager m_manager;

    private String kernelVersion;
    private MutablePostLiveData<String> deviceName = new MutablePostLiveData<>();
    private String bluetoothName;
    private String androidVersion;
    private String apiVersion;
    private String brand;
    private String product;
    private String model;
    private String manufacturer;
    private String board;
    private boolean bleSupported;
    private boolean lollipopSupported;
    private boolean scanBatchSupported;
    private boolean multiAdvSupported;


    public void init(Context context)
    {
        if (m_manager == null)
        {
            m_manager = BleHelper.get().getMgr();
            getCacheValues(context);
        }
    }

    public String getKernelVersion()
    {
        return kernelVersion;
    }

    public MutableLiveData<String> getDeviceName()
    {
        return deviceName;
    }

    public String getBluetoothName()
    {
        return bluetoothName;
    }

    public String getAndroidVersion()
    {
        return androidVersion;
    }

    public String getApiVersion()
    {
        return apiVersion;
    }

    public String getBrand()
    {
        return brand;
    }

    public String getProduct()
    {
        return product;
    }

    public String getModel()
    {
        return model;
    }

    public String getManufacturer()
    {
        return manufacturer;
    }

    public String getBoard()
    {
        return board;
    }

    public boolean isBleSupported()
    {
        return bleSupported;
    }

    public boolean isLollipopSupported()
    {
        return lollipopSupported;
    }

    public boolean isScanBatchSupported()
    {
        return scanBatchSupported;
    }

    public boolean isMultiAdvSupported()
    {
        return multiAdvSupported;
    }

    @SuppressLint("MissingPermission")
    private void getCacheValues(Context context)
    {
        kernelVersion = DeviceUtil.getKernelVersion(context);
        final String unknown = context.getString(R.string.unknown_device);
        DeviceUtil.getDeviceName(context, Build.MODEL, unknown, name -> deviceName.setValue(name));
        bluetoothName = m_manager.getNativeAdapter().getName();
        androidVersion = Build.VERSION.RELEASE;
        apiVersion = String.valueOf(Build.VERSION.SDK_INT);
        brand = Build.BRAND;
        manufacturer = Build.MANUFACTURER;
        board = Build.BOARD;
        model = Build.MODEL;
        product = Build.PRODUCT;
        bleSupported = m_manager.isBleSupported();
        lollipopSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
        scanBatchSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && m_manager.getNativeAdapter().isOffloadedScanBatchingSupported();
        multiAdvSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && m_manager.getNativeAdapter().isMultipleAdvertisementSupported();
    }


}
