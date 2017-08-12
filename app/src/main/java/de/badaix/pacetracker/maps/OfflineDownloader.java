package de.badaix.pacetracker.maps;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.maps.TileDownloadProgress.TileDownloadState;
import de.badaix.pacetracker.util.Hint;

public class OfflineDownloader extends Service {
    private final IBinder mBinder = new LocalBinder();
    // private OnProgressListener listener;
    private NotificationCompat.Builder mBuilder;
    private NotificationManager mNotificationManager;
    private int tileCount = 0;
    private Vector<String> logs = new Vector<String>();
    private OnProgressListener listener = null;
    private DownloadTilesTask downloadTilesTask = null;
    private Vector<TileDownloadProgress> progressHistory;
    private TileDownloadState state;
    private Queue<Job> jobQueue;
    private PendingIntent contentIntent;
    public OfflineDownloader() {
        progressHistory = new Vector<TileDownloadProgress>();
        jobQueue = new LinkedBlockingQueue<Job>();
        state = TileDownloadState.NULL;
    }

    public TileDownloadState getState() {
        return state;
    }

    public Vector<TileDownloadProgress> getProgressHistory() {
        return progressHistory;
    }

    private void updateState(TileDownloadState state) {
        this.state = state;
        if (listener != null)
            listener.onStateChanged(state);
    }

    private void updateProgress(TileDownloadProgress progress) {
        progressHistory.add(progress);
        mBuilder.setProgress(progress.getTotal(), progress.getDone(), false).setContentText(
                "Downloading tile " + progress.getDone() + "/" + progress.getTotal());
        Hint.log(this, "Downloading tile " + progress.getDone() + "/" + progress.getTotal());
        if (listener != null)
            listener.onProgress(progress);

        mNotificationManager.notify(android.R.drawable.stat_sys_download, mBuilder.build());
    }

    public void removeNotification() {
        stopForeground(true);
        stopSelf();
        mNotificationManager.cancelAll();
    }

    private void notifyDone(String tickerMsg, String contentMsg) {

        if (getState() == TileDownloadState.DONE) {
            // contentIntent = PendingIntent.getActivity(
            // getApplicationContext(),
            // 0,
            // new Intent(),
            // PendingIntent.FLAG_UPDATE_CURRENT);
            stopForeground(true);
            // stopSelf();
        }

        mBuilder.setContentText(contentMsg).setTicker(tickerMsg).setSmallIcon(android.R.drawable.ic_menu_mapmode)
                .setContentIntent(contentIntent).setProgress(0, 0, false).setAutoCancel(true);
        mNotificationManager.notify(android.R.drawable.stat_sys_download, mBuilder.build());
    }

    public void setListener(OnProgressListener listener) {
        this.listener = listener;
    }

    public void abort() {
        if ((downloadTilesTask != null) && (downloadTilesTask.getStatus() == Status.RUNNING))
            downloadTilesTask.cancel(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(this).setSmallIcon(android.R.drawable.stat_sys_download)
                .setTicker(getString(R.string.downloading_tiles)).setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.downloading) + "...").setProgress(0, 0, true);

