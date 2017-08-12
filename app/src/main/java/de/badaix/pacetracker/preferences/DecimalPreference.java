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
import de.badaix.pacetracker.widgets.NumberPicker.OnChangedListener;

public class DecimalPreference extends ValueDialogPreference implements OnChangedListener {
    private static final String androidns = "http://schemas.android.com/apk/res/android";

    private NumberPicker mPickInteger, mPickDecimal;
    private TextView mSplashText;
    private Context mContext;
    private ScrollView scrollView;

    private String mDialogMessage;
    private String mUnit;
    private float mDefault, mValue = 0;
    private int mInteger, mDecimal = 0;
    private NumberPicker.Formatter mFormatter = NumberPicker.ONE_DIGIT_FORMATTER;
    private int mPrecision;
    private int mMax;

    public DecimalPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        mDialogMessage = attrs.getAttributeValue(androidns, "dialogMessage");
        mDefault = attrs.getAttributeIntValue(androidns, "defaultValue", 0);
        setFormatter(NumberPicker.ONE_DIGIT_FORMATTER);
        mMax = 999;
    }

    public void setFormatter(NumberPicker.Formatter formatter) {
        mFormatter = formatter;
        mPrecision = (int) Math.pow(10, mFormatter.getPrecision());
    }

    public void setUnit(String unit) {
        mUnit = unit;
    }

    public void setMax(int max) {
        mMax = max;
    }

    public float getValue() {
        return mValue;
    }

    public void setValue(float value) {
        mValue = value;
        mDefault = value;
        bindData();
    }

    @Override
    protected View onCreateDialogView() {
        scrollView = new ScrollView(mContext);
        scrollView.setLayoutParams(new ScrollView.LayoutParams(ScrollView.LayoutParams.FILL_PARENT,
                ScrollView.LayoutParams.FILL_PARENT));
        TableLayout layout = new TableLayout(mContext);
        // layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(6, 6, 6, 6);

        mSplashText = new TextView(mContext);
        if (mDialogMessage != null)
            mSplashText.setText(mDialogMessage);

        TableRow row_header = new TableRow(mContext);
        row_header.addView(mSplashText);

        mPickInteger = new NumberPicker(mContext);
        mPickDecimal = new NumberPicker(mContext);
        mPickDecimal.setFormatter(mFormatter);
        mPickDecimal.setOnChangeListener(this);

        TextView dot = new TextView(mContext);
        dot.setText(".");
        dot.setTextSize(32);

        TextView percent = new TextView(mContext);
        percent.setText(mUnit);
        percent.setTextSize(32);

        TableRow row_one = new TableRow(mContext);
        row_one.setGravity(Gravity.CENTER);
        row_one.addView(mPickInteger);
        row_one.addView(dot);
        row_one.addView(mPickDecimal);
        row_one.addView(percent);

        layout.addView(row_header);

        TableLayout table_main = new TableLayout(mContext);
        table_main.addView(row_one);

        TableRow row_main = new TableRow(mContext);
        row_main.setGravity(Gravity.CENTER_HORIZONTAL);
        row_main.addView(table_main);

        layout.addView(row_main);
        scrollView.addView(layout);

        if (shouldPersist())
            mValue = getPersistedFloat(mDefault);

        bindData();

        return scrollView;
    }

    private void bindData() {
        mInteger = (int) Math.floor(mValue);
        float decimal = (mValue * mPrecision) - (mInteger * mPrecision);
        mDecimal = (int) decimal;
        try {
            mPickDecimal.setRange(0, mPrecision - 1);
            mPickInteger.setRange(0, mMax);
            mPickInteger.setCurrent(mInteger);
            mPickDecimal.setCurrent(mDecimal);
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
        if (restore) {
            mValue = getPersistedFloat(mDefault);
        } else {
            mValue = (Float) defaultValue;
            persistFloat(mValue);
        }
        // super.onSetInitialValue(restore, defaultValue);
        // if (restore) {
        // try {
        // mValue = shouldPersist() ? getPersistedFloat(mDefault) : 0;
        // } catch (Exception ex) {
        // mValue = mDefault;
        // }
        // } else
        // mValue = (Float) defaultValue;
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
            mPickDecimal.onClick(null);
            String value = mPickInteger.getCurrent() + "." + mPickDecimal.getCurrentFormatted();
            mValue = Float.valueOf(value);
            if (shouldPersist())
                persistFloat(mValue);
            callChangeListener(Float.toString(mValue));
        }
    }

    @Override
    public void onChanged(NumberPicker picker, int oldVal, int newVal) {
        if ((oldVal == mPrecision - 1) && (newVal == 0)) {
            if (mPickInteger.getCurrent() < mMax)
                mPickInteger.setCurrent(mPickInteger.getCurrent() + 1);
        } else if ((oldVal == 0) && (newVal == mPrecision - 1)) {
            if (mPickInteger.getCurrent() > 0)
                mPickInteger.setCurrent(mPickInteger.getCurrent() - 1);
        }
        /*
		 * else if ((oldVal == 0) && (newVal == mPrecision - 1)) {
		 * picker.setCurrent(0);
		 * mPickInteger.setCurrent(mPickInteger.getCurrent() + 1); }
		 */
    }
}
