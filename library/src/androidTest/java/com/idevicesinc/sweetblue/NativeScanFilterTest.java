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

package com.idevicesinc.sweetblue;


import android.os.Build;
import android.os.ParcelUuid;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import android.util.Log;
import com.idevicesinc.sweetblue.defaults.DefaultScanFilter;
import com.idevicesinc.sweetblue.utils.NativeScanFilter;
import com.idevicesinc.sweetblue.utils.Utils;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.ArrayList;
import java.util.UUID;


@RunWith(AndroidJUnit4.class)
public class NativeScanFilterTest extends BaseTester
{


    @Test(timeout = 15000L)
    public void nativeScanFilterTest() throws Exception
    {
        BleManagerConfig config = new BleManagerConfig();
        ArrayList<NativeScanFilter> list = new ArrayList<>();
        NativeScanFilter.Builder b = new NativeScanFilter.Builder();
        b.setServiceUuid(ParcelUuid.fromString("24e5ff1d-93da-4f25-8498-e4e2da2f01a6"));
        list.add(b.build());
        config.defaultNativeScanFilterList = list;

        mgr.setConfig(config);

        mgr.setListener_Discovery((e) ->
        {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                assertTrue("Found a device that doesn't have our UUID!", e.device().getScanInfo().getServiceData().containsKey(UUID.fromString("0000ff1d-0000-1000-8000-00805f9b34fb")));
                mgr.stopScan();
                succeed();
            }
        });

        if (!Utils.isLollipop())
        {
            // If we're not on lollipop, we'll just bail out here
            Log.e("NativeScanFilterTest", "Bailing out of test as this phone isn't running lollipop or higher!");
            System.out.println("NativeScanFilterTest -- Bailing out of test as this phone isn't running lollipop or higher!");
            return;
        }

        if (Build.MANUFACTURER.contains("sony") || Build.MANUFACTURER.contains("Sony"))
        {
            mgr.reset(event ->
            {
                if (event.progress() == ResetListener.Progress.COMPLETED)
                    mgr.startScan();
            });
        }
        else
            mgr.startScan();

        startAsyncTest();
    }

    @Test(timeout = 15000L)
    public void nativeAndSweetBlueFilterTest() throws Exception
    {
        BleManagerConfig config = new BleManagerConfig();
        ArrayList<NativeScanFilter> list = new ArrayList<>();
        NativeScanFilter.Builder b = new NativeScanFilter.Builder();
        b.setServiceUuid(ParcelUuid.fromString("24e5ff1d-93da-4f25-8498-e4e2da2f01a6"));
        list.add(b.build());
        config.defaultNativeScanFilterList = list;
        config.defaultScanFilter = new DefaultScanFilter("wall");

        mgr.setConfig(config);

        mgr.setListener_Discovery((e) ->
        {
            if (e.was(DiscoveryListener.LifeCycle.DISCOVERED))
            {
                assertTrue("Found a device that doesn't have our UUID!", e.device().getScanInfo().getServiceData().containsKey(UUID.fromString("0000ff1d-0000-1000-8000-00805f9b34fb")));
                assertTrue("Device found doesn't match requested name filter!", e.device().getName_normalized().contains("wall"));
                mgr.stopScan();
                succeed();
            }
        });

        if (!Utils.isLollipop())
        {
            // If we're not on lollipop, we'll just bail out here
            Log.e("NativeAndSweetBlueFilterTest", "Bailing out of test as this phone isn't running lollipop or higher!");
            System.out.println("NativeAndSweetBlueFilterTest -- Bailing out of test as this phone isn't running lollipop or higher!");
            return;
        }

        if (Build.MANUFACTURER.contains("sony") || Build.MANUFACTURER.contains("Sony"))
        {
            mgr.reset(event ->
            {
                if (event.progress() == ResetListener.Progress.COMPLETED)
                    mgr.startScan();
            });
        }
        else
            mgr.startScan();

        startAsyncTest();
    }


}
