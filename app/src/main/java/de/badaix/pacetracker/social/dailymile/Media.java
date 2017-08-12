package de.badaix.pacetracker.social.dailymile;

import org.json.JSONException;
import org.json.JSONObject;

public class Media extends JsonSerializable {
    private Preview preview = null;
    private Content content = null;

    public Media(JSONObject json) throws JSONException {
        if (json.has("preview"))
            this.preview = new Preview(json.getJSONObject("preview"));
        if (json.has("content"))
            this.content = new Content(json.getJSONObject("content"));
    }

    public Media(Preview preview, Content content) {
        this.preview = preview;
        this.content = content;
    }

    public Preview getPreview() {
        return preview;
    }

    public Content getContent() {
        return content;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        if (preview != null)
            json.put("preview", preview.toJson());
        if (content != null)
            json.put("content", content.toJson());
        return json;
    }

    @Override
    public String toString() {
        String strPreview = "";
        if (preview != null)
            strPreview = preview.toString();

        String strContent = "";
        if (content != null)
            strContent = content.toString();

        return "preview: " + strPreview + "; content: " + strContent;
    }

    public enum Type {
        IMAGE("image"), VIDEO("video");

        private String asString = "";

        Type(String asString) {
            this.asString = asString;
        }

        static Type fromString(String type) {
            if (type.equalsIgnoreCase(IMAGE.toString()))
                return IMAGE;
            if (type.equalsIgnoreCase(VIDEO.toString()))
                return VIDEO;
            throw new IllegalArgumentException("Unknown content type: " + type);
        }

        @Override
        public String toString() {
            return asString;
        }
    }

    public class Content {

        private Type type = null;
        private String url = null;

        public Content(JSONObject json) throws JSONException {
            this(Type.fromString(json.getString("type")), json.getString("url"));
        }

        public Content(Type type, String url) {
            this.type = type;
            this.url = url;
        }

        public Type getType() {
            return type;
        }

        public String getUrl() {
            return url;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("type", type);
            json.put("url", url);
            return json;
        }

        @Override
        public String toString() {
            return "type: " + this.type + "; " + this.url;
        }
    }

    public class Preview {
        Integer width = null;
        Integer height = null;
        private Type type = null;
        private String url = null;

        public Preview(JSONObject json) throws JSONException {
            if (json.has("type"))
                this.type = Type.fromString(json.getString("type"));
            if (json.has("url"))
                this.url = json.getString("url");
            if (json.has("width"))
                this.width = json.getInt("width");
            if (json.has("height"))
                this.height = json.getInt("height");
        }

        public Preview(Type type, String url, Integer width, Integer height) {
            this.type = type;
            this.url = url;
            this.width = width;
            this.height = height;
        }

        public Type getType() {
            return type;
        }

        public String getUrl() {
            return url;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            if (type != null)
                json.put("type", type);
            if (url != null)
                json.put("url", url);
            if (width != null)
                json.put("width", width);
            if (height != null)
                json.put("height", height);
            return json;
        }

        @Override
        public String toString() {
            return "type: " + this.type + "; width: " + this.width + "; height: " + this.height + "; url: " + this.url;
        }

    }
}
