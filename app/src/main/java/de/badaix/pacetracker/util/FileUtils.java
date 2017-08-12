package de.badaix.pacetracker.util;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtils {
    public static File getCacheStreamPath(Context context, String name) {
        return new File(getCacheDir(context).getAbsolutePath(), name);
    }

    public static File getCacheDir(final Context context) {
        // e.g. "<sdcard>/Android/data/<package_name>/cache/"
        File result;
        if (isExternalStorageAvailable())
            result = context.getExternalCacheDir();
        else
            result = context.getCacheDir();
        if (!result.exists())
            result.mkdirs();
        return result;
    }

    public static FileInputStream openCacheInput(Context context, String name) throws FileNotFoundException {
        return new FileInputStream(getCacheStreamPath(context, name));
    }

    public static FileInputStream openCacheInput(Context context, String subDir, String name)
            throws FileNotFoundException {
        return new FileInputStream(getCacheStreamPath(context, subDir + "/" + name));
    }

    public static FileOutputStream openCacheOutput(Context context, String name) throws FileNotFoundException {
        return new FileOutputStream(getCacheStreamPath(context, name));
    }

    public static FileOutputStream openCacheOutput(Context context, String subDir, String name)
            throws FileNotFoundException {
        getCacheStreamPath(context, subDir).mkdirs();
        return new FileOutputStream(getCacheStreamPath(context, subDir + "/" + name));
    }

    public static boolean isExternalStorageAvailable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    public static File getExternalDir(String appName, String subdir) {
        return new File(Environment.getExternalStorageDirectory(), appName + "/" + subdir);
    }

    public static File getDir(Context context, String subdir) {
        try {
            File root;
            if (isExternalStorageAvailable())
                root = context.getExternalFilesDir(null);
            else
                root = context.getFilesDir();

            root = new File(root, subdir);
            if (!root.exists()) {
                root.mkdirs();
            }
            return root;
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("FileUtils", e.getMessage());
        }
        return null;
    }

    public static File getFilename(Context context, String filename, String subdir) {
        File root = getDir(context, subdir);
        if (root != null)
            return new File(root, filename);
        return null;
    }

    public static void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

}
