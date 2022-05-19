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

package com.idevicesinc.sweetblue.ble_util;

import android.app.Activity;
import android.os.Bundle;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BondListener;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.DiscoveryListener;
import com.idevicesinc.sweetblue.ScanOptions;
import com.idevicesinc.sweetblue.UhOhListener;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.BleManagerState;
import com.idevicesinc.sweetblue.DeviceStateListener;
import com.idevicesinc.sweetblue.LogOptions;
import com.idevicesinc.sweetblue.ManagerStateListener;
import com.idevicesinc.sweetblue.defaults.DefaultDeviceReconnectFilter;
import com.idevicesinc.sweetblue.utils.BleSetupHelper;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Utils_String;
import java.util.ArrayList;
import java.util.List;


// A slightly more in-depth application to show how to do various operations with SweetBlue.
public class MyActivity extends Activity
{
    // We're keeping an instance of BleManager for convenience, but it's not really necessary since it's a singleton. It's helpful so you
    // don't have to keep passing in a Context to retrieve it.
    private BleManager m_bleManager;

    // Here we maintain a list of all discovered BleDevices so that we can display that later on in a RecyclerView.
    private List<BleDevice> m_bleDeviceList = new ArrayList<>();

    private RecyclerViewAdapter m_recyclerViewAdapter = new RecyclerViewAdapter();

    // We arbitrarily create a scan timeout of 5 seconds.
    private static final Interval SCAN_TIMEOUT = Interval.secs(5.0);



