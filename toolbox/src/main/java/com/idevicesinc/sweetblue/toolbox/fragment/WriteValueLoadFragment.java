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
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;

import com.idevicesinc.sweetblue.toolbox.R;
import com.idevicesinc.sweetblue.toolbox.activity.WriteValueActivity;
import com.idevicesinc.sweetblue.toolbox.viewmodel.WriteValueViewModel;

import java.util.List;


public class WriteValueLoadFragment extends Fragment
{
    WriteValueActivity mParent;

    SavedValueAdapter mAdapter;
    ListView mListView;
    List<WriteValueViewModel.SavedValue> mSavedValueList = null;
    WriteValueViewModel.SavedValue mSelectedValue = null;

    public static WriteValueLoadFragment newInstance(WriteValueActivity parent, List<WriteValueViewModel.SavedValue> savedValueList)
    {
        WriteValueLoadFragment wvlf = new WriteValueLoadFragment();
        wvlf.mParent = parent;
        wvlf.mSavedValueList = savedValueList;
        return wvlf;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        @SuppressLint("InflateParams") View layout = inflater.inflate(R.layout.layout_write_value_load, null);

        mListView = layout.findViewById(R.id.savedValueListView);

        mListView.setOnItemClickListener((parent, view, position, id) -> {
            view.setBackgroundColor(getResources().getColor(R.color.light_blue));
            mSelectedValue = mAdapter.getItem(position);

            for (int i = 0; i < parent.getChildCount(); ++i)
            {
                View child = parent.getChildAt(i);
                if (child == null)
                    continue;

                int pos = parent.getFirstVisiblePosition() + i;
                child.setBackgroundColor(getResources().getColor(pos == position ? R.color.very_light_blue : R.color.white));
            }

        });

        if (mSavedValueList != null)
        {
            mAdapter = new SavedValueAdapter(getContext(), mSavedValueList);
            mListView.setAdapter(mAdapter);
        }

        return layout;
    }

    public WriteValueViewModel.SavedValue getSelectedValue()
    {
        return mSelectedValue;
    }

    private class SavedValueAdapter extends ArrayAdapter<WriteValueViewModel.SavedValue>
    {
        public SavedValueAdapter(Context context, List<WriteValueViewModel.SavedValue> savedValues)
        {
            super(context, 0, savedValues);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            final WriteValueViewModel.SavedValue sv = getItem(position);
            String names[] = getResources().getStringArray(R.array.gatt_format_type_names);

            if (convertView == null)
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.saved_value_layout, parent, false);

            TextView valueNameLabel = convertView.findViewById(R.id.valueNameLabel);
            TextView valueTypeLabel = convertView.findViewById(R.id.valueTypeLabel);
            TextView valueLabel = convertView.findViewById(R.id.valueLabel);

            valueNameLabel.setText(sv.getName());
            valueTypeLabel.setText(names[sv.getGATTFormatType().ordinal()]);
            valueLabel.setText(sv.getValueString());

            convertView.setBackgroundColor(getResources().getColor(sv.equals(mSelectedValue) ? R.color.very_light_blue : R.color.white));

            // Bind (or re-bind) the fake oveflow menu
            View v = convertView.findViewById(R.id.fakeOverflowMenu);
            final View anchor = convertView.findViewById(R.id.fakeOverflowMenuAnchor);
            v.setOnClickListener(v1 -> {
                //Creating the instance of PopupMenu
                PopupMenu popup = new PopupMenu(getContext(), anchor);
                //Inflating the Popup using xml file
                popup.getMenuInflater().inflate(R.menu.saved_value_popup, popup.getMenu());

                //registering popup with OnMenuItemClickListener
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.writeValueEdit) {
                        mParent.editSavedValue(sv);
                    }
                    else if (item.getItemId() == R.id.writeValueDelete) {
                        mParent.deleteSavedValue(sv);
                        mSavedValueList.remove(sv);
                        mAdapter.notifyDataSetChanged();
                        if (sv.equals(mSelectedValue))
                            mSelectedValue = null;
                    }
                    return true;
                });

                popup.show();
            });

            return convertView;
        }
    }

}
