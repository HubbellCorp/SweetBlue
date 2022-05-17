package com.idevicesinc.sweetblue.simple_service;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.BuildConfig;
import com.idevicesinc.sweetblue.DiscoveryListener;
import com.idevicesinc.sweetblue.LogOptions;
import com.idevicesinc.sweetblue.utils.BleSetupHelper;
import com.idevicesinc.sweetblue.utils.NativeScanFilter;
import com.idevicesinc.sweetblue.utils.Utils_String;
import java.util.ArrayList;
import java.util.List;

import static com.idevicesinc.sweetblue.simple_service.Constants.ACTION_CONNECT;
import static com.idevicesinc.sweetblue.simple_service.Constants.ACTION_DISCONNECT;
import static com.idevicesinc.sweetblue.simple_service.Constants.ACTION_START;
import static com.idevicesinc.sweetblue.simple_service.Constants.ACTION_STOP;
import static com.idevicesinc.sweetblue.simple_service.Constants.EXTRA_MAC_ADDRESS;

import androidx.appcompat.app.AppCompatActivity;

/**
 * A simple example showing how to use SweetBlue in a service.
 */
public class MainActivity extends AppCompatActivity
{
    // We're keeping an instance of BleManager for convenience, but it's not really necessary since it's a singleton. It's helpful so you
    // don't have to keep passing in a Context to retrieve it.
    private BleManager mBleManager;

    // List View and Adaptor for displaying all of the scan results.
    private ListView mListView;
    private ScanAdaptor mAdaptor;
    private ArrayList<BleDevice> mDevices;

    // Button for starting the foreground service.
    private Button mStartScanService;

    // Button for stopping the foreground service.
    private Button mStopScanService;

    // Button for clearing the list of devices.
    private Button mClearList;

    private final static int STATE_CHANGE_MIN_TIME = 50;
    private long mLastStateChange;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mListView = findViewById(R.id.listview_devices);

        mStartScanService = findViewById(R.id.button_start_service);
        mStopScanService = findViewById(R.id.button_stop_service);
        mClearList = findViewById(R.id.button_clear_list);


        BleManagerConfig config = new BleManagerConfig();

        // Only enable logging in debug builds.
        config.loggingOptions = BuildConfig.DEBUG ? LogOptions.ON : LogOptions.OFF;

        // In Android 8.1+ a BLE scan occurring while the phone's screen is off requires a scan filter to be set.
        // The Native Android Scan Filter is only available in 5.0+ and if set on earlier versions, will be ignored. 
        // The scan filter can be set to filter one or more of the following:
        // 1. DeviceName (NOTE: This is an exact match which means the match is case sensitive and wild cards cannot be used.)
        //      SweetBlue does provide filtering on device names which are NOT a strict match.
        //      These filters can be used in CONJUNCTION with the native filters but if scanning is being done while the screen is off,
        //      a native scan filter MUST be set.
        //      The following is an example of starting a scan with filtering on device name:
        //          mBleManager.startScan(new DefaultScanFilter("DEVICE NAME HERE"))
        // 2. ServiceUUID
        // 3. ServiceData
        // 4. ServiceAddress (MAC)
        // 5. ManufacturerData
        ArrayList<NativeScanFilter> list = new ArrayList<>();
        NativeScanFilter.Builder b;
        b = new NativeScanFilter.Builder();
        //        TODO: REPLACE WITH YOUR FILTER
        //        b.setServiceUuid(ParcelUuid.fromString("00000000-0000-0000-0000-000000000000"));
        list.add(b.build());
        config.defaultNativeScanFilterList = list;

        //This config must also be set so the scan is not paused if the app is backgrounded or the phone's screen is shut off.
        config.autoPauseResumeDetection = false;

        // Get the instance of the BleManager, and pass in the config.
        mBleManager = BleManager.get(this, config);

        // The app can either be launched from the home screen or from a notification where there is an ongoing scan.
        // Depending on where it is started, we want to make sure the initial state of the buttons are correct.
        toggleButtonState(mBleManager.isScanning());

        // This listener is set to toggle the start/stop buttons depending on if a scan is ongoing.
        mBleManager.setListener_State(e -> toggleButtonState(mBleManager.isScanning()));

        // This listener is set to update the row in the list view as the device state is changing.
        // Since this listener can be called often, the updating is gated by a simple time check.
        mBleManager.setListener_DeviceState(e ->
        {
            if (System.currentTimeMillis() - mLastStateChange > STATE_CHANGE_MIN_TIME)
            {
                mAdaptor.notifyDataSetChanged();
                mLastStateChange = System.currentTimeMillis();
            }
        });

