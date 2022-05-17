package com.idevicesinc.sweetblue.toolbox.activity;


import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuItemCompat;
import androidx.lifecycle.ViewModelProviders;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import com.idevicesinc.sweetblue.toolbox.R;
import com.idevicesinc.sweetblue.toolbox.viewmodel.LoggerViewModel;
import com.idevicesinc.sweetblue.toolbox.viewmodel.LoggerViewModel.LogLevel;


public class LoggerActivity extends BaseActivity
{

    private SwipeRefreshLayout mSwipeLayout;
    private TextView m_logTextView;

    private LoggerViewModel m_viewModel;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logger);

        m_viewModel = ViewModelProviders.of(this).get(LoggerViewModel.class);

        Toolbar toolbar = find(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        setTitle(getString(R.string.logger_title));

        ImageView logo = find(R.id.navBarLogo);
        logo.setVisibility(View.GONE);

        mSwipeLayout = find(R.id.swipeLayout);

        mSwipeLayout.setOnRefreshListener(m_viewModel::refreshLog);

        m_logTextView = find(R.id.logTextView);

        m_viewModel.getLogStringData().observe(this, stringBuilder -> {
            m_logTextView.setText(stringBuilder);
            if (mSwipeLayout.isRefreshing())
            {
                mSwipeLayout.setRefreshing(false);
            }
        });

        m_viewModel.refreshLog();

    }



    @Override
    public boolean onCreateOptionsMenu(final Menu menu)
    {
        getMenuInflater().inflate(R.menu.logger, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.filter));
        if (searchView != null)
        {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            searchView.setIconifiedByDefault(true);
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener()
            {
                @Override
                public boolean onQueryTextSubmit(String query)
                {
                    m_viewModel.setFilter(query);
                    m_viewModel.refreshLog();
                    MenuItemCompat.collapseActionView(menu.findItem(R.id.filter));
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText)
                {
                    m_viewModel.setFilter(newText);
                    m_viewModel.refreshLog();
                    return true;
                }
            });
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == R.id.filter)
        {
            return true;
        }
        else
        {
            item.setChecked(true);
            switch (item.getItemId())
            {
                case R.id.verbose:
                    m_viewModel.setLogLevel(LogLevel.Verbose);
                    break;
                case R.id.debug:
                    m_viewModel.setLogLevel(LogLevel.Debug);
                    break;
                case R.id.info:
                    m_viewModel.setLogLevel(LogLevel.Info);
                    break;
                case R.id.warn:
                    m_viewModel.setLogLevel(LogLevel.Warn);
                    break;
                case R.id.error:
                    m_viewModel.setLogLevel(LogLevel.Error);
                    break;
                default:
                    return super.onOptionsItemSelected(item);
            }
            m_viewModel.refreshLog();
            return true;
        }
    }

    @Override
    public boolean onNavigateUp()
    {
        finish();
        return true;
    }

}
