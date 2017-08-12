package de.badaix.pacetracker.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.UUID;

import de.badaix.pacetracker.sensor.Sensor;
import de.badaix.pacetracker.sensor.Sensor.SensorType;
import de.badaix.pacetracker.session.Route;
import de.badaix.pacetracker.session.SessionService;
import de.badaix.pacetracker.session.SessionSummary;
import de.badaix.pacetracker.social.dailymile.User;
import de.badaix.pacetracker.util.Distance;
import de.badaix.pacetracker.util.Distance.System;
import de.badaix.pacetracker.util.Hint;
import de.badaix.pacetracker.util.Weight;
import de.badaix.pacetracker.weather.Weather;

public class GlobalSettings {
    private static final boolean IS_DEVELOPER = false;
    private static GlobalSettings instance = null;
    public Route route = null;
    public SessionService service = null;
    private Context ctx = null;
    private VoiceFeedbackSettings voiceFeedbackSettings = null;
    private SessionSummary sessionSummary = null;

    /**
     * Default-Konstruktor, der nicht außerhalb dieser Klasse aufgerufen werden
     * kann
     */
    private GlobalSettings() {
    }

    /**
     * Statische Methode, liefert die einzige Instanz dieser Klasse zurück
     */
    public static GlobalSettings getInstance(Context context) {
        if (instance == null) {
            instance = new GlobalSettings();
        }
        if (context != null)
            instance.ctx = context;

        return instance;
    }

    public static GlobalSettings getInstance() {
        return getInstance(null);
    }

    public Context getContext() {
        return ctx;
    }

