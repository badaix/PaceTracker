package de.badaix.pacetracker.widgets;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.location.Location;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import de.badaix.pacetracker.PositionListener;
import de.badaix.pacetracker.R;

public class GpsIndicator extends LinearLayout implements PositionListener {
    private ProgressBar progressBarGps;
    private ImageView imageViewFix;
    private AnimationDrawable gpsAnimation;

    public GpsIndicator(Context context) {
        this(context, null);
    }

    public GpsIndicator(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GpsIndicator(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.gps_indicator, this, true);
        progressBarGps = (ProgressBar) findViewById(R.id.progressBarGps);
        progressBarGps.setMax(10);
        progressBarGps.setProgress(0);
        imageViewFix = (ImageView) findViewById(R.id.imageView);
        gpsAnimation = null;
    }

    @Override
    public void onLocationChanged(Location location) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onGpsStatusChanged(boolean active, boolean hasFix, int fixCount, int satCount) {
        if (!active) {
            hasFix = false;
            fixCount = 0;
            satCount = 0;
        }
        progressBarGps.setMax(Math.max(progressBarGps.getMax(), satCount));
        progressBarGps.setSecondaryProgress(satCount);
        if (hasFix)
            progressBarGps.setProgress(fixCount);
        else
            progressBarGps.setProgress(0);

        if (!active) {
            imageViewFix.setImageResource(R.drawable.gps_off);
            if (gpsAnimation != null)
                gpsAnimation.stop();
        } else if (hasFix) {
            imageViewFix.setImageResource(R.drawable.gps_fix);
            if (gpsAnimation != null)
                gpsAnimation.stop();
        } else {
            imageViewFix.setImageResource(R.drawable.gps_no_fix);
            gpsAnimation = (AnimationDrawable) imageViewFix.getDrawable();
            gpsAnimation.start();
        }
    }

}
