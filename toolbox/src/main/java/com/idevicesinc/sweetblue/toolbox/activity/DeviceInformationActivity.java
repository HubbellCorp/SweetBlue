package com.idevicesinc.sweetblue.toolbox.activity;


import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProviders;

import com.idevicesinc.sweetblue.toolbox.R;
import com.idevicesinc.sweetblue.toolbox.viewmodel.DeviceInformationViewModel;


public class DeviceInformationActivity extends BaseActivity
{

    private TextView kernelVersion;
    private TextView deviceName;
    private TextView bluetoothName;
    private TextView androidVer;
    private TextView apiVersion;
    private TextView brand;
    private TextView manufacturer;
    private TextView model;
    private TextView product;
    private TextView board;
    private ImageView bleSupported;
    private ImageView lollipopScanSupported;
    private ImageView scanBatchSupported;
    private ImageView multiAdvSupported;

    private DeviceInformationViewModel m_viewModel;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_info);

        Toolbar toolbar = find(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.findViewById(R.id.navBarLogo).setVisibility(View.GONE);

        m_viewModel = ViewModelProviders.of(this).get(DeviceInformationViewModel.class);
        m_viewModel.init(this);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        setTitle(getString(R.string.device_information_title));

        kernelVersion = find(R.id.kernelVersion);
        kernelVersion.setText(m_viewModel.getKernelVersion());

        deviceName = find(R.id.deviceName);

        deviceName.setText(m_viewModel.getDeviceName().getValue());

        m_viewModel.getDeviceName().observe(this, deviceName::setText);


        bluetoothName = find(R.id.bluetoothName);
        bluetoothName.setText(m_viewModel.getBluetoothName());

        androidVer = find(R.id.osVersion);
        androidVer.setText(m_viewModel.getAndroidVersion());

        apiVersion = find(R.id.apiVersion);
        apiVersion.setText(m_viewModel.getApiVersion());

        brand = find(R.id.brand);
        brand.setText(m_viewModel.getBrand());

        manufacturer = find(R.id.manufacturer);
        manufacturer.setText(m_viewModel.getManufacturer());

        model = find(R.id.model);
        model.setText(m_viewModel.getModel());

        product = find(R.id.product);
        product.setText(m_viewModel.getProduct());

        board = find(R.id.board);
        board.setText(m_viewModel.getBoard());

        int supported = R.drawable.icon_check;
        int notsupported = R.drawable.icon_x;

        bleSupported = find(R.id.bleSupported);
        bleSupported.setImageResource(m_viewModel.isBleSupported() ? supported : notsupported);

        lollipopScanSupported = find(R.id.lollipopScanSupported);
        lollipopScanSupported.setImageResource(m_viewModel.isLollipopSupported() ? supported : notsupported);

        scanBatchSupported = find(R.id.scanBatchingSupported);
        scanBatchSupported.setImageResource(m_viewModel.isScanBatchSupported() ? supported : notsupported);

        multiAdvSupported = find(R.id.multipleAdvertisementSupported);
        multiAdvSupported.setImageResource(m_viewModel.isMultiAdvSupported() ? supported : notsupported);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        return true;
    }

    @Override
    public boolean onNavigateUp()
    {
        finish();
        return true;
    }
}
