package de.badaix.pacetracker.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.text.TextUtils;
import android.util.TypedValue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Helper {
    public static void SendMail(Context context, String emailTo, String emailCC, String subject, String emailText,
                                List<String> filePaths) {
        // need to "send multiple" to get more than one attachment
        final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND_MULTIPLE);
        emailIntent.setType("plain/text");
        if (!TextUtils.isEmpty(emailTo))
            emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{emailTo});
        if (!TextUtils.isEmpty(emailCC))
            emailIntent.putExtra(android.content.Intent.EXTRA_CC, new String[]{emailCC});
        if (!TextUtils.isEmpty(subject))
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        // has to be an ArrayList
        ArrayList<Uri> uris = new ArrayList<Uri>();
        // convert from paths to Android friendly Parcelable Uri's
        for (String file : filePaths) {
            File fileIn = new File(file);
            Uri u = Uri.fromFile(fileIn);
            uris.add(u);
        }
        emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        context.startActivity(Intent.createChooser(emailIntent, "Send..."));
    }

    public static String bytesToHex(byte[] buffer, int length) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            if (i > 0)
                sb.append(':');
            sb.append(Integer.toString((buffer[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    public static int dipToPix(Context context, float dip) {
        return (int) Math.ceil(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, context.getResources()
                .getDisplayMetrics()));
    }

    public static int spToPix(Context context, float sp) {
        return (int) Math.ceil(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, (float) sp, context.getResources()
                .getDisplayMetrics()));
    }

    public static boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected())
            return true;

        return false;
    }

    public static boolean isIntentAvailable(Context context, String action) {
        final PackageManager packageManager = context.getPackageManager();
        final Intent intent = new Intent(action);
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }
}
