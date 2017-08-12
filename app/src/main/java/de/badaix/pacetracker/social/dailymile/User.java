package de.badaix.pacetracker.social.dailymile;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Vector;

public class User extends DMItem {
    String username = null;
    String displayName = null;
    String photoUrl = null;
    String url = null;
    String goal = null;
    String location = null;
    Vector<User> friends = null;

    public User(JSONObject json) throws JSONException {
        this.username = json.getString("username");
        this.displayName = json.getString("display_name");
        this.photoUrl = json.getString("photo_url");
        this.url = json.getString("url");
        if (json.has("goal"))
            this.goal = json.getString("goal");
        if (json.has("location"))
            this.location = json.getString("location");
    }

    public User(String username, String displayName, String photoUrl, String url, String goal, String location) {
        this.username = username;
        this.displayName = displayName;
        this.photoUrl = photoUrl;
        this.url = url;
        this.goal = goal;
        this.location = location;
    }

    public String getUsername() {
        return username;
    }

    public Vector<User> getFriends() {
        return friends;
    }

    public void setFriends(Vector<User> friends) {
        this.friends = friends;
    }

    public String getLocation() {
        return location;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public String getAvatarPhotoUrl() {
        return getPhotoUrl();
    }

    public String getMiniPhotoUrl() {
        String url = getPhotoUrl();
        if (url.contains("avatar."))
            url = url.replace("avatar.", "mini.");
        return url;
    }

    public String getProfilePhotoUrl() {
        String url = getPhotoUrl();
        if (url.contains("avatar."))
            url = url.replace("avatar.", "profile.");
        return url;
    }

    public String getUrl() {
        return url;
    }

    public String getGoal() {
        return goal;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("username", username);
        json.put("display_name", displayName);
        json.put("photo_url", photoUrl);
        json.put("url", url);
        if (goal != null)
            json.put("goal", goal);
        if (location != null)
            json.put("location", location);
        return json;
    }

    @Override
    public String toString() {
        return "username: " + username + "; displayName: " + displayName + "; photoUrl: " + photoUrl + "; url: " + url;
    }

    @Override
    public int compareTo(DMItem another) {
        User user = (User) another;
        return this.getDisplayName().compareTo(user.getDisplayName());
    }

}
