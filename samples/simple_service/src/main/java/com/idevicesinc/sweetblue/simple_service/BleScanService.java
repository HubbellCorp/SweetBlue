package com.idevicesinc.sweetblue.simple_service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleManager;

import java.util.Objects;

import static com.idevicesinc.sweetblue.simple_service.Constants.ACTION_CONNECT;
import static com.idevicesinc.sweetblue.simple_service.Constants.ACTION_DISCONNECT;
import static com.idevicesinc.sweetblue.simple_service.Constants.ACTION_START;
import static com.idevicesinc.sweetblue.simple_service.Constants.ACTION_STOP;
import static com.idevicesinc.sweetblue.simple_service.Constants.EXTRA_MAC_ADDRESS;
import static com.idevicesinc.sweetblue.simple_service.Constants.FROM_NOTIFICATION;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

/**
 * Simple Foreground Service.
 * This will act on two different actions, ACTION_START and ACTION_STOP.
 * ACTION_START: Starts the service in the foreground, creating a notification indicating an ongoing scan.
 * ACTION_STOP: Stops the ongoing scan and closes the foreground service.
 */
public class BleScanService extends Service
{
    private BleManager mBleManager;

    public int onStartCommand(@Nullable Intent intent, int flags, int startId)
    {
        mBleManager = BleManager.get(this);

        String action = null;
        if (intent != null)
        {
            action = intent.getAction();
        }
        if (action != null)
        {
            switch (action)
            {
                case ACTION_START:
                {
                    Intent notificationIntent = new Intent(this, MainActivity.class);
                    notificationIntent.putExtra(FROM_NOTIFICATION, true);
                    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
                    NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "notification_channel_id")
                            .setSmallIcon(R.drawable.settings)
                            .setContentTitle("BLE Scan")
                            .setContentText("Scanning for devices...")
                            .setContentIntent(pendingIntent);
                    Notification notification = builder.build();

                    // For Android 8.0+ a notification channel is required to show anything in the notification drawer.
                    if (Build.VERSION.SDK_INT >= 26)
                    {
                        NotificationChannel channel = new NotificationChannel("notification_channel_id", "BLE Scan", NotificationManager.IMPORTANCE_DEFAULT);
                        channel.setDescription("Description");
                        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        if (notificationManager != null)
                        {
                            notificationManager.createNotificationChannel(channel);
                        }
                    }

                    startForeground(1, notification);

                    mBleManager.startScan();
                    break;
                }
                case ACTION_STOP:
                {
                    mBleManager.stopScan();
                    stopForeground(true);
                    stopSelf();
                    break;
                }
                case ACTION_CONNECT:
                {
                    String macAddress = intent.getStringExtra(EXTRA_MAC_ADDRESS);
                    BleDevice device = mBleManager.getDevice(macAddress);
                    device.connect((connectEvent) ->
                    {
                        // TODO: Do something with the device
                        if (connectEvent.wasSuccess())
                            Log.i("SweetBlueExample", connectEvent.device().getName_debug() + " just initialized!");

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
                    break;
                }
                case ACTION_DISCONNECT:
                {
                    String macAddress = intent.getStringExtra(EXTRA_MAC_ADDRESS);
                    BleDevice device = mBleManager.getDevice(macAddress);
                    if (device.is(BleDeviceState.CONNECTED))
                    {
                        device.disconnect();
                    }
                    break;
                }
            }
        }

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

}
