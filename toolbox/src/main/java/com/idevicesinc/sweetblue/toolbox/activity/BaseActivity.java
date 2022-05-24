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


import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.idevicesinc.sweetblue.toolbox.R;


public abstract class BaseActivity extends AppCompatActivity
{


    <T extends View> T find(int id)
    {
        return findViewById(id);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(getMenuResId(), menu);
        return super.onCreateOptionsMenu(menu);
    }

    public int getMenuResId()
    {
        return R.menu.main;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (!(this instanceof DashboardActivity))
        {
            if (item.getItemId() == android.R.id.home)
            {
                onBackPressed();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

}