        // Set the discovery listener. You can pass in a listener when calling startScan() if you want, but in most cases, this is preferred so you don't
        // have to keep passing in the listener when calling any of the startScan methods.
        mBleManager.setListener_Discovery(mDiscoveryListener);

        setupButtons();

        setupListView();

        // The BleSetupHelper will take care of requesting the necessary permissions on Android M and above. It needs A> Bluetooth to be on of course,
        // B> Location permissions, and C> Location services enabled in order for a BLE scan to return any results.
        BleSetupHelper enabler = new BleSetupHelper(mBleManager, MainActivity.this, result ->
        {
            Log.d("+++", "Enabler finished with result " + result.getSuccessful());
            if (result.getSuccessful())
                if (!mBleManager.isScanning())
                    mStartScanService.setEnabled(true);
        });
        enabler.start();
    }

    private void setupButtons()
    {
        // Sends an intent to the BleScanService which indicates that it should start a scan in the foreground.
        mStartScanService.setOnClickListener(v ->
        {
            // The BleSetupHelper will take care of requesting the necessary permissions on Android M and above. It needs A> Bluetooth to be on of course,
            // B> Location permissions, and C> Location services enabled in order for a BLE scan to return any results.
            BleSetupHelper.runEnabler(mBleManager, this, result ->
            {
                if (result.getSuccessful())
                {
                    Intent startIntent = new Intent(MainActivity.this, BleScanService.class);
                    startIntent.setAction(ACTION_START);
                    startService(startIntent);
                }
            });
        });

        // Sends an intent to the BleScanService which indicates that it should stop the ongoing scan.
        mStopScanService.setOnClickListener(v ->
        {
            Intent stopIntent = new Intent(MainActivity.this, BleScanService.class);
            stopIntent.setAction(ACTION_STOP);
            startService(stopIntent);
        });

        mClearList.setOnClickListener(v ->
        {
            mClearList.setEnabled(false);
            mDevices.clear();
            mAdaptor.notifyDataSetChanged();
        });
    }

    private void setupListView()
    {
        mDevices = new ArrayList<>();
        mAdaptor = new ScanAdaptor(this, mDevices);
        mListView.setAdapter(mAdaptor);

        // Set a click listener which will connect to the device if not already connected.
        // If the device is connected, this will disconnect it.
        mListView.setOnItemClickListener((parent, view, position, id) ->
        {
            BleDevice device = mDevices.get(position);

            if (device.is(BleDeviceState.CONNECTED))
            {
                Intent disconnectIntent = new Intent(MainActivity.this, BleScanService.class);
                disconnectIntent.setAction(ACTION_DISCONNECT);
                // SweetBlue caches all discovered devices so they can later retrieved by using the MAC Address
                // which is the simplest to send to a service.
                disconnectIntent.putExtra(EXTRA_MAC_ADDRESS, device.getMacAddress());
                startService(disconnectIntent);
            }
            else
            {
                Intent connectIntent = new Intent(MainActivity.this, BleScanService.class);
                connectIntent.setAction(ACTION_CONNECT);
                connectIntent.putExtra(EXTRA_MAC_ADDRESS, device.getMacAddress());
                startService(connectIntent);
            }
        });
    }

    // Convenience method to toggle the state of the start/stop buttons depending on if a scan is ongoing.
    private void toggleButtonState(boolean scanning)
    {
        mStartScanService.setEnabled(!scanning);
        mStopScanService.setEnabled(scanning);
    }

    // Simple discovery listener implementation.
    private final DiscoveryListener mDiscoveryListener = event ->
    {
        // If the device is not already in the list, add it and notify the adapter
        if (!mDevices.contains(event.device()))
        {
            mClearList.setEnabled(true);
            mDevices.add(event.device());
            mAdaptor.notifyDataSetChanged();
        }
    };

    /**
     * Simple ListView Adapter displaying the name of the device received through the scan.
     */
    private class ScanAdaptor extends ArrayAdapter<BleDevice>
    {

        private List<BleDevice> mDevices;


        ScanAdaptor(Context context, List<BleDevice> objects)
        {
            super(context, R.layout.scan_listitem_layout, objects);
            mDevices = objects;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            ViewHolder v;
            final BleDevice device = mDevices.get(position);
            if (convertView == null)
            {
                convertView = View.inflate(getContext(), R.layout.scan_listitem_layout, null);
                v = new ViewHolder();
                v.name = convertView.findViewById(R.id.name);
                convertView.setTag(v);
            }
            else
            {
                v = (ViewHolder) convertView.getTag();
            }
            v.name.setText(Utils_String.concatStrings(device.toString(), "\nNative Name: ", device.getName_native()));
            return convertView;
        }

        private class ViewHolder
        {
            private TextView name;
        }
    }

}
