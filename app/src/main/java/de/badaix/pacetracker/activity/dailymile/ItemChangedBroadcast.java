package de.badaix.pacetracker.activity.dailymile;

import java.util.HashSet;

import de.badaix.pacetracker.social.dailymile.PersonEntry;
import de.badaix.pacetracker.util.Hint;

public class ItemChangedBroadcast {
    private static ItemChangedBroadcast instance = null;
    private HashSet<OnItemTouchListener> listener = new HashSet<OnItemTouchListener>();

    public static ItemChangedBroadcast getInstance() {
        if (instance == null) {
            instance = new ItemChangedBroadcast();
        }
        return instance;
    }

    public void addListener(OnItemTouchListener onItemTouchListener) {
        synchronized (listener) {
            listener.add(onItemTouchListener);
        }
    }

    public void removeListener(OnItemTouchListener onItemTouchListener) {
        synchronized (listener) {
            if (listener.contains(onItemTouchListener))
                listener.remove(onItemTouchListener);
        }
    }

    public void notifyChanged(PersonEntry entry) {
        synchronized (listener) {
            for (OnItemTouchListener l : listener) {
                try {
                    if (l != null)
                        l.onEntryChanged(entry);
                } catch (Exception e) {
                    Hint.log(this, e);
                }
            }
        }
    }
}
