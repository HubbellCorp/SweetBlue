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

package com.idevicesinc.sweetblue.current_time_server;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import com.idevicesinc.sweetblue.AddServiceListener;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BuildConfig;
import com.idevicesinc.sweetblue.DiscoveryListener;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.ExchangeListener;
import com.idevicesinc.sweetblue.LogOptions;
import com.idevicesinc.sweetblue.ScanFilter;
import com.idevicesinc.sweetblue.BleServer;
import com.idevicesinc.sweetblue.IncomingListener;
import com.idevicesinc.sweetblue.OutgoingListener;
import com.idevicesinc.sweetblue.BleServerState;
import com.idevicesinc.sweetblue.BleStatuses;
import com.idevicesinc.sweetblue.ServerStateListener;
import com.idevicesinc.sweetblue.utils.BleSetupHelper;
import com.idevicesinc.sweetblue.utils.GattDatabase;
import com.idevicesinc.sweetblue.utils.Utils_Time;
import com.idevicesinc.sweetblue.utils.Uuids;

/**
 * This sample demonstrates setting up a server, scanning for a peripheral, connecting to that peripheral, then connecting our local time server
 * to the peripheral (treating it as a "client" as far as time synchronization is concerned), and providing the time to the peripheral when it
 * changes or when it asks for it. It will work with 4.3 and up.
 */
public class MyActivity extends Activity
{
    // The name of the peripheral we'll be looking for during a BLE scan. This is the peripheral we will be connecting to, to provide
    // the time
    private static final String MY_DEVICE_NAME = null; // CHANGE to your device name or a substring thereof.

    // Our instance of BleManager, keeping around for convenience.
    private BleManager m_bleManager;

    // The instance of BleServer we will be using to serve the time
    private BleServer m_bleServer;

    private BroadcastReceiver m_broadcastReceiver = null;



    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Check that required variables were initialized
        if (MY_DEVICE_NAME == null)
            throw new RuntimeException("You need to set a valid device name!");

        BleManagerConfig config = new BleManagerConfig();

        // Only enable logging in debug builds.
        config.loggingOptions = BuildConfig.DEBUG ? LogOptions.ON : LogOptions.OFF;

        m_bleManager = BleManager.get(this, config);

        registerTimeChangedBroadcastReceiver();

