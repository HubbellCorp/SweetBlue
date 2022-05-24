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

package com.idevicesinc.sweetblue.toolbox.viewmodel;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.ViewModel;
import com.idevicesinc.sweetblue.toolbox.util.BleHelper;
import com.idevicesinc.sweetblue.toolbox.util.UuidUtil;


public class SplashViewModel extends ViewModel
{

    public void init(Context context)
    {
        UuidUtil.makeStrings(context.getApplicationContext());
        BleHelper.get().init(context);
        Log.e("SplashViewModel", "BleHelper init called.");
    }

}
