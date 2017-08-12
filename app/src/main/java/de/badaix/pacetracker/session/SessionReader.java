package de.badaix.pacetracker.session;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;

import de.badaix.pacetracker.SessionPersistance;
import de.badaix.pacetracker.goal.GoalFactory;
import de.badaix.pacetracker.posprovider.PositionProviderFactory;
import de.badaix.pacetracker.session.Session.State;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.settings.SessionSettings;
import de.badaix.pacetracker.util.FileUtils;
import de.badaix.pacetracker.util.Hint;

public class SessionReader {
    private DataInputStream dis;
    private EventElement eventElement;
    private PosElement posElement;
    private FilteredPosElement filteredPosElement;
    private StartElement startElement;
    private GpsElement gpsElement;
    private CommandElement commandElement;
    private SummaryOldElement summaryOldElement;
    private SummaryElement summaryElement;
    private HxmElement hxmElement;
    private StateElement stateElement;
    private SensorDataElement sensorDataElement;
    private int fileVersion;
    private SessionHeader header;

    // private boolean isV1;

    public void openSession(String filename) throws FileNotFoundException {
        fileVersion = 1;
        dis = new DataInputStream(new FileInputStream(filename));
        filteredPosElement = new FilteredPosElement();
        eventElement = new EventElement();
        posElement = new PosElement();
        startElement = new StartElement();
        gpsElement = new GpsElement();
        commandElement = new CommandElement();
        summaryOldElement = new SummaryOldElement();
        hxmElement = new HxmElement();
        stateElement = new StateElement();
        sensorDataElement = new SensorDataElement();
        summaryElement = new SummaryElement();
        header = new SessionHeader();
        try {
            if (dis.readByte() == SessionHeaderV1.magicNum)
                header = new SessionHeaderV1();
        } catch (IOException e) {
            Hint.log(this, "Stream is exception: " + e.getMessage());
            e.printStackTrace();
        }

        Hint.log(this, "Stream header is: " + header.getClass().getSimpleName());
        dis = new DataInputStream(new FileInputStream(filename));
    }

    public boolean getNextHeader(SessionHeader header, boolean skip) throws IOException {
        try {
            if (header instanceof SessionHeaderV1) {
                if (dis.readByte() != SessionHeaderV1.magicNum)
                    throw new IOException("Invalid message header");
                ((SessionHeaderV1) header).len = dis.readInt();
                header.msgType = dis.readInt();
            } else {
                // vielleicht doch V0
                header.msgType = dis.readInt();
                if ((header.msgType >> 24) == SessionHeaderV1.magicNum) {
                    dis.readByte();
                    header.msgType = dis.readInt();
                }
            }
            if (skip) {
                dis.skipBytes(24);
            } else {
                header.now = dis.readLong();
                header.duration = dis.readLong();
                header.distance = dis.readDouble();
            }
        } catch (EOFException e) {
            return false;
        }
        return true;
    }

