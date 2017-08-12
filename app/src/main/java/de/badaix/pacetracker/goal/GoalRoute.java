package de.badaix.pacetracker.goal;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.text.TextUtils;
import android.util.Pair;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Vector;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.SessionPersistance;
import de.badaix.pacetracker.activity.ActivityRoutes;
import de.badaix.pacetracker.activity.FragmentSessionGmsMap;
import de.badaix.pacetracker.activity.FragmentSessionOfflineOverview;
import de.badaix.pacetracker.activity.FragmentSessionOverview;
import de.badaix.pacetracker.activity.SessionUI;
import de.badaix.pacetracker.session.GeoPos;
import de.badaix.pacetracker.session.GpsPos;
import de.badaix.pacetracker.session.Route;
import de.badaix.pacetracker.session.RouteInfo;
import de.badaix.pacetracker.session.Segment;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.util.BoundingBox;
import de.badaix.pacetracker.util.CheapDistance;
import de.badaix.pacetracker.util.Hint;
import de.badaix.pacetracker.util.RouteLoader;
import de.badaix.pacetracker.util.RouteLoader.OnFinishedListener;
import de.badaix.pacetracker.util.TTS;
import de.badaix.pacetracker.views.OverviewItem;

public class GoalRoute extends Goal implements OnFinishedListener {
    private final int BOUNDING_BOX_RADIUS = 500;
    private int lastIdx;
    private CheapDistance cheapDistance;
    private Vector<Pair<BoundingBox, Segment>> segments;
    private Route route = null;
    private OverviewItem overviewItem;
    private int maxDistance = 500;
    // private long routeId = -1;
    private String routeName = "";
    private String routeFilename = "";
    private boolean speechNavigation;
    private Preference preference;
    private CheckBoxPreference preference2;
    private FragmentSessionGmsMap mapFragment = null;
    private Vector<Pair<String, String>> items;

    // private SessionOverviewFragment sessionOverviewFragment;

    public GoalRoute(Activity activity) {
        super(activity);
    }

    @Override
    protected void init() {
        items = new Vector<Pair<String, String>>();
        items.add(Pair.create(context.getString(R.string.goal), context.getString(R.string.goalRoute)));
        if (!TextUtils.isEmpty(routeName))
            items.set(0, Pair.create(context.getString(R.string.route), routeName));
        overviewItem = new OverviewItem(context, items);
        if (!offline) {
            preference = new Preference(context);
            preference.setTitle(activity.getString(R.string.configureRoute));
            preferences.add(preference);
            preference2 = new CheckBoxPreference(context);
            preference2.setTitle(this.activity.getString(R.string.speechNavigation));
            preference2.setOnPreferenceChangeListener(this);
            preference2.setChecked(GlobalSettings.getInstance().getBoolean("speechNavigation", false));
            preferences.add(preference2);
            speechNavigation = preference2.isChecked();
            // items.add(Pair.create("Route", "1"));
            // overviewItem = new OverviewItem(context, items);
        }
    }

    @Override
    public String getName() {
        return context.getString(R.string.goalRoute);
    }

    @Override
    public void addSessionUI(SessionUI sessionUI) {
        if (sessionUI instanceof FragmentSessionGmsMap) {
            if ((route == null) && !TextUtils.isEmpty(routeFilename)) {
                mapFragment = (FragmentSessionGmsMap) sessionUI;
                loadRoute(routeFilename, false);
            } else {
                ((FragmentSessionGmsMap) sessionUI).setRoute(route);
            }
        } else if (sessionUI instanceof FragmentSessionOverview) {
            // if (GlobalSettings.getInstance(context).isDebug())
            ((FragmentSessionOverview) sessionUI).addOverviewItem(overviewItem);
        } else if (sessionUI instanceof FragmentSessionOfflineOverview)
            ((FragmentSessionOfflineOverview) sessionUI).addOverviewItem(overviewItem);
    }

    @Override
    public void updateGui() {
        overviewItem.update(items);
    }

    @Override
    protected void putSettings(JSONObject json) throws JSONException {
        // routeId = -1;
        routeName = "";
        routeFilename = "";
        // if (json.has("routeid"))
        // routeId = json.getLong("routeid");
        if (json.has("filename"))
            routeFilename = json.getString("filename");
        if (json.has("name")) {
            routeName = json.getString("name");
            items.set(0, Pair.create(context.getString(R.string.route), routeName));
        }
        if (json.has("speechNavigation")) {
            speechNavigation = json.getBoolean("speechNavigation");
        }
        updateGui();
    }

    public boolean isConfigured() {
        return (route != null);
    }

