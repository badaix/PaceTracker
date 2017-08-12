package de.badaix.pacetracker.widgets;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;

import de.badaix.pacetracker.util.Hint;

public class ScaleButton extends Button implements OnTouchListener {
    private Rect rect;

    public ScaleButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        Animation animation;
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            rect = new Rect(0, 0, v.getWidth(), v.getHeight());
            Hint.log(this, "new Rect: " + rect.toShortString());
            animation = new ScaleAnimation(1.0f, 0.95f, 1.0f, 0.95f, v.getWidth() / 2.0f, v.getHeight() / 2.0f);
            animation.setDuration(30);
            this.setPressed(true);
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            animation = new ScaleAnimation(0.95f, 1.0f, 0.95f, 1.0f, v.getWidth() / 2.0f, v.getHeight() / 2.0f);
            animation.setDuration(10);
            this.setPressed(false);
            Hint.log(this, "Rect: " + rect.toShortString() + " " + event.getX() + " " + event.getY());
            if ((rect != null) && rect.contains((int) event.getX(), (int) event.getY()))
                this.performClick();
        } else {
            // Hint.log(this, "Action: " + event.getAction());
            return true;
        }

        animation.setFillEnabled(true);
        animation.setFillAfter(true);
        v.startAnimation(animation);
        return true;
    }

}
