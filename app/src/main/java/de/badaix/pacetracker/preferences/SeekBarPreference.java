package de.badaix.pacetracker.preferences;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import de.badaix.pacetracker.util.Helper;

public class SeekBarPreference extends ValueDialogPreference {
    private static final String androidns = "http://schemas.android.com/apk/res/android";
    private SeekBar seekBar;
    private LinearLayout linearLayout;
    private Context context;
    private int mDefault;
    private int mValue;
    private int mMax;

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        // mDialogMessage = attrs.getAttributeValue(androidns, "dialogMessage");
        mDefault = attrs.getAttributeIntValue(androidns, "defaultValue", 0);
        mMax = attrs.getAttributeIntValue(androidns, "maxValue", 10);
    }

    @Override
    protected View onCreateDialogView() {
        linearLayout = new LinearLayout(context);
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                LinearLayout.LayoutParams.FILL_PARENT));
        seekBar = new SeekBar(context);
        seekBar.setMax(mMax);
        linearLayout.addView(seekBar);
        mValue = getPersistedInt(mDefault);
        seekBar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        int padding = Helper.dipToPix(context, 8);
        seekBar.setPadding(padding, padding, padding, padding);
        seekBar.setProgress(mValue);
        return linearLayout;
    }

    @Override
    protected void onSetInitialValue(boolean restore, Object defaultValue) {
        super.onSetInitialValue(restore, defaultValue);
        if (restore) {
            try {
                mValue = shouldPersist() ? getPersistedInt(mDefault) : 0;
            } catch (Exception ex) {
                mValue = mDefault;
            }
        } else
            mValue = (Integer) defaultValue;
    }

    public int getMax() {
        return mMax;
    }

    public void setMax(int max) {
        mMax = max;
    }

    public int getProgress() {
        return mValue;
    }

    public void setProgress(int progress) {
        mValue = progress;
        mDefault = progress;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult == true) {
            super.onDialogClosed(positiveResult);
            mValue = seekBar.getProgress();
            if (shouldPersist())
                persistInt(mValue);
            callChangeListener(mValue);
        }
    }

}
