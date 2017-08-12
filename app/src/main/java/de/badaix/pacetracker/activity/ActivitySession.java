package de.badaix.pacetracker.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

import java.util.Vector;

import de.badaix.pacetracker.LockableViewPager;
import de.badaix.pacetracker.R;
import de.badaix.pacetracker.sensor.SensorData;
import de.badaix.pacetracker.sensor.SensorProvider;
import de.badaix.pacetracker.sensor.SensorProvider.SensorState;
import de.badaix.pacetracker.session.GpsPos;
import de.badaix.pacetracker.session.HxmData;
import de.badaix.pacetracker.session.Session;
import de.badaix.pacetracker.session.Session.State;
import de.badaix.pacetracker.session.SessionListener;
import de.badaix.pacetracker.session.SessionService;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.util.Hint;

public class ActivitySession extends AppCompatActivity implements SessionListener, OnClickListener,
        android.content.DialogInterface.OnClickListener, MenuItem.OnMenuItemClickListener, Callback {

    // boolean mBound = false;
    SessionService mService;
    private VectorFragmentPagerAdapter mAdapter;
    private LockableViewPager mPager;
    private Session session;
    private FragmentSessionPaceTable sessionStatisticsFragment;
    private FragmentSessionOverview sessionOverviewFragment;
    private FragmentSessionMap sessionMapFragment;
    private ImageButton btnStop;
    private ImageButton btnPause;
    private Vector<SessionGUI> vSessionGUIs;
    private MenuItem ttsItem;
    private MenuItem lockItem;
    private AlertDialog.Builder dialogBuilder;
    private AlertDialog stopDialog;
    private int choice;
    private boolean isResumed = false;
    private Handler handler;
    private Runnable updateRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Hint.log(this, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session);
        mService = GlobalSettings.getInstance(this).service;
        if (mService == null) {
            this.finish();
        } else {
            handler = new Handler(this);
            session = mService.getSession();
            getSupportActionBar().setTitle(session.getName(this));

            Vector<Fragment> fragments = new Vector<Fragment>();
            if (savedInstanceState != null) {
                sessionStatisticsFragment = (FragmentSessionPaceTable) getSupportFragmentManager().getFragment(
                        savedInstanceState, FragmentSessionPaceTable.class.getName());
                sessionMapFragment = (FragmentSessionGmsMap) getSupportFragmentManager().getFragment(
                        savedInstanceState, FragmentSessionGmsMap.class.getName());
                sessionOverviewFragment = (FragmentSessionOverview) getSupportFragmentManager().getFragment(
                        savedInstanceState, FragmentSessionOverview.class.getName());
            } else {
                sessionStatisticsFragment = new FragmentSessionPaceTable();
                sessionOverviewFragment = new FragmentSessionOverview();
                sessionMapFragment = new FragmentSessionGmsMap();
            }
            sessionStatisticsFragment.setTitle(getResources().getString(R.string.fragmentPaceTable));
            sessionOverviewFragment.setTitle(getResources().getString(R.string.fragmentOverview));

            sessionMapFragment.lock(true);
            sessionMapFragment.setOffline(false);
            sessionMapFragment.setTitle(getResources().getString(R.string.fragmentMap));

            fragments.add(sessionOverviewFragment);
            fragments.add(sessionStatisticsFragment);
            fragments.add(sessionMapFragment);

            btnStop = (ImageButton) findViewById(R.id.buttonSessionStop);
            btnPause = (ImageButton) findViewById(R.id.buttonSessionPause);

            btnStop.setOnClickListener(this);
            btnPause.setOnClickListener(this);

            btnStop.setEnabled(false);
            btnPause.setEnabled(false);

            vSessionGUIs = new Vector<SessionGUI>();
            vSessionGUIs.add(sessionOverviewFragment);
            vSessionGUIs.add(sessionStatisticsFragment);
            vSessionGUIs.add(sessionMapFragment);
            for (SessionGUI s : vSessionGUIs) {
                s.setSession(session);
            }

            dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle(getResources().getString(R.string.confirmStopSession));
            dialogBuilder.setCancelable(false);
            dialogBuilder.setSingleChoiceItems(
                    new CharSequence[]{getResources().getString(R.string.saveSession),
                            getResources().getString(R.string.editAndSaveSession),
                            getResources().getString(R.string.discardSession)}, 0, this);
            dialogBuilder.setPositiveButton(getResources().getString(android.R.string.ok), this);
            dialogBuilder.setNegativeButton(getResources().getString(android.R.string.cancel), this);
            choice = 0;

            mAdapter = new VectorFragmentPagerAdapter(getSupportFragmentManager(), fragments);
            mPager = (LockableViewPager) findViewById(R.id.sessionPager);
            mPager.setAdapter(mAdapter);
//			new AdMobHelper(this);
        }
    }

    void updateGui() {
        Hint.log(this, "updateGui - !session: " + (session == null) + " !resumed: " + !this.isResumed + " !tts: "
                + (ttsItem == null));
        if ((session == null) || !this.isResumed || (ttsItem == null))
            return;
        Hint.log(this, "updateGui not null");

        Hint.log(this, "updateGui");
        ttsItem.setChecked(session.getSettings().isVoiceFeedback());
        lockItem.setChecked(session.getSettings().getBundle().getBoolean("isLocked", false));
        updateActionItems();

        Session.State state = session.getState();
        getSupportActionBar().setSubtitle(state.toActionString(this));
        try {
            updateActionItems();
        } catch (Exception e) {
        }
        boolean stopEnabled = true;
        boolean pauseEnabled = true;

        if (state.equals(State.STOPPED)) {
            stopEnabled = false;
            pauseEnabled = false;
        } else if (state.equals(State.WAITSTART)) {
            pauseEnabled = false;
        }

        btnStop.setEnabled(stopEnabled);
        btnPause.setEnabled(pauseEnabled);
        btnPause.setImageResource(state.equals(State.PAUSED) ? R.drawable.av_play_over_video
                : R.drawable.av_pause_over_video);
//        getSupportActionBar().setLogo(session.getLightDrawable());
//        getSupportActionBar().setDisplayUseLogoEnabled(true);
//        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Hint.log(this, "onConfigurationChanged");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Hint.log(this, "onSaveInstanceState");
        super.onSaveInstanceState(outState);
        for (int i = 0; i < mAdapter.getCount(); ++i) {
            Fragment fragment = mAdapter.getItem(i);
            getSupportFragmentManager().putFragment(outState, fragment.getClass().getName(), fragment);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Hint.log(this, "onCreateOptionsMenu");
        ttsItem = menu.add("TTS");
        ttsItem.setOnMenuItemClickListener(this).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        lockItem = menu.add("Lock");
        lockItem.setOnMenuItemClickListener(this).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        updateGui();
        return true;
    }

    @Override
    protected void onStart() {
        Hint.log(this, "onStart");
        super.onStart();
    }

    @Override
    protected void onStop() {
        Hint.log(this, "onStop");
        super.onStop();
    }

    @Override
    public void onResume() {
        Hint.log(this, "onResume");
        super.onResume();
        mService.setListener(this);
        try {
            Bundle bundle = session.getSettings().getBundle();
            if (bundle != null) {
                mPager.setCurrentItem(bundle.getInt(this.getClass().getSimpleName() + "_activepage"));
            }
        } catch (Exception e) {
        }
        isResumed = true;
        updateGui();
        triggerOnGuiTimer();
    }

    @Override
    public void onPause() {
        Hint.log(this, "onPause");
        isResumed = false;
        mService.setListener(null);
        try {
            session.getSettings().getBundle()
                    .putInt(this.getClass().getSimpleName() + "_activepage", mPager.getCurrentItem());
        } catch (Exception e) {
        }
        super.onPause();
        // if (session.isStarted() && !isFinishing()) {
        // Intent intent = new Intent(this, ActivityPaceTracker.class);
        // intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        // startActivity(intent);
        // }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // If the back Key was pressed, then finish the program.
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            Hint.log(this, "KEYCODE_BACK");

            // Intent intent = new Intent(this, ActivityPaceTracker.class);
            // intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            // startActivity(intent);
            // return true;

            if (session.isStopped()) {
                Hint.log(this, "removing location updates");
                finish();
            } else {
                Hint.show(this, "You can not go back while a session is running");
                return true;
            }
        }

        // else return the normal function of whatever key was pressed
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        Hint.log(this, "onDestroy");
        try {
            handler.removeCallbacks(updateRunnable);
            vSessionGUIs.clear();
        } catch (Exception e) {
            Hint.log(this, e);
        }
        super.onDestroy();
    }

    @Override
    public void onStateChanged(Session.State oldState, Session.State newState) {
        if (newState == State.STOPPED)
            handler.removeCallbacks(updateRunnable);

        updateGui();

        for (SessionUI ui : vSessionGUIs)
            ui.onStateChanged(oldState, newState);
    }

    @Override
    public void onLocationChanged(Location location) {
        for (SessionUI ui : vSessionGUIs) {
            ui.onLocationChanged(location);
        }
    }

    @Override
    public void onSensorData(SensorProvider provider, SensorData sensorData) {
        for (SessionGUI ui : vSessionGUIs) {
            ui.onSensorData(provider, sensorData);
        }
    }

    @Override
    public void onSensorStateChanged(SensorProvider provider, boolean active, SensorState sensorState) {
        for (SessionGUI ui : vSessionGUIs) {
            ui.onSensorStateChanged(provider, active, sensorState);
        }
    }

    @Override
    public void onSensorDataChanged(HxmData hxmData) {
        for (SessionGUI ui : vSessionGUIs) {
            ui.onSensorDataChanged(hxmData);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == btnStop) {
            btnStop.setEnabled(false);
            session.pause();
            stopDialog = dialogBuilder.show();
        } else if (v == btnPause) {
            if ((session.getState() == State.RUNNING) || (session.getState() == State.AUTOPAUSED))
                session.pause();
            else if (session.getState() == State.PAUSED)
                session.resume();
        }
    }

    @Override
    public void onGpsStatusChanged(boolean active, boolean hasFix, int fixCount, int satCount) {
        for (SessionGUI ui : vSessionGUIs)
            ui.onGpsStatusChanged(active, hasFix, fixCount, satCount);
    }

    @Override
    public void onSessionCommand(int command) {
        for (SessionGUI ui : vSessionGUIs)
            ui.onSessionCommand(command);
    }

    @Override
    public void onFilteredLocationChanged(GpsPos location) {
        for (SessionGUI ui : vSessionGUIs)
            ui.onFilteredLocationChanged(location);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (dialog == stopDialog) {
            Hint.log(this, "stop clicked: " + which);
            if (which >= 0)
                choice = which;
            else if (which == DialogInterface.BUTTON_POSITIVE) {
                if (choice == 0) {
                    mService.stopSession(false);
                    Intent intent = new Intent(this, ActivityViewSession.class);
                    intent.putExtra("ClearTop", true);
                    startActivity(intent);
                } else if (choice == 1) {
                    mService.stopSession(false);
                    Intent intent = new Intent(this, ActivityEditSession.class);
                    intent.putExtra("OpenViewSession", true);
                    startActivity(intent);
                } else if (choice == 2) {
                    mService.stopSession(true);
                    Intent intent = new Intent(this, ActivityPaceTracker.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }
            } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                btnStop.setEnabled(true);
                session.resume();
            }
        }
    }

    public void triggerOnGuiTimer() {
        // Hint.log(this, "updateUIs, resumed: " + isResumed);
        if (session != null) {
            for (SessionGUI ui : vSessionGUIs)
                ui.onGuiTimer(isResumed);
            if (session.getSettings().getGoal() != null) {
                session.getSettings().getGoal().updateGui();
            }
        }
        handler.removeCallbacks(updateRunnable);
        updateRunnable = new Runnable() {

            @Override
            public void run() {
                triggerOnGuiTimer();
            }
        };
        handler.postDelayed(updateRunnable, 1000);
    }

    private void updateActionItems() {
        if (ttsItem.isChecked()) {
            ttsItem.setIcon(R.drawable.ic_action_unmute);
        } else {
            ttsItem.setIcon(R.drawable.ic_action_mute);
        }
        session.getSettings().setVoiceFeedback(ttsItem.isChecked());
        mService.enableTts(ttsItem.isChecked());

        if (lockItem.isChecked())
            lockItem.setIcon(R.drawable.ic_action_lock);
        else
            lockItem.setIcon(R.drawable.ic_action_unlock);
        session.getSettings().getBundle().putBoolean("isLocked", lockItem.isChecked());
        Hint.log(this, "Lock: " + lockItem.isChecked());
        sessionMapFragment.lock(lockItem.isChecked());
        mPager.lock(lockItem.isChecked());
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        item.setChecked(!item.isChecked());
        updateActionItems();
        return true;
    }

    @Override
    public boolean handleMessage(Message msg) {
        return false;
    }

}
