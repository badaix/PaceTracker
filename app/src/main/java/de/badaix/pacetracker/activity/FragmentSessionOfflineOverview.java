package de.badaix.pacetracker.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.List;
import java.util.Vector;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.goal.Goal;
import de.badaix.pacetracker.sensor.SensorData;
import de.badaix.pacetracker.sensor.SensorProvider;
import de.badaix.pacetracker.sensor.SensorProvider.SensorState;
import de.badaix.pacetracker.session.Felt;
import de.badaix.pacetracker.session.GpsPos;
import de.badaix.pacetracker.session.HxmData;
import de.badaix.pacetracker.session.Session;
import de.badaix.pacetracker.session.SessionSummary;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.util.DateUtils;
import de.badaix.pacetracker.util.Distance;
import de.badaix.pacetracker.views.OverviewItem;
import de.badaix.pacetracker.views.WeatherItem;
import de.badaix.pacetracker.weather.Weather;

@SuppressLint("ValidFragment")
public class FragmentSessionOfflineOverview extends ListFragment implements SessionGUI {
    java.text.DecimalFormat decFormat;
    private OverviewItemAdapter adapter;
    private Vector<OverviewItem> items;
    private OverviewItem dateItem;
    private OverviewItem textItem;
    private OverviewItem distanceItem;
    private OverviewItem timeItem;
    private OverviewItem speedItem;
    private OverviewItem hxmItem = null;
    private OverviewItem paceItem;
    private OverviewItem feltItem;
    private WeatherItem weatherItem;
    private SessionSummary sessionSummary = null;
    private String fragmentName = "Overview";

    public FragmentSessionOfflineOverview(SessionSummary sessionSummary) {
        this.sessionSummary = sessionSummary;
        items = new Vector<OverviewItem>();
    }

    public void setTitle(String title) {
        fragmentName = title;
    }

    public void addOverviewItem(OverviewItem item) {
        items.add(item);
        if (adapter != null)
            adapter.notifyDataSetChanged();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.session_overview, container, false);

        dateItem = new OverviewItem(getActivity(), getString(R.string.startItemName));
        dateItem.addItem(getString(R.string.endItemName));
        dateItem.setDrawable(R.drawable.history);

        textItem = new OverviewItem(getActivity(), getString(R.string.descriptionItemName));
        textItem.addItem(getString(R.string.commentItemName));
        textItem.setDrawable(R.drawable.speech_balloon);

        distanceItem = new OverviewItem(getActivity(), getString(R.string.distanceItemName));
        distanceItem.addItem(getString(R.string.caloriesItemName), "0");
        distanceItem.setDrawable(R.drawable.distance);

        timeItem = new OverviewItem(getActivity(), getString(R.string.timeItemName));
        timeItem.addItem(getString(R.string.totalTimeItemName));
        timeItem.setDrawable(R.drawable.stop_watch);

        speedItem = new OverviewItem(getActivity(), getString(R.string.averageSpeedItemName));
        speedItem.addItem(getString(R.string.maxSpeedItemName));
        speedItem.setDrawable(R.drawable.tacho);

        paceItem = new OverviewItem(getActivity(), getString(R.string.averagePaceItemName));
        paceItem.setDrawable(R.drawable.stop_watch);

        feltItem = new OverviewItem(getActivity(), getString(R.string.iFelt));

        weatherItem = new WeatherItem(getActivity());

        hxmItem = new OverviewItem(getActivity(), getString(R.string.meanHeartRate));
        hxmItem.addItem(getString(R.string.maxHeartRate));
        hxmItem.setDrawable(R.drawable.heart_ekg);

        int idx = 0;
        items.add(idx++, dateItem);
        items.add(idx++, textItem);
        items.add(idx++, distanceItem);
        items.add(idx++, timeItem);
        items.add(idx++, speedItem);
        items.add(idx++, paceItem);
        if (sessionSummary.hasHr())
            items.add(idx++, hxmItem);
        Weather weather = sessionSummary.getSettings().getWeather();
        weatherItem.setWeather(weather);
        if (!GlobalSettings.getInstance().isPro())
            items.add(idx++, weatherItem);
        else if (weather != null)
            items.add(idx++, weatherItem);

        Felt felt = sessionSummary.getSettings().getFelt();
        if (felt == null)
            felt = Felt.NONE;
        items.add(idx++, feltItem);
        feltItem.setValue(0, sessionSummary.getSettings().getFelt().toLocaleString(getActivity()));
        feltItem.setDrawable(felt.getDrawable(getActivity()));

        this.adapter = new OverviewItemAdapter(getActivity(), R.layout.overview_item, items);
        setListAdapter(this.adapter);

