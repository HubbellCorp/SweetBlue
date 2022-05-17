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

package com.idevicesinc.sweetblue.internal.android;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import com.idevicesinc.sweetblue.compat.L_Util;
import java.util.List;


final class ManagerListenerImpl implements IManagerListener, BluetoothAdapter.LeScanCallback, L_Util.ScanCallback
{


    private final Callback m_callback;


    ManagerListenerImpl(Callback callback)
    {
        m_callback = callback;
    }


    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord)
    {
        if (m_callback != null)
        {
            L_Util.ScanResult result = new L_Util.ScanResult(P_DeviceHolder.newHolder(device), rssi, scanRecord);
            m_callback.onScanResult(SCAN_CALLBACK_TYPE_PRE_LOLLIPOP, result);
        }
    }

    @Override
    public final BluetoothAdapter.LeScanCallback getPreLollipopCallback()
    {
        return this;
    }

    @Override
    public final L_Util.ScanCallback getPostLollipopCallback()
    {
        return this;
    }

    @Override
    public final void onScanResult(int callbackType, L_Util.ScanResult result)
    {
        if (m_callback != null)
            m_callback.onScanResult(callbackType, result);
    }

    @Override
    public final void onBatchScanResults(List<L_Util.ScanResult> results)
    {
        if (m_callback != null)
            m_callback.onBatchScanResult(SCAN_CALLBACK_TYPE_POST_LOLLIPOP, results);
    }

    @Override
    public final void onScanFailed(int errorCode)
    {
        if (m_callback != null)
            m_callback.onScanFailed(errorCode);
    }
}
