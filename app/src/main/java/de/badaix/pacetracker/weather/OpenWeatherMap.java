package de.badaix.pacetracker.weather;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;

import de.badaix.pacetracker.session.GeoPos;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.util.Hint;
import de.badaix.pacetracker.util.HttpDownloader;

public class OpenWeatherMap {
    public static final String WWO_BASE_URL = "http://api.openweathermap.org/data/2.5/weather";
    // http://api.worldweatheronline.com/free/v1/weather.ashx?q=50.54+6.06&format=json&date=today&key=c4r4y7v7pydqqzruk2nwpqph
    // http://api.openweathermap.org/data/2.5/weather?lat=50.54&lon=6.06&format=json&date=today&appid=3cfc500db71540f699479b5e5d86c9ca
    // {"coord":{"lon":6.06,"lat":50.54},"weather":[{"id":701,"main":"Mist","description":"mist","icon":"50n"}],"base":"stations","main":{"temp":289.38,"pressure":1019,"humidity":93,"temp_min":287.15,"temp_max":291.15},"visibility":1500,"wind":{"speed":0.5},"clouds":{"all":0},"dt":1501620900,"sys":{"type":1,"id":4853,"message":0.0028,"country":"BE","sunrise":1501560270,"sunset":1501615114},"id":2794990,"name":"Jalhay","cod":200}


    ///TODO!!! http://openweathermap.org/weather-conditions
    static public int codeToIdx(int code) {
        switch (code) {
            case 200:
                return 0; //thunderstorm with light rain
            case 201:
                return 1; //thunderstorm with rain
            case 202:
                return 2; //thunderstorm with heavy rain
            case 210:
                return 3; //light thunderstorm
            case 211:
                return 4; //thunderstorm
            case 212:
                return 5; //heavy thunderstorm
            case 221:
                return 6; //ragged thunderstorm
            case 230:
                return 7; //thunderstorm with light drizzle
            case 231:
                return 8; //thunderstorm with drizzle
            case 232:
                return 9; //thunderstorm with heavy drizzle

            case 300:
                return 10; //light intensity drizzle
            case 301:
                return 11; //drizzle
            case 302:
                return 12; //heavy intensity drizzle
            case 310:
                return 13; //light intensity drizzle rain
            case 311:
                return 14; //drizzle rain
            case 312:
                return 15; //heavy intensity drizzle rain
            case 313:
                return 16; //shower rain and drizzle
            case 314:
                return 17; //heavy shower rain and drizzle
            case 321:
                return 18; //shower drizzle

            case 500:
                return 19; //light rain
            case 501:
                return 20; //moderate rain:

            case 502:
                return 21; //heavy intensity rain
            case 503:
                return 22; //very heavy rain
            case 504:
                return 23; //extreme rain
            case 511:
                return 24; //freezing rain
            case 520:
                return 25; //light intensity shower rain
            case 521:
                return 26; //shower rain
            case 522:
                return 27; //heavy intensity shower rain
            case 531:
                return 28; //ragged shower rain

            case 600:
                return 29; //light snow
            case 601:
                return 30; //snow
            case 602:
                return 31; //heavy snow
            case 611:
                return 32; //sleet
            case 612:
                return 33; //shower sleet
            case 615:
                return 34; //light rain and snow
            case 616:
                return 35; //rain and snow
            case 620:
                return 36; //light shower snow
            case 621:
                return 37; //shower snow
            case 622:
                return 38; //heavy shower snow

            case 701:
                return 39; //mist
            case 711:
                return 40; //smoke
            case 721:
                return 41; //haze
            case 731:
                return 42; //sand, dust whirls
            case 741:
                return 43; //fog
            case 751:
                return 44; //sand
            case 761:
                return 45; //dust
            case 762:
                return 46; //volcanic ash
            case 771:
                return 47; //squalls
            case 781:
                return 48; //tornado

            case 800:
                return 49; //clear sky
            case 801:
                return 50; //few clouds
            case 802:
                return 51; //scattered clouds
            case 803:
                return 52; //broken clouds
            case 804:
                return 53; //overcast clouds

            case 900:
                return 54; //tornado
            case 901:
                return 55; //tropical storm
            case 902:
                return 56; //hurricane
            case 903:
                return 57; //cold
            case 904:
                return 58; //hot
            case 905:
                return 59; //windy
            case 906:
                return 60; //hail

            case 951:
                return 61; //calm
            case 952:
                return 62; //light breeze
            case 953:
                return 63; //gentle breeze
            case 954:
                return 64; //moderate breeze
            case 955:
                return 65; //fresh breeze
            case 956:
                return 66; //strong breeze
            case 957:
                return 67; //high wind, near gale
            case 958:
                return 68; //gale
            case 959:
                return 69; //severe gale
            case 960:
                return 70; //storm
            case 961:
                return 71; //violent storm
            case 962:
                return 72; //hurricane
        }
        return 61;
    }

