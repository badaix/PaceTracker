package de.badaix.pacetracker.util;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.session.Exporter;
import de.badaix.pacetracker.session.Session;
import de.badaix.pacetracker.session.SessionFactory;
import de.badaix.pacetracker.session.SessionReader;
import de.badaix.pacetracker.session.SessionSummary;
import de.badaix.pacetracker.settings.SessionSettings;

public class GoogleEarth {
    /**
     * KML mime type.
     */
    public static final String KML_MIME_TYPE = "application/vnd.google-earth.kml+xml";
    public static final String TOUR_FEATURE_ID = "com.google.earth.EXTRA.tour_feature_id";

    public static final String GOOGLE_EARTH_PACKAGE = "com.google.earth";
    public static final String GOOGLE_EARTH_CLASS = "com.google.earth.EarthActivity";
    private static final String EARTH_MARKET_URI = "market://details?id=" + GOOGLE_EARTH_PACKAGE;

    private GoogleEarth() {
    }

    /**
     * Returns true if a Google Earth app that can handle KML mine type is
     * installed.
     *
     * @param context the context
     */
    public static boolean isEarthInstalled(Context context) {
        List<ResolveInfo> infos = context.getPackageManager().queryIntentActivities(
                new Intent().setType(KML_MIME_TYPE), PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo info : infos) {
            if (info.activityInfo != null && info.activityInfo.packageName != null
                    && info.activityInfo.packageName.equals(GOOGLE_EARTH_PACKAGE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Plays a track by sending an intent to {@link SaveActivity}.
     *
     * @param context the context
     * @param trackId the track id
     */
    public static void playTrack(Context context, File kmlFilename) {
        if (!isEarthInstalled(context)) {
            createInstallEarthDialog(context);
            return;
        }
        Uri uri = Uri.fromFile(kmlFilename);
        Hint.log("GoogleEarth", "play track: " + kmlFilename.getPath());
        //Uri uri = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).path(kmlFilename.getPath()).build();
        context.grantUriPermission(GOOGLE_EARTH_PACKAGE, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Intent intent = new Intent().addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(TOUR_FEATURE_ID, "tour").setClassName(GOOGLE_EARTH_PACKAGE, GOOGLE_EARTH_CLASS)
                .setDataAndType(uri, KML_MIME_TYPE);
        context.startActivity(intent);
    }

    public static void playTrack(final Context context, final SessionSummary summary) {
        AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            Exception exception;
            File file;
            ProgressDialog progressDialog;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progressDialog = ProgressDialog.show(context, "",
                        context.getResources().getString(R.string.creatingTour), true);
                progressDialog.show();
            }

            @Override
            protected Void doInBackground(Void... params) {
                Session session = SessionFactory.getInstance().getSessionByType(summary.getType(), null,
                        new SessionSettings(false));
                SessionReader reader = new SessionReader();
                try {
                    reader.readSessionFromFile(summary.getFilename(), session);
                    File dir = FileUtils.getExternalDir("PaceTracker", "tmp");
                    dir.mkdirs();
                    file = new File(dir, "earth.kml");
                    //FileUtils.getFilename(context, "earth.kml", "export");
                    FileWriter writer;
                    writer = new FileWriter(file);
                    Exporter.toKmlTour(context, session, writer, 4.0f);
                    writer.close();
                } catch (Exception e) {
                    exception = e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                progressDialog.dismiss();
                if (exception != null) {
                    Hint.show(context, exception);
                    return;
                }

                GoogleEarth.playTrack(context, file);
            }
        };

        if (!isEarthInstalled(context)) {
            createInstallEarthDialog(context).show();
            return;
        }
        asyncTask.execute((Void) null);
    }

    /**
     * Creates a dialog to install Google Earth from the Android Market.
     *
     * @param context the context
     */
    public static Dialog createInstallEarthDialog(final Context context) {
        return new AlertDialog.Builder(context).setCancelable(true).setMessage(R.string.installEarth)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent();
                        intent.setData(Uri.parse(EARTH_MARKET_URI));
                        context.startActivity(intent);
                    }
                }).create();
    }
}
