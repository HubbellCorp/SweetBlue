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

package com.idevicesinc.sweetblue.ble_util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.UhOhListener.Remedy;
import com.idevicesinc.sweetblue.UhOhListener.UhOhEvent;


public class AlertManager
{
    public static void onEvent(final Context context, final UhOhEvent uhOhEvent)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        final AlertDialog dialog = builder.create();

        OnClickListener clickListener = (dialog1, which) -> {
            dialog1.dismiss();

            if(which == DialogInterface.BUTTON_POSITIVE)
            {
                BleManager.get(context).reset();
            }
        };

        String title = context.getResources().getString(R.string.uhoh_title);

        title = title.replace("{{reason}}", uhOhEvent.uhOh().name());

        dialog.setTitle(title);

        if(uhOhEvent.uhOh().getRemedy() == Remedy.RESET_BLE)
        {
            dialog.setMessage(context.getResources().getString(R.string.uhoh_message_nuke));

            dialog.setButton(DialogInterface.BUTTON_POSITIVE, context.getResources().getString(R.string.uhoh_message_nuke_drop), clickListener);

            dialog.setButton(DialogInterface.BUTTON_NEGATIVE, context.getResources().getString(R.string.uhoh_message_nuke_cancel), clickListener);
        }
        else if(uhOhEvent.uhOh().getRemedy() == Remedy.RESTART_PHONE)
        {
            dialog.setMessage(context.getResources().getString(R.string.uhoh_message_phone_restart));

            dialog.setButton(DialogInterface.BUTTON_NEUTRAL, context.getResources().getString(R.string.uhoh_message_phone_restart_ok), clickListener);
        }
        else if(uhOhEvent.uhOh().getRemedy() == Remedy.WAIT_AND_SEE)
        {
            dialog.setMessage(context.getResources().getString(R.string.uhoh_message_weirdness));

            dialog.setButton(DialogInterface.BUTTON_NEUTRAL, context.getResources().getString(R.string.uhoh_message_weirdness_ok), clickListener);
        }

        dialog.show();
    }
}