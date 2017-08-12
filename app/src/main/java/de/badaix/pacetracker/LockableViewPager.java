package de.badaix.pacetracker;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class LockableViewPager extends ViewPager {
    private boolean locked;

    public LockableViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOffscreenPageLimit(3);
        locked = false;
    }

    public LockableViewPager(Context context) {
        super(context);
        setOffscreenPageLimit(3);
        locked = false;
    }

    public void lock(boolean lock) {
        locked = lock;
        if (lock) {
            setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
        } else
            setOnTouchListener(null);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (locked)
            return false;

        return super.onInterceptTouchEvent(event);
    }
}
