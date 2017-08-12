package de.badaix.pacetracker.preferences;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;

import de.badaix.pacetracker.R;

public class IconListPreference extends AdvancedListPreference {
    private int[] mResourceIds = null;
    private int backgroundDrawableId = -1;
    private ImageView imageView;

    public IconListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference);
        setWidgetLayoutResource(R.layout.preference_widget_imageview);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ImageListPreference);

        if (typedArray.getIndexCount() > 0) {
            String[] imageNames = context.getResources().getStringArray(
                    typedArray.getResourceId(typedArray.getIndexCount() - 1, -1));

            mResourceIds = new int[imageNames.length];

            for (int i = 0; i < imageNames.length; i++) {
                String imageName = imageNames[i].substring(imageNames[i].lastIndexOf('/') + 1,
                        imageNames[i].lastIndexOf('.'));

                mResourceIds[i] = context.getResources().getIdentifier(imageName, "drawable", context.getPackageName());
            }
        }

        typedArray.recycle();
    }

    public void setBackgroundDrawable(int resourceId) {
        backgroundDrawableId = resourceId;
    }

    public void setEntryDrawables(int[] entryDrawables) {
        mResourceIds = entryDrawables.clone();
    }

    public int[] getEntryDrawables() {
        return mResourceIds;
    }

    public void setEntryDrawables(int entriesResId) {
        getContext().getResources().getIntArray(entriesResId);
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        if (mResourceIds == null) {
            throw new IllegalStateException("IconListPreference requires an image ressource id array.");
        }

        int index = findIndexOfValue(getSharedPreferences().getString(getKey(), "0"));

        ListAdapter listAdapter = new ImageArrayAdapter(getContext(), R.layout.image_list_item_single_choice,
                getEntries(), mResourceIds, index);

        // Order matters.
        builder.setAdapter(listAdapter, this);
        super.onPrepareDialogBuilder(builder);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        if ((imageView != null) && (getValue() != null)) {
            imageView.setImageDrawable(getContext().getResources().getDrawable(
                    mResourceIds[findIndexOfValue(getValue())]));
            if (backgroundDrawableId != -1)
                imageView.setBackgroundDrawable(getContext().getResources().getDrawable(backgroundDrawableId));
            // if ((getSummary() == null) || (getSummary().length() == 0))
            // setSummary("");
        }
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        View shell = super.onCreateView(parent);
        imageView = (ImageView) shell.findViewById(R.id.preference_widget_imageview);
        return shell;
    }
    //
    // LinearLayout layout = new LinearLayout(getContext());
    // layout.setOrientation(LinearLayout.HORIZONTAL);
    // layout.setGravity(Gravity.CENTER_VERTICAL);
    // return shell;
    // imageView = new ImageView(getContext());
    // imageView.setScaleType(ScaleType.CENTER_INSIDE);
    // imageView.setId(6666);
    // tvSummary = null;
    //
    // try
    // {
    // RelativeLayout relativeLayout =
    // (RelativeLayout)ColoredPreferenceHelper.findParentView(shell,
    // android.R.id.title);
    // if (relativeLayout != null)
    // {
    // relativeLayout.addView(imageView, 0);
    // TextView tvTitle =
    // (TextView)ColoredPreferenceHelper.findSubViewById(relativeLayout,
    // android.R.id.title);
    // tvSummary =
    // (TextView)ColoredPreferenceHelper.findSubViewById(relativeLayout,
    // android.R.id.summary);
    // if ((tvTitle != null) && (tvSummary != null))
    // {
    // LayoutParams params = (LayoutParams) imageView.getLayoutParams();
    // params.setMargins(params.leftMargin, params.topMargin,
    // Helper.dipToPix(getContext(), 8), params.rightMargin);
    //
    // params = (RelativeLayout.LayoutParams)tvTitle.getLayoutParams();
    // params.addRule(RelativeLayout.RIGHT_OF, 6666);
    // tvTitle.setLayoutParams(params);
    //
    // params = (RelativeLayout.LayoutParams)tvSummary.getLayoutParams();
    // params.addRule(RelativeLayout.RIGHT_OF, 6666);
    // tvSummary.setLayoutParams(params);
    // setLayoutResource(R.layout.preference_widget_imageview);
    // return shell;
    // }
    // }
    // }
    // catch (ClassCastException e)
    // {
    // imageView = null;
    // }
    //
    // return shell;

    // layout.setPadding(Helper.dipToPix(getContext(), 16), 0, 0, 0);
    // layout.addView(imageView);
    // layout.addView(shell);
    // layout.setId(android.R.id.widget_frame);
    //
    // return layout;

    // View shell = super.onCreateView(parent);
    //
    // ViewGroup widget =
    // (ViewGroup)shell.findViewById(android.R.id.widget_frame);
    //
    // LinearLayout linearLayout;
    // linearLayout = new LinearLayout(getContext());
    // linearLayout.setOrientation(LinearLayout.VERTICAL);
    // parent.removeView(widget);
    // linearLayout.addView(widget);
    // parent.addView(linearLayout);
    // widget.addView(new TextView(getContext()));
    // parent.addView(linearLayout);

	/*
     * View root = LayoutInflater.from(getContext()).inflate(
	 * R.layout.slider_preference, widget, true);
	 * 
	 * if (mMinText != null) { TextView minText =
	 * (TextView)root.findViewById(R.id.min); minText.setText(mMinText); }
	 * 
	 * if (mMaxText != null) { TextView minText =
	 * (TextView)root.findViewById(R.id.max); minText.setText(mMaxText); }
	 * 
	 * SeekBar bar = (SeekBar)root.findViewById(R.id.slider);
	 * bar.setMax(MAX_SLIDER_VALUE); bar.setProgress(mValue);
	 * bar.setOnSeekBarChangeListener(this);
	 */
    // return shell;
    // }

}