        // The BleSetupHelper will take care of requesting the necessary permissions on Android M and above. It needs A> Bluetooth to be on of course,
        // B> Location permissions, and C> Location services enabled in order for a BLE scan to return any results.
        BleSetupHelper.runEnabler(m_bleManager, this, result ->
        {
            if (result.getSuccessful())
            {
                // The GattDatabase class sets up, well, the gatt database for our BleServer. It's setup in the builder style, so you can chain
                // methods together.
                GattDatabase db = new GattDatabase();
                // Add the current time service
                db.addService(Uuids.CURRENT_TIME_SERVICE_UUID)

                        // Now add the local time info characteristic, and it's permissions and properties
                        .addCharacteristic(Uuids.CURRENT_TIME_SERVICE__LOCAL_TIME_INFO).setPermissions().read().setProperties().read().build()/*properties*/.build()/*characteristic*/
                        .addCharacteristic(Uuids.CURRENT_TIME_SERVICE__CURRENT_TIME).setPermissions().read().setProperties().read().notify_prop().build()/*properties*/
                        .addDescriptor(Uuids.CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR_UUID).setPermissions().readWrite().completeService();

                // Set up our incoming listener to listen for explicit read/write requests and respond accordingly.
                // Pass in our gattdatabase instance to have SweetBlue add all services to the server instance
                // We are also adding a listener to know when each service has been added to the BleServer (it's an asynchronous operation)
                m_bleServer = m_bleManager.getServer(MyActivity.this::onIncomingEvent, db, MyActivity.this::onServiceAdd);

                m_bleServer.setListener_Incoming(MyActivity.this::onIncomingEvent);

                // Set up our outgoing listener to confirm data was sent in response to reads/writes.
                m_bleServer.setListener_Outgoing(MyActivity.this::onOutgoing);

                // Set a listener so we know when the server has finished connecting.
                m_bleServer.setListener_State(MyActivity.this::onStateEvent);
            }
        });

    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        if (m_broadcastReceiver != null)
        {
            unregisterReceiver(m_broadcastReceiver);
            m_broadcastReceiver = null;
        }
    }

    // Register a BroadcastReceiver to get notified when the time changes locally, and when it does, update any connected clients.
    private void registerTimeChangedBroadcastReceiver()
    {
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(Intent.ACTION_DATE_CHANGED);

        intentFilter.addAction(Intent.ACTION_TIME_CHANGED);

        intentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);

        // Set up a broadcast receiver to get updates to the phone's time and forward them to the client through BLE notifications.
        m_broadcastReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                if (m_bleServer == null)
                    return;

                m_bleServer.getClients(macAddress ->
                {
                    // We use the "future data" construct here because SweetBlue's job queue might force
                    // this operation to wait (absolute worst case second or two if you're really pounding SweetBlue, but still) a bit
                    // before it actually gets sent out over the air, and we want to send the most recent time.
                    m_bleServer.sendNotification(macAddress, Uuids.CURRENT_TIME_SERVICE__CURRENT_TIME, Utils_Time.getFutureTime());
                });
            }
        };

        registerReceiver(m_broadcastReceiver, intentFilter);
    }

    // Handle incoming events, and respond accordingly.
    private IncomingListener.Please onIncomingEvent(IncomingListener.IncomingEvent incomingEvent)
    {
        if (incomingEvent.target() == ExchangeListener.Target.CHARACTERISTIC)
        {
            if (incomingEvent.charUuid().equals(Uuids.CURRENT_TIME_SERVICE__CURRENT_TIME))
            {
                return IncomingListener.Please.respondWithSuccess(Utils_Time.getFutureTime());
            }
            else if (incomingEvent.charUuid().equals(Uuids.CURRENT_TIME_SERVICE__LOCAL_TIME_INFO))
            {
                return IncomingListener.Please.respondWithSuccess(Utils_Time.getFutureLocalTimeInfo());
            }
        }
        else if (incomingEvent.target() == ExchangeListener.Target.DESCRIPTOR)
        {
            return IncomingListener.Please.respondWithSuccess();
        }

        return IncomingListener.Please.respondWithError(BleStatuses.GATT_ERROR);
    }

    // In a real app you can use this listener to confirm that data was sent -
    // maybe pop up a toast or something to user depending on requirements.
    private void onOutgoing(OutgoingListener.OutgoingEvent outgoingEvent)
    {
        if (outgoingEvent.wasSuccess())
        {
            if (outgoingEvent.type().isNotificationOrIndication())
            {
                Log.i("", "Current time change sent!");
            }
            else
            {
                Log.i("", "Current time or local info request successfully responded to!");
            }
        }
        else
        {
            Log.e("", "Problem sending time change or read request thereof.");
        }
    }

    // Track the state events. Here, we're just printing a log when the server has connected.
    private void onStateEvent(ServerStateListener.StateEvent stateEvent)
    {
        if (stateEvent.didEnter(BleServerState.CONNECTED))
        {
            Log.i("", "Server connected!");
        }
    }

    // Listening for the callback for when a service has successfully been added. Seeing as we're only using one service,
    // we start scanning once we get a success.
    private void onServiceAdd(AddServiceListener.ServiceAddEvent serviceAddEvent)
    {
        if (serviceAddEvent.wasSuccess())
        {
            m_bleManager.startScan(this::onScanFilter, this::onDiscoverEvent);
        }
    }

    // Filter out all devices except for ones which contain MY_DEVICE_NAME, then stop the scan once found.
    private ScanFilter.Please onScanFilter(ScanFilter.ScanEvent scanEvent)
    {
        return ScanFilter.Please.acknowledgeIf(scanEvent.name_normalized().contains(MY_DEVICE_NAME)).thenStopScan();
    }

    // Listen for discovery events. Once it is discovered, we then connect to it, to start the process.
    private void onDiscoverEvent(DiscoveryListener.DiscoveryEvent discoveryEvent)
    {
        if (discoveryEvent.was(DiscoveryListener.LifeCycle.DISCOVERED))
        {
            discoveryEvent.device().connect(connectEvent ->
            {
                if (connectEvent.wasSuccess())
                {
                    // Note that the peripheral may have already connected itself
                    // as a client so this call may be redundant.
                    m_bleServer.connect(connectEvent.device().getMacAddress());
                }
            });
        }
    }
}