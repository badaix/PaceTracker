package de.badaix.pacetracker.social;

import android.widget.ImageView;

public interface UrlImageViewCallback {
    void onLoaded(ImageView imageView, String url, boolean loadedFromCache);
}
