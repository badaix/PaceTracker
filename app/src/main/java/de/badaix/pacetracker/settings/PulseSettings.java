package de.badaix.pacetracker.settings;

import org.json.JSONObject;

public class PulseSettings extends BaseSettings {

    private boolean sensorEnabled;
    private boolean minAlarmEnabled;
    private boolean maxAlarmEnabled;
    private int minPulse;
    private int maxPulse;

    public PulseSettings(JSONObject jsonSettings) {
        super(jsonSettings);
    }

    private void initWithDefaults() {
        sensorEnabled = false;
        minPulse = 50;
        maxPulse = 170;
        minAlarmEnabled = false;
        maxAlarmEnabled = true;
    }

    @Override
    public void fromJson(JSONObject json) {
        if (json == null) {
            initWithDefaults();
            return;
        }

        try {
            // JSONObject settings = json.getJSONObject("sensorSettings");
            sensorEnabled = json.getBoolean("enabled");
            JSONObject alarm = json.getJSONObject("alarm");
            JSONObject minAlarm = alarm.getJSONObject("min");
            minAlarmEnabled = minAlarm.getBoolean("enabled");
            minPulse = minAlarm.getInt("pulse");
            JSONObject maxAlarm = alarm.getJSONObject("max");
            maxAlarmEnabled = maxAlarm.getBoolean("enabled");
            maxPulse = maxAlarm.getInt("pulse");
        } catch (Exception e) {
            initWithDefaults();
        }
    }

    @Override
    public JSONObject toJson() {
        JSONObject settings = new JSONObject();
        try {
            // JSONObject settings = new JSONObject();
            settings.put("enabled", sensorEnabled);

            JSONObject alarm = new JSONObject();
            JSONObject minAlarm = new JSONObject();
            JSONObject maxAlarm = new JSONObject();
            minAlarm.put("enabled", minAlarmEnabled);
            minAlarm.put("pulse", minPulse);
            maxAlarm.put("enabled", maxAlarmEnabled);
            maxAlarm.put("pulse", maxPulse);
            alarm.put("min", minAlarm);
            alarm.put("max", maxAlarm);
            // json.put("sensorSettings", settings);
            settings.put("alarm", alarm);
        } catch (Exception e) {
        }

        return settings;
    }

    public boolean isSensorEnabled() {
        return sensorEnabled;
    }

    public void setSensorEnabled(boolean sensorEnabled) {
        this.sensorEnabled = sensorEnabled;
    }

    public boolean isMinAlarmEnabled() {
        return minAlarmEnabled;
    }

    public void setMinAlarmEnabled(boolean minAlarmEnabled) {
        this.minAlarmEnabled = minAlarmEnabled;
    }

    public boolean isMaxAlarmEnabled() {
        return maxAlarmEnabled;
    }

    public void setMaxAlarmEnabled(boolean maxAlarmEnabled) {
        this.maxAlarmEnabled = maxAlarmEnabled;
    }

    public int getMinPulse() {
        return minPulse;
    }

    public void setMinPulse(int minPulse) {
        this.minPulse = minPulse;
    }

    public int getMaxPulse() {
        return maxPulse;
    }

    public void setMaxPulse(int maxPulse) {
        this.maxPulse = maxPulse;
    }

}
