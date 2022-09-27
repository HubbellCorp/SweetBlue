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


import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;

import com.idevicesinc.sweetblue.toolbox.R;
import com.idevicesinc.sweetblue.toolbox.util.AppConfig;
import com.idevicesinc.sweetblue.toolbox.viewmodel.SplashViewModel;


// TODO - Use built-in android splash screen for android 12 and higher
@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity
{

    private ImageView mSweetLogo;
    private ImageView mBlueLogo;
    private ImageView mSlogan;
    private ViewGroup mOuterLayout;
    private Handler mHandler;
    private SplashViewModel mViewModel;
    private boolean advanceCalled = false;
    private Runnable advanceRunnable = () ->
    {
        advanceCalled = true;
        advanceToMain();
    };


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        mViewModel = ViewModelProviders.of(this).get(SplashViewModel.class);
        mViewModel.init(getApplicationContext());

        mHandler = new Handler();

        mOuterLayout = findViewById(R.id.outerLayout);
        mOuterLayout.setOnClickListener(v ->
        {
            if (!advanceCalled)
            {
                mHandler.removeCallbacks(advanceRunnable);
                advanceToMain();
            }
        });
        mSweetLogo = findViewById(R.id.sweetLogo);
        mBlueLogo = findViewById(R.id.blueLogo);
        mSlogan = findViewById(R.id.slogan);

        startAnimation();

        // Fire up the config manager
        AppConfig.startup(this);

        mHandler.postDelayed(advanceRunnable, 3000);
    }

    private void startAnimation()
    {
        AnimationSet set1 = new AnimationSet(false);
        TranslateAnimation anim = new TranslateAnimation(TranslateAnimation.RELATIVE_TO_SELF, -2.0f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f);
        anim.setDuration(1000);
        anim.setInterpolator(new BounceInterpolator());
        set1.addAnimation(anim);
        mSweetLogo.startAnimation(set1);
        AnimationSet set2 = new AnimationSet(false);
        TranslateAnimation anim2 = new TranslateAnimation(TranslateAnimation.RELATIVE_TO_SELF, 2.0f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f);
        anim2.setDuration(1000);
        anim2.setInterpolator(new BounceInterpolator());
        set2.addAnimation(anim2);
        mBlueLogo.startAnimation(set2);
        TranslateAnimation anim3 = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, -3f, Animation.RELATIVE_TO_SELF, 0f);
        anim3.setDuration(500);
        anim3.setInterpolator(new DecelerateInterpolator());
        anim3.setStartOffset(1200);
        mSlogan.startAnimation(anim3);
    }

    private void advanceToMain()
    {
        startActivity(new Intent(this, DashboardActivity.class));
        finish();
    }

}
