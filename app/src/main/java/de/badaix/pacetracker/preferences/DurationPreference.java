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

public class DurationPreference extends ValueDialogPreference {
    private static final String androidns = "http://schemas.android.com/apk/res/android";
    private ScrollView scrollView;
    private TextView mSplashText;
    private Context context;
    private long mValue;
    private long mDefault;
    private String mDialogMessage;
    private NumberPicker mPickHour, mPickMinute, mPickSecond;
    private NumberPicker.Formatter mFormatter = NumberPicker.TWO_DIGIT_FORMATTER;

    public DurationPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        mDialogMessage = attrs.getAttributeValue(androidns, "dialogMessage");
        mDefault = attrs.getAttributeIntValue(androidns, "defaultValue", 0);
    }

    @Override
    protected View onCreateDialogView() {
        scrollView = new ScrollView(context);
        scrollView.setLayoutParams(new ScrollView.LayoutParams(ScrollView.LayoutParams.FILL_PARENT,
                ScrollView.LayoutParams.FILL_PARENT));
        TableLayout layout = new TableLayout(context);
        // layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(6, 6, 6, 6);

        mSplashText = new TextView(context);
        if (mDialogMessage != null)
            mSplashText.setText(mDialogMessage);

        TableRow row_header = new TableRow(context);
        row_header.addView(mSplashText);

        mPickHour = new NumberPicker(context);
        mPickMinute = new NumberPicker(context);
        mPickSecond = new NumberPicker(context);
        mPickHour.setFormatter(mFormatter);
        mPickMinute.setFormatter(mFormatter);
        mPickSecond.setFormatter(mFormatter);
        mPickHour.setRange(0, 99);
        mPickMinute.setRange(0, 59);
        mPickSecond.setRange(0, 59);

        TextView sepHM = new TextView(context);
        sepHM.setText(":");
        sepHM.setTextSize(32);

        TextView sepMS = new TextView(context);
        sepMS.setText(":");
        sepMS.setTextSize(32);

        TableRow row_one = new TableRow(context);
        row_one.setGravity(Gravity.CENTER);
        row_one.addView(mPickHour);
        row_one.addView(sepHM);
        row_one.addView(mPickMinute);
        row_one.addView(sepMS);
        row_one.addView(mPickSecond);

        layout.addView(row_header);

        TableLayout table_main = new TableLayout(context);
        table_main.addView(row_one);

        TableRow row_main = new TableRow(context);
        row_main.setGravity(Gravity.CENTER_HORIZONTAL);
        row_main.addView(table_main);

        layout.addView(row_main);
        scrollView.addView(layout);

        if (shouldPersist())
            mValue = getPersistedLong(mDefault);

        bindData();

        return scrollView;
    }

    @Override
    protected void onSetInitialValue(boolean restore, Object defaultValue) {
        if (restore) {
            mValue = getPersistedLong(mDefault);
        } else {
            mValue = (Long) defaultValue;
            persistLong(mValue);
        }
    }

    public long getDurationMs() {
        return mValue;
    }

    private void bindData() {
        int tmp = (int) (mValue / 1000) / 3600;
        mPickHour.setCurrent(tmp);

        tmp = (int) (mValue / 1000) % 3600;
        tmp /= 60;
        mPickMinute.setCurrent(tmp);

        tmp = (int) (mValue / 1000) % 60;
        mPickSecond.setCurrent(tmp);
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);
        bindData();
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult == true) {
            super.onDialogClosed(positiveResult);
            mValue = mPickHour.getCurrent() * 3600 + mPickMinute.getCurrent() * 60 + mPickSecond.getCurrent();
            mValue *= 1000;
            if (shouldPersist())
                persistLong(mValue);
            callChangeListener(Long.valueOf(mValue));
        }
    }

}
