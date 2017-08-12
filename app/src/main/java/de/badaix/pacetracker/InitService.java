package de.badaix.pacetracker;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.badaix.pacetracker.maps.TileCache;
import de.badaix.pacetracker.session.SessionReader;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.social.ImageCache;
import de.badaix.pacetracker.social.dailymile.DailyMile;
import de.badaix.pacetracker.social.dailymile.User;
import de.badaix.pacetracker.util.FileUtils;
import de.badaix.pacetracker.util.Hint;
import de.badaix.pacetracker.util.LocationUtils;

public class InitService extends Service {

    private AsyncTask<Void, Void, Void> initAsyncTask = null;

    @SuppressLint("NewApi")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (initAsyncTask == null || initAsyncTask.getStatus().equals(AsyncTask.Status.FINISHED)) {
            initAsyncTask = new InitAsyncTask(this);
            if (Build.VERSION.SDK_INT >= 13)
                initAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
            else
                initAsyncTask.execute((Void[]) null);
        }

        new Thread(new Runnable() {

            @Override
            public void run() {
                Location location = LocationUtils.getLastKnownLocation(InitService.this, false);
                if (location != null) {
                    try {
                        LocationUtils.lastKnownAddress = LocationUtils.getAddressFromLocation(InitService.this,
                                location);
                    } catch (IOException e) {
                        LocationUtils.lastKnownAddress = null;
                        e.printStackTrace();
                    }
                }

                DailyMile dm = new DailyMile(InitService.this);
                User user = dm.getMe();
                if (user != null)
                    GlobalSettings.getInstance().setMe(user);

                importSessions();
                Hint.log(this, "Done");
            }
        }).start();

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    private void importSessions() {
        File dir = FileUtils.getDir(this, "ImportOld");
        if (dir == null)
            return;
        Hint.log(this, "dir: " + dir.getAbsolutePath());
        final File[] files = dir.listFiles();
        for (final File file : files) {
            Hint.log(this, "file: " + file.getAbsolutePath());
            if (!file.getAbsolutePath().endsWith(".pts"))
                continue;

            SessionReader reader = new SessionReader();
            try {
                Hint.log(this, "Importing: " + file.getName());
                reader.readSessionFromFile(file);
                // SessionSummary summary =
                // reader.readSummaryFromFile(file.getAbsolutePath());
                // Hint.log(this, summary.toJson().toString());
            } catch (Exception e) {
                Hint.log(this, e);
            }
        }
    }

    private void copyRawFiles() {
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_NOTIFICATIONS);
        File file = new File(path, "Pacetracker Beep.wav");
        Hint.log(this, "Alarm file: " + file.getAbsolutePath());
        if (file.exists())
            return;
        try {
            // Make sure the Pictures directory exists.
            path.mkdirs();
            InputStream is = getResources().openRawResource(R.raw.beep);
            OutputStream os = new FileOutputStream(file);
            byte[] data = new byte[is.available()];
            is.read(data);
            os.write(data);
            is.close();
            os.close();

            // Tell the media scanner about the new file so that it is
            // immediately available to the user.
            MediaScannerConnection.scanFile(this, new String[]{file.getAbsolutePath()}, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            Hint.log(this, "Scanned " + path + ":");
                            Hint.log(this, "-> uri=" + uri);
                            Uri settingsUri = GlobalSettings.getInstance().getPulseAlarmUri();
                            if ((settingsUri == null) || settingsUri.equals(Uri.EMPTY))
                                GlobalSettings.getInstance().setPulseAlarmUri(uri);
                        }
                    });
        } catch (IOException e) {
            Hint.log(this, e);
        }
    }

    private class InitAsyncTask extends AsyncTask<Void, Void, Void> {
        private Context context;

        public InitAsyncTask(Context context) {
            this.context = context;
        }

        private void initTileCache() {
            Hint.log(this, "initTileCache start");
            TileCache.getInstance().initDiskCache(FileUtils.getCacheStreamPath(context, "maptiles"), 1024 * 1024 * 50);
            Hint.log(this, "initTileCache done");
        }

        private void initImageCache() {
            Hint.log(this, "initImageCache start");
            final int memClass = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE))
                    .getMemoryClass();
            // Use 1/8th of the available memory for this memory cache.
            final int cacheSize = 1024 * 1024 * memClass / 8;
            ImageCache.getInstance().initCache(FileUtils.getCacheStreamPath(context, "images"), cacheSize,
                    1024 * 1024 * 20);
            Hint.log(this, "initImageCache end");
        }

        @Override
        protected Void doInBackground(Void... item) {
            Hint.log(this, "Started");

            try {
                new File(Environment.getExternalStorageDirectory(), "PaceTracker/Import").mkdirs();
                new File(Environment.getExternalStorageDirectory(), "PaceTracker/Export").mkdirs();
                FileUtils.getDir(context, "sessions").mkdirs();
                FileUtils.getExternalDir("PaceTracker", ".nomedia").createNewFile();
                FileUtils.getFilename(context, ".nomedia", ".").createNewFile();
                new File(FileUtils.getCacheDir(context), ".nomedia").createNewFile();
            } catch (Exception e) {
                Hint.log(this, e);
            }

            copyRawFiles();
            // LocationUtils.setLastKnownLocation(LocationUtils
            // .getLastKnownLoaction(context, true));

            // Location lastLocation = LocationUtils.getLastKnownLocation();
            // if (lastLocation != null) {
            // try {
            // WorldWeather.getCurrentConditions(new
            // GeoPos(lastLocation.getLatitude(), lastLocation.getLongitude()));
            // } catch (Exception e) {
            // Hint.log(this, e);
            // }
            // }

            initImageCache();
            initTileCache();

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            stopSelf();
        }
    }

}
