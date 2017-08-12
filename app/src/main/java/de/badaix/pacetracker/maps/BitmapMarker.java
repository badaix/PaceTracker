package de.badaix.pacetracker.maps;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.maps.MarkerInfo.MarkerType;
import de.badaix.pacetracker.social.ImageCache;
import de.badaix.pacetracker.util.Helper;

public class BitmapMarker extends BaseMarker {
    private Context context;

    public BitmapMarker(final Context context, final GoogleMap googleMap, MarkerOptions markerOptions,
                        MarkerInfo markerInfo, int textSize, String text) {
        this.context = context;
        Bitmap bmp = createMarker(markerInfo.markerType, textSize, text);
        markerOptions = markerOptions.icon(BitmapDescriptorFactory.fromBitmap(bmp));
        marker = googleMap.addMarker(markerOptions);
    }

    private Bitmap createMarker(MarkerType markerType, int textSize, String text) {
        Bitmap bm = ImageCache.getInstance().getBitmapFromCache(markerType.toString() + textSize + text);
        if (bm != null)
            return bm;

        Paint paint = new Paint();
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(Helper.spToPix(context, textSize));
        paint.setARGB(255, 255, 255, 255);
        int marker;
        if (markerType == MarkerType.finish)
            marker = R.drawable.marker_small_finish;
        else if (markerType == MarkerType.start)
            marker = R.drawable.marker_small_start;
        else
            marker = R.drawable.marker_small;

        bm = BitmapFactory.decodeResource(context.getResources(), marker);
        android.graphics.Bitmap.Config bitmapConfig = bm.getConfig();
        if (bitmapConfig == null) {
            bitmapConfig = android.graphics.Bitmap.Config.ARGB_8888;
        }
        bm = bm.copy(bitmapConfig, true);

        int sizeX = bm.getWidth();
        int sizeY = bm.getHeight();
        float textOffset = sizeY - paint.getTextSize();
        Canvas canvas = new Canvas(bm);
        canvas.drawText(text, sizeX / 2, textOffset, paint);
        ImageCache.getInstance().addBitmapToCache(markerType.toString() + textSize + text, bm);
        return bm;
    }

}
