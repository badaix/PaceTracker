package de.badaix.pacetracker.session;

import android.content.Context;

import java.util.Vector;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.settings.SessionSettings;

public class SessionFactory {
    private static SessionFactory instance = null;
    private static Context ctx = null;
    private Vector<SessionType> vecTypeName = null;

    public static SessionFactory getInstance(Context context) {
        ctx = context;
        if (instance == null) {
            instance = new SessionFactory();
            instance.fillVec();
        }
        return instance;
    }

    public static SessionFactory getInstance() {
        if (instance == null) {
            instance = new SessionFactory();
            instance.fillVec();
        }
        return instance;
    }

    private void addSession(Session session) {
        vecTypeName.add(new SessionType(session.getType(), session.getName(ctx), session.getVerb(ctx), session
                .getDrawable(), session.getLightDrawable()));
    }

    private void fillVec() {
        vecTypeName = new Vector<SessionType>();
        addSession(new RunningSession(null, null));
        addSession(new CyclingSession(null, null));
        addSession(new HikingSession(null, null));
        addSession(new InlineSession(null, null));
        addSession(new SailingSession(null, null));
        addSession(new CarSession(null, null));
        addSession(new ScooterSession(null, null));
        addSession(new TestSession(null, null));
    }

    public Vector<SessionType> getSessionTypeName() {
        return vecTypeName;
    }

    public SessionType getSessionData(String type) {
        for (int i = 0; i < vecTypeName.size(); ++i)
            if (vecTypeName.get(i).type.equals(type))
                return vecTypeName.get(i);
        return null;
    }

    public String getSessionNameFromType(String type) {
        for (int i = 0; i < vecTypeName.size(); ++i)
            if (vecTypeName.get(i).type.equals(type))
                return vecTypeName.get(i).name;

        return "";
    }

    public String getSessionVerbFromType(String type) {
        for (int i = 0; i < vecTypeName.size(); ++i)
            if (vecTypeName.get(i).type.equals(type))
                return vecTypeName.get(i).verb;

        return "";
    }

    public int getSessionDrawableFromType(String type) {
        for (int i = 0; i < vecTypeName.size(); ++i)
            if (vecTypeName.get(i).type.equals(type))
                return vecTypeName.get(i).drawable;

        return R.drawable.icon;
    }

    public int getSessionLightDrawableFromType(String type) {
        for (int i = 0; i < vecTypeName.size(); ++i)
            if (vecTypeName.get(i).type.equals(type))
                return vecTypeName.get(i).lightDrawable;

        return R.drawable.icon;
    }

    public Session getSessionByType(String type, SessionListener listener, SessionSettings settings) {
        if (settings != null)
            settings.setSessionType(type);
        if (type.equals("RunningSession"))
            return new RunningSession(listener, settings);
        else if (type.equals("CyclingSession"))
            return new CyclingSession(listener, settings);
        else if (type.equals("HikingSession"))
            return new HikingSession(listener, settings);
        else if (type.equals("InlineSession"))
            return new InlineSession(listener, settings);
        else if (type.equals("SailingSession"))
            return new SailingSession(listener, settings);
        else if (type.equals("CarSession"))
            return new CarSession(listener, settings);
        else if (type.equals("ScooterSession"))
            return new ScooterSession(listener, settings);
        else if (type.equals("TestSession"))
            return new TestSession(listener, settings);
        else
            return null;
    }
}
