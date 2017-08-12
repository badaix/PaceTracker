package de.badaix.pacetracker.settings;

import android.os.Bundle;

import org.json.JSONObject;

import de.badaix.pacetracker.goal.Goal;
import de.badaix.pacetracker.goal.GoalFactory;
import de.badaix.pacetracker.goal.GoalStandard;
import de.badaix.pacetracker.posprovider.PositionProvider;
import de.badaix.pacetracker.posprovider.PositionProviderFactory;
import de.badaix.pacetracker.sensor.Sensor;
import de.badaix.pacetracker.session.Felt;
import de.badaix.pacetracker.util.Hint;
import de.badaix.pacetracker.weather.Weather;

public class SessionSettings extends BaseSettings {
    private boolean autoStart;
    private boolean autoPause;
    private boolean voiceFeedback;
    private boolean online;
    private Goal goal;
    private JSONObject jsonGoal;
    private PositionProvider positionProvider;
    private JSONObject jsonPosProvider;
    private Sensor sensor;
    private PulseSettings pulseSettings;
    private Bundle bundle = null;
    private String sessionType;
    private String description;
    private String comment;
    private int dailyMileId;
    private int gPlusId;
    private String fbId;
    private Felt felt;
    private Weather weather;

    // private PulseSettings sensorSettings = null;

    // Der Kram muss weg
    // private Session session;
    // private SessionSummary sessionSummary;
    // private static SessionSettings instance = null;

    public SessionSettings(boolean online, JSONObject jsonSettings) {
        super(jsonSettings);
        this.online = online;
        if (online && (jsonSettings == null)) {
            this.setComment("");
            this.setDescription("");
            this.setDailyMileId(-1);
            this.setGPlusId(-1);
            this.setFbId("");
            this.setFelt(Felt.NONE);
            this.setWeather(null);
        }
    }

    public SessionSettings(boolean online) {
        super();
        initWithDefaults();
        this.online = online;
    }

    /**
     * Statische Methode, liefert die einzige Instanz dieser Klasse zurück
     */
    // public static SessionSettings getInstance() {
    // if (instance == null) {
    // instance = new SessionSettings(null);
    // instance.bundle = new Bundle();
    // }
    // return instance;
    // }
    public void clear() {
        // sessionSummary = null;
        goal = null;
        // session = null;
        positionProvider = null;
        bundle.clear();
        bundle = null;
        // instance = null;
    }

    public boolean isOnline() {
        return online;
    }

    public Bundle getBundle() {
        if (bundle == null)
            bundle = new Bundle();
        return bundle;
    }

