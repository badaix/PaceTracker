package de.badaix.pacetracker.activity;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerTabStrip;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Date;
import java.util.Vector;

import de.badaix.pacetracker.LockableViewPager;
import de.badaix.pacetracker.R;
import de.badaix.pacetracker.SessionPersistance;
import de.badaix.pacetracker.session.SessionFactory;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.util.DateUtils;
import de.badaix.pacetracker.util.Distance;
import de.badaix.pacetracker.util.ExceptionHandler;
import de.badaix.pacetracker.util.Hint;
import de.badaix.pacetracker.util.TextItemPair;

public class ActivitySessionHistory extends AppCompatActivity implements OnItemSelectedListener,
        MenuItem.OnMenuItemClickListener {
    private Spinner spinnerTypeFilter;
    private Spinner spinnerDateFilter;
    private TextView tvTotalDuration;
    private TextView tvTotalDistance;
    private TextView tvMeanSpeed;
    private TextView tvNumSessions;
    private LinearLayout filterLayout;
    private String typeFilter;
    private String dateFilter;
    private VectorFragmentPagerAdapter mAdapter;
    private PagerTabStrip pagerTabStrip;
    private LockableViewPager mPager;
    private FragmentHistoryList historyListFragment;
    private FragmentHistoryGraph historyGraphFragment;
    private MenuItem lockItem;
    private MenuItem filterItem;
    private MenuItem addItem;
    private Intent manualIntent;
    private boolean doUpdate;
    private SessionPersistance sessionPersistance = null;
    private Button buttonStartNewSession;
    private LinearLayout linearLayoutNoSessions;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (GlobalSettings.getInstance().getContext() == null)
            this.finish();
        try {
            super.onCreate(savedInstanceState);
            doUpdate = false;
            Log.d("PaceTracker", "PaceTrackerActivity.onCreate()");
            setContentView(R.layout.activity_session_history);

//            getSupportActionBar().setLogo(R.drawable.dashboard_button_history);
//            getSupportActionBar().setDisplayUseLogoEnabled(true);
//            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.mainMenuSessionHistory);

            tvTotalDistance = (TextView) findViewById(R.id.historyTvTotalDistance);
            tvTotalDuration = (TextView) findViewById(R.id.historyTvTotalDuration);
            tvMeanSpeed = (TextView) findViewById(R.id.historyTvMeanSpeed);
            tvNumSessions = (TextView) findViewById(R.id.historyTvNumSessions);

            buttonStartNewSession = (Button) findViewById(R.id.buttonStartNewSession);
            linearLayoutNoSessions = (LinearLayout) findViewById(R.id.linearLayoutNoSessions);
            linearLayoutNoSessions.setVisibility(View.GONE);
            buttonStartNewSession.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getApplicationContext(), ActivityNewSession.class);
                    startActivity(intent);
                }
            });

            sessionPersistance = SessionPersistance.getInstance(this);

            spinnerDateFilter = (Spinner) findViewById(R.id.spinnerDateFilter);
            ArrayAdapter<TextItemPair<Integer>> dateAdapter = new ArrayAdapter<TextItemPair<Integer>>(this,
                    android.R.layout.simple_spinner_item);

            String dateFilterNames[] = getResources().getStringArray(R.array.dateFilterNames);
            int dateFilterValues[] = getResources().getIntArray(R.array.dateFilterValues);
            for (int i = 0; i < dateFilterNames.length; ++i)
                dateAdapter.add(new TextItemPair<Integer>(dateFilterNames[i], dateFilterValues[i]));
            dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            int lastDateFilter = GlobalSettings.getInstance(this).getInt("dateFilter", 0);
            spinnerDateFilter.setAdapter(dateAdapter);
            spinnerDateFilter.setOnItemSelectedListener(this);
            filterLayout = (LinearLayout) findViewById(R.id.linearLayoutFilter);

            manualIntent = new Intent(this, ActivityManualSession.class);

            typeFilter = "";
            dateFilter = "";
            spinnerTypeFilter = (Spinner) findViewById(R.id.spinnerTypeFilter);
            ArrayAdapter<TextItemPair<String>> typeAdapter = new ArrayAdapter<TextItemPair<String>>(this,
                    android.R.layout.simple_spinner_item);
            typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            typeAdapter.add(new TextItemPair<String>(getString(R.string.typeFilterAll), ""));
            String fields[] = {"type"};
            Cursor cursor = null;
            try {
                cursor = sessionPersistance.querySessions(fields, null, null, "type asc", "type");
                int typeColumn = cursor.getColumnIndex("type");
                if ((cursor != null) && (cursor.moveToFirst())) {
                    do {
                        typeAdapter.add(new TextItemPair<String>(SessionFactory.getInstance().getSessionNameFromType(
                                cursor.getString(typeColumn)), cursor.getString(typeColumn)));
                    } while (cursor.moveToNext());
                }
            } finally {
                if ((cursor != null) && !cursor.isClosed())
                    cursor.close();
            }

            int lastTypeFilter = GlobalSettings.getInstance().getInt("typeFilter", 0);
            spinnerTypeFilter.setAdapter(typeAdapter);
            spinnerTypeFilter.setOnItemSelectedListener(this);
            if (sessionPersistance != null)
                sessionPersistance.closeDB();
            spinnerDateFilter.setSelection(lastDateFilter);
            spinnerTypeFilter.setSelection(lastTypeFilter);
            onItemSelected(spinnerDateFilter, null, lastDateFilter, 0);
            onItemSelected(spinnerTypeFilter, null, lastTypeFilter, 0);

            Vector<Fragment> fragments = new Vector<Fragment>();
            historyListFragment = new FragmentHistoryList(getResources().getString(R.string.historyList), this, this);
            fragments.add(historyListFragment);
            historyGraphFragment = new FragmentHistoryGraph();
            historyGraphFragment.setTitle(getResources().getString(R.string.historyGraph));
            fragments.add(historyGraphFragment);

            mAdapter = new VectorFragmentPagerAdapter(getSupportFragmentManager(), fragments);
            mPager = (LockableViewPager) findViewById(R.id.historyPager);
            mPager.setAdapter(mAdapter);

            pagerTabStrip = (PagerTabStrip) findViewById(R.id.historyIndicator);
            pagerTabStrip.setTextColor(getResources().getColor(R.color.text));
            pagerTabStrip.setTabIndicatorColor(getResources().getColor(R.color.orange));

            doUpdate = true;
