/*
 * Copyright (C) 2013 XuiMod
 * Based on source obtained from the PACman Project, Copyright (C) 2012
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zst.xposed.xuimod.mods.batterybar;

import com.zst.xposed.xuimod.Common;

import de.robv.android.xposed.XSharedPreferences;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.ColorDrawable;
import android.os.BatteryManager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class BatteryBarView extends RelativeLayout implements Animatable {
    // Total animation duration
    private static final int ANIM_DURATION = 1000; // 1 second
    public static final int STYLE_REGULAR = 0;
    public static final int STYLE_SYMMETRIC = 1;
    
    private boolean mAttached = false;
    private int mBatteryLevel = 0;
    private boolean mBatteryCharging = false;
    private boolean shouldAnimateCharging = true;
    private boolean isAnimating = false;

    LinearLayout mBatteryBarBackground;
    LinearLayout mBatteryBarLayout;
    View mBatteryBar;
    LinearLayout mChargerLayout;
    View mCharger;



    boolean vertical = false;


    public BatteryBarView(Context context) {
    	this(context, null , 0);
    }
    public BatteryBarView(Context context, boolean isCharging, int currentCharge) {
        this(context, null , 0);

        mBatteryLevel = currentCharge;
        mBatteryCharging = isCharging;
    }

    public BatteryBarView(Context context, boolean isCharging, int currentCharge, boolean isVertical) {
        this(context, null,0);

        mBatteryLevel = currentCharge;
        mBatteryCharging = isCharging;
        vertical = isVertical;
    }
    public BatteryBarView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!mAttached) {
            mAttached = true;

            mBatteryBarBackground = new LinearLayout(this.getContext());
            addView(mBatteryBarBackground, new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT));
            
            mBatteryBarLayout = new LinearLayout(this.getContext());
            addView(mBatteryBarLayout, new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT));

            mBatteryBar = new View(this.getContext());
            mBatteryBarLayout.addView(mBatteryBar, new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

            DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
            float dp = 4f;
            int pixels = (int) (metrics.density * dp + 0.5f);

            mChargerLayout = new LinearLayout(this.getContext());

            if (vertical)
                addView(mChargerLayout, new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                        pixels));
            else
                addView(mChargerLayout, new RelativeLayout.LayoutParams(pixels,
                        LayoutParams.MATCH_PARENT));

            mCharger = new View(this.getContext());
            mChargerLayout.setVisibility(View.GONE);
            mChargerLayout.addView(mCharger, new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_BATTERY_CHANGED);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_SCREEN_ON);
			filter.addAction(Common.ACTION_SETTINGS_CHANGED);
            getContext().registerReceiver(mIntentReceiver, filter, null, getHandler());
            updateSettings();
        }
    }
    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                mBatteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                mBatteryCharging = intent.getIntExtra(BatteryManager.EXTRA_STATUS, 0) == BatteryManager.BATTERY_STATUS_CHARGING;
                if (mBatteryCharging && mBatteryLevel < 100) {
                    start();
                } else {
                    stop();
                }
                setProgress(mBatteryLevel);
                updateBatteryColor(null);
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                stop();
            } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                if (mBatteryCharging && mBatteryLevel < 100) {
                    start();
                }
            } else if (Common.ACTION_SETTINGS_CHANGED.equals(action)) {
				updateSettings();
			}
        }
    };

    private void updateSettings() {
        XSharedPreferences pref = new XSharedPreferences(Common.MY_PACKAGE_NAME);
        
        setProgress(mBatteryLevel);
        updateBatteryColor(pref);
        updateBatteryBackground(pref);
        
        boolean oldShouldAnimateCharging = shouldAnimateCharging;
        shouldAnimateCharging = pref.getBoolean(Common.KEY_BATTERYBAR_ANIMATE,
        		Common.DEFAULT_BATTERYBAR_ANIMATE);
        if (oldShouldAnimateCharging == shouldAnimateCharging) return;
        if (mBatteryCharging && mBatteryLevel < 100 && shouldAnimateCharging) {
            start();
        } else {
            stop();
        }
    }
    
    private void setProgress(int n) {
        if (vertical) {
            int w = (int) (((getHeight() / 100.0) * n) + 0.5);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mBatteryBarLayout.getLayoutParams();
            params.height = w;
            mBatteryBarLayout.setLayoutParams(params);
        } else {
            int w = (int) (((getWidth() / 100.0) * n) + 0.5);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mBatteryBarLayout.getLayoutParams();
            params.width = w;
            mBatteryBarLayout.setLayoutParams(params);
        }

    }

    private void updateBatteryColor(XSharedPreferences pref){
    	if (pref==null) pref = new XSharedPreferences(Common.MY_PACKAGE_NAME);
    	
    	String s = "FF33B5E5";
    	if (mBatteryCharging){
            s = pref.getString(Common.KEY_BATTERYBAR_COLOR_CHARGING, Common.DEFAULT_BATTERYBAR_COLOR);
    	}else if(mBatteryLevel <= 20){
            s = pref.getString(Common.KEY_BATTERYBAR_COLOR_20, Common.DEFAULT_BATTERYBAR_COLOR);
    	}else if(mBatteryLevel <= 40){
            s = pref.getString(Common.KEY_BATTERYBAR_COLOR_40, Common.DEFAULT_BATTERYBAR_COLOR);
    	}else if(mBatteryLevel <= 60){
            s = pref.getString(Common.KEY_BATTERYBAR_COLOR_60, Common.DEFAULT_BATTERYBAR_COLOR);
    	}else if(mBatteryLevel <= 80){
            s = pref.getString(Common.KEY_BATTERYBAR_COLOR_80, Common.DEFAULT_BATTERYBAR_COLOR);
    	}else if(mBatteryLevel <= 99){
            s = pref.getString(Common.KEY_BATTERYBAR_COLOR_99, Common.DEFAULT_BATTERYBAR_COLOR);
    	}else if(mBatteryLevel == 100){
            s = pref.getString(Common.KEY_BATTERYBAR_COLOR_100, Common.DEFAULT_BATTERYBAR_COLOR);
    	}
    	int color = Common.parseColorFromString(s, "FF33B5E5");
		fadeBarColor(color, mBatteryBar);
		mCharger.setBackgroundColor(color);
		// No need to fade charger view. Just set it.
		// charge color is constant & will be hidden when discharging
    }
    
	private void fadeBarColor(int newColor, final View view) {
		int oldColor = 0;
		try {
			oldColor = ((ColorDrawable) view.getBackground()).getColor();
		} catch (Exception e) {
			// NullPointerException if background color not found. Just set new
			// color and move on.
			view.setBackgroundColor(newColor);
			return;
		}
		final ValueAnimator anim = ValueAnimator.ofObject(new ArgbEvaluator(), oldColor, newColor);
		final AnimatorUpdateListener listener = new AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animator) {
				view.setBackgroundColor((Integer) animator.getAnimatedValue());
			}
		};
		anim.addUpdateListener(listener);
		anim.setDuration(1000);
		anim.start();
	}

    public void updateBatteryBackground(XSharedPreferences pref){
        String colorString = pref.getString(Common.KEY_BATTERYBAR_BACKGROUND_COLOR, "");
        int color = Common.parseColorFromString(colorString, "00000000");
        fadeBarColor(color, mBatteryBarBackground);
    }
    
    @Override
    public void start() {
        if (!shouldAnimateCharging)  return;

        if (vertical) {
            TranslateAnimation a = new TranslateAnimation(getX(), getX(), getHeight(),
                    mBatteryBarLayout.getHeight());
            a.setInterpolator(new AccelerateInterpolator());
            a.setDuration(ANIM_DURATION);
            a.setRepeatCount(Animation.INFINITE);
            mChargerLayout.startAnimation(a);
            mChargerLayout.setVisibility(View.VISIBLE);
        } else {
            TranslateAnimation a = new TranslateAnimation(getWidth(), mBatteryBarLayout.getWidth(),
                    getTop(), getTop());
            a.setInterpolator(new AccelerateInterpolator());
            a.setDuration(ANIM_DURATION);
            a.setRepeatCount(Animation.INFINITE);
            mChargerLayout.startAnimation(a);
            mChargerLayout.setVisibility(View.VISIBLE);
        }
        isAnimating = true;
    }
    @Override
    public void stop() {
        mChargerLayout.clearAnimation();
        mChargerLayout.setVisibility(View.GONE);
        isAnimating = false;
    }
    @Override
    public boolean isRunning() {
        return isAnimating;
    }

}