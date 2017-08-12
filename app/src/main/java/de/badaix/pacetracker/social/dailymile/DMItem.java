package de.badaix.pacetracker.social.dailymile;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public abstract class DMItem implements Comparable<DMItem> {
    public abstract JSONObject toJson() throws JSONException;

    @Override
    public int compareTo(DMItem another) {
        // TODO Auto-generated method stub
        return 0;
    }

    public Date getAt() {
        return new Date();
    }
}
