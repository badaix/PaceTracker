package de.badaix.pacetracker.activity.dailymile;

import de.badaix.pacetracker.social.dailymile.PersonEntry;
import de.badaix.pacetracker.views.dailymile.DailyMileItem;

public interface OnItemTouchListener {
    public void onEntryClick(DailyMileItem item);

    public boolean onEntryLongClick(DailyMileItem item);

    public void onEntryChanged(PersonEntry entry);
}
