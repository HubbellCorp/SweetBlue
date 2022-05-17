package com.idevicesinc.sweetblue.toolbox.activity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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
import com.idevicesinc.sweetblue.DeviceStateListener;
import com.idevicesinc.sweetblue.toolbox.R;
import com.idevicesinc.sweetblue.toolbox.fragment.WriteValueLoadFragment;
import com.idevicesinc.sweetblue.toolbox.fragment.WriteValueNewFragment;
import com.idevicesinc.sweetblue.toolbox.viewmodel.WriteValueViewModel;
import com.idevicesinc.sweetblue.utils.Uuids;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class WriteValueActivity extends BaseActivity
{
    WriteValueViewModel mViewModel;

    // UI configuration
    private TabLayout mTabLayout;
    private ViewPager mViewPager;
    private DetailsTabsAdaptor mPagerAdapter;
    private ArrayList<DeviceDetailsActivity.Listener> mListeners;


    public static final Uuids.GATTFormatType[] sAllowedFormats =
            {
                    Uuids.GATTFormatType.GCFT_boolean,
                    Uuids.GATTFormatType.GCFT_2bit,
                    Uuids.GATTFormatType.GCFT_nibble,
                    Uuids.GATTFormatType.GCFT_uint8,
                    Uuids.GATTFormatType.GCFT_uint12,
                    Uuids.GATTFormatType.GCFT_uint16,
                    Uuids.GATTFormatType.GCFT_uint24,
                    Uuids.GATTFormatType.GCFT_uint32,
                    Uuids.GATTFormatType.GCFT_uint48,
                    Uuids.GATTFormatType.GCFT_uint64,
                    Uuids.GATTFormatType.GCFT_uint128,
                    Uuids.GATTFormatType.GCFT_sint8,
                    Uuids.GATTFormatType.GCFT_sint12,
                    Uuids.GATTFormatType.GCFT_sint16,
                    Uuids.GATTFormatType.GCFT_sint24,
                    Uuids.GATTFormatType.GCFT_sint32,
                    Uuids.GATTFormatType.GCFT_sint48,
                    Uuids.GATTFormatType.GCFT_sint64,
                    Uuids.GATTFormatType.GCFT_sint128,
                    Uuids.GATTFormatType.GCFT_float32,
                    Uuids.GATTFormatType.GCFT_float64,
                    Uuids.GATTFormatType.GCFT_utf8s,
                    Uuids.GATTFormatType.GCFT_utf16s,
                    Uuids.GATTFormatType.GCFT_struct
            };

    private enum Tabs
    {
        New,
        Load
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bleservices);

        mViewModel = ViewModelProviders.of(this).get(WriteValueViewModel.class);

        mListeners = new ArrayList<>(2);

        Toolbar toolbar = find(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.findViewById(R.id.navBarLogo).setVisibility(View.GONE);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        final String mac = getIntent().getStringExtra("mac");
        final String serviceUUID = getIntent().getStringExtra("serviceUUID");
        final String characteristicUUID = getIntent().getStringExtra("characteristicUUID");

        mViewModel.init(mac, serviceUUID, characteristicUUID);

        if (mViewModel.isServiceNull())
        {
            Toast.makeText(this, R.string.need_connection_write_characteristics_error, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (mViewModel.isCharacteristicNull())
        {
            Toast.makeText(this, R.string.need_connection_write_characteristics_error, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        actionBar.setTitle(mViewModel.getCharacteristicName());

        mTabLayout = find(R.id.tabLayout);
        mViewPager = find(R.id.viewPager);
        mPagerAdapter = new DetailsTabsAdaptor(getSupportFragmentManager());

        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener()
        {
            @Override
            public void onTabSelected(TabLayout.Tab tab)
            {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), 0);
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

        mViewPager.setAdapter(mPagerAdapter);
        mTabLayout.setupWithViewPager(mViewPager);

        if (mViewModel.deviceNativeServicesListEmpty())
        {
            mViewPager.setCurrentItem(1);
        }

        mViewModel.loadSavedValues();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.write_value, menu);

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == R.id.write)
        {
            if (mViewPager.getCurrentItem() == Tabs.New.ordinal())
            {
                Fragment f = mPagerAdapter.getFragmentAtPosition(Tabs.New.ordinal());
                WriteValueNewFragment wvnf = (WriteValueNewFragment) f;
                if (wvnf != null)
                {
                    String valueString = wvnf.getValueString();
                    Uuids.GATTFormatType gft = wvnf.getValueGATTFormatType();
                    String saveAsName = wvnf.getSaveAsName();
                    if (saveAsName != null)
                    {
                        mViewModel.addSavedValue(saveAsName, valueString, gft);
                    }

                    writeValue(valueString, gft);
                }
                return true;
            }
            else if (mViewPager.getCurrentItem() == Tabs.Load.ordinal())
            {
                Fragment f = mPagerAdapter.getFragmentAtPosition(Tabs.Load.ordinal());
                WriteValueLoadFragment wvlf = (WriteValueLoadFragment) f;
                if (wvlf != null)
                {
                    WriteValueViewModel.SavedValue sv = wvlf.getSelectedValue();
                    if (sv != null)
                        writeValue(sv.getValueString(), sv.getGATTFormatType());
                    else
                        Toast.makeText(this, R.string.write_value_select_value_toast, Toast.LENGTH_LONG).show();
                }
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy()
    {
        mListeners.clear();
        super.onDestroy();
    }


    private class DetailsTabsAdaptor extends FragmentPagerAdapter
    {
        private WeakReference<Fragment>[] mFragments = new WeakReference[Tabs.values().length];

        DetailsTabsAdaptor(FragmentManager fm)
        {
            super(fm);
        }

        Fragment getFragmentAtPosition(int position)
        {
            try
            {
                return mFragments[position].get();
            }
            catch (Exception e)
            {
                return null;
            }
        }

        @Override
        public Fragment getItem(int position)
        {
            Fragment f = null;
            if (position == Tabs.New.ordinal())
                f = new WriteValueNewFragment();
            else if (position == Tabs.Load.ordinal())
            {
                WriteValueLoadFragment wvlf = WriteValueLoadFragment.newInstance(WriteValueActivity.this, mViewModel.getSavedValues());
                f = wvlf;
            }
            mFragments[position] = new WeakReference<>(f);
            return f;
        }

        @Override
        public int getCount()
        {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position)
        {
            if (position == Tabs.New.ordinal())
                return getString(R.string.write_value_new_tab);
            else if (position == Tabs.Load.ordinal())
                return getString(R.string.write_value_load_tab);
            return null;
        }
    }

    public interface Listener extends DeviceStateListener
    {
    }

    @Override
    public boolean onNavigateUp()
    {
        mViewModel.saveSavedValues();
        setResult(RESULT_CANCELED);
        finish();
        return true;
    }


    private void writeValue(final String valueString, final Uuids.GATTFormatType gft)
    {
        final Dialog d = ProgressDialog.show(WriteValueActivity.this, getString(R.string.write_value_writing_dialog_title), getString(R.string.write_value_writing_dialog_message));

        try
        {
            mViewModel.writeValue(valueString, gft, e ->
            {
                if (e.wasSuccess())
                {
                    // TODO - See if commenting this out breaks anything. The objects held by the wrapper classes should be the same instance
                    // the native android side uses...hence no need to update. But this may not be the case.
                    //                        mCharacteristic.setValue(rawVal);  // Update local cache
                    Toast.makeText(getApplicationContext(), R.string.write_value_writing_success_toast, Toast.LENGTH_LONG).show();
                    mViewModel.saveSavedValues();
                    setResult(RESULT_OK);
                    finish();
                }
                else
                {
                    Toast.makeText(getApplicationContext(), R.string.write_value_writing_fail_toast, Toast.LENGTH_LONG).show();
                }

                d.dismiss();
            });
        }
        catch (Uuids.GATTCharacteristicFormatTypeConversionException e)
        {
            Toast.makeText(getApplicationContext(), getString(R.string.write_value_invalid_value_toast) + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private Fragment getFragmentForTab(Tabs t)
    {
        try
        {
            return mPagerAdapter.getFragmentAtPosition(t.ordinal());
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public void deleteSavedValue(WriteValueViewModel.SavedValue sv)
    {
        mViewModel.deleteSavedValue(sv);
    }

    public void editSavedValue(WriteValueViewModel.SavedValue sv)
    {
        // Change to the new tab
        // Populate it with the saved value
        mViewPager.setCurrentItem(Tabs.New.ordinal());
        Fragment f = getFragmentForTab(Tabs.New);
        WriteValueNewFragment wvnf = (WriteValueNewFragment) f;
        if (wvnf != null)
            wvnf.setFromSavedValue(sv);
    }
}
