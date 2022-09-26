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

package com.idevicesinc.sweetblue.toolbox.activity;


import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.DeviceStateListener;
import com.idevicesinc.sweetblue.toolbox.R;
import com.idevicesinc.sweetblue.toolbox.fragment.BleDetailsFragment;
import com.idevicesinc.sweetblue.toolbox.fragment.BleServicesFragment;
import com.idevicesinc.sweetblue.toolbox.util.BleHelper;
import com.idevicesinc.sweetblue.toolbox.view.DialogHelper;
import com.idevicesinc.sweetblue.toolbox.viewmodel.DeviceDetailsViewModel;

import java.math.BigInteger;
import java.util.UUID;

public class DeviceDetailsActivity extends BaseActivity
{
    private TabLayout m_tabLayout;
    private ViewPager m_viewPager;
    private DetailsTabsAdaptor m_pagerAdapter;

    private DeviceDetailsViewModel m_viewModel;


    @Override protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bleservices);

        Toolbar toolbar = find(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.findViewById(R.id.navBarLogo).setVisibility(View.GONE);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        String mac = getIntent().getStringExtra("mac");
        final BleDevice m_device = BleHelper.get().getMgr(this).getDevice(mac);

        m_viewModel = ViewModelProviders.of(this).get(DeviceDetailsViewModel.class);
        m_viewModel.init(m_device);

        String title = m_device.getName_native();
        setTitle(title);

        m_tabLayout = find(R.id.tabLayout);
        m_tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener()
        {
            @Override
            public void onTabSelected(TabLayout.Tab tab)
            {
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab)
            {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab)
            {
            }
        });

        m_viewPager = find(R.id.viewPager);
        m_pagerAdapter = new DetailsTabsAdaptor(getSupportFragmentManager());

        m_viewPager.setAdapter(m_pagerAdapter);
        m_tabLayout.setupWithViewPager(m_viewPager);

        if (m_device.getNativeServices_List() == null || m_device.getNativeServices_List().size() == 0)
        {
            m_viewPager.setCurrentItem(1);
        }

    }

    public void saveUuidName(UUID serviceUuid, String name)
    {
        m_viewModel.saveCustomServiceName(serviceUuid, name);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == R.id.negotiateMtu) {
            showNegotiateMtuDialog();
        }
        else if (item.getItemId() == R.id.connect) {
            m_viewModel.connect();
        }
        else if (item.getItemId() == R.id.disconnect) {
            m_viewModel.disconnect();
        }
        else if (item.getItemId() == R.id.bond) {
            m_viewModel.bond();
        }
        else if (item.getItemId() == R.id.unbond) {
            m_viewModel.unbond();
        }
        return super.onOptionsItemSelected(item);
    }

    private void showNegotiateMtuDialog()
    {
        AlertDialog dialog = DialogHelper.showNumberEntryDialog(this, "Negotiate MTU", null, new DialogHelper.TextDialogListener()
        {
            @Override
            public void onCanceled()
            {
                Toast.makeText(DeviceDetailsActivity.this, "Canceled MTU negotiation.", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onOk(String text)
            {
                BigInteger mtu;

                if (!TextUtils.isEmpty(text))
                {
                    mtu = new BigInteger(text);

                }
                else
                {
                    Toast.makeText(DeviceDetailsActivity.this, "Please enter a valid integer", Toast.LENGTH_LONG).show();

                    return;
                }

                m_viewModel.negotiateMtu(mtu.intValue(), e ->
                {
                    if (e.wasSuccess())
                        Toast.makeText(DeviceDetailsActivity.this, "MTU negotiation completed! MTU size is now " + e.mtu(), Toast.LENGTH_LONG).show();
                    else
                        Toast.makeText(DeviceDetailsActivity.this, "Failed MTU negotiation with status of " + e.status(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        menu.clear();
        getMenuInflater().inflate(getMenuResId(), menu);
        return true;
    }

    @Override
    public int getMenuResId()
    {
        if (m_viewModel != null)
        {
            boolean connected = m_viewModel.isConnectedOrConnecting();
            boolean bonded = m_viewModel.isBonded();
            if (connected)
            {
                if (bonded)
                {
                    return R.menu.connected_bonded;
                }
                return R.menu.connected_unbonded;
            }
            if (bonded)
            {
                return R.menu.disconnected_bonded;
            }
            return R.menu.disconnected_unbonded;
        }
        return super.getMenuResId();
    }


    private class DetailsTabsAdaptor extends FragmentPagerAdapter
    {
        public DetailsTabsAdaptor(FragmentManager fm)
        {
            super(fm);
        }

        @Override
        public Fragment getItem(int position)
        {
            if (position == 0)
            {
                return new BleServicesFragment();
            }
            return new BleDetailsFragment();
        }

        @Override
        public int getCount()
        {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position)
        {
            if (position == 0)
            {
                return getString(R.string.services);
            }
            return getString(R.string.details);
        }
    }

    public interface Listener extends DeviceStateListener
    {
    }
}
