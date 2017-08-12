package de.badaix.pacetracker.views;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.social.UrlImageViewCallback;
import de.badaix.pacetracker.social.UrlImageViewHelper;
import de.badaix.pacetracker.util.Distance;
import de.badaix.pacetracker.weather.Weather;

public class WeatherItem extends OverviewItem {
    private Context context;

    public WeatherItem(Context context) {
        super(context, context.getString(R.string.weather));
        addItem(context.getString(R.string.temperature), "--");
        // weatherItem.addItem(getString(R.string.humidity), "--");
        addItem(context.getString(R.string.wind), "--");
        this.context = context;
    }

    public void setWeather(Weather weather) {
        if (weather != null) {
            setValue(0, weather.getDescription(context));
            setValue(1, weather.getTempString());
            // weatherItem.setValue(2, "" + (int)(weather.getHumidity() *
            // 100.f)
            // + "%");
            setValue(
                    2,
                    Distance.speedToString(weather.getWindSpeed()) + " "
                            + GlobalSettings.getInstance().getDistUnit().perHourString() + ", "
                            + weather.getWindDir16Point(context));
            final ImageView imageView = getImageView();
            imageView.setVisibility(View.VISIBLE);
            ImageView icon = new ImageView(context);
            imageView.setImageResource(R.drawable.red_border);
            UrlImageViewHelper.getInstance().setUrlDrawable(icon, weather.getIconUrl(),
                    getResources().getDrawable(R.drawable.transparent), new UrlImageViewCallback() {
                        @Override
                        public void onLoaded(ImageView imageView, String url, boolean loadedFromCache) {
                            getImageView().setBackgroundDrawable(imageView.getDrawable());
                        }
                    });
        }
    }
}
