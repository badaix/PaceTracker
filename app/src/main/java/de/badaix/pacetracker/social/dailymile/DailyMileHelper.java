package de.badaix.pacetracker.social.dailymile;

import de.badaix.pacetracker.session.SessionSummary;
import de.badaix.pacetracker.session.SessionType;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.social.dailymile.Distance.DistanceUnit;
import de.badaix.pacetracker.social.dailymile.Workout.ActivityType;
import de.badaix.pacetracker.util.Distance.Unit;

public class DailyMileHelper {
    public static ActivityType getActivity(SessionType type) {
        if (type.getType().equals("RunningSession"))
            return ActivityType.RUNNING;
        else if (type.getType().equals("HikingSession"))
            return ActivityType.HIKING;
        else if (type.getType().equals("InlineSession"))
            return ActivityType.INLINE_SKATING;
        else if (type.getType().equals("CyclingSession"))
            return ActivityType.CYCLING;
        else
            return null;
    }

    public static DistanceUnit getDistanceUnit(de.badaix.pacetracker.util.Distance.Unit unit) {
        if (unit == Unit.KILOMETERS)
            return DistanceUnit.KILOMETERS;
        else if (unit == Unit.MILES)
            return DistanceUnit.MILES;
        else if (unit == Unit.METER)
            return DistanceUnit.METERS;
        else if (unit == Unit.YARDS)
            return DistanceUnit.YARDS;
        else
            return null;
    }

    public static de.badaix.pacetracker.util.Distance.Unit getDistanceUnit(DistanceUnit unit) {
        if (unit == DistanceUnit.KILOMETERS)
            return Unit.KILOMETERS;
        else if (unit == DistanceUnit.MILES)
            return Unit.MILES;
        else if (unit == DistanceUnit.METERS)
            return Unit.METER;
        else if (unit == DistanceUnit.YARDS)
            return Unit.YARDS;
        else
            return null;
    }

    public static PostEntry sessionToEntry(SessionSummary session) {
        String message = session.getSettings().getComment();
        Position position = null;
        if (session.getStartPos() != null)
            position = new Position((float) session.getStartPos().latitude, (float) session.getStartPos().longitude);
        Workout workout = new Workout(getActivity(session), session.getSessionStop(), new Distance(
                (float) de.badaix.pacetracker.util.Distance.distanceToDouble(session.getDistance()),
                getDistanceUnit(GlobalSettings.getInstance().getDistUnit())), Integer.valueOf((int) (session
                .getDuration() / 1000)), session.getSettings().getFelt(), Integer.valueOf(session.getCalories()),
                session.getSettings().getDescription(), null);

        return new PostEntry(message, position, workout);
    }
}
