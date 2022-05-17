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