// /*
// * Copyright (C) 2010 The Android Open Source Project
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// * http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
// package com.replica.replicaisland;
//
// import android.util.AttributeSet;
// import android.view.LayoutInflater;
// import android.view.View;
// import android.view.ViewGroup;
// import android.widget.SeekBar;
// import android.widget.TextView;
// import android.widget.SeekBar.OnSeekBarChangeListener;
// import android.content.Context;
// import android.content.res.TypedArray;
// import android.preference.Preference;
//
// public class SliderPreference extends Preference implements
// OnSeekBarChangeListener {
// private final static int MAX_SLIDER_VALUE = 100;
// private final static int INITIAL_VALUE = 50;
//
// private int mValue = INITIAL_VALUE;
// private String mMinText;
// private String mMaxText;
//
//
// public SliderPreference(Context context) {
// super(context);
// }
//
// public SliderPreference(Context context, AttributeSet attrs) {
// this(context, attrs, android.R.attr.preferenceStyle);
// }
//
// public SliderPreference(Context context, AttributeSet attrs, int defStyle) {
// super(context, attrs, defStyle);
//
// TypedArray a = context.obtainStyledAttributes(attrs,
// R.styleable.SliderPreference, defStyle, 0);
// mMinText = a.getString(R.styleable.SliderPreference_minText);
// mMaxText = a.getString(R.styleable.SliderPreference_maxText);
//
// a.recycle();
// }
//
// @Override
// protected View onCreateView(ViewGroup parent){
// View shell = super.onCreateView(parent);
//
// ViewGroup widget = (ViewGroup)shell.findViewById(android.R.id.widget_frame);
//
// View root = LayoutInflater.from(getContext()).inflate(
// R.layout.slider_preference, widget, true);
//
// if (mMinText != null) {
// TextView minText = (TextView)root.findViewById(R.id.min);
// minText.setText(mMinText);
// }
//
// if (mMaxText != null) {
// TextView minText = (TextView)root.findViewById(R.id.max);
// minText.setText(mMaxText);
// }
//
// SeekBar bar = (SeekBar)root.findViewById(R.id.slider);
// bar.setMax(MAX_SLIDER_VALUE);
// bar.setProgress(mValue);
// bar.setOnSeekBarChangeListener(this);
//
// return shell;
// }
//
// public void onProgressChanged(SeekBar seekBar, int progress, boolean
// fromUser) {
//
// mValue = progress;
// persistInt(mValue);
//
// notifyChanged();
// }
//
// public void onStartTrackingTouch(SeekBar seekBar) {
// }
//
// public void onStopTrackingTouch(SeekBar seekBar) {
// }
//
//
// @Override
// protected Object onGetDefaultValue(TypedArray ta,int index){
// int dValue = (int)ta.getInt(index, INITIAL_VALUE);
//
// return (int)Utils.clamp(dValue, 0, MAX_SLIDER_VALUE);
// }
//
//
// @Override
// protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
// mValue = defaultValue != null ? (Integer)defaultValue : INITIAL_VALUE;
//
// if (!restoreValue) {
// persistInt(mValue);
// } else {
// mValue = getPersistedInt(mValue);
// }
// }
//
//
// }

