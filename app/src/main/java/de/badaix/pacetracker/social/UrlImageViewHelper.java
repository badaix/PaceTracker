package de.badaix.pacetracker.social;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Build;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.URL;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import de.badaix.pacetracker.util.Hint;
import de.badaix.pacetracker.util.HttpDownloader;

class Job {
    ImageView imageView;
    String url;
    Drawable defaultDrawable;
    UrlImageViewCallback callback;
    Bitmap newBitmap = null;
    boolean valid = true;

    public Job(final ImageView imageView, final String url, final Drawable defaultDrawable,
               final UrlImageViewCallback callback) {
        this.imageView = imageView;
        this.url = url;
        this.defaultDrawable = defaultDrawable;
        this.callback = callback;
        this.valid = true;
    }
}

class DownloadFilesTask extends AsyncTask<Void, Job, Void> {
    private Stack<Job> jobs = null;
    private Job currentJob = null;

    public DownloadFilesTask(Stack<Job> jobs) {
        this.jobs = jobs;
    }

    public void invalidate(ImageView imageView) {
        try {
            if ((imageView != null) && (currentJob != null) && (imageView.equals(currentJob.imageView)))
                currentJob.valid = false;
        } catch (Exception e) {
            Hint.log(this, "Exception: " + e.getMessage());
        }
    }

    @Override
    protected Void doInBackground(Void... params) {
        while (!isCancelled()) {
            Job job = null;
            try {

                synchronized (jobs) {
                    if (jobs.empty())
                        jobs.wait(5000);
                    if (jobs.empty())
                        continue;

                    do {
                        job = jobs.pop();
                    } while (((job == null) || !job.valid) && !jobs.empty());

                    if ((job == null) || !job.valid)
                        continue;
                }

                currentJob = job;

                // Bitmap bitmap =
                // ImageCache.getInstance().getBitmapFromMemCache(job.url);
                // if (bitmap != null) {
                // job.newBitmap = bitmap;
                // publishProgress(job);
                // continue;
                // }

                if (!currentJob.valid)
                    continue;

                // Hint.log(this, "new job: " + job.url);
                Bitmap bitmap = ImageCache.getInstance().getBitmapFromCache(job.url);
                if (bitmap != null) {
                    // Hint.log(this, "Cache hit on: " + job.url);
                    job.newBitmap = bitmap;
                    publishProgress(job);
                    continue;
                }

                if (!currentJob.valid)
                    continue;

                InputStream inputStream = new HttpDownloader().getStream(new URL(job.url));
                if (inputStream != null) {
                    Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = false;
                    options.inSampleSize = 1;
                    job.newBitmap = BitmapFactory.decodeStream(inputStream, null, options);
                    // while ((job.newBitmap == null) && (options.inSampleSize
                    // <= 16)) {
                    // inputStream.close();
                    // inputStream = new HttpDownloader().getStream(new
                    // URL(job.url));
                    // options.inSampleSize *= 2;
                    // Hint.log(this, "Bitmap == null. Settings scale to " +
                    // options.inSampleSize);
                    // job.newBitmap = BitmapFactory.decodeStream(inputStream,
                    // null, options);
                    // }
                    if (job.newBitmap != null)
                        ImageCache.getInstance().addBitmapToCache(job.url, job.newBitmap);
                    publishProgress(job);
                }
            } catch (OutOfMemoryError e) {
                continue;
            } catch (InterruptedException ex) {
                publishProgress(job);
            } catch (Exception ex) {
                Hint.log(this, ex);
                publishProgress(job);
                continue;
            }
        }
        return null;
    }

    protected void onProgressUpdate(Job... progress) {
        try {
            Job job = progress[0];

            if (job.newBitmap != null) {
                if ((job.imageView != null) && job.valid)
                    job.imageView.setImageBitmap(job.newBitmap);
            } else if ((job.imageView != null) && job.valid)
                job.imageView.setImageDrawable(job.defaultDrawable);

            if (job.callback != null)
                job.callback.onLoaded(job.imageView, job.url, false);
        } catch (Exception e) {
            Hint.log(this, "Exception: " + e.getMessage());
        }
    }
};

