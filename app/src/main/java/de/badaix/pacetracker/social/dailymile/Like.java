package de.badaix.pacetracker.social.dailymile;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import de.badaix.pacetracker.util.DateUtils;

public class Like extends JsonSerializable {
    Date createdAt = null;
    User user = null;

    Like(JSONObject json) throws JSONException {
        if (json.has("created_at"))
            try {
                this.createdAt = DateUtils.fromISODateString(json.getString("created_at"));
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        if (json.has("user"))
            user = new User(json.getJSONObject("user"));
    }

    Like(Date createdAt, User user) {
        this.createdAt = createdAt;
        this.user = user;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public User getUser() {
        return user;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        if (createdAt != null)
            json.put("created_at", DateUtils.toISOString(createdAt));
        if (user != null)
            json.put("user", user.toJson());
        return json;
    }

}