//			new AdMobHelper(this);
        } catch (Exception e) {
            Hint.log(this, e);
            finish();
        }

		/*
         * ViewPagerIndicator indicator =
		 * (ViewPagerIndicator)findViewById(R.id.historyIndicator);
		 * indicator.init(0, mAdapter.getCount(), this); Resources res =
		 * getResources(); Drawable prev =
		 * res.getDrawable(R.drawable.indicator_prev_arrow); Drawable next =
		 * res.getDrawable(R.drawable.indicator_next_arrow);
		 * mPager.setOnPageChangeListener(indicator);
		 */
        // Set images for previous and next arrows.
        // indicator.setArrows(prev, next);
    }

    private void updateNoSessionsInfo() {
        String fields[] = {"count(*) as count"};
        int count = 0;
        Cursor cursor = null;
        try {
            cursor = sessionPersistance.querySessions(fields, null, null, "", null);
            if ((cursor != null) && (cursor.moveToFirst())) {
                count = cursor.getInt(cursor.getColumnIndex("count"));
            }
        } finally {
            if ((cursor != null) && !cursor.isClosed())
                cursor.close();
        }

        if (count == 0)
            linearLayoutNoSessions.setVisibility(View.VISIBLE);
        else
            linearLayoutNoSessions.setVisibility(View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        filterItem = menu.add(R.string.filter);
        filterItem.setOnMenuItemClickListener(this).setIcon(R.drawable.navigation_collapse)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        lockItem = menu.add(R.string.lock);
        lockItem.setOnMenuItemClickListener(this).setIcon(R.drawable.ic_action_unlock)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        addItem = menu.add(R.string.add);
        addItem.setOnMenuItemClickListener(this).setIcon(R.drawable.ic_action_plus)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        return true;
    }

    void updateHistory() {
        if (!doUpdate)
            return;

        updateNoSessionsInfo();

        double totalDistance = 0;
        long totalDuration = 0;
        int numSessions = 0;
        try {
            String fields[] = {"sum(duration) as duration, sum(distance) as distance, count(_id) as count"};
            String where = "";
            if (!TextUtils.isEmpty(typeFilter))
                where = typeFilter;
            if (!TextUtils.isEmpty(dateFilter)) {
                if (!TextUtils.isEmpty(where))
                    where = where + " and " + dateFilter;
                else
                    where = dateFilter;
            }

            Cursor cursor = null;
            try {
                cursor = sessionPersistance.querySessions(fields, where, null, "", null);
                if ((cursor != null) && cursor.moveToFirst()) {
                    totalDistance = cursor.getDouble(cursor.getColumnIndex("distance"));
                    totalDuration = cursor.getLong(cursor.getColumnIndex("duration"));
                    numSessions = cursor.getInt(cursor.getColumnIndex("count"));
                }
            } finally {
                if ((cursor != null) && !cursor.isClosed())
                    cursor.close();
            }

            historyGraphFragment.onUpdate(where);
            historyGraphFragment.onUpdateGui();

            historyListFragment.onUpdate(where);
            historyListFragment.onUpdateGui();

            // cursor = sessionPersistance.querySessions(fields, where, null,
            // "type ASC, start ASC",
            // null);
            // if (cursor != null) {
            // cursor.moveToFirst();
            // historyGraphFragment.onUpdate(cursor);
            // historyGraphFragment.onUpdateGui();
            // }

        } catch (Exception e) {
            ExceptionHandler.Handle("PaceTracker", e);
        }

        tvTotalDuration.setText(getString(R.string.historyTotalDuration) + ": "
                + DateUtils.secondsToHHMMSSString(totalDuration / 1000));
        tvTotalDistance.setText(getString(R.string.historyTotalDistance) + ": "
                + Distance.distanceToString(totalDistance, 2));
        tvNumSessions.setText(getString(R.string.historyNumSessions) + ": " + Integer.toString(numSessions));
        if (totalDuration == 0)
            tvMeanSpeed.setText(getString(R.string.historyMeanSpeed) + ": N/A");
        else
            tvMeanSpeed.setText(getString(R.string.historyMeanSpeed) + ": "
                    + Distance.speedToString(totalDistance / totalDuration * 3600.));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        historyListFragment.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onResume() {
        super.onResume();
        doUpdate = true;
        updateHistory();
        // Hint.show(this, "Long click on a session for more options");
    }

    @Override
    public void onPause() {
        sessionPersistance.closeDB();
        super.onPause();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == spinnerDateFilter) {
            GlobalSettings.getInstance().put("dateFilter", position);
            @SuppressWarnings("unchecked")
            Integer value = ((TextItemPair<Integer>) spinnerDateFilter.getItemAtPosition(position)).getItem();
            String sValue = value.toString();

            if (value != 0)
                sValue = new String("start >= '"
                        + Long.toString((new Date()).getTime() - 1000 * 60 * 60 * 24 * (long) value) + "'");
            else
                sValue = "";

            if (!dateFilter.equals(sValue)) {
                dateFilter = sValue;
                updateHistory();
            }
        } else if (parent == spinnerTypeFilter) {
            GlobalSettings.getInstance().put("typeFilter", position);
            String sessionType = "";
//			@SuppressWarnings("unchecked")
            try {
                sessionType = ((TextItemPair<String>) spinnerTypeFilter.getItemAtPosition(position)).getItem();
            } catch (Exception e) {
                Hint.log(this, e);
                position = 0;
                GlobalSettings.getInstance().put("typeFilter", position);
//				@SuppressWarnings("unchecked")
                sessionType = ((TextItemPair<String>) spinnerTypeFilter.getItemAtPosition(position)).getItem();
            }
            if (!TextUtils.isEmpty(sessionType))
                sessionType = new String("type = '" + sessionType + "'");
            if (!typeFilter.equals(sessionType)) {
                typeFilter = sessionType;
                updateHistory();
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        item.setChecked(!item.isChecked());
        mPager.lock(lockItem.isChecked());
        if (item == lockItem) {
            if (lockItem.isChecked())
                lockItem.setIcon(R.drawable.ic_action_lock);
            else
                lockItem.setIcon(R.drawable.ic_action_unlock);
        } else if (item == addItem) {
            manualIntent.putExtra("OpenViewSession", false);
            startActivity(manualIntent);
        } else if (item == filterItem) {
            if (filterItem.isChecked()) {
                filterLayout.setVisibility(View.GONE);
                filterItem.setIcon(R.drawable.navigation_expand);
            } else {
                filterLayout.setVisibility(View.VISIBLE);
                filterItem.setIcon(R.drawable.navigation_collapse);
            }
        }
        return true;
    }

}
