package de.badaix.pacetracker.weather;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.badaix.pacetracker.session.GeoPos;
import de.badaix.pacetracker.util.DateUtils;
import de.badaix.pacetracker.util.Hint;
import de.badaix.pacetracker.util.HttpDownloader;

public class WorldWeather {
    public static final String WWO_BASE_URL = "http://api.worldweatheronline.com/free/v1/weather.ashx";
    public static final String WWO_API_KEY = "c4r4y7v7pydqqzruk2nwpqph";

    // http://api.worldweatheronline.com/free/v1/weather.ashx?q=50.54+6.06&format=json&date=today&key=c4r4y7v7pydqqzruk2nwpqph
    // http://api.openweathermap.org/data/2.5/weather?lat=50.54&lon=6.06&format=json&date=today&appid=3cfc500db71540f699479b5e5d86c9ca
    // {"coord":{"lon":6.06,"lat":50.54},"weather":[{"id":701,"main":"Mist","description":"mist","icon":"50n"}],"base":"stations","main":{"temp":289.38,"pressure":1019,"humidity":93,"temp_min":287.15,"temp_max":291.15},"visibility":1500,"wind":{"speed":0.5},"clouds":{"all":0},"dt":1501620900,"sys":{"type":1,"id":4853,"message":0.0028,"country":"BE","sunrise":1501560270,"sunset":1501615114},"id":2794990,"name":"Jalhay","cod":200}

    static public int codeToIdx(int code) {
        switch (code) {
            case 395:
                return 0;
            case 392:
                return 1;
            case 389:
                return 2;
            case 386:
                return 3;
            case 377:
                return 4;
            case 374:
                return 5;
            case 371:
                return 6;
            case 368:
                return 7;
            case 365:
                return 8;
            case 362:
                return 9;
            case 359:
                return 10;
            case 356:
                return 11;
            case 353:
                return 12;
            case 350:
                return 13;
            case 338:
                return 14;
            case 335:
                return 15;
            case 332:
                return 16;
            case 329:
                return 17;
            case 326:
                return 18;
            case 323:
                return 19;
            case 320:
                return 20;
            case 317:
                return 21;
            case 314:
                return 22;
            case 311:
                return 23;
            case 308:
                return 24;
            case 305:
                return 25;
            case 302:
                return 26;
            case 299:
                return 27;
            case 296:
                return 28;
            case 293:
                return 29;
            case 284:
                return 30;
            case 281:
                return 31;
            case 266:
                return 32;
            case 263:
                return 33;
            case 260:
                return 34;
            case 248:
                return 35;
            case 230:
                return 36;
            case 227:
                return 37;
            case 200:
                return 38;
            case 185:
                return 39;
            case 182:
                return 40;
            case 179:
                return 41;
            case 176:
                return 42;
            case 143:
                return 43;
            case 122:
                return 44;
            case 119:
                return 45;
            case 116:
                return 46;
            case 113:
                return 47;
        }
        return 48;
    }

    public Weather getCurrentConditions(GeoPos geoPos) throws IOException, JSONException {
        String uriString = WWO_BASE_URL + "?q=" + geoPos.latitude + "+" + geoPos.longitude
                + "&format=json&date=today&key=" + WWO_API_KEY;
        HttpGet httpGet = new HttpGet(uriString);
        HttpClient httpClient = new DefaultHttpClient();
        HttpResponse response = httpClient.execute(httpGet);

        String responseEntity = HttpDownloader.getResponse(response);
        JSONObject json = new JSONObject(responseEntity);

        // http://api.openweathermap.org/data/2.5/weather?lat=50.54&lon=6.06&format=json&date=today&appid=3cfc500db71540f699479b5e5d86c9ca
        // {"coord":{"lon":6.06,"lat":50.54},"weather":[{"id":701,"main":"Mist","description":"mist","icon":"50n"}],"base":"stations","main":{"temp":289.38,"pressure":1019,"humidity":93,"temp_min":287.15,"temp_max":291.15},"visibility":1500,"wind":{"speed":0.5},"clouds":{"all":0},"dt":1501620900,"sys":{"type":1,"id":4853,"message":0.0028,"country":"BE","sunrise":1501560270,"sunset":1501615114},"id":2794990,"name":"Jalhay","cod":200}

        json = json.getJSONObject("data").getJSONArray("current_condition").getJSONObject(0);

        Weather weather = new Weather();
        weather.setCloudCover((float) json.getJSONObject("clouds").getDouble("all") / 100.f)
                .setDescription(json.getJSONArray("weather").getJSONObject(0).getString("description"))
                .setHumidity((float) json.getJSONObject("main").getDouble("humidity") / 100.f)
                .setCopyright(
                        "Powered by <a href=\"http://www.openweathermap.org/\" title=\"Current weather and forecast\" target=\"_blank\">OpenWeatherMap</a>")
                //.setIconUrl(json.getJSONArray("weatherIconUrl").getJSONObject(0).getString("value"))
                //.setPrecipitationMM(json.getInt("precipMM"))
                .setPressure(json.getJSONObject("main").getInt("pressure"))
                .setTempC(json.getJSONObject("main").getInt("temp")).setVisibility(json.getInt("visibility"))
                .setWeatherCode(codeToIdx(json.getInt("weatherCode")))
                .setWindDirDegree(json.getInt("winddirDegree")).setGeoPos(geoPos).setNow(new Date())
                .setWindSpeed(json.getInt("windspeedKmph"));
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("h:mm a", Locale.US);
            weather.setObservationTime(new Date(DateUtils.getStart(new Date()).getTime()
                    + formatter.parse(json.getString("observation_time")).getTime()));
        } catch (ParseException e) {
            weather.setObservationTime(new Date());
            Hint.log("WorldWeather", e);
        }

        return weather;
    }

}
