package de.badaix.pacetracker.activity.dailymile;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONException;

import java.io.IOException;
import java.util.Date;
import java.util.Vector;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.social.dailymile.DMItem;
import de.badaix.pacetracker.social.dailymile.DailyMile;
import de.badaix.pacetracker.social.dailymile.PersonEntry;
import de.badaix.pacetracker.views.dailymile.DailyMileEntry;

class DailyMileEntryAdapter extends DailyMileItemAdapter {

    public DailyMileEntryAdapter(Context context, int textViewResourceId, OnItemTouchListener touchListener,
                                 Vector<DMItem> list) {
        super(context, textViewResourceId, touchListener, list);
    }

    @Override
    protected View getItemView(int position, View convertView, ViewGroup parent) {
        synchronized (entries) {
            DMItem entry = getItem(position);
            if (!(entry instanceof PersonEntry))
                return null;

            PersonEntry personEntry = (PersonEntry) entry;
            convertView = new DailyMileEntry(context, personEntry, false);

            // ((DailyMileEntry)
            // convertView).setOnItemTouchListener(touchListener);
            return convertView;
        }
    }
}

public class DailyMileFragmentEntry extends DailyMileFragment {
    private PersonEntry personEntry;

    public DailyMileFragmentEntry(PersonEntry personEntry, OnStreamUpdateListener streamUpdateListener) {
        super("", streamUpdateListener);
        this.personEntry = personEntry;
    }

    @Override
    public void onStart() {
        Vector<PersonEntry> entries = new Vector<PersonEntry>();
        entries.add(personEntry);
        addEntries(entries);
        super.onStart();
    }

    @Override
    public Vector<? extends DMItem> getEntries(int page) throws JSONException, IOException {
        DailyMile dm = new DailyMile(this.getActivity());
        Vector<PersonEntry> result = new Vector<PersonEntry>();
        result.add(dm.getEntry(personEntry.getId()));
        return result;
    }

    @Override
    public Vector<? extends DMItem> getEntries(Date since) throws JSONException, IOException {
        return getEntries(0);
    }

    @Override
    protected void createAdapter() {
        if (this.adapter == null) {
            this.adapter = new DailyMileEntryAdapter(this.getActivity(), R.layout.imageitem, onItemTouchListener,
                    this.entries);
            setListAdapter(adapter);
        }
    }

}
