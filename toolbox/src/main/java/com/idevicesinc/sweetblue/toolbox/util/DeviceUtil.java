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
import android.os.Build;
import android.text.TextUtils;
import com.idevicesinc.sweetblue.toolbox.R;
import com.jaredrummler.android.device.DeviceName;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;



public class DeviceUtil
{

    private DeviceUtil() {}


    public static IDeviceUtil m_deviceUtilImpl = new DeviceUtilImpl();


    public static String getKernelVersion(Context context)
    {
        return m_deviceUtilImpl.getKernelVersion(context);
    }

    public static void getDeviceName(Context context, String model, String unknownName, DeviceNameCallback callback)
    {
        m_deviceUtilImpl.getDeviceName(context, model, unknownName, callback);
    }


    public interface IDeviceUtil
    {
        String getKernelVersion(Context context);
        void getDeviceName(Context context, String model, String unknownName, DeviceNameCallback callback);
    }

    public interface DeviceNameCallback
    {
        void onReturn(String name);
    }


    public static final class DeviceUtilImpl implements IDeviceUtil
    {

        @Override
        public String getKernelVersion(Context context)
        {
            try
            {
                Process p = Runtime.getRuntime().exec("uname -r");
                InputStream is = null;
                if (p.waitFor() == 0)
                {
                    is = p.getInputStream();
                }
                else
                {
                    is = p.getErrorStream();
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String line = reader.readLine();
                reader.close();
                return line;
            }
            catch (Exception e)
            {
                String backup = System.getProperty("os.version");
                return TextUtils.isEmpty(backup) ? context.getString(R.string.unknown) : backup;
            }
        }

        @Override
        public void getDeviceName(Context context, String model, String unknownName, final DeviceNameCallback callback)
        {
            String name = DeviceName.getDeviceName(Build.MODEL, unknownName);
            if (callback != null)
                callback.onReturn(name);

            if (name.equals(unknownName))
            {
                DeviceName.with(context).request(new DeviceName.Callback()
                {
                    @Override
                    public void onFinished(DeviceName.DeviceInfo info, Exception error)
                    {
                        if (callback != null)
                            callback.onReturn(info.getName());
                    }
                });
            }
        }
    }

}
