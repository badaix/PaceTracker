package de.badaix.pacetracker.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.badaix.pacetracker.R;

public class SpinEdit extends LinearLayout implements View.OnClickListener, View.OnLongClickListener {

    private TextView textView;
    private Button buttonLess;
    private Button buttonMore;
    private int minValue;
    private int maxValue;
    private int step;
    private int bigStep;
    private boolean enabled = true;
    private int value = Integer.MIN_VALUE;
    private OnValueChangedListener listener = null;
    public SpinEdit(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_spin_edit, this);
        textView = (TextView) findViewById(R.id.SpinEditTextView);
        buttonLess = (Button) findViewById(R.id.SpinEditButtonLess);
        buttonMore = (Button) findViewById(R.id.SpinEditButtonMore);
        buttonLess.setOnClickListener(this);
        buttonMore.setOnClickListener(this);
        buttonLess.setOnLongClickListener(this);
        buttonMore.setOnLongClickListener(this);
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.SpinEdit);

        minValue = (a.getInteger(R.styleable.SpinEdit_min_value, 0));
        maxValue = (a.getInteger(R.styleable.SpinEdit_max_value, 20000));
        step = (a.getInteger(R.styleable.SpinEdit_step, 0));
        bigStep = (a.getInteger(R.styleable.SpinEdit_big_step, 0));
        setValue(a.getInteger(R.styleable.SpinEdit_default_value, minValue));
        Log.d("SpinEdit", "MinValue: " + minValue);
        Log.d("SpinEdit", "MaxValue: " + maxValue);
        Log.d("SpinEdit", "Step: " + step);
        // Don't forget this
        a.recycle();
    }

    public void setOnValueChangedListener(OnValueChangedListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        buttonLess.setEnabled(enabled);
        buttonMore.setEnabled(enabled);
        textView.setEnabled(enabled);
        this.enabled = enabled;
    }

    public int getValue() {
        return value;
        // return Integer.valueOf(textView.getText().toString());
    }

    public final void setValue(int newValue) {
        if (newValue < minValue)
            newValue = minValue;
        else if (newValue > maxValue)
            newValue = maxValue;

        if (value != newValue) {
            value = newValue;
            textView.setText(Integer.toString(value));
            CheckEnabled();
            if (listener != null)
                listener.onValueChanged(this, value);
        }
    }

    public int getMinValue() {
        return minValue;
    }

    public void setMinValue(int minValue) {
        this.minValue = minValue;
        setValue(getValue());
    }

    public int getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(int maxValue) {
        this.maxValue = maxValue;
        setValue(getValue());
    }

    private void CheckEnabled() {
        buttonLess.setEnabled(enabled && (getValue() > minValue));
        buttonMore.setEnabled(enabled && (getValue() < maxValue));
    }

    @Override
    public void onClick(View v) {
        if ((Button) v == buttonLess) {
            setValue(Math.max(minValue, getValue() - step));
        } else if ((Button) v == buttonMore) {
            setValue(Math.min(maxValue, getValue() + step));
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if ((Button) v == buttonLess) {
            setValue(Math.max(minValue, getValue() - bigStep));
        } else if ((Button) v == buttonMore) {
            setValue(Math.min(maxValue, getValue() + bigStep));
        }
        v.setPressed(false);
        return true;
    }

    public abstract interface OnValueChangedListener {
        public void onValueChanged(SpinEdit spinEdit, int newValue);
    }

}
