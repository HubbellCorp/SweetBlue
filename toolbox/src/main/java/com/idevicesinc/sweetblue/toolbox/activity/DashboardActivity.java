package com.idevicesinc.sweetblue.toolbox.activity;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuItemCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import com.idevicesinc.sweetblue.toolbox.BuildConfig;
import com.idevicesinc.sweetblue.toolbox.R;
import com.idevicesinc.sweetblue.toolbox.util.BleHelper;
import com.idevicesinc.sweetblue.toolbox.util.SwipeToDeleteUtil;
import com.idevicesinc.sweetblue.toolbox.view.DialogHelper;
import com.idevicesinc.sweetblue.toolbox.view.ScanAdapter;
import com.idevicesinc.sweetblue.toolbox.viewmodel.DashboardViewModel;
import com.idevicesinc.sweetblue.utils.BleSetupHelper;


public class DashboardActivity extends BaseActivity
{
    private RecyclerView m_deviceRecycler;
    private ScanAdapter m_adapter;

    private TextView m_scanTextView;
    private ImageView m_scanImageView;

    private DrawerLayout m_drawerLayout;
    private View m_navDrawerLayout;
    private ActionBarDrawerToggle m_drawerToggler;

    private ItemTouchHelper itemTouchHelper;

    private SearchView m_searchView;

    private DashboardViewModel m_viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = find(R.id.toolbar);
        setSupportActionBar(toolbar);

        setTitle("");

        m_viewModel = ViewModelProviders.of(this).get(DashboardViewModel.class);

        m_viewModel.getScanImageRes().observe(this, resId -> m_scanImageView.setImageResource(resId));

        m_viewModel.getScanTextRes().observe(this, resId -> m_scanTextView.setText(resId));

        m_viewModel.init(this);

        m_scanTextView = find(R.id.scanTextView);
        m_scanImageView = find(R.id.scanImageView);

        LinearLayout ll = find(R.id.scanLayout);
        ll.setOnClickListener(v -> {
            if (!m_viewModel.requestScanViewsUpdate())
            {
                BleSetupHelper.runEnabler(BleHelper.get().getMgr(), DashboardActivity.this, result ->
                {
                    if (result.getSuccessful())
                        m_viewModel.requestScanViewsUpdate();
                });
            }
        });

        m_deviceRecycler = find(R.id.recyclerView);

        m_adapter = new ScanAdapter(this, m_viewModel.getDisplayList().getValue());

        m_viewModel.getDisplayList().observe(this, bleDevices -> m_adapter.notifyDataSetChanged());

        m_viewModel.getQueryString().observe(this, query ->
        {
            if (m_searchView != null)
                m_searchView.setQuery(query, false);
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);

        m_deviceRecycler.setAdapter(m_adapter);
        m_deviceRecycler.setLayoutManager(layoutManager);

        Drawable icon = ContextCompat.getDrawable(this, R.drawable.ic_delete_white_24dp);
        int color = ContextCompat.getColor(this, R.color.weber_igrill_red);

        itemTouchHelper = new ItemTouchHelper(new SwipeToDeleteUtil(icon, color)
        {
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction)
            {
                m_viewModel.removeRow(viewHolder.getAdapterPosition());
            }
        });

        m_viewModel.getIsScanning().observe(this, isScanning -> {
            if (isScanning != null)
                toggleItemTouchHelper(isScanning);
        });

        setupNavDrawer();

        BleSetupHelper.runEnabler(BleHelper.get().getMgr(), this, result ->
        {
            if (result.getSuccessful())
                m_viewModel.enablerDone();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu)
    {
        super.onCreateOptionsMenu(menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final MenuItem searchItem = menu.findItem(R.id.scanFilter);
        if (m_searchView == null)
        {
            m_searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        }
        if (m_searchView != null)
        {
            m_searchView.setOnSearchClickListener(v -> m_viewModel.requestQueryString());
            m_searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            m_searchView.setIconifiedByDefault(true);
            m_searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener()
            {
                boolean isClosing = false;

                @Override
                public boolean onQueryTextSubmit(String query)
                {
                    isClosing = true;
                    m_searchView.setQuery("", false);
                    m_searchView.setIconified(true);
                    MenuItemCompat.collapseActionView(searchItem);
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(m_searchView.getWindowToken(), 0);
                    m_viewModel.updateList(query);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText)
                {
                    Log.d("test", "onQTC - " + newText);
                    if (!isClosing)
                    {
                        m_viewModel.updateList(newText);
                    }
                    isClosing = false;
                    return true;
                }
            });
            m_searchView.setOnCloseListener(() ->
            {
                //Log.d("tag, ", "Got closing");
                return false;
            });
            String queryString = m_viewModel.getQueryString().getValue();
            if (queryString != null && queryString.length() > 0)
                m_searchView.setQuery(queryString, true);

        }
        return true;
    }

    @Override
    protected void onResume()
    {
        if (m_adapter != null)
        {
            // For some reason this has to be delayed. If it's not, then it's possible that it misses a state when transitioning
            // back from another screen.
            new Handler().postDelayed(() -> m_adapter.notifyDataSetChanged(), 500);
        }
        super.onResume();
    }

    @Override
    protected void onDestroy()
    {
        if (isFinishing())
        {
            m_viewModel.shutdownBle();
        }
        super.onDestroy();
    }

    // Add or remove the ItemTouchHelper depending on if a scan is ongoing or not
    public void toggleItemTouchHelper(boolean isScanning)
    {
        if (isScanning)
        {
            // Setting to null detaches the ItemTouchHelper from the RecyclerView
            itemTouchHelper.attachToRecyclerView(null);

            Toast.makeText(this, "Swipe to delete is disabled while scanning.", Toast.LENGTH_LONG).show();
        }
        else
            itemTouchHelper.attachToRecyclerView(m_deviceRecycler);
    }

    private void closeNavDrawer()
    {
        m_drawerLayout.closeDrawer(m_navDrawerLayout, true);
    }

    private void setupNavDrawer()
    {
        // Grab the drawer
        m_drawerLayout = find(R.id.drawerLayout);
        m_navDrawerLayout = find(R.id.navigationDrawer);

        m_drawerToggler = new ActionBarDrawerToggle(this, m_drawerLayout, R.string.drawer_open, R.string.drawer_close)
        {
            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView)
            {
                int i;
            }

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view)
            {
                int i;
            }
        };

