package de.badaix.pacetracker.activity;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import java.io.IOException;
import java.util.Date;
import java.util.Vector;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.posprovider.ManualPositionProvider;
import de.badaix.pacetracker.posprovider.PositionProviderFactory;
import de.badaix.pacetracker.preferences.DateTimePreference;
import de.badaix.pacetracker.preferences.DecimalPreference;
import de.badaix.pacetracker.preferences.DurationPreference;
import de.badaix.pacetracker.preferences.IconListPreference;
import de.badaix.pacetracker.session.Felt;
import de.badaix.pacetracker.session.GeoPos;
import de.badaix.pacetracker.session.Session;
import de.badaix.pacetracker.session.SessionFactory;
import de.badaix.pacetracker.session.SessionType;
import de.badaix.pacetracker.session.SessionWriter;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.settings.SessionSettings;
import de.badaix.pacetracker.util.DateUtils;
import de.badaix.pacetracker.util.Hint;
import de.badaix.pacetracker.util.LocationUtils;

public class ActivityManualSession extends PreferenceActivity implements OnClickListener,
        OnPreferenceChangeListener {
    private Button btnOk;
    private EditTextPreference textPrefComment;
    private EditTextPreference textPrefDescription;
    private DateTimePreference dateTimePrefStart;
    private DecimalPreference decimalPrefDistance;
    private IconListPreference listPrefType;
    private IconListPreference listPrefFeel;
    private DurationPreference durationPrefDuration;
    private String description = "";
    private String comment = "";

    // /TODO: "Clone" session from history

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (GlobalSettings.getInstance().getContext() == null)
            this.finish();
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.preferences_manual_session);
        setContentView(R.layout.activity_manual_session);

        btnOk = (Button) findViewById(R.id.buttonManualSessionOk);

        listPrefType = (IconListPreference) findPreference("listPrefManualType");
        Vector<SessionType> sessionTypeName = SessionFactory.getInstance().getSessionTypeName();
        String[] sessionType = new String[sessionTypeName.size()];
        String[] sessionName = new String[sessionTypeName.size()];
        int[] sessionDrawable = new int[sessionTypeName.size()];
        String lastSession = GlobalSettings.getInstance(this).getString("lastSession", sessionType[0]);
        int lastSessionIdx = 0;
        for (int i = 0; i < sessionTypeName.size(); ++i) {
            sessionDrawable[i] = sessionTypeName.get(i).getDrawable();
            sessionType[i] = sessionTypeName.get(i).getType();
            sessionName[i] = sessionTypeName.get(i).getName(this);
            if (sessionType[i].equals(lastSession))
                lastSessionIdx = i;
        }
        listPrefType.setEntries(sessionName);
        listPrefType.setEntryValues(sessionType);
        listPrefType.setValueIndex(lastSessionIdx);
        listPrefType.setEntryDrawables(sessionDrawable);
        // GlobalSettings.getInstance().setSessionSummary(new
        // SessionSummary(SessionFactory.getInstance().getSessionData(sessionType[lastSessionIdx])));
        listPrefType.setTitle(sessionName[lastSessionIdx]);
        listPrefType.setBackgroundDrawable(R.drawable.history_item_image_background);
        listPrefType.setOnPreferenceChangeListener(this);

        textPrefComment = (EditTextPreference) findPreference("textPrefManualComment");
        textPrefComment.setSummary(getResources().getString(R.string.sessionCommentSummary));
        textPrefComment.setOnPreferenceChangeListener(this);
        textPrefComment.setText("");

        textPrefDescription = (EditTextPreference) findPreference("textPrefManualDescription");
        textPrefDescription.setOnPreferenceChangeListener(this);
        textPrefDescription.setText("");

        dateTimePrefStart = (DateTimePreference) findPreference("dateTimePrefManualStart");
        Date date = new Date();
        date.setSeconds(0);
        dateTimePrefStart.setValueText(date.toLocaleString());
        dateTimePrefStart.setOnPreferenceChangeListener(this);
        dateTimePrefStart.setDate(date);

        durationPrefDuration = (DurationPreference) findPreference("durationPrefManualDuration");
        durationPrefDuration.setValueText(DateUtils.secondsToHHMMSSString(0));
        durationPrefDuration.setOnPreferenceChangeListener(this);

        decimalPrefDistance = (DecimalPreference) findPreference("decimalPrefManualDistance");
        decimalPrefDistance.setUnit(GlobalSettings.getInstance(this).getDistUnit().toShortString());
        decimalPrefDistance.setValueText("0.0 " + GlobalSettings.getInstance().getDistUnit().toShortString());
        decimalPrefDistance.setOnPreferenceChangeListener(this);

        listPrefFeel = (IconListPreference) findPreference("listPrefManuelFeel");
        listPrefFeel.setOnPreferenceChangeListener(this);
        listPrefFeel.setTitle(getResources().getText(R.string.iFelt) + "...");
        listPrefFeel.setDefaultValue("good");
        try {
            listPrefFeel.setSummary(Felt.fromString(listPrefFeel.getValue()).toLocaleString(this));
        } catch (IllegalArgumentException e) {
            listPrefFeel.setSummary(Felt.fromString("good").toLocaleString(this));
        }
        // for (int i=0; i<listPrefFeel.getEntries().length; ++i)
        // listPrefFeel.getEntryValues()[i] =
        // Felt.fromString(listPrefFeel.getEntries()[i].toString()).toLocaleString(getApplicationContext());

        btnOk.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == btnOk) {
            SessionSettings settings = new SessionSettings(false);
            settings.setComment(comment);
            settings.setDescription(description);
            settings.setPositionProvider(PositionProviderFactory.getPosProvider(this, ManualPositionProvider.class));
            settings.setFelt(Felt.fromString(listPrefFeel.getValue()));
            Session session = SessionFactory.getInstance().getSessionByType(listPrefType.getValue(), null, settings);
            session.setSessionStart(dateTimePrefStart.getDate());
            session.setSessionStop(new Date(dateTimePrefStart.getDate().getTime()
                    + durationPrefDuration.getDurationMs()));
            session.setDistance(decimalPrefDistance.getValue() * 1000);
            session.setDuration(durationPrefDuration.getDurationMs());
            Location currentLocation = LocationUtils.getLastKnownLocation();
            if (currentLocation != null)
                session.setStartPos(new GeoPos(currentLocation.getLatitude(), currentLocation.getLongitude()));
            // SessionSettings.getInstance().setSession(session);
            SessionWriter sessionWriter = new SessionWriter(this);
            sessionWriter.setSession(session);
            try {
                sessionWriter.writeOffline();
            } catch (IOException e) {
                Hint.show(this, e);
            }
        }

        if (getIntent().getBooleanExtra("OpenViewSession", false) == true) {
            Intent intent = new Intent(this, ActivityViewSession.class);
            intent.putExtra("ClearTop", true);
            startActivity(intent);
        } else
            this.finish();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == textPrefComment) {
            comment = (String) newValue;
            if (!TextUtils.isEmpty(comment))
                textPrefComment.setSummary(comment);
            else
                textPrefComment.setSummary(getResources().getString(R.string.sessionCommentSummary));
            textPrefComment.setText(comment);
        } else if (preference == dateTimePrefStart)
            dateTimePrefStart.setValueText(dateTimePrefStart.getDate().toLocaleString());
        else if (preference == textPrefDescription) {
            description = (String) newValue;
            if (!TextUtils.isEmpty(description))
                textPrefDescription.setSummary(description);
            else
                textPrefDescription.setSummary(getResources().getString(R.string.newSessionDescriptionSummary));
            textPrefDescription.setText(description);
        } else if (preference == decimalPrefDistance) {
            decimalPrefDistance.setValueText(Float.toString(decimalPrefDistance.getValue()) + " "
                    + GlobalSettings.getInstance().getDistUnit().toShortString());
        } else if (preference == durationPrefDuration) {
            durationPrefDuration
                    .setValueText(DateUtils.secondsToHHMMSSString(durationPrefDuration.getDurationMs() / 1000));
        } else if (preference == listPrefType) {
            listPrefType.setTitle(SessionFactory.getInstance().getSessionNameFromType((String) newValue));
            // GlobalSettings.getInstance().setSessionSummary(new
            // SessionSummary(SessionFactory.getInstance().getSessionData(listPrefType.getValue())));
            return true;
        } else if (preference == listPrefFeel) {
            listPrefFeel.setSummary(Felt.fromString((String) newValue).toLocaleString(this));
            return true;
        }

        return true;
    }
}
