package de.badaix.pacetracker.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.util.Date;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.preferences.AdvancedListPreference;
import de.badaix.pacetracker.preferences.ColoredPreferenceCategory;
import de.badaix.pacetracker.preferences.DecimalPreference;
import de.badaix.pacetracker.preferences.SeekBarPreference;
import de.badaix.pacetracker.preferences.SpeechPreference;
import de.badaix.pacetracker.preferences.ValuePreference;
import de.badaix.pacetracker.sensor.Sensor;
import de.badaix.pacetracker.sensor.Sensor.SensorType;
import de.badaix.pacetracker.sensor.SensorManager;
import de.badaix.pacetracker.session.SessionBackup;
import de.badaix.pacetracker.session.SessionBackup.ExportListener;
import de.badaix.pacetracker.session.SessionBackup.ImportListener;
import de.badaix.pacetracker.session.SessionFactory;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.settings.VoiceFeedbackSettings;
import de.badaix.pacetracker.util.Hint;
import de.badaix.pacetracker.util.TextToSpeechHelper;
import de.badaix.pacetracker.util.TextToSpeechHelper.OnTtsListener;
import de.badaix.pacetracker.util.TextToSpeechHelper.TtsResult;
import de.badaix.pacetracker.util.Weight;

public class ActivityGlobalSettings extends AppCompatPreferenceActivity implements OnPreferenceChangeListener,
        OnPreferenceClickListener, ExportListener, ImportListener, OnTtsListener {
    private final int BTACTIVITY = 123;
    // private final int REQUEST_ENABLE_BT = 124;
    private final int PULSE_ALARM = 125;
    private final int MY_DATA_CHECK_CODE = 666;
    private SpeechPreference prefVoiceFeedback;
    private DecimalPreference decimalPrefWeight;
    private SeekBarPreference volumePref;
    private AdvancedListPreference unitPref;
    private ValuePreference prefPulseAlarm;
    private Preference importPref;
    // private Preference dailymilePref;
    private Preference exportPref;
    private Preference pairingPref;
    private Preference installTtsPref;
    private PreferenceCategory prefCatVoiceFeedback;
    private PreferenceCategory prefCatTest;
    private AdvancedListPreference listPrefBtSensor;
    private AudioManager am;
    private TextToSpeech tts = null;
    private Sensor sensor;
    private SessionBackup sessionBackup;
    private ProgressDialog progressDialog;
    private TextToSpeechHelper ttsHelper;
    private GlobalSettings settings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = GlobalSettings.getInstance(this);

        Hint.log(this, "NewSessionActivity.onCreate");
        addPreferencesFromResource(R.layout.preferences_global_settings);
        setContentView(R.layout.activity_global_settings);

        prefCatVoiceFeedback = (PreferenceCategory) findPreference("prefCatVoiceFeedback");
        installTtsPref = null;

        prefVoiceFeedback = (SpeechPreference) findPreference("prefVoiceFeedback");
        prefVoiceFeedback.setOnPreferenceChangeListener(this);
        setSpeechSummary(GlobalSettings.getInstance(this).getVoiceFeedback());

        decimalPrefWeight = (DecimalPreference) findPreference("decimalPrefWeight");
        decimalPrefWeight.setValue((float) Weight.weightToDouble(settings.getUserWeight()));
        decimalPrefWeight.setOnPreferenceChangeListener(this);
        updateWeight();

        // dailymilePref = (Preference) findPreference("dailymilePref");
        updateDailymile();

        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        volumePref = (SeekBarPreference) findPreference("ttsVolumePref");
        volumePref.setMax(am.getStreamMaxVolume(TextToSpeech.Engine.DEFAULT_STREAM));
        volumePref.setProgress(settings.getVoiceVolume());
        volumePref.setValueText(Integer.toString(100 * volumePref.getProgress() / volumePref.getMax()) + "%");
        volumePref.setOnPreferenceChangeListener(this);

        unitPref = (AdvancedListPreference) findPreference("listPrefUnits");
        unitPref.setValue(settings.getDistSystem().toString());
        unitPref.setValueText(unitPref.getEntry());
        unitPref.setOnPreferenceChangeListener(this);

        pairingPref = (Preference) findPreference("prefBtPairing");
        pairingPref.setOnPreferenceClickListener(this);

        sensor = settings.getSensor();
        listPrefBtSensor = (AdvancedListPreference) findPreference("listPrefBtSensor");
        listPrefBtSensor.setOnPreferenceChangeListener(this);
        listPrefBtSensor.setOnPreferenceClickListener(this);
        String sensorName = sensor.getName();
        if (TextUtils.isEmpty(sensorName))
            sensorName = getString(R.string.noDevice);
        listPrefBtSensor.setValueText(sensorName);
        updateSensorList();

        prefPulseAlarm = (ValuePreference) findPreference("prefPulseAlarm");
        prefPulseAlarm.setOnPreferenceClickListener(this);
        prefPulseAlarm.setValueText(alarmUriToString());

        importPref = (Preference) findPreference("prefImport");
        importPref.setOnPreferenceClickListener(this);

        exportPref = (Preference) findPreference("prefExport");
        exportPref.setOnPreferenceClickListener(this);

        getSupportActionBar().setTitle(R.string.globalSettings);

        if (GlobalSettings.getInstance(this).isDeveloper()) {
            prefCatTest = new ColoredPreferenceCategory(this);
            prefCatTest.setTitle("Developer");
            CheckBoxPreference cbpDebug = new CheckBoxPreference(this);
            cbpDebug.setKey("prefDebug");
            cbpDebug.setTitle("Debug");
            cbpDebug.setPersistent(true);
            CheckBoxPreference cbpPro = new CheckBoxPreference(this);
            cbpPro.setKey("prefPro");
            cbpPro.setTitle("Pro");
            cbpPro.setPersistent(true);
            this.getPreferenceScreen().addPreference(prefCatTest);
            prefCatTest.addPreference(cbpDebug);
            prefCatTest.addPreference(cbpPro);
        }
        // View v = new View(this);
        // v.setMinimumHeight(10);
        // getListView().addFooterView(v);

        Intent checkIntent = new Intent();
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, MY_DATA_CHECK_CODE);
    }

    private void setSpeechSummary(VoiceFeedbackSettings settings) {
        String summary = "";
        if (settings.isDistanceEnabled())
            summary += getString(R.string.distance);
        if (settings.isDurationEnabled()) {
            if (summary.length() > 0)
                summary += ", ";
            summary += getString(R.string.time);
        }
        prefVoiceFeedback.setValueText(summary);
    }

    private void updateDailymile() {
        // User user = GlobalSettings.getInstance(this).getMe();
        // if (user == null) {
        // dailymilePref.setSummary(R.string.login);
        // return;
        // }
        //
        // dailymilePref.setSummary(user.getDisplayName());
        // ImageView iv = new ImageView(this);
        // UrlImageViewHelper.getInstance().setUrlDrawable(iv,
        // user.getPhotoUrl(), R.drawable.user_mini, null);
        // dailymilePref.setIcon(iv.getDrawable());
    }

    private void updateWeight() {
        decimalPrefWeight.setUnit(GlobalSettings.getInstance(this).getWeightUnit().toShortString());
        decimalPrefWeight.setValue((float) Weight.weightToDouble(settings.getUserWeight()));
        decimalPrefWeight.setValueText(Weight.weightToString(settings.getUserWeight(), 1) + " "
                + GlobalSettings.getInstance(this).getWeightUnit().toShortString());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == prefVoiceFeedback) {
            setSpeechSummary((VoiceFeedbackSettings) newValue);
        } else if (preference == unitPref) {
            unitPref.setValueText(((AdvancedListPreference) preference).getEntry((String) newValue));
            GlobalSettings.getInstance(this).setDistSystem((String) newValue);
            updateWeight();
        } else if (preference == decimalPrefWeight) {
            GlobalSettings.getInstance(this).setUserWeight(Float.parseFloat((String) newValue));
            updateWeight();
        } else if (preference == volumePref)
            volumePref.setValueText(Integer.toString(100 * volumePref.getProgress() / volumePref.getMax()) + "%");
        else if (preference == listPrefBtSensor) {
            String entryValue = (String) newValue;
            String entry = listPrefBtSensor.getEntry(entryValue).toString();
            listPrefBtSensor.setValueText(entry);
            if (TextUtils.isEmpty(entryValue))
                settings.setSensor(new Sensor(SensorType.NONE, entry));
            else
                settings.setSensor(new Sensor(SensorType.POLAR, entryValue));
        }
        return true;
    }

    private void updateSensorList() {
        BluetoothDevice[] devices = SensorManager.getBluetoothSensors();
        if (devices == null)
            devices = new BluetoothDevice[0];

        Sensor configuredSensor = settings.getSensor();
        boolean configuredInList = TextUtils.isEmpty(configuredSensor.getName())
                || (configuredSensor.getType() == SensorType.NONE);
        int plusDevices = 1;
        for (int i = 0; i < devices.length; ++i)
            configuredInList = (configuredInList || devices[i].getName().equals(configuredSensor));

        if (!configuredInList)
            plusDevices++;

        CharSequence[] entries = new CharSequence[devices.length + plusDevices];
        CharSequence[] entryValues = new CharSequence[devices.length + plusDevices];
        entries[0] = getResources().getText(R.string.noDevice);
        entryValues[0] = "";
        if (!configuredInList) {
            entries[1] = configuredSensor.getName();
            entryValues[1] = configuredSensor.getName();
        }
        for (int i = 0; i < devices.length; ++i) {
            entries[i + plusDevices] = devices[i].getName();
            entryValues[i + plusDevices] = devices[i].getName();
        }
        listPrefBtSensor.setEntries(entries);
        listPrefBtSensor.setEntryValues(entryValues);
    }

    private String alarmUriToString() {
        Uri uri = settings.getPulseAlarmUri();
        if ((uri == null) || uri.equals(Uri.EMPTY))
            return getString(R.string.globalPrefSilent);

        Ringtone ringtone = RingtoneManager.getRingtone(this, uri);
        return ringtone.getTitle(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (tts == null) {
            ttsHelper = new TextToSpeechHelper(this);
            // ttsHelper.initTts(this);
            if (!settings.getBoolean("TTS", false))
                ttsHelper.checkTtsData(this);
            else
                ttsHelper.initTts(this);
        }
    }

    @Override
    protected void onStop() {
        // new BackupManager(this).dataChanged();
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Hint.log(this, "Request: " + requestCode + ", Result: " + resultCode);

        if (requestCode == TextToSpeechHelper.resultCodeCheckData) {
            Log.d("TTS", "requestCode == TextToSpeechHelper.resultCodeCheckData, Result: " + resultCode);
            ttsHelper.onCheckResult(resultCode);
        } else if (requestCode == TextToSpeechHelper.resultCodeInstallData) {
            Log.d("TTS", "requestCode == TextToSpeechHelper.resultCodeInstallData, Result: " + resultCode);
            ttsHelper.checkTtsData(this);
        } else if (requestCode == BTACTIVITY) {
            updateSensorList();
        } else if (requestCode == PULSE_ALARM) {
            if (resultCode != Activity.RESULT_OK)
                return;
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);

            settings.setPulseAlarmUri(uri);
            prefPulseAlarm.setValueText(alarmUriToString());
        }
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
    public boolean onPreferenceClick(Preference preference) {
        if ((preference == importPref) || (preference == exportPref)) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        123);
                return true;
            }
        }

        if (preference == importPref) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            // Add the buttons
            builder.setTitle(getString(R.string.importSessions));
            builder.setMessage(getString(R.string.importMessage))
                    .setPositiveButton(R.string.doImport, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            progressDialog = new ProgressDialog(ActivityGlobalSettings.this);
                            progressDialog.setMax(1);
                            progressDialog.setMessage(getString(R.string.importing));
                            progressDialog.setTitle(getString(R.string.importSessions));
                            progressDialog.setCancelable(false);
                            progressDialog.setCanceledOnTouchOutside(false);
                            progressDialog.setIndeterminate(true);
                            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                            progressDialog.setButton(DialogInterface.BUTTON_POSITIVE, getText(android.R.string.cancel),
                                    new DialogInterface.OnClickListener() {

                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            sessionBackup.abortImport();
                                        }
                                    });
                            progressDialog.show();
                            sessionBackup = new SessionBackup(getApplicationContext());
                            sessionBackup.importSessions(ActivityGlobalSettings.this);
                        }
                    }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User cancelled the dialog
                }
            });
            builder.create().show();
            return true;
        } else if (preference == exportPref) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            // Add the buttons
            builder.setTitle(getString(R.string.exportSessions));
            builder.setMessage(getString(R.string.exportMessage))
                    .setPositiveButton(R.string.doExport, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            progressDialog = new ProgressDialog(ActivityGlobalSettings.this);
                            progressDialog.setMax(1);
                            progressDialog.setMessage(getString(R.string.exporting));
                            progressDialog.setTitle(getString(R.string.exportSessions));
                            progressDialog.setCancelable(false);
                            progressDialog.setCanceledOnTouchOutside(false);
                            progressDialog.setIndeterminate(true);
                            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                            progressDialog.setButton(DialogInterface.BUTTON_POSITIVE, getText(android.R.string.cancel),
                                    new DialogInterface.OnClickListener() {

                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            sessionBackup.abortExport();
                                        }
                                    });
                            progressDialog.show();
                            sessionBackup = new SessionBackup(getApplicationContext());
                            sessionBackup.exportSessions(ActivityGlobalSettings.this);
                        }
                    }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User cancelled the dialog
                }
            });
            builder.create().show();
            return true;
        } else if (preference == pairingPref) {
            Intent settingsIntent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivityForResult(settingsIntent, BTACTIVITY);
            return true;
        } else if (preference == listPrefBtSensor) {
            // BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            // if ((adapter != null) && (!adapter.isEnabled())) {
            // Intent enableBtIntent = new
            // Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            // startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            // }
            return true;
        } else if (preference == prefPulseAlarm) {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Tone");
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) settings.getPulseAlarmUri());
            this.startActivityForResult(intent, PULSE_ALARM);
        }

        return false;
    }

    @Override
    public void onExport(int num, int of, Date date, final String type, File filename) {
        String typeName = SessionFactory.getInstance().getSessionNameFromType(type);
        Hint.log(this, num + "/" + of + ", init: " + date.toLocaleString() + ", type: " + typeName + ", file: "
                + filename.getName());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressDialog.setMessage(getString(R.string.exporting) + " " + type);
            }
        });

        if (progressDialog.isIndeterminate())
            progressDialog.setIndeterminate(false);
        progressDialog.setMax(of);
        progressDialog.setProgress(num);
    }

    @Override
    public void onExportFinished(boolean aborted, int exported, int failed, Exception e) {
        progressDialog.dismiss();
        if (!aborted) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            String message = getString(R.string.exported) + ": " + exported + "\n" + getString(R.string.failed) + ": "
                    + failed;
            if (e != null)
                message = message + getString(R.string.error) + ": " + e.getMessage();
            builder.setMessage(message).setTitle(R.string.sessionExport)
                    .setPositiveButton(getResources().getString(android.R.string.ok), null);
            builder.create().show();
        }
    }

    @Override
    public void onImport(int num, int of, final String type) {
        if (progressDialog.isIndeterminate())
            progressDialog.setIndeterminate(false);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressDialog.setMessage(getString(R.string.importing) + " " + type);
            }
        });
        progressDialog.setMax(of);
        progressDialog.setProgress(num);
    }

    @Override
    public void onImportFinished(boolean aborted, int imported, int failed, Exception e) {
        progressDialog.dismiss();
        if (!aborted) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            String message = getString(R.string.imported) + ": " + imported + "\n" + getString(R.string.failed) + ": "
                    + failed;
            if (e != null)
                message = message + getString(R.string.error) + ": " + e.getMessage();
            builder.setMessage(message).setTitle(R.string.sessionImport)
                    .setPositiveButton(getResources().getString(android.R.string.ok), null);
            builder.create().show();
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void initialized(TextToSpeech tts, TtsResult result) {
        Log.d("TTS", "initialized: " + result.toString());
        if ((result == TtsResult.LANG_MISSING_DATA) || (result == TtsResult.LANG_NOT_SUPPORTED)) {
            if (installTtsPref == null) {
                Log.d("TTS", "installTtsPref == null");
                installTtsPref = new Preference(this);
                installTtsPref.setTitle("Install TTS");
                installTtsPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        ttsHelper.installTtsData(ActivityGlobalSettings.this);
                        // ttsHelper.initTts(ActivityWhosCalling.this);
                        return true;
                    }
                });
                prefCatVoiceFeedback.addPreference(installTtsPref);

                AlertDialog.Builder ttsInstallDialogBuilder;
                ttsInstallDialogBuilder = new AlertDialog.Builder(this);
                String ttsErrorMsg = this.getString(R.string.ttsErrorMessage);
                ttsErrorMsg = ttsErrorMsg + " Download now?";
                ttsInstallDialogBuilder.setMessage(ttsErrorMsg);
                ttsInstallDialogBuilder.setCancelable(false);

                ttsInstallDialogBuilder.setPositiveButton(android.R.string.yes, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ttsHelper.installTtsData(ActivityGlobalSettings.this);
                    }
                });
                ttsInstallDialogBuilder.setNegativeButton(android.R.string.cancel, null);
                ttsInstallDialogBuilder.show();
            }
        } else if (result == TtsResult.CHECK_OK) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            ttsHelper.initTts(this);
        } else if (result == TtsResult.SUCCESS) {
            this.tts = tts;
            if (installTtsPref != null)
                prefCatVoiceFeedback.removePreference(installTtsPref);
            settings.put("TTS", true);
        }
    }
}
