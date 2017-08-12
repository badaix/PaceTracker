package de.badaix.pacetracker.preferences;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TimePicker;

import java.util.Date;
import java.util.GregorianCalendar;

import de.badaix.pacetracker.util.Hint;

public class DateTimePreference extends ValueDialogPreference {
    private DatePicker datePicker;
    private TimePicker timePicker;
    private LinearLayout linearLayout;
    private ScrollView scrollView;
    private Context context;
    // private static final String androidns =
    // "http://schemas.android.com/apk/res/android";
    private long mValue;
    private long mDefault;

    public DateTimePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        // mDialogMessage = attrs.getAttributeValue(androidns, "dialogMessage");
        // mDefault = attrs.geta.getAttributeLongValue(androidns,
        // "defaultValue", 0);
        mDefault = new Date().getTime();
    }

    @Override
    protected View onCreateDialogView() {
        scrollView = new ScrollView(context);
        scrollView.setLayoutParams(new ScrollView.LayoutParams(ScrollView.LayoutParams.FILL_PARENT,
                ScrollView.LayoutParams.FILL_PARENT));
        linearLayout = new LinearLayout(context);
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                LinearLayout.LayoutParams.FILL_PARENT));
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        datePicker = new DatePicker(context);
        linearLayout.addView(datePicker);
        timePicker = new TimePicker(context);
        scrollView.addView(linearLayout);
        linearLayout.addView(timePicker);
        if (shouldPersist())
            mValue = getPersistedLong(mDefault);
        else
            mValue = mDefault;

        bindData();
        return scrollView;
    }

    @Override
    protected void onSetInitialValue(boolean restore, Object defaultValue) {
        super.onSetInitialValue(restore, defaultValue);
        if (restore) {
            try {
                mValue = shouldPersist() ? getPersistedLong(mDefault) : mDefault;
            } catch (Exception ex) {
                mValue = mDefault;
            }
        } else
            mValue = (Long) defaultValue;
    }

    public Date getDate() {
        return new Date(mValue);
    }

    public void setDate(Date date) {
        mValue = date.getTime();
        mDefault = mValue;
        bindData();
    }

    private void bindData() {
        GregorianCalendar calender = new GregorianCalendar();
        calender.setTime(new Date(mValue));
        try {
            Hint.log(this, calender.getTime().toLocaleString());
            datePicker.updateDate(calender.get(GregorianCalendar.YEAR), calender.get(GregorianCalendar.MONTH),
                    calender.get(GregorianCalendar.DAY_OF_MONTH));
            timePicker.setCurrentHour(calender.get(GregorianCalendar.HOUR_OF_DAY));
            timePicker.setCurrentMinute(calender.get(GregorianCalendar.MINUTE));
        } catch (Exception e) {
        }
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
            Date date = new Date(datePicker.getYear() - 1900, datePicker.getMonth(), datePicker.getDayOfMonth(),
                    timePicker.getCurrentHour(), timePicker.getCurrentMinute());
            Hint.log(this, date.toLocaleString());
            // Log.d("PaceTracker", date.getYear() + " " + date.getMonth() + " "
            // + date.getDay());
            // Log.d("PaceTracker", datePicker.getYear() + " " +
            // datePicker.getMonth() + " " + datePicker.getDayOfMonth());
            mValue = date.getTime();
            if (shouldPersist())
                persistLong(mValue);
            callChangeListener(Long.valueOf(mValue));
        }
    }

}
