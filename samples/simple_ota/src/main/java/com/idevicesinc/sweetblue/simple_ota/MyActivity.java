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

package com.idevicesinc.sweetblue.simple_ota;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.BleTransaction;
import com.idevicesinc.sweetblue.BleWrite;
import com.idevicesinc.sweetblue.BuildConfig;
import com.idevicesinc.sweetblue.DiscoveryListener;
import com.idevicesinc.sweetblue.LogOptions;
import com.idevicesinc.sweetblue.ReadWriteListener;
import com.idevicesinc.sweetblue.utils.BleSetupHelper;
import com.idevicesinc.sweetblue.utils.Uuids;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A very simple example which shows how to scan for BLE devices, connect to the first one seen, and then perform an over-the-air (OTA) firmware update.
 */
public class MyActivity extends Activity
{
    // This is the UUID of the characteristic we want to write to (make sure you change it to a valid UUID for the device you want to connect to)
    private static final UUID MY_UUID = Uuids.BATTERY_LEVEL;  // NOTE: Replace with your actual UUID.

    // There's really no need to keep this up here, it's just here for convenience.
    private static final byte[] MY_DATA = {(byte) 0xC0, (byte) 0xFF, (byte) 0xEE};//  NOTE: Replace with your actual data, not 0xC0FFEE.

    // We're keeping an instance of BleManager for convenience, but it's not really necessary since it's a singleton. It's helpful so you
    // don't have to keep passing in a Context to retrieve it.
    private BleManager m_bleManager;

    // The instance of the device we're going to connect to and write to.
    private BleDevice m_bleDevice;

    private boolean m_discovered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Check that required variables were initialized
        if (MY_UUID.equals(Uuids.INVALID))
            throw new RuntimeException("You need to set a valid UUID for MY_UUID!");

        startScan();
    }

    private void startScan()
    {
        BleManagerConfig config = new BleManagerConfig();
        // Only enable logging in debug builds
        config.loggingOptions = BuildConfig.DEBUG ? LogOptions.ON : LogOptions.OFF;

        // Get the instance of the BleManager, and pass in the config.
        m_bleManager = BleManager.get(this, config);

        // Set the discovery listener. You can pass in a listener when calling startScan() if you want, but in most cases, this is preferred so you don't
        // have to keep passing in the listener when calling any of the startScan methods.
        m_bleManager.setListener_Discovery(this::onDeviceDiscovered);

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

    private void onDeviceDiscovered(DiscoveryListener.DiscoveryEvent discoveryEvent)
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

                connectToDevice();
            }
        }
    }

    private void connectToDevice()
    {
        // Connect to the device, and pass in a device connect listener, so we know when we are connected, and also to know if/when the connection failed.
        // In this instance, we're only worried about when the connection fails, and SweetBlue has given up trying to connect.
        m_bleDevice.connect(connectEvent ->
        {
            if (connectEvent.wasSuccess())
            {
                Log.i("SweetBlueExample", connectEvent.device().getName_debug() + " just got connected and is ready to use!");

                final ArrayList<byte[]> writeQueue = new ArrayList<>();

                writeQueue.add(MY_DATA);
                writeQueue.add(MY_DATA);
                writeQueue.add(MY_DATA);
                writeQueue.add(MY_DATA);

                connectEvent.device().performOta(new SimpleOtaTransaction(writeQueue));
            }
            else
            {
                if (!connectEvent.isRetrying())
                {
                    // If the connectEvent says it's NOT a retry, then SweetBlue has given up trying to connect, so let's print an error log
                    // The ConnectEvent also keeps an instance of the ConnectionFailEvent, so you can find out the reason for the failure.
                    Log.e("SweetBlueExample", connectEvent.device().getName_debug() + " failed to connect with a status of " + connectEvent.failEvent().status().name());
                }
            }
        });

    }

    // A simple implementation of an OTA transaction class. This simply holds a list of byte arrays. Each array will be sent in it's own
    // write operation.
    private static class SimpleOtaTransaction extends BleTransaction.Ota
    {
        // Our list of byte arrays to be sent to the device
        private final List<byte[]> m_dataQueue;

        // The current index we're on in the list
        private int m_currentIndex = 0;

        // A ReadWriteListener for listening to the result of each write.
        private final ReadWriteListener m_readWriteListener = readWriteEvent ->
        {
            // If the last write was a success, go ahead and move on to the next one
            if (readWriteEvent.wasSuccess())
                doNextWrite();
            else
            {
                // When running a transaction, you must remember to call succeed(), or fail() to release the queue for other operations to be
                // performed.
                fail();
            }
        };

        // Cache an instance of BleWrite, then we simply change the data we're sending.
        private final BleWrite m_bleWrite = new BleWrite(MY_UUID).setReadWriteListener(m_readWriteListener);

        public SimpleOtaTransaction(final List<byte[]> dataQueue)
        {
            m_dataQueue = dataQueue;
        }

        @Override
        protected void start()
        {
            doNextWrite();
        }

        private void doNextWrite()
        {
            if (m_currentIndex == m_dataQueue.size())
            {
                // Now that we've sent all data, we succeed the transaction, so that other operations may be performed on the device.
                succeed();
            }
            else
            {
                final byte[] nextData = m_dataQueue.get(m_currentIndex);
                m_bleWrite.setBytes(nextData);

                // The transaction classes have convenience methods for common operations, such as read, write, enable/disable notifications etc.
                // It's highly recommended you use these methods to enforce proper transaction rules
                write(m_bleWrite);

                m_currentIndex++;
            }
        }
    }
}