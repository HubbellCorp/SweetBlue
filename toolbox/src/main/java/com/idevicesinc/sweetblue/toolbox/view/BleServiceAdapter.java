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


import android.app.Activity;
import android.bluetooth.BluetoothGattService;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.TextViewCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleService;
import com.idevicesinc.sweetblue.toolbox.R;
import com.idevicesinc.sweetblue.toolbox.util.UuidUtil;
import java.util.ArrayList;


public class BleServiceAdapter extends ArrayAdapter<BleService>
{

    private ArrayList<BleService> m_serviceList;
    private BleDevice m_device;
    private AdapterView.OnItemClickListener m_itemClickListener;


    public BleServiceAdapter(@NonNull Activity context, @NonNull ArrayList<BleService> serviceList, BleDevice device)
    {
        super(context, R.layout.service_layout, serviceList);

        m_serviceList = serviceList;
        m_device = device;
    }

    public void setOnItemClickListener(AdapterView.OnItemClickListener listner)
    {
        m_itemClickListener = listner;
    }

    @NonNull @Override public View getView(final int position, @Nullable View convertView, @NonNull final ViewGroup parent)
    {
        final ViewHolder h;
        if (convertView == null)
        {
            convertView = View.inflate(getContext(), R.layout.service_layout, null);
            h = new ViewHolder();
            h.name = convertView.findViewById(R.id.name);

            h.uuid = convertView.findViewById(R.id.uuid);
            h.type = convertView.findViewById(R.id.serviceType);

            TextViewCompat.setAutoSizeTextTypeWithDefaults(h.uuid, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);

            convertView.setTag(h);
        }
        else
        {
            h = (ViewHolder) convertView.getTag();
        }

        final BleService service = m_serviceList.get(position);

        h.name.setBleDevice(m_device).setUuid(service.getUuid());

        final View finalview = convertView;

        convertView.setOnClickListener(view -> {
            if (m_itemClickListener != null && !h.name.isEditing())
                m_itemClickListener.onItemClick(null, finalview, position, -1);
        });

        final UuidUtil.Name serviceName = UuidUtil.getServiceName(m_device, service);
        h.name.setName(serviceName.name);
        h.name.setAsCustom(serviceName.custom);

        final String uuid = serviceName.custom ? service.getUuid().toString() : UuidUtil.getShortUuid(service.getUuid());

        h.uuid.setText(uuid);

        final String type = service.getService().getType() == BluetoothGattService.SERVICE_TYPE_PRIMARY ? getContext().getString(R.string.primary_service) : getContext().getString(R.string.secondary_service);
        h.type.setText(type);

        return convertView;
    }


    private final static class ViewHolder
    {
        private NameEditView name;
        private TextView uuid;
        private TextView type;
    }

}
