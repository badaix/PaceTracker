package de.badaix.pacetracker.activity;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Vector;

import de.badaix.pacetracker.LockableViewPager;
import de.badaix.pacetracker.R;
import de.badaix.pacetracker.SessionPersistance;
import de.badaix.pacetracker.session.Route;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.util.GpxReader;
import de.badaix.pacetracker.util.Hint;
import de.badaix.pacetracker.util.HttpDownloader;
import de.badaix.pacetracker.util.KmlReader;

public class ActivityViewRoute extends AppCompatActivity implements OnPageChangeListener, OnClickListener,
        android.content.DialogInterface.OnClickListener, MenuItem.OnMenuItemClickListener {
    VectorFragmentPagerAdapter mAdapter;
    LockableViewPager mPager;
    private FragmentSessionGmsMap sessionMapFragment = null;
    private FragmentRouteSegment routeSegmentFragment = null;
    // private ToggleAction lockAction;
    private MenuItem lockItem;
    private Button btnFooter;
    private AlertDialog saveDialog;
    private LinearLayout llFooter;
    private EditText dialogEditText;
    private Route route = null;
    private String type;
    private String scheme;
    private URI uri;

    // RouteLoader loader = new RouteLoader(this, true, this);
    // loader.execute(new File(filename));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (GlobalSettings.getInstance().getContext() == null)
            this.finish();
        try {
            super.onCreate(savedInstanceState);
            requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
            GlobalSettings.getInstance(this);
            setContentView(R.layout.activity_view_route);

            btnFooter = (Button) findViewById(R.id.btnFooter);
            btnFooter.setOnClickListener(this);

            llFooter = (LinearLayout) findViewById(R.id.llFooter);
            Intent intent = getIntent();
            if (getIntent().getBooleanExtra("view", false))
                llFooter.setVisibility(View.GONE);

            Vector<Fragment> fragments = new Vector<Fragment>();
            // mapFragment = new GmsFragment();
            sessionMapFragment = new FragmentSessionGmsMap();

            // sessionMapFragment.setMapFragment(mapFragment);
            sessionMapFragment.lock(true);
            sessionMapFragment.setOffline(true);
            sessionMapFragment.setTitle(getResources().getString(R.string.fragmentMap));

            fragments.add(sessionMapFragment);
            routeSegmentFragment = new FragmentRouteSegment();
            routeSegmentFragment.setTitle(getResources().getString(R.string.fragmentSegments));
            fragments.add(routeSegmentFragment);

            mAdapter = new VectorFragmentPagerAdapter(getSupportFragmentManager(), fragments);
            mPager = (LockableViewPager) findViewById(R.id.viewRoutePager);
            mPager.setAdapter(mAdapter);

            if ((intent.getExtras() != null) && ("RoutesActivity".equals(intent.getExtras().getString("openIntent"))))
                setRoute(GlobalSettings.getInstance(this).route);
            else
                loadRoute(intent);

            // Set images for previous and next arrows.
            lock(false);
            mPager.setOnPageChangeListener(this);
        } catch (Exception e) {
            Hint.log(this, e);
            finish();
        }
    }

    private void setRoute(Route route) {
        ActivityViewRoute.this.route = route;
        sessionMapFragment.setRoute(route);
        routeSegmentFragment.setRoute(route);
        sessionMapFragment.zoomToRoute(false);
        setProgressBarIndeterminateVisibility(false);
        getSupportActionBar().setTitle(route.getName());
        if (!TextUtils.isEmpty(route.getDescription()))
            getSupportActionBar().setSubtitle(route.getDescription());
    }

    private void loadRoute(Intent intent) {
        if (!Intent.ACTION_VIEW.equals(intent.getAction()))
            return;

        type = "application/vnd.google-earth.kml+xml";
        if (intent.getType() != null)
            type = intent.getType();

        scheme = "file";
        if (intent.getScheme() != null)
            scheme = intent.getScheme();

        uri = null;
        try {
            if (intent.getDataString() != null)
                uri = new URI(intent.getDataString());
            else
                uri = new URI(intent.getData().toString());
        } catch (URISyntaxException e) {
            Hint.show(this, e);
            return;
        }

        Hint.log(this, "Type: " + type + ", scheme: " + scheme + ", uri: " + uri.toString());

        AsyncTask<Void, Void, Route> loaderTask = new AsyncTask<Void, Void, Route>() {
            Exception exception = null;

            @Override
            protected Route doInBackground(Void... params) {
                Route route = null;
                String name = "";
                try {
                    if ("application/json".equals(type))
                        route = new Route(new File(uri));
                    else if (type.contains("kml") || type.contains("gpx")) {
                        InputStreamReader reader = null;
                        if ("file".equals(scheme)) {
                            reader = new FileReader(new File(uri));
                            name = new File(uri).getName();
                        } else if ("content".equals(scheme)) {
                            ContentResolver contentResolver = ActivityViewRoute.this.getContentResolver();
                            Uri tmpUri = Uri.parse(uri.toASCIIString());
                            reader = new InputStreamReader(contentResolver.openInputStream(tmpUri));
                            name = tmpUri.getLastPathSegment();
                            Hint.log(this, "uri: " + tmpUri.toString());
                        } else if ("http".equals(scheme) || "https".equals(scheme)) {
                            HttpDownloader downloader = new HttpDownloader();
                            reader = new InputStreamReader(downloader.getStream(uri.toURL()));
                        }

                        BufferedReader br = new BufferedReader(reader);
                        String line;
                        StringBuilder text = new StringBuilder();
                        while ((line = br.readLine()) != null) {
                            text.append(line);
                            text.append('\n');
                        }

                        if (type.contains("gpx"))
                            route = GpxReader.parseGpx(text.toString());
                        else
                            route = KmlReader.parseKml(text.toString());

                        if (TextUtils.isEmpty(route.getName()))
                            route.setName(name);

                    } else {
                        throw new Exception("Failed to load route");
                    }
                } catch (Exception e) {
                    exception = e;
                }
                return route;
            }

            protected void onPostExecute(Route route) {
                if (exception != null) {
                    setProgressBarIndeterminateVisibility(false);
                    Hint.show(ActivityViewRoute.this, exception);
                    return;
                }
                setRoute(route);
            }
        };

        setProgressBarIndeterminateVisibility(true);
        loaderTask.execute((Void) null);
    }

    private void lock(boolean lock) {
        mPager.lock(lock);
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

    @Override
    public void onResume() {
        Hint.log(this, "onResume");
        super.onResume();
        if (route != null) {
            sessionMapFragment.setRoute(route);
        }
        sessionMapFragment.getMyLocation(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        lockItem = menu.add("Lock");
        lockItem.setIcon(R.drawable.ic_action_unlock).setOnMenuItemClickListener(this)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        return true;
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        // sessionMapFragment.setCompassActive(positionOffset == 0);
    }

    @Override
    public void onPageSelected(int position) {
    }

    @Override
    public void onClick(View view) {
        if (view == btnFooter) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            dialogEditText = new EditText(this);
            dialogEditText.setText(route.getName());
            builder.setView(dialogEditText);
            builder.setTitle(getResources().getString(R.string.saveRouteDialog));
            builder.setMessage(getResources().getString(R.string.saveRouteText)).setCancelable(true)
                    .setPositiveButton(getResources().getText(android.R.string.ok), this);
            saveDialog = builder.create();
            saveDialog.show();
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (dialog == saveDialog) {
            String routeName = dialogEditText.getText().toString();
            route.setName(routeName);
            SessionPersistance sessionPersistance = SessionPersistance.getInstance(this);
            try {
                // TODO route
                sessionPersistance.addRoute(route);
                closeActivity();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Hint.show(this, "Failed to store route: " + e.getMessage());
            }
        }
    }

    private void closeActivity() {
        String intentToOpen = getIntent().getStringExtra("openIntent");
        if (intentToOpen != null) {
            if (intentToOpen.equals("RoutesActivity")) {
                Intent intent = new Intent(this, ActivityRoutes.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            } else
                this.finish();
        } else
            this.finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // If the back Key was pressed, then finish the program.
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0)
            closeActivity();

        // else return the normal function of whatever key was pressed
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item == lockItem) {
            lockItem.setChecked(!lockItem.isChecked());
            this.lock(lockItem.isChecked());
            sessionMapFragment.lock(lockItem.isChecked());
            if (lockItem.isChecked())
                lockItem.setIcon(R.drawable.ic_action_lock);
            else
                lockItem.setIcon(R.drawable.ic_action_unlock);
        }
        return true;
    }

}
