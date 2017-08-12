package de.badaix.pacetracker.util;

import android.content.Context;
import android.media.AudioManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.text.TextUtils;
import android.util.Log;

import java.util.HashMap;

import de.badaix.pacetracker.settings.GlobalSettings;

public class TTS implements OnInitListener, OnUtteranceCompletedListener {
    private static final String TAG = TTS.class.getSimpleName();
    private static TTS instance = null;
    private AudioManager am;
    private TextToSpeech tts = null;
    private int ttsVolume;
    private int confTtsVolume;
    private boolean isInitialized = false;
    private String queuedTts = "";
    private int tts_stream;

    private TTS() {
    }

    public static synchronized TTS getInstance() {
        if (instance == null) {
            instance = new TTS();
            // instance.init();
        }
        return instance;
    }

    public void init(Context context) {
        if (tts == null) {
            tts_stream = TextToSpeech.Engine.DEFAULT_STREAM; //AudioManager.STREAM_MUSIC; // TextToSpeech.Engine.DEFAULT_STREAM
            am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            ttsVolume = am.getStreamVolume(tts_stream);
            confTtsVolume = GlobalSettings.getInstance(context).getVoiceVolume();
            tts = new TextToSpeech(context, this);
            isInitialized = false;
            queuedTts = "";
        }
    }

    public synchronized void shutdown() {
        Log.d(TAG, "shutdown");
        isInitialized = false;
        if (tts == null)
            return;
        tts.stop();
        tts.shutdown();
        tts = null;
    }

    public void speak(String message, boolean flush) {
        // init();
        if (!isInitialized) {
            if (flush)
                queuedTts = message;
            else
                queuedTts = queuedTts + ", " + message;
            return;
        }

        Log.d(TAG, "Speak: " + message);
        // if (active)
        {
            am.setStreamVolume(tts_stream, confTtsVolume, 0);//AudioManager.FLAG_ALLOW_RINGER_MODES);
            HashMap<String, String> hm = new HashMap<String, String>();

            int queueMode = TextToSpeech.QUEUE_ADD;
            if (flush)
                queueMode = TextToSpeech.QUEUE_FLUSH;

            int result = am.requestAudioFocus(null, tts_stream,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
            if (result == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
                Log.w(TAG, "Failed to request audio focus.");
            }

            hm.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, message);
            if (tts.speak(message, queueMode, hm) == TextToSpeech.ERROR) {
                hm.clear();
                am.setStreamVolume(tts_stream, ttsVolume, 0);//AudioManager.FLAG_ALLOW_RINGER_MODES);
            } else {
                hm.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "silence after " + message);
                tts.speak(" ", TextToSpeech.QUEUE_ADD, null);
                tts.playSilence(200, TextToSpeech.QUEUE_ADD, hm);
            }
        }
    }

    public void interrupt() {
        if (tts == null)
            return;

        try {
            tts.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onInit(int status) {
        isInitialized = (status == TextToSpeech.SUCCESS);
        if (!isInitialized) {
            Log.w(TAG, "Failed to init TTS");
            tts = null;
        } else {
            tts.setOnUtteranceCompletedListener(this);
            if (!TextUtils.isEmpty(queuedTts))
                speak(queuedTts, false);
            queuedTts = "";
        }
    }

    @Override
    public void onUtteranceCompleted(String utteranceId) {
        am.setStreamVolume(tts_stream, ttsVolume, 0);//AudioManager.FLAG_ALLOW_RINGER_MODES);
        Log.d(TAG, "TTS completed: " + utteranceId);
    }

}
