package com.idevicesinc.sweetblue.compat;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import java.util.ArrayList;
import java.util.List;


@TargetApi(Build.VERSION_CODES.S)
public class S_Util {

    private S_Util() {}


    public static void requestPermissions(Activity context, int requestCode, boolean requestBackgroundOperation, boolean requestAdvertise)
    {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.BLUETOOTH_SCAN);
        permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
        if (requestBackgroundOperation)
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        if (requestAdvertise)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE);
        context.requestPermissions(permissions.toArray(new String[0]), requestCode);
    }

}
