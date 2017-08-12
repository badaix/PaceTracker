package de.badaix.pacetracker.maps;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.Vector;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.maps.OfflineDownloader.LocalBinder;
import de.badaix.pacetracker.maps.OfflineDownloader.OnProgressListener;
import de.badaix.pacetracker.maps.TileDownloadProgress.TileDownloadState;
import de.badaix.pacetracker.util.Hint;

public class DownloadActivity extends Activity implements OnProgressListener, OnClickListener {
    boolean mBound = false;
    OfflineDownloader mService;
    private ProgressBar progressBar;
    private Button buttonAbort;
    private ListView listView;
    private ArrayList<String> arrayList;
    private ArrayAdapter<String> arrayAdapter;
    private TileDownloadState lastState;

    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocalBinder binder = (LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            Vector<TileDownloadProgress> history = mService.getProgressHistory();
            for (TileDownloadProgress tdp : history) {
                log(tdp);
            }

            if (!history.isEmpty()) {
                TileDownloadProgress progress = history.lastElement();
                progressBar.setMax(progress.getTotal());
                progressBar.setProgress(progress.getDone());
                progressBar.setSecondaryProgress(progress.getFailed());
                updateButtonState();
            }

            arrayAdapter.notifyDataSetChanged();
            scrollToBottom(false);
            mService.setListener(DownloadActivity.this);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        buttonAbort = (Button) findViewById(R.id.button);
        buttonAbort.setOnClickListener(this);

        listView = (ListView) findViewById(R.id.listView);

        arrayList = new ArrayList<String>();
        arrayAdapter = new ArrayAdapter<String>(this, R.layout.simple_list_item, arrayList);
        // View v = new View(this);
        // v.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
        // Helper.dipToPix(this, 40)));
        // listView.addHeaderView(v);
        arrayList.add("");
        arrayList.add("");
        listView.setAdapter(arrayAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, OfflineDownloader.class);
        if (!bindService(intent, connection, Context.BIND_AUTO_CREATE))
            this.finish();
    }

    @Override
    protected void onStop() {
        if (mBound) {
            mService.setListener(null);
            if (mService.getState() == TileDownloadState.FAILED) {
                mService.stopForeground(true);
                mService.stopSelf();
            }
        }
        unbindService(connection);
        super.onStop();
    }

    private boolean log(TileDownloadProgress progress) {
        if (progress == null)
            return false;
        if (progress.getState() == TileDownloadState.DONE)
            return false;

        String logLine = progress.getState().toString();
        if (progress.getException() != null) {
            logLine += " (" + progress.getException().getClass().getSimpleName();
            if (progress.getException().getLocalizedMessage() != null)
                logLine += ": " + progress.getException().getLocalizedMessage();
            logLine += ")";
        }
        if (progress.getUrl() != null)
            logLine += ": " + progress.getUrl();
        arrayList.add(logLine);
        return true;
    }

    private void scrollToBottom(final boolean animated) {
        listView.post(new Runnable() {
            @Override
            public void run() {
                if (animated)
                    listView.smoothScrollToPosition(arrayAdapter.getCount() - 1);
                else
                    listView.setSelection(arrayAdapter.getCount() - 1);
            }
        });
    }

    private void updateButtonState() {
        TileDownloadState state = mService.getState();
        if (state == lastState)
            return;

        if (state.isFinished()) {
            buttonAbort.setText(R.string.close);
        } else if (state == TileDownloadState.FAILED) {
            buttonAbort.setText(R.string.retry);
        } else if (state == TileDownloadState.DOWNLOADING) {
            buttonAbort.setText(R.string.abort);
        }
        lastState = state;
    }

    @Override
    public void onProgress(TileDownloadProgress progress) {
        progressBar.setMax(progress.getTotal());
        progressBar.setProgress(progress.getDone());
        progressBar.setSecondaryProgress(progress.getFailed());
        if (log(progress)) {
            arrayAdapter.notifyDataSetChanged();
            scrollToBottom(true);
        }
        updateButtonState();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // If the back Key was pressed, then finish the program.
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            Hint.log(this, "KEYCODE_BACK");

            if (mService.getState().isFinished()) {
                mService.removeNotification();
                this.finish();
            } else
                return super.onKeyDown(keyCode, event);
        }

        // else return the normal function of whatever key was pressed
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onClick(View v) {
        if (mBound) {
            if (mService.getState() == TileDownloadState.DOWNLOADING)
                mService.abort();
            else if (mService.getState() == TileDownloadState.FAILED)
                mService.retry();
            else if (mService.getState().isFinished()) {
                mService.removeNotification();
                this.finish();
            }
        }
    }

    @Override
    public void onStateChanged(TileDownloadState state) {
        updateButtonState();
        if (state == TileDownloadState.DONE)
            progressBar.setProgress(progressBar.getMax());
        if (state != TileDownloadState.DOWNLOADING)
            scrollToBottom(false);
    }

}
