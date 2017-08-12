package de.badaix.pacetracker.preferences;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import de.badaix.pacetracker.widgets.NumberPicker;

public class NumberPickerPreference extends ValueDialogPreference {
    private static final String androidns = "http://schemas.android.com/apk/res/android";

    private NumberPicker mPickInteger;
    private TextView mSplashText;
    private Context mContext;
    private ScrollView scrollView;

    private String mDialogMessage, mSuffix;
    private int mDefault, mMin, mMax, mValue = 0;

    public NumberPickerPreference(Context context, String dialogMessage, String suffix, int defaultValue, int minValue,
                                  int maxValue) {
        super(context, null);
        mContext = context;

        mDialogMessage = dialogMessage;
        mSuffix = suffix;
        mDefault = defaultValue;
        mMax = maxValue;
        mMin = minValue;
    }

    public NumberPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        mDialogMessage = attrs.getAttributeValue(androidns, "dialogMessage");
        mSuffix = attrs.getAttributeValue(androidns, "text");
        mDefault = attrs.getAttributeIntValue(androidns, "defaultValue", 0);
        mMax = attrs.getAttributeIntValue(androidns, "max", 100);
        mMin = attrs.getAttributeIntValue(androidns, "min", 0);
    }

    @Override
    protected View onCreateDialogView() {
        scrollView = new ScrollView(mContext);
        scrollView.setLayoutParams(new ScrollView.LayoutParams(ScrollView.LayoutParams.FILL_PARENT,
                ScrollView.LayoutParams.FILL_PARENT));

        TableLayout layout = new TableLayout(mContext);
        layout.setPadding(6, 6, 6, 6);

        mSplashText = new TextView(mContext);
        if (mDialogMessage != null)
            mSplashText.setText(mDialogMessage);

        TableRow row_header = new TableRow(mContext);
        row_header.addView(mSplashText);

        mPickInteger = new NumberPicker(mContext);
        mPickInteger.setRange(mMin, mMax);

        TextView suffix = new TextView(mContext);
        suffix.setText(mSuffix);
        suffix.setTextSize(32);

        TableRow row_one = new TableRow(mContext);
        row_one.setGravity(Gravity.CENTER);
        row_one.addView(mPickInteger);
        row_one.addView(suffix);

        layout.addView(row_header);

        TableLayout table_main = new TableLayout(mContext);
        table_main.addView(row_one);

        TableRow row_main = new TableRow(mContext);
        row_main.setGravity(Gravity.CENTER_HORIZONTAL);
        row_main.addView(table_main);

        layout.addView(row_main);
        scrollView.addView(layout);

        if (shouldPersist())
            mValue = getPersistedInt(mDefault);

        bindData();

        return scrollView;
    }

    private void bindData() {
        try {
            mPickInteger.setCurrent(mValue);
        } catch (Exception ex) {

        }
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);
        bindData();
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

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult == true) {
            super.onDialogClosed(positiveResult);
            // HACK: "click" both picker inputs to validate inputs before
            // closing the dialog
            // this is to fix a problem of closing the dialog not causing the
            // onFocusChange of the picker
            // to be called
            mPickInteger.onClick(null);
            mValue = mPickInteger.getCurrent();
            if (shouldPersist())
                persistInt(mValue);
        }
    }
}