// public class SeekBarPreference extends Preference implements
// OnSeekBarChangeListener {
//
// public static int maximum = 100;
// public static int interval = 5;
//
// private float oldValue = 50;
// private TextView monitorBox;
//
// public SeekBarPreference(Context context) {
// super(context);
// }
//
// public SeekBarPreference(Context context, AttributeSet attrs) {
// super(context, attrs);
// }
//
// public SeekBarPreference(Context context, AttributeSet attrs, int defStyle) {
// super(context, attrs, defStyle);
// }
//
// @Override
// protected View onCreateView(ViewGroup parent) {
//
// LinearLayout layout = new LinearLayout(getContext());
//
// LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(
// LinearLayout.LayoutParams.WRAP_CONTENT,
// LinearLayout.LayoutParams.WRAP_CONTENT);
// params1.gravity = Gravity.LEFT;
// params1.weight = 1.0f;
//
// LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(80,
// LinearLayout.LayoutParams.WRAP_CONTENT);
// params2.gravity = Gravity.RIGHT;
//
// LinearLayout.LayoutParams params3 = new LinearLayout.LayoutParams(30,
// LinearLayout.LayoutParams.WRAP_CONTENT);
// params3.gravity = Gravity.CENTER;
//
// layout.setPadding(15, 5, 10, 5);
// layout.setOrientation(LinearLayout.HORIZONTAL);
//
// TextView view = new TextView(getContext());
// view.setText(getTitle());
// view.setTextSize(18);
// view.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
// view.setGravity(Gravity.LEFT);
// view.setLayoutParams(params1);
//
// SeekBar bar = new SeekBar(getContext());
// bar.setMax(maximum);
// bar.setProgress((int) this.oldValue);
// bar.setLayoutParams(params2);
// bar.setOnSeekBarChangeListener(this);
//
// this.monitorBox = new TextView(getContext());
// this.monitorBox.setTextSize(12);
// this.monitorBox.setTypeface(Typeface.MONOSPACE, Typeface.ITALIC);
// this.monitorBox.setLayoutParams(params3);
// this.monitorBox.setPadding(2, 5, 0, 0);
// this.monitorBox.setText(bar.getProgress() + "");
//
// layout.addView(view);
// layout.addView(bar);
// layout.addView(this.monitorBox);
// layout.setId(android.R.id.widget_frame);
//
// return layout;
// }
//
// @Override
// public void onProgressChanged(SeekBar seekBar, int progress,
// boolean fromUser) {
//
// progress = Math.round(((float) progress) / interval) * interval;
//
// if (!callChangeListener(progress)) {
// seekBar.setProgress((int) this.oldValue);
// return;
// }
//
// seekBar.setProgress(progress);
// this.oldValue = progress;
// this.monitorBox.setText(progress + "");
// updatePreference(progress);
//
// notifyChanged();
// }
//
// @Override
// public void onStartTrackingTouch(SeekBar seekBar) {
// }
//
// @Override
// public void onStopTrackingTouch(SeekBar seekBar) {
// }
//
// @Override
// protected Object onGetDefaultValue(TypedArray ta, int index) {
//
// int dValue = (int) ta.getInt(index, 50);
//
// return validateValue(dValue);
// }
//
// @Override
// protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
//
// int temp = restoreValue ? getPersistedInt(50) : (Integer) defaultValue;
//
// if (!restoreValue)
// persistInt(temp);
//
// this.oldValue = temp;
// }
//
// private int validateValue(int value) {
//
// if (value > maximum)
// value = maximum;
// else if (value < 0)
// value = 0;
// else if (value % interval != 0)
// value = Math.round(((float) value) / interval) * interval;
//
// return value;
// }
//
// private void updatePreference(int newValue) {
//
// SharedPreferences.Editor editor = getEditor();
// editor.putInt(getKey(), newValue);
// editor.commit();
// }
//
// }

