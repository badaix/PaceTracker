package de.badaix.pacetracker.social.dailymile;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import de.badaix.pacetracker.util.DateUtils;

public class Comment extends JsonSerializable {
    String body = null;
    Date createdAt = null;
    User user = null;

    public Comment(JSONObject json) throws JSONException {
        if (json.has("body"))
            this.body = json.getString("body");
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

    Comment(String body, Date createdAt, User user) {
        this.body = body;
        this.createdAt = createdAt;
        this.user = user;
    }

    public String getBody() {
        return body;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public User getUser() {
        return user;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        if (body != null)
            json.put("body", body);
        if (createdAt != null)
            json.put("created_at", DateUtils.toISOString(createdAt));
        if (user != null)
            json.put("user", user.toJson());
        return json;
    }

}
