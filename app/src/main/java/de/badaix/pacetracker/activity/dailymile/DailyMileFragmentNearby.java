package de.badaix.pacetracker.activity.dailymile;

import android.location.Address;

import org.json.JSONException;

import java.io.IOException;
import java.util.Date;
import java.util.Vector;

import de.badaix.pacetracker.social.dailymile.DailyMile;
import de.badaix.pacetracker.social.dailymile.PersonEntry;
import de.badaix.pacetracker.util.LocationUtils;

public class DailyMileFragmentNearby extends DailyMileFragmentStream {

    public DailyMileFragmentNearby(String title, OnStreamUpdateListener streamUpdateListener) {
        super(title, streamUpdateListener);
        if (LocationUtils.lastKnownAddress != null) {
            Address address = LocationUtils.lastKnownAddress;
            if (address.getLocality() != null)
                this.title = address.getLocality();
            if (address.getCountryCode() != null)
                this.title += ", " + address.getCountryCode();
        }
    }

    @Override
    public Vector<PersonEntry> getEntries(int page) throws JSONException, IOException {
        DailyMile dm = new DailyMile(this.getActivity());
        Vector<PersonEntry> result = dm.getNearby(LocationUtils.getLastKnownLocation(), page);
        hasMore = (result.size() > 0);
        return result;
    }

    @Override
    public Vector<PersonEntry> getEntries(Date since) throws JSONException, IOException {
        DailyMile dm = new DailyMile(this.getActivity());
        return dm.getNearby(LocationUtils.getLastKnownLocation(), since);
    }

}
