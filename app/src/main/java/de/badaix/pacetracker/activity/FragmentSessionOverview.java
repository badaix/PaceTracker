package de.badaix.pacetracker.activity;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.Vector;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.sensor.SensorData;
import de.badaix.pacetracker.sensor.SensorProvider;
import de.badaix.pacetracker.sensor.SensorProvider.SensorState;
import de.badaix.pacetracker.session.GpsPos;
import de.badaix.pacetracker.session.HxmData;
import de.badaix.pacetracker.session.Session;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.util.DateUtils;
import de.badaix.pacetracker.util.Distance;
import de.badaix.pacetracker.util.Hint;
import de.badaix.pacetracker.views.OverviewItem;
import de.badaix.pacetracker.views.WeatherItem;
import de.badaix.pacetracker.weather.Weather;
import de.badaix.pacetracker.widgets.GpsIndicator;

class OverviewItemAdapter extends ArrayAdapter<OverviewItem> {
    public OverviewItemAdapter(Context context, int textViewResourceId, Vector<OverviewItem> items) {
        super(context, textViewResourceId, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getItem(position);
    }
}

class GpsOverviewItem extends OverviewItem {
    private GpsIndicator gpsIndicator;

    public GpsOverviewItem(Context context) {
        super(context);
        title = this.getClass().getSimpleName();
    }

    public GpsIndicator getGpsIndicator() {
        return gpsIndicator;
    }

    @Override
    protected void update() {
    }

    @Override
    protected void Init(Context context) {
        ctx = context;
        LayoutInflater vi = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        vi.inflate(R.layout.overview_item_gps, this);
        gpsIndicator = (GpsIndicator) findViewById(R.id.linearLayoutGpsSignal);
    }

    @Override
    public void swapItems() {
    }

}

public class FragmentSessionOverview extends ListFragment implements SessionGUI {
    private GpsOverviewItem gpsIndicator;
    private OverviewItem hxmItem = null;
    private OverviewItem distanceItem;
    private OverviewItem timeItem;
    private OverviewItem speedItem;
    private OverviewItem paceItem;
    private WeatherItem weatherItem;

    // private Preference sessionPref;
    private int iSatCount = 0;
    private int iFixCount = 0;
    private boolean bFix = false;

    private Session session = null;

    private OverviewItemAdapter adapter = null;
    private Vector<OverviewItem> items = null;
    private String fragmentName = "Overview";

    public FragmentSessionOverview() {
        super();
        items = new Vector<OverviewItem>();

        if (GlobalSettings.getInstance(getActivity()).service != null)
            setSession(GlobalSettings.getInstance().service.getSession());
    }

    public void setTitle(String title) {
        fragmentName = title;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Hint.log(this, "onCreate");

        this.adapter = new OverviewItemAdapter(getActivity(), R.layout.overview_item, items);
        setListAdapter(this.adapter);
        initGui();
    }

    protected void initGui() {
        if ((items == null) || (session == null))
            return;

        items.clear();

        gpsIndicator = new GpsOverviewItem(getActivity());
        if (session != null) {
            if (session.hasHr() && (session.getSensorProvider() != null)) {
                hxmItem = new OverviewItem(getActivity(), getString(R.string.heartRate), "--");
                hxmItem.addItem(getString(R.string.mean_max), "--");
                if (session.getSensorProvider().supportsBattery())
                    hxmItem.addItem(getString(R.string.battery), "--%");
                hxmItem.setDrawable(R.drawable.heart_ekg);
            }
        }

        distanceItem = new OverviewItem(getActivity(), getString(R.string.distanceItemName));
        distanceItem.addItem(getString(R.string.caloriesItemName), "0");
        distanceItem.setDrawable(R.drawable.distance);

        timeItem = new OverviewItem(getActivity(), getString(R.string.timeItemName));
        timeItem.addItem(getString(R.string.totalTimeItemName));
        timeItem.setDrawable(R.drawable.stop_watch);

        speedItem = new OverviewItem(getActivity(), getString(R.string.speedItemName));
        speedItem.addItem(getString(R.string.averageSpeedItemName));
        // speedItem.addItem("Last km speed");
        speedItem.addItem(getString(R.string.maxSpeedItemName));
        speedItem.setDrawable(R.drawable.tacho);

        paceItem = new OverviewItem(getActivity(), getString(R.string.paceItemName));
        paceItem.addItem(getString(R.string.averagePaceItemName));
        paceItem.setDrawable(R.drawable.stop_watch);
        // paceItem.addItem("Last km pace");

        weatherItem = new WeatherItem(getActivity());

        int idx = 0;
        items.add(idx++, gpsIndicator);
        if (hxmItem != null)
            items.add(idx++, hxmItem);
        items.add(idx++, distanceItem);
        items.add(idx++, timeItem);
        items.add(idx++, speedItem);
        items.add(idx++, paceItem);

        if (session != null) {
            Weather weather = session.getSettings().getWeather();
            weatherItem.setWeather(weather);
            if (!GlobalSettings.getInstance().isPro())
                items.add(idx++, weatherItem);
            else if (weather != null)
                items.add(idx++, weatherItem);
        }

        if (session.getSettings().getGoal() != null) {
            session.getSettings().getGoal().addSessionUI(this);
        }

        UpdateSession();
        adapter.notifyDataSetChanged();
    }

    public void addOverviewItem(OverviewItem item) {
        items.add(item);
        if (adapter != null)
            adapter.notifyDataSetChanged();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Hint.log(this, "onCreateView");
        View v = inflater.inflate(R.layout.session_overview, container, false);

        return v;
    }

