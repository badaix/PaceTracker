package de.badaix.pacetracker.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Vector;

import de.badaix.pacetracker.PositionListener;
import de.badaix.pacetracker.R;
import de.badaix.pacetracker.goal.Goal;
import de.badaix.pacetracker.goal.GoalDistance;
import de.badaix.pacetracker.goal.GoalDuration;
import de.badaix.pacetracker.goal.GoalFactory;
import de.badaix.pacetracker.goal.GoalRoute;
import de.badaix.pacetracker.goal.GoalStandard;
import de.badaix.pacetracker.posprovider.FakeGpsPositionProvider;
import de.badaix.pacetracker.posprovider.FakePositionProvider;
import de.badaix.pacetracker.posprovider.GpsPositionProvider;
import de.badaix.pacetracker.posprovider.PositionProvider;
import de.badaix.pacetracker.preferences.AdvancedListPreference;
import de.badaix.pacetracker.preferences.ColoredPreferenceCategory;
import de.badaix.pacetracker.preferences.IconListPreference;
import de.badaix.pacetracker.preferences.PulsePreference;
import de.badaix.pacetracker.sensor.Sensor.SensorType;
import de.badaix.pacetracker.sensor.SensorData;
import de.badaix.pacetracker.sensor.SensorManager;
import de.badaix.pacetracker.sensor.SensorProvider;
import de.badaix.pacetracker.sensor.SensorProvider.SensorListener;
import de.badaix.pacetracker.sensor.SensorProvider.SensorState;
import de.badaix.pacetracker.session.SessionFactory;
import de.badaix.pacetracker.session.SessionService;
import de.badaix.pacetracker.session.SessionService.LocalBinder;
import de.badaix.pacetracker.session.SessionType;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.settings.SessionSettings;
import de.badaix.pacetracker.util.Hint;
import de.badaix.pacetracker.util.LocationUtils;
import de.badaix.pacetracker.weather.Weather;
import de.badaix.pacetracker.weather.WeatherProvider;
import de.badaix.pacetracker.weather.WeatherProvider.WeatherListener;
import de.badaix.pacetracker.widgets.GpsIndicator;
import de.badaix.pacetracker.widgets.ImageViewHeart;

