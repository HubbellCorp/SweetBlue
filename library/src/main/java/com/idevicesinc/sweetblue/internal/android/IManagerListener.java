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
import com.idevicesinc.sweetblue.compat.L_Util;
import java.util.List;


public interface IManagerListener
{

    BluetoothAdapter.LeScanCallback getPreLollipopCallback();
    L_Util.ScanCallback getPostLollipopCallback();


    interface Callback
    {
        void onScanResult(int callbackType, L_Util.ScanResult result);
        void onBatchScanResult(int callbackType, List<L_Util.ScanResult> results);
        void onScanFailed(int errorCode);
    }

    interface Factory
    {
        IManagerListener newInstance(Callback callback);
    }

    Factory DEFAULT_FACTORY = new DefaultFactory();
    int SCAN_CALLBACK_TYPE_PRE_LOLLIPOP = -1;
    int SCAN_CALLBACK_TYPE_POST_LOLLIPOP = -2;

    class DefaultFactory implements Factory
    {
        @Override
        public IManagerListener newInstance(Callback callback)
        {
            return new ManagerListenerImpl(callback);
        }
    }

}
