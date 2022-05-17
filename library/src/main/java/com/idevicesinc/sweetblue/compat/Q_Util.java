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

package com.idevicesinc.sweetblue.compat;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;


@TargetApi(Build.VERSION_CODES.Q)
public class Q_Util
{
    public static boolean shouldShowRequestPermissionRationale(Activity context) {
        return context.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    public static void requestPermissions(Activity context, int requestCode, boolean requestBackgroundOperation) {
        String[] permissions;
        if (requestBackgroundOperation)
            permissions = new String[] { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION };
        else
            permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
        context.requestPermissions(permissions, requestCode);
    }
}
