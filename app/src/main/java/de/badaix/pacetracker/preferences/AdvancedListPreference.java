package de.badaix.pacetracker.preferences;

import android.content.Context;
import android.util.AttributeSet;

public class AdvancedListPreference extends ValueListPreference {
    protected Context context;

    public AdvancedListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    @Override
    public void setDefaultValue(Object defaultValue) {
        String entryValue = (String) defaultValue;
        for (int i = 0; i < getEntryValues().length; ++i) {
            String current = (String) getEntryValues()[i];
            if (current.equalsIgnoreCase(entryValue)) {
                super.setDefaultValue(defaultValue);
                if (getValue() == null)
                    setValue((String) defaultValue);
                return;
            }
        }

        if (getValue() == null)
            setValueIndex(0);
    }

    public CharSequence getEntry(String entryValue) {
        for (int i = 0; i < getEntryValues().length; ++i)
            if (getEntryValues()[i].equals(entryValue))
                return getEntries()[i];
        return "";
    }

}
