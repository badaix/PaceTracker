package de.badaix.pacetracker.util;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import de.badaix.pacetracker.settings.GlobalSettings;

public class Hint {
    final static public void show(final Context context, final String hint) {
        if (GlobalSettings.getInstance(context).showHints()) {

            Handler handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, hint, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    final static public void show(final Context context, Exception e) {
        e.printStackTrace();
        if (GlobalSettings.getInstance(context).showHints()) {
            String message = "Error (" + e.getClass().getSimpleName() + ")";
            if (e.getMessage() != null)
                message = message + ":\n" + e.getMessage();
            show(context, message);
        }
    }

    final synchronized static public void log(Object who, String msg) {
        if (msg == null)
            msg = "";
        Log.d(who.getClass().getSimpleName(), msg);
    }

    final synchronized static public void log(String who, String msg) {
        if (msg == null)
            msg = "";
        Log.d(who, msg);
    }

    final synchronized static public void log(Object who, Exception e) {
        String message = "Exception (" + e.getClass().getSimpleName() + ")";
        if (e.getMessage() != null)
            message = message + ": " + e.getMessage();
        Hint.log(who, message);
        e.printStackTrace();
    }

    final synchronized static public void log(String who, Exception e) {
        String message = "Exception (" + e.getClass().getSimpleName() + ")";
        if (e.getMessage() != null)
            message = message + ": " + e.getMessage();
        Hint.log(who, message);
        e.printStackTrace();
    }
}
