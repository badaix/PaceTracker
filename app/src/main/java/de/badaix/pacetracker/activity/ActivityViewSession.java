package de.badaix.pacetracker.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.Window;

import java.io.IOException;
import java.util.Vector;

import de.badaix.pacetracker.LockableViewPager;
import de.badaix.pacetracker.R;
import de.badaix.pacetracker.session.Session;
import de.badaix.pacetracker.session.SessionFactory;
import de.badaix.pacetracker.session.SessionReader;
import de.badaix.pacetracker.session.SessionSummary;
import de.badaix.pacetracker.session.post.PostGPlus;
import de.badaix.pacetracker.session.post.PostSessionDialog;
import de.badaix.pacetracker.session.post.PostSessionListener;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.settings.SessionSettings;
import de.badaix.pacetracker.social.dailymile.DailyMileHelper;
import de.badaix.pacetracker.util.ExceptionHandler;
import de.badaix.pacetracker.util.Hint;

public class ActivityViewSession extends AppCompatActivity implements OnPageChangeListener,
        MenuItem.OnMenuItemClickListener, PostSessionListener {
    VectorFragmentPagerAdapter mAdapter;
    LockableViewPager mPager;
    private Session session;
    private FragmentSessionPaceTable sessionStatisticsFragment = null;
    private FragmentSessionGraph sessionGraphFragment = null;
    private FragmentSessionMap sessionMapFragment = null;
    private FragmentSessionOfflineOverview sessionOverviewFragment = null;
    private Vector<SessionUI> vSessionUIs;
    private MenuItem lockItem = null;
    //private MenuItem earthItem = null;
    private MenuItem dmItem;
    private MenuItem gpItem;
    private MenuItem fbItem;
    private LoadSessionTask loadSessionTask;
    private SessionSummary sessionSummary;
    private PostGPlus postGPlus;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (GlobalSettings.getInstance().getContext() == null) {
            Intent intent = new Intent(this, ActivityPaceTracker.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        try {
            super.onCreate(savedInstanceState);
            requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
            setContentView(R.layout.activity_view_session);

            sessionSummary = GlobalSettings.getInstance(this).getSessionSummary();

            getSupportActionBar().setTitle(sessionSummary.getName(this));
//            getSupportActionBar().setLogo(sessionSummary.getLightDrawable());
//            getSupportActionBar().setDisplayUseLogoEnabled(true);
//            getSupportActionBar().setDisplayShowHomeEnabled(true);
            String description = sessionSummary.getSettings().getDescription();
            if ((description != null) && (description.length() > 0))
                getSupportActionBar().setSubtitle(description);

            Vector<Fragment> fragments = new Vector<Fragment>();
            sessionOverviewFragment = new FragmentSessionOfflineOverview(sessionSummary);
            sessionOverviewFragment.setTitle(getResources().getString(R.string.fragmentOverview));
            fragments.add(sessionOverviewFragment);

            vSessionUIs = new Vector<SessionUI>();
            vSessionUIs.add(sessionOverviewFragment);
            SessionSettings settings = sessionSummary.getSettings();
            // if (settings.getGoal() != null)
            // settings.getGoal().addSessionUI(sessionOverviewFragment);
            if (settings.getGoal() != null)
                settings.getGoal().init(this, true);

            sessionOverviewFragment.setSession(sessionSummary);

            if ((settings.getPositionProvider() != null) && settings.getPositionProvider().hasLocationInfo()) {
                // mapFragment = new GmsFragment();
                sessionMapFragment = new FragmentSessionGmsMap();

                // sessionMapFragment.setMapFragment(mapFragment);
                sessionMapFragment.lock(true);
                sessionMapFragment.setOffline(true);
                sessionMapFragment.setTitle(getResources().getString(R.string.fragmentMap));

                sessionStatisticsFragment = new FragmentSessionPaceTable();
                sessionStatisticsFragment.setTitle(getResources().getString(R.string.fragmentPaceTable));
                sessionGraphFragment = new FragmentSessionGraph();
                sessionGraphFragment.setTitle(getResources().getString(R.string.fragmentGraph));
                fragments.add(sessionStatisticsFragment);
                fragments.add(sessionMapFragment);
                fragments.add(sessionGraphFragment);

                vSessionUIs.add(sessionStatisticsFragment);
                vSessionUIs.add(sessionMapFragment);
                vSessionUIs.add(sessionGraphFragment);

                if (settings.getGoal() != null)
                    vSessionUIs.add(settings.getGoal());
            }

            // ViewPagerIndicator indicator =
            // (ViewPagerIndicator)findViewById(R.id.viewSessionIndicator);
            mAdapter = new VectorFragmentPagerAdapter(getSupportFragmentManager(), fragments);
            mPager = (LockableViewPager) findViewById(R.id.viewSessionPager);
            mPager.setAdapter(mAdapter);
            // PagerTitleStrip titleStrip =
            // (PagerTitleStrip)findViewById(R.id.viewSessionIndicator);
            // titleStrip.setEnabled(false);
            // mPager.setOnPageChangeListener(indicator);
            // indicator.init(0, mAdapter.getCount(), this);

            // Set images for previous and next arrows.
            lock(true);
            mPager.setOnPageChangeListener(this);

            loadSessionTask = new LoadSessionTask(sessionSummary);
            if (Build.VERSION.SDK_INT >= 13)
                loadSessionTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this);
            else
                loadSessionTask.execute(this);

        } catch (Exception e) {
            Hint.log(this, e);
            finish();
        }
    }

    // @Override
    // public void onStart() {
    // super.onStart();
    // }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //earthItem = menu.add(getString(R.string.viewEarth));
        //earthItem.setIcon(R.drawable.location_web_site).setOnMenuItemClickListener(this)
        //        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        //earthItem.setVisible(sessionSummary.getSettings().getPositionProvider().hasLocationInfo());
        lockItem = menu.add(R.string.lock);
        lockItem.setIcon(R.drawable.ic_action_unlock).setOnMenuItemClickListener(this)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        SubMenu subMenuShare = menu.addSubMenu(R.string.share);

        dmItem = subMenuShare.add(R.string.dailymile).setIcon(R.drawable.dailymile32);
        fbItem = subMenuShare.add(R.string.facebook).setIcon(R.drawable.facebook32);
        gpItem = subMenuShare.add(R.string.googlePlus).setIcon(R.drawable.gplus32);

        dmItem.setOnMenuItemClickListener(this);
        fbItem.setOnMenuItemClickListener(this);
        gpItem.setOnMenuItemClickListener(this);

        if (DailyMileHelper.getActivity(sessionSummary) == null)
            dmItem.setVisible(false);
        if (sessionSummary.getSettings().getDailyMileId() != -1)
            dmItem.setEnabled(false);

        if (!GlobalSettings.getInstance(this).isFacebookEnabled())
            fbItem.setVisible(false);
        if (!TextUtils.isEmpty(sessionSummary.getSettings().getFbId()))
            fbItem.setEnabled(false);

        if (!GlobalSettings.getInstance(this).isGplusEnabled())
            gpItem.setVisible(false);
        if (sessionSummary.getSettings().getGPlusId() != -1)
            gpItem.setEnabled(false);

        // if (!dmItem.isVisible() && !fbItem.isVisible() &&
        // !gpItem.isVisible())
        // subMenuShare.h

        MenuItem menuShare = subMenuShare.getItem();
        menuShare.setIcon(R.drawable.ic_menu_share_holo_dark);
        menuShare.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        menuShare.setVisible(subMenuShare.hasVisibleItems());

        return true;
    }

    private void lock(boolean lock) {
        // if (sessionOsMapFragment != null)
        // sessionOsMapFragment.lock(!lock);
        mPager.lock(lock);
        sessionMapFragment.lock(lock);
    }

    public void setSession(Session session) {

    }

    @Override
    public void onPause() {
        super.onPause();
        if (loadSessionTask != null)
            loadSessionTask.cancel(true);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // If the back Key was pressed, then finish the program.
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            if (getIntent().getBooleanExtra("ClearTop", false) == false) {
                this.finish();
            } else {
                Intent intent = new Intent(this, ActivityPaceTracker.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        }

        // else return the normal function of whatever key was pressed
        return super.onKeyDown(keyCode, event);
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
    public boolean onMenuItemClick(MenuItem item) {
        //if (item == earthItem) {
        //    GoogleEarth.playTrack(this, sessionSummary);
        //} else
        if (item == lockItem) {
            lockItem.setChecked(!lockItem.isChecked());
            this.lock(lockItem.isChecked());
            if (lockItem.isChecked())
                lockItem.setIcon(R.drawable.ic_action_lock);
            else
                lockItem.setIcon(R.drawable.ic_action_unlock);
        } else if (item == dmItem) {
            PostSessionDialog postSessionDialog = new PostSessionDialog();
            postSessionDialog.post(this, getSupportFragmentManager(), this, session);
        } else if (item == gpItem) {
            postGPlus = new PostGPlus(this);
            postGPlus.post(this, sessionSummary);
//		} else if (item == fbItem) {
//			final FragmentFacebookShare facebookFragment = new FragmentFacebookShare();
//			facebookFragment.setSession(sessionSummary);
//			facebookFragment.setListener(new OnShareListener() {
//
//				@Override
//				public void onSuccess(SessionSummary session, String postId) {
//					session.getSettings().setFbId(postId);
//					SessionWriter sessionWriter = new SessionWriter(ActivityViewSession.this);
//					try {
//						sessionWriter.updateSession(session);
//						fbItem.setEnabled(false);
//					} catch (IOException e) {
//						Hint.log(this, e);
//					}
//					facebookFragment.dismiss();
//				}
//
//				@Override
//				public void onException(SessionSummary session, Exception exception) {
//					Hint.log(this, exception);
//					facebookFragment.dismiss();
//				}
//			});
//			facebookFragment.show(getSupportFragmentManager(),
//					"facebookFragment");
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == PostGPlus.GPLUS_REQUEST_CODE) && (resultCode == RESULT_OK)) {
            try {
                postGPlus.updateSession();
                gpItem.setEnabled(false);
            } catch (IOException e) {
                Hint.show(this, e);
            }
        }
    }

    @Override
    public void onSessionPostet(SessionSummary sessionSummary) {
        dmItem.setEnabled(false);
        for (SessionUI s : vSessionUIs) {
            s.update();
        }
    }

    @Override
    public void onPostSessionFailed(SessionSummary sessionSummary, Exception exception) {
        Hint.show(this, getString(R.string.error_posting_session) + ":\n" + exception.getMessage());
    }

    private class LoadSessionTask extends AsyncTask<ActivityViewSession, Void, ActivityViewSession> {
        private SessionSummary sessionSummary;

        public LoadSessionTask(SessionSummary sessionSummary) {
            setProgressBarIndeterminateVisibility(true);
            this.sessionSummary = sessionSummary;
        }

        /**
         * The system calls this to perform work in a worker thread and delivers
         * it the parameters given to AsyncTask.execute()
         */
        @Override
        protected ActivityViewSession doInBackground(ActivityViewSession... item) {
            ActivityViewSession viewSessionActivity = item[0];
            if (sessionSummary instanceof Session)
                viewSessionActivity.session = (Session) sessionSummary;
            else {
                try {
                    // viewSessionActivity.session =
                    // sessionSummary.getSession();
                    viewSessionActivity.session = SessionFactory.getInstance(getApplicationContext()).getSessionByType(
                            sessionSummary.getType(), null, new SessionSettings(false));
                    SessionReader reader = new SessionReader();
                    if (!isCancelled()) {
                        reader.readSessionFromFile(sessionSummary.getFilename(), viewSessionActivity.session);
                    }
                } catch (Exception e) {
                    ExceptionHandler.Handle("PaceTracker", e);
                }
            }
            return viewSessionActivity;
        }

        @Override
        protected void onPostExecute(ActivityViewSession result) {

            for (SessionUI s : result.vSessionUIs) {
                s.setSession(result.session);
                s.update();
            }

            Hint.log(this, "onPostExecute");
            if (result.sessionMapFragment != null)
                result.sessionMapFragment.zoomToRoute(false);

            result.lock(false);
            setSupportProgressBarIndeterminateVisibility(false);
        }
    }

}
