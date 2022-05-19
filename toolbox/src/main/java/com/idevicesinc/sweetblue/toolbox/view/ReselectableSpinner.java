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


import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.AdapterView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatSpinner;


public class ReselectableSpinner extends AppCompatSpinner
{

    private boolean m_touched = false;
    private AdapterView.OnItemSelectedListener m_listener;


    public ReselectableSpinner(Context context)
    {
        super(context);
    }

    public ReselectableSpinner(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public ReselectableSpinner(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    @Override public boolean onTouchEvent(MotionEvent event)
    {
        if (event.getAction() == MotionEvent.ACTION_DOWN)
        {
            m_touched = true;
        }
        return super.onTouchEvent(event);
    }

    @Override public void setSelection(int position)
    {
        super.setSelection(position);

        if(position == getSelectedItemPosition() && m_listener != null)
        {
            m_listener.onItemSelected(this, null, position, 0);
        }
    }

    @Override public void setOnItemSelectedListener(@Nullable AdapterView.OnItemSelectedListener listener)
    {
        m_listener = listener;
        super.setOnItemSelectedListener(listener);
    }

    public boolean isTouched()
    {
        return m_touched;
    }

    public void unTouch()
    {
        m_touched = false;
    }

}
