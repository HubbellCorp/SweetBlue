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


import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;


public class DialogHelper
{

    private DialogHelper() {}


    public static AlertDialog showRadioGroupDialog(Context context, String title, String message, final String[] choices, int selectedIndex, final RadioGroupListener listener)
    {
        AlertDialog.Builder build = new AlertDialog.Builder(context);
        if (!TextUtils.isEmpty(title))
        {
            build.setTitle(title);
        }
        build.setSingleChoiceItems(choices, selectedIndex, (dialog, which) -> {
            if (which == -1)
            {
                if (listener != null)
                {
                    listener.onChoiceSelected("");
                }
                return;
            }
            if (listener != null)
            {
                listener.onChoiceSelected(choices[which]);
            }
            dialog.dismiss();
        });
        build.setOnCancelListener(dialog -> {
            if (listener != null)
            {
                listener.onCanceled();
            }
        });
        if (!TextUtils.isEmpty(message))
        {
            build.setMessage(message);
        }
        return build.show();
    }

    public interface TextDialogListener
    {
        void onCanceled();
        void onOk(String text);
    }

    public static AlertDialog showNumberEntryDialog(Context context, String title, String message, final TextDialogListener listener)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        if (!TextUtils.isEmpty(title))
            builder.setTitle(title);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        if (!TextUtils.isEmpty(message))
        {
            TextView textView = new TextView(context);
            textView.setText(message);
            layout.addView(textView);
        }
        final EditText editText = new EditText(context);
        editText.setHint("Enter Desired MTU");
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(editText);
        builder.setPositiveButton("OK", (dialog, which) -> {
            String size;
            if (editText.getText() == null)
                size = "";
            else
                size = editText.getText().toString();

            if (listener != null)
                listener.onOk(size);
        });
        builder.setOnCancelListener(dialog -> {
            if (listener != null)
                listener.onCanceled();
        });
        builder.setView(layout);
        return builder.show();
    }


    public interface RadioGroupListener
    {
        void onChoiceSelected(String choice);
        void onCanceled();
    }


}
