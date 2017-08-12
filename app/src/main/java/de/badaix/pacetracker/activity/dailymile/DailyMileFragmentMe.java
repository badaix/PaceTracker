package de.badaix.pacetracker.activity.dailymile;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import org.apache.http.client.HttpResponseException;
import org.json.JSONException;

import java.io.IOException;
import java.util.Date;
import java.util.Vector;

import javax.security.auth.login.LoginException;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.social.dailymile.DailyMile;
import de.badaix.pacetracker.social.dailymile.PersonEntry;
import de.badaix.pacetracker.util.Helper;
import de.badaix.pacetracker.util.Hint;

@SuppressLint("ValidFragment")
public class DailyMileFragmentMe extends DailyMileFragmentStream implements OnClickListener {

    private boolean loggedIn = false;

    public DailyMileFragmentMe(String title, OnStreamUpdateListener streamUpdateListener) {
        super(title, streamUpdateListener);
        loggedIn = !TextUtils.isEmpty(DailyMile.getToken());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        buttonLogin.setOnClickListener(this);
        if (Helper.isOnline(getActivity()) && !loggedIn) {
            buttonLogin.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        }
        return v;
    }

    @Override
    public Vector<PersonEntry> getEntries(int page) throws JSONException, IOException, LoginException {
        try {
            DailyMile dm = new DailyMile(this.getActivity());
            Vector<PersonEntry> result = dm.getMeAndFriends(page);
            hasMore = (result.size() > 0);
            return result;
        } catch (HttpResponseException e) {
            buttonLogin.post(new Runnable() {
                @Override
                public void run() {
                    buttonLogin.setVisibility(View.VISIBLE);
                }
            });
            throw new LoginException();
        }
    }

    @Override
    public Vector<PersonEntry> getEntries(Date since) throws JSONException, IOException {
        try {
            DailyMile dm = new DailyMile(this.getActivity());
            return dm.getMeAndFriends(since);
        } catch (HttpResponseException e) {
            buttonLogin.post(new Runnable() {
                @Override
                public void run() {
                    buttonLogin.setVisibility(View.VISIBLE);
                }
            });
        }
        return new Vector<PersonEntry>();
    }

    @Override
    public void onClick(View v) {
        if (v == buttonLogin) {
            DailyMile dm = new DailyMile(this.getActivity());
            dm.authorize(this);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == DailyMile.DM_AUTH_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_CANCELED)
                Hint.show(getActivity(), getString(R.string.not_authorized));
            else if (resultCode == Activity.RESULT_OK) {
                buttonLogin.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                executeUpdate(new UpdateTask(this.getActivity(), null));
            }
        }
    }
}
