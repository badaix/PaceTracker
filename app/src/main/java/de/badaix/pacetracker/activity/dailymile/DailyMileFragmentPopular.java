package de.badaix.pacetracker.activity.dailymile;

import org.json.JSONException;

import java.io.IOException;
import java.util.Date;
import java.util.Vector;

import de.badaix.pacetracker.social.dailymile.DailyMile;
import de.badaix.pacetracker.social.dailymile.PersonEntry;

public class DailyMileFragmentPopular extends DailyMileFragmentStream {
    public DailyMileFragmentPopular(String title, OnStreamUpdateListener streamUpdateListener) {
        super(title, streamUpdateListener);
    }

    @Override
    public Vector<PersonEntry> getEntries(int page) throws JSONException, IOException {
        DailyMile dm = new DailyMile(this.getActivity());
        Vector<PersonEntry> result = dm.getPopular(page);
        hasMore = (result.size() > 0);
        return result;
    }

    @Override
    public Vector<PersonEntry> getEntries(Date since) throws JSONException, IOException {
        DailyMile dm = new DailyMile(this.getActivity());
        return dm.getPopular(since);
    }

}
