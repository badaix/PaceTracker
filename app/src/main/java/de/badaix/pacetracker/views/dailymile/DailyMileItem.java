package de.badaix.pacetracker.views.dailymile;

import android.content.Context;
import android.widget.LinearLayout;

import de.badaix.pacetracker.activity.dailymile.OnItemTouchListener;

public class DailyMileItem extends LinearLayout {
    protected OnItemTouchListener onItemTouchListener = null;
    protected Context context;

    public DailyMileItem(Context context) {
        super(context);
        this.context = context;
    }

    public void update() {
    }

    public void setOnItemTouchListener(OnItemTouchListener listener) {
        onItemTouchListener = listener;
    }

}