    @Override protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.my_activity);

        BleManagerConfig config = new BleManagerConfig();

        // Only enable logging in debug builds
        config.loggingOptions = BuildConfig.DEBUG ? LogOptions.ON : LogOptions.OFF;

        // Disabling undiscovery so the list doesn't jump around...ultimately a UI problem so should be fixed there eventually.
        config.undiscoveryKeepAlive = Interval.DISABLED;

        // Get the instance of the BleManager, and pass in the config.
        m_bleManager = BleManager.get(this, config);

        // Set a listener for when we get an error, otherwise known as an "UhOh". These UhOhEvents also come with some best-guess suggestions for remedies.
        m_bleManager.setListener_UhOh(this::onUhOh);

        // You must cast this method reference, as there are 2 setListener_State() methods in BleManager (this will change in v3 to only be one)
        m_bleManager.setListener_State(this::onManagerStateEvent);

        // Set the discovery listener. You can pass in a listener when calling startScan() if you want, but in most cases, this is preferred so you don't
        // have to keep passing in the listener when calling any of the startScan methods.
        m_bleManager.setListener_Discovery(this::onDiscovery);

        // The BleSetupHelper will take care of requesting the necessary permissions on Android M and above. It needs A> Bluetooth to be on of course,
        // B> Location permissions, and C> Location services enabled in order for a BLE scan to return any results.
        BleSetupHelper.runEnabler(m_bleManager, this, result ->
        {
            if (result.getSuccessful())
            {
                setEnableButton();

                setDisableButton();

                setUnbondAllButton();

                setNukeButton();

                setScanInfinitelyButton();

                setStopScanButton();

                setScanForFiveSecondsButton();

                setScanPeriodicallyButton();

                setAbstractedStatesTextView();

                setRecyclerView();
            }
        });

    }

    private void setEnableButton()
    {
        final Button enableButton = findViewById(R.id.enableButton);

        enableButton.setOnClickListener(view -> m_bleManager.turnOn());
    }

    private void setDisableButton()
    {
        final Button disableButton = findViewById(R.id.disableButton);

        disableButton.setOnClickListener(view -> m_bleManager.turnOff());
    }

    private void setUnbondAllButton()
    {
        final Button unbondAllButton = findViewById(R.id.unbondAllButton);

        unbondAllButton.setOnClickListener(view -> m_bleManager.unbondAll());
    }

    private void setNukeButton()
    {
        final Button nukeButton = findViewById(R.id.nukeButton);

        nukeButton.setOnClickListener(view -> m_bleManager.reset());
    }

    private void setScanInfinitelyButton()
    {
        final Button scanInfinitelyButton = findViewById(R.id.scanInfinitelyButton);

        scanInfinitelyButton.setOnClickListener(view -> m_bleManager.startScan());
    }

    private void setStopScanButton()
    {
        final Button stopScanButton = findViewById(R.id.stopScanButton);

        stopScanButton.setOnClickListener(view -> m_bleManager.stopScan());
    }


    private void setScanForFiveSecondsButton()
    {
        final Button scanForFiveSecondsButton = findViewById(R.id.scanForFiveSecondsButton);

        int timeout = (int) SCAN_TIMEOUT.secs();

        scanForFiveSecondsButton.setText(getString(R.string.scan_for_x_sec).replace("{{seconds}}", timeout + ""));

        scanForFiveSecondsButton.setOnClickListener(view -> m_bleManager.startScan(SCAN_TIMEOUT));
    }

    private void setScanPeriodicallyButton()
    {
        final Button scanPeriodicallyButton = findViewById(R.id.scanPeriodicallyButton);

        int timeout = (int) SCAN_TIMEOUT.secs();

        scanPeriodicallyButton.setText(getString(R.string.scan_for_x_sec_repeated).replace("{{seconds}}", timeout + ""));

        ScanOptions opts = new ScanOptions().scanPeriodically(SCAN_TIMEOUT, SCAN_TIMEOUT);

        scanPeriodicallyButton.setOnClickListener(view -> m_bleManager.startScan(opts));
    }

    private void setAbstractedStatesTextView()
    {
        final TextView abstractedStatesTextView = findViewById(R.id.abstractedStatesTextView);

        abstractedStatesTextView.setText(Utils_String.makeStateString(BleManagerState.values(), m_bleManager.getStateMask()));
    }

    private void setRecyclerView()
    {
        final RecyclerView recyclerView = findViewById(R.id.recyclerView);

        recyclerView.setAdapter(m_recyclerViewAdapter);

        recyclerView.getItemAnimator().setChangeDuration(0);
    }


    // Adapter for our recyclerview. This hooks up the layout for each device, and sets the click listeners for the relevant buttons.
    private class RecyclerViewAdapter extends RecyclerView.Adapter<ViewHolder>
    {
        @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            final View view = LayoutInflater.from(MyActivity.this).inflate(R.layout.device_cell, parent, false);

            return new ViewHolder(view);
        }

        @Override public void onBindViewHolder(final ViewHolder viewHolder, final int position)
        {
            final BleDevice device = m_bleDeviceList.get(position);

            // Background color.
            viewHolder.contentLinearLayout.setBackgroundColor(getResources().getColor(position % 2 == 0 ? R.color.light_blue : R.color.dark_blue));

            // Device name.
            String name = device.getName_normalized();

            if(name.length() == 0)
            {
                name = device.getMacAddress();
            }
            else
            {
                name += "(" + device.getMacAddress() + ")";
            }

            viewHolder.nameTextView.setText(name);

            // Connect button.
            viewHolder.connectButton.setOnClickListener(view -> device.connect());

            // Disconnect button.
            viewHolder.disconnectButton.setOnClickListener(view -> device.disconnect());

            // Bond button
            viewHolder.bondButton.setOnClickListener(view -> device.bond());

            // Unbond button.
            viewHolder.unbondButton.setOnClickListener(view -> device.unbond());

            // Status.
            viewHolder.statusTextView.setText(Utils_String.makeStateString(BleDeviceState.values(), device.getStateMask()));
        }

        @Override public int getItemCount()
        {
            return m_bleDeviceList.size();
        }
    }

    private final static class ViewHolder extends RecyclerView.ViewHolder
    {
        private LinearLayout contentLinearLayout;

        private TextView nameTextView;

        private Button connectButton;

        private Button disconnectButton;

        private Button bondButton;

        private Button unbondButton;

        private TextView statusTextView;

        private ViewHolder(View view)
        {
            super(view);

            contentLinearLayout = view.findViewById(R.id.contentLinearLayout);

            nameTextView = view.findViewById(R.id.nameTextView);

            connectButton = view.findViewById(R.id.connectButton);

            disconnectButton = view.findViewById(R.id.disconnectButton);

            bondButton = view.findViewById(R.id.bondButton);

            unbondButton = view.findViewById(R.id.unbondButton);

            statusTextView = view.findViewById(R.id.statusTextView);
        }
    }

    private void onUhOh(UhOhListener.UhOhEvent uhOhEvent)
    {
        AlertManager.onEvent(this, uhOhEvent);
    }

    private void onManagerStateEvent(ManagerStateListener.StateEvent stateEvent)
    {
        setAbstractedStatesTextView();
    }

    private void onDiscovery(DiscoveryListener.DiscoveryEvent discoveryEvent)
    {
        if(discoveryEvent.was(DiscoveryListener.LifeCycle.DISCOVERED))
        {
            BleDevice device = discoveryEvent.device();

            m_bleDeviceList.add(device);

            m_recyclerViewAdapter.notifyItemInserted(m_bleDeviceList.size() - 1);

            device.setListener_State(this::onDeviceStateEvent);

            device.setListener_Reconnect(new SimpleConnectionFailListener());

            device.setListener_Bond(this::onBondEvent);
        }
        else if(discoveryEvent.was(DiscoveryListener.LifeCycle.UNDISCOVERED))
        {
            BleDevice device = discoveryEvent.device();

            int position = m_bleDeviceList.indexOf(device);

            m_bleDeviceList.remove(device);

            m_recyclerViewAdapter.notifyItemRemoved(position);

            device.setListener_State(null);

            device.setListener_Reconnect(null);

            device.setListener_Bond(null);
        }
    }

    private void onDeviceStateEvent(DeviceStateListener.StateEvent stateEvent)
    {
        BleDevice device = stateEvent.device();

        int position = m_bleDeviceList.indexOf(device);

        m_recyclerViewAdapter.notifyItemChanged(position);
    }

    private void onBondEvent(BondListener.BondEvent bondEvent)
    {
        final String message = bondEvent.device().getName_debug() + " bond attempt finished with status " + bondEvent.status();

        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }



    private class SimpleConnectionFailListener extends DefaultDeviceReconnectFilter
    {
        @Override
        public ConnectFailPlease onConnectFailed(ConnectFailEvent connectFailEvent)
        {
            ConnectFailPlease please = super.onConnectFailed(connectFailEvent);

            // If the returned please is NOT a retry, then SweetBlue has given up trying to connect, so let's show an error.
            if(!please.isRetry())
            {
                // As the ConnectionFailListener returns a value, it cannot be automatically posted to the main thread. So, we post to the UI thread
                // here to avoid any crashes.
                runOnUiThread(() ->
                {
                    final String message = connectFailEvent.device().getName_debug() + " connection failed with " + connectFailEvent.failureCountSoFar() + " retries - " + connectFailEvent.status();

                    Toast.makeText(MyActivity.this, message, Toast.LENGTH_LONG).show();
                });
            }

            // Return the please from the default listener implementation.
            return please;
        }

    }

}