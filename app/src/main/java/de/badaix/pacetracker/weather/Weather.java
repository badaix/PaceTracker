package de.badaix.pacetracker.weather;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.session.GeoPos;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.social.dailymile.JsonSerializable;
import de.badaix.pacetracker.util.Distance;
import de.badaix.pacetracker.util.Hint;

public class Weather extends JsonSerializable {
    private Date observationTime = new Date();
    private float cloudCover;
    private int pressure;
    private int visibility;
    private float tempC;
    private float windSpeed;
    private int precipitationMM;
    private int windDirDegree;
    private String iconUrl;
    private float humidity;
    private int weatherCode;
    private String description;
    private String copyright;
    private GeoPos geoPos;
    private Date now;

    public Weather() {
    }

    public Weather(JSONObject json) {
        try {
            JSONObject jsonGeoPos = json.getJSONObject("geoPos");
            this.setCloudCover(json.getInt("cloudCover")).setCopyright(json.getString("copyright"))
                    .setDescription(json.getString("description")).setHumidity((float) json.getDouble("humidity"))
                    .setIconUrl(json.getString("iconUrl"))
                    .setObservationTime(new Date(json.getLong("observationTime")))
                    .setPrecipitationMM(json.getInt("precipitationMM")).setPressure(json.getInt("pressure"))
                    .setTempC((float) json.getDouble("tempC")).setVisibility(json.getInt("visibility"))
                    .setWeatherCode(json.getInt("weatherCode"))
                    .setWindDirDegree(json.getInt("windDirDegree")).setWindSpeed((float) json.getDouble("windSpeed"))
                    .setNow(new Date(json.getLong("now")))
                    .setGeoPos(new GeoPos(jsonGeoPos.getDouble("Lat"), jsonGeoPos.getDouble("Lon")));
        } catch (JSONException e) {
            Hint.log(this, e);
        }
    }

    public Date getObservationTime() {
        return observationTime;
    }

    public Weather setObservationTime(Date observationTime) {
        this.observationTime = observationTime;
        return this;
    }

    public float getCloudCover() {
        return cloudCover;
    }

    public Weather setCloudCover(float cloudCover) {
        this.cloudCover = cloudCover;
        return this;
    }

    public int getPressure() {
        return pressure;
    }

    public Weather setPressure(int pressure) {
        this.pressure = pressure;
        return this;
    }

    public int getVisibility() {
        return visibility;
    }

    public Weather setVisibility(int visibility) {
        this.visibility = visibility;
        return this;
    }

    public float getTempC() {
        return tempC;
    }

    public Weather setTempC(float tempC) {
        this.tempC = tempC;
        return this;
    }

    public float getTempF() {
        return (float) Math.floor(getTempC() * 1.8f + 32);
    }

    public String getTempString() {
        if (GlobalSettings.getInstance().getDistSystem() == Distance.System.IMPERIAL)
            return Distance.doubleToString(getTempF(), 1) + "°F";
        else
            return Distance.doubleToString(getTempC(), 1) + "°C";
    }

    public float getTemp() {
        if (GlobalSettings.getInstance().getDistSystem() == Distance.System.IMPERIAL)
            return getTempF();
        else
            return getTempC();
    }

    public float getWindSpeed() {
        return windSpeed;
    }

    public Weather setWindSpeed(float windSpeed) {
        this.windSpeed = windSpeed;
        return this;
    }

    public int getPrecipitationMM() {
        return precipitationMM;
    }

    public Weather setPrecipitationMM(int precipitationMM) {
        this.precipitationMM = precipitationMM;
        return this;
    }

    public int getWindDirDegree() {
        return windDirDegree;
    }

    public Weather setWindDirDegree(int windDirDegree) {
        this.windDirDegree = windDirDegree;
        return this;
    }

    public String getWindDir16Point(Context context) {
        int dir = Math.round((float) getWindDirDegree() / 22.5f);
        if (dir >= 16)
            dir = 0;
        String dirs[] = context.getResources().getStringArray(R.array.wind_dir_array);
        if (dirs.length > dir)
            return dirs[dir];
        return "";
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public Weather setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
        return this;
    }

    public float getHumidity() {
        return humidity;
    }

    public Weather setHumidity(float humidity) {
        this.humidity = humidity;
        return this;
    }

    public int getWeatherCode() {
        return weatherCode;
    }

    public Weather setWeatherCode(int weatherCode) {
        this.weatherCode = weatherCode;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Weather setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getDescription(Context context) {
        String desc[] = context.getResources().getStringArray(R.array.weather_array);
        if (desc.length > getWeatherCode())
            return desc[getWeatherCode()];
        return getDescription();
    }

    public String getCopyright() {
        return copyright;
    }

    public Weather setCopyright(String copyright) {
        this.copyright = copyright;
        return this;
    }

    public GeoPos getGeoPos() {
        return geoPos;
    }

    public Weather setGeoPos(GeoPos geoPos) {
        this.geoPos = geoPos;
        return this;
    }

    public Date getNow() {
        return now;
    }

    public Weather setNow(Date now) {
        this.now = now;
        return this;
    }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("observationTime", observationTime.getTime());
        json.put("cloudCover", cloudCover);
        json.put("pressure", pressure);
        json.put("visibility", visibility);
        json.put("tempC", tempC);
        json.put("windSpeed", windSpeed);
        json.put("precipitationMM", precipitationMM);
        json.put("windDirDegree", windDirDegree);
        json.put("iconUrl", iconUrl);
        json.put("humidity", humidity);
        json.put("weatherCode", weatherCode);
        json.put("description", description);
        json.put("copyright", copyright);
        JSONObject jsonGeoPos = new JSONObject();
        jsonGeoPos.put("Lat", geoPos.latitude);
        jsonGeoPos.put("Lon", geoPos.longitude);
        json.put("geoPos", jsonGeoPos);
        json.put("now", now.getTime());
        return json;
    }

}
