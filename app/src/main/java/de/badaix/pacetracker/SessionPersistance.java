package de.badaix.pacetracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.text.TextUtils;
import android.text.format.Time;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Vector;

import de.badaix.pacetracker.goal.Goal;
import de.badaix.pacetracker.goal.GoalFactory;
import de.badaix.pacetracker.posprovider.PositionProviderFactory;
import de.badaix.pacetracker.session.Felt;
import de.badaix.pacetracker.session.GeoPos;
import de.badaix.pacetracker.session.Route;
import de.badaix.pacetracker.session.RouteInfo;
import de.badaix.pacetracker.session.Session;
import de.badaix.pacetracker.session.SessionFactory;
import de.badaix.pacetracker.session.SessionSummary;
import de.badaix.pacetracker.util.ExceptionHandler;
import de.badaix.pacetracker.util.FileUtils;
import de.badaix.pacetracker.util.Hint;

public class SessionPersistance extends SQLiteOpenHelper {
    final static String DB_NAME = "PaceTracker.db";
    final static String SESSION_TABLE = "overview";
    final static String ROUTE_TABLE = "route";
    final static int ID_IDX = 0;
    final static int VERSION_IDX = 1;
    final static int TYPE_IDX = 2;
    final static int POSPROVIDER_IDX = 3;
    final static int FILENAME_IDX = 4;
    final static int INIT_IDX = 5;
    final static int NOW_IDX = 6;
    final static int START_IDX = 7;
    final static int STOP_IDX = 8;
    final static int DESCRIPTION_IDX = 9;
    final static int COMMENT_IDX = 10;
    final static int AUTOSTART_IDX = 11;
    final static int AUTOPAUSE_IDX = 12;
    final static int DURATION_IDX = 13;
    final static int DISTANCE_IDX = 14;
    final static int CALORIES_IDX = 15;
    final static int AVG_SPEED_IDX = 16;
    final static int MAX_SPEED_IDX = 17;
    final static int MIN_HEIGHT_IDX = 18;
    final static int AVG_HEIGHT_IDX = 19;
    final static int MAX_HEIGHT_IDX = 20;
    final static int HEIGHT_PLUS_IDX = 21;
    final static int HEIGHT_MINUS_IDX = 22;
    final static int MAX_HR_IDX = 23;
    final static int MEAN_HR_IDX = 24;
    final static int MAX_CADENCE_IDX = 25;
    final static int MEAN_CADENCE_IDX = 26;
    final static int GOAL_IDX = 27;
    final static int GOAL_SETTINGS_IDX = 28;
    final static int ALT_CORRECTION_IDX = 29;
    final static int REFERENCE_FILE_IDX = 30;
    final static int FELT_IDX = 31;
    final static int LAT_START_IDX = 32;
    final static int LON_START_IDX = 33;
    final static int ROUTE_ID_IDX = 34;
    final static int FACEBOOK_ID_IDX = 35;
    final static int DAILYMILE_ID_IDX = 36;
    final static int GPLUS_ID_IDX = 37;
    final static int SUMMARY_IDX = 38;
    final static int COLUMN_COUNT = 39;
    private static final int DATABASE_VERSION = 19;
    private static SessionPersistance instance = null;
    private int[] summaryIdx = null;
    private Context context;
    // private Cursor cursor = null;
    private SQLiteDatabase db = null;

