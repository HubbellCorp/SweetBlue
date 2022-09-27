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

package com.idevicesinc.sweetblue.toolbox.view;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDeviceConfig;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.DeviceStateListener;
import com.idevicesinc.sweetblue.toolbox.R;


public class DeviceRow extends FrameLayout
{

    private static final BleDeviceState[] STATES = new BleDeviceState[] { BleDeviceState.BONDING, BleDeviceState.CONNECTED, BleDeviceState.CONNECTING,
            BleDeviceState.DISCONNECTED, BleDeviceState.INITIALIZING, BleDeviceState.AUTHENTICATING, BleDeviceState.DISCOVERING_SERVICES };

    private static String CONNECT;
    private static String DISCONNECT;
    private static String BOND;
    private static String UNBOND;

    private BleDevice m_device;
    private final View m_content;
    private TextView m_name;
    private TextView m_rssi;
    private TextView m_connectTextView;
    private TextView m_bondTextView;
    private ImageView m_connectImageView;
    private ImageView m_bondImageView;


    @SuppressLint("InflateParams")
    public DeviceRow(Context context)
    {
        super(context);
        m_content = LayoutInflater.from(context).inflate(R.layout.device_layout, null);
        addView(m_content);
        if (CONNECT == null)
        {
            CONNECT = context.getString(R.string.connect);
            DISCONNECT = context.getString(R.string.disconnect);
            BOND = context.getString(R.string.bond);
            UNBOND = context.getString(R.string.unbond);
        }
        getViews();
    }

    public void setBleDevice(BleDevice device)
    {
        m_device = device;
        m_device.setListener_State(new StateListener());
        BleDeviceConfig config = m_device.getConfig();
        config.defaultDeviceStates = STATES;
        m_device.setConfig(config);
        m_name.setText(m_device.getName_debug());
        TextViewCompat.setAutoSizeTextTypeWithDefaults(m_name, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
        m_rssi.setText(m_device.getRssiPercent().toString());
        refreshConnectTextView();
        refreshBondTextView();
        updateRSSILabel();
    }

    public void clearDevice()
    {
        m_device.setListener_State((DeviceStateListener) null);
        m_device = null;
    }

    public void undiscover()
    {
        m_device.undiscover();
    }

    public boolean hasDevice()
    {
        return m_device != null;
    }

    public boolean isConnected()
    {
        return hasDevice() && m_device.is(BleDeviceState.INITIALIZED);
    }

    public String macAddress()
    {
        return hasDevice() ? m_device.getMacAddress() : "00:00:00:00:00:00";
    }

    private void refreshConnectTextView()
    {
        // Inspect device state and update labels accordingly
        if (!hasDevice())
            return;

        if (m_device.is(BleDeviceState.DISCONNECTED))
        {
            m_connectTextView.setText(CONNECT);
            m_connectImageView.setColorFilter(ContextCompat.getColor(getContext(), R.color.gray));
            m_connectImageView.setBackgroundResource(R.drawable.grey_ring);
        }
        else
        {
            m_connectTextView.setText(DISCONNECT);
            m_connectImageView.setColorFilter(ContextCompat.getColor(getContext(), R.color.white));

            if (m_device.is(BleDeviceState.CONNECTING))
                m_connectImageView.setBackgroundResource(R.drawable.yellow_circle);
            else
                m_connectImageView.setBackgroundResource(R.drawable.green_circle);
        }
    }

    private void refreshBondTextView()
    {
        // Inspect device state and update labels accordingly
        if (!hasDevice())
            return;

        if (m_device.isAny(BleDeviceState.BONDED, BleDeviceState.BONDING))
        {
            m_bondTextView.setText(UNBOND);
            if (m_device.is(BleDeviceState.BONDING))
            {
                m_bondImageView.setColorFilter(ContextCompat.getColor(getContext(), R.color.white));
                m_bondImageView.setBackgroundResource(R.drawable.yellow_circle);
            }
            else
            {
                m_bondImageView.setColorFilter(ContextCompat.getColor(getContext(), R.color.white));
                m_bondImageView.setBackgroundResource(R.drawable.green_circle);
            }
        }
        else
        {
            m_bondImageView.setColorFilter(ContextCompat.getColor(getContext(), R.color.gray));
            m_bondImageView.setBackgroundResource(R.drawable.grey_ring);
            m_bondTextView.setText(BOND);
        }
    }

    private void getViews()
    {
        m_name = findViewById(R.id.name);
        m_rssi = findViewById(R.id.rssiStatusLabel);

        m_connectImageView = findViewById(R.id.connectImageView);

        m_connectTextView = findViewById(R.id.connectTextView);

        LinearLayout ll = findViewById(R.id.connectLayout);
        ll.setOnClickListener(v -> {
            // Perform action depending on what our text says
            if (m_connectTextView.getText().equals(CONNECT))
                connectClicked();
            else if (m_connectTextView.getText().equals(DISCONNECT))
                disconnectClicked();

            refreshConnectTextView();
        });


        m_bondImageView = findViewById(R.id.bondImageView);

        m_bondTextView = findViewById(R.id.bondTextView);

        ll = findViewById(R.id.bondLayout);
        ll.setOnClickListener(v -> {
            // Perform action depending on what our text says
            if (m_bondTextView.getText().equals(BOND))
                bondClicked();
            else if (m_bondTextView.getText().equals(UNBOND))
                unbondClicked();

            refreshBondTextView();
        });
    }

    private void unbondClicked()
    {
        if (m_device != null)
        {
            m_device.unbond();
        }
    }

    private void bondClicked()
    {
        if (m_device != null)
        {
            m_device.bond();
        }
    }

    private void disconnectClicked()
    {
        if (m_device != null)
        {
            m_device.disconnect();
        }
    }

    private void connectClicked()
    {
        if (m_device != null)
        {
            m_device.connect();
        }
    }

    private final class StateListener implements DeviceStateListener
    {

        @Override public void onEvent(DeviceStateListener.StateEvent e)
        {
            if (m_device != null)
            {
                // TODO - Filter out the current relevant state here, instead of entire state string
                updateStatus(e);

                refreshConnectTextView();

                refreshBondTextView();

            }
        }
    }

    private void updateRSSILabel()
    {
        // See if we're in a state that should show a custom message

        if (m_device.is(BleDeviceState.BONDING))  // This will cover bonding even if disconnected
            m_rssi.setText(R.string.bonding);
        else if (m_device.is(BleDeviceState.INITIALIZED))
            m_rssi.setText(R.string.connected);
        else if (m_device.is(BleDeviceState.INITIALIZING))
            m_rssi.setText(R.string.initializing);
        else if (m_device.is(BleDeviceState.AUTHENTICATING))
            m_rssi.setText(R.string.authenticating);
        else if (m_device.is(BleDeviceState.DISCOVERING_SERVICES))
            m_rssi.setText(R.string.discovering_services);
        else if (m_device.isAny(BleDeviceState.CONNECTING))
            m_rssi.setText(R.string.connecting);
        else
            m_rssi.setText(getResources().getString(R.string.signal_strength_colon, m_device.getRssiPercent().toString()));

        refreshConnectTextView();
        refreshBondTextView();
    }

    private void updateStatus(DeviceStateListener.StateEvent e)
    {
        updateRSSILabel();
    }
}
