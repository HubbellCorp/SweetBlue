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

package com.idevicesinc.sweetblue.simple_write;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.BleWrite;
import com.idevicesinc.sweetblue.BuildConfig;
import com.idevicesinc.sweetblue.DiscoveryListener;
import com.idevicesinc.sweetblue.LogOptions;
import com.idevicesinc.sweetblue.utils.BleSetupHelper;
import com.idevicesinc.sweetblue.utils.Uuids;
import java.util.UUID;

/**
 * A very simple example which shows how to scan for BLE devices, connect to the first one seen, and then write to a characteristic.
 */
public class MyActivity extends Activity
{
	// This is the UUID of the characteristic we want to write to (make sure you change it to a valid UUID for the device you want to connect to)
	private static final UUID MY_UUID = Uuids.INVALID;									// NOTE: Replace with your actual UUID.

	// There's really no need to keep this up here, it's just here for convenience
	private static final byte[] MY_DATA = {(byte) 0xC0, (byte) 0xFF, (byte) 0xEE};		//  NOTE: Replace with your actual data, not 0xC0FFEE

	// We're keeping an instance of the BleManager around here for convenience, but it's not too necessary, as it's a singleton. But it's helpful so you
	// don't have to keep passing in a Context to retrieve it.
	private BleManager m_bleManager;

	// The instance of the device we're going to connect and write to
	private BleDevice m_bleDevice;

	private Button m_connect;
	private Button m_disconnect;

	// Textview displaying the device name
	private TextView m_name;

	// Textview displaying the current device states
	private TextView m_state;

	private boolean m_discovered = false;


	@Override protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.my_activity);

		m_name = findViewById(R.id.name);
		m_state = findViewById(R.id.state);
		m_connect = findViewById(R.id.connect);
		m_disconnect = findViewById(R.id.disconnect);

		m_connect.setOnClickListener(v ->
		{
            // Disable the connect button when we start trying to connect
            m_connect.setEnabled(false);
            connectDevice();
        });

		m_disconnect.setOnClickListener(v -> m_bleDevice.disconnect());

		// This is just a check to make sure you changed the MY_UUID field. You did, right? :)
		if (MY_UUID.equals(Uuids.INVALID))
			throw new RuntimeException("You need to set a valid UUID for MY_UUID!");


		BleManagerConfig config = new BleManagerConfig();

		// Only enable logging in debug builds
		config.loggingOptions = BuildConfig.DEBUG ? LogOptions.ON : LogOptions.OFF;

		// Get the instance of the BleManager, and pass in the config.
		m_bleManager = BleManager.get(this, config);

		// Set the discovery listener. You can pass in a listener when calling startScan() if you want, but in most cases, this is preferred so you don't
		// have to keep passing in the listener when calling any of the startScan methods.
		m_bleManager.setListener_Discovery(this::processDiscoveryEvent);

		// The BleSetupHelper will take care of requesting the necessary permissions on Android M and above. It needs A> Bluetooth to be on of course,
		// B> Location permissions, and C> Location services enabled in order for a BLE scan to return any results.
		BleSetupHelper.runEnabler(m_bleManager, this, result ->
		{
			if (result.getSuccessful())
				startScanning();
		});

	}

	private void startScanning()
	{
		// Start the scan
		m_bleManager.startScan();
	}

	private void connectDevice()
	{
		m_bleDevice.setListener_State(stateEvent -> {
			m_state.setText(stateEvent.device().printState());

			if (stateEvent.didEnter(BleDeviceState.DISCONNECTED))
			{
				// If the device got disconnected, then we disable the disconnect button, and enable the connect button
				m_connect.setEnabled(true);
				m_disconnect.setEnabled(false);
			}
		});

		// Connect to the device, and pass in a device connect listener, so we know when we are connected and when it fails to connect.
		m_bleDevice.connect(connectEvent ->
		{
			if (connectEvent.wasSuccess())
			{
				Log.i("SweetBlueExample", connectEvent.device().getName_debug() + " just initialized!");

				// Now that we're connected, we enable the disconnect button. It can be argued that the disconnect button should stay enabled until
				// the device is disconnected, so that you could cancel a connect in process.
				m_disconnect.setEnabled(true);

				writeChar();
			}
			else
			{
				// If the connectEvent says it's NOT a retry, then SweetBlue has given up trying to connect, so let's print an error log
				// The ConnectEvent also keeps an instance of the ConnectionFailEvent, so you can find out the reason for the failure.
				if (!connectEvent.isRetrying())
					Log.e("SweetBlueExample", connectEvent.device().getName_debug() + " failed to connect with a status of " + connectEvent.failEvent().status().name());
			}
		});

	}

	private void writeChar()
	{
		BleWrite write = new BleWrite(MY_UUID).setBytes(MY_DATA).setReadWriteListener(readWriteEvent ->
		{
			if (readWriteEvent.wasSuccess())
				Toast.makeText(MyActivity.this, "Write completed successfully!", Toast.LENGTH_LONG).show();
			else
				Toast.makeText(MyActivity.this, "Write failed with a status of " + readWriteEvent.status().toString(), Toast.LENGTH_LONG).show();
		});
		m_bleDevice.write(write);
	}

	private void processDiscoveryEvent(DiscoveryListener.DiscoveryEvent discoveryEvent)
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
			if (discoveryEvent.was(DiscoveryListener.LifeCycle.DISCOVERED))
			{
				// Grab the device from the DiscoveryEvent instance
				m_bleDevice = discoveryEvent.device();

				m_name.setText(m_bleDevice.getName_debug());
				m_state.setText(m_bleDevice.printState());
				connectDevice();
			}
		}
	}


}
