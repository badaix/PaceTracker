package de.badaix.pacetracker.util;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class ExceptionHandler {
    public static void Handle(String tag, Exception e) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        e.printStackTrace(ps);

        Log.d(tag,
                "Exception: " + e.getClass().getName() + "\n\nMessage:\n" + e.getMessage() + "\n\nTrace:\n"
                        + baos.toString());
    }
}
