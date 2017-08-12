package de.badaix.pacetracker.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;

import java.util.Locale;

public class TextToSpeechHelper {
    public static int resultCodeCheckData = 0xCDA1A;
    public static int resultCodeInstallData = 0x1DA1A;
    public static int resultCodeSettings = 0x5DA1A;
    public final String TAG = "TTS";
    private final OnTtsListener listener;
    // private final String TAG = "ActivityWhosCalling";
    private TextToSpeech tts = null;
    public TextToSpeechHelper(final OnTtsListener listener) {
        this.listener = listener;
    }

    public void checkTtsData(final Activity activity) {
        Log.d(TAG, "checkTtsData");
        try {
            Intent checkIntent = new Intent();
            checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
            activity.startActivityForResult(checkIntent, resultCodeCheckData);
        } catch (Exception e) {
            Handler mainHandler = new Handler(activity.getMainLooper());
            Runnable myRunnable = new Runnable() {
                @Override
                public void run() {
                    onCheckResult(TextToSpeech.Engine.CHECK_VOICE_DATA_FAIL);
                }
            };
            mainHandler.post(myRunnable);
        }
    }

    public void onCheckResult(int resultCode) {
        Log.d(TAG, "onCheckResult: " + resultCode);
        switch (resultCode) {
            case TextToSpeech.Engine.CHECK_VOICE_DATA_PASS:
                Log.d(TAG, "onCheckResult: CHECK_VOICE_DATA_PASS");
                listener.initialized(tts, TtsResult.CHECK_OK);
                break;
            case TextToSpeech.Engine.CHECK_VOICE_DATA_FAIL:
            case TextToSpeech.Engine.CHECK_VOICE_DATA_BAD_DATA:
            case TextToSpeech.Engine.CHECK_VOICE_DATA_MISSING_DATA:
                Log.d(TAG, "onCheckResult: CHECK_VOICE_DATA_MISSING_DATA");
                if (android.os.Build.VERSION.SDK_INT >= 16) {
                    Log.d(TAG, "onCheckResult: SDK_INT >= 16 => OK");
                    listener.initialized(tts, TtsResult.CHECK_OK);
                    break;
                }
            case TextToSpeech.Engine.CHECK_VOICE_DATA_MISSING_VOLUME:
                Log.d(TAG, "onCheckResult: CHECK_VOICE_DATA_MISSING_VOLUME");
                listener.initialized(tts, TtsResult.LANG_MISSING_DATA);
        }
    }

    public void installTtsData(final Activity activity) {
        Intent installIntent = new Intent();
        installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
        activity.startActivityForResult(installIntent, resultCodeInstallData);
    }

    public void openTtsSettings(final Activity activity) {
        Intent intent = new Intent();
        intent.setAction("com.android.settings.TTS_SETTINGS");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivityForResult(intent, resultCodeSettings);
    }

    public void initTts(final Context context) {
        Log.d(TAG, "initTts");

        tts = new TextToSpeech(context, new OnInitListener() {
            @Override
            public void onInit(int status) {
                int avail = TextToSpeech.LANG_AVAILABLE;
                if (tts == null)
                    avail = TextToSpeech.LANG_MISSING_DATA;
                else {
                    try {
                        avail = tts.isLanguageAvailable(tts.getLanguage());
                    } catch (Exception e) {
                        avail = TextToSpeech.LANG_NOT_SUPPORTED;
                        e.printStackTrace();
                    }
                }

                Log.d(TAG, "onInit: " + avail);
                if (avail == TextToSpeech.LANG_MISSING_DATA) {
                    Log.d(TAG, "onInit: LANG_MISSING_DATA");
                    listener.initialized(tts, TtsResult.LANG_MISSING_DATA);
                    return;
                } else if (avail == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.d(TAG, "onInit: LANG_NOT_SUPPORTED");
                    if (tts.setLanguage(Locale.getDefault()) == TextToSpeech.LANG_AVAILABLE) {
                        Log.d(TAG, "onInit: setLanguage success");
                        listener.initialized(tts, TtsResult.SUCCESS);
                    } else {
                        Log.d(TAG, "onInit: setLanguage failed");
                        listener.initialized(tts, TtsResult.LANG_NOT_SUPPORTED);
                    }
                    return;
                }

                if (status == TextToSpeech.SUCCESS) {
                    Log.d(TAG, "onInit: success");
                    listener.initialized(tts, TtsResult.SUCCESS);
                } else {
                    Log.d(TAG, "onInit: error");
                    listener.initialized(tts, TtsResult.ERROR);
                }
            }
        });
    }

    public enum TtsResult {
        CHECK_OK, SUCCESS, ERROR, LANG_MISSING_DATA, LANG_NOT_SUPPORTED;
    }

    public interface OnTtsListener {
        public void initialized(TextToSpeech tts, TtsResult result);
    }

    // @Override
    // public void onInit(int status) {
    // if (status == TextToSpeech.SUCCESS) {
    // Locale defaultOrPassedIn = locale;
    // if (locale == null) {
    // defaultOrPassedIn = Locale.getDefault();
    // }
    // // check if language is available
    // switch (tts.isLanguageAvailable(defaultOrPassedIn)) {
    // case TextToSpeech.LANG_AVAILABLE:
    // case TextToSpeech.LANG_COUNTRY_AVAILABLE:
    // case TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE:
    // Log.d(TAG, "SUPPORTED");
    // tts.setLanguage(locale);
    // // pass the tts back to the main
    // // activity for use
    // listener.initialized(tts, TtsResult.SUCCESS);
    // break;
    // case TextToSpeech.LANG_MISSING_DATA:
    // Log.d(TAG, "MISSING_DATA");
    // Log.d(TAG, "require data...");
    // listener.initialized(tts, TtsResult.LANG_MISSING_DATA);
    // break;
    // case TextToSpeech.LANG_NOT_SUPPORTED:
    // Log.d(TAG, "NOT SUPPORTED");
    // listener.initialized(tts, TtsResult.LANG_NOT_SUPPORTED);
    // break;
    // }
    // } else
    // listener.initialized(tts, TtsResult.ERROR);
    // }
    // });

}