public final class UrlImageViewHelper {
    private static final int NUM_DOWNLOADER = 3;
    private static UrlImageViewHelper instance = null;
    private Vector<DownloadFilesTask> downloader = null;
    private Stack<Job> mJobs = null;
    private Executor executor = Executors.newFixedThreadPool(NUM_DOWNLOADER + 1);

    public static UrlImageViewHelper getInstance() {
        if (instance == null) {
            instance = new UrlImageViewHelper();
        }
        return instance;
    }

    boolean isNullOrEmpty(CharSequence s) {
        return (s == null || s.equals("") || s.equals("null") || s.equals("NULL"));
    }

    private void cancelDownloader() {
        if (downloader == null)
            return;

        for (DownloadFilesTask downloadFilesTask : downloader) {
            if ((downloadFilesTask != null) && (downloadFilesTask.getStatus() == Status.RUNNING)) {
                downloadFilesTask.cancel(true);
                downloadFilesTask = null;
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void spawnDownloader() {
        if (downloader == null) {
            downloader = new Vector<DownloadFilesTask>();
            for (int i = 0; i < NUM_DOWNLOADER; ++i) {
                downloader.add(new DownloadFilesTask(mJobs));

                if (Build.VERSION.SDK_INT >= 13)
                    downloader.lastElement().executeOnExecutor(executor, (Void[]) null);
                else
                    downloader.lastElement().execute((Void[]) null);
            }
        }

        for (DownloadFilesTask downloadFilesTask : downloader) {
            if ((downloadFilesTask == null) || (downloadFilesTask.getStatus() != Status.RUNNING)) {
                downloadFilesTask = new DownloadFilesTask(mJobs);
                if (Build.VERSION.SDK_INT >= 13)
                    downloadFilesTask.executeOnExecutor(executor, (Void[]) null);
                else
                    downloadFilesTask.execute((Void[]) null);
            }
        }
    }

    public void setUrlDrawable(final ImageView imageView, final String url, final int defaultDrawableResource) {
        setUrlDrawable(imageView, url, imageView.getContext().getResources().getDrawable(defaultDrawableResource), null);
    }

    public void setUrlDrawable(final ImageView imageView, final String url, final int defaultDrawableResource,
                               final UrlImageViewCallback callback) {
        setUrlDrawable(imageView, url, imageView.getContext().getResources().getDrawable(defaultDrawableResource),
                callback);
    }

    public void setUrlDrawable(final ImageView imageView, final String url, final Drawable defaultDrawable) {
        setUrlDrawable(imageView, url, defaultDrawable, null);
    }

    public void setUrlDrawable(final ImageView imageView, final String url, final Drawable defaultDrawable,
                               final UrlImageViewCallback callback) {

        if (mJobs == null) {
            cancelDownloader();
            mJobs = new Stack<Job>();
            spawnDownloader();
        }

        try {
            synchronized (mJobs) {
                // no imageview => nothing to do
                if (imageView == null)
                    return;

                // imageView is part of another job? Invalidate this job
                for (Job job : mJobs) {
                    if (imageView.equals(job.imageView))
                        job.valid = false;
                }

                // imageView is currently processed? Invalidate this job
                for (DownloadFilesTask downloadFilesTask : downloader)
                    downloadFilesTask.invalidate(imageView);

                // no url => default
                if (isNullOrEmpty(url)) {
                    imageView.setImageDrawable(defaultDrawable);
                    if (callback != null)
                        callback.onLoaded(imageView, url, false);
                    return;
                }

                // Check mem cache
                Bitmap bitmap = ImageCache.getInstance().getBitmapFromMemCache(url);
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                    if (callback != null)
                        callback.onLoaded(imageView, url, false);
                    return;
                }

                if (defaultDrawable != null)
                    imageView.setImageDrawable(defaultDrawable);

                mJobs.push(new Job(imageView, url, defaultDrawable, callback));
                mJobs.notify();
            }
        } catch (Exception e) {
            Hint.log("setUrlDrawable", "Exception: " + e.getMessage());
        }
    }

    public void cleanUp() {
        cancelDownloader();
        downloader = null;
        mJobs = null;
        ImageCache.getInstance().cleanUp();
        instance = null;
    }
}
