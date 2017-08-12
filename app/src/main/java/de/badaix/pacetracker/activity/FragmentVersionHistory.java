package de.badaix.pacetracker.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.util.Hint;
import de.badaix.pacetracker.views.VersionItem;

public class FragmentVersionHistory extends ListFragment implements OnItemClickListener {
    private String fragmentName = "Version History";
    private VersionItemAdapter arrayAdapter;
    private int clickCount;

    public void setTitle(String title) {
        fragmentName = title;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_version_history, container, false);
        arrayAdapter = new VersionItemAdapter(getActivity(), 0);
        arrayAdapter.add(new Pair<String, String>("1.0 beta 4", getString(R.string.version_1_0_beta4)));
        arrayAdapter.add(new Pair<String, String>("1.0 beta 3", getString(R.string.version_1_0_beta3)));
        arrayAdapter.add(new Pair<String, String>("1.0 beta 2", getString(R.string.version_1_0_beta2)));
        arrayAdapter.add(new Pair<String, String>("1.0 beta 1", getString(R.string.version_1_0_beta1)));
        setListAdapter(arrayAdapter);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        clickCount = 0;
        this.getListView().setOnItemClickListener(this);
    }

    @Override
    public String toString() {
        return fragmentName;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (position != 0)
            return;

        float weight = GlobalSettings.getInstance(getActivity()).getUserWeight();
        if (Math.abs(weight - 102.4) > 0.1)
            return;

        int volume = GlobalSettings.getInstance(getActivity()).getVoiceVolume();
        if (volume != 0)
            return;

        ++clickCount;
        if (clickCount == 10) {
            Hint.show(getActivity(), "Pro mode enabled");
            GlobalSettings.getInstance().setPro(true);
        }
    }

    private class VersionItemAdapter extends ArrayAdapter<Pair<String, String>> {

        public VersionItemAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
            // TODO Auto-generated constructor stub
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            VersionItem versionItem;
            Pair<String, String> current = this.getItem(position);
            if (convertView != null) {
                versionItem = (VersionItem) convertView;
                versionItem.setVersion(current.first, current.second);
            } else {
                versionItem = new VersionItem(getActivity(), current.first, current.second);
            }
            return versionItem;
        }

    }

}
