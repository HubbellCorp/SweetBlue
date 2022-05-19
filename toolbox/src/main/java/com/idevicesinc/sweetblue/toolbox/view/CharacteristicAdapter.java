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

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.widget.TextViewCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.idevicesinc.sweetblue.BleCharacteristic;
import com.idevicesinc.sweetblue.BleDescriptor;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleNotify;
import com.idevicesinc.sweetblue.BleService;
import com.idevicesinc.sweetblue.ReadWriteListener;
import com.idevicesinc.sweetblue.toolbox.R;
import com.idevicesinc.sweetblue.toolbox.activity.CharacteristicsActivity;
import com.idevicesinc.sweetblue.toolbox.device.BleDeviceLogManager;
import com.idevicesinc.sweetblue.toolbox.util.UuidUtil;
import com.idevicesinc.sweetblue.utils.Utils_String;
import com.idevicesinc.sweetblue.utils.Uuids;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CharacteristicAdapter extends BaseExpandableListAdapter
{
    private static String READ;
    private static String WRITE;
    private static String NOTIFY;
    private static String INDICATE;
    private static String BROADCAST;
    private static String SIGNED_WRITE;
    private static String EXTENDED_PROPS;
    private static String WRITE_NO_RESPONSE;


    private CharacteristicsActivity mParent;
    private BleDevice mDevice;
    private BleService mService;
    private Map<BleCharacteristic, List<BleDescriptor>> mCharDescMap;
    private List<BleCharacteristic> mCharacteristicList;

    public CharacteristicAdapter(CharacteristicsActivity parent, @NonNull BleDevice device, @NonNull BleService service, @NonNull List<BleCharacteristic> charList)
    {
        mParent = parent;

        READ = mParent.getString(R.string.read);
        WRITE = mParent.getString(R.string.write);
        NOTIFY = mParent.getString(R.string.notify);
        INDICATE = mParent.getString(R.string.indicate);
        BROADCAST = mParent.getString(R.string.broadcast);
        SIGNED_WRITE = mParent.getString(R.string.signed_write);
        EXTENDED_PROPS = mParent.getString(R.string.extended_properties);
        WRITE_NO_RESPONSE = mParent.getString(R.string.write_no_response);
        mDevice = device;
        mService = service;
        mCharDescMap = new HashMap<>(charList.size());
        mCharacteristicList = charList;

        for (BleCharacteristic ch : charList)
            mCharDescMap.put(ch, ch.getDescriptors());

        Collections.sort(mCharacteristicList, new CharacteristicComparator());
    }

    @Override
    public int getGroupCount()
    {
        return mCharacteristicList.size();
    }

    @Override
    public int getChildrenCount(int groupPosition)
    {
        final BleCharacteristic ch = mCharacteristicList.get(groupPosition);
        final List<BleDescriptor> dList = mCharDescMap.get(ch);
        int count = dList != null ? dList.size() : 0;
        return count;
    }

    @Override
    public BleCharacteristic getGroup(int groupPosition)
    {
        return mCharacteristicList.get(groupPosition);
    }

    @Override
    public BleDescriptor getChild(int groupPosition, int childPosition)
    {
        final BleCharacteristic ch = mCharacteristicList.get(groupPosition);
        final List<BleDescriptor> dList = mCharDescMap.get(ch);
        return dList != null ? dList.get(childPosition) : null;
    }

    @Override
    public long getGroupId(int groupPosition)
    {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition)
    {
        return childPosition;
    }

    @Override
    public boolean hasStableIds()
    {
        return true;
    }

    @Override
    public View getGroupView(final int groupPosition, boolean isExpanded, View convertView, final ViewGroup parent)
    {
        final BleCharacteristic characteristic = mCharacteristicList.get(groupPosition);
        final Context context = parent.getContext();
        final ExpandableListView elv = (ExpandableListView) parent;

        // Figure out how we should format the characteristic
        Uuids.GATTCharacteristic gc = Uuids.GATTCharacteristic.getCharacteristicForUUID(characteristic.getUuid());
        Uuids.GATTDisplayType dt = gc != null ? gc.getDisplayType() : Uuids.GATTDisplayType.Hex;

        final CharViewHolder h;
        if (convertView == null)
        {
            convertView = View.inflate(context, R.layout.characteristic_layout, null);

            h = new CharViewHolder();
            h.parentLayout = convertView.findViewById(R.id.parentLayout);
            h.name = convertView.findViewById(R.id.name);
            h.uuid = convertView.findViewById(R.id.uuid);
            h.uuidOriginalTextSize = h.uuid.getTextSize();
            h.properties = convertView.findViewById(R.id.properties);
            h.valueDisplayTypeLabel = convertView.findViewById(R.id.valueDisplayTypeLabel);
            h.value = convertView.findViewById(R.id.value);
            h.valueReadTime = convertView.findViewById(R.id.valueReadTimeLabel);
            h.displayType = dt;
            h.expandArrow = convertView.findViewById(R.id.expandArrow);
            h.notificationSwitch = convertView.findViewById(R.id.notificationSwitch);
            h.notificationLayout = convertView.findViewById(R.id.notificationLinearLayout);

            h.notifyOverrideState = null;

            // Turn on auto sizing text
            TextViewCompat.setAutoSizeTextTypeWithDefaults(h.uuid, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);

            // We have to do the following to 'fix' clicking on the cell itself
            h.parentLayout.setOnClickListener(v ->
            {
                if (elv.isGroupExpanded(groupPosition))
                    elv.collapseGroup(groupPosition);
                else
                    elv.expandGroup(groupPosition);
            });

            {
                View v = convertView.findViewById(R.id.fakeOverflowMenu);

                final View anchor = convertView.findViewById(R.id.fakeOverflowMenuAnchor);

                v.setOnClickListener(v1 ->
                {
                    //Creating the instance of PopupMenu
                    PopupMenu popup = new PopupMenu(context, anchor);
                    //Inflating the Popup using xml file
                    popup.getMenuInflater().inflate(R.menu.char_value_type_popup, popup.getMenu());

                    popup.getMenu().getItem(h.displayType.ordinal()).setChecked(true);

                    //registering popup with OnMenuItemClickListener
                    popup.setOnMenuItemClickListener(item ->
                    {
                        switch (item.getItemId())
                        {
                            // TODO - Android Gradle Plugin version 8 will break this switch statement
                            // TODO - Will need to figure out an alternative
                            case R.id.displayTypeBoolean:
                                h.displayType = Uuids.GATTDisplayType.Boolean;
                                break;

                            case R.id.displayTypeBitfield:
                                h.displayType = Uuids.GATTDisplayType.Bitfield;
                                break;

                            case R.id.displayTypeUnsignedInteger:
                                h.displayType = Uuids.GATTDisplayType.UnsignedInteger;
                                break;

                            case R.id.displayTypeSignedInteger:
                                h.displayType = Uuids.GATTDisplayType.SignedInteger;
                                break;

                            case R.id.displayTypeDecimal:
                                h.displayType = Uuids.GATTDisplayType.Decimal;
                                break;

                            case R.id.displayTypeString:
                                h.displayType = Uuids.GATTDisplayType.String;
                                break;

                            case R.id.displayTypeHex:
                                h.displayType = Uuids.GATTDisplayType.Hex;
                                break;
                        }

                        //TODO:  Refresh type label

                        refreshValue(context, h, characteristic);
                        return true;
                    });

                    popup.show();
                });
            }

            convertView.setTag(h);
        }
        else
        {
            h = (CharViewHolder) convertView.getTag();
        }

        h.name.setBleDevice(mDevice).setUuid(characteristic.getUuid());

        refreshCharacteristicView(elv, groupPosition, h, characteristic);

        return convertView;
    }

    private void refreshCharacteristicView(final ExpandableListView parent, int groupPosition, final CharViewHolder cvh, final BleCharacteristic bgc)
    {
        final UuidUtil.Name name = UuidUtil.getCharacteristicName(mDevice, bgc);
        final Context context = parent.getContext();

        boolean writable = (bgc.getCharacteristic().getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0;
        boolean readable = (bgc.getCharacteristic().getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) != 0;
        boolean notify = (bgc.getCharacteristic().getProperties() & (BluetoothGattCharacteristic.PROPERTY_NOTIFY | BluetoothGattCharacteristic.PROPERTY_INDICATE)) != 0;

        // Make value editable or not depending on what's allowed
        if (writable)
        {
            cvh.valueDisplayTypeLabel.setVisibility(View.VISIBLE);
            cvh.value.setVisibility(View.VISIBLE);
            cvh.value.setTextColor(context.getResources().getColor(R.color.item_title_blue));
            cvh.value.setOnClickListener(v ->
            {
                // Navigate to the write activity
                mParent.openWriteCharacteristicActivity(mService.getUuid(), bgc.getUuid());
            });
        }
        else if (readable)
        {
            // Adjust format of text
            cvh.valueDisplayTypeLabel.setVisibility(View.VISIBLE);
            cvh.value.setVisibility(View.VISIBLE);
            cvh.value.setTextColor(context.getResources().getColor(R.color.primary_gray));
        }
        else
        {
            // Hide the value area
            cvh.valueDisplayTypeLabel.setVisibility(View.GONE);
            cvh.value.setVisibility(View.GONE);
        }

        cvh.name.setName(name.name);

        cvh.name.setAsCustom(name.custom);

        if (cvh.name.isEditing())
            cvh.name.activateEditing();

        final String uuid;

        if (name.custom)
        {
            uuid = bgc.getUuid().toString();
        }
        else
        {
            uuid = UuidUtil.getShortUuid(bgc.getUuid());
        }
        cvh.uuid.setText(uuid);

        final String properties = getPropertyString(bgc);

        cvh.properties.setText(properties);

        // Update expand arrow
        {
            if (getChildrenCount(groupPosition) < 1)
                cvh.expandArrow.setVisibility(View.GONE);
            else
                cvh.expandArrow.setVisibility(View.VISIBLE);

            boolean expanded = parent.isGroupExpanded(groupPosition);
            cvh.expandArrow.setImageResource(expanded ? R.drawable.ic_expand_less_black_24dp : R.drawable.ic_expand_more_black_24dp);
        }

        // Remove ripple if not clickable
        {
            if (getChildrenCount(groupPosition) < 1)
                cvh.parentLayout.setBackground(null);
        }

        // Update the
        refreshValue(context, cvh, bgc);

        // Hide/show notification toggle
        cvh.notificationLayout.setVisibility(notify ? View.VISIBLE : View.GONE);

        // Update the notification toggle
        boolean checkedState = cvh.notifyOverrideState != null ? cvh.notifyOverrideState : mDevice.isNotifyEnabled(bgc.getUuid());
        cvh.notificationSwitch.setChecked(checkedState);
        cvh.notificationSwitch.setOnClickListener(view ->
        {
            // Toggle notifications on or off
            boolean enabled = !cvh.notificationSwitch.isChecked();

            ReadWriteListener.ReadWriteEvent result = null;
            BleNotify notify1 = new BleNotify(bgc.getUuid()).setNotificationListener(e ->
            {
                // We don't really care what happened, we just need to update the toggle now to reflect the state of the device and re-enable it
                cvh.notificationSwitch.setEnabled(true);
                cvh.notificationSwitch.setChecked(mDevice.isNotifyEnabled(bgc.getUuid()));
                cvh.notifyOverrideState = null;
            });
            if (enabled)
                result = mDevice.disableNotify(notify1);
            else
                result = mDevice.enableNotify(notify1);

            if (result == null || result.isNull())
            {
                // If we were able to toggle to notify, disable the switch until it finishes
                cvh.notificationSwitch.setEnabled(false);
                cvh.notifyOverrideState = !enabled;
            }
        });
    }

    private void refreshValue(Context context, CharViewHolder cvh, BleCharacteristic bgc)
    {
        cvh.valueReadTime.setText("");

        String valueString = cvh.name.getContext().getString(R.string.loading);

        if ((bgc.getCharacteristic().getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) == 0)  //FIXME:  Shouldn't we also be checking for writability here?
            valueString = cvh.name.getContext().getString(R.string.write_value);

        if (bgc.getValue() != null)
        {
            try
            {
                Uuids.GATTDisplayType dt = Uuids.GATTDisplayType.values()[cvh.displayType.ordinal()];

                BleDeviceLogManager.BleDeviceLogEntry bdle = BleDeviceLogManager.getInstance().getLatestEntryForDevice(mDevice, /*BleDeviceLogManager.EventType.Read, */bgc.getUuid().toString());

                if (bdle != null)
                {
                    long ts = bdle.getTimestamp();

                    byte[] val = bdle.getValue();

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                    String timeString = sdf.format(new Date(ts));

                    valueString = dt.toString(/*bgc.getValue()*/ val);

                    timeString = String.format(context.getString(R.string.timestamp_format_string), timeString);

                    cvh.valueReadTime.setText(timeString);
                }
            }
            catch (Exception e)
            {
                valueString = "<" + cvh.name.getContext().getString(R.string.error) + ">";
            }
        }
        cvh.value.setText(valueString);

        cvh.value.setVisibility(View.VISIBLE);
        cvh.valueDisplayTypeLabel.setVisibility(View.VISIBLE);
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent)
    {
        final DescViewHolder h;
        if (convertView == null)
        {
            convertView = View.inflate(parent.getContext(), R.layout.descriptor_layout, null);
            h = new DescViewHolder();
            h.name = convertView.findViewById(R.id.name);
            h.uuid = convertView.findViewById(R.id.uuid);
            h.value = convertView.findViewById(R.id.value);
            convertView.setTag(h);
        }
        else
        {
            h = (DescViewHolder) convertView.getTag();
        }

        final BleCharacteristic characteristic = mCharacteristicList.get(groupPosition);
        final List<BleDescriptor> descList = mCharDescMap.get(characteristic);
        final BleDescriptor descriptor = descList.get(childPosition);

        final UuidUtil.Name name = UuidUtil.getDescriptorName(mDevice, descriptor);
        h.name.setName(name.name);
        h.name.setAsCustom(name.custom);

        final String uuid;
        if (name.custom)
        {
            uuid = descriptor.getUuid().toString();
        }
        else
        {
            uuid = UuidUtil.getShortUuid(descriptor.getUuid());
        }

        // Set the UUID
        h.uuid.setText(uuid);

        // Show value (as hex, for now)
        String hexString = (descriptor != null && descriptor.getValue() != null ? Utils_String.bytesToHexString(descriptor.getValue()) : "<null>");
        h.value.setText(hexString);

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition)
    {
        return false;
    }


    private static String getPropertyString(BleCharacteristic characteristic)
    {
        StringBuilder b = new StringBuilder();
        int propMask = characteristic.getCharacteristic().getProperties();
        if ((propMask & BluetoothGattCharacteristic.PROPERTY_BROADCAST) != 0)
        {
            b.append(BROADCAST);
        }
        if ((propMask & BluetoothGattCharacteristic.PROPERTY_READ) != 0)
        {
            if (b.length() > 0)
            {
                b.append(", ");
            }
            b.append(READ);
        }
        if ((propMask & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0)
        {
            if (b.length() > 0)
            {
                b.append(", ");
            }
            b.append(WRITE_NO_RESPONSE);
        }
        if ((propMask & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0)
        {
            if (b.length() > 0)
            {
                b.append(", ");
            }
            b.append(WRITE);
        }
        if ((propMask & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0)
        {
            if (b.length() > 0)
            {
                b.append(", ");
            }
            b.append(NOTIFY);
        }
        if ((propMask & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0)
        {
            if (b.length() > 0)
            {
                b.append(", ");
            }
            b.append(INDICATE);
        }
        if ((propMask & BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE) != 0)
        {
            if (b.length() > 0)
            {
                b.append(", ");
            }
            b.append(SIGNED_WRITE);
        }
        if ((propMask & BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS) != 0)
        {
            if (b.length() > 0)
            {
                b.append(", ");
            }
            b.append(EXTENDED_PROPS);
        }
        return b.toString();
    }

    private static final class CharViewHolder
    {
        private RelativeLayout parentLayout;
        private NameEditView name;
        private TextView uuid;
        private float uuidOriginalTextSize;
        private TextView properties;
        private TextView valueDisplayTypeLabel;
        private TextView value;
        private TextView valueReadTime;
        private Uuids.GATTDisplayType displayType;
        private ImageView expandArrow;
        private LinearLayout notificationLayout;
        private SwitchCompat notificationSwitch;

        private Boolean notifyOverrideState = null;
        private ReadWriteListener pendingRWListener = null;
    }

    private static final class DescViewHolder
    {
        private NameEditView name;
        private TextView uuid;
        private TextView value;
    }

    private static class CharacteristicComparator implements Comparator<BleCharacteristic>
    {
        public int valueForCharacteristic(BleCharacteristic bgc)
        {
            int value = 0;

            int properties = bgc.getCharacteristic().getProperties();
            if ((properties & BluetoothGattCharacteristic.PROPERTY_READ) != 0)
                value = 3;
            if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0)
                value = value == 0 ? 1 : 2;

            return value;
        }

        public int compare(BleCharacteristic bgc1, BleCharacteristic bgc2)
        {
            return valueForCharacteristic(bgc2) - valueForCharacteristic(bgc1);
        }
    }
}