    private SessionPersistance(Context context) {
        super(context, DB_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    public static SessionPersistance getInstance(Context context) {
        if (instance == null) {
            instance = new SessionPersistance(context);
        }
        if (context != null)
            instance.context = context;

        return instance;
    }

    @Override
    protected void finalize() throws Throwable {
        closeDB();
        super.finalize(); // not necessary if extending Object.
    }

    public void test() {
        try {
            String fields[] = {"*"};
            Cursor cursor = querySessions(fields, "", null, "_id ASC", null);

            int versionColumn = cursor.getColumnIndex("version");
            int typeColumn = cursor.getColumnIndex("type");
            int providerColumn = cursor.getColumnIndex("posprovider");

            int fileColumn = cursor.getColumnIndex("filename");
            int startColumn = cursor.getColumnIndex("start");
            int stopColumn = cursor.getColumnIndex("stop");
            int descriptionColumn = cursor.getColumnIndex("description");

            int commentColumn = cursor.getColumnIndex("comment");
            int autostartColumn = cursor.getColumnIndex("autostart");
            int autopauseColumn = cursor.getColumnIndex("autopause");
            int durationColumn = cursor.getColumnIndex("duration");

            int distanceColumn = cursor.getColumnIndex("distance");
            int caloriesColumn = cursor.getColumnIndex("calories");
            int avgSpeedColumn = cursor.getColumnIndex("avgspeed");
            int maxSpeedColumn = cursor.getColumnIndex("maxspeed");

            int altCorrectionColumn = cursor.getColumnIndex("altcorrection");

            Vector<ContentValues> vContent = new Vector<ContentValues>();
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        ContentValues content = new ContentValues();
                        content.put("version", cursor.getInt(versionColumn));
                        content.put("type", cursor.getString(typeColumn));
                        content.put("posprovider", cursor.getString(providerColumn));

                        content.put("filename", cursor.getString(fileColumn));
                        content.put("init", cursor.getLong(startColumn));
                        content.put("now", cursor.getLong(startColumn));
                        content.put("start", cursor.getLong(startColumn));
                        content.put("stop", cursor.getLong(stopColumn));
                        content.put("description", cursor.getString(descriptionColumn));

                        content.put("comment", cursor.getString(commentColumn));
                        content.put("autostart", cursor.getInt(autostartColumn));
                        content.put("autopause", cursor.getInt(autopauseColumn));
                        content.put("duration", cursor.getLong(durationColumn));

                        content.put("distance", cursor.getDouble(distanceColumn));
                        content.put("calories", cursor.getInt(caloriesColumn));
                        content.put("avgspeed", cursor.getDouble(avgSpeedColumn));
                        content.put("maxspeed", cursor.getDouble(maxSpeedColumn));

                        content.put("altcorrection", cursor.getInt(altCorrectionColumn));
                        vContent.add(content);
                    } while (cursor.moveToNext());
                }
            }
            cursor.close();

            if (db.isOpen())
                db.close();

            db = getWritableDatabase();
            db.execSQL("DROP TABLE IF EXISTS TMP_TABLE");
            db.execSQL("DROP TABLE IF EXISTS NEW_TMP_TABLE");

            String sCreate = "CREATE TABLE IF NOT EXISTS TMP_TABLE";
            sCreate += "(_id             integer PRIMARY KEY,";
            sCreate += " version         integer NOT NULL,"; // /StartElement
            sCreate += " type            text NOT NULL,"; // /StartElement

            sCreate += " posprovider     text NOT NULL,"; // /StartElement
            sCreate += " filename        text,";

            sCreate += " init            integer NOT NULL,";
            sCreate += " now             integer,";
            sCreate += " start           integer,";
            sCreate += " stop            integer,";

            sCreate += " description     text,"; // /StartElement
            sCreate += " comment         text,"; // /StartElement

            sCreate += " autostart       integer,"; // /StartElement
            sCreate += " autopause       integer,"; // /StartElement

            sCreate += " duration        integer,";
            sCreate += " distance        real,";
            sCreate += " calories        integer,";

            sCreate += " avgspeed        real,";
            sCreate += " maxspeed        real,";

            sCreate += " minheight       real,";
            sCreate += " avgheight       real,";
            sCreate += " maxheight       real,";

            sCreate += " heightplus      real,";
            sCreate += " heightminus     real,";

            sCreate += " maxhr           integer,";
            sCreate += " meanhr          integer,";

            sCreate += " maxcadence      integer,";
            sCreate += " meancadence     integer,";

            sCreate += " modetype        integer,"; //
            sCreate += " modedescription text,"; //
            sCreate += " modesettings    text,"; //

            sCreate += " altcorrection   integer,"; // /StartElement
            sCreate += " referencefile   text);";

            Hint.log(this, sCreate);
            db.execSQL(sCreate);

            for (int i = 0; i < vContent.size(); ++i) {
                Hint.log(this, vContent.get(i).valueSet().toString());
                db.insert("TMP_TABLE", "altcorrection", vContent.get(i));
            }
            db.execSQL("ALTER TABLE " + SESSION_TABLE + " RENAME TO OLD_OVERVIEW");
            db.execSQL("ALTER TABLE TMP_TABLE RENAME TO " + SESSION_TABLE);
            db.close();
        } catch (Exception e) {
            ExceptionHandler.Handle("PaceTracker", e);
        }
    }

    public void closeDB() {
        if ((db != null) && db.isOpen())
            db.close();
        // if ((cursor != null) && !cursor.isClosed())
        // cursor.close();
    }

    // public static File backupDatabase(String filename) {
    // // test();
    //
    // if (TextUtils.isEmpty(filename))
    // filename = DB_NAME;
    //
    // File file =
    // FileUtils.getFilename(GlobalSettings.getInstance().getContext(),
    // filename, "backup");
    // if (file == null)
    // return null;
    //
    // try {
    // File data = Environment.getDataDirectory();
    // String currentDBPath = "/data/de.badaix.pacetracker/databases/" + DB_NAME;
    // File currentDB = new File(data, currentDBPath);
    //
    // if (currentDB.exists()) {
    // FileChannel src = new FileInputStream(currentDB).getChannel();
    // FileChannel dst = new FileOutputStream(file).getChannel();
    // dst.transferFrom(src, 0, src.size());
    // src.close();
    // dst.close();
    // }
    //
    // } catch (Exception e) {
    // return null;
    // }
    //
    // Hint.log("backupDatabase", "Backup to: " + file);
    // return file;
    // }
    //
    // public static void restoreDatabase(String filename) {
    // // test();
    //
    // if (TextUtils.isEmpty(filename))
    // filename = DB_NAME;
    //
    // File file =
    // FileUtils.getFilename(GlobalSettings.getInstance().getContext(),
    // filename, "backup");
    // if (file == null) {
    // Hint.log("restoreDatabase", "file == null");
    // return;
    // }
    //
    // try {
    // File data = Environment.getDataDirectory();
    // String currentDBPath = "databases/" + DB_NAME;
    // // File currentDB = new File(data, currentDBPath);
    //
    // if (data.exists()) {
    // FileChannel src = new FileInputStream(data).getChannel();
    // FileChannel dst =
    // GlobalSettings.getInstance().getContext().openFileOutput(currentDBPath,
    // Context.MODE_PRIVATE).getChannel();
    // dst.transferFrom(src, 0, src.size());
    // src.close();
    // dst.close();
    // }
    //
    // } catch (Exception e) {
    // Hint.log("", e.getMessage());
    // return;
    // }
    //
    // Hint.log("restoreDatabase", "restored");
    // }

    public int updateSession(long sessionId, SessionSummary session) {
        if (sessionId == -1)
            return 0;

        ContentValues content = new ContentValues();

        content.put("summary", session.toJson().toString());
        // Hint.log(this, "updateSession duration: " +
        // DateUtils.secondsToHHMMSSString(session.getDuration() / 1000));
        // Hint.log(this, "updateSession json: " + session.toJson().toString());

        if (session.getSessionStart() != null)
            content.put("start", session.getSessionStart().getTime());
        if (session.getSessionStop() != null)
            content.put("stop", session.getSessionStop().getTime());
        content.put("filename", session.getFilename());

        content.put("duration", session.getDuration());
        content.put("distance", session.getDistance());
        content.put("calories", session.getCalories());

        content.put("maxspeed", session.getMaxSpeed());
        content.put("description", session.getSettings().getDescription());
        content.put("comment", session.getSettings().getComment());

        content.put("avgspeed", session.getAvgSpeed());
        content.put("dailymile_id", session.getSettings().getDailyMileId());
        content.put("gplus_id", session.getSettings().getGPlusId());
        content.put("fb_id", session.getSettings().getFbId());

        if (session.getStartPos() != null) {
            content.put("lat_start", session.getStartPos().latitude);// .lat);
            content.put("lon_start", session.getStartPos().longitude);
        }

        if (session.hasHr() && (session.getHrMax() > 0)) {
            content.put("maxhr", session.getHrMax());
            content.put("meanhr", session.getHrMean());
        }

        if (session.getSettings().getGoal() != null) {
            content.put("goal", session.getSettings().getGoal().getType());
            content.put("goalsettings", session.getSettings().getGoal().toJson().toString());
        }

        if (session.getSettings().getFelt() != null)
            content.put("felt", session.getSettings().getFelt().toString());

        int result = -1;
        SQLiteDatabase db = getWritableDatabase();
        if (db != null) {
            result = db.update(SESSION_TABLE, content, "_id = " + sessionId, null);
            Hint.log(this, "Insert result: " + result);
            db.close();
        }
        return result;
    }

    private void initSummaryIdx(Cursor cursor) {
        summaryIdx = new int[COLUMN_COUNT];
        summaryIdx[ID_IDX] = cursor.getColumnIndex("_id");
        summaryIdx[TYPE_IDX] = cursor.getColumnIndex("type");
        summaryIdx[START_IDX] = cursor.getColumnIndex("start");
        summaryIdx[STOP_IDX] = cursor.getColumnIndex("stop");
        summaryIdx[FILENAME_IDX] = cursor.getColumnIndex("filename");
        summaryIdx[DURATION_IDX] = cursor.getColumnIndex("duration");
        summaryIdx[DISTANCE_IDX] = cursor.getColumnIndex("distance");
        summaryIdx[CALORIES_IDX] = cursor.getColumnIndex("calories");
        summaryIdx[MAX_SPEED_IDX] = cursor.getColumnIndex("maxspeed");
        summaryIdx[DESCRIPTION_IDX] = cursor.getColumnIndex("description");
        summaryIdx[COMMENT_IDX] = cursor.getColumnIndex("comment");
        summaryIdx[LAT_START_IDX] = cursor.getColumnIndex("lat_start");
        summaryIdx[LON_START_IDX] = cursor.getColumnIndex("lon_start");
        summaryIdx[DAILYMILE_ID_IDX] = cursor.getColumnIndex("dailymile_id");
        summaryIdx[GOAL_IDX] = cursor.getColumnIndex("goal");
        summaryIdx[FELT_IDX] = cursor.getColumnIndex("felt");
        summaryIdx[POSPROVIDER_IDX] = cursor.getColumnIndex("posprovider");
        summaryIdx[GOAL_SETTINGS_IDX] = cursor.getColumnIndex("goalsettings");
        summaryIdx[GPLUS_ID_IDX] = cursor.getColumnIndex("gplus_id");
        summaryIdx[FACEBOOK_ID_IDX] = cursor.getColumnIndex("fb_id");
        summaryIdx[SUMMARY_IDX] = cursor.getColumnIndex("summary");
    }

    public SessionSummary getSummary(Cursor cursor) {
        if (summaryIdx == null)
            initSummaryIdx(cursor);

        if (!cursor.isNull(summaryIdx[SUMMARY_IDX]) && !TextUtils.isEmpty(cursor.getString(summaryIdx[SUMMARY_IDX]))) {
            try {
                SessionSummary result = new SessionSummary(new JSONObject(cursor.getString(summaryIdx[SUMMARY_IDX])));
                result.setId(cursor.getInt(summaryIdx[ID_IDX]));
                // Hint.log(this, "getSummary duration: " +
                // DateUtils.secondsToHHMMSSString(result.getDuration() /
                // 1000));
                return result;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        String type = cursor.getString(summaryIdx[TYPE_IDX]);
        SessionSummary summary = new SessionSummary(type, SessionFactory.getInstance().getSessionNameFromType(type),
                SessionFactory.getInstance().getSessionVerbFromType(type), SessionFactory.getInstance()
                .getSessionDrawableFromType(type), SessionFactory.getInstance()
                .getSessionLightDrawableFromType(type));
        summary.setId(cursor.getInt(summaryIdx[ID_IDX]));
        summary.setCalories(cursor.getInt(summaryIdx[CALORIES_IDX]));
        summary.getSettings().setComment(cursor.getString(summaryIdx[COMMENT_IDX]));
        summary.getSettings().setDescription(cursor.getString(summaryIdx[DESCRIPTION_IDX]));
        summary.setDistance(cursor.getFloat(summaryIdx[DISTANCE_IDX]));
        summary.setSessionStart(new Date(cursor.getLong(summaryIdx[START_IDX])));
        summary.setSessionStop(new Date(cursor.getLong(summaryIdx[STOP_IDX])));
        summary.setFilename(cursor.getString(summaryIdx[FILENAME_IDX]));
        summary.setMaxSpeed(cursor.getFloat(summaryIdx[MAX_SPEED_IDX]));
        summary.setDuration(cursor.getLong(summaryIdx[DURATION_IDX]));

        // int routeId = -1;
        // if (!cursor.isNull(routeColumn)) {
        // routeId = cursor.getInt(routeColumn);
        // Hint.log(this, "routeId: " + routeId);
        // }

        int dailyMileId = -1;
        if (!cursor.isNull(summaryIdx[DAILYMILE_ID_IDX])) {
            dailyMileId = cursor.getInt(summaryIdx[DAILYMILE_ID_IDX]);
        }
        summary.getSettings().setDailyMileId(dailyMileId);

        int gPlusId = -1;
        if (!cursor.isNull(summaryIdx[GPLUS_ID_IDX])) {
            gPlusId = cursor.getInt(summaryIdx[GPLUS_ID_IDX]);
        }
        summary.getSettings().setGPlusId(gPlusId);

        String fbId = "";
        if (!cursor.isNull(summaryIdx[FACEBOOK_ID_IDX])) {
            fbId = cursor.getString(summaryIdx[FACEBOOK_ID_IDX]);
        }
        summary.getSettings().setFbId(fbId);

        if (!cursor.isNull(summaryIdx[LAT_START_IDX]) && !cursor.isNull(summaryIdx[LON_START_IDX])) {
            summary.setStartPos(new GeoPos(cursor.getFloat(summaryIdx[LAT_START_IDX]), cursor
                    .getFloat(summaryIdx[LON_START_IDX])));
            // Log.d("db", "lat/lon: " + lat + "/" + lon);
        } else {
            summary.setStartPos(null);
            // Log.d("db", "lat/lon: isNULL");
        }

        Goal goal = null;
        if (!cursor.isNull(summaryIdx[GOAL_IDX]) && !cursor.isNull(summaryIdx[GOAL_SETTINGS_IDX])) {
            goal = GoalFactory.getOfflineGoal(cursor.getString(summaryIdx[GOAL_IDX]), context);
            if (goal != null) {
                try {
                    goal.initFromJson(new JSONObject(cursor.getString(summaryIdx[GOAL_SETTINGS_IDX])));
                } catch (JSONException e) {
                    goal = null;
                }
            }
        }
        summary.getSettings().setGoal(goal);

        summary.getSettings().setFelt(null);
        if (!cursor.isNull(summaryIdx[FELT_IDX]) && !TextUtils.isEmpty(cursor.getString(summaryIdx[FELT_IDX]))) {
            summary.getSettings().setFelt(Felt.fromString(cursor.getString(summaryIdx[FELT_IDX])));
        }

        summary.getSettings().setPositionProvider(null);
        if (!cursor.isNull(summaryIdx[POSPROVIDER_IDX])
                && !TextUtils.isEmpty(cursor.getString(summaryIdx[POSPROVIDER_IDX]))) {
            summary.getSettings().setPositionProvider(
                    PositionProviderFactory.getOfflinePosProvider(context,
                            cursor.getString(summaryIdx[POSPROVIDER_IDX])));
        }

        return summary;
    }

    public int deleteSession(long sessionId) {
        String fields[] = {"_id", "filename"};
        Cursor cursor = null;

        try {
            cursor = querySessions(fields, "_id = " + sessionId, null, "", null);
            int fileColumn = cursor.getColumnIndex("filename");
            if ((cursor != null) && (cursor.moveToFirst())) {
                Hint.log(this, "Deleting file: " + cursor.getString(fileColumn));
                File file = new File(cursor.getString(fileColumn));
                file.delete();
            }
        } finally {
            if ((cursor != null) && !cursor.isClosed())
                cursor.close();
        }

        int result = -1;
        SQLiteDatabase db = null;
        try {
            db = getWritableDatabase();
            if (db != null)
                result = db.delete(SESSION_TABLE, "_id = " + sessionId, null);
        } finally {
            if ((db != null) && db.isOpen())
                db.close();
        }

        return result;
    }

    public long addManualSession(Session session) {
        ContentValues content = new ContentValues();
        content.put("version", 1);
        content.put("summary", session.toJson().toString());

        content.put("type", session.getType());

        content.put("posprovider", session.getSettings().getPositionProvider().getName());
        content.put("filename", "");

        content.put("now", (new Date()).getTime());
        content.put("init", (new Date()).getTime());
        content.put("start", session.getSessionStart().getTime());
        content.put("stop", session.getSessionStop().getTime());

        content.put("comment", session.getSettings().getComment());
        content.put("description", session.getSettings().getDescription());

        content.put("autopause", false);
        content.put("autostart", false);

        content.put("duration", session.getDuration());
        content.put("distance", session.getDistance());
        content.put("calories", session.getCalories());

        // /minheight
        // /avgheight
        // /maxheight

        // /heightplus
        // /heightminus

        // /maxhr
        // /meanhr

        // /maxcadence
        // /meancadence

        // /goal
        // /goalsettings
        content.put("altcorrection", 0);
        // referencefile

        if (session.getSettings().getFelt() != null)
            content.put("felt", session.getSettings().getFelt().toString());
        // /lat_start
        // /lon_start

        // /route_id
        // /facebook_id
        // /dailymile_id

        long result = getWritableDatabase().insert(SESSION_TABLE, "altcorrection", content);
        Hint.log(this, "Insert result: " + result);
        return result;
    }

    public long addSession(SessionSummary session)// , String filename)
    {
        ContentValues content = new ContentValues();
        content.put("version", 1);
        content.put("summary", session.toJson().toString());
        // Hint.log(this, "addSession duration: " +
        // DateUtils.secondsToHHMMSSString(session.getDuration() / 1000));
        // Hint.log(this, "addSession json: " + session.toJson().toString());

        content.put("type", session.getType());

        content.put("posprovider", session.getSettings().getPositionProvider().getName());
        content.put("filename", session.getFilename());

        content.put("now", (new Date()).getTime());
        content.put("init", (new Date()).getTime());
        content.put("start", (new Date()).getTime());
        // /---stop

        // /---comment
        content.put("description", session.getSettings().getDescription());

        content.put("autopause", session.getSettings().isAutoPause());
        content.put("autostart", session.getSettings().isAutoStart());
        // TODO replace route_id with goal
        content.put("route_id", -1);// session.getRouteId());

        // /---duration
        // /---distance
        // /---calories

        // /---avgspeed
        // /---maxspeed

        // /minheight
        // /avgheight
        // /maxheight

        // /heightplus
        // /heightminus

        // /maxhr
        // /meanhr

        // /maxcadence
        // /meancadence

        if (session.getSettings().getGoal() != null) {
            content.put("goal", session.getSettings().getGoal().getType());
            content.put("goalsettings", session.getSettings().getGoal().toJson().toString());
        }

        content.put("altcorrection", 0);
        // /referencefile

        // /strreserved1
        // /strreserved2
        // /strreserved3
        // /intreserved1
        // /intreserved2
        // /intreserved3
        // /floatreserved1
        // /floatreserved2
        // /floatreserved3

        long result = getWritableDatabase().insert(SESSION_TABLE, "altcorrection", content);
        Hint.log(this, "Insert result: " + result);
        session.setId((int) result);

        return result;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sCreate = "CREATE TABLE IF NOT EXISTS " + SESSION_TABLE;
        sCreate += "(_id             integer PRIMARY KEY,";
        sCreate += " version         integer NOT NULL,"; // /StartElement
        sCreate += " type            text NOT NULL,"; // /StartElement

        sCreate += " posprovider     text NOT NULL,"; // /StartElement
        sCreate += " filename        text,";

        sCreate += " init            integer NOT NULL,";
        sCreate += " now             integer,";
        sCreate += " start           integer,";
        sCreate += " stop            integer,";

        sCreate += " description     text,"; // /StartElement
        sCreate += " comment         text,"; // /StartElement

        sCreate += " autostart       integer,"; // /StartElement
        sCreate += " autopause       integer,"; // /StartElement

        sCreate += " duration        integer,";
        sCreate += " distance        real,";
        sCreate += " calories        integer,";

        sCreate += " avgspeed        real,";
        sCreate += " maxspeed        real,";

        sCreate += " minheight       real,";
        sCreate += " avgheight       real,";
        sCreate += " maxheight       real,";

        sCreate += " heightplus      real,";
        sCreate += " heightminus     real,";

        sCreate += " maxhr           integer,";
        sCreate += " meanhr          integer,";

        sCreate += " maxcadence      integer,";
        sCreate += " meancadence     integer,";

        // sCreate += " modetype        integer,"; //
        sCreate += " goal            text,";
        sCreate += " goalsettings    text,";

        sCreate += " altcorrection   integer,"; // /StartElement
        sCreate += " referencefile   text,";

        sCreate += " felt            string,";
        sCreate += " lat_start       real,"; // /StartElement
        sCreate += " lon_start       real,"; // /StartElement

        sCreate += " route_id        integer,";

        sCreate += " summary         text,";
        sCreate += " gplus_id        integer,";
        sCreate += " fb_id           text DEFAULT '',";
        sCreate += " dailymile_id    integer);";

        Hint.log(this, sCreate);
        db.execSQL(sCreate);

        sCreate = "CREATE TABLE IF NOT EXISTS " + ROUTE_TABLE;
        sCreate += "(_id             integer PRIMARY KEY,";
        sCreate += " version         integer NOT NULL,";
        sCreate += " name            string NOT NULL,";
        sCreate += " filename        string NOT NULL,";
        sCreate += " deleted         integer,";
        sCreate += " copyright       string,";
        sCreate += " source          string,";
        sCreate += " description     string,";
        sCreate += " type            string,";
        sCreate += " from_str        string,";
        sCreate += " to_str          string,";
        sCreate += " created         integer,";
        sCreate += " lat_start       real,";
        sCreate += " lon_start       real,";
        sCreate += " lat_end         real,";
        sCreate += " lon_end         real,";
        sCreate += " distance        real,";
        sCreate += " thumbnail       blob);";

        Hint.log(this, sCreate);
        db.execSQL(sCreate);
    }

    public RouteInfo getRoute(long routeId) {
        RouteInfo routeInfo = null;
        Cursor cursor = null;
        try {
            String fields[] = {"*"};
            cursor = queryRoutes(fields, "_id = " + routeId, null, "", null);
            if (cursor.moveToFirst())
                routeInfo = getRoute(cursor);
        } finally {
            if ((cursor != null) && !cursor.isClosed())
                cursor.close();
        }
        return routeInfo;
    }

    public RouteInfo getRoute(Cursor cursor) {
        if (cursor == null)
            return null;

        RouteInfo routeInfo = null;
        int idColumn = cursor.getColumnIndex("_id");
        int nameColumn = cursor.getColumnIndex("name");
        int copyrightColumn = cursor.getColumnIndex("copyright");
        int sourceColumn = cursor.getColumnIndex("source");
        int distanceColumn = cursor.getColumnIndex("distance");
        int descriptionColumn = cursor.getColumnIndex("description");
        int filenameColumn = cursor.getColumnIndex("filename");
        int typeColumn = cursor.getColumnIndex("type");
        int fromColumn = cursor.getColumnIndex("from_str");
        int toColumn = cursor.getColumnIndex("to_str");
        int created = cursor.getColumnIndex("created");
        int latStart = cursor.getColumnIndex("lat_start");
        int lonStart = cursor.getColumnIndex("lon_start");
        int latEnd = cursor.getColumnIndex("lat_end");
        int lonEnd = cursor.getColumnIndex("lon_end");
        routeInfo = new RouteInfo();
        routeInfo.setId(cursor.getInt(idColumn));
        routeInfo.setCopyright(cursor.getString(copyrightColumn));
        routeInfo.setName(cursor.getString(nameColumn));
        routeInfo.setSource(cursor.getString(sourceColumn));
        routeInfo.setDistance(cursor.getDouble(distanceColumn));
        routeInfo.setDescription(cursor.getString(descriptionColumn));
        routeInfo.setFilename(cursor.getString(filenameColumn));
        routeInfo.setType(cursor.getString(typeColumn));
        routeInfo.setFrom(cursor.getString(fromColumn));
        routeInfo.setTo(cursor.getString(toColumn));
        routeInfo.setCreated(new Date(cursor.getLong(created)));
        routeInfo.setStartPos(new GeoPos(cursor.getDouble(latStart), cursor.getDouble(lonStart)));
        routeInfo.setEndPos(new GeoPos(cursor.getDouble(latEnd), cursor.getDouble(lonEnd)));
        return routeInfo;
    }

    public String MD5(String md5) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(md5.getBytes());
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < array.length; ++i) {
                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
        }
        return null;
    }

    public long addRoute(Route route) throws IOException {
        Time time = new Time();
        time.setToNow();
        File file = null;
        if (!TextUtils.isEmpty(route.getFilename()))
            file = FileUtils.getFilename(this.context, new File(route.getFilename()).getName(), "routes");
        else
            file = FileUtils.getFilename(this.context, time.format2445() + "_" + MD5(route.toJson().toString())
                    + ".json", "routes");
        Hint.log(this, "Route filename: " + file.getAbsolutePath());
        route.setFilename(file.getAbsolutePath());
        route.saveToFile(file);
        // RoutePersistance routePersistance = new RoutePersistance();
        // Hint.log(this, "storing route: " + file.getAbsolutePath());
        // routePersistance.saveRoute(dos, route);

        ContentValues content = new ContentValues();
        content.put("version", route.getVersion());
        content.put("name", route.getName());
        content.put("copyright", route.getCopyright());
        content.put("source", route.getSource());
        content.put("distance", route.getDistance());
        content.put("description", route.getDescription());
        content.put("filename", file.getAbsolutePath());
        content.put("type", route.getType());
        content.put("from_str", route.getFrom());
        content.put("to_str", route.getTo());
        content.put("created", route.getCreated().getTime());
        content.put("lat_start", route.getStartPos().latitude);
        content.put("lon_start", route.getStartPos().longitude);
        content.put("lat_end", route.getEndPos().latitude);
        content.put("lon_end", route.getEndPos().longitude);
        content.put("deleted", 0);

        long result = getWritableDatabase().insert(ROUTE_TABLE, null, content);
        route.setId(result);
        Hint.log(this, "Insert result: " + result);
        return result;
    }

    public int deleteRoute(long routeId) {
        String fields[] = {"_id", "filename"};
        Cursor cursor = null;
        try {
            cursor = queryRoutes(fields, "_id = " + routeId, null, "", null);
            if ((cursor != null) && (cursor.moveToFirst())) {
                int fileColumn = cursor.getColumnIndex("filename");
                Hint.log(this, "Deleting file: " + cursor.getString(fileColumn));
                File file = new File(cursor.getString(fileColumn));
                file.delete();
            }
        } finally {
            if ((cursor != null) && !cursor.isClosed())
                cursor.close();
        }

        int result = -1;
        SQLiteDatabase db = null;
        try {
            db = getWritableDatabase();
            if (db != null)
                result = db.delete(ROUTE_TABLE, "_id = " + routeId, null);
        } finally {
            if ((db != null) && db.isOpen())
                db.close();
        }

        return result;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion <= 17) {
            String sCreate = "ALTER TABLE " + SESSION_TABLE + " ADD summary text;";
            db.execSQL(sCreate);
            sCreate = "ALTER TABLE " + SESSION_TABLE + " ADD gplus_id integer;";
            db.execSQL(sCreate);
        }
        if (oldVersion <= 18) {
            String sCreate = "ALTER TABLE " + SESSION_TABLE + " ADD fb_id text DEFAULT NULL;";
            db.execSQL(sCreate);
        }
    }

    public Cursor querySessions(String[] projection, String selection, String[] selectionArgs, String sortOrder,
                                String groupBy) {
        // If no sort order is specified, uses the default
        if (TextUtils.isEmpty(sortOrder)) {
            sortOrder = "start DESC";
        }

        return query(SESSION_TABLE, projection, selection, selectionArgs, sortOrder, groupBy);
    }

    public Cursor queryRoutes(String[] projection, String selection, String[] selectionArgs, String sortOrder,
                              String groupBy) {

        return query(ROUTE_TABLE, projection, selection, selectionArgs, sortOrder, groupBy);
    }

    private Cursor query(String table, String[] projection, String selection, String[] selectionArgs, String sortOrder,
                         String groupBy) {
        // Constructs a new query builder and sets its table name
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(table);

        Hint.log(this, "Query on " + table + ", SortOrder: " + sortOrder);
        if (TextUtils.isEmpty(selection))
            selection = null;

        // Opens the database object in "read" mode, since no writes need to be
        // done.
        // closeDB();
        if ((db == null) || !db.isOpen())
            db = getReadableDatabase();

		/*
         * Performs the query. If no problems occur trying to read the database,
		 * then a Cursor object is returned; otherwise, the cursor variable
		 * contains null. If no records were selected, then the Cursor object is
		 * empty, and Cursor.getCount() returns 0.
		 */
        Cursor c = qb.query(db, // The database to query
                projection, // The columns to return from the query
                selection, // The columns for the where clause
                selectionArgs, // The values for the where clause
                groupBy, // don't group the rows
                null, // don't filter by row groups
                sortOrder // The sort order
        );

        // cursor = c;
        return c;
    }

}
