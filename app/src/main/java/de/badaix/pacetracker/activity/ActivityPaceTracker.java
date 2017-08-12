package de.badaix.pacetracker.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import de.badaix.pacetracker.InitService;
import de.badaix.pacetracker.R;
import de.badaix.pacetracker.activity.dailymile.DailyMileActivity;
import de.badaix.pacetracker.posprovider.GpsPositionProvider;
import de.badaix.pacetracker.session.SessionFactory;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.social.UrlImageViewHelper;
import de.badaix.pacetracker.util.CustomExceptionHandler;
import de.badaix.pacetracker.util.FileUtils;
import de.badaix.pacetracker.util.Hint;
import de.badaix.pacetracker.views.dailymile.CommentAnimator;

public class ActivityPaceTracker extends AppCompatActivity implements OnClickListener {
    private Button buttonNewSession;
    private Button buttonHistory;
    private Button buttonSettings;
    private Button buttonDailyMile;
    private Button buttonRoutes;
    private Button buttonTest;
    private MenuItem aboutItem;
    private MenuItem settingsItem;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Hint.log(this, "onCreate");
        Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(FileUtils.getCacheStreamPath(this,
                "stacktrace").getAbsolutePath(), null));

        if (GlobalSettings.getInstance(this).service == null) {
            SessionFactory.getInstance(this);
            startService(new Intent(this, InitService.class));
        } else {
            Intent intent = new Intent(this, ActivitySession.class);
            startActivity(intent);
            return;
        }

        // getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.background_dark));
        // Log.d("PaceTracker", "PaceTrackerActivity.onCreate()");
        setContentView(R.layout.activity_pacetracker);

        buttonNewSession = (Button) findViewById(R.id.dashboard_button_new_session);
        buttonNewSession.setOnClickListener(this);
        // buttonNewSession.setOnTouchListener(this);
        buttonHistory = (Button) findViewById(R.id.dashboard_button_history);
        buttonHistory.setOnClickListener(this);
        buttonSettings = (Button) findViewById(R.id.dashboard_button_settings);
        buttonSettings.setOnClickListener(this);
        buttonDailyMile = (Button) findViewById(R.id.dashboard_button_dailymile);
        buttonDailyMile.setOnClickListener(this);
        buttonRoutes = (Button) findViewById(R.id.dashboard_button_routes);
        buttonRoutes.setOnClickListener(this);
        buttonTest = (Button) findViewById(R.id.dashboard_button_test);
        buttonTest.setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        settingsItem = menu.add(R.string.globalSettings);
        settingsItem.setIcon(R.drawable.ic_action_settings).setIntent(new Intent(this, ActivityGlobalSettings.class))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        aboutItem = menu.add(R.string.about);
        aboutItem.setIcon(R.drawable.ic_action_about).setIntent(new Intent(this, ActivityAbout.class))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        return true;
    }

    @Override
    protected void onStart() {
        Hint.log(this, "onStart");
        super.onStart();
    }

    @Override
    protected void onResume() {
        Hint.log(this, "onResume");
        super.onResume();
        if (GlobalSettings.getInstance(this).service == null) {
            GpsPositionProvider.getInstance(this, false).stop(null);
            GlobalSettings.getInstance().route = null;
            GlobalSettings.getInstance().setSessionSummary(null);
            UrlImageViewHelper.getInstance().cleanUp();
            CommentAnimator.getInstance().stop();
            System.gc();
        }
    }

    @Override
    public void onClick(View v) {
        if (v == buttonNewSession) {
            Intent intent = new Intent(this, ActivityNewSession.class);
            startActivity(intent);
        } else if (v == buttonHistory) {
            Intent intent = new Intent(this, ActivitySessionHistory.class);
            startActivity(intent);
        } else if (v == buttonSettings) {
            Intent intent = new Intent(this, ActivityGlobalSettings.class);
            startActivity(intent);
        } else if (v == buttonDailyMile) {
            Intent intent = new Intent(this, DailyMileActivity.class);
            startActivity(intent);
        } else if (v == buttonRoutes) {
            Intent intent = new Intent(this, ActivityRoutes.class);
            startActivity(intent);
        } else if (v == buttonTest) {
            Intent intent = new Intent(this, ActivityTest.class);
            startActivity(intent);
        }
    }

}
