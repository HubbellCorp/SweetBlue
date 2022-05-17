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


import android.app.Activity;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import com.idevicesinc.sweetblue.internal.P_StringHandler;


public class UIUtil
{

    private UIUtil() {}


    public static final String TEXT_ALLOW = "Allow";
    public static final String TEXT_DENY = "Deny";
    public static final String TEXT_ALLOW_UPPER = "ALLOW";
    public static final String TEXT_DENY_UPPER = "DENY";
    public static final String TEXT_YES = "Yes";
    public static final String TEXT_YES_UPPER = "YES";
    public static final String TEXT_ACCESS_DEVICE_LOCATION = "access this device's location";
    public static final String TEXT_BLUETOOTH = "Bluetooth";
    public static final String TEXT_BLUETOOTH_LOWER = "bluetooth";
    public static final String TEXT_ALLOW_ALL_TIME = "Allow all the time";
    public static final String TEXT_ALLOW_ONLY_APP = "Allow only while using the app";
    public static final String TEXT_BLUETOOTH_PERMISSION = "Bluetooth permission request";



    public static boolean turnOnPermissionDialogShowing(UiDevice device)
    {
        UiObject allowButton = device.findObject(new UiSelector().text(TEXT_ALLOW));
        return allowButton.exists();
    }

    public static void allowPermission(UiDevice device) throws UiObjectNotFoundException
    {
        UiObject allow = device.findObject(new UiSelector().text(TEXT_ALLOW));
        allow.click();
    }

    public static boolean viewExistsExact(UiDevice device, String messageText) throws UiObjectNotFoundException
    {
        UiObject view = device.findObject(new UiSelector().text(messageText));
        return view.exists();
    }

    public static boolean viewExistsContains(UiDevice device, String textToMatch) throws UiObjectNotFoundException
    {
        UiObject view = device.findObject(new UiSelector().textContains(textToMatch));
        return view.exists();
    }

    public static boolean viewExistsContains(UiDevice device, String... textToMatch) throws UiObjectNotFoundException
    {
        if (textToMatch == null || textToMatch.length < 1)
        {
            return false;
        }
        UiObject view;
        boolean contains = false;
        for (int i = 0; i < textToMatch.length; i++)
        {
            view = device.findObject(new UiSelector().textContains(textToMatch[i]));
            contains = view.exists();
            if (contains) return true;
        }
        return false;
    }

    /**
     * Clicks a button with the given text. This will try to find the widget exactly as the text is given, and
     * will try upper casing the text if it is not found.
     */
    public static void clickButton(UiDevice device, String buttonText) throws UiObjectNotFoundException
    {
        UiObject accept = device.findObject(new UiSelector().text(buttonText));
        if (accept.exists())
        {
            accept.click();
            return;
        }
        accept = device.findObject(new UiSelector().text(buttonText.toUpperCase()));
        accept.click();
    }

    public static void denyPermission(UiDevice device) throws UiObjectNotFoundException
    {
        UiObject deny = device.findObject(new UiSelector().text(TEXT_DENY));
        deny.click();
    }

    public static void handleBluetoothEnablerDialogs(Activity activity)
    {
        handleBluetoothEnablerDialogs(UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()), activity);
    }

    public static void handleBluetoothEnablerDialogs(UiDevice uiDevice, Activity activity)
    {
        try
        {
            if (viewExistsExact(uiDevice, TEXT_BLUETOOTH_PERMISSION))
            {
                if (viewExistsExact(uiDevice, TEXT_ALLOW_UPPER))
                {
                    clickButton(uiDevice, TEXT_ALLOW_UPPER);
                }
                else if (viewExistsExact(uiDevice, TEXT_YES))
                {
                    clickButton(uiDevice, TEXT_YES);
                }
            }
            if (viewExistsExact(uiDevice, P_StringHandler.getString(activity, P_StringHandler.REQUIRES_LOCATION_PERMISSION)))
            {
                clickButton(uiDevice, P_StringHandler.getString(activity, P_StringHandler.ACCEPT));
            }
            if (viewExistsExact(uiDevice, P_StringHandler.getString(activity, P_StringHandler.REQUIRES_LOCATION_PERMISSION_AND_SERVICES)))
            {
                clickButton(uiDevice, P_StringHandler.getString(activity, P_StringHandler.ACCEPT));
            }
            if (viewExistsExact(uiDevice, P_StringHandler.getString(activity, P_StringHandler.LOCATION_SERVICES_NEEDS_ENABLING)))
            {
                clickButton(uiDevice, P_StringHandler.getString(activity, P_StringHandler.ACCEPT));
            }
            if (viewExistsContains(uiDevice, TEXT_ALLOW, TEXT_ACCESS_DEVICE_LOCATION) && UIUtil.viewExistsExact(uiDevice, TEXT_ALLOW_UPPER) && UIUtil.viewExistsExact(uiDevice, TEXT_DENY_UPPER))
            {
                clickButton(uiDevice, TEXT_ALLOW_UPPER);
            }
            if (viewExistsContains(uiDevice, TEXT_ALLOW, TEXT_ACCESS_DEVICE_LOCATION) && UIUtil.viewExistsExact(uiDevice, TEXT_ALLOW_ALL_TIME) && UIUtil.viewExistsExact(uiDevice, TEXT_DENY))
            {
                clickButton(uiDevice, TEXT_ALLOW_ALL_TIME);
            }
            else if (viewExistsContains(uiDevice, TEXT_ALLOW, TEXT_ACCESS_DEVICE_LOCATION) && UIUtil.viewExistsExact(uiDevice, TEXT_ALLOW_ONLY_APP) && UIUtil.viewExistsExact(uiDevice, TEXT_DENY))
            {
                clickButton(uiDevice, TEXT_ALLOW_ONLY_APP);
            }

            if (viewExistsContains(uiDevice, TEXT_BLUETOOTH) || viewExistsContains(uiDevice, TEXT_BLUETOOTH_LOWER))
            {
                if (viewExistsExact(uiDevice, TEXT_YES))
                    clickButton(uiDevice, TEXT_YES);
                else if (viewExistsExact(uiDevice, TEXT_YES_UPPER))
                    clickButton(uiDevice, TEXT_YES_UPPER);
                else if (viewExistsExact(uiDevice, TEXT_ALLOW))
                    clickButton(uiDevice, TEXT_ALLOW);
                else if (viewExistsExact(uiDevice, TEXT_ALLOW_UPPER))
                    clickButton(uiDevice, TEXT_ALLOW_UPPER);
            }

        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

}
