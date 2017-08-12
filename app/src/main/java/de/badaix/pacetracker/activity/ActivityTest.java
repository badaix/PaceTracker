package de.badaix.pacetracker.activity;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.View;
import android.view.View.OnClickListener;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;

import java.io.IOException;
import java.util.List;
import java.util.Vector;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.preferences.IconListPreference;
import de.badaix.pacetracker.session.GeoPos;
import de.badaix.pacetracker.session.SessionFactory;
import de.badaix.pacetracker.session.SessionType;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.social.dailymile.DailyMile;
import de.badaix.pacetracker.util.CheapDistance;
import de.badaix.pacetracker.util.Distance;
import de.badaix.pacetracker.util.Helper;
import de.badaix.pacetracker.util.Hint;
import de.badaix.pacetracker.util.LocationUtils;

public class ActivityTest extends AppCompatPreferenceActivity implements OnPreferenceChangeListener, OnPreferenceClickListener,
        OnClickListener {
    private EditTextPreference textPrefTest;
    private EditTextPreference textPrefTest2;
    private IconListPreference listPrefTest;
    private IconListPreference feelPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (GlobalSettings.getInstance().getContext() == null)
            this.finish();
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.test_preferences);
        setContentView(R.layout.test);

        textPrefTest = (EditTextPreference) findPreference("textPrefTest");
        textPrefTest.setOnPreferenceChangeListener(this);
        textPrefTest.setOnPreferenceClickListener(this);

        textPrefTest2 = (EditTextPreference) findPreference("textPrefTest2");
        textPrefTest2.setOnPreferenceChangeListener(this);
        textPrefTest2.setOnPreferenceClickListener(this);

        listPrefTest = (IconListPreference) findPreference("listPrefTest");
        Vector<SessionType> sessionTypeName = SessionFactory.getInstance().getSessionTypeName();
        String[] sessionType = new String[sessionTypeName.size()];
        String[] sessionName = new String[sessionTypeName.size()];
        int[] sessionDrawable = new int[sessionTypeName.size()];
        String lastSession = GlobalSettings.getInstance(this).getString("lastSession", sessionType[0]);
        int lastSessionIdx = 0;
        for (int i = 0; i < sessionTypeName.size(); ++i) {
            sessionDrawable[i] = sessionTypeName.get(i).getDrawable();
            sessionType[i] = sessionTypeName.get(i).getType();
            sessionName[i] = sessionTypeName.get(i).getName(getApplicationContext());
            if (sessionType[i].equals(lastSession))
                lastSessionIdx = i;
        }
        listPrefTest.setEntries(sessionName);
        listPrefTest.setEntryValues(sessionType);
        listPrefTest.setEntryDrawables(sessionDrawable);
        listPrefTest.setBackgroundDrawable(R.drawable.history_item_image_background);

        listPrefTest.setValueIndex(lastSessionIdx);
        listPrefTest.setTitle(sessionName[lastSessionIdx]);
        listPrefTest.setOnPreferenceChangeListener(this);

        feelPref = (IconListPreference) findPreference("feelPref");
        feelPref.setOnPreferenceChangeListener(this);

        Location location = LocationUtils.getLastKnownLocation(this, true);
        GeoPos myPos = new GeoPos(location.getLatitude(), location.getLongitude());
        CheapDistance cheapDistance = new CheapDistance(myPos);
        Hint.log(this, "Lat: " + location.getLatitude() + ", Lon: " + location.getLongitude());
        Hint.log(this,
                "Lat 50m: " + cheapDistance.latitudeOffset(50) + ", Lon 50m: " + cheapDistance.longitudeOffset(50));
        Hint.log(this, "one Lat: " + cheapDistance.getOneLatitude() + ", One Lon: " + cheapDistance.getOneLongitude());
        GeoPos p1 = new GeoPos(myPos.latitude - cheapDistance.latitudeOffset(50), myPos.longitude);
        Hint.log(this,
                "Distance Lat: " + Distance.calculateDistance(myPos, p1) + "  " + cheapDistance.distance(myPos, p1));
        GeoPos p2 = new GeoPos(myPos.latitude, myPos.longitude - cheapDistance.longitudeOffset(50));
        Hint.log(this,
                "Distance Lon: " + Distance.calculateDistance(myPos, p2) + "  " + cheapDistance.distance(myPos, p2));
        Location locLon = new Location(location);
        locLon.setLongitude(myPos.longitude - cheapDistance.longitudeOffset(50));
        Hint.log(this, "Distance Lon: " + locLon.distanceTo(location));
        locLon = new Location(location);
        locLon.setLatitude(myPos.latitude - cheapDistance.latitudeOffset(50));
        Hint.log(this, "Distance Lat: " + locLon.distanceTo(location));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == textPrefTest) {
            return true;
        } else if (preference == listPrefTest) {
            listPrefTest.setTitle(SessionFactory.getInstance().getSessionNameFromType((String) newValue));
            return true;
        } else if (preference == feelPref) {
            int selected = Integer.parseInt((String) newValue);
            feelPref.setTitle("I felt: " + feelPref.getEntries()[selected]);
            return true;
        }

        return false;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        // if (preference == textPrefTest) {
        // SessionPersistance.backupDatabase("backup.db3");
        // } else
        if (preference == textPrefTest2) {
            DailyMile dm = new DailyMile(this);
            try {
                dm.getMeAndFriends(1);
            } catch (ClientProtocolException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            if (Helper.isOnline(this)) {
                Location location = LocationUtils.getLastKnownLocation(this, false);
                if (location != null) {
                    Hint.log(this, "Location: " + location.getLongitude() + "  " + location.getLatitude() + "  "
                            + location.getProvider());
                    Geocoder geocoder = new Geocoder(this);
                    try {
                        List<Address> addresses = geocoder.getFromLocation(location.getLatitude(),
                                location.getLongitude(), 1);
                        for (Address adress : addresses) {
                            if (adress != null) {
                                Hint.log(this, adress.getLocality() + ", " + adress.getCountryCode());
                            }
                        }
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                }
            }
        }

        return false;
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub

    }

}
