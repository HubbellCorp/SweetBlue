package com.idevicesinc.sweetblue.toolbox.view;


import android.content.Context;
import android.content.res.TypedArray;

import androidx.annotation.Nullable;
import androidx.core.widget.TextViewCompat;
import android.text.InputType;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.toolbox.R;
import com.idevicesinc.sweetblue.toolbox.util.UuidUtil;
import com.idevicesinc.sweetblue.toolbox.util.ViewUtil;
import java.util.UUID;


public class NameEditView extends FrameLayout
{

    private final EditText name;
    private final FrameLayout edit;
    private final FrameLayout save;
    private final FrameLayout cancel;
    private BleDevice m_device;
    private UUID m_uuid;
    private boolean m_editing;


    public NameEditView(Context context)
    {
        this(context, null);
    }

    public NameEditView(Context context, @Nullable AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public NameEditView(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);

        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.NameEditView);

        String title = typedArray.getString(R.styleable.NameEditView_title);

        final View layout = View.inflate(context, R.layout.name_edit_layout, null);

        addView(layout);

        name = layout.findViewById(R.id.nameBox);
        if (!TextUtils.isEmpty(title))
            name.setText(title);

        TextViewCompat.setAutoSizeTextTypeWithDefaults(name, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);

        edit = layout.findViewById(R.id.editName);
        save = layout.findViewById(R.id.saveName);
        cancel = layout.findViewById(R.id.cancelEdit);

        typedArray.recycle();

        setAsCustom(false);
    }

    public NameEditView setBleDevice(BleDevice device)
    {
        m_device = device;
        return this;
    }

    public NameEditView setUuid(UUID uuid)
    {
        m_uuid = uuid;
        return this;
    }

    public void setName(String name)
    {
        this.name.setText(name);
    }

    public boolean isEditing()
    {
        return m_editing;
    }

    public void setAsCustom(boolean custom)
    {
        if (custom)
        {
            edit.setVisibility(View.VISIBLE);
            edit.setOnClickListener(view -> activateEditing());
            save.setOnClickListener(view -> {
                // TODO - Add some name checking here to make sure we're not saving null, or empty strings
                if (m_device != null)
                {
                    UuidUtil.saveUuidName(m_device, m_uuid, name.getText().toString());
                    stopEditing();
                }
            });
            cancel.setOnClickListener(view -> stopEditing());
        }
        else
        {
            name.setBackground(null);
            name.setInputType(InputType.TYPE_NULL);
            edit.setVisibility(View.GONE);
            save.setVisibility(View.GONE);
            cancel.setVisibility(View.GONE);
        }
    }

    public void activateEditing()
    {
        m_editing = true;
        edit.setVisibility(View.GONE);
        save.setVisibility(View.VISIBLE);
        cancel.setVisibility(View.VISIBLE);
        ViewUtil.showKeyboard(getContext());
        name.setBackgroundResource(android.R.drawable.editbox_background);
        name.setInputType(InputType.TYPE_CLASS_TEXT);
        name.requestFocus();
    }

    public void stopEditing()
    {
        m_editing = false;
        name.setBackground(null);
        name.setInputType(InputType.TYPE_NULL);
        edit.setVisibility(View.VISIBLE);
        save.setVisibility(View.GONE);
        cancel.setVisibility(View.GONE);
        ViewUtil.hideKeyboard(getContext(), name);
    }
}
