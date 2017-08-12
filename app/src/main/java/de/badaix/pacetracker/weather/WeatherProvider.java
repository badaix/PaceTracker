package de.badaix.pacetracker.weather;

import android.annotation.TargetApi;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;

import java.util.Date;

import de.badaix.pacetracker.session.GeoPos;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.util.Distance;
import de.badaix.pacetracker.util.Hint;

public class WeatherProvider {

    private static Object syncObj = new Object();
    private WeatherListener listener;

    public WeatherProvider(WeatherListener listener) {
        this.listener = listener;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void getWeather(GeoPos geoPos) {
        WeatherTask weatherTask = new WeatherTask(geoPos, listener);
        if (Build.VERSION.SDK_INT >= 13)
            weatherTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
        else
            weatherTask.execute((Void) null);
    }

    public void getWeather(Location location) {
        if (GlobalSettings.getInstance().isPro())
            getWeather(new GeoPos(location.getLatitude(), location.getLongitude()));
    }

    public interface WeatherListener {
        public void onWeatherData(Weather weather, Exception exception);
    }

    private class WeatherTask extends AsyncTask<Void, Void, Weather> {
        private GeoPos geoPos;
        private WeatherListener listener;
        private Exception exception;

        public WeatherTask(GeoPos geoPos, WeatherListener listener) {
            this.geoPos = geoPos;
            this.listener = listener;
            this.exception = null;
        }

        @Override
        protected Weather doInBackground(Void... arg0) {
            exception = null;
            Weather weather = GlobalSettings.getInstance().getLastWeather();

            synchronized (syncObj) {
                if (weather != null) {
                    if ((new Date().getTime()) - weather.getNow().getTime() < 60 * 60 * 1000) {
                        if (Distance.calculateDistance(weather.getGeoPos(), geoPos) < 20 * 1000) {
                            Hint.log(this, "Found recent weather");
                            return weather;
                        }
                    }
                }

                OpenWeatherMap openWeatherMap = new OpenWeatherMap();
                try {
                    weather = openWeatherMap.getCurrentConditions(geoPos);
                    GlobalSettings.getInstance().setLastWeather(weather);
                } catch (Exception e) {
                    Hint.log(this, e);
                    exception = e;
                }
                return null;
            }

        }

        @Override
        protected void onPostExecute(Weather result) {
            if (listener != null) {
                listener.onWeatherData(result, exception);
            }
        }
    }
}
