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

package com.idevicesinc.sweetblue.hello_ble;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.BleRead;
import com.idevicesinc.sweetblue.BuildConfig;
import com.idevicesinc.sweetblue.DiscoveryListener;
import com.idevicesinc.sweetblue.LogOptions;
import com.idevicesinc.sweetblue.ReadWriteListener;
import com.idevicesinc.sweetblue.utils.BleSetupHelper;
import com.idevicesinc.sweetblue.utils.Uuids;

/**
 * A very simple example which shows how to scan for BLE devices, connect to the first one seen, and then read it's battery level.
 */
public class MyActivity extends Activity
{
    // We're keeping an instance of BleManager for convenience, but it's not really necessary since it's a singleton. It's helpful so you
    // don't have to keep passing in a Context to retrieve it.
    private BleManager m_bleManager;

    // The instance of the device we're going to connect to, and read it's battery characteristic, if it exists.
    private BleDevice m_device;

    // Button for connecting to the first discovered device.
    private Button m_connect;

    // Button for disconnecting from the currently connected device.
    private Button m_disconnect;

    // TextView for displaying the device name.
    private TextView m_name;

    // TextView for displaying the current device states.
    private TextView m_state;

    // TextView for displaying the battery level, once it has been read.
    private TextView m_battery_level;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.my_activity);

        m_connect = findViewById(R.id.connect);

        m_disconnect = findViewById(R.id.disconnect);

        m_name = findViewById(R.id.name);

        m_state = findViewById(R.id.state);

        m_battery_level = findViewById(R.id.battery_level);

        setConnectButton();

        setDisconnectButton();

        startScan();
    }

    private void setConnectButton()
    {
        m_connect.setOnClickListener(view -> {
            // Disable the connect button when we start trying to connect.
            m_connect.setEnabled(false);

            connectToDevice();
        });
    }

    private void setDisconnectButton()
    {
        m_disconnect.setOnClickListener(view -> m_device.disconnect());
    }

    private void startScan()
    {
        BleManagerConfig config = new BleManagerConfig();

        // Only enable logging in debug builds.
        config.loggingOptions = BuildConfig.DEBUG ? LogOptions.ON : LogOptions.OFF;

        // Get the instance of the BleManager, and pass in the config.
        m_bleManager = BleManager.get(this, config);

        // Set the discovery listener. You can pass in a listener when calling startScan() if you want, but in most cases, this is preferred so you don't
        // have to keep passing in the listener when calling any of the startScan methods.
        m_bleManager.setListener_Discovery(new SimpleDiscoveryListener());

        // The BleSetupHelper will take care of requesting the necessary permissions on Android M and above. It needs A> Bluetooth to be on of course,
        // B> Location permissions, and C> Location services enabled in order for a BLE scan to return any results.
        BleSetupHelper.runEnabler(m_bleManager, this, result ->
        {
            if (result.getSuccessful())
            {
                // Start the scan.
                m_bleManager.startScan();
            }
        });
    }

    // Our simple discovery listener implementation.
    private final class SimpleDiscoveryListener implements DiscoveryListener
    {
        private boolean m_discovered = false;

        @Override
        public void onEvent(DiscoveryEvent discoveryEvent)
        {
            // We're only going to connect to the first device we see, so let's stop the scan now. However, it's possible that more devices have already been discovered and will get piped
            // into the discovery listener, hence the need for the discovered boolean here (on newer API levels the system dispatches devices found in batches)
            if (!m_discovered)
            {
                m_discovered = true;

                // While SweetBlue will automatically stop the scan when you perform any other BLE operation, it's good practice to manually stop the scan here as in this case,
                // we're only concerned with the first device we find. Also, if there is no stopScan method, scanning will resume as soon as all other BLE operations are done.
                m_bleManager.stopScan();

                // We only care about the DISCOVERED event. REDISCOVERED can get posted many times for a single device during a scan.
                if (discoveryEvent.was(LifeCycle.DISCOVERED))
                {
                    // Grab the device from the DiscoveryEvent instance.
                    m_device = discoveryEvent.device();

                    m_name.setText(m_device.getName_debug());

                    m_state.setText(m_device.printState());

                    connectToDevice();
                }
            }
        }
    }

    private void connectToDevice()
    {
        m_device.setListener_State(stateEvent ->
        {
            // Update the device's state in the TextView
            m_state.setText(stateEvent.device().printState());
        });

        // Connect to the device, and pass in a DeviceConnectListener, so we know when we are connected
        // In this same listener, we can find out if the connection failed, and if SweetBlue is going to retry the connection.
        // In this instance, we're only worried about when the connection fails, and SweetBlue has given up trying to connect.
        m_device.connect(connectEvent ->
        {
            if (connectEvent.wasSuccess())
            {
                Log.i("SweetBlueExample", connectEvent.device().getName_debug() + " just initialized!");

                // Now that we're connected, we enable the disconnect button.
                m_disconnect.setEnabled(true);

                // Now that we're connected, we can read the battery level characteristic of the device.
                readBatteryLevelCharacteristic(connectEvent.device());
            }
            else
            {
                if (!connectEvent.isRetrying())
                {
                    // If the connectEvent says it's NOT a retry, then SweetBlue has given up trying to connect, so let's print an error log
                    // The ConnectEvent also keeps an instance of the ConnectionFailEvent, so you can find out the reason for the failure.
                    m_connect.setEnabled(true);

                    m_disconnect.setEnabled(false);

                    Log.e("SweetBlueExample", connectEvent.device().getName_debug() + " failed to connect with a status of " + connectEvent.failEvent().status().name());
                }
            }

        });
    }

    private void readBatteryLevelCharacteristic(BleDevice device)
    {
        // Read the battery level of the device. You don't necessarily have to pass in the service UUID here, as SweetBlue will scan the service database
        // for the characteristic you're looking for, however, it's most efficient to be this explicit so it can avoid having to iterate over everything.
        BleRead read = new BleRead(Uuids.BATTERY_SERVICE_UUID, Uuids.BATTERY_LEVEL).setReadWriteListener(readEvent ->
        {
            if (readEvent.wasSuccess())
            {
                m_battery_level.setText(String.format("%s%%", readEvent.data_byte()));

                Log.i("SweetBlueExample", "Battery level is " + readEvent.data_byte() + "%");
            }
            else
            {
                // If SweetBlue couldn't find the battery service and characteristic, then the device must not have it, or it's in a custom
                // characteristic
                if (readEvent.status() == ReadWriteListener.Status.NO_MATCHING_TARGET)
                    m_battery_level.setText("[No battery characteristic]");

                // There are several possible failures here. This will still get called if the battery service/characteristic is not found.
                Log.e("SweetBlueExample", "Reading battery level failed with status " + readEvent.status().name());
            }
        });
        device.read(read);
    }
}