        Intent sessionIntent = new Intent(this, DownloadActivity.class);
        sessionIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        contentIntent = PendingIntent.getActivity(this, 0, sessionIntent, 0);
        mBuilder.setContentIntent(contentIntent);
        startForeground(android.R.drawable.stat_sys_download, mBuilder.build());
        return START_NOT_STICKY;
    }

    @SuppressLint("NewApi")
    public void retry() {
        if ((downloadTilesTask != null) && (downloadTilesTask.getStatus() == Status.RUNNING))
            return;

        tileCount = 0;
        progressHistory.clear();
        downloadTilesTask = new DownloadTilesTask();
        for (Job job : jobQueue)
            downloadTilesTask.addJob(job);

        if (Build.VERSION.SDK_INT >= 13)
            downloadTilesTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Job[]) null);
        else
            downloadTilesTask.execute((Job[]) null);
    }

    @SuppressLint("NewApi")
    public void download(Job... jobs) {
        for (int i = 0; i < jobs.length; ++i)
            jobQueue.add(jobs[i]);

        if ((downloadTilesTask != null) && (downloadTilesTask.getStatus() == Status.RUNNING)) {
            downloadTilesTask.addJobs(jobs);
            return;
        }

        downloadTilesTask = new DownloadTilesTask();
        if (Build.VERSION.SDK_INT >= 13)
            downloadTilesTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, jobs);
        else
            downloadTilesTask.execute(jobs);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public abstract interface OnProgressListener {
        public void onProgress(TileDownloadProgress progress);

        public void onStateChanged(TileDownloadState state);
    }

    public class LocalBinder extends Binder {
        public OfflineDownloader getService() {
            // Return this instance of LocalService so clients can call public
            // methods
            return OfflineDownloader.this;
        }
    }

    private class DownloadTilesTask extends AsyncTask<Job, TileDownloadProgress, Void> {
        Queue<Job> jobQueue = new LinkedBlockingQueue<Job>();
        int done = 0;
        int failed = 0;

        public void addJob(Job job) {
            tileCount += job.tiles.size();
            jobQueue.add(job);
        }

        public void addJobs(Job... jobs) {
            if (jobs == null)
                return;

            for (int i = 0; i < jobs.length; ++i) {
                addJob(jobs[i]);
            }
        }

        @Override
        protected void onCancelled() {
            updateState(TileDownloadState.ABORTED);
            publishProgress(new TileDownloadProgress(done, failed, tileCount, TileDownloadState.ABORTED, null, null));
            notifyDone(getString(R.string.download_canceled), getString(R.string.download_canceled));
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            updateState(TileDownloadState.DOWNLOADING);
        }

        protected Void doInBackground(Job... jobs) {
            addJobs(jobs);

            while (!jobQueue.isEmpty()) {
                Job job = jobQueue.poll();
                CachedTileProvider provider = TileSourceFactory
                        .getTileProvider(getApplicationContext(), job.tileSource);
                for (TilePos tile : job.tiles) {
                    if (isCancelled())
                        return null;

                    String url = provider.getTileSource().getTileURL(tile).toExternalForm();
                    publishProgress(new TileDownloadProgress(done + 1, failed, tileCount,
                            TileDownloadState.DOWNLOADING, provider.getTileName((int) tile.x, (int) tile.y,
                            (int) tile.z), null));
                    File tileFile = provider.getTileFile((int) tile.x, (int) tile.y, tile.z);

                    ++done;
                    logs.add(url);

                    if (tileFile.exists()) {
                        if ((tileFile.length() == 0) || BitmapFactory.decodeFile(tileFile.getAbsolutePath()) != null)
                            continue;
                        else
                            tileFile.delete();
                    }

                    Tile t = provider.getTileFromCache((int) tile.x, (int) tile.y, tile.z);
                    if (t == null) {
                        try {
                            t = provider.getTileFromUrl((int) tile.x, (int) tile.y, tile.z);
                            if (!TileProvider.NO_TILE.equals(t)
                                    && (BitmapFactory.decodeByteArray(t.data, 0, t.data.length) == null))
                                throw new IOException("Error decoding tile");
                            Thread.sleep(100);
                        } catch (Exception e) {
                            ++failed;
                            publishProgress(new TileDownloadProgress(done, failed, tileCount, TileDownloadState.FAILED,
                                    url, e));
                            continue;
                        }
                    }

                    try {
                        tileFile.getParentFile().mkdirs();
                        FileOutputStream fos = new FileOutputStream(tileFile);
                        try {
                            if (TileProvider.NO_TILE.equals(t))
                                fos.write(new byte[0]);
                            else
                                fos.write(t.data);
                        } finally {
                            if (fos != null)
                                fos.close();
                        }
                    } catch (IOException e) {
                        ++failed;
                        tileFile.delete();
                        publishProgress(new TileDownloadProgress(done, failed, tileCount, TileDownloadState.FAILED,
                                url, e));
                        continue;
                    }
                    publishProgress(new TileDownloadProgress(done, failed, tileCount, TileDownloadState.DONE, url, null));
                }
            }
            return null;
        }

        protected void onProgressUpdate(TileDownloadProgress... progress) {
            if (!isCancelled())
                updateProgress(progress[0]);
        }

        protected void onPostExecute(Void result) {
            if (!isCancelled()) {
                if (failed > 0)
                    updateState(TileDownloadState.FAILED);
                else
                    updateState(TileDownloadState.DONE);
            } else {
                updateState(TileDownloadState.ABORTED);
            }

            notifyDone(getString(R.string.download_completed), getString(R.string.download_completed));
        }
    }

}
