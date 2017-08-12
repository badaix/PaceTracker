package de.badaix.pacetracker.maps;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;

import de.badaix.pacetracker.social.UrlImageViewCallback;
import de.badaix.pacetracker.social.UrlImageViewHelper;
import de.badaix.pacetracker.util.Helper;
import de.badaix.pacetracker.util.Hint;

public class UrlMarker extends BaseMarker implements UrlImageViewCallback {
    private static HashMap<String, BitmapDescriptor> bmMap = new HashMap<String, BitmapDescriptor>();
    private MarkerOptions markerOptions;
    // private final Context context;
    private ImageView imageView;
    private int size;

    public UrlMarker(final Context context, final GoogleMap googleMap, MarkerOptions markerOptions,
                     MarkerInfo markerInfo) {
        this.googleMap = googleMap;
        this.markerOptions = markerOptions;
        // this.context = context;
        imageView = new ImageView(context);
        imageView.setScaleType(ScaleType.FIT_CENTER);
        size = Helper.dipToPix(context, 24);
        imageView.setDrawingCacheEnabled(true);
        marker = null;
        UrlImageViewHelper.getInstance().setUrlDrawable(imageView, markerInfo.url, null, this);
    }

    public static void clearDescriptors() {
        bmMap.clear();
    }

    @Override
    public void onLoaded(ImageView imageView, String url, boolean loadedFromCache) {
        BitmapDrawable bitmapDrawable = (BitmapDrawable) imageView.getDrawable();

        BitmapDescriptor bmDescription = null;
        if (bmMap.containsKey(url)) {
            bmDescription = bmMap.get(url);
            Hint.log(this, "Recycling bm for: " + url);
        } else {
            float scale = (float) size
                    / (float) Math.max(bitmapDrawable.getBitmap().getWidth(), bitmapDrawable.getBitmap().getHeight());
            bmDescription = BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitmapDrawable.getBitmap(),
                    (int) (bitmapDrawable.getBitmap().getWidth() * scale), (int) (bitmapDrawable.getBitmap()
                            .getHeight() * scale), true));
            bmMap.put(url, bmDescription);
        }

        marker = googleMap.addMarker(markerOptions.icon(bmDescription));
    }

}
