package de.badaix.pacetracker.social.dailymile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Vector;

import de.badaix.pacetracker.util.DateUtils;

public class PersonEntry extends DMItem {
    int id = -1;
    String url = null;
    Date at = null;
    String message = null;
    Vector<Comment> comments = new Vector<Comment>();
    Vector<Media> media = new Vector<Media>();
    Vector<Like> likes = new Vector<Like>();
    Location location = null;
    User user = null;
    Workout workout = null;

    public PersonEntry(User user) {
        Date now = new Date();
        this.setAt(new Date(now.getTime() + 1000 * 60 * 60 * 24 * 365));
        this.user = user;
        this.id = -1;
    }

    public PersonEntry(JSONObject json) throws JSONException {
        update(json);
    }

    public int getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public Date getAt() {
        return at;
    }

    public void setAt(Date at) {
        this.at = at;
    }

    public String getMessage() {
        return message;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public User getUser() {
        return user;
    }

    public Workout getWorkout() {
        return workout;
    }

    public Vector<Comment> getComments() {
        return comments;
    }

    public Vector<Like> getLikes() {
        return likes;
    }

    public Vector<Media> getMedia() {
        return media;
    }

    public void update(JSONObject json) throws JSONException {
        at = null;
        message = null;
        comments.clear();
        media.clear();
        likes.clear();
        location = null;
        user = null;
        workout = null;

        id = json.getInt("id");
        url = json.getString("url");
        try {
            at = DateUtils.fromISODateString(json.getString("at"));
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (json.has("message"))
            message = json.getString("message");
        if (json.has("location"))
            location = new Location(json.getJSONObject("location"));
        if (json.has("user"))
            user = new User(json.getJSONObject("user"));
        if (json.has("workout"))
            workout = new Workout(json.getJSONObject("workout"));
        if (json.has("comments")) {
            JSONArray jComments = json.getJSONArray("comments");
            for (int i = 0; i < jComments.length(); ++i)
                comments.add(new Comment(jComments.getJSONObject(i)));
        }
        if (json.has("likes")) {
            JSONArray jLikes = json.getJSONArray("likes");
            for (int i = 0; i < jLikes.length(); ++i)
                likes.add(new Like(jLikes.getJSONObject(i)));
        }
        if (json.has("media")) {
            JSONArray jMedia = json.getJSONArray("media");
            for (int i = 0; i < jMedia.length(); ++i)
                media.add(new Media(jMedia.getJSONObject(i)));
        }
    }

    @Override
    public String toString() {
        return "id: " + id + "; url: " + url + "; at: " + DateUtils.toISOString(at) + "; Location: "
                + location.toString() + "; User: " + user.toString() + "; Workout: " + workout.toString();
    }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject result = new JSONObject();
        result.put("id", id);
        result.put("url", url);
        result.put("at", DateUtils.toISOString(at));
        if (message != null)
            result.put("message", message);
        if (location != null)
            result.put("location", location.toJson());
        if (user != null)
            result.put("user", user.toJson());
        if (workout != null)
            result.put("workout", workout.toJson());
        if (!comments.isEmpty()) {
            JSONArray jComments = new JSONArray();
            for (int i = 0; i < comments.size(); ++i)
                jComments.put(comments.get(i).toJson());
            result.put("comments", jComments);
        }
        if (!likes.isEmpty()) {
            JSONArray jLikes = new JSONArray();
            for (int i = 0; i < likes.size(); ++i)
                jLikes.put(likes.get(i).toJson());
            result.put("likes", jLikes);
        }
        if (!media.isEmpty()) {
            JSONArray jMedia = new JSONArray();
            for (int i = 0; i < media.size(); ++i)
                jMedia.put(media.get(i).toJson());
            result.put("media", jMedia);
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final PersonEntry other = (PersonEntry) obj;
        if ((other.getId() == -1) && (this.getId() == -1))
            return (other.getUser().displayName.equals(this.getUser().displayName));
        return (other.getId() == this.getId());// && (this.getComments().size()
        // == other
        // .getComments().size()));
    }

    @Override
    public int compareTo(DMItem entry) {
        PersonEntry personEntry = (PersonEntry) entry;
        if ((this.getId() == -1) && (personEntry.getId() == -1))
            return this.getUser().getDisplayName().compareTo(personEntry.getUser().getDisplayName());
        return entry.getAt().compareTo(this.getAt());
    }
}
