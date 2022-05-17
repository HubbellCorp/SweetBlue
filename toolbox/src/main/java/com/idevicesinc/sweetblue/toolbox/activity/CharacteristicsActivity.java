package com.idevicesinc.sweetblue.toolbox.activity;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProviders;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.toolbox.R;
import com.idevicesinc.sweetblue.toolbox.util.BleHelper;
import com.idevicesinc.sweetblue.toolbox.view.CharacteristicAdapter;
import com.idevicesinc.sweetblue.toolbox.viewmodel.CharacteristicsViewModel;
import java.util.UUID;


public class CharacteristicsActivity extends BaseActivity
{
    private static final int WRITE_VALUE_RESULT_CODE = 101;

    private TextView m_noCharacteristicsTextView;

    private CharacteristicAdapter m_adapter;
    private ExpandableListView m_charListView;
    private SwipeRefreshLayout m_swipeRefreshLayout;

    private CharacteristicsViewModel m_viewModel;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_characteristics);

        Toolbar toolbar = find(R.id.toolbar);
        setSupportActionBar(toolbar);

        m_viewModel = ViewModelProviders.of(this).get(CharacteristicsViewModel.class);

        // Hide logo
        toolbar.findViewById(R.id.navBarLogo).setVisibility(View.GONE);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        final String mac = getIntent().getStringExtra("mac");
        final String uuid = getIntent().getStringExtra("uuid");

        final BleDevice m_device = BleHelper.get().getMgr(this).getDevice(mac);

        m_viewModel.init(m_device, UUID.fromString(uuid));


        // If the service is (somehow) null, bail out of the activity
        if (m_viewModel.isServiceNull())
        {
            Toast.makeText(this, R.string.need_connection_characteristics_error, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        String title = m_viewModel.getServiceName();
        setTitle(title);

        m_swipeRefreshLayout = find(R.id.swipeRefreshLayout);

        m_adapter = new CharacteristicAdapter(this, m_device, m_viewModel.getService(), m_viewModel.getCharacteristicList());

        m_charListView = find(R.id.expandingListView);

        m_charListView.setAdapter(m_adapter);

        // Disable native indicator, we will use our own
        m_charListView.setGroupIndicator(null);

        boolean haveCharacteristics = m_viewModel.hasCharacteristics();

        m_charListView.setVisibility(haveCharacteristics ? View.VISIBLE : View.GONE);

        m_noCharacteristicsTextView = find(R.id.noCharacteristicsTextView);

        m_noCharacteristicsTextView.setVisibility(haveCharacteristics ? View.GONE : View.VISIBLE);

        m_swipeRefreshLayout.setOnRefreshListener(() -> m_viewModel.refresh());

        m_viewModel.getReadWriteEvent().observe(this, readWriteEvent ->
        {
            if (readWriteEvent != null)
            {
                if (readWriteEvent.wasSuccess())
                    m_adapter.notifyDataSetChanged();

                if (m_viewModel.isQueueEmpty())
                    m_swipeRefreshLayout.setRefreshing(false);
            }
        });

        m_viewModel.getNotifyEvent().observe(this, notifyEvent -> m_adapter.notifyDataSetChanged() );

    }

    public void openWriteCharacteristicActivity(UUID serviceUUID, UUID characteristicUUID)
    {
        // Navigate to the write activity
        //final BluetoothGattService service = m_serviceList.get(position);
        Intent intent = new Intent(this, WriteValueActivity.class);
        intent.putExtra("mac", m_viewModel.device().getMacAddress());
        intent.putExtra("serviceUUID", serviceUUID.toString());
        intent.putExtra("characteristicUUID", characteristicUUID.toString());
        startActivityForResult(intent, WRITE_VALUE_RESULT_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == WRITE_VALUE_RESULT_CODE && resultCode == RESULT_OK)
            m_adapter.notifyDataSetChanged();
    }

    public int getMenuResId()
    {
        return R.menu.blank_menu;
    }
}