    public Weather getCurrentConditions(GeoPos geoPos) throws IOException, JSONException {

        String uriString = WWO_BASE_URL + "?lat=" + geoPos.latitude + "&lon=" + geoPos.longitude
                + "&format=json&date=today&units=metric&appid=" + GlobalSettings.getInstance().getMetaData("open_weather_map.api_key");
        Hint.log(this, "getCurrentConditions URI: " + uriString);
        HttpGet httpGet = new HttpGet(uriString);
        HttpClient httpClient = new DefaultHttpClient();
        HttpResponse response = httpClient.execute(httpGet);

        String responseEntity = HttpDownloader.getResponse(response);
        JSONObject json = new JSONObject(responseEntity);
        Hint.log(this, "getCurrentConditions json: " + json);

        // http://api.openweathermap.org/data/2.5/weather?lat=50.54&lon=6.06&format=json&units=metric&date=today&appid=3cfc500db71540f699479b5e5d86c9ca
        // http://api.openweathermap.org/data/2.5/weather?lat=50.54&lon=6.06&format=json&date=today&appid=3cfc500db71540f699479b5e5d86c9ca
        // {"coord":{"lon":6.06,"lat":50.54},"weather":[{"id":701,"main":"Mist","description":"mist","icon":"50n"}],"base":"stations","main":{"temp":289.38,"pressure":1019,"humidity":93,"temp_min":287.15,"temp_max":291.15},"visibility":1500,"wind":{"speed":0.5},"clouds":{"all":0},"dt":1501620900,"sys":{"type":1,"id":4853,"message":0.0028,"country":"BE","sunrise":1501560270,"sunset":1501615114},"id":2794990,"name":"Jalhay","cod":200}

        //json = json.getJSONObject("data").getJSONArray("current_condition").getJSONObject(0);
        JSONObject jweather = json.getJSONArray("weather").getJSONObject(0);
        JSONObject jwind = json.getJSONObject("wind");
        JSONObject jmain = json.getJSONObject("main");

        Weather weather = new Weather();
        weather.setCloudCover((float) json.getJSONObject("clouds").getDouble("all") / 100.f)
                .setDescription(jweather.getString("description"))
                .setHumidity((float) jmain.getDouble("humidity") / 100.f)
                .setCopyright(
                        "Powered by <a href=\"http://www.openweathermap.org/\" title=\"Current weather and forecast\" target=\"_blank\">OpenWeatherMap</a>")
                .setIconUrl("http://openweathermap.org/img/w/" + jweather.getString("icon") + ".png")
                //.setPrecipitationMM(json.getInt("precipMM"))
                .setPressure(jmain.optInt("pressure", 0))
                .setTempC(jmain.optInt("temp", 0))
                //.setVisibility(json.optInt("visibility", -1))
                .setWeatherCode(codeToIdx(jweather.getInt("id")))
                .setWindDirDegree((int) jwind.optDouble("deg", 0)).setGeoPos(geoPos).setNow(new Date())
                .setWindSpeed((int) jwind.optDouble("speed", 0))
                .setObservationTime(new Date(1000 * json.optLong("dt", 0)));

        return weather;
    }

}
