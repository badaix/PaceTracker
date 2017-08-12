package de.badaix.pacetracker.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;

import java.io.File;
import java.util.Date;
import java.util.Vector;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.SessionPersistance;
import de.badaix.pacetracker.goal.Goal;
import de.badaix.pacetracker.session.GeoPos;
import de.badaix.pacetracker.session.Segment;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.social.dailymile.DailyMile;
import de.badaix.pacetracker.social.dailymile.Route;
import de.badaix.pacetracker.util.Hint;
import de.badaix.pacetracker.util.LocationUtils;
import de.badaix.pacetracker.util.TextItemPair;
import de.badaix.pacetracker.views.RouteItem;

public class ActivityRoutes extends AppCompatActivity implements OnItemClickListener, OnClickListener,
        OnItemSelectedListener, MenuItem.OnMenuItemClickListener {

    private RouteItemAdapter adapter = null;
    private ListView routesList = null;
    private MenuItem dmItem;
    // private MenuItem mapItem;
    private android.view.MenuItem menuOpen;
    private android.view.MenuItem menuDelete;
    private AlertDialog deleteDialog;
    private RouteItem selectedItem = null;
    private SessionPersistance sessionPersistance;
    private Spinner spinnerSort = null;
    private String sortOrder = "name COLLATE NOCASE";
    private Button buttonPlanNewRoute;
    private LinearLayout linearLayoutNoRoutes;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (GlobalSettings.getInstance().getContext() == null)
            this.finish();
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_routes);

