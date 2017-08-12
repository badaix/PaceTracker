package de.badaix.pacetracker.preferences;

import android.content.Context;
import android.preference.Preference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.badaix.pacetracker.R;

public class ValuePreference extends Preference implements PreferenceWithValue {

    private TextView tvValue;
    private LinearLayout widget;
    private String valueText = "";

    public ValuePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        View view = super.onCreateView(parent);
        tvValue = (TextView) view.findViewById(R.id.value_text);
        widget = (LinearLayout) view.findViewById(android.R.id.widget_frame);
        updateGui();
        return view;
    }

    public void updateGui() {
        if (tvValue == null)
            return;
        tvValue.setText(valueText);
        if ((valueText != null) && !TextUtils.isEmpty(valueText))
            widget.setVisibility(View.GONE);
        else
            widget.setVisibility(View.VISIBLE);
    }

    public void setValueText(CharSequence text) {
        valueText = text.toString();
        updateGui();
    }

}
