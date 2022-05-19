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

package com.idevicesinc.sweetblue.toolbox.util;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public abstract class SwipeToDeleteUtil extends ItemTouchHelper.SimpleCallback {


    private Drawable deleteIcon;
    private ColorDrawable backgroundColor;
    private int intrinsicWidth;
    private int intrinsicHeight;
    private Paint clearPaint;

    public SwipeToDeleteUtil(Drawable icon, int color)
    {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);

        deleteIcon = icon;
        backgroundColor = new ColorDrawable();
        backgroundColor.setColor(color);

        if (deleteIcon != null)
        {
            intrinsicWidth = deleteIcon.getIntrinsicWidth();
            intrinsicHeight = deleteIcon.getIntrinsicHeight();
        }

        clearPaint = new Paint();
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder)
    {
        // to disable swipe for certain items return 0 here
        return super.getMovementFlags(recyclerView, viewHolder);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target)
    {
        // don't allow moving rows up and down
        return false;
    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive)
    {
        View itemView = viewHolder.itemView;
        int itemHeight = itemView.getBottom() - itemView.getTop();
        int iconMargin = (itemHeight - intrinsicHeight) / 2;
        boolean isCanceled = dX == 0f && !isCurrentlyActive;

        if (isCanceled)
        {
            clearCanvas(c, itemView.getRight() + dX, (float) itemView.getTop(), (float) itemView.getRight(), (float) itemView.getBottom());
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, false);
            return;
        }

        int itemLeft;
        int itemRight;
        int iconLeft;
        int iconRight;

        if (dX < 0)
        {
            // left
            itemLeft = itemView.getRight() + (int) dX;
            itemRight = itemView.getRight();

            iconLeft = itemView.getRight() - iconMargin - intrinsicWidth;
            iconRight = itemView.getRight() - iconMargin;
        }
        else
        {
            // right
            itemLeft = itemView.getLeft();
            itemRight = itemView.getLeft() + (int) dX;

            iconLeft = itemView.getLeft() + iconMargin;
            iconRight = itemView.getLeft() + iconMargin + intrinsicWidth;
        }

        // draw red background
        backgroundColor.setBounds(itemLeft, itemView.getTop(), itemRight, itemView.getBottom());
        backgroundColor.draw(c);

        // position of delete icon
        int iconTop = itemView.getTop() + (itemHeight - intrinsicWidth) / 2;
        int iconBottom = iconTop + intrinsicHeight;

        // draw delete icon
        deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
        deleteIcon.draw(c);

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    private void clearCanvas(Canvas c, float left, float top, float right, float bottom)
    {
        c.drawRect(left, top, right, bottom, clearPaint);
    }
}
