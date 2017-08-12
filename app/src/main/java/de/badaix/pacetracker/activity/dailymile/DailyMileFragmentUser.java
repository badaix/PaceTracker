package de.badaix.pacetracker.activity.dailymile;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONException;

import java.io.IOException;
import java.util.Date;
import java.util.Vector;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.social.dailymile.DMItem;
import de.badaix.pacetracker.social.dailymile.DailyMile;
import de.badaix.pacetracker.social.dailymile.PersonEntry;
import de.badaix.pacetracker.social.dailymile.User;
import de.badaix.pacetracker.views.dailymile.DailyMileEntry;
import de.badaix.pacetracker.views.dailymile.DailyMileUser;

class DailyMileUserAdapter extends DailyMileItemAdapter {
    private DailyMileUser userView = null;

    public DailyMileUserAdapter(Context context, int textViewResourceId, OnItemTouchListener touchListener, User user,
                                Vector<DMItem> list) {
        super(context, textViewResourceId, touchListener, list);
        setUser(user);
    }

    public void setUser(User user) {
        if ((user != null) && (userView == null))
            userView = new DailyMileUser(context, user);
    }

    @Override
    protected View getItemView(int position, View convertView, ViewGroup parent) {
        synchronized (entries) {
            if (position == 0)
                return userView;

            PersonEntry entry = (PersonEntry) getItem(position - 1);
            if ((convertView != null) && (convertView instanceof DailyMileEntry))
                ((DailyMileEntry) convertView).setEntry(entry, true);
            else
                convertView = new DailyMileEntry(context, entry, true);

            return convertView;
        }
    }
}

public class DailyMileFragmentUser extends DailyMileFragmentMultiPage {
    private User user;
    private boolean hasUserDetails;

    public DailyMileFragmentUser(String title, User user, OnStreamUpdateListener streamUpdateListener) {
        super(title, streamUpdateListener);
        this.user = user;
        hasUserDetails = false;
    }

    @Override
    protected void createAdapter() {
        if (this.adapter == null) {
            this.adapter = new DailyMileUserAdapter(this.getActivity(), R.layout.imageitem, onItemTouchListener, user,
                    this.entries);
            ((DailyMileUserAdapter) adapter).setUser(user);
            setListAdapter(adapter);
        }
    }

    public Vector<PersonEntry> addUserDetails(Vector<PersonEntry> entries) throws IOException, JSONException {
        if (this.adapter != null)
            ((DailyMileUserAdapter) adapter).setUser(user);

        if (hasUserDetails)
            return entries;

        DailyMile dm = new DailyMile(getActivity());
        user = dm.getUser(user.getUsername());
        user.setFriends(dm.getFriends(this.user.getUsername()));
        // createAdapter();
        // ((DailyMileUserAdapter)adapter).setUser(user);
        hasUserDetails = true;
        return entries;
    }

    @Override
    public Vector<PersonEntry> getEntries(int page) throws JSONException, IOException {
        DailyMile dm = new DailyMile(this.getActivity());

        Vector<PersonEntry> result = dm.getPerson(user.getUsername(), page);
        hasMore = (result.size() > 0);
        return addUserDetails(result);
    }

    @Override
    public Vector<PersonEntry> getEntries(Date since) throws JSONException, IOException {
        DailyMile dm = new DailyMile(this.getActivity());

        return addUserDetails(dm.getPerson(user.getUsername(), since));
    }

}