public class ActivityNewSession extends AppCompatPreferenceActivity implements PositionListener,
        OnPreferenceChangeListener, SensorListener, OnClickListener, WeatherListener {
    private static final int GPS_ENABLE = 0;
    private static final int MENU_TEST_GPS = Menu.FIRST + 3;
    private static final int MENU_TEST = Menu.FIRST + 4;
    private Button btnStart;
    private CheckBoxPreference cbPrefAutoStart;
    private CheckBoxPreference cbPrefAutoPause;
    private EditTextPreference textPrefDescription;
    private IconListPreference listPrefType;
    private PulsePreference prefSensor;
    private Vector<Preference> prefGoal;
    private ColoredPreferenceCategory prefCatGoal;
    private AdvancedListPreference listPrefGoal;
    private PreferenceCategory newSessionPrefCat;
    private TextView tvHrs;
    private ImageViewHeart ivHrs;
    private LinearLayout llHrs;
    private LocationManager lm;
    // private Preference customPrefGps;
    private GpsIndicator gpsIndicator;
    private int iSatCount = 0;
    private int iFixCount = 0;
    private boolean bFix = false;
    private MenuItem ttsItem;
    private PositionProvider gpsProvider = null;
    private String description = "";
    private boolean gpsEnabled;
    private Goal selectedGoal = null;
    private SensorProvider provider;
    private SessionSettings sessionSettings;
    // private PulseSettings pulseSettings;
    private WeatherProvider weatherProvider;
    private boolean firstFix;
    private GlobalSettings settings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (GlobalSettings.getInstance().getContext() == null)
            this.finish();
        try {
            super.onCreate(savedInstanceState);
            settings = GlobalSettings.getInstance(this);

            addPreferencesFromResource(R.layout.preferences_new_session);
            setContentView(R.layout.activity_new_session);

//            getSupportActionBar().setLogo(R.drawable.dashboard_button_new_session);
//            getSupportActionBar().setDisplayUseLogoEnabled(true);
//            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.mainMenuNewSession);

            btnStart = (Button) findViewById(R.id.buttonSessionStart);
            btnStart.setOnClickListener(this);
            gpsEnabled = false;
            updateButtonState();

            gpsIndicator = (GpsIndicator) findViewById(R.id.linearLayoutGpsSignal);
            tvHrs = (TextView) findViewById(R.id.tvHrs);
            ivHrs = (ImageViewHeart) findViewById(R.id.imageViewHeart);
            llHrs = (LinearLayout) findViewById(R.id.llHrs);

            // customPrefGps = (Preference) findPreference("customPrefGps");
            sessionSettings = new SessionSettings(true, null);
            sessionSettings.setSensor(settings.getSensor());
            newSessionPrefCat = (PreferenceCategory) findPreference("newSessionPrefCat");
            cbPrefAutoStart = (CheckBoxPreference) findPreference("cbPrefAutoStart");
            cbPrefAutoStart.setChecked(sessionSettings.isAutoStart());
            cbPrefAutoStart.setOnPreferenceChangeListener(this);

            cbPrefAutoPause = (CheckBoxPreference) findPreference("cbPrefAutoPause");
            cbPrefAutoPause.setChecked(sessionSettings.isAutoPause());
            cbPrefAutoPause.setOnPreferenceChangeListener(this);

            prefSensor = (PulsePreference) findPreference("prefSensor");
            prefSensor.setOnPreferenceChangeListener(this);
            prefSensor.setSettings(sessionSettings.getPulseSettings());
            if (!settings.isPro()) {
                prefSensor.getSettings().setMaxAlarmEnabled(false);
                prefSensor.getSettings().setMinAlarmEnabled(false);
            }

            if (settings.getSensor().getType() == SensorType.NONE) {
                newSessionPrefCat.removePreference(prefSensor);
                enableSensor(false);
            } else {
                enableSensor(prefSensor.getSettings().isSensorEnabled());
            }
            Hint.log(this, "Sensor enabled: " + prefSensor.getSettings().isSensorEnabled());

            textPrefDescription = (EditTextPreference) findPreference("textPrefDescription");
            textPrefDescription.setOnPreferenceChangeListener(this);

            prefCatGoal = (ColoredPreferenceCategory) findPreference("prefCatGoal");
            listPrefGoal = (AdvancedListPreference) findPreference("listPrefGoal");
            listPrefGoal.setOnPreferenceChangeListener(this);
            String[] goalName = new String[4];
            String[] goalType = new String[4];
            goalName[0] = GoalFactory.getOfflineGoal(GoalStandard.class, this.getApplication()).getName();
            goalType[0] = GoalStandard.class.getSimpleName();
            goalName[1] = GoalFactory.getOfflineGoal(GoalDistance.class, this.getApplication()).getName();
            goalType[1] = GoalDistance.class.getSimpleName();
            goalName[2] = GoalFactory.getOfflineGoal(GoalDuration.class, this.getApplication()).getName();
            goalType[2] = GoalDuration.class.getSimpleName();
            goalName[3] = GoalFactory.getOfflineGoal(GoalRoute.class, this.getApplication()).getName();
            goalType[3] = GoalRoute.class.getSimpleName();
            selectedGoal = sessionSettings.getGoal();

            listPrefGoal.setEntries(goalName);
            listPrefGoal.setEntryValues(goalType);
            listPrefGoal.setDefaultValue(selectedGoal.getType());

            prefGoal = new Vector<Preference>();
            // prefGoal = (Preference) findPreference("prefGoal");
            // prefGoal.setOnPreferenceClickListener(this);
            listPrefGoal.setValue(sessionSettings.getGoal().getType());
            onPreferenceChange(listPrefGoal, listPrefGoal.getValue());

            listPrefType = (IconListPreference) findPreference("listPrefType");
            Vector<SessionType> sessionTypeName = SessionFactory.getInstance().getSessionTypeName();

            if (!settings.isDebug()) {
                for (SessionType sessionType : sessionTypeName) {
                    if ("TestSession".equals(sessionType.getType())) {
                        sessionTypeName.remove(sessionType);
                        break;
                    }
                }
            }

            String[] sessionType = new String[sessionTypeName.size()];
            String[] sessionName = new String[sessionTypeName.size()];
            int[] sessionDrawable = new int[sessionTypeName.size()];

            int lastSessionIdx = 0;
            for (int i = 0; i < sessionTypeName.size(); ++i) {
                sessionType[i] = sessionTypeName.get(i).getType();
                sessionName[i] = sessionTypeName.get(i).getName(this);
                sessionDrawable[i] = sessionTypeName.get(i).getDrawable();
                if (sessionSettings.getSessionType().equals(sessionType[i]))
                    lastSessionIdx = i;
            }
            listPrefType.setEntries(sessionName);
            listPrefType.setEntryValues(sessionType);
            listPrefType.setEntryDrawables(sessionDrawable);
            listPrefType.setBackgroundDrawable(R.drawable.history_item_image_background);
            listPrefType.setValueIndex(lastSessionIdx);
            listPrefType.setTitle(sessionName[lastSessionIdx]);
            listPrefType.setOnPreferenceChangeListener(this);

            lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            weatherProvider = new WeatherProvider(this);
            if (LocationUtils.getLastKnownLocation() != null)
                weatherProvider.getWeather(LocationUtils.getLastKnownLocation());
            firstFix = true;
        } catch (Exception e) {
            Hint.log(this, e);
            finish();
        }
    }

    public void updateButtonState() {
        btnStart.setEnabled(bFix || !gpsEnabled);
        if (!gpsEnabled)
            btnStart.setText(getResources().getString(R.string.newSessionButtonGps));
        else {
            if (bFix)
                btnStart.setText(getResources().getString(R.string.newSessionButtonStart));
            else
                btnStart.setText(getResources().getString(R.string.newSessionButtonWaitFix));
        }
    }

    public void UpdateGui() {
        updateButtonState();
        gpsIndicator.onGpsStatusChanged(true, bFix, iFixCount, iSatCount);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 123:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //granted
                } else {
                    //not granted
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        onActivityResult(GPS_ENABLE, 0, null);

        if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) ||
                (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    123);
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sessionSettings.storeSettings();
    }

    @Override
    protected void onStart() {
        super.onStart();
        gpsProvider = GpsPositionProvider.getInstance(this, false);
        // PositionProviderFactory.getPosProvider(this,
        // GpsPositionProvider.class);
    }

    @Override
    protected void onStop() {
        // gpsProvider.stop(this);
        // gpsProvider = null;
        super.onStop();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // If the back Key was pressed, then finish the program.
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            Hint.log(this, "KEYCODE_BACK");
            enableSensor(false);
            if (gpsProvider != null)
                gpsProvider.stop(this);
        }

        // else return the normal function of whatever key was pressed
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // R.drawable.ic_action_unmute,
        // R.drawable.ic_action_mute
        ttsItem = menu.add("TTS");
        ttsItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        ttsItem.setChecked(settings.getBoolean("TtsActive", true));
        sessionSettings.setVoiceFeedback(ttsItem.isChecked());
        if (ttsItem.isChecked())
            ttsItem.setIcon(R.drawable.ic_action_unmute);
        else
            ttsItem.setIcon(R.drawable.ic_action_mute);

        ttsItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                item.setChecked(!item.isChecked());
                settings.put("TtsActive", item.isChecked());
                sessionSettings.setVoiceFeedback(item.isChecked());
                if (item.isChecked())
                    ttsItem.setIcon(R.drawable.ic_action_unmute);
                else
                    ttsItem.setIcon(R.drawable.ic_action_mute);
                return true;
            }
        });

        if (settings.isDebug()) {
            MenuItem mnuTestGps = menu.add(0, MENU_TEST_GPS, 1, "Test GPS");
            mnuTestGps.setIcon(android.R.drawable.ic_menu_help);
            MenuItem mnuTest = menu.add(0, MENU_TEST, 2, "Test");
            mnuTest.setIcon(android.R.drawable.ic_menu_help);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_TEST_GPS:
                sessionSettings.setPositionProvider(new FakeGpsPositionProvider(this, false));
                startSession();
                break;

            case MENU_TEST:
                sessionSettings.setPositionProvider(new FakePositionProvider(this, false));
                startSession();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void startSession() {
        Intent intent = new Intent(this, SessionService.class);
        // intent.putExtra("drawable",
        // SessionFactory.getInstance().getSessionLightDrawableFromType(sessionSettings.getSessionType()));
        bindService(intent, new ServiceConnection() {

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                LocalBinder binder = (LocalBinder) service;
                SessionService sessionService = binder.getService();
                sessionService.startSession(sessionSettings);
                Intent intent = new Intent(ActivityNewSession.this, ActivitySession.class);
                startActivity(intent);
                unbindService(this);
            }
        }, Context.BIND_AUTO_CREATE);
        startService(intent);
    }

    @Override
    public void onClick(View v) {
        if (v == btnStart) {
            if (!gpsEnabled) {
                Intent GPSIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(GPSIntent, GPS_ENABLE);
            } else {
                if ((selectedGoal != null) && !selectedGoal.isConfigured()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(getResources().getString(R.string.configureGoal))
                            .setTitle(getResources().getString(android.R.string.dialog_alert_title))
                            .setCancelable(false)
                            .setNeutralButton(getResources().getString(android.R.string.ok),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    }).create().show();
                } else {
                    sessionSettings.setPositionProvider(gpsProvider);
                    btnStart.setEnabled(false);

                    startSession();
                }
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GPS_ENABLE) {
            // If GPS_ENABLE, then re-check to make sure GPS was turned on
            if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                gpsEnabled = false;
                updateButtonState();
            } else {
                gpsEnabled = true;
                updateButtonState();
                Hint.log(this, "requestLocationUpdates");
                try {
                    gpsProvider.start(this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else if (requestCode == Goal.GOAL_REQUEST_CODE) {
            selectedGoal.setData(resultCode, data);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (firstFix) {
            weatherProvider.getWeather(location);
            firstFix = false;
        }
        UpdateGui();
    }

    @Override
    public void onGpsStatusChanged(boolean active, boolean hasFix, int fixCount, int satCount) {
        bFix = hasFix;
        iSatCount = satCount;
        iFixCount = fixCount;
        UpdateGui();
    }

    private void updateSensorSummary() {
        String summary = "";
        if (prefSensor.getSettings().isSensorEnabled()) {
            if (prefSensor.getSettings().isMinAlarmEnabled())
                summary = getString(R.string.min_hr) + ": " + prefSensor.getSettings().getMinPulse();
            if (prefSensor.getSettings().isMaxAlarmEnabled()) {
                if (!TextUtils.isEmpty(summary))
                    summary = summary + ", ";
                summary = summary + getString(R.string.max_hr) + ": " + prefSensor.getSettings().getMaxPulse();
            }
        } else
            summary = getString(R.string.dont_use_sensor);

        prefSensor.setValueText(summary);
    }

    private void enableSensor(boolean enabled) {
        if (enabled) {
            // tvHrs.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
            llHrs.setVisibility(View.VISIBLE);
            provider = SensorManager.getSensorProvider(settings.getSensor());
            try {
                provider.start(this);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            // tvHrs.setWidth(0);
            // SessionSettings.getInstance().setSensorSettings(new
            // PulseSettings());
            SensorManager.stopSensor();
            llHrs.setVisibility(View.GONE);
        }
        updateSensorSummary();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == listPrefType) {
            listPrefType.setTitle(SessionFactory.getInstance().getSessionNameFromType((String) newValue));
            sessionSettings.setSessionType((String) newValue);
            return true;
        } else if (preference == textPrefDescription) {
            description = (String) newValue;
            if (!TextUtils.isEmpty(description))
                textPrefDescription.setSummary(description);
            else
                textPrefDescription.setSummary(getResources().getString(R.string.newSessionDescriptionSummary));
            textPrefDescription.setText(description);
            sessionSettings.setDescription(description);
        } else if (preference == listPrefGoal) {
            String goalType = (String) newValue;
            listPrefGoal.setTitle(listPrefGoal.getEntry(goalType));
            for (Preference pref : prefGoal)
                prefCatGoal.removePreference(pref);
            selectedGoal = GoalFactory.getGoal(goalType, this);
            if (selectedGoal != null) {
                prefGoal = selectedGoal.getPreferences();
                for (Preference pref : prefGoal)
                    prefCatGoal.addPreference(pref);
            }
            if (selectedGoal != null)
                selectedGoal.initPreferences();

            sessionSettings.setGoal(selectedGoal);
            return true;
        } else if (preference == prefSensor) {
            sessionSettings.setPulseSettings(prefSensor.getSettings());
            enableSensor((Boolean) newValue);
            return true;
        } else if (preference == cbPrefAutoStart) {
            sessionSettings.setAutoStart((Boolean) newValue);
            return true;
        } else if (preference == cbPrefAutoPause) {
            sessionSettings.setAutoPause((Boolean) newValue);
            return true;
        }
        return false;
    }

    @Override
    public void onSensorData(SensorProvider provider, SensorData sensorData) {
        if (sensorData.getHeartRate() != 0) {
            tvHrs.setText(Integer.toString(sensorData.getHeartRate()));
            ivHrs.setConnected();
        } else {
            tvHrs.setText("--");
            ivHrs.setConnecting();
        }
    }

    @Override
    public void onSensorStateChanged(SensorProvider provider, boolean active, SensorState sensorState) {
        if (sensorState.isConnected())
            ivHrs.setConnected();
        else if (sensorState.isDisconnected())
            ivHrs.setDisconnected();

        Hint.log(this, sensorState.toString());
    }

    @Override
    public void onWeatherData(Weather weather, Exception exception) {
        if (weather != null) {
            sessionSettings.setWeather(weather);
        }
    }

}
