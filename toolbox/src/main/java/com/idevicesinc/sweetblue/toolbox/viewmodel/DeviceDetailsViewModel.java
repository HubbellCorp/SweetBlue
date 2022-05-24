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

package com.idevicesinc.sweetblue.toolbox.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleService;
import com.idevicesinc.sweetblue.DeviceStateListener;
import com.idevicesinc.sweetblue.ReadWriteListener;
import com.idevicesinc.sweetblue.toolbox.util.MutablePostLiveData;
import com.idevicesinc.sweetblue.toolbox.util.UuidUtil;

import java.util.ArrayList;
import java.util.UUID;


public class DeviceDetailsViewModel extends ViewModel
{

    private DeviceStateListener m_currentStateListener;
    private MutablePostLiveData<DeviceStateListener.StateEvent> m_stateEvent;

    private MutablePostLiveData<ArrayList<BleService>> m_serviceList;

    private BleDevice m_device;


    public DeviceDetailsViewModel()
    {
        m_stateEvent = new MutablePostLiveData<>();
        m_serviceList = new MutablePostLiveData<>();
        m_serviceList.setValue(new ArrayList<>());
    }


    public void init(BleDevice device)
    {
        m_device = device;
        m_currentStateListener = m_device.getListener_State();
        m_device.setListener_State(e ->
        {
            if (e.didEnter(BleDeviceState.INITIALIZED))
            {
                if (m_serviceList.getValue().size() == 0)
                {
                    m_serviceList.getValue().addAll(m_device.getNativeServices_List());
                    m_serviceList.setValue(m_serviceList.getValue());
                }
            }
            m_stateEvent.setValue(e);
        });
        if (m_serviceList.getValue().size() == 0)
        {
            m_serviceList.getValue().addAll(m_device.getNativeServices_List());
            m_serviceList.setValue(m_serviceList.getValue());
        }
    }


    public MutableLiveData<ArrayList<BleService>> getServiceList()
    {
        return m_serviceList;
    }

    public BleDevice getDevice()
    {
        return m_device;
    }

    public void saveCustomServiceName(UUID serviceUuid, String name)
    {
        UuidUtil.saveUuidName(m_device, serviceUuid, name);
    }

    public boolean isConnectedOrConnecting()
    {
        if (m_device != null)
            return !m_device.is(BleDeviceState.DISCONNECTED);
        else
            return false;
    }

    public boolean isInitializedAndGattAvailable()
    {
        return m_device.is(BleDeviceState.INITIALIZED) && !m_device.is(BleDeviceState.RECONNECTING_SHORT_TERM) &&
                m_device.getNativeGatt() != null;
    }

    public boolean isBonded()
    {
        if (m_device != null)
            return m_device.is(BleDeviceState.BONDED);
        else
            return false;
    }

    public void connect()
    {
        m_device.connect();
    }

    public void disconnect()
    {
        m_device.disconnect();
    }

    public void bond()
    {
        m_device.bond();
    }

    public void unbond()
    {
        m_device.unbond();
    }

    public void negotiateMtu(int mtu, ReadWriteListener listener)
    {
        m_device.negotiateMtu(mtu, listener);
    }

    public String printState()
    {
        if (m_device != null)
            return m_device.printState();
        return "";
    }

    public BleDevice device()
    {
        return m_device;
    }

    public MutableLiveData<DeviceStateListener.StateEvent> getStateEvent()
    {
        return m_stateEvent;
    }


    @Override
    protected void onCleared()
    {
        m_device.setListener_State(m_currentStateListener);
        m_currentStateListener = null;
        m_device = null;
    }
}
