package de.badaix.pacetracker.activity;

import android.content.Intent;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import java.io.IOException;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.preferences.IconListPreference;
import de.badaix.pacetracker.session.Felt;
import de.badaix.pacetracker.session.SessionSummary;
import de.badaix.pacetracker.session.SessionWriter;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.util.Hint;

public class ActivityEditSession extends AppCompatPreferenceActivity implements OnClickListener, OnPreferenceChangeListener {
    private Button btnOk;
    private Button btnCancel;
    private EditTextPreference textPrefComment;
    private EditTextPreference textPrefDescription;
    private IconListPreference listPrefFeel;
    private String description = "";
    private String comment = "";
    private Felt felt = Felt.NONE;
    private SessionSummary session;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (GlobalSettings.getInstance().getContext() == null)
            this.finish();
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.edit_session_preferences);
        setContentView(R.layout.edit_session);
        // sessionPref = getPreferenceScreen().getPreference(0);
        btnOk = (Button) findViewById(R.id.buttonEditSessionOk);
        btnCancel = (Button) findViewById(R.id.buttonEditSessionCancel);

        // actionBar = (ActionBar) findViewById(R.id.editSessionActionbar);
        session = GlobalSettings.getInstance(this).getSessionSummary();

        textPrefComment = (EditTextPreference) findPreference("textPrefComment");
        textPrefComment.setSummary(getResources().getString(R.string.sessionCommentSummary));
        textPrefComment.setOnPreferenceChangeListener(this);
        textPrefComment.setText(session.getSettings().getComment());

        textPrefDescription = (EditTextPreference) findPreference("textPrefDescription");
        textPrefDescription.setOnPreferenceChangeListener(this);
        textPrefDescription.setText(session.getSettings().getDescription());

        listPrefFeel = (IconListPreference) findPreference("listPrefFeel");
        listPrefFeel.setOnPreferenceChangeListener(this);
        listPrefFeel.setTitle(getResources().getText(R.string.iFelt) + "...");
        listPrefFeel.setDefaultValue("good");
        try {
            listPrefFeel.setSummary(Felt.fromString(listPrefFeel.getValue()).toLocaleString(this));
        } catch (IllegalArgumentException e) {
            listPrefFeel.setSummary(Felt.fromString("good").toLocaleString(this));
        }

        felt = Felt.fromString(listPrefFeel.getValue());
        btnOk.setOnClickListener(this);
        btnCancel.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == btnOk) {
            SessionWriter sessionWriter = new SessionWriter(this);
            try {
                session.getSettings().setComment(comment);
                session.getSettings().setDescription(description);
                session.getSettings().setFelt(felt);
                sessionWriter.updateSession(session);
            } catch (IOException e) {
                Hint.show(this, e);
            }
        } else if (v == btnCancel) {
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
        } else if (preference == textPrefDescription) {
            description = (String) newValue;
            if (!TextUtils.isEmpty(description))
                textPrefDescription.setSummary(description);
            else
                textPrefDescription.setSummary(getResources().getString(R.string.newSessionDescriptionSummary));
            textPrefDescription.setText(description);
        } else if (preference == listPrefFeel) {
            felt = Felt.fromString((String) newValue);
            listPrefFeel.setSummary(felt.toLocaleString(this));
        }

        return true;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // If the back Key was pressed, then finish the program.
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            return true;
        }

        // else return the normal function of whatever key was pressed
        return super.onKeyDown(keyCode, event);
    }

}
