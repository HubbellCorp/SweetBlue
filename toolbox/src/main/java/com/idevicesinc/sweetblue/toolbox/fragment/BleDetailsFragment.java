package com.idevicesinc.sweetblue.toolbox.fragment;


import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProviders;

import com.idevicesinc.sweetblue.DeviceStateListener;
import com.idevicesinc.sweetblue.toolbox.R;
import com.idevicesinc.sweetblue.toolbox.viewmodel.DeviceDetailsViewModel;
import com.idevicesinc.sweetblue.utils.AdvertisingFlag;
import com.idevicesinc.sweetblue.utils.BleScanRecord;
import com.idevicesinc.sweetblue.utils.Utils_String;

import java.util.List;
import java.util.Map;
import java.util.UUID;


public class BleDetailsFragment extends BaseFragment
{

    private TextView m_status;

    private DeviceDetailsViewModel m_viewModel;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View layout = inflater.inflate(R.layout.layout_bledevice_details, null);

        m_viewModel = ViewModelProviders.of(getActivity()).get(DeviceDetailsViewModel.class);

        m_viewModel.getStateEvent().observe(getViewLifecycleOwner(), stateEvent -> {
            if (stateEvent != null)
            {
                onEvent(stateEvent);
            }
        });

        m_status = layout.findViewById(R.id.status);

        m_status.setText(m_viewModel.printState());

        setupViews(layout);

        return layout;
    }

    private void setupViews(final View view)
    {

        TextView nativeName = view.findViewById(R.id.nativeName);

        nativeName.setText(m_viewModel.device().getName_native());

        TextView mac = view.findViewById(R.id.macAddress);
        mac.setText(m_viewModel.device().getMacAddress());

        final BleScanRecord info = m_viewModel.device().getScanInfo();

        TextView txPowerLabel = view.findViewById(R.id.txPowerLabel);
        TextView txPower = view.findViewById(R.id.txPower);
        TextView advNameLabel = view.findViewById(R.id.advNameLabel);
        TextView advName = view.findViewById(R.id.advName);
        TextView serviceUuidLabel = view.findViewById(R.id.serviceUuidLabel);
        TextView serviceUuids = view.findViewById(R.id.serviceUuids);
        TextView manIdLabel = view.findViewById(R.id.manufacturerIdLabel);
        TextView manId = view.findViewById(R.id.manufacturerId);
        TextView manDataLabel = view.findViewById(R.id.manufacturerDataLabel);
        TextView manData = view.findViewById(R.id.manufacturerData);
        TextView advFlagsLabel = view.findViewById(R.id.advFlagsLabel);
        TextView advFlags = view.findViewById(R.id.advFlags);

        if (info.getTxPower().value != null && info.getTxPower().value != -1)
        {
            txPower.setText(String.valueOf(m_viewModel.device().getTxPower()));
        }
        else
        {
            txPower.setVisibility(View.GONE);
            txPowerLabel.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(info.getName()))
        {
            advName.setText(info.getName());
        }
        else
        {
            advNameLabel.setVisibility(View.GONE);
            advName.setVisibility(View.GONE);
        }

        if (info.getServiceUUIDS().size() > 0 || info.getServiceData().size() > 0)
        {
            List<UUID> uuids = info.getServiceUUIDS();
            Map<UUID, byte[]> data = info.getServiceData();
            StringBuilder b = new StringBuilder();
            boolean both = info.getServiceUUIDS().size() > 0 && info.getServiceData().size() > 0;
            for (UUID u : uuids)
            {
                b.append(getString(R.string.uuid_colon)).append(" ").append(u).append("\n");
            }
            if (both)
            {
                b.append("\n");
            }
            for (UUID u : data.keySet())
            {
                b.append(getString(R.string.uuid_colon)).append(" ").append(u).append("\n").append(getString(R.string.data_colon)).append(" ").append(Utils_String.bytesToHexString(data.get(u))).append("\n");
            }
            serviceUuids.setText(b.toString());
        }
        else
        {
            serviceUuidLabel.setVisibility(View.GONE);
            serviceUuids.setVisibility(View.GONE);
        }

        if (info.getManufacturerId() != -1)
        {
            manId.setText(String.valueOf(info.getManufacturerId()));
        }
        else
        {
            manIdLabel.setVisibility(View.GONE);
            manId.setVisibility(View.GONE);
        }

        if (info.getManufacturerData() != null && info.getManufacturerData().length > 0)
        {
            manData.setText(Utils_String.bytesToHexString(info.getManufacturerData()));
        }
        else
        {
            manDataLabel.setVisibility(View.GONE);
            manData.setVisibility(View.GONE);
        }

        if (info.getAdvFlags().value != null && info.getAdvFlags().value != -1)
        {
            StringBuilder b = new StringBuilder();
            for (AdvertisingFlag flag : AdvertisingFlag.values())
            {
                if (flag.overlaps(info.getAdvFlags().value))
                {
                    b.append(flag.name().replace("_", " ")).append("\n");
                }
            }
            advFlags.setText(b.toString());
        }
        else
        {
            advFlagsLabel.setVisibility(View.GONE);
            advFlags.setVisibility(View.GONE);
        }
    }


    public void onEvent(DeviceStateListener.StateEvent e)
    {
        if (m_status != null)
        {
            m_status.setText(m_viewModel.printState());
        }
    }
}
