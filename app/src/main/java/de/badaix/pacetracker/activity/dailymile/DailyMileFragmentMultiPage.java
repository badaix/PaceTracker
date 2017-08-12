package de.badaix.pacetracker.activity.dailymile;

public abstract class DailyMileFragmentMultiPage extends DailyMileFragment {
    protected boolean hasMore = true;

    public DailyMileFragmentMultiPage(String title, OnStreamUpdateListener streamUpdateListener) {
        super(title, streamUpdateListener);
    }

    @Override
    public boolean hasMoreEntries() {
        return hasMore;
    }
}