        update();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        update();
    }

    @Override
    public void onStateChanged(Session.State oldState, Session.State newState) {
    }

    @Override
    public void onSessionCommand(int command) {
    }

    @Override
    public void onFilteredLocationChanged(GpsPos location) {
    }

    @Override
    public void onLocationChanged(Location location) {
    }

    @Override
    public void onGpsStatusChanged(boolean active, boolean hasFix, int fixCount, int satCount) {
    }

    @Override
    public void onSensorData(SensorProvider provider, SensorData sensorData) {
    }

    @Override
    public void onSensorStateChanged(SensorProvider provider, boolean active, SensorState sensorState) {
    }

    @Override
    public void onSensorDataChanged(HxmData hxmData) {
    }

    @Override
    public void setSession(Session session) {
        this.sessionSummary = session;
    }

    public void setSession(SessionSummary session) {
        this.sessionSummary = session;
        Goal goal = sessionSummary.getSettings().getGoal();
        if (goal != null)
            goal.addSessionUI(this);
    }

    @Override
    public void onGuiTimer(boolean resumed) {
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        OverviewItem ovItem = (OverviewItem) v;
        ovItem.swapItems();
        ovItem.invalidate();
        Log.d("PaceTracker", "Item clicked: " + id);
    }

    @Override
    public void update() {
        if (!isResumed())
            return;

        if (!TextUtils.isEmpty(sessionSummary.getSettings().getDescription())
                || !TextUtils.isEmpty(sessionSummary.getSettings().getComment()))
            textItem.setVisibility(View.VISIBLE);
        else
            textItem.setVisibility(View.GONE);

        Felt felt = sessionSummary.getSettings().getFelt();
        if (felt != Felt.NONE) {
            feltItem.setVisibility(View.VISIBLE);
            feltItem.setDrawable(felt.getDrawable(getActivity()));
        } else
            feltItem.setVisibility(View.GONE);

        // Log.d("PaceTracker", "UpdateSession");
        String sDistance = "N/A";
        String sTotalTime = "N/A";
        String sTime = "N/A";
        String sMaxSpeed = "N/A";
        String sAvgSpeed = "N/A";
        String sCalories = "N/A";
        String sAvgPace = "N/A";
        String sStart = "N/A";
        String sEnd = "N/A";
        String sHrMean = "N/A";
        String sHrMax = "N/A";
        // sessionPref.setTitle(session.getName() + " - " +
        // session.getStateDescription());

        if (sessionSummary != null) {
            long totalDuration = 0;
            try {
                totalDuration = sessionSummary.getSessionStop().getTime() - sessionSummary.getSessionStart().getTime();
                sStart = sessionSummary.getSessionStart().toLocaleString();
                sEnd = sessionSummary.getSessionStop().toLocaleString();
            } catch (Exception e) {
            }
            try {
                sDistance = Distance.distanceToString(sessionSummary.getDistance(), 2);
                sTime = DateUtils.secondsToHHMMSSString(sessionSummary.getDuration() / 1000);
                sTotalTime = DateUtils.secondsToHHMMSSString(totalDuration / 1000);
                if (sessionSummary.getDistance() > 0)
                    sAvgPace = DateUtils.secondsToMMSSString((long) (sessionSummary.getDuration() / (1000 * Distance
                            .distanceToDouble(sessionSummary.getDistance()))));

                sCalories = Integer.toString(sessionSummary.getCalories());
                sAvgSpeed = Distance.speedToString(sessionSummary.getAvgSpeed());
                sMaxSpeed = Distance.speedToString(sessionSummary.getMaxSpeed());
                sHrMean = Distance.doubleToString(sessionSummary.getHrMean(), 1);
                sHrMax = "" + sessionSummary.getHrMax();
            } catch (Exception e) {
            }
        }
        dateItem.setValue(0, sStart);
        dateItem.setValue(1, sEnd);
        textItem.setValue(0, sessionSummary.getSettings().getDescription());
        textItem.setValue(1, sessionSummary.getSettings().getComment());
        distanceItem.setValue(0, sDistance);
        distanceItem.setValue(1, sCalories);
        timeItem.setValue(0, sTime);
        timeItem.setValue(1, sTotalTime);
        speedItem.setValue(0, sAvgSpeed);
        speedItem.setValue(1, sMaxSpeed);
        paceItem.setValue(0, sAvgPace);
        hxmItem.setValue(0, sHrMean);
        hxmItem.setValue(1, sHrMax);
        Goal goal = sessionSummary.getSettings().getGoal();
        if (goal != null) {
            goal.updateGui();
        }

        adapter.notifyDataSetChanged();
    }

    @Override
    public String toString() {
        return fragmentName;
    }

    class OverviewItemAdapter extends ArrayAdapter<OverviewItem> {
        private List<OverviewItem> list;

        public OverviewItemAdapter(Context context, int textViewResourceId, Vector<OverviewItem> items) {
            super(context, textViewResourceId, items);
            list = items;
        }

        @Override
        public int getCount() {
            int count = 0;
            for (int i = 0; i < list.size(); ++i)
                if (list.get(i).getVisibility() == View.VISIBLE)
                    ++count;
            return count;
        }

        @Override
        public long getItemId(int position) {
            int count = 0;

            for (int i = 0; i < list.size(); ++i) {
                if (list.get(i).getVisibility() == View.VISIBLE) {
                    if (count == position)
                        return i;
                    ++count;
                }
            }
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return list.get((int) getItemId(position));
        }
    }

}