    public boolean skipElement(SessionHeader header) {
        if ((dis == null) || (header == null))
            return false;

        if (header instanceof SessionHeaderV1) {
            int len = ((SessionHeaderV1) header).len;
            dis.mark(len + 1);
            try {
                return (len == dis.skipBytes(len));
            } catch (IOException e) {
                try {
                    dis.reset();
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                return false;
            }
        }

        return false;
    }

    public SessionElement getNextElement(SessionHeader header) {
        if ((dis == null) || (header == null))
            return null;

        try {
            if (header.msgType == commandElement.getType().toInt()) {
                commandElement.read(dis, header);
                return commandElement;
            } else if (header.msgType == eventElement.getType().toInt()) {
                eventElement.read(dis, header);
                return eventElement;
            } else if (header.msgType == gpsElement.getType().toInt()) {
                gpsElement.read(dis, header);
                return gpsElement;
            } else if (header.msgType == posElement.getType().toInt()) {
                posElement.read(dis, header);
                return posElement;
            } else if (header.msgType == filteredPosElement.getType().toInt()) {
                filteredPosElement.read(dis, header);
                return filteredPosElement;
            } else if (header.msgType == startElement.getType().toInt()) {
                startElement.read(dis, header);
                return startElement;
            } else if (header.msgType == summaryOldElement.getType().toInt()) {
                summaryOldElement.read(dis, header);
                return summaryOldElement;
            } else if (header.msgType == hxmElement.getType().toInt()) {
                hxmElement.read(dis, header);
                return hxmElement;
            } else if (header.msgType == stateElement.getType().toInt()) {
                stateElement.read(dis, header);
                return stateElement;
            } else if (header.msgType == sensorDataElement.getType().toInt()) {
                sensorDataElement.read(dis, header);
                return sensorDataElement;
            } else if (header.msgType == summaryElement.getType().toInt()) {
                summaryElement.read(dis, header);
                return summaryElement;
            } else {
                Hint.log(this, "Unknown event: " + MsgType.fromInt(header.msgType));
            }
        } catch (Exception e) {
            Hint.log(this, "Exception e: " + e.getMessage());
            e.printStackTrace();
            try {
                dis.close();
                dis = null;
            } catch (Exception ex) {
                Hint.log(this, "Exception ex: " + ex.getMessage());
                ex.printStackTrace();
            }
            return null;
        }
        return null;
    }

    public SessionSummary readSummaryFromFile(String filename) throws FileNotFoundException, IOException, JSONException {
        SessionElement element = null;
        SessionSummary result = new SessionSummary((SessionSettings) null);
        // boolean summaryFound = false;

        openSession(filename);
        while (getNextHeader(header, true)) {
            if (header.msgType != MsgType.SUMMARY.toInt())
                if (skipElement(header))
                    continue;

            element = getNextElement(header);
            if (element.getType() == MsgType.SUMMARY) {
                result.fromJson(((SummaryElement) element).getSessionSummary().toJson());
                Hint.log(this, "Summary: " + result.toJson());
                // summaryFound = true;
            }
        }
        dis.close();
        return result;
    }

    public Session readSessionFromFile(File file) throws FileNotFoundException, IOException {
        SessionElement element = null;
        SessionElement lastElement = null;
        String filename = file.getName();
        if (!filename.contains("_") || !filename.contains("."))
            return null;
        String sessionType = filename.substring(filename.indexOf("_") + 1, filename.indexOf("."));
        Hint.log(this, "type: " + sessionType);
        filename = file.getAbsolutePath();
        openSession(filename);
        Session session = SessionFactory.getInstance().getSessionByType(sessionType, null, new SessionSettings(false));
        fileVersion = 1;
        while (getNextHeader(header, false)) {
            // if ((fileVersion == 2) && (header.msgType ==
            // MsgType.POS.toInt()))
            // if (skipElement(header))
            // continue;

            element = getNextElement(header);
            if (element.getType() == MsgType.EVENT) {
                EventElement event = (EventElement) element;
                // session.newEvent(event.getEvent(), event.getDescription());
                final int SESSION_EVENT_STARTED = 3;
                final int SESSION_EVENT_STOPPED = 4;
                if (event.getEvent() == SESSION_EVENT_STARTED)
                    session.setSessionStart(new Date(element.header.now));
                else if (event.getEvent() == SESSION_EVENT_STOPPED)
                    session.setSessionStop(new Date(element.header.now));
            } else if (element.getType() == MsgType.STATE) {
                StateElement stateElement = (StateElement) element;
                if (stateElement.getNewState() == State.RUNNING)
                    session.setSessionStart(new Date(element.header.now));
                else if (stateElement.getNewState() == State.STOPPED)
                    session.setSessionStop(new Date(element.header.now));
            } else if (element.getType() == MsgType.POS) {
                lastElement = element;
                if (fileVersion == 1) {
                    PosElement pos = (PosElement) element;
                    if (session.getStartPos() == null)
                        session.setStartPos(new GeoPos(pos.getLocation().getLatitude(), pos.getLocation()
                                .getLongitude()));
                    session.setMaxSpeed(Math.max(session.getMaxSpeed(), pos.getLocation().getSpeed() * 3.6f));
                    session.getGpsPos().add(
                            new GpsPos(pos.getLocation().getLatitude(), pos.getLocation().getLongitude(), pos
                                    .getLocation().getAltitude(), pos.getLocation().getTime(), pos.getLocation()
                                    .getSpeed(), pos.getLocation().getBearing(), pos.header.duration,
                                    pos.header.distance));
                }
            } else if (element.getType() == MsgType.FILTERED_POS) {
                lastElement = element;
                if (fileVersion >= 2) {
                    PosElement pos = (PosElement) element;
                    if (session.getStartPos() == null)
                        session.setStartPos(new GeoPos(pos.getLocation().getLatitude(), pos.getLocation()
                                .getLongitude()));
                    session.setMaxSpeed(Math.max(session.getMaxSpeed(), pos.getLocation().getSpeed() * 3.6f));
                    session.getGpsPos().add(
                            new GpsPos(pos.getLocation().getLatitude(), pos.getLocation().getLongitude(), pos
                                    .getLocation().getAltitude(), pos.getLocation().getTime(), pos.getLocation()
                                    .getSpeed(), pos.getLocation().getBearing(), pos.header.duration,
                                    pos.header.distance));
                }
            } else if (element.getType() == MsgType.START) {
                fileVersion = ((StartElement) element).getFileVersion();
                session.getSettings().setPositionProvider(
                        PositionProviderFactory.getPosProvider(GlobalSettings.getInstance().getContext(),
                                ((StartElement) element).getProviderName()));
            } else if (element.getType() == MsgType.SUMMARY) {
                session.fromJson(((SummaryElement) element).getSessionSummary().toJson());
            } else if (element.getType() == MsgType.SUMMARY_OLD) {
                SummaryOldElement summary = (SummaryOldElement) element;
                session.setCalories(summary.getCalories());
                session.setDistance(summary.getDistance());
                session.setDuration(summary.getDuration());
                session.setMaxSpeed(summary.getMaxSpeed());
                session.setTotalPause(summary.getStop() - summary.getStart() - summary.getDuration());
                session.getSettings().setComment(summary.getComment());
                session.getSettings().setDailyMileId(summary.getDailyMileId());
                session.getSettings().setDescription(summary.getDescription());
                session.getSettings().setFelt(Felt.fromString(summary.getFelt()));
                try {
                    session.getSettings().setGoal(
                            GoalFactory.getOfflineGoal(summary.getGoal(), GlobalSettings.getInstance().getContext()));
                    if (summary.getGoalSettings().length() > 0) {
                        session.getSettings().getGoal().initFromJson(new JSONObject(summary.getGoalSettings()));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                session.setStartPos(new GeoPos(summary.getLatStart(), summary.getLonStart()));
                session.getSettings().setPositionProvider(
                        PositionProviderFactory.getOfflinePosProvider(GlobalSettings.getInstance().getContext(),
                                summary.getProviderName()));

                // session.setId(id)
                // session.setFilename(filename)
                session.setSessionStart(new Date(summary.getStart()));
                session.setSessionStop(new Date(summary.getStop()));
            }
        }

        if ((session.getSessionStop() == null) && (lastElement != null)) {
            session.setSessionStop(new Date(lastElement.header.now));
            // needsUpdate = true;
            session.setDistance(lastElement.header.distance);
            session.setDuration(lastElement.header.duration);
            Hint.log(this, "SessionStart: " + session.getSessionStart().getTime());
            Hint.log(this, "SessionStop: " + session.getSessionStop().getTime());
            Hint.log(this, "last.header.now: " + lastElement.header.now);
            Hint.log(this, "last.header.duration: " + lastElement.header.duration);
        }
        dis.close();
        SessionPersistance persistance = SessionPersistance.getInstance(GlobalSettings.getInstance().getContext());
        File dest = FileUtils.getFilename(GlobalSettings.getInstance().getContext(), file.getName(), "sessions");
        FileUtils.copy(file, dest);
        file.delete();
        session.setFilename(dest.getAbsolutePath());
        session.setCalories(session.getCalories());
        long sessionId = persistance.addSession(session);
        persistance.close();
        session.setId((int) sessionId);
        SessionWriter sessionWriter = new SessionWriter(GlobalSettings.getInstance().getContext());
        sessionWriter.updateSession(session);
        return null;
        //
        // Session = SessionFactory.getInstance().getSessionByType(type,
        // listener, settings)
        // session.state = State.OFFLINE;
        //
        // //TODO: lat/lon start
        // openSession(filename);
        // while (getNextHeader(header, false)) {
        // if ((fileVersion == 2) && (header.msgType == MsgType.POS.toInt()))
        // if (skipElement(header))
        // continue;
        //
        // element = getNextElement(header);
        // // Hint.log(this, element.toString());
        // if (element.getType() == MsgType.COMMAND) {
        // Hint.log(this, element.toString());
        // } else if (element.getType() == MsgType.EVENT) {
        // EventElement event = (EventElement) element;
        // // session.newEvent(event.getEvent(), event.getDescription());
        // final int SESSION_EVENT_STARTED = 3;
        // final int SESSION_EVENT_STOPPED = 4;
        // if (event.getEvent() == SESSION_EVENT_STARTED)
        // session.setSessionStart(new Date(element.header.now));
        // else if (event.getEvent() == SESSION_EVENT_STOPPED) {
        // session.setSessionStop(new Date(element.header.now));
        // Hint.log(this, "Start: "
        // + session.getSessionStart().toGMTString()
        // + "\nStop: "
        // + session.getSessionStop().toGMTString()
        // + "\nTotal duration: "
        // + (session.getSessionStop().getTime() - session.getSessionStart()
        // .getTime()) + ", duration: " + element.header.duration
        // + "\nduration: " + session.getDuration());
        // }
        // } else if (element.getType() == MsgType.GPS) {
        // // Hint.log(this, element.toString());
        // } else if (element.getType() == MsgType.STATE) {
        // StateElement stateElement = (StateElement)element;
        // if (stateElement.getNewState() == State.RUNNING)
        // session.setSessionStart(new Date(element.header.now));
        // else if (stateElement.getNewState() == State.STOPPED)
        // session.setSessionStop(new Date(element.header.now));
        // } else if (element.getType() == MsgType.HXM) {
        // HxmElement hxm = (HxmElement)element;
        // session.getHxmData().add(hxm.getHxmData());
        // } else if (element.getType() == MsgType.SENSORDATA) {
        // } else if (element.getType() == MsgType.POS) {
        // lastElement = element;
        // if (fileVersion == 1) {
        // PosElement pos = (PosElement) element;
        // session.setMaxSpeed(Math.max(session.getMaxSpeed(),
        // pos.getLocation().getSpeed() * 3.6f));
        // session.getGpsPos().add(
        // new GpsPos(pos.getLocation().getLatitude(),
        // pos.getLocation().getLongitude(), pos.getLocation()
        // .getAltitude(), pos.getLocation().getTime(),
        // pos.getLocation().getSpeed(),
        // pos.getLocation().getBearing(), pos.header.duration,
        // pos.header.distance));
        // }
        // } else if (element.getType() == MsgType.FILTERED_POS) {
        // lastElement = element;
        // if (fileVersion >= 2) {
        // PosElement pos = (PosElement) element;
        // session.setMaxSpeed(Math.max(session.getMaxSpeed(),
        // pos.getLocation().getSpeed() * 3.6f));
        // session.getGpsPos().add(
        // new GpsPos(pos.getLocation().getLatitude(),
        // pos.getLocation().getLongitude(), pos.getLocation()
        // .getAltitude(), pos.getLocation().getTime(),
        // pos.getLocation().getSpeed(),
        // pos.getLocation().getBearing(), pos.header.duration,
        // pos.header.distance));
        // }
        // } else if (element.getType() == MsgType.START) {
        // fileVersion = ((StartElement) element).getFileVersion();
        // session.getSettings().setPositionProvider(PositionProviderFactory.getPosProvider(GlobalSettings.getInstance().getContext(),
        // ((StartElement) element).getProviderName()));
        // } else if (element.getType() == MsgType.SUMMARY) {
        // session.fromJson(((SummaryElement)element).getSessionSummary().toJson());
        // } else if (element.getType() == MsgType.SUMMARY_OLD) {
        // SummaryOldElement summary = (SummaryOldElement)element;
        // Hint.log(this, summary.toString());
        // }
        // }
        //
        // Hint.log(this, "SessionId: " + session.getId());
        // boolean needsUpdate = false;
        // if ((session.getSessionStop() == null) && (lastElement != null)) {
        // session.setSessionStop(new Date(lastElement.header.now));
        // needsUpdate = true;
        // session.setDistance(lastElement.header.distance);
        // session.setDuration(lastElement.header.duration);
        // Hint.log(this, "SessionStart: " +
        // session.getSessionStart().getTime());
        // Hint.log(this, "SessionStop: " + session.getSessionStop().getTime());
        // Hint.log(this, "last.header.now: " + lastElement.header.now);
        // Hint.log(this, "last.header.duration: " +
        // lastElement.header.duration);
        // }
        //
        // session.totalPause = (session.getSessionStop().getTime() -
        // session.getSessionStart()
        // .getTime()) - lastElement.header.duration;
        // session.totalPause = Math.max(0, session.totalPause);
        // Hint.log(this, "Session.totalPause: " + session.totalPause);
        // session.setDistance(lastElement.header.distance);
        // session.updateBoundingBox();
        // session.setFilename(filename);
        // dis.close();
        //
        // if (needsUpdate) {
        // SessionWriter sessionWriter = new
        // SessionWriter(GlobalSettings.getInstance().getContext());
        // sessionWriter.updateSession(session);
        // }
    }

    public void readSessionFromFile(String filename, Session session) throws FileNotFoundException, IOException {
        SessionElement element = null;
        SessionElement lastElement = null;
        session.state = State.OFFLINE;
        int id = session.getId();

        // TODO: lat/lon start
        openSession(filename);
        while (getNextHeader(header, false)) {
            if ((fileVersion == 2) && (header.msgType == MsgType.POS.toInt()))
                if (skipElement(header))
                    continue;

            element = getNextElement(header);
            // Hint.log(this, element.toString());
            if (element.getType() == MsgType.COMMAND) {
                Hint.log(this, element.toString());
            } else if (element.getType() == MsgType.EVENT) {
                EventElement event = (EventElement) element;
                // session.newEvent(event.getEvent(), event.getDescription());
                final int SESSION_EVENT_STARTED = 3;
                final int SESSION_EVENT_STOPPED = 4;
                if (event.getEvent() == SESSION_EVENT_STARTED)
                    session.setSessionStart(new Date(element.header.now));
                else if (event.getEvent() == SESSION_EVENT_STOPPED) {
                    session.setSessionStop(new Date(element.header.now));
                    Hint.log(this, "Start: " + session.getSessionStart().toGMTString() + "\nStop: "
                            + session.getSessionStop().toGMTString() + "\nTotal duration: "
                            + (session.getSessionStop().getTime() - session.getSessionStart().getTime())
                            + ", duration: " + element.header.duration + "\nduration: " + session.getDuration());
                }
            } else if (element.getType() == MsgType.GPS) {
                // Hint.log(this, element.toString());
            } else if (element.getType() == MsgType.STATE) {
                StateElement stateElement = (StateElement) element;
                if (stateElement.getNewState() == State.RUNNING)
                    session.setSessionStart(new Date(element.header.now));
                else if (stateElement.getNewState() == State.STOPPED)
                    session.setSessionStop(new Date(element.header.now));
            } else if (element.getType() == MsgType.HXM) {
                HxmElement hxm = (HxmElement) element;
                session.getHxmData().add(hxm.getHxmData());
            } else if (element.getType() == MsgType.SENSORDATA) {
            } else if (element.getType() == MsgType.POS) {
                lastElement = element;
                if (fileVersion == 1) {
                    PosElement pos = (PosElement) element;
                    session.setMaxSpeed(Math.max(session.getMaxSpeed(), pos.getLocation().getSpeed() * 3.6f));
                    session.getGpsPos().add(
                            new GpsPos(pos.getLocation().getLatitude(), pos.getLocation().getLongitude(), pos
                                    .getLocation().getAltitude(), pos.getLocation().getTime(), pos.getLocation()
                                    .getSpeed(), pos.getLocation().getBearing(), pos.header.duration,
                                    pos.header.distance));
                }
            } else if (element.getType() == MsgType.FILTERED_POS) {
                lastElement = element;
                if (fileVersion >= 2) {
                    PosElement pos = (PosElement) element;
                    session.setMaxSpeed(Math.max(session.getMaxSpeed(), pos.getLocation().getSpeed() * 3.6f));
                    session.getGpsPos().add(
                            new GpsPos(pos.getLocation().getLatitude(), pos.getLocation().getLongitude(), pos
                                    .getLocation().getAltitude(), pos.getLocation().getTime(), pos.getLocation()
                                    .getSpeed(), pos.getLocation().getBearing(), pos.header.duration,
                                    pos.header.distance));
                }
            } else if (element.getType() == MsgType.START) {
                fileVersion = ((StartElement) element).getFileVersion();
                session.getSettings().setPositionProvider(
                        PositionProviderFactory.getPosProvider(GlobalSettings.getInstance().getContext(),
                                ((StartElement) element).getProviderName()));
            } else if (element.getType() == MsgType.SUMMARY) {
                session.fromJson(((SummaryElement) element).getJson());
            } else if (element.getType() == MsgType.SUMMARY_OLD) {
                SummaryOldElement summary = (SummaryOldElement) element;
                Hint.log(this, summary.toString());
            }
        }

        if (session.getId() == -1)
            session.setId(id);
        Hint.log(this, "SessionId: " + session.getId());
        boolean needsUpdate = false;
        if ((session.getSessionStop() == null) && (lastElement != null)) {
            session.setSessionStop(new Date(lastElement.header.now));
            needsUpdate = true;
            session.setDistance(lastElement.header.distance);
            session.setDuration(lastElement.header.duration);
            session.totalPause = (session.getSessionStop().getTime() - session.getSessionStart().getTime())
                    - lastElement.header.duration;
            session.totalPause = Math.max(0, session.totalPause);
            Hint.log(this, "Session.totalPause: " + session.totalPause);
            session.setDistance(lastElement.header.distance);

            Hint.log(this, "SessionStart: " + session.getSessionStart().getTime());
            Hint.log(this, "SessionStop: " + session.getSessionStop().getTime());
            Hint.log(this, "last.header.now: " + lastElement.header.now);
            Hint.log(this, "last.header.duration: " + lastElement.header.duration);
        }

        session.updateBoundingBox();
        session.setFilename(filename);
        dis.close();

        if (needsUpdate) {
            SessionWriter sessionWriter = new SessionWriter(GlobalSettings.getInstance().getContext());
            sessionWriter.updateSession(session);
        }

		/*
         * Hint.log(this, "starting with Distance"); double totalDistance = 0;
		 * for (int i=1; i<session.getGpsPos().size(); ++i) totalDistance +=
		 * Distance.calculateDistance(session.getGpsPos().get(i-1),
		 * session.getGpsPos().get(i)); Hint.log(this, "Distance: " +
		 * totalDistance);
		 * 
		 * CheapDistance cheapDistance = new
		 * CheapDistance(session.getGpsPos().firstElement()); Hint.log(this,
		 * "starting with CheapDistance"); totalDistance = 0; for (int i=1;
		 * i<session.getGpsPos().size(); ++i) totalDistance +=
		 * cheapDistance.distance(session.getGpsPos().get(i-1),
		 * session.getGpsPos().get(i)); Hint.log(this, "Distance: " +
		 * totalDistance);
		 */
    }
}
