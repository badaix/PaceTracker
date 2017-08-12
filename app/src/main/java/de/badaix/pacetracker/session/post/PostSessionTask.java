package de.badaix.pacetracker.session.post;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import org.json.JSONObject;

import java.io.StringWriter;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.session.Exporter;
import de.badaix.pacetracker.session.Session;
import de.badaix.pacetracker.session.SessionFactory;
import de.badaix.pacetracker.session.SessionReader;
import de.badaix.pacetracker.session.SessionSummary;
import de.badaix.pacetracker.session.SessionWriter;
import de.badaix.pacetracker.settings.SessionSettings;
import de.badaix.pacetracker.social.dailymile.DailyMile;
import de.badaix.pacetracker.social.dailymile.DailyMileHelper;
import de.badaix.pacetracker.social.dailymile.PersonEntry;
import de.badaix.pacetracker.util.Hint;

public class PostSessionTask extends AsyncTask<SessionSummary, String, Void> {
    private SessionSummary exportSession;
    private Session session;
    private ProgressDialog progressDialog;
    private Context context;
    private Exception exception = null;
    private boolean uploadRoute = false;
    private PostSessionListener listener;

    public PostSessionTask(Context context, PostSessionListener listener, boolean uploadRoute) {
        this.context = context;
        progressDialog = ProgressDialog.show(context, "",
                context.getResources().getString(R.string.historyPostSession), true);
        this.uploadRoute = uploadRoute;
        this.listener = listener;
        progressDialog.show();
    }

    /**
     * The system calls this to perform work in a worker thread and delivers it
     * the parameters given to AsyncTask.execute()
     */
    protected Void doInBackground(SessionSummary... item) {
        exportSession = item[0];
        if (uploadRoute)
            publishProgress(context.getResources().getString(R.string.historyCreatingGpx));
        try {
            StringWriter stringWriter = new StringWriter();
            if (uploadRoute) {
                if (exportSession instanceof Session) {
                    session = (Session) exportSession;
                } else {
                    session = SessionFactory.getInstance().getSessionByType(exportSession.getType(), null,
                            new SessionSettings(false));
                    SessionReader reader = new SessionReader();
                    reader.readSessionFromFile(exportSession.getFilename(), session);
                }

                if (isCancelled())
                    return null;

                Exporter.toGpx(session, stringWriter, 3.0f);
            }

            publishProgress(context.getResources().getString(R.string.historyPostSession));
            DailyMile dailyMile = new DailyMile(context);
            PersonEntry newEntry = new PersonEntry(new JSONObject(dailyMile.postSession(DailyMileHelper
                    .sessionToEntry(exportSession))));
            int entryId = newEntry.getId();
            Hint.log(this, "Entry created: " + entryId);
            // dailyMile.deleteSession(entryId);
            if (uploadRoute) {
                publishProgress(context.getResources().getString(R.string.historyUploadingGpx));
                dailyMile.updateGpxForEntry(entryId, stringWriter.getBuffer().toString());
            }
            exportSession.getSettings().setDailyMileId(entryId);
            SessionWriter sessionWriter = new SessionWriter(context);
            sessionWriter.updateSession(exportSession);

            // SessionPersistance persistance = new
            // SessionPersistance(this.context);
            // persistance.updateSession(exportSession.getId(), exportSession);
        } catch (Exception e) {
            exception = e;
        }

        return null;
        // return loadImageFromNetwork(filename);
    }

    protected void onProgressUpdate(String... progress) {
        progressDialog.setMessage(progress[0]);
    }

    /**
     * The system calls this to perform work in the UI thread and delivers the
     * result from doInBackground()
     */
    protected void onPostExecute(Void result) {
        progressDialog.cancel();

        if (listener != null) {
            if (exception != null)
                listener.onPostSessionFailed(exportSession, exception);
            else
                listener.onSessionPostet(exportSession);
        }
    }
}
