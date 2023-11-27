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

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProviders;

import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.idevicesinc.sweetblue.toolbox.R;
import com.idevicesinc.sweetblue.toolbox.util.ExportResult;
import com.idevicesinc.sweetblue.toolbox.viewmodel.SettingsViewModel;
import com.idevicesinc.sweetblue.utils.Interval;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class SettingsActivity extends BaseActivity
{
    private final int REQUEST_CODE_EXTERNAL_STORAGE = 101;

    private final int REQUEST_CODE_IMPORT_SETTINGS = 102;


    protected PrefsFragment mCurrentFragment = null;

    private SettingsViewModel m_viewModel;


    public static class PrefsFragment extends PreferenceFragment
    {

        protected boolean mDirty = false;

        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState)
        {
            super.onViewCreated(view, savedInstanceState);

            initPreferences();
        }

        void initPreferences()
        {
            try
            {
                buildSettingsForObject(((SettingsActivity)getActivity()).m_viewModel.getSettingsObject());
            }
            catch (IllegalAccessException e)
            {

            }
        }

        private Preference createPreferenceForField(final Object o, final Field f)
        {
            Preference p = null;

            try
            {
                int modifiers = f.getModifiers();

                if (Modifier.isFinal(modifiers))
                    return null;

                final Type t = f.getGenericType();

                if (t == boolean.class || t == Boolean.class)
                {
                    CheckBoxPreference cbp = new CheckBoxPreference(getActivity());
                    boolean b = (Boolean)f.get(o);
                    cbp.setChecked(b);
                    cbp.setOnPreferenceChangeListener((preference, newValue) -> {
                        try
                        {
                            Boolean val = (Boolean)newValue;
                            if (t == boolean.class)
                                f.setBoolean(o, val);
                            else
                                f.set(o, val);
                            mDirty = true;
                        }
                        catch (Exception e)
                        {
                            return false;
                        }
                        return true;
                    });
                    p = cbp;
                }
                else if (t == short.class || t == Short.class)
                {
                    //TODO
                }
                else if (t == int.class || t == Integer.class)
                {
                    final EditTextPreference etp = new EditTextPreference(getActivity());
                    etp.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
                    etp.setDefaultValue(f.get(o).toString());
                    etp.setSummary(f.get(o).toString());
                    etp.setOnPreferenceChangeListener((preference, newValue) -> {
                        try
                        {
                            Integer i = Integer.valueOf((String)newValue);
                            if (t == int.class)
                                f.setInt(o, i);
                            else
                                f.set(o, i);

                            etp.setSummary(i.toString());

                            mDirty = true;
                        }
                        catch (Exception e)
                        {
                            return false;
                        }
                        return true;
                    });
                    p = etp;
                }
                else if (t == long.class || t == Long.class)
                {
                    //TODO
                }
                else if (t == float.class || t == Float.class)
                {
                    //TODO
                }
                else if (t == double.class || t == Double.class)
                {
                    //TODO
                }
                else if (t == Interval.class)
                {
                    Interval i = (Interval)f.get(o);
                    Double val = Double.valueOf(i.secs());
                    final EditTextPreference etp = new EditTextPreference(getActivity());
                    etp.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
                    etp.setDefaultValue(val.toString());
                    etp.setSummary(getResources().getString(Interval.isDisabled(val) ? R.string.x_seconds_disabled : R.string.x_seconds, val.toString()));
                    etp.setOnPreferenceChangeListener((preference, newValue) -> {
                        try
                        {
                            Double d = Double.valueOf((String)newValue);

                            Interval i1 = Interval.secs(d);
                            f.set(o, i1);

                            etp.setSummary(getResources().getString(Interval.isDisabled(d) ? R.string.x_seconds_disabled : R.string.x_seconds, d.toString()));

                            mDirty = true;
                        }
                        catch (Exception e)
                        {
                            return false;
                        }
                        return true;
                    });
                    p = etp;
                }
                else
                {
                }

                if (p != null)
                    p.setTitle(unHumpCamelCase(f.getName()));
            }
            catch (Exception e)
            {
                p = null;
            }

            return p;
        }

        private void buildSettingsForObject(final Object o) throws IllegalAccessException
        {
            PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(getActivity());
            setPreferenceScreen(screen);

            Class c = o.getClass();
            Field[] fields = c.getFields();
            List<Field> fl = Arrays.asList(fields);
            sortFieldList(fl, o);

            Class prevFieldClass = null;
            PreferenceCategory category = null;

            for (final Field f : fl)
            {
                Preference p = createPreferenceForField(o, f);
                if (p != null)
                {
                    // Add heading, if appropriate
                    if (f.getDeclaringClass() != prevFieldClass)
                    {
                        category = new PreferenceCategory(getActivity());
                        category.setTitle(getResources().getString(R.string.preferences_for, f.getDeclaringClass().getSimpleName()));
                        screen.addPreference(category);
                        prevFieldClass = f.getDeclaringClass();
                    }

                    category.addPreference(p);
                }
            }
        }

        private List<Field> sortFieldList(List<Field> l, Object baseObject)
        {
            // First, make a map of class priorities based on the inheritence tree of the base object
            Class c = baseObject.getClass();

            final Map<Class, Integer> classPriorityMap = new HashMap<>();

            int nextPriority = 1;
            while (c != null)
            {
                classPriorityMap.put(c, nextPriority++);
                c = c.getSuperclass();
            }

            // OK, now sort each field first by priority (ascending) then by name (ascending)
            Collections.sort(l, (o1, o2) -> {
                Class o1Class = o1.getDeclaringClass();
                Class o2Class = o2.getDeclaringClass();

                // First go by class
                Integer o1classPriority = classPriorityMap.get(o1Class);
                Integer o2classPriority = classPriorityMap.get(o2Class);

                if (o1classPriority != o2classPriority)
                    return (o1classPriority - o2classPriority);

                // OK, now go by name
                return o1.getName().compareTo(o2.getName());
            });

            return l;
        }

        private static String unHumpCamelCase(String camelCaseString)
        {
            if (camelCaseString == null)
                return null;
            String[] splits = camelCaseString.split("(?=\\p{Upper})");

            StringBuilder sb = new StringBuilder();
            if (splits.length > 0)
            {
                String first = splits[0];
                first = first.substring(0, 1).toUpperCase(Locale.US) + first.substring(1);
                sb.append(first);
                for (int i = 1; i < splits.length; ++i)
                {
                    sb.append(' ');
                    String split = splits[i];
                    split = split.substring(0, 1).toLowerCase(Locale.US) + split.substring(1);
                    sb.append(split);
                }
            }

            return sb.toString();
        }

        public boolean getIsDirty()
        {
            return mDirty;
        }

        public void clearDirty()
        {
            mDirty = false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = find(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.findViewById(R.id.navBarLogo).setVisibility(View.GONE);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        setTitle(getString(R.string.settings_title));

        m_viewModel = ViewModelProviders.of(this).get(SettingsViewModel.class);

        m_viewModel.init(this);

        createUI();

    }

    protected void createUI()
    {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        PrefsFragment f = new PrefsFragment();
        fragmentTransaction.replace(R.id.fragment_container, f);
        fragmentTransaction.commit();

        mCurrentFragment = f;
    }

    @Override
    public boolean onNavigateUp()
    {


        navigateBack();

        return true;
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed()
    {
        navigateBack();
    }

    private void navigateBack()
    {
        if (mCurrentFragment != null && mCurrentFragment.getIsDirty())
        {
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle(getString(R.string.settings_save_dialog_title));
            alertDialog.setMessage(getString(R.string.settings_save_dialog_message));
            alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel),
                    (dialog, which) -> {
                        dialog.dismiss();
                        finish();
                    });
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.ok),
                    (dialog, which) -> {
                        m_viewModel.saveAndAppySettings();

                        dialog.dismiss();
                        finish();
                    });
            alertDialog.show();
        }
        else
        {
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.settings, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == R.id.importSettings)
            importSettings();
        else if (item.getItemId() == R.id.exportSettingsEmail)
            exportSettingsToEmail();
        else if (item.getItemId() == R.id.exportSettings)
            exportSettingsToDisk();
        else if (item.getItemId() == R.id.resetSettings)
            resetSettings();
        return super.onOptionsItemSelected(item);
    }



    private void importSettings()
    {
        Intent intent = new Intent()
                .setType("*/*")
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setAction(Intent.ACTION_GET_CONTENT);

        startActivityForResult(Intent.createChooser(intent, getString(R.string.select_a_file)), REQUEST_CODE_IMPORT_SETTINGS);
    }

    private void exportSettingsToEmail()
    {
        final Intent emailIntent = new Intent(Intent.ACTION_SEND);

        emailIntent.setDataAndType(Uri.parse("mailto:"), "message/rfc822");

        //emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] { getString(R.string.send_feedback_email_address) });

        //emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.send_feedback_email_subject));

        emailIntent.putExtra(Intent.EXTRA_TEXT, m_viewModel.getSettingsJSON());

        startActivity(Intent.createChooser(emailIntent, getString(R.string.send_feedback_send_mail)));
    }

    private void exportSettingsToDisk()
    {
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE_EXTERNAL_STORAGE);
            return;
        }

        ExportResult result = m_viewModel.exportSettingsToDisk();

        if (!result.wasSuccess())
        {
            Toast.makeText(getApplicationContext(), getString(R.string.settings_toast_unable_to_save), Toast.LENGTH_LONG).show();
            return;
        }

        Intent i = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        i.setData(Uri.fromFile(result.file()));
        sendBroadcast(i);

        String toastMsg = String.format(getString(R.string.settings_toast_saved), result.fileName());
        Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode)
        {
            case REQUEST_CODE_EXTERNAL_STORAGE:
            {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    exportSettingsToDisk();
                }
                return;
            }
        }
    }

    private void resetSettings()
    {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(getString(R.string.settings_reset_dialog_title));
        alertDialog.setMessage(getString(R.string.settings_reset_dialog_message));
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel),
                (dialog, which) -> dialog.dismiss());
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.ok),
                (dialog, which) -> {

                    m_viewModel.resetSettings();

                    createUI();

                    dialog.dismiss();
                });
        alertDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_IMPORT_SETTINGS && resultCode == RESULT_OK)
        {
            final Uri selectedfile = data.getData(); //The uri with the location of the file

            if (!m_viewModel.readAndImportFile(this, selectedfile))
            {
                String messageString = String.format(getString(R.string.settings_toast_unable_to_load), selectedfile.getLastPathSegment());
                Toast.makeText(getApplicationContext(), messageString, Toast.LENGTH_LONG).show();
            }
            else
            {
                createUI();

                String messageString = String.format(getString(R.string.settings_toast_loaded), selectedfile.getLastPathSegment());
                Toast.makeText(getApplicationContext(), messageString, Toast.LENGTH_LONG).show();
            }
        }
    }


}
