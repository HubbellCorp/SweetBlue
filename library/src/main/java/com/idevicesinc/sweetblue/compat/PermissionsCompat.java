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

import android.app.Activity;
import com.idevicesinc.sweetblue.utils.Utils;

public final class PermissionsCompat
{
    private PermissionsCompat() {}

    /**
     * Safely wraps {@link Activity#shouldShowRequestPermissionRationale(String)} behind API level checks
     * Android 8 - 9 use {@link android.Manifest.permission#ACCESS_COARSE_LOCATION}, whereas
     * Android 10 and above use {@link android.Manifest.permission#ACCESS_FINE_LOCATION}
     */
    public static boolean shouldShowRequestPermissionRationale(Activity context) {
        if (Utils.isAndroid10())
            return Q_Util.shouldShowRequestPermissionRationale(context);
        else if (Utils.isMarshmallow())
            return M_Util.shouldShowRequestPermissionRationale(context);
        else
            return true;
    }

    /**
     * Safely wraps {@link Activity#requestPermissions(String[], int)} behind API level checks
     * Android 8 - 9 use {@link android.Manifest.permission#ACCESS_COARSE_LOCATION}, whereas
     * Android 10 and above use {@link android.Manifest.permission#ACCESS_FINE_LOCATION}. The
     * requestBackgroundOperation only applies to Android 10+, and is required if you want to get
     * scan results when the app is in the background.
     */
    public static void requestPermissions(Activity context, int requestCode, boolean requestBackgroundOperation) {
        if (Utils.isAndroid10())
            Q_Util.requestPermissions(context, requestCode, requestBackgroundOperation);
        else if (Utils.isMarshmallow())
            M_Util.requestPermissions(context, requestCode);
    }
}
