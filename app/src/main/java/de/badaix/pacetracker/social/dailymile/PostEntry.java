package de.badaix.pacetracker.social.dailymile;

import org.json.JSONException;
import org.json.JSONObject;

public class PostEntry extends JsonSerializable {
    int id = -1;
    String message = null;
    Position position = null;
    Workout workout = null;

    PostEntry() {
    }

    public PostEntry(String message, Position position, Workout workout) {
        this.message = message;
        this.position = position;
        this.workout = workout;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        if (message != null) {
            json.put("message", message);
            // message.replaceAll("ä", "ae")
            // .replaceAll("ü", "ue").replaceAll("ö", "oe")
            // .replaceAll("Ä", "Ae").replaceAll("Ö", "Oe").replaceAll("Ü",
            // "Ue")
            // );
        }
        if (position != null) {
            json.put("lat", position.latitude);
            json.put("lon", position.longitude);
        }
        if (workout != null)
            json.put("workout", workout.toJson());
        return json;
    }
}
