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
import de.badaix.pacetracker.social.dailymile.User;
import de.badaix.pacetracker.views.dailymile.DailyMileFriend;

class DailyMileFriendAdapter extends DailyMileItemAdapter {

    public DailyMileFriendAdapter(Context context, int textViewResourceId, OnItemTouchListener touchListener,
                                  Vector<DMItem> list) {
        super(context, textViewResourceId, touchListener, list);
    }

    @Override
    protected View getItemView(int position, View convertView, ViewGroup parent) {
        synchronized (entries) {
            DMItem entry = getItem(position);
            if (!(entry instanceof User))
                return null;

            User user = (User) entry;
            if (convertView != null)
                ((DailyMileFriend) convertView).setUser(user);
            else
                convertView = new DailyMileFriend(context, user);

            // ((DailyMileFriend)convertView).setOnItemTouchListener(touchListener);
            return convertView;
        }
    }
}

public class DailyMileFragmentFriends extends DailyMileFragment {

    boolean friendsLoaded = false;
    private User user;

    public DailyMileFragmentFriends(String title, User user, OnStreamUpdateListener streamUpdateListener) {
        super(title, streamUpdateListener);
        this.user = user;
    }

    @Override
    protected void createAdapter() {
        if (this.adapter == null) {
            this.adapter = new DailyMileFriendAdapter(this.getActivity(), R.layout.imageitem, onItemTouchListener,
                    this.entries);
            setListAdapter(adapter);
        }
    }

    public Vector<User> getFriends() throws IOException, JSONException {
        if (friendsLoaded)
            return user.getFriends();

        DailyMile dm = new DailyMile(getActivity());
        user.setFriends(dm.getFriends(this.user.getUsername()));
        friendsLoaded = true;
        return user.getFriends();
    }

    @Override
    public Vector<User> getEntries(int page) throws JSONException, IOException {

        return getFriends();
    }

    @Override
    public Vector<User> getEntries(Date since) throws JSONException, IOException {

        return getFriends();
    }

}