        m_drawerLayout.addDrawerListener(m_drawerToggler);

        m_drawerLayout.post(() -> m_drawerToggler.syncState());

        m_drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener()
        {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset)
            {
                View focusedView = getCurrentFocus();

                if (focusedView != null)
                {
                    focusedView.clearFocus();

                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

                    imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                }
            }

            @Override
            public void onDrawerOpened(View drawerView)
            {
                m_drawerToggler.syncState();
            }

            @Override
            public void onDrawerClosed(View drawerView)
            {
                m_drawerToggler.syncState();
            }

            @Override
            public void onDrawerStateChanged(int newState)
            {
            }
        });

        // For hamburger menu
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ImageView iv = find(R.id.closeButton);
        iv.setOnClickListener(v -> closeNavDrawer());

        LinearLayout ll = find(R.id.logger);
        ll.setOnClickListener(v ->
        {
            closeNavDrawer();
            Intent intent = new Intent(DashboardActivity.this, LoggerActivity.class);
            startActivity(intent);
        });

        ll = find(R.id.deviceInfo);
        ll.setOnClickListener(v ->
        {
            closeNavDrawer();
            Intent intent = new Intent(DashboardActivity.this, DeviceInformationActivity.class);
            startActivity(intent);
        });

        ll = find(R.id.about);
        ll.setOnClickListener(v ->
        {
            closeNavDrawer();
            launchWebsite();
        });

        ll = find(R.id.settings);
        ll.setOnClickListener(v ->
        {
            closeNavDrawer();
            Intent intent = new Intent(DashboardActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        ll = find(R.id.feedback);
        ll.setOnClickListener(v ->
        {
            closeNavDrawer();
            sendFeedbackEmail();
        });

        TextView tv = find(R.id.appVersion);
        tv.setText(getString(R.string.app_version, BuildConfig.VERSION_NAME));

        tv = find(R.id.sweetBlueVersion);
        String sbVersion = getString(R.string.sweetblue_version_text, getString(R.string.sweetblue_version));
        tv.setText(sbVersion);
    }

    private void launchWebsite()
    {
        // Launch a web view
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.visit_website_url)));

        startActivity(browserIntent);
    }

    private void sendFeedbackEmail()
    {
        final Intent emailIntent = new Intent(Intent.ACTION_SEND);

        emailIntent.setDataAndType(Uri.parse("mailto:"), "message/rfc822");

        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.send_feedback_email_address)});

        emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.send_feedback_email_subject));

        emailIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.send_feedback_email_body));

        startActivity(Intent.createChooser(emailIntent, getString(R.string.send_feedback_send_mail)));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (m_drawerToggler.onOptionsItemSelected(item))
            return true;
        switch (item.getItemId())
        {
            case R.id.scanOptions:
                openScanOptionsDialog();
                return true;
            case R.id.sortOptions:
                openSortOptionsDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openSortOptionsDialog()
    {
        final String[] choices = getResources().getStringArray(R.array.sort_options);
        int current = m_viewModel.getCurrentComparatorIndex();
        DialogHelper.showRadioGroupDialog(this, getString(R.string.sort_by), null, choices, current, new DialogHelper.RadioGroupListener()
        {
            @Override
            public void onChoiceSelected(String choice)
            {
                m_viewModel.updateComparator(DashboardActivity.this, choice);
            }

            @Override
            public void onCanceled()
            {
            }
        });
    }

    private void openScanOptionsDialog()
    {
        String[] choices = getResources().getStringArray(R.array.scan_apis);
        int current;
        switch (m_viewModel.getCurrentScanApi())
        {
            case CLASSIC:
                current = 1;
                break;
            case PRE_LOLLIPOP:
                current = 2;
                break;
            case POST_LOLLIPOP:
                current = 3;
                break;
            default: // Auto
                current = 0;
        }

        DialogHelper.showRadioGroupDialog(this, getString(R.string.select_scan_api), null, choices, current, new DialogHelper.RadioGroupListener()
        {
            @Override
            public void onChoiceSelected(String choice)
            {
                m_viewModel.updateScanApi(DashboardActivity.this, choice);
            }

            @Override
            public void onCanceled()
            {
            }
        });

    }


}
