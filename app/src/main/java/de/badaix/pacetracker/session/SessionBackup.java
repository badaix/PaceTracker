package de.badaix.pacetracker.session;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;

import java.io.File;
import java.util.Date;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.SessionPersistance;
import de.badaix.pacetracker.util.FileUtils;
import de.badaix.pacetracker.util.Hint;
import de.badaix.pacetracker.util.RefCountWakeLock;

public class SessionBackup {
    private Context context;
    private ExportTask exportTask;
    private ImportTask importTask;
    public SessionBackup(Context context) {
        this.context = context;
    }

    public void exportSessions(ExportListener listener) {
        exportTask = new ExportTask(listener);
        exportTask.execute((Void) null);
    }

    public void abortExport() {
        if ((exportTask != null) && (exportTask.getStatus() == Status.RUNNING))
            exportTask.cancel(true);
    }

    public void importSessions(ImportListener listener) {
        importTask = new ImportTask(listener);
        importTask.execute((Void) null);
    }

    public void abortImport() {
        if ((importTask != null) && (importTask.getStatus() == Status.RUNNING))
            importTask.cancel(true);
    }

    public void importSessions() {
        File dir = FileUtils.getDir(context, "Import");
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
                SessionSummary summary = reader.readSummaryFromFile(file.getAbsolutePath());
                if (summary != null) {
                    // SessionSummary summary =
                    // reader.readSummaryFromFile(file.getAbsolutePath());
                    // Hint.log(this, summary.toJson().toString());
                }
            } catch (Exception e) {
                Hint.log(this, e);
            }
        }
    }

    public interface ExportListener {
        public void onExport(int num, int of, Date date, String type, File filename);

        public void onExportFinished(boolean aborted, int exported, int failed, Exception e);
    }

    public interface ImportListener {
        public void onImport(int num, int of, String type);

        public void onImportFinished(boolean aborted, int imported, int failed, Exception e);
    }

    private class ExportTask extends AsyncTask<Void, Void, Void> {

        private ExportListener listener;
        private Exception exception = null;
        private int num;
        private int of;
        private int failed;
        private String type;
        private File file;
        private Date init;

        public ExportTask(ExportListener listener) {
            this.listener = listener;
        }

        private void exportSessions() {
            SessionPersistance sessionPersistance = SessionPersistance.getInstance(context);
            String fields[] = {"*"};
            Cursor cursor = null;
            try {
                cursor = sessionPersistance.querySessions(fields, "", null, "init asc", null);
                int dateColumn = cursor.getColumnIndex("init");
                int fileColumn = cursor.getColumnIndex("filename");
                // int typeColumn = cursor.getColumnIndex("type");
                type = context.getString(R.string.sessions);
                of = 0;
                failed = 0;

                if (cursor != null) {
                    try {
                        File path = FileUtils.getExternalDir("PaceTracker", "Export/Sessions");
                        path.mkdirs();
                        of = 0;
                        if (cursor.moveToFirst()) {
                            do {
                                ++of;
                            } while (cursor.moveToNext());
                        }
                        if (cursor.moveToFirst()) {
                            num = 0;
                            do {
                                try {
                                    // type = cursor.getString(typeColumn);
                                    String filename = cursor.getString(fileColumn);
                                    file = new File(filename);
                                    init = new Date(cursor.getLong(dateColumn));
                                    this.publishProgress((Void) null);
                                    File dest = new File(path, file.getName());
                                    if (!dest.exists() || !dest.isFile() || (dest.length() != file.length()))
                                        FileUtils.copy(file, dest);
                                    ++num;
                                    this.publishProgress((Void) null);
                                } catch (Exception e) {
                                    exception = e;
                                    ++failed;
                                }
                            } while (cursor.moveToNext() && !this.isCancelled());
                        }
                    } catch (Exception e) {
                        exception = e;
                    }
                }
            } finally {
                if ((cursor != null) && !cursor.isClosed())
                    cursor.close();
            }
        }

        private void exportRoutes() {
            SessionPersistance sessionPersistance = SessionPersistance.getInstance(context);
            String fields[] = {"*"};
            Cursor cursor = null;
            try {
                cursor = sessionPersistance.queryRoutes(fields, "", null, "created asc", null);
                int dateColumn = cursor.getColumnIndex("created");
                int fileColumn = cursor.getColumnIndex("filename");
                of = 0;
                failed = 0;
                type = context.getString(R.string.routes);

                if (cursor != null) {
                    try {
                        File path = FileUtils.getExternalDir("PaceTracker", "Export/Routes");
                        path.mkdirs();
                        of = 0;
                        if (cursor.moveToFirst()) {
                            do {
                                ++of;
                            } while (cursor.moveToNext());
                        }
                        if (cursor.moveToFirst()) {
                            num = 0;
                            do {
                                try {
                                    String filename = cursor.getString(fileColumn);
                                    file = new File(filename);
                                    init = new Date(cursor.getLong(dateColumn));
                                    this.publishProgress((Void) null);
                                    File dest = new File(path, file.getName());
                                    if (!dest.exists() || !dest.isFile() || (dest.length() != file.length()))
                                        FileUtils.copy(file, dest);
                                    ++num;
                                    this.publishProgress((Void) null);
                                } catch (Exception e) {
                                    exception = e;
                                    ++failed;
                                }
                            } while (cursor.moveToNext() && !this.isCancelled());
                        }
                    } catch (Exception e) {
                        exception = e;
                    }
                }
            } finally {
                if ((cursor != null) && !cursor.isClosed())
                    cursor.close();
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            RefCountWakeLock.getInstance(context).acquire(this);
            exportRoutes();
            exportSessions();
            RefCountWakeLock.getInstance(context).release(this);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if (listener != null)
                listener.onExportFinished(this.isCancelled(), num, failed, exception);
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            if (listener != null)
                listener.onExport(num, of, init, type, file);
        }
    }

    private class ImportTask extends AsyncTask<Void, Void, Void> {

        private ImportListener listener;
        private Exception exception = null;
        private int num;
        private int of;
        private int failed;
        private File destPath;
        private String type;

        public ImportTask(ImportListener listener) {
            this.listener = listener;
        }

        private void importSessions() {
            File importPath = FileUtils.getExternalDir("PaceTracker", "Import/Sessions");
            if (!importPath.exists())
                return;

            File failedPath = new File(importPath, "Failed");
            failedPath.mkdirs();

            destPath = FileUtils.getDir(context, "sessions");

            final File[] files = importPath.listFiles();
            of = 0;
            num = 0;
            failed = 0;
            type = context.getString(R.string.sessions);
            for (final File file : files) {
                if (file.getAbsolutePath().endsWith(".pts"))
                    ++of;
            }

            SessionPersistance sessionPersistance = SessionPersistance.getInstance(context);
            String fields[] = {"_id", "filename"};
            for (final File file : files) {
                Hint.log(this, "file: " + file.getAbsolutePath());
                if (isCancelled())
                    break;

                if (!file.getAbsolutePath().endsWith(".pts"))
                    continue;

                Cursor cursor = null;
                try {
                    try {
                        cursor = sessionPersistance.querySessions(fields, "filename like '%" + file.getName() + "%'",
                                null, "init asc", null);
                        if ((cursor != null) && cursor.moveToFirst()) {
                            int fileColumn = cursor.getColumnIndex("filename");
                            int idColumn = cursor.getColumnIndex("_id");
                            long id = cursor.getLong(idColumn);
                            File fileInDb = new File(cursor.getString(fileColumn));
                            if (fileInDb.length() >= file.length()) {
                                Hint.log(this, "File: " + fileInDb.getName() + " exists and is newer");
                                file.delete();
                                continue;
                            }
                            SessionPersistance.getInstance(context).deleteSession(id);
                        }
                    } finally {
                        if ((cursor != null) && !cursor.isClosed())
                            cursor.close();
                    }

                    try {
                        SessionWriter sessionWriter = new SessionWriter(context);
                        SessionReader reader = new SessionReader();
                        Hint.log(this, "Importing: " + file.getName());
                        SessionSummary summary = reader.readSummaryFromFile(file.getAbsolutePath());
                        if (summary != null) {
                            Hint.log(this, summary.toJson().toString());
                            long sessionId = sessionPersistance.addSession(summary);
                            sessionPersistance.close();
                            summary.setId((int) sessionId);
                            File destFilename = new File(destPath, file.getName());
                            summary.setFilename(destFilename.getAbsolutePath());
                            Hint.log(this, "Filename: " + destFilename.getAbsolutePath());
                            file.renameTo(destFilename);
                            // FileUtils.copy(file, destFilename);
                            sessionWriter.updateSession(summary);
                            // file.delete();
                        } else
                            throw new Exception("Summary == null");
                    } catch (Exception e) {
                        Hint.log(this, e);
                        file.renameTo(new File(failedPath, file.getName()));
                        ++failed;
                    }
                } finally {
                    ++num;
                    this.publishProgress((Void) null);
                }
            }
        }

        private void importRoutes() {
            File importPath = FileUtils.getExternalDir("PaceTracker", "Import/Routes");
            if (!importPath.exists())
                return;

            File failedPath = new File(importPath, "Failed");
            failedPath.mkdirs();

            destPath = FileUtils.getDir(context, "routes");

            final File[] files = importPath.listFiles();
            of = 0;
            num = 0;
            failed = 0;
            type = context.getString(R.string.routes);
            for (final File file : files) {
                if (file.getAbsolutePath().endsWith(".json"))
                    ++of;
            }

            SessionPersistance sessionPersistance = SessionPersistance.getInstance(context);
            String fields[] = {"_id", "filename"};
            for (final File file : files) {
                Hint.log(this, "file: " + file.getAbsolutePath());
                if (isCancelled())
                    break;

                if (!file.getAbsolutePath().endsWith(".json"))
                    continue;

                Cursor cursor = null;
                try {
                    try {
                        cursor = sessionPersistance.queryRoutes(fields, "filename like '%" + file.getName() + "%'",
                                null, "created asc", null);
                        if ((cursor != null) && cursor.moveToFirst()) {
                            int fileColumn = cursor.getColumnIndex("filename");
                            int idColumn = cursor.getColumnIndex("_id");
                            long id = cursor.getLong(idColumn);
                            File fileInDb = new File(cursor.getString(fileColumn));
                            if (fileInDb.length() >= file.length()) {
                                Hint.log(this, "File: " + fileInDb.getName() + " exists and is newer");
                                file.delete();
                                continue;
                            }
                            SessionPersistance.getInstance(context).deleteRoute(id);
                        }
                    } finally {
                        if ((cursor != null) && !cursor.isClosed())
                            cursor.close();
                    }

                    try {
                        Hint.log(this, "Importing: " + file.getName());
                        Route route = new Route(file);
                        if (route != null) {
                            sessionPersistance.addRoute(route);
                            sessionPersistance.close();
                            file.delete();
                        }
                    } catch (Exception e) {
                        Hint.log(this, e);
                        file.renameTo(new File(failedPath, file.getName()));
                        ++failed;
                    }
                } finally {
                    ++num;
                    this.publishProgress((Void) null);
                }
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            RefCountWakeLock.getInstance(context).acquire(this);
            importRoutes();
            importSessions();
            RefCountWakeLock.getInstance(context).release(this);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if (listener != null)
                listener.onImportFinished(this.isCancelled(), num, failed, exception);
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            if (listener != null)
                listener.onImport(num, of, type);
        }
    }

}
