package de.badaix.pacetracker.session;

import android.content.Context;
import android.location.Location;
import android.text.format.Time;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import de.badaix.pacetracker.SessionPersistance;
import de.badaix.pacetracker.activity.SessionUI;
import de.badaix.pacetracker.sensor.SensorData;
import de.badaix.pacetracker.sensor.SensorProvider;
import de.badaix.pacetracker.sensor.SensorProvider.SensorState;
import de.badaix.pacetracker.session.Session.State;
import de.badaix.pacetracker.util.DateUtils;
import de.badaix.pacetracker.util.FileUtils;
import de.badaix.pacetracker.util.Hint;

enum MsgType {
    EVENT(0), POS(1), START(2), GPS(3), COMMAND(4), END(5), FILTERED_POS(6), STOP(7), SUMMARY_OLD(8), HXM(9), SENSORDATA(
            10), STATE(11), SUMMARY(12);

    private int type;

    MsgType(int type) {
        this.type = type;
    }

    static MsgType fromInt(int type) {
        switch (type) {
            case 0:
                return EVENT;
            case 1:
                return POS;
            case 2:
                return START;
            case 3:
                return GPS;
            case 4:
                return COMMAND;
            case 5:
                return END;
            case 6:
                return FILTERED_POS;
            case 7:
                return STOP;
            case 8:
                return SUMMARY_OLD;
            case 9:
                return HXM;
            case 10:
                return SENSORDATA;
            case 11:
                return STATE;
            case 12:
                return SUMMARY;
        }
        throw new IllegalArgumentException(Integer.toString(type));
    }

    int toInt() {
        return type;
    }
}

class SessionHeader {
    int msgType;
    long now;
    long duration;
    double distance;
}

class SessionHeaderV1 extends SessionHeader {
    static byte magicNum = 101;
    int len = -1;
}

/*
 * public static final int MSG_TYPE_EVENT = 0; public static final int
 * MSG_TYPE_POS = 1; public static final int MSG_TYPE_START = 2; public static
 * final int MSG_TYPE_GPS = 3; public static final int MSG_TYPE_COMMAND = 4;
 * public static final int MSG_TYPE_END = 5; public static final int
 * MSG_TYPE_FILTERED_POS = 6; public static final int MSG_TYPE_STOP = 7;
 */

abstract class SessionElement {
    protected SessionHeader header;
    protected ByteArrayOutputStream byteArrayOutputStream;
    protected DataOutputStream stream;

    public SessionElement() {
        byteArrayOutputStream = new ByteArrayOutputStream();
        stream = new DataOutputStream(byteArrayOutputStream);
    }

    abstract protected void write(DataOutputStream dos, SessionSummary session) throws IOException;

    abstract public void read(DataInputStream dis, SessionHeader header) throws IOException;

    abstract public MsgType getType();

