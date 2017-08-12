package de.badaix.pacetracker.session;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import de.badaix.pacetracker.settings.JsonSerializable;
import de.badaix.pacetracker.settings.SessionSettings;
import de.badaix.pacetracker.util.Hint;
import de.badaix.pacetracker.util.LocationUtils;

public class SessionSummary extends SessionType implements JsonSerializable {
    protected long totalPause;
    protected SessionSettings settings;
    private String filename = "";
    private Date sessionStart = null;
    private Date sessionStop = null;
    private float maxSpeed;
    private int calories;
    private double distance;
    private GeoPos startPos = null;
    private long duration;
    private int id;
    private short hrMin;
    private short hrMax;
    private float hrMean;

    public SessionSummary(SessionType sessionData) {
        super(sessionData.type, sessionData.name, sessionData.verb, sessionData.drawable, sessionData.lightDrawable);
        id = -1;
    }

    public SessionSummary(SessionSettings settings) {
        super(settings == null ? null : SessionFactory.getInstance().getSessionData(settings.getSessionType()));
        id = -1;
        this.settings = settings;
    }

    public SessionSummary(JSONObject json) throws JSONException {
        // super();
        super(json.getJSONObject("sessionType"));
        id = -1;
        fromJson(json);
    }

    public SessionSummary(String type, String name, String verb, int drawable, int lightDrawable) {
        super(type, name, verb, drawable, lightDrawable);
        id = -1;
        this.settings = new SessionSettings(false);
        // setSession(null);
    }

    public SessionSettings getSettings() {
        return settings;
    }

    public Date getSessionStart() {
        return sessionStart;
    }

    public void setSessionStart(Date sessionStart) {
        this.sessionStart = sessionStart;
    }

    public Date getSessionStop() {
        return sessionStop;
    }

    public void setSessionStop(Date sessionStop) {
        this.sessionStop = sessionStop;
    }

    public long getTotalPause() {
        return totalPause;
    }

    public void setTotalPause(long totalPause) {
        this.totalPause = totalPause;
    }

    public float getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(float maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public int getCalories() {
        return calories;
    }

    public void setCalories(int calories) {
        this.calories = calories;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public float getAvgSpeed() {
        if (getDuration() == 0)
            return 0;
        return (float) (3600.f * (getDistance() / getDuration()));
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public GeoPos getStartPos() {
        return startPos;
    }

    public void setStartPos(GeoPos startPos) {
        this.startPos = startPos;
    }

    public short getHrMin() {
        return hrMin;
    }

    public void setHrMin(short hrMin) {
        this.hrMin = hrMin;
    }

    public short getHrMax() {
        return hrMax;
    }

    public void setHrMax(short hrMax) {
        this.hrMax = hrMax;
    }

    public float getHrMean() {
        return hrMean;
    }

    public void setHrMean(float hrMean) {
        this.hrMean = hrMean;
    }

    public boolean hasHr() {
        return getSettings().getPulseSettings().isSensorEnabled();
    }

    @Override
    public void fromJson(JSONObject json) {
        try {
            super.fromJson(json.getJSONObject("sessionType"));
            filename = json.getString("filename");
            settings = new SessionSettings(false, json.getJSONObject("settings"));
            try {
                sessionStart = new Date(json.getLong("sessionStart"));
            } catch (Exception e) {
                sessionStart = null;
            }
            try {
                sessionStop = new Date(json.getLong("sessionStop"));
            } catch (Exception e) {
                sessionStop = null;
            }
            // sessionStop = new Date(json.getLong("sessionStop"));
            totalPause = json.getLong("totalPause");
            maxSpeed = (float) json.getDouble("maxSpeed");
            calories = json.getInt("calories");
            distance = json.getDouble("distance");
            startPos = LocationUtils.geoPosFromJson(json.getJSONObject("startPos"));
            duration = json.getLong("duration");
            id = json.getInt("id");
            if (json.has("hrMin"))
                hrMin = (short) json.getInt("hrMin");
            if (json.has("hrMax"))
                hrMax = (short) json.getInt("hrMax");
            if (json.has("hrMean"))
                hrMean = (float) json.getDouble("hrMean");
        } catch (Exception e) {
            Hint.log(this, e);
        }
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("filename", filename);
            json.put("sessionType", super.toJson());
            json.put("settings", settings.toJson());
            if (sessionStart != null)
                json.put("sessionStart", sessionStart.getTime());
            if (sessionStop != null)
                json.put("sessionStop", sessionStop.getTime());
            json.put("totalPause", totalPause);
            json.put("maxSpeed", maxSpeed);
            json.put("calories", calories);
            json.put("distance", distance);
            json.put("startPos", LocationUtils.geoPosToJson(startPos));
            json.put("duration", duration);
            json.put("id", id);
            json.put("hrMin", hrMin);
            json.put("hrMax", hrMax);
            json.put("hrMean", (double) hrMean);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return json;
    }

}
