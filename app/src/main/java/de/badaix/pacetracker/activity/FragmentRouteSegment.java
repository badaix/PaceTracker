package de.badaix.pacetracker.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.Vector;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.session.Route;
import de.badaix.pacetracker.session.Segment;
import de.badaix.pacetracker.views.SegmentItem;

public class FragmentRouteSegment extends ListFragment {
    private SegmentItemAdapter adapter = null;
    private Route route;
    private String fragmentName = "Instructions";

    public FragmentRouteSegment() {
        super();
    }

    public void setTitle(String title) {
        fragmentName = title;
    }

    public void setRoute(Route route) {
        synchronized (this) {
            this.route = route;
            if (isResumed() && (route != null)) {
                this.adapter = new SegmentItemAdapter(this.getActivity(), R.layout.imageitem, route.getSegments());
                setListAdapter(adapter);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        synchronized (this) {
            setRoute(route);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.segment_list, container, false);
        return v;
    }

    @Override
    public String toString() {
        return fragmentName;
    }

    private class SegmentItemAdapter extends ArrayAdapter<Segment> {
        private Vector<Segment> items;
        private Context context;

        public SegmentItemAdapter(Context context, int textViewResourceId, Vector<Segment> items) {
            super(context, textViewResourceId, items);
            this.items = items;
            this.context = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = new SegmentItem(context, items.get(position));
            else
                ((SegmentItem) convertView).setSegment(items.get(position));
            return convertView;
        }
    }
}
