package de.badaix.pacetracker.util;

import android.os.AsyncTask;

public class Timer extends AsyncTask<OnTimerListener, Void, Void> {

    private OnTimerListener onTimerListener = null;
    private volatile boolean running = true;
    private int interval = 1000;

    public Timer() {
        this(1000);
    }

    public Timer(int interval) {
        this.interval = interval;
    }

    @Override
    protected void onCancelled() {
        running = false;
    }

    @Override
    protected Void doInBackground(OnTimerListener... params) {
        onTimerListener = params[0];
        while (running) {
            try {
                Thread.sleep(interval);
                publishProgress((Void) null);
            } catch (InterruptedException e) {
                break;
            }
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        onTimerListener.onTimer();
    }
}
