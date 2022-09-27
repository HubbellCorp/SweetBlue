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

package com.idevicesinc.sweetblue.toolbox.fragment;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import com.idevicesinc.sweetblue.BleService;
import com.idevicesinc.sweetblue.DeviceStateListener;
import com.idevicesinc.sweetblue.toolbox.R;
import com.idevicesinc.sweetblue.toolbox.activity.CharacteristicsActivity;
import com.idevicesinc.sweetblue.toolbox.view.BleServiceAdapter;
import com.idevicesinc.sweetblue.toolbox.viewmodel.DeviceDetailsViewModel;


public class BleServicesFragment extends BaseFragment
{

    private ListView m_serviceListView;

    private BleServiceAdapter m_adapter;

    private DeviceDetailsViewModel m_viewModel;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        @SuppressLint("InflateParams") View layout = inflater.inflate(R.layout.layout_service_list, null);

        m_viewModel = new ViewModelProvider(getActivity()).get(DeviceDetailsViewModel.class);

        m_viewModel.getServiceList().observe(getViewLifecycleOwner(), bluetoothGattServices -> m_adapter.notifyDataSetChanged());

        m_adapter = new BleServiceAdapter(getActivity(), m_viewModel.getServiceList().getValue(), m_viewModel.getDevice());

        m_serviceListView = layout.findViewById(R.id.serviceListView);

        m_serviceListView.setAdapter(m_adapter);

        m_serviceListView.setEmptyView(layout.findViewById(android.R.id.empty));

        m_adapter.setOnItemClickListener((parent, view, position, id) -> {
            if (m_viewModel.isInitializedAndGattAvailable())
            {
                final BleService service = m_viewModel.getServiceList().getValue().get(position);
                Intent intent = new Intent(getActivity(), CharacteristicsActivity.class);
                intent.putExtra("mac", m_viewModel.device().getMacAddress());
                intent.putExtra("uuid", service.getUuid().toString());
                startActivity(intent);
            }
            else
            {
                Toast.makeText(getContext(), R.string.need_connection_characteristics_error, Toast.LENGTH_LONG).show();
            }
        });

        m_serviceListView = layout.findViewById(R.id.serviceListView);

        m_serviceListView.setAdapter(m_adapter);

        m_serviceListView.setEmptyView(layout.findViewById(android.R.id.empty));



        return layout;
    }

    public void onEvent(DeviceStateListener.StateEvent e)
    {
//        if (m_device != null && m_adapter != null)
//        {
//            if (m_device.getNativeServices_List() != null && m_device.getNativeServices_List().size() > 0)
//            {
//                m_serviceList.clear();
//                m_serviceList.addAll(m_device.getNativeServices_List());
//                m_adapter.notifyDataSetChanged();
//            }
//        }

    }
}
