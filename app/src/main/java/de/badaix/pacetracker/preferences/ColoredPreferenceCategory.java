package de.badaix.pacetracker.preferences;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.util.Hint;

public class ColoredPreferenceCategory extends PreferenceCategory {
    private Context context;
    private int visibility = View.VISIBLE;
    private int minHeight = -1;

    public ColoredPreferenceCategory(Context context) {
        super(context);
        // setLayoutResource(R.layout.preference_category);
        super.setLayoutResource(R.layout.preference_category);
        this.context = context;
    }

    public ColoredPreferenceCategory(Context context, AttributeSet attrs) {
        super(context, attrs);
        getVisibility(attrs);
        // setLayoutResource(R.layout.preference_category);

        super.setLayoutResource(R.layout.preference_category);
        this.context = context;
    }

    public ColoredPreferenceCategory(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        getVisibility(attrs);
        // setLayoutResource(R.layout.preference_category);
        super.setLayoutResource(R.layout.preference_category);
        this.context = context;
    }

    public void getVisibility(AttributeSet attrs) {
        int[] attrsArray = new int[]{android.R.attr.visibility, android.R.attr.minHeight};
        TypedArray a = getContext().obtainStyledAttributes(attrs, attrsArray);

        int visibility = a.getInteger(0, 0);
        if (visibility == 1)
            this.visibility = View.INVISIBLE;
        else if (visibility == 0)
            this.visibility = View.VISIBLE;
        else
            throw new IllegalArgumentException("Visibility must not be GONE");

        this.minHeight = a.getDimensionPixelSize(1, -1);
        Hint.log(this, "Visibility: " + visibility + ", height: " + minHeight);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        View result = super.onCreateView(parent);
        result.setVisibility(visibility);

        if ((visibility == View.INVISIBLE) && (minHeight != -1)) {
            AbsListView.LayoutParams params = new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    minHeight);
            result.setLayoutParams(params);
        }
        // if (visibility != View.VISIBLE)
        // view.setLayoutParams(new LayoutParams(0, 100));

        return result;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        ColoredPreferenceHelper.setTextColor(view, context.getResources().getColor(R.color.white));
        // view.setBackgroundColor(context.getResources().getColor(R.color.blue));
    }

    public void updatePreferences() {
        if (getPreferenceCount() == 0)
            return;
        for (int i = 0; i < getPreferenceCount() - 1; ++i) {
            Preference preference = getPreference(i);
            preference.setLayoutResource(R.layout.preference);
        }
        Preference preference = getPreference(getPreferenceCount() - 1);
        preference.setLayoutResource(R.layout.preference_last);
    }

    @Override
    public boolean addPreference(Preference preference) {
        boolean result = super.addPreference(preference);
        updatePreferences();
        return result;
    }

    @Override
    public boolean removePreference(Preference preference) {
        boolean result = super.removePreference(preference);
        updatePreferences();
        return result;
    }

    @Override
    protected void onAttachedToActivity() {
        updatePreferences();
        super.onAttachedToActivity();
    }

}