    protected boolean writeElement(DataOutputStream dos, SessionSummary session) {
        if (dos == null)
            return false;
        try {
            synchronized (dos) {
                dos.writeByte(SessionHeaderV1.magicNum);
                dos.writeInt(byteArrayOutputStream.size());
                dos.writeInt(getType().toInt());
                dos.writeLong((new Date()).getTime());
                dos.writeLong(session.getDuration());
                dos.writeDouble(session.getDistance());
                dos.write(byteArrayOutputStream.toByteArray());
                byteArrayOutputStream.reset();
            }
            return true;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            Hint.log(this, "Exception: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public String toString() {
        Time time = new Time();
        time.set(header.now);
        return time.format2445() + "; " + getType().toString() + "; Duration: " + header.duration / 1000.
                + "; Distance: " + header.distance + ";";
    }
}

class SummaryElement extends SessionElement {
    private SessionSummary summary;
    private JSONObject json = null;

    @Override
    protected void write(DataOutputStream dos, SessionSummary session) throws IOException {
        stream.writeUTF(summary.toJson().toString());
        writeElement(dos, session);
    }

    @Override
    public void read(DataInputStream dis, SessionHeader header) throws IOException {
        this.header = header;
        try {
            json = new JSONObject(dis.readUTF());
            summary = null;
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public JSONObject getJson() {
        return json;
    }

    public SessionSummary getSessionSummary() {
        try {
            if (summary == null)
                summary = new SessionSummary(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return summary;
    }

    public void write(DataOutputStream dos, SessionSummary session, SessionSummary summary) throws IOException {
        this.summary = summary;
        write(dos, session);
    }

    @Override
    public MsgType getType() {
        return MsgType.SUMMARY;
    }

    @Override
    public String toString() {
        return super.toString() + " Summary: " + summary.toJson().toString() + ";";
    }
}

class EventElement extends SessionElement {
    private int event;
    private String description;

    @Override
    protected void write(DataOutputStream dos, SessionSummary session) throws IOException {
        stream.writeInt(event);
        stream.writeUTF(description);
        writeElement(dos, session);
    }

    @Override
    public void read(DataInputStream dis, SessionHeader header) throws IOException {
        this.header = header;
        event = dis.readInt();
        description = dis.readUTF();
    }

    public int getEvent() {
        return event;
    }

    public String getDescription() {
        return description;
    }

    public void write(DataOutputStream dos, SessionSummary session, int event, String description) throws IOException {
        this.event = event;
        this.description = description;
        write(dos, session);
    }

    @Override
    public MsgType getType() {
        return MsgType.EVENT;
    }

    @Override
    public String toString() {
        return super.toString() + " Event: " + event + "; Description: " + description + ";";
    }
}

class StateElement extends SessionElement {
    private Session.State oldState;
    private Session.State newState;

    @Override
    protected void write(DataOutputStream dos, SessionSummary session) throws IOException {
        stream.writeInt(oldState.asInt());
        stream.writeInt(newState.asInt());
        writeElement(dos, session);
    }

    @Override
    public void read(DataInputStream dis, SessionHeader header) throws IOException {
        this.header = header;
        oldState = State.fromInt(dis.readInt());
        newState = State.fromInt(dis.readInt());
    }

    public State getOldState() {
        return oldState;
    }

    public State getNewState() {
        return newState;
    }

    public void write(DataOutputStream dos, SessionSummary session, State oldState, State newState) throws IOException {
        this.oldState = oldState;
        this.newState = newState;
        write(dos, session);
    }

    @Override
    public MsgType getType() {
        return MsgType.STATE;
    }

    @Override
    public String toString() {
        return super.toString() + " OldState: " + oldState + "; NewState: " + newState + ";";
    }
}

class HxmElement extends SessionElement {
    private HxmData hxmData;

    @Override
    protected void write(DataOutputStream dos, SessionSummary session) throws IOException {
        stream.writeLong(hxmData.time);
        stream.writeShort(hxmData.heartRate);
        stream.writeShort(hxmData.cadence);
        writeElement(dos, session);
    }

    @Override
    public void read(DataInputStream dis, SessionHeader header) throws IOException {
        this.header = header;
        hxmData = new HxmData(new SensorData(), header.duration, header.distance);
        hxmData.time = dis.readLong();
        hxmData.heartRate = dis.readShort();
        hxmData.cadence = dis.readShort();
    }

    public HxmData getHxmData() {
        return hxmData;
    }

    public void write(DataOutputStream dos, SessionSummary session, HxmData hxmData) throws IOException {
        this.hxmData = hxmData;
        write(dos, session);
    }

    @Override
    public MsgType getType() {
        return MsgType.HXM;
    }

    @Override
    public String toString() {
        return super.toString() + " HR: " + hxmData.heartRate + "; Cadence: " + hxmData.cadence + ";";
    }
}

class SensorDataElement extends SessionElement {
    private SensorData sensorData;

    @Override
    protected void write(DataOutputStream dos, SessionSummary session) throws IOException {
        stream.writeLong(sensorData.getCreationTime().getTime());
        stream.writeShort(sensorData.getHeartRate());
        stream.writeShort(sensorData.getPower());
        stream.writeShort(sensorData.getCadence());
        stream.writeFloat(sensorData.getBatteryLevel());
        writeElement(dos, session);
    }

    @Override
    public void read(DataInputStream dis, SessionHeader header) throws IOException {
        this.header = header;
        sensorData = new SensorData();
        sensorData.setCreationTime(new Date(dis.readLong())).setHeartRate(dis.readShort()).setPower(dis.readShort())
                .setCadence(dis.readShort()).setBatteryLevel(dis.readFloat());
    }

    public SensorData getSensorData() {
        return sensorData;
    }

    public void write(DataOutputStream dos, SessionSummary session, SensorData sensorData) throws IOException {
        this.sensorData = sensorData;
        write(dos, session);
    }

    @Override
    public MsgType getType() {
        return MsgType.SENSORDATA;
    }

    @Override
    public String toString() {
        return super.toString() + " HR: " + sensorData.getHeartRate() + "; Cadence: " + sensorData.getCadence()
                + "; Battery: " + sensorData.getBatteryLevel();
    }
}

class CommandElement extends SessionElement {
    private int command;

    public int getCommand() {
        return command;
    }

    @Override
    protected void write(DataOutputStream dos, SessionSummary session) throws IOException {
        stream.writeInt(command);
        writeElement(dos, session);
    }

    @Override
    public void read(DataInputStream dis, SessionHeader header) throws IOException {
        this.header = header;
        command = dis.readInt();
    }

    public void write(DataOutputStream dos, SessionSummary session, int command) throws IOException {
        this.command = command;
        write(dos, session);
    }

    @Override
    public MsgType getType() {
        return MsgType.COMMAND;
    }

    @Override
    public String toString() {
        return super.toString() + " Command: " + command + ";";
    }
}

class GpsElement extends SessionElement {
    private boolean hasFix;
    private int fixCount;
    private int satCount;
    private boolean active;

    public boolean hasFix() {
        return hasFix;
    }

    public int getFixCount() {
        return fixCount;
    }

    public int getSatCount() {
        return satCount;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    protected void write(DataOutputStream dos, SessionSummary session) throws IOException {
        stream.writeBoolean(hasFix);
        stream.writeInt(fixCount);
        stream.writeInt(satCount);
        stream.writeBoolean(active);
        writeElement(dos, session);
    }

    @Override
    public void read(DataInputStream dis, SessionHeader header) throws IOException {
        this.header = header;
        hasFix = dis.readBoolean();
        fixCount = dis.readInt();
        satCount = dis.readInt();
        active = dis.readBoolean();
    }

    public void write(DataOutputStream dos, SessionSummary session, boolean hasFix, int fixCount, int satCount,
                      boolean active) throws IOException {
        this.hasFix = hasFix;
        this.fixCount = fixCount;
        this.satCount = satCount;
        this.active = active;
        write(dos, session);
    }

    @Override
    public MsgType getType() {
        return MsgType.GPS;
    }

    @Override
    public String toString() {
        return super.toString() + " Active: " + active + "; HasFix: " + hasFix + "; FixCount: " + fixCount
                + "; SatCount: " + satCount + ";";
    }
}

class StartElement extends SessionElement {
    private int fileVersion;
    private String type;
    private String description;
    private String comment;
    private String providerName;
    private boolean autoStart;
    private boolean autoPause;
    private int altCorrection;

    public int getFileVersion() {
        return fileVersion;
    }

    public String getName() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public String getComment() {
        return comment;
    }

    public String getProviderName() {
        return providerName;
    }

    public int getAltCorrection() {
        return altCorrection;
    }

    public boolean isAutoStart() {
        return autoStart;
    }

    public boolean isAutoPause() {
        return autoPause;
    }

    @Override
    protected void write(DataOutputStream dos, SessionSummary session) throws IOException {
        stream.writeInt(fileVersion);
        stream.writeUTF(type);
        stream.writeUTF(description);
        stream.writeUTF(comment);
        stream.writeUTF(providerName);
        stream.writeInt(altCorrection);
        stream.writeBoolean(autoStart);
        stream.writeBoolean(autoPause);
        writeElement(dos, session);
    }

    @Override
    public void read(DataInputStream dis, SessionHeader header) throws IOException {
        this.header = header;
        fileVersion = dis.readInt();
        type = dis.readUTF();
        description = dis.readUTF();
        comment = dis.readUTF();
        providerName = dis.readUTF();
        altCorrection = dis.readInt();
        autoStart = dis.readBoolean();
        autoPause = dis.readBoolean();
    }

    public void write(DataOutputStream dos, SessionSummary session, int fileVersion, String type, String description,
                      String comment, String providerName, int altCorrection, boolean autoStart, boolean autoPause)
            throws IOException {
        this.fileVersion = fileVersion;
        this.type = type;
        this.description = description;
        this.comment = comment;
        this.providerName = providerName;
        this.altCorrection = altCorrection;
        this.autoStart = autoStart;
        this.autoPause = autoPause;
        write(dos, session);
    }

    @Override
    public MsgType getType() {
        return MsgType.START;
    }

    @Override
    public String toString() {
        return super.toString() + " FileVersion: " + fileVersion + "; Type: " + type + "; Description: " + description
                + "; Comment: " + comment + "; ProviderName: " + providerName + "; AltCorrection: " + altCorrection
                + "; AutoStart: " + autoStart + "; AutoPause: " + autoPause + ";";
    }
}

class SummaryOldElement extends SessionElement {
    private String type;
    private long start;
    private long stop;
    private long duration;
    private double distance;
    private int calories;
    private float maxSpeed;
    private String description;
    private String comment;
    private double latStart;
    private double lonStart;
    private int dailyMileId;
    private String goal;
    private String goalSettings;
    private String felt;
    private String providerName;

    public long getStart() {
        return start;
    }

    public long getStop() {
        return stop;
    }

    public long getDuration() {
        return duration;
    }

    public double getDistance() {
        return distance;
    }

    public int getCalories() {
        return calories;
    }

    public float getMaxSpeed() {
        return maxSpeed;
    }

    public String getDescription() {
        return description;
    }

    public String getComment() {
        return comment;
    }

    public double getLatStart() {
        return latStart;
    }

    public double getLonStart() {
        return lonStart;
    }

    public int getDailyMileId() {
        return dailyMileId;
    }

    public String getGoal() {
        return goal;
    }

    public String getFelt() {
        return felt;
    }

    public String getProviderName() {
        return providerName;
    }

    public String getGoalSettings() {
        return goalSettings;
    }

    @Override
    protected void write(DataOutputStream dos, SessionSummary session) throws IOException {
        stream.writeUTF(type);
        stream.writeLong(start);
        stream.writeLong(stop);
        stream.writeLong(duration);
        stream.writeDouble(distance);
        stream.writeInt(calories);
        stream.writeFloat(maxSpeed);
        stream.writeUTF(description);
        stream.writeUTF(comment);
        stream.writeDouble(latStart);
        stream.writeDouble(lonStart);
        stream.writeInt(dailyMileId);
        stream.writeUTF(goal);
        stream.writeUTF(goalSettings);
        stream.writeUTF(felt);
        stream.writeUTF(providerName);
        writeElement(dos, session);
    }

    @Override
    public void read(DataInputStream dis, SessionHeader header) throws IOException {
        this.header = header;
        type = dis.readUTF();
        start = dis.readLong();
        stop = dis.readLong();
        duration = dis.readLong();
        distance = dis.readDouble();
        calories = dis.readInt();
        maxSpeed = dis.readFloat();
        description = dis.readUTF();
        comment = dis.readUTF();
        latStart = dis.readDouble();
        lonStart = dis.readDouble();
        dailyMileId = dis.readInt();
        goal = dis.readUTF();
        goalSettings = dis.readUTF();
        felt = dis.readUTF();
        providerName = dis.readUTF();
    }

    public void write(DataOutputStream dos, SessionSummary session, SessionSummary summary) throws IOException {
        this.type = summary.getType();
        if (summary.getSessionStart() != null)
            this.start = summary.getSessionStart().getTime();
        else
            this.start = new Date().getTime();
        if (summary.getSessionStart() != null)
            this.stop = summary.getSessionStop().getTime();
        else
            this.stop = new Date().getTime();
        this.duration = summary.getDuration();
        this.distance = summary.getDistance();
        this.calories = summary.getCalories();
        this.maxSpeed = summary.getMaxSpeed();
        this.description = summary.getSettings().getDescription();
        this.comment = summary.getSettings().getComment();
        if (summary.getStartPos() != null) {
            this.latStart = summary.getStartPos().latitude;
            this.lonStart = summary.getStartPos().longitude;
        } else {
            this.latStart = -1;
            this.lonStart = -1;
        }
        this.dailyMileId = summary.getSettings().getDailyMileId();
        if (summary.getSettings().getGoal() != null) {
            this.goal = summary.getSettings().getGoal().getType();
            this.goalSettings = summary.getSettings().getGoal().toJson().toString();
        } else {
            this.goal = "";
            this.goalSettings = "";
        }

        if (summary.getSettings().getFelt() != null) {
            this.felt = summary.getSettings().getFelt().toString();
        } else {
            this.felt = "";
        }

        this.providerName = summary.getSettings().getPositionProvider().getName();
        write(dos, session);
    }

    @Override
    public MsgType getType() {
        return MsgType.SUMMARY_OLD;
    }

    @Override
    public String toString() {
        return super.toString();// + " FileVersion: " + fileVersion + "; Type: "
        // + type + "; Description: " + description +
        // "; Comment: " + comment + "; ProviderName: "
        // + providerName + "; AltCorrection: " +
        // altCorrection + "; AutoStart: " + autoStart +
        // "; AutoPause: " + autoPause + ";";
    }
}

class PosElement extends SessionElement {
    protected Location location;

    @Override
    protected void write(DataOutputStream dos, SessionSummary session) throws IOException {
        stream.writeLong(location.getTime());
        stream.writeDouble(location.getLatitude());
        stream.writeDouble(location.getLongitude());
        stream.writeDouble(location.getAltitude());
        stream.writeBoolean(location.hasBearing());
        stream.writeFloat(location.getBearing());
        stream.writeBoolean(location.hasSpeed());
        stream.writeFloat(location.getSpeed());
        writeElement(dos, session);
    }

    public Location getLocation() {
        return location;
    }

    @Override
    public void read(DataInputStream dis, SessionHeader header) throws IOException {
        this.header = header;
        location = new Location("");
        location.setTime(dis.readLong());
        location.setLatitude(dis.readDouble());
        location.setLongitude(dis.readDouble());
        location.setAltitude(dis.readDouble());
        boolean hasBearing = dis.readBoolean();
        float bearing = dis.readFloat();
        if (hasBearing)
            location.setBearing(bearing);
        boolean hasSpeed = dis.readBoolean();
        float speed = dis.readFloat();
        if (hasSpeed)
            location.setSpeed(speed);
    }

    /*
     * public GpsPos getGpsPos() { return new GpsPos(latitude, longitude,
     * altitude, time, speed, bearing, header.duration, header.distance); }
     */
    public void write(DataOutputStream dos, SessionSummary session, Location location) throws IOException {
        this.location = location;
        write(dos, session);
    }

    @Override
    public MsgType getType() {
        return MsgType.POS;
    }

    @Override
    public String toString() {
        return super.toString() + " Time: " + location.getTime() + "; Longitude: " + location.getLongitude()
                + "; Latitude: " + location.getLatitude() + "; Altitude: " + location.getAltitude() + "; HasBearing: "
                + location.hasBearing() + "; Bearing: " + location.getBearing() + "; HasSpeed: " + location.hasSpeed()
                + "; Speed: " + location.getSpeed() + ";";
    }

}

class FilteredPosElement extends PosElement {
    private static Location location = new Location("");

    @Override
    public MsgType getType() {
        return MsgType.FILTERED_POS;
    }

    public void write(DataOutputStream dos, SessionSummary session, GpsPos pos) throws IOException {
        location.setTime(pos.time);
        location.setLatitude(pos.latitude);
        location.setLongitude(pos.longitude);
        location.setAltitude(pos.altitude);
        location.setBearing(pos.bearing);
        location.setSpeed(pos.speed);
        this.write(dos, session, location);
    }
}

public class SessionWriter implements SessionUI {
    private Session session;
    private int fileVersion = 2;
    private SessionPersistance persistance;
    private Time time;
    private DataOutputStream dos;
    // private EventElement eventElement;
    private StateElement stateElement;
    private PosElement posElement;
    private FilteredPosElement filteredPosElement;
    private StartElement startElement;
    private GpsElement gpsElement;
    private CommandElement commandElement;
    // private SummaryOldElement summaryOldElement;
    private SummaryElement summaryElement;
    private HxmElement hxmElement;
    private SensorDataElement sensorDataElement;
    private long sessionId;
    private Context context;

    public SessionWriter(Context context) {
        persistance = SessionPersistance.getInstance(context);
        time = new Time();
        dos = null;
        this.context = context;
    }

    @Override
    public void onStateChanged(Session.State oldState, Session.State newState) {
        try {
            stateElement.write(dos, session, oldState, newState);
            if (oldState == State.INIT)
                persistance.updateSession(sessionId, session);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        try {
            posElement.write(dos, session, location);
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    @Override
    public void onFilteredLocationChanged(GpsPos location) {
        try {
            filteredPosElement.write(dos, session, location);
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    @Override
    public void setSession(Session session) {
        this.session = session;
        // + session.getSessionStart().getTime() + ";" +
        // session.getSessionStop().getTime() session.getDistance() + ";" +
        // session.getDuration() + ";";
        // eventElement = new EventElement();
        posElement = new PosElement();
        filteredPosElement = new FilteredPosElement();
        startElement = new StartElement();
        gpsElement = new GpsElement();
        commandElement = new CommandElement();
        // summaryOldElement = new SummaryOldElement();
        summaryElement = new SummaryElement();
        hxmElement = new HxmElement();
        stateElement = new StateElement();
        sensorDataElement = new SensorDataElement();
        sessionId = -1;
    }

    @Override
    public void onGpsStatusChanged(boolean active, boolean hasFix, int fixCount, int satCount) {
        try {
            gpsElement.write(dos, session, hasFix, fixCount, satCount, active);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void onSensorData(SensorProvider provider, SensorData sensorData) {
        try {
            sensorDataElement.write(dos, session, sensorData);
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    @Override
    public void onSensorStateChanged(SensorProvider provider, boolean active, SensorState sensorState) {
    }

    @Override
    public void onSensorDataChanged(HxmData hxmData) {
        try {
            hxmElement.write(dos, session, hxmData);
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    public void writeOffline() throws IOException {
        dos = createFileStream();
        sessionId = persistance.addSession(session);
        summaryElement.write(dos, session, session);
        closeFileStream(dos);
        persistance.updateSession(sessionId, session);
    }

    public void updateSession(SessionSummary session) throws IOException {
        dos = new DataOutputStream(new FileOutputStream(session.getFilename(), true));
        summaryElement = new SummaryElement();
        summaryElement.write(dos, session, session);
        Hint.log(this, "updateSession duration: " + DateUtils.secondsToHHMMSSString(session.getDuration() / 1000));
        closeFileStream(dos);
        persistance.updateSession(session.getId(), session);
    }

    private DataOutputStream createFileStream() {
        time.setToNow();
        File file = FileUtils.getFilename(context, time.format2445() + "_" + session.getType() + ".pts", "sessions");

        DataOutputStream dos = null;
        try {
            dos = new DataOutputStream(new FileOutputStream(file));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            dos = null;
        }

        session.setFilename(file.getAbsolutePath());
        int todoAltCorrection = 0;
        try {
            startElement.write(dos, session, fileVersion, session.getName(context), session.getSettings()
                    .getDescription(), session.getSettings().getComment(), session.getSettings().getPositionProvider()
                    .getName(), todoAltCorrection, session.getSettings().isAutoStart(), session.getSettings()
                    .isAutoPause());
            summaryElement.write(dos, session, session);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return dos;
    }

    private void closeFileStream(DataOutputStream dos) {
        if (dos != null)
            try {
                dos.close();
            } catch (IOException e) {
                e.printStackTrace();
                dos = null;
            }
    }

    @Override
    public void onSessionCommand(int command) {
        try {
            if (command == Session.SESSION_COMMAND_START) {
                dos = createFileStream();
                sessionId = persistance.addSession(session);
                summaryElement.write(dos, session, session);
                Hint.log(this, "SESSION_EVENT_STARTED");
            }

            if (command != Session.SESSION_COMMAND_DISCARD)
                commandElement.write(dos, session, command);

            if (command == Session.SESSION_COMMAND_CLOSE) {
                summaryElement.write(dos, session, session);
                closeFileStream(dos);
                persistance.updateSession(sessionId, session);
            } else if (command == Session.SESSION_COMMAND_DISCARD) {
                persistance.deleteSession(sessionId);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void update() {
    }

}
