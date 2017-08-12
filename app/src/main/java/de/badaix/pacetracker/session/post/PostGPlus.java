package de.badaix.pacetracker.session.post;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.TextUtils;

import com.google.android.gms.plus.PlusShare;

import java.io.IOException;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.session.SessionSummary;
import de.badaix.pacetracker.session.SessionWriter;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.util.Distance;

public class PostGPlus {
    public static final int GPLUS_REQUEST_CODE = 7452;
    public static final String GOOGLE_PLUS_PACKAGE = "com.google.android.apps.plus";
    private static final String GOOGLE_PLUS_MARKET_URI = "market://details?id=" + GOOGLE_PLUS_PACKAGE;

    private Activity activity;
    private SessionSummary sessionSummary;

    public PostGPlus(Activity activity) {
        this.activity = activity;
    }

    public static boolean isGooglePlusInstalled(Context context) {
        try {
            context.getPackageManager().getApplicationInfo(GOOGLE_PLUS_PACKAGE, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static Dialog createInstallPlusDialog(final Context context) {
        return new AlertDialog.Builder(context).setCancelable(true).setMessage(R.string.installPlus)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent();
                        intent.setData(Uri.parse(GOOGLE_PLUS_MARKET_URI));
                        context.startActivity(intent);
                    }
                }).create();
    }

    public void updateSession() throws IOException {
        sessionSummary.getSettings().setGPlusId(1);
        SessionWriter sessionWriter = new SessionWriter(activity);
        sessionWriter.updateSession(sessionSummary);
    }

    public void post(Context context, SessionSummary sessionSummary) {
        if (!isGooglePlusInstalled(context)) {
            createInstallPlusDialog(context).show();
            return;
        }

        this.sessionSummary = sessionSummary;

        String comment = sessionSummary.getSettings().getComment();
        String description = sessionSummary.getSettings().getDescription();

        String message = context.getString(R.string.postBodyGplus);
        message = message.replace("$what$", sessionSummary.getVerb(context));
        message = message.replace("$distance$", Distance.distanceToString(sessionSummary.getDistance(), 2) + " "
                + GlobalSettings.getInstance().getDistUnit().toShortString());
        message = message.replace("$app$", "#PaceTracker");

        if (!TextUtils.isEmpty(comment))
            message = message + "\n\n" + context.getString(R.string.commentItemName) + ": " + comment;
        if (!TextUtils.isEmpty(description))
            message = message + "\n\n" + context.getString(R.string.descriptionItemName) + ": " + description;

        String url = "http://play.google.com/store/apps/details?id=de.badaix.pacetracker";
        Intent shareIntent = new PlusShare.Builder(activity).setType("image/png").setText(message)
                .setContentUrl(Uri.parse(url)).getIntent();

        activity.startActivityForResult(shareIntent, GPLUS_REQUEST_CODE);
    }

}
