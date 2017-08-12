package de.badaix.pacetracker.settings;

import org.json.JSONObject;

public class VoiceFeedbackSettings extends BaseSettings {
    public boolean speakDistTotal;
    public boolean speakDistDuration;
    public boolean speakDistTotalDuration;

    public boolean speakDurationTotal;
    public boolean speakDurationDistance;
    public boolean speakDurationTotalDistance;

    public int distanceInterval;
    public int durationInterval;

    public VoiceFeedbackSettings() {
        super(null);
    }

    @Override
    public void fromJson(JSONObject json) {
        try {
            durationInterval = json.getInt("voiceDurationInterval");
            distanceInterval = json.getInt("voiceDistanceInterval");
            speakDistTotal = json.getBoolean("voiceSpeakDistTotal");
            speakDistDuration = json.getBoolean("voiceSpeakDistDuration");
            speakDistTotalDuration = json.getBoolean("voiceSpeakDistTotalDuration");
            speakDurationTotal = json.getBoolean("voiceSpeakDurationTotal");
            speakDurationDistance = json.getBoolean("voiceSpeakDurationDistance");
            speakDurationTotalDistance = json.getBoolean("voiceSpeakDurationTotalDistance");
        } catch (Exception e) {
            durationInterval = 0;
            distanceInterval = 1000;
            speakDistTotal = true;
            speakDistDuration = true;
            speakDistTotalDuration = false;
            speakDurationTotal = true;
            speakDurationDistance = true;
            speakDurationTotalDistance = false;
        }
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("voiceDurationInterval", durationInterval);
            json.put("voiceDistanceInterval", distanceInterval);
            json.put("voiceSpeakDistTotal", speakDistTotal);
            json.put("voiceSpeakDistDuration", speakDistDuration);
            json.put("voiceSpeakDistTotalDuration", speakDistTotalDuration);
            json.put("voiceSpeakDurationTotal", speakDurationTotal);
            json.put("voiceSpeakDurationDistance", speakDurationDistance);
            json.put("voiceSpeakDurationTotalDistance", speakDurationTotalDistance);
        } catch (Exception e) {
        }

        return json;
    }

    public boolean isDistanceEnabled() {
        return (distanceInterval != 0);
    }

    public boolean isDurationEnabled() {
        return (durationInterval != 0);
    }

}
