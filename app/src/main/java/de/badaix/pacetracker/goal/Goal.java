package de.badaix.pacetracker.goal;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.location.Location;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.AttributeSet;
import android.util.Xml;

import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Vector;

import de.badaix.pacetracker.activity.SessionUI;
import de.badaix.pacetracker.sensor.SensorData;
import de.badaix.pacetracker.sensor.SensorProvider;
import de.badaix.pacetracker.sensor.SensorProvider.SensorState;
import de.badaix.pacetracker.session.GpsPos;
import de.badaix.pacetracker.session.HxmData;
import de.badaix.pacetracker.session.Session;

public abstract class Goal implements OnPreferenceClickListener, SessionUI, OnPreferenceChangeListener {

    public final static int GOAL_REQUEST_CODE = 1231;
    protected Vector<Preference> preferences = new Vector<Preference>();
    protected Activity activity = null;
    protected Session session = null;
    protected boolean offline;
    protected Context context = null;

    public Goal(Activity activity) {
        this.activity = activity;
    }

    public boolean isConfigured() {
        return true;
    }

    public void initPreferences() {
    }

    protected AttributeSet getAttributeSet(int layoutId, String key) {
        Resources r = activity.getResources();
        XmlResourceParser parser = r.getLayout(layoutId);
        int state = 0;
        do {
            try {
                state = parser.next();
            } catch (XmlPullParserException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            if (state == XmlPullParser.START_TAG) {
                if (parser.getName().equals(key)) {
                    return Xml.asAttributeSet(parser);
                }
            }
        } while (state != XmlPullParser.END_DOCUMENT);
        return null;
    }

    public void setData(int resultCode, Intent data) {
    }

    public abstract String getName();

    protected abstract void init();

    public void init(Context context, boolean offline) {
        this.offline = offline;
        this.context = context;
        init();
    }

    public Vector<Preference> getPreferences() {
        for (Preference preference : preferences) {
            preference.setOnPreferenceClickListener(this);
            preference.setOnPreferenceChangeListener(this);
        }
        return preferences;
    }

    public void addSessionUI(SessionUI sessionUI) {
    }

    final public void initFromJson(JSONObject json) throws JSONException {
        if (json != null)
            putSettings(json);
    }

    final public String getType() {
        return this.getClass().getSimpleName();
    }

    final public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put(getType(), getSettings());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    protected abstract JSONObject getSettings();

    protected abstract void putSettings(JSONObject json) throws JSONException;

    @Override
    public void setSession(Session session) {
        this.session = session;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }

    @Override
    public void update() {
    }

    public void updateGui() {
    }

    @Override
    public void onStateChanged(Session.State oldState, Session.State newState) {
    }

    @Override
    public void onSessionCommand(int command) {
    }

    @Override
    public void onFilteredLocationChanged(GpsPos location) {
    }

    @Override
    public void onLocationChanged(Location location) {
    }

    @Override
    public void onGpsStatusChanged(boolean active, boolean hasFix, int fixCount, int satCount) {
    }

    @Override
    public void onSensorData(SensorProvider provider, SensorData sensorData) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSensorStateChanged(SensorProvider provider, boolean active, SensorState sensorState) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSensorDataChanged(HxmData hxmData) {
    }

}
