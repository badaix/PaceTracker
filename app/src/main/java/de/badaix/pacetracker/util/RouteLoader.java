package de.badaix.pacetracker.util;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import java.io.File;
import java.io.IOException;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.session.Route;

public class RouteLoader extends AsyncTask<File, Void, Route> {
    private Context context;
    private OnFinishedListener onFinishedListener;
    private Exception exception;
    private ProgressDialog dialog = null;
    private boolean showProgress = false;
    public RouteLoader(Context context, boolean showProgress, OnFinishedListener listener) {
        this.context = context;
        this.onFinishedListener = listener;
        exception = null;
        this.showProgress = showProgress;
    }

    @Override
    protected void onPreExecute() {
        if (showProgress)
            dialog = ProgressDialog.show(context, "",
                    context.getResources().getString(R.string.loadingRoutePleaseWait), true);
    }

    @Override
    protected Route doInBackground(File... params) {
        Route route = null;
        try {
            if (!params[0].toString().endsWith(".json"))
                throw new IOException("File format not supported");
            route = new Route(params[0]);
            route.setFilename(params[0].getAbsolutePath());
        } catch (Exception e) {
            exception = e;
            return null;
        }
        return route;
    }

    @Override
    protected void onPostExecute(Route result) {
        if (dialog != null)
            dialog.dismiss();
        if (this.exception != null)
            Hint.show(context, exception);
        onFinishedListener.onFinished(result, exception);
    }

    public interface OnFinishedListener {
        void onFinished(Route route, Exception e);
    }
}