    public boolean isAutoStart() {
        return autoStart;
    }

    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }

    public boolean isAutoPause() {
        return autoPause;
    }

    public void setAutoPause(boolean autoPause) {
        this.autoPause = autoPause;
    }

    public boolean isVoiceFeedback() {
        Hint.log(this, "isVoiceFeedback: " + voiceFeedback);
        return voiceFeedback;
    }

    public void setVoiceFeedback(boolean voiceFeedback) {
        Hint.log(this, "setVoiceFeedback: " + voiceFeedback);
        this.voiceFeedback = voiceFeedback;
    }

    public PositionProvider getPositionProvider() {
        if (positionProvider == null) {
            if (jsonPosProvider == null) {
                positionProvider = PositionProviderFactory.getOfflinePosProvider(GlobalSettings.getInstance()
                        .getContext(), "Manual");
                jsonPosProvider = PositionProviderFactory.toJson(positionProvider);
            } else {
                positionProvider = PositionProviderFactory.getOfflinePosProvider(GlobalSettings.getInstance()
                        .getContext(), jsonPosProvider);
            }
        }

        return positionProvider;
    }

    public void setPositionProvider(PositionProvider positionProvider) {
        if (this.positionProvider != null)
            this.positionProvider.stop(null);
        this.positionProvider = positionProvider;
        jsonPosProvider = PositionProviderFactory.toJson(positionProvider);
    }

    public Goal getGoal() {
        if (goal == null) {
            if (jsonGoal == null) {
                goal = GoalFactory.getOfflineGoal(GoalStandard.class.getSimpleName(), GlobalSettings.getInstance()
                        .getContext());
                jsonGoal = GoalFactory.toJson(goal);
            } else {
                goal = GoalFactory.getOfflineGoal(jsonGoal, GlobalSettings.getInstance().getContext());
            }
        }
        return goal;
    }

    public void setGoal(Goal goal) {
        this.goal = goal;
        jsonGoal = GoalFactory.toJson(goal);
    }

    private void initWithDefaults() {
        autoStart = true;
        autoPause = true;
        voiceFeedback = true;
        positionProvider = PositionProviderFactory.getOfflinePosProvider(GlobalSettings.getInstance().getContext(),
                "Manual");
        jsonPosProvider = PositionProviderFactory.toJson(positionProvider);
        sessionType = "RunningSession";
        description = "";
        comment = "";
        felt = Felt.NONE;
        dailyMileId = -1;
        gPlusId = -1;
        fbId = "";
        goal = GoalFactory
                .getOfflineGoal(GoalStandard.class.getSimpleName(), GlobalSettings.getInstance().getContext());
        jsonGoal = GoalFactory.toJson(goal);

        sensor = GlobalSettings.getInstance().getSensor();
        pulseSettings = new PulseSettings(null);
    }

    @Override
    public void fromJson(JSONObject json) {
        if (json == null) {
            initWithDefaults();
            return;
        }

        try {
            autoStart = json.getBoolean("autoStart");
            autoPause = json.getBoolean("autoPause");
            voiceFeedback = json.getBoolean("voiceFeedback");
            sessionType = json.getString("sessionType");
            description = json.getString("description");
            comment = json.getString("comment");
            felt = Felt.fromString(json.getString("felt"));
            dailyMileId = json.getInt("dailyMileId");
            sensor = new Sensor(json.getJSONObject("sensor"));
            jsonGoal = json.getJSONObject("goal");
            goal = null;
            pulseSettings = new PulseSettings(json.getJSONObject("pulseSettings"));
            jsonPosProvider = json.getJSONObject("positionProvider");
            positionProvider = null;
            try {
                gPlusId = json.getInt("gPlusId");
            } catch (Exception e) {
                gPlusId = -1;
            }
            try {
                fbId = json.getString("fbId");
            } catch (Exception e) {
                fbId = "";
            }
            try {
                weather = new Weather(json.getJSONObject("weather"));
            } catch (Exception e) {
                weather = null;
            }
        } catch (Exception e) {
            Hint.log(this, e);
            initWithDefaults();
        }
    }

    @Override
    public JSONObject toJson() {
        JSONObject settings = new JSONObject();
        try {
            settings.put("autoStart", autoStart);
            settings.put("autoPause", autoPause);
            settings.put("voiceFeedback", voiceFeedback);
            settings.put("sessionType", sessionType);
            settings.put("description", description);
            settings.put("comment", comment);
            settings.put("felt", felt);
            settings.put("dailyMileId", dailyMileId);
            settings.put("gPlusId", gPlusId);
            settings.put("fbId", fbId);
            settings.put("sensor", sensor.toJson());
            settings.put("goal", GoalFactory.toJson(getGoal()));
            settings.put("pulseSettings", pulseSettings.toJson());
            settings.put("positionProvider", PositionProviderFactory.toJson(getPositionProvider()));
            settings.put("weather", weather.toJson());
        } catch (Exception e) {
        }

        return settings;
    }

    public PulseSettings getPulseSettings() {
        return pulseSettings;
    }

    public void setPulseSettings(PulseSettings pulseSettings) {
        this.pulseSettings = pulseSettings;
    }

    public String getSessionType() {
        return sessionType;
    }

    public void setSessionType(String sessionType) {
        this.sessionType = sessionType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public int getDailyMileId() {
        return dailyMileId;
    }

    public void setDailyMileId(int dailyMileId) {
        this.dailyMileId = dailyMileId;
    }

    public int getGPlusId() {
        return gPlusId;
    }

    public void setGPlusId(int gPlusId) {
        this.gPlusId = gPlusId;
    }

    public String getFbId() {
        return fbId;
    }

    public void setFbId(String fbId) {
        this.fbId = fbId;
    }

    public Felt getFelt() {
        return felt;
    }

    public void setFelt(Felt felt) {
        this.felt = felt;
    }

    public Sensor getSensor() {
        return sensor;
    }

    public void setSensor(Sensor sensor) {
        this.sensor = sensor;
    }

    public Weather getWeather() {
        return weather;
    }

    public void setWeather(Weather weather) {
        this.weather = weather;
    }

}