    @Override
    protected JSONObject getSettings() {
        JSONObject json = new JSONObject();
        try {
            if (route != null) {
                json.put("name", route.getName());
                // json.put("routeid", route.getId());
                json.put("filename", route.getFilename());
            } else {
                json.put("name", routeName);
                // json.put("routeid", routeId);
                json.put("filename", routeFilename);
            }
            json.put("speechNavigation", speechNavigation);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    private void initRoute() {
        lastIdx = -1;
        cheapDistance = null;

        if (route == null)
            return;

        // routeId = route.getId();
        routeName = route.getName();
        routeFilename = route.getFilename();
        items.set(0, Pair.create(context.getString(R.string.route), routeName));

        preference.setSummary(route.getName());

        if (!route.getPositions().isEmpty()) {
            cheapDistance = new CheapDistance(route.getPositions().firstElement());
            segments = new Vector<Pair<BoundingBox, Segment>>();
            for (Segment segment : route.getSegments()) {
                if (segment.getPositions().isEmpty())
                    continue;
                GeoPos pos = segment.getPositions().firstElement();
                double latOffset = cheapDistance.latitudeOffset(BOUNDING_BOX_RADIUS);
                double lonOffset = cheapDistance.longitudeOffset(BOUNDING_BOX_RADIUS);
                BoundingBox bb = new BoundingBox(pos.latitude - latOffset, pos.longitude - lonOffset, pos.latitude
                        + latOffset, pos.longitude + lonOffset);
                segments.add(new Pair<BoundingBox, Segment>(bb, segment));
            }
        }
        updateGui();
    }

    private void loadRoute(String routeFilename, boolean showProgress) {
        if ((routeFilename == null) || TextUtils.isEmpty(routeFilename))
            return;

        Cursor cursor = null;
        SessionPersistance sessionPersistance = null;
        try {
            sessionPersistance = SessionPersistance.getInstance(context);
            String fields[] = {"*"};
            cursor = null;
            try {
                cursor = sessionPersistance.queryRoutes(fields, "filename like '%" + new File(routeFilename).getName()
                        + "%'", null, "", null);
                if ((cursor != null) && (cursor.moveToFirst())) {
                    RouteInfo routeInfo = sessionPersistance.getRoute(cursor);
                    if (routeInfo != null) {
                        // this.routeId = routeInfo.getId();
                        this.routeFilename = routeInfo.getFilename();
                        this.routeName = routeInfo.getName();
                        RouteLoader loader = new RouteLoader(context, showProgress, this);
                        loader.execute(new File(routeInfo.getFilename()));
                    }
                }
            } finally {
                if ((cursor != null) && !cursor.isClosed())
                    cursor.close();
            }

        } catch (Exception e) {
            Hint.show(context, e);
            this.routeFilename = "";
        } finally {
            if (sessionPersistance != null)
                sessionPersistance.closeDB();
        }
        return;
    }

    @Override
    public void setData(int resultCode, Intent data) {
        if (data != null) {
            loadRoute(data.getStringExtra("RouteFilename"), false);
        }
        if ((resultCode == Activity.RESULT_CANCELED) || (data == null)) {
            route = null;
            return;
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == preference2) {
            speechNavigation = (Boolean) newValue;
            GlobalSettings.getInstance().put("speechNavigation", speechNavigation);
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceClick(Preference pref) {
        if (pref == preference) {
            Intent intent = new Intent(activity, ActivityRoutes.class);
            intent.putExtra("RequestCode", GOAL_REQUEST_CODE);
            activity.startActivityForResult(intent, GOAL_REQUEST_CODE);
        }
        return true;
    }

    @Override
    public void onFilteredLocationChanged(GpsPos location) {
        if ((cheapDistance == null) || !speechNavigation)
            return;

        maxDistance = 50;
        if (session.getCurrentSpeed() > 100)
            maxDistance = 1000;
        else if (session.getCurrentSpeed() > 50)
            maxDistance = 500;
        else if (session.getCurrentSpeed() < 20)
            maxDistance = 20;

        // String overviewText = "";//location.getLongitude() + " " +
        // location.getLatitude();

        Segment closestSegment = null;
        int startIdx = lastIdx + 1;
        int newIdx = -1;
        GeoPos geoPos = new GeoPos(location.latitude, location.longitude);

        for (int i = startIdx; i < segments.size(); ++i) {
            try {
                Pair<BoundingBox, Segment> segment = segments.get(i);
                if (!segment.first.isInside(geoPos))
                    continue;

                GeoPos pos = segment.second.getPositions().firstElement();
                double distance = cheapDistance.distance(geoPos, pos);

                if ((distance < maxDistance) && (i > lastIdx)) {
                    closestSegment = segment.second;
                    newIdx = i;
                    Hint.log(this, "closestSegment (" + distance + "m): " + closestSegment.getName());
                    // overviewText = "next segment: " + distance;
                    break;
                }
            } catch (Exception e) {
            }
        }

        if (closestSegment != null) {
            if (newIdx != lastIdx) {
                TTS.getInstance().speak("in " + maxDistance + " Metern " + closestSegment.getInstruction(), false);
                lastIdx = newIdx;
            }
        }

        // items.set(0, Pair.create(items.firstElement().first, overviewText));
    }

    @Override
    public void onFinished(Route route, Exception e) {
        this.route = route;
        // this.route.setId(routeId);
        if (!offline)
            initRoute();
        if (mapFragment != null) {
            mapFragment.setRoute(this.route);
            mapFragment = null;
        }
    }

}