//            getSupportActionBar().setLogo(R.drawable.dashboard_button_routes);
//            getSupportActionBar().setDisplayUseLogoEnabled(true);
//            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.mainMenuRoutes);

            buttonPlanNewRoute = (Button) findViewById(R.id.buttonPlanNewRoute);
            buttonPlanNewRoute.setOnClickListener(new android.view.View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(getApplicationContext(), ActivityPlanRoute.class));
                }
            });

            linearLayoutNoRoutes = (LinearLayout) findViewById(R.id.linearLayoutNoRoutes);
            showNoRoutesInfo(false);

            routesList = (ListView) findViewById(R.id.listViewRoutes);
            routesList.setOnItemClickListener(this);

            spinnerSort = (Spinner) findViewById(R.id.spinnerSort);
            ArrayAdapter<TextItemPair<String>> sortAdapter = new ArrayAdapter<TextItemPair<String>>(this,
                    android.R.layout.simple_spinner_item);
            sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            sortAdapter.add(new TextItemPair<String>(getString(R.string.name_asc), "name COLLATE NOCASE ASC"));
            sortAdapter.add(new TextItemPair<String>(getString(R.string.name_desc), "name COLLATE NOCASE DESC"));
            sortAdapter.add(new TextItemPair<String>(getString(R.string.distance_asc), "distance ASC"));
            sortAdapter.add(new TextItemPair<String>(getString(R.string.distance_desc), "distance DESC"));
            sortAdapter.add(new TextItemPair<String>(getString(R.string.date_asc), "created DESC"));
            sortAdapter.add(new TextItemPair<String>(getString(R.string.date_desc), "created ASC"));
            spinnerSort.setAdapter(sortAdapter);
            spinnerSort.setOnItemSelectedListener(this);
            int lastSort = GlobalSettings.getInstance(this).getInt("routeSort", 0);
            spinnerSort.setSelection(lastSort);

            sessionPersistance = SessionPersistance.getInstance(this);

            this.adapter = new RouteItemAdapter(this, getRoutes(sortOrder), 0);
            View v = new View(this);
            v.setMinimumHeight(((LinearLayout) findViewById(R.id.linearLayoutSort)).getLayoutParams().height + 2);
            v.setBackgroundColor(Color.TRANSPARENT);
            routesList.addHeaderView(v, null, false);
            routesList.setAdapter(adapter);
            onItemSelected(spinnerSort, null, lastSort, 0);

            registerForContextMenu(routesList);
        } catch (Exception e) {
            Hint.log(this, e);
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNoRoutesInfo();
    }

    private void showNoRoutesInfo(boolean show) {
        if (show)
            linearLayoutNoRoutes.setVisibility(View.VISIBLE);
        else
            linearLayoutNoRoutes.setVisibility(View.GONE);
    }

    private void updateNoRoutesInfo() {
        showNoRoutesInfo(adapter.getCount() == 0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem mapItem = menu.add(R.string.map);
        mapItem.setIcon(R.drawable.location_map).setIntent(new Intent(this, ActivityMap.class))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        SubMenu subMenuAdd = menu.addSubMenu(R.string.share);
        MenuItem menuAdd = subMenuAdd.getItem();
        menuAdd.setIcon(R.drawable.ic_action_plus);
        menuAdd.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        subMenuAdd.add(R.string.planMapQuest).setIntent(new Intent(this, ActivityPlanRoute.class))
                .setIcon(R.drawable.mapquest32);

        dmItem = subMenuAdd.add(R.string.dmRouteImport).setIcon(R.drawable.dailymile32);
        dmItem.setOnMenuItemClickListener(this);
        dmItem.setEnabled(DailyMile.hasAccount());

        return true;
    }

    private Cursor getRoutes(String sortBy) {
        try {
            String fields[] = {"*"};
            Cursor cursor = sessionPersistance.queryRoutes(fields, null, null, sortBy, null);
            return cursor;
        } finally {
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menuOpen = menu.add(getResources().getString(R.string.open));
        menuDelete = menu.add(getResources().getString(R.string.deleteRoute));
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        selectedItem = (RouteItem) info.targetView;

        if (item == menuOpen) {
            viewRoute(selectedItem);
        } else if (item == menuDelete) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getResources().getString(R.string.confirmDeleteRoute)).setCancelable(false)
                    .setPositiveButton(getResources().getString(android.R.string.yes), this)
                    .setNegativeButton(getResources().getString(android.R.string.no), this);
            deleteDialog = builder.create();
            deleteDialog.show();
        }

        return true;
    }

    private void viewRoute(RouteItem route) {
        if ((getIntent().getExtras() != null)
                && (getIntent().getExtras().getInt("RequestCode", -1) == Goal.GOAL_REQUEST_CODE)) {
            Intent intent = new Intent();
            intent.putExtra("RouteId", route.getRouteInfo().getId());
            intent.putExtra("RouteFilename", route.getRouteInfo().getFilename());
            setResult(RESULT_OK, intent);
            finish();
        } else {
            String filename = route.getRouteInfo().getFilename();
            Intent intent = new Intent(getApplicationContext(), ActivityViewRoute.class);
            intent.putExtra("view", true);
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(new File(filename)), "application/json");
            startActivity(intent);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        viewRoute((RouteItem) view);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (dialog == deleteDialog) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                if (selectedItem != null) {
                    sessionPersistance.deleteRoute(selectedItem.getRouteInfo().getId());
                    adapter.swapCursor(getRoutes(sortOrder)).close();
                    adapter.notifyDataSetChanged();
                    showNoRoutesInfo(adapter.getCount() == 0);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == spinnerSort) {
            sortOrder = (String) ((TextItemPair<String>) spinnerSort.getItemAtPosition(position)).getItem();

            adapter.swapCursor(getRoutes(sortOrder)).close();
            adapter.notifyDataSetChanged();
            GlobalSettings.getInstance().put("routeSort", position);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item == dmItem) {
            class RouteTask extends AsyncTask<Void, String, Void> {
                private ProgressDialog progressDialog;
                private Exception exception = null;
                private Context context;
                private int totalCount = 0;
                private int newCount = 0;
                private Vector<String> newRoutes = new Vector<String>();

                public RouteTask(Context context) {
                    this.context = context;
                }

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    progressDialog = ProgressDialog.show(context, "",
                            context.getResources().getString(R.string.dmFetchingRoutes), true);
                    progressDialog.show();
                }

                @Override
                protected Void doInBackground(Void... params) {
                    DailyMile dm = new DailyMile(context);
                    Vector<Route> routes;
                    try {
                        routes = dm.getRoutes();
                        totalCount = routes.size();
                        if (totalCount == 0)
                            return null;

                        Cursor cursor = null;
                        SessionPersistance persistance = SessionPersistance.getInstance(context);
                        for (Route route : routes) {
                            String fields[] = {"count(*) as count"};
                            try {
                                cursor = sessionPersistance.queryRoutes(fields, "source = 'dm_" + route.getId() + "'",
                                        null, "", null);
                                if (!cursor.moveToFirst() || (cursor.getInt(cursor.getColumnIndex("count")) > 0))
                                    continue;

                                newCount++;
                                newRoutes.add(route.getName());

                                Vector<GeoPos> geoRoute = LocationUtils.decodePolyLine(route.getEncodedSamples(), 5);

                                for (int i = 1; i < geoRoute.size(); ++i)
                                    geoRoute.get(i).distance = geoRoute.get(i - 1).distance
                                            + de.badaix.pacetracker.util.Distance.calculateDistance(geoRoute.get(i - 1),
                                            geoRoute.get(i));

                                GeoPos lastGeoPos = geoRoute.lastElement();
                                geoRoute.removeElement(lastGeoPos);

                                de.badaix.pacetracker.session.Route ptRoute = new de.badaix.pacetracker.session.Route();

                                Segment segment = new Segment(route.getName());
                                segment.setImageUrl("http://content.mapquest.com/mqsite/turnsigns/icon-dirs-start_sm.gif");
                                segment.setPositions(geoRoute);
                                ptRoute.addSegment(segment);

                                segment = new Segment(context.getString(R.string.end));
                                segment.setImageUrl("http://content.mapquest.com/mqsite/turnsigns/icon-dirs-end_sm.gif");
                                segment.addPosition(lastGeoPos);
                                ptRoute.addSegment(segment);

                                ptRoute.setCreated(new Date());
                                ptRoute.setDistance(geoRoute.lastElement().distance);
                                ptRoute.setEndPos(geoRoute.lastElement());
                                ptRoute.setStartPos(geoRoute.firstElement());
                                ptRoute.setName(route.getName());
                                String description = "";
                                if (!TextUtils.isEmpty(route.getLocation()))
                                    description = route.getLocation() + ": ";
                                description = description + route.getName();
                                ptRoute.setDescription(description);
                                ptRoute.setSource("dm_" + route.getId());
                                ptRoute.setType(route.getActivityType().toString());
                                persistance.addRoute(ptRoute);
                            } finally {
                                if (cursor != null)
                                    cursor.close();
                            }
                        }
                    } catch (Exception e) {
                        exception = e;
                    }
                    return null;
                }

                protected void onProgressUpdate(String... progress) {
                    progressDialog.setMessage(progress[0]);
                }

                @Override
                protected void onPostExecute(Void result) {
                    progressDialog.cancel();
                    Hint.log(this, "Total: " + totalCount + ", New: " + newCount);
                    String message = context.getString(R.string.totalRoutes) + ": " + totalCount + "\n"
                            + context.getString(R.string.newRoutes) + ": " + newCount + "\n";
                    for (String newRoute : newRoutes)
                        message = message + " -" + newRoute + "\n";
                    if (totalCount == 0)
                        message = getString(R.string.noRoutesFound) + "\n";
                    if (exception != null)
                        message = message + "\n" + getString(R.string.error) + ": " + exception.toString() + "\n";
                    adapter.swapCursor(getRoutes(sortOrder)).close();
                    adapter.notifyDataSetChanged();
                    updateNoRoutesInfo();

                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage(message).setTitle(R.string.dmRouteImport)
                            .setPositiveButton(context.getResources().getString(android.R.string.ok), null);

                    // 3. Get the AlertDialog from create()
                    builder.create().show();
                }
            }

            RouteTask routeTask = new RouteTask(this);
            routeTask.execute((Void) null);
            return true;
        }
        return false;
    }

    private class RouteItemAdapter extends CursorAdapter {

        public RouteItemAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            try {
                ((RouteItem) view).setRouteInfo(sessionPersistance.getRoute(cursor));
            } catch (Exception e) {
                Hint.log(this, e);
            }
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            try {
                return new RouteItem(context, sessionPersistance.getRoute(cursor));
            } catch (Exception e) {
                Hint.log(this, e);
                return null;
            }
        }
    }

}
