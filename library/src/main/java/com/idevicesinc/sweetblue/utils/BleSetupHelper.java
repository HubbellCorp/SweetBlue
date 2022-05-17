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

package com.idevicesinc.sweetblue.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.BleManagerState;
import com.idevicesinc.sweetblue.P_Bridge_User;
import com.idevicesinc.sweetblue.UhOhListener;
import com.idevicesinc.sweetblue.compat.S_Util;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to handle the new hairy logic for getting bluetooth low-energy scan results that is introduced with {@link android.os.Build.VERSION_CODES#M}.
 * With {@link android.os.Build.VERSION_CODES#M} you need to have {@link android.Manifest.permission#ACCESS_COARSE_LOCATION} or {@link android.Manifest.permission#ACCESS_FINE_LOCATION}
 * in your AndroidManifest.xml, and also enable them at runtime, AND also make sure location services are on.
 * <br><br>
 * See more information at <a target="_blank" href="https://github.com/iDevicesInc/SweetBlue/wiki/Android-BLE-Issues#android-m-issues">https://github.com/iDevicesInc/SweetBlue/wiki/Android-BLE-Issues#android-m-issues</a>
 * <br><br>
 * This class is simply a convenience that wraps various helper methods of {@link BleManager} (see the "See Also" section, which has enough links that it might give you
 * an idea of why {@link BleSetupHelper} was written). As such you don't need to use it, but it comes in handy as a simple addition to most simple apps.
 *
 * @see BleManager#isLocationEnabledForScanning()
 * @see BleManager#isLocationEnabledForScanning_byManifestPermissions()
 * @see BleManager#isLocationEnabledForScanning_byRuntimePermissions()
 * @see BleManager#isLocationEnabledForScanning_byOsServices()
 * @see BleManager#turnOnLocationWithIntent_forPermissions(Activity, int)
 * @see BleManager#turnOnLocationWithIntent_forOsServices(Activity, int)
 * @see BleManager#turnOnWithIntent(Activity, int)
 * @see BleManager#willLocationPermissionSystemDialogBeShown(Activity)
 * @see <a target="_blank" href="https://github.com/iDevicesInc/SweetBlue/wiki/Android-BLE-Issues#android-m-issues">https://github.com/iDevicesInc/SweetBlue/wiki/Android-BLE-Issues#android-m-issues</a>
 *
 */
public class BleSetupHelper
{
    /**
     * Enumerates the various permissions that we will try to enable (if needed)
     */
    public enum Permission
    {
        /**
         * Android 12+ permission, used for being able to scan for BLE devices. Note that SweetBlue also requests
         * {@link android.Manifest.permission#BLUETOOTH_CONNECT} in addition to {@link android.Manifest.permission#BLUETOOTH_SCAN}, as it
         * is needed to process the scan results. This will also request {@link android.Manifest.permission#BLUETOOTH_ADVERTISE},
         * if {@link com.idevicesinc.sweetblue.BleManagerConfig#requestAdvertisePermission} is set to <code>true</code>.
         * If you want to request these permissions separately, then do not use this class to handle
         * requesting permissions.
         * See <a href="https://developer.android.com/guide/topics/connectivity/bluetooth/permissions"></a> for more details on
         * permission handling for Android 12+
         */
        ANDROID_12_BLUETOOTH(false),

        /**
         * The Bluetooth permission...  This is always required in order to be able to do just about anything.
         */
        BLUETOOTH(false),

        /**
         * Used when checking and requesting location permissions from the user. This is not required unless the version is at least {@link android.os.Build.VERSION_CODES#M}.
         */
        LOCATION_PERMISSION(true),

        /**
         * Used when checking if the device needs Location services turned on and enabling Location services if they are disabled. This step isn't necessarily needed for overall
         * Bluetooth scanning. It is only needed for Bluetooth Low Energy scanning in {@link android.os.Build.VERSION_CODES#M}; otherwise, SweetBlue will default to classic scanning.
         */
        LOCATION_SERVICES(true),

        /**
         * Custom permission, this has no built in implementation.  If you want to use the enabler to handle enabling of other permissions, you can override some functionality
         * and use this enum value to indicate other permissions.  See {@link PermissionInstance}
         */
        CUSTOM(false);

        private final static int kRequestCodeBase = 7269;  // Arbitrary value to use for the request codes.

        private boolean mIsLocationRelated;

        Permission(boolean isLocationRelated)
        {
            mIsLocationRelated = isLocationRelated;
        }

        public static Permission[] getPermissions() {
            List<Permission> l = new ArrayList<>();
            for (Permission p : values()) {
                if (!Utils.isAndroid12() && p == ANDROID_12_BLUETOOTH)
                {
                    continue;
                }
                l.add(p);
            }
            return l.toArray(new Permission[0]);
        }

        public int getRequestCode()
        {
            return this.ordinal() + kRequestCodeBase;
        }

        public boolean getIsLocationRelated()
        {
            return mIsLocationRelated;
        }
    }

    /**
     * Enumerates the various errors that can happen.  Any time that the enabler isn't able to successfully obtain all required permissions, one of these codes
     * will be availble in the {@link Result}.  You can also check the error message in the {@link Result} for more information.
     */
    public enum ErrorCode
    {
        /**
         * This code indicates that the user declined to grant a required permission
         */
        USER_REJECTED("User refused to allow a permission"),

        /**
         * This code indicates that the {@link Activity} or {@link BleManager} instance that we were given became null, which prevented the enabler from finishing
         */
        LOST_ACTIVITY_OR_MANAGER("Lost reference to Activity or BleManager"),

        /**
         * This code indicates that the enabler was misused and had to shut down.  Check the error message for more details
         */
        CRITICAL_MISUSE("Critical misuse of enabler"),

        /**
         * This code indicates that the enabler malfunctioned in such a way that it could not continue normal operation.  If this happens, please file a bug report
         */
        INTERNAL_ERROR("Internal logic error detected"),

        /**
         * This code should never be used.  It only exists in case we somehow hit an error state w/o an error code being set already
         */
        UNKNOWN("Unknown error")
        ;

        String mDefaultErrorMessage;

        ErrorCode(String defaultErrorMessage)
        {
            mDefaultErrorMessage = defaultErrorMessage;
        }

        public String getDefaultErrorMessage()
        {
            return mDefaultErrorMessage;
        }
    }

    // Internal state enum, invisible to the user
    protected enum State
    {
        UNINITIALIZED,
        STARTED,
        SHOWING_UI_FOR_PERMISSION,
        REQUESTING_PERMISSION,
        FINISHING,  // Intermediate state where we wrap things up before moving to a final state
        FINISHED_SUCCESS(true),  // Final success state
        FINISHED_FAILED(true),  // Final failure state
        CRITICAL_ERROR(true);  // Final state we enter if some major error happens that stops us from finishing normally

        State()
        {
        }

        State(boolean isHalt)
        {
            mIsHalt = isHalt;
        }

        private boolean mIsHalt;

        // You can check in...  but you can never leave!
        public boolean getIsHalt()
        {
            return mIsHalt;
        }
    };

    /**
     * Enumerates the built in strings used by the enabler to convey information to users.  There are two different ways to alter these strings:
     * -Add string resources to project with the appropriate resource identifier (see the individual enum values for more information)
     * -Override the {@link BluetoothEnablerImpl#getString} method of the implementation class and return custom strings
     */
    public enum DefaultString
    {
        /**
         * Dialog message telling the user what the consequences of denying the location permission are.
         */
        DENYING_LOCATION_ACCESS("denying_location_access", "Denying location access means low-energy scanning will not work."),

        /**
         * Toast shown to users asking them to grant the location permission.  This appears over the system options
         */
        APP_NEEDS_PERMISSION("app_needs_permission", "App needs android.permission.ACCESS_COARSE_LOCATION or android.permission.ACCESS_FINE_LOCATION in its AndroidManifest.xml!"),

        /**
         * Toast shown to users asking them to grant the location permission.  This appears over the system options
         */
        LOCATION_PERMISSION_TOAST("location_permission_toast", "Please click the Permissions button, then enable Location, then press back twice."),

        /**
         * Dialog message telling user why the app needs location permission
         */
        REQUIRES_LOCATION_PERMISSION("requires_location_permission", "Android Marshmallow (6.0+) requires Location Permission to be able to scan for Bluetooth devices. Please accept to allow Location Permission."),

        /**
         * Dialog message telling user why the app needs location services permission, and location services to be enabled
         */
        REQUIRES_LOCATION_PERMISSION_AND_SERVICES("requires_location_permission_and_services",
                "Android Marshmallow (6.0+) requires Location Permission to the app to be able to scan for Bluetooth devices.\\n\" +\n" +
                        "                \"\\n\" + \"Marshmallow also requires Location Services to improve Bluetooth device discovery.  While it is not required for use in this app, it is recommended to better discover devices.\\n\" +\n" +
                        "                \"\\n\" + \"Please accept to allow Location Permission and Services.\""),

        /**
         * Dialog message telling user why they should turn on location services
         */
        LOCATION_SERVICES_NEEDS_ENABLING("location_services_needs_enabling", "Android Marshmallow (6.0+) requires Location Services for improved Bluetooth device scanning. While it is not required, it is recommended that Location Services are turned on to improve device discovery."),

        /**
         * Toast shown to users asking them to enable location services.  This appears over the system options
         */
        LOCATION_SERVICES_TOAST("location_services_toast", "Please enable Location Services then press back."),

        /**
         * Used for the positive button of single button dialogs
         */
        OK("ok", "Ok"),

        /**
         * Used for the negative button of two button dialogs
         */
        DENY("deny", "Deny"),

        /**
         * Used as the positive button on two button dialogs
         */
        ACCEPT("accept", "Accept");

        private static final String kBaseStringId = "sb_%s";

        private String mStringId;
        private String mFallbackString;

        DefaultString(String stringId, String fallbackString)
        {
            mStringId = String.format(kBaseStringId, stringId);
            mFallbackString = fallbackString;
        }

        public String getStringId()
        {
            return mStringId;
        }

        public String getFallbackString()
        {
            return mFallbackString;
        }
    }

    /**
     * This class holds a permission and user defined metadata.  It exists solely to provide a mechanism to associate extra information with
     * the {@link Permission#CUSTOM} permission.
     */
    public static final class PermissionInstance
    {
        protected Permission m_permission;
        protected Object m_metadata;

        protected PermissionInstance(Permission p)
        {
            m_permission = p;
        }

        protected PermissionInstance(Permission p, Object metadata)
        {
            m_permission = p;
            m_metadata = metadata;
        }

        public Permission getPermission()
        {
            return m_permission;
        }

        public Object getMetadata()
        {
            return m_metadata;
        }
    }

    /**
     * This class will be passed into your {@link ResultListener} when the enabler finishes
     */
    public static class Result
    {
        boolean mSuccessful;
        List<PermissionInstance> mEnabledPermissions = new ArrayList<>();
        List<PermissionInstance> mSkippedPermissions = new ArrayList<>();
        ErrorCode mErrorCode = null;
        String mErrorMessage = null;

        /**
         * If true, the enabler was able to obtain all of the required permissions.  If false, at least one permission was not obtained.
         * You can call {@link Result#getEnabledPermissions()} and {@link Result#getSkippedPermissions()} to determine the status of each
         * individual permission.  You can also use {@link Result#getErrorMessage()} {@link Result#getErrorCode()} to get
         * more information about errors in the event that the enabler wasn't successful.
         */
        public boolean getSuccessful()
        {
            return mSuccessful;
        }

        /**
         * Returns a list of permissions that were successfully enabled
         */
        public List<PermissionInstance> getEnabledPermissions()
        {
            return mEnabledPermissions;
        }

        /**
         * Returns a list of permissions that were not successfully enabled, but needed to be
         */
        public List<PermissionInstance> getSkippedPermissions()
        {
            return mSkippedPermissions;
        }

        /**
         * If {@link Result#getSuccessful()} returned false, this will contain an error code indicating the reason.  It will be null
         * in the event that the enabler was successful
         */
        public ErrorCode getErrorCode()
        {
            return mErrorCode;
        }

        /**
         * If {@link Result#getSuccessful()} returned false, this will contain an error message describing the reason.  It will be null
         * in the event that the enabler was successful
         */
        public String getErrorMessage()
        {
            return mErrorMessage;
        }
    }

    /**
     * This class holds a permission and user defined metadata.  It exists solely to provide a mechanism to associate extra information with
     * the {@link Permission#CUSTOM} permission.
     */
    public interface ResultListener
    {
        /**
         * This method will be invoked on the listener whenever the enabler finishes.  See {@link Result} for details on what information
         * you will be provided with
         */
        void onFinished(Result result);
    }

    /**
     * This class implements all of the enabler logic that can be overridden to alter the look or the messaging of the enabler.  Additionally,
     * if you want to use the enabler to request custom permissions, you must provide your own subclass of the implementation that implements
     * {@link BluetoothEnablerImpl#checkIsCustomPermissionRequired}, {@link BluetoothEnablerImpl#checkIsCustomPermissionEnabled} and
     * {@link BluetoothEnablerImpl#requestCustomPermission}.  See each individual method for more information
     */
    public static class BluetoothEnablerImpl
    {
        protected BleSetupHelper mEnabler;

        /**
         * This method is used by the enabler to obtain strings that will be shown to the user.  If you override
         * it in a subclass, you can alter the strings used by the enabler.  See {@link DefaultString} for possible
         * enum values
         *
         * @param ds - enum value for the string being requested
         * @return the value for the requested string
         */
        public String getString(DefaultString ds)
        {
            // Try to look up string by resource Id
            Activity activity = mEnabler.mWeakActivity.get();

            String foundString = null;
            if (activity != null)
            {
                Resources r = activity.getResources();
                if (r != null)
                {
                    int resId = r.getIdentifier(ds.getStringId(), "string", activity.getPackageName());
                    if (resId != 0)
                        foundString = activity.getString(resId);
                }
            }
            if (foundString != null)
                return foundString;

            // Fall back on hardcode
            return ds.getFallbackString();
        }

        /**
         * This method is used by the enabler to determine if a given custom permission is required.  You must
         * implement this in a subclass if you are using custom permissions.
         *
         * @param metadata - the metadata associated with the custom permission being checked
         * @return if this permission needs to be enabled
         */
        public boolean checkIsCustomPermissionRequired(Object metadata)
        {
            return false;
        }

        /**
         * This method is used by the enabler to determine if a given custom permission enabled.  You must
         * implement this in a subclass if you are using custom permissions.  Any custom permissions that
         * are both required and not enabled will be requested.
         *
         * @param metadata - the metadata associated with the custom permission being checked
         * @return if this permission is enabled
         */
        public boolean checkIsCustomPermissionEnabled(Object metadata)
        {
            return false;
        }

        /**
         * This method implements the actual logic required to request a permission from the user.  You must
         * implement this in a subclass if you are using custom permissions.  The enabler will call this method
         * and provide the metadata associated with the permission in question.  When you are done requesting
         * the permission, you MUST call {@link BluetoothEnablerImpl#requestCustomPermission(Object)} afterward,
         * regardless of the outcome.
         *
         * @param metadata - the metadata associated with the custom permission being checked
         */
        public void requestCustomPermission(Object metadata)
        {
            mEnabler.errorOut(ErrorCode.CRITICAL_MISUSE, "You must provide a BluetoothEnablerImpl that implements requestCustomPermission in order to use custom permissions");
        }

        /**
         * This method is only used if you are implementing custom permissions in a subclass.  If not,
         * you can safely ignore it.  This method must be called once and only once after a custom
         * permission has been requested from the user via the
         * {@link BluetoothEnablerImpl#requestCustomPermission(Object)}  method.
         */
        protected final void onCustomPermissionRequestComplete()
        {
            mEnabler.doneRequestingCustomPermission();
        }

        /**
         * This method is responsible for creating and showing a dialog to the user.  If you want to
         * customize the appearance of the dialogs, or if you want to show some other type of UI
         * instead of a dialog, you can subclass this method.  You MUST call either the clickListener
         * or the dismissListener when you are done (but not both).  If the clickListener is called,
         * the enabler will treat this as the user agreeing to grant the permission, and will continue
         * with the next step.  If the dismissListener is called, the permission will be rejected.
         *
         * @param pi - the permission instance this dialog is associated with
         * @param message - the message the dialog should display
         * @param button1Text - the text for the first button on the dialog (neutral, or positive in 2 button dialog)
         * @param button2Text - the text for the negative button on the dialog, or null if there is only one button
         * @param clickListener - the listener that must be called if the positive button is clicked,
         *                     or if the user agrees to allowing the permission in a custom UI
         * @param dismissListener - the listener that must be called if the user declines to allow
         *                       the permission, or if the dialog is dismissed in any other way
         */
        public void showDialog(PermissionInstance pi, String message, String button1Text, String button2Text, final DialogInterface.OnClickListener clickListener, final DialogInterface.OnDismissListener dismissListener)
        {
            Activity activity = mEnabler.mWeakActivity.get();
            if (activity == null)
            {
                mEnabler.errorOut(ErrorCode.LOST_ACTIVITY_OR_MANAGER);
                return;
            }

            final AlertDialog.Builder builder = new AlertDialog.Builder(activity);

            builder.setMessage(message);

            if (button1Text != null && button2Text != null)
            {
                // Show positive/negative buttons

                builder.setPositiveButton(button1Text, (dialog, which) -> {
                    // Call user click listener
                    if (clickListener != null)
                        clickListener.onClick(dialog, which);

                    // Clear the onDismiss listener since we fired a result
                    ((AlertDialog) dialog).setOnDismissListener(null);
                });

                builder.setNegativeButton(button2Text, (dialog, which) -> {
                    // Call user click listener
                    if (dismissListener != null)
                        dismissListener.onDismiss(dialog);

                    // Clear the onDismiss listener since we fired a result
                    ((AlertDialog) dialog).setOnDismissListener(null);
                });
            }
            else if (button1Text != null)
            {
                builder.setNeutralButton(button1Text, (dialog, which) -> {
                    // Call user click listener
                    if (clickListener != null)
                        clickListener.onClick(dialog, which);

                    // Clear the onDismiss listener since we fired a result
                    ((AlertDialog)dialog).setOnDismissListener(null);
                });
            }

            // We need to track whenever the dialog is dismissed
            builder.setOnDismissListener(dialog -> {
                if (dismissListener != null)
                    dismissListener.onDismiss(dialog);
            });

            try
            {
                builder.show();
            }
            catch (WindowManager.BadTokenException e)
            {
                Log.e("Show Dialog", "WindowManagerBadTokenException thrown when trying to show a dialog: " + e.getMessage());

                P_Bridge_User.getIBleManager(mEnabler.mWeakManager.get()).uhOh(UhOhListener.UhOh.SETUP_HELPER_DIALOG_ERROR);

                mEnabler.errorOut(ErrorCode.CRITICAL_MISUSE);
            }
        }

        /**
         * This method is responsible for showing a toast to the user.  If you want to customize the
         * appearance of the toasts, or if you want to show some other type of UI instead, you can
         * subclass this method.  Toasts are normally used when we must open the system settings
         * panel, so that we can show the user a message ontop of the settings
         *
         * @param pi - the permission instance this toast is associated with
         * @param message - the message the toast should display
         */
        public void showToast(PermissionInstance pi, String message)
        {
            Activity activity = mEnabler.mWeakActivity.get();
            if (activity == null)
            {
                mEnabler.errorOut(ErrorCode.LOST_ACTIVITY_OR_MANAGER);
                return;
            }

            Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * This function handles the creation and startup of the enabler using default options.  Assuming
     * you don't want to customize any behavior and you just want the standard permissions requested,
     * this is the quickest &amp; easiest way to use the enabler.
     *
     * @param manager - the {@link BleManager} to use.  If it becomes null, the enabler will halt
     * @param activity - the {@link Activity} to use.  If it becomes null, the enabler will halt
     * @param listener - the {@link ResultListener} to use.  Can be null if you do not want a callback
     */
    public static void runEnabler(BleManager manager, Activity activity, ResultListener listener)
    {
        // Add permissions that we know we need
        BleSetupHelper instance = new BleSetupHelper(manager, activity, listener);
        instance.addRequiredPermissions();
        instance.start();
    }

    // State management
    State mState = State.UNINITIALIZED;  // We start uninitialized
    Application.ActivityLifecycleCallbacks m_lifecycleCallbacks = null;
    boolean mHaveShownLocationPrompt = false;  // Used for some custom logic regarding the combining of both location related permissions into one

    // Implementation of our core functionality
    BluetoothEnablerImpl mImpl = new BluetoothEnablerImpl();

    // Weak references to core classes
    WeakReference<BleManager> mWeakManager;
    WeakReference<Activity> mWeakActivity;

    // Cache an instance of the main thread Handler
    Handler mHandler = new Handler(Looper.getMainLooper());

    // Permission tracking
    PermissionInstance mCurrentPermission = null;
    List<PermissionInstance> mPermissionList = new ArrayList<>();
    List<PermissionInstance> mSucceededPermissionList = new ArrayList<>();
    List<PermissionInstance> mFailedPermissionList = new ArrayList<>();
    ResultListener mResultListener = null;

    // Error tracking
    ErrorCode mErrorCode = null;
    String mCustomErrorMessage = null;

    /**
     * Constructor for use when you don't want a result listener.  Before the enabler will do anything,
     * you must add some permissions via {@link BleSetupHelper#addRequiredPermissions()} and/or
     * {@link BleSetupHelper#addCustomPermission(Object)}, and then call {@link BleSetupHelper#start()}
     *
     * @param manager - the {@link BleManager} to use.  If it becomes null, the enabler will halt
     * @param activity - the {@link Activity} to use.  If it becomes null, the enabler will halt
     */
    public BleSetupHelper(BleManager manager, Activity activity)
    {
        mImpl.mEnabler = this;
        mWeakManager = new WeakReference<>(manager);
        mWeakActivity = new WeakReference<>(activity);
    }

    /**
     * Constructor for use when you want a result listener.  Before the enabler will do anything,
     * you must add some permissions via {@link BleSetupHelper#addRequiredPermissions()} and/or
     * {@link BleSetupHelper#addCustomPermission(Object)}, and then call {@link BleSetupHelper#start()}
     *
     * @param manager - the {@link BleManager} to use.  If it becomes null, the enabler will halt
     * @param activity - the {@link Activity} to use.  If it becomes null, the enabler will halt
     * @param listener - the {@link ResultListener} that will be called when the enabler finishes
     */
    public BleSetupHelper(BleManager manager, Activity activity, ResultListener listener)
    {
        this(manager, activity);
        mImpl.mEnabler = this;
        mResultListener = listener;
    }

    /**
     * Constructor for use when you want a result listener, and to provide your own implementation of
     * the enabler, or add on to the implementation with custom permissions. Before the enabler will do anything,
     * you must add some permissions via {@link BleSetupHelper#addRequiredPermissions()} and/or
     * {@link BleSetupHelper#addCustomPermission(Object)}, and then call {@link BleSetupHelper#start()}
     *
     * @param manager - the {@link BleManager} to use.  If it becomes null, the enabler will halt
     * @param activity - the {@link Activity} to use.  If it becomes null, the enabler will halt
     * @param listener - the {@link ResultListener} that will be called when the enabler finishes
     * @param enablerImpl - the {@link BluetoothEnablerImpl} that will be used to handle the logic of getting permissions
     */
    public BleSetupHelper(BleManager manager, Activity activity, BluetoothEnablerImpl enablerImpl, ResultListener listener)
    {
        if (enablerImpl == null)
            throw new RuntimeException("Implementation instance null");

        mImpl = enablerImpl;
        mImpl.mEnabler = this;
        mWeakManager = new WeakReference<>(manager);
        mWeakActivity = new WeakReference<>(activity);
        mResultListener = listener;
    }

    /**
     * This method allows you to set a custom implementation which can be used to implement custom
     * permissions, or to customize the look or text of the dialogs and toasts shown by the enabler.
     *
     * @param impl - the custom implementation
     */
    public void setImpl(BluetoothEnablerImpl impl)
    {
        mImpl = impl;
        mImpl.mEnabler = this;
    }

    /**
     * This method allows you to change the {@link ResultListener} after construction of the enabler
     *
     * @param resultListener - the new {@link ResultListener}
     */
    public void setResultListener(ResultListener resultListener)
    {
        mResultListener = resultListener;
    }

    /**
     * This method will populate the list of required permissions with the standard defaults.  You
     * probably always want these to be enabled if you're using the enabler, unless you are only
     * using it for custom permissions or you have a special situation where you don't need all of
     * the standard bluetooth permissions.  If you are going to call this, you must call it before
     * {@link BleSetupHelper#start()} is called.  If you use the static utility method
     * {@link BleSetupHelper#runEnabler(BleManager, Activity, ResultListener)}, this will be
     * done for you.
     */
    public void addRequiredPermissions()
    {
        for (Permission p : Permission.getPermissions())
        {
            // Skip custom permission
            if (p == Permission.CUSTOM)
                continue;

            PermissionInstance pi = new PermissionInstance(p, null);
            if (checkIsRequired(pi) && !checkIsEnabled(pi))
            {
                Log.d("enabler", "Adding " + p + " as a required permission");
                mPermissionList.add(pi);
            }
            else
            {
                boolean b1 = checkIsRequired(pi);
                boolean b2 = checkIsEnabled(pi);
                Log.d("enabler", "NOT adding " + p + " as a required permission");
            }
        }
    }

    /**
     * Adds a custom permission to the required permission list.  In order to use this, you must
     * provide a custom {@link BluetoothEnablerImpl} via {@link BleSetupHelper#setImpl(BluetoothEnablerImpl)}.
     * If you don't, the enabler will error out when it tries to check or enable the custom permission.
     * The metadata object you provide will be passed into all methods that pertain to the custom
     * permission.  You can use this to distinguish multiple custom permissions from eachother, or
     * to tack on and track additional information
     *
     * @param metadata - the metadata that will accompany the custom permission
     *
     */
    public void addCustomPermission(Object metadata)
    {
        mPermissionList.add(new PermissionInstance(Permission.CUSTOM, metadata));
    }

    /**
     * This method starts the enabler.  You can only call this once, and you should have set up
     * all of the required permissions and any other customization you want ahead of time.
     */
    public void start()
    {
        mHandler.post(this::startInternal);
    }

    private void startInternal()
    {
        if (mState == State.UNINITIALIZED)
            setState(State.STARTED);
        else
            errorOut(ErrorCode.CRITICAL_MISUSE);
    }

    protected void setState(State newState)
    {
        //TODO:  Transition rules here, make sure we're in the appropriate state
        if (mState.getIsHalt())
            return;

        //TODO:  Clean up old state
        Log.d("enabler", "Transitioning from state "  + mState + " to " + newState);
        mState = newState;

        // Grab hard references
        Activity activity = mWeakActivity.get();
        if (activity == null)
        {
            errorOut(ErrorCode.LOST_ACTIVITY_OR_MANAGER);
            return;
        }

        // Do whatever we need to for the new state
        switch (newState)
        {
            case STARTED:
                // OK, lets get things kicked off

                // Register the lifecycle callbacks
                m_lifecycleCallbacks = createLifecycleCallbacks();
                activity.getApplication().registerActivityLifecycleCallbacks(m_lifecycleCallbacks);

                // Begin by transitioning to the next permission
                requestNextPermission();

                // Return since we may have recursively transitioned to other states
                return;

            case SHOWING_UI_FOR_PERMISSION:
                // Instruct the impl to present whatever UI is needed to prompt for the permission
                showUIToEnable(mCurrentPermission);

                // Note, it could have been a no-op if there was no UI to show.  If that was the case, there will be another state transition

                // Return since we may have recursively transitioned to other states
                return;

            case REQUESTING_PERMISSION:
                // Instruct the impl to actually request the permission from the system
                requestPermission(mCurrentPermission);
                return;

            case FINISHING:
                // OK, we are 'done' at this point...  Do any cleanup here and enter one of the final states

                // Remove our callbacks
                if (activity != null && m_lifecycleCallbacks != null)
                    activity.getApplication().unregisterActivityLifecycleCallbacks(m_lifecycleCallbacks);

                // Enter final halt state
                if (mFailedPermissionList.size() > 0)
                {
                    mErrorCode = ErrorCode.USER_REJECTED;
                    setState(State.FINISHED_FAILED);
                }
                else
                    setState(State.FINISHED_SUCCESS);
                return;

            case FINISHED_FAILED:
                // Dispatch callback if appropriate
                if (mResultListener != null)
                {
                    Result r = new Result();
                    r.mSuccessful = false;
                    r.mEnabledPermissions = mSucceededPermissionList;
                    r.mSkippedPermissions = mFailedPermissionList;
                    r.mErrorCode = mErrorCode != null ? mErrorCode : ErrorCode.USER_REJECTED;
                    r.mErrorMessage = mCustomErrorMessage != null ? mCustomErrorMessage : r.mErrorCode.getDefaultErrorMessage();
                    postCallback(r);
                }
                return;

            case FINISHED_SUCCESS:
                // Dispatch callback if appropriate
                if (mResultListener != null)
                {
                    Result r = new Result();
                    r.mSuccessful = true;
                    r.mEnabledPermissions = mSucceededPermissionList;
                    r.mSkippedPermissions = mFailedPermissionList;
                    postCallback(r);
                }
                return;

            case CRITICAL_ERROR:
            {
                Log.d("enabler", "Critical error encountered, halting enabler");

                // Add anything that hasn't already been enabled to the fail list, then call the listener
                if (mCurrentPermission != null)
                    mFailedPermissionList.add(mCurrentPermission);
                mFailedPermissionList.addAll(mPermissionList);

                if (mResultListener != null)
                {
                    Result r = new Result();
                    r.mSuccessful = false;
                    r.mEnabledPermissions = mSucceededPermissionList;
                    r.mSkippedPermissions = mFailedPermissionList;
                    r.mErrorCode = mErrorCode != null ? mErrorCode : ErrorCode.UNKNOWN;
                    r.mErrorMessage = mCustomErrorMessage != null ? mCustomErrorMessage : r.mErrorCode.getDefaultErrorMessage();

                    postCallback(r);
                }

                return;
            }
        }
    }

    protected void postCallback(final Result r)
    {
        BleManager mgr = mWeakManager.get();
        if (mgr == null && mState != State.CRITICAL_ERROR)
        {
            errorOut(ErrorCode.LOST_ACTIVITY_OR_MANAGER);
            return;
        }

        mHandler.post(() -> mResultListener.onFinished(r));
    }

    protected void requestNextPermission()
    {
        // Handle finishing current?
        mCurrentPermission = null;

        // See if we have any more permissions left to request
        if (mPermissionList.size() < 1)
        {
            setState(State.FINISHING);
            return;
        }

        // Extract the next permission from the list
        mCurrentPermission = mPermissionList.get(0);
        mPermissionList.remove(0);

        // Is the permission actually needed?  If not, skip it
        if (!checkIsRequired(mCurrentPermission) || checkIsEnabled(mCurrentPermission))
        {
            mCurrentPermission = null;
            requestNextPermission();
            return;
        }

        // Transition to next state where we show the UI for the permission
        setState(State.SHOWING_UI_FOR_PERMISSION);
    }

    // Callback fired when a permission is granted or rejected by the system
    protected void handleUIResult(PermissionInstance pi, boolean result)
    {
        if (mState != State.SHOWING_UI_FOR_PERMISSION)
        {
            errorOut(ErrorCode.INTERNAL_ERROR, "handleUIResult called in improper state.  State is " + mState + " but should be " + State.SHOWING_UI_FOR_PERMISSION);
            return;
        }

        if (result)
        {
            // Enter request state permission
            setState(State.REQUESTING_PERMISSION);
        }
        else
        {
            // If the user declined, just finish the permission
            finishCurrentPermission(false);
        }
    }

    // Called once we are done with the current permission, one way or another.  After this, we either get the next permission and request that, or we halt
    protected void finishCurrentPermission(boolean success)
    {
        if (mCurrentPermission == null)
            return;

        PermissionInstance lastPermission = mCurrentPermission;
        List<PermissionInstance> l = success ? mSucceededPermissionList : mFailedPermissionList;
        l.add(mCurrentPermission);
        mCurrentPermission = null;

        if (success)
            requestNextPermission();
        else
        {
            if (lastPermission.getPermission().getIsLocationRelated())
                mImpl.showDialog(lastPermission, mImpl.getString(DefaultString.DENYING_LOCATION_ACCESS), mImpl.getString(DefaultString.OK), null, null, null);

            setState(State.FINISHING);
        }
    }

    protected void doneRequestingCustomPermission()
    {
        if (mState != State.REQUESTING_PERMISSION || mCurrentPermission == null || mCurrentPermission.getPermission() != Permission.CUSTOM)
        {
            errorOut(ErrorCode.CRITICAL_MISUSE);
            return;
        }

        // Finish the permission
        finishCurrentPermission(checkIsEnabled(mCurrentPermission));
    }

    protected void afterPermissionRequested()
    {
        // Ignore if the current permission is a custom one, it's up to the enabler to call doneRequestingCustomPermission instead
        if (mCurrentPermission.getPermission() == Permission.CUSTOM)
            return;

        if (mState != State.REQUESTING_PERMISSION)
        {
            // Ignore calls if we weren't requesting a permission
            errorOut(ErrorCode.INTERNAL_ERROR);
            return;
        }

        // Advance
        finishCurrentPermission(checkIsEnabled(mCurrentPermission));
    }

    protected boolean checkIsEnabled(PermissionInstance pi)
    {
        BleManager manager = mWeakManager.get();
        if (manager == null)
        {
            errorOut(ErrorCode.LOST_ACTIVITY_OR_MANAGER);
            return false;
        }

        switch (pi.getPermission())
        {
            case BLUETOOTH:
                return manager.isBleSupported() && manager.is(BleManagerState.ON);
            case LOCATION_PERMISSION:
                return manager.isLocationEnabledForScanning_byRuntimePermissions();
            case LOCATION_SERVICES:
                return manager.isLocationEnabledForScanning_byOsServices();
            case ANDROID_12_BLUETOOTH:
                return manager.areBluetoothPermissionsEnabled();
            case CUSTOM:
                return mImpl.checkIsCustomPermissionEnabled(pi.getMetadata());
        }

        return true;
    }

    protected boolean checkIsRequired(PermissionInstance pi)
    {
        switch (pi.getPermission())
        {
            case BLUETOOTH:
                return true;
            case LOCATION_PERMISSION:
            case LOCATION_SERVICES:
                if (Utils.isMarshmallow())
                {
                    if (Utils.isAndroid12() && mWeakManager.get().getConfigClone().doNotRequestLocation)
                        return false;
                    return true;
                }
                return false;
            case ANDROID_12_BLUETOOTH:
                if (Utils.isAndroid12())
                    return true;
                return false;
            case CUSTOM:
                return mImpl.checkIsCustomPermissionRequired(pi.getMetadata());
        }
        return false;
    }

    protected void requestPermission(PermissionInstance pi)
    {
        Activity activity = mWeakActivity.get();
        BleManager manager = mWeakManager.get();
        if (activity == null || manager == null)
        {
            errorOut(ErrorCode.LOST_ACTIVITY_OR_MANAGER);
            return;
        }

        switch (pi.getPermission())
        {
            case BLUETOOTH:
            {
                manager.turnOnWithIntent(activity, mCurrentPermission.getPermission().getRequestCode());
                break;
            }

            case LOCATION_PERMISSION:
            {
                boolean toast = !manager.willLocationPermissionSystemDialogBeShown(activity);
                manager.turnOnLocationWithIntent_forPermissions(activity, mCurrentPermission.getPermission().getRequestCode());
                if (toast)
                    mImpl.showToast(pi, mImpl.getString(DefaultString.LOCATION_PERMISSION_TOAST));
                break;
            }

            case LOCATION_SERVICES:
            {
                manager.turnOnLocationWithIntent_forOsServices(activity, mCurrentPermission.getPermission().getRequestCode());
                mImpl.showToast(pi, mImpl.getString(DefaultString.LOCATION_SERVICES_TOAST));
                break;
            }

            case ANDROID_12_BLUETOOTH:
            {
                BleManagerConfig cfg = manager.getConfigClone();
                S_Util.requestPermissions(activity, mCurrentPermission.getPermission().getRequestCode(),
                        cfg.requestBackgroundOperation, cfg.requestAdvertisePermission);
                break;
            }

            case CUSTOM:
                mImpl.requestCustomPermission(pi.getMetadata());
                break;
        }
    }

    protected void errorOut(ErrorCode ec)
    {
        errorOut(ec, null);
    }

    protected void errorOut(ErrorCode ec, String errorMessage)
    {
        mErrorCode = ec;
        mCustomErrorMessage = errorMessage;
        setState(State.CRITICAL_ERROR);
    }

    protected void showUIToEnable(final PermissionInstance pi)
    {
        BleManager manager = mWeakManager.get();
        if (manager == null)
        {
            errorOut(ErrorCode.LOST_ACTIVITY_OR_MANAGER);
            return;
        }

        if (pi.getPermission() == Permission.LOCATION_PERMISSION)
        {
            if (!manager.isLocationEnabledForScanning_byManifestPermissions())
            {
                //FIXME:  Do we want to abort immediately, or only on dismiss?
                mImpl.showDialog(pi, mImpl.getString(DefaultString.APP_NEEDS_PERMISSION), mImpl.getString(DefaultString.OK), null, null,null);

                finishCurrentPermission(false);
                return;
            }
        }

        // Decide if we should show a dialog before asking for permissions
        if (pi.getPermission().getIsLocationRelated() && !mHaveShownLocationPrompt)
        {
            mHaveShownLocationPrompt = true;

            // Show either the individual or group pairing dialog
            int otherLocationPermissionCount = 0;
            for (PermissionInstance pi2 : mPermissionList)
            {
                if (pi2.getPermission().getIsLocationRelated())
                    ++otherLocationPermissionCount;
            }

            DefaultString message = DefaultString.REQUIRES_LOCATION_PERMISSION_AND_SERVICES;
            if (otherLocationPermissionCount < 1)
            {
                switch (pi.getPermission())
                {
                    case LOCATION_PERMISSION:
                        break;

                    case LOCATION_SERVICES:
                        break;
                }
            }

            // Pop a dialog
            mImpl.showDialog(pi, mImpl.getString(otherLocationPermissionCount > 0 ? DefaultString.REQUIRES_LOCATION_PERMISSION_AND_SERVICES : DefaultString.REQUIRES_LOCATION_PERMISSION),
                    mImpl.getString(DefaultString.ACCEPT),
                    mImpl.getString(DefaultString.DENY),
                    (dialog, which) -> handleUIResult(pi, true),
                    dialog -> handleUIResult(pi, false));

            // Return, we don't want to fire a UI result callback just yet
            return;
        }

        // If we get to this point, we don't have any UI to show before asking for the permission
        // Lets just ask directly
        handleUIResult(pi, true);
    }

    private Application.ActivityLifecycleCallbacks createLifecycleCallbacks()
    {
        return new Application.ActivityLifecycleCallbacks()
        {
            @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState)
            {
            }

            @Override public void onActivityStarted(Activity activity)
            {
            }

            @Override public void onActivityStopped(Activity activity)
            {
            }

            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState)
            {
            }

            @Override public void onActivityDestroyed(Activity activity)
            {
            }

            @Override public void onActivityPaused(Activity activity)
            {
            }

            @Override public void onActivityResumed(Activity activity)
            {
                // Fire up this event on every resume...  We will just ignore ones we don't care about anyway
                afterPermissionRequested();
            }
        };
    }
}
