package de.badaix.pacetracker.widgets;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import de.badaix.pacetracker.R;

public class ImageViewHeart extends ImageView {
    private AnimationDrawable hrsAnimation;

    public ImageViewHeart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.setImageResource(R.drawable.heart_disconnected);
        hrsAnimation = null;
    }

    public ImageViewHeart(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setImageResource(R.drawable.heart_disconnected);
        hrsAnimation = null;
    }

    public void setConnected() {
        setImageResource(R.drawable.heart_beat);
        hrsAnimation = (AnimationDrawable) getDrawable();
        hrsAnimation.start();
    }

    public void setConnecting() {
        if (hrsAnimation != null)
            hrsAnimation.stop();
        setImageResource(R.drawable.heart_connected);
    }

    public void setDisconnected() {
        if (hrsAnimation != null)
            hrsAnimation.stop();
        setImageResource(R.drawable.heart_disconnected);
    }

}