    public SharedPreferences getPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(ctx);
    }


    public String getMetaData(String key) {
        ApplicationInfo ai = null;
        try {
            ai = ctx.getPackageManager().getApplicationInfo(ctx.getPackageName(), PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "";
        }
        Bundle bundle = ai.metaData;
        return bundle.getString(key, "");
    }

    public GlobalSettings put(String key, String value) {
        Editor editor = getPrefs().edit();
        editor.putString(key, value);
        editor.commit();
        return this;
    }

    public GlobalSettings put(String key, boolean value) {
        Editor editor = getPrefs().edit();
        editor.putBoolean(key, value);
        editor.commit();
        return this;
    }

    public GlobalSettings put(String key, float value) {
        Editor editor = getPrefs().edit();
        editor.putFloat(key, value);
        editor.commit();
        return this;
    }

    public GlobalSettings put(String key, int value) {
        Editor editor = getPrefs().edit();
        editor.putInt(key, value);
        editor.commit();
        return this;
    }

    public GlobalSettings put(String key, long value) {
        Editor editor = getPrefs().edit();
        editor.putLong(key, value);
        editor.commit();
        return this;
    }

    public GlobalSettings put(String key, JSONObject value) {
        Editor editor = getPrefs().edit();
        editor.putString(key, value.toString());
        editor.commit();
        return this;
    }

    public String getString(String key, String defaultValue) {
        return getPrefs().getString(key, defaultValue);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return getPrefs().getBoolean(key, defaultValue);
    }

    public float getFloat(String key, float defaultValue) {
        return getPrefs().getFloat(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        return getPrefs().getInt(key, defaultValue);
    }

    public long getLong(String key, long defaultValue) {
        return getPrefs().getLong(key, defaultValue);
    }

    public Distance.System getDistSystem() {
        return System.fromString(getString("units", System.METRIC.toString()));
    }

    public void setDistSystem(Distance.System distSystem) {
        put("units", distSystem.toString());
    }

    public void setDistSystem(String distSystem) {
        put("units", System.fromString(distSystem).toString());
    }

    public Weather getLastWeather() {
        String jsonString = getString("lastWeather", "");
        try {
            if (!TextUtils.isEmpty(jsonString))
                return new Weather(new JSONObject(jsonString));
        } catch (JSONException e) {
        }
        return null;
    }

    public void setLastWeather(Weather weather) {
        if (weather == null)
            return;
        try {
            put("lastWeather", weather.toJson().toString());
        } catch (JSONException e) {
        }
    }

    public Distance.Unit getDistUnit() {
        if (getDistSystem() == System.METRIC)
            return Distance.Unit.KILOMETERS;
        else
            return Distance.Unit.MILES;
    }

    public Weight.Unit getWeightUnit() {
        if (getDistSystem() == System.METRIC)
            return Weight.Unit.KILOGRAM;
        else
            return Weight.Unit.POUND;
    }

    public boolean showHints() {
        // /TODO
        return true;
    }

    public int getVoiceVolume() {
        int def = ((AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE))
                .getStreamVolume(TextToSpeech.Engine.DEFAULT_STREAM);
        return getInt("ttsVolumePref", def);
    }

    public VoiceFeedbackSettings getVoiceFeedback() {
        if (voiceFeedbackSettings == null)
            voiceFeedbackSettings = new VoiceFeedbackSettings();

        return voiceFeedbackSettings;
    }

    public void storeVoiceFeedback() {
        getVoiceFeedback().storeSettings();
    }

    public Sensor getSensor() {
        String sensorName = getString("sensorName", "");
        SensorType sensorType = SensorType.byType(getString("sensorType", ""));
        return new Sensor(sensorType, sensorName);
    }

    public void setSensor(Sensor sensor) {
        put("sensorName", sensor.getName());
        put("sensorType", sensor.getType().getType());
    }

    public Uri getPulseAlarmUri() {
        String strUri = getString("pulseAlarmUri", "");
        if (TextUtils.isEmpty(strUri))
            return Uri.EMPTY;
        return Uri.parse(strUri);
    }

    public void setPulseAlarmUri(Uri uri) {
        if (uri == null)
            put("pulseAlarmUri", "");
        else
            put("pulseAlarmUri", uri.toString());
    }

    public float getUserWeight() {
        try {
            return getFloat("userWeight", 75.0f);
        } catch (Exception e) {
            return 75.0f;
        }
    }

    public void setUserWeight(double weight) {
        float newWeight = (float) Weight.weightToKilograms(weight);
        Hint.log(this, "setUserWeight: " + newWeight);
        put("userWeight", newWeight);
    }

    public User getMe() {
        String json = getString("dmUserMe", "");
        User me = null;
        if (!TextUtils.isEmpty(json)) {
            try {
                me = new User(new JSONObject(json));
            } catch (JSONException e) {
                return null;
            }
        }
        return me;
    }

    public void setMe(User user) {
        try {
            put("dmUserMe", user.toJson().toString());
        } catch (JSONException e) {
        }
    }

    public SessionSummary getSessionSummary() {
        return sessionSummary;
    }

    public void setSessionSummary(SessionSummary sessionSummary) {
        this.sessionSummary = sessionSummary;
    }

    public boolean isFacebookEnabled() {
        return false;
    }

    public boolean isGplusEnabled() {
        return true;
    }

    @SuppressWarnings("unused")
    public boolean isDeveloper() {
        HashSet<String> devDevices = new HashSet<String>();
        devDevices.add("ffffffff-c63a-f87c-ffff-ffffc01532d9");
        devDevices.add("ffffffff-c63a-f87c-0000-000000000000");
        devDevices.add("00000000-0bde-1111-f9d3-09471fe21405");
        devDevices.add("00000000-0bde-1111-0000-000000000000");
        devDevices.add("00000000-13f4-cf39-9c0c-2ff800000000");
        return (IS_DEVELOPER && devDevices.contains(getDeviceId().toString()));
    }

    public boolean isDebug() {
        return isDeveloper() && getBoolean("prefDebug", false);
    }

    public boolean isPro() {
        return getBoolean("prefPro", true);
    }

    public void setPro(boolean pro) {
        put("prefPro", pro);
    }

    // public boolean isDeveloper() {
    // return true;
    // }
    //
    // public boolean isDebug() {
    // return isDeveloper() && getBoolean("prefDebug", false);
    // }
    //
    // public void setPro(boolean pro) {
    // put("prefPro", pro);
    // }
    //
    // public boolean isPro() {
    // if (isDeveloper()) {
    // return getBoolean("prefPro", false);
    // } else {
    // return false;
    // }
    // }

    public UUID getDeviceId() {
        String androidId = "";
        UUID devId = new UUID(0, 0);
        try {
            final String tmDevice, tmSerial;
            androidId = ""
                    + android.provider.Settings.Secure.getString(ctx.getContentResolver(),
                    android.provider.Settings.Secure.ANDROID_ID);
            final TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
            tmDevice = "" + tm.getDeviceId();
            tmSerial = "" + tm.getSimSerialNumber();

            devId = new UUID(androidId.hashCode(), ((long) tmDevice.hashCode() << 32) | tmSerial.hashCode());

        } catch (Exception e) {
            devId = new UUID(androidId.hashCode(), 0);
        }
        Log.d("GlobalSettings", devId.toString());
        return devId;
    }
}
