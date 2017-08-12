package de.badaix.pacetracker.activity.dailymile;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONException;

import java.io.IOException;
import java.util.Date;
import java.util.Vector;

import javax.security.auth.login.LoginException;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.social.dailymile.DMItem;
import de.badaix.pacetracker.social.dailymile.PersonEntry;
import de.badaix.pacetracker.views.dailymile.DailyMileEntry;

class DailyMileStreamAdapter extends DailyMileItemAdapter {

    public DailyMileStreamAdapter(Context context, int textViewResourceId, OnItemTouchListener touchListener,
                                  Vector<DMItem> list) {
        super(context, textViewResourceId, touchListener, list);
    }

    @Override
    protected View getItemView(int position, View convertView, ViewGroup parent) {
        synchronized (entries) {
            DMItem entry = getItem(position);
            if (!(entry instanceof PersonEntry))
                return null;

            PersonEntry personEntry = (PersonEntry) entry;
            if (convertView != null)
                ((DailyMileEntry) convertView).setEntry(personEntry, true);
            else
                convertView = new DailyMileEntry(context, personEntry, true);

            return convertView;
        }
    }
}

@SuppressLint("ValidFragment")
public class DailyMileFragmentStream extends DailyMileFragmentMultiPage {

    public DailyMileFragmentStream(String title, OnStreamUpdateListener streamUpdateListener) {
        super(title, streamUpdateListener);
    }

    @Override
    public Vector<? extends DMItem> getEntries(int page) throws JSONException, IOException, LoginException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Vector<? extends DMItem> getEntries(Date since) throws JSONException, IOException, LoginException {
        // TODO Auto-generated method stub
        return null;
    }

    protected void createAdapter() {
        if (this.adapter == null) {
            this.adapter = new DailyMileStreamAdapter(this.getActivity(), R.layout.imageitem, onItemTouchListener,
                    this.entries);
            setListAdapter(adapter);
        }
    }
}