    @Override
    public void setSession(Session session) {
        this.session = session;
        if (this.isResumed())
            initGui();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Hint.log(this, "onActivityCreated");
    }

    private void UpdateSession() {
        // Hint.log(this, "isResumed: " + isResumed());
        if (!isResumed())
            return;

        String sDistance = "N/A";
        String sTime = "N/A";
        String sTotalTime = "N/A";
        String sSpeed = "N/A";
        // String sLastKilometerSpeed = "N/A";
        String sMaxSpeed = "N/A";
        String sAvgSpeed = "N/A";
        String sCalories = "N/A";
        String sPace = "N/A";
        // String sLastKilometerPace = "N/A";
        String sAvgPace = "N/A";

        bFix = session.getSettings().getPositionProvider().isFix();
        iFixCount = session.getSettings().getPositionProvider().getFixCount();
        iSatCount = session.getSettings().getPositionProvider().getSatCount();
        gpsIndicator.getGpsIndicator().onGpsStatusChanged(true, bFix, iFixCount, iSatCount);

        if ((session != null) && session.hasGpsInfo()) {
            sDistance = Distance.distanceToString(session.getDistance(), 2);
            sTime = DateUtils.secondsToHHMMSSString(session.getDuration() / 1000);
            sTotalTime = DateUtils.secondsToHHMMSSString(session.getTotalDuration() / 1000);
            sSpeed = Distance.speedToString(session.getCurrentSpeed());
            if (session.getCurrentSpeed() > 0.)
                sPace = DateUtils.secondsToMMSSString((long) ((1. / (session.getCurrentSpeed())) * 3.6 * GlobalSettings
                        .getInstance().getDistUnit().getFactor()));
            if (session.getDistance() > 0)
                sAvgPace = DateUtils.secondsToMMSSString((long) (session.getDuration() / (1000 * Distance
                        .distanceToDouble(session.getDistance()))));
            // decFormat.format(60. / session.getAverageSpeed());
            sCalories = Integer.toString(session.getCalories());
            float fAvgSpeed = session.getAvgSpeed();
            sAvgSpeed = Distance.speedToString(fAvgSpeed);
            sMaxSpeed = Distance.speedToString(session.getMaxSpeed());
        }
        distanceItem.setValue(0, sDistance);
        distanceItem.setValue(1, sCalories);
        timeItem.setValue(0, sTime);
        timeItem.setValue(1, sTotalTime);
        speedItem.setValue(0, sSpeed);
        speedItem.setValue(1, sAvgSpeed);
        // speedItem.setValue(2, sLastKilometerSpeed);
        speedItem.setValue(2, sMaxSpeed);
        paceItem.setValue(0, sPace);
        paceItem.setValue(1, sAvgPace);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        OverviewItem ovItem = (OverviewItem) v;
        ovItem.swapItems();
        Log.d("PaceTracker", "Item clicked: " + id);
    }

    @Override
    public void onLocationChanged(Location location) {
        // Log.d("PaceTracker", "onLocationChanged");
        // this.location = location;
    }

    @Override
    public void onStateChanged(Session.State oldState, Session.State newState) {
        Log.d("PaceTracker", "SessionOverviewFragment.onStateChanged: " + newState + " <= " + oldState);
    }

    @Override
    public void onGuiTimer(boolean resumed) {
        if (resumed)
            UpdateSession();
    }

    @Override
    public void onGpsStatusChanged(boolean active, boolean hasFix, int fixCount, int satCount) {
        Hint.log(this, "onGpsStatusChanged: " + active + " " + hasFix + " " + fixCount + " " + satCount);
        bFix = hasFix;
        iSatCount = satCount;
        iFixCount = fixCount;
    }

    @Override
    public void onSessionCommand(int command) {
        // TODO Auto-generated method stub

    }

    @Override
    public void update() {
    }

    @Override
    public void onFilteredLocationChanged(GpsPos location) {
    }

    @Override
    public String toString() {
        return fragmentName;
    }

    @Override
    public void onSensorData(SensorProvider provider, SensorData sensorData) {
        hxmItem.setValue(0, Integer.toString(sensorData.getHeartRate()));
        hxmItem.setValue(1, Distance.doubleToString(session.getHrMean(), 1) + "/" + session.getHrMax());
        if (provider.supportsBattery())
            hxmItem.setValue(2, Integer.toString(Math.round(100 * sensorData.getBatteryLevel())) + "%");
    }

    @Override
    public void onSensorStateChanged(SensorProvider provider, boolean active, SensorState sensorState) {
    }

    @Override
    public void onSensorDataChanged(HxmData hxmData) {
    }

    @Override
    public void onAttach(Activity activity) {
        Hint.log(this, "onAttach");
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        Hint.log(this, "onDetach");
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        Hint.log(this, "onDestroy");
        super.onDestroy();
    }

    @Override
    public void onPause() {
        Hint.log(this, "onPause");
        try {
            for (OverviewItem item : items)
                session.getSettings().getBundle().putInt(item.getTitle(), item.getFirstIndex());
        } catch (Exception e) {
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        Hint.log(this, "onResume");
        try {
            Bundle bundle = session.getSettings().getBundle();
            if (bundle != null) {
                for (OverviewItem item : items)
                    item.setFirstIndex(bundle.getInt(item.getTitle()));
            }
        } catch (Exception e) {
        }
        super.onResume();
    }

    @Override
    public void onStart() {
        Hint.log(this, "onStart");
        super.onStart();
    }

    @Override
    public void onStop() {
        Hint.log(this, "onStop");
        super.onStop();
    }

}
