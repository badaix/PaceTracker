package de.badaix.pacetracker.preferences;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.settings.PulseSettings;
import de.badaix.pacetracker.widgets.SpinEdit;
import de.badaix.pacetracker.widgets.SpinEdit.OnValueChangedListener;

public class PulsePreference extends ValueDialogPreference implements OnValueChangedListener, OnCheckedChangeListener {
    private CheckBox cbEnabled;
    private CheckBox cbAlarmMin;
    private CheckBox cbAlarmMax;
    private SpinEdit editMinPulse;
    private SpinEdit editMaxPulse;
    // private boolean sensorEnabled;
    // private boolean minAlarmEnabled;
    // private boolean maxAlarmEnabled;
    // private int minPulse;
    // private int maxPulse;
    private PulseSettings settings;

    public PulsePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.pulse_preference);
        settings = new PulseSettings(null);
    }

    // @Override
    // protected void onSetInitialValue(boolean restore, Object defaultValue) {
    // super.onSetInitialValue(restore, defaultValue);
    // if (restore) {
    // SharedPreferences pref = getSharedPreferences();
    // minPulse = pref.getInt("minPulseAlarm", 60);
    // maxPulse = pref.getInt("maxPulseAlarm", 170);
    // minAlarmEnabled = pref.getBoolean("minPulseAlamrEnabled", false);
    // maxAlarmEnabled = pref.getBoolean("maxPulseAlarmEnabled", false);
    // sensorEnabled = pref.getBoolean("sensorEnabled", false);
    // }
    // }

    @Override
    protected void onBindDialogView(View view) {
        editMinPulse = (SpinEdit) view.findViewById(R.id.editMinPulse);
        editMaxPulse = (SpinEdit) view.findViewById(R.id.editMaxPulse);
        cbEnabled = (CheckBox) view.findViewById(R.id.cbEnabled);
        cbAlarmMin = (CheckBox) view.findViewById(R.id.cbAlarmMin);
        cbAlarmMax = (CheckBox) view.findViewById(R.id.cbAlarmMax);

        editMinPulse.setOnValueChangedListener(this);
        editMaxPulse.setOnValueChangedListener(this);

        cbAlarmMax.setOnCheckedChangeListener(this);
        cbAlarmMin.setOnCheckedChangeListener(this);
        cbEnabled.setOnCheckedChangeListener(this);
        updateSettings();
        super.onBindDialogView(view);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {

        if (!positiveResult)
            return;

        settings.setMinPulse(editMinPulse.getValue());
        settings.setMaxPulse(editMaxPulse.getValue());
        settings.setSensorEnabled(cbEnabled.isChecked());
        settings.setMinAlarmEnabled(cbAlarmMin.isChecked());
        settings.setMaxAlarmEnabled(cbAlarmMax.isChecked());
        settings.storeSettings();
        callChangeListener(Boolean.valueOf(cbEnabled.isChecked()));

        super.onDialogClosed(positiveResult);
    }

    public PulseSettings getSettings() {
        return settings;
    }

    public void setSettings(PulseSettings settings) {
        this.settings = settings;
        updateSettings();
    }

    private void updateSettings() {
        if (editMinPulse == null)
            return;

        editMinPulse.setValue(settings.getMinPulse());
        editMaxPulse.setValue(settings.getMaxPulse());
        cbAlarmMin.setChecked(settings.isMinAlarmEnabled());
        cbAlarmMax.setChecked(settings.isMaxAlarmEnabled());
        cbEnabled.setChecked(settings.isSensorEnabled());
        this.onCheckedChanged(cbEnabled, settings.isSensorEnabled());
        this.onCheckedChanged(cbAlarmMin, settings.isMinAlarmEnabled());
        this.onCheckedChanged(cbAlarmMax, settings.isMaxAlarmEnabled());
    }

    @Override
    public void onValueChanged(SpinEdit spinEdit, int newValue) {
        if (spinEdit == editMinPulse)
            editMaxPulse.setValue(Math.max(editMaxPulse.getValue(), editMinPulse.getValue() + 1));
        else if (spinEdit == editMaxPulse)
            editMinPulse.setValue(Math.min(editMinPulse.getValue(), editMaxPulse.getValue() - 1));
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == cbAlarmMin)
            editMinPulse.setEnabled(isChecked);
        else if (buttonView == cbAlarmMax)
            editMaxPulse.setEnabled(isChecked);

        cbAlarmMax.setEnabled(cbEnabled.isChecked());
        cbAlarmMin.setEnabled(cbEnabled.isChecked());
        editMaxPulse.setEnabled(cbAlarmMax.isChecked() && cbEnabled.isChecked());
        editMinPulse.setEnabled(cbAlarmMin.isChecked() && cbEnabled.isChecked());
    }
}
