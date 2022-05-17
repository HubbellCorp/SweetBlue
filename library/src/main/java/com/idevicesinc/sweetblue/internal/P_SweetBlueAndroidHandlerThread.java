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

package com.idevicesinc.sweetblue.internal;

import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;

final class P_SweetBlueAndroidHandlerThread implements P_SweetHandler {

    private HandlerThread mHandlerThread;
    private Handler mHandler;

    P_SweetBlueAndroidHandlerThread(String threadName) {
        mHandlerThread = new HandlerThread(threadName);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    P_SweetBlueAndroidHandlerThread() {
        this("SweetBlue Update Thread");
    }

    Handler getAndroidHandler() {
        return mHandler;
    }

    @Override
    public void post(Runnable action) {
        mHandler.post(action);
    }

    @Override
    public void postDelayed(Runnable action, long delay) {
        mHandler.postDelayed(action, delay);
    }

    @Override
    public void postDelayed(Runnable action, long delay, Object tag) {
        if (Build.VERSION.SDK_INT > 27) {
            mHandler.postDelayed(action, tag, delay);
        } else {
            postDelayed(action, delay);
        }
    }

    @Override
    public void removeCallbacks(Runnable action) {
        mHandler.removeCallbacks(action);
    }

    @Override
    public void removeCallbacks(Object tag) {
        mHandler.removeCallbacksAndMessages(tag);
    }

    @Override
    public void quit() {
        mHandler.removeCallbacksAndMessages(null);
        mHandlerThread.quit();
    }

    @Override
    public Thread getThread() {
        return mHandlerThread.getLooper().getThread();
    }